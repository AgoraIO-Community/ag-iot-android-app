package io.agora.iotlinkdemo.utils;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.agora.baselibrary.base.BaseApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * wifi 工具类
 */
public class WifiUtils {
    /**
     * 获取wifi 列表
     */
    public static ArrayList<ScanResult> getWifiList() {
        WifiManager wifiManager = (WifiManager) BaseApplication.mInstance.getSystemService(AppCompatActivity.WIFI_SERVICE);
        ArrayList<ScanResult> scanWifiList = new ArrayList<>();
        HashMap<String, Integer> hashMap = new HashMap<>();
        List<ScanResult> scanResults = wifiManager.getScanResults();
        if (scanResults != null && !scanResults.isEmpty()) {
            for (int i = 0; i < scanResults.size(); i++) {
                String ssid = scanResults.get(i).SSID;
                if (!TextUtils.isEmpty(ssid) && hashMap.get(scanResults.get(i).SSID) == null) {
                    //去重
                    hashMap.put(ssid, 1);
                    scanWifiList.add(scanResults.get(i));
                }
            }
        }
        return scanWifiList;
    }

}
