package io.taucoin.torrent.publishing.core.model;

import android.content.Context;
import android.os.Build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposables;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Statistic;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.StatisticRepository;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.Sampler;
import io.taucoin.torrent.publishing.core.utils.SessionStatistics;
import io.taucoin.torrent.publishing.core.utils.TrafficUtil;
import io.taucoin.torrent.publishing.ui.constant.Constants;

/**
 * Provides runtime information about Tau, which isn't saved to the database.
 */
public class TauInfoProvider {
    private static final String TAG = TauInfoProvider.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TAG);
    private static final int STATISTICS_PERIOD = 2000;
    private static final int MEM_STATISTICS_PERIOD = 60 * 1000;                 // 单位为ms
    private static final int CPU_STATISTICS_PERIOD = 4 * 1000;                  // 单位为ms

    private static volatile TauInfoProvider INSTANCE;
    private TauDaemon daemon;
    private Sampler sampler;
    private SettingsRepository settingsRepo;
    private StatisticRepository statisticRepo;

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
    }

    /**
     * 观察torrent SessionStats的工作流
     * @return Flowable
     */
    public Flowable<Long> observeSessionStats() {
        return makeSessionStatsFlowable();
    }

    /**
     * 创建torrent SessionStats的工作流
     * @return Flowable
     */
    private Flowable<Long> makeSessionStatsFlowable() {
        return Flowable.create((emitter) -> {
            try {
                Thread.currentThread().setName("SessionNodes");
                long oldNodes = -1;
                while (!emitter.isCancelled()) {
                    long sessionNodes = daemon.getSessionNodes();
                    if (oldNodes == -1 || oldNodes != sessionNodes) {
                        oldNodes = sessionNodes;
                        emitter.onNext(sessionNodes);
                    }
                    if (!emitter.isCancelled()) {
                        Thread.sleep(STATISTICS_PERIOD);
                    }
                }
            } catch (InterruptedException ignore) {
            } catch (Exception e) {
                logger.error("makeSessionStatsFlowable is error", e);
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
                long trafficSize;
                long oldTrafficTotal = 0;
                SessionStatistics sessionStatistics = new SessionStatistics();
                Statistic statistic = new Statistic();
                int seconds = 0;
                long lastMemQueryTime = DateUtil.getMillisTime();
                long lastCPUQueryTime = DateUtil.getMillisTime();
                while (!emitter.isCancelled()) {
                    handlerTrafficStatistics(sessionStatistics);
                    long trafficTotal = sessionStatistics.getTotalDownload() + sessionStatistics.getTotalUpload();
                    trafficSize = trafficTotal - oldTrafficTotal;
                    trafficSize = Math.max(trafficSize, 0);
                    oldTrafficTotal = trafficTotal;

                    long currentTime = DateUtil.getMillisTime();
                    // 内存采样：AndroidQ开始限制采样频率5分钟
                    if (samplerStatistics.totalMemory == 0 ||
                            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ||
                            currentTime - lastMemQueryTime >= MEM_STATISTICS_PERIOD) {
                        lastMemQueryTime = currentTime;
                        samplerStatistics.totalMemory = sampler.sampleMemory();
                        settingsRepo.setMemoryUsage(samplerStatistics.totalMemory);
                    }
                    // CPU采样
                    if (currentTime - lastCPUQueryTime >= CPU_STATISTICS_PERIOD) {
                        lastCPUQueryTime = currentTime;
                        float cpuUsageRate = sampler.sampleCPU();
                        if (cpuUsageRate < 0) {
                            cpuUsageRate = 0;
                        } else if (cpuUsageRate > 100) {
                            cpuUsageRate = 100;
                        }
                        samplerStatistics.cpuUsage = cpuUsageRate;
                        settingsRepo.setCpuUsage(samplerStatistics.cpuUsage);
                    }

                    statistic.timestamp = DateUtil.getTime();
                    statistic.dataSize = trafficSize;
                    statistic.memorySize = samplerStatistics.totalMemory;
                    statistic.cpuUsageRate = samplerStatistics.cpuUsage;
                    statistic.isMetered = NetworkSetting.isMeteredNetwork() ? 1 : 0;
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
        TrafficUtil.saveTrafficTotal(statistics);
        // 更新网速采样数据
        NetworkSetting.updateNetworkSpeed(statistics);
        // 更新UI展示链端主循环时间间隔
        NetworkSetting.calculateMainLoopInterval();
        // 根据当前的流量包的使用，判断是否给用户更换流量包的提示
        daemon.handleNoRemainingDataTips();
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
