/**
 * @file IAgoraIotAppSdk.java
 * @brief This file define the SDK interface for Agora Iot AppSdk 2.0
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.sdkimpl;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;


import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAccountMgr;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.IAlarmMgr;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.IDevMessageMgr;
import io.agora.iotlink.IDeviceMgr;
import io.agora.iotlink.IRtcPlayer;
import io.agora.iotlink.IRtmMgr;
import io.agora.iotlink.aws.AWSUtils;
import io.agora.iotlink.callkit.AgoraService;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.lowservice.AgoraLowService;
import io.agora.iotlink.rtcsdk.TalkingEngine;

import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;



/**
 * @brief SDK引擎接口
 */
public class AgoraIotAppSdk implements IAgoraIotAppSdk {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/AgoraIotAppSdk";
    private static final int EXIT_WAIT_TIMEOUT = 3000;

    //
    // The mesage Id
    //
    private static final int MSGID_WORK_EXIT = 0xFFFF;



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private InitParam mInitParam;
    private AccountMgr mAccountMgr;
    private CallkitImpl mCallkitImpl;
    private DeviceMgr mDeviceMgr;
    private AlarmMgr mAlarmMgr;
    private DevMessageMgr mDevmsgMgr;
    private RtmMgr mRtmMgr;
    private RtcPlayer mRtcPlayer;

    public static final Object mDataLock = new Object();    ///< 同步访问锁,类中所有变量需要进行加锁处理
    private HandlerThread mWorkThread;  ///< 呼叫系统和设备管理器的工作线程，在该线程中串行运行
    private Handler mWorkHandler;       ///< 呼叫系统和设备管理器的工作线程处理器
    private final Object mWorkExitEvent = new Object();
    private ThreadPoolExecutor mThreadPool; ///< 设备消息和告警消息的工作线程池，支持并发执行

    private volatile int mStateMachine = AgoraIotAppSdk.SDK_STATE_INVALID;     ///< 当前呼叫状态机


    ///////////////////////////////////////////////////////////////////////
    //////////////// Override Methods of IAgoraIotAppSdk //////////////////
    ///////////////////////////////////////////////////////////////////////

    @Override
    public int initialize(InitParam initParam) {
        mInitParam = initParam;

        //
        // 初始化日志系统
        //
        if ((initParam.mLogFilePath != null) && (!initParam.mLogFilePath.isEmpty())) {
            boolean logRet = ALog.getInstance().initialize(initParam.mLogFilePath);
            if (!logRet) {
                Log.e(TAG, "<initialize > [ERROR] fail to initialize logger");
            }
        }

        // 设置基本的BaseUrl
        if (initParam.mSlaveServerUrl != null) {
            AgoraService.getInstance().setBaseUrl(initParam.mSlaveServerUrl);
        }
        if (initParam.mMasterServerUrl != null) {
            AgoraLowService.getInstance().setBaseUrl(initParam.mMasterServerUrl);
        }

        //
        // 启动工作线程
        //
        workThreadCreate();

        //
        // 创建接口实例对象
        //
        mAccountMgr = new AccountMgr();
        mAccountMgr.initialize(this);

        mDeviceMgr = new DeviceMgr();
        mDeviceMgr.initialize(this);

        mCallkitImpl = new CallkitImpl();
        mCallkitImpl.initialize(this);

        mAlarmMgr = new AlarmMgr();
        mAlarmMgr.initialize(this);

        mDevmsgMgr = new DevMessageMgr();
        mDevmsgMgr.initialize(this);

        mRtmMgr = new RtmMgr();
        mRtmMgr.initialize(this);

        mRtcPlayer = new RtcPlayer();
        mRtcPlayer.initialize(this);

        //
        // 设置AwsUtil的回调
        //
        AWSUtils.getInstance().setAWSListener(new AWSUtils.AWSListener() {
            @Override
            public void onConnectStatusChange(String status) {
                ALog.getInstance().d(TAG, "<onConnectStatusChange> status=" + status);

                // 账号管理系统会做 登录和登出处理
                mAccountMgr.onAwsConnectStatusChange(status);

                if (status.compareToIgnoreCase("Subscribed") == 0) {
                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put("appId", mInitParam.mRtcAppId);
                    params.put("deviceAlias", mAccountMgr.getAccountInfo().mAccount);
                    if ((mInitParam.mPusherId != null) && (mInitParam.mPusherId.length() > 0)) {
                        params.put("pusherId", mInitParam.mPusherId);
                    }
                    params.put("localRecord", 0);
                    params.put("disabledPush", false); // 使用离线推送

                    AWSUtils.getInstance().updateRtcStatus(params);
                    AWSUtils.getInstance().getRtcStatus();

                    Map<String, Object> rtmParams = new HashMap<String, Object>();
                    rtmParams.put("appId", mInitParam.mRtcAppId);
                    AWSUtils.getInstance().updateRtmStatus(rtmParams);

                    ALog.getInstance().e(TAG, "<onConnectStatusChange> update and get RTC status");
                }
            }

            @Override
            public void onConnectFail(String message) {
                ALog.getInstance().e(TAG, "<onConnectFail> message=" + message);

                // 账号管理系统会做 登录和登出处理
                mAccountMgr.onAwsConnectFail(message);
            }

            @Override
            public void onReceiveShadow(String things_name, JSONObject jsonObject) {
                ALog.getInstance().d(TAG, "<onReceiveShadow> things_name=" + things_name
                        + ", jsonObject=" + jsonObject.toString());

                if (mDeviceMgr != null) {
                    mDeviceMgr.onReceiveShadow(things_name, jsonObject);
                }
            }

            @Override
            public void onUpdateRtcStatus(JSONObject jsonObject, long timestamp) {
                ALog.getInstance().d(TAG, "<onUpdateRtcStatus> timestamp=" + timestamp
                            + ", jsonObject=" + jsonObject.toString());
                if (mCallkitImpl != null) {
                    mCallkitImpl.onAwsUpdateClient(jsonObject, timestamp);
                }
            }

            @Override
            public void onDevOnlineChanged(String deviceMac, String deviceId, boolean online) {
                ALog.getInstance().d(TAG, "<onDevOnlineChanged> deviceMac=" + deviceMac
                    + ", deviceId=" + deviceId + ", online=" + online);
                if (mDeviceMgr != null) {
                    mDeviceMgr.onDevOnlineChanged(deviceMac, deviceId, online);
                }
            }

            @Override
            public void onDevActionUpdated(String deviceMac, String actionType) {
                ALog.getInstance().d(TAG, "<onDevActionUpdated> deviceMac=" + deviceMac
                        + ", actionType=" + actionType);
                if (mDeviceMgr != null) {
                    mDeviceMgr.onDevActionUpdated(deviceMac, actionType);
                }
            }

            @Override
            public void onDevPropertyUpdated(String deviceMac, String deviceId,
                                             Map<String, Object> properties) {
                ALog.getInstance().d(TAG, "<onDevPropertyUpdated> deviceMac=" + deviceMac
                        + ", deviceId=" + deviceId + ", properties=" + properties);
                if (mDeviceMgr != null) {
                    mDeviceMgr.onDevPropertyUpdated(deviceMac, deviceId, properties);
                }
            }
        });

        //
        // SDK初始化完成
        //
        synchronized (mDataLock) {
            mStateMachine = SDK_STATE_READY;  // 状态机切换到 SDK就绪
        }
        ALog.getInstance().d(TAG, "<initialize> done");
        return ErrCode.XOK;
    }

    @Override
    public void release() {
        //
        // 销毁工作线程
        //
        workThreadDestroy();

        //
        // 销毁接口实例对象
        //
        if (mAccountMgr != null) {
            mAccountMgr.release();
            mAccountMgr = null;
        }

        if (mCallkitImpl != null) {
            mCallkitImpl.release();
            mCallkitImpl = null;
        }

        if (mDeviceMgr != null) {
            mDeviceMgr.release();
            mDeviceMgr = null;
        }

        if (mAlarmMgr != null) {
            mAlarmMgr.release();
            mAlarmMgr = null;
        }

        if (mDevmsgMgr != null) {
            mDevmsgMgr.release();
            mDevmsgMgr = null;
        }

        if (mRtmMgr != null) {
            mRtmMgr.release();
            mRtmMgr = null;
        }

        if (mRtcPlayer != null) {
            mRtcPlayer.release();
            mRtcPlayer = null;
        }

        synchronized (mDataLock) {
            mStateMachine = SDK_STATE_INVALID;  // 状态机切换到 无效状态
        }
        ALog.getInstance().d(TAG, "<release> done");
        ALog.getInstance().release();
    }

    @Override
    public int getStateMachine() {
        synchronized (mDataLock) {
            return mStateMachine;
        }
    }

    @Override
    public IAccountMgr getAccountMgr() {
        return mAccountMgr;
    }

    @Override
    public ICallkitMgr getCallkitMgr() {
        return mCallkitImpl;
    }

    @Override
    public IDeviceMgr getDeviceMgr() {
        return mDeviceMgr;
    }

    @Override
    public IAlarmMgr getAlarmMgr() {
        return mAlarmMgr;
    }

    @Override
    public IDevMessageMgr getDevMessageMgr() {
        return mDevmsgMgr;
    }

    @Override
    public IRtmMgr getRtmMgr() {
        return mRtmMgr;
    }

    @Override
    public IRtcPlayer getRtcPlayer() {
        return mRtcPlayer;
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////////////////// Methods for each sub-module ///////////////////////
    //////////////////////////////////////////////////////////////////////////

    /*
     * @brief SDK状态机设置，仅在 AccountMgr 模块中设置
     */
    void setStateMachine(int newState) {
        synchronized (mDataLock) {
            mStateMachine = newState;
        }
    }

    IAgoraIotAppSdk.InitParam getInitParam() {
        return mInitParam;
    }

    Handler getWorkHandler() {
        return mWorkHandler;
    }

    ThreadPoolExecutor getThreadPool() {
        return mThreadPool;
    }

    public AccountMgr.AccountInfo getAccountInfo() {
        if (mAccountMgr == null) {
            return null;
        }
        return mAccountMgr.getAccountInfo();
    }

    boolean isAccountReady() {
        synchronized (mDataLock) {
            if (mStateMachine == SDK_STATE_INVALID ||
                mStateMachine == SDK_STATE_READY ||
                mStateMachine == SDK_STATE_LOGINING ||
                mStateMachine == SDK_STATE_LOGOUTING)   {
                return false;
            }
          }

        return true;
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////////////////// Innternal Utility Methods ////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    void workThreadCreate() {
        mWorkThread = new HandlerThread("AppSdk");
        mWorkThread.start();
        mWorkHandler = new Handler(mWorkThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                mAccountMgr.workThreadProcessMessage(msg);
                mRtmMgr.workThreadProcessMessage(msg);
                mRtcPlayer.workThreadProcessMessage(msg);
                workThreadProcessMessage(msg);
            }
        };

        mThreadPool = new ThreadPoolExecutor(4, 8, 8,
                TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(128));
    }

    void workThreadDestroy() {
        if (mWorkHandler != null) {

            // 清除所有消息队列中消息
            mAccountMgr.workThreadClearMessage();

            // 同步等待线程中所有任务处理完成后，才能正常退出线程
            mWorkHandler.sendEmptyMessage(MSGID_WORK_EXIT);
            synchronized (mWorkExitEvent) {
                try {
                    mWorkExitEvent.wait(EXIT_WAIT_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    ALog.getInstance().e(TAG, "<release> exception=" + e.getMessage());
                }
            }

            mWorkHandler = null;
        }

        if (mWorkThread != null) {
            mWorkThread.quit();
            mWorkThread = null;
        }

        if (mThreadPool != null) {
            mThreadPool.shutdown();
            try {
                mThreadPool.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException interruptedExp) {
                interruptedExp.printStackTrace();
            }
            mThreadPool = null;
        }
    }

    void workThreadProcessMessage(Message msg) {
        switch (msg.what) {
            case MSGID_WORK_EXIT:  // 工作线程退出消息
                synchronized (mWorkExitEvent) {
                    mWorkExitEvent.notify();    // 事件通知
                }
                break;
        }
    }


}
