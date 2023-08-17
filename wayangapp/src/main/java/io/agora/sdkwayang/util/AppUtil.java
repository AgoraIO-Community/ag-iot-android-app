package io.agora.sdkwayang.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

import io.agora.sdkwayang.logger.WLog;


public class AppUtil {
    final static String TAG = "IOTWY/AppUtil";

    public static final String OS = "Android";
    public static final String OS_VERSION = android.os.Build.VERSION.RELEASE;
    public static final String MANUFACTURER = android.os.Build.MANUFACTURER;
    public static final String MODEL = android.os.Build.MODEL;
    public static final int API_LEVEL = android.os.Build.VERSION.SDK_INT;

    public static final String APP_DIRECTORY =  Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/io.agora.wayangdemo/files";


    public static boolean isValidJsonString(String json) {
        try {
            new JSONObject(json);
        } catch (JSONException ex) {
            try {
                new JSONArray(json);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    public static String getDeviceID(Context context) {
        // XXX according to the API docs, this value may change after factory reset
        // use Android id as device id
        return Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    }



    private static byte[] readInputStream(InputStream inStream) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        byte[] data = outStream.toByteArray();
        outStream.close();
        inStream.close();
        return data;
    }


    public static boolean isWifiConnected(Context context) {
        if (context != null) {
            ConnectivityManager connMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (wifi == null) {
                WLog.getInstance().w(TAG, "current device does not support WIFI data connection"); // such as some IOT devices
                return false;
            }

            if (wifi.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                WLog.getInstance().d(TAG,"isWifiConnected: true");
                return true;
            } else {
                WLog.getInstance().d(TAG,"isWifiConnected: false " + wifi.getDetailedState());
                return false;
            }
        }
        return false;
    }

    public static boolean isCellularDataConnected(Context context) {
        if (context != null) {
            ConnectivityManager connMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if (mobile == null) {
                WLog.getInstance().w(TAG, "current device does not support Cellular data connection"); // such as some tablets
                return false;
            }

            if (mobile.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                WLog.getInstance().d(TAG, "isCellularDataConnected: true");
                return true;
            } else {
                WLog.getInstance().d(TAG, "isCellularDataConnected: false " + mobile.getDetailedState());
                return false;
            }
        }
        return false;
    }

    public static boolean isEtherConnected(Context context) {
        if (context != null) {
            ConnectivityManager connMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ether = connMgr.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

            if (ether == null) {
                WLog.getInstance().w(TAG, "current device does not support Ether data connection");
                return false;
            }

            if (ether.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                WLog.getInstance().d(TAG, "isEtherConnected: true");
                return true;
            } else {
                WLog.getInstance().d(TAG, "isEtherConnected: false " + ether.getDetailedState());
                return false;
            }
        }
        return false;
    }

    //往字符串数组追加新数据
    public static String[] concat(String[] a, String[] b) {
        String[] c= new String[a.length+b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

}
