package io.taucoin.torrent.publishing.core.model;

import android.os.SystemClock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * tau休息模式管理
 *
 * 如果3分钟内（固定参数）用户和TAU app没有“鼠标键盘手指交互”和“doze过”就进入“TAU 休息模式”。TAU休息模式时，
 * app根据电池和网络流量剩余来间歇工作（根据两个指标较低剩余百分比90%（包括充电状态），70%，50%，30%，10%可以定休眠时间长度），
 * 目前先就先采用“区块链业务进程挂起”相应0，3，6，12，24分钟策略。当用户从休眠状态启动交互，区块链模块要被唤醒。
 */
class TauDozeManager {
    private static final Logger logger = LoggerFactory.getLogger("TauDozeManager");
    protected static final long TAU_UP_TIME = 3 * 60;
    private final TauDaemon daemon;
    private boolean chargingState = false;
    private int batteryLevel = 100;
    private int dataAvailableRate = 100;

    private long dozeTime = 0;
    private boolean isDozeMode = false;
    private long dozeStartTime;

    TauDozeManager(TauDaemon daemon) {
        this.daemon = daemon;
    }

    public boolean isDozeMode() {
        return this.isDozeMode;
    }

    public void setDozeMode(boolean dozeMode) {
        this.isDozeMode = dozeMode;
        this.dozeStartTime = SystemClock.uptimeMillis();
    }

    public void setChargingState(boolean chargingState) {
        if (this.chargingState != chargingState) {
            logger.debug("setChargingState::{}", chargingState);
            this.chargingState = chargingState;
            checkDozeTime();
        }
    }

    public void setBatteryLevel(int batteryLevel) {
        if (this.batteryLevel != batteryLevel) {
            logger.debug("setBatteryLevel::{}", batteryLevel);
            this.batteryLevel = batteryLevel;
            checkDozeTime();
        }
    }

    public void setDataAvailableRate(int dataAvailableRate) {
        if (this.dataAvailableRate != dataAvailableRate) {
            logger.debug("setDataAvailableRate::{}", dataAvailableRate);
            this.dataAvailableRate = dataAvailableRate;
            checkDozeTime();
        }
    }

    /**
     * 检查doze time是否变化，变化重新开始
     */
    public void checkDozeTime() {
        long dozeTime = calculateDozeTime(false);
        if (this.dozeTime != dozeTime) {
            daemon.newActionEvent();
        }
    }

    public long calculateDozeTime(boolean reset) {
        int rate;
        if (chargingState) {
            rate = dataAvailableRate;
        } else {
            rate = Math.min(batteryLevel, dataAvailableRate);
        }
        long dozeTime;
        if (rate >= 90) {
            dozeTime = 0;
        } else if (rate >= 70) {
            dozeTime = 3 * 60 * 1000;
        } else if (rate >= 50) {
            dozeTime = 6 * 60 * 1000;
        } else if (rate >= 30) {
            dozeTime = 12 * 60 * 1000;
        } else {
            dozeTime = 24 * 60 * 100;
        }
        if (reset) {
            this.dozeTime = dozeTime;
        }
        logger.debug("calculateDozeTime charging::{}, battery::{}%, data::{}%, reset::{}, " +
                        "oldDozeTime::{}%, dozeTime::{}ms",
                chargingState, batteryLevel, dataAvailableRate, reset, this.dozeTime, dozeTime);
        return dozeTime;
    }

    public long calculateRealDozeTime() {
        if (isDozeMode) {
            long dozeEndTime = SystemClock.uptimeMillis();
            return (dozeEndTime - dozeStartTime) / 1000;
        }
        return 0;
    }
}
