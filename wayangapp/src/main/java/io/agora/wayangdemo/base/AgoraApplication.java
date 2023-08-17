package io.agora.wayangdemo.base;

import android.app.Activity;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.Log;

import io.agora.iotlink.SdkWayangFactory;
import io.agora.sdkwayang.SdkWayangComp;
import io.agora.sdkwayang.logger.crashDeal.CrashLogDeal;
import io.agora.wayangdemo.BuildConfig;
import com.alibaba.android.arouter.launcher.ARouter;

import io.agora.wayangdemo.utils.AppStorageUtil;
import me.jessyan.autosize.AutoSizeConfig;
import me.jessyan.autosize.onAdaptListener;
import me.jessyan.autosize.utils.ScreenUtils;

public class AgoraApplication extends PushApplication {
    private static final String TAG = "IOTWY/AgoraApp";

    public String mServerUrl = "ws://114.236.93.153:8083/iov/websocket/dual?topic=";
    public String mDeviceInfo = "LXH0001";

//    private SignalHandle signalHandle = null;
    private boolean isRegister =false;
    CrashLogDeal crashHandler = null;


    //////////////////////////////////////////////////////////////////
    ////////////////////// Public Methods ///////////////////////////
    //////////////////////////////////////////////////////////////////

    public void onCreate() {
        super.onCreate();
        AppStorageUtil.init(this);

        String serverUrl = AppStorageUtil.safeGetString(this, AppStorageUtil.KEY_SRVURL, null);
        String deviceInfo = AppStorageUtil.safeGetString(this, AppStorageUtil.KEY_DEVINFO, null);

        if (!TextUtils.isEmpty(serverUrl)) {
            mServerUrl = serverUrl;
        } else {
            AppStorageUtil.safePutString(this, AppStorageUtil.KEY_SRVURL, mServerUrl);
            Log.d(TAG, "<onCreate> use default server url: " + mServerUrl);
        }

        if (!TextUtils.isEmpty(deviceInfo)) {
            mDeviceInfo = deviceInfo;
        } else {
            AppStorageUtil.safePutString(this, AppStorageUtil.KEY_DEVINFO, mDeviceInfo);
            Log.d(TAG, "<onCreate> use default device info: " + mDeviceInfo);
        }

        int ret = SdkWayangFactory.getInstance().initialize(this, mServerUrl, mDeviceInfo);

        initARouter();
        initAutoSize();
    }

    private void initARouter() {
        if (BuildConfig.DEBUG) {
            ARouter.openLog();
            ARouter.openDebug();
        }

        ARouter.init(this);
    }

    private void initAutoSize() {
        AutoSizeConfig.getInstance().setOnAdaptListener(new onAdaptListener() {
            @Override
            public void onAdaptBefore(Object o, Activity activity) {
                AutoSizeConfig.getInstance().setScreenWidth(ScreenUtils.getScreenSize(activity)[0]);
                AutoSizeConfig.getInstance().setScreenHeight(ScreenUtils.getScreenSize(activity)[1]);
                if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    AutoSizeConfig.getInstance()
                            .setDesignWidthInDp(812)
                            .setDesignHeightInDp(375);
                } else {
                    AutoSizeConfig.getInstance()
                            .setDesignWidthInDp(375)
                            .setDesignHeightInDp(812);
                }
            }

            @Override
            public void onAdaptAfter(Object o, Activity activity) {

            }
        });
    }



    public void initCrashHandler(){
        crashHandler = CrashLogDeal.getInstance();
        crashHandler.init(getApplicationContext());
//        signalHandle = new SignalHandle();
//        if(!isRegister){
//            signalHandle.registerSignal();
//            isRegister = true;
//        }

    }

    public synchronized void uninstallCrashHandler() {
//        if(isRegister) {
//            signalHandle.unRegisterSignal();
//            isRegister = false;
//        }
        crashHandler.unregisterANRReceiver();
        crashHandler.releaseContext();
        crashHandler = null;
    }
}