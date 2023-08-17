/**
 * @file PushApplication.java
 * @brief This file implement the application entry
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2021-10-13
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.wayangdemo.base;

import android.app.ActivityManager;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.agora.baselibrary.base.BaseApplication;
//import io.agora.wayangdemo.huanxin.EmAgent;
import io.agora.sdkwayang.logger.crashDeal.CrashLogDeal;
import io.agora.wayangdemo.huanxin.EmAgent;
import io.agora.wayangdemo.utils.AppStorageUtil;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.utils.PreferenceManager;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * 原封引用sdk demo中的代码
 */
public class PushApplication extends BaseApplication {
    private static final String TAG = "IOTWY/PushApp";
    private static PushApplication instance = null;
    private static ActivityLifecycleCallback mLifeCycleCallbk = new ActivityLifecycleCallback();
    private static final Object mDataLock = new Object();       ///< 同步访问锁,类中所有变量需要进行加锁处理


    private Bundle mMetaData = null;
    private volatile boolean mIotAppSdkReady = false;       ///< SDK是否已经就绪

    private boolean mIsChkedOverlayWnd = false;         ///< 是否检测过一次悬浮窗权限

    private UUID mFullscrnSessionId = null;             ///< 全屏时的 sessionId


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

}