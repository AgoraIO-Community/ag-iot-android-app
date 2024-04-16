package io.agora.falcondemo.models.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.falcondemo.models.home.HomePageFragment;
import io.agora.falcondemo.thirdpartyaccount.ThirdAccountMgr;
import io.agora.iotlink.ErrCode;
import io.agora.falcondemo.base.BaseViewBindingActivity;

import io.agora.falcondemo.base.PushApplication;
import io.agora.falcondemo.common.Constant;
import io.agora.falcondemo.databinding.ActivityRegisterBinding;
import io.agora.falcondemo.utils.AppStorageUtil;


/**
 * 注册
 */
public class AccountRegisterActivity extends BaseViewBindingActivity<ActivityRegisterBinding> {
    private final String TAG = "IOTLINK/RegisterAct";


    private AccountRegisterActivity mActivity;


    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseActivity /////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected ActivityRegisterBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityRegisterBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
    }

    @Override
    public void initListener() {
        mActivity = this;

        getBinding().titleView.setLeftClick(view -> {
            PushApplication.getInstance().setUiPage(Constant.UI_PAGE_LOGIN); // 切回登录界面
            finish();
        });

        getBinding().btnRegister.setOnClickListener(view -> {
            onBtnRegister();
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            PushApplication.getInstance().setUiPage(Constant.UI_PAGE_LOGIN); // 切回登录界面
        }
        return super.onKeyDown(keyCode, event);
    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////// Internal Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    void onBtnRegister() {

        String userId = getBinding().etUserId.getText().toString();
        if (TextUtils.isEmpty(userId)) {
            popupMessage("Please input valid userId!");
            return;
        }

        String appId = AppStorageUtil.safeGetString(this, Constant.APP_ID, null);
        if (TextUtils.isEmpty(appId)) {
            popupMessage("Fail to get appId!");
            return;
        }

        showLoadingView();

        ThirdAccountMgr.UserCreateParam createParam = new ThirdAccountMgr.UserCreateParam();
        createParam.mAppId = appId;
        createParam.mUserId = userId;
        createParam.mClientType = 2;
        ThirdAccountMgr.getInstance().userCreate(createParam, new ThirdAccountMgr.IUserCreateCallback() {
            @Override
            public void onThirdAccountUserCreateDone(ThirdAccountMgr.UserCreateParam createParam,
                                                     ThirdAccountMgr.UserCreateResult createResult) {
                Log.d(TAG, "<onBtnRegister.onThirdAccountUserCreateDone> errCode=" + createResult.mErrCode);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoadingView();
                        if (createResult.mErrCode != ErrCode.XOK) {
                            popupMessage("Fail to register account, errCode=" + createResult.mErrCode
                                    + ", errMessage=" + createResult.mMessage);
                            return;
                        }

                        popupMessageLongTime("Register account successful, nodeId=" + createResult.mNodeId
                                + ", region=" + createResult.mRegion);

                        AppStorageUtil.safePutString(mActivity, Constant.ACCOUNT, createResult.mNodeId);
                        finish();
                        PushApplication.getInstance().setUiPage(Constant.UI_PAGE_LOGIN); // 切回登录界面
                    }
                });
            }
        });
    }



}
