
package com.agora.iotlink.huanxin;
//
//import android.content.Context;
//import com.agora.iotsdk20.logger.ALog;
//import com.hyphenate.EMValueCallBack;
//import com.hyphenate.chat.EMClient;
//import com.hyphenate.chat.EMPushClient;
//import com.hyphenate.notification.EMNotificationMessage;
//import com.hyphenate.notification.core.EMNotificationIntentReceiver;
//import com.hyphenate.push.EMPushConfig;
//
//import org.json.JSONObject;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
//
//
///*
// * @brief 离线接收器
// */
//public class EmReceiver extends EMNotificationIntentReceiver {
//
//    ////////////////////////////////////////////////////////////////////////
//    //////////////////////// Constant Definition ///////////////////////////
//    ////////////////////////////////////////////////////////////////////////
//    private static final String TAG = "LINK/EmReceiver";
//
//    ////////////////////////////////////////////////////////////////////////
//    //////////////////////// Variable Definition ///////////////////////////
//    ////////////////////////////////////////////////////////////////////////
//
//
//
//    ///////////////////////////////////////////////////////////////////////////
//    /////////////// Override Methods of EMNotificationIntentReceiver //////////
//    //////////////////////////////////////////////////////////////////////////
//
//    @Override
//    public void onNotifyMessageArrived(Context context, EMNotificationMessage notificationMessage) {
//
//     //   if(!notificationMessage.isNeedNotification())
//        {
//            String params = notificationMessage.getExtras(); // 判断是透传消息，获取附加字段去处理
//            ALog.getInstance().d(TAG, "<onNotifyMessageArrived> params=" + params);
//        }
//    }
//
//    @Override
//    public void onNotificationClick(Context context, EMNotificationMessage notificationMessage)
//    {
//        String params = notificationMessage.getExtras(); // 判断是透传消息，获取附加字段去处理
//        ALog.getInstance().d(TAG, "<onNotificationClick> params=" + params);
//    }
//
//}
