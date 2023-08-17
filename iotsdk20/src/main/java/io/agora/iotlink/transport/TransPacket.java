package io.agora.iotlink.transport;


import java.util.UUID;

/**
 * @brief 传输的数据包
 */
public class TransPacket {
    public String mTopic;
    public int mMessageId;
    public String mContent;
    public UUID mSessionId;

    @Override
    public String toString() {
        String infoText = "{ mTopic=" + mTopic
                + ", mMessageId=" + mMessageId
                + ", mContent=" + mContent
                + ", mSessionId=" + mSessionId + " }";
        return infoText;
    }
}
