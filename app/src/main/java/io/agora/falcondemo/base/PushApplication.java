/**
 * @file PushApplication.java
 * @brief This file implement the application entry
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2021-10-13
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.falcondemo.base;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.agora.baselibrary.base.BaseApplication;

import io.agora.falcondemo.models.home.MainActivity;
import io.agora.falcondemo.thirdpartyaccount.ThirdAccountMgr;
import io.agora.falcondemo.utils.AppStorageUtil;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.utils.PreferenceManager;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * 原封引用sdk demo中的代码
 */
public class PushApplication extends BaseApplication {
    private static final String TAG = "IOTLINK/PushApp";
    private static PushApplication instance = null;
    private static ActivityLifecycleCallback mLifeCycleCallbk = new ActivityLifecycleCallback();
    private static final Object mDataLock = new Object();       ///< 同步访问锁,类中所有变量需要进行加锁处理


    private Bundle mMetaData = null;
    private volatile boolean mIotAppSdkReady = false;       ///< SDK是否已经就绪

    private boolean mIsChkedOverlayWnd = false;         ///< 是否检测过一次悬浮窗权限

    private UUID mFullscrnSessionId = null;             ///< 全屏时的 sessionId

    private MainActivity mMainActivity = null;

    //////////////////////////////////////////////////////////////////
    ////////////////////// Public Methods ///////////////////////////
    //////////////////////////////////////////////////////////////////

    //获取APP单例对象
    public static PushApplication getInstance() {
        return instance;
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

    /**
     * @brief 判断是否在主进程
     */
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

    public void initializeEngine(final String appId) {
        if (mIotAppSdkReady) {
            return;
        }

        String serverUrl = "https://api.sd-rtn.com/agoralink/cn/api";

        //
        //初始化IotAppSdk2.0 引擎
        //
        IAgoraIotAppSdk.InitParam initParam = new IAgoraIotAppSdk.InitParam();
        initParam.mContext = this;
        initParam.mAppId = appId;
        initParam.mServerUrl = serverUrl;

        File file = this.getExternalFilesDir(null);
        String cachePath = file.getAbsolutePath();
        initParam.mLogFilePath = cachePath + "/callkit.log";
        initParam.mStateListener = new IAgoraIotAppSdk.OnSdkStateListener() {
            @Override
            public void onSdkStateChanged(int oldSdkState, int newSdkState, int reason) {
                ALog.getInstance().d(TAG, "<initializeEngine.onSdkStateChanged> oldSdkState=" + oldSdkState
                        + ", newSdkState=" + newSdkState + ", reason=" + reason);
                MainActivity mainActivity = instance.getMainActivity();
                if (mainActivity == null) {
                    return;
                }
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.onSdkStateChanged(oldSdkState, newSdkState, reason);
                    }
                });
            }
        };
        int ret = AIotAppSdkFactory.getInstance().initialize(initParam);

        //
        // 设置第三方账号服务器地址
        //
        ThirdAccountMgr.getInstance().setAccountServerUrl(serverUrl);

        mIotAppSdkReady = true;
    }


    public boolean isChkedOverlayWnd() {
        return mIsChkedOverlayWnd;
    }

    public void SetChkedOverlayWnd(boolean checked) {
        mIsChkedOverlayWnd = checked;
    }

    public Bundle getMetaData() {
        return mMetaData;
    }

    /**
     * @brief 设置 全屏播放的 sessionId
     */
    public void setFullscrnSessionId(final UUID sessionId) {
        synchronized (mDataLock) {
            mFullscrnSessionId = sessionId;
        }
    }

    /**
     * @brief 获取 全屏播放的 sessionId
     */
    public UUID getFullscrnSessionId() {
        synchronized (mDataLock) {
            return mFullscrnSessionId;
        }
    }

    /**
     * @brief 设置 主Activity
     */
    public void setMainActivity(final MainActivity mainActivity) {
        mMainActivity = mainActivity;
    }

    /**
     * @brief 获取 主Activity
     */
    public MainActivity getMainActivity() {
        return mMainActivity;
    }

}