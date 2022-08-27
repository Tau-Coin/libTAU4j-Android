package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;
import android.os.SystemClock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.DozeEvent;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;

/**
 * 网络流量设置相关工具类
 * 前后流量用户设置，后台流量受流量包限制
 */
public class NetworkSetting {
    private static final Logger logger = LoggerFactory.getLogger("NetworkSetting");
    private static final int[] METERED_LIMITED;                                   // 单位MB
    private static final int[] DEVELOPED_METERED_LIMITED;                         // 单位MB
    private static final int[] WIFI_LIMITED;                                      // 单位MB

    private static final SettingsRepository settingsRepo;
    private static long lastElapsedRealTime = 0;
    private static long lastUptime = 0;
    private static boolean isDevelopCountry = false;
    private static final MutableLiveData<Boolean> developCountry = new MutableLiveData<>(false);
    static {
        Context context = MainApplication.getInstance();
        settingsRepo = RepositoryHelper.getSettingsRepository(context);
        METERED_LIMITED = context.getResources().getIntArray(R.array.metered_limit);
        DEVELOPED_METERED_LIMITED = context.getResources().getIntArray(R.array.developed_metered_limit);
        WIFI_LIMITED = context.getResources().getIntArray(R.array.wifi_limit);
        isDevelopCountry = Utils.isDevelopedCountry();
        developCountry.postValue(isDevelopCountry);
    }



    /**
     * 获取计费网络流量限制值
     * 先优先使用无流量时，提升的流量包，重置流量时恢复到用户收到设置的流量包
     * @return long
     */
    public static int getMeteredLimitPos() {
        Context context = MainApplication.getInstance();
        int pos = settingsRepo.getIntValue(context.getString(R.string.pref_key_metered_prompt_limit), -1);
        if (pos < 0) {
            pos = settingsRepo.getIntValue(context.getString(R.string.pref_key_metered_limit), 1);
        }
        return pos;
    }

    public static int getMeteredLimitValue() {
        int pos = getMeteredLimitPos();
        if (isDevelopCountry()) {
            if (pos >= DEVELOPED_METERED_LIMITED.length) {
                pos = DEVELOPED_METERED_LIMITED.length - 1;
            }
            return DEVELOPED_METERED_LIMITED[pos];
        } else {
            if (pos >= METERED_LIMITED.length) {
                pos = METERED_LIMITED.length - 1;
            }
            return METERED_LIMITED[pos];
        }
    }

    public static MutableLiveData<Boolean> getDevelopCountry() {
        return developCountry;
    }

    public static boolean isDevelopCountry() {
        return isDevelopCountry(false);
    }

    public static boolean isDevelopCountry(boolean isNeedUpdate) {
        if (isNeedUpdate) {
            boolean isNewDevelopCountry = Utils.isDevelopedCountry();
            if (isDevelopCountry != isNewDevelopCountry) {
                isDevelopCountry = isNewDevelopCountry;
                developCountry.postValue(isDevelopCountry);
            }
        }
        return isDevelopCountry;
    }

    public static int[] getMeteredLimits() {
        if (isDevelopCountry()) {
            return DEVELOPED_METERED_LIMITED;
        } else {
            return METERED_LIMITED;
        }
    }

    /**
     * 设置计费网络流量限制值
     * @param pos
     */
    public static void setMeteredLimitPos(int pos, boolean isClearPrompt) {
        Context context = MainApplication.getInstance();
        if (isClearPrompt) {
            clearMeteredPromptLimit();
            settingsRepo.setIntValue(context.getString(R.string.pref_key_metered_limit), pos);
        } else {
            settingsRepo.setIntValue(context.getString(R.string.pref_key_metered_prompt_limit), pos);
        }
    }

    /**
     * 清除计费提升的流量包
     */
    static void clearMeteredPromptLimit() {
        Context context = MainApplication.getInstance();
        settingsRepo.setIntValue(context.getString(R.string.pref_key_metered_prompt_limit), -1);
    }

    /**
     * 清除Wifi提升的流量包
     */
    static void clearWifiPromptLimit() {
        Context context = MainApplication.getInstance();
        settingsRepo.setIntValue(context.getString(R.string.pref_key_wifi_prompt_limit), -1);
    }

    /**
     * 获取WiFi网络流量限制值
     * 先优先使用无流量时，提升的流量包，重置流量时恢复到用户收到设置的流量包
     * @return long
     */
    public static int getWiFiLimitPos() {
        Context context = MainApplication.getInstance();
        int pos = settingsRepo.getIntValue(context.getString(R.string.pref_key_wifi_prompt_limit), -1);
        if (pos < 0) {
            pos = settingsRepo.getIntValue(context.getString(R.string.pref_key_wifi_limit), 1);
        }
        return pos;
    }

    public static int getWiFiLimitValue() {
        int pos = getWiFiLimitPos();
        if (pos >= WIFI_LIMITED.length) {
            pos = WIFI_LIMITED.length - 1;
        }
        return WIFI_LIMITED[pos];
    }

    public static int[] getWifiLimits() {
        return WIFI_LIMITED;
    }

    /**
     * 设置WiFi网络流量限制值
     * @param pos
     */
    public static void setWiFiLimitPos(int pos, boolean isClearPrompt) {
        Context context = MainApplication.getInstance();
        if (isClearPrompt) {
            clearWifiPromptLimit();
            settingsRepo.setIntValue(context.getString(R.string.pref_key_wifi_limit), pos);
        } else {
            settingsRepo.setIntValue(context.getString(R.string.pref_key_wifi_prompt_limit), pos);
        }
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
//        logger.debug("updateSpeed, CurrentSpeed::{}/s",
//                Formatter.formatFileSize(context, currentSpeed).toUpperCase());
    }

    /**
     * APP是否在前台运行
     */
    public static boolean isForegroundRunning() {
        Context appContext = MainApplication.getInstance();
        String foregroundRunningKey = appContext.getString(R.string.pref_key_foreground_running);
        return settingsRepo.getBooleanValue(foregroundRunningKey, true);
    }

    /**
     * 更新运行时间
     */
    private static void updateRunningTime() {
        long currentElapsedRealTime = SystemClock.elapsedRealtime();
        long currentUptime = SystemClock.uptimeMillis();
        if (lastElapsedRealTime > 0 && lastUptime > 0) {
            int realSeconds = (int)(currentElapsedRealTime - lastElapsedRealTime) / 1000;
            if (realSeconds > 0) {
                if (isForegroundRunning()) {
                    int foregroundRunningTime = getForegroundRunningTime() + realSeconds;
                    logger.debug("updateRunningTime foregroundRunningTime::{}s", foregroundRunningTime);
                    updateForegroundRunningTime(foregroundRunningTime);
                } else {
                    int backgroundRunningTime = getBackgroundRunningTime() + realSeconds;
                    logger.debug("updateRunningTime backgroundRunningTime::{}s", backgroundRunningTime);
                    updateBackgroundRunningTime(backgroundRunningTime);
                }
            }
            // 计算Doze Time
            int upSeconds = (int)(currentUptime - lastUptime) / 1000;
            int dozeSeconds = realSeconds - upSeconds;
            if (dozeSeconds > 0) {
                int dozeTime = getDozeTime() + dozeSeconds;
                updateDozeTime(dozeTime);
                logger.debug("updateRunningTime dozeTime::{}s", dozeSeconds);
                TauDaemon.getInstance(MainApplication.getInstance()).newActionEvent(DozeEvent.SYS_DOZE_END);
            }
        }
        lastElapsedRealTime = currentElapsedRealTime;
        lastUptime = currentUptime;
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
        long limit =  getMeteredLimitValue();
        BigInteger availableData = BigInteger.ZERO;

        BigInteger bigUnit = new BigInteger("1024");
        BigInteger bigLimit = BigInteger.valueOf(limit).multiply(bigUnit).multiply(bigUnit);
        BigInteger bigUsage = BigInteger.valueOf(usage);

        logger.debug("updateSpeedLimit meteredLimit::{}, meteredUsage::{}, compareTo::{}",
                bigLimit.longValue(), bigUsage, bigLimit.compareTo(bigUsage));

        if (bigLimit.compareTo(bigUsage) > 0) {
            availableData = bigLimit.subtract(bigUsage);
        }
        int rate = 100;
        if (isMeteredNetwork()) {
            if (bigLimit.compareTo(BigInteger.ZERO) > 0) {
                rate = availableData.multiply(Constants.PERCENTAGE).divide(bigLimit).intValue();
            }
            updateDataAvailableRate(rate);
        }
        settingsRepo.setLongValue(context.getString(R.string.pref_key_metered_available_data), availableData.longValue());
        logger.debug("updateSpeedLimit meteredLimit::{}, meteredUsage::{}, availableData::{}, rate::{}",
                bigLimit.longValue(), bigUsage, availableData.longValue(), rate);
    }

    /**
     * 更新流量剩余可用率
     */
    public static void updateDataAvailableRate(int rate) {
        Context context = MainApplication.getInstance();
        TauDaemon.getInstance(context).setDataAvailableRate(rate);
    }

    /**
     * 更新WiFi网络网速限制值
     */
    public static void updateWiFiSpeedLimit() {
        Context context = MainApplication.getInstance();
        long usage = TrafficUtil.getWifiTrafficTotal();
        long limit = getWiFiLimitValue();
        BigInteger availableData = BigInteger.ZERO;

        BigInteger bigUnit = new BigInteger("1024");
        BigInteger bigLimit = BigInteger.valueOf(limit).multiply(bigUnit).multiply(bigUnit);
        BigInteger bigUsage = BigInteger.valueOf(usage);

        if (bigLimit.compareTo(bigUsage) > 0) {
            availableData = bigLimit.subtract(bigUsage);
        }
        int rate = 100;
        if (!isMeteredNetwork()) {
            if (bigLimit.compareTo(BigInteger.ZERO) > 0) {
                rate = availableData.multiply(Constants.PERCENTAGE).divide(bigLimit).intValue();
            }
            updateDataAvailableRate(rate);
        }
        settingsRepo.setLongValue(context.getString(R.string.pref_key_wifi_available_data), availableData.longValue());

        logger.debug("updateSpeedLimit wifiLimit::{}, wifiUsage::{}, availableData::{}, rate::{}",
                bigLimit.longValue(), bigUsage, availableData.longValue(), rate);
    }

    /**
     * 获取计费网络可用数据
     */
    public static long getMeteredAvailableData() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_metered_available_data));
    }

    /**
     * 获取WiFi网络可用数据
     */
    public static long getWiFiAvailableData() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_wifi_available_data));
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
}