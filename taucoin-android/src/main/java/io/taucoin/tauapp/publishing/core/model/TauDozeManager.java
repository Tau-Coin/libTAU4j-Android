package io.taucoin.tauapp.publishing.core.model;

import android.os.SystemClock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;
import io.taucoin.tauapp.publishing.core.utils.NetworkSetting;

/**
 * TAU休息模式
 *
 * APP会根据电量和网络流量剩余来间歇工作，来达到节能和节约流量的目的。其中“区块链模块”挂起时间计算策略为：
 * 根据两个指标较低剩余百分比90%（包括充电状态）、70%、50%、30%、10%对应为0、3、6、12、24分钟。
 * "TAU休息模式"的两种方式：
 * 第一种方式：
 * 进入条件：
 *
 * APP在3分钟（固定参数）未发生以下情况：
 *
 * 鼠标键盘手指交互；
 * Android Doze结束；
 * 前后台切换；
 * 充电断电、电量百分比变化和可用流量百分比变化时重新计算TAU的挂起时间，与上次的计算挂起时间不一致；
 * 退出条件：
 *
 * APP在处于"TAU休息模式"时发生以下情况：
 *
 * 鼠标键盘手指交互；
 * Android Doze结束；
 * 前后台切换；
 * 充电断电、电量百分比变化和可用流量百分比变化时重新计算TAU的挂起时间，与上次的计算挂起时间不一致；
 * 第二种方式：
 * 进入条件：
 *
 * DHT节点数为0（如果当前处于doze休息模式，继续保持；否则立即进入）
 * 退出条件：
 *
 * DHT节点数大于0
 *
 * @see <a href="https://github.com/Tau-Coin/libTAU4j-Android/blob/main/docs/tau_doze_mode.md">TAU休息模式</a>
 */
public class TauDozeManager {
    private static final Logger logger = LoggerFactory.getLogger("TauDozeManager");
    protected static final long TAU_UP_TIME = 3 * 60;           // 单位：s
    protected static final long HOURS24_TIME = 24 * 60 * 60;    // 单位：s
    private final TauDaemon daemon;
    private final SettingsRepository settingsRepo;
    private final LinkedBlockingQueue<DozeEvent> eventsQueue = new LinkedBlockingQueue<>();
    private boolean chargingState = false;
    private int batteryLevel = 100;
    private int dataAvailableRate = 100;

    private long dozeTime = 0;
    private boolean isDozeMode = false;
    private long dozeStartTime;
    private final Disposable handlerDisposable;
    private final Disposable observerDisposable;

    TauDozeManager(TauDaemon daemon, SettingsRepository settingsRepo) {
        this.daemon = daemon;
        this.settingsRepo = settingsRepo;
        handlerDisposable = createTauDozeHandler();
        observerDisposable = createEventObserver();
    }

    void onCleared() {
        eventsQueue.clear();
        if (observerDisposable != null && !observerDisposable.isDisposed()) {
            observerDisposable.dispose();
        }
        if (handlerDisposable != null && !handlerDisposable.isDisposed()) {
            handlerDisposable.dispose();
        }
    }

    public boolean isDozeMode() {
        return this.isDozeMode;
    }

    public void setDozeMode(boolean dozeMode) {
        this.isDozeMode = dozeMode;
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
        int currentRate = dataAvailableRate;
        // 发达国家WiFi网络流量不限制
        if (NetworkSetting.isDevelopCountry()) {
            currentRate = 100;
        }
        if (this.dataAvailableRate != currentRate) {
            logger.debug("setDataAvailableRate::{}", currentRate);
            this.dataAvailableRate = currentRate;
            checkDozeTime();
        }
    }

    /**
     * 检查doze time是否变化，变化重新开始
     */
    public void checkDozeTime() {
        long dozeTime = calculateDozeTime(false);
        if (this.dozeTime != dozeTime) {
            daemon.newActionEvent(DozeEvent.DOZE_TIME_CHANGED);
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
            dozeTime = 3 * 60;
        } else if (rate >= 50) {
            dozeTime = 6 * 60;
        } else if (rate >= 30) {
            dozeTime = 12 * 60;
        } else {
            dozeTime = 24 * 60;
        }
        if (reset) {
            this.dozeTime = dozeTime;
        }
        logger.debug("calculateDozeTime charging::{}, battery::{}%, data::{}%, reset::{}, " +
                        "oldDozeTime::{}s, dozeTime::{}s",
                chargingState, batteryLevel, dataAvailableRate, reset, this.dozeTime, dozeTime);
        return dozeTime;
    }

    public long calculateRealDozeTime() {
        if (isDozeMode) {
            long dozeEndTime = SystemClock.uptimeMillis();
            long realDozeTime = (dozeEndTime - dozeStartTime) / 1000;
            return Math.max(realDozeTime, 0);
        }
        return 0;
    }

    public void resetDozeStartTime() {
        this.dozeStartTime = SystemClock.uptimeMillis();
    }

    /**
     * 有nodes和无nodes触发一次
     * @param nodes 节点数
     */
    public void setNodesChanged(long nodes) {
        if (this.nodes > 0 && nodes <= 0) {
            // 无nodes
            this.nodes = nodes;
            newActionEvent(DozeEvent.NODES_CHANGED);
        } else if (this.nodes <= 0 && nodes > 0) {
            // 有nodes
            this.nodes = nodes;
            newActionEvent(DozeEvent.NODES_CHANGED);
        } else {
            this.nodes = nodes;
        }
    }

    /**
     * 设置当前是否是处于前台
     * 这里防止第一次启动造成多余处理
     * @param isForeground 否是前台
     */
    private boolean isForeground = true;                // 是否是前台
    public void setForeground(boolean isForeground) {
        if (this.isForeground != isForeground) {
            // 无nodes
            this.isForeground = isForeground;
            newActionEvent(DozeEvent.FORE_BACK);
        }
    }

    public boolean isForegroundRunning() {
        return isForeground;
    }

    public void newActionEvent(DozeEvent event) {
        long currentTime = DateUtil.getMillisTime();
        // 15s内非doze模式的事件忽略，处于doze模式要唤醒
        boolean isIgnore = !isDozeMode && currentTime - eventTime < 15 * 1000;
        if (!isIgnore) {
            this.eventTime = currentTime;
        }
        // 只有有nodes的时候事件才入队列，无nodes
        boolean isEnterQueue = (nodes > 0 && !isIgnore) || event == DozeEvent.FORE_BACK ||
                event == DozeEvent.NODES_CHANGED;
        logger.debug("TauDoze newActionEvent::{}, isEnterQueue::{}", event.name(), isEnterQueue);
        if (isEnterQueue) {
            eventsQueue.add(event);
        }
    }

    private long eventTime;                         // 上一次事件时间
    private long waitTime;                          // 下一次线程等待时间
    private boolean isWaitTimeout;                  // 是否是线程等待超时
    private boolean isDirectDoze = false;           // 是否直接进入tau doze模式（主要针对无网络）
    private boolean isForeDozeTime = false;         // 更新doze时间，判断是否是前台时间
    private long nodes = 0;                         // nodes数
    private final Object taskLock = new Object();   // 线程处理任务锁

    public Disposable createEventObserver() {
        return Observable.create(emitter -> {
            Thread.currentThread().setName("TauDoze");
            while (!emitter.isDisposed()) {
                try {
                    DozeEvent event = eventsQueue.take();
                    logger.debug("TauDoze newActionEvent::{}, eventsQueue::{}", event.name(), eventsQueue.size());
                    synchronized (taskLock) {
                        isForeDozeTime = isForegroundRunning();
                        if (event == DozeEvent.NODES_CHANGED) {
                            isDirectDoze = nodes <= 0;
                        } else if (event == DozeEvent.FORE_BACK) {
                            isForeDozeTime = !isForegroundRunning();
                        }
                        logger.debug("TauDoze newActionEvent::{}, isForeDozeTime::{}", event.name(), isForeDozeTime);
                        isWaitTimeout = false;
                        taskLock.notifyAll();
                    }
                } catch (Exception e) {
                    break;
                }
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
    }

    public Disposable createTauDozeHandler() {
        return Observable.create(emitter -> {
            waitTime = TauDozeManager.TAU_UP_TIME;
            synchronized (taskLock) {
                while (!emitter.isDisposed()) {
                    try {
                        logger.debug("TauDoze wait::{}s, isDozeMode::{}", waitTime, isDozeMode);
                        // 等待时间, 默认等待结束，有点击事件等会直接退出
                        isWaitTimeout = true;
                        taskLock.wait(waitTime * 1000);
                        logger.debug("TauDoze isWaitTimeout::{}", isWaitTimeout);
                        // 等待结束
                        if (isDozeMode) {
                            // 计算真实进入doze模式的时间
                            long realDozeTime = calculateRealDozeTime();
                            if (realDozeTime > 0) {
                                settingsRepo.updateTauDozeTime(realDozeTime, isForeDozeTime);
                            }
                            logger.debug("TauDoze end totalDozeTime::{}s, realDozeTime::{}s, isForeDozeTime::{}",
                                    settingsRepo.getTauDozeTime(isForeDozeTime), realDozeTime, isForeDozeTime);
                            if (!isDirectDoze) {
                                daemon.resumeService();
                                waitTime = TauDozeManager.TAU_UP_TIME;
                                setDozeMode(false);
                            } else {
                                waitTime = TauDozeManager.HOURS24_TIME;
                                resetDozeStartTime();
                                logger.debug("TauDoze continue doze::{}s", waitTime);
                            }
                            // 恢复由于前后台切换设置的值
                            isForeDozeTime = isForegroundRunning();
                        } else {
                            // 等待进入doze模式的时间（3分钟）完成，并且dozeTime大于0（满足进入doze模式的条件）
                            // 才能进入doze模式
                            long dozeTime = calculateDozeTime(true);
                            if (isDirectDoze) {
                                waitTime = TauDozeManager.HOURS24_TIME;
                                daemon.pauseService();
                                setDozeMode(true);
                                resetDozeStartTime();
                                logger.debug("TauDoze direct doze::{}s", waitTime);
                            } else if (isWaitTimeout && dozeTime > 0) {
                                waitTime = dozeTime;
                                daemon.pauseService();
                                setDozeMode(true);
                                resetDozeStartTime();
                                logger.debug("TauDoze start doze::{}s", waitTime);
                            } else {
                                // 重新等待3分钟，再次查看是否满足进入doze模式的条件
                                waitTime = TauDozeManager.TAU_UP_TIME;
                                logger.debug("TauDoze continue wait::{}s", waitTime);
                            }
                        }
                    } catch (Exception e) {
                        break;
                    }
                }
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
    }
}
