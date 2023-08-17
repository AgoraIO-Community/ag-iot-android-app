/**
 * @file AccountMgr.java
 * @brief This file implement the call kit and RTC management
 *
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2023-05-19
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.sdkimpl;


import android.os.Message;
import android.text.TextUtils;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.base.BaseThreadComp;
import io.agora.iotlink.callkit.DisplayViewMgr;
import io.agora.iotlink.callkit.SessionCtx;
import io.agora.iotlink.callkit.SessionMgr;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.rtcsdk.TalkingEngine;
import io.agora.iotlink.transport.TransPacket;
import io.agora.iotlink.transport.TransPktQueue;
import io.agora.iotlink.utils.JsonUtils;
import io.agora.rtc2.Constants;

/*
 * @brief 呼叫系统管理器
 */
public class CallkitMgr extends BaseThreadComp implements ICallkitMgr, TalkingEngine.ICallback {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/CallkitMgr";
    private static final long TIMER_INTERVAL = 2000;             ///< 定时器间隔 2秒
    private static final long DIAL_WAIT_TIMEOUT = 30000;          ///< 呼叫超时请求 30秒
    private static final long ANSWER_WAIT_TIMEOUT = 30000;        ///< 接听处理超时 30秒
    private static final long DEVONLINE_WAIT_TIMEOUT = 15000;    ///< 来电时设备上线超时15秒
    private static final int DEFAULT_DEV_UID = 10;               ///< 设备端uid，固定为10

    //
    // The method of all callkit
    //
    private static final String METHOD_USER_START_CALL = "user-start-call";
    private static final String METHOD_DEVICE_START_CALL = "device-start-call";



    //
    // The mesage Id
    //
    private static final int MSGID_CALL_BASE = 0x3000;
    private static final int MSGID_CALL_RECV_PKT = 0x3001;          ///< 处理数据包接收
    private static final int MSGID_CALL_RTC_PEER_ONLINE = 0x3002;   ///< 对端RTC上线
    private static final int MSGID_CALL_RTC_PEER_OFFLINE = 0x3003;  ///< 对端RTC掉线
    private static final int MSGID_CALL_RTC_PEER_FIRSTVIDEO = 0x3004;  ///< 对端RTC首帧出图
    private static final int MSGID_CLL_RTC_SHOTTAKEN = 0x3005;      ///< 截图完成回调
    private static final int MSGID_RECORDING_ERROR = 0x3009;        ///< 录像出现错误
    private static final int MSGID_CALL_TIMER = 0x30FF;             ///< 线程中的定时器


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private ArrayList<ICallkitMgr.ICallback> mCallbackList = new ArrayList<>();
    private AgoraIotAppSdk mSdkInstance;    ///< 由外部输入的
    private TransPktQueue mRecvPktQueue = new TransPktQueue();  ///< 接收数据包队列
    private String mAppId;

    private DisplayViewMgr mViewMgr = new DisplayViewMgr();
    private SessionMgr mSessionMgr = new SessionMgr();
    private TalkingEngine mTalkEngine = new TalkingEngine();   ///< 通话引擎
    private static final Object mTalkEngLock = new Object();   ///< 通话引擎同步访问锁


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    int initialize(AgoraIotAppSdk sdkInstance) {
        ALog.getInstance().d(TAG, "<initialize> ==>Enter");
        mSdkInstance = sdkInstance;

        IAgoraIotAppSdk.InitParam sdkInitParam = sdkInstance.getInitParam();
        mAppId = sdkInitParam.mAppId;

        mSessionMgr.clear();
        mViewMgr.clear();
        mRecvPktQueue.clear();

        // 启动呼叫系统的工作线程
        runStart(TAG);

        sendSingleMessage(MSGID_CALL_TIMER, 0, 0, null,0);
        ALog.getInstance().d(TAG, "<initialize> <==Exit");
        return ErrCode.XOK;
    }

    void release() {
        ALog.getInstance().d(TAG, "<release> ==>Enter");
        runStop();

        mSessionMgr.clear();
        mViewMgr.clear();
        mRecvPktQueue.clear();
        ALog.getInstance().d(TAG, "<release> <==Exit");

    }



    ///////////////////////////////////////////////////////////////////////////
    //////////////// Methods for Override BaseThreadComp //////////////////////
    //////////////////////////////////////////////////////////////////////////
    @Override
    public void processWorkMessage(Message msg) {
        switch (msg.what) {
            case MSGID_CALL_RECV_PKT:
                DoMqttRecvPacket(msg);
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
            case MSGID_CLL_RTC_SHOTTAKEN:
                onRtcSnapshotTaken(msg);
                break;

            case MSGID_RECORDING_ERROR:
                DoRecordingError(msg);
                break;

            case MSGID_CALL_TIMER:
                DoTimer(msg);
                break;
        }
    }

    @Override
    protected void removeAllMessages() {
        synchronized (mMsgQueueLock) {
            mWorkHandler.removeMessages(MSGID_CALL_RECV_PKT);
            mWorkHandler.removeMessages(MSGID_CALL_RTC_PEER_ONLINE);
            mWorkHandler.removeMessages(MSGID_CALL_RTC_PEER_OFFLINE);
            mWorkHandler.removeMessages(MSGID_CALL_RTC_PEER_FIRSTVIDEO);
            mWorkHandler.removeMessages(MSGID_CLL_RTC_SHOTTAKEN);
            mWorkHandler.removeMessages(MSGID_RECORDING_ERROR);
            mWorkHandler.removeMessages(MSGID_CALL_TIMER);
        }
    }

    @Override
    protected void processTaskFinsh() {

        // 退出所有频道，释放通话引擎
        talkingRelease();

        synchronized (mCallbackList) {
            mCallbackList.clear();
        }

        ALog.getInstance().d(TAG, "<processTaskFinsh> done");
    }



    ///////////////////////////////////////////////////////////////////////
    /////////////////// Override Methods of ICallkitMgr //////////////////
    ///////////////////////////////////////////////////////////////////////
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
    public SessionInfo getSessionInfo(final UUID sessionId) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<getSessionInfo> not found session, sessionId=" + sessionId);
            return null;
        }

        LocalNode localNode = mSdkInstance.getLoalNode();
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.mSessionId = sessionId;
        sessionInfo.mLocalUserId = localNode.mUserId;
        sessionInfo.mLocalNodeId = localNode.mNodeId;
        sessionInfo.mPeerNodeId = sessionCtx.mDevNodeId;
        sessionInfo.mState = sessionCtx.mState;
        sessionInfo.mType = sessionCtx.mType;
        sessionInfo.mUserCount = sessionCtx.mUserCount;

        ALog.getInstance().d(TAG, "<getSessionInfo> sessionInfo=" + sessionInfo);
        return sessionInfo;
    }


    @Override
    public DialResult callDial(final DialParam dialParam)   {
        long t1 = System.currentTimeMillis();
        DialResult result = new DialResult();
        LocalNode loalNode = mSdkInstance.getLoalNode();
        int sdkState = mSdkInstance.getStateMachine();

        // SDK状态检测
        if (sdkState != IAgoraIotAppSdk.SDK_STATE_RUNNING) {
            ALog.getInstance().e(TAG, "<callDial> bad state, SDK not running"
                + ", sdkState=" + sdkState);
            result.mErrCode = ErrCode.XERR_NETWORK;
            return result;
        }
        SessionCtx sessionCtx = mSessionMgr.findSessionByDevNodeId(dialParam.mPeerNodeId);
        if (sessionCtx != null) {
            ALog.getInstance().e(TAG, "<callDial> bad state, already in session");
            result.mErrCode = ErrCode.XERR_BAD_STATE;
            return result;
        }

        // 发送请求消息
        ALog.getInstance().d(TAG, "<callDial> ==> BEGIN, dialParam=" + dialParam);
        UUID sessionId = UUID.randomUUID();
        long traceId = System.currentTimeMillis();

        // body内容
        JSONObject body = new JSONObject();
        try {
            JSONObject header = new JSONObject();
            header.put("traceId", traceId );
            header.put("timestamp", System.currentTimeMillis());
            header.put("nodeToken", loalNode.mToken);
            header.put("method", METHOD_USER_START_CALL);
            body.put("header", header);

            JSONObject payloadObj = new JSONObject();
            payloadObj.put("appId", mAppId);
            payloadObj.put("deviceId", dialParam.mPeerNodeId);
            payloadObj.put("extraMsg", dialParam.mAttachMsg);
            body.put("payload", payloadObj);

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<callDial>> [Exit] failure with JSON exp!");
            result.mErrCode = ErrCode.XERR_JSON_WRITE;
            return result;
        }


        SessionCtx newSession = new SessionCtx();
        newSession.mSessionId = sessionId;
        newSession.mLocalNodeId = loalNode.mNodeId;
        newSession.mDevNodeId = dialParam.mPeerNodeId;
        newSession.mPeerUid = DEFAULT_DEV_UID;
        newSession.mAttachMsg = dialParam.mAttachMsg;
        newSession.mTimestamp = System.currentTimeMillis();
        newSession.mState = SESSION_STATE_DIAL_REQING;        // 会话状态机: 呼叫请求中
        newSession.mType = SESSION_TYPE_DIAL;                 // 主叫
        newSession.mPubLocalAudio = dialParam.mPubLocalAudio; // 接通后是否本地推流
        newSession.mSubDevAudio = true;     // 订阅设备端音音频频流
        newSession.mSubDevVideo = true;     // 订阅设备端视频流
        newSession.mTraceId = traceId;

        // 添加到会话管理器中
        mSessionMgr.addSession(newSession);

        // 发送主叫的 MQTT呼叫请求数据包
        TransPacket transPacket = new TransPacket();
        transPacket.mTopic = mSdkInstance.getMqttPublishTopic();
        transPacket.mContent = body.toString();
        transPacket.mSessionId = sessionId;
        mSdkInstance.sendPacket(transPacket);

        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<callDial> <==End done" + ", costTime=" + (t2-t1));
        result.mSessionId = newSession.mSessionId;
        result.mErrCode = ErrCode.XOK;
        return result;
    }

    @Override
    public int callHangup(final UUID sessionId) {
        long t1 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<callHangup> ==> BEGIN");

        // 在MQTT发送队列中删除主叫呼叫请求
        mSdkInstance.removePacketBySessionId(sessionId);

        // 会话管理器中直接删除该会话
        SessionCtx sessionCtx = mSessionMgr.removeSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<callHangup> <==END, already hangup, not found");
            return ErrCode.XOK;
        }

        // 离开通话频道
        talkingStop(sessionCtx);

        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<callHangup> <==END, costTime=" + (t2-t1));
        return ErrCode.XOK;
    }

    @Override
    public int callAnswer(final UUID sessionId, boolean pubLocalAudio) {
        long t1 = System.currentTimeMillis();

        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<callAnswer> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }
        if (sessionCtx.mState != SESSION_STATE_INCOMING) {
            ALog.getInstance().e(TAG, "<callAnswer> not incoming state, state=" + sessionCtx.mState);
            return ErrCode.XERR_BAD_STATE;
        }

        // 更新 sessionCtx内容
        sessionCtx.mPubLocalAudio = pubLocalAudio;
        mSessionMgr.updateSession(sessionCtx);

        // 进入通话操作
        talkingStart(sessionCtx);

        // 更新会话状态机
        sessionCtx.mState = SESSION_STATE_TALKING;
        mSessionMgr.updateSession(sessionCtx);

        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<callAnswer> done, costTime=" + (t2-t1));
        return ErrCode.XOK;
    }


    @Override
    public int setPeerVideoView(final UUID sessionId, final View peerView) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<setPeerVideoView> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        boolean ret = true;
        if (sessionCtx != null) {  // 当前有相应的会话，直接更新设备显示控件
            synchronized (mTalkEngLock) {
                if (mTalkEngine.isReady()) {
                    ret = mTalkEngine.setRemoteVideoView(sessionCtx, peerView);
                }
            }
        }

        // 设置设备映射控件
        mViewMgr.setDisplayView(sessionCtx.mDevNodeId, peerView);

        ALog.getInstance().d(TAG, "<setPeerVideoView> done, ret=" + ret);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }

    @Override
    public int muteLocalAudio(final UUID sessionId, boolean mute) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<muteLocalAudio> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        // 更新 sessionCtx 内容
        sessionCtx.mPubLocalAudio = (!mute);
        mSessionMgr.updateSession(sessionCtx);

        boolean ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.muteLocalAudioStream(sessionCtx);
        }

        ALog.getInstance().d(TAG, "<muteLocalAudio> done, ret=" + ret);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }

    @Override
    public int mutePeerVideo(final UUID sessionId, boolean mute) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<mutePeerVideo> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        // 更新 sessionCtx 内容
        sessionCtx.mSubDevVideo = (!mute);
        mSessionMgr.updateSession(sessionCtx);

        boolean ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.mutePeerVideoStream(sessionCtx);
        }

        ALog.getInstance().d(TAG, "<mutePeerVideo> done, ret=" + ret);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }

    @Override
    public int mutePeerAudio(final UUID sessionId, boolean mute) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<mutePeerAudio> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        // 更新 sessionCtx 内容
        sessionCtx.mSubDevAudio = (!mute);
        mSessionMgr.updateSession(sessionCtx);

        boolean ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.mutePeerAudioStream(sessionCtx);
        }

        ALog.getInstance().d(TAG, "<mutePeerAudio> done, ret=" + ret);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }

    @Override
    public int capturePeerVideoFrame(final UUID sessionId, final String saveFilePath) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<capturePeerVideoFrame> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        boolean ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.takeSnapshot(sessionCtx, saveFilePath);
        }

        ALog.getInstance().d(TAG, "<capturePeerVideoFrame> done, ret=" + ret
                + ", saveFilePath=" + saveFilePath);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }

    @Override
    public int talkingRecordStart(final UUID sessionId, final String outFilePath) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<talkingRecordStart> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        int ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.recordingStart(sessionCtx, outFilePath);
        }

        ALog.getInstance().d(TAG, "<talkingRecordStart> done, ret=" + ret
                + ", outFilePath=" + outFilePath);
        return ret;
    }

    @Override
    public int talkingRecordStop(final UUID sessionId) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<talkingRecordStop> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        int ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.recordingStop(sessionCtx);
        }

        ALog.getInstance().d(TAG, "<talkingRecordStop> done, ret=" + ret);
        return ret;
    }

    /**
     * @brief 判断当前是否正在本地录制
     * @return true 表示正在本地录制频道； false: 不在录制
     */
    @Override
    public boolean isTalkingRecording(final UUID sessionId) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<isTalkingRecording> not found session, sessionId=" + sessionId);
            return false;
        }

        boolean recording;
        synchronized (mTalkEngLock) {
            recording = mTalkEngine.isRecording(sessionCtx);
        }

        ALog.getInstance().d(TAG, "<isTalkingRecording> done, recording=" + recording);
        return recording;
    }

    @Override
    public RtcNetworkStatus getNetworkStatus() {
        if (mTalkEngine == null) {
            ALog.getInstance().e(TAG, "<RtcNetworkStatus> bad status");
            return null;
        }

        // 这里不用加锁
        RtcNetworkStatus networkStatus = mTalkEngine.getNetworkStatus();
        return networkStatus;
    }

    @Override
    public int setPlaybackVolume(int volumeLevel) {
        boolean ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.setPlaybackVolume(volumeLevel);
        }

        ALog.getInstance().d(TAG, "<setPlaybackVolume> done, ret=" + ret
                + ", volumeLevel=" + volumeLevel);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }

    @Override
    public int setAudioEffect(AudioEffectId effectId) {
        int voice_changer = Constants.AUDIO_EFFECT_OFF;
        switch (effectId) {
            case OLDMAN:
                voice_changer = Constants.VOICE_CHANGER_EFFECT_OLDMAN;
                break;

            case BABYBOY:
                voice_changer = Constants.VOICE_CHANGER_EFFECT_BOY;
                break;

            case BABYGIRL:
                voice_changer = Constants.VOICE_CHANGER_EFFECT_GIRL;
                break;

            case ZHUBAJIE:
                voice_changer = Constants.VOICE_CHANGER_EFFECT_PIGKING;
                break;

            case ETHEREAL:
                voice_changer = Constants.VOICE_CHANGER_EFFECT_SISTER;
                break;

            case HULK:
                voice_changer = Constants.VOICE_CHANGER_EFFECT_HULK;
                break;
        }

        boolean ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.setAudioEffect(voice_changer);
        }

        ALog.getInstance().d(TAG, "<setAudioEffect> done, ret=" + ret
                + ", voice_changer=" + voice_changer);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }

    @Override
    public AudioEffectId getAudioEffect() {
        int voice_changer;
        synchronized (mTalkEngLock) {
            voice_changer = mTalkEngine.getAudioEffect();
        }

        AudioEffectId effectId = AudioEffectId.NORMAL;
        switch (voice_changer) {
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
        }

        return effectId;
    }

    @Override
    public int setRtcPrivateParam(String privateParam) {
        if (mTalkEngine == null) {
            ALog.getInstance().e(TAG, "<setRtcPrivateParam> bad status");
            return ErrCode.XERR_BAD_STATE;
        }

        int ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.setParameters(privateParam);
        }
        return (ret == Constants.ERR_OK) ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED;
    }


    /////////////////////////////////////////////////////////////////////////////
    //////////////////// TalkingEngine.ICallback 回调处理 ////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    @Override
    public void onTalkingJoinDone(final UUID sessionId, final String channel, int uid) {
    }

    @Override
    public void onTalkingLeftDone(final UUID sessionId) {
    }

    @Override
    public void onUserOnline(final UUID sessionId, int uid, int elapsed) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().w(TAG, "<onUserOnline> session removed, sessionId=" + sessionId
                    + ", uid=" + uid);
            return;
        }
        ALog.getInstance().d(TAG, "<onUserOnline> uid=" + uid + ", sessionCtx=" + sessionCtx);

        if (uid == sessionCtx.mPeerUid) {  // 对端设备加入频道
            // 发送对端设备TC上线事件
            sendSingleMessage(MSGID_CALL_RTC_PEER_ONLINE, 0, 0, sessionId, 0);
            return;
        }

        // 回调其他用户上线事件
        if (uid != sessionCtx.mLocalUid) {
            // 更新session上下文中 用户数量
            sessionCtx.mUserCount++;
            mSessionMgr.updateSession(sessionCtx);

            ALog.getInstance().d(TAG, "<onUserOnline> callback online event");
            CallbackOtherUserOnline(sessionCtx, uid);
        }
    }

    @Override
    public void onUserOffline(final UUID sessionId, int uid, int reason) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().w(TAG, "<onUserOffline> session removed, sessionId=" + sessionId
                    + ", uid=" + uid + ", reason=" + reason);
            return;
        }
        ALog.getInstance().d(TAG, "<onUserOffline> uid=" + uid
                + ", reason=" + reason + ", sessionCtx=" + sessionCtx);

        if (uid == sessionCtx.mPeerUid) {  // 对端设备退出频道
            // 发送对端设备TC掉线事件
            sendSingleMessage(MSGID_CALL_RTC_PEER_OFFLINE, 0, 0, sessionId, 0);
            return;
        }

        // 回调其他用户上线事件
        if (uid != sessionCtx.mLocalUid) {
            // 更新session上下文中 用户数量
            sessionCtx.mUserCount--;
            mSessionMgr.updateSession(sessionCtx);

            ALog.getInstance().d(TAG, "<onUserOffline> callback online event");
            CallbackOtherUserOffline(sessionCtx, uid);
        }
    }

    @Override
    public void onPeerFirstVideoDecoded(final UUID sessionId, int peerUid, int videoWidth, int videoHeight) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().d(TAG, "<onPeerFirstVideoDecoded> session removed"
                    + ", peerUid=" + peerUid + ", width=" + videoWidth + ", height=" + videoHeight );
            return;
        }
        ALog.getInstance().d(TAG, "<onPeerFirstVideoDecoded> sessionCtx=" + sessionCtx
                + ", peerUid=" + peerUid + ", width=" + videoWidth + ", height=" + videoHeight );

        // 发送对端RTC首帧出图事件
        sendSingleMessage(MSGID_CALL_RTC_PEER_FIRSTVIDEO, videoWidth, videoHeight, sessionId, 0);
    }

    @Override
    public void onSnapshotTaken(final UUID sessionId, int uid,
                                final String filePath, int width, int height, int errCode) {
        // 再处理设备通话的会话
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().d(TAG, "<onSnapshotTaken> session removed"
                    + ", uid=" + uid + ", filePath=" + filePath
                    + ", width=" + width + ", height=" + height + ", errCode=" + errCode );
            return;
        }
        ALog.getInstance().d(TAG, "<onSnapshotTaken> sessionCtx=" + sessionCtx
                + ", uid=" + uid + ", filePath=" + filePath
                + ", width=" + width + ", height=" + height + ", errCode=" + errCode );

        // 发送截图完成回调事件
        Object[] params = { sessionId, filePath, width, height, errCode};
        sendSingleMessage(MSGID_CLL_RTC_SHOTTAKEN, 0, 0, params, 0);

    }

    @Override
    public void onRecordingError(final UUID sessionId, int errCode) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().d(TAG, "<onRecordingError> session removed"
                    + ", sessionId=" + sessionId + ", errCode=" + errCode);
            return;
        }
        ALog.getInstance().d(TAG, "<onRecordingError> sessionCtx=" + sessionCtx
                + ", errCode=" + errCode);

        // 发送录像错误回调消息
        sendSingleMessage(MSGID_RECORDING_ERROR, errCode, 0, sessionId, 0);
    }


    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////// 工作线程中各种消息处理 /////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 工作线程中运行，处理接收MQTT订阅数据包
     */
    void DoMqttRecvPacket(Message msg) {
        TransPacket recvedPkt = mRecvPktQueue.dequeue();
        if (recvedPkt == null) {  // 接收队列为空，没有要接收到的数据包要分发了
            return;
        }

        DoMqttParsePacket(recvedPkt);

        // 队列中还有数据包，放到下次消息中处理
        if (mRecvPktQueue.size() > 0) {
            sendSingleMessage(MSGID_CALL_RECV_PKT, 0, 0, null, 0);
        }
    }

    void DoMqttParsePacket(final TransPacket recvedPkt) {

        JSONObject recvJsonObj = JsonUtils.generateJsonObject(recvedPkt.mContent);
        if (recvJsonObj == null) {
            ALog.getInstance().e(TAG, "<DoMqttParsePacket> Invalid json=" + recvedPkt.mContent);
            return;
        }

        JSONObject headJsonObj = JsonUtils.parseJsonObject(recvJsonObj, "header", null);
        JSONObject payloadJsonObj = JsonUtils.parseJsonObject(recvJsonObj,"payload", null);
        int respCode = JsonUtils.parseJsonIntValue(recvJsonObj, "code", 0);

        // 解析 Header 头信息
        if (headJsonObj == null) {
            ALog.getInstance().e(TAG, "<DoMqttParsePacket> NO header json obj");
            return;
        }
        String headerMethod = JsonUtils.parseJsonStringValue(headJsonObj, "method", null);
        if (TextUtils.isEmpty(headerMethod)) {
            ALog.getInstance().e(TAG, "<DoMqttParsePacket> NO method field in header");
            return;
        }
        long traceId = JsonUtils.parseJsonLongValue(headJsonObj, "traceId", -1);
        if (traceId < 0) {
            ALog.getInstance().e(TAG, "<DoMqttParsePacket> NO traceId field in header");
            return;
        }

        if (headerMethod.compareToIgnoreCase(METHOD_USER_START_CALL) == 0) {
            // APP主叫设备回应数据包
            SessionCtx sessionCtx = mSessionMgr.findSessionByTraceId(traceId);
            if (sessionCtx == null) {  // 主叫会话已经不存在了，丢弃该包
                ALog.getInstance().e(TAG, "<DoMqttParsePacket> [DIALING] session NOT found, drop packet!");
                return;
            }
            if (sessionCtx.mState != SESSION_STATE_DIAL_REQING) { // 主叫会话状态不对，丢弃改包
                ALog.getInstance().e(TAG, "<DoMqttParsePacket> [DIALING] state not right"
                        + ", mState=" + sessionCtx.mState + ", drop packet!");
                return;
            }

            if (respCode == ErrCode.XOK) {
                sessionCtx.mRtcToken = JsonUtils.parseJsonStringValue(payloadJsonObj, "token", null);
                sessionCtx.mChnlName = JsonUtils.parseJsonStringValue(payloadJsonObj, "cname", null);
                sessionCtx.mLocalUid = JsonUtils.parseJsonIntValue(payloadJsonObj, "uid", -1);
            }

            // 处理主叫服务器回应完成
            DoDialResponse(sessionCtx, respCode);

        } else if (headerMethod.compareToIgnoreCase(METHOD_DEVICE_START_CALL) == 0) {
            // 设备来电数据包
            if (respCode != ErrCode.XOK) {
                ALog.getInstance().d(TAG, "<DoMqttParsePacket> [INCOMING] response error, respCode=" + respCode);
                return;
            }

            // 判断参数字段，注意: 协议包中这里 peerNodeId 是 APP账号的 NodeId，不是设备的NodeId
            //  这里 peerNodeId 就用不到了，判断哪个设备直接用 cname 来判断
            String rtcToken = JsonUtils.parseJsonStringValue(payloadJsonObj, "token", null);
            String chnlName = JsonUtils.parseJsonStringValue(payloadJsonObj, "cname", null);
            String peerNodeId = JsonUtils.parseJsonStringValue(payloadJsonObj, "peerId", null);
            String attachMsg = JsonUtils.parseJsonStringValue(payloadJsonObj, "extraMsg", null);
            int localUid = JsonUtils.parseJsonIntValue(payloadJsonObj, "uid", -1);
            long timestamp = JsonUtils.parseJsonLongValue(payloadJsonObj, "timestamp", -1);
            long version = JsonUtils.parseJsonLongValue(payloadJsonObj, "version", -1);
            if (TextUtils.isEmpty(rtcToken) || TextUtils.isEmpty(chnlName) || TextUtils.isEmpty(peerNodeId)) {
                ALog.getInstance().d(TAG, "<DoMqttParsePacket> [INCOMING] invalid parameter, drop packet");
                return;
            }

            // 通过频道名判断该设备是否已经在通话
            SessionCtx sessionCtx = mSessionMgr.findSessionByChannelName(chnlName);
            if (sessionCtx != null) {
                ALog.getInstance().e(TAG, "<DoMqttParsePacket> [INCOMING] session already exist, drop packet!");
                return;
            }

            // 处理MQTT来电消息
            DoMqttEventIncoming(traceId, attachMsg, chnlName, rtcToken, localUid);
        }
    }


    /**
     * @brief 工作线程中运行，处理来电事件
     */
    void DoMqttEventIncoming(long traceId, final String attachMsg, final String chnlName, final String rtcToken, int localUid) {
        ALog.getInstance().d(TAG, "<DoMqttEventIncoming> Enter, traceId=" + traceId
                    + ", attachMsg=" + attachMsg + ", chnlName=" + chnlName + ", localUid=" + localUid );

        LocalNode loalNode = mSdkInstance.getLoalNode();
        IAgoraIotAppSdk.InitParam sdkInitParam = mSdkInstance.getInitParam();

        // 添加新会话 到会话管理器中
        SessionCtx newSession = new SessionCtx();
        newSession.mChnlName = chnlName;
        newSession.mRtcToken = rtcToken;
        newSession.mLocalNodeId = loalNode.mNodeId;
        newSession.mDevNodeId = chnlName;  // 这里使用cname作为设备端的 nodeId
        newSession.mAttachMsg = attachMsg;
        newSession.mType = SESSION_TYPE_INCOMING;       // 被叫
        newSession.mState = SESSION_STATE_INCOMING;     // 会话状态机: 来电中
        newSession.mLocalUid = localUid;
        newSession.mPeerUid = DEFAULT_DEV_UID;
        newSession.mTimestamp = System.currentTimeMillis();
        newSession.mTraceId = traceId;
        newSession.mPubLocalAudio = false;  // 不推送本地音频流
        newSession.mSubDevAudio = true;     // 订阅设备音频流
        newSession.mSubDevVideo = true;     // 订阅设备视频流
        mSessionMgr.addSession(newSession);
        ALog.getInstance().w(TAG, "<DoMqttEventIncoming> sessionCtx=" + newSession);

        // 进入频道，准备被叫通话
        talkingPrepare(newSession);

        ALog.getInstance().d(TAG, "<DoMqttEventIncoming> Exit");

        // 回调对端来电
        CallbackPeerIncoming(newSession);
    }

    /**
     * @brief 工作线程中运行，处理主叫回应数据包
     */
    void DoDialResponse(final SessionCtx sessionCtx, int respCode) {
        ALog.getInstance().d(TAG, "<DoDialResponse> Enter");
        SessionCtx tempSession = mSessionMgr.findSessionBySessionId(sessionCtx.mSessionId);
        if (tempSession == null) {  // 该会话已经挂断删除，不做任何处理
            ALog.getInstance().e(TAG, "<DoDialResponse> session already removed, do nothing!");
            return;
        }

        if (respCode != ErrCode.XOK) {  // 服务器返回错误码
            ALog.getInstance().d(TAG, "<DoDialResponse> Exit with error respose, respCode=" + respCode);
            mSessionMgr.removeSession(sessionCtx.mSessionId);   // 删除当前会话
            CallbackCallDialDone(sessionCtx, ErrCode.XERR_HTTP_RESP_CODE); // 回调主叫拨号失败
            return;
        }

        if (TextUtils.isEmpty(sessionCtx.mRtcToken) || TextUtils.isEmpty(sessionCtx.mChnlName)) {
            ALog.getInstance().d(TAG, "<DoDialResponse> Exit with failure, token or chnlName is empty!");
            mSessionMgr.removeSession(sessionCtx.mSessionId);   // 删除当前会话
            CallbackCallDialDone(sessionCtx, ErrCode.XERR_INVALID_PARAM); // 回调主叫拨号失败
            return;
        }

        // 更新会话上下文信息
        sessionCtx.mState = SESSION_STATE_DIALING;    // 切换状态机到 呼叫中
        sessionCtx.mTimestamp = System.currentTimeMillis();  // 更新会话时间戳
        mSessionMgr.updateSession(sessionCtx);


        // 进入频道，准备主叫通话
        int errCode = talkingPrepare(sessionCtx);
        if (errCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<DoDialResponse> failure to join channel, errCode=" + errCode
                    + ", mDevNodeId=" + sessionCtx.mDevNodeId);
            mSessionMgr.removeSession(sessionCtx.mSessionId);   // 删除当前会话
            CallbackCallDialDone(sessionCtx, errCode); // 回调主叫拨号失败
            return;
        }

        ALog.getInstance().d(TAG, "<DoDialResponse> Exit, sessionCtx=" + sessionCtx.toString());

        // 回调主叫拨号成功
        CallbackCallDialDone(sessionCtx, ErrCode.XOK);
    }


    /**
     * @brief 工作线程中运行，对端RTC上线
     */
    void DoRtcPeerOnline(Message msg) {
        UUID sessionId = (UUID)(msg.obj);
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().w(TAG, "<DoRtcPeerOnline> session removed, sessionId=" + sessionId);
            return;
        }
        ALog.getInstance().d(TAG, "<DoRtcPeerOnline> sessionCtx=" + sessionCtx);

        // 更新设备上线状态
        sessionCtx.mDevOnline = true;
        mSessionMgr.updateSession(sessionCtx);

        // 如果当前正在主叫状态，则回调对端应答
        if (sessionCtx.mState == SESSION_STATE_DIALING) {
            ALog.getInstance().d(TAG, "<DoRtcPeerOnline> Peer answer, enter talking...");

            // 更新会话的状态机
            sessionCtx.mState = SESSION_STATE_TALKING;
            sessionCtx.mTimestamp = System.currentTimeMillis();
            sessionCtx.mDevOnline = true;
            mSessionMgr.updateSession(sessionCtx);

            // 回调对端设备接听
            CallbackPeerAnswer(sessionCtx);
        }
    }

    /**
     * @brief 工作线程中运行，对端RTC下线
     */
    void DoRtcPeerOffline(Message msg) {
        UUID sessionId = (UUID)(msg.obj);
        SessionCtx sessionCtx = mSessionMgr.removeSession(sessionId);  // 会话列表中删除会话
        if (sessionCtx == null) {
            ALog.getInstance().w(TAG, "<DoRtcPeerOffline> session removed, sessionId=" + sessionId);
            return;
        }
        ALog.getInstance().d(TAG, "<DoRtcPeerOffline> sessionCtx=" + sessionCtx);

        // 结束通话
        talkingStop(sessionCtx);

        // 回调对端设备挂断
        CallbackPeerHangup(sessionCtx);
    }

    /**
     * @brief 工作线程中运行，对端RTC首帧出图
     */
    void DoRtcPeerFirstVideo(Message msg) {
        int width = msg.arg1;
        int height = msg.arg2;
        UUID sessionId = (UUID)(msg.obj);
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().w(TAG, "<DoRtcPeerFirstVideo> session removed, sessionId=" + sessionId);
            return;
        }
        ALog.getInstance().d(TAG, "<DoRtcPeerFirstVideo> sessionCtx=" + sessionCtx
                    + ", width=" + width + ", height=" + height);

        if (sessionCtx.mState != SESSION_STATE_IDLE) {
            // 回调对端首帧出图
            CallbackPeerFirstVideo(sessionCtx, width, height);
        }
    }


    /**
     * @brief 工作线程中运行，截图完成
     */
    void onRtcSnapshotTaken(Message msg) {
        Object[] params = (Object[])msg.obj;
        UUID sessionId = (UUID)(params[0]);
        String filePath = (String) (params[1]);
        Integer width = (Integer) (params[2]);
        Integer height = (Integer) (params[3]);
        Integer errCode = (Integer) (params[4]);

        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().w(TAG, "<onRtcSnapshotTaken> session removed, sessionId=" + sessionId);
            return;
        }
        ALog.getInstance().d(TAG, "<onRtcSnapshotTaken> sessionCtx=" + sessionCtx
                + ", width=" + width + ", height=" + height
                + ", filePath=" + filePath + ", errCode=" + errCode);

        if (sessionCtx.mState == SESSION_STATE_TALKING || sessionCtx.mState == SESSION_STATE_INCOMING) {
            // 回调截图完成
            int respCode = ErrCode.XOK;
            if (errCode == -1) { // 文件写入失败
                respCode = ErrCode.XERR_FILE_WRITE;
            } else if (errCode == -2) { // 没有收到指定的适配帧
                respCode = ErrCode.XERR_FILE_WRITE;
            } else if (errCode == -3) { // 调用太频繁
                respCode = ErrCode.XERR_INVOKE_TOO_OFTEN;

            } else if (errCode != 0) {
                respCode = ErrCode.XERR_UNKNOWN;
            }
            CallbackShotTakeDone(sessionCtx, respCode, filePath, width, height);
        }
    }

    /*
     * @brief 工作线程中运行，发送HTTP呼叫请求
     */
    void DoRecordingError(Message msg) {
        int errCode = msg.arg1;
        UUID sessionId = (UUID)(msg.obj);
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().w(TAG, "<DoRecordingError> session removed, sessionId=" + sessionId
                                + ", errCode=" + errCode);
            return;
        }
        ALog.getInstance().d(TAG, "<DoRecordingError> sessionCtx=" + sessionCtx + ", errCode=" + errCode);

        // 回调录像失败
        CallbackRecordingError(sessionCtx, errCode);
    }

    /**
     * @brief 工作线程中运行，定时器
     */
    void DoTimer(Message msg) {
        SessionMgr.QueryTimeoutResult queryTimeoutResult = mSessionMgr.queryTimeoutSessionList(
                DIAL_WAIT_TIMEOUT, ANSWER_WAIT_TIMEOUT, DEVONLINE_WAIT_TIMEOUT);

        //
        // 处理主叫呼叫超时
        //
        for(SessionCtx dialSessionCtx : queryTimeoutResult.mDialTimeoutList) {
            talkingStop(dialSessionCtx);    // 退出通话
            mSessionMgr.removeSession(dialSessionCtx.mSessionId);   // 从会话管理器中删除本次会话

            // 回调呼叫超时失败
            ALog.getInstance().d(TAG, "<DoTimer> callback peer timeout, sessionCtx=" + dialSessionCtx);
            CallbackPeerTimeout(dialSessionCtx);
        }

        //
        // 处理来电后，本地接听超时
        //
        for(SessionCtx answerSessionCtx : queryTimeoutResult.mAnswerTimeoutList) {
            talkingStop(answerSessionCtx);    // 退出通话
            mSessionMgr.removeSession(answerSessionCtx.mSessionId);   // 从会话管理器中删除本次会话

            // 回调对端设备挂断
            ALog.getInstance().d(TAG, "<DoTimer> answer timeout, answerSessionCtx=" + answerSessionCtx);
            CallbackPeerHangup(answerSessionCtx);
        }

        //
        // 处理来电后，设备上线超时
        //
        for(SessionCtx devOnlineSessionCtx : queryTimeoutResult.mDevOnlineTimeoutList) {
            talkingStop(devOnlineSessionCtx);    // 退出通话
            mSessionMgr.removeSession(devOnlineSessionCtx.mSessionId);   // 从会话管理器中删除本次会话

            // 回调对端设备挂断
            ALog.getInstance().d(TAG, "<DoTimer> dev online timeout, devOnlineSessionCtx=" + devOnlineSessionCtx);
            CallbackPeerHangup(devOnlineSessionCtx);
        }

        sendSingleMessage(MSGID_CALL_TIMER, 0, 0, null, TIMER_INTERVAL);
    }

    /////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////// 录像的处理 //////////////////////////////
    /////////////////////////////////////////////////////////////////////////////




    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////// 所有的对上层回调处理 //////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    void CallbackPeerIncoming(final SessionCtx sessionCtx) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onPeerIncoming(sessionCtx.mSessionId, sessionCtx.mDevNodeId, sessionCtx.mAttachMsg);
            }
        }
    }

    void CallbackCallDialDone(final SessionCtx sessionCtx, int errCode) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onDialDone(sessionCtx.mSessionId, sessionCtx.mDevNodeId, errCode);
            }
        }
    }

    void CallbackPeerAnswer(final SessionCtx sessionCtx) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onPeerAnswer(sessionCtx.mSessionId, sessionCtx.mDevNodeId);
            }
        }
    }

    void CallbackPeerFirstVideo(final SessionCtx sessionCtx, int width, int height) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onPeerFirstVideo(sessionCtx.mSessionId, width, height);
            }
        }
    }

    void CallbackPeerHangup(final SessionCtx sessionCtx) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onPeerHangup(sessionCtx.mSessionId, sessionCtx.mDevNodeId);
            }
        }
    }

    void CallbackPeerTimeout(final SessionCtx sessionCtx) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onPeerTimeout(sessionCtx.mSessionId, sessionCtx.mDevNodeId);
            }
        }
    }

    void CallbackOtherUserOnline(final SessionCtx sessionCtx, int uid) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onOtherUserOnline(sessionCtx.mSessionId, uid, sessionCtx.mUserCount);
            }
        }
    }

    void CallbackOtherUserOffline(final SessionCtx sessionCtx, int uid) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onOtherUserOffline(sessionCtx.mSessionId, uid, sessionCtx.mUserCount);
            }
        }
    }

    void CallbackRecordingError(final SessionCtx sessionCtx, int errCode) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onRecordingError(sessionCtx.mSessionId, errCode);
            }
        }
    }

    void CallbackShotTakeDone(final SessionCtx sessionCtx, int errCode, final String filePath,
                              int width, int height) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onCaptureFrameDone(sessionCtx.mSessionId, errCode, filePath, width, height);
            }
        }
    }

    void CallbackError(final SessionCtx sessionCtx, int errCode) {
        synchronized (mCallbackList) {
            for (ICallkitMgr.ICallback listener : mCallbackList) {
                listener.onSessionError(sessionCtx.mSessionId, errCode);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////// 通话处理方法 /////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    /**
     * @brief 通话准备处理，创建RTC并且进入频道
     */
    int talkingPrepare(final SessionCtx sessionCtx) {
        boolean bRet;

        synchronized (mTalkEngLock) {

            // 如果RtcSdk还未创建，则进行创建并且初始化
            if (!mTalkEngine.isReady()) {
                IAgoraIotAppSdk.InitParam sdkInitParam = mSdkInstance.getInitParam();
                mTalkEngine = new TalkingEngine();
                TalkingEngine.InitParam talkInitParam = mTalkEngine.new InitParam();
                talkInitParam.mContext = sdkInitParam.mContext;
                talkInitParam.mAppId = sdkInitParam.mAppId;
                talkInitParam.mCallback = this;
                mTalkEngine.initialize(talkInitParam);
            }

            // 加入频道
            bRet = mTalkEngine.joinChannel(sessionCtx);

            // 设置音频效果
            //mTalkEngine.setAudioEffect(mAudioEffect);

            // 设置设备视频帧显示控件
            View displayView = mViewMgr.getDisplayView(sessionCtx.mDevNodeId);
            if (displayView != null)  {
                mTalkEngine.setRemoteVideoView(sessionCtx, displayView);
            }
        }

        return (bRet ? ErrCode.XOK : ErrCode.XERR_INVALID_PARAM);
    }

    /**
     * @brief 应答对方或者对方应答后，开始通话，根据配置决定是否推送本地音频流
     */
    void talkingStart(final SessionCtx sessionCtx) {

        synchronized (mTalkEngLock) {
            mTalkEngine.muteLocalAudioStream(sessionCtx);
        }
    }

    /**
     * @brief 停止通话，状态机切换到空闲，清除对端设备和peerUid
     */
    void talkingStop(final SessionCtx sessionCtx) {
        int sessionCount = mSessionMgr.size();

        synchronized (mTalkEngLock) {
            mTalkEngine.leaveChannel(sessionCtx);

            // 如果当前没有会话了，直接释放整个RtcSDK
            if (sessionCount <= 0) {
                mTalkEngine.release();
            }
        }
    }

    /**
     * @brief 退出所有的频道，并且释放整个通话引擎
     */
    void talkingRelease() {
        List<SessionCtx> sessionList = mSessionMgr.getAllSessionList();
        int sessionCount = sessionList.size();
        int i;

        synchronized (mTalkEngLock) {
            for (i = 0; i < sessionCount; i++) {
                SessionCtx sessionCtx = sessionList.get(i);
                mTalkEngine.leaveChannel(sessionCtx);
            }

            mTalkEngine.release();
        }

        ALog.getInstance().d(TAG, "<talkingRelease> done, sessionCount=" + sessionCount);
    }



    ///////////////////////////////////////////////////////////////////////
    ////////////////////////////// Inner Methods //////////////////////////
    ///////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////
    ///////////////////// Methods for MQTT Packet ////////////////////////////
    //////////////////////////////////////////////////////////////////////////
    void inqueueRecvPkt(final TransPacket recvPacket) {
        mRecvPktQueue.inqueue(recvPacket);
        sendSingleMessage(MSGID_CALL_RECV_PKT, 0, 0, null, 0);
    }


}
