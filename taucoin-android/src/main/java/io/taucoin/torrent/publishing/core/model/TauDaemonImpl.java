package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.libTAU4j.Message;
import org.libTAU4j.alerts.Alert;
import org.libTAU4j.alerts.AlertType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.core.model.data.FriendInfo;
import io.taucoin.torrent.publishing.core.model.data.AlertAndUser;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;

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
        // 日志线程
        Disposable logDisposable = Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            if (emitter.isDisposed()) {
                return;
            }
            TauDaemonAlertListener listener = new TauDaemonAlertListener() {
                @Override
                public int[] types() {
                    return new int[]{
                        AlertType.PORTMAP_LOG.swig(),
                        AlertType.DHT_BOOTSTRAP.swig(),
                        AlertType.SES_START_OVER.swig(),
                        AlertType.DHT_GET_PEERS.swig(),
                        AlertType.EXTERNAL_IP.swig(),
                        AlertType.LISTEN_SUCCEEDED.swig(),
                        AlertType.SES_STOP_OVER.swig(),
                        AlertType.SESSION_STATS.swig(),
                        AlertType.LISTEN_FAILED.swig(),
                        AlertType.UDP_ERROR.swig(),
                        AlertType.DHT_ERROR.swig(),
                        AlertType.LOG.swig(),
                        AlertType.DHT_STATS.swig(),
                        AlertType.DHT_LOG.swig(),
                        AlertType.COMM_LOG.swig(),
                        AlertType.DHT_PKT.swig(),
                        AlertType.SESSION_ERROR.swig(),
                        AlertType.SESSION_STATS_HEADER.swig(),
                        AlertType.ALERTS_DROPPED.swig()
                    };
                }
                @Override
                public void alert(Alert<?> alert) {
                    if (!emitter.isDisposed() && alert != null) {
                        tauDaemonAlertHandler.handleLogAlert(alert);
                    }
                }
            };
            if (!emitter.isDisposed()) {
                registerAlertListener(listener);
                emitter.setDisposable(Disposables.fromAction(() -> unregisterAlertListener(listener)));
            }
        }).subscribeOn(Schedulers.io())
                .subscribe();
        disposables.add(logDisposable);

        // alertQueue 生产线程
        Disposable disposable = Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            if (emitter.isDisposed()) {
                return;
            }
            Thread.currentThread().setName("alertQueue");
            TauDaemonAlertListener listener = new TauDaemonAlertListener() {
                @Override
                public int[] types() {
                    return new int[]{
                        AlertType.PORTMAP.swig(),
                        AlertType.PORTMAP_ERROR.swig(),
                        AlertType.LISTEN_SUCCEEDED.swig(),
                        AlertType.COMM_NEW_DEVICE_ID.swig(),
                        AlertType.COMM_FRIEND_INFO.swig(),
                        AlertType.COMM_NEW_MSG.swig(),
                        AlertType.COMM_CONFIRM_ROOT.swig(),
                        AlertType.COMM_SYNC_MSG.swig(),
                        AlertType.COMM_LAST_SEEN.swig()
                    };
                }

                @Override
                public void alert(Alert<?> alert) {
                    if (!emitter.isDisposed() && alert != null) {
                        // 防止OOM，此处超过队列容量，直接丢弃
                        if (alertQueue.size() <= ALERT_QUEUE_CAPACITY) {
                            alertQueue.offer(new AlertAndUser(alert, seed));
                        } else {
                            logger.warn("Queue full, Alert data is discarded::{}", alert.message());
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
            Thread.currentThread().setName("alertConsumer");
            this.alertConsumerEmitter = emitter;
            while (!emitter.isDisposed()) {
                // 如果Session已停止结束，队列为空，生产者和消费者线程都结束
                if (isSessionStopped.get() && alertQueue.isEmpty()) {
                    disposable.dispose();
                    emitter.onComplete();
                    break;
                }
                try {
                    AlertAndUser alertAndUser = alertQueue.take();
                    long startTime = System.currentTimeMillis();
                    tauDaemonAlertHandler.handleAlertAndUser(alertAndUser);
                    long endTime = System.currentTimeMillis();
                    String alertType = alertAndUser.getAlert().type().name();
                    logger.trace("alertQueue size::{}, alertType::{}, time cost::{}ms",
                            alertQueue.size(), alertType, endTime - startTime);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    logger.error("alertConsumer error::", e);
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
    public boolean updateFriendInfo(User friend) {
        boolean isSuccess = false;
        if (friend != null) {
            String friendPk = friend.publicKey;
            // 添加新朋友
            isSuccess = addNewFriend(friend.publicKey);

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
        return isSuccess;
    }

    /**
     * 添加新的朋友
     * @param friendPk 朋友公钥
     */
    private boolean addNewFriend(String friendPk) {
        boolean isSuccess = false;
        if (isRunning) {
            isSuccess = sessionManager.addNewFriend(friendPk);
            logger.debug("addNewFriend friendPk::{}, isSuccess::{}", friendPk, isSuccess);
        }
        return isSuccess;
    }

    /**
     * 删除朋友
     * @param friendPk 朋友公钥
     */
    @Override
    public boolean deleteFriend(String friendPk) {
        boolean isSuccess = false;
        if (isRunning) {
            isSuccess = sessionManager.deleteFriend(friendPk);
        }
        logger.debug("deleteFriend friendPk::{}, isSuccess::{}", friendPk, isSuccess);
        return isSuccess;
    }

    /**
     * 更新朋友信息
     * @param friendPk 朋友公钥
     * @param friendInfo 朋友信息
     */
    private boolean updateFriendInfo(String friendPk, byte[] friendInfo) {
        boolean isSuccess = false;
        if (isRunning) {
            isSuccess = sessionManager.updateFriendInfo(friendPk, friendInfo);
            logger.debug("updateFriendInfo friendPk::{}, isSuccess::{}", friendPk, isSuccess);
        }
        return isSuccess;
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
    public boolean addNewMessage(Message msg) {
        if (isRunning) {
            boolean isAddSuccess = sessionManager.addNewMsg(msg);
            logger.debug("addNewMessage success::{}", isAddSuccess);
            return isAddSuccess;
        }
        return false;
    }
}
