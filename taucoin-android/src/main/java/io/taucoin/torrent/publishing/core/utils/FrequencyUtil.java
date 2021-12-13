package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.Interval;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;

/**
 * 网络流量设置相关工具类
 */
public class FrequencyUtil {
    private static final Logger logger = LoggerFactory.getLogger("FrequencyUtil");
    private static final int internal_sample = 5;            // 主循环采样大小，单位s

    private static SettingsRepository settingsRepo;
    static {
        Context context = MainApplication.getInstance();
        settingsRepo = RepositoryHelper.getSettingsRepository(context);
    }

    /**
     * 获取当前主循环平均时间间隔
     */
    static int getMainLoopAverageInterval() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getIntValue(context.getString(R.string.pref_key_main_loop_average_interval), 0);
    }

    /**
     * 更新主循环时间间隔
     */
    static void updateMainLoopInterval(int interval) {
        Context context = MainApplication.getInstance();
        int average = getMainLoopAverageInterval();
        if (average > 0 ) {
            average = (average * internal_sample + interval) / (internal_sample + 1);
        } else {
            average = interval;
        }

        settingsRepo.setIntValue(context.getString(R.string.pref_key_main_loop_average_interval), average);
        setMainLoopFrequency(convertTimeToFreq(interval));
    }

    /**
     * 获取当前主循环时间间隔
     */
    public static int getMainLoopInterval() {
        float frequency = getMainLoopFrequency();
        return convertFreqToTime(frequency);
    }

    /**
     * 获取当前主循环频率
     */
    public static void setMainLoopFrequency(float frequency) {
        Context context = MainApplication.getInstance();
        settingsRepo.setFloatValue(context.getString(R.string.pref_key_main_loop_frequency), frequency);
        logger.trace("setMainLoopFrequency:: frequency::{}", frequency);
    }

    /**
     * 获取当前主循环频率
     */
    public static float getMainLoopFrequency() {
        Context context = MainApplication.getInstance();
        float frequency = settingsRepo.getFloatValue(context.getString(R.string.pref_key_main_loop_frequency), 0);
        logger.trace("getMainLoopFrequency:: frequency::{}", frequency);
        return frequency;
    }

    /**
     * 转化时间间隔为频率
     */
    private static float convertTimeToFreq(long interval) {
        if (interval > 0) {
            return (float) (1.0 * 1000 / interval);
        }
        return 0;
    }

    /**
     * 转化时间间隔为频率
     */
    private static int convertFreqToTime(float freq) {
        if (freq > 0) {
            return (int)(1.0 * 1000 / freq);
        }
        return 0;
    }

    /**
     * 获取前台Wifi网络固定主循环频率
     */
    public static int getWifiFixedFrequency() {
        Context context = MainApplication.getInstance();
        int frequency = settingsRepo.getIntValue(context.getString(R.string.pref_key_wifi_fixed_frequency),
                Interval.FORE_DEFAULT_WIFI_FREQUENCY.getInterval());
        logger.trace("getWifiFixedFrequency:: frequency::{}", frequency);
        return frequency;
    }

    /**
     * 设置前台Wifi网络固定主循环频率
     */
    public static void setWifiFixedFrequency(int freq) {
        Context context = MainApplication.getInstance();
        settingsRepo.setIntValue(context.getString(R.string.pref_key_wifi_fixed_frequency), freq);
        logger.trace("setWifiFixedFrequency:: freq::{}", freq);
        if (!NetworkSetting.isMeteredNetwork()) {
            setMainLoopFrequency(freq);
        }
    }

    /**
     * 获取前台Metered网络固定主循环频率
     */
    public static int getMeteredFixedFrequency() {
        Context context = MainApplication.getInstance();
        int frequency = settingsRepo.getIntValue(context.getString(R.string.pref_key_metered_fixed_frequency),
                Interval.FORE_DEFAULT_METERED_FREQUENCY.getInterval());
        logger.trace("getMeteredFixedFrequency:: frequency::{}", frequency);
        return frequency;
    }

    /**
     * 设置前台Metered网络固定主循环频率
     */
    public static void setMeteredFixedFrequency(int freq) {
        Context context = MainApplication.getInstance();
        settingsRepo.setIntValue(context.getString(R.string.pref_key_metered_fixed_frequency), freq);
        logger.trace("setMeteredFixedInterval:: freq::{}", freq);
        if (NetworkSetting.isMeteredNetwork()) {
            setMainLoopFrequency(freq);
        }
    }
}