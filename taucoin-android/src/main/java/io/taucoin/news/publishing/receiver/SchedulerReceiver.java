package io.taucoin.news.publishing.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.taucoin.news.publishing.core.model.TauDaemon;
import io.taucoin.news.publishing.service.Scheduler;

import static io.taucoin.news.publishing.service.Scheduler.SCHEDULER_WORK_WAKE_UP_APP_SERVICE;

/**
 * The receiver for AlarmManager scheduling
 */
public class SchedulerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null){
            return;
        }

        Context appContext = context.getApplicationContext();
        switch (intent.getAction()) {
            case SCHEDULER_WORK_WAKE_UP_APP_SERVICE:
                wakeUpAppService(appContext);
                break;
        }
    }

    /**
     * 唤醒App Service
     */
    private void wakeUpAppService(Context appContext) {
        TauDaemon.getInstance(appContext).start();
        Scheduler.setWakeUpAppAlarm(appContext);
    }
}
