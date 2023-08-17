package io.agora.sdkwayang;

import android.content.Context;
import android.view.SurfaceView;
import android.view.View;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.util.EventListener;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;

import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.logger.ALog;
import io.agora.sdkwayang.logger.WLog;
import io.agora.sdkwayang.protocol.ApiResult;
import io.agora.sdkwayang.protocol.BaseData;
import io.agora.sdkwayang.util.JsonUtil;
import io.agora.sdkwayang.util.ToolUtil;


/**
 * @brief IotSDK 接口执行类
 */
public class SdkExecutor
{
    private static final String TAG = "IOTWY/SdkExecutor";


    /**
     * @brief 账号管理回调接口
     */
    public static interface IEventCallback {

        /**
         * @brief 要将回调消息发回给服务器
         * @param cmd : 回调命令
         * @param info : 回调参数数据
         * @param extra : 附加信息
         */
        default void sendCallbackToServer(String cmd, ConcurrentHashMap<String, Object> info,
                                          ConcurrentHashMap<String, Object> extra) {}
    }



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private Context mContext;
    private IEventCallback mEventCallback;
    private String mCachePath;          ///< 缓存路径

    private final Object mPrepareEvent = new Object();
    private int mPrepareResult;

    private HashMap<String, ICallkitMgr.ICallback> mCallbackMap = new HashMap<>();

    private View mDisplayView;

    //////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Public Methods ////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    public SdkExecutor(Context ctx, final IEventCallback eventCallback) {
        mContext = ctx;
        mEventCallback = eventCallback;

        File file = mContext.getExternalFilesDir(null);
        mCachePath = file.getAbsolutePath();
    }

    /**
     * @brief 设置视频帧显示控件
     */
    public void setDisplayView(final View displayView) {
        mDisplayView = displayView;
    }


    //////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Executor for IAgoraIotAppSdk //////////////////
    //////////////////////////////////////////////////////////////////////////////
    public ApiResult IotAppSdk_initialize(final BaseData receiveData) throws Exception {
        long t1 = System.currentTimeMillis();
        IAgoraIotAppSdk.InitParam initParam = new IAgoraIotAppSdk.InitParam();
        initParam.mContext = mContext;
        initParam.mAppId = (String)receiveData.getInfo().get("mAppId");
        initParam.mServerUrl = (String)receiveData.getInfo().get("mMasterServerUrl");
        initParam.mLogFilePath = mCachePath + "/callkit.log";

        String sdkListenerName = (String)receiveData.getInfo().get("mSdkListenerName");
        initParam.mStateListener = new IAgoraIotAppSdk.OnSdkStateListener() {
            @Override
            public void onSdkStateChanged(int oldSdkState, int newSdkState, int reason) {
                WLog.getInstance().d(TAG, "<onSdkStateChanged> oldSdkState=" + oldSdkState
                        + ", newSdkState=" + newSdkState
                        + ", reason=" + reason);

                if (mEventCallback != null) {
                    ConcurrentHashMap<String, Object> param = new ConcurrentHashMap<>();
                    param.put("listener", sdkListenerName);
                    param.put("oldSdkState", oldSdkState);
                    param.put("newSdkState", newSdkState);
                    param.put("reason", reason);
                    mEventCallback.sendCallbackToServer("onSdkStateChanged", param, null);
                }
            }
        };

        int ret = AIotAppSdkFactory.getInstance().initialize(initParam);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }

    public ApiResult IotAppSdk_release(final BaseData receiveData) throws Exception {
        long t1 = System.currentTimeMillis();
        AIotAppSdkFactory.getInstance().release();
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ErrCode.XOK, (t2-t1)));
    }

    public ApiResult IotAppSdk_getStateMachine(final BaseData receiveData) throws Exception {
        long t1 = System.currentTimeMillis();
        int sdkState = AIotAppSdkFactory.getInstance().getStateMachine();
        long t2 = System.currentTimeMillis();

        return (new ApiResult(sdkState, (t2-t1)));
    }

    public ApiResult IotAppSdk_prepare(final BaseData receiveData) throws Exception {
        long t1 = System.currentTimeMillis();
        mPrepareResult = ErrCode.XOK;

        IAgoraIotAppSdk.PrepareParam prepareParam = new IAgoraIotAppSdk.PrepareParam();
        prepareParam.mUserId = (String)receiveData.getInfo().get("mUserId");
        prepareParam.mClientType = Integer.valueOf((String)receiveData.getInfo().get("mClientType"));
        int ret = AIotAppSdkFactory.getInstance().prepare(prepareParam, new IAgoraIotAppSdk.OnPrepareListener() {
            @Override
            public void onSdkPrepareDone(IAgoraIotAppSdk.PrepareParam paramParam, int errCode) {
                WLog.getInstance().d(TAG, "<onSdkPrepareDone> errCode=" + errCode);
                mPrepareResult = errCode;
                synchronized (mPrepareEvent) {
                    mPrepareEvent.notify();
                }
            }
        });
        if (ret == ErrCode.XOK) {
            ALog.getInstance().w(TAG, "<IotAppSdk_prepare> begin waiting event...");
            synchronized (mPrepareEvent) {  // 同步等待回调返回
                try {
                    mPrepareEvent.wait(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    mPrepareResult = ErrCode.XERR_TIMEOUT;
                    ALog.getInstance().e(TAG, "<IotAppSdk_prepare> exception=" + e.getMessage());
                }
            }
            ALog.getInstance().w(TAG, "<IotAppSdk_prepare> waiting event done");
        } else {
            mPrepareResult = ret;
        }


        long t2 = System.currentTimeMillis();
        return (new ApiResult(mPrepareResult, (t2-t1)));
    }

    public ApiResult IotAppSdk_unprepare(final BaseData receiveData) throws Exception {
        long t1 = System.currentTimeMillis();
        int ret = AIotAppSdkFactory.getInstance().unprepare();
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }

    public ApiResult IotAppSdk_getLocalUserId(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        String localUserId = AIotAppSdkFactory.getInstance().getLocalUserId();
        long t2 = System.currentTimeMillis();

        return (new ApiResult(localUserId, (t2-t1)));
    }

    public ApiResult IotAppSdk_getLocalNodeId(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        String localNodeId = AIotAppSdkFactory.getInstance().getLocalNodeId();
        long t2 = System.currentTimeMillis();

        return (new ApiResult(localNodeId, (t2-t1)));
    }

    //////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Executor for ICallkitMgr //////////////////////
    //////////////////////////////////////////////////////////////////////////////

    public ApiResult CallkitMgr_registerListener(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();

        String listenerName = (String)receiveData.getInfo().get("mListenerName");
        ICallkitMgr.ICallback callback = mCallbackMap.get(listenerName);
        if (callback != null) { // 已经有相应的监听器了
            WLog.getInstance().d(TAG, "<CallkitMgr_registerListener> listener="
                    + listenerName + " already registered!");
            return (new ApiResult(ErrCode.XERR_INVALID_PARAM, 0));
        }

        CallkitEvent callkitEvent = new CallkitEvent(listenerName);
        mCallbackMap.put(listenerName, callkitEvent);

        int ret = callkitMgr.registerListener(callkitEvent);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }

    public ApiResult CallkitMgr_unregisterListener(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();


        String listenerName = (String)receiveData.getInfo().get("mListenerName");
        ICallkitMgr.ICallback callback = mCallbackMap.remove(listenerName);
        if (callback == null) { // 找不到相应的监听器
            WLog.getInstance().d(TAG, "<CallkitMgr_unregisterListener> listener="
                    + listenerName + " NOT found!");
            return (new ApiResult(ErrCode.XERR_NOT_FOUND, 0));
        }

        int ret = callkitMgr.unregisterListener(callback);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }


    public ApiResult CallkitMgr_getSessionInfo(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        String seesionIdText = (String)receiveData.getInfo().get("mSessionId");
        UUID sessionId = UUID.fromString(seesionIdText);

        ICallkitMgr.SessionInfo sessionInfo = callkitMgr.getSessionInfo(sessionId);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(sessionInfo, (t2-t1)));
    }

    public ApiResult CallkitMgr_callDial(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        ICallkitMgr.DialParam dialParam = new ICallkitMgr.DialParam();
        dialParam.mPeerNodeId = (String)receiveData.getInfo().get("mPeerNodeId");
        dialParam.mAttachMsg = (String)receiveData.getInfo().get("mAttachMsg");
        dialParam.mPubLocalAudio = (Boolean) receiveData.getInfo().get("mPubLocalAudio");

        ICallkitMgr.DialResult dialResult = callkitMgr.callDial(dialParam);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(dialResult, (t2-t1)));
    }

    public ApiResult CallkitMgr_callHangup(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        String seesionIdText = (String)receiveData.getInfo().get("mSessionId");
        UUID sessionId = UUID.fromString(seesionIdText);

        int ret = callkitMgr.callHangup(sessionId);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }

    public ApiResult CallkitMgr_callAnswer(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        String seesionIdText = (String)receiveData.getInfo().get("mSessionId");
        UUID sessionId = UUID.fromString(seesionIdText);
        boolean pubLocalAudio = (Boolean) receiveData.getInfo().get("mPubLocalAudio");

        int ret = callkitMgr.callAnswer(sessionId, pubLocalAudio);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }

    public ApiResult CallkitMgr_setPeerVideoView(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        String seesionIdText = (String)receiveData.getInfo().get("mSessionId");
        UUID sessionId = UUID.fromString(seesionIdText);
        boolean isDisplayView = (Boolean) receiveData.getInfo().get("mIsDisplay");

        int ret;
        if (isDisplayView) {
            ret = callkitMgr.setPeerVideoView(sessionId, mDisplayView);
        } else {
            ret = callkitMgr.setPeerVideoView(sessionId, null);
        }
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }

    public ApiResult CallkitMgr_muteLocalAudio(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        String seesionIdText = (String)receiveData.getInfo().get("mSessionId");
        UUID sessionId = UUID.fromString(seesionIdText);
        boolean mute = (Boolean) receiveData.getInfo().get("mMute");

        int ret = callkitMgr.muteLocalAudio(sessionId, mute);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }

    public ApiResult CallkitMgr_mutePeerVideo(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        String seesionIdText = (String)receiveData.getInfo().get("mSessionId");
        UUID sessionId = UUID.fromString(seesionIdText);
        boolean mute = (Boolean) receiveData.getInfo().get("mMute");

        int ret = callkitMgr.mutePeerVideo(sessionId, mute);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }

    public ApiResult CallkitMgr_mutePeerAudio(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        String seesionIdText = (String)receiveData.getInfo().get("mSessionId");
        UUID sessionId = UUID.fromString(seesionIdText);
        boolean mute = (Boolean) receiveData.getInfo().get("mMute");

        int ret = callkitMgr.mutePeerAudio(sessionId, mute);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }

    public ApiResult CallkitMgr_capturePeerVideoFrame(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        String seesionIdText = (String)receiveData.getInfo().get("mSessionId");
        UUID sessionId = UUID.fromString(seesionIdText);
        String imgFileName = (String)receiveData.getInfo().get("mImgFileName");
        String saveFilePath = mCachePath + "/" + imgFileName;

        int ret = callkitMgr.capturePeerVideoFrame(sessionId, saveFilePath);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }

    public ApiResult CallkitMgr_talkingRecordStart(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        String seesionIdText = (String)receiveData.getInfo().get("mSessionId");
        UUID sessionId = UUID.fromString(seesionIdText);
        String videoFileName = (String)receiveData.getInfo().get("mVideoFileName");
        String outFilePath = mCachePath + "/" + videoFileName;

        int ret = callkitMgr.talkingRecordStart(sessionId, outFilePath);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }

    public ApiResult CallkitMgr_talkingRecordStop(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        String seesionIdText = (String)receiveData.getInfo().get("mSessionId");
        UUID sessionId = UUID.fromString(seesionIdText);

        int ret = callkitMgr.talkingRecordStop(sessionId);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }

    public ApiResult CallkitMgr_isTalkingRecording(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        String seesionIdText = (String)receiveData.getInfo().get("mSessionId");
        UUID sessionId = UUID.fromString(seesionIdText);

        boolean isRecording = callkitMgr.isTalkingRecording(sessionId);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(isRecording, (t2-t1)));
    }

    public ApiResult CallkitMgr_getNetworkStatus(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();

        ICallkitMgr.RtcNetworkStatus networkStatus = callkitMgr.getNetworkStatus();
        long t2 = System.currentTimeMillis();

        return (new ApiResult(networkStatus, (t2-t1)));
    }

    public ApiResult CallkitMgr_setPlaybackVolume(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        int volumeLevel = (Integer)receiveData.getInfo().get("mVolumeLevel");

        int ret = callkitMgr.setPlaybackVolume(volumeLevel);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }

    public ApiResult CallkitMgr_setAudioEffect(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        int effectId = (Integer)receiveData.getInfo().get("mEffectId");

        ICallkitMgr.AudioEffectId effectEnum = ICallkitMgr.AudioEffectId.NORMAL;
        switch (effectId) {
            case 1:
                effectEnum = ICallkitMgr.AudioEffectId.OLDMAN;
                break;
            case 2:
                effectEnum = ICallkitMgr.AudioEffectId.BABYBOY;
                break;
            case 3:
                effectEnum = ICallkitMgr.AudioEffectId.BABYGIRL;
                break;
            case 4:
                effectEnum = ICallkitMgr.AudioEffectId.ZHUBAJIE;
                break;
            case 5:
                effectEnum = ICallkitMgr.AudioEffectId.ETHEREAL;
                break;
            case 6:
                effectEnum = ICallkitMgr.AudioEffectId.HULK;
                break;
        }
        int ret = callkitMgr.setAudioEffect(effectEnum);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }

    public ApiResult CallkitMgr_getAudioEffect(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();

        ICallkitMgr.AudioEffectId effectEnum = callkitMgr.getAudioEffect();
        int effectId = 0;
        if (effectEnum == ICallkitMgr.AudioEffectId.OLDMAN) {
            effectId = 1;
        } else if (effectEnum == ICallkitMgr.AudioEffectId.BABYBOY) {
            effectId = 2;
        } else if (effectEnum == ICallkitMgr.AudioEffectId.BABYGIRL) {
            effectId = 3;
        } else if (effectEnum == ICallkitMgr.AudioEffectId.ZHUBAJIE) {
            effectId = 4;
        } else if (effectEnum == ICallkitMgr.AudioEffectId.ETHEREAL) {
            effectId = 5;
        } else if (effectEnum == ICallkitMgr.AudioEffectId.HULK) {
            effectId = 6;
        }
        long t2 = System.currentTimeMillis();

        return (new ApiResult(effectId, (t2-t1)));
    }

    public ApiResult CallkitMgr_setRtcPrivateParam(final BaseData receiveData)  throws Exception {
        long t1 = System.currentTimeMillis();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        String privateParam = (String) receiveData.getInfo().get("mPrivateParam");

        int ret = callkitMgr.setRtcPrivateParam(privateParam);
        long t2 = System.currentTimeMillis();

        return (new ApiResult(ret, (t2-t1)));
    }



    class CallkitEvent implements ICallkitMgr.ICallback {

        private String mListenerName;

        public CallkitEvent(final String listenerName) {
            mListenerName = listenerName;
        }

        @Override
        public void onPeerIncoming(UUID sessionId, String peerNodeId, String attachMsg) {
            WLog.getInstance().d(TAG, "<onPeerIncoming> listenerName=" + mListenerName
                + ", sessionId=" + sessionId + ", peerNodeId=" + peerNodeId + ", attachMsg=" + attachMsg);

            if (mEventCallback != null) {
                ConcurrentHashMap<String, Object> param = new ConcurrentHashMap<>();
                param.put("listener", mListenerName);
                param.put("sessionId", sessionId);
                param.put("peerNodeId", peerNodeId);
                param.put("attachMsg", attachMsg);
                mEventCallback.sendCallbackToServer("onPeerIncoming", param, null);
            }
        }

        @Override
        public void onDialDone(UUID sessionId, String peerNodeId, int errCode) {
            WLog.getInstance().d(TAG, "<onDialDone> listenerName=" + mListenerName
                    + ", sessionId=" + sessionId + ", peerNodeId=" + peerNodeId + ", errCode=" + errCode);

            if (mEventCallback != null) {
                ConcurrentHashMap<String, Object> param = new ConcurrentHashMap<>();
                param.put("listener", mListenerName);
                param.put("sessionId", sessionId);
                param.put("peerNodeId", peerNodeId);
                param.put("errCode", errCode);
                mEventCallback.sendCallbackToServer("onDialDone", param, null);
            }
        }

        @Override
        public void onPeerAnswer(UUID sessionId, String peerNodeId) {
            WLog.getInstance().d(TAG, "<onPeerAnswer> listenerName=" + mListenerName
                    + ", sessionId=" + sessionId + ", peerNodeId=" + peerNodeId);

            if (mEventCallback != null) {
                ConcurrentHashMap<String, Object> param = new ConcurrentHashMap<>();
                param.put("listener", mListenerName);
                param.put("sessionId", sessionId);
                param.put("peerNodeId", peerNodeId);
                mEventCallback.sendCallbackToServer("onPeerAnswer", param, null);
            }
        }

        @Override
        public void onPeerHangup(UUID sessionId, String peerNodeId) {
            WLog.getInstance().d(TAG, "<onPeerHangup> listenerName=" + mListenerName
                    + ", sessionId=" + sessionId + ", peerNodeId=" + peerNodeId);

            if (mEventCallback != null) {
                ConcurrentHashMap<String, Object> param = new ConcurrentHashMap<>();
                param.put("listener", mListenerName);
                param.put("sessionId", sessionId);
                param.put("peerNodeId", peerNodeId);
                mEventCallback.sendCallbackToServer("onPeerHangup", param, null);
            }
        }

        @Override
        public void onPeerTimeout(UUID sessionId, String peerNodeId) {
            WLog.getInstance().d(TAG, "<onPeerTimeout> listenerName=" + mListenerName
                    + ", sessionId=" + sessionId + ", peerNodeId=" + peerNodeId);

            if (mEventCallback != null) {
                ConcurrentHashMap<String, Object> param = new ConcurrentHashMap<>();
                param.put("listener", mListenerName);
                param.put("sessionId", sessionId);
                param.put("peerNodeId", peerNodeId);
                mEventCallback.sendCallbackToServer("onPeerTimeout", param, null);
            }
        }

        @Override
        public void onPeerFirstVideo(UUID sessionId, int videoWidth, int videoHeight) {
        }

        @Override
        public void onOtherUserOnline(UUID sessionId, int uid, int onlineUserCount) {
            WLog.getInstance().d(TAG, "<onOtherUserOnline> listenerName=" + mListenerName
                    + ", sessionId=" + sessionId + ", onlineUserCount=" + onlineUserCount);

            if (mEventCallback != null) {
                ConcurrentHashMap<String, Object> param = new ConcurrentHashMap<>();
                param.put("listener", mListenerName);
                param.put("sessionId", sessionId);
                param.put("onlineUserCount", onlineUserCount);
                mEventCallback.sendCallbackToServer("onOtherUserOnline", param, null);
            }
        }

        @Override
        public void onOtherUserOffline(UUID sessionId, int uid, int onlineUserCount) {
            WLog.getInstance().d(TAG, "<onOtherUserOffline> listenerName=" + mListenerName
                    + ", sessionId=" + sessionId + ", onlineUserCount=" + onlineUserCount);

            if (mEventCallback != null) {
                ConcurrentHashMap<String, Object> param = new ConcurrentHashMap<>();
                param.put("listener", mListenerName);
                param.put("sessionId", sessionId);
                param.put("onlineUserCount", onlineUserCount);
                mEventCallback.sendCallbackToServer("onOtherUserOffline", param, null);
            }
        }

        @Override
        public void onSessionError(UUID sessionId, int errCode) {
            WLog.getInstance().d(TAG, "<onSessionError> listenerName=" + mListenerName
                    + ", sessionId=" + sessionId + ", errCode=" + errCode);

            if (mEventCallback != null) {
                ConcurrentHashMap<String, Object> param = new ConcurrentHashMap<>();
                param.put("listener", mListenerName);
                param.put("sessionId", sessionId);
                param.put("errCode", errCode);
                mEventCallback.sendCallbackToServer("onSessionError", param, null);
            }
        }

        @Override
        public void onRecordingError(UUID sessionId, int errCode) {
            WLog.getInstance().d(TAG, "<onRecordingError> listenerName=" + mListenerName
                    + ", sessionId=" + sessionId + ", errCode=" + errCode);

            if (mEventCallback != null) {
                ConcurrentHashMap<String, Object> param = new ConcurrentHashMap<>();
                param.put("listener", mListenerName);
                param.put("sessionId", sessionId);
                param.put("errCode", errCode);
                mEventCallback.sendCallbackToServer("onRecordingError", param, null);
            }
        }

    }

}
