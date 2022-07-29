package io.taucoin.torrent.publishing.core.model;

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
import org.libTAU4j.alerts.BlockChainNewConsensusPointBlockAlert;
import org.libTAU4j.alerts.BlockChainNewHeadBlockAlert;
import org.libTAU4j.alerts.BlockChainNewTailBlockAlert;
import org.libTAU4j.alerts.BlockChainNewTransactionAlert;
import org.libTAU4j.alerts.BlockChainRollbackBlockAlert;
import org.libTAU4j.alerts.BlockChainStateArrayAlert;
import org.libTAU4j.alerts.BlockChainSyncingBlockAlert;
import org.libTAU4j.alerts.BlockChainSyncingHeadBlockAlert;
import org.libTAU4j.alerts.BlockChainTxArrivedAlert;
import org.libTAU4j.alerts.BlockChainTxSentAlert;
import org.libTAU4j.alerts.CommConfirmRootAlert;
import org.libTAU4j.alerts.CommLastSeenAlert;
import org.libTAU4j.alerts.CommMsgArrivedAlert;
import org.libTAU4j.alerts.CommNewMsgAlert;
import org.libTAU4j.alerts.CommSyncMsgAlert;
import org.libTAU4j.alerts.CommUserEventAlert;
import org.libTAU4j.alerts.CommUserInfoAlert;
import org.libTAU4j.alerts.ListenSucceededAlert;
import org.libTAU4j.alerts.PortmapAlert;
import org.libTAU4j.alerts.PortmapErrorAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import androidx.lifecycle.MutableLiveData;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.UserEvent;
import io.taucoin.torrent.publishing.core.model.data.UserHeadPic;
import io.taucoin.torrent.publishing.core.model.data.UserInfo;
import io.taucoin.torrent.publishing.core.model.data.AlertAndUser;
import io.taucoin.torrent.publishing.core.model.data.message.DataKey;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;

/**
 * TauDaemonListener处理程序
 */
class TauDaemonAlertHandler {

    private static final Logger logger = LoggerFactory.getLogger("libTAU");
    private final MsgAlertHandler msgListenHandler;
    private final TauListenHandler tauListenHandler;
    private final SettingsRepository settingsRepo;
    private final Context appContext;
    private final TauDaemon daemon;
    private final MutableLiveData<CopyOnWriteArraySet<String>> chainStoppedSet = new MutableLiveData<>();

    TauDaemonAlertHandler(Context appContext, TauDaemon daemon) {
        this.appContext = appContext;
        this.daemon = daemon;
        this.msgListenHandler = new MsgAlertHandler(appContext, daemon);
        this.tauListenHandler = new TauListenHandler(appContext, daemon);
        settingsRepo = RepositoryHelper.getSettingsRepository(appContext);
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
            case COMM_USER_EVENT:
                // 对方点击等事件
                onUserEvent(alert, alertAndUser.getUserPk());
                break;
            case COMM_NEW_MSG:
                // 新消息
                onNewMessage(alert, alertAndUser.getUserPk());
                break;
            case COMM_SYNC_MSG:
                // Sent to Internet 桔黄色 (traversal complete > 1)
                onMsgSync(alert, alertAndUser.getUserPk());
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
                onNewHeadBlock(alert);
                break;
            case BLOCK_CHAIN_TAIL_BLOCK:
                onNewTailBlock(alert);
                break;
            case BLOCK_CHAIN_CONSENSUS_POINT_BLOCK:
                onNewConsensusBlock(alert);
                break;
            case BLOCK_CHAIN_SYNCING_BLOCK:
                onSyncingBlock(alert);
                break;
            case BLOCK_CHAIN_SYNCING_HEAD_BLOCK:
                onSyncingHeadBlock(alert);
                break;
            case BLOCK_CHAIN_ROLLBACK_BLOCK:
                onRollbackBlock(alert);
                break;
            case BLOCK_CHAIN_NEW_TX:
                onNewTransaction(alert);
                break;
            case BLOCK_CHAIN_FORK_POINT:
                onNewForkPoint(alert);
                break;
            case BLOCK_CHAIN_TX_SENT:
                onTxSent(alert);
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
     * Sent to Internet 桔黄色 (traversal complete > 1)
     * @param alert libTAU上报
     * @param userPk 当前用户公钥
     */
    private void onMsgSync(Alert alert, String userPk) {
        CommSyncMsgAlert msgSyncAlert = (CommSyncMsgAlert) alert;
        logger.info(msgSyncAlert.get_message());
        long timestamp = msgSyncAlert.get_timestamp();
        byte[] msgHash = msgSyncAlert.getSyncing_msg_hash();
        msgListenHandler.onMsgSync(msgHash, BigInteger.valueOf(timestamp), userPk);
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
     * 处理朋友事件逻辑
     * @param alert libTAU上报
     */
    private void onUserEvent(Alert alert, String userPk) {
        CommUserEventAlert eventAlert = (CommUserEventAlert) alert;
        logger.info(eventAlert.get_message());
        byte[] peer = eventAlert.get_peer();
        UserEvent userEvent = new UserEvent(eventAlert.get_user_event());
        UserEvent.Event event = UserEvent.Event.parse(userEvent.getEvent());
        if (event == UserEvent.Event.FOCUS_FRIEND) {
            msgListenHandler.onUserEvent(userPk, peer);
        }
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
//        ListenFailedAlert a = (ListenFailedAlert) alert;
//        String interfaces = a.address().toString() + ":" + a.port();
//        logger.info("onListenFailed IP::{}", interfaces);
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
    private void onNewHeadBlock(Alert alert) {
        BlockChainNewHeadBlockAlert a = (BlockChainNewHeadBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.onNewHeadBlock(block);
    }

    /**
     * libTAU上报新的Tail区块
     * @param alert libTAU上报
     */
    private void onNewTailBlock(Alert alert) {
        BlockChainNewTailBlockAlert a = (BlockChainNewTailBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.handleNewTailBlock(block);
    }

    /**
     * libTAU上报当前共识点区块
     * @param alert libTAU上报
     */
    private void onNewConsensusBlock(Alert alert) {
        BlockChainNewConsensusPointBlockAlert a = (BlockChainNewConsensusPointBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.handleNewConsensusBlock(block);
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
     * Sent to Internet 桔黄色 (traversal complete > 1)
     * @param alert libTAU上报
     */
    private void onTxSent(Alert alert) {
        BlockChainTxSentAlert a = (BlockChainTxSentAlert) alert;
        logger.info(a.get_message());
        byte[] txID = a.getSent_tx_hash();
        tauListenHandler.onTxSent(txID);
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
        tauListenHandler.onStateArray(chainId, accounts);
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
        CopyOnWriteArraySet<String> set = chainStoppedSet.getValue();
        if (null == set) {
            set = new CopyOnWriteArraySet<>();
        }
        set.add(chainId);
        chainStoppedSet.postValue(set);
    }

    public MutableLiveData<CopyOnWriteArraySet<String>> getChainStoppedSet() {
        return chainStoppedSet;
    }

    /**
     * 重启失败停止的链
     * @param chainId 链ID
     */
    void restartFailedChain(String chainId) {
        logger.info("restartChain chainID::{}", chainId);
        CopyOnWriteArraySet<String> set = chainStoppedSet.getValue();
        if (set != null) {
            set.remove(chainId);
            chainStoppedSet.postValue(set);
        }
    }

    /**
     * 账户自动更新
     */
    void accountAutoRenewal() {
        tauListenHandler.accountAutoRenewal();
    }

    void handleBlockData(Block block, TauListenHandler.BlockStatus status) {
        tauListenHandler.handleBlockData(block, status);
    }

    public void onCleared() {
        tauListenHandler.onCleared();
    }

    /**
     * 添加社区
     * @param chainURL 链URL
     */
    public void addCommunity(String chainURL) {
        tauListenHandler.addCommunity(chainURL);
    }
}
