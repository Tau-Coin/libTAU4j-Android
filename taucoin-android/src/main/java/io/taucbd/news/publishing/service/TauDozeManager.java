package io.taucbd.news.publishing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucbd.news.publishing.core.model.TauDaemon;
import io.taucbd.news.publishing.core.utils.ObservableUtil;

/**
 * TAU doze manager
 * 1、当电池80%以上并且自己在后台且前台灭屏工作五分钟休息5分钟；
 * 2、同样40%以上工作三分钟休息三分钟；
 * 3、40以下取消doze让安卓接管；
 * 4、当自己在后台，前台屏幕亮时，不要触发doze；
 */
public class TauDozeManager {
    private static final Logger logger = LoggerFactory.getLogger("TauDozeManager");
    private Disposable serviceDisposable;
    private final TauDaemon daemon;
    private boolean screenState = true;
    private boolean chargeState = true;
    private int intervalSeconds = 0;

    public TauDozeManager(TauDaemon daemon) {
        this.daemon = daemon;
    }

    public void onCleared() {
        if (serviceDisposable != null && !serviceDisposable.isDisposed()) {
            serviceDisposable.dispose();
        }
    }

    public void setBatteryLevel(int batteryLevel) {
        int intervalSeconds = 0;
        if (batteryLevel > 80) {
            intervalSeconds = 5 * 60;
        } else if (batteryLevel > 40) {
            intervalSeconds = 3 * 60;
        }
        if (intervalSeconds > 0) {
            if (this.intervalSeconds != intervalSeconds && !screenState && !chargeState) {
                this.intervalSeconds = intervalSeconds;
                logger.debug("batteryLevel::{}, intervalSeconds::{}, screenState::false, resetLibTAUService",
                        batteryLevel, intervalSeconds);
                resetLibTAUService();
            } else {
                this.intervalSeconds = intervalSeconds;
            }
        } else {
            logger.debug("batteryLevel::{}, intervalSeconds::{}, resumeService", batteryLevel, intervalSeconds);
            disposeServiceDisposable();
            daemon.resumeService();
        }
    }

    public void setChargeState(boolean chargeState) {
        this.chargeState = chargeState;
        logger.debug("setChargeState::{}", chargeState);
        if (chargeState) {
            disposeServiceDisposable();
            daemon.resumeService();
            logger.debug("setChargeState resumeService");
        } else {
            if (intervalSeconds > 0 && !screenState) {
                logger.debug("setChargeState resetLibTAUService");
                resetLibTAUService();
            } else {
                logger.debug("setChargeState waiting");
            }
        }
    }

    public void setScreenState(boolean screenState) {
        this.screenState = screenState;
        logger.debug("setScreenState::{}", screenState);
        if (screenState) {
            disposeServiceDisposable();
            daemon.resumeService();
            logger.debug("setScreenState resumeService");
        } else {
            if (intervalSeconds > 0 && !chargeState) {
                logger.debug("setScreenState resetLibTAUService");
                resetLibTAUService();
            } else {
                logger.debug("setScreenState waiting");
            }
        }
    }

    private void disposeServiceDisposable() {
        if (serviceDisposable != null && !serviceDisposable.isDisposed()) {
            serviceDisposable.dispose();
        }
    }

    /**
     * 重置libTAU服务
     */
    private void resetLibTAUService() {
        logger.debug("resetLibTAUService... intervalSeconds::{}", intervalSeconds);
        disposeServiceDisposable();
        boolean[] state = new boolean[1];
        serviceDisposable = ObservableUtil.intervalSeconds(intervalSeconds)
                .subscribeOn(Schedulers.io())
                .subscribe(l -> {
                    if (state[0]) {
                        logger.debug("resetLibTAUService pauseService");
                        daemon.pauseService();
                        state[0] = false;
                    } else {
                        logger.debug("resetLibTAUService resumeService");
                        daemon.resumeService();
                        state[0] = true;
                    }
                });
    }
}
