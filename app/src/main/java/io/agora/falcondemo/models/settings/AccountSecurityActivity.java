package io.agora.falcondemo.models.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.agora.baselibrary.base.BaseDialog;

import io.agora.falcondemo.base.PushApplication;
import io.agora.falcondemo.thirdpartyaccount.ThirdAccountMgr;
import io.agora.iotlink.ErrCode;
import io.agora.falcondemo.R;
import io.agora.falcondemo.base.BaseViewBindingActivity;
import io.agora.falcondemo.common.Constant;
import io.agora.falcondemo.databinding.ActivityAccountSecurityBinding;
import io.agora.falcondemo.dialog.CommonDialog;
import io.agora.falcondemo.models.login.AccountLoginActivity;
import io.agora.falcondemo.utils.AppStorageUtil;


/**
 * 账号安全
 */
public class AccountSecurityActivity extends BaseViewBindingActivity<ActivityAccountSecurityBinding> {


    private AccountSecurityActivity mActivity;

    @Override
    protected ActivityAccountSecurityBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityAccountSecurityBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        mActivity = this;

        String localNodeId = ThirdAccountMgr.getInstance().getLocalNodeId();
        getBinding().tvNodeId.setText(localNodeId);
    }

    @Override
    public void initListener() {

        getBinding().btnLogout.setOnClickListener(view -> {
            accountLogout();
        });
        getBinding().tvLogOff.setOnClickListener(view -> {
            accountUnregister();
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    /**
     * @brief 登出账号
     */
    public void accountLogout() {
        if (commonDialog == null) {
            commonDialog = new CommonDialog(this);
            commonDialog.setDialogTitle(getString(R.string.you_want_to_log_out));
            commonDialog.setDialogBtnText(getString(R.string.cancel), getString(R.string.confirm));
            commonDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {
                }

                @Override
                public void onRightButtonClick() {
                    // 进行登出操作
                    int errCode = ErrCode.XOK; // AIotAppSdkFactory.getInstance().logout();
                    if (errCode == ErrCode.XOK) {
                        popupMessage("User account logout successful!");

                        AppStorageUtil.safePutString(mActivity, Constant.ACCOUNT, "");
                        gotoLoginActivity();

                    } else {
                        popupMessage("User account logout failure, errCode=" + errCode);
                    }

                }
            });
        }
        commonDialog.setCanceledOnTouchOutside(false);
        commonDialog.show();
    }

    /**
     * @brief 注销账号
     */
    public void accountUnregister() {
        popupMessage(getString(R.string.current_does_not_support_logout));
    }


    void gotoLoginActivity() {
        Intent intent = new Intent(AccountSecurityActivity.this, AccountLoginActivity.class);
        startActivity(intent);
        PushApplication.getInstance().setUiPage(Constant.UI_PAGE_LOGIN);
    }
}
