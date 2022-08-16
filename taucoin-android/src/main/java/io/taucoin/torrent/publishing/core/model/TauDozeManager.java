package io.taucoin.torrent.publishing.core.model;

import android.os.SystemClock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.utils.DateUtil;

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
    protected static final long HOURS24_TIME = 24 * 60 * 60;
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

    public void newActionEvent(DozeEvent event) {
        long currentTime = DateUtil.getMillisTime();
        // 15s内非doze模式的事件忽略，处于doze模式要唤醒
        boolean isIgnore = !isDozeMode && currentTime - eventTime < 15 * 1000;
        if (!isIgnore) {
            this.eventTime = currentTime;
        }
        // 只有有nodes的时候事件才入队列，无nodes
        boolean isEnterQueue = (nodes > 0 && !isIgnore) || event == DozeEvent.DOZE_INIT ||
                event == DozeEvent.NODES_CHANGED;
        logger.debug("TauDoze newActionEvent::{}, isEnterQueue::{}", event.name(), isEnterQueue);
        if (isEnterQueue) {
            eventsQueue.add(event);
        }
    }

    private long eventTime;
    private long waitTime;
    private boolean isWaitTimeout;
    private boolean isDirectDoze = false;
    private long nodes = 0;
    private final Object taskLock = new Object();

    public Disposable createEventObserver() {
        return Observable.create(emitter -> {
            Thread.currentThread().setName("TauDoze");
            while (!emitter.isDisposed()) {
                try {
                    DozeEvent event = eventsQueue.take();
                    logger.debug("TauDoze newActionEvent::{}, eventsQueue::{}", event.name(), eventsQueue.size());
                    synchronized (taskLock) {
                        if (event == DozeEvent.NODES_CHANGED) {
                            isDirectDoze = nodes <= 0;
                        }
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
                                settingsRepo.updateTauDozeTime(realDozeTime);
                            }
                            logger.debug("TauDoze end totalDozeTime::{}s, realDozeTime::{}s",
                                    settingsRepo.getTauDozeTime(), realDozeTime);
                            if (!isDirectDoze) {
                                daemon.resumeService();
                                waitTime = TauDozeManager.TAU_UP_TIME;
                                setDozeMode(false);
                            } else {
                                waitTime = TauDozeManager.HOURS24_TIME;
                                resetDozeStartTime();
                                logger.debug("TauDoze continue doze::{}s", waitTime);
                            }
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
