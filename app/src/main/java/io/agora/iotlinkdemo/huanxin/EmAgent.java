//
//package io.agora.iotlinkdemo.huanxin;
//
//import android.content.Context;
//import android.content.Intent;
//import android.os.Handler;
//import android.os.HandlerThread;
//import android.os.Message;
//import android.text.TextUtils;
//import android.util.Log;
//
//import io.agora.iotlink.ErrCode;
//import io.agora.iotlink.logger.ALog;
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
//    private static final String TAG = "IOTSDK/EmAgent";
//    private static final int INIT_DONE_TIMEOUT = 2500;
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
//    //
//    // Phone Type
//    //
//    private static final int PHONE_TYPE_UNKNOWN = 0;
//    private static final int PHONE_TYPE_MI = 1;
//    private static final int PHONE_TYPE_OPPO = 2;
//    private static final int PHONE_TYPE_MEIZU = 3;
//    private static final int PHONE_TYPE_VIVO = 4;
//    private static final int PHONE_TYPE_HUAWEI = 5;
//
//
//    ////////////////////////////////////////////////////////////////////////
//    //////////////////////// Variable Definition ///////////////////////////
//    ////////////////////////////////////////////////////////////////////////
//    private final Object mInitDoneEvent = new Object();
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
//
//
//        ALog.getInstance().d(TAG, "<initialize> initParam=" + initParam.toString());
//        int phoneType = detectPhoneType();
//
//        //
//        // 初始化环信的推送
//        //
//        mContext = ctx;
//        EMPushConfig.Builder builder = new EMPushConfig.Builder(mContext);
//        boolean enableHWPush = false;
//
//        // 设置小米推送的appid和appkey
//        if ( (!TextUtils.isEmpty(mInitParam.mMiAppId)) &&
//                (!TextUtils.isEmpty(mInitParam.mMiAppKey)) && (phoneType == PHONE_TYPE_MI) ) {
//            builder.enableMiPush(mInitParam.mMiAppId, mInitParam.mMiAppKey);
//            ALog.getInstance().d(TAG, "<initialize> enable XiaoMi phone"
//                    + ", mMiAppId=" + mInitParam.mMiAppId
//                    + ", mMiAppKey=" + mInitParam.mMiAppKey);
//        }
//
//        // 设置oppo推送的 appKey 和 appSecret
//        if ( (!TextUtils.isEmpty(mInitParam.mOppoAppKey)) &&
//                (!TextUtils.isEmpty(mInitParam.mOppoAppSecret)) && (phoneType == PHONE_TYPE_OPPO) ) {
//            HeytapPushManager.init(mContext, true);  // OPPO需要单独调用初始化方法
//            builder.enableOppoPush(mInitParam.mOppoAppKey, mInitParam.mOppoAppSecret);
//            ALog.getInstance().d(TAG, "<initialize> enable OPPO phone"
//                    + ", mOppoAppKey=" + mInitParam.mOppoAppKey
//                    + ", mOppoAppSecret=" + mInitParam.mOppoAppSecret);
//        }
//
//
//        // 设置魅族appid和appkey
//        if ( (!TextUtils.isEmpty(mInitParam.mMeizuAppId)) &&
//                (!TextUtils.isEmpty(mInitParam.mMeizuAppKey)) && (phoneType == PHONE_TYPE_MEIZU) ) {
//            builder.enableMeiZuPush(mInitParam.mMeizuAppId, mInitParam.mMeizuAppKey);
//            ALog.getInstance().d(TAG, "<initialize> enable MEIZU phone"
//                    + ", mMeizuAppId=" + mInitParam.mMeizuAppId
//                    + ", mMeizuAppKey=" + mInitParam.mMeizuAppKey);
//        }
//
//        // 设置华为推送
//        if ((!TextUtils.isEmpty(mInitParam.mHuaweiAppId)) && (phoneType == PHONE_TYPE_HUAWEI)) {
//            builder.enableHWPush();
//            ALog.getInstance().d(TAG, "<initialize> enable HUAWEI phone"
//                    + ", mHuaweiAppId=" + mInitParam.mHuaweiAppId);
//            enableHWPush = true;
//        }
//
//        // 设置VIVO推送
//        if ( (!TextUtils.isEmpty(mInitParam.mVivoAppId)) &&
//                (!TextUtils.isEmpty(mInitParam.mVivoAppKey)) && (phoneType == PHONE_TYPE_VIVO) ) {
//            builder.enableVivoPush();
//            ALog.getInstance().d(TAG, "<initialize> enable VIVO phone");
//        }
//
//        // 启动FCM，设置参数 senderId
//        boolean enableFCM = false;
//        if (!TextUtils.isEmpty(mInitParam.mFcmSenderId)) {
//            builder.enableFCM(mInitParam.mFcmSenderId);
//            enableFCM = true;
//            ALog.getInstance().d(TAG, "<initialize> enable Firebase push");
//        }
//
//        EMPushClient.getInstance().init(mContext, enableFCM, builder,
//            new EMValueCallBack<String>() {
//                @Override
//                public void onSuccess(String s) {
//                    mEid = EMPushClient.getInstance().getEid();
//                    ALog.getInstance().d(TAG, "<initialize.EMPushClient.onSuccess> s=" + s
//                            + ", mEid=" + mEid);
//                    synchronized (mInitDoneEvent) {
//                        mInitDoneEvent.notify();
//                    }
//                }
//
//                @Override
//                public void onError(int i, String s) {
//                    ALog.getInstance().d(TAG, "<initialize.EMPushClient.onError> i=" + i
//                            + ", s=" + s);
//                    synchronized (mInitDoneEvent) {
//                        mInitDoneEvent.notify();
//                    }
//                }
//            });
//
//        // Waiting for init done
//        synchronized (mInitDoneEvent) {
//            try {
//                mInitDoneEvent.wait(INIT_DONE_TIMEOUT);
//            } catch (InterruptedException e) {
//                ALog.getInstance().e(TAG, "<initialize> exception=" + e.getMessage());
//            }
//        }
//        mEid = EMPushClient.getInstance().getEid();
//        ALog.getInstance().d(TAG, "<initialize> mEid=" + mEid);
//
//        // 华为的离线推送还需要单独上传Token
//        if (enableHWPush) {
//            HMSPushHelper.getInstance().uploadToken(ctx, mInitParam.mHuaweiAppId);
//        }
//
//        return ErrCode.XOK;
//    }
//
//    public String getEid() {
//        return mEid;
//    }
//
//
//    private int detectPhoneType() {
//        String manuFacturer = android.os.Build.MANUFACTURER; // 制造商
//        String phoneModel = android.os.Build.MODEL; // 型号
//        String brand = android.os.Build.BRAND; // 品牌
//        String devName = android.os.Build.DEVICE; // 设备名
//
//        ALog.getInstance().d(TAG, "<detectPhoneType> manuFacturer=" + manuFacturer
//                + ", phoneModel=" + phoneModel
//                + ", brand=" + brand
//                + ", devName=" + devName);
//
//        if ( (manuFacturer.compareToIgnoreCase(MANU_FACTURER_XIAOMI) == 0) ||
//                (manuFacturer.compareToIgnoreCase(MANU_FACTURER_REDMI) == 0)) {
//            return PHONE_TYPE_MI;
//        }
//
//        if (manuFacturer.compareToIgnoreCase(MANU_FACTURER_OPPO) == 0) {
//            return PHONE_TYPE_OPPO;
//        }
//
//        if (manuFacturer.compareToIgnoreCase(MANU_FACTURER_MEIZU) == 0) {
//            return PHONE_TYPE_MEIZU;
//        }
//
//        if (manuFacturer.compareToIgnoreCase(MANU_FACTURER_VIVO) == 0)  {
//            return PHONE_TYPE_VIVO;
//        }
//
//        if ( (manuFacturer.compareToIgnoreCase(MANU_FACTURER_HUAWEI) == 0) ||
//                (manuFacturer.compareToIgnoreCase(MANU_FACTURER_HONOR) == 0) ||
//                (manuFacturer.compareToIgnoreCase(MANU_FACTURER_NOVA) == 0)) {
//            return PHONE_TYPE_HUAWEI;
//        }
//
//        return PHONE_TYPE_UNKNOWN;
//    }
//
//}
