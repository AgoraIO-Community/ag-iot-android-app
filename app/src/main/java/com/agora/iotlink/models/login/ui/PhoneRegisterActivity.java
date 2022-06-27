package com.agora.iotlink.models.login.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.utils.StringUtils;
import com.agora.baselibrary.utils.ToastUtils;
import com.agora.iotlink.R;
import com.agora.iotlink.api.bean.CountryBean;
import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.common.Constant;
import com.agora.iotlink.databinding.ActivityPhoneRegisterBinding;
import com.agora.iotlink.manager.PagePathConstant;
import com.agora.iotlink.manager.PagePilotManager;
import com.agora.iotlink.models.login.LoginViewModel;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import kotlin.jvm.JvmField;

/**
 * 注册
 */
@Route(path = PagePathConstant.pagePhoneRegister)
public class PhoneRegisterActivity extends BaseViewBindingActivity<ActivityPhoneRegisterBinding> {

    /**
     * 登录模块统一ViewModel
     */
    private LoginViewModel phoneLoginViewModel;

    /**
     * 当前选择的国家
     */
    private CountryBean countryBean;

    /**
     * 流程类型 true 忘记密码流程 false 注册流程
     */
    @JvmField
    @Autowired(name = Constant.IS_FORGE_PASSWORD)
    boolean isForgePassword = false;

    @Override
    protected ActivityPhoneRegisterBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityPhoneRegisterBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        ARouter.getInstance().inject(this);
        countryBean = new CountryBean("中国", 10);
        getBinding().tvSelectCountry.setText(countryBean.countryName);
        getBinding().etAccounts.setEnabled(true);
        phoneLoginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        phoneLoginViewModel.setLifecycleOwner(this);
        phoneLoginViewModel.setISingleCallback((var1, var2) -> {
            if (var1 == Constant.CALLBACK_TYPE_EXIT_STEP) {
                mHealthActivityManager.finishActivityByClass("PhoneRegisterActivity");
            } else if (var1 == Constant.CALLBACK_TYPE_LOGIN_REQUEST_V_CODE_SUCCESS) {
                ToastUtils.INSTANCE.showToast(R.string.vcode_has_send);
                String type = countryBean.countryId == 10 ? "REGISTER_SMS" : "REGISTER";
                PagePilotManager.pageInputVCode((String) var2, type, isForgePassword);
            } else if (var1 == Constant.CALLBACK_TYPE_LOGIN_REQUEST_V_CODE_FAIL) {
            }
            hideLoadingView();
        });
        if (isForgePassword) {
            getBinding().tvRegister.setText(getString(R.string.forget_password));
            getBinding().tvInputTips.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void initListener() {
        getBinding().tvSelectCountry.setOnClickListener(view -> {
            PagePilotManager.pageSelectCountry(this);
        });
        getBinding().btnGetVCode.setOnClickListener(view -> {
            showLoadingView();
            String type = "";
            if (isForgePassword) {
                type = countryBean.countryId == 10 ? "PWD_RESET_SMS" : "PWD_RESET";
            } else {
                type = countryBean.countryId == 10 ? "REGISTER_SMS" : "REGISTER";
            }
            //获取验证码
            phoneLoginViewModel.requestVCode(getBinding().etAccounts.getText().toString(), type);
        });
        getBinding().etAccounts.addTextChangedListener(new TextWatcher() {
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
                    if (editable.length() > 0) {
                        getBinding().iBtnClearAccount.setVisibility(View.VISIBLE);
                    } else {
                        getBinding().iBtnClearAccount.setVisibility(View.GONE);
                    }
                    if (countryBean.countryId == 10 && StringUtils.INSTANCE.checkPhoneNum(input)) {
                        getBinding().btnGetVCode.setEnabled(true);
                    } else {
                        getBinding().btnGetVCode.setEnabled(StringUtils.INSTANCE.checkEmailFormat(input));
                    }
                } else {
                    getBinding().iBtnClearAccount.setVisibility(View.GONE);
                    getBinding().btnGetVCode.setEnabled(false);
                }
            }
        });
        getBinding().iBtnClearAccount.setOnClickListener(view -> {
            getBinding().etAccounts.setText("");
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                CountryBean countryBean = (CountryBean) data.getSerializableExtra(Constant.COUNTRY);
                if (countryBean != null) {
                    this.countryBean = countryBean;
                    getBinding().tvSelectCountry.setText(countryBean.countryName);
                    getBinding().etAccounts.setEnabled(true);
                    getBinding().etAccounts.setText("");
                    setAccountStatus();
                }
            }
        }
    }

    /**
     * 设置帐号输入框输入状态
     */
    private void setAccountStatus() {
        if (countryBean.countryId == 10) {
            //手机号登录
            getBinding().etAccounts.setHint("请输入手机号");
            getBinding().etAccounts.setInputType(InputType.TYPE_CLASS_PHONE);
            getBinding().etAccounts.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
        } else {
            //邮箱登录
            getBinding().etAccounts.setHint("请输入邮箱");
            getBinding().etAccounts.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            getBinding().etAccounts.setKeyListener(DigitsKeyListener.getInstance("1234567890qwertyuiopasdfghjklzxcvbnm.@_-"));
        }
        getBinding().etAccounts.setEnabled(true);
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
