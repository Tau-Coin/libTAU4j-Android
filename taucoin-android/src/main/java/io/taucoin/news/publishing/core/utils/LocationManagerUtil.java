package io.taucoin.news.publishing.core.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.model.TauDaemon;
import io.taucoin.news.publishing.ui.customviews.permission.EasyPermissions;

/**
 * 原生定位管理工具
 */
public class LocationManagerUtil {
    private static final Logger logger = LoggerFactory.getLogger("LocationManagerUtil");
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Context appContext;
    private double longitude;
    private double latitude;
    public static LocationManagerUtil instance;

    public LocationManagerUtil(Context appContext) {
        this.appContext = appContext;
        locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {

            // Provider的状态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                logger.debug("onStatusChanged::{}, status::{}", provider, status);
            }
            // Provider被enable时触发此函数，比如GPS被打开
            @Override
            public void onProviderEnabled(String provider) {
                logger.debug("onProviderEnabled::{}", provider);
            }
            // Provider被disable时触发此函数，比如GPS被关闭
            @Override
            public void onProviderDisabled(String provider) {
                logger.debug("onProviderDisabled::{}", provider);
            }
            //当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    logger.debug("location listener Provider::{}, Longitude::{}, Latitude::{}",
                            location.getProvider(), location.getLongitude(), location.getLatitude());
                    updateToNewLocation(location);
                }
            }
        };
    }

    public boolean isNeedPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) ||
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    /**
     * 开始定位
     */
    public void startLocation() {
        if (null == locationManager) {
            return;
        }
        logger.debug("startLocation");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) ||
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = null;
        List<String> providers = locationManager.getProviders(true);
        logger.debug("providers.size::{}", null == providers ? 0 : providers.size());
        if (providers != null) {
            for (String provider: providers) {
                Location l = locationManager.getLastKnownLocation(provider);
                logger.debug("provider::{}, enabled::{}, location==null::{}", provider,
                        locationManager.isProviderEnabled(provider), null == l);
                if (null == l) {
                    continue;
                }
                logger.debug("provider::{}, last known location Longitude::{}, Latitude::{}",
                        provider, l.getLongitude(), l.getLatitude());
                if (location == null || l.getAccuracy() < location.getAccuracy()) {
                    location = l;
                }
            }
        }
        if (location != null) {
            updateToNewLocation(location);
        } else {
            // 监视地理位置变化，第二个和第三个参数分别为更新的最短时间minTime和最短距离minDistance
            long minTime = 0;
            long minDistance = 0;
            locationManager.removeUpdates(locationListener);
            if (providers != null) {
                for (String provider: providers) {
                    locationManager.requestLocationUpdates(provider, minTime, minDistance, locationListener);
                    logger.debug("requestLocationUpdates provider::{}", provider);
                }
            }
            logger.debug("request Location Updates...");
        }
    }

    private void updateToNewLocation(Location location) {
        if (location != null && locationManager != null) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            longitude = FmtMicrometer.formatDecimal(longitude, 6);
            latitude = FmtMicrometer.formatDecimal(latitude, 6);
            TauDaemon.getInstance(appContext).updateCurrentUserInfo(true);
            logger.debug("update location provider::{}, Longitude::{}, Latitude::{}",
                    location.getProvider(), location.getLongitude(), location.getLatitude());

            if (locationManager != null) {
                locationManager.removeUpdates(locationListener);
            }
        }
    }

    /**
     * 停止定位
     */

    public void stopLocation() {
        logger.debug("stopLocation");
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
            locationManager = null;
        }
        if (locationListener != null) {
            locationListener = null;
        }
    }

    /**
     * 请求定位权限
     */
    public static void requestLocationPermissions(Activity activity) {
        String finePermission = Manifest.permission.ACCESS_FINE_LOCATION;
        String coarsePermission = Manifest.permission.ACCESS_COARSE_LOCATION;
        if(!EasyPermissions.hasPermissions(activity, finePermission, coarsePermission)){
            EasyPermissions.requestPermissions(activity,
                    activity.getString(R.string.permission_tip_location_denied),
                    PermissionUtils.REQUEST_PERMISSIONS_LOCATION, finePermission, coarsePermission);
        } else {
            TauDaemon.getInstance(activity.getApplicationContext()).startLocation();
        }
    }

    public static void onRequestPermissionsResult(AppCompatActivity activity, int requestCode,
                                                  @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtils.REQUEST_PERMISSIONS_LOCATION) {
            if (grantResults.length > 0) {
                List<String> list = new ArrayList<>();
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        list.add(permissions[i]);
                    }
                }
                if (list.size() > 0) {
                    PermissionUtils.checkUserBanPermission(activity, (dialog, which) -> { },
                            list, R.string.permission_tip_location_never_ask_again);
                } else {
                    TauDaemon.getInstance(activity.getApplicationContext()).startLocation();
                }
            }
        }
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }
}
