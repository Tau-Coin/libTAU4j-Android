package io.taucoin.torrent.publishing.core.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageStats;
import android.os.Debug;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.RandomAccessFile;
import java.util.Date;

import androidx.annotation.NonNull;
import io.reactivex.FlowableEmitter;
import io.taucoin.torrent.publishing.MainApplication;

public class Sampler {
    private static final Logger logger = LoggerFactory.getLogger("Sampler");
    private volatile static Sampler instance = null;
    private ActivityManager activityManager;
    private Long lastCpuTime;
    private Long lastAppCpuTime;
    private RandomAccessFile procStatFile;
    private RandomAccessFile appStatFile;

    public static Sampler getInstance() {
        if (instance == null) {
            synchronized (Sampler.class) {
                if (instance == null) {
                    instance = new Sampler();
                }
            }
        }
        return instance;
    }

    private Sampler () {
        Context context = MainApplication.getInstance();
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public boolean isAccessibleFromFile() {
        try {
            RandomAccessFile procStatFile = new RandomAccessFile("/proc/stat", "r");
            return procStatFile.readBoolean();
        } catch (Exception ignore) {
        }
        return false;
    }

    public float sampleCPU() {
        long cpuTime = 0;
        long appTime = 0;
        float sampleValue = 0.0f;
        try {
            if (appStatFile == null) {
                try {
                    procStatFile = new RandomAccessFile("/proc/stat", "r");
                } catch (Exception ignore) {
                }
                appStatFile = new RandomAccessFile("/proc/" + Process.myPid() + "/stat", "r");
            } else {
                appStatFile.seek(0L);
            }
            if (procStatFile != null) {
                procStatFile.seek(0L);
                while (true) {
                    String procStatString = procStatFile.readLine();
                    logger.debug("loadTotalLine procStatString::{}", procStatString);
                    if (StringUtil.isNotEmpty(procStatString)) {
                        String[] procStats = procStatString.split(" ");
                        if (procStats.length > 8) {
                            cpuTime = Long.parseLong(procStats[2]) + Long.parseLong(procStats[3])
                                    + Long.parseLong(procStats[4]) + Long.parseLong(procStats[5])
                                    + Long.parseLong(procStats[6]) + Long.parseLong(procStats[7])
                                    + Long.parseLong(procStats[8]);
                        }
                        break;
                    }
                }
            }
            if (cpuTime == 0) {
                Date date = new Date();
                cpuTime = date.getTime();
            }

            while (true) {
                String appStatString = appStatFile.readLine();
                logger.debug("loadTotalLine appStatString::{}", appStatString);
                if (StringUtil.isNotEmpty(appStatString)) {
                    String[] appStats = appStatString.split(" ");
                    appTime = Long.parseLong(appStats[13]) + Long.parseLong(appStats[14]);
                    break;
                }
            }
            if (lastCpuTime == null || lastAppCpuTime == null) {
                lastCpuTime = cpuTime;
                lastAppCpuTime = appTime;
                logger.debug("sampleValue0::{}", sampleValue);
                return sampleValue;
            }
            sampleValue = ((float) (appTime - lastAppCpuTime) /
                    (float) (cpuTime - lastCpuTime)) * 100f;
            logger.debug("sampleValue::{}, cpuTime::{}({}, {}), appTime::{}({}, {})",
                    sampleValue, (cpuTime - lastCpuTime), lastCpuTime, cpuTime, (appTime - lastAppCpuTime)
                    , lastAppCpuTime, appTime);
            lastCpuTime = cpuTime;
            lastAppCpuTime = appTime;
        } catch (Exception e) {
            logger.error("sampleValue error::{}", e.getMessage());
        }
        return sampleValue;
    }

    public Debug.MemoryInfo sampleMemory() {
        try {
            // 统计进程的内存信息 totalPss
            Debug.MemoryInfo dbm;
            // 太消耗CPU（5%）, 图形内存不能统计
//            dbm = new Debug.MemoryInfo();
//            Debug.getMemoryInfo(dbm);
            Context context = MainApplication.getInstance();
            activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            Debug.MemoryInfo[] memInfo = activityManager.getProcessMemoryInfo(
                    new int[]{Process.myPid()});
            dbm = memInfo[0];
            return dbm;
        } catch (Exception ignore) {
        }
        return null;
    }

    public static class Statistics implements Parcelable {
        public long totalMemory;
        public long storageSize;
        public float cpuUsage;

        public Statistics() {

        }
        Statistics(Parcel in) {
            totalMemory = in.readLong();
            cpuUsage = in.readFloat();
        }

        public final Creator<Statistics> CREATOR = new Creator<Statistics>() {
            @Override
            public Statistics createFromParcel(Parcel in) {
                return new Statistics(in);
            }

            @Override
            public Statistics[] newArray(int size) {
                return new Statistics[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(totalMemory);
            dest.writeFloat(cpuUsage);
        }
    }


    private static class PackageStatsObserver extends IPackageStatsObserver.Stub {
        FlowableEmitter<Statistics> emitter;

        public void setEmitter(FlowableEmitter<Statistics> emitter) {
            this.emitter = emitter;
        }

        public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) {

        }
    }

    public PackageStatsObserver packageStatsObserver = new PackageStatsObserver() {
        private IBinder mBinder;
        private IInterface mInterface;

        public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) {
            if (pStats != null) {
                long dataSize = pStats.dataSize + pStats.cacheSize + pStats.codeSize;
                dataSize += pStats.externalCacheSize + pStats.externalCodeSize + pStats.externalDataSize;
                dataSize += pStats.externalMediaSize + pStats.externalObbSize;
                if (emitter != null && !emitter.isCancelled()) {
                    Statistics statistics = new Statistics();
                    statistics.storageSize = dataSize;
                    emitter.onNext(statistics);
                }
            }
        }

        @Override
        public IBinder asBinder() {
            mBinder = super.asBinder();
            return mBinder;
        }

        @Override
        public IInterface queryLocalInterface(@NonNull String descriptor) {
            mInterface = super.queryLocalInterface(descriptor);
            return mInterface;
        }

        public void stopObserver() {
            if(mInterface != null){
                mBinder = mInterface.asBinder();
            }
            mBinder = null;
            mInterface = null;
        }
    };
}
