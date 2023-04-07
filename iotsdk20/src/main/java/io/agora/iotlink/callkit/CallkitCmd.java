
package io.agora.iotlink.callkit;


import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.view.SurfaceView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

import io.agora.avmodule.AvBaseFrame;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.IotDevice;
import io.agora.iotlink.aws.AWSUtils;
import io.agora.iotlink.callkit.AgoraService;
import io.agora.iotlink.callkit.CallkitContext;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.rtcsdk.TalkingEngine;
import io.agora.iotlink.sdkimpl.AccountMgr;
import io.agora.iotlink.sdkimpl.AgoraIotAppSdk;
import io.agora.rtc2.Constants;

/**
 * @brief 呼叫命令队列
 */
public class CallkitCmd {

    public static final int CMD_TYPE_DIAL = 1;
    public static final int CMD_TYPE_HANGUP = 2;

    public int mType;         // 1：呼叫；  2：挂断
    public UUID mTalkId = UUID.randomUUID();
    public String mToken;

    // 呼叫使用的参数
    public String mAppId;
    public String mIdentityId;
    public String mPeerId;
    public String mAttachMsg;
    public CallkitScheduler.IAsyncDialCallback mDialCallbk;

    // 挂断使用的参数
    public String mSessionId;
    public String mCallerId;
    public String mCalleeId;
    public String mLocalId;
    public CallkitScheduler.IAsyncHangupCallback mHangupCallbk;

    @Override
    public String toString() {
        String infoText = "{mType=" + mType
                + ", mTalkingId=" + mTalkId
                + ", mIdentityId=" + mIdentityId
                + ", mPeerId=" + mPeerId
                + ", mAttachMsg=" + mAttachMsg
                + ", mSessionId=" + mSessionId
                + ", mCallerId=" + mCallerId
                + ", mCalleeId=" + mCalleeId
                + ", mLocalId=" + mLocalId + " }";
        return infoText;
    }

}