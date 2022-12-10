package io.taucoin.tauapp.publishing.core.utils;

import android.content.Context;
import android.os.SystemClock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.tauapp.publishing.core.storage.RepositoryHelper;

/**
 * 网络流量设置相关工具类
 * 前后流量用户设置，后台流量受流量包限制
 */
public class NetworkSetting {
    private static final Logger logger = LoggerFactory.getLogger("NetworkSetting");

    private static final SettingsRepository settingsRepo;
    private static long lastElapsedRealTime = 0;
    private static long lastUptime = 0;
    private static boolean isDevelopCountry = false;
    private static final MutableLiveData<Boolean> developCountry = new MutableLiveData<>(false);
    static {
        Context context = MainApplication.getInstance();
        settingsRepo = RepositoryHelper.getSettingsRepository(context);
    }

    /**
     * 设置当前是否为WiFi网络(存在计费和非计费的情况)
     */
    public static void setWiFiNetwork(boolean isWiFi) {
        Context context = MainApplication.getInstance();
        settingsRepo.setBooleanValue(context.getString(R.string.pref_key_is_wifi_network), isWiFi);
    }

    /**
     * 返回当前是否为WiFi网络
     */
    public static boolean isWiFiNetwork() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getBooleanValue(context.getString(R.string.pref_key_is_wifi_network), false);
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
        logger.debug("updateSpeed, CurrentSpeed::{}/s",
                Formatter.formatFileSize(context, currentSpeed).toUpperCase());
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

    public static boolean isDevelopCountry() {
        return isDevelopCountry;
    }

    /**
     * 返回是否限制网络（在发达国家，WiFi网络，并且不是计费网络不限制网络）
     */
    public static boolean isUnlimitedNetwork() {
//        return isDevelopCountry() && isWiFiNetwork() && !isMeteredNetwork();
        return isWiFiNetwork() && !isMeteredNetwork();
    }

    public static void updateDevelopCountry() {
        boolean isNewDevelopCountry = Utils.isDevelopedCountry();
        if (isDevelopCountry != isNewDevelopCountry) {
            isDevelopCountry = isNewDevelopCountry;
            developCountry.postValue(isDevelopCountry);
        }
    }
}