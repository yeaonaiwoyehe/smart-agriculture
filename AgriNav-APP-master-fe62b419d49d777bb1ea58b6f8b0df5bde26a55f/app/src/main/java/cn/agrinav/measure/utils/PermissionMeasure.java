package cn.agrinav.measure.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.util.Log;

import cn.agrinav.R;

/**
 * @author 18506
 */
public class PermissionMeasure {
    public static final String[] PERMISSIONS_LOCATION = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION};
    public static boolean isAllowWifi(Activity activity) {
        WifiManager wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        Log.i(activity.getResources().getString(R.string.trajectory_tag),"当前wifi允许状态："+wifiManager.isWifiEnabled());
        return wifiManager.isWifiEnabled();
    }
    public static boolean isAllowTargeting(Activity activity) {
        LocationManager locationManager = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetWorkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.i(activity.getResources().getString(R.string.trajectory_tag),"当前gps与网络定位状态："+isGpsEnabled+","+isNetWorkEnabled);
        return isGpsEnabled && isNetWorkEnabled;
    }
}
