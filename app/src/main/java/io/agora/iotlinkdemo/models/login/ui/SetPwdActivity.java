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

import io.agora.iotlink.ErrCode;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivitySetPwdBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.login.LoginViewModel;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import io.agora.iotlinkdemo.utils.AppStorageUtil;
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

            } else if (var1 == Constant.CALLBACK_TYPE_THIRD_REGISTER_DONE) {
                getBinding().etPwd.post(() -> {
                    LoginViewModel.ErrInfo result = (LoginViewModel.ErrInfo)var2;
                    hideLoadingView();

                    if (result.mErrCode == ErrCode.XOK) {
                        ToastUtils.INSTANCE.showToast("注册成功");
                        AppStorageUtil.safePutString(this, Constant.ACCOUNT, "");
                        AppStorageUtil.safePutString(this, Constant.PASSWORD, "");
                        PagePilotManager.pagePhoneLogin();
                        mHealthActivityManager.popAllActivity();

                    } else {
                        String errTips = "注册失败";
                        if (!TextUtils.isEmpty(result.mErrTips)) {
                            errTips = errTips + " " + result.mErrTips;
                        }
                        ToastUtils.INSTANCE.showToast(errTips);
                    }
                });
            }
        });
        getBinding().btnFinish.setOnClickListener(view -> {
            if (!checkPwd()) {
                getBinding().tvErrorTips.setVisibility(View.VISIBLE);
                return;
            }

            if (StringUtils.INSTANCE.checkPhoneNum(account)) {
                // 注册操作
                showLoadingView();
                phoneLoginViewModel.accountRegister(account, getBinding().etPwd.getText().toString(), code);
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
        } else if (inputPwd.length() < 1 || inputPwd.length() > 20) {
            return false;
        } else {
            //return StringUtils.INSTANCE.checkPwdFormat(inputPwd);
            return true;
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
