package com.agora.iotlink.manager;

import android.text.TextUtils;

import com.agora.iotsdk20.AIotAppSdkFactory;

public class UserManager {
    public static boolean isLogin() {
        return !TextUtils.isEmpty(AIotAppSdkFactory.getInstance().getAccountMgr().getLoggedAccount());
    }
}
