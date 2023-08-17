package io.agora.iotlink.callkit;

import android.view.View;

import java.util.UUID;


/**
 * @brief 记录通话相关信息
 */
public class SessionCtx  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public UUID mSessionId = UUID.randomUUID();    ///< 会话Id，是会话的唯一标识
    public String mChnlName;        ///< 频道名
    public String mRtcToken;        ///< 分配的RTC token
    public String mLocalNodeId;     ///< 本地的 NodeId
    public String mDevNodeId;       ///< 设备的 NodeId
    public String mAttachMsg;       ///< 呼叫或者来电时的附带消息
    public int mType;               ///< 会话类型
    public int mState;              ///< 通话状态
    public int mLocalUid;           ///< APP端的 Rtc Uid
    public int mPeerUid;            ///< 设备端的 Rtc Uid
    public boolean mDevOnline;      ///< 设备端是否上线
    public long mTimestamp;         ///< 时间戳
    public int mUserCount = 1;      ///< 在线的用户数量，默认至少有一个用户

    public long mTraceId;           ///< MQTT通信时的traceId
    public boolean mPubLocalAudio;  ///< 是否推送本地音频流
    public boolean mSubDevAudio;    ///< 当前是否订阅设备端音频流
    public boolean mSubDevVideo;    ///< 当前是否订阅设备端视频流


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        String infoText = "{ mChnlName=" + mChnlName
                + ", mAttachMsg=" + mAttachMsg
                + ", mType=" + mType
                + ", mLocalNodeId=" + mLocalNodeId
                + ", mDevNodeId=" + mDevNodeId
                + ", mLocalUid=" + mLocalUid
                + ", mPeerUid=" + mPeerUid
                + ", mDevOnline=" + mDevOnline
                + ", mUserCount=" + mUserCount
                + ", mPubLocalAudio=" + mPubLocalAudio
                + ", mSubDevAudio=" + mSubDevAudio
                + ", mSubDevVideo=" + mSubDevVideo
                + ", mTraceId=" + mTraceId
                + ",\n mRtcToken=" + mRtcToken + " }";
        return infoText;
    }
}
