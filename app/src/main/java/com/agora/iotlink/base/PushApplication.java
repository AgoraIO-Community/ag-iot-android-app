/**
 * @file PushApplication.java
 * @brief This file implement the application entry
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2021-10-13
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package com.agora.iotlink.base;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import com.agora.baselibrary.base.BaseApplication;
import com.agora.iotlink.utils.AppStorageUtil;
import com.agora.iotsdk20.AIotAppSdkFactory;
import com.agora.iotsdk20.IAgoraIotAppSdk;
import com.agora.iotsdk20.IotDevice;
import com.agora.iotsdk20.utils.PreferenceManager;

import java.util.List;

/**
 * 原封引用sdk demo中的代码
 */
public class PushApplication extends BaseApplication {
    private static PushApplication instance = null;
    private static ActivityLifecycleCallback mLifeCycleCallbk = new ActivityLifecycleCallback();


    private Bundle mMetaData = null;
    private volatile boolean mIotAppSdkReady = false;       ///< SDK是否已经就绪

    private volatile int mAudCodecIndex = 12;   // 12 means opus, it's default value

    private IotDevice mLivingDevice;    ///< 当前正在通话的设备

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
        super.onCreate();

        instance = this;

        AppStorageUtil.init(this);

        //偏好设置初始化
        PreferenceManager.init(this);

        //仅主进程运行一次
        if (isMainProcess(this)) {
            //获取applicationInfo标签内的数据
            try {
                PackageManager packageManager = this.getPackageManager();
                ApplicationInfo applicationInfo =
                        packageManager.getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
                mMetaData = applicationInfo.metaData;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return;
            }
        }

        //注册Activity回调
//        registerActivityLifecycleCallbacks(mLifeCycleCallbk);
    }


    //判断是否在主进程
    private boolean isMainProcess(Context context) {
        int pid = Process.myPid();
        String pkgName = context.getApplicationInfo().packageName;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcList = activityManager.getRunningAppProcesses();

        for (ActivityManager.RunningAppProcessInfo appProcess : runningProcList) {
            if (appProcess.pid == pid) {
                return (pkgName.compareToIgnoreCase(appProcess.processName) == 0);
            }
        }

        return false;
    }


    public void setAudioCodecIndex(int index) {
        mAudCodecIndex = index;
    }

    public int getAudioCodecIndex() {
        return mAudCodecIndex;
    }

    public void initializeEngine() {
        if (mIotAppSdkReady) {
            return;
        }

        //
        //初始化IotAppSdk2.0 引擎
        //
        IAgoraIotAppSdk.InitParam initParam = new IAgoraIotAppSdk.InitParam();
        initParam.mContext = this;
        initParam.mRtcAppId = mMetaData.getString("AGORA_APPID", "");
        initParam.mProjectID = mMetaData.getString("PROJECT_ID", "");
        initParam.mMasterServerUrl = mMetaData.getString("MASTER_SERVER_URL", "");
        initParam.mSlaveServerUrl = mMetaData.getString("SALVE_SERVER_URL", "");

        //String storageRootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        //initParam.mLogFilePath = storageRootPath + "/callkit.log";
        initParam.mPublishVideo = false;
        initParam.mPublishAudio = false;
        initParam.mSubscribeAudio = true;
        initParam.mSubscribeVideo = true;

        int ret = AIotAppSdkFactory.getInstance().initialize(initParam);

        //初始化离线/后台运行通知
        EaseNotifier.getInstance().init(this, mMetaData);

        mIotAppSdkReady = true;
    }

    public void setLivingDevice(IotDevice iotDevice) {
        mLivingDevice = iotDevice;
    }

    public IotDevice getLivingDevice() {
        return mLivingDevice;
    }


}