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

import io.agora.iotlink.IRtmMgr;
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
public class RtmViewModel extends BaseViewModel implements IRtmMgr.ICallback {
    private final String TAG = "IOTLINK/RtmViewModel";

    /**
     * 当前设备信息
     */
    public IotDevice mLivingDevice = AgoraApplication.getInstance().getLivingDevice();


    public void onStart() {
        AIotAppSdkFactory.getInstance().getRtmMgr().registerListener(this);
    }

    public void onStop() {
        AIotAppSdkFactory.getInstance().getRtmMgr().unregisterListener(this);
    }


    public void connect() {
        IRtmMgr rtmMgr = AIotAppSdkFactory.getInstance().getRtmMgr();
        int errCode = rtmMgr.connect(mLivingDevice);
        if (errCode != ErrCode.XOK) {
            Log.e(TAG, "<connect> errCode=" + errCode);
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_RTM_CONNECT_DONE, errCode);
        }
    }

    public void disconnect() {
        IRtmMgr rtmMgr = AIotAppSdkFactory.getInstance().getRtmMgr();
        int errCode = rtmMgr.disconnect();
        if (errCode != ErrCode.XOK) {
            Log.e(TAG, "<disconnect> errCode=" + errCode);
        }
    }

    public int getRtmState() {
        IRtmMgr rtmMgr = AIotAppSdkFactory.getInstance().getRtmMgr();
        int state = rtmMgr.getStateMachine();
        return state;
    }

    public void sendMessage(byte[] messageData) {
        IRtmMgr rtmMgr = AIotAppSdkFactory.getInstance().getRtmMgr();
        rtmMgr.sendMessage(messageData, new IRtmMgr.ISendCallback() {
            @Override
            public void onSendDone(int errCode) {
                Log.d(TAG, "<sendMessage.onSendDone> errCode=" + errCode);
                getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_RTM_SEND_DONE, errCode);
            }
        });
    }

    public void sendMessageWithoutCallback(byte[] messageData) {
        IRtmMgr rtmMgr = AIotAppSdkFactory.getInstance().getRtmMgr();
        rtmMgr.sendMessage(messageData, new IRtmMgr.ISendCallback() {
            @Override
            public void onSendDone(int errCode) {
                Log.d(TAG, "<sendMessageWithoutCallback.onSendDone> errCode=" + errCode);
            }
        });
    }

    @Override
    public void onConnectDone(int errCode, final IotDevice iotDevice) {
        Log.d(TAG, "<onConnectDone> errCode=" + errCode);
        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_RTM_CONNECT_DONE, errCode);
    }

    @Override
    public void onMessageReceived(byte[] messageData) {
        Log.d(TAG, "<onMessageReceived> messageData.length=" + messageData.length);
        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_RTM_RECVED, messageData);
    }

}
