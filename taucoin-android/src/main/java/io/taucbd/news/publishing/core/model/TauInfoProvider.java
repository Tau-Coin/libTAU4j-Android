package io.taucbd.news.publishing.core.model;

import android.content.Context;
import android.os.Build;
import android.os.Debug;
import android.os.PowerManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposables;
import io.taucbd.news.publishing.BuildConfig;
import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.storage.sp.SettingsRepository;
import io.taucbd.news.publishing.core.storage.RepositoryHelper;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Statistic;
import io.taucbd.news.publishing.core.storage.sqlite.repo.StatisticRepository;
import io.taucbd.news.publishing.core.utils.DateUtil;
import io.taucbd.news.publishing.core.utils.Formatter;
import io.taucbd.news.publishing.core.utils.NetworkSetting;
import io.taucbd.news.publishing.core.utils.Sampler;
import io.taucbd.news.publishing.core.utils.SessionStatistics;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.TrafficUtil;
import io.taucbd.news.publishing.core.Constants;
import io.taucbd.news.publishing.service.SystemServiceManager;

import static java.lang.Runtime.getRuntime;

/**
 * Provides runtime information about Tau, which isn't saved to the database.
 */
public class TauInfoProvider {
    private static final String TAG = TauInfoProvider.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TAG);
    public static final int STATISTICS_PERIOD = 2000;
    private static final int MEM_STATISTICS_PERIOD = 60 * 1000;                 // 单位为ms
    private static final int CPU_STATISTICS_PERIOD = 4 * 1000;                  // 单位为ms

    private static volatile TauInfoProvider INSTANCE;
    private TauDaemon daemon;
    private Sampler sampler;
    private SettingsRepository settingsRepo;
    private StatisticRepository statisticRepo;
    private PowerManager pm;

    public static TauInfoProvider getInstance(@NonNull Context appContext) {
        if (INSTANCE == null) {
            synchronized (TauInfoProvider.class) {
                if (INSTANCE == null)
                    INSTANCE = new TauInfoProvider(TauDaemon.getInstance(appContext));
            }
        }
        return INSTANCE;
    }

    public static TauInfoProvider getInstance(TauDaemon tauDaemon) {
        if (INSTANCE == null) {
            synchronized (TauInfoProvider.class) {
                if (INSTANCE == null)
                    INSTANCE = new TauInfoProvider(tauDaemon);
            }
        }
        return INSTANCE;
    }

    private TauInfoProvider(TauDaemon daemon) {
        this.daemon = daemon;
        this.sampler = Sampler.getInstance();
        settingsRepo = RepositoryHelper.getSettingsRepository(MainApplication.getInstance());
        statisticRepo = RepositoryHelper.getStatisticRepository(MainApplication.getInstance());
        pm = (PowerManager) MainApplication.getInstance().getSystemService(Context.POWER_SERVICE);
    }

    /**
     * 创建APP CPU统计的工作流
     * @return Flowable
     */
    Flowable<Double> observeCPUStatistics() {
        return Flowable.create((emitter) -> {
            if (sampler.isAccessibleFromFile() || !BuildConfig.DEBUG) {
                emitter.onComplete();
                return;
            }
            String line;
            String cmd = "top";
            Process process;
            try {
                process = getRuntime().exec(new String[]{"sh", "-c", cmd});
                emitter.setDisposable(Disposables.fromAction(() -> {
                    if (process != null) {
                        process.destroy();
                        logger.debug("CpuStatistics: shell destroy");
                    }
                }));
                getRuntime().addShutdownHook(new Thread(){
                    @Override
                    public void run() {
                        if (process != null) {
                            process.destroy();
                            logger.debug("CpuStatistics process shutdown");
                        }
                    }
                });
                int processors = getRuntime().availableProcessors();
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                int currentPid = android.os.Process.myPid();
//                int shellPid = AppUtil.getPid(process);
//                logger.debug("CpuStatistics currentPid::{}, shellPid::{}", currentPid, shellPid);
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    logger.debug("CpuStatistics line::{}", line);
                    String[] resp = line.split(" ");
                    if (resp.length > 0) {
                        int pid = StringUtil.getIntString(resp[0]);
                        if (currentPid == pid) {
                            int count = 0;
                            for (String value : resp) {
                                if (StringUtil.isNotEmpty(value) ) {
                                    count ++;
                                    if (count == 9) {
                                        float cpuUsageRate = StringUtil.getFloatString(value);
                                        cpuUsageRate = cpuUsageRate / processors;
                                        cpuUsageRate = Math.max(0, cpuUsageRate);
                                        cpuUsageRate = Math.min(100, cpuUsageRate);
                                        settingsRepo.setCpuUsage(cpuUsageRate);
                                        logger.debug("CpuStatistics cpuUsage::{}%, " +
                                                        "processors::{}, cpuUsage/processors::{}%",
                                                value, processors, cpuUsageRate);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                process.waitFor();
            } catch (IOException | InterruptedException ignore) {
            } catch (Exception e) {
                logger.error("CpuStatistics:", e);
            }
        }, BackpressureStrategy.LATEST);
    }

    /**
     * 观察APP统计
     * @return
     */
    Flowable<Void> observeAppStatistics() {
        return makeAppStatisticsFlowable();
    }

    /**
     * 创建流量统计流
     * @return
     */
    private Flowable<Void> makeAppStatisticsFlowable() {
        return Flowable.create((emitter) -> {
            try {
                Thread.currentThread().setName("AppStatistics");
                Sampler.Statistics samplerStatistics = new Sampler.Statistics();
                if (!emitter.isCancelled()) {
                    emitter.setDisposable(Disposables.fromAction(() -> {

                    }));
                }
                SessionStatistics sessionStatistics = new SessionStatistics();
                Statistic statistic = new Statistic();
                int seconds = 0;
                long lastMemQueryTime = DateUtil.getMillisTime();
                long lastCPUQueryTime = DateUtil.getMillisTime();
                int processors = getRuntime().availableProcessors();

                // nodes和invoked统计
                long oldNodes = -1;
                long oldInvokedRequests = -1;
                String invokedKey = MainApplication.getInstance().getString(R.string.pref_key_dht_invoked_requests);
                String nodesKey = MainApplication.getInstance().getString(R.string.pref_key_dht_nodes);

                while (!emitter.isCancelled()) {
                    // 防止android doze造成延迟
                    daemon.checkDataResend();

                    long currentTime = DateUtil.getMillisTime();
                    // 内存采样：AndroidQ开始限制采样频率5分钟
                    if (samplerStatistics.totalMemory == 0 ||
                            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ||
                            currentTime - lastMemQueryTime >= MEM_STATISTICS_PERIOD) {
                        lastMemQueryTime = currentTime;
                        Debug.MemoryInfo memoryInfo = sampler.sampleMemory();
                        if (memoryInfo != null && memoryInfo.getTotalPss() > 0) {
                            samplerStatistics.totalMemory = memoryInfo.getTotalPss() * 1024;
                            // 设置最大内存限制
                            long heapSize = (memoryInfo.dalvikPss + memoryInfo.nativePss) * 1024;
                            long totalPss = memoryInfo.dalvikPss + memoryInfo.nativePss + memoryInfo.otherPss;
                            logger.debug("NetworkSetting totalPss::{}, totalPssNotSwapped::{}, dalvikPss::{}, nativePss::{}," +
                                            " otherPss::{}",
                                    Formatter.formatFileSize(MainApplication.getInstance(), memoryInfo.getTotalPss() * 1024),
                                    Formatter.formatFileSize(MainApplication.getInstance(), totalPss * 1024),
                                    Formatter.formatFileSize(MainApplication.getInstance(), memoryInfo.dalvikPss * 1024),
                                    Formatter.formatFileSize(MainApplication.getInstance(), memoryInfo.nativePss * 1024),
                                    Formatter.formatFileSize(MainApplication.getInstance(), memoryInfo.otherPss * 1024));
                            settingsRepo.setCurrentHeapSize(heapSize);
                        }
                        settingsRepo.setMemoryUsage(samplerStatistics.totalMemory);
                    }
                    // CPU采样
                    if (sampler.isAccessibleFromFile() && currentTime - lastCPUQueryTime >= CPU_STATISTICS_PERIOD) {
                        lastCPUQueryTime = currentTime;
                        float cpuUsageRate = sampler.sampleCPU();
                        cpuUsageRate = cpuUsageRate / processors;
                        cpuUsageRate = Math.max(0, cpuUsageRate);
                        cpuUsageRate = Math.min(100, cpuUsageRate);
                        samplerStatistics.cpuUsage = cpuUsageRate;
                        settingsRepo.setCpuUsage(samplerStatistics.cpuUsage);
                    } else {
                        samplerStatistics.cpuUsage = settingsRepo.getCpuUsage();
                    }

                    // 流量统计
                    boolean isWiFi = SystemServiceManager.getInstance().isWiFi();
                    NetworkSetting.setWiFiNetwork(isWiFi);
                    handlerTrafficStatistics(sessionStatistics);

                    // nodes和invoked统计
                    long sessionNodes = daemon.getSessionNodes();
                    if (oldNodes == -1 || oldNodes != sessionNodes) {
                        oldNodes = sessionNodes;
                        settingsRepo.setLongValue(nodesKey, sessionNodes);
                    }
                    long invokedRequests = daemon.getInvokedRequests();
                    long requests;
                    if (invokedRequests < oldInvokedRequests) {
                        requests = invokedRequests;
                    } else {
                        requests = invokedRequests - oldInvokedRequests;
                    }
                    settingsRepo.setLongValue(invokedKey, requests);
                    oldInvokedRequests = invokedRequests;

                    logger.info("invokedRequests::{} requests::{}, sessionNodes::{}",
                            invokedRequests, requests, sessionNodes);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        logger.debug("deviceIdleMode::{}, interactive::{}, powerSaveMode::{}",
                                pm.isDeviceIdleMode(), pm.isInteractive(), pm.isPowerSaveMode());
                    }

                    statistic.timestamp = DateUtil.getTime();
                    statistic.dataSize = sessionStatistics.getDownloadRate() + sessionStatistics.getUploadRate();
                    statistic.memorySize = samplerStatistics.totalMemory;
                    statistic.cpuUsageRate = samplerStatistics.cpuUsage;
                    statistic.isMetered = NetworkSetting.isWiFiNetwork() ? 0 : 1;
                    statistic.nodes = sessionNodes;
                    statistic.invokedRequests = requests;
                    statisticRepo.addStatistic(statistic);
                    if (seconds > Constants.STATISTICS_CLEANING_PERIOD) {
                        statisticRepo.deleteOldStatistics();
                        seconds = 0;
                    }
                    seconds ++;
                    Thread.sleep(STATISTICS_PERIOD);
                }
            } catch (InterruptedException ignore) {
            } catch (Exception e) {
                logger.error("makeAppStatisticsFlowable is error", e);
            }
        }, BackpressureStrategy.LATEST);
    }

    /**
     * 统计流量使用信息
     */
    private void handlerTrafficStatistics(SessionStatistics statistics) {
        daemon.getSessionStatistics(statistics);
        // 保存流量统计
//        TrafficUtil.saveTrafficTotal(statistics);
        TrafficUtil.resetTrafficInfo();
        // 更新网速采样数据
        NetworkSetting.updateNetworkSpeed(statistics);
//        Context context = MainApplication.getInstance();
//        logger.debug("Network statistical:: totalDownload::{}({}), totalUpload::{}({})" +
//                        ", downloadRate::{}/s, uploadRate::{}/s",
//                Formatter.formatFileSize(context, statistics.getTotalDownload()),
//                statistics.getTotalDownload(),
//                Formatter.formatFileSize(context, statistics.getTotalUpload()),
//                statistics.getTotalUpload(),
//                Formatter.formatFileSize(context, statistics.getDownloadRate()),
//                Formatter.formatFileSize(context, statistics.getUploadRate()));
    }
}
