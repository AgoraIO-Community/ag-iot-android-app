package io.agora.iotlink.callkit;


public class CallkitContext implements Cloneable {
    public String appId;            ///< 协商的最终appid
    public String callerId;         ///< 呼叫方ID
    public String calleeId;         ///< 被叫方ID
    public String channelName;      ///< 分配的channel号
    public String rtcToken;         ///< 分配的RTC token
    public String uid;              ///< 分配的UID
    public String peerUid;          ///< 对端的UID
    public String sessionId;        ///< 本次呼叫会话ID
    public int callStatus;          ///< 当前呼叫状态，1：空闲，2：主叫中，3：被叫中，4：通话中
    public int reason;              ///< 呼叫原因
    public int cloudRcdStatus;      ///< 云录状态
    public String attachMsg;        ///< 呼叫时附带信息
    public String deviceAlias;      ///< 对端设备别名

    public int mLocalUid;           ///< APP端的UID
    public int mPeerUid;            ///< 设备端的UID


    @Override
    public CallkitContext clone(){
        try {
            CallkitContext tmp = (CallkitContext) super.clone();
            return tmp;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        String infoText = "{\n appId=" + appId
                + ",\n callerId=" + callerId
                + ",\n calleeId=" + calleeId
                + ",\n channelName=" + channelName
                + ",\n rtcToken=" + rtcToken
                + ",\n uid=" + uid
                + ",\n peerUid=" + peerUid
                + ",\n mLocalUid=" + mLocalUid
                + ",\n mPeerUid=" + mPeerUid
                + ",\n sessionId=" + sessionId
                + ",\n callStatus=" + callStatus
                + ",\n reason=" + reason
                + ",\n cloudRcdStatus=" + cloudRcdStatus
                + ",\n attachMsg=" + attachMsg
                + ",\n deviceAlias=" + deviceAlias + " }";
        return infoText;
    }
}
