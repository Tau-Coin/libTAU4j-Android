package io.taucbd.news.publishing.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucbd.news.publishing.core.storage.RepositoryHelper;
import io.taucbd.news.publishing.core.storage.sp.SettingsRepository;
import io.taucbd.news.publishing.core.utils.NetworkSetting;
import io.taucbd.news.publishing.service.SystemServiceManager;

/*
 * The receiver for Network connection state changes state.
 */
public class ConnectionReceiver extends BroadcastReceiver {
    private final static Logger logger = LoggerFactory.getLogger("ConnectionReceiver");
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null){
            return;
        }
        Context appContext = context.getApplicationContext();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(appContext);
        String action = intent.getAction();
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            int internetType = SystemServiceManager.getInstance().getInternetType();
            if (internetType >= 0) {
                // 1、更新是否是计费网络
                boolean isMetered = SystemServiceManager.getInstance().isNetworkMetered();
                NetworkSetting.setMeteredNetwork(isMetered);

                // 2、更新是否是WiFi
                boolean isWiFi = SystemServiceManager.getInstance().isWiFi();
                logger.debug("network wifi::{}, metered::{}", isWiFi, isMetered);
                NetworkSetting.setWiFiNetwork(isWiFi);

                // 3、更新是否为发达国家
                NetworkSetting.updateDevelopCountry();

                // 4、最后一步更新（保证上面三个条件判断的准确性）
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