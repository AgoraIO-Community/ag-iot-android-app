
package com.agora.iotlink.huanxin;
//
//import android.content.Context;
//import android.content.Intent;
//import android.os.Handler;
//import android.os.HandlerThread;
//import android.os.Message;
//import android.util.Log;
//
//import com.agora.iotsdk20.ErrCode;
//import com.agora.iotsdk20.IAgoraIotAppSdk;
//import com.agora.iotsdk20.logger.ALog;
//import com.heytap.msp.push.HeytapPushManager;
//import com.hyphenate.EMValueCallBack;
//import com.hyphenate.chat.EMClient;
//import com.hyphenate.chat.EMPushClient;
//import com.hyphenate.push.EMPushConfig;
//
//
//
//
///*
// * @brief 账号管理器
// */
//public class EmAgent {
//    /*
//     * @brief 离线推送相关的参数
//     */
//    public static class EmPushParam {
//        public String mFcmSenderId;        ///< Firebase的推送配置，如果配置Firebase，则优先使用
//
//        public String mMiAppId;            ///< 小米&红米 机型的推送配置
//        public String mMiAppKey;
//
//        public String mMeizuAppId;         ///< 魅族 机型推送配置
//        public String mMeizuAppKey;
//
//        public String mOppoAppKey;         ///< OPPO 机型的推送配置
//        public String mOppoAppSecret;
//
//        public String mVivoAppId;          ///< VIVO 机型推送配置
//        public String mVivoAppKey;
//
//        public String mHuaweiAppId;         ///< 华为 机型推送配置
//
//        @Override
//        public String toString() {
//            String infoText = "{ mFcmSenderId=" + mFcmSenderId
//                    + ", mOppoAppKey=" + mOppoAppKey + ", mOppoAppSecret=" + mOppoAppSecret
//                    + ", mMiAppId=" + mMiAppId  + ", mMiAppKey=" + mMiAppKey
//                    + ", mMeizuAppId=" + mMeizuAppId  + ", mMeizuAppKey=" + mMeizuAppKey
//                    + ", mVivoAppId=" + mVivoAppId  + ", mVivoAppKey=" + mVivoAppKey
//                    + ", mHuaweiAppId=" + mHuaweiAppId  + "}";
//            return infoText;
//        }
//    }
//
//    ////////////////////////////////////////////////////////////////////////
//    //////////////////////// Constant Definition ///////////////////////////
//    ////////////////////////////////////////////////////////////////////////
//    private static final String TAG = "LINK/EmAgent";
//
//    private static final String MANU_FACTURER_HUAWEI = "Huawei";    ///< 华为
//    private static final String MANU_FACTURER_HONOR = "HONOR";      ///< 荣耀
//    private static final String MANU_FACTURER_NOVA = "nova";        ///< 华为 NOVA
//    private static final String MANU_FACTURER_XIAOMI = "xiaomi";    ///< 小米
//    private static final String MANU_FACTURER_REDMI = "redmi";      ///< 红米
//    private static final String MANU_FACTURER_VIVO = "vivo";        ///< VIVO
//    private static final String MANU_FACTURER_MEIZU = "Meizu";      ///< 魅族
//    private static final String MANU_FACTURER_OPPO = "OPPO";        ///< OPPO
//    private static final String MANU_FACTURER_ONEPLUS = "OnePlus";  ///< 一加
//    private static final String MANU_FACTURER_LETV = "letv";        ///< 乐视
//    private static final String MANU_FACTURER_SAMSUNG = "samsung";  ///< 三星
//    private static final String MANU_FACTURER_SMARTISAN = "smartisan";  ///< 锤子
//    private static final String MANU_FACTURER_LENOVO = "lenovo";    ///< 联想
//    private static final String MANU_FACTURER_SONY = "sony";        ///< 索尼
//    private static final String MANU_FACTURER_HTC = "htc";          ///< HTC
//    private static final String MANU_FACTURER_LG = "lg";            ///< LG
//
//
//
//    ////////////////////////////////////////////////////////////////////////
//    //////////////////////// Variable Definition ///////////////////////////
//    ////////////////////////////////////////////////////////////////////////
//    static private EmAgent mInstance = null;
//    private Context mContext;
//    private EmPushParam mInitParam;
//    private String mEid = "";
//
//
//
//    ///////////////////////////////////////////////////////////////////////
//    ////////////////////////// Public Methods  ////////////////////////////
//    ///////////////////////////////////////////////////////////////////////
//    public static EmAgent getInstance() {
//        if (mInstance == null){
//            synchronized (EmAgent.class) {
//                if (mInstance == null) {
//                    mInstance = new EmAgent();
//                }
//            }
//        }
//        return mInstance;
//    }
//
//
//    public int initialize(Context ctx, EmPushParam initParam) {
//        mInitParam = initParam;
//
//        String manuFacturer = android.os.Build.MANUFACTURER; // 制造商
//        String phoneModel = android.os.Build.MODEL; // 型号
//        String brand = android.os.Build.BRAND; // 品牌
//        String devName = android.os.Build.DEVICE; // 设备名
//
//        ALog.getInstance().d(TAG, "<initialize> manuFacturer=" + manuFacturer
//                + ", phoneModel=" + phoneModel
//                + ", brand=" + brand
//                + ", devName=" + devName
//                + ", initParam=" + initParam.toString());
//
//        //
//        // 初始化环信的推送
//        //
//        mContext = ctx;
//        EMPushConfig.Builder builder = new EMPushConfig.Builder(mContext);
//
//        // 设置小米推送的appid和appkey
//        if ( (manuFacturer.compareToIgnoreCase(MANU_FACTURER_XIAOMI) == 0) ||
//             (manuFacturer.compareToIgnoreCase(MANU_FACTURER_REDMI) == 0)) {
//            builder.enableMiPush(mInitParam.mMiAppId, mInitParam.mMiAppKey);
//        }
//
//        // 设置oppo推送的appkey和mastersecret
//        if (manuFacturer.compareToIgnoreCase(MANU_FACTURER_OPPO) == 0) {
//            HeytapPushManager.init(mContext, true);
//            builder.enableOppoPush(mInitParam.mOppoAppKey, mInitParam.mOppoAppSecret);
//        }
//
//        // 设置魅族appid和appkey
//        if (manuFacturer.compareToIgnoreCase(MANU_FACTURER_MEIZU) == 0) {
//            builder.enableMeiZuPush(mInitParam.mMeizuAppId, mInitParam.mMeizuAppKey);
//        }
//
//        // 设置华为推送
//        if ( (manuFacturer.compareToIgnoreCase(MANU_FACTURER_HUAWEI) == 0) ||
//             (manuFacturer.compareToIgnoreCase(MANU_FACTURER_HONOR) == 0) ||
//             (manuFacturer.compareToIgnoreCase(MANU_FACTURER_NOVA) == 0)) {
//            builder.enableHWPush();
//        }
//
//        // 设置VIVO推送
//        if (manuFacturer.compareToIgnoreCase(MANU_FACTURER_VIVO) == 0)  {
//            builder.enableVivoPush();
//        }
//
//        // 启动FCM，设置参数 senderId
//        boolean enableFCM = false;
//        if ((mInitParam.mFcmSenderId != null) && (mInitParam.mFcmSenderId.length() > 0)) {
//            builder.enableFCM(mInitParam.mFcmSenderId);
//            enableFCM = true;
//        }
//
//        EMPushClient.getInstance().init(mContext, enableFCM, builder,
//            new EMValueCallBack<String>() {
//                @Override
//                public void onSuccess(String s) {
//                    mEid = EMPushClient.getInstance().getEid();
//                    ALog.getInstance().d(TAG, "<initialize.EMPushClient.onSuccess> s=" + s
//                            + ", mEid=" + mEid);
//                }
//
//                @Override
//                public void onError(int i, String s) {
//                    ALog.getInstance().d(TAG, "<initialize.EMPushClient.onError> i=" + i
//                            + ", s=" + s);
//                }
//            });
//
//        mEid = EMPushClient.getInstance().getEid();
//        ALog.getInstance().d(TAG, "<initialize> mEid=" + mEid);
//
//        return ErrCode.XOK;
//    }
//
//    public String getEid() {
//        return mEid;
//    }
//
//}
