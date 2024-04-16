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

import io.agora.falcondemo.common.Constant;
import io.agora.falcondemo.models.home.MainActivity;
import io.agora.falcondemo.thirdpartyaccount.ThirdAccountMgr;
import io.agora.falcondemo.utils.AppStorageUtil;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IConnectionObj;
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

    private IConnectionObj mFullscrnConnectObj = null;             ///< 全屏时的连接对象

    private MainActivity mMainActivity = null;

    private volatile int mUiPage = Constant.UI_PAGE_WELCOME;

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
     * @brief 设置 全屏播放的 连接对象
     */
    public void setFullscrnConnectionObj(final IConnectionObj connectObj) {
        synchronized (mDataLock) {
            mFullscrnConnectObj = connectObj;
        }
    }

    /**
     * @brief 获取 全屏播放的 连接对象
     */
    public IConnectionObj getFullscrnConnectionObj() {
        synchronized (mDataLock) {
            return mFullscrnConnectObj;
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


    public int ThirdAccountMgrInit(final String appId) {

        ThirdAccountMgr.getInstance();

        return ErrCode.XOK;
    }



    /**
     * @brief 设置/获取当前显示界面
     */
    public int getUiPage() {
        return mUiPage;
    }

    public void setUiPage(int uiPage) {
        mUiPage = uiPage;
        Log.d(TAG, "<setUiPage> uiPage=" + uiPage);
    }
}