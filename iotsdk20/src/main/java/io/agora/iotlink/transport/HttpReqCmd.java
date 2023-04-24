
package io.agora.iotlink.transport;



/**
 * @brief HTTP命令队列
 */
public class HttpReqCmd {

    public String mAppId;
    public String mToken;
    public String mTalkingId;

    // 呼叫 HTTP使用的参数
    public String mUserId;
    public String mDeviceId;
    public String mAttachMsg;
    public HttpReqScheduler.IAsyncDialCallback mDialCallbk;


    @Override
    public String toString() {
        String infoText = "{ mAppId=" + mAppId
                + ", mTalkingId=" + mTalkingId
                + ", mUserId=" + mUserId
                + ", mDeviceId=" + mDeviceId
                + ", mAttachMsg=" + mAttachMsg + " }";
        return infoText;
    }

}