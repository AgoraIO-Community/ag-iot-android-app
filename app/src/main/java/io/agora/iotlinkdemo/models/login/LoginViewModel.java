package io.agora.iotlinkdemo.models.login;

import static io.agora.iotlink.ErrCode.XOK;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.agora.baselibrary.base.BaseViewModel;
import com.agora.baselibrary.utils.SPUtil;
import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.event.ExitLoginEvent;
import io.agora.iotlinkdemo.thirdpartyaccount.ThirdAccountMgr;
import io.agora.iotlinkdemo.utils.AppStorageUtil;
import io.agora.iotlinkdemo.utils.ErrorToastUtils;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAccountMgr;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

public class LoginViewModel extends BaseViewModel implements IAccountMgr.ICallback {
    private final String TAG = "IOTLINK/LoginViewModel";

    public static class ErrInfo {
        public int mErrCode = ErrCode.XOK;
        public String mErrTips;
    }

    /**
     * @brief 请求手机验证码结果
     */
    public static class ReqVCodeResult {
        public int mErrCode = ErrCode.XOK;
        public String mErrTips;
        public String mPhoneNumber;
        public boolean mIsResetPswd;
    }


    private volatile boolean mUnregistering = false;    ///< 是否正在注销
    private volatile boolean mIsListening = false;      ///< 是否已经注册回调监听,确保只注册一次回调监听

    public LoginViewModel() {
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onCleared() {
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(@Nullable ExitLoginEvent event) {
        if (getISingleCallback() != null) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_EXIT_STEP, null);
        }
    }

    public void onStart() {
        // 注册账号管理监听
        IAccountMgr accountMgr = AIotAppSdkFactory.getInstance().getAccountMgr();
        if ((accountMgr != null) && (!mIsListening)) {
            accountMgr.registerListener(this);
            mIsListening = true;
        }
    }

    public void onStop() {
        // 注册账号管理监听
        IAccountMgr accountMgr = AIotAppSdkFactory.getInstance().getAccountMgr();
        if ((accountMgr != null) && (mIsListening)) {
            accountMgr.unregisterListener(this);
            mIsListening = false;
        }
    }


    /**
     * @param accountName : 注册账号名
     * @param password    : 注册密码
     * @brief 第三方账号注册
     */
    public void accountRegister(final String accountName, final String password,
                                final String verifyCode) {
        ThirdAccountMgr.getInstance().register(accountName, password, verifyCode, new ThirdAccountMgr.IRegisterCallback() {
            @Override
            public void onThirdAccountRegisterDone(int errCode, final String errMessage,
                                                   final String account, final String password) {
                ErrInfo errInfo = new ErrInfo();
                errInfo.mErrCode = errCode;
                errInfo.mErrTips = errMessage;
                getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_THIRD_REGISTER_DONE, errInfo);
            }
        });
    }

    /**
     * @param accountName : 注销账号名
     * @param password    : 注销密码
     * @brief 第三方账号注销
     */
    public void accountUnregister(String accountName, String password) {

        // 先要进行SDK的登出操作
        AppStorageUtil.safePutString(AgoraApplication.getInstance(), Constant.ACCOUNT, null);

        mUnregistering = true;
        int errCode = AIotAppSdkFactory.getInstance().getAccountMgr().logout();
        if (errCode != XOK) {
            Log.e(TAG, "<accountUnregister> fail to logout, errCode=" + errCode);
        }

        ThirdAccountMgr.getInstance().unregister(accountName, password, new ThirdAccountMgr.IUnregisterCallback() {
            @Override
            public void onThirdAccountUnregisterDone(int errCode, final String errMessage,
                                                     final String account, final String password) {
                ErrInfo errInfo = new ErrInfo();
                errInfo.mErrCode = errCode;
                errInfo.mErrTips = errMessage;
                getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_THIRD_UNREGISTER_DONE, errInfo);
            }
        });
    }

    /**
     * @param accountName 账号
     * @param password    密码
     * @brief 第三方账号登录
     */
    public void accountLogin(final String accountName, final String password) {
        // 生成 RSA密钥对
        AIotAppSdkFactory.getInstance().getAccountMgr().generateRsaKeyPair();
        byte[] publickKeyData = AIotAppSdkFactory.getInstance().getAccountMgr().getRsaPublickKey();
        String rsaPublicKey =  Base64.encodeToString(publickKeyData, Base64.NO_WRAP);
        ThirdAccountMgr.getInstance().login(accountName, password, rsaPublicKey, new ThirdAccountMgr.ILoginCallback() {
            @Override
            public void onThirdAccountLoginDone(int errCode, final String errMessage,
                                                final String account, final String password,
                                                final String rsaPublicKey,
                                                final IAccountMgr.LoginParam loginParam) {
                ErrInfo errInfo = new ErrInfo();
                errInfo.mErrCode = errCode;
                errInfo.mErrTips = errMessage;

                if (errCode != ErrCode.XOK) {  // 第三方账号登录失败
                    getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_THIRD_LOGIN_DONE, errInfo);
                    return;
                }

                // SDK登录操作
                int ret = AIotAppSdkFactory.getInstance().getAccountMgr().login(loginParam);
                if (ret != ErrCode.XOK) {
                    errInfo.mErrCode = errCode;
                    getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_THIRD_LOGIN_DONE, errInfo);
                    return;
                }
            }
        });
    }

    @Override
    public void onLoginDone(int errCode, String account) {
        Log.d(TAG, "<onLoginDone> errCode=" + errCode
                + ", account=" + account);

        if (account == null) {
            ErrInfo errInfo = new ErrInfo();
            errInfo.mErrCode = ErrCode.XERR_ACCOUNT_LOGIN;
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_THIRD_LOGIN_DONE, errInfo);
            return;
        }

        ErrInfo errInfo = new ErrInfo();
        errInfo.mErrCode = errCode;
        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_THIRD_LOGIN_DONE, errInfo);
    }


    /**
     * @brief 第三方账号登出
     */
    public void accountLogout() {
        AppStorageUtil.safePutString(AgoraApplication.getInstance(), Constant.ACCOUNT, null);

        mUnregistering = false;
        int errCode = AIotAppSdkFactory.getInstance().getAccountMgr().logout();
        if (errCode != ErrCode.XOK) {
            ErrInfo errInfo = new ErrInfo();
            errInfo.mErrCode = errCode;
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_THIRD_LOGOUT_DONE, errInfo);
        }
    }

    @Override
    public void onLogoutDone(int errCode, String account) {
        Log.d(TAG, "<onLogoutDone> errCode=" + errCode + ", account=" + account);
        if (mUnregistering) {   // 注销时的登出不做处理
            return;
        }
        ErrInfo errInfo = new ErrInfo();
        errInfo.mErrCode = errCode;
        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_THIRD_LOGOUT_DONE, errInfo);
    }


    /**
     * @brief 获取验证码
     */
    public void requestVCode(final String phoneNumber, boolean isForgetPswd) {
        ThirdAccountMgr.getInstance().requestPhoneVCode(phoneNumber, isForgetPswd, new ThirdAccountMgr.IReqVerifyCodeCallback() {
            @Override
            public void onThirdAccountReqVCodeDone( int errCode, boolean isResetPswd, final String errMessage,
                                                    final String phoneNumber) {
                ReqVCodeResult reqResult = new ReqVCodeResult();
                reqResult.mErrCode = errCode;
                reqResult.mErrTips = errMessage;
                reqResult.mPhoneNumber = phoneNumber;
                reqResult.mIsResetPswd = isResetPswd;
                getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_THIRD_REQVCODE_DONE, reqResult);
            }
        });
    }

    /**
     * @brief 第三方账号重置密码
     * @param accountName 账号
     * @param password    密码
     * @param verifyCode  验证码
     */
    public void accountResetPassword(final String accountName, final String password,
                                     final String verifyCode) {
        ThirdAccountMgr.getInstance().resetPassword(accountName, password, verifyCode, new ThirdAccountMgr.IResetPswdCallback() {
            @Override
            public void onThirdAccountResetPswdDone(int errCode, final String errMessage,
                                                   final String account, final String password) {
                ErrInfo errInfo = new ErrInfo();
                errInfo.mErrCode = errCode;
                errInfo.mErrTips = errMessage;
                getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_THIRD_RESETPSWD_DONE, errInfo);
            }
        });
    }

}