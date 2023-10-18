package io.agora.falcondemo.models.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.falcondemo.base.BaseViewBindingActivity;
import io.agora.falcondemo.base.PushApplication;
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

        showLoadingView();

        IAgoraIotAppSdk.LoginParam prepareParam = new IAgoraIotAppSdk.LoginParam();
        prepareParam.mUserId = account;
        prepareParam.mClientType = 2;
        int ret = AIotAppSdkFactory.getInstance().login(prepareParam, new IAgoraIotAppSdk.OnLoginListener() {
            @Override
            public void onSdkLoginDone(IAgoraIotAppSdk.LoginParam loginParam, int errCode) {
                Log.d(TAG, "<onBtnLogin.onSdkLoginDone> errCode=" + errCode);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoadingView();
                        if (errCode != ErrCode.XOK) {
                            popupMessage("Fail to login account, errCode=" + errCode);
                            return;
                        }

                        popupMessageLongTime("Login account successful!");
                        gotoMainActivity();

                    }
                });
            }
        });

        if (ret != ErrCode.XOK) {
            hideLoadingView();
            popupMessage("Fail to login account, ret=" + ret);
            return;
        }

        AppStorageUtil.safePutString(this, Constant.ACCOUNT, account);
    }


    void gotoRegisterActivity() {
        Intent intent = new Intent(AccountLoginActivity.this, AccountRegisterActivity.class);
        startActivity(intent);
    }

    void gotoMainActivity() {
        Intent intent = new Intent(AccountLoginActivity.this, MainActivity.class);
        startActivity(intent);
    }
}
