/**
 * @file AccountMgr.java
 * @brief This file implement the account management
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.sdkimpl;


import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAccountMgr;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.aws.AWSUtils;
import io.agora.iotlink.callkit.AgoraService;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.lowservice.AgoraLowService;
import io.agora.iotlink.utils.RSAUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*
 * @brief 账号管理器
 */
public class AccountMgr implements IAccountMgr {

    /*
     * @brief 登录成功后的账号信息
     */
    public static class AccountInfo {
        public String mAccount;                 ///< 账号名称
        public String mEndpoint;                ///< iot 平台节点
        public String mRegion;                  ///< 节点
        public String mPlatformToken;           ///< 平台凭证
        public int mExpiration;                 ///< mPlatformToken 过期时间
        public String mRefresh;                 ///< 平台刷新凭证密钥

        public String mPoolIdentifier;          ///< 用户身份
        public String mPoolIdentityId;          ///< 用户身份Id
        public String mPoolToken;               ///< 用户身份凭证
        public String mIdentityPoolId;          ///< 用户身份池标识

        public String mProofAccessKeyId;        ///< IOT 临时账号凭证
        public String mProofSecretKey;          ///< IOT 临时密钥
        public String mProofSessionToken;       ///< IOT 临时Token
        public long mProofSessionExpiration;    ///< 过期时间(时间戳)

        public String mInventDeviceName;        ///< 虚拟设备thing name

        public String mAgoraScope;
        public String mAgoraTokenType;
        public String mAgoraAccessToken;
        public String mAgoraRefreshToken;
        public long mAgoraExpriesIn;

        @Override
        public String toString() {
            String infoText = "{ mAccount=" + mAccount + ", mPoolIdentifier=" + mPoolIdentifier
                    + ", mPoolIdentityId=" + mPoolIdentityId + ", mPoolToken=" + mPoolToken
                    + ", mIdentityPoolId=" + mIdentityPoolId
                    + ", mProofAccessKeyId=" + mProofAccessKeyId
                    + ", mProofSecretKey=" + mProofSecretKey
                    + ", mProofSessionToken=" + mProofSessionToken
                    + ", mInventDeviceName=" + mInventDeviceName
                    + ", mAgoraScope=" + mAgoraScope
                    + ", mAgoraTokenType=" + mAgoraTokenType
                    + ", mAgoraAccessToken=" + mAgoraAccessToken
                    + ", mAgoraRefreshToken=" + mAgoraRefreshToken
                    + ", mAgoraExpriesIn=" + mAgoraExpriesIn + " }";
            return infoText;
        }
    }

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/AccountMgr";


    //
    // The mesage Id
    //
    private static final int MSGID_ACCOUNT_BASE = 0x1000;
    private static final int MSGID_ACCOUNT_LOGIN = 0x1001;
    private static final int MSGID_AWSLOGIN_DONE = 0x1002;
    private static final int MSGID_ACCOUNT_LOGOUT = 0x1003;
    private static final int MSGID_ACCOUNT_TOKEN_INVALID = 0x1004;



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private ArrayList<IAccountMgr.ICallback> mCallbackList = new ArrayList<>();
    private AgoraIotAppSdk mSdkInstance;                        ///< 由外部输入的
    private Handler mWorkHandler;                               ///< 工作线程Handler，从SDK获取到

    private static final Object mDataLock = new Object();       ///< 同步访问锁,类中所有变量需要进行加锁处理
    private volatile int mStateMachine = ACCOUNT_STATE_IDLE;    ///< 当前账号系统状态机
    private AccountInfo mLocalAccount;                          ///< 当前已经登录账号, null表示未登录

    private byte[] mRsaPublicKey = null;                        ///< RSA的公钥
    private byte[] mRsaPrivateKey = null;                       ///< RSA的私钥

    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    int initialize(AgoraIotAppSdk sdkInstance) {
        mSdkInstance = sdkInstance;
        mWorkHandler = sdkInstance.getWorkHandler();
        mStateMachine = ACCOUNT_STATE_IDLE;

        return ErrCode.XOK;
    }

    void release() {
        workThreadClearMessage();

        synchronized (mCallbackList) {
            mCallbackList.clear();
        }
    }

    void workThreadProcessMessage(Message msg) {
        switch (msg.what) {
            case MSGID_ACCOUNT_LOGIN: {
                DoAccountLogin(msg);
            } break;

            case MSGID_ACCOUNT_LOGOUT: {
                DoAccountLogout(msg);
            } break;

           case MSGID_ACCOUNT_TOKEN_INVALID: {
                DoTokenInvalid(msg);
            } break;
        }
    }

    void workThreadClearMessage() {
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(MSGID_ACCOUNT_LOGIN);
            mWorkHandler.removeMessages(MSGID_AWSLOGIN_DONE);
            mWorkHandler.removeMessages(MSGID_ACCOUNT_LOGOUT);
            mWorkHandler.removeMessages(MSGID_ACCOUNT_TOKEN_INVALID);
            mWorkHandler = null;
        }
    }

    void sendMessage(int what, int arg1, int arg2, Object obj) {
        Message msg = new Message();
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = obj;
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(what);
            mWorkHandler.sendMessage(msg);
        }
    }

    void sendMessageDelay(int what, int arg1, int arg2, Object obj, long delayTime) {
        Message msg = new Message();
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = obj;
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(what);
            mWorkHandler.sendMessageDelayed(msg, delayTime);
        }
    }


    ///////////////////////////////////////////////////////////////////////
    /////////////////// Override Methods of IAccountMgr //////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public int getStateMachine() {
        synchronized (mDataLock) {
            return mStateMachine;
        }
    }


    @Override
    public int registerListener(IAccountMgr.ICallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.add(callback);
        }
        return ErrCode.XOK;
    }

    @Override
    public int unregisterListener(IAccountMgr.ICallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.remove(callback);
        }
        return ErrCode.XOK;
    }

    @Override
    public void generateRsaKeyPair() {
        Map<String, byte[]> keyPair = RSAUtils.generateKeyPair();
        mRsaPublicKey = keyPair.get("public");
        mRsaPrivateKey = keyPair.get("private");
    }

    @Override
    public byte[] getRsaPublickKey() {
        return mRsaPublicKey;
    }

    @Override
    public int login(final LoginParam loginParam) {
        if (getStateMachine() != ACCOUNT_STATE_IDLE) {
            ALog.getInstance().e(TAG, "<login> bad state, mStateMachine=" + mStateMachine);
            return ErrCode.XERR_BAD_STATE;
        }

        synchronized (mDataLock) {
            mStateMachine = ACCOUNT_STATE_LOGINING;  // 状态机切换到 正在登录中
            mLocalAccount = null;
        }
        mSdkInstance.setStateMachine(IAgoraIotAppSdk.SDK_STATE_LOGINING);

        sendMessage(MSGID_ACCOUNT_LOGIN, 0, 0, loginParam);
        ALog.getInstance().d(TAG, "<login> loginParam=" + loginParam.toString());
        return ErrCode.XOK;
    }

    @Override
    public int logout() {
        if (getStateMachine() != ACCOUNT_STATE_RUNNING) {
            ALog.getInstance().e(TAG, "<logout> bad state, mStateMachine=" + mStateMachine);
            return ErrCode.XERR_BAD_STATE;
        }

        CallkitMgr callkitMgr = (CallkitMgr)mSdkInstance.getCallkitMgr();
        if (callkitMgr != null) {
            int callkitState = callkitMgr.getStateMachine();
            if (callkitState != ICallkitMgr.CALLKIT_STATE_IDLE) {  // 当前通话正在进行中，不能登出
                ALog.getInstance().e(TAG, "<logout> bad state, callkit is ongoing, callkitState=" + callkitState);
                return ErrCode.XERR_CALLKIT_LOCAL_BUSY;
            }
        }


        synchronized (mDataLock) {
            mStateMachine = ACCOUNT_STATE_LOGOUTING;  // 状态机切换到 正在登出中
        }
        mSdkInstance.setStateMachine(IAgoraIotAppSdk.SDK_STATE_LOGOUTING);

        sendMessage(MSGID_ACCOUNT_LOGOUT, 0, 0, null);
        ALog.getInstance().d(TAG, "<logout> account=" + mLocalAccount.mAccount);
        return ErrCode.XOK;
    }

    @Override
    public String getLoggedAccount() {
        synchronized (mDataLock) {
            if (mLocalAccount == null) {
                return null;
            }
            return mLocalAccount.mAccount;
        }
    }

    @Override
    public int getMqttState() {
        return AWSUtils.getInstance().getAwsState();
    }


    @Override
    public String getQRCodeUserId() {
        String userId = "";
        synchronized (mDataLock) {
            if (mLocalAccount == null) {
                return "";
            }

            String identifier = mLocalAccount.mPoolIdentifier;
            String[] strArr = identifier.split("_");
            if (strArr.length > 0) {
                userId = strArr[strArr.length - 1];
            }
            return userId;
        }
    }


    public AccountInfo getAccountInfo() {
        synchronized (mDataLock) {
            return mLocalAccount;
        }
    }

    /**
     * @brief 获取生成的私钥
     * @return 返回私钥
     */
    byte[] getRsaPrivateKey() {
        return mRsaPrivateKey;
    }



    ///////////////////////////////////////////////////////////////////////
    ///////////////////// Methods for Account Login //////////////////////
    ///////////////////////////////////////////////////////////////////////
    /**
     * @brief 工作线程中进行,使用第三方的信息进行登录操作
     */
    void DoAccountLogin(Message msg) {
        LoginParam loginParam = (LoginParam)(msg.obj);
        IAgoraIotAppSdk.InitParam initParam = mSdkInstance.getInitParam();

        synchronized (mDataLock) {
            // 设置当前已经登录账号信息
            mLocalAccount = new AccountInfo();
            mLocalAccount.mAccount = loginParam.mAccount;
            mLocalAccount.mEndpoint = loginParam.mEndpoint;
            mLocalAccount.mRegion = loginParam.mRegion;
            mLocalAccount.mPlatformToken = loginParam.mPlatformToken;
            mLocalAccount.mExpiration = loginParam.mExpiration;
            mLocalAccount.mRefresh = loginParam.mRefresh;
            mLocalAccount.mPoolIdentifier = loginParam.mPoolIdentifier;
            mLocalAccount.mPoolIdentityId = loginParam.mPoolIdentityId;
            mLocalAccount.mPoolToken = loginParam.mPoolToken;
            mLocalAccount.mIdentityPoolId = loginParam.mIdentityPoolId;
            mLocalAccount.mProofAccessKeyId = loginParam.mProofAccessKeyId;
            mLocalAccount.mProofSecretKey = loginParam.mProofSecretKey;
            mLocalAccount.mProofSessionToken = loginParam.mProofSessionToken;
            mLocalAccount.mProofSessionExpiration = loginParam.mProofSessionExpiration;
            mLocalAccount.mInventDeviceName = loginParam.mInventDeviceName;

            // 赋值Agora账号相关信息
            mLocalAccount.mAgoraScope = loginParam.mLsScope;
            mLocalAccount.mAgoraTokenType = loginParam.mLsTokenType;
            mLocalAccount.mAgoraAccessToken = loginParam.mLsAccessToken;
            mLocalAccount.mAgoraRefreshToken = loginParam.mLsRefreshToken;
            mLocalAccount.mAgoraExpriesIn = loginParam.mLsExpiresIn;

            mStateMachine = ACCOUNT_STATE_RUNNING;  // 状态机切换到 登录成 状态
        }
        mSdkInstance.setStateMachine(IAgoraIotAppSdk.SDK_STATE_RUNNING);
        ALog.getInstance().d(TAG, "<DoAccountLogin> done, successful");
        CallbackLogInDone(ErrCode.XOK, mLocalAccount.mAccount);

        //
        // 初始化 AWS 联接
        //
        String aws_account = mLocalAccount.mAccount;
        String aws_endpoint = mLocalAccount.mEndpoint;
        String aws_identityId = mLocalAccount.mPoolIdentityId;
        String aws_token = mLocalAccount.mPoolToken;
        String aws_accountId = mLocalAccount.mPoolIdentifier;
        String aws_identityPoolId = mLocalAccount.mIdentityPoolId;
        String aws_region = mLocalAccount.mRegion;
        String aws_inventDeviceName = mLocalAccount.mInventDeviceName;
        AWSUtils.getInstance().initIoTClient(initParam.mContext,
                aws_identityId, aws_endpoint, aws_token, aws_accountId,
                aws_identityPoolId, aws_region, aws_inventDeviceName);

        ALog.getInstance().d(TAG, "<DoAccountLogin> initAWSIotClient"
                + ", aws_account=" + aws_account
                + ", aws_identityId=" + aws_identityId
                + ", aws_endpoint=" + aws_endpoint
                + ", aws_token=" + aws_token
                + ", aws_accountId=" + aws_accountId
                + ", aws_identityPoolId=" + aws_identityPoolId
                + ", aws_region=" + aws_region);
    }

    void CallbackLogInDone(int errCode, String account) {
        synchronized (mCallbackList) {
            for (IAccountMgr.ICallback listener : mCallbackList) {
                listener.onLoginDone(errCode, account);
            }
        }
    }

    /*
     * @brief 在AWS回调中被调用，用于处理AWS登录状态
     */
    void onAwsConnectStatusChange(String status) {
        ALog.getInstance().d(TAG, "<onAwsConnectStatusChange> status=" + status
                + ", mStateMachine=" + mStateMachine);

        if (status.compareToIgnoreCase("Connecting") == 0) {
            CallbackAwsStateChaned(MQTT_STATE_CONNECTING);

        } else if (status.compareToIgnoreCase("Connected") == 0) {
            CallbackAwsStateChaned(MQTT_STATE_CONNECTED);

        } else if (status.compareToIgnoreCase("Subscribed") == 0) {

        } else if (status.compareToIgnoreCase("ConnectionLost") == 0) {
            CallbackAwsStateChaned(MQTT_STATE_DISCONNECTED);

        }
    }

    void CallbackAwsStateChaned(int mqttState) {
        synchronized (mCallbackList) {
            for (IAccountMgr.ICallback listener : mCallbackList) {
                listener.onMqttStateChanged(mqttState);
            }
        }
    }

    /**
     * @brief 在AWS回调中被调用，用于处理AWS联接错误
     */
    void onAwsConnectFail(final String errMessage) {
        ALog.getInstance().d(TAG, "<onAwsConnectFail> errMessage=" + errMessage);
        CallbackAwsError(errMessage);
    }

    void CallbackAwsError(final String errMessage) {
        synchronized (mCallbackList) {
            for (IAccountMgr.ICallback listener : mCallbackList) {
                listener.onMqttError(errMessage);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////////// Methods for Account Logout //////////////////////
    ///////////////////////////////////////////////////////////////////////
    /*
     * @brief 工作线程中进行实际的登出操作，需要等登出结果消息回来
     */
    void DoAccountLogout(Message msg)
    {
        // 断开 AWS 联接
        AWSUtils.getInstance().disConnect();
        String account;
        synchronized (mDataLock) {
            account = mLocalAccount.mAccount;
            mLocalAccount = null;               // 清空本地账号
            mStateMachine = ACCOUNT_STATE_IDLE;    // 状态机切换到 未登录 状态
        }
        mSdkInstance.setStateMachine(IAgoraIotAppSdk.SDK_STATE_READY);
        ALog.getInstance().d(TAG, "<DoAccountLogout> finished with successful");
        CallbackLogoutDone(ErrCode.XOK, account);
    }

    void CallbackLogoutDone(int errCode, String account) {
        synchronized (mCallbackList) {
            for (IAccountMgr.ICallback listener : mCallbackList) {
                listener.onLogoutDone(errCode, account);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////
    ///////////////////// Methods for Account Token Invalid ////////////////
    ///////////////////////////////////////////////////////////////////////
    /*
     * @brief Token过期的回调处理
     */
    void onTokenInvalid() {
        ALog.getInstance().e(TAG, "<onTokenInvalid> ");

        // 在底层服务的HTTP请求收到Toke过期错误后，延时一些进行回调，方便应用层处理
        sendMessageDelay(MSGID_ACCOUNT_TOKEN_INVALID, 0, 0, null, 500);
    }

    void DoTokenInvalid(Message msg) {
        // 断开 AWS 联接
        AWSUtils.getInstance().disConnect();
        String account;
        synchronized (mDataLock) {
            account = mLocalAccount.mAccount;
            mLocalAccount = null;               // 清空本地账号
            mStateMachine = ACCOUNT_STATE_IDLE;    // 状态机切换到 未登录 状态
        }
        mSdkInstance.setStateMachine(IAgoraIotAppSdk.SDK_STATE_READY);
        ALog.getInstance().d(TAG, "<DoTokenInvalid> finished with successful");

        synchronized (mCallbackList) {  // 回调给应用层
            for (IAccountMgr.ICallback listener : mCallbackList) {
                listener.onTokenInvalid();
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////////// Methods for Account Be Preempted ////////////////
    ///////////////////////////////////////////////////////////////////////
    /*
     * @brief 工作线程中处理账号异地登录事件，本地被强制登出结果
     */
    void DoAccountLoginOtherDev(Message msg)
    {
//        synchronized (mDataLock) {
//            mLocalAccount = null;               // 清空本地账号
//            mStateMachine = ACCOUNT_STATE_IDLE;    // 状态机切换到 未登录 状态
//        }
//        ALog.getInstance().d(TAG, "<DoAccountLogoutDone> finished with successful");
//        CallbackLogoutDone(ErrCode.XOK, account);
    }




    ///////////////////////////////////////////////////////////////////////
    ///////////////////// Inner Data Structure and Methods ////////////////
    ///////////////////////////////////////////////////////////////////////

}
