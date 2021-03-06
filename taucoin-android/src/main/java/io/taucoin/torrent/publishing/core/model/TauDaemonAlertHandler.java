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
 * TauDaemonListener????????????
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
     * ??????AlertAndUser, ??????libTAU?????????alert??? ????????????????????????
     * @param alertAndUser LibTAU?????????Alert??????
     */
    void handleAlertAndUser(AlertAndUser alertAndUser) {
        Alert alert = alertAndUser.getAlert();
        switch (alert.type()) {
            case PORTMAP:
                // ????????????
                onPortMapped(alert);
                break;
            case PORTMAP_ERROR:
                // ??????????????????
                onPortUnmapped(alert);
                break;
            case LISTEN_SUCCEEDED:
                // ??????????????????
                onListenSucceeded(alert);
                break;
            case LISTEN_FAILED:
                // ??????????????????
                onListenFailed(alert);
                break;
            case COMM_LAST_SEEN:
                // ???????????????DeviceID
                onDiscoverFriend(alert, alertAndUser.getUserPk());
                break;
            case COMM_USER_INFO:
                // ????????????
                onUserInfo(alert, alertAndUser.getUserPk());
                break;
            case COMM_USER_EVENT:
                // ?????????????????????
                onUserEvent(alert, alertAndUser.getUserPk());
                break;
            case COMM_NEW_MSG:
                // ?????????
                onNewMessage(alert, alertAndUser.getUserPk());
                break;
            case COMM_SYNC_MSG:
                // Sent to Internet ????????? (traversal complete > 1)
                onMsgSync(alert, alertAndUser.getUserPk());
                break;
            case COMM_MSG_ARRIVED:
                // Arrived Prefix Swarm ??????(??????????????????>1)
                onMsgArrived(alert, alertAndUser.getUserPk());
                break;
            case COMM_CONFIRM_ROOT:
                // ???????????? Displayed on Device (????????????)??????
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
     * ??????????????????
     * @param alert libTAU??????
     * @param userPk ??????????????????
     */
    private void onDiscoverFriend(Alert alert, String userPk) {
        CommLastSeenAlert lastSeenAlert = (CommLastSeenAlert) alert;
        logger.info(lastSeenAlert.get_message());
        byte[] friendPk = lastSeenAlert.get_peer();
        long lastSeenTime = lastSeenAlert.get_last_seen();
        msgListenHandler.onDiscoveryFriend(ByteUtil.toHexString(friendPk), lastSeenTime, userPk);
    }

    /**
     * Sent to Internet ????????? (traversal complete > 1)
     * @param alert libTAU??????
     * @param userPk ??????????????????
     */
    private void onMsgSync(Alert alert, String userPk) {
        CommSyncMsgAlert msgSyncAlert = (CommSyncMsgAlert) alert;
        logger.info(msgSyncAlert.get_message());
        long timestamp = msgSyncAlert.get_timestamp();
        byte[] msgHash = msgSyncAlert.getSyncing_msg_hash();
        msgListenHandler.onMsgSync(msgHash, BigInteger.valueOf(timestamp), userPk);
    }


    /**
     * Arrived Prefix Swarm ??????(??????????????????>1)
     * @param alert libTAU??????
     * @param userPk ??????????????????
     */
    private void onMsgArrived(Alert alert, String userPk) {
        CommMsgArrivedAlert msgArrivedAlert = (CommMsgArrivedAlert) alert;
        logger.info(msgArrivedAlert.get_message());
        long timestamp = msgArrivedAlert.get_timestamp();
        byte[] msgHash = msgArrivedAlert.getMsg_arrived_hash();
        msgListenHandler.onMsgArrived(msgHash, BigInteger.valueOf(timestamp), userPk);
    }

    /**
     * ???????????????????????????Displayed on Device (????????????)??????
     * @param alert libTAU??????
     * @param userPk ??????????????????
     */
    private void onConfirmRoot(Alert alert, String userPk) {
        CommConfirmRootAlert confirmRootAlert = (CommConfirmRootAlert) alert;
        logger.info(confirmRootAlert.get_message());
        long timestamp = confirmRootAlert.get_timestamp();
        List<byte[]> rootList = confirmRootAlert.getConfirmation_roots();
        msgListenHandler.onReadMessageRoot(rootList, BigInteger.valueOf(timestamp), userPk);
    }

    /**
     * ?????????
     * @param alert libTAU??????
     */
    private void onNewMessage(Alert alert, String userPk) {
        CommNewMsgAlert newMsgAlert = (CommNewMsgAlert) alert;
        logger.info(newMsgAlert.get_message());
        Message message = newMsgAlert.get_new_message();
        msgListenHandler.onNewMessage(message, userPk);
    }

    /**
     * ????????????????????????
     * @param alert libTAU??????
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
     * ????????????????????????
     * @param alert libTAU??????
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
     * ?????????????????????DeviceID
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
     * ????????????
     * @param alert libTAU??????
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
     * ???????????????
     * @param alert libTAU??????
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
     * ??????????????????
     * @param alert libTAU??????
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
     * ??????????????????
     * @param alert libTAU??????
     */
    private void onListenFailed(Alert alert) {
//        ListenFailedAlert a = (ListenFailedAlert) alert;
//        String interfaces = a.address().toString() + ":" + a.port();
//        logger.info("onListenFailed IP::{}", interfaces);
//        settingsRepo.setStringValue(appContext.getString(R.string.pref_key_network_interfaces),
//                interfaces);
    }

    /**
     * ????????????Alert
     * @param alert libTAU??????
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
     * libTAU???????????????????????????
     * @param alert libTAU??????
     */
    private void onSyncingBlock(Alert alert) {
        BlockChainSyncingBlockAlert a = (BlockChainSyncingBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.handleSyncingBlock(block);
    }

    /**
     * libTAU??????????????????head block
     * @param alert
     */
    private void onSyncingHeadBlock(Alert alert) {
        BlockChainSyncingHeadBlockAlert a = (BlockChainSyncingHeadBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.handleSyncingHeadBlock(block);
    }

    /**
     * libTAU????????????????????????
     * @param alert libTAU??????
     */
    private void onRollbackBlock(Alert alert) {
        BlockChainRollbackBlockAlert a = (BlockChainRollbackBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.handleRollbackBlock(block);
    }

    /**
     * libTAU?????????head block
     * @param alert libTAU??????
     */
    private void onNewHeadBlock(Alert alert) {
        BlockChainNewHeadBlockAlert a = (BlockChainNewHeadBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.onNewHeadBlock(block);
    }

    /**
     * libTAU????????????Tail??????
     * @param alert libTAU??????
     */
    private void onNewTailBlock(Alert alert) {
        BlockChainNewTailBlockAlert a = (BlockChainNewTailBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.handleNewTailBlock(block);
    }

    /**
     * libTAU???????????????????????????
     * @param alert libTAU??????
     */
    private void onNewConsensusBlock(Alert alert) {
        BlockChainNewConsensusPointBlockAlert a = (BlockChainNewConsensusPointBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.handleNewConsensusBlock(block);
    }

    /**
     * libTAU??????????????????????????????????????????
     * @param alert libTAU??????
     */
    private void onNewTransaction(Alert alert) {
        BlockChainNewTransactionAlert a = (BlockChainNewTransactionAlert) alert;
        logger.info(a.get_message());
        Transaction tx = a.get_new_transaction();
        tauListenHandler.handleNewTransaction(tx);
    }

    /**
     * libTAU???????????????
     * @param alert libTAU??????
     */
    private void onNewForkPoint(Alert alert) {
        BlockChainForkPointBlockAlert a = (BlockChainForkPointBlockAlert) alert;
        logger.info(a.get_message());
        Block block = a.get_new_block();
        tauListenHandler.handleNewForkPoint(block);
    }

    /**
     * Sent to Internet ????????? (traversal complete > 1)
     * @param alert libTAU??????
     */
    private void onTxSent(Alert alert) {
        BlockChainTxSentAlert a = (BlockChainTxSentAlert) alert;
        logger.info(a.get_message());
        byte[] txID = a.getSent_tx_hash();
        tauListenHandler.onTxSent(txID);
    }

    /**
     * Arrived Prefix Swarm ??????(??????????????????>1
     * @param alert libTAU??????
     */
    private void onTxArrived(Alert alert) {
        BlockChainTxArrivedAlert a = (BlockChainTxArrivedAlert) alert;
        logger.info(a.get_message());
        byte[] txID = a.getArrived_tx_hash();
        tauListenHandler.onTxArrived(txID);
    }

    /**
     * ????????????????????????
     * @param alert libTAU??????
     */
    private void onStateArray(Alert alert) {
        BlockChainStateArrayAlert a = (BlockChainStateArrayAlert) alert;
        logger.info(a.get_message());
        byte[] chainId = a.get_chain_id();
        List<Account> accounts = a.get_accounts();
        tauListenHandler.onStateArray(chainId, accounts);
    }

    /**
     * ?????????????????????????????????
     * @param alert libTAU??????
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
     * ????????????????????????
     * @param chainId ???ID
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
     * ??????????????????
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
     * ????????????
     * @param chainURL ???URL
     */
    public void addCommunity(String chainURL) {
        tauListenHandler.addCommunity(chainURL);
    }
}
