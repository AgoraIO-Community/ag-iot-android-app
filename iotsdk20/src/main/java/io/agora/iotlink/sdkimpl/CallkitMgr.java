/**
 * @file AccountMgr.java
 * @brief This file implement the call kit and RTC management
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.sdkimpl;


import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.view.SurfaceView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.IotDevice;
import io.agora.iotlink.aws.AWSUtils;
import io.agora.iotlink.base.BaseThreadComp;
import io.agora.iotlink.callkit.AgoraService;
import io.agora.iotlink.callkit.CallkitContext;
import io.agora.iotlink.callkit.CallkitScheduler;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.rtcsdk.TalkingEngine;
import io.agora.iotlink.transport.HttpReqScheduler;
import io.agora.rtc2.Constants;

/*
 * @brief 呼叫系统管理器
 */
public class CallkitMgr extends BaseThreadComp implements ICallkitMgr, TalkingEngine.ICallback {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/CallkitMgr";
    private static final int DIAL_WAIT_TIMEOUT = 30000;          ///< 呼叫超时请求 30秒
    private static final int ANSWER_WAIT_TIMEOUT = 30000;        ///< 接听处理超时 30秒
    private static final int HANGUP_WAIT_TIMEOUT = 1000;
    private static final int DEFAULT_DEV_UID = 10;               ///< 设备端uid，固定为10


    //
    // The mesage Id
    //
    private static final int MSGID_CALL_BASE = 0x3000;
    private static final int MSGID_CALL_AWSEVENT_INCOMING = 0x3001; ///< 处理AWS端的事件
    private static final int MSGID_CALL_RESP_DIAL = 0x3002;          ///< 发送拨号请求
    private static final int MSGID_CALL_REQ_ANSWER = 0x3003;        ///< 发送接听请求
    private static final int MSGID_CALL_REQ_HANGUP = 0x3004;        ///< 发送挂断请求
    private static final int MSGID_CALL_RTC_PEER_ONLINE = 0x3005;   ///< 对端RTC上线
    private static final int MSGID_CALL_RTC_PEER_OFFLINE = 0x3006;  ///< 对端RTC掉线
    private static final int MSGID_CALL_RTC_PEER_FIRSTVIDEO = 0x3007;  ///< 对端RTC首帧出图
    private static final int MSGID_CALL_DIAL_TIMEOUT = 0x3008;      ///< 呼叫超时定时器
    private static final int MSGID_CALL_ANSWER_TIMEOUT = 0x3009;    ///< 接听超时定时器
    private static final int MSGID_RECORDING_ERROR = 0x3010;        ///< 录像出现错误




    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private ArrayList<ICallkitMgr.ICallback> mCallbackList = new ArrayList<>();
    private AgoraIotAppSdk mSdkInstance;    ///< 由外部输入的

    private static final Object mDataLock = new Object();       ///< 同步访问锁,类中所有变量需要进行加锁处理
    private final Object mReqHangupEvent = new Object();
    private volatile int mReqHangupErrCode = ErrCode.XOK;

    private volatile int mStateMachine = CALLKIT_STATE_IDLE;    ///< 当前呼叫状态机
    private String mAppId;
    private CallkitContext mCallkitCtx;             ///< 当前呼叫的上下文数据
    private IotDevice mPeerDevice;                  ///< 通信的对端设备

    private TalkingEngine mTalkEngine;              ///< 通话引擎
    private SurfaceView mPeerVidew;                 ///< 对端视频帧显示控件
    private boolean mMuteLocalVideo;
    private boolean mMuteLocalAudio;
    private boolean mMutePeerVideo;
    private boolean mMutePeerAudio;
    private int mAudioEffect = Constants.AUDIO_EFFECT_OFF;
    private volatile int mOnlineUserCount = 0;               ///< 在线的用户数量（不包括设备端）

    private HttpReqScheduler mScheduler = new HttpReqScheduler();


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    int initialize(AgoraIotAppSdk sdkInstance) {
        mSdkInstance = sdkInstance;
        mStateMachine = CALLKIT_STATE_IDLE;

        IAgoraIotAppSdk.InitParam sdkInitParam = sdkInstance.getInitParam();
        mAppId = sdkInitParam.mRtcAppId;

        // 初始化通话引擎参数
        mPeerVidew = null;
        mMuteLocalVideo = true;
        mMuteLocalAudio = true;
        mMutePeerVideo = false;
        mMutePeerAudio = false;
        mAudioEffect = Constants.AUDIO_EFFECT_OFF;

        // 初始化调度器，在登录登出的时候要停止操作
        mScheduler.initialize(sdkInstance);

        // 启动呼叫系统的工作线程
        runStart(TAG);

        return ErrCode.XOK;
    }

    void release() {

        // 释放调度器
        mScheduler.release();

        runStop();

    }

    /*
     * @brief 在AWS事件中被调用，来电消息
     */
    void onAwsEventIncoming(JSONObject jsonState, long timestamp) {
        //ALog.getInstance().d(TAG, "<onAwsUpdateClient> jsonState=" + jsonState.toString());
        if (mWorkHandler != null) {
            Message msg = new Message();
            msg.what = MSGID_CALL_AWSEVENT_INCOMING;
            msg.obj = jsonState;
            mWorkHandler.sendMessage(msg);   // 所有事件都不要遗漏，全部发送
        }
    }



    ///////////////////////////////////////////////////////////////////////////
    //////////////// Methods for Override BaseThreadComp //////////////////////
    //////////////////////////////////////////////////////////////////////////
    @Override
    public void processWorkMessage(Message msg) {
        switch (msg.what) {
            case MSGID_CALL_AWSEVENT_INCOMING:
                DoAwsEventIncoming(msg);
                break;
            case MSGID_CALL_ANSWER_TIMEOUT:
                DoAnswerTimeout(msg);
                break;
            case MSGID_CALL_REQ_ANSWER:
                DoRequestAnswer(msg);
                break;

            case MSGID_CALL_RESP_DIAL:
                DoDialResponse(msg);
                break;
            case MSGID_CALL_DIAL_TIMEOUT:
                DoDialTimeout(msg);
                break;
            case MSGID_CALL_REQ_HANGUP:
                DoRequestHangup(msg);
                break;

            case MSGID_CALL_RTC_PEER_ONLINE:
                DoRtcPeerOnline(msg);
                break;
            case MSGID_CALL_RTC_PEER_OFFLINE:
                DoRtcPeerOffline(msg);
                break;
            case MSGID_CALL_RTC_PEER_FIRSTVIDEO:
                DoRtcPeerFirstVideo(msg);
                break;

            case MSGID_RECORDING_ERROR:
                DoRecordingError(msg);
                break;
        }
    }

    @Override
    protected void removeAllMessages() {
        synchronized (mMsgQueueLock) {
            mWorkHandler.removeMessages(MSGID_CALL_AWSEVENT_INCOMING);
            mWorkHandler.removeMessages(MSGID_CALL_ANSWER_TIMEOUT);
            mWorkHandler.removeMessages(MSGID_CALL_REQ_ANSWER);
            mWorkHandler.removeMessages(MSGID_CALL_RESP_DIAL);
            mWorkHandler.removeMessages(MSGID_CALL_DIAL_TIMEOUT);
            mWorkHandler.removeMessages(MSGID_CALL_REQ_HANGUP);
            mWorkHandler.removeMessages(MSGID_CALL_RTC_PEER_ONLINE);
            mWorkHandler.removeMessages(MSGID_CALL_RTC_PEER_OFFLINE);
            mWorkHandler.removeMessages(MSGID_RECORDING_ERROR);
        }
    }

    @Override
    protected void processTaskFinsh() {

        // 销毁通话引擎
        talkingStop();

        synchronized (mCallbackList) {
            mCallbackList.clear();
        }
    }



    ///////////////////////////////////////////////////////////////////////
    /////////////////// Override Methods of ICallkitMgr //////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public int getStateMachine() {
        synchronized (mDataLock) {
            return mStateMachine;
        }
    }

    @Override
    public int registerListener(ICallkitMgr.ICallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.add(callback);
        }
        return ErrCode.XOK;
    }

    @Override
    public int unregisterListener(ICallkitMgr.ICallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.remove(callback);
        }
        return ErrCode.XOK;
    }

    @Override
    public int callDial(IotDevice iotDevice, String attachMsg) {
        long t1 = System.currentTimeMillis();
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<callDial> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_ACCOUNT_LOGIN;
        }
        int currState = getStateMachine();
        if (currState != CALLKIT_STATE_IDLE) {
            ALog.getInstance().e(TAG, "<callDial> bad state, currState=" + currState);
            return ErrCode.XERR_BAD_STATE;
        }

        // 发送请求消息
        ALog.getInstance().d(TAG, "<callDial> ==> BEGIN, attachMsg=" + attachMsg);
        setStateMachine(CALLKIT_STATE_DIAL_REQING);  // 呼叫请求中

        synchronized (mDataLock) {
            mPeerDevice = iotDevice;
        }

        // 启动呼叫超时定时器
        dialTimeoutStart();

        // 在调度器中执行 呼叫HTTP请求
        AccountMgr.AccountInfo accountInfo = mSdkInstance.getAccountInfo();
        mScheduler.dial(accountInfo.mAgoraAccessToken, mAppId,
                accountInfo.mInventDeviceName, iotDevice.mDeviceID, attachMsg, new HttpReqScheduler.IAsyncDialCallback() {
                    @Override
                    public void onAsyncDialDone(AgoraService.CallReqResult dialResult) {
                        ALog.getInstance().d(TAG, "<onAsyncDialDone> dialResult=" + dialResult.toString()
                                + ", mStateMachine=" + getStateMachine());
                        dialTimeoutStop();  // 停止呼叫超时定时器

                        if (getStateMachine() == CALLKIT_STATE_DIAL_REQING) { // 当前还在呼叫请求中
                            Object callParams = new Object[]{iotDevice, attachMsg, dialResult};
                            sendSingleMessage(MSGID_CALL_RESP_DIAL, 0, 0, callParams,0);
                        }
                    }
                });

        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<callDial> <==End done"
                + ", iotDevice=" + iotDevice.toString()
                + ", attachMsg=" + attachMsg + ", costTime=" + (t2-t1));
        return ErrCode.XOK;
    }



    @Override
    public int callHangup() {
        long t1 = System.currentTimeMillis();
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<callHangup> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_ACCOUNT_LOGIN;
        }
        int currState = getStateMachine();

        if ((currState == CALLKIT_STATE_IDLE) || (currState == CALLKIT_STATE_HANGUP_REQING)) {
            ALog.getInstance().e(TAG, "<callHangup> already hangup, currState=" + currState);
            return ErrCode.XOK;
        }

        // 停止接听超时
        answerTimeoutStop();

        // 停止呼叫超时定时器
        dialTimeoutStop();

        // 发送请求消息，同步等待执行完成
        ALog.getInstance().d(TAG, "<callHangup> ==> BEGIN");
        setStateMachine(CALLKIT_STATE_HANGUP_REQING);  // 挂断请求中

        // 在调度器中执行 挂断HTTP请求
        mScheduler.hangup();

        if (mWorkHandler != null) {  // 移除所有中间的消息
            mWorkHandler.removeMessages(MSGID_CALL_AWSEVENT_INCOMING);
            mWorkHandler.removeMessages(MSGID_CALL_RESP_DIAL);
            mWorkHandler.removeMessages(MSGID_CALL_REQ_ANSWER);
            mWorkHandler.removeMessages(MSGID_CALL_RTC_PEER_ONLINE);
            mWorkHandler.removeMessages(MSGID_CALL_RTC_PEER_OFFLINE);
            mWorkHandler.removeMessages(MSGID_RECORDING_ERROR);
        }
        sendSingleMessage(MSGID_CALL_REQ_HANGUP, 0, 0, null, 0);  // 发送挂断请求

        synchronized (mReqHangupEvent) {
            try {
                mReqHangupEvent.wait(HANGUP_WAIT_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
                ALog.getInstance().e(TAG, "<callHangup> exception=" + e.getMessage());
            }
        }

        setStateMachine(CALLKIT_STATE_IDLE);
        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<callHangup> <==END done, errCode=" + mReqHangupErrCode
                + ", costTime=" + (t2-t1));
        return mReqHangupErrCode;
    }

    @Override
    public int callAnswer() {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<callAnswer> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_ACCOUNT_LOGIN;
        }
        int currState = getStateMachine();
        if (currState != CALLKIT_STATE_INCOMING) {
            ALog.getInstance().e(TAG, "<callAnswer> bad state, currState=" + currState);
            return ErrCode.XERR_BAD_STATE;
        }

        // 停止呼叫超时计时器
        dialTimeoutStop();

        // 停止接听超时计时器
        answerTimeoutStop();

        // 发送请求消息，同步等待执行完成
        setStateMachine(CALLKIT_STATE_ANSWER_REQING);  // 接听请求中
        sendSingleMessage(MSGID_CALL_REQ_ANSWER, 0, 0, null, 0);

        ALog.getInstance().d(TAG, "<callAnswer> done");
        return ErrCode.XOK;
    }


    @Override
    public int setLocalVideoView(SurfaceView localView) {
        return ErrCode.XOK;
    }

    @Override
    public int setPeerVideoView(SurfaceView peerView) {
        mPeerVidew = peerView;
        if (mTalkEngine != null) {
            mTalkEngine.setRemoteVideoView(mPeerVidew);
        }
        return ErrCode.XOK;
    }

    @Override
    public int muteLocalVideo(boolean mute) {
        mMuteLocalVideo = mute;

        if (mTalkEngine != null) {
            boolean ret = mTalkEngine.muteLocalVideoStream(mute);
            return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
        }
        return ErrCode.XOK;
    }

    @Override
    public int muteLocalAudio(boolean mute) {
        mMuteLocalAudio = mute;

        if (mTalkEngine != null) {
            boolean ret = mTalkEngine.muteLocalAudioStream(mute);
            return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
        }
        return ErrCode.XOK;
    }

    @Override
    public int mutePeerVideo(boolean mute) {
        mMutePeerVideo = mute;

        if (mTalkEngine != null) {
            boolean ret = mTalkEngine.mutePeerVideoStream(mute);
            return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
        }
        return ErrCode.XOK;
    }

    @Override
    public int mutePeerAudio(boolean mute) {
        mMutePeerAudio = mute;

        if (mTalkEngine != null) {
            boolean ret = mTalkEngine.mutePeerAudioStream(mute);
            return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
        }
        return ErrCode.XOK;
    }

    @Override
    public int setVolume(int volumeLevel) {
        return ErrCode.XERR_UNSUPPORTED;
    }

    @Override
    public int setAudioEffect(AudioEffectId effectId) {
        synchronized (mDataLock) {
            mAudioEffect = Constants.AUDIO_EFFECT_OFF;
            switch (effectId) {
                case OLDMAN:
                    mAudioEffect = Constants.VOICE_CHANGER_EFFECT_OLDMAN;
                    break;

                case BABYBOY:
                    mAudioEffect = Constants.VOICE_CHANGER_EFFECT_BOY;
                    break;

                case BABYGIRL:
                    mAudioEffect = Constants.VOICE_CHANGER_EFFECT_GIRL;
                    break;

                case ZHUBAJIE:
                    mAudioEffect = Constants.VOICE_CHANGER_EFFECT_PIGKING;
                    break;

                case ETHEREAL:
                    mAudioEffect = Constants.VOICE_CHANGER_EFFECT_SISTER;
                    break;

                case HULK:
                    mAudioEffect = Constants.VOICE_CHANGER_EFFECT_HULK;
                    break;
            }
        }

        if (mTalkEngine != null) {
            boolean ret = mTalkEngine.setAudioEffect(mAudioEffect);
            return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
        }

        return ErrCode.XOK;
    }

    @Override
    public AudioEffectId getAudioEffect() {
        AudioEffectId effectId = AudioEffectId.NORMAL;

        synchronized (mDataLock) {
            switch (mAudioEffect) {
                case Constants.VOICE_CHANGER_EFFECT_OLDMAN:
                    effectId = AudioEffectId.OLDMAN;
                    break;

                case Constants.VOICE_CHANGER_EFFECT_BOY:
                    effectId = AudioEffectId.BABYBOY;
                    break;

                case Constants.VOICE_CHANGER_EFFECT_GIRL:
                    effectId = AudioEffectId.BABYGIRL;
                    break;

                case Constants.VOICE_CHANGER_EFFECT_PIGKING:
                    effectId = AudioEffectId.ZHUBAJIE;
                    break;

                case Constants.VOICE_CHANGER_EFFECT_SISTER:
                    effectId = AudioEffectId.ETHEREAL;
                    break;

                case Constants.VOICE_CHANGER_EFFECT_HULK:
                    effectId = AudioEffectId.HULK;
                    break;

                default:
                    effectId = AudioEffectId.NORMAL;
                    break;
            }
        }

        ALog.getInstance().d(TAG, "<getAudioEffect> effectId=" + effectId);
        return effectId;
    }

    @Override
    public int talkingRecordStart(final String outFilePath) {
        if (mTalkEngine == null) {
            ALog.getInstance().e(TAG, "<talkingRecordStart> talk engine NOT running");
            return ErrCode.XERR_BAD_STATE;
        }

        int ret = mTalkEngine.recordingStart(outFilePath);
        return ret;
    }

    @Override
    public int talkingRecordStop() {
        if (mTalkEngine == null) {
            ALog.getInstance().e(TAG, "<talkingRecordStop> talk engine NOT running");
            return ErrCode.XERR_BAD_STATE;
        }

        int ret = mTalkEngine.recordingStop();
        return ret;
    }

    @Override
    public void onRecordingError(int errCode) {
        ALog.getInstance().e(TAG, "<onRecordingError> errCode=" + errCode);

        // 发送录像错误回调消息
        sendSingleMessage(MSGID_RECORDING_ERROR, errCode, 0, null, 0);
    }

    /**
     * @brief 判断当前是否正在本地录制
     * @return true 表示正在本地录制频道； false: 不在录制
     */
    @Override
    public boolean isTalkingRecording() {
        if (mTalkEngine == null) {
            return false;
        }
        return mTalkEngine.isRecording();
    }

    @Override
    public RtcNetworkStatus getNetworkStatus() {
        if (mTalkEngine == null) {
            ALog.getInstance().e(TAG, "<RtcNetworkStatus> bad status");
            return null;
        }

        RtcNetworkStatus networkStatus = mTalkEngine.getNetworkStatus();
        return networkStatus;
    }

    @Override
    public Bitmap capturePeerVideoFrame() {
        if (mTalkEngine == null) {
            ALog.getInstance().e(TAG, "<capturePeerVideoFrame> bad status");
            return null;
        }

        Bitmap capturedBmp = mTalkEngine.capturePeerVideoFrame();
        return capturedBmp;
    }

    @Override
    public int setRtcPrivateParam(String privateParam) {
        if (mTalkEngine == null) {
            ALog.getInstance().e(TAG, "<setRtcPrivateParam> bad status");
            return ErrCode.XERR_BAD_STATE;
        }

        int ret = mTalkEngine.setParameters(privateParam);
        return (ret == Constants.ERR_OK) ? ErrCode.XOK : ErrCode.XERR_INVALID_PARAM;
    }

    @Override
    public int getOnlineUserCount() {
        synchronized (mDataLock) {
            return mOnlineUserCount;
        }
    }


    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////// APP端发送RESTful请求到服务器 //////////////////////
    /////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 工作线程中运行，处理AWS要求切换到被叫状态事件
     */
    void DoAwsEventIncoming(Message msg) {
        int stateMachine = getStateMachine();

        if (stateMachine == CALLKIT_STATE_HANGUP_REQING) {
            ALog.getInstance().e(TAG, "<DoAwsEventToIncoming> hangup ongoing, do nothing");
            return;
        }
        if (stateMachine != CALLKIT_STATE_IDLE) {    // 不是在空闲状态中来电，呼叫状态有问题
            ALog.getInstance().e(TAG, "<DoAwsEventToIncoming> bad state machine, ignore it, stateMachine" + stateMachine);
            return;
        }

        JSONObject jsonState = (JSONObject)(msg.obj);
        ALog.getInstance().d(TAG, "<DoAwsEventIncoming> jsonState=" + jsonState + ", Peer incoming call...");

        // 解析来电数据包
        String traceId = parseJsonStringValue(jsonState, "traceId", null);
        String rtcToken = parseJsonStringValue(jsonState, "token", null);
        int localUid = parseJsonIntValue(jsonState, "uid", 0);
        String chnlName = parseJsonStringValue(jsonState, "cname", null);
        String extraMsg = parseJsonStringValue(jsonState, "extraMsg", null);
        String deviceId = parseJsonStringValue(jsonState, "peerId", null);
        if (TextUtils.isEmpty(chnlName) || TextUtils.isEmpty(deviceId)) {
            ALog.getInstance().e(TAG, "<DoAwsEventToIncoming> parse parameter error, ignore it");
            return;
        }

        AccountMgr.AccountInfo accountInfo = mSdkInstance.getAccountInfo();
        IAgoraIotAppSdk.InitParam sdkInitParam = mSdkInstance.getInitParam();

        synchronized (mDataLock) {
            mStateMachine = CALLKIT_STATE_INCOMING;      // 切换当前状态机到来电

            mCallkitCtx = new CallkitContext();
            mCallkitCtx.appId = sdkInitParam.mRtcAppId;
            mCallkitCtx.callerId = deviceId;
            mCallkitCtx.calleeId = accountInfo.mInventDeviceName;
            mCallkitCtx.channelName = chnlName;
            mCallkitCtx.rtcToken = rtcToken;
            mCallkitCtx.attachMsg = extraMsg;
            mCallkitCtx.mLocalUid = localUid;
            mCallkitCtx.mPeerUid = DEFAULT_DEV_UID;
            mCallkitCtx.callStatus = 3;

            // 这里需要从已有的绑定设备列表中，找到相应的绑定设备
            DeviceMgr deviceMgr = (DeviceMgr)(mSdkInstance.getDeviceMgr());
            IotDevice iotDevice = deviceMgr.findBindDeviceByDevMac(mCallkitCtx.callerId);
            if (iotDevice == null) {
                mPeerDevice = new IotDevice();
                mPeerDevice.mDeviceName = mCallkitCtx.callerId;
                mPeerDevice.mDeviceID = mCallkitCtx.callerId;
                ALog.getInstance().e(TAG, "<DoAwsEventIncoming> cannot found incoming device"
                        + ", callerId=" + mCallkitCtx.callerId);
            } else {
                mPeerDevice = iotDevice;
            }
        }

        // 进入频道，准备被叫通话
        talkingPrepare(chnlName, rtcToken, localUid, DEFAULT_DEV_UID);

        // 启动接听超时定时器
        answerTimeoutStart();

        // 回调对端来电
        CallbackPeerIncoming(mPeerDevice, mCallkitCtx.attachMsg);
    }

    /**
     * @brief 工作线程中运行，接听处理
     */
    void DoRequestAnswer(Message msg) {
        if (getStateMachine() != CALLKIT_STATE_ANSWER_REQING) {
            ALog.getInstance().e(TAG, "<DoRequestAnswer> failure, bad status, state=" + getStateMachine());
            return;
        }

        // 在频道内推送音频流，开始通话
        talkingStart();

        // 切换到 正在通话中状态
        setStateMachine(CALLKIT_STATE_TALKING);

        ALog.getInstance().d(TAG, "<DoRequestAnswer> done");
    }


    /**
     * @brief 工作线程中运行，处理接听超时
     */
    void DoAnswerTimeout(Message msg) {
        int currState = getStateMachine();
        if (currState != CALLKIT_STATE_INCOMING) {
            ALog.getInstance().e(TAG, "<DoAnswerTimeout> bad state, currState=" + currState);
            return;
        }

        // 停止接听超时记时
        answerTimeoutStop();

        // 停止呼叫超时记时
        dialTimeoutStop();

        // 结束通话
        talkingStop();

        ALog.getInstance().d(TAG, "<DoAnswerTimeout> done");
    }





    /**
     * @brief 工作线程中运行，HTTP请求有响应
     */
    void DoDialResponse(Message msg) {
        int currState = getStateMachine();
        if (currState != CALLKIT_STATE_DIAL_REQING) {
            ALog.getInstance().e(TAG, "<DoDialResponse> failure, bad status, state=" + getStateMachine());
            return;
        }
        ALog.getInstance().d(TAG, "<DoDialResponse> Enter");
        Object[] callParams = (Object[]) (msg.obj);
        IotDevice iotDevice = (IotDevice)(callParams[0]);
        String attachMsg = (String)(callParams[1]);
        AgoraService.CallReqResult dialResult = (AgoraService.CallReqResult)(callParams[2]);

        // Token过期统一处理
        processTokenErrCode(dialResult.mErrCode);

        if (dialResult.mErrCode != ErrCode.XOK)   {  // 呼叫失败
            ALog.getInstance().d(TAG, "<DoDialResponse> Exit with failure, errCode=" + dialResult.mErrCode);
            talkingStop();      // 此时应该还没有进入频道，复位状态
            dialTimeoutStop();  // 停止呼叫超时定时器
            CallbackCallDialDone(dialResult.mErrCode, iotDevice); // 回调主叫拨号失败
            return;
        }

        // 更新呼叫上下文数据
        synchronized (mDataLock) {
            mCallkitCtx = dialResult.mCallkitCtx;
        }

        // 切换到 正在呼叫中
        setStateMachine(CALLKIT_STATE_DIALING);

        // 重新启动呼叫超时定时器
        dialTimeoutStart();

        // 进入频道，准备主叫通话
        talkingPrepare(dialResult.mCallkitCtx.channelName, dialResult.mCallkitCtx.rtcToken,
                        dialResult.mCallkitCtx.mLocalUid, dialResult.mCallkitCtx.mPeerUid);

        ALog.getInstance().d(TAG, "<DoDialResponse> Exit, mCallkitCtx=" + mCallkitCtx.toString());

        // 回调主叫拨号成功
        CallbackCallDialDone(ErrCode.XOK, mPeerDevice);
    }

    /**
     * @brief 工作线程中运行，呼叫超时事件（APP主叫后设备端一直未上线）
     */
    void DoDialTimeout(Message msg) {
        int currState = getStateMachine();
        if( (currState != CALLKIT_STATE_DIALING) && (currState != CALLKIT_STATE_DIAL_REQING)) {  // AWS事件 Idle-->Dial已经过来了
            ALog.getInstance().e(TAG, "<DoDialTimeout> bad state, currState=" + currState);
            return;
        }

        // 停止呼叫超时记时
        dialTimeoutStop();

        // 结束通话
        IotDevice iotDevice = mPeerDevice;
        talkingStop();

        // 回调呼叫失败
        CallbackPeerTimeout(iotDevice);

        ALog.getInstance().d(TAG, "<DoDialTimeout> done");
    }

    /*
     * @brief 工作线程中运行，进行挂断退出频道处理
     */
    void DoRequestHangup(Message msg) {
        ALog.getInstance().d(TAG, "<DoRequestHangup> Enter");

        // 停止呼叫超时定时器
        dialTimeoutStop();

        // 停止接听超时
        answerTimeoutStop();

        // 结束通话，清除信息，设置状态到空闲，
        talkingStop();

        ALog.getInstance().d(TAG, "<DoRequestHangup> Exit");
        synchronized (mReqHangupEvent) {
            mReqHangupEvent.notify();    // 事件通知
        }
    }



    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////// 通话处理方法 /////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    /*
     * @brief 主叫或者被叫时准备通话，本地不推流，订阅对端音视频流
     */
    void talkingPrepare(final String channelName, final String rtcToken,
                        int localUid, int peerUid) {

        if (mTalkEngine == null) {  // RTC引擎未创建

            // 根据SDK初始化参数创建 RTC通话引擎
            IAgoraIotAppSdk.InitParam sdkInitParam = mSdkInstance.getInitParam();
            mTalkEngine = new TalkingEngine();
            TalkingEngine.InitParam talkInitParam = mTalkEngine.new InitParam();
            talkInitParam.mContext = sdkInitParam.mContext;
            talkInitParam.mAppId = sdkInitParam.mRtcAppId;
            talkInitParam.mCallback = this;
            talkInitParam.mPublishVideo = sdkInitParam.mPublishVideo;
            talkInitParam.mPublishAudio = sdkInitParam.mPublishAudio;
            talkInitParam.mSubscribeAudio = sdkInitParam.mSubscribeAudio;
            talkInitParam.mSubscribeVideo = sdkInitParam.mSubscribeVideo;
            mTalkEngine.initialize(talkInitParam);

            mMuteLocalVideo = (!sdkInitParam.mPublishVideo);
            mMuteLocalAudio = (!sdkInitParam.mPublishAudio);

            // 设置RTC初始参数
            mTalkEngine.setPeerUid(peerUid);
            mTalkEngine.joinChannel(channelName, rtcToken, localUid);
            mTalkEngine.muteLocalVideoStream(mMuteLocalVideo);     // 本地不推视频流
            mTalkEngine.muteLocalAudioStream(mMuteLocalAudio);     // 本地不推音频流
            mTalkEngine.mutePeerVideoStream(mMutePeerVideo);     // 订阅对端视频流
            mTalkEngine.mutePeerAudioStream(mMutePeerAudio);     // 订阅对端音频流
            mTalkEngine.setAudioEffect(mAudioEffect);       // 设置音频效果

            synchronized (mDataLock) {  // 至少有一个在线用户了
                mOnlineUserCount = 1;
                mAudioEffect = Constants.AUDIO_EFFECT_OFF;  // 默认无变声
            }
        }
    }

    /*
     * @brief 应答对方或者对方应答后，奔溃开始推音频流，通话
     */
    void talkingStart() {
        synchronized (mDataLock) {
            mStateMachine = CALLKIT_STATE_TALKING;  // 切换到 通话状态机
        }

        if (mTalkEngine.isInChannel()) {   // 已经在频道内进行处理
            mTalkEngine.muteLocalAudioStream(mMuteLocalAudio);    // 本地推送音频流
        } else {
            ALog.getInstance().e(TAG, "<talkingStart> NOT in a channel");
        }
    }

    /*
     * @brief 停止通话，状态机切换到空闲，清除对端设备和peerUid
     */
    void talkingStop() {
        if (mTalkEngine != null) {  // 释放RTC通话引擎SDK
            mTalkEngine.leaveChannel();     // 离开频道，结束通话
            mTalkEngine.release();
            mTalkEngine = null;
        }

        synchronized (mDataLock) {      // 清除当前呼叫上下文数据，恢复状态
            mStateMachine = CALLKIT_STATE_IDLE;
            mCallkitCtx = null;
            mOnlineUserCount = 0;
            mAudioEffect = Constants.AUDIO_EFFECT_OFF;  // 默认无变声
        }
    }


    /*
     * @brief 异常情况下的处理
     *          主动挂断，停止通话，状态机切换到空闲，清除对端设备和peerUid
     */
    void exceptionProcess() {
        dialTimeoutStop();  // 停止拨号超时
        answerTimeoutStop();  // 停止接听超时
        talkingStop();  // 挂断通话
        
        ALog.getInstance().d(TAG, "<exceptionProcess> done");
    }

    /**
     * @brief 启动呼叫超时定时器
     */
    void dialTimeoutStart() {
        sendSingleMessage(MSGID_CALL_DIAL_TIMEOUT, 0, 0, null, DIAL_WAIT_TIMEOUT);
    }

    /**
     * @brief 停止呼叫超时定时器
     */
    void dialTimeoutStop() {
        synchronized (mMsgQueueLock) {
            mWorkHandler.removeMessages(MSGID_CALL_DIAL_TIMEOUT);
        }
    }

    /**
     * @brief 启动接听超时定时器
     */
    void answerTimeoutStart() {
        sendSingleMessage(MSGID_CALL_ANSWER_TIMEOUT, 0, 0, null, ANSWER_WAIT_TIMEOUT);
    }

    /**
     * @brief 停止接听超时定时器
     */
    void answerTimeoutStop() {
        synchronized (mMsgQueueLock) {
            mWorkHandler.removeMessages(MSGID_CALL_ANSWER_TIMEOUT);
        }
    }


    /////////////////////////////////////////////////////////////////////////////
    //////////////////// TalkingEngine.ICallback 回调处理 ////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    @Override
    public void onTalkingPeerJoined(int localUid, int peerUid) {
        int stateMachine = getStateMachine();
        ALog.getInstance().d(TAG, "<onTalkingPeerJoined> localUid=" + localUid
                + ", peerUid=" + peerUid
                + ", stateMachine=" + stateMachine);
        if (getStateMachine() == CALLKIT_STATE_HANGUP_REQING) {
            return;
        }

        // 发送对端RTC上线事件
        sendSingleMessage(MSGID_CALL_RTC_PEER_ONLINE, localUid, peerUid, null, 0);
    }

    @Override
    public void onTalkingPeerLeft(int localUid, int peerUid, int reason) {
        int stateMachine = getStateMachine();
        ALog.getInstance().d(TAG, "<onTalkingPeerLeft> localUid=" + localUid
                + ", peerUid=" + peerUid + ", reason=" + reason
                + ", stateMachine=" + stateMachine);
        if (getStateMachine() == CALLKIT_STATE_HANGUP_REQING) {
            return;
        }

        // 发送对端RTC掉线事件
        Object leftParam = new Object[]{ (Integer)localUid, (Integer)peerUid, (Integer)reason};
        sendSingleMessage(MSGID_CALL_RTC_PEER_OFFLINE, 0, 0, leftParam, 0);
    }

    @Override
    public void onPeerFirstVideoDecoded(int peerUid, int videoWidth, int videoHeight) {
        int stateMachine = getStateMachine();
        ALog.getInstance().d(TAG, "<onPeerFirstVideoDecoded> peerUid=" + peerUid
                + ", videoWidth=" + videoWidth
                + ", videoHeight=" + videoHeight);
        if (getStateMachine() == CALLKIT_STATE_HANGUP_REQING) {
            return;
        }

        // 发送对端RTC首帧出图事件
        sendSingleMessage(MSGID_CALL_RTC_PEER_FIRSTVIDEO, videoWidth, videoHeight, null, 0);
    }


    /////////////////////////////////////////////////////////////////////////////
    //////////////////////////// RTC Engine异常相关处理 //////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    /*
     * @brief 工作线程中运行，对端RTC上线
     */
    void DoRtcPeerOnline(Message msg) {
        int localUid = msg.arg1;
        int peerUid = msg.arg2;
        int stateMachine = getStateMachine();
        ALog.getInstance().d(TAG, "<DoRtcPeerOnline> localUid=" + localUid
                + ", peerUid=" + peerUid
                + ", stateMachine=" + stateMachine);
        if (stateMachine == CALLKIT_STATE_HANGUP_REQING) {
            return;
        }

        // 停止呼叫超时定时器
        dialTimeoutStop();

        // 如果当前正在主叫状态，则回调对端应答
        if (stateMachine == CALLKIT_STATE_DIALING) {
            ALog.getInstance().d(TAG, "<DoRtcPeerOnline> Peer answer, enter talking...");

            // 进入通话状态
            setStateMachine(CALLKIT_STATE_TALKING);

            // 回调对端应答
            CallbackPeerAnswer(ErrCode.XOK, mPeerDevice);
        }


        if ((mPeerVidew != null) && (mTalkEngine != null)) {
            mTalkEngine.setRemoteVideoView(mPeerVidew);
        }
    }

    /*
     * @brief 工作线程中运行，对端RTC下线
     */
    void DoRtcPeerOffline(Message msg) {
        Object[] offlineParam = (Object[])(msg.obj);
        int localUid = (Integer)(offlineParam[0]);
        int peerUid = (Integer)(offlineParam[1]);
        int reason = (Integer)(offlineParam[2]);
        int stateMachine = getStateMachine();
        ALog.getInstance().d(TAG, "<DoRtcPeerOffline> localUid=" + localUid
                + ", peerUid=" + peerUid + ", reason=" + reason
                + ", stateMachine=" + stateMachine);

        if (reason == 0) {  // 对端主动退出RTC
            if (stateMachine == CALLKIT_STATE_INCOMING ||
                stateMachine == CALLKIT_STATE_DIALING ||
                stateMachine == CALLKIT_STATE_INCOMING ||
                stateMachine == CALLKIT_STATE_TALKING)
            {
                IotDevice callbackDev = mPeerDevice;
                dialTimeoutStop();  // 停止呼叫超时
                answerTimeoutStop();  // 停止接听超时
                talkingStop();   // 结束通话

                CallbackPeerHangup(callbackDev);   // 回调对端挂断
            }

        } else { // 对端丢包太多后掉线

        }
    }

    /*
     * @brief 工作线程中运行，对端RTC首帧出图
     */
    void DoRtcPeerFirstVideo(Message msg) {
        int width = msg.arg1;
        int height = msg.arg2;
        int stateMachine = getStateMachine();
        ALog.getInstance().d(TAG, "<DoRtcPeerFirstVideo> width=" + width
                + ", height=" + height);

        if ((stateMachine != CALLKIT_STATE_IDLE) && (stateMachine != CALLKIT_STATE_HANGUP_REQING)) {
            IotDevice callbackDev = mPeerDevice;

            // 回调对端首帧出图
            synchronized (mCallbackList) {
                for (ICallkitMgr.ICallback listener : mCallbackList) {
                    listener.onPeerFirstVideo(callbackDev, width, height);
                }
            }
        }
    }

    @Override
    public void onUserOnline(int uid) {
        boolean eventReport = false;
        synchronized (mDataLock) {
            if (uid != mCallkitCtx.mPeerUid) {  // 不是设备端
                mOnlineUserCount++;
                eventReport = true;
            }
        }
        ALog.getInstance().d(TAG, "<onUserOnline> uid=" + uid
                + ", mOnlineUserCount=" + mOnlineUserCount);
        if (getStateMachine() == CALLKIT_STATE_HANGUP_REQING) {
            return;
        }

        if (eventReport) {  // 回调用户上线事件
            ALog.getInstance().d(TAG, "<onUserOnline> callback online event");
            synchronized (mCallbackList) {
                for (ICallkitMgr.ICallback listener : mCallbackList) {
                    listener.onUserOnline(uid, mOnlineUserCount);
                }
            }
        }
    }

    @Override
    public void onUserOffline(int uid) {
        boolean eventReport = false;
        synchronized (mDataLock) {
            if (uid != mCallkitCtx.mPeerUid) {  // 不是设备端
                mOnlineUserCount--;
                eventReport = true;
            }
        }
        ALog.getInstance().d(TAG, "<onUserOffline> uid=" + uid
                + ", mOnlineUserCount=" + mOnlineUserCount);
        if (getStateMachine() == CALLKIT_STATE_HANGUP_REQING) {
            return;
        }

        if (eventReport) {  // 回调用户下线事件
            ALog.getInstance().d(TAG, "<onUserOffline> callback offline event");
            synchronized (mCallbackList) {
                for (ICallkitMgr.ICallback listener : mCallbackList) {
                    listener.onUserOffline(uid, mOnlineUserCount);
                }
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////// 录像的处理 //////////////////////////////
    /////////////////////////////////////////////////////////////////////////////

    /*
     * @brief 工作线程中运行，发送HTTP呼叫请求
     */
    void DoRecordingError(Message msg) {
        int errCode = msg.arg1;
        ALog.getInstance().e(TAG, "<DoRecordingError> errCode=" + errCode);

        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onRecordingError(errCode);
            }
        }
    }


    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////// 所有的对上层回调处理 //////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    void CallbackCallDialDone(int errCode, IotDevice iotDevice) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onDialDone(errCode, iotDevice);
            }
        }
    }

    void CallbackPeerIncoming(IotDevice iotDevice, String attachMsg) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onPeerIncoming(iotDevice, attachMsg);
            }
        }
    }

    void CallbackPeerAnswer(int errCode, IotDevice iotDevice) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onPeerAnswer(iotDevice);
            }
        }
    }

    void CallbackPeerHangup(IotDevice iotDevice) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onPeerHangup(iotDevice);
            }
        }
    }

    void CallbackPeerTimeout(IotDevice iotDevice) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onPeerTimeout(iotDevice);
            }
        }
    }

    void CallbackError(int errCode) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onCallkitError(errCode);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////////// Inner Methods //////////////////////////
    ///////////////////////////////////////////////////////////////////////
    void setStateMachine(int newStateMachine) {
        synchronized (mDataLock) {
            mStateMachine = newStateMachine;
        }
    }


    String getStateMachineTip(int callStatus) {
        if (callStatus == CALLKIT_STATE_IDLE) {
            return "1(Idle)";
        } else if (callStatus == CALLKIT_STATE_DIALING) {
            return "2(Dial)";
        } else if (callStatus == CALLKIT_STATE_INCOMING) {
            return "3(Incoming)";
        } else if (callStatus == CALLKIT_STATE_TALKING) {
            return "4(Talking)";
        } else if (callStatus == CALLKIT_STATE_DIAL_REQING) {
            return "5(Dial_Requesting)";
        } else if (callStatus == CALLKIT_STATE_DIAL_RSPING) {
            return "6(Dial_Responsing)";
        } else if (callStatus == CALLKIT_STATE_ANSWER_REQING) {
            return "7(Answer_Requesting)";
        } else if (callStatus == CALLKIT_STATE_ANSWER_RSPING) {
            return "8(Answer_Responsing)";
        } else if (callStatus == CALLKIT_STATE_HANGUP_REQING) {
            return "9(Hangup_Requesting)";
        }

        return (callStatus + "(Unknown)");
    }



    /**
     * @brief 统一处理Token过期错误码
     */
    void processTokenErrCode(int errCode) {
        if (errCode == ErrCode.XERR_TOKEN_INVALID)    {
            AccountMgr accountMgr = (AccountMgr)(mSdkInstance.getAccountMgr());
            accountMgr.onTokenInvalid();
        }
    }



    //////////////////////////////////////////////////////////////////////////////
    ///////////////////////////// Methods for JSON parse /////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    int parseJsonIntValue(JSONObject jsonState, String fieldName, int defVal) {
        try {
            int value = jsonState.getInt(fieldName);
            return value;

        } catch (JSONException e) {
            ALog.getInstance().e(TAG, "<parseJsonIntValue> "
                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return defVal;
        }
    }

    long parseJsonLongValue(JSONObject jsonState, String fieldName, long defVal) {
        try {
            long value = jsonState.getLong(fieldName);
            return value;

        } catch (JSONException e) {
            ALog.getInstance().e(TAG, "<parseJsonLongValue> "
                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return defVal;
        }
    }

    boolean parseJsonBoolValue(JSONObject jsonState, String fieldName, boolean defVal) {
        try {
            boolean value = jsonState.getBoolean(fieldName);
            return value;

        } catch (JSONException e) {
            ALog.getInstance().e(TAG, "<parseJsonBoolValue> "
                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return defVal;
        }
    }

    String parseJsonStringValue(JSONObject jsonState, String fieldName, String defVal) {
        try {
            String value = jsonState.getString(fieldName);
            return value;

        } catch (JSONException e) {
            ALog.getInstance().e(TAG, "<parseJsonIntValue> "
                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return defVal;
        }
    }

}
