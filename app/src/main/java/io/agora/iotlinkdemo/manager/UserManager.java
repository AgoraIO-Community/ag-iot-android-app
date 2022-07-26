package io.agora.iotlinkdemo.manager;

import android.text.TextUtils;

import io.agora.iotlink.AIotAppSdkFactory;

public class UserManager {
    public static boolean isLogin() {
        return !TextUtils.isEmpty(AIotAppSdkFactory.getInstance().getAccountMgr().getLoggedAccount());
    }
}
