/**
 * @file PushApplication.java
 * @brief This file implement the application entry
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2021-10-13
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlinkdemo.base;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import com.agora.baselibrary.base.BaseApplication;
//import io.agora.iotlinkdemo.huanxin.EmAgent;
import io.agora.iotlink.IDeviceMgr;
import io.agora.iotlinkdemo.huanxin.EmAgent;
import io.agora.iotlinkdemo.utils.AppStorageUtil;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.IotDevice;
import io.agora.iotlink.utils.PreferenceManager;

import java.util.List;

/**
 * 原封引用sdk demo中的代码
 */
public class PushApplication extends BaseApplication {
    private static final String TAG = "LINK/PushApp";
    private static PushApplication instance = null;
    private static ActivityLifecycleCallback mLifeCycleCallbk = new ActivityLifecycleCallback();


    private Bundle mMetaData = null;
    private volatile boolean mIotAppSdkReady = false;       ///< SDK是否已经就绪

    private volatile int mAudCodecIndex = 12;   // 12 means opus, it's default value

    private IotDevice mLivingDevice;    ///< 当前正在通话的设备
    private IDeviceMgr.McuVersionInfo mLivingMcuVersion;    ///< 当前正在通话设备的固件版本信息

    //////////////////////////////////////////////////////////////////
    ////////////////////// Public Methods ///////////////////////////
    //////////////////////////////////////////////////////////////////

    //获取APP单例对象
    public static PushApplication getInstance() {
        return instance;
    }

    //获取活动页面生命期回调
    public static ActivityLifecycleCallback getLifecycleCallbacks() {
        return mLifeCycleCallbk;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "<onCreate> ==>Enter");
        super.onCreate();

        instance = this;

        AppStorageUtil.init(this);

        //偏好设置初始化
        PreferenceManager.init(this);

        //注册Activity回调
        registerActivityLifecycleCallbacks(mLifeCycleCallbk);

        //仅主进程运行一次
        if (isMainProcess(this)) {
            //获取applicationInfo标签内的数据
            try {
                PackageManager packageManager = this.getPackageManager();
                ApplicationInfo applicationInfo =
                        packageManager.getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
                mMetaData = applicationInfo.metaData;
                Log.d(TAG, "<onCreate> get meta data");

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return;
            }
        }
        Log.d(TAG, "<onCreate> <==Exit");
    }


    //判断是否在主进程
    private boolean isMainProcess(Context context) {
        int pid = Process.myPid();
        String pkgName = context.getApplicationInfo().packageName;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = Application.getProcessName();
            return (pkgName.compareToIgnoreCase(processName) == 0);

        } else {
            ActivityManager activityManager = (ActivityManager)context.getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningProcList = activityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo appProcess : runningProcList) {
                if (appProcess.pid == pid) {
                    return (pkgName.compareToIgnoreCase(appProcess.processName) == 0);
                }
            }
        }

        return false;
    }

    public void initializeEngine() {
        if (mIotAppSdkReady) {
            return;
        }

        //
        // 初始化环信的离线推送
        //
//        if (mMetaData != null)
//        {
//            EmAgent.EmPushParam  pushParam = new EmAgent.EmPushParam();
//            pushParam.mFcmSenderId = mMetaData.getString("com.fcm.push.senderid", "");
//            pushParam.mMiAppId = mMetaData.getString("com.mi.push.app_id", "");
//            pushParam.mMiAppKey = mMetaData.getString("com.mi.push.api_key", "");
//            pushParam.mOppoAppKey = mMetaData.getString("com.oppo.push.api_key", "");
//            pushParam.mOppoAppSecret = mMetaData.getString("com.oppo.push.app_secret", "");;
//            pushParam.mVivoAppId = String.valueOf(mMetaData.getInt("com.vivo.push.app_id", 0));
//            pushParam.mVivoAppKey = mMetaData.getString("com.vivo.push.api_key", "");
//            pushParam.mHuaweiAppId = mMetaData.getString("com.huawei.hms.client.appid", "");
//            EmAgent.getInstance().initialize(this,  pushParam);
//        }

        //
        //初始化IotAppSdk2.0 引擎
        //
        IAgoraIotAppSdk.InitParam initParam = new IAgoraIotAppSdk.InitParam();
        initParam.mContext = this;
        initParam.mRtcAppId = mMetaData.getString("AGORA_APPID", "");
        initParam.mProjectID = mMetaData.getString("PROJECT_ID", "");
        initParam.mMasterServerUrl = mMetaData.getString("MASTER_SERVER_URL", "");
        initParam.mSlaveServerUrl = mMetaData.getString("SALVE_SERVER_URL", "");
        //initParam.mPusherId = EmAgent.getInstance().getEid();  // 设置离线推送Id
        initParam.mPublishVideo = false;
        initParam.mPublishAudio = false;
        initParam.mSubscribeAudio = true;
        initParam.mSubscribeVideo = true;
        //String storageRootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        //initParam.mLogFilePath = storageRootPath + "/callkit.log";

        int ret = AIotAppSdkFactory.getInstance().initialize(initParam);

        mIotAppSdkReady = true;
    }

    public void setLivingDevice(IotDevice iotDevice) {
        mLivingDevice = iotDevice;
    }

    public IotDevice getLivingDevice() {
        return mLivingDevice;
    }

    public void setLivingMcuVersion(IDeviceMgr.McuVersionInfo mcuVersion) {
        mLivingMcuVersion = mcuVersion;
    }

    public IDeviceMgr.McuVersionInfo getLivingMcuVersion() {
        return mLivingMcuVersion;
    }

}