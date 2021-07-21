package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.libTAU4j.Ed25519;
import org.libTAU4j.Pair;
import org.libTAU4j.PortmapTransport;
import org.libTAU4j.Vectors;
import org.libTAU4j.alerts.Alert;
import org.libTAU4j.alerts.CommFriendInfoAlert;
import org.libTAU4j.alerts.CommNewDeviceIdAlert;
import org.libTAU4j.alerts.PortmapAlert;
import org.libTAU4j.alerts.PortmapErrorAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.core.FriendInfo;
import io.taucoin.torrent.publishing.core.model.data.AlertAndUser;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.util.ByteUtil;

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
                logger.info(alertAndUser.getMessage());
                onPortMapped(alert);
                break;
            case PORTMAP_ERROR:
                // 端口映射出错
                logger.info(alertAndUser.getMessage());
                onPortUnmapped(alert);
                break;
            case COMM_NEW_DEVICE_ID:
                // 多设备新的DeviceID
                logger.info(alertAndUser.getMessage());
                addNewDeviceID(alert, alertAndUser.getUserPk());
                break;
            case COMM_FRIEND_INFO:
                // 朋友信息
                logger.info(alertAndUser.getMessage());
                updateLocalFriendInfo(alert, alertAndUser.getUserPk());
                break;
            default:
                logger.info(alertAndUser.getMessage());
                break;
        }
    }

    /**
     * 多设备新的DeviceID
     * @param alert libTAU上报
     * @param userPk 当前用户公钥
     */
    private void addNewDeviceID(Alert alert, String userPk) {
        CommNewDeviceIdAlert deviceIdAlert = (CommNewDeviceIdAlert) alert;
        byte[] deviceID = Vectors.byte_vector2bytes(deviceIdAlert.swig().get_device_id());
        msgListenHandler.onNewDeviceID(deviceID, userPk);
    }

    /**
     * 更新本地朋友信息
     * @param alert libTAU上报
     * @param userPk 当前用户公钥
     */
    private void updateLocalFriendInfo(Alert alert, String userPk) {
        CommFriendInfoAlert friendInfoAlert = (CommFriendInfoAlert) alert;
        byte[] friendInfo = Vectors.byte_vector2bytes(friendInfoAlert.swig().get_friend_info());
        if (friendInfo.length > 0) {
            FriendInfo bean = new FriendInfo(friendInfo);
            String friendPk = ByteUtil.toHexString(bean.getPubKey());
            // 发现朋友，添加朋友或者更新朋友lastSeenTime
            msgListenHandler.onDiscoveryFriend(friendPk, alert.timestamp());
            // 更新朋友信息：昵称
            msgListenHandler.onNewFriendFromMultiDevice(userPk, bean.getPubKey(), bean.getNickname(),
                    bean.getTimestamp());
        }
        logger.info(alert.message());
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
            msgListenHandler.onNewDeviceID(deviceID.getBytes(), userPk);
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
