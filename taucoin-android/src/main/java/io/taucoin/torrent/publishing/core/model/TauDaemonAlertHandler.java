package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.libTAU4j.Ed25519;
import org.libTAU4j.Message;
import org.libTAU4j.Pair;
import org.libTAU4j.PortmapTransport;
import org.libTAU4j.alerts.Alert;
import org.libTAU4j.alerts.CommConfirmRootAlert;
import org.libTAU4j.alerts.CommFriendInfoAlert;
import org.libTAU4j.alerts.CommLastSeenAlert;
import org.libTAU4j.alerts.CommNewDeviceIdAlert;
import org.libTAU4j.alerts.CommNewMsgAlert;
import org.libTAU4j.alerts.CommSyncMsgAlert;
import org.libTAU4j.alerts.PortmapAlert;
import org.libTAU4j.alerts.PortmapErrorAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;
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
    private SettingsRepository settingsRepo;

    TauDaemonAlertHandler(Context appContext, TauDaemon daemon){
        this.msgListenHandler = new MsgAlertHandler(appContext, daemon);
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
     * 处理日志Alert
     * @param alert libTAU上报
     */
    void handleLogAlert(Alert<?> alert) {
        logger.info(alert.message());
    }
}
