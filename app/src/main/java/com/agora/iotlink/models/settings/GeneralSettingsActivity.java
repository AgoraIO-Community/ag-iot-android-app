package com.agora.iotlink.models.settings;

import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.databinding.ActivityGeneralSettingsBinding;
import com.agora.iotlink.event.UserLogoutEvent;
import com.agora.iotlink.manager.PagePathConstant;
import com.agora.iotlink.manager.PagePilotManager;
import com.agora.iotlink.models.login.LoginViewModel;
import com.alibaba.android.arouter.facade.annotation.Route;

import org.greenrobot.eventbus.EventBus;

/**
 * 通用设置
 */
@Route(path = PagePathConstant.pageGeneralSettings)
public class GeneralSettingsActivity extends BaseViewBindingActivity<ActivityGeneralSettingsBinding> {
    /**
     * 登录模块统一ViewModel
     */
    private LoginViewModel phoneLoginViewModel;

    @Override
    protected ActivityGeneralSettingsBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityGeneralSettingsBinding.inflate(inflater);
    }

    @Override
    public void initListener() {
        phoneLoginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        phoneLoginViewModel.setLifecycleOwner(this);
        getBinding().tvMsgPush.setOnClickListener(view -> PagePilotManager.pageMessagePushSetting());
        getBinding().tvCheckUpdate.setOnClickListener(view -> PagePilotManager.pageAppUpdate());
        getBinding().tvAccountSecurity.setOnClickListener(view -> PagePilotManager.pageAccountSecurity());
        getBinding().tvSystemPermissionSetting.setOnClickListener(view -> PagePilotManager.pageSystemPermissionSetting());
        getBinding().btnLogout.setOnClickListener(view -> {
            phoneLoginViewModel.requestLogout();
            EventBus.getDefault().post(new UserLogoutEvent());
            PagePilotManager.pagePhoneLogin();
            mHealthActivityManager.popActivity();
        });
    }
}
