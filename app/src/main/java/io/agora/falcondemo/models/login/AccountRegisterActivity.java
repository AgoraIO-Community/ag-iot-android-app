package io.agora.falcondemo.models.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.iotlink.ErrCode;
import io.agora.falcondemo.base.BaseViewBindingActivity;

import io.agora.falcondemo.base.PushApplication;
import io.agora.falcondemo.common.Constant;
import io.agora.falcondemo.databinding.ActivityRegisterBinding;
import io.agora.falcondemo.thirdpartyaccount.ThirdAccountMgr;
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

        ThirdAccountMgr.RegisterParam registerParam = new ThirdAccountMgr.RegisterParam();
        registerParam.mMasterAppId = appId;
        registerParam.mUserId = userId;
        registerParam.mClientType = 2;
        ThirdAccountMgr.getInstance().register(registerParam, new ThirdAccountMgr.IRegisterCallback() {
            @Override
            public void onThirdAccountRegisterDone(int errCode, String errMsg, ThirdAccountMgr.RegisterParam registerParam, String retrievedNodeId, String region) {
                Log.d(TAG, "<onBtnRegister.onThirdAccountRegisterDone> errCode=" + errCode);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoadingView();
                        if (errCode != ErrCode.XOK) {
                            popupMessage("Fail to register account, errCode=" + errCode
                                    + ", errMessage=" + errMsg);
                            return;
                        }

                        popupMessageLongTime("Register account successful, nodeId=" + retrievedNodeId
                                + ", region=" + region);

                        AppStorageUtil.safePutString(mActivity, Constant.ACCOUNT, retrievedNodeId);
                        finish();
                    }
                });
            }
        });
    }



}
