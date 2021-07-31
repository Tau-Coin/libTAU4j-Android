package io.taucoin.torrent.publishing.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.BatteryManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;

import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.utils.StringUtil;

/**
 * 系统服务管理
 */
public class SystemServiceManager {
    private static final Logger logger = LoggerFactory.getLogger("SystemService");
    private ConnectivityManager connectivityManager;
    private Context appContext;
    private static volatile SystemServiceManager instance;
    private SystemServiceManager(){
        this.appContext = MainApplication.getInstance();;
        connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static SystemServiceManager getInstance() {
        if (instance == null) {
            synchronized (SystemServiceManager.class) {
                if (instance == null)
                    instance = new SystemServiceManager();
            }
        }
        return instance;
    }

    /**
     * 是否有网络连接
     * @return boolean
     */
    public int getInternetType() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                && networkInfo.isConnected()) {
            return networkInfo.getType();
        } else {
            return -1;
        }
    }

    /**
     * 是否有网络连接
     * @return boolean
     */
    public boolean isHaveNetwork() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * 是否是Mobile连接
     * @return boolean
     */
    public boolean isNetworkMetered() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && connectivityManager.isActiveNetworkMetered();
    }

    /**
     * 是否在充电
     */
    public boolean isPlugged() {
        //创建过滤器拦截电量改变广播
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        //通过过滤器来获取电量改变intent 电量改变是系统广播所以无需去设置所以receiver传null即可
        Intent intent = appContext.registerReceiver(null, intentFilter);
        if(intent != null){
            //获取电量信息
            int isPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            //电源充电
            boolean acPlugged = BatteryManager.BATTERY_PLUGGED_AC == isPlugged;
            //usb充电
            boolean usbPlugged = BatteryManager.BATTERY_PLUGGED_USB == isPlugged;
            //无线充电
            boolean wirePlugged = BatteryManager.BATTERY_PLUGGED_WIRELESS == isPlugged;

            //满足充电即返回true
            return acPlugged || usbPlugged || wirePlugged;
        }
        return false;
    }

    /**
     * 获取网络地址
     * 如果有IPV4直接取IPV4，否则获取第一个IPV6
     */
    public String getNetworkAddress() {
        Network[] networks = connectivityManager.getAllNetworks();
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        String networkAddress = null;
        if (networks != null && activeNetworkInfo != null) {
            for (Network network : networks) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                if (networkInfo.toString().equals(activeNetworkInfo.toString())) {
                    LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
                    String name = linkProperties.getInterfaceName();
                    logger.debug("ActiveNetworkInfo InterfaceName::{}", name);
                    List<LinkAddress> linkAddresses = linkProperties.getLinkAddresses();
                    if (linkAddresses != null) {
                        for (LinkAddress linkAddress : linkAddresses) {
                            logger.debug("ActiveNetworkInfo Flags::{}, PrefixLength::{},Scope::{}",
                                    linkAddress.getFlags(), linkAddress.getPrefixLength(), linkAddress.getScope());
                            InetAddress address = linkAddress.getAddress();
                            if (isIPv4(address)) {
                                networkAddress = address.getHostAddress();
                                logger.debug("ActiveNetworkInfo IPv4 HostAddress::{}", networkAddress);
                            } else {
                                if (StringUtil.isEmpty(networkAddress)) {
                                    networkAddress = address.getHostAddress();
                                }
                                logger.debug("ActiveNetworkInfo IPv6 HostAddress::{}, isIPv6ULA::{}",
                                        networkAddress, isIPv6ULA(address));
                            }
                        }
                    }
                }
            }
        }
        return networkAddress;
    }

    private boolean isIPv6ULA(InetAddress address) {
        if (isIPv6(address)) {
            byte[] bytes = address.getAddress();
            return ((bytes[0] & (byte)0xfe) == (byte)0xfc);
        }
        return false;
    }

    /**
     * @return true if the address is IPv6.
     * @hide
     */
    public boolean isIPv6(InetAddress address) {
        return address instanceof Inet6Address;
    }

    /**
     * @return true if the address is IPv4 or is a mapped IPv4 address.
     * @hide
     */
    public boolean isIPv4(InetAddress address) {
        return address instanceof Inet4Address;
    }
}
