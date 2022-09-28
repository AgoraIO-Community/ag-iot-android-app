/**
 * @file AlarmMgr.java
 * @brief This file implement the alarm management
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.sdkimpl;


import android.os.Handler;
import android.os.Message;
import android.util.Log;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAccountMgr;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.IAlarmMgr;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.IDeviceMgr;
import io.agora.iotlink.IotAlarm;
import io.agora.iotlink.IotAlarmImage;
import io.agora.iotlink.IotAlarmPage;
import io.agora.iotlink.IotAlarmVideo;
import io.agora.iotlink.IotDevice;
import io.agora.iotlink.callkit.AgoraService;
import io.agora.iotlink.callkit.CallkitContext;
import io.agora.iotlink.logger.ALog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @brief 告警信息管理器
 */
public class AlarmMgr implements IAlarmMgr {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/AlarmMgr";


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private ArrayList<IAlarmMgr.ICallback> mCallbackList = new ArrayList<>();
    private AgoraIotAppSdk mSdkInstance;        ///< 由外部输入的 SDK实例对象
    private ThreadPoolExecutor mThreadPool;     ///< 执行操作的线程池，由外部输入

    private static final Object mDataLock = new Object();       ///< 同步访问锁,类中所有变量需要进行加锁处理
    private volatile int mStateMachine = ALARMMGR_STATE_IDLE;   ///< 当前呼叫状态机



    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    int initialize(AgoraIotAppSdk sdkInstance) {
        mSdkInstance = sdkInstance;
        mThreadPool = sdkInstance.getThreadPool();
        mStateMachine = ALARMMGR_STATE_IDLE;

        return ErrCode.XOK;
    }

    void release() {
        synchronized (mCallbackList) {
            mCallbackList.clear();
        }

        mThreadPool = null;
    }

    ///////////////////////////////////////////////////////////////////////
    /////////////////// Override Methods of IAlarmMgr /////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public int getStateMachine() {
        synchronized (mDataLock) {
            return mStateMachine;
        }
    }

    @Override
    public int registerListener(IAlarmMgr.ICallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.add(callback);
        }
        return ErrCode.XOK;
    }

    @Override
    public int unregisterListener(IAlarmMgr.ICallback callback){
        synchronized (mCallbackList) {
            mCallbackList.remove(callback);
        }
        return ErrCode.XOK;
    }

    @Override
    public int insert(InsertParam insertParam) {
        ALog.getInstance().d(TAG, "<insert> insertParam=" + insertParam.toString());
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {

                AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
                if (account == null) {
                    ALog.getInstance().e(TAG, "<insert> failure, cannot get account");
                    CallbackInsertDone(ErrCode.XERR_ALARM_ADD, insertParam);
                    return;
                }

                int errCode = AgoraService.getInstance().alarmInsert(account.mAgoraAccessToken,
                        account.mInventDeviceName, account.mInventDeviceName, insertParam);
                ALog.getInstance().d(TAG, "<insert> done, errCode=" + errCode
                        + ", insertParam=" + insertParam.toString());
                CallbackInsertDone(errCode, insertParam);
            }
        });

        return ErrCode.XOK;
    }

    @Override
    public int delete(List<Long> alarmIdList) {
        ALog.getInstance().d(TAG, "<delete> idList=" + idListToString(alarmIdList));
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {

                AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
                if (account == null) {
                    ALog.getInstance().e(TAG, "<delete> failure, cannot get account");
                    CallbackDeleteDone(ErrCode.XERR_ALARM_DEL, alarmIdList);
                    return;
                }

                int errCode = AgoraService.getInstance().alarmDelete(account.mAgoraAccessToken,
                        account.mInventDeviceName, alarmIdList);
                ALog.getInstance().d(TAG, "<delete> done, errCode=" + errCode
                        + ", alarmIdList=" + idListToString(alarmIdList));
                CallbackDeleteDone(errCode, alarmIdList);
            }
        });

        return ErrCode.XOK;
    }

    @Override
    public int mark(List<Long> alarmIdList) {
        ALog.getInstance().d(TAG, "<mark> idList=" + idListToString(alarmIdList));
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
                if (account == null) {
                    ALog.getInstance().e(TAG, "<mark> failure, cannot get account");
                    CallbackMarkDone(ErrCode.XERR_ALARM_DEL, alarmIdList);
                    return;
                }

                int errCode = AgoraService.getInstance().alarmMarkRead(account.mAgoraAccessToken,
                        account.mInventDeviceName, alarmIdList);
                ALog.getInstance().d(TAG, "<mark> done, errCode=" + errCode
                        + ", alarmIdList=" + idListToString(alarmIdList));
                CallbackMarkDone(errCode, alarmIdList);
            }
        });

        return ErrCode.XOK;
    }

    @Override
    public int queryById(long alarmId) {
        ALog.getInstance().d(TAG, "<queryById> alarmId=" + alarmId);
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
                if (account == null) {
                    ALog.getInstance().e(TAG, "<queryById> failure, cannot get account");
                    CallbackQueryInfoDone(ErrCode.XERR_ALARM_QUERY, null);
                    return;
                }

                AgoraService.AlarmInfoResult infoResult = AgoraService.getInstance().queryAlarmInfoById(
                        account.mAgoraAccessToken, account.mInventDeviceName, alarmId);
                ALog.getInstance().d(TAG, "<queryById> done, errCode=" + infoResult.mErrCode
                        + ", iotAlarm=" + infoResult.mAlarm.toString());
                CallbackQueryInfoDone(infoResult.mErrCode, infoResult.mAlarm);
            }
        });

        return ErrCode.XOK;
    }

    @Override
    public int queryByPage(QueryParam queryParam) {
        ALog.getInstance().d(TAG, "<queryByPage> queryParam=" + queryParam.toString());
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
                if (account == null) {
                    ALog.getInstance().e(TAG, "<queryByPage> cannot get account");
                    CallbackQueryPageDone(ErrCode.XERR_ALARM_QUERY, queryParam, null);
                    return;
                }

                AgoraService.AlarmPageResult pageResult = AgoraService.getInstance().queryAlarmByPage(
                        account.mAgoraAccessToken, account.mInventDeviceName,
                        account.mInventDeviceName, queryParam);
                ALog.getInstance().d(TAG, "<queryByPage> done, errCode=" + pageResult.mErrCode
                        + ", mAlarmPage=" + pageResult.mAlarmPage.toString());
                CallbackQueryPageDone(pageResult.mErrCode, queryParam, pageResult.mAlarmPage);
            }
        });

        return ErrCode.XOK;
    }

    @Override
    public int queryNumber(QueryParam queryParam) {
         ALog.getInstance().d(TAG, "<queryNumber> queryParam=" + queryParam.toString());
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
                if (account == null) {
                    ALog.getInstance().e(TAG, "<queryNumber> failure, cannot get account");
                    CallbackQueryNumberDone(ErrCode.XERR_ALARM_QUERY, queryParam, 0);
                    return;
                }

                AgoraService.AlarmNumberResult numberResult = AgoraService.getInstance().queryAlarmNumber(
                        account.mAgoraAccessToken, account.mInventDeviceName,
                        account.mInventDeviceName, queryParam);
                ALog.getInstance().d(TAG, "<queryNumber> done, errCode=" + numberResult.mErrCode
                        + ", alarmNumber=" + numberResult.mAlarmNumber);
                CallbackQueryNumberDone(numberResult.mErrCode, queryParam, numberResult.mAlarmNumber);
            }
        });
        return ErrCode.XOK;
    }


    @Override
    public int queryImageById(final String imageId) {
        ALog.getInstance().d(TAG, "<queryImageById> imageId=" + imageId);

        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
                if (account == null) {
                    ALog.getInstance().e(TAG, "<queryImageById> failure, cannot get account");
                    CallbackQueryImageDone(ErrCode.XERR_ALARM_IMAGE, imageId, null);                 return;
                }

                AgoraService.AlarmImageResult imgResult;
                imgResult = AgoraService.getInstance().queryAlarmImageInfo(account.mAgoraAccessToken,
                        account.mInventDeviceName, imageId);

                ALog.getInstance().d(TAG, "<queryImageById> done, errCode=" + imgResult.mErrCode
                        + ", mAlarmImg=" + imgResult.mAlarmImg);
                CallbackQueryImageDone(imgResult.mErrCode, imageId, imgResult.mAlarmImg);
            }
        });
        return ErrCode.XOK;
    }

    @Override
    public int queryVideoByTimestamp(final String deviceID, final long timestamp) {
        ALog.getInstance().d(TAG, "<queryVideoByTimestamp> timestamp=" + timestamp);

        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
                if (account == null) {
                    ALog.getInstance().e(TAG, "<queryVideoByTimestamp> failure, cannot get account");
                    CallbackQueryVideoDone(ErrCode.XERR_ALARM_VIDEO, deviceID, timestamp, null);                 return;
                }

                AgoraService.CloudRecordResult videoResult;
                videoResult = AgoraService.getInstance().queryAlarmRecordInfo(account.mAgoraAccessToken,
                        account.mInventDeviceName, deviceID, timestamp);

                ALog.getInstance().d(TAG, "<queryVideoByTimestamp> done, errCode=" + videoResult.mErrCode
                        + ", mAlarmImg=" + videoResult.mAlarmVideo);
                CallbackQueryVideoDone(videoResult.mErrCode, deviceID, timestamp, videoResult.mAlarmVideo);
            }
        });
        return ErrCode.XOK;
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////// Callback Methods for Public Invoke ///////////////////
    ///////////////////////////////////////////////////////////////////////
     void CallbackInsertDone(int errCode, final InsertParam insertParam) {
        synchronized (mCallbackList) {
            for (IAlarmMgr.ICallback listener : mCallbackList) {
                listener.onAlarmAddDone(errCode, insertParam);
            }
        }
    }

    void CallbackDeleteDone(int errCode, final List<Long> alarmIdList) {
        synchronized (mCallbackList) {
            for (IAlarmMgr.ICallback listener : mCallbackList) {
                listener.onAlarmDeleteDone(errCode, alarmIdList);
            }
        }
    }

    void CallbackMarkDone(int errCode, final List<Long> alarmIdList) {
        synchronized (mCallbackList) {
            for (IAlarmMgr.ICallback listener : mCallbackList) {
                listener.onAlarmMarkDone(errCode, alarmIdList);
            }
        }
    }

    void CallbackQueryInfoDone(int errCode, final IotAlarm iotAlarm) {
        synchronized (mCallbackList) {
            for (IAlarmMgr.ICallback listener : mCallbackList) {
                listener.onAlarmInfoQueryDone(errCode, iotAlarm);
            }
        }
    }

    void CallbackQueryPageDone(int errCode, final IAlarmMgr.QueryParam queryParam,
                               final IotAlarmPage iotAlarmPage) {
        synchronized (mCallbackList) {
            for (IAlarmMgr.ICallback listener : mCallbackList) {
                listener.onAlarmPageQueryDone(errCode, queryParam, iotAlarmPage);
            }
        }
    }

    void CallbackQueryNumberDone(int errCode, final IAlarmMgr.QueryParam queryParam, long number) {
        synchronized (mCallbackList) {
            for (IAlarmMgr.ICallback listener : mCallbackList) {
                listener.onAlarmNumberQueryDone(errCode, queryParam, number);
            }
        }
    }

    void CallbackQueryImageDone(int errCode, final String imageId, final IotAlarmImage alarmImage) {
        synchronized (mCallbackList) {
            for (IAlarmMgr.ICallback listener : mCallbackList) {
                listener.onAlarmImageQueryDone(errCode, imageId, alarmImage);
            }
        }
    }

    void CallbackQueryVideoDone(int errCode, final String deviceID, long timestamp,
                                final IotAlarmVideo alarmVideo) {
        synchronized (mCallbackList) {
            for (IAlarmMgr.ICallback listener : mCallbackList) {
                listener.onAlarmVideoQueryDone(errCode, deviceID, timestamp, alarmVideo);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////////// Inner Data Structure and Methods ////////////////
    ///////////////////////////////////////////////////////////////////////
    String idListToString(List<Long> idList) {
        String text_info = "( ";
        for (int i = 0; i < idList.size(); i++) {
            String idText = String.valueOf(idList.get(i));
            text_info = text_info + idText + ", ";
        }
        text_info = text_info + " )";
        return text_info;
    }

}
