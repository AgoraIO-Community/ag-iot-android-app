package io.agora.sdkwayang.transport;


import java.util.UUID;

/**
 * @brief 传输的数据包
 */
public class TransWyPacket {
    public long mPacketId;
    public String mContent;

    @Override
    public String toString() {
        String infoText = "{ mPacketId=" + mPacketId
                + ", mContent=" + mContent + " }";
        return infoText;
    }
}
