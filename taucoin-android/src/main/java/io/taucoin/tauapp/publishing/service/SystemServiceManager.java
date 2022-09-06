package io.taucoin.tauapp.publishing.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.util.SparseArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;

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
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
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
            return newNetworkInfo.getType();
        } else {
            logger.debug("network type::-1");
            return -1;
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
     * 当前网络是否是WiFi
     * @return boolean
     */
    public boolean isWiFi() {
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo != null && networkInfo.isConnected();
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
     * 获取电池电量
     */
    public int getBatteryLevel() {
        //创建过滤器拦截电量改变广播
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        //通过过滤器来获取电量改变intent 电量改变是系统广播所以无需去设置所以receiver传null即可
        Intent intent = appContext.registerReceiver(null, intentFilter);
        int level = 0;
        if(intent != null){
            //获取电量信息
            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        }
        logger.debug("battery changed::{}", level);
        return level;
    }

    /**
     * 通过NetworkInterface.getNetworkInterfaces()获取解析整个设备的网络地址
     * @param ipv4List List<String>
     * @param ipv6List List<String>
     */
    private void parseAllNetworks(List<String> ipv4List, List<String> ipv6List) {
        try {
            boolean isAddIPAddress = ipv4List.isEmpty();
            // 防止统计重复
            if (isAddIPAddress) {
                ipv6List.clear();
            }
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            int count = 0;
            while (en.hasMoreElements()) {
                count += 1;
                NetworkInterface intf = en.nextElement();
                logger.debug("testAllNetworks count::{}, Networks::{}", count, intf.toString());

                List<InterfaceAddress> interfaceAddresses = intf.getInterfaceAddresses();
                StringBuilder address = new StringBuilder();
                for (InterfaceAddress ia : interfaceAddresses) {
                    InetAddress inetAddress = ia.getAddress();
                    if (inetAddress != null && !inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                        String ipAddress = inetAddress.getHostAddress();
                        address.append(", ").append(ipAddress);

                        if (isAddIPAddress) {
                            if (isIPv4(inetAddress)) {
                                ipv4List.add(ipAddress);
                            } else {
                                ipv6List.add(ipAddress);
                            }
                        }
                    }
                }
                logger.debug("testAllNetworks count::{}, Networks::{}, address::{}",
                        count, intf.toString(), address.toString());
            }
            logger.debug("testAllNetworks IPv4 size::{}, IPv6 size::{}, isAddIPAddress::{}",
                    ipv4List.size(), ipv6List.size(), isAddIPAddress);
        } catch (SocketException e) {
            logger.debug("testAllNetworks error::", e);
        }
    }

    /**
     * 获取网络地址
     * 1、获取当前ActiveNetworkInfo的type，
     * 2、遍历所有的Networks，满足type和第一步中的type相等 && isConnected()的从中间看是否有ipv4
     */
    public SparseArray<List<String>> getNetworkAddress() {
        Network[] networks = connectivityManager.getAllNetworks();
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        List<String> ipv4List = new ArrayList<>();
        List<String> ipv6List = new ArrayList<>();
        if (networks != null && activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            int activeType = activeNetworkInfo.getType();
            logger.debug("ActiveNetworkInfo::{}, ActiveType::{}, AllNetworks::{}", activeNetworkInfo.toString(),
                    activeType, networks.length);
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
                                        ipv4List.add(ipv4);
                                    }
                                    logger.debug("NetworkInfo IPv4 HostAddress::{}", ipv4);
                                } else {
                                    String ipv6 = address.getHostAddress();
                                    if (isActive && !address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                                        // 需要加中括号，代表地址（地址可能用::被省略）
                                        ipv6List.add(ipv6);
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
        logger.debug("NetworkInfo IPv4 size::{}, IPv6 size::{}", ipv4List.size(), ipv6List.size());
        parseAllNetworks(ipv4List, ipv6List);
        SparseArray<List<String>> ipList = new SparseArray<>();
        ipList.append(4, ipv4List);
        ipList.append(6, ipv6List);
        return ipList;
    }

    /**
     * 获取当前ActiveNetworkInfo的IPv4
     */
    @Deprecated
    public String getActiveNetworkAddress() {
        String ipv4 = "";
        Network[] networks = connectivityManager.getAllNetworks();
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (networks != null && activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            for (Network network : networks) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                if (null == networkInfo) {
                    continue;
                }
                if (StringUtil.isEquals(networkInfo.toString(), activeNetworkInfo.toString())) {
                    LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
                    if (linkProperties != null) {
                        List<LinkAddress> linkAddresses = linkProperties.getLinkAddresses();
                        if (linkAddresses != null) {
                            for (LinkAddress linkAddress : linkAddresses) {
                                InetAddress address = linkAddress.getAddress();
                                if (address != null) {
                                    if (isIPv4(address) && !address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                                        ipv4 = address.getHostAddress();
                                        logger.debug("ActiveNetworkAddress IPv4::{}", ipv4);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return ipv4;
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
