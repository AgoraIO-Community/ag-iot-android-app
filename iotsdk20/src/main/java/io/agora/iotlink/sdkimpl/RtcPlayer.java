/**
 * @file RtmMgr.java
 * @brief This file implement the RTM management
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-08-11
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.sdkimpl;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceView;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.IRtcPlayer;
import io.agora.iotlink.IRtcPlayer;
import io.agora.iotlink.IotDevice;
import io.agora.iotlink.callkit.AgoraService;
import io.agora.iotlink.logger.ALog;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import io.agora.iotlink.rtcsdk.TalkingEngine;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmFileMessage;
import io.agora.rtm.RtmImageMessage;
import io.agora.rtm.RtmMediaOperationProgress;
import io.agora.rtm.RtmMessage;
import io.agora.rtm.RtmStatusCode;
import io.agora.rtm.SendMessageOptions;

/**
 * @brief RTC频道内播放器
 */
public class RtcPlayer implements IRtcPlayer, TalkingEngine.ICallback {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/RtcPlayer";


    //
    // The mesage Id
    //
    private static final int MSGID_RTCPLAYER_BASE = 0x7000;
    private static final int MSGID_RTCPLAYER_REQTOKEN = 0x7001;
    private static final int MSGID_RTCPLAYER_PREPARED = 0x7002;



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private ArrayList<IRtcPlayer.ICallback> mCallbackList = new ArrayList<>();
    private AgoraIotAppSdk mSdkInstance;                        ///< 由外部输入的
    private Handler mWorkHandler;                               ///< 工作线程Handler，从SDK获取到
    private Handler mEntryHandler;                              ///< 入口调用者线程Handler
    IAgoraIotAppSdk.InitParam mSdkInitParam;

    private static final Object mDataLock = new Object();       ///< 同步访问锁,类中所有变量需要进行加锁处理
    private volatile int mStateMachine = RTCPLAYER_STATE_STOPPED;  ///< 当前播放状态机

    private TalkingEngine mTalkEngine;              ///< 通话引擎
    private PlayerParam mPlayerParam;               ///< 播放参数


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    int initialize(AgoraIotAppSdk sdkInstance) {
        mSdkInstance = sdkInstance;
        mWorkHandler = sdkInstance.getWorkHandler();

        return ErrCode.XOK;
    }

    void release() {
        stop();
        workThreadClearMessage();
        mTalkEngine = null;
    }

    boolean isRunning() {
        return (getStateMachine() != RTCPLAYER_STATE_STOPPED);
    }

    void workThreadProcessMessage(Message msg) {
        switch (msg.what) {
            case MSGID_RTCPLAYER_REQTOKEN: {
                DoRequestToken(msg);
            } break;

            case MSGID_RTCPLAYER_PREPARED: {
                DoPrepareDone(msg);
            } break;
        }
    }

    void workThreadClearMessage() {
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(MSGID_RTCPLAYER_REQTOKEN);
            mWorkHandler.removeMessages(MSGID_RTCPLAYER_PREPARED);
            mWorkHandler = null;
        }
    }

    void sendTaskMessage(int what, int arg1, int arg2, Object obj) {
        Message msg = new Message();
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = obj;
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(what);
            mWorkHandler.sendMessage(msg);
        }
    }


    ///////////////////////////////////////////////////////////////////////
    /////////////////// Override Methods of IRtcPlayer ///////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public int getStateMachine() {
        synchronized (mDataLock) {
            return mStateMachine;
        }
    }

    @Override
    public int registerListener(IRtcPlayer.ICallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.add(callback);
        }
        return ErrCode.XOK;
    }

    @Override
    public int unregisterListener(IRtcPlayer.ICallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.remove(callback);
        }
        return ErrCode.XOK;
    }

    @Override
    public int start(final String channelName, final int localUid, final SurfaceView displayView) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<start> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }
        int currState = getStateMachine();
        if (currState != RTCPLAYER_STATE_STOPPED) {
            ALog.getInstance().e(TAG, "<start> bad state, currState=" + currState);
            return ErrCode.XERR_BAD_STATE;
        }

        setStateMachine(RTCPLAYER_STATE_PREPARING);  // 播放准备状态


        mEntryHandler= new Handler(Looper.myLooper());

        mPlayerParam = new PlayerParam();
        mPlayerParam.mChannelName = channelName;
        mPlayerParam.mLocalUid = localUid;
        mPlayerParam.mDisplayView = displayView;

        sendTaskMessage(MSGID_RTCPLAYER_REQTOKEN, 0, 0, mPlayerParam);
        ALog.getInstance().d(TAG, "<start> finished, channelName=" + channelName
                                + ", localUid=" + localUid);
        return ErrCode.XOK;
    }

    @Override
    public int stop() {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<stop> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }
        int currState = getStateMachine();
        if (currState == RTCPLAYER_STATE_STOPPED) {
            ALog.getInstance().e(TAG, "<stop> bad state, currState=" + currState);
            return ErrCode.XERR_BAD_STATE;
        }


        if (mTalkEngine != null) {
            mTalkEngine.leaveChannel();
            ALog.getInstance().d(TAG, "<stop> done");
        }

        setStateMachine(RTCPLAYER_STATE_STOPPED);  // 播放停止状态
        return ErrCode.XOK;
    }


    ///////////////////////////////////////////////////////////////////////
    /////////////////// Internal Methods of RtcPlayer //////////////////////
    ///////////////////////////////////////////////////////////////////////

    /**
     * @brief 在工作线程中执行，请求Token信息
     */
    void DoRequestToken(Message msg) {
        PlayerParam playerParam = (PlayerParam)msg.obj;
        RtcPlayer rtcPlayer = this;

        //
        // 向服务器请求 RtcPlayer 的token信息
        //
        String rtcToken = null;


        //
        // 在入口调用线程中，加入频道进行拉流
        //
        mEntryHandler.post(new Runnable() {
            @Override
            public void run() {
                //
                // 创建 RTC 拉流引擎
                //
                IAgoraIotAppSdk.InitParam sdkInitParam = mSdkInstance.getInitParam();
                mTalkEngine = new TalkingEngine();
                TalkingEngine.InitParam talkInitParam = mTalkEngine.new InitParam();
                talkInitParam.mContext = sdkInitParam.mContext;
                talkInitParam.mAppId = "aab8b8f5a8cd4469a63042fcfafe7063"; // sdkInitParam.mRtcAppId;
                talkInitParam.mCallback = rtcPlayer;
                talkInitParam.mPublishVideo = false;
                talkInitParam.mPublishAudio = false;
                talkInitParam.mSubscribeAudio = true;
                talkInitParam.mSubscribeVideo = true;
                mTalkEngine.initialize(talkInitParam);
                // mTalkEngine.setPeerUid(peerUid);
                // mTalkEngine.setRemoteVideoView(playerParam.mDisplayView);

                // 加入频道
                boolean ret = mTalkEngine.joinChannel(playerParam.mChannelName, rtcToken,
                        playerParam.mLocalUid);
                if (!ret) {
                    setStateMachine(RTCPLAYER_STATE_STOPPED);  // 停止状态
                    sendTaskMessage(MSGID_RTCPLAYER_PREPARED, ErrCode.XERR_UNSUPPORTED,
                            0, playerParam);
                }
            }
        });

        ALog.getInstance().d(TAG, "<DoRequestToken> finished, rtcToken=" + rtcToken);
     }

    /**
     * @brief 在工作线程中执行，准备完成
     */
    void DoPrepareDone(Message msg) {
        int errCode = msg.arg1;
        PlayerParam playerParam = (PlayerParam)msg.obj;
        ALog.getInstance().e(TAG, "<DoPrepareDone> errCode=" + errCode);

        synchronized (mCallbackList) {
            for (IRtcPlayer.ICallback listener : mCallbackList) {
                listener.onPrepareDone(errCode, playerParam.mChannelName, playerParam.mLocalUid);
            }
        }
    }


    /////////////////////////////////////////////////////////////////////////////
    //////////////////// TalkingEngine.ICallback 回调处理 ////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    @Override
    public void onTalkingJoinDone(String channel, int localUid) {
        ALog.getInstance().d(TAG, "<onTalkingJoinDone> channel=" + channel
                + ", localUid=" + localUid);
    }

    @Override
    public void onTalkingLeftDone() {
        ALog.getInstance().d(TAG, "<onTalkingLeftDone> ");
    }


    @Override
    public void onTalkingPeerJoined(int localUid, int peerUid) {
        int stateMachine = getStateMachine();
        ALog.getInstance().d(TAG, "<onTalkingPeerJoined> localUid=" + localUid
                + ", peerUid=" + peerUid
                + ", stateMachine=" + stateMachine);
    }

    @Override
    public void onTalkingPeerLeft(int localUid, int peerUid) {
        int stateMachine = getStateMachine();
        ALog.getInstance().d(TAG, "<onTalkingPeerLeft> localUid=" + localUid
                + ", peerUid=" + peerUid
                + ", stateMachine=" + stateMachine);

    }

    @Override
    public void onPeerFirstVideoDecoded(int peerUid, int videoWidth, int videoHeight) {
        int stateMachine = getStateMachine();
        ALog.getInstance().d(TAG, "<onPeerFirstVideoDecoded> peerUid=" + peerUid
                + ", videoWidth=" + videoWidth
                + ", videoHeight=" + videoHeight);

        if (stateMachine == RTCPLAYER_STATE_PREPARING) {
            setStateMachine(RTCPLAYER_STATE_PLAYING);  // 正常播放状态
            sendTaskMessage(MSGID_RTCPLAYER_PREPARED, ErrCode.XOK, 0, mPlayerParam);
        }

    }


    /////////////////////////////////////////////////////////////////////////////
    //////////////////// Internal Methods or Data Structure /////////////////////
    /////////////////////////////////////////////////////////////////////////////
    void setStateMachine(int newStateMachine) {
        synchronized (mDataLock) {
            mStateMachine = newStateMachine;
        }
    }

    /**
     * @brief 播放参数信息
     */
    private class PlayerParam {
        String mChannelName;
        int    mLocalUid;
        SurfaceView mDisplayView;
    }

}
