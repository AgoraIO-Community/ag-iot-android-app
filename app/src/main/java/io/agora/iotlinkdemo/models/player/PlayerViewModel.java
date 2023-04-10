package io.agora.iotlinkdemo.models.player;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;

import com.agora.baselibrary.base.BaseViewModel;
import com.agora.baselibrary.utils.ToastUtils;

import io.agora.iotlink.IotPropertyDesc;
import io.agora.iotlinkdemo.api.bean.IotDeviceProperty;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.utils.FileUtils;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.IDeviceMgr;
import io.agora.iotlink.IotDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 播放相关viewModel
 */
public class PlayerViewModel extends BaseViewModel implements IDeviceMgr.ICallback, ICallkitMgr.ICallback {
    private final String TAG = "IOTLINK/PlayerViewModel";

    /**
     * @brief 通话状态机
     */
    public final static int CALL_STATE_DISCONNECTED = 0x0000;    // 已经断开
    public final static int CALL_STATE_CONNECTING = 0x0001;      // 正在接通中
    public final static int CALL_STATE_CONNECTED = 0x0002;       // 已经接通


    /**
     * 当前设备信息
     */
    public IotDevice mLivingDevice = AgoraApplication.getInstance().getLivingDevice();
    /**
     * 设备当前属性
     */
    public static final IotDeviceProperty mDevProperty = new IotDeviceProperty();

    /**
     * 是否正在发送语音
     */
    public volatile boolean mSendingVoice = false;

    /**
     * 是否正常运行
     */
    public volatile boolean mRunning = true;

    public void onStart() {
        Log.d(TAG, "<onStart>");
        AIotAppSdkFactory.getInstance().getDeviceMgr().registerListener(this);
        AIotAppSdkFactory.getInstance().getCallkitMgr().registerListener(this);
    }

    public void onStop() {
        Log.d(TAG, "<onStop>");
        AIotAppSdkFactory.getInstance().getDeviceMgr().unregisterListener(this);
        AIotAppSdkFactory.getInstance().getCallkitMgr().unregisterListener(this);
    }

    public Bitmap saveScreenshotToSD() {
        Bitmap videoFrameBmp = AIotAppSdkFactory.getInstance().getCallkitMgr().capturePeerVideoFrame();
        String strFilePath = FileUtils.getFileSavePath(mLivingDevice.mDeviceID, true);
        boolean ret = FileUtils.saveScreenshotToSD(videoFrameBmp, strFilePath);
        if (ret) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_PLAYER_SAVE_SCREENSHOT, null);
        } else {
            ToastUtils.INSTANCE.showToast("保存截图失败");
        }
        return videoFrameBmp;
    }

    private Handler mMsgHandler = null;                 ///< 主线程中的消息处理
    public static final int MSGID_UPDATE_NETSTATUS = 0x1001;    ///< 更新网络状态
    private static final int TIMER_UPDATE_NETSATUS = 2000;     ///< 网络状态定时2秒刷新一次

    public void initHandler() {
        mMsgHandler = new Handler(Looper.myLooper()) {
            @SuppressLint("HandlerLeak")
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSGID_UPDATE_NETSTATUS:
                        onMsgUpdateNetStatus();
                        break;
                }
            }
        };
    }

    /*
     * @brief 更新网络状态
     */
    private void onMsgUpdateNetStatus() {
        if (!mRunning) {
            return;
        }
        ICallkitMgr.RtcNetworkStatus networkStatus;
        networkStatus = AIotAppSdkFactory.getInstance().getCallkitMgr().getNetworkStatus();
        if (networkStatus == null) {
            return;
        }

        String status1 = String.format(Locale.getDefault(),
                "Lastmile Delay: %d ms", networkStatus.lastmileDelay);
        String status2 = String.format(Locale.getDefault(),
                "Video Send/Recv: %d kbps / %d kbps",
                networkStatus.txVideoKBitRate, networkStatus.rxVideoKBitRate);
        String status3 = String.format(Locale.getDefault(), "Audio Send/Recv: %d kbps / %d kbps",
                networkStatus.txAudioKBitRate, networkStatus.rxAudioKBitRate);

        String status = status1 + "\n" + status2 + "\n" + status3;
        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_NET_RECEIVING_SPEED, networkStatus.rxVideoKBitRate);
        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_LAST_MILE_DELAY, networkStatus.lastmileDelay);
        mMsgHandler.removeMessages(MSGID_UPDATE_NETSTATUS);
        mMsgHandler.sendEmptyMessageDelayed(MSGID_UPDATE_NETSTATUS, TIMER_UPDATE_NETSATUS);
    }

    public void release() {
        if (mMsgHandler != null) {
            mMsgHandler.removeMessages(MSGID_UPDATE_NETSTATUS);
            mMsgHandler = null;
        }
        mRunning = false;
    }

    /**
     * 设备连接状态
     */
    private boolean mConnected;

    /**
     * 初始化设备
     */
    public void initMachine() {
        int callkitStatus = AIotAppSdkFactory.getInstance().getCallkitMgr().getStateMachine();
        mConnected = (callkitStatus == ICallkitMgr.CALLKIT_STATE_TALKING);
        if (mConnected) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_BROWSE, null);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_CONNING, null);
        }
        mMsgHandler.sendEmptyMessageDelayed(MSGID_UPDATE_NETSTATUS, TIMER_UPDATE_NETSATUS);
    }

    public int getCallStatus() {
        int callkitStatus = AIotAppSdkFactory.getInstance().getCallkitMgr().getStateMachine();
        int callStatus = CALL_STATE_DISCONNECTED;

        switch (callkitStatus) {
            case ICallkitMgr.CALLKIT_STATE_TALKING: {  // 正在通话中
                callStatus = CALL_STATE_CONNECTED;
            } break;

            case ICallkitMgr.CALLKIT_STATE_DIALING:
            case ICallkitMgr.CALLKIT_STATE_DIAL_REQING:
            case ICallkitMgr.CALLKIT_STATE_DIAL_RSPING: {   // 正在呼叫中
                callStatus = CALL_STATE_CONNECTING;
            } break;

            case ICallkitMgr.CALLKIT_STATE_INCOMING:
            case ICallkitMgr.CALLKIT_STATE_ANSWER_REQING:
            case ICallkitMgr.CALLKIT_STATE_ANSWER_RSPING: {   // 正在接听中
                callStatus = CALL_STATE_CONNECTING;
            } break;

            case ICallkitMgr.CALLKIT_STATE_IDLE:
            case ICallkitMgr.CALLKIT_STATE_HANGUP_REQING: {   // 正在挂断或者已经挂断
                callStatus = CALL_STATE_DISCONNECTED;
            } break;
        }

        Log.d(TAG, "<getCallStatus> callkitStatus=" + callkitStatus
                + ", callStatus=" + callStatus);
        return callStatus;
    }

    @Override
    public void onDialDone(int errCode, IotDevice iotDevice) {
        Log.d(TAG, "<onDialDone> errCode=" + errCode);
        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_DIAL_DONE, errCode);
    }

    @Override
    public void onPeerIncoming(IotDevice iotDevice, String attachMsg) {}

    @Override
    public void onPeerAnswer(IotDevice iotDevice) {
        Log.d(TAG, "<onPeerAnswer> iotDevice=" + iotDevice.mDeviceID);
        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_ANSWER, 0);
    }

    @Override
    public void onPeerHangup(IotDevice iotDevice) {
        if (iotDevice == null) {
            return;
        }
        Log.d(TAG, "<onPeerHangup> iotDevice=" + iotDevice.mDeviceID);
        if (mLivingDevice == null) {
            return;
        }
        if (mLivingDevice.mDeviceID.compareToIgnoreCase(iotDevice.mDeviceID) == 0) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_HANGUP, 0);
        }
    }

    @Override
    public void onPeerTimeout(IotDevice iotDevice) {
        Log.e(TAG, "<onPeerTimeout> iotDevice=" + iotDevice.mDeviceID);
        if (mLivingDevice == null) {
            return;
        }
        if (mLivingDevice.mDeviceID.compareToIgnoreCase(iotDevice.mDeviceID) == 0) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_DIAL_TIMEOUT, 0);
        }
    }

    @Override
    public void onPeerFirstVideo(IotDevice iotDevice, int videoWidth, int videoHeight) {
        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_PEER_FIRST_VIDEO, null);
    }

    /**
     * 初始化播放view
     *
     * @param peerView surfaceView
     */
    public void initPeerVideo(SurfaceView peerView) {
        AIotAppSdkFactory.getInstance().getCallkitMgr().setPeerVideoView(peerView);
    }

    /*
     * @brief 通话功能
     */
    public void onBtnVoiceTalk() {
        if (mSendingVoice) {
            // 结束语音发送处理
            AIotAppSdkFactory.getInstance().getCallkitMgr().muteLocalAudio(true);
            mSendingVoice = false;
        } else {
            // 开始语音发送处理
            AIotAppSdkFactory.getInstance().getCallkitMgr().muteLocalAudio(false);
            mSendingVoice = true;
        }
    }

    public void pausePlayer() {
        AIotAppSdkFactory.getInstance().getCallkitMgr().setPeerVideoView(null);
    }

    public void setNightView(int type) {
        requestSetDeviceProperty(mDevProperty.setNightView(type));
    }

    public void setSoundDetection(boolean isOpen) {
        requestSetDeviceProperty(mDevProperty.setSoundDetection(isOpen));
    }

    public void setMotionAlarm(boolean isOpen) {
        requestSetDeviceProperty(mDevProperty.setMotionAlarm(isOpen));
    }

    public void setPirSwitch(int type) {
        requestSetDeviceProperty(mDevProperty.setPirSwitch(type));
    }

    public void setLedSwitch(boolean isOpen) {
        requestSetDeviceProperty(mDevProperty.setLedSwitch(isOpen));
    }

    public void setVideoQuality(int type) {
        requestSetDeviceProperty(mDevProperty.setVideoQuality(type));
    }

    /*
     * 流对端音频
     * @param mute: 是否禁止 true 禁止 false 不禁止
     * @return 错误码
     */
    public void setMutePeer(boolean mute) {
        AIotAppSdkFactory.getInstance().getCallkitMgr().mutePeerAudio(mute);
    }

    public void setVolume(int value) {
        requestSetDeviceProperty(mDevProperty.setVolume(value));
    }

    /**
     * 变声
     *
     * @param effectId ICallkitMgr.AudioEffectId.NORMAL  正常
     *                 ICallkitMgr.AudioEffectId.OLDMAN 老人
     *                 ICallkitMgr.AudioEffectId.BABYBOY 小孩
     *                 ICallkitMgr.AudioEffectId.BABYGIRL 萝莉
     *                 ICallkitMgr.AudioEffectId.ZHUBAJIE 猪八戒
     *                 ICallkitMgr.AudioEffectId.ETHEREAL
     *                 ICallkitMgr.AudioEffectId.HULK
     */
    public void setAudioEffect(ICallkitMgr.AudioEffectId effectId) {
        AIotAppSdkFactory.getInstance().getCallkitMgr().setAudioEffect(effectId);
    }

    public ICallkitMgr.AudioEffectId getAudioEffect() {
        return AIotAppSdkFactory.getInstance().getCallkitMgr().getAudioEffect();
    }

    /**
     * 本地挂断
     */
    public int callHangup() {
        int errCode = AIotAppSdkFactory.getInstance().getCallkitMgr().callHangup();
        return errCode;
    }

    /**
     * 设置设备属性 配置信息
     */
    private void requestSetDeviceProperty(Map<String, Object> properties) {
        Log.d("cwtsw", "requestSetDeviceProperty properties = " + properties);
        int ret = AIotAppSdkFactory.getInstance().getDeviceMgr().setDeviceProperty(mLivingDevice, properties);
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能设置设备属性 配置信息, 错误码: " + ret);
        }
    }

    /**
     * 获取设备属性信息回调
     */
    @Override
    public void onReceivedDeviceProperty(IotDevice device, Map<String, Object> properties) {
        Log.d(TAG, "设备信息 properties = " + properties);
        if (!mRunning) {
            return;
        }

        // 更新设备属性
        synchronized (mDevProperty) {
            mDevProperty.update(properties);
        }
        Log.d(TAG, "设备信息 mDevProperty.mVoiceDetect = " + mDevProperty.mVoiceDetect);
        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_PLAYER_UPDATE_PROPERTY, null);
    }

    /**
     * 查询所有属性描述符
     */
    public void queryAllPropDesc() {
        Log.d(TAG, "<queryAllPropDesc> deviceID=" + mLivingDevice.mDeviceID
                    + ", mProductID=" + mLivingDevice.mProductID);
        int ret = AIotAppSdkFactory.getInstance().getDeviceMgr().queryAllPropertyDesc(
                mLivingDevice.mDeviceID, null);
//        int ret = AIotAppSdkFactory.getInstance().getDeviceMgr().queryAllPropertyDesc(
//                null, mLivingDevice.mProductNumber);
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能查询所有属性描述符, 错误码=" + ret);
        }
    }

    @Override
    public void onQueryAllPropertyDescDone(int errCode,
                                           final String deviceID,
                                           final String productNumber,
                                           final List<IotPropertyDesc> propDescList) {
        Log.d(TAG, "<onQueryAllPropertyDescDone> errCode=" + errCode);
        if (!mRunning) {
            return;
        }

        if (errCode == ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_PLAYER_UPDATEPROPDESC, propDescList);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_PLAYER_UPDATEPROPDESC, null);
        }
    }

    /**
     * 呼叫设备 即连接设备
     */
    public int callDial(String attachMsg) {
        if (mLivingDevice == null) {
            return ErrCode.XERR_BAD_STATE;
        }
        return AIotAppSdkFactory.getInstance().getCallkitMgr().callDial(mLivingDevice, attachMsg);
    }



    /**
     * @brief 获取视图需要的数据
     */
    public void requestViewModelData() {
        // 先查询 设备属性信息
        int ret = AIotAppSdkFactory.getInstance().getDeviceMgr().getDeviceProperty(mLivingDevice);
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能获取设备属性 配置信息, 错误码: " + ret);
        }
    }

    /**
     * @brief 获取固件版本号
     */
    public void queryMcuVersion() {
        int ret = AIotAppSdkFactory.getInstance().getDeviceMgr().getMcuVersionInfo(mLivingDevice);
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能获取固件版本, 错误码: " + ret);
        }
    }

    @Override
    public void onGetMcuVerInfoDone(int errCode, final IotDevice iotDevice,
                                    final IDeviceMgr.McuVersionInfo mcuVerInfo) {
        if ((errCode != ErrCode.XOK) || (mcuVerInfo == null)) {
            Log.e(TAG, "<onGetMcuVerInfoDone> [ERROR] errCode=" + errCode);
            return;
        }

        Log.d(TAG, "<onGetMcuVerInfoDone> iotDevice=" + iotDevice
                + ",  mcuVerInfo=" + mcuVerInfo.toString());
        if (!mRunning) {
            return;
        }
        AgoraApplication.getInstance().setLivingMcuVersion(mcuVerInfo);
        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_FIRM_GETVERSION, mcuVerInfo);
    }


    public int getOnlineUserCount() {
        int userCount = AIotAppSdkFactory.getInstance().getCallkitMgr().getOnlineUserCount();
        Log.d(TAG, "<getOnlineUserCount> userCount=" + userCount);
        return userCount;
    }

    @Override
    public void onUserOnline(int uid, int onlineUserCount) {
        Log.d(TAG, "<onUserOnline> uid=" + uid + ", onlineUserCount=" + onlineUserCount);
        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_USER_ONLINE, onlineUserCount);
    }

    @Override
    public void onUserOffline(int uid, int onlineUserCount) {
        Log.d(TAG, "<onUserOnline> uid=" + uid + ", onlineUserCount=" + onlineUserCount);
        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_USER_OFFLINE, onlineUserCount);
    }


    ///////////////////////////////////////////////////////////
    ////////////////////////// 频道内录像处理 ////////////////////
    ///////////////////////////////////////////////////////////
    public boolean isRecording() {
        boolean bRecording = AIotAppSdkFactory.getInstance().getCallkitMgr().isTalkingRecording();
        return bRecording;
    }

    public int recordingStart() {
        String outFilePath = FileUtils.getFileSavePath(mLivingDevice.mDeviceID, false);
        //outFilePath = "/sdcard/Android/data/io.agora.iotlinkdemo/" + System.currentTimeMillis() + ".mp4";
        int ret = AIotAppSdkFactory.getInstance().getCallkitMgr().talkingRecordStart(outFilePath);
        Log.d(TAG, "<recordingStart> outFilePath=" + outFilePath + ", ret=" + ret);
        return ret;
    }

    public int recordingStop() {
        int ret = AIotAppSdkFactory.getInstance().getCallkitMgr().talkingRecordStop();
        Log.d(TAG, "<recordingStop> ret=" + ret);
        return ret;
    }

    public Bitmap captureRtcVideoFrame() {
        Bitmap videoFrameBmp = AIotAppSdkFactory.getInstance().getCallkitMgr().capturePeerVideoFrame();
        return videoFrameBmp;
    }

    @Override
    public void onRecordingError(int errCode) {
        Log.d(TAG, "<onRecordingError> errCode=" + errCode);
        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_RECORDING_ERROR, errCode);
    }

}
