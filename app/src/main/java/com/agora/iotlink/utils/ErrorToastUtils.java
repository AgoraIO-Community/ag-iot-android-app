package com.agora.iotlink.utils;

import com.agora.baselibrary.utils.ToastUtils;
import com.agora.iotsdk20.ErrCode;

public class ErrorToastUtils {
    /**
     * 显示登录错误日志
     */
    public static void showLoginError(int errorCode) {
        switch (errorCode) {
            case ErrCode.XOK:
                break;
            case ErrCode.XERR_ACCOUNT_NOT_EXIST:
                showErrorText("账号不存在");
                break;
            case ErrCode.XERR_ACCOUNT_ALREADY_EXIST:
                showErrorText("账号已经存在");
                break;
            case ErrCode.XERR_ACCOUNT_PASSWORD_ERR:
                showErrorText("密码错误");
                break;
            case ErrCode.XERR_ACCOUNT_REGISTER:
                showErrorText("账号注册失败");
                break;
            case ErrCode.XERR_ACCOUNT_UNREGISTER:
                showErrorText("账号注销失败");
                break;
            case ErrCode.XERR_ACCOUNT_LOGIN:
                showErrorText("账号登录失败");
                break;
            case ErrCode.XERR_ACCOUNT_CHGPSWD:
                showErrorText("账号更换密码失败");
                break;
            case ErrCode.XERR_ACCOUNT_RESETPSWD:
                showErrorText("账号重置密码失败");
                break;
            case ErrCode.XERR_ACCOUNT_GETCODE:
                showErrorText("获取验证码失败");
                break;
            case ErrCode.XERR_ACCOUNT_USRINFO_QUERY:
                showErrorText("查询用户信息失败");
                break;
            case ErrCode.XERR_ACCOUNT_USRINFO_UPDATE:
                showErrorText("更新用户信息失败");
                break;
            case ErrCode.XERR_BAD_STATE:
                showErrorText("当前状态不正确，不能操作");
                break;
            case ErrCode.XERR_HTTP_RESP_CODE:
                showErrorText("HTTP回应错误");
                break;
            default:
                showErrorText("操作失败 错误码：" + errorCode);
                break;
        }
    }

    /**
     * 显示呼叫错误日志
     */
    public static void showCallError(int errorCode) {
        switch (errorCode) {
            case ErrCode.XOK:
                break;
            case ErrCode.XERR_CALLKIT_TIMEOUT:
                showErrorText("呼叫超时无响应");
                break;
            case ErrCode.XERR_CALLKIT_DIAL:
                showErrorText("呼叫拨号失败");
                break;
            case ErrCode.XERR_CALLKIT_HANGUP:
                showErrorText("呼叫挂断失败");
                break;
            case ErrCode.XERR_CALLKIT_ANSWER:
                showErrorText("呼叫接听失败");
                break;
            case ErrCode.XERR_CALLKIT_REJECT:
                showErrorText("呼叫拒绝失败");
                break;
            case ErrCode.XERR_CALLKIT_PEER_BUSY:
                showErrorText("对端忙");
                break;
            case ErrCode.XERR_CALLKIT_PEERTIMEOUT:
                showErrorText("对端超时无响应");
                break;
            case ErrCode.XERR_CALLKIT_LOCAL_BUSY:
                showErrorText("本地端忙");
                break;
            case ErrCode.XERR_CALLKIT_ERR_OPT:
                showErrorText("不支持的错误操作");
                break;
            case ErrCode.XERR_CALLKIT_PEER_UNREG:
                showErrorText("对端未注册");
                break;
            default:
                showErrorText("操作失败 错误码：" + errorCode);
                break;
        }
    }

    private static void showErrorText(String text) {
        ToastUtils.INSTANCE.showToast(text);
    }
}
