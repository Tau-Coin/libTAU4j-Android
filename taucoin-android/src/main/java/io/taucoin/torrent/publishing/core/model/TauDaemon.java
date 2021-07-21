package io.taucoin.torrent.publishing.core.model;

import android.content.Context;
import android.content.Intent;

import org.libTAU4j.SessionManager;
import org.libTAU4j.SessionParams;
import org.libTAU4j.alerts.Alert;
import org.libTAU4j.alerts.AlertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.core.FriendInfo;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.AlertAndUser;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.AppUtil;
import io.taucoin.torrent.publishing.core.utils.DeviceUtils;
import io.taucoin.torrent.publishing.core.utils.FrequencyUtil;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.SessionStatistics;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.TrafficUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.receiver.ConnectionReceiver;
import io.taucoin.torrent.publishing.service.SystemServiceManager;
import io.taucoin.torrent.publishing.receiver.PowerReceiver;
import io.taucoin.torrent.publishing.service.TauService;
import io.taucoin.torrent.publishing.ui.setting.TrafficTipsActivity;
import io.taucoin.util.ByteUtil;

/**
 * 区块链业务Daemon
 */
public class TauDaemon {
    private static final String TAG = TauDaemon.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TAG);
    private static final int REOPEN_NETWORKS_THRESHOLD = 15; // 单位s

    private Context appContext;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private PowerReceiver powerReceiver = new PowerReceiver();
    private ConnectionReceiver connectionReceiver = new ConnectionReceiver();
    private SessionManager sessionManager;
    private SystemServiceManager systemServiceManager;
    private ExecutorService exec = Executors.newSingleThreadExecutor();
    private TauDaemonAlertHandler tauDaemonAlertHandler;
    private TauInfoProvider tauInfoProvider;
    private Disposable restartSessionTimer; // 重启Sessions定时任务
    private volatile boolean isRunning = false;
    private volatile boolean trafficTips = true; // 剩余流量用完提示
    private volatile String seed;
    private String deviceID;
    private long noRemainingDataTimes = 0; // 触发无剩余流量的次数
    private int noNodesCount = 0; // 无节点计数, nodes查询频率为1s

    private static volatile TauDaemon instance;

    public static TauDaemon getInstance(@NonNull Context appContext) {
        if (instance == null) {
            synchronized (TauDaemon.class) {
                if (instance == null)
                    instance = new TauDaemon(appContext);
            }
        }
        return instance;
    }

    /**
     * TauDaemon构造函数
     */
    private TauDaemon(@NonNull Context appContext) {
        this.appContext = appContext;
        settingsRepo = RepositoryHelper.getSettingsRepository(appContext);
        systemServiceManager = SystemServiceManager.getInstance();
        tauDaemonAlertHandler = new TauDaemonAlertHandler(appContext, this);
        tauInfoProvider = TauInfoProvider.getInstance(this);

        deviceID = DeviceUtils.getCustomDeviceID(appContext);
        sessionManager = new SessionManager(true);

        observeTauDaemon();
        rescheduleTAUBySettings();
        initLocalParam();
    }

    private void observeTauDaemon() {
        // 监听libTAU启动成功
        sessionManager.addListener(new TauDaemonAlertListener() {

            @Override
            public int[] types() {
                return new int[]{AlertType.SES_START_OVER.swig()};
            }

            @Override
            public void alert(Alert<?> alert) {
                if (alert != null && alert.type() == AlertType.SES_START_OVER) {
                    logger.debug("Tau start successfully");
                    isRunning = true;
                    handleSettingsChanged(appContext.getString(R.string.pref_key_foreground_running));
                }
            }
        });
        observeTauDaemonAlertListener();
        disposables.add(settingsRepo.observeSettingsChanged()
                .subscribe(this::handleSettingsChanged));
        disposables.add(tauInfoProvider.observeAppStatistics()
                .subscribeOn(Schedulers.io())
                .subscribe());
        disposables.add(tauInfoProvider.observeSessionStats()
            .subscribeOn(Schedulers.io())
                .subscribe(nodes -> {
                    if (nodes > 0) {
                        noNodesCount = 0;
                    } else {
                        noNodesCount += 1;
                        if (noNodesCount > REOPEN_NETWORKS_THRESHOLD) {
                            logger.trace("No nodes more than {}s, restartSessions...",
                                    REOPEN_NETWORKS_THRESHOLD);
                            restartSessions();
                            noNodesCount = 0;
                        }
                    }
                }));
    }

    /**
     * 观察TauDaemonAlert变化
     * 选择BUFFER背压策略
     */
    private void observeTauDaemonAlertListener() {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<AlertAndUser>) emitter -> {
            if (emitter.isCancelled()) {
                return;
            }
            TauDaemonAlertListener listener = new TauDaemonAlertListener() {
                @Override
                public void alert(Alert<?> alert) {
                    if (!emitter.isCancelled() && alert != null) {
                        emitter.onNext(new AlertAndUser(alert, seed));
                        switch (alert.type()) {
                            case PORTMAP:
                            case PORTMAP_ERROR:
                            case COMM_NEW_DEVICE_ID:
                            case COMM_FRIEND_INFO:
                                emitter.onNext(new AlertAndUser(alert, seed));
                                break;
                            default:
                                logger.info(alert.message());
                                break;
                        }
                    }
                }
            };
            if (!emitter.isCancelled()) {
                registerAlertListener(listener);
                emitter.setDisposable(Disposables.fromAction(() -> unregisterAlertListener(listener)));
            }
        }, BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(alert -> tauDaemonAlertHandler.handleAlertAndUser(alert));
        disposables.add(disposable);
    }

    /**
     * 初始化本地参数
     */
    private void initLocalParam() {
        switchPowerReceiver();
        switchConnectionReceiver();
        settingsRepo.setUPnpMapped(false);
        settingsRepo.setNATPMPMapped(false);
        // 初始化主循环频率
        FrequencyUtil.clearMainLoopIntervalList();
    }

    /**
     * 更新用户Seed
     * @param seed Seed
     */
    public void updateSeed(String seed) {
        if (StringUtil.isEmpty(seed) || StringUtil.isEquals(seed, this.seed)) {
            return;
        }
        // 更新用户登录的设备信息
        tauDaemonAlertHandler.addNewDeviceID(deviceID, seed);
        logger.debug("updateUserDeviceInfo deviceID::{}", deviceID);

        this.seed = seed;
        logger.debug("updateSeed ::{}", seed);
        byte[] bytesSeed = ByteUtil.toByte(seed);
        sessionManager.updateAccountSeed(bytesSeed);
    }

    /**
     * Daemon启动
     */
    public void start() {
        if (isRunning){
            return;
        }
        Intent intent = new Intent(appContext, TauService.class);
        Utils.startServiceBackground(appContext, intent);
    }

    /**
     * 观察是否需要启动Daemon
     * @return Flowable
     */
    public Flowable<Boolean> observeNeedStartDaemon() {
        return Flowable.create((emitter) -> {
            if (emitter.isCancelled()){
                return;
            }
            Runnable emitLoop = () -> {
                while (!Thread.interrupted()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (emitter.isCancelled() || isRunning){
                        return;
                    }
                    emitter.onNext(true);
                }
            };

            Disposable d = observeDaemonRunning()
                    .subscribeOn(Schedulers.io())
                    .subscribe((isRunning) -> {
                        if (emitter.isCancelled())
                            return;

                        if (!isRunning) {
                            emitter.onNext(true);
                            exec.submit(emitLoop);
                        }
                    });
            if (!emitter.isCancelled()) {
                emitter.onNext(!isRunning);
                emitter.setDisposable(d);
            }

        }, BackpressureStrategy.LATEST);
    }

    /**
     * 观察Daemon是否是在运行
     */
    public Flowable<Boolean> observeDaemonRunning() {
        return Flowable.create((emitter) -> {
            if (emitter.isCancelled())
                return;

            TauDaemonAlertListener listener = new TauDaemonAlertListener() {

                @Override
                public int[] types() {
                    return new int[]{AlertType.SES_START_OVER.swig(), AlertType.SES_STOP_OVER.swig()};
                }

                @Override
                public void alert(Alert<?> alert) {
                    if (alert != null && alert.type() == AlertType.SES_START_OVER) {
                        if (!emitter.isCancelled())
                            emitter.onNext(true);
                    } else if (alert != null && alert.type() == AlertType.SES_STOP_OVER) {
                        if (!emitter.isCancelled())
                            emitter.onNext(false);
                    }
                }
            };

            if (!emitter.isCancelled()) {
                emitter.onNext(isRunning);
                registerAlertListener(listener);
                emitter.setDisposable(Disposables.fromAction(() -> unregisterAlertListener(listener)));
            }

        }, BackpressureStrategy.LATEST);
    }

    /**
     * Only calls from TauService
     */
    public void doStart(String seed) {
        logger.info("doStart");
        if (isRunning)
            return;

        // 设置SessionManager启动参数
        SessionParams sessionParams = SessionSettings.getSessionParamsBuilder()
                .setAccountSeed(seed)
                .setDeviceID(deviceID)
                .setDatabaseDir(appContext.getApplicationInfo().dataDir)
                .build();
        sessionManager.start(sessionParams);
    }

    /**
     * Only calls from TauService
     */
    public void doStop() {
        if (!isRunning)
            return;
        isRunning = false;
        disposables.clear();
        sessionManager.stop();
    }

    /**
     * 强制停止
     */
    public void forceStop() {
        Intent i = new Intent(appContext, TauService.class);
        i.setAction(TauService.ACTION_SHUTDOWN);
        Utils.startServiceBackground(appContext, i);
    }

    /**
     * 注册Alert监听事件
     * @param listener TauDaemonAlertListener
     */
    public void registerAlertListener(TauDaemonAlertListener listener) {
        sessionManager.addListener(listener);
    }

    /**
     * 反注册Alert监听事件
     * @param listener TauDaemonAlertListener
     */
    public void unregisterAlertListener(TauDaemonAlertListener listener) {
        sessionManager.removeListener(listener);
    }

    /**
     * 电源充电状态切换广播接受器
     */
    private void switchPowerReceiver() {
        settingsRepo.chargingState(systemServiceManager.isPlugged());
        try {
            appContext.unregisterReceiver(powerReceiver);
        } catch (IllegalArgumentException ignore) {
            /* Ignore non-registered receiver */
        }
        appContext.registerReceiver(powerReceiver, PowerReceiver.getCustomFilter());
    }

    /**
     * 网络连接切换广播接受器
     */
    private void switchConnectionReceiver() {
        settingsRepo.internetState(systemServiceManager.isHaveNetwork());
        settingsRepo.setInternetType(systemServiceManager.getInternetType());
        NetworkSetting.setMeteredNetwork(systemServiceManager.isNetworkMetered());
        try {
            appContext.unregisterReceiver(connectionReceiver);
        } catch (IllegalArgumentException ignore) {
            /* Ignore non-registered receiver */
        }
        appContext.registerReceiver(connectionReceiver, ConnectionReceiver.getFilter());
    }

    /**
     * 处理设置的改变
     * @param key 存储key
     */
    private void handleSettingsChanged(String key) {
        if (key.equals(appContext.getString(R.string.pref_key_internet_state))) {
            logger.info("SettingsChanged, internet state::{}", settingsRepo.internetState());
        } else if (key.equals(appContext.getString(R.string.pref_key_internet_type))) {
            logger.info("SettingsChanged, internet type::{}", settingsRepo.getInternetType());
            rescheduleTAUBySettings(true);
        } else if (key.equals(appContext.getString(R.string.pref_key_charging_state))) {
            logger.info("SettingsChanged, charging state::{}", settingsRepo.chargingState());
        } else if (key.equals(appContext.getString(R.string.pref_key_is_metered_network))) {
            logger.info("isMeteredNetwork::{}", NetworkSetting.isMeteredNetwork());
        } else if (key.equals(appContext.getString(R.string.pref_key_current_speed))) {
            if (restartSessionTimer != null && !restartSessionTimer.isDisposed()
                    && NetworkSetting.getCurrentSpeed() > 0) {
                restartSessionTimer.dispose();
                disposables.remove(restartSessionTimer);
                logger.info("restartSessionTimer dispose");
            }
        } else if (key.equals(appContext.getString(R.string.pref_key_foreground_running))) {
            boolean isForeground = settingsRepo.getBooleanValue(key);
            logger.info("foreground running::{}", isForeground);
            restartSessionTimer();
        } else if (key.equals(appContext.getString(R.string.pref_key_nat_pmp_mapped))) {
            logger.info("SettingsChanged, Nat-PMP mapped::{}", settingsRepo.isNATPMPMapped());
        } else if (key.equals(appContext.getString(R.string.pref_key_upnp_mapped))) {
            logger.info("SettingsChanged, UPnP mapped::{}", settingsRepo.isUPnpMapped());
        }
    }

    /**
     * 定时任务：APP从后台到前台触发, 网速采样时间后，网速依然为0，重启Sessions
     */
    private void restartSessionTimer() {
        boolean isForeground = settingsRepo.getBooleanValue(appContext.getString(R.string.pref_key_foreground_running));
        if (!isForeground) {
            return;
        }
        if (restartSessionTimer != null && !restartSessionTimer.isDisposed()) {
            return;
        }
        logger.info("restartSessionTimer start");
        restartSessionTimer = Observable.timer(10, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(time -> {
                    // 当前有网络连接, 并且还有剩余可用流量，网速为0，重新启动Session
                    boolean isHaveAvailableData = NetworkSetting.isHaveAvailableData();
                    boolean isRestart = settingsRepo.internetState() && isHaveAvailableData
                            && NetworkSetting.getCurrentSpeed() == 0;
                    logger.info("restartSessionTimer isRestart::{}", isRestart);
                    if (isRestart) {
                        rescheduleTAUBySettings(true);
                    }

                });
        disposables.add(restartSessionTimer);
    }

    /**
     * 根据当前设置重新调度DHT
     */
    void rescheduleTAUBySettings() {
        rescheduleTAUBySettings(false);
    }

    /**
     * 根据当前设置重新调度DHT
     * @param isRestart 是否重启Session
     */
    private synchronized void rescheduleTAUBySettings(boolean isRestart) {
        if (!isRunning) {
            return;
        }
        try {
            // 判断有无网络连接
            if (settingsRepo.internetState()) {
                if (isRestart) {
                    reopenNetworks();
                } else {
                    if (NetworkSetting.isHaveAvailableData()) {
                        // 重置无可用流量提示对话框的参数
                        trafficTips = true;
                        noRemainingDataTimes = 0;
                    } else {
                        showNoRemainingDataTipsDialog();
                    }
                }

            }
        } catch (Exception e) {
            logger.error("rescheduleDHTBySettings errors", e);
        }
    }

    /**
     * 重新打开网络
     */
    private void reopenNetworks() {
        if (!isRunning) {
            return;
        }
        sessionManager.reopenNetworkSockets();
        logger.debug("reopenNetworks...");
    }

    /**
     * 重新启动Sessions
     */
    private void restartSessions() {
        if (!isRunning) {
            return;
        }
        sessionManager.restart();
        TrafficUtil.resetTrafficTotalOld();
        logger.debug("restartSessions...");
    }

    /**
     * 显示没有剩余流量提示对话框
     * 必须同时满足需要提示、触发次数大于等于网速采样数、APP在前台、目前没有打开的流量提示Activity
     */
    private void showNoRemainingDataTipsDialog() {
        if (trafficTips) {
            if (noRemainingDataTimes < 10) {
                noRemainingDataTimes += 1;
                return;
            }
        }
        if (trafficTips && AppUtil.isOnForeground(appContext) &&
                !AppUtil.isForeground(appContext, TrafficTipsActivity.class)) {
            Intent intent = new Intent(appContext, TrafficTipsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(intent);
        }
    }

    /**
     * 处理用户流量提示选择
     * @param updateDailyDataLimit 是否更新每日流量限制
     */
    public void handleUserSelected(boolean updateDailyDataLimit) {
        trafficTips = updateDailyDataLimit;
    }

    /**
     * 统计Sessions的nodes数
     */
    long getSessionNodes() {
        if (!isRunning) {
            return 0;
        }
        return sessionManager.dhtNodes();
    }

    /**
     * 获取Sessions的流量统计
     */
    void getSessionStatistics(@NonNull SessionStatistics statistics) {
        if (isRunning) {
            statistics.setTotalUpload(sessionManager.totalUpload());
            statistics.setTotalDownload(sessionManager.totalDownload());
            statistics.setUploadRate(sessionManager.downloadRate());
            statistics.setDownloadRate(sessionManager.uploadRate());
        }
    }

    /**
     * 添加新的朋友
     * @param friendPk 朋友公钥
     */
    private void addNewFriend(String friendPk) {
        if (isRunning) {
            sessionManager.addNewFriend(friendPk);
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
        }
    }

    /**
     *  更新libTAU朋友信息
     *  包含加朋友和朋友信息
     * @param friend 朋友对象
     */
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
}