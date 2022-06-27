package com.agora.iotlink.models.welcome;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.SPUtil;
import com.agora.iotlink.R;
import com.agora.iotlink.base.AgoraApplication;
import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.common.Constant;
import com.agora.iotlink.databinding.ActivityWelcomeBinding;
import com.agora.iotlink.dialog.CommonDialog;
import com.agora.iotlink.dialog.UserAgreementDialog;
import com.agora.iotlink.manager.PagePilotManager;
import com.agora.iotlink.models.login.LoginViewModel;
import com.agora.iotsdk20.AIotAppSdkFactory;

public class WelcomeActivity extends BaseViewBindingActivity<ActivityWelcomeBinding> {
    private UserAgreementDialog userAgreementDialog;
    /**
     * 登录模块统一ViewModel
     */
    private LoginViewModel phoneLoginViewModel;

    @Override
    protected ActivityWelcomeBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityWelcomeBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        phoneLoginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        phoneLoginViewModel.setLifecycleOwner(this);
    }

    @Override
    public void initListener() {
        phoneLoginViewModel.setISingleCallback((var1, var2) -> {
            if (var1 == Constant.CALLBACK_TYPE_LOGIN_REQUEST_LOGIN_SUCCESS) {
                PagePilotManager.pageMainHome();
            } else if (var1 == Constant.CALLBACK_TYPE_LOGIN_REQUEST_LOGIN_FAIL) {
                PagePilotManager.pagePhoneLogin();
            }
            hideLoadingView();
        });
    }

    @Override
    public boolean isBlackDarkStatus() {
        return false;
    }

    /**
     * 显示用户协议 隐私政策对话框
     */
    private void showUserAgreementDialog() {
        if (userAgreementDialog == null) {
            userAgreementDialog = new UserAgreementDialog(this);
            userAgreementDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {
                    userAgreementDialog.dismiss();
                    mHealthActivityManager.popActivity();
                }

                @Override
                public void onRightButtonClick() {
                    startMainActivity();
                    userAgreementDialog.dismiss();
                    SPUtil.Companion.getInstance(WelcomeActivity.this).putBoolean(Constant.IS_AGREE, true);

                }
            });
        }
        userAgreementDialog.show();
    }

    private boolean isRequestSuspension = false;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void getPermissions() {
        AgoraApplication appInstance = (AgoraApplication) getApplication();
        appInstance.initializeEngine();
        phoneLoginViewModel.onStart();
        if (!Settings.canDrawOverlays(this)) {
            showRequestSuspensionDialog();
        } else {
            checkStatusToStart();
        }
    }

    private void checkStatusToStart() {
        if (!SPUtil.Companion.getInstance(WelcomeActivity.this).getBoolean(Constant.IS_AGREE, false)) {
            showUserAgreementDialog();
        } else {
            startMainActivity();
        }
    }

    /**
     * 获取悬浮窗权限提示
     */
    public void showRequestSuspensionDialog() {
        if (commonDialog == null) {
            commonDialog = new CommonDialog(this);
            commonDialog.setDialogTitle("请给软件设置悬浮窗权限，否则收不到被叫通知！");
            commonDialog.setDialogBtnText(getString(R.string.cancel), getString(R.string.confirm));
            commonDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {
                    checkStatusToStart();
                }

                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onRightButtonClick() {
                    isRequestSuspension = true;
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    startActivity(intent);
                }
            });
        }
        commonDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isRequestSuspension) {
            checkStatusToStart();
        }
    }

    private void startMainActivity() {
        String account = AIotAppSdkFactory.getInstance().getAccountMgr().getLoggedAccount();
        if (TextUtils.isEmpty(account)) {
            account = SPUtil.Companion.getInstance(this).getString(Constant.ACCOUNT, null);
            if (!TextUtils.isEmpty(account)) {
                String password = SPUtil.Companion.getInstance(this).getString(Constant.PASSWORD, null);
                if (!TextUtils.isEmpty(password)) {
                    phoneLoginViewModel.requestLogin(account, password);
                }
            } else {
                PagePilotManager.pagePhoneLogin();
            }
        } else {
            PagePilotManager.pageMainHome();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isRequestSuspension) {
            phoneLoginViewModel.onStop();
            mHealthActivityManager.finishActivityByClass("WelcomeActivity");
        }
    }

}
