package io.taucoin.torrent.publishing.core.model;

import android.content.Context;
import android.content.Intent;

import org.libTAU4j.Account;
import org.libTAU4j.Block;
import org.libTAU4j.Message;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.AlertAndUser;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.AppUtil;
import io.taucoin.torrent.publishing.core.utils.DeviceUtils;
import io.taucoin.torrent.publishing.core.utils.FrequencyUtil;
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
    private static final int UPDATE_INTERFACE_THRESHOLD = 30;   // 单位s
    static final int ALERT_QUEUE_CAPACITY = 10000;              // Alert缓存队列
    private static volatile TauDaemon instance;

    Context appContext;
    private SettingsRepository settingsRepo;
    CompositeDisposable disposables = new CompositeDisposable();
    private PowerReceiver powerReceiver = new PowerReceiver();
    private ConnectionReceiver connectionReceiver = new ConnectionReceiver();
    SessionManager sessionManager;
    private SystemServiceManager systemServiceManager;
    private ExecutorService exec = Executors.newSingleThreadExecutor();
    private TauInfoProvider tauInfoProvider;
    private Disposable updateInterfacesTimer;    // 更新libTAU监听接口定时任务
    private Disposable noRemainingDataTimer; // 触发无剩余流量的提示定时任务
    TauDaemonAlertHandler tauDaemonAlertHandler; // libTAU上报的Alert处理程序
    volatile boolean isRunning = false;
    private volatile boolean trafficTips = true; // 剩余流量用完提示
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

        deviceID = DeviceUtils.getCustomDeviceID(appContext);
        sessionManager = new SessionManager(true);

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
                    // 把自己当作朋友添加进libTAU
                    addYourselfAsFriend();
                    // 账户自动更新
                    accountAutoRenewal();
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
                .setDhtNonReferable(true)
                .setDhtPingInterval(3600)
                .setDhtBootstrapInterval(60)
                .setNetworkInterface(getLocalNetworkAddress())
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
        if (updateInterfacesTimer != null && !updateInterfacesTimer.isDisposed()) {
            updateInterfacesTimer.dispose();
        }
        if (noRemainingDataTimer != null && !noRemainingDataTimer.isDisposed()) {
            noRemainingDataTimer.dispose();
        }
        appContext.unregisterReceiver(powerReceiver);
        appContext.unregisterReceiver(connectionReceiver);
        sessionManager.stop();
        tauDaemonAlertHandler.onCleared();
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
            updateListenInterfaces();
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
        } else if (key.equals(appContext.getString(R.string.pref_key_dht_nodes))) {
            long nodes = settingsRepo.getLongValue(key, 0);
            updateInterfacesTimer(nodes);
        }
    }

    /**
     * APP启动30s无peer触发更新libTAU的ListenInterfaces
     */
    private void updateInterfacesTimer(long nodes) {
        if (nodes > 0) {
            if (updateInterfacesTimer != null && !updateInterfacesTimer.isDisposed()) {
                updateInterfacesTimer.dispose();
            }
        } else {
            if (null == updateInterfacesTimer || updateInterfacesTimer.isDisposed()) {
                updateInterfacesTimer = ObservableUtil.intervalSeconds(UPDATE_INTERFACE_THRESHOLD)
                    .subscribeOn(Schedulers.io())
                    .subscribe( l -> {
                        logger.trace("No nodes more than {}s, updateListenInterfaces...",
                                UPDATE_INTERFACE_THRESHOLD);
                        updateListenInterfaces();
                    });
            }
        }
    }

    /**
     * 更新libTAU监听接口
     * 必须有网络才会更新
     */
    private void updateListenInterfaces() {
        if (!settingsRepo.internetState()) {
            return;
        }
        if (!isRunning) {
            // 必须有用户seed才能启动
            if (StringUtil.isNotEmpty(seed)) {
                doStart(seed);
            }
        } else {
            String ipv4 = getLocalNetworkAddress();
            sessionManager.updateListenInterfaces(ipv4);
            logger.debug("updateListenInterfaces ipv4::{}", ipv4);
        }
    }

    private String getLocalNetworkAddress() {
        SystemServiceManager.getInstance().getNetworkAddress();
        String ipv4 = SystemServiceManager.getInstance().getActiveNetworkAddress();
        if (StringUtil.isEmpty(ipv4)) {
            ipv4 = "0.0.0.0";
        }
        ipv4 += ":6881";
        return ipv4;
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
     * 更新朋友信息
     */
    private void addYourselfAsFriend() {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            User user = RepositoryHelper.getUserRepository(appContext).getCurrentUser();
            updateFriendInfo(user);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     *  更新libTAU Bootstrap Interval
     * @param interval 时间间隔
     */
    public boolean updateBootstrapInterval(int interval) {
        if (isRunning) {
            logger.debug("updateBootstrapInterval::{}s, success::{}", interval, true);
            return true;
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
    public abstract boolean createNewCommunity(byte[] chainID, Map<String, Account> accounts);

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