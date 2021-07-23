package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.libTAU4j.alerts.Alert;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepository;
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
    private FriendRepository friendRepo;

    TauDaemonImpl(@NonNull Context appContext) {
        super(appContext);
        tauDaemonAlertHandler = new TauDaemonAlertHandler(appContext, this);
        friendRepo = RepositoryHelper.getFriendsRepository(appContext);
        observeActiveFriends();
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
                            case COMM_NEW_MSG:
                            case COMM_CONFIRM_ROOT:
                            case COMM_SYNC_MSG:
                            case COMM_LAST_SEEN:
                                // 防止OOM，此处超过队列容量，直接丢弃
                                if (alertQueue.size() <= ALERT_QUEUE_CAPACITY) {
                                    alertQueue.offer(new AlertAndUser(alert, seed));
                                } else {
                                    logger.warn("Queue full, Alert data is discarded::{}", alert.message());
                                }
                                break;
                            case SES_STOP_OVER:
                                tauDaemonAlertHandler.handleLogAlert(alert);
                                sessionStopOver();
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
     * Session通知结束
     */
    @Override
    public void sessionStopOver() {
        isSessionStopped.set(true);
        logger.info("Session stopped");
        // 如果Session已停止结束，队列为空，消费者线程直接结束
        if (alertQueue.isEmpty() && alertConsumerEmitter != null) {
            alertConsumerEmitter.onComplete();
        }
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

    /**
     * 设置正在聊天的朋友
     * @param friendPk 朋友公钥
     */
    @Override
    public void setChattingFriend(String friendPk) {
        if (isRunning) {
            sessionManager.setChattingFriend(friendPk);
            logger.debug("setChattingFriend friendPk::{}", friendPk);
        }
    }

    /**
     * 取消正在聊天的朋友
     */
    @Override
    public void unsetChattingFriend() {
        if (isRunning) {
            sessionManager.unsetChattingFriend();
            logger.debug("unsetChattingFriend success");
        }
    }

    /**
     * 观察活跃朋友的变化
     */
    private void observeActiveFriends() {
        Disposable disposable = friendRepo.getActiveFriends()
                .subscribeOn(Schedulers.io())
                .sample(1, TimeUnit.MINUTES, true)
                .subscribe(this::setActiveFriends);
        disposables.add(disposable);
    }

    /**
     * 设置活跃的朋友
     */
    @Override
    public void setActiveFriends(List<String> friends) {
        if (isRunning) {
            ArrayList<String> activeFriends = new ArrayList<>(friends);
            sessionManager.setActiveFriends(activeFriends);
            logger.debug("setActiveFriends size::{}", activeFriends.size());
        }
    }

    /**
     * 设置libTAU主循环时间间隔
     */
    @Override
    public void setMainLoopInterval(int interval) {
        if (isRunning) {
            sessionManager.setLoopTimeInterval(interval);
            logger.debug("setMainLoopInterval interval::{}ms", interval);
        }
    }

    /**
     * 添加新消息
     */
    @Override
    public boolean addNewMessage(byte[] msg) {
        if (isRunning) {
            boolean isAddSuccess = sessionManager.addNewMsg(msg);
            logger.debug("addNewMessage success::{}", isAddSuccess);
            return isAddSuccess;
        }
        return false;
    }
}