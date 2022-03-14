package io.taucoin.torrent.publishing.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.service.SystemServiceManager;

/*
 * The receiver for Network connection state changes state.
 */
public class ConnectionReceiver extends BroadcastReceiver {
    private Logger logger = LoggerFactory.getLogger("ConnectionReceiver");
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null){
            return;
        }
        Context appContext = context.getApplicationContext();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(appContext);
        String action = intent.getAction();
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            boolean isMetered = activeNetworkInfo != null && connectivityManager.isActiveNetworkMetered();
            NetworkSetting.setMeteredNetwork(isMetered);
            int internetType = SystemServiceManager.getInstance().getInternetType();
            if (internetType >= 0) {
                logger.debug("network type::{}", internetType);
                settingsRepo.internetState(true);
                settingsRepo.setInternetType(internetType);
            } else {
                logger.debug("network type::-1");
                settingsRepo.internetState(false);
                settingsRepo.setInternetType(-1);
            }
        }
    }

    public static IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        return filter;
    }
}