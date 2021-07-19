package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.libTAU4j.PortmapTransport;
import org.libTAU4j.Vectors;
import org.libTAU4j.alerts.Alert;
import org.libTAU4j.alerts.CommFriendInfoAlert;
import org.libTAU4j.alerts.CommNewDeviceIdAlert;
import org.libTAU4j.alerts.PortmapAlert;
import org.libTAU4j.alerts.PortmapErrorAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.core.FriendInfo;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.util.ByteUtil;

import static org.spongycastle.crypto.tls.ContentType.alert;

/**
 * TauDaemonListener处理程序
 */
class TauDaemonAlertHandler {

    private static final Logger logger = LoggerFactory.getLogger("AlertHandler");
    private TauDaemon daemon;
    private MsgListenHandler msgListenHandler;
    private SettingsRepository settingsRepo;

    TauDaemonAlertHandler(Context appContext, TauDaemon daemon){
        this.daemon = daemon;
        this.msgListenHandler = new MsgListenHandler(appContext);
        settingsRepo = RepositoryHelper.getSettingsRepository(appContext);
    }

    /**
     * 处理Alert
     * @param alert LibTAU上报的Alert事件
     */
    void handleAlert(Alert alert) {
        switch (alert.type()) {
            case PORTMAP:
                // 端口映射
                logger.info(alert.message());
                onPortMapped(alert);
                break;
            case PORTMAP_ERROR:
                // 端口映射出错
                logger.info(alert.message());
                onPortUnmapped(alert);
                break;
            case COMM_NEW_DEVICE_ID:
                // 多设备新的DeviceID
                logger.info(alert.message());
                CommNewDeviceIdAlert deviceIdAlert = (CommNewDeviceIdAlert) alert;
                byte[] deviceID = Vectors.byte_vector2bytes(deviceIdAlert.swig().get_device_id());
                msgListenHandler.onNewDeviceID(deviceID);
                break;
            case COMM_FRIEND_INFO:
                // 朋友信息
                logger.info(alert.message());
                updateLocalFriendInfo(alert);
                break;
            default:
                logger.info(alert.message());
                break;
        }
    }

    /**
     * 更新本地朋友信息
     */
    private void updateLocalFriendInfo(Alert alert) {
        CommFriendInfoAlert friendInfoAlert = (CommFriendInfoAlert) alert;
        byte[] friendInfo = Vectors.byte_vector2bytes(friendInfoAlert.swig().get_friend_info());
        if (friendInfo.length > 0) {
            FriendInfo bean = new FriendInfo(friendInfo);
            String friendPk = ByteUtil.toHexString(bean.getPubKey());
            // 发现朋友，添加朋友或者更新朋友lastSeenTime
            msgListenHandler.onDiscoveryFriend(friendPk, alert.timestamp());
            daemon.addNewFriend(friendPk);
            daemon.updateFriendInfo(friendPk, friendInfo);
            // 更新朋友信息：昵称
            msgListenHandler.onNewFriendFromMultiDevice(bean.getPubKey(), bean.getNickname(),
                    bean.getTimestamp());
        }
        logger.info(alert.message());
    }

    /**
     * 为自己添加新的DeviceID
     * @param deviceID String
     */
    void addNewDeviceID(String deviceID) {
        Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            msgListenHandler.onNewDeviceID(deviceID.getBytes());
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
            settingsRepo.setNATPMPMapped(true);
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
            settingsRepo.setNATPMPMapped(false);
        } else if (a.mapTransport() == PortmapTransport.NAT_PMP) {
            logger.info("Nat-PMP mapped::{}", false);
            settingsRepo.setNATPMPMapped(false);
        }
    }
}
