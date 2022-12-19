package io.agora.iotlinkdemo.models.login.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.utils.CountDownTimerUtils;
import com.agora.baselibrary.utils.ToastUtils;

import io.agora.iotlink.ErrCode;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityInputVCodeBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.login.LoginViewModel;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import kotlin.jvm.JvmField;

/**
 * 输入验证码
 */
@Route(path = PagePathConstant.pageInputVCode)
public class InputVCodeActivity extends BaseViewBindingActivity<ActivityInputVCodeBinding> {
    private final String TAG = "IOTLINK/InputVCodeAct";

    /**
     * 输入的账号
     */
    @JvmField
    @Autowired(name = "account")
    String account = "";

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

    /**
     * 当前选中的是第几个输入框
     */
    private int currentPosition = 0;

    /**
     * 记时器
     */
    private CountDownTimerUtils countDownTimerUtils;

    @Override
    protected ActivityInputVCodeBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityInputVCodeBinding.inflate(inflater);
    }


    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        ARouter.getInstance().inject(this);
        phoneLoginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        phoneLoginViewModel.setLifecycleOwner(this);
        phoneLoginViewModel.setISingleCallback((var1, var2) -> {
            if (var1 == Constant.CALLBACK_TYPE_EXIT_STEP) {
                mHealthActivityManager.finishActivityByClass("InputVCodeActivity");

            } else if (var1 == Constant.CALLBACK_TYPE_THIRD_REQVCODE_DONE) {  // 完成验证码请求
                LoginViewModel.ReqVCodeResult result = (LoginViewModel.ReqVCodeResult)var2;
                Log.d(TAG, "<initView.ISingleCallback> errCode=" + result.mErrCode
                        + ", mErrTips=" + result.mErrTips + ", phoneNumber=" + result.mPhoneNumber);
                hideLoadingView();

                getBinding().tvTitle.post(() -> {
                    if (result.mErrCode == ErrCode.XOK) {
                        ToastUtils.INSTANCE.showToast(R.string.vcode_has_send);
                    } else if (result.mErrCode == ErrCode.XERR_VCODE_VALID) {
                        ToastUtils.INSTANCE.showToast(result.mErrTips);
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
        countDownTimerUtils = new CountDownTimerUtils(getBinding().tvTimeCount, 60000, 1000);
        countDownTimerUtils.start();
    }

    @Override
    public void initListener() {
        getBinding().tvTimeCount.setOnClickListener(view -> {
            onBtnRetrieveVCode();
        });
        getBinding().etCode1.setOnFocusChangeListener(editFocusListener);
        getBinding().etCode2.setOnFocusChangeListener(editFocusListener);
        getBinding().etCode3.setOnFocusChangeListener(editFocusListener);
        getBinding().etCode4.setOnFocusChangeListener(editFocusListener);
        getBinding().etCode5.setOnFocusChangeListener(editFocusListener);
        getBinding().etCode6.setOnFocusChangeListener(editFocusListener);

        getBinding().etCode1.setOnKeyListener(onKeyListener);
        getBinding().etCode2.setOnKeyListener(onKeyListener);
        getBinding().etCode3.setOnKeyListener(onKeyListener);
        getBinding().etCode4.setOnKeyListener(onKeyListener);
        getBinding().etCode5.setOnKeyListener(onKeyListener);
        getBinding().etCode6.setOnKeyListener(onKeyListener);

        getBinding().etCode1.addTextChangedListener(textWatcher);
        getBinding().etCode2.addTextChangedListener(textWatcher);
        getBinding().etCode3.addTextChangedListener(textWatcher);
        getBinding().etCode4.addTextChangedListener(textWatcher);
        getBinding().etCode5.addTextChangedListener(textWatcher);
        getBinding().etCode6.addTextChangedListener(textWatcher);

        getBinding().btnFinish.setOnClickListener(view -> {
            if (!isAllInput()) {
                ToastUtils.INSTANCE.showToast("请输入正确验证码");
            } else {
                String code =
                        getBinding().etCode1.getText().toString()
                                + getBinding().etCode2.getText()
                                + getBinding().etCode3.getText()
                                + getBinding().etCode4.getText()
                                + getBinding().etCode5.getText()
                                + getBinding().etCode6.getText();
                //执行验证码注册
                PagePilotManager.pageSetPwd(account, code, isForgePassword);
            }
        });

        getBinding().etCode1.requestFocus();

        //InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        //inputMethodManager.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
        //inputMethodManager.showSoftInput(getBinding().etCode1, InputMethodManager.SHOW_FORCED);
    }

    /**
     * @brief 重新获取验证码
     */
    private void onBtnRetrieveVCode() {
        showLoadingView();
        countDownTimerUtils.start();
        phoneLoginViewModel.requestVCode(account, isForgePassword);
    }


    /**
     * 检查是否已输入完毕
     */
    private boolean isAllInput() {
        if (TextUtils.isEmpty(getBinding().etCode1.getText())) {
            return false;
        }
        if (TextUtils.isEmpty(getBinding().etCode2.getText())) {
            return false;
        }
        if (TextUtils.isEmpty(getBinding().etCode3.getText())) {
            return false;
        }
        if (TextUtils.isEmpty(getBinding().etCode4.getText())) {
            return false;
        }
        if (TextUtils.isEmpty(getBinding().etCode5.getText())) {
            return false;
        }
        if (TextUtils.isEmpty(getBinding().etCode6.getText())) {
            return false;
        }
        return true;
    }

    /**
     * 记录当前焦点位置
     */
    private final View.OnFocusChangeListener editFocusListener = (view, hasFocus) -> {
        if (hasFocus) {
            switch (view.getId()) {
                case R.id.etCode1:
                    currentPosition = 0;
                    break;
                case R.id.etCode2:
                    currentPosition = 1;
                    break;
                case R.id.etCode3:
                    currentPosition = 2;
                    break;
                case R.id.etCode4:
                    currentPosition = 3;
                    break;
                case R.id.etCode5:
                    currentPosition = 4;
                    break;
                case R.id.etCode6:
                    currentPosition = 5;
                    break;
            }
        }
    };
    /**
     * 删除前一个输入内
     */
    private final View.OnKeyListener onKeyListener = (v, keyCode, event) -> {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                if (v instanceof AppCompatEditText) {
                    if (((AppCompatEditText) v).getText().length() == 0) {
                        findNextFocus(false);
                    }
                    ((AppCompatEditText) v).setText("");
                }

                return true;
            }
        }
        return false;
    };

    /**
     * 寻找下一个需要获取焦点的控件
     *
     * @param isNext true 下一个  false 上一个
     */
    private void findNextFocus(boolean isNext) {
        if (isNext) {
            switch (currentPosition) {
                case 0:
                    getBinding().etCode2.setEnabled(true);
                    getBinding().etCode2.requestFocus();
                    break;
                case 1:
                    getBinding().etCode3.setEnabled(true);
                    getBinding().etCode3.requestFocus();
                    break;
                case 2:
                    getBinding().etCode4.setEnabled(true);
                    getBinding().etCode4.requestFocus();
                    break;
                case 3:
                    getBinding().etCode5.setEnabled(true);
                    getBinding().etCode5.requestFocus();
                    break;
                case 4:
                    getBinding().etCode6.setEnabled(true);
                    getBinding().etCode6.requestFocus();
                    break;
                case 5: {
                    //全部填完
                    hideInput();
                    getBinding().btnFinish.setVisibility(View.VISIBLE);
                    break;
                }
            }
        } else {
            switch (currentPosition) {
                case 0:
                    mHealthActivityManager.popActivity();
                    break;
                case 1:
                    getBinding().etCode1.requestFocus();
                    break;
                case 2:
                    getBinding().etCode2.requestFocus();
                    break;
                case 3:
                    getBinding().etCode3.requestFocus();
                    break;
                case 4:
                    getBinding().etCode4.requestFocus();
                    break;
                case 5:
                    getBinding().etCode5.requestFocus();
                    break;
            }
        }
    }

    /**
     * 输入历史记录
     */
    private String oldInput = "";

    /**
     * 监听每一个输入框输状态
     */
    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            oldInput = charSequence.toString();
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (editable.length() > 0) {
                if (editable.length() > 1) {
                    //检查是否要替换当前输入内容
                    String newInput;
                    if (String.valueOf(editable.charAt(0)).equals(oldInput)) {
                        newInput = String.valueOf(editable.charAt(1));
                    } else {
                        newInput = String.valueOf(editable.charAt(0));
                    }
                    switch (currentPosition) {
                        case 0:
                            getBinding().etCode1.setText(newInput);
                            break;
                        case 1:
                            getBinding().etCode2.setText(newInput);
                            break;
                        case 2:
                            getBinding().etCode3.setText(newInput);
                            break;
                        case 3:
                            getBinding().etCode4.setText(newInput);
                            break;
                        case 4:
                            getBinding().etCode5.setText(newInput);
                            break;
                        case 5:
                            getBinding().etCode6.setText(newInput);
                            break;
                    }
                } else {
                    //寻焦
                    findNextFocus(true);
                }
            }
        }
    };

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
