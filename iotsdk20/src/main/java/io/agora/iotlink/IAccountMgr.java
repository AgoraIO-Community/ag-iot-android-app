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
    public static final int ACCOUNT_STATE_LOGOUTING = 0x0002;      ///< 正在登出用户账号
    public static final int ACCOUNT_STATE_USRINF_QUERYING = 0x0003;///< 正在查询用户信息
    public static final int ACCOUNT_STATE_USRINF_UPDATING = 0x0004;///< 正在更新用户信息
    public static final int ACCOUNT_STATE_UPLOADING_PORTRAIT = 0x0005;   ///< 正在上传用户头像
    public static final int ACCOUNT_STATE_RUNNING = 0x0006;        ///< 当前已经有一个账号登录



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


    /*
     * @brief 用户信息
     *        int/long 类型的数值为-1时，表示该值无效（未获取到 或者 未设置）
     *        例如：获取用户信息时，mSex=-1表示为获取到
     */
    public static class UserInfo {
        public long mId;                ///< 用户Id，必定有正常只
        public String mAccount;         ///< 账号
        public String mEmail;           ///< 邮箱
        public String mPhoneNumber;     ///< 手机号码

        public int mStatus = -1;        ///< 状态：1--启用； 2--禁用；
        public int mDeleted = -1;       ///< 0--未删除；  1--已删除
        public String mIdentityId;      ///< 身份Id
        public String mIdentityPoolId;  ///< 身份池Id
        public long mMerchantId = -1;   ///< 商户Id
        public String mMerchantName;    ///< 商户名字
        public long mCreateBy = -1;     ///< 创建人Id
        public long mCreateTime = -1;   ///< 创建时间戳
        public long mUpdateBy = -1;     ///< 最后更新人Id
        public long mUpdateTime = -1;   ///< 最后更新时间戳
        public String mParam;           ///< 上报参数

        //
        // 如下字段信息可以通过接口更新到服务器端
        //
        public String mName;        ///< 用户名称
        public String mAvatar;      ///< 用户头像路径，通过上传接口将头像图片上传后，服务器返回的路径
        public int mSex = -1;       ///< 性别： 1--男;  2--女
        public int mAge = -1;       ///< 年龄
        public long mBirthday = -1; ///< 生日
        public int mHeight = -1;    ///< 身高
        public int mWeight = -1;    ///< 体重
        public String mBackground;  ///< 背景图片

        public String mCountryId;   ///< 国家编号
        public String mCountry;     ///< 国家
        public String mProvinceId;  ///< 省份编号
        public String mProvince;    ///< 省份名称
        public String mCityId;      ///< 城市编号
        public String mCity;        ///< 城市名称
        public String mAreaId;      ///< 区县编号
        public String mArea;        ///< 区县名称
        public String mAddress;     ///< 详细地址

        @Override
        public String toString() {
            String infoText = "{ mId=" + mId + ", mAccount=" + mAccount
                    + ", mEmail=" + mEmail + ", mPhoneNumber=" + mPhoneNumber
                    + ", mStatus=" + mStatus + ", mDeleted=" + mDeleted
                    + ", mIdentityId=" + mIdentityId + ", mIdentityPoolId=" + mIdentityPoolId
                    + ", mMerchantId=" + mMerchantId + ", mMerchantName=" + mMerchantName
                    + ", mCreateBy=" + mCreateBy + ", mCreateTime=" + mCreateTime
                    + ", mUpdateBy=" + mUpdateBy + ", mUpdateTime=" + mUpdateTime
                    + ", mParam=" + mParam
                    + ", mName=" + mName + ", mAvatar=" + mAvatar
                    + ", mSex=" + mSex + ", mAge=" + mAge
                    + ", mBirthday=" + mBirthday + ", mBackground=" + mBackground
                    + ", mHeight=" + mHeight + ", mWeight=" + mWeight
                    + ", mCountryId=" + mCountryId + ", mCountry=" + mCountry
                    + ", mProvinceId=" + mProvinceId + ", mProvince=" + mProvince
                    + ", mCityId=" + mCityId + ", mCity=" + mCity
                    + ", mAreaId=" + mAreaId + ", mArea=" + mArea
                    + ", mAddress=" + mAddress + " }";
            return infoText;
        }
    }


    /*
     * @brief 账号管理回调接口
     */
    public static interface ICallback {
        /*
         * @brief 账号登录回调
         */
        default void onLoginDone(int errCode, String account) {}

        /*
         * @brief 账号登出回调
         */
        default void onLogoutDone(int errCode, String account) {}

        /*
         * @brief 账号在其他设备上登录
         */
        default void onLoginOtherDevice(String account) {}

        /*
         * @brief 获取用户信息回调
         */
        default void onQueryUserInfoDone(int errCode, UserInfo userInfo) {}

        /*
         * @brief 更新用户信息完成回调
         */
        default void onUpdateUserInfoDone(int errCode, UserInfo userInfo) {}

        /*
         * @brief 上传用户图像完成回调
         * @param errCode : 错误码
         * @param cloudFilePath : 上传后生成的图片云路径，可以用来填充 UserInfo.mAvator 字段
         */
        default void onUploadPortraitDone(int errCode, final byte[] fileContent,
                                          final String cloudFilePath) {}

        /*
         * @brief Token过期的回调，只能重新登录处理
         */
        default void onTokenInvalid() {}
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


    /**
     * @brief 通过第三方登录参数来进行登录，这种登录方式不需要进行注册
     * @param loginParam : 第三方登录参数
     * @return 返回错误码
     */
    int login(final LoginParam loginParam);

    /*
     * @brief 登出当前账号，触发 onLogoutDone() 回调
     */
    int logout();

    /*
     * @brief 获取当前用户信息，触发 onQueryUserInfoDone() 回调
     *
     */
    int queryUserInfo();

    /*
     * @brief 更新当前用户信息，触发 onUpdateUserInfoDone() 回调
     *
     */
    int updateUserInfo(final UserInfo newUserInfo);

    /*
     * @brief 上传图像图片到服务器，触发 onUploadPortraitDone() 回调
     * @param fileContent : 本地要上传的文件内容，只能是 .jpg，并且内容 < 512KB 长度
     * @return 错误码
     */
    int uploadPortrait(final byte[] fileContent);

    /*
     * @brief 获取当前已经登录的账号，如果未登录则返回null
     *
     */
    String getLoggedAccount();

    /*
     * @brief 获取用于QRCode二维码的用户Id
     *        如果当前处于未登录状态，则返回空字符串
     *
     */
    String getQRCodeUserId();

}
