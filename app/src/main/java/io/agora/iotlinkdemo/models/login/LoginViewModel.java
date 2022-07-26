package io.agora.iotlinkdemo.models.login;

import static io.agora.iotlink.ErrCode.XOK;

import android.util.Log;

import com.agora.baselibrary.base.BaseViewModel;
import com.agora.baselibrary.utils.SPUtil;
import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.event.ExitLoginEvent;
import io.agora.iotlinkdemo.utils.ErrorToastUtils;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAccountMgr;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

public class LoginViewModel extends BaseViewModel implements IAccountMgr.ICallback {
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
        if (accountMgr != null) {
            accountMgr.registerListener(this);
        }
    }

    public void onStop() {
        // 注册账号管理监听
        IAccountMgr accountMgr = AIotAppSdkFactory.getInstance().getAccountMgr();
        if (accountMgr != null) {
            accountMgr.unregisterListener(this);
        }
    }

    /**
     * 获取验证码
     */
    public void requestVCode(String account, String type) {
        int ret = AIotAppSdkFactory.getInstance().getAccountMgr().getCode(account, type);
        if (ret != XOK) {
            ToastUtils.INSTANCE.showToast("不能获取验证码, 错误码: " + ret);
            return;
        }
    }

    /**
     * 注册邮箱帐号 onRegisterDone
     *
     * @param account  账号
     * @param password 密码
     * @param code     验证码
     */
    public void registerMailAccount(String account, String password, String code) {
        AIotAppSdkFactory.getInstance().getAccountMgr().register(account, password, code, null, account);
    }

    /**
     * 注册手机号帐号 onRegisterDone
     *
     * @param account  账号
     * @param password 密码
     * @param code     验证码
     */
    public void registerPhoneAccount(String account, String password, String code) {
        AIotAppSdkFactory.getInstance().getAccountMgr().register(account, password, code, account, null);
    }

    /**
     * 登录 onLoginDone
     *
     * @param account  账号
     * @param password 密码
     */
    public void requestLogin(String account, String password) {
        int ret = AIotAppSdkFactory.getInstance().getAccountMgr().login(account, password);
        if (ret != XOK) {
            ErrorToastUtils.showLoginError(ret);
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_LOGIN_REQUEST_LOGIN_FAIL, null);
        }
    }

    /**
     * 退出登录
     */
    public void requestLogout() {
        SPUtil.Companion.getInstance(AgoraApplication.getInstance()).putString(Constant.ACCOUNT, null);
        try {
            AIotAppSdkFactory.getInstance().getAccountMgr().logout();
        } catch (Exception e) {
            Log.d("cwtsw", "getAccountMgr() == null");
        }
    }

    /*
     * @brief 注销一个用户账号，触发 onUnregisterDone() 回调
     */
    public void unregister() {
        int ret = AIotAppSdkFactory.getInstance().getAccountMgr().unregister();
        if (ret != XOK) {
            ErrorToastUtils.showLoginError(ret);
            ToastUtils.INSTANCE.showToast("要注销帐号失败 错误码：" + ret);
        }
    }

    @Override
    public void onUnregisterDone(int errCode, String account) {
        if (errCode == ErrCode.XOK) {
            SPUtil.Companion.getInstance(AgoraApplication.getInstance()).putString(Constant.ACCOUNT, null);
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_LOGOFF_SUCCESS, account);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_LOGOFF_FAIL, account);
            ToastUtils.INSTANCE.showToast("注销帐号失败 错误码：" + errCode);
        }
    }

    /**
     * 重置账号密码，触发 onPasswordResetDone() 回调
     *
     * @param accounts :  账号
     * @param pws      :  密码
     * @param code     : 新密码
     */
    public void passwordReset(String accounts, String pws, String code) {
        AIotAppSdkFactory.getInstance().getAccountMgr().passwordReset(accounts, pws, code);
    }

    @Override
    public void onPasswordResetDone(int errCode, String account) {
        if (errCode == ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_LOGIN_REQUEST_RESET_PWD_SUCCESS, account);
        } else {
            ErrorToastUtils.showLoginError(errCode);
        }
    }

    /**
     * 更改账号密码，触发 onPasswordChangeDone() 回调
     *
     * @param oldPassword : 旧密码
     * @param newPassword : 新密码
     */
    public int passwordChange(String oldPassword, String newPassword) {
        return AIotAppSdkFactory.getInstance().getAccountMgr().passwordChange(oldPassword, newPassword);
    }

    @Override
    public void onPasswordChangeDone(int errCode, String account) {
        if (errCode != ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_LOGIN_REQUEST_RESET_PWD_FAIL, null);
            ErrorToastUtils.showLoginError(errCode);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_LOGIN_REQUEST_RESET_PWD_SUCCESS, null);
            ToastUtils.INSTANCE.showToast("更换密码成功");
        }
    }

    @Override
    public void onGetCodeDone(int errCode, String account) {
        if (errCode == ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_LOGIN_REQUEST_V_CODE_SUCCESS, account);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_LOGIN_REQUEST_V_CODE_FAIL, account);
            ErrorToastUtils.showLoginError(errCode);
        }
    }

    @Override
    public void onRegisterDone(int errCode, String account) {
        if (errCode == ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_LOGIN_REQUEST_REGISTER_SUCCESS, account);
        } else {
            ErrorToastUtils.showLoginError(errCode);
        }
    }

    @Override
    public void onLoginDone(int errCode, String account) {
        if (errCode == ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_LOGIN_REQUEST_LOGIN_SUCCESS, account);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_LOGIN_REQUEST_LOGIN_FAIL, account);
            ErrorToastUtils.showLoginError(errCode);
        }
    }
}
