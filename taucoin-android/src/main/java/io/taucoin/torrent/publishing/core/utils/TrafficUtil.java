package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;

/**
 * 流量统计工具类
 */
public class TrafficUtil {
    private static final Logger logger = LoggerFactory.getLogger("TrafficUtil");
    private static final String TRAFFIC_DOWN = "download";
    private static final String TRAFFIC_UP = "upload";
    private static final String TRAFFIC_METERED = "metered";
    private static final String TRAFFIC_WIFI = "wifi";

    private static final String TRAFFIC_VALUE_OLD = "pref_key_traffic_old_";
    private static final String TRAFFIC_VALUE = "pref_key_traffic_";
    private static final String TRAFFIC_TIME = "pref_key_traffic_time";
    public static  int TRAFFIC_RESET_TIME = 20; // 流量统计重置时间为20点

    private static SettingsRepository settingsRepo;
    static {
        Context context = MainApplication.getInstance();
        settingsRepo = RepositoryHelper.getSettingsRepository(context);
    }

    /**
     * 保存当前流量总量（统计入口）
     * @param statistics 当前网络数据总量
     */
    public static void saveTrafficTotal(@NonNull SessionStatistics statistics) {
        long incrementalDown = saveTrafficTotal(TRAFFIC_DOWN, statistics.getTotalDownload());
        long incrementalUp = saveTrafficTotal(TRAFFIC_UP, statistics.getTotalUpload());
        if (NetworkSetting.isMeteredNetwork()) {
            long total = getMeteredTrafficTotal();
            total += incrementalDown + incrementalUp;
            settingsRepo.setLongValue(TRAFFIC_VALUE + TRAFFIC_METERED, total);
            logger.debug("Save metered traffic::{}", total);
        } else {
            long total = getWifiTrafficTotal();
            total += incrementalDown + incrementalUp;
            settingsRepo.setLongValue(TRAFFIC_VALUE + TRAFFIC_WIFI, total);
            logger.debug("Save wifi traffic::::{}", total);
        }
    }

    /**
     * 根据流量类型，计算增量流量，更新流量统计值
     * @param trafficType
     * @param byteSize
     */
    private static long saveTrafficTotal(String trafficType, long byteSize) {
        resetTrafficInfo();
        String trafficValueOld = TRAFFIC_VALUE_OLD + trafficType;
        long incrementalSize = calculateIncrementalSize(trafficType, byteSize);
        settingsRepo.setLongValue(trafficValueOld, byteSize);
        String trafficValue = TRAFFIC_VALUE + trafficType;
        long trafficTotal = settingsRepo.getLongValue(trafficValue);
        trafficTotal += incrementalSize;
        settingsRepo.setLongValue(trafficValue, trafficTotal);
        return incrementalSize;
    }

    /**
     * 计算增量大小
     * @param trafficType
     * @param byteSize
     * @return
     */
    static long calculateIncrementalSize(String trafficType, long byteSize) {
        resetTrafficInfo();
        String trafficValueOld = TRAFFIC_VALUE_OLD + trafficType;
        long oldTraffic = settingsRepo.getLongValue(trafficValueOld, -1);
        // 重置流量数据时oldTraffic == -1， 第一次流量统计为0
        if (oldTraffic >= 0 && byteSize >= oldTraffic) {
            byteSize = byteSize - oldTraffic;
        } else {
            byteSize = 0;
        }
        return byteSize;
    }

    /**
     * 重置上一次本地流量统计信息
     */
    public static void resetTrafficTotalOld() {
        settingsRepo.setLongValue(TRAFFIC_VALUE_OLD + TRAFFIC_DOWN, -1);
        settingsRepo.setLongValue(TRAFFIC_VALUE_OLD + TRAFFIC_UP, -1);
        settingsRepo.setLongValue(TRAFFIC_VALUE_OLD + TRAFFIC_METERED, -1);
        settingsRepo.setLongValue(TRAFFIC_VALUE_OLD + TRAFFIC_WIFI, -1);
    }

    /**
     * 重置本地流量统计信息
     */
    private synchronized static void resetTrafficInfo() {
        long currentTrafficTime = new Date().getTime();
        long oldTrafficTime = settingsRepo.getLongValue(TRAFFIC_TIME);
        if (oldTrafficTime == 0) {
            // 第一次直接数据重置
            resetTrafficInfo(oldTrafficTime, currentTrafficTime);
        } else {
            // 当前时间与前一次重置时间相隔天数
            int days = DateUtil.compareDay(oldTrafficTime, currentTrafficTime);
            if (days >= 2) {
                // 相隔天数大于等于2天数据重置
                resetTrafficInfo(oldTrafficTime, currentTrafficTime);
            } else if (days == 1 && DateUtil.getHourOfDay() >= TRAFFIC_RESET_TIME) {
                // 相隔天数等于1天，并且当前时间点大于等于重置时间点
                resetTrafficInfo(oldTrafficTime, currentTrafficTime);
            } else if (days == 0 && DateUtil.getHourOfDay() >= TRAFFIC_RESET_TIME
                    && DateUtil.getHourOfDay(oldTrafficTime) < TRAFFIC_RESET_TIME) {
                // 相隔天数等于0天（同一天），当前时间点大于等于重置时间点，并且前一次重置时间点小于重置时间点
                resetTrafficInfo(oldTrafficTime, currentTrafficTime);
            }
        }
    }

    /**
     * 重置本地流量统计信息
     */
    private static void resetTrafficInfo(long oldTrafficTime, long currentTrafficTime) {
        logger.debug("resetTrafficInfo oldTrafficTime::{}, currentTrafficTime::{}",
                DateUtil.format(oldTrafficTime, DateUtil.pattern6),
                DateUtil.format(currentTrafficTime, DateUtil.pattern6));
        settingsRepo.setLongValue(TRAFFIC_TIME, currentTrafficTime);
        settingsRepo.setLongValue(TRAFFIC_VALUE + TRAFFIC_DOWN, 0);
        settingsRepo.setLongValue(TRAFFIC_VALUE + TRAFFIC_UP, 0);
        settingsRepo.setLongValue(TRAFFIC_VALUE + TRAFFIC_METERED, 0);
        settingsRepo.setLongValue(TRAFFIC_VALUE + TRAFFIC_WIFI, 0);
        resetTrafficTotalOld();
        // 同时重置前台运行时间
        NetworkSetting.updateForegroundRunningTime(0);
        NetworkSetting.updateBackgroundRunningTime(0);
        NetworkSetting.updateDozeTime(0);
        Context context = MainApplication.getInstance();
        TauDaemon.getInstance(context).resetDozeStartTime();
        settingsRepo.setTauDozeTime(0);
        NetworkSetting.updateDataAvailableRate(100);
        NetworkSetting.clearWifiPromptLimit();
        NetworkSetting.clearMeteredPromptLimit();

        logger.debug("resetTrafficInfo TRAFFIC_DOWN::{}, TRAFFIC_UP::{}, TRAFFIC_METERED::{}, " +
                        "TRAFFIC_WIFI::{}, TRAFFIC_DOWN_OLD::{}, TRAFFIC_UP_OLD::{}",
                settingsRepo.getLongValue(TRAFFIC_VALUE + TRAFFIC_DOWN),
                settingsRepo.getLongValue(TRAFFIC_VALUE + TRAFFIC_UP),
                settingsRepo.getLongValue(TRAFFIC_VALUE + TRAFFIC_METERED),
                settingsRepo.getLongValue(TRAFFIC_VALUE + TRAFFIC_WIFI),
                settingsRepo.getLongValue(TRAFFIC_VALUE_OLD + TRAFFIC_DOWN),
                settingsRepo.getLongValue(TRAFFIC_VALUE_OLD + TRAFFIC_UP));
    }

    /**
     * 获取当天计费网络流量值
     */
    static long getMeteredTrafficTotal() {
        resetTrafficInfo();
        return settingsRepo.getLongValue(TRAFFIC_VALUE + TRAFFIC_METERED);
    }

    /**
     * 获取当天非计费网络流量值
     */
    static long getWifiTrafficTotal() {
        resetTrafficInfo();
        return settingsRepo.getLongValue(TRAFFIC_VALUE + TRAFFIC_WIFI);
    }

    public static String getUpKey() {
        return TRAFFIC_VALUE + TRAFFIC_UP;
    }

    public static String getDownKey() {
        return TRAFFIC_VALUE + TRAFFIC_DOWN;
    }

    /**
     * 获取当天下行网络流量值
     */
    public static long getTrafficDownloadTotal() {
        resetTrafficInfo();
        return settingsRepo.getLongValue(TRAFFIC_VALUE + TRAFFIC_DOWN);
    }

    /**
     * 获取当天上行网络流量值
     */
    public static long getTrafficUploadTotal() {
        resetTrafficInfo();
        return settingsRepo.getLongValue(TRAFFIC_VALUE + TRAFFIC_UP);
    }
}
