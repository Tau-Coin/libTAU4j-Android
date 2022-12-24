package io.taucoin.news.publishing.core.log;

import android.content.Context;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import ch.qos.logback.classic.Logger;
import io.taucoin.news.publishing.BuildConfig;
import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.core.model.TauDaemon;

public class LogUtil {
    // 是否强制升级等级（在发生Crash时会升级日志等级）
    private static boolean isForceUpgradeLevel = false;

    public static Level getUILogLevel() {
        if (BuildConfig.DEBUG || isForceUpgradeLevel) {
            return Level.DEBUG;
        } else {
            return Level.toLevel(BuildConfig.UI_RELEASE_LOG_LEVEL);
        }
    }

    public static int getTauLogLevel() {
        if (BuildConfig.DEBUG || isForceUpgradeLevel) {
            return TauLogLevel.DEBUG.getLevel();
        } else {
            return BuildConfig.TAU_RELEASE_LOG_LEVEL;
        }
    }

    public static void increaseLogLevel() {
        if (BuildConfig.DEBUG) {
            return;
        }
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (root != null) {
            root.setLevel(Level.DEBUG);
        }
        // 防止libTAU还未启动，在启动时可以直接使用新的日志等级
        isForceUpgradeLevel = true;
        // 防止libTAU已启动成功后错过设置
        Context context = MainApplication.getInstance();
        TauDaemon.getInstance(context).setLogLevel(TauLogLevel.DEBUG.getLevel());
    }
}
