/**
 * @file IAccountMgr.java
 * @brief This file define the interface of account management
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink;


/*
 * @brief 账号管理接口
 */
public interface IAccountMgr  {

    //
    // 账号管理的状态机
    //
    public static final int ACCOUNT_STATE_IDLE = 0x0000;           ///< 当前未登录
    public static final int ACCOUNT_STATE_LOGINING = 0x0001;       ///< 正在登录用户账号
    public static final int ACCOUNT_STATE_RUNNING = 0x0002;        ///< 当前已经有一个账号登录
    public static final int ACCOUNT_STATE_LOGOUTING = 0x0003;      ///< 正在登出用户账号


    //
    // MQTT 的状态
    //
    public static final int MQTT_STATE_DISCONNECTED = 0x0000;        ///< MQTT 没有联接
    public static final int MQTT_STATE_CONNECTING = 0x0001;          ///< MQTT 正在联接中
    public static final int MQTT_STATE_CONNECTED = 0x0002;           ///< MQTT 已经联接



    /**
     * @brief 账号登录信息
     */
    public static class LoginParam {
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

        public String mLsAccessToken;             ///< 认证令牌
        public String mLsTokenType;               ///< 认证令牌类型
        public String mLsRefreshToken;            ///< 刷新认证令牌
        public long mLsExpiresIn;                 ///< 认证令牌过期时间
        public String mLsScope;                   ///< 令牌作用域


        @Override
        public String toString() {
            String infoText = "{ mAccount=" + mAccount + ", mPoolIdentifier=" + mPoolIdentifier
                    + ", mPoolIdentityId=" + mPoolIdentityId + ", mPoolToken=" + mPoolToken
                    + ", mIdentityPoolId=" + mIdentityPoolId
                    + ", mProofAccessKeyId=" + mProofAccessKeyId
                    + ", mProofSecretKey=" + mProofSecretKey
                    + ", mProofSessionToken=" + mProofSessionToken
                    + ", mInventDeviceName=" + mInventDeviceName
                    + ", mLsAccessToken=" + mLsAccessToken
                    + ", mLsTokenType=" + mLsTokenType
                    + ", mLsRefreshToken=" + mLsRefreshToken
                    + ", mLsExpiresIn=" + mLsExpiresIn
                    + ", mLsScope=" + mLsScope + " }";
            return infoText;
        }
    }


    /**
     * @brief 账号管理回调接口
     */
    public static interface ICallback {
        /**
         * @brief 账号登录回调
         */
        default void onLoginDone(int errCode, String account) {}

        /**
         * @brief Mqtt 状态回调
         */
        default void onMqttStateChanged(int mqttState) {}

        /**
         * @brief Mqtt 产生错误回调，此时只能进行登出，然后重新登录操作
         */
        default void onMqttError(final String errMessage) {}

        /**
         * @brief 账号登出回调
         */
        default void onLogoutDone(int errCode, String account) {}

        /**
         * @brief 账号在其他设备上登录
         */
        default void onLoginOtherDevice(String account) {}

         /**
         * @brief Token过期的回调，只能重新登录处理
         */
        default void onTokenInvalid() {}

        /**
         * @brief 设置公钥完成回调
         * @param errCode : 设置公钥结果，0表示设置成功
         */
        default void onSetPublicKeyDone(int errCode, final String lsAccessToken,
                                        final String inventDeviceName, final String publicKey) {}
    }




    ////////////////////////////////////////////////////////////////////////
    //////////////////////////// Public Methods ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 获取当前账号管理状态机
     * @return 返回状态机
     */
    int getStateMachine();

    /**
     * @brief 注册回调接口
     * @param callback : 回调接口
     * @return 错误码
     */
    int registerListener(IAccountMgr.ICallback callback);

    /**
     * @brief 注销回调接口
     * @param callback : 回调接口
     * @return 错误码
     */
    int unregisterListener(IAccountMgr.ICallback callback);

    /*
     * @brief 生成密钥对
     *
     */
    void generateRsaKeyPair();

    /*
     * @brief 获取生成的公钥，至少要调用一次 generateRsaKeyPair() 之后才会有
     * @param
     */
    byte[] getRsaPublickKey();


    /**
     * @brief 通过第三方登录参数来进行登录，这种登录方式不需要进行注册
     * @param loginParam : 第三方登录参数
     * @return 返回错误码
     */
    int login(final LoginParam loginParam);

    /**
     * @brief 登出当前账号，触发 onLogoutDone() 回调
     */
    int logout();

    /**
     * @brief 获取当前已经登录的账号，如果未登录则返回null
     *
     */
    String getLoggedAccount();

    /**
     * @brief 获取当前MQTT状态
     *
     */
    int getMqttState();

    /**
     * @brief 获取用于QRCode二维码的用户Id
     *        如果当前处于未登录状态，则返回空字符串
     *
     */
    String getQRCodeUserId();

    /**
     * @brief 上传公钥到服务器，这个接口可以在任意时刻调用 （即使没有调用 login()之前也可以调用）
     * @param lsAccessToken : 同 LoginParam.mLsAccessToken
     * @param inventDeviceName : 同 LoginParam.mInventDeviceName
     * @param publickKey : 要设置的公钥
     */
    int setPublicKey(final String lsAccessToken, final String inventDeviceName, final String publickKey);

}
