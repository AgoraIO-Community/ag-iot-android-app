package io.agora.falcondemo.models.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

import io.agora.falcondemo.base.PushApplication;
import io.agora.falcondemo.thirdpartyaccount.ThirdAccountMgr;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.falcondemo.base.BaseViewBindingActivity;
import io.agora.falcondemo.common.Constant;
import io.agora.falcondemo.databinding.ActivityLoginBinding;
import io.agora.falcondemo.models.home.MainActivity;
import io.agora.falcondemo.utils.AppStorageUtil;


public class AccountLoginActivity extends BaseViewBindingActivity<ActivityLoginBinding> {
    private final String TAG = "IOTLINK/LoginAct";




    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseActivity /////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected ActivityLoginBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityLoginBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        String account = AppStorageUtil.safeGetString(this, Constant.ACCOUNT, null);
        if (!TextUtils.isEmpty(account)) {
            getBinding().etAccounts.setText(account);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected boolean isCanExit() {
        return true;
    }

    @Override
    public void initListener() {

        getBinding().iBtnClearAccount.setOnClickListener(view -> {
            getBinding().etAccounts.setText("");
        });

        getBinding().btnLogin.setOnClickListener(view -> {
            onBtnLogin();
        });

        getBinding().btnRegister.setOnClickListener(view -> {
            gotoRegisterActivity();
        });
    }



    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////// Internal Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    void onBtnLogin() {
        String account = getBinding().etAccounts.getText().toString();
        if (TextUtils.isEmpty(account)) {
            popupMessage("Please input valid account!");
            return;
        }

        String appId = AppStorageUtil.safeGetString(this, Constant.APP_ID, null);
        if (TextUtils.isEmpty(appId)) {
            popupMessage("appId is empty, please clear application cache and input appId!");
            return;
        }

        // 第三方账号系统登录
        ThirdAccountMgr.UserActiveParam activeParam = new ThirdAccountMgr.UserActiveParam();
        activeParam.mAppId = appId;
        activeParam.mUserId = account;
        activeParam.mClientType = 2;
        showLoadingView();
        ThirdAccountMgr.getInstance().userActive(activeParam, new ThirdAccountMgr.IUserActiveCallback() {
            @Override
            public void onThirdAccountUserActiveDone(ThirdAccountMgr.UserActiveParam activeParam,
                                                     ThirdAccountMgr.UserActiveResult activeResult) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoadingView();
                        if (activeResult.mErrCode != ErrCode.XOK) {
                            popupMessage("appId is empty, please clear application cache and input appId!");
                            return;
                        }

                        doAIotSdkInitialize(appId, account, activeResult.mNodeId, activeResult.mNodeToken);
                    }
                });
            }
        });
    }

    /**
     * @brief 根据第三方账号登录信息，初始化 AIotSdk 引擎
     */
    void doAIotSdkInitialize(final String appId, final String userId,
                             final String localNodeId, final String localNodeToken  ) {

        IAgoraIotAppSdk.InitParam initParam = new IAgoraIotAppSdk.InitParam();
        initParam.mContext = this;
        initParam.mAppId = appId;
        initParam.mLocalNodeId = localNodeId;
        initParam.mLocalNodeToken = localNodeToken;
        initParam.mRegion = 1;
        initParam.mCustomerKey = ThirdAccountMgr.getInstance().getRequestKey();
        initParam.mCustomerSecret = ThirdAccountMgr.getInstance().getRequestSecret();
        initParam.mLogFileName = "aiotsdk.log";

        int ret = AIotAppSdkFactory.getInstance().initialize(initParam);
        if (ret != ErrCode.XOK) {
            hideLoadingView();
            popupMessage("Fail to initialize AIoT SDK, ret=" + ret);
            ThirdAccountMgr.getInstance().userDeactive();
            return;
        }

        AppStorageUtil.safePutString(this, Constant.ACCOUNT, userId);
        popupMessage("Account Login successful!");
        gotoMainActivity();
    }

    void gotoRegisterActivity() {
        Intent intent = new Intent(AccountLoginActivity.this, AccountRegisterActivity.class);
        startActivity(intent);
        PushApplication.getInstance().setUiPage(Constant.UI_PAGE_REGISTER);
    }

    void gotoMainActivity() {
        Intent intent = new Intent(AccountLoginActivity.this, MainActivity.class);
        startActivity(intent);
        PushApplication.getInstance().setUiPage(Constant.UI_PAGE_HOME);
    }
}
