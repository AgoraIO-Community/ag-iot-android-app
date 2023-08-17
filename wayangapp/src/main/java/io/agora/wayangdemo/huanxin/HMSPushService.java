package io.agora.wayangdemo.huanxin;

import io.agora.iotlink.logger.ALog;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;
import com.hyphenate.chat.EMClient;


public class HMSPushService extends HmsMessageService {
    private final String TAG = "IOTSDK/HMSPushSrv";

    @Override
    public void onNewToken(String token) {
        if ((token != null) && (token.length() > 0)) {
            //没有失败回调，假定token失败时token为null
            ALog.getInstance().d(TAG, "<onNewToken> new token success, token=" + token);
            EMClient.getInstance().sendHMSPushTokenToServer(token);

        } else {
            ALog.getInstance().e(TAG, "<onNewToken> new token fail!");
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        // 判断消息是否为空
        if (message == null) {
            ALog.getInstance().getInstance().e(TAG, "<onMessageReceived> message is NULL");
            return;
        }

        // 获取消息内容
        ALog.getInstance().d(TAG, "<onMessageReceived> message= " + message.getData()
                + "\n getFrom: " + message.getFrom()
                + "\n getTo: " + message.getTo()
                + "\n getMessageId: " + message.getMessageId()
                + "\n getSendTime: " + message.getSentTime()
                + "\n getDataMap: " + message.getDataOfMap()
                + "\n getMessageType: " + message.getMessageType()
                + "\n getTtl: " + message.getTtl()
                + "\n getToken: " + message.getToken());
    }
}
