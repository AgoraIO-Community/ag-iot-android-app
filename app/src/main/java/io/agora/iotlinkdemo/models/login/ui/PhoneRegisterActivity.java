package io.agora.iotlinkdemo.models.login.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.utils.StringUtils;
import com.agora.baselibrary.utils.ToastUtils;

import io.agora.iotlink.ErrCode;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.api.bean.CountryBean;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityPhoneRegisterBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.login.LoginViewModel;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import kotlin.jvm.JvmField;

/**
 * 注册
 */
@Route(path = PagePathConstant.pagePhoneRegister)
public class PhoneRegisterActivity extends BaseViewBindingActivity<ActivityPhoneRegisterBinding> {
    private final String TAG = "IOTLINK/PhoneRegAct";

    private CountryBean countryBean;                ///< 当前选择的国家
    private LoginViewModel phoneLoginViewModel;



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
            hideLoadingView();

            if (var1 == Constant.CALLBACK_TYPE_EXIT_STEP) {
                mHealthActivityManager.finishActivityByClass("PhoneRegisterActivity");
            }
            else if (var1 == Constant.CALLBACK_TYPE_THIRD_REQVCODE_DONE) {  // 完成验证码请求
                LoginViewModel.ReqVCodeResult result = (LoginViewModel.ReqVCodeResult)var2;
                Log.d(TAG, "<initView.ISingleCallback> errCode=" + result.mErrCode
                        + ", mErrTips=" + result.mErrTips + ", phoneNumber=" + result.mPhoneNumber);

                getBinding().etAccounts.post(() -> {
                    if (result.mErrCode == ErrCode.XOK) {
                        ToastUtils.INSTANCE.showToast(R.string.vcode_has_send);
                        PagePilotManager.pageInputVCode((String)result.mPhoneNumber);

                    } else if (result.mErrCode == ErrCode.XERR_VCODE_VALID) {
                        ToastUtils.INSTANCE.showToast(result.mErrTips);
                        PagePilotManager.pageInputVCode((String)result.mPhoneNumber);

                    } else {
                        String errTips = getString(R.string.vcode_send_err);
                        if (!TextUtils.isEmpty(result.mErrTips)) {
                            errTips = errTips + " " + result.mErrTips;
                        }
                        ToastUtils.INSTANCE.showToast(errTips);
                    }
                });
            }
        });

    }

    @Override
    public void initListener() {
        getBinding().tvSelectCountry.setOnClickListener(view -> {
            // PagePilotManager.pageSelectCountry(this);  // 当前没有其他国家可选
        });

        getBinding().btnGetVCode.setOnClickListener(view -> {
            showLoadingView();
            //获取验证码
            phoneLoginViewModel.requestVCode(getBinding().etAccounts.getText().toString());
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
        //手机号登录
        getBinding().etAccounts.setHint("请输入手机号");
        getBinding().etAccounts.setInputType(InputType.TYPE_CLASS_PHONE);
        getBinding().etAccounts.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
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
