package io.agora.iotlinkdemo.models.login.ui;

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

import com.agora.baselibrary.utils.SPUtil;
import com.agora.baselibrary.utils.StringUtils;
import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivitySetPwdBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.login.LoginViewModel;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import kotlin.jvm.JvmField;

/**
 * 设置密码
 */
@Route(path = PagePathConstant.pageSetPwd)
public class SetPwdActivity extends BaseViewBindingActivity<ActivitySetPwdBinding> {

    /**
     * 输入的账号
     */
    @JvmField
    @Autowired(name = "account")
    String account = "";

    /**
     * 验证码
     */
    @JvmField
    @Autowired(name = Constant.CODE)
    String code = "";
    /**
     * 流程类型 true 忘记密码流程 false 注册流程
     */
    @JvmField
    @Autowired(name = Constant.IS_FORGE_PASSWORD)
    boolean isForgePassword = false;
    /**
     * 登录模块统一ViewModel
     */
    private LoginViewModel phoneLoginViewModel;

    @Override
    protected ActivitySetPwdBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivitySetPwdBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        ARouter.getInstance().inject(this);
        phoneLoginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        phoneLoginViewModel.setLifecycleOwner(this);
    }

    @Override
    public void initListener() {
        phoneLoginViewModel.setISingleCallback((var1, var2) -> {
            if (var1 == Constant.CALLBACK_TYPE_EXIT_STEP) {
                mHealthActivityManager.finishActivityByClass("PhoneLoginActivity");
            } else if (var1 == Constant.CALLBACK_TYPE_LOGIN_REQUEST_REGISTER_SUCCESS) {
                ToastUtils.INSTANCE.showToast("注册成功");
                SPUtil.Companion.getInstance(this).putString(Constant.ACCOUNT, "");
                SPUtil.Companion.getInstance(this).putString(Constant.PASSWORD, "");
                PagePilotManager.pagePhoneLogin();
                mHealthActivityManager.popAllActivity();
            } else if (var1 == Constant.CALLBACK_TYPE_LOGIN_REQUEST_RESET_PWD_SUCCESS) {
                SPUtil.Companion.getInstance(this).putString(Constant.ACCOUNT, "");
                SPUtil.Companion.getInstance(this).putString(Constant.PASSWORD, "");
                ToastUtils.INSTANCE.showToast("重置密码成功");
                PagePilotManager.pagePhoneLogin();
                mHealthActivityManager.popAllActivity();
            }
        });
        getBinding().btnFinish.setOnClickListener(view -> {
            if (!checkPwd()) {
                getBinding().tvErrorTips.setVisibility(View.VISIBLE);
                return;
            }
            if (isForgePassword) {
                phoneLoginViewModel.passwordReset(account, getBinding().etPwd.getText().toString(), code);
                return;
            }
            if (StringUtils.INSTANCE.checkPhoneNum(account)) {
                phoneLoginViewModel.registerPhoneAccount(account, getBinding().etPwd.getText().toString(), code);
            } else {
                phoneLoginViewModel.registerMailAccount(account, getBinding().etPwd.getText().toString(), code);
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
                } else {
                    getBinding().iBtnClear.setVisibility(View.GONE);
                }
            }
        });
        getBinding().iBtnClear.setOnClickListener(view -> {
            getBinding().etPwd.setText("");
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

    /**
     * 检查密码
     *
     * @return true 密码合规
     */
    private boolean checkPwd() {
        String inputPwd = getBinding().etPwd.getText().toString();
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
