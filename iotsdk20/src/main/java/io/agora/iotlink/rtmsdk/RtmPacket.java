package io.agora.iotlink.rtmsdk;


import java.util.UUID;

import io.agora.iotlink.ICallkitMgr;

/**
 * @brief RTM传输的数据包
 */
public class RtmPacket {
    public String mPeerId;               ///< 对端 NodeId
    public String mPktData;             ///< 消息数据包内容
    public boolean mIsRecvPkt;         ///< 是否是接收数据包

    public UUID mSessionId;             ///< 会话唯一Id（仅对发送数据包有效）
    public long mSendTimestamp;         ///< 发送的时间戳（仅对发送数据包有效）
    public ICallkitMgr.OnCmdSendListener mSendListener; ///< 发送监听器（仅对发送数据包有效）

    public long mRecvTimestamp;         ///< 接收到的时间戳（仅对接收数据包有效）


    @Override
    public String toString() {
        String infoText = "{ mPeerNodeId=" + mPeerId
                + ", mPktData=" + mPktData
                + ", mIsRecvPkt=" + mIsRecvPkt
                + ", mSessionId=" + mSessionId + " }";
        return infoText;
    }
}
