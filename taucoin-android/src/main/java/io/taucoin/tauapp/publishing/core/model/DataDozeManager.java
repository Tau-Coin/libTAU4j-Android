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
 * 1. Data Doze
 * 大多数操作系统包括android和chromeOS都会对电池电源做精细管理，但是对流量一般不做控制。TAU考虑到非洲地区流量成本，增加这个模式
 * 进入条件：
 * 1. 如果3分钟内（固定参数）用户和TAU app没有“鼠标键盘手指交互”并且没有“android doze过”就进入“TAU Doze”。
 * 2. 没有网络时，节点为0
 *
 * Data Doze模式：
 * app根据网络流量剩余来间歇工作 >70%，>50%，>30%，>10%，就进入“区块链业务进程挂起”对应的0，5，10，20分钟策略。
 *
 * 退出tau doze模式的条件：
 * 1、鼠标键盘手指交互
 * 2、网络任何变化
 * 3、Android doze结束
 * 4、切入前台 foreground
 * 5、流量包选择变化
 *
 * @see <a href="https://github.com/Tau-Coin/libTAU4j-Android/blob/main/docs/data_doze_mode.md">Data Doze模式</a>
 */
public class DataDozeManager {
    private static final Logger logger = LoggerFactory.getLogger("DataDozeManager");
    protected static final long TAU_UP_TIME = 3 * 60;           // 单位：s
    protected static final long HOURS24_TIME = 24 * 60 * 60;    // 单位：s
    private final TauDaemon daemon;
    private final SettingsRepository settingsRepo;
    private final LinkedBlockingQueue<DozeEvent> eventsQueue = new LinkedBlockingQueue<>();
    private int dataAvailableRate = 100;

    private long dozeTime = 0;
    private boolean isDozeMode = false;
    private long dozeStartTime;
    private final Disposable handlerDisposable;
    private final Disposable observerDisposable;

    DataDozeManager(TauDaemon daemon, SettingsRepository settingsRepo) {
        this.daemon = daemon;
        this.settingsRepo = settingsRepo;
        handlerDisposable = createDataDozeHandler();
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

    public void setDataAvailableRate(int dataAvailableRate) {
        int currentRate = dataAvailableRate;
        // 发达国家WiFi网络流量不限制
        if (NetworkSetting.isUnlimitedNetwork()) {
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
        int rate = dataAvailableRate;
        long dozeTime;
        if (rate > 70) {
            dozeTime = 0;
        } else if (rate > 50) {
            dozeTime = 5 * 60;
        } else if (rate > 30) {
            dozeTime = 10 * 60;
        } else {
            dozeTime = 20 * 60;
        }
        if (reset) {
            this.dozeTime = dozeTime;
        }
        logger.debug("calculateDozeTime data::{}%, reset::{}, oldDozeTime::{}s, dozeTime::{}s",
                dataAvailableRate, reset, this.dozeTime, dozeTime);
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

    public Disposable createDataDozeHandler() {
        return Observable.create(emitter -> {
            waitTime = DataDozeManager.TAU_UP_TIME;
            synchronized (taskLock) {
                while (!emitter.isDisposed()) {
                    try {
                        logger.debug("DataDoze wait::{}s, isDozeMode::{}", waitTime, isDozeMode);
                        // 等待时间, 默认等待结束，有点击事件等会直接退出
                        isWaitTimeout = true;
                        taskLock.wait(waitTime * 1000);
                        logger.debug("DataDoze isWaitTimeout::{}", isWaitTimeout);
                        // 等待结束
                        if (isDozeMode) {
                            // 计算真实进入doze模式的时间
                            long realDozeTime = calculateRealDozeTime();
                            if (realDozeTime > 0) {
                                settingsRepo.updateDataDozeTime(realDozeTime, isForeDozeTime);
                            }
                            logger.debug("DataDoze end totalDozeTime::{}s, realDozeTime::{}s, isForeDozeTime::{}",
                                    settingsRepo.getDataDozeTime(isForeDozeTime), realDozeTime, isForeDozeTime);
                            if (!isDirectDoze) {
                                daemon.resumeService();
                                waitTime = DataDozeManager.TAU_UP_TIME;
                                setDozeMode(false);
                            } else {
                                waitTime = DataDozeManager.HOURS24_TIME;
                                resetDozeStartTime();
                                logger.debug("DataDoze continue doze::{}s", waitTime);
                            }
                            // 恢复由于前后台切换设置的值
                            isForeDozeTime = isForegroundRunning();
                        } else {
                            // 等待进入doze模式的时间（3分钟）完成，并且dozeTime大于0（满足进入doze模式的条件）
                            // 才能进入doze模式
                            long dozeTime = calculateDozeTime(true);
                            if (isDirectDoze) {
                                waitTime = DataDozeManager.HOURS24_TIME;
                                daemon.pauseService();
                                setDozeMode(true);
                                resetDozeStartTime();
                                logger.debug("DataDoze direct doze::{}s", waitTime);
                            } else if (isWaitTimeout && dozeTime > 0) {
                                waitTime = dozeTime;
                                daemon.pauseService();
                                setDozeMode(true);
                                resetDozeStartTime();
                                logger.debug("DataDoze start doze::{}s", waitTime);
                            } else {
                                // 重新等待3分钟，再次查看是否满足进入doze模式的条件
                                waitTime = DataDozeManager.TAU_UP_TIME;
                                logger.debug("DataDoze continue wait::{}s", waitTime);
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
