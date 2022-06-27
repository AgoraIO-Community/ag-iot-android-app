package com.agora.iotlink.models.settings;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.listener.ISingleCallback;
import com.agora.iotlink.R;
import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.common.Constant;
import com.agora.iotlink.databinding.ActivityAccountSecurityBinding;
import com.agora.iotlink.dialog.CommonDialog;
import com.agora.iotlink.event.UserLogoutEvent;
import com.agora.iotlink.manager.PagePathConstant;
import com.agora.iotlink.manager.PagePilotManager;
import com.agora.iotlink.models.login.LoginViewModel;
import com.alibaba.android.arouter.facade.annotation.Route;

import org.greenrobot.eventbus.EventBus;

/**
 * 账号安全
 */
@Route(path = PagePathConstant.pageAccountSecurity)
public class AccountSecurityActivity extends BaseViewBindingActivity<ActivityAccountSecurityBinding> {
    /**
     * 登录模块统一ViewModel
     */
    private LoginViewModel phoneLoginViewModel;

    @Override
    protected ActivityAccountSecurityBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityAccountSecurityBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        phoneLoginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        phoneLoginViewModel.setLifecycleOwner(this);
    }

    @Override
    public void initListener() {
        getBinding().tvChangePassword.setOnClickListener(view -> PagePilotManager.pageChangePassword());
        getBinding().btnLogout.setOnClickListener(view -> {
            phoneLoginViewModel.requestLogout();
            EventBus.getDefault().post(new UserLogoutEvent());
            PagePilotManager.pagePhoneLogin();
            mHealthActivityManager.popActivity();
        });
        getBinding().tvLogOff.setOnClickListener(view -> {
            showRequestSuspensionDialog();
        });
        phoneLoginViewModel.setISingleCallback((var1, var2) -> {
            if (var1 == Constant.CALLBACK_TYPE_LOGOFF_SUCCESS) {
                EventBus.getDefault().post(new UserLogoutEvent());
                PagePilotManager.pagePhoneLogin();
                mHealthActivityManager.popAllActivity();
            }
        });
    }

    /**
     * 获取悬浮窗权限提示
     */
    public void showRequestSuspensionDialog() {
        if (commonDialog == null) {
            commonDialog = new CommonDialog(this);
            commonDialog.setDialogTitle("确定要注销帐号吗？");
            commonDialog.setDialogBtnText(getString(R.string.cancel), getString(R.string.confirm));
            commonDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {

                }

                @Override
                public void onRightButtonClick() {
                    phoneLoginViewModel.unregister();
                }
            });
        }
        commonDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        phoneLoginViewModel.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        phoneLoginViewModel.onStop();
    }
}
