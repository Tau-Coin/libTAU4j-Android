package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;

/**
 * 网络流量设置相关工具类
 */
public class FrequencyUtil {
    private static final Logger logger = LoggerFactory.getLogger("FrequencyUtil");
    private static final int internal_sample = 50;            // 主循环采样大小，单位s

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
        average = (average * internal_sample + interval) / (internal_sample + 1);
        settingsRepo.setIntValue(context.getString(R.string.pref_key_main_loop_average_interval), average);
        settingsRepo.setIntValue(context.getString(R.string.pref_key_main_loop_interval), interval);
    }

    /**
     * 清除主循环时间间隔采样值列表
     */
    public static void clearMainLoopIntervalList() {
        Context context = MainApplication.getInstance();
        settingsRepo.setIntValue(context.getString(R.string.pref_key_main_loop_average_interval), 0);
    }

    /**
     * 获取当前主循环时间间隔
     */
    public static int getMainLoopInterval() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getIntValue(context.getString(R.string.pref_key_main_loop_interval), 0);
    }

    /**
     * 获取当前主循环频率
     */
    public static double getMainLoopFrequency() {
        long interval = getMainLoopInterval();
        logger.trace("getMainLoopFrequency:: interval::{}", interval);
        if (interval > 0) {
            return  1.0 * 1000 / interval;
        }
        return 0;
    }
}