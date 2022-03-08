package io.taucoin.torrent.publishing.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.List;

import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;

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
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                NetworkInfo newNetworkInfo = activeNetworkInfo;
                Network[] allNetworks = connectivityManager.getAllNetworks();
                if (allNetworks != null) {
                    for (Network network : allNetworks) {
                        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                        if (networkInfo != null) {
                            parseNetworkAddress(networkInfo.getType(), connectivityManager.getLinkProperties(network));
                            if (networkInfo.isConnected()) {
                                if ( networkInfo.getType() > newNetworkInfo.getType()) {
                                    newNetworkInfo = networkInfo;
                                }
                            }
                        }
                    }
                }
                logger.debug("network type::{}, activeNetworkInfo::{}",
                        newNetworkInfo.getType(), newNetworkInfo);
                settingsRepo.internetState(true);
                settingsRepo.setInternetType(newNetworkInfo.getType());
            } else {
                logger.debug("network type::-1, activeNetworkInfo::{}",
                        null == activeNetworkInfo ? "null" : activeNetworkInfo);
                settingsRepo.internetState(false);
                settingsRepo.setInternetType(-1);
            }
        }
    }

    private void parseNetworkAddress(int type, LinkProperties linkProperties) {
        if (linkProperties != null) {
            List<LinkAddress> linkAddresses = linkProperties.getLinkAddresses();
            if (linkAddresses != null) {
                for (LinkAddress linkAddress : linkAddresses) {
                    InetAddress address = linkAddress.getAddress();
                    if (address != null) {
                        if (address instanceof Inet4Address && !address.isLoopbackAddress() &&
                                !address.isLinkLocalAddress()) {
                            String ipv4 = address.getHostAddress();
                            logger.debug("parseNetworkAddress type::{} IPv4::{}", type, ipv4);
                            break;
                        }
                    }
                }
            }
        }
    }

    public static IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        return filter;
    }
}