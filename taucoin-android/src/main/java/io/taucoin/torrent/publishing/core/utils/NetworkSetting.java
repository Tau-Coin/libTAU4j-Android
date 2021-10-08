package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Locale;

import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.Interval;
import io.taucoin.torrent.publishing.core.model.TauInfoProvider;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;

/**
 * 网络流量设置相关工具类
 */
public class NetworkSetting {
//    private static final Logger logger = LoggerFactory.getLogger("NetworkSetting");
    private static final int METERED_LIMITED;                                   // 单位MB
    private static final int WIFI_LIMITED;                                      // 单位MB
    private static final int SURVIVAL_SPEED_LIMIT = 10;                         // 单位B
    private static final float MAX_CPU_LIMIT = 5;                               // 单位%
    public static final int HEAP_SIZE_LIMIT = 50 * 1024 * 1024;                 // 单位为B

    private static SettingsRepository settingsRepo;
    private static long lastStatisticsTime = 0;
    static {
        Context context = MainApplication.getInstance();
        settingsRepo = RepositoryHelper.getSettingsRepository(context);
        METERED_LIMITED = context.getResources().getIntArray(R.array.metered_limit)[1];
        WIFI_LIMITED = context.getResources().getIntArray(R.array.wifi_limit)[1];
    }

    /**
     * 获取计费网络流量限制值
     * @return long
     */
    public static int getMeteredLimit() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getIntValue(context.getString(R.string.pref_key_metered_limit), METERED_LIMITED);
    }

    /**
     * 设置计费网络流量限制值
     * @param limited
     */
    public static void setMeteredLimit(int limited) {
        Context context = MainApplication.getInstance();
        settingsRepo.setIntValue(context.getString(R.string.pref_key_metered_limit), limited);
    }

    /**
     * 获取WiFi网络流量限制值
     * @return long
     */
    public static int getWiFiLimit() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getIntValue(context.getString(R.string.pref_key_wifi_limit), WIFI_LIMITED);
    }

    /**
     * 设置WiFi网络流量限制值
     * @param limited
     */
    public static void setWiFiLimit(int limited) {
        Context context = MainApplication.getInstance();
        settingsRepo.setIntValue(context.getString(R.string.pref_key_wifi_limit), limited);
    }

    /**
     * 设置当前是否为计费网络
     */
    public static void setMeteredNetwork(boolean isMetered) {
        Context context = MainApplication.getInstance();
        settingsRepo.setBooleanValue(context.getString(R.string.pref_key_is_metered_network), isMetered);
    }

    /**
     * 返回当前是否为计费网络
     */
    public static boolean isMeteredNetwork() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getBooleanValue(context.getString(R.string.pref_key_is_metered_network),
                true);
    }

    /**
     * 更新网络速度
     */
    public static void updateNetworkSpeed(@NonNull SessionStatistics statistics) {
        Context context = MainApplication.getInstance();
        long currentSpeed = statistics.getDownloadRate() + statistics.getUploadRate();
        settingsRepo.setLongValue(context.getString(R.string.pref_key_current_speed), currentSpeed);

        // 更新Mode运行时间
        updateRunningTime();
        updateMeteredSpeedLimit();
        updateWiFiSpeedLimit();
//        logger.trace("updateSpeed, CurrentSpeed::{}/s",
//                Formatter.formatFileSize(context, currentSpeed).toUpperCase());
    }

    /**
     * APP是否在前台运行
     */
    private static boolean isForegroundRunning() {
        Context appContext = MainApplication.getInstance();
        String foregroundRunningKey = appContext.getString(R.string.pref_key_foreground_running);
        return settingsRepo.getBooleanValue(foregroundRunningKey);
    }

    /**
     * 更新运行时间
     */
    private static void updateRunningTime() {
        long currentTime = DateUtil.getMillisTime();
        if (isForegroundRunning()) {
            int foregroundRunningTime = getForegroundRunningTime() + 1;
            updateForegroundRunningTime(foregroundRunningTime);
        } else {
            int backgroundRunningTime = getBackgroundRunningTime() + 1;
            updateBackgroundRunningTime(backgroundRunningTime);

            // 去除流量统计时间间隔
            int dozeTime = (int) ((currentTime - lastStatisticsTime - TauInfoProvider.STATISTICS_PERIOD) / 1000);
            if (lastStatisticsTime > 0 && dozeTime > 0 && !isForegroundRunning()) {
                dozeTime += getDozeTime();
                updateDozeTime(dozeTime);
            }
        }
        lastStatisticsTime = currentTime;
    }

    /**
     * 获取APP前台运行时间
     */
    public static int getForegroundRunningTime() {
        Context appContext = MainApplication.getInstance();
        String foregroundRunningTimeKey = appContext.getString(R.string.pref_key_foreground_running_time);
        return settingsRepo.getIntValue(foregroundRunningTimeKey, 0);
    }

    /**
     * 更新APP前台运行时间
     */
    static void updateForegroundRunningTime(int foregroundRunningTime) {
        Context appContext = MainApplication.getInstance();
        String foregroundRunningTimeKey = appContext.getString(R.string.pref_key_foreground_running_time);
        settingsRepo.setIntValue(foregroundRunningTimeKey, foregroundRunningTime);
    }

    /**
     * 获取APP后台运行时间
     */
    public static int getBackgroundRunningTime() {
        Context appContext = MainApplication.getInstance();
        String backgroundRunningTimeKey = appContext.getString(R.string.pref_key_background_running_time);
        return settingsRepo.getIntValue(backgroundRunningTimeKey, 0);
    }

    /**
     * 更新APP后台运行时间
     */
    public static void updateBackgroundRunningTime(int backgroundRunningTime) {
        Context appContext = MainApplication.getInstance();
        String backgroundRunningTimeKey = appContext.getString(R.string.pref_key_background_running_time);
        settingsRepo.setIntValue(backgroundRunningTimeKey, backgroundRunningTime);
    }

    /**
     * 获取APP后台Doze时间
     */
    public static int getDozeTime() {
        Context appContext = MainApplication.getInstance();
        String dozeRunningTimeKey = appContext.getString(R.string.pref_key_doze_running_time);
        return settingsRepo.getIntValue(dozeRunningTimeKey, 0);
    }

    /**
     * 更新APP后台Doze时间
     */
    public static void updateDozeTime(int dozeTime) {
        Context appContext = MainApplication.getInstance();
        String dozeRunningTimeKey = appContext.getString(R.string.pref_key_doze_running_time);
        settingsRepo.setIntValue(dozeRunningTimeKey, dozeTime);
    }

    /**
     * 获取当前网络网速
     */
    public static long getCurrentSpeed() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_current_speed),
                0);
    }

    /**
     * 更新计费网络网速限制值
     */
    public static void updateMeteredSpeedLimit() {
        Context context = MainApplication.getInstance();
        long usage = TrafficUtil.getMeteredTrafficTotal();
        long limit =  getMeteredLimit();
        long averageSpeed = 0;
        long availableData = 0;

        BigInteger bigUnit = new BigInteger("1024");
        BigInteger bigLimit = BigInteger.valueOf(limit).multiply(bigUnit).multiply(bigUnit);
        BigInteger bigUsage = BigInteger.valueOf(usage);

        // 今天剩余的秒数
        long today24HLastSeconds = DateUtil.getTomorrowLastSeconds(TrafficUtil.TRAFFIC_UPDATE_TIME);;

        if (bigLimit.compareTo(bigUsage) > 0) {
            availableData = bigLimit.subtract(bigUsage).longValue();
            if (today24HLastSeconds > 0) {
                averageSpeed = availableData / today24HLastSeconds;
            }
        }
        settingsRepo.setLongValue(context.getString(R.string.pref_key_metered_available_data), availableData);
        settingsRepo.setLongValue(context.getString(R.string.pref_key_metered_average_speed), averageSpeed);
    }


    /**
     * 更新WiFi网络网速限制值
     */
    public static void updateWiFiSpeedLimit() {
        Context context = MainApplication.getInstance();
        long total = TrafficUtil.getTrafficUploadTotal() + TrafficUtil.getTrafficDownloadTotal();
        long usage = total - TrafficUtil.getMeteredTrafficTotal();
//        logger.trace("updateWiFiSpeedLimit total::{}, MeteredTotal::{}, wifiUsage::{}", total,
//                TrafficUtil.getMeteredTrafficTotal(), usage);
        long limit = getWiFiLimit();
        long averageSpeed = 0;
        long availableData = 0;

        BigInteger bigUnit = new BigInteger("1024");
        BigInteger bigLimit = BigInteger.valueOf(limit).multiply(bigUnit).multiply(bigUnit);
        BigInteger bigUsage = BigInteger.valueOf(usage);
//        logger.trace("updateWiFiSpeedLimit bigLimit::{}, bigUsage::{}, compareTo::{}",
//                bigLimit.longValue(),
//                bigUsage.longValue(),
//                bigLimit.compareTo(bigUsage));

        // 今天剩余的秒数
        long today24HLastSeconds = DateUtil.getTomorrowLastSeconds(TrafficUtil.TRAFFIC_UPDATE_TIME);

        if (bigLimit.compareTo(bigUsage) > 0) {
            availableData = bigLimit.subtract(bigUsage).longValue();
            if (today24HLastSeconds > 0) {
                averageSpeed = availableData / today24HLastSeconds;
            }
        }
        settingsRepo.setLongValue(context.getString(R.string.pref_key_wifi_available_data), availableData);
        settingsRepo.setLongValue(context.getString(R.string.pref_key_wifi_average_speed), averageSpeed);
    }

    /**
     * 获取计费网络可用数据
     */
    public static long getMeteredAvailableData() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_metered_available_data));
    }

    /**
     * 获取计费网络平均网速
     */
    public static long getMeteredAverageSpeed() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_metered_average_speed));
    }

    /**
     * 获取WiFi网络平均网速
     */
    public static long getWifiAverageSpeed() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_wifi_average_speed));
    }

    /**
     * 获取WiFi网络可用数据
     */
    public static long getWiFiAvailableData() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_wifi_available_data));
    }

    /**
     * 获取DHT Sessions数
     */
    public static int getDHTSessions() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getIntValue(context.getString(R.string.pref_key_sessions),
                0);
    }

    /**
     * 更新DHT Sessions数
     */
    public static void updateDHTSessions(int sessions) {
        Context context = MainApplication.getInstance();
        settingsRepo.setIntValue(context.getString(R.string.pref_key_sessions),
                sessions);
    }

    /**
     * 设置是否启动后台数据模式
     */
    public static void enableBackgroundMode(boolean enable) {
        Context context = MainApplication.getInstance();
        settingsRepo.setBooleanValue(context.getString(R.string.pref_key_bg_data_mode), enable);
    }

    /**
     * 获取是否启动后台数据模式
     */
    public static boolean isEnableBackgroundMode() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getBooleanValue(context.getString(R.string.pref_key_bg_data_mode),
                false);
    }

    /**
     * 当前网络是否还有剩余可用流量
     */
    public static boolean isHaveAvailableData() {
        boolean isHaveAvailableData;
        if (NetworkSetting.isMeteredNetwork()) {
            isHaveAvailableData = getMeteredAvailableData() > 0;
        } else {
            isHaveAvailableData = getWiFiAvailableData() > 0;
        }
        return isHaveAvailableData;
    }

    /**
     * 计算主循环时间间隔
     * @return 返回计算的时间间隔
     */
    public static void calculateMainLoopInterval() {
        Interval min = Interval.MAIN_LOOP_MIN;
        Interval max = Interval.MAIN_LOOP_MAX;
        int timeInterval = 0;
        if (!isHaveAvailableData()) {
            timeInterval = max.getInterval();
//            logger.trace("calculateMainLoopInterval timeInterval::{}, isHaveAvailableData::false",
//                    timeInterval);

        } else if (getCurrentSpeed() < SURVIVAL_SPEED_LIMIT) {
            timeInterval = (min.getInterval() + max.getInterval()) / 2;
//            logger.trace("calculateMainLoopInterval timeInterval::{}, currentSpeed::{}",
//                    timeInterval, getCurrentSpeed());
            FrequencyUtil.updateMainLoopInterval(timeInterval);
        } else {
            long averageSpeed;
            if (isMeteredNetwork()) {
                // 当前网络为计费网络
                averageSpeed = NetworkSetting.getMeteredAverageSpeed();
            } else {
                // 当前网络为非计费网络
                averageSpeed = NetworkSetting.getWifiAverageSpeed();
            }
            long currentSpeed = NetworkSetting.getCurrentSpeed();
            if (averageSpeed > 0) {
                // 平均网速和网速限制的比率
                double rate = currentSpeed * 1.0f / averageSpeed;
                int lastTimeInterval = FrequencyUtil.getMainLoopAverageInterval();
                timeInterval = Math.max(min.getInterval(), (int)(lastTimeInterval * rate));
                timeInterval = Math.min(timeInterval, max.getInterval());
//                logger.debug("calculateMainLoopInterval currentSpeed::{}, averageSpeed::{}, " +
//                                "rate::{}, timeInterval::{}, lastTimeInterval::{}, mainLoopMax::{}",
//                        currentSpeed, averageSpeed, rate, timeInterval, lastTimeInterval,
//                        max.getInterval());
            }
        }

        // 根据系统资源(cpu, memory)的使用
        float cpuUsage = settingsRepo.getAverageCpuUsage();
        long memoryUsage = settingsRepo.getAverageMemoryUsage();
        long maxMemoryLimit = settingsRepo.getMaxMemoryLimit();
        if (cpuUsage > MAX_CPU_LIMIT || memoryUsage > maxMemoryLimit) {
            // 超出部分大小
            float excessSize = cpuUsage - MAX_CPU_LIMIT;
            float maxLimit = MAX_CPU_LIMIT;
            if (excessSize <= 0) {
                excessSize = memoryUsage - maxMemoryLimit;
                maxLimit = HEAP_SIZE_LIMIT;
            }
            // 计算出来需要增加的大小
            if (excessSize > 0) {
                int increase = (int)(excessSize * (max.getInterval() - min.getInterval()) / maxLimit);
                timeInterval += increase;
                timeInterval = Math.min(max.getInterval(), timeInterval);
            }
//            logger.debug("calculateTimeInterval timeInterval::{}, increase::{}, cpuUsage::{}%," +
//                            " memoryUsage::{}", timeInterval, increase,
//                    String.format(Locale.CHINA, "%.2f", cpuUsage),
//                    Formatter.formatFileSize(MainApplication.getInstance(), memoryUsage));
        }

        if (timeInterval > 0) {
            FrequencyUtil.updateMainLoopInterval(timeInterval);
        }
    }
}