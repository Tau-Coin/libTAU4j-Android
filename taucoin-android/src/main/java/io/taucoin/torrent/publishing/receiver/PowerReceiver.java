package io.taucoin.torrent.publishing.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;

/*
 * The receiver for power monitoring.
 */

public class PowerReceiver extends BroadcastReceiver {
    private static final Logger logger = LoggerFactory.getLogger("PowerReceiver");
    private int level = 100;
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null){
            return;
        }
        Context appContext = context.getApplicationContext();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(appContext);
        switch (action) {
            case Intent.ACTION_POWER_CONNECTED:
                settingsRepo.chargingState(true);
                TauDaemon.getInstance(appContext).setChargingState(true);
                break;
            case Intent.ACTION_POWER_DISCONNECTED:
                settingsRepo.chargingState(false);
                TauDaemon.getInstance(appContext).setChargingState(false);
                break;
            case Intent.ACTION_BATTERY_CHANGED:
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                if (this.level != level) {
                    logger.debug("battery changed::{}", level);
                    TauDaemon.getInstance(appContext).setBatteryLevel(level);
                }
                break;
        }
    }

    public static IntentFilter getCustomFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        return filter;
    }
}
