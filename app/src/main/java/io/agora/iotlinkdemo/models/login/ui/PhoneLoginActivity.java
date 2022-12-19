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

import io.agora.iotlink.ErrCode;
import io.agora.iotlinkdemo.api.bean.CountryBean;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityPhoneLoginBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.login.LoginViewModel;
import io.agora.iotlinkdemo.utils.AppStorageUtil;

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

        getBinding().etAccounts.setEnabled(true);
        getBinding().etPwd.setEnabled(true);
        phoneLoginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        phoneLoginViewModel.setLifecycleOwner(this);
        phoneLoginViewModel.setISingleCallback((var1, var2) -> {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideLoadingView();

                    if (var1 == Constant.CALLBACK_TYPE_EXIT_STEP) {
                        mHealthActivityManager.finishActivityByClass("PhoneLoginActivity");

                    } else if (var1 == Constant.CALLBACK_TYPE_THIRD_REGISTER_DONE) {
                        LoginViewModel.ErrInfo errInfo = (LoginViewModel.ErrInfo)var2;

                        if (errInfo.mErrCode == ErrCode.XOK) {
                            popupMessage("账号注册成功，可以进行登录了！");
                        } else if (errInfo.mErrCode == ErrCode.XERR_ACCOUNT_ALREADY_EXIST) {
                            popupMessage("账号注册失败，该账号已经存在！");
                        } else {
                            if (TextUtils.isEmpty(errInfo.mErrTips)) {
                                popupMessage("账号注册失败，错误码=" + errInfo.mErrCode);
                            } else {
                                popupMessage("账号注册失败，" + errInfo.mErrTips);
                            }
                        }

                    } else if (var1 == Constant.CALLBACK_TYPE_THIRD_LOGIN_DONE) {
                        LoginViewModel.ErrInfo errInfo = (LoginViewModel.ErrInfo)var2;

                        if (errInfo.mErrCode == ErrCode.XOK) {
                            PagePilotManager.pageMainHome();
                        } else if (errInfo.mErrCode == ErrCode.XERR_ACCOUNT_NOT_EXIST) {
                            popupMessage("账号登录失败，账号不存在！");
                        } else if (errInfo.mErrCode == ErrCode.XERR_ACCOUNT_PASSWORD_ERR) {
                            popupMessage("账号登录失败，密码错误！");
                        } else {
                            if (TextUtils.isEmpty(errInfo.mErrTips)) {
                                popupMessage("账号登录失败，错误码=" + errInfo.mErrCode);
                            } else {
                                popupMessage("账号登录失败，" + errInfo.mErrTips);
                            }
                        }
                    }
                }
            });
        });

        setAccountStatus();
        String account = AppStorageUtil.safeGetString(this, Constant.ACCOUNT, null);
        if (!TextUtils.isEmpty(account)) {
            getBinding().etAccounts.setText(account);

            String password = AppStorageUtil.safeGetString(this, Constant.PASSWORD, null);
            if (!TextUtils.isEmpty(password)) {
                getBinding().etPwd.setText(password);
            }
        }
    }

    /**
     * 设置帐号输入框输入状态
     */
    private void setAccountStatus() {
        getBinding().etAccounts.setEnabled(true);
        getBinding().etPwd.setEnabled(true);
    }

    @Override
    protected boolean isCanExit() {
        return true;
    }

    @Override
    public void initListener() {

        getBinding().btnLogin.setOnClickListener(view -> {
            if (!checkAccount2Pwd()) {
                popupMessage("登录失败，账号或密码无效!");
                return;
            }
            showLoadingView();
            String account = getBinding().etAccounts.getText().toString();
            String password = getBinding().etPwd.getText().toString();
            phoneLoginViewModel.accountLogin(account, password);
            AppStorageUtil.safePutString(this, Constant.ACCOUNT, account);
            AppStorageUtil.safePutString(this, Constant.PASSWORD, password);
        });

        getBinding().btnRegister.setOnClickListener(view -> {
            PagePilotManager.pagePhoneRegister(false);
        });

        getBinding().iBtnClear.setOnClickListener(view -> {
            getBinding().etPwd.setText("");
        });
        getBinding().iBtnClearAccount.setOnClickListener(view -> {
            getBinding().etAccounts.setText("");
        });

        getBinding().tvForgetPassword.setOnClickListener(view -> {
            PagePilotManager.pagePhoneRegister(true);
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
    }

    private boolean checkAccount2Pwd() {
        String account = getBinding().etAccounts.getText().toString();
        String password = getBinding().etPwd.getText().toString();

        if (TextUtils.isEmpty(account)) {
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            return false;
        }

        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                CountryBean countryBean = (CountryBean) data.getSerializableExtra(Constant.COUNTRY);
                if (countryBean != null) {
                    this.countryBean = countryBean;

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
