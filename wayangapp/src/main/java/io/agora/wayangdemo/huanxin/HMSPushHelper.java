package io.agora.wayangdemo.huanxin;

import android.content.Context;
import android.text.TextUtils;

import io.agora.iotlink.logger.ALog;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;
import com.hyphenate.chat.EMClient;
import com.hyphenate.util.EMLog;

import java.lang.reflect.Method;



public class HMSPushHelper {
    private final String TAG = "IOTSDK/HMSPushHelper";
    private static HMSPushHelper instance;

    private HMSPushHelper(){}

    public static HMSPushHelper getInstance() {
        if (instance == null) {
            instance = new HMSPushHelper();
        }
        return instance;
    }

    /**
     * 申请华为Push Token
     * 1、getToken接口只有在AppGallery Connect平台开通服务后申请token才会返回成功。
     *
     * 2、EMUI10.0及以上版本的华为设备上，getToken接口直接返回token。如果当次调用失败Push会缓存申请，之后会自动重试申请，成功后则以onNewToken接口返回。
     *
     * 3、低于EMUI10.0的华为设备上，getToken接口如果返回为空，确保Push服务开通的情况下，结果后续以onNewToken接口返回。
     *
     * 4、服务端识别token过期后刷新token，以onNewToken接口返回。
     */
    public boolean uploadToken(Context ctx, final String appId) {
        //
        // Check build version
        //
        String buildVersion = "";
        try {
            Class<?> clsClient = Class.forName("com.huawei.hms.api.HuaweiApiClient");
            if (clsClient == null) {
                ALog.getInstance().e(TAG, "<uploadToken> NOT found API client");
                return false;
            }
            Class<?> clsSysProp = Class.forName("android.os.SystemProperties");
            if (clsSysProp == null) {
                ALog.getInstance().e(TAG, "<uploadToken> NOT found Sys Prop");
                return false;
            }
            Method methodGet = clsSysProp.getDeclaredMethod("get", new Class<?>[] {String.class});
            if (methodGet == null) {
                ALog.getInstance().e(TAG, "<uploadToken> NOT found get method");
                return false;
            }
            buildVersion = (String)methodGet.invoke(methodGet, new Object[]{"ro.build.version.emui"});

        } catch (ClassNotFoundException classExp) {
            classExp.printStackTrace();
            ALog.getInstance().e(TAG, "<uploadToken> find class exception, exp=" + classExp);
            return false;

        } catch (NoSuchMethodException methodExp) {
            methodExp.printStackTrace();
            ALog.getInstance().e(TAG, "<uploadToken> find method exception, exp=" + methodExp);
            return false;

        } catch (Exception exp) {
            exp.printStackTrace();
            ALog.getInstance().e(TAG, "<uploadToken> get ver exception, exp=" + exp);
            return false;
        }
        if (TextUtils.isEmpty(buildVersion)) {
            ALog.getInstance().e(TAG, "<uploadToken> invalid buildVersion");
            return false;
        }
        ALog.getInstance().d(TAG, "<uploadToken> buildVersion=" + buildVersion);

        //
        // get token and upload it
        //
        new Thread() {
            @Override
            public void run() {
                String token = "";
                try {
                    // 申请华为推送token
                    token = HmsInstanceId.getInstance(ctx).getToken(appId, "HCM");
                } catch (ApiException apiExp) {
                    apiExp.printStackTrace();
                    ALog.getInstance().e(TAG, "<uploadToken> getToken exception, exp=" + apiExp);
                    return;
                }
                if (TextUtils.isEmpty(token)) {
                    ALog.getInstance().e(TAG, "<uploadToken> invalid token");
                    return;
                }

                EMClient.getInstance().sendHMSPushTokenToServer(token);
                ALog.getInstance().d(TAG, "<uploadToken> uploaded token, token=" + token);
            }
        }.start();

        return true;
    }
}
