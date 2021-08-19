package io.taucoin.torrent.publishing.core.model;

import android.content.Context;
import android.content.Intent;

import org.libTAU4j.Message;
import org.libTAU4j.SessionHandle;
import org.libTAU4j.SessionManager;
import org.libTAU4j.SessionParams;
import org.libTAU4j.alerts.Alert;
import org.libTAU4j.alerts.AlertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.AlertAndUser;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.AppUtil;
import io.taucoin.torrent.publishing.core.utils.DeviceUtils;
import io.taucoin.torrent.publishing.core.utils.FrequencyUtil;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.SessionStatistics;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.TrafficUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.receiver.ConnectionReceiver;
import io.taucoin.torrent.publishing.service.SystemServiceManager;
import io.taucoin.torrent.publishing.receiver.PowerReceiver;
import io.taucoin.torrent.publishing.service.TauService;
import io.taucoin.torrent.publishing.ui.setting.TrafficTipsActivity;

/**
 * 区块链业务Daemon
 */
public abstract class TauDaemon {
    private static final String TAG = TauDaemon.class.getSimpleName();
    static final Logger logger = LoggerFactory.getLogger(TAG);
    private static final int REOPEN_NETWORKS_THRESHOLD = 15; // 单位s
    static final int ALERT_QUEUE_CAPACITY = 10000; // Alert缓存队列
    private static volatile TauDaemon instance;

    private Context appContext;
    private SettingsRepository settingsRepo;
    CompositeDisposable disposables = new CompositeDisposable();
    private PowerReceiver powerReceiver = new PowerReceiver();
    private ConnectionReceiver connectionReceiver = new ConnectionReceiver();
    SessionManager sessionManager;
    private SystemServiceManager systemServiceManager;
    private ExecutorService exec = Executors.newSingleThreadExecutor();
    private TauInfoProvider tauInfoProvider;
    private Disposable updateInterfacesTimer; // 更新libTAU监听接口定时任务
    volatile boolean isRunning = false;
    private volatile boolean trafficTips = true; // 剩余流量用完提示
    volatile String seed;
    String deviceID;
    private long noRemainingDataTimes = 0; // 触发无剩余流量的次数
    private int noNodesCount = 0; // 无节点计数, nodes查询频率为1s

    // libTAU上报的Alert缓存队列
    LinkedBlockingQueue<AlertAndUser> alertQueue = new LinkedBlockingQueue<>();
    AtomicBoolean isSessionStopped = new AtomicBoolean(false);

    public static TauDaemon getInstance(@NonNull Context appContext) {
        if (instance == null) {
            synchronized (TauDaemon.class) {
                if (instance == null)
                    instance = new TauDaemonImpl(appContext);
            }
        }
        return instance;
    }

    /**
     * TauDaemon构造函数
     */
    TauDaemon(@NonNull Context appContext) {
        this.appContext = appContext;
        settingsRepo = RepositoryHelper.getSettingsRepository(appContext);
        systemServiceManager = SystemServiceManager.getInstance();
        tauInfoProvider = TauInfoProvider.getInstance(this);

        deviceID = DeviceUtils.getCustomDeviceID(appContext);
        sessionManager = new SessionManager(true);

        observeTauDaemon();
        initLocalParam();
        handleNoRemainingDataTips();
    }

    private void observeTauDaemon() {
        // 监听libTAU启动成功
        sessionManager.addListener(new TauDaemonAlertListener() {

            @Override
            public int[] types() {
                return new int[]{AlertType.SES_START_OVER.swig()};
            }

            @Override
            public void alert(Alert<?> alert) {
                if (alert != null && alert.type() == AlertType.SES_START_OVER) {
                    logger.debug("Tau start successfully");
                    isRunning = true;
                    handleSettingsChanged(appContext.getString(R.string.pref_key_foreground_running));
                    // 把自己当作朋友添加进libTAU
                    addYourselfAsFriend();
                }
            }
        });
        observeTauDaemonAlertListener();
        disposables.add(settingsRepo.observeSettingsChanged()
                .subscribe(this::handleSettingsChanged));
        disposables.add(tauInfoProvider.observeAppStatistics()
                .subscribeOn(Schedulers.io())
                .subscribe());
        disposables.add(tauInfoProvider.observeSessionStats()
            .subscribeOn(Schedulers.io())
                .subscribe(nodes -> {
                    if (nodes > 0) {
                        noNodesCount = 0;
                    } else {
                        noNodesCount += 1;
                        if (noNodesCount > REOPEN_NETWORKS_THRESHOLD) {
                            logger.trace("No nodes more than {}s, restartSessions...",
                                    REOPEN_NETWORKS_THRESHOLD);
                            restartSessions();
                            noNodesCount = 0;
                        }
                    }
                }));
    }

    /**
     * 初始化本地参数
     */
    private void initLocalParam() {
        switchPowerReceiver();
        switchConnectionReceiver();
        settingsRepo.setUPnpMapped(false);
        settingsRepo.setNATPMPMapped(false);
        // 初始化主循环频率
        FrequencyUtil.clearMainLoopIntervalList();
    }

    /**
     * Daemon启动
     */
    public void start() {
        if (isRunning){
            return;
        }
        Intent intent = new Intent(appContext, TauService.class);
        Utils.startServiceBackground(appContext, intent);
    }

    /**
     * 观察是否需要启动Daemon
     * @return Flowable
     */
    public Flowable<Boolean> observeNeedStartDaemon() {
        return Flowable.create((emitter) -> {
            if (emitter.isCancelled()){
                return;
            }
            Runnable emitLoop = () -> {
                while (!Thread.interrupted()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (emitter.isCancelled() || isRunning){
                        return;
                    }
                    emitter.onNext(true);
                }
            };

            Disposable d = observeDaemonRunning()
                    .subscribeOn(Schedulers.io())
                    .subscribe((isRunning) -> {
                        if (emitter.isCancelled())
                            return;

                        if (!isRunning) {
                            emitter.onNext(true);
                            exec.submit(emitLoop);
                        }
                    });
            if (!emitter.isCancelled()) {
                emitter.onNext(!isRunning);
                emitter.setDisposable(d);
            }

        }, BackpressureStrategy.LATEST);
    }

    /**
     * 观察Daemon是否是在运行
     */
    public Flowable<Boolean> observeDaemonRunning() {
        return Flowable.create((emitter) -> {
            if (emitter.isCancelled())
                return;

            TauDaemonAlertListener listener = new TauDaemonAlertListener() {

                @Override
                public int[] types() {
                    return new int[]{AlertType.SES_START_OVER.swig(), AlertType.SES_STOP_OVER.swig()};
                }

                @Override
                public void alert(Alert<?> alert) {
                    if (alert != null && alert.type() == AlertType.SES_START_OVER) {
                        if (!emitter.isCancelled())
                            emitter.onNext(true);
                    } else if (alert != null && alert.type() == AlertType.SES_STOP_OVER) {
                        if (!emitter.isCancelled())
                            emitter.onNext(false);
                    }
                }
            };

            if (!emitter.isCancelled()) {
                emitter.onNext(isRunning);
                registerAlertListener(listener);
                emitter.setDisposable(Disposables.fromAction(() -> unregisterAlertListener(listener)));
            }

        }, BackpressureStrategy.LATEST);
    }

    /**
     * Only calls from TauService
     */
    public void doStart(String seed) {
        String networkAddress = getNetworkInterface();
        if (StringUtil.isEmpty(networkAddress)) {
            logger.info("Failed to get network address");
            return;
        }
        logger.info("doStart");
        if (isRunning)
            return;

        // 设置SessionManager启动参数
        SessionParams sessionParams = SessionSettings.getSessionParamsBuilder()
                .setAccountSeed(seed)
                .setNetworkInterface(networkAddress)
                .setDeviceID(deviceID)
                .setDatabaseDir(appContext.getApplicationInfo().dataDir)
                .build();
        sessionManager.start(sessionParams);
    }

    /**
     * Session通知结束
     */
    abstract void sessionStopOver();

    /**
     * Only calls from TauService
     * Session停止结束方案：直接是stop()方法执行结束，就认为Session内存已释放结束；
     *
     * TODO:: 这种方案是否可行，有待商榷
     */
    public void doStop() {
        if (!isRunning)
            return;
        isRunning = false;
        disposables.clear();
        sessionManager.stop();

        sessionStopOver();
    }

    /**
     * 强制停止
     */
    public void forceStop() {
        Intent i = new Intent(appContext, TauService.class);
        i.setAction(TauService.ACTION_SHUTDOWN);
        Utils.startServiceBackground(appContext, i);
    }

    /**
     * 注册Alert监听事件
     * @param listener TauDaemonAlertListener
     */
    void registerAlertListener(TauDaemonAlertListener listener) {
        sessionManager.addListener(listener);
    }

    /**
     * 反注册Alert监听事件
     * @param listener TauDaemonAlertListener
     */
    void unregisterAlertListener(TauDaemonAlertListener listener) {
        sessionManager.removeListener(listener);
    }

    /**
     * 电源充电状态切换广播接受器
     */
    private void switchPowerReceiver() {
        settingsRepo.chargingState(systemServiceManager.isPlugged());
        try {
            appContext.unregisterReceiver(powerReceiver);
        } catch (IllegalArgumentException ignore) {
            /* Ignore non-registered receiver */
        }
        appContext.registerReceiver(powerReceiver, PowerReceiver.getCustomFilter());
    }

    /**
     * 网络连接切换广播接受器
     */
    private void switchConnectionReceiver() {
        settingsRepo.internetState(systemServiceManager.isHaveNetwork());
        settingsRepo.setInternetType(systemServiceManager.getInternetType());
        NetworkSetting.setMeteredNetwork(systemServiceManager.isNetworkMetered());
        try {
            appContext.unregisterReceiver(connectionReceiver);
        } catch (IllegalArgumentException ignore) {
            /* Ignore non-registered receiver */
        }
        appContext.registerReceiver(connectionReceiver, ConnectionReceiver.getFilter());
    }

    /**
     * 处理设置的改变
     * @param key 存储key
     */
    private void handleSettingsChanged(String key) {
        if (key.equals(appContext.getString(R.string.pref_key_internet_state))) {
            logger.info("SettingsChanged, internet state::{}", settingsRepo.internetState());
        } else if (key.equals(appContext.getString(R.string.pref_key_internet_type))) {
            logger.info("SettingsChanged, internet type::{}", settingsRepo.getInternetType());
            updateListenInterfaces();
        } else if (key.equals(appContext.getString(R.string.pref_key_charging_state))) {
            logger.info("SettingsChanged, charging state::{}", settingsRepo.chargingState());
        } else if (key.equals(appContext.getString(R.string.pref_key_is_metered_network))) {
            logger.info("isMeteredNetwork::{}", NetworkSetting.isMeteredNetwork());
        } else if (key.equals(appContext.getString(R.string.pref_key_current_speed))) {
            if (updateInterfacesTimer != null && !updateInterfacesTimer.isDisposed()
                    && NetworkSetting.getCurrentSpeed() > 0) {
                updateInterfacesTimer.dispose();
                disposables.remove(updateInterfacesTimer);
                logger.info("updateInterfacesTimer dispose");
            }
        } else if (key.equals(appContext.getString(R.string.pref_key_foreground_running))) {
            boolean isForeground = settingsRepo.getBooleanValue(key);
            logger.info("foreground running::{}", isForeground);
            updateInterfacesTimer();
        } else if (key.equals(appContext.getString(R.string.pref_key_nat_pmp_mapped))) {
            logger.info("SettingsChanged, Nat-PMP mapped::{}", settingsRepo.isNATPMPMapped());
        } else if (key.equals(appContext.getString(R.string.pref_key_upnp_mapped))) {
            logger.info("SettingsChanged, UPnP mapped::{}", settingsRepo.isUPnpMapped());
        } else if (key.equals(appContext.getString(R.string.pref_key_main_loop_interval_list))) {
            setMainLoopInterval(FrequencyUtil.getMainLoopInterval());
        }
    }

    /**
     * 定时任务：APP从后台到前台触发, 网速采样时间后，网速依然为0，更新网络监听接口
     */
    private void updateInterfacesTimer() {
        boolean isForeground = settingsRepo.getBooleanValue(appContext.getString(R.string.pref_key_foreground_running));
        if (!isForeground) {
            return;
        }
        if (updateInterfacesTimer != null && !updateInterfacesTimer.isDisposed()) {
            return;
        }
        logger.info("updateInterfacesTimer start");
        updateInterfacesTimer = Observable.timer(10, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(time -> {
                    // 当前有网络连接, 并且还有剩余可用流量，网速为0，重新启动Session
                    boolean isHaveAvailableData = NetworkSetting.isHaveAvailableData();
                    boolean isRestart = settingsRepo.internetState() && isHaveAvailableData
                            && NetworkSetting.getCurrentSpeed() == 0;
                    logger.info("updateInterfacesTimer isRestart::{}", isRestart);
                    if (isRestart) {
                        updateListenInterfaces();
                    }

                });
        disposables.add(updateInterfacesTimer);
    }

    /**
     * 根据当前的流量包的使用，判断是否给用户更换流量包的提示
     */
    void handleNoRemainingDataTips() {
        if (!isRunning) {
            return;
        }
        // 判断有无网络连接
        if (settingsRepo.internetState()) {
            if (NetworkSetting.isHaveAvailableData()) {
                // 重置无可用流量提示对话框的参数
                trafficTips = true;
                noRemainingDataTimes = 0;
            } else {
                showNoRemainingDataTipsDialog();
            }
        }
    }

    /**
     * 更新libTAU监听接口
     * 必须有网络才会更新
     */
    private void updateListenInterfaces() {
        if (!settingsRepo.internetState()) {
            return;
        }
        if (!isRunning) {
            // 必须有用户seed才能启动
            if (StringUtil.isNotEmpty(seed)) {
                doStart(seed);
            }
        } else {
            String networkInterface = getNetworkInterface();
            if (StringUtil.isNotEmpty(networkInterface) && sessionManager.swig() != null) {
                new SessionHandle(sessionManager.swig()).updateListenInterfaces(networkInterface);
            }
            logger.debug("updateListenInterfaces::{}", networkInterface);
        }
    }

    /**
     * 获取网络接口
     */
    private static String getNetworkInterface() {
        return SystemServiceManager.getInstance().getIpAddress();
    }


    /**
     * 重新启动Sessions
     */
    private void restartSessions() {
        if (!isRunning) {
            return;
        }
        sessionManager.restart();
        TrafficUtil.resetTrafficTotalOld();
        logger.debug("restartSessions...");
    }

    /**
     * 显示没有剩余流量提示对话框
     * 必须同时满足需要提示、触发次数大于等于网速采样数、APP在前台、目前没有打开的流量提示Activity
     */
    private void showNoRemainingDataTipsDialog() {
        if (trafficTips) {
            if (noRemainingDataTimes < 10) {
                noRemainingDataTimes += 1;
                return;
            }
        }
        if (trafficTips && AppUtil.isOnForeground(appContext) &&
                !AppUtil.isForeground(appContext, TrafficTipsActivity.class)) {
            Intent intent = new Intent(appContext, TrafficTipsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(intent);
        }
    }

    /**
     * 处理用户流量提示选择
     * @param updateDailyDataLimit 是否更新每日流量限制
     */
    public void handleUserSelected(boolean updateDailyDataLimit) {
        trafficTips = updateDailyDataLimit;
    }

    /**
     * 统计Sessions的nodes数
     */
    long getSessionNodes() {
        if (!isRunning) {
            return 0;
        }
        return sessionManager.dhtNodes();
    }

    /**
     * 获取Sessions的流量统计
     */
    void getSessionStatistics(@NonNull SessionStatistics statistics) {
        if (isRunning) {
            statistics.setTotalUpload(sessionManager.totalUpload());
            statistics.setTotalDownload(sessionManager.totalDownload());
            statistics.setUploadRate(sessionManager.downloadRate());
            statistics.setDownloadRate(sessionManager.uploadRate());
        }
    }

    /**
     * 更新朋友信息
     */
    private void addYourselfAsFriend() {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            User user = RepositoryHelper.getUserRepository(appContext).getCurrentUser();
            updateFriendInfo(user);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 获取Alert消费者的发射器
     */
    public abstract ObservableEmitter getAlertConsumerEmitter();

    /**
     * 更新用户Seed
     * @param seed Seed
     */
    public abstract void updateSeed(String seed);

    /**
     * 观察TauDaemonAlert变化
     * 直接进队列，然后单独线程处理
     */
    abstract void observeTauDaemonAlertListener();

    /**
     *  更新libTAU朋友信息
     *  包含加朋友和朋友信息
     * @param friend 朋友对象
     */
    public abstract boolean updateFriendInfo(User friend);

    /**
     * 设置正在聊天的朋友
     * @param friendPk 朋友公钥
     */
    public abstract void setChattingFriend(String friendPk);

    /**
     * 取消正在聊天的朋友
     */
    public abstract void unsetChattingFriend();

    /**
     * 设置活跃的朋友
     */
    public abstract void setActiveFriends(List<String> friends);

    /**
     * 设置libTAU主循环时间间隔
     */
    public abstract void setMainLoopInterval(int interval);

    /**
     * 添加新消息
     */
    public abstract boolean addNewMessage(Message msg);
}