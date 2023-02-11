package io.taucbd.news.publishing.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucbd.news.publishing.core.model.TauDaemon;
import io.taucbd.news.publishing.core.storage.sp.SettingsRepository;
import io.taucbd.news.publishing.core.storage.RepositoryHelper;

/*
 * The receiver for power monitoring.
 */

public class PowerReceiver extends BroadcastReceiver {
    private static final Logger logger = LoggerFactory.getLogger("PowerReceiver");
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
                break;
            case Intent.ACTION_POWER_DISCONNECTED:
                settingsRepo.chargingState(false);
                break;
            case Intent.ACTION_BATTERY_CHANGED:
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                logger.debug("battery changed::{}", level);
                TauDaemon.getInstance(appContext).getTauDozeManager().setBatteryLevel(level);
                break;
            case Intent.ACTION_SCREEN_ON:
                TauDaemon.getInstance(appContext).getTauDozeManager().setScreenState(true);
                break;
            case Intent.ACTION_SCREEN_OFF:
                TauDaemon.getInstance(appContext).getTauDozeManager().setScreenState(false);
                break;
        }
    }

    public static IntentFilter getCustomFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        return filter;
    }
}
