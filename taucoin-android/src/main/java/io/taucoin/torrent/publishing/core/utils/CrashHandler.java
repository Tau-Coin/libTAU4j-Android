package io.taucoin.torrent.publishing.core.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UncaughtException处理类,当程序发生Uncaught异常的时候,有该类来接管程序,并记录发送错误报告.
 *
 */
public class CrashHandler implements UncaughtExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger("CrashHandler");

    // 系统默认的UncaughtException处理类
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    // CrashHandler实例
    private static CrashHandler INSTANCE = new CrashHandler();
    // 程序的Context对象
    private Context mContext;
    // 用来存储设备信息和异常信息
    private Map<String, String> infos = new HashMap<>();

    /** 保证只有一个CrashHandler实例 */
    private CrashHandler() {
    }

    /** 获取CrashHandler实例 ,单例模式 */
    public static CrashHandler getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化
     * @param context 上下文
     */
    public void init(Context context) {
        mContext = context;
        // 获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        String threadName = null == thread ? "" : thread.getName();
        logger.error("uncaughtException threadName::{}", threadName);
        if (ex != null) {
            handleException(ex);
            logger.error("uncaughtException handleException");
        } else {
            logger.error("uncaughtException ex null");
        }
        if (mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
            logger.error("uncaughtException defaultHandler");
        } else {
            logger.error("uncaughtException defaultHandler ex null");
            // 重启APP
//            Context appContext = MainApplication.getInstance();
//            Intent intent = appContext.getPackageManager().getLaunchIntentForPackage(appContext.getPackageName());
//            if (intent != null) {
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
//                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                appContext.startActivity(intent);
//                logger.error("uncaughtException restart app");
//            }
            // 退出程序
            int pid = android.os.Process.myPid();
            logger.error("uncaughtException killProcess pid::{}", pid);
            android.os.Process.killProcess(pid);
            System.exit(0);
        }
    }

    /**
     * 自定义错误处理, 收集错误信息保存到日志文件，单独保存一个dump文件
     * @param ex 异常信息
     */
    private void handleException(Throwable ex) {
        try {
            if (null == ex) {
                return;
            }
            logger.error("handleException start");
            //收集设备参数信息
            logger.error("handleException collect device info");
            collectDeviceInfo(mContext);
            //保存日志文件
            logger.error("handleException save crash info");
            String crashInfo = saveCrashInfo2LogFile(ex);
            logger.error("handleException crate dump file");
            createCrashDumpFile(crashInfo);
            logger.error("handleException end");
        } catch (Exception e) {
            logger.error("handleException ", e);
        }
    }

    /**
     * 收集设备参数信息
     * @param ctx
     */
    private void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            logger.error("an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
            } catch (Exception e) {
                logger.error("an error occured when collect crash info", e);
            }
        }
    }

    /**
     * 保存错误信息到日志文件
     * @param ex 异常
     */
    private String saveCrashInfo2LogFile(Throwable ex) {
        StringBuffer sb = new StringBuffer();
        sb.append("------------------start----------------------");
        sb.append("\t\n");
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key).append("=").append(value).append("\t\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        sb.append("\t\n");
        sb.append("-------------------end-----------------------");
        sb.append("\t\n");
        String crashInfo = sb.toString();
        logger.error("Crash::{}", crashInfo);
        return crashInfo;
    }

    /**
     * 创建崩溃dump文件
     */
    private void createCrashDumpFile(String info) {
        FileWriter fWriter = null;
        try {
            String path = FileUtil.getDumpfileDir();
            path += File.separator + FileUtil.getDumpFileName();
            File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            fWriter = new FileWriter(path);
            fWriter.write(info);
        } catch (IOException ex) {
            logger.error("createCrashDumpFile", ex);
            ex.printStackTrace();
        } finally {
            try {
                if (fWriter != null) {
                    fWriter.flush();
                    fWriter.close();
                }
            } catch (IOException ignore) {
            }
        }
    }
}
