package io.taucoin.torrent.publishing.core.model;

import android.content.Context;
import android.content.Intent;

import org.libTAU4j.Account;
import org.libTAU4j.Block;
import org.libTAU4j.Message;
import org.libTAU4j.SessionHandle;
import org.libTAU4j.SessionManager;
import org.libTAU4j.SessionParams;
import org.libTAU4j.Transaction;
import org.libTAU4j.alerts.Alert;
import org.libTAU4j.alerts.AlertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.log.LogUtil;
import io.taucoin.torrent.publishing.core.model.data.AlertAndUser;
import io.taucoin.torrent.publishing.core.model.data.message.DataKey;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.utils.AppUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.DeviceUtils;
import io.taucoin.torrent.publishing.core.utils.FileUtil;
import io.taucoin.torrent.publishing.core.utils.LinkUtil;
import io.taucoin.torrent.publishing.core.utils.LocationManagerUtil;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.ObservableUtil;
import io.taucoin.torrent.publishing.core.utils.SessionStatistics;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.TrafficUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.torrent.publishing.receiver.ConnectionReceiver;
import io.taucoin.torrent.publishing.receiver.PowerReceiver;
import io.taucoin.torrent.publishing.service.SystemServiceManager;
import io.taucoin.torrent.publishing.service.TauService;
import io.taucoin.torrent.publishing.ui.TauNotifier;
import io.taucoin.torrent.publishing.ui.setting.TrafficTipsActivity;

/**
 * ???????????????Daemon
 */
public abstract class TauDaemon {
    private static final String TAG = TauDaemon.class.getSimpleName();
    static final Logger logger = LoggerFactory.getLogger(TAG);
    private static final int SHOW_DIALOG_THRESHOLD = 10;        // ??????s
    static final int ALERT_QUEUE_CAPACITY = 10000;              // Alert????????????
    private static volatile TauDaemon instance;
    public static long daemonStartTime = DateUtil.getMillisTime(); // Daemon????????????

    Context appContext;
    private final SettingsRepository settingsRepo;
    CompositeDisposable disposables = new CompositeDisposable();
    private final PowerReceiver powerReceiver = new PowerReceiver();
    private final ConnectionReceiver connectionReceiver = new ConnectionReceiver();
    SessionManager sessionManager;
    private final SystemServiceManager systemServiceManager;
    private final TauInfoProvider tauInfoProvider;
    private final LocationManagerUtil locationManager;
    private Disposable updateBootstrapIntervalTimer; // ??????BootstrapInterval????????????
    private Disposable updateLocationTimer;          // ??????????????????????????????
    private Disposable noRemainingDataTimer;         // ??????????????????????????????????????????
    private Disposable libTauDozeTimer;              // ??????libTAU??????????????????
    private Disposable onlineTimer;                  // ??????????????????????????????
    TauDaemonAlertHandler tauDaemonAlertHandler;     // libTAU?????????Alert????????????
    private final TxQueueManager txQueueManager;     // ??????????????????
    private final TauDozeManager tauDozeManager;     // tau??????????????????
    volatile boolean isRunning = false;
    private volatile boolean trafficTips = true;     // ????????????????????????
    volatile String seed;
    String deviceID;

    // libTAU?????????Alert????????????
    LinkedBlockingQueue<AlertAndUser> alertQueue = new LinkedBlockingQueue<>();
    AtomicBoolean isSessionStopped = new AtomicBoolean(false);

    public static TauDaemon getInstance(@NonNull Context appContext) {
        if (instance == null) {
            synchronized (TauDaemon.class) {
                if (instance == null)
                    instance = new TauDaemonImpl(appContext);
            }
        }
        return instance;
    }

    /**
     * TauDaemon????????????
     */
    TauDaemon(@NonNull Context appContext) {
        this.appContext = appContext;
        settingsRepo = RepositoryHelper.getSettingsRepository(appContext);
        systemServiceManager = SystemServiceManager.getInstance();
        tauInfoProvider = TauInfoProvider.getInstance(this);
        locationManager = new LocationManagerUtil(appContext);
        deviceID = DeviceUtils.getCustomDeviceID(appContext);
        sessionManager = new SessionManager(true);
        txQueueManager = new TxQueueManager(this);
        tauDozeManager = new TauDozeManager(this);

        observeTauDaemon();
        initLocalParam();
        handleNoRemainingDataTips();
    }

    private void observeTauDaemon() {
        // ??????libTAU????????????
        sessionManager.addListener(new TauDaemonAlertListener() {

            @Override
            public int[] types() {
                return new int[]{AlertType.SES_START_OVER.swig()};
            }

            @Override
            public void alert(Alert<?> alert) {
                if (alert != null && alert.type() == AlertType.SES_START_OVER) {
                    logger.info("Tau start successfully");
                    isRunning = true;
                    handleSettingsChanged(appContext.getString(R.string.pref_key_foreground_running));
                    // ?????????????????????????????????
                    updateCurrentUserInfo(true);
                    // ??????????????????????????????
                    startOnlineTimer();
                    // ?????????????????????????????????????????????
                    updateChainsAndAccountInfo();
                    // ??????????????????
                    accountAutoRenewal();
                    // ??????Crash????????? ??????????????????????????????????????????
                    setLogLevel(LogUtil.getTauLogLevel());
                }
            }
        });
        observeTauDaemonAlertListener();
        disposables.add(settingsRepo.observeSettingsChanged()
                .subscribe(this::handleSettingsChanged));
        disposables.add(tauInfoProvider.observeAppStatistics()
                .subscribeOn(Schedulers.io())
                .subscribe());
        disposables.add(tauInfoProvider.observeCPUStatistics()
                .subscribeOn(Schedulers.io())
                .subscribe());
    }

    /**
     * ?????????????????????
     */
    private void initLocalParam() {
        switchPowerReceiver();
        switchConnectionReceiver();
        TrafficUtil.resetTrafficTotalOld();
        settingsRepo.initData();
    }

    /**
     * Daemon??????
     */
    public void start() {
        if (isRunning){
            return;
        }
        Intent intent = new Intent(appContext, TauService.class);
        Utils.startServiceBackground(appContext, intent);
    }

    /**
     * ????????????????????????Daemon
     * @return Flowable
     */
    public Flowable<Boolean> observeNeedStartDaemon() {
        return Flowable.create((emitter) -> {
            if (!emitter.isCancelled()) {
                emitter.onNext(!isRunning);
                if (!isRunning) {
                    while (!Thread.interrupted() && !emitter.isCancelled()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            break;
                        }
                        if (emitter.isCancelled() || isRunning) {
                            break;
                        }
                        emitter.onNext(true);
                    }
                }
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST);
    }

    /**
     * Only calls from TauService
     */
    public void doStart(String seed) {
        logger.info("doStart");
        if (isRunning)
            return;

        // ??????SessionManager????????????
        SessionParams sessionParams = SessionSettings.getSessionParamsBuilder()
                .setAccountSeed(seed)
                .setDeviceID(deviceID)
                .setDatabaseDir(appContext.getApplicationInfo().dataDir) // ???????????????
                .setDumpfileDir(FileUtil.getDumpfileDir())  // Dump File??????
                .setDhtNonReferable(true)
                .setDhtPingInterval(3600)
                .setDhtBootstrapInterval(10)
                .setLogLevel(LogUtil.getTauLogLevel())
                .build();
        sessionManager.start(sessionParams);
    }

    /**
     * Session????????????
     */
    abstract void sessionStopOver();

    /**
     * Only calls from TauService
     * Session??????????????????????????????stop()??????????????????????????????Session????????????????????????
     *
     * TODO:: ???????????????????????????????????????
     */
    public void doStop() {
        if (!isRunning)
            return;
        isRunning = false;
        disposables.clear();
        if (noRemainingDataTimer != null && !noRemainingDataTimer.isDisposed()) {
            noRemainingDataTimer.dispose();
        }
        if (updateLocationTimer != null && !updateLocationTimer.isDisposed()) {
            updateLocationTimer.dispose();
        }
        if (onlineTimer != null && !onlineTimer.isDisposed()) {
            onlineTimer.dispose();
        }
        if (updateBootstrapIntervalTimer != null && !updateBootstrapIntervalTimer.isDisposed()) {
            updateBootstrapIntervalTimer.dispose();
        }
        TauNotifier.getInstance().cancelAllNotify();
        locationManager.stopLocation();
        appContext.unregisterReceiver(powerReceiver);
        appContext.unregisterReceiver(connectionReceiver);
        sessionManager.stop();
        tauDaemonAlertHandler.onCleared();
        txQueueManager.onCleared();
        sessionStopOver();
    }

    /**
     * ????????????
     */
    public void forceStop() {
        Intent i = new Intent(appContext, TauService.class);
        i.setAction(TauService.ACTION_SHUTDOWN);
        Utils.startServiceBackground(appContext, i);
    }

    /**
     * ??????Alert????????????
     * @param listener TauDaemonAlertListener
     */
    void registerAlertListener(TauDaemonAlertListener listener) {
        sessionManager.addListener(listener);
    }

    /**
     * ?????????Alert????????????
     * @param listener TauDaemonAlertListener
     */
    void unregisterAlertListener(TauDaemonAlertListener listener) {
        sessionManager.removeListener(listener);
    }

    /**
     * ???????????????????????????????????????
     */
    private void switchPowerReceiver() {
        boolean chargeState = systemServiceManager.isPlugged();
        setChargingState(chargeState);
        settingsRepo.chargingState(systemServiceManager.isPlugged());

        int batteryLevel = systemServiceManager.getBatteryLevel();
        setBatteryLevel(batteryLevel);
        try {
            appContext.unregisterReceiver(powerReceiver);
        } catch (IllegalArgumentException ignore) {
            /* Ignore non-registered receiver */
        }
        appContext.registerReceiver(powerReceiver, PowerReceiver.getCustomFilter());
    }

    /**
     * ?????????????????????????????????
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
     * ?????????????????????
     * @param key ??????key
     */
    private void handleSettingsChanged(String key) {
        if (key.equals(appContext.getString(R.string.pref_key_internet_state))) {
            logger.info("SettingsChanged, internet state::{}", settingsRepo.internetState());
        } else if (key.equals(appContext.getString(R.string.pref_key_internet_type))) {
            logger.info("SettingsChanged, internet type::{}", settingsRepo.getInternetType());
            reopenNetworkSockets();
        } else if (key.equals(appContext.getString(R.string.pref_key_charging_state))) {
            logger.info("SettingsChanged, charging state::{}", settingsRepo.chargingState());
        } else if (key.equals(appContext.getString(R.string.pref_key_is_metered_network))) {
            logger.info("isMeteredNetwork::{}", NetworkSetting.isMeteredNetwork());
        } else if (key.equals(appContext.getString(R.string.pref_key_foreground_running))) {
            boolean isForeground = settingsRepo.getBooleanValue(key);
            logger.info("foreground running::{}", isForeground);
        } else if (key.equals(appContext.getString(R.string.pref_key_nat_pmp_mapped))) {
            logger.info("SettingsChanged, Nat-PMP mapped::{}", settingsRepo.isNATPMPMapped());
        } else if (key.equals(appContext.getString(R.string.pref_key_upnp_mapped))) {
            logger.info("SettingsChanged, UPnP mapped::{}", settingsRepo.isUPnpMapped());
        } else if (key.equals(appContext.getString(R.string.pref_key_dht_nodes))) {
            long nodes = settingsRepo.getLongValue(key, 0);
            logger.info("SettingsChanged, nodes::{}", nodes);

            if (nodes > 0) {
                newActionEvent();
            } else {
                stopTauDozeTimer();
                // ??????????????????24?????????????????????????????????
                startTauDozeTimer(TauDozeManager.HOURS24_TIME);
            }
        }
    }

    public void setBatteryLevel(int level) {
        tauDozeManager.setBatteryLevel(level);
    }

    public void setDataAvailableRate(int rate) {
        tauDozeManager.setDataAvailableRate(rate);
    }

    public void setChargingState(boolean on) {
        tauDozeManager.setChargingState(on);
    }

    /**
     * ????????????????????????
     */
    public void newActionEvent() {
        logger.debug("TauDoze newActionEvent");
        String nodesKey = appContext.getString(R.string.pref_key_dht_nodes);
        long nodes = settingsRepo.getLongValue(nodesKey, 0);
        if (nodes > 0) {
            stopTauDozeTimer();
            startTauDozeTimer(TauDozeManager.TAU_UP_TIME);
        }
    }

    /**
     * ??????libTAU Doze????????????
     */
    public void resetDozeStartTime() {
        if (tauDozeManager.isDozeMode()) {
            tauDozeManager.resetDozeStartTime();
        }
    }

    /**
     * ??????Tau doze????????????
     */
    private void stopTauDozeTimer() {
        logger.debug("TauDoze stop timer");
        if (libTauDozeTimer != null && !libTauDozeTimer.isDisposed()) {
            libTauDozeTimer.dispose();
            // TAU?????????doze mode???????????????????????????doze??????
            if (tauDozeManager.isDozeMode()) {
                long realDozeTime = tauDozeManager.calculateRealDozeTime();
                if (realDozeTime > 0) {
                    settingsRepo.updateTauDozeTime(realDozeTime);
                    logger.debug("TauDoze totalDozeTime::{}, realDozeTime::{}",
                            settingsRepo.getTauDozeTime(), realDozeTime);
                }
            }
        }
        if (tauDozeManager.isDozeMode()) {
            resumeService();
        }
        tauDozeManager.setDozeMode(false);
    }

    /**
     * ??????Tau doze????????????
     * @param interval ??????s
     */
    private void startTauDozeTimer(long interval) {
        logger.debug("TauDoze start timer");
        libTauDozeTimer = ObservableUtil.intervalSeconds(interval)
                .subscribeOn(Schedulers.io())
                .subscribe(l -> {
                    if (tauDozeManager.isDozeMode()) {
                        settingsRepo.updateTauDozeTime(interval);
                        logger.debug("TauDoze totalDozeTime::{}, realDozeTime::{}",
                                settingsRepo.getTauDozeTime(), interval);
                        if (!libTauDozeTimer.isDisposed()) {
                            libTauDozeTimer.dispose();
                        }
                        newActionEvent();
                    } else {
                        long dozeTime = tauDozeManager.calculateDozeTime(true);
                        logger.debug("TauDoze start dozeTime::{}", dozeTime);
                        if (!libTauDozeTimer.isDisposed()) {
                            libTauDozeTimer.dispose();
                        }
                        if (dozeTime > 0) {
                            // TAU??????Doze Mode?????????????????????????????????doze??????
                            pauseService();
                            tauDozeManager.setDozeMode(true);
                            startTauDozeTimer(dozeTime);
                        }
                    }
                });
    }

    /**
     * ?????????????????????
     */
    private void pauseService() {
        if (isRunning) {
            sessionManager.pauseService();
        }
        logger.info("pauseService isRunning::{}", isRunning);
    }

    /**
     * ?????????????????????
     */
    private void resumeService() {
        if (isRunning) {
            sessionManager.resumeService();
        }
        logger.info("resumeService isRunning::{}", isRunning);
    }

    /**
     * ??????libTAU????????????
     * ???????????????????????????
     */
    private void reopenNetworkSockets() {
        if (!settingsRepo.internetState()) {
            return;
        }
        if (!isRunning) {
            // ???????????????seed????????????
            if (StringUtil.isNotEmpty(seed)) {
                doStart(seed);
            }
        } else {
            SystemServiceManager.getInstance().getNetworkAddress();
            sessionManager.reopenNetworkSockets();
            logger.info("Network change reopen network sockets...");
        }
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????
     */
    void handleNoRemainingDataTips() {
        if (!isRunning || !NetworkSetting.isForegroundRunning()) {
            return;
        }
        // ????????????????????????
        if (settingsRepo.internetState()) {
            if (NetworkSetting.isHaveAvailableData()) {
                // ?????????????????????????????????????????????
                trafficTips = true;
                if (noRemainingDataTimer != null && !noRemainingDataTimer.isDisposed()) {
                    noRemainingDataTimer.dispose();
                }
            } else {
                showNoRemainingDataTipsDialog();
            }
        }
    }

    /**
     * ???????????????????????????????????????
     * ???????????????????????????????????????????????????????????????????????????APP?????????????????????????????????????????????Activity
     */
    private void showNoRemainingDataTipsDialog() {
        if (!trafficTips) {
            return;
        }
        if (noRemainingDataTimer != null && !noRemainingDataTimer.isDisposed()) {
            return;
        }
        noRemainingDataTimer = Observable.timer(SHOW_DIALOG_THRESHOLD, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(aLong -> {
                    if (AppUtil.isOnForeground(appContext) && !AppUtil.isForeground(appContext,
                            TrafficTipsActivity.class)) {
                        Intent intent = new Intent(appContext, TrafficTipsActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        appContext.startActivity(intent);
                    }
                });
    }

    /**
     * ??????????????????????????????
     * @param updateDailyDataLimit ??????????????????????????????
     */
    public void handleUserSelected(boolean updateDailyDataLimit) {
        trafficTips = updateDailyDataLimit;
        if (!trafficTips) {
            if (noRemainingDataTimer != null && !noRemainingDataTimer.isDisposed()) {
                noRemainingDataTimer.dispose();
            }
        }
    }

    /**
     * ??????Sessions???nodes???
     */
    long getSessionNodes() {
        if (!isRunning) {
            return 0;
        }
        return sessionManager.dhtNodes();
    }

    /**
     * ??????Sessions???nodes???
     */
    long getInvokedRequests() {
        if (!isRunning) {
            return 0;
        }
        return sessionManager.invokedRequests();
    }

    /**
     * ??????Sessions???????????????
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
     * ??????????????????????????????
     */
    public void startOnlineTimer() {
        if (null == onlineTimer || onlineTimer.isDisposed()) {
            onlineTimer = ObservableUtil.intervalSeconds(12 * 60 * 60)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe( l -> updateCurrentUserInfo(false));
        }
    }

    /**
     * ????????????????????????
     * @param isUpdateLocation ???????????????????????????
     */
    public void updateCurrentUserInfo(boolean isUpdateLocation) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            UserRepository userRepo = RepositoryHelper.getUserRepository(appContext);
            User user = userRepo.getCurrentUser();
            if (user != null) {
                if (isUpdateLocation) {
                    double longitude = locationManager.getLongitude();
                    double latitude = locationManager.getLatitude();
                    if ((longitude > 0 && user.longitude != longitude) ||
                            (latitude > 0 && user.latitude != latitude)) {
                        user.longitude = longitude;
                        user.latitude = latitude;
                        user.updateLocationTime = getSessionTime() / 1000;
                        userRepo.updateUser(user);
                    }
                } else {
                    updateUserInfo(user);
                }
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * ?????????????????????????????????????????????
     */
    private void updateChainsAndAccountInfo() {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            UserRepository userRepo = RepositoryHelper.getUserRepository(appContext);
            MemberRepository memberRepo = RepositoryHelper.getMemberRepository(appContext);
            User user = userRepo.getCurrentUser();
            if (user != null) {
                String userPk = user.publicKey;
                // 1???????????????followed????????????libTAU???followed???????????????????????????
                List<String> tauChains = getTauAllChains();
                List<String> localChains = memberRepo.queryFollowedCommunities(userPk);
                logger.info("checkAllChains localChain::{}, tauChains::{}",
                        localChains.size(), tauChains.size());
                // 0???????????????TAU Testing Community
                if (localChains.size() == 0) {
                    String peer = "a13e3563ad23048e388ecbaa8e384a83d08c88e77ee79b1b3ba42fd17f736968";
                    String chainID = "278ac0c475551b4aTAU Testing";
                    String tauTesting = LinkUtil.encodeChain(peer, chainID);
                    tauDaemonAlertHandler.addCommunity(tauTesting);
                }
                // 1????????????????????????chains, libTAU??????????????????
                for (String chainID : localChains) {
                    if (!tauChains.contains(chainID)) {
                        // libTAU followChain
                        List<String> list = memberRepo.queryCommunityMembersLimit(chainID, Constants.CHAIN_LINK_BS_LIMIT);
                        Set<String> peers = new HashSet<>(list);
                        boolean success = followChain(chainID, peers);
                        logger.info("checkAllChains followChain chainID::{}, success::{}", chainID, success);
                    } else {
                        // ????????????????????????1.2????????????
                        tauChains.remove(chainID);
                    }
                }
                // 2???????????????????????????chains, libTAU???????????????
                for (String chainID : tauChains) {
                    // libTAU unfollowChain
                    boolean success = unfollowChain(chainID);
                    logger.debug("checkAllChains unfollowChain chainID::{}, success::{}", chainID, success);
                }
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * ??????????????????????????????
     */
    private List<String> getTauAllChains() {
        List<String> list = null;
        if (isRunning) {
            list = sessionManager.getAllChains();
        }
        if (null == list) {
            list = new ArrayList<>();
        }
        return list;
    }

    /**
     * ????????????????????????
     */
    public void requestFriendInfo(String friendPk) {
        subFriendInfo(friendPk);
        subFriendHeadPic(friendPk);
    }

    public void pubUserInfo(String publicKey, byte[] value) {
        if (isRunning) {
            byte[] keySrc = ByteUtil.toByte(publicKey);
            byte[] key = DataKey.getKey(keySrc, DataKey.Suffix.INFO);
            sessionManager.publishData(key, value);
            logger.info("pubUserInfo userPk::{}, key.length::{}", publicKey, key.length);
        }
    }

    public void subFriendInfo(String publicKey) {
        if (isRunning) {
            byte[] keySrc = ByteUtil.toByte(publicKey);
            byte[] key = DataKey.getKey(keySrc, DataKey.Suffix.INFO);
            sessionManager.subscribeFromPeer(keySrc, key);
            logger.info("subFriendInfo peer::{}, key.length::{}", publicKey, key.length);
        }
    }

    public void pubUserHeadPic(String publicKey, byte[] value) {
        if (isRunning) {
            byte[] keySrc = ByteUtil.toByte(publicKey);
            byte[] key = DataKey.getKey(keySrc, DataKey.Suffix.PIC);
            sessionManager.publishData(key, value);
            logger.info("pubUserHeadPic userPk::{}, key.length::{}", publicKey, key.length);
        }
    }

    public void subFriendHeadPic(String publicKey) {
        if (isRunning) {
            byte[] keySrc = ByteUtil.toByte(publicKey);
            byte[] key = DataKey.getKey(keySrc, DataKey.Suffix.PIC);
            logger.info("subFriendHeadPic peer::{}, key.length::{}", publicKey, key.length);
        }
    }

    /**
     * ?????????peer(??????)?????????
     * @param publicKey ??????
     * @param data ????????????
     */
    public void sendToPeer(String publicKey, byte[] data) {
        if (isRunning) {
            byte[] peer = ByteUtil.toByte(publicKey);
            sessionManager.sendToPeer(peer, data);
            logger.info("sendToPeer peer::{}", peer);
        }
    }

    /**
     * ????????????????????????
     * @param chainID ???ID
     */
    public void restartFailedChain(String chainID) {
        if (isRunning) {
            boolean isSuccess = sessionManager.startChain(ChainIDUtil.encode(chainID));
            if (isSuccess) {
                tauDaemonAlertHandler.restartFailedChain(chainID);
            }
            logger.info("restartFailedChain chainID::{}, isSuccess::{}", chainID, isSuccess);
        }
    }

    public MutableLiveData<CopyOnWriteArraySet<String>> getChainStoppedSet() {
        return tauDaemonAlertHandler.getChainStoppedSet();
    }

    /**
     * ??????peer??????????????????
     * @param publicKey ??????
     * @param chainID ???ID
     */
    public void requestChainData(String publicKey, String chainID) {
        if (isRunning) {
            sessionManager.requestChainData(ChainIDUtil.encode(chainID), publicKey);
            logger.info("requestChainData peer::{}, chainID::{}", publicKey, chainID);
        }
    }

    /**
     * publish????????????
     * @param chainID ???ID
     */
    public void pubChainData(String chainID) {
        if (isRunning) {
            sessionManager.putAllChainData(ChainIDUtil.encode(chainID));
            logger.info("pubChainData chainID::{}", chainID);
        }
    }

    /**
     *  ??????libTAU Non Referable
     */
    public void setNonReferable(boolean nonReferable) {
        if (isRunning) {
            if (this.sessionManager != null) {
                (new SessionHandle(sessionManager.swig())).setNonReferrable(nonReferable);
                logger.info("setNonReferable::{}", nonReferable);
            }
        }
    }

    /**
     * ?????????????????????????????????????????????????????? ??????libTAU Bootstrap Interval???600s
     */
    void updateBootstrapInterval() {
        if (updateBootstrapIntervalTimer != null && !updateBootstrapIntervalTimer.isDisposed()) {
            updateBootstrapIntervalTimer.dispose();
        }
        updateBootstrapIntervalTimer = ObservableUtil.intervalSeconds(60)
                .subscribeOn(Schedulers.io())
                .subscribe(l -> updateBootstrapInterval(600));
    }

    /**
     *  ??????libTAU Bootstrap Interval
     * @param interval ????????????
     */
    public boolean updateBootstrapInterval(int interval) {
        if (isRunning) {
            sessionManager.updateBootstrapIntervel(interval);
            logger.info("updateBootstrapInterval::{}s", interval);
            return true;
        }
        return false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * ????????????????????????
     * @return ????????????
     */
    public ArrayList<String> getCommunityAccessList(byte[] chainID) {
        logger.info("getCommunityAccessList isRunning::{}", isRunning);
        if (isRunning) {
            return sessionManager.getAccessList(chainID);
        }
        return null;
    }

    /**
     * ??????????????????
     * ??????-1?????????????????????
     */
    public long getMiningTime(byte[] chainID) {
        logger.info("getMiningTime isRunning::{}", isRunning);
        if (isRunning) {
            return sessionManager.getMiningTime(chainID);
        }
        return -1;
    }

    /**
     * ?????????Bootstrap??????
     */
    public boolean addNewBootstrapPeers(String chainID, Set<String> peers) {
        if (isRunning) {
            boolean isSuccess = sessionManager.addNewBootstrapPeers(ChainIDUtil.encode(chainID), peers);
            logger.info("addNewBootstrapPeers chainID::{}, peers size::{}", chainID, peers.size());
            return isSuccess;
        }
        return false;
    }

    /**
     * ??????????????????
     */
    public void setLogLevel(int level) {
        if (isRunning) {
            sessionManager.setLogLevel(level);
        }
        logger.info("setLogLevel level::{}, isRunning::{}", level, isRunning);
    }

    /**
     * crash??????
     */
    public void crashTest() {
        if (isRunning) {
            sessionManager.crashTest();
        }
        logger.warn("crashTest isRunning::{}", isRunning);
    }

    /**
     * sql??????
     */
    public void sqlTest() {
        if (isRunning) {
            sessionManager.sqlTest();
        }
        logger.warn("sqlTest isRunning::{}", isRunning);
    }

    /**
     * ??????????????????
     */
    public void accountAutoRenewal() {
        tauDaemonAlertHandler.accountAutoRenewal();
    }

    /**
     * reload chain
     * ??????????????????
     */
    public void handleBlockData(Block block, TauListenHandler.BlockStatus status) {
        tauDaemonAlertHandler.handleBlockData(block, status);
    }

    /**
     * ????????????
     */
    public void startLocation() {
        if (locationManager.isNeedPermission()) {
            logger.debug("No location permission");
            return;
        }
        if (null == updateLocationTimer || updateLocationTimer.isDisposed()) {
            locationManager.startLocation();
            updateLocationTimer = ObservableUtil.intervalSeconds(3600)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe( l -> locationManager.startLocation());
        }
    }

    /**
     * ??????????????????????????????
     */
    public void updateTxQueue(String chainID) {
        txQueueManager.updateTxQueue(chainID);
    }

    public void updateTxQueue(String chainID, boolean isResendTx) {
        txQueueManager.updateTxQueue(chainID, isResendTx);
    }

    public void sendTxQueue(TxQueue txQueue, long pinnedTime) {
        txQueueManager.sendTxQueue(txQueue, pinnedTime);
    }

    /**
     * ??????Alert?????????????????????
     */
    public abstract ObservableEmitter getAlertConsumerEmitter();

    /**
     * ????????????Seed
     * @param seed Seed
     */
    public abstract void updateSeed(String seed);

    /**
     * ??????TauDaemonAlert??????
     * ??????????????????????????????????????????
     */
    abstract void observeTauDaemonAlertListener();

    /**
     *  ??????libTAU????????????
     * @param user ????????????
     */
    public abstract boolean updateUserInfo(User user);

    public abstract boolean addNewFriend(String friendPk);

    /**
     * ????????????
     * @param friendPk ????????????
     */
    public abstract boolean deleteFriend(String friendPk);

    /**
     * ???????????????
     */
    public abstract boolean addNewMessage(Message msg);

    /**
     * ??????????????????
     */
    public abstract boolean createNewCommunity(byte[] chainID, Set<Account> accounts);

    /**
     * ?????????????????????ID
     * @param communityName ????????????
     * @return chainID
     */
    public abstract String createNewChainID(String type, String communityName);

    /**
     * ??????????????????
     * @param chainID ??????ID
     * @param publicKey ????????????
     * @return Account ????????????
     */
    public abstract Account getAccountInfo(byte[] chainID, String publicKey);

    /**
     * ????????????????????????
     * @param tx ????????????
     */
    public abstract boolean submitTransaction(Transaction tx);

    /**
     * ?????????
     * @param chainID ???ID
     * @param peers ??????peers
     */
    public abstract boolean followChain(String chainID, Set<String> peers);

    /**
     * ???????????????
     * @param chainID ???ID
     */
    public abstract boolean unfollowChain(String chainID);

    /**
     * ??????Session Time
     */
    public abstract long getSessionTime();

    /**
     * ???????????????????????????
     * @param chainID ???ID
     * @param blockNumber ?????????
     */
    public abstract Block getBlockByNumber(String chainID, long blockNumber);
}
