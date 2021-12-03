package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.libTAU4j.Block;
import org.libTAU4j.Ed25519;
import org.libTAU4j.Message;
import org.libTAU4j.Pair;
import org.libTAU4j.PortmapTransport;
import org.libTAU4j.Transaction;
import org.libTAU4j.Vote;
import org.libTAU4j.alerts.Alert;
import org.libTAU4j.alerts.BlockChainForkPointBlockAlert;
import org.libTAU4j.alerts.BlockChainNewConsensusPointBlockAlert;
import org.libTAU4j.alerts.BlockChainNewHeadBlockAlert;
import org.libTAU4j.alerts.BlockChainNewTailBlockAlert;
import org.libTAU4j.alerts.BlockChainNewTransactionAlert;
import org.libTAU4j.alerts.BlockChainRollbackBlockAlert;
import org.libTAU4j.alerts.BlockChainTopThreeVotesAlert;
import org.libTAU4j.alerts.CommConfirmRootAlert;
import org.libTAU4j.alerts.CommFriendInfoAlert;
import org.libTAU4j.alerts.CommLastSeenAlert;
import org.libTAU4j.alerts.CommNewDeviceIdAlert;
import org.libTAU4j.alerts.CommNewMsgAlert;
import org.libTAU4j.alerts.CommSyncMsgAlert;
import org.libTAU4j.alerts.ListenSucceededAlert;
import org.libTAU4j.alerts.PortmapAlert;
import org.libTAU4j.alerts.PortmapErrorAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.FriendInfo;
import io.taucoin.torrent.publishing.core.model.data.AlertAndUser;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;

/**
 * TauDaemonListener处理程序
 */
class TauDaemonAlertHandler {

    private static final Logger logger = LoggerFactory.getLogger("libTAU");
    private MsgAlertHandler msgListenHandler;
    private TauListenHandler tauListenHandler;
    private SettingsRepository settingsRepo;
    private Context appContext;

    TauDaemonAlertHandler(Context appContext, TauDaemon daemon){
        this.appContext = appContext;
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
            case COMM_LAST_SEEN:
                // 多设备新的DeviceID
                onDiscoverFriend(alert, alertAndUser.getUserPk());
                break;
            case COMM_NEW_DEVICE_ID:
                // 多设备新的DeviceID
                onNewDeviceID(alert, alertAndUser.getUserPk());
                break;
            case COMM_FRIEND_INFO:
                // 朋友信息
                onFriendInfo(alert, alertAndUser.getUserPk());
                break;
            case COMM_NEW_MSG:
                // 新消息
                onNewMessage(alert, alertAndUser.getUserPk());
                break;
            case COMM_CONFIRM_ROOT:
                // 消息确认
                onConfirmRoot(alert, alertAndUser.getUserPk());
                break;
            case COMM_SYNC_MSG:
                // 消息同步
                onSyncMessage(alert, alertAndUser.getUserPk());
                break;
            case BLOCK_CHAIN_HEAD_BLOCK:
                onNewBlock(alert);
                break;
            case BLOCK_CHAIN_TAIL_BLOCK:
                onSyncBlock(alert);
                break;
            case BLOCK_CHAIN_CONSENSUS_POINT_BLOCK:
                onNewConsensusBlock(alert);
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
            case BLOCK_CHAIN_TOP_THREE_VOTES:
                onNewTopVotes(alert);
                break;
            default:
                logger.info("Unknown alert");
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
     * 消息正在同步
     * @param alert libTAU上报
     * @param userPk 当前用户公钥
     */
    private void onSyncMessage(Alert alert, String userPk) {
        CommSyncMsgAlert syncMsgAlert = (CommSyncMsgAlert) alert;
        logger.info(syncMsgAlert.get_message());
        byte[] hash = syncMsgAlert.getSyncing_msg_hash();
        long timestamp = syncMsgAlert.get_timestamp();
        msgListenHandler.onSyncMessage(hash, timestamp, userPk);
    }

    /**
     * 消息确认（已接收）
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
     * 多设备新的DeviceID
     * @param alert libTAU上报
     * @param userPk 当前用户公钥
     */
    private void onNewDeviceID(Alert alert, String userPk) {
        CommNewDeviceIdAlert deviceIdAlert = (CommNewDeviceIdAlert) alert;
        logger.info(deviceIdAlert.get_message());
        String deviceID = deviceIdAlert.get_device_id();
        msgListenHandler.onNewDeviceID(deviceID, userPk);
    }

    /**
     * 更新本地朋友信息
     * @param alert libTAU上报
     * @param userPk 当前用户公钥
     */
    private void onFriendInfo(Alert alert, String userPk) {
        CommFriendInfoAlert friendInfoAlert = (CommFriendInfoAlert) alert;
        logger.info(friendInfoAlert.get_message());
        byte[] friendInfo = friendInfoAlert.get_friend_info();
        if (friendInfo.length > 0) {
            FriendInfo bean = new FriendInfo(friendInfo);
            // 更新朋友信息：昵称
            msgListenHandler.onNewFriendFromMultiDevice(userPk, bean.getPubKey(), bean.getNickname(),
                    bean.getTimestamp());
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
            msgListenHandler.onNewDeviceID(deviceID, userPk);
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
        ListenSucceededAlert a = (ListenSucceededAlert) alert;
        String interfaces = a.address().toString() + ":" + a.port();
        logger.info("onListenSucceeded IP::{}", interfaces);
        settingsRepo.setStringValue(appContext.getString(R.string.pref_key_network_interfaces),
                interfaces);
    }

    /**
     * 处理日志Alert
     * @param alert libTAU上报
     */
    void handleLogAlert(Alert<?> alert) {
        logger.info(alert.message());
    }

    /**
     * libTAU上报新的区块
     * @param alert libTAU上报
     */
    private void onNewBlock(Alert alert) {
        BlockChainNewHeadBlockAlert a = (BlockChainNewHeadBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.handleNewBlock(block);
    }

    /**
     * libTAU上报向前同步的区块
     * @param alert libTAU上报
     */
    private void onSyncBlock(Alert alert) {
        BlockChainNewTailBlockAlert a = (BlockChainNewTailBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.handleSyncBlock(block);
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
     * libTAU上报节点投票
     * @param alert libTAU上报
     */
    private void onNewTopVotes(Alert alert) {
        BlockChainTopThreeVotesAlert a = (BlockChainTopThreeVotesAlert) alert;
        logger.info(a.get_message());
        List<Vote> votes = a.get_votes();
        byte[] chainID = a.get_chain_id();
        tauListenHandler.handleNewTopVotes(chainID, votes);
    }

    /**
     * 账户自动更新
     */
    void accountAutoRenewal() {
        tauListenHandler.accountAutoRenewal();
    }

    public void onCleared() {
        tauListenHandler.onCleared();
    }
}
