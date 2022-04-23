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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
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
import io.taucoin.torrent.publishing.core.model.data.AlertAndUser;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.utils.AppUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DeviceUtils;
import io.taucoin.torrent.publishing.core.utils.FrequencyUtil;
import io.taucoin.torrent.publishing.core.utils.LocationManagerUtil;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.ObservableUtil;
import io.taucoin.torrent.publishing.core.utils.SessionStatistics;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.TrafficUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.receiver.ConnectionReceiver;
import io.taucoin.torrent.publishing.service.SystemServiceManager;
import io.taucoin.torrent.publishing.receiver.PowerReceiver;
import io.taucoin.torrent.publishing.service.TauService;
import io.taucoin.torrent.publishing.ui.setting.TrafficTipsActivity;

/**
 * 区块链业务Daemon
 */
public abstract class TauDaemon {
    private static final String TAG = TauDaemon.class.getSimpleName();
    static final Logger logger = LoggerFactory.getLogger(TAG);
    private static final int SHOW_DIALOG_THRESHOLD = 10;        // 单位s
    static final int ALERT_QUEUE_CAPACITY = 10000;              // Alert缓存队列
    private static volatile TauDaemon instance;

    Context appContext;
    private SettingsRepository settingsRepo;
    CompositeDisposable disposables = new CompositeDisposable();
    private PowerReceiver powerReceiver = new PowerReceiver();
    private ConnectionReceiver connectionReceiver = new ConnectionReceiver();
    SessionManager sessionManager;
    private SystemServiceManager systemServiceManager;
    private TauInfoProvider tauInfoProvider;
    private LocationManagerUtil locationManager;
    private Disposable updateBootstrapIntervalTimer; // 更新BootstrapInterval定时任务
    private Disposable updateLocationTimer;          // 更新位置信息定时任务
    private Disposable noRemainingDataTimer;         // 触发无剩余流量的提示定时任务
    TauDaemonAlertHandler tauDaemonAlertHandler;     // libTAU上报的Alert处理程序
    private TxQueueManager txQueueManager;           //交易队列管理
    volatile boolean isRunning = false;
    private volatile boolean trafficTips = true;     // 剩余流量用完提示
    volatile String seed;
    String deviceID;

    // libTAU上报的Alert缓存队列
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
     * TauDaemon构造函数
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

        observeTauDaemon();
        initLocalParam();
        handleNoRemainingDataTips();
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
                    // 防止第一次更新时，链端未启动成功，后面无法触发
                    handleSettingsChanged(appContext.getString(R.string.pref_key_main_loop_frequency));
                    // 更新当前用户自己的信息
                    updateCurrentUserInfo();
                    // 账户自动更新
                    accountAutoRenewal();
                    // 更新用户社区账户状态
                    updateUserAccountInfo();
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
     * 初始化本地参数
     */
    private void initLocalParam() {
        switchPowerReceiver();
        switchConnectionReceiver();
        TrafficUtil.resetTrafficTotalOld();
        settingsRepo.initData();
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

        // 设置SessionManager启动参数
        SessionParams sessionParams = SessionSettings.getSessionParamsBuilder()
                .setAccountSeed(seed)
                .setDeviceID(deviceID)
                .setDatabaseDir(appContext.getApplicationInfo().dataDir)
                .setDhtNonReferable(true)
                .setDhtPingInterval(3600)
                .setDhtBootstrapInterval(10)
                .build();
        sessionManager.start(sessionParams);
    }

    /**
     * Session通知结束
     */
    abstract void sessionStopOver();

    /**
     * Only calls from TauService
     * Session停止结束方案：直接是stop()方法执行结束，就认为Session内存已释放结束；
     *
     * TODO:: 这种方案是否可行，有待商榷
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
        if (updateBootstrapIntervalTimer != null && !updateBootstrapIntervalTimer.isDisposed()) {
            updateBootstrapIntervalTimer.dispose();
        }
        locationManager.stopLocation();
        appContext.unregisterReceiver(powerReceiver);
        appContext.unregisterReceiver(connectionReceiver);
        sessionManager.stop();
        tauDaemonAlertHandler.onCleared();
        txQueueManager.onCleared();
        sessionStopOver();
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
    void registerAlertListener(TauDaemonAlertListener listener) {
        sessionManager.addListener(listener);
    }

    /**
     * 反注册Alert监听事件
     * @param listener TauDaemonAlertListener
     */
    void unregisterAlertListener(TauDaemonAlertListener listener) {
        sessionManager.removeListener(listener);
    }

    /**
     * 电源充电状态切换广播接受器
     */
    private void switchPowerReceiver() {
        settingsRepo.chargingState(systemServiceManager.isPlugged());
        settingsRepo.setBatteryLevel(systemServiceManager.getBatteryLevel());
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
        } else if (key.equals(appContext.getString(R.string.pref_key_main_loop_frequency))) {
            setMainLoopInterval(FrequencyUtil.getMainLoopInterval());
        }
    }

    /**
     * 更新libTAU监听接口
     * 必须有网络才会更新
     */
    private void reopenNetworkSockets() {
        if (!settingsRepo.internetState()) {
            return;
        }
        if (!isRunning) {
            // 必须有用户seed才能启动
            if (StringUtil.isNotEmpty(seed)) {
                doStart(seed);
            }
        } else {
            SystemServiceManager.getInstance().getNetworkAddress();
            sessionManager.reopenNetworkSockets();
            logger.debug("Network change reopen network sockets...");
        }
    }

    /**
     * 根据当前的流量包的使用，判断是否给用户更换流量包的提示
     */
    void handleNoRemainingDataTips() {
        if (!isRunning || !NetworkSetting.isForegroundRunning()) {
            return;
        }
        // 判断有无网络连接
        if (settingsRepo.internetState()) {
            if (NetworkSetting.isHaveAvailableData()) {
                // 重置无可用流量提示对话框的参数
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
     * 显示没有剩余流量提示对话框
     * 必须同时满足需要提示、触发次数大于等于网速采样数、APP在前台、目前没有打开的流量提示Activity
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
     * 处理用户流量提示选择
     * @param updateDailyDataLimit 是否更新每日流量限制
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
     * 统计Sessions的nodes数
     */
    long getSessionNodes() {
        if (!isRunning) {
            return 0;
        }
        return sessionManager.dhtNodes();
    }

    /**
     * 统计Sessions的nodes数
     */
    long getInvokedRequests() {
        if (!isRunning) {
            return 0;
        }
        return sessionManager.invokedRequests();
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
     * 更新当前用户信息
     */
    public void updateCurrentUserInfo() {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            UserRepository userRepo = RepositoryHelper.getUserRepository(appContext);
            User user = userRepo.getCurrentUser();
            if (user != null) {
                double longitude = locationManager.getLongitude();
                double latitude = locationManager.getLatitude();
                if ((longitude > 0 && user.longitude != longitude) ||
                        (latitude > 0 && user.latitude != latitude)) {
                    user.longitude = longitude;
                    user.latitude = latitude;
                    user.updateLocationTime = getSessionTime() / 1000;
                    userRepo.updateUser(user);
                }
                updateFriendInfo(user);
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 更新用户社区账户状态
     */
    public void updateUserAccountInfo() {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            UserRepository userRepo = RepositoryHelper.getUserRepository(appContext);
            MemberRepository memberRepo = RepositoryHelper.getMemberRepository(appContext);
            User user = userRepo.getCurrentUser();
            if (user != null) {
                // 获取未加入或者过期社区列表
                List<Member> list = memberRepo.getUnJoinedExpiredCommunityList(user.publicKey);
                logger.debug("updateUserAccountInfo count::{}", null == list ? 0 : list.size());
                if (list != null && list.size() > 0) {
                    for (Member m: list) {
                        requestAccountState(m.chainID);
                    }
                }
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 请求用户社区账户状态
     */
    public void requestAccountState(String chainID) {
        if (isRunning) {
            if (StringUtil.isNotEmpty(chainID)) {
                sessionManager.requestChainState(ChainIDUtil.encode(chainID));
                logger.debug("requestAccountState::{}", chainID);
            }
        }
    }

    /**
     * 请求更新朋友信息
     */
    public void requestFriendInfo(String friendPk) {
        if (isRunning) {
            if (StringUtil.isNotEmpty(friendPk)) {
                sessionManager.requestFriendInfo(friendPk);
                logger.debug("requestFriendInfo::{}", friendPk);
            }
        }
    }

    /**
     *  设置libTAU Non Referable
     */
    public void setNonReferable(boolean nonReferable) {
        if (isRunning) {
            if (this.sessionManager != null) {
                (new SessionHandle(sessionManager.swig())).setNonReferrable(nonReferable);
                logger.debug("setNonReferable::{}", nonReferable);
            }
        }
    }

    /**
     * 每次更新网络接口监听成功，定时一分钟 更新libTAU Bootstrap Interval为600s
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
     *  更新libTAU Bootstrap Interval
     * @param interval 时间间隔
     */
    public boolean updateBootstrapInterval(int interval) {
        if (isRunning) {
            sessionManager.updateBootstrapIntervel(interval);
            logger.debug("updateBootstrapInterval::{}s", interval);
            return true;
        }
        return false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * 获取挖矿时间
     * 返回-1：代表出不了块
     */
    public long getMiningTime(byte[] chainID) {
        logger.debug("getMiningTime isRunning::{}", isRunning);
        if (isRunning) {
            return sessionManager.getMiningTime(chainID);
        }
        return -1;
    }

    /**
     * 设置优先级链（用户正在查看的链）
     */
    public void setPriorityChain(String chainID) {
        if (isRunning) {
            sessionManager.setPriorityChain(ChainIDUtil.encode(chainID));
            logger.debug("focusOnChain::{}", chainID);
        }
    }

    /**
     * 取消设置优先级链（用户正在查看的链）
     */
    public void unsetPriorityChain(String chainID) {
        if (isRunning) {
            sessionManager.unsetPriorityChain();
            logger.debug("focusOnChain::{}", chainID);
        }
    }

    /**
     * 判断交易是否在交易池
     */
    public boolean isTxInFeePool(String chainID, String txID) {
        if (isRunning) {
            boolean isExist = sessionManager.isTxInFeePool(ChainIDUtil.encode(chainID), txID);
            logger.debug("isTxInFeePool::{}, chainID::{}, txID::{}", isExist, chainID, txID);
            return isExist;
        }
        return false;
    }

    /**
     * 账户自动更新
     */
    public void accountAutoRenewal() {
        tauDaemonAlertHandler.accountAutoRenewal();
    }

    /**
     * reload chain
     * 处理区块数据
     */
    public void handleBlockData(Block block, TauListenHandler.BlockStatus status) {
        tauDaemonAlertHandler.handleBlockData(block, status);
    }

    /**
     * 开始定位
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
     * 发送更新转账交易队列
     */
    public void updateTxQueue(String chainID) {
        txQueueManager.updateTxQueue(chainID);
    }

    public void updateTxQueue(String chainID, boolean isResendTx) {
        txQueueManager.updateTxQueue(chainID, isResendTx);
    }

    /**
     * 获取Alert消费者的发射器
     */
    public abstract ObservableEmitter getAlertConsumerEmitter();

    /**
     * 更新用户Seed
     * @param seed Seed
     */
    public abstract void updateSeed(String seed);

    /**
     * 观察TauDaemonAlert变化
     * 直接进队列，然后单独线程处理
     */
    abstract void observeTauDaemonAlertListener();

    /**
     *  更新libTAU朋友信息
     *  包含加朋友和朋友信息
     * @param friend 朋友对象
     */
    public abstract boolean updateFriendInfo(User friend);

    /**
     * 删除朋友
     * @param friendPk 朋友公钥
     */
    public abstract boolean deleteFriend(String friendPk);

    /**
     * 设置libTAU主循环时间间隔
     */
    public abstract void setMainLoopInterval(int interval);

    /**
     * 添加新消息
     */
    public abstract boolean addNewMessage(Message msg);

    /**
     * 创建新的社区
     */
    public abstract boolean createNewCommunity(byte[] chainID, Map<String, Account> accounts,
                                               Transaction tx);

    /**
     * 创建新的社区链ID
     * @param communityName 社区名称
     * @return chainID
     */
    public abstract String createNewChainID(String communityName);

    /**
     * 获取账户信息
     * @param chainID 社区ID
     * @param publicKey 用户公钥
     * @return Account 账户信息
     */
    public abstract Account getAccountInfo(byte[] chainID, String publicKey);

    /**
     * 提交交易到交易池
     * @param tx 交易对象
     */
    public abstract boolean submitTransaction(Transaction tx);

    /**
     * 跟随链
     * @param chainID 链ID
     * @param peers 链上peers
     */
    public abstract boolean followChain(String chainID, Set<String> peers);

    /**
     * 取消跟随链
     * @param chainID 链ID
     */
    public abstract boolean unfollowChain(String chainID);

    /**
     * 获取tip前三名区块号和哈希
     * @param chainID 链ID
     * @param topNum 获取数目
     */
    public abstract List<Block> getTopTipBlock(String chainID, int topNum);

    /**
     * 获取交易打包的最小交易费
     * @param chainID 链ID
     */
    public abstract long getMedianTxFree(String chainID);

    /**
     * 获取Session Time
     */
    public abstract long getSessionTime();

    /**
     * 通过区块号查询区块
     * @param chainID 链ID
     * @param blockNumber 区块号
     */
    public abstract Block getBlockByNumber(String chainID, long blockNumber);

    /**
     * 通过区块hash查询区块
     * @param chainID 链ID
     * @param hash 区块hash
     */
    public abstract Block getBlockByHash(String chainID, String hash);
}