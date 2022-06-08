package io.taucoin.torrent.publishing.core.log;

import android.content.Context;

import org.slf4j.LoggerFactory;

import java.io.File;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import io.taucoin.torrent.publishing.BuildConfig;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.utils.FileUtil;
import io.taucoin.torrent.publishing.core.utils.LogbackSizeBasedTriggeringPolicy;

/**
 * 日志配置器
 * 是否有sdcard，日志存储路径不同
 * 是否是测试版本，日志存储的大小不同，等级不同
 */
public class LogConfigurator {
    private static final int DEBUG_MAX_INDEX = 5;                   // 测试版本最大编号
    private static final String DEBUG_MAX_FILE_SIZE = "50MB";       // 测试最大文件大小
    private static final Level DEBUG_LOG_LEVEL = Level.TRACE;       // 测试日志等级

    private static final int RELEASE_MAX_INDEX = 5;                 // 发布版本最大编号
    private static final String RELEASE_MAX_FILE_SIZE = "50MB";     // 发布版最大文件大小
    private static final Level RELEASE_LOG_LEVEL = Level.WARN;      // 发布版日志等级

    public static void configure() {
        Context context = MainApplication.getInstance();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(context);
        boolean logEnable = settingsRepo.getBooleanValue(context.getString(R.string.pref_key_log_enable), false);
        String logDir = getLogDir();
        configureLogbackDirectly(logDir, logEnable);
    }

    public static String getLogDir() {
        String logDir = FileUtil.getExternalDir();
        String prefix = "logs";
        logDir += File.separator + prefix;
        return logDir;
    }

    private static void configureLogbackDirectly(String log_dir, boolean logEnable) {
        // reset the default context (which may already have been initialized)
        // since we want to reconfigure it
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<>();
        rollingFileAppender.setAppend(true);
        rollingFileAppender.setContext(context);
        rollingFileAppender.setFile(log_dir + File.separator + "tau.latest.log");

        FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
        rollingPolicy.setFileNamePattern(log_dir + File.separator + "tau.%i.log.zip");
        rollingPolicy.setMinIndex(1);
        if (BuildConfig.DEBUG) {
            rollingPolicy.setMaxIndex(DEBUG_MAX_INDEX);
        } else {
            rollingPolicy.setMaxIndex(RELEASE_MAX_INDEX);
        }
        rollingPolicy.setParent(rollingFileAppender);
        rollingPolicy.setContext(context);
        rollingPolicy.start();

        LogbackSizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new LogbackSizeBasedTriggeringPolicy<>();
        if (BuildConfig.DEBUG) {
            triggeringPolicy.setMaxFileSize(DEBUG_MAX_FILE_SIZE);
        } else {
            triggeringPolicy.setMaxFileSize(RELEASE_MAX_FILE_SIZE);
        }
        triggeringPolicy.setContext(context);
        triggeringPolicy.start();

        rollingFileAppender.setRollingPolicy(rollingPolicy);
        rollingFileAppender.setTriggeringPolicy(triggeringPolicy);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.setContext(context);
        encoder.start();

        rollingFileAppender.setEncoder(encoder);
        rollingFileAppender.start();

        PatternLayoutEncoder logcatEncoder = new PatternLayoutEncoder();
        logcatEncoder.setPattern("%msg");
        logcatEncoder.setContext(context);
        logcatEncoder.start();

        LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(context);
        logcatAppender.setEncoder(logcatEncoder);
        logcatAppender.setName("logcat");
        logcatAppender.start();

        // add the newly created appenders to the root logger;
        // qualify Logger to disambiguate from org.slf4j.Logger
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (BuildConfig.DEBUG) {
            root.setLevel(DEBUG_LOG_LEVEL);
        } else {
            if (logEnable) {
                root.setLevel(Level.toLevel(BuildConfig.ENABLE_RELEASE_LOG_LEVEL));
            } else {
                root.setLevel(RELEASE_LOG_LEVEL);
            }
        }
        root.addAppender(rollingFileAppender);
        root.addAppender(logcatAppender);

        // print any status messages (warnings, etc) encountered in logback config
        //StatusPrinter.print(context);
    }

    public static void setLogLevel(boolean logEnable) {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (logEnable) {
            root.setLevel(Level.toLevel(BuildConfig.ENABLE_RELEASE_LOG_LEVEL));
        } else {
            root.setLevel(RELEASE_LOG_LEVEL);
        }
    }
}
