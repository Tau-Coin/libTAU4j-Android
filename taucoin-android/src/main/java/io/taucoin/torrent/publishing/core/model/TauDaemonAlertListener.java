package io.taucoin.torrent.publishing.core.model;

import org.libTAU4j.AlertListener;
import org.libTAU4j.alerts.Alert;

/**
 * 监听libTAU Alert上报
 */
public abstract class TauDaemonAlertListener implements AlertListener {

    @Override
    public int[] types() {
        return null;
    }

    /**
     * libTAU所有的上报
     */
    @Override
    public void alert(Alert<?> alert) {

    }

    void onTauStarted(boolean success, String errMsg) {

    }

    void onTauStopped() {

    }
}
