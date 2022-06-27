package com.agora.iotlink.models.settings;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.listener.ISingleCallback;
import com.agora.baselibrary.utils.StringUtils;
import com.agora.baselibrary.utils.ToastUtils;
import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.common.Constant;
import com.agora.iotlink.databinding.ActivityChangePwdBinding;
import com.agora.iotlink.event.UserLogoutEvent;
import com.agora.iotlink.manager.PagePathConstant;
import com.agora.iotlink.manager.PagePilotManager;
import com.agora.iotlink.models.login.LoginViewModel;
import com.agora.iotsdk20.ErrCode;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import org.greenrobot.eventbus.EventBus;

/**
 * 修改密码
 */
@Route(path = PagePathConstant.pageChangePassword)
public class ChangePasswordActivity extends BaseViewBindingActivity<ActivityChangePwdBinding> {

    /**
     * 登录模块统一ViewModel
     */
    private LoginViewModel phoneLoginViewModel;

    @Override
    protected ActivityChangePwdBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityChangePwdBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        ARouter.getInstance().inject(this);
        phoneLoginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        phoneLoginViewModel.setLifecycleOwner(this);
    }

    @Override
    public void initListener() {
        getBinding().iBtnEyeOld.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                getBinding().etPwdOld.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                getBinding().etPwdOld.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            getBinding().etPwdOld.setSelection(getBinding().etPwdOld.getText().length());
        });
        getBinding().iBtnEyeNew.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                getBinding().etPwdNew.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                getBinding().etPwdNew.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            getBinding().etPwdNew.setSelection(getBinding().etPwdNew.getText().length());
        });
        getBinding().btnFinish.setOnClickListener(view -> {
            if (!checkPwd()) {
                getBinding().tvErrorTips.setVisibility(View.VISIBLE);
                return;
            }
            int ret = phoneLoginViewModel.passwordChange(getBinding().etPwdOld.getText().toString(), getBinding().etPwdNew.getText().toString());
            if (ret != ErrCode.XOK) {
                ToastUtils.INSTANCE.showToast("更换密码失败，错误码: " + ret);
            }
        });
        getBinding().etPwdOld.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String input = editable.toString();
                if (!TextUtils.isEmpty(input)) {
                    getBinding().iBtnOldClear.setVisibility(View.VISIBLE);
                } else {
                    getBinding().iBtnOldClear.setVisibility(View.GONE);
                }
            }
        });
        getBinding().etPwdNew.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String input = editable.toString();
                if (!TextUtils.isEmpty(input)) {
                    getBinding().iBtnNewClear.setVisibility(View.VISIBLE);
                } else {
                    getBinding().iBtnNewClear.setVisibility(View.GONE);
                }
            }
        });
        phoneLoginViewModel.setISingleCallback(new ISingleCallback<Integer, Object>() {
            @Override
            public void onSingleCallback(Integer var1, Object var2) {
                if (var1 == Constant.CALLBACK_TYPE_LOGIN_REQUEST_RESET_PWD_SUCCESS) {
                    phoneLoginViewModel.requestLogout();
                    EventBus.getDefault().post(new UserLogoutEvent());
                    PagePilotManager.pagePhoneLogin();
                    mHealthActivityManager.popActivity();
                }
            }
        });
    }

    /**
     * 检查密码
     *
     * @return true 密码合规
     */
    private boolean checkPwd() {
        String inputPwd = getBinding().etPwdNew.getText().toString();
        if (TextUtils.isEmpty(inputPwd)) {
            return false;
        } else if (inputPwd.length() < 8 || inputPwd.length() > 20) {
            return false;
        } else return StringUtils.INSTANCE.checkPwdFormat(inputPwd);
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
