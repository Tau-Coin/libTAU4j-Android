package io.taucoin.torrent.publishing.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
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
     * 如果有IPV4直接取IPv4，否则获取第一个IPv6
     */
    public String getNetworkAddress() {
        Network[] networks = connectivityManager.getAllNetworks();
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        String networkAddress = null;
        if (networks != null && activeNetworkInfo != null) {
            logger.debug("ActiveNetworkInfo ::{}", activeNetworkInfo.toString());
            for (Network network : networks) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                logger.debug("NetworkInfo ::{}", networkInfo.toString());
                if (networkInfo.toString().equals(activeNetworkInfo.toString())) {
                    LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
                    String name = linkProperties.getInterfaceName();
                    logger.debug("ActiveNetworkInfo InterfaceName::{}", name);
                    List<LinkAddress> linkAddresses = linkProperties.getLinkAddresses();
                    if (linkAddresses != null) {
                        for (LinkAddress linkAddress : linkAddresses) {
                            logger.debug("ActiveNetworkInfo Flags::{}, PrefixLength::{}, Scope::{}",
                                    linkAddress.getFlags(), linkAddress.getPrefixLength(), linkAddress.getScope());
                            InetAddress address = linkAddress.getAddress();
                            if (isIPv4(address)) {
                                String ipv4 = address.getHostAddress();
                                networkAddress = ipv4 + ":0";
                                logger.debug("ActiveNetworkInfo IPv4 HostAddress::{}", ipv4);
                            } else {
                                String ipv6 = address.getHostAddress();
                                if (StringUtil.isEmpty(networkAddress)) {
                                    // 需要加中括号，代表地址（地址可能用::被省略）
                                    networkAddress = "[" + ipv6 + "]:0";
                                }
                                logger.debug("ActiveNetworkInfo IPv6 HostAddress::{}, isIPv6ULA::{}",
                                        ipv6, isIPv6ULA(address));
                            }
                            logger.debug("ActiveNetworkInfo ****************************************");
                        }
                    }
                }
            }
        }
        return networkAddress;
    }

    /**
     * 获取本机IPv4地址
     *
     * @return 本机IPv4地址；null：无网络连接
     */
    public String getIpAddress() {
        // TODO: 测试，以后可删除
        getNetworkAddress();
        // 获取WiFi服务
        String ipv4;
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        // 判断WiFi是否开启
        logger.debug("getIpAddress isWifiConnected::{}", wifiNetworkInfo.isConnected());
        if (wifiNetworkInfo.isConnected()) {
            // 已经开启了WiFi
            WifiManager wifiManager = (WifiManager) appContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            ipv4 = intToIp(ipAddress);
        } else {
            // 未开启WiFi
            ipv4 = getLocalIpAddress();
        }
        ipv4 += ":0";
        logger.debug("getIpAddress ipv4::{}", ipv4);
        return ipv4;
    }

    private String intToIp(int ipAddress) {
        return (ipAddress & 0xFF) + "." +
                ((ipAddress >> 8) & 0xFF) + "." +
                ((ipAddress >> 16) & 0xFF) + "." +
                (ipAddress >> 24 & 0xFF);
    }

    /**
     * 获取本地电信的IP地址
     */
    private String getLocalIpAddress() {
        try {
            String ipv4;
            ArrayList<NetworkInterface> list = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni: list) {
                ArrayList<InetAddress> addresses = Collections.list(ni.getInetAddresses());
                for (InetAddress address: addresses) {
                    // 不是环回地址和链接本地地址
                    if (!address.isLoopbackAddress() && !address.isLinkLocalAddress() && isIPv4(address)) {
                        ipv4 = address.getHostAddress();
                        return ipv4;
                    }
                }
            }
        } catch (SocketException ignore) {
        }
        return null;
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
