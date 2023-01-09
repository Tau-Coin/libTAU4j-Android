package io.taucoin.news.publishing.core.model;

import android.content.Context;

import org.libTAU4j.Account;
import org.libTAU4j.Block;
import org.libTAU4j.Ed25519;
import org.libTAU4j.Message;
import org.libTAU4j.Pair;
import org.libTAU4j.PortmapTransport;
import org.libTAU4j.Transaction;
import org.libTAU4j.alerts.Alert;
import org.libTAU4j.alerts.BlockChainFailToGetChainDataAlert;
import org.libTAU4j.alerts.BlockChainForkPointBlockAlert;
import org.libTAU4j.alerts.BlockChainNewHeadBlockAlert;
import org.libTAU4j.alerts.BlockChainNewTransactionAlert;
import org.libTAU4j.alerts.BlockChainOnlinePeerAlert;
import org.libTAU4j.alerts.BlockChainPicSliceAlert;
import org.libTAU4j.alerts.BlockChainRollbackBlockAlert;
import org.libTAU4j.alerts.BlockChainStateArrayAlert;
import org.libTAU4j.alerts.BlockChainSyncingBlockAlert;
import org.libTAU4j.alerts.BlockChainSyncingHeadBlockAlert;
import org.libTAU4j.alerts.BlockChainTxArrivedAlert;
import org.libTAU4j.alerts.CommConfirmRootAlert;
import org.libTAU4j.alerts.CommLastSeenAlert;
import org.libTAU4j.alerts.CommMsgArrivedAlert;
import org.libTAU4j.alerts.CommNewMsgAlert;
import org.libTAU4j.alerts.CommPeerAttentionAlert;
import org.libTAU4j.alerts.CommUserInfoAlert;
import org.libTAU4j.alerts.ListenFailedAlert;
import org.libTAU4j.alerts.ListenSucceededAlert;
import org.libTAU4j.alerts.PortmapAlert;
import org.libTAU4j.alerts.PortmapErrorAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import androidx.lifecycle.MutableLiveData;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.model.data.UserHeadPic;
import io.taucoin.news.publishing.core.model.data.UserInfo;
import io.taucoin.news.publishing.core.model.data.AlertAndUser;
import io.taucoin.news.publishing.core.model.data.message.DataKey;
import io.taucoin.news.publishing.core.storage.RepositoryHelper;
import io.taucoin.news.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.news.publishing.core.utils.ChainIDUtil;
import io.taucoin.news.publishing.core.utils.DateUtil;
import io.taucoin.news.publishing.core.utils.ObservableUtil;
import io.taucoin.news.publishing.core.utils.rlp.ByteUtil;

/**
 * TauDaemonListener处理程序
 */
public class TauDaemonAlertHandler {

    private static final Logger logger = LoggerFactory.getLogger("libTAU");
    private final MsgAlertHandler msgListenHandler;
    private final TauListenHandler tauListenHandler;
    private final SettingsRepository settingsRepo;
    private final Context appContext;
    private final TauDaemon daemon;
    private final Disposable clearExpiredPeersDis;
    private final int peersExpiredTime = 5 * 60;    // 单位：s

    private final CopyOnWriteArraySet<String> chainStoppedSet = new CopyOnWriteArraySet<>();
    private final MutableLiveData<CopyOnWriteArraySet<String>> chainStoppedData = new MutableLiveData<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> onlinePeerMap = new ConcurrentHashMap<>();
    private final MutableLiveData<ConcurrentHashMap<String, ConcurrentHashMap<String, Long>>> onlinePeerData = new MutableLiveData<>();

    TauDaemonAlertHandler(Context appContext, TauDaemon daemon) {
        this.appContext = appContext;
        this.daemon = daemon;
        this.msgListenHandler = new MsgAlertHandler(appContext, daemon);
        this.tauListenHandler = new TauListenHandler(appContext, daemon);
        settingsRepo = RepositoryHelper.getSettingsRepository(appContext);
        clearExpiredPeersDis = clearExpiredPeersTask();
    }

    public void resetAllData() {
        chainStoppedSet.clear();
        chainStoppedData.postValue(chainStoppedSet);
        onlinePeerMap.clear();
        onlinePeerData.postValue(onlinePeerMap);
    }

    /**
     * 处理AlertAndUser, 包含libTAU上报的alert， 还有当前用户信息
     * @param alertAndUser LibTAU上报的Alert事件
     */
    void handleAlertAndUser(AlertAndUser alertAndUser) {
        Alert alert = alertAndUser.getAlert();
        switch (alert.type()) {
            case PORTMAP:
                // 端口映射
                onPortMapped(alert);
                break;
            case PORTMAP_ERROR:
                // 端口映射出错
                onPortUnmapped(alert);
                break;
            case LISTEN_SUCCEEDED:
                // 端口监听成功
                onListenSucceeded(alert);
                break;
            case LISTEN_FAILED:
                // 端口监听失败
                onListenFailed(alert);
                break;
            case COMM_LAST_SEEN:
                // 多设备新的DeviceID
                onDiscoverFriend(alert, alertAndUser.getUserPk());
                break;
            case COMM_USER_INFO:
                // 用户信息
                onUserInfo(alert, alertAndUser.getUserPk());
                break;
            case COMM_PEER_ATTENTION:
                // 对方点击等事件
                onPeerAttention(alert, alertAndUser.getUserPk());
                break;
            case COMM_NEW_MSG:
                // 新消息
                onNewMessage(alert, alertAndUser.getUserPk());
                break;
            case COMM_MSG_ARRIVED:
                // Arrived Prefix Swarm 绿色(等价网络节点>1)
                onMsgArrived(alert, alertAndUser.getUserPk());
                break;
            case COMM_CONFIRM_ROOT:
                // 消息确认 Displayed on Device (来温斯坦)蓝色
                onConfirmRoot(alert, alertAndUser.getUserPk());
                break;
            case BLOCK_CHAIN_HEAD_BLOCK:
                onNewHeadBlock(alert, alertAndUser.getUserPk());
                break;
            case BLOCK_CHAIN_SYNCING_BLOCK:
                onSyncingBlock(alert);
                break;
            case BLOCK_CHAIN_SYNCING_HEAD_BLOCK:
                onSyncingHeadBlock(alert);
                break;
            case BLOCK_CHAIN_ROLLBACK_BLOCK:
				//Modified tc
                //onRollbackBlock(alert);
                break;
            case BLOCK_CHAIN_NEW_TX:
                onNewTransaction(alert);
                break;
            case BLOCK_CHAIN_FORK_POINT:
                onNewForkPoint(alert);
                break;
            case BLOCK_CHAIN_TX_ARRIVED:
                onTxArrived(alert);
                break;
            case BLOCK_CHAIN_STATE_ARRAY:
                onStateArray(alert);
                break;
            case BLOCK_CHAIN_FAIL_TO_GET_CHAIN_DATA:
                onGetChainDataFailed(alert);
                break;
            case BLOCK_CHAIN_ONLINE_PEER:
                onlinePeer(alert);
                break;
            case BLOCK_CHAIN_PIC_SLICE:
                onPicSlice(alert);
                break;
            default:
                logger.warn("Unknown alert");
                break;
        }
    }

    /**
     * 消息正在同步
     * @param alert libTAU上报
     * @param userPk 当前用户公钥
     */
    private void onDiscoverFriend(Alert alert, String userPk) {
        CommLastSeenAlert lastSeenAlert = (CommLastSeenAlert) alert;
        logger.info(lastSeenAlert.get_message());
        byte[] friendPk = lastSeenAlert.get_peer();
        long lastSeenTime = lastSeenAlert.get_last_seen();
        msgListenHandler.onDiscoveryFriend(ByteUtil.toHexString(friendPk), lastSeenTime, userPk);
    }

    /**
     * Arrived Prefix Swarm 绿色(等价网络节点>1)
     * @param alert libTAU上报
     * @param userPk 当前用户公钥
     */
    private void onMsgArrived(Alert alert, String userPk) {
        CommMsgArrivedAlert msgArrivedAlert = (CommMsgArrivedAlert) alert;
        logger.info(msgArrivedAlert.get_message());
        long timestamp = msgArrivedAlert.get_timestamp();
        byte[] msgHash = msgArrivedAlert.getMsg_arrived_hash();
        msgListenHandler.onMsgArrived(msgHash, BigInteger.valueOf(timestamp), userPk);
    }

    /**
     * 消息确认（已接收）Displayed on Device (来温斯坦)蓝色
     * @param alert libTAU上报
     * @param userPk 当前用户公钥
     */
    private void onConfirmRoot(Alert alert, String userPk) {
        CommConfirmRootAlert confirmRootAlert = (CommConfirmRootAlert) alert;
        logger.info(confirmRootAlert.get_message());
        long timestamp = confirmRootAlert.get_timestamp();
        List<byte[]> rootList = confirmRootAlert.getConfirmation_roots();
        msgListenHandler.onReadMessageRoot(rootList, BigInteger.valueOf(timestamp), userPk);
    }

    /**
     * 新消息
     * @param alert libTAU上报
     */
    private void onNewMessage(Alert alert, String userPk) {
        CommNewMsgAlert newMsgAlert = (CommNewMsgAlert) alert;
        logger.info(newMsgAlert.get_message());
        Message message = newMsgAlert.get_new_message();
        msgListenHandler.onNewMessage(message, userPk);
    }

    /**
     * 更新本地朋友信息
     * @param alert libTAU上报
     */
    private void onUserInfo(Alert alert, String userPk) {
        CommUserInfoAlert infoAlert = (CommUserInfoAlert) alert;
        logger.info(infoAlert.get_message());
        byte[] peer = infoAlert.get_peer();
        byte[] data = infoAlert.get_user_info();
        byte[] key = infoAlert.get_key();
        DataKey.Suffix suffix = DataKey.getSuffix(key);
        switch (suffix) {
            case INFO:
                UserInfo userInfo = new UserInfo(data);
                msgListenHandler.onUserInfo(userPk, peer, userInfo);
                break;
            case PIC:
                UserHeadPic headPic = new UserHeadPic(data);
                msgListenHandler.onUserHeadPic(peer, headPic);
                break;
            case UNKNOWN:
                break;
        }
    }

    /**
     * 处理朋友关注事件
     * @param alert libTAU上报
     */
    private void onPeerAttention(Alert alert, String userPk) {
        CommPeerAttentionAlert eventAlert = (CommPeerAttentionAlert) alert;
        logger.info(eventAlert.get_message());
        byte[] peer = eventAlert.get_peer();
        msgListenHandler.onPeerAttention(userPk, peer);
    }

    /**
     * 为自己添加新的DeviceID
     * @param deviceID String
     */
    void addNewDeviceID(String deviceID, String seed) {
        Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            byte[] seedBytes = ByteUtil.toByte(seed);
            Pair<byte[], byte[]> keypair = Ed25519.createKeypair(seedBytes);
            String userPk = ByteUtil.toHexString(keypair.first);
            msgListenHandler.addNewDeviceID(deviceID, userPk);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
    }

    /**
     * 端口映射
     * @param alert libTAU上报
     */
    private void onPortMapped(Alert alert) {
        PortmapAlert a = (PortmapAlert) alert;
        if (a.mapTransport() == PortmapTransport.UPNP) {
            logger.info("UPnP mapped::{}", true);
            settingsRepo.setUPnpMapped(true);
        } else if (a.mapTransport() == PortmapTransport.NAT_PMP) {
            logger.info("Nat-PMP mapped::{}", true);
            settingsRepo.setNATPMPMapped(true);
        }
    }

    /**
     * 端口未映射
     * @param alert libTAU上报
     */
    private void onPortUnmapped(Alert alert) {
        PortmapErrorAlert a = (PortmapErrorAlert) alert;
        if (a.mapTransport() == PortmapTransport.UPNP) {
            logger.info("UPnP mapped::{}", false);
            settingsRepo.setUPnpMapped(false);
        } else if (a.mapTransport() == PortmapTransport.NAT_PMP) {
            logger.info("Nat-PMP mapped::{}", false);
            settingsRepo.setNATPMPMapped(false);
        }
    }

    /**
     * 端口监听成功
     * @param alert libTAU上报
     */
    private void onListenSucceeded(Alert alert) {
        daemon.updateBootstrapInterval();
        ListenSucceededAlert a = (ListenSucceededAlert) alert;
        String interfaces = a.address().toString() + ":" + a.port();
        logger.info("onListenSucceeded IP::{}", interfaces);
        settingsRepo.setStringValue(appContext.getString(R.string.pref_key_network_interfaces),
                interfaces);
    }

    /**
     * 端口监听失败
     * @param alert libTAU上报
     */
    private void onListenFailed(Alert alert) {
        ListenFailedAlert a = (ListenFailedAlert) alert;
        String interfaces = a.address().toString() + ":" + a.port();
        logger.info("onListenFailed IP::{}", interfaces);
//        settingsRepo.setStringValue(appContext.getString(R.string.pref_key_network_interfaces),
//                interfaces);
    }

    /**
     * 处理日志Alert
     * @param alert libTAU上报
     */
    void handleLogAlert(Alert<?> alert) {
        if (logger.isInfoEnabled()) {
            logger.info("{}: {}", alert.type().name(), alert.message());
        } else if (logger.isWarnEnabled()) {
            logger.warn("{}: {}", alert.type().name(), alert.message());
        } else {
            logger.error("{}: {}", alert.type().name(), alert.message());
        }
    }

    /**
     * libTAU上报向前同步的区块
     * @param alert libTAU上报
     */
    private void onSyncingBlock(Alert alert) {
        BlockChainSyncingBlockAlert a = (BlockChainSyncingBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.handleSyncingBlock(block);
    }

    /**
     * libTAU上报同步中的head block
     * @param alert
     */
    private void onSyncingHeadBlock(Alert alert) {
        BlockChainSyncingHeadBlockAlert a = (BlockChainSyncingHeadBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.handleSyncingHeadBlock(block);
    }

    /**
     * libTAU上报链的区块回滚
     * @param alert libTAU上报
     */
    private void onRollbackBlock(Alert alert) {
        BlockChainRollbackBlockAlert a = (BlockChainRollbackBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.handleRollbackBlock(block);
    }

    /**
     * libTAU上报新head block
     * @param alert libTAU上报
     */
    private void onNewHeadBlock(Alert alert, String userPk) {
        BlockChainNewHeadBlockAlert a = (BlockChainNewHeadBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        List<Account> accounts = a.get_accounts();
        tauListenHandler.onNewHeadBlock(block, userPk, accounts);
    }

    /**
     * libTAU上报接收到新的交易（未上链）
     * @param alert libTAU上报
     */
    private void onNewTransaction(Alert alert) {
        BlockChainNewTransactionAlert a = (BlockChainNewTransactionAlert) alert;
        logger.info(a.get_message());
        Transaction tx = a.get_new_transaction();
        tauListenHandler.handleNewTransaction(tx);
    }

    /**
     * libTAU上报分叉点
     * @param alert libTAU上报
     */
    private void onNewForkPoint(Alert alert) {
        BlockChainForkPointBlockAlert a = (BlockChainForkPointBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.handleNewForkPoint(block);
    }

    /**
     * Arrived Prefix Swarm 绿色(等价网络节点>1
     * @param alert libTAU上报
     */
    private void onTxArrived(Alert alert) {
        BlockChainTxArrivedAlert a = (BlockChainTxArrivedAlert) alert;
        logger.info(a.get_message());
        byte[] txID = a.getArrived_tx_hash();
        tauListenHandler.onTxArrived(txID);
    }

    /**
     * 上报社区账户数组
     * @param alert libTAU上报
     */
    private void onStateArray(Alert alert) {
        BlockChainStateArrayAlert a = (BlockChainStateArrayAlert) alert;
        logger.info(a.get_message());
        byte[] chainId = a.get_chain_id();
        List<Account> accounts = a.get_accounts();
        tauListenHandler.onStateArray(chainId, accounts, false);
    }

    /**
     * 上报图片内容
     * @param alert libTAU上报
     */
    private void onPicSlice(Alert alert) {
        BlockChainPicSliceAlert a = (BlockChainPicSliceAlert) alert;
        logger.info(a.get_message());
        byte[] chainId = a.get_chain_id();
        byte[] newsHash = a.get_hash();
        byte[] key = a.get_key();
        byte[] slice = a.get_slice();
        tauListenHandler.onPicSlice(chainId, newsHash, key, slice);
    }

    /**
     * 底层获取区块链数据失败
     * @param alert libTAU上报
     */
    private void onGetChainDataFailed(Alert alert) {
        BlockChainFailToGetChainDataAlert a = (BlockChainFailToGetChainDataAlert) alert;
        logger.info(a.get_message());
        String chainId = ChainIDUtil.decode(a.get_chain_id());
        logger.info("onGetChainDataFailed chainID::{}", chainId);
        chainStoppedSet.add(chainId);
        chainStoppedData.postValue(chainStoppedSet);
    }

    public boolean isChainStopped(String chainID) {
        return chainStoppedSet.contains(chainID);
    }

    public MutableLiveData<CopyOnWriteArraySet<String>> getChainStoppedData() {
        return chainStoppedData;
    }

    /**
     * 在线Peer
     * @param alert libTAU上报
     */
    private void onlinePeer(Alert alert) {
        BlockChainOnlinePeerAlert a = (BlockChainOnlinePeerAlert) alert;
        logger.info(a.get_message());
        String chainId = ChainIDUtil.decode(a.get_chain_id());
        String peer = ByteUtil.toHexString(a.get_peer());
        long time = a.get_time();
        logger.info("onlinePeer chainID::{}, peer::{}, time::{}", chainId, peer, time);
        ConcurrentHashMap<String, Long> peerMap = null;
        if (onlinePeerMap.containsKey(chainId)) {
            peerMap = onlinePeerMap.get(chainId);
        }
        if (null == peerMap) {
            peerMap = new ConcurrentHashMap<>();
        }
        peerMap.put(peer, time);
        onlinePeerMap.put(chainId, peerMap);
        onlinePeerData.postValue(onlinePeerMap);
        tauListenHandler.onlinePeer(a.get_chain_id(), peer);
    }

    /**
     * 获取社区在线节点数
     * @param chainID
     * @return
     */
    public int getOnlinePeersCount(String chainID) {
        if (onlinePeerMap.containsKey(chainID)) {
            ConcurrentHashMap<String, Long> peerMap = onlinePeerMap.get(chainID);
            if (peerMap != null) {
                clearExpiredPeers(peerMap);
                return peerMap.size();
            }
        }
        return 0;
    }

    public List<String> getOnlinePeersList(String chainID) {
        if (onlinePeerMap.containsKey(chainID)) {
            ConcurrentHashMap<String, Long> peerMap = onlinePeerMap.get(chainID);
            if (peerMap != null) {
                clearExpiredPeers(peerMap);
                return new ArrayList<>(peerMap.keySet());
            }
        }
        return new ArrayList<>();
    }

    /**
     * 清除超过5分钟的数据
     * @param peerMap
     */
    private void clearExpiredPeers(ConcurrentHashMap<String, Long> peerMap) {
        if (peerMap != null) {
            Set<String> keys = peerMap.keySet();
            long currentTime = DateUtil.getTime();
            for (String peer : keys) {
                Long time = peerMap.get(peer);
                if (time != null && currentTime > time + peersExpiredTime) {
                    peerMap.remove(peer);
                }
            }
        }
    }

    /**
     * 定时5分钟清理一次过期在线peers
     */
    private Disposable clearExpiredPeersTask() {
        return ObservableUtil.intervalSeconds(peersExpiredTime)
                .subscribeOn(Schedulers.io())
                .subscribe( l -> {
                    if (onlinePeerMap.size() > 0) {
                        Set<String> keys = onlinePeerMap.keySet();
                        for (String key : keys) {
                            try {
                                clearExpiredPeers(onlinePeerMap.get(key));
                            } catch (Exception ignore) {}
                        }
                        onlinePeerData.postValue(onlinePeerMap);
                    }
                });
    }

    public MutableLiveData<ConcurrentHashMap<String, ConcurrentHashMap<String, Long>>> getOnlinePeerData() {
        return onlinePeerData;
    }

    /**
     * 重启失败停止的链
     * @param chainId 链ID
     */
    void restartFailedChain(String chainId) {
        logger.info("restartChain chainID::{}", chainId);
        chainStoppedSet.remove(chainId);
        chainStoppedData.postValue(chainStoppedSet);
    }

    void handleBlockData(Block block, TauListenHandler.BlockStatus status) {
        tauListenHandler.handleBlockData(block, status);
    }

    public void onCleared() {
        tauListenHandler.onCleared();
        if (clearExpiredPeersDis != null && !clearExpiredPeersDis.isDisposed()) {
            clearExpiredPeersDis.dispose();
        }
    }

    /**
     * 添加社区
     * @param chainURL 链URL
     */
    public void addCommunity(String chainURL) {
        tauListenHandler.addCommunity(chainURL);
    }
}
