package io.agora.iotlinkdemo.models.player;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;

import com.agora.baselibrary.base.BaseViewModel;
import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.api.bean.IotDeviceProperty;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.utils.FileUtils;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.IDeviceMgr;
import io.agora.iotlink.IotDevice;

import java.util.Locale;
import java.util.Map;

/**
 * 播放相关viewModel
 */
public class PlayerViewModel extends BaseViewModel implements IDeviceMgr.ICallback, ICallkitMgr.ICallback {
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

    public void onStart() {
        AIotAppSdkFactory.getInstance().getDeviceMgr().registerListener(this);
        AIotAppSdkFactory.getInstance().getCallkitMgr().registerListener(this);
    }

    public void onStop() {
        AIotAppSdkFactory.getInstance().getDeviceMgr().unregisterListener(this);
        AIotAppSdkFactory.getInstance().getCallkitMgr().unregisterListener(this);
    }

    public Bitmap saveScreenshotToSD() {
        Bitmap videoFrameBmp = AIotAppSdkFactory.getInstance().getCallkitMgr().capturePeerVideoFrame();
        String strFilePath = FileUtils.getFileSavePath(mLivingDevice.mDeviceNumber, true);
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
        ICallkitMgr.RtcNetworkStatus networkStatus;
        networkStatus = AIotAppSdkFactory.getInstance().getCallkitMgr().getNetworkStatus();

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

    /**
     * 获取设备属性 配置信息
     */
    public void requestDeviceProperty() {
        int ret = AIotAppSdkFactory.getInstance().getDeviceMgr().getDeviceProperty(mLivingDevice);
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能获取设备属性 配置信息, 错误码: " + ret);
        }
    }

    /**
     * 获取设备信息回调
     */
    @Override
    public void onReceivedDeviceProperty(IotDevice device, Map<String, Object> properties) {
        Log.d("cwtsw", "设备信息 properties = " + properties);
        // 更新设备属性
        synchronized (mDevProperty) {
            mDevProperty.update(properties);
        }
        Log.d("cwtsw", "设备信息 mDevProperty.mVoiceDetect = " + mDevProperty.mVoiceDetect);
        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_PLAYER_UPDATE_PROPERTY, null);
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

    /**
     * 本地挂断
     */
    public void callHangup() {
        AIotAppSdkFactory.getInstance().getCallkitMgr().callHangup();
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
     * 呼叫设备 即连接设备
     */
    public int callDial(IotDevice iotDevice, String attachMsg) {
        return AIotAppSdkFactory.getInstance().getCallkitMgr().callDial(iotDevice, attachMsg);
    }
}
