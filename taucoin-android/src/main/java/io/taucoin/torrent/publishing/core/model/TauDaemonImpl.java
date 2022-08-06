package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.libTAU4j.Account;
import org.libTAU4j.Block;
import org.libTAU4j.Message;
import org.libTAU4j.Transaction;
import org.libTAU4j.alerts.Alert;
import org.libTAU4j.alerts.AlertType;

import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.core.model.data.UserHeadPic;
import io.taucoin.torrent.publishing.core.model.data.UserInfo;
import io.taucoin.torrent.publishing.core.model.data.AlertAndUser;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.DeviceUtils;
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
        logger.info("updateUserDeviceInfo deviceID::{}", deviceID);

        logger.info("updateSeed ::{}", seed);
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
                        AlertType.ALERTS_DROPPED.swig(),
                        AlertType.BLOCK_CHAIN_LOG.swig(),
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
                        AlertType.LISTEN_FAILED.swig(),
                        AlertType.COMM_NEW_MSG.swig(),
                        AlertType.COMM_SYNC_MSG.swig(),
                        AlertType.COMM_MSG_ARRIVED.swig(),
                        AlertType.COMM_CONFIRM_ROOT.swig(),
                        AlertType.COMM_LAST_SEEN.swig(),
                        AlertType.COMM_USER_INFO.swig(),
                        AlertType.COMM_USER_EVENT.swig(),
                        AlertType.BLOCK_CHAIN_HEAD_BLOCK.swig(),
                        AlertType.BLOCK_CHAIN_CONSENSUS_POINT_BLOCK.swig(),
                        AlertType.BLOCK_CHAIN_ROLLBACK_BLOCK.swig(),
                        AlertType.BLOCK_CHAIN_NEW_TX.swig(),
                        AlertType.BLOCK_CHAIN_FORK_POINT.swig(),
                        AlertType.BLOCK_CHAIN_STATE.swig(),
                        AlertType.BLOCK_CHAIN_SYNCING_BLOCK.swig(),
                        AlertType.BLOCK_CHAIN_SYNCING_BLOCK.swig(),
                        AlertType.BLOCK_CHAIN_SYNCING_HEAD_BLOCK.swig(),
                        AlertType.BLOCK_CHAIN_TX_SENT.swig(),
                        AlertType.BLOCK_CHAIN_TX_ARRIVED.swig(),
                        AlertType.BLOCK_CHAIN_STATE_ARRAY.swig(),
                        AlertType.BLOCK_CHAIN_FAIL_TO_GET_CHAIN_DATA.swig(),
                        AlertType.BLOCK_CHAIN_ONLINE_PEER.swig(),
                    };
                }

                @Override
                public void alert(Alert<?> alert) {
                    if (!emitter.isDisposed() && alert != null) {
                        // 防止OOM，此处超过队列容量，直接丢弃
                        if (alertQueue.size() <= ALERT_QUEUE_CAPACITY) {
                            alertQueue.offer(new AlertAndUser(alert));
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
                    logger.debug("alertQueue size::{}, alertType::{}, time cost::{}ms",
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
     * 更新libTAU自己的缓存信息
     * @param user 用户对象
     */
    @Override
    public boolean updateUserInfo(User user) {
        boolean isSuccess = false;
        if (user != null) {
            String userPk = user.publicKey;
            // 添加新朋友
            isSuccess = addNewFriend(userPk);
            String deviceID = DeviceUtils.getCustomDeviceID(appContext);
            long onlineTime = getSessionTime() / 1000;
            MemberRepository memRepo = RepositoryHelper.getMemberRepository(appContext);
            List<String> communities = memRepo.queryFollowedCommunities(user.publicKey);
            UserInfo friendInfo = new UserInfo(deviceID, user, communities, onlineTime);
            byte[] encoded = friendInfo.getEncoded();
            if (encoded.length < 1000) {
                int size = communities.size();
                for (int i = 0; i < communities.size(); i++) {
                    List<String> newCommunities = communities.subList(0, size - 1);
                    friendInfo = new UserInfo(deviceID, user, newCommunities, onlineTime);
                    encoded = friendInfo.getEncoded();
                    if (encoded.length < 1000) {
                        logger.info("updateUserInfo publicKey::{}, newCommunities::{}",
                                userPk, null == newCommunities ? 0 : newCommunities.size());
                        break;
                    }
                }
            }
            logger.info("updateUserInfo publicKey::{}, nickname::{}, longitude::{}, " +
                            "latitude::{}, isSuccess::{}, encoded.length::{}, communities::{}",
                    userPk, user.nickname, user.longitude, user.latitude, isSuccess, encoded.length,
                    null == communities ? 0 : communities.size());
            // 更新用户信息
            pubUserInfo(userPk, encoded);
            // 更新用户头像
            UserHeadPic userHeadPic = new UserHeadPic(user.headPic, user.updateHPTime);
            pubUserHeadPic(userPk, userHeadPic.getEncoded());
        }
        return isSuccess;
    }

    /**
     * 添加新的朋友
     * @param friendPk 朋友公钥
     */
    public boolean addNewFriend(String friendPk) {
        boolean isSuccess = false;
        if (isRunning) {
            isSuccess = sessionManager.addNewFriend(friendPk);
            logger.info("addNewFriend friendPk::{}, isSuccess::{}", friendPk, isSuccess);
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
        logger.info("deleteFriend friendPk::{}, isSuccess::{}", friendPk, isSuccess);
        return isSuccess;
    }

    /**
     * 添加新消息
     */
    @Override
    public boolean addNewMessage(Message msg) {
        if (isRunning) {
            boolean isAddSuccess = sessionManager.addNewMsg(msg);
            logger.info("addNewMessage success::{}", isAddSuccess);
            return isAddSuccess;
        }
        return false;
    }

    /**
     * 创建新的社区
     * @param chainID chainID
     * @param accounts 创世区块的账户信息
     * @return boolean 是否创建成功
     */
    @Override
    public boolean createNewCommunity(byte[] chainID, Set<Account> accounts) {
        if (isRunning) {
            boolean isAddSuccess = sessionManager.createNewCommunity(chainID, accounts);
            logger.info("createNewCommunity success::{}", isAddSuccess);
            return isAddSuccess;
        }
        return true;
    }

    /**
     * 创建新的社区链ID
     * @param communityName 社区名称
     * @return chainID
     */
    @Override
    public String createNewChainID(String type, String communityName) {
        String newChainID = null;
        if (isRunning) {
            byte[] typeBytes = Utils.textStringToBytes(type);
            byte[] chainID = sessionManager.createChainID(typeBytes, communityName);
            logger.debug("createNewChainID typeBytes::{} nameBytes::{}, communityName::{}",
                    null == typeBytes ? 0 : typeBytes.length, chainID.length, communityName);
            newChainID = ChainIDUtil.decode(chainID);
            logger.debug("createNewChainID String length::{}", newChainID.length());
        }
        logger.info("createNewChainID isRunning::{}, type::{}, chainID::{}", isRunning, type, newChainID);
        return newChainID;
    }

    /**
     * 获取账户信息
     * @param chainID 社区ID
     * @param publicKey 用户公钥
     * @return Account 账户信息
     */
    @Override
    public Account getAccountInfo(byte[] chainID, String publicKey) {
        logger.debug("getAccountInfo isRunning::{}", isRunning);
        Account account = null;
        if (isRunning) {
            account = sessionManager.getAccountInfo(chainID, publicKey);
            logger.info("getAccountInfo balance::{}, nonce::{}", account.getBalance(), account.getNonce());
        }
        return account;
    }

    /**
     * 提交交易到交易池
     * @param tx 交易对象
     */
    @Override
    public boolean submitTransaction(Transaction tx) {
        if (isRunning) {
            logger.info("submitTransaction txID::{}, nonce::{}", tx.getTxID().to_hex(), tx.getNonce());
            return sessionManager.submitTransaction(tx);
        }
        return false;
    }

    /**
     * 跟随链
     * @param chainID 链ID
     * @param peers 链上peers
     */
    @Override
    public boolean followChain(String chainID, Set<String> peers) {
        boolean success = false;
        if (isRunning) {
            success = sessionManager.followChain(ChainIDUtil.encode(chainID), peers);
        }
        logger.info("followChain chainID::{}, peers size::{}, success::{}, isRunning::{}", chainID,
                peers.size(), success, isRunning);
        return success;
    }

    /**
     * 取消跟随链
     * @param chainID 链ID
     */
    @Override
    public boolean unfollowChain(String chainID) {
        boolean success = false;
        if (isRunning) {
            success = sessionManager.unfollowChain(ChainIDUtil.encode(chainID));
        }
        logger.info("unfollowChain chainID::{}, success::{}, isRunning::{}", chainID, success, isRunning);
        return success;
    }

    /**
     * 获取Session Time
     */
    @Override
    public long getSessionTime() {
        long time = 0;
        if (isRunning) {
            time = sessionManager.getSessionTime();
        }
        boolean isLocalTime = time <= 0;
        if (isLocalTime) {
            time = DateUtil.getMillisTime();
        }
        logger.info("SessionTime::{}({}), isLocalTime::{}", DateUtil.format(time, DateUtil.pattern9),
                time, isLocalTime);
        return time;
    }

    /**
     * 通过区块号查询区块
     * @param chainID 链ID
     * @param blockNumber 区块号
     */
    @Override
    public Block getBlockByNumber(String chainID, long blockNumber) {
        if (isRunning) {
            return sessionManager.getBlockByNumber(ChainIDUtil.encode(chainID), blockNumber);
        }
        return null;
    }

    @Override
    public Block getBlockByHash(String chainID, String blockHash) {
        if (isRunning) {
            return sessionManager.getBlockByHash(ChainIDUtil.encode(chainID), blockHash);
        }
        return null;
    }
}
