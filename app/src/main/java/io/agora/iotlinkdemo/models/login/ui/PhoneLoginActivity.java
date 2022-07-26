package io.agora.iotlinkdemo.models.login.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.utils.SPUtil;
import com.agora.baselibrary.utils.StringUtils;
import io.agora.iotlinkdemo.api.bean.CountryBean;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityPhoneLoginBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.login.LoginViewModel;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 登录
 */
@Route(path = PagePathConstant.pagePhoneLogin)
public class PhoneLoginActivity extends BaseViewBindingActivity<ActivityPhoneLoginBinding> {

    /**
     * 登录模块统一ViewModel
     */
    private LoginViewModel phoneLoginViewModel;

    /**
     * 当前选择的国家
     */
    private CountryBean countryBean;

    @Override
    protected ActivityPhoneLoginBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityPhoneLoginBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        countryBean = new CountryBean("中国", 10);
        getBinding().tvSelectCountry.setText(countryBean.countryName);
        getBinding().etAccounts.setEnabled(true);
        getBinding().etPwd.setEnabled(true);
        phoneLoginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        phoneLoginViewModel.setLifecycleOwner(this);
        phoneLoginViewModel.setISingleCallback((var1, var2) -> {
            if (var1 == Constant.CALLBACK_TYPE_EXIT_STEP) {
                mHealthActivityManager.finishActivityByClass("PhoneLoginActivity");
            } else if (var1 == Constant.CALLBACK_TYPE_LOGIN_REQUEST_LOGIN_SUCCESS) {
                PagePilotManager.pageMainHome();
            } else if (var1 == Constant.CALLBACK_TYPE_LOGIN_REQUEST_LOGIN_FAIL) {
//                ThreadManager.getMainHandler().post(() -> {
//                    getBinding().tvErrorTips.setVisibility(View.VISIBLE);
//                });
            }
            hideLoadingView();
        });
        String countryName = SPUtil.Companion.getInstance(this).getString(Constant.COUNTRY_NAME, null);
        if (!TextUtils.isEmpty(countryName)) {
            getBinding().tvSelectCountry.setText(countryName);
            if (countryName.equals("中国")) {
                countryBean = new CountryBean("中国", 10);
            } else {
                countryBean = new CountryBean("美国", 11);
            }
        } else {
            countryBean = new CountryBean("中国", 10);
            getBinding().tvSelectCountry.setText(countryBean.countryName);
        }
        setAccountStatus();
        String account = SPUtil.Companion.getInstance(this).getString(Constant.ACCOUNT, null);
        if (!TextUtils.isEmpty(account)) {
            getBinding().etAccounts.setText(account);
            String password = SPUtil.Companion.getInstance(this).getString(Constant.PASSWORD, null);
            if (!TextUtils.isEmpty(password)) {
                getBinding().etPwd.setText(password);
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
        getBinding().etPwd.setEnabled(true);
    }

    @Override
    protected boolean isCanExit() {
        return true;
    }

    @Override
    public void initListener() {
        getBinding().tvSelectCountry.setOnClickListener(view -> {
            PagePilotManager.pageSelectCountry(this);
        });
        getBinding().btnLogin.setOnClickListener(view -> {
            if (checkAccount2Pwd()) {
                showLoadingView();
                String account = getBinding().etAccounts.getText().toString();
                String password = getBinding().etPwd.getText().toString();
                phoneLoginViewModel.requestLogin(account, password);
                SPUtil.Companion.getInstance(this).putString(Constant.ACCOUNT, account);
                SPUtil.Companion.getInstance(this).putString(Constant.PASSWORD, password);
                SPUtil.Companion.getInstance(this).putString(Constant.COUNTRY_NAME, getBinding().tvSelectCountry.getText().toString());
            }
        });
        getBinding().btnRegister.setOnClickListener(view -> {
            PagePilotManager.pagePhoneRegister(false);
        });
        getBinding().tvForgetPassword.setOnClickListener(view -> {
            PagePilotManager.pagePhoneRegister(true);
        });
        getBinding().iBtnClear.setOnClickListener(view -> {
            getBinding().etPwd.setText("");
        });
        getBinding().iBtnClearAccount.setOnClickListener(view -> {
            getBinding().etAccounts.setText("");
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
                if (editable != null && editable.length() > 0) {
                    getBinding().iBtnClearAccount.setVisibility(View.VISIBLE);
                    getBinding().btnLogin.setEnabled(checkAccount2Pwd());
                } else {
                    getBinding().iBtnClearAccount.setVisibility(View.GONE);
                }
            }
        });
        getBinding().etPwd.addTextChangedListener(new TextWatcher() {
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
                    getBinding().iBtnClear.setVisibility(View.VISIBLE);
                    getBinding().btnLogin.setEnabled(checkAccount2Pwd());
                } else {
                    getBinding().iBtnClear.setVisibility(View.GONE);
                }
            }
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
                    getBinding().iBtnClearAccount.setVisibility(View.VISIBLE);
                    getBinding().btnLogin.setEnabled(checkAccount2Pwd());
                } else {
                    getBinding().iBtnClearAccount.setVisibility(View.GONE);
                    getBinding().btnLogin.setEnabled(false);
                }
            }
        });
        getBinding().iBtnEye.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                getBinding().etPwd.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                getBinding().etPwd.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            getBinding().etPwd.setSelection(getBinding().etPwd.getText().length());
        });
//        if (BuildConfig.DEBUG) {
//            getBinding().etAccounts.setText("21705471@163.com");
//            getBinding().etPwd.setText("a1111111");
//            getBinding().btnLogin.setEnabled(true);
//        }
    }

    private boolean checkAccount2Pwd() {
        String account = getBinding().etAccounts.getText().toString();
        String password = getBinding().etPwd.getText().toString();
        if (countryBean.countryId == 10) {
            if (!StringUtils.INSTANCE.checkPhoneNum(account)) {
                return false;
            }
        } else {
            if (!StringUtils.INSTANCE.checkEmailFormat(account)) {
                return false;
            }
        }

        if (!StringUtils.INSTANCE.checkPwdFormat(password)) {
            getBinding().btnLogin.setEnabled(false);
            return false;
        }
        return password.length() >= 8 && password.length() <= 20;
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
                    getBinding().etPwd.setEnabled(true);
                    getBinding().etPwd.setText("");
                    setAccountStatus();
                }
            }
        }
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
