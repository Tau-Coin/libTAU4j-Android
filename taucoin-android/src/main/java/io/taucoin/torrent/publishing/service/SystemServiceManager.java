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
import java.util.ArrayList;
import java.util.List;

import io.taucoin.torrent.publishing.MainApplication;

/**
 * 系统服务管理
 */
public class SystemServiceManager {
    private static final Logger logger = LoggerFactory.getLogger("SystemService");
    private ConnectivityManager connectivityManager;
    private Context appContext;
    private static volatile SystemServiceManager instance;
    private SystemServiceManager(){
        this.appContext = MainApplication.getInstance();
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
     * 1、获取当前ActiveNetworkInfo的type，
     * 2、遍历所有的Networks，满足type和第一步中的type相等 && isConnected()的从中间看是否有ipv4
     */
    public List<String> getNetworkAddress() {
        Network[] networks = connectivityManager.getAllNetworks();
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        List<String> ipv4List = new ArrayList<>();
        List<String> ipv6List = new ArrayList<>();
        if (networks != null && activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            int activeType = activeNetworkInfo.getType();
            logger.debug("ActiveNetworkInfo ::{}", activeNetworkInfo.toString());
            for (Network network : networks) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                if (null == networkInfo) {
                    continue;
                }
                logger.debug("NetworkInfo ::{}", networkInfo.toString());
                boolean isActive = activeType == networkInfo.getType() && networkInfo.isConnected();
                LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
                if (linkProperties != null) {
                    String name = linkProperties.getInterfaceName();
                    logger.debug("Active::{}, NetworkInfo InterfaceName::{}", isActive, name);
                    List<LinkAddress> linkAddresses = linkProperties.getLinkAddresses();
                    if (linkAddresses != null) {
                        for (LinkAddress linkAddress : linkAddresses) {
                            logger.debug("NetworkInfo Flags::{}, PrefixLength::{}, Scope::{}",
                                    linkAddress.getFlags(), linkAddress.getPrefixLength(), linkAddress.getScope());
                            InetAddress address = linkAddress.getAddress();
                            if (address != null) {
                                if (isIPv4(address)) {
                                    String ipv4 = address.getHostAddress();
                                    if (isActive && !address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                                        ipv4List.add(ipv4 + ":0");
                                    }
                                    logger.debug("NetworkInfo IPv4 HostAddress::{}", ipv4);
                                } else {
                                    String ipv6 = address.getHostAddress();
                                    if (isActive && !address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                                        // 需要加中括号，代表地址（地址可能用::被省略）
                                        ipv6List.add("[" + ipv6 + "]:0");
                                    }
                                    logger.debug("NetworkInfo IPv6 HostAddress::{}, isIPv6ULA::{}",
                                            ipv6, isIPv6ULA(address));
                                }
                            }
                            logger.debug("NetworkInfo ****************************************");
                        }
                    }
                }
            }
        }
        logger.debug("getNetworkAddress IPv4 size::{}, IPv6 size::{}", ipv4List.size(), ipv6List.size());
        if (ipv4List.size() > 0) {
            return ipv4List;
        } else {
            return ipv6List;
        }
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
