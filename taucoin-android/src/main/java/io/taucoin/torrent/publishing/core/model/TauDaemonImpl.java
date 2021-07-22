package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.libTAU4j.alerts.Alert;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.core.FriendInfo;
import io.taucoin.torrent.publishing.core.model.data.AlertAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.util.ByteUtil;

/**
 * 对TauDaemon的详细实现
 * 主要是APP UI与libTAU通信的交互实现
 */
public class TauDaemonImpl extends TauDaemon {
    // libTAU上报的Alert消费者发射器
    private ObservableEmitter alertConsumerEmitter;
    // libTAU上报的Alert处理程序
    private TauDaemonAlertHandler tauDaemonAlertHandler;

    TauDaemonImpl(@NonNull Context appContext) {
        super(appContext);
        tauDaemonAlertHandler = new TauDaemonAlertHandler(appContext, this);
    }

    /**
     * 更新用户Seed
     * @param seed Seed
     */
    @Override
    public void updateSeed(String seed) {
        if (StringUtil.isEmpty(seed) || StringUtil.isEquals(seed, this.seed)) {
            return;
        }
        this.seed = seed;
        // 更新用户登录的设备信息
        tauDaemonAlertHandler.addNewDeviceID(deviceID, seed);
        logger.debug("updateUserDeviceInfo deviceID::{}", deviceID);

        logger.debug("updateSeed ::{}", seed);
        byte[] bytesSeed = ByteUtil.toByte(seed);
        if (isRunning) {
            // SessionManager Start()之后再更新，
            sessionManager.updateAccountSeed(bytesSeed);
        }
    }

    /**
     * 观察TauDaemonAlert变化
     * 直接进队列，然后单独线程处理
     *
     * TODO: APP正常或异常退出数据管理
     */
    @Override
    void observeTauDaemonAlertListener() {
        // alertQueue 生产线程
        AtomicBoolean isSessionStopped = new AtomicBoolean(false);
        Disposable disposable = Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            if (emitter.isDisposed()) {
                return;
            }
            TauDaemonAlertListener listener = new TauDaemonAlertListener() {
                @Override
                public void alert(Alert<?> alert) {
                    if (!emitter.isDisposed() && alert != null) {
                        switch (alert.type()) {
                            case PORTMAP:
                            case PORTMAP_ERROR:
                            case COMM_NEW_DEVICE_ID:
                            case COMM_FRIEND_INFO:
                                // 防止OOM，此处超过队列容量，直接丢弃
                                if (alertQueue.size() > ALERT_QUEUE_CAPACITY) {
                                    alertQueue.offer(new AlertAndUser(alert, seed));
                                }
                                break;
                            case SES_STOP_OVER:
                                tauDaemonAlertHandler.handleLogAlert(alert);
                                isSessionStopped.set(true);
                                // 如果Session已停止结束，队列为空，消费者线程直接结束
                                if (alertQueue.isEmpty() && alertConsumerEmitter != null) {
                                    alertConsumerEmitter.onComplete();
                                }
                                break;
                            default:
                                tauDaemonAlertHandler.handleLogAlert(alert);
                                break;
                        }
                    }
                }
            };
            if (!emitter.isDisposed()) {
                registerAlertListener(listener);
                emitter.setDisposable(Disposables.fromAction(() -> unregisterAlertListener(listener)));
            }
        }).subscribeOn(Schedulers.io())
                .subscribe();

        // alertQueue 消费线程
        Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            this.alertConsumerEmitter = emitter;
            while (!emitter.isDisposed()) {
                // 如果Session已停止结束，队列为空，生产者和消费者线程都结束
                if (isSessionStopped.get() && alertQueue.isEmpty()) {
                    disposable.dispose();
                    emitter.onComplete();
                    break;
                }
                logger.trace("alertQueue size::{}", alertQueue.size());
                try {
                    AlertAndUser alertAndUser = alertQueue.take();
                    tauDaemonAlertHandler.handleAlertAndUser(alertAndUser);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).subscribeOn(Schedulers.io())
                .subscribe();
    }

    /**
     * 获取Alert消费者的发射器
     */
    @Override
    public ObservableEmitter getAlertConsumerEmitter() {
        return alertConsumerEmitter;
    }

    /**
     *  更新libTAU朋友信息
     *  包含加朋友和朋友信息
     * @param friend 朋友对象
     */
    @Override
    public void updateFriendInfo(User friend) {
        if (friend != null) {
            String friendPk = friend.publicKey;
            // 添加新朋友
            addNewFriend(friend.publicKey);

            byte[] nickname = null;
            BigInteger timestamp = BigInteger.ZERO;
            if (StringUtil.isNotEmpty(friend.nickname)) {
                nickname = Utils.textStringToBytes(friend.nickname);
                timestamp = BigInteger.valueOf(friend.updateTime);
            }
            FriendInfo friendInfo = new FriendInfo(ByteUtil.toByte(friendPk), nickname, timestamp);
            // 更新朋友信息
            updateFriendInfo(friendPk, friendInfo.getEncoded());
        }
    }

    /**
     * 添加新的朋友
     * @param friendPk 朋友公钥
     */
    private void addNewFriend(String friendPk) {
        if (isRunning) {
            sessionManager.addNewFriend(friendPk);
            logger.debug("addNewFriend friendPk::{}", friendPk);
        }
    }

    /**
     * 更新朋友信息
     * @param friendPk 朋友公钥
     * @param friendInfo 朋友信息
     */
    private void updateFriendInfo(String friendPk, byte[] friendInfo) {
        if (isRunning) {
            sessionManager.updateFriendInfo(friendPk, friendInfo);
            logger.debug("updateFriendInfo friendPk::{}", friendPk);
        }
    }
}