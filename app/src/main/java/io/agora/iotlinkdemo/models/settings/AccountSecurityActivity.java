package io.agora.iotlinkdemo.models.settings;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.listener.ISingleCallback;

import io.agora.iotlink.ErrCode;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityAccountSecurityBinding;
import io.agora.iotlinkdemo.dialog.CommonDialog;
import io.agora.iotlinkdemo.event.UserLogoutEvent;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.login.LoginViewModel;
import io.agora.iotlinkdemo.utils.AppStorageUtil;

import com.agora.baselibrary.utils.SPUtil;
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
    private AccountSecurityActivity mActivity;

    @Override
    protected ActivityAccountSecurityBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityAccountSecurityBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        phoneLoginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        phoneLoginViewModel.setLifecycleOwner(this);
        mActivity = this;
    }

    @Override
    public void initListener() {
        getBinding().tvChangePassword.setOnClickListener(view -> {
            // PagePilotManager.pageChangePassword();
            popupMessage("当前不支持密码修改功能!");
        });
        getBinding().btnLogout.setOnClickListener(view -> {
            showLoadingView();
            phoneLoginViewModel.accountLogout();
        });
        getBinding().tvLogOff.setOnClickListener(view -> {
            showRequestSuspensionDialog();
        });
        phoneLoginViewModel.setISingleCallback((var1, var2) -> {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideLoadingView();

                    if (var1 == Constant.CALLBACK_TYPE_THIRD_LOGOUT_DONE) {
                        LoginViewModel.ErrInfo errInfo = (LoginViewModel.ErrInfo)var2;
                        if (errInfo.mErrCode == ErrCode.XOK) {
                            EventBus.getDefault().post(new UserLogoutEvent());
                            PagePilotManager.pagePhoneLogin();
                            mHealthActivityManager.popActivity();
                        } else {
                            popupMessage("登出失败！");
                        }

                    } else if (var1 == Constant.CALLBACK_TYPE_THIRD_UNREGISTER_DONE) {
                        LoginViewModel.ErrInfo errInfo = (LoginViewModel.ErrInfo)var2;
                        if (errInfo.mErrCode == ErrCode.XOK) {
                            EventBus.getDefault().post(new UserLogoutEvent());
                            PagePilotManager.pagePhoneLogin();
                            mHealthActivityManager.popActivity();
                        } else {
                            popupMessage("注销失败！");
                        }
                    }
                }
            });


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
                    String account = AppStorageUtil.safeGetString(mActivity, Constant.ACCOUNT, null);
                    String password = AppStorageUtil.safeGetString(mActivity, Constant.PASSWORD, null);
                    showLoadingView();
                    phoneLoginViewModel.accountUnregister(account, password);
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
