package io.taucoin.torrent.publishing.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.utils.AppUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.receiver.NotificationReceiver;
import io.taucoin.torrent.publishing.ui.TauNotifier;

/**
 * TAUService: 链端业务服务
 * 包含初始化、启动、停止等
 */
public class TauService extends Service {

    private static final String TAG = TauService.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TAG);
    public static final String ACTION_SHUTDOWN = "io.taucoin.torrent.publishing.service.ACTION_SHUTDOWN";

    // 是不是已经在运行
    private AtomicBoolean isAlreadyRunning = new AtomicBoolean(false);
    private TauDaemon daemon;
    private UserRepository userRepo;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        userRepo = RepositoryHelper.getUserRepository(getApplicationContext());
        daemon = TauDaemon.getInstance(getApplicationContext());
        TauNotifier.makeForegroundNotify(this);
        Utils.enableBootReceiver(getApplicationContext(), true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.warn("Process.myPid::{}", android.os.Process.myPid());
        String action = null;
        if (intent != null){
            action = intent.getAction();
        }

        // 处理关闭动作
        if (action != null && (StringUtil.isEquals(action, ACTION_SHUTDOWN)
                || StringUtil.isEquals(action, NotificationReceiver.NOTIFY_ACTION_SHUTDOWN_APP))) {
            shutdown();
            return START_NOT_STICKY;
        }

        // 是不是已经在运行
        if (isAlreadyRunning.compareAndSet(false, true)){
            subscribeCurrentUser();
        }
        return START_NOT_STICKY;
    }

    /**
     * 订阅当前用户
     */
    private void subscribeCurrentUser() {
        final AtomicBoolean isAlreadyInit = new AtomicBoolean(false);
        disposables.add(userRepo.observeCurrentUser()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(user -> {
                    if (null == user) {
                        return;
                    }
                    // 更新内存中用户信息
                    MainApplication.getInstance().setCurrentUser(user);
                    logger.info("Update userPk::{}", user.publicKey);
                    logger.info("Update user seed");
                    if(isAlreadyInit.compareAndSet(false, true)){
                        // 更新设置用户seed
                        daemon.updateSeed(user.seed);
                        initAndStart(user.seed);
                    }
                }));
    }

    /**
     * 初始化并启动TauDaemon
     */
    private void initAndStart(String seed) {
        logger.info("initAndStart {}", TAG);

        TauNotifier.makeForegroundNotify(this);

        daemon.doStart(seed);
    }

    /**
     * 停止服务：动作需要在Tau链组件全部停止之后调用
     */
    private void stopService() {
        logger.info("stopService");
        disposables.clear();

        isAlreadyRunning.set(false);
        WorkloadManager.stopWakeUpWorker(getApplicationContext());
        stopForeground(true);
        stopSelf();
    }

    /**
     * 关闭APP
     */
    private void shutdown() {
        logger.info("shutdown");
        disposables.add(Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            logger.info("Wait daemon stop");
            daemon.doStop();
            logger.info("Daemon stop successfully");
            emitter.onNext(true);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::stopServiceWaitAlertDisposed, it -> {
                    logger.error("Daemon stop error ", it);
                    stopService();
                }));
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        logger.info("onTaskRemoved");
        shutdown();
    }

    /**
     * 等待Alert被处理完停止服务
     */
    private void stopServiceWaitAlertDisposed(Boolean aBoolean) {
        ObservableEmitter alertConsumerEmitter = daemon.getAlertConsumerEmitter();
        if (alertConsumerEmitter != null && !alertConsumerEmitter.isDisposed()) {
            logger.info("Wait alert disposed");
            daemon.getAlertConsumerEmitter().setDisposable(Disposables.fromAction(this::stopService));
        } else {
            stopService();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.clear();
        logger.info("Stop {}", TAG);
        AppUtil.killProcess();
    }
}
