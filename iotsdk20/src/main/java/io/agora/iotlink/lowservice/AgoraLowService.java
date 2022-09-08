package io.agora.iotlink.lowservice;


import android.os.Build;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAccountMgr;
import io.agora.iotlink.IDeviceMgr;
import io.agora.iotlink.IotDevice;
import io.agora.iotlink.IotOutSharer;
import io.agora.iotlink.IotShareMessage;
import io.agora.iotlink.IotShareMsgPage;
import io.agora.iotlink.logger.ALog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AgoraLowService {

    //
    // response err code
    //
    private static final int RESP_OK = 0;                   ///< 没有错误
    private static final int RESP_KNOWN = 1;                ///< 未知错误
    private static final int RESP_PARAM_INVALID = 1001;     ///< 参数错误
    private static final int RESP_SYS_EXCEPTION = 1016;     ///< 系统异常
    private static final int RESP_TOKEN_INVALID = 1010;     ///< Token过期或者失效
    private static final int RESP_NOT_ALLOW_OPT = 1033;     ///< 不允许的操作
    private static final int RESP_VERYCODE_ERR = 1040;      ///< 验证码错误
    private static final int RESP_UPGRADE_INVALID = 2324;   ///< 设备升级信息不存在或无效
    private static final int RESP_DEV_NOT_EXIST = 12011;    ///< 设备找不到
    private static final int RESP_USER_ALREADY_EXIST = 10001;   ///< 账户已经存在
    private static final int RESP_USER_NOT_EXIST = 10002;       ///< 账户不存在
    private static final int RESP_PASSWORD_ERR = 10003;         ///< 密码错误
    private static final int RESP_DEV_ALREADY_SHARED = 12013;   ///< 设备已经共享到同一个账号了




    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief HTTP请求后，服务器回应数据
     */
    private static class ResponseObj {
        public int mErrorCode;                ///< 错误码
        public int mRespCode;               ///< 回应数据包中HTTP代码
        public String mTip;                 ///< 回应数据
        public JSONObject mRespJsonObj;     ///< 回应包中的JSON对象
    }

    /*
     * @brief 登录成功后的账号信息
     */
    public static class AccountInfo {
        public String mAccount;                 ///< 账号
        public String mEndpoint;                ///< iot 平台节点
        public String mRegion;                  ///< 节点
        public String mPlatformToken;           ///< 平台凭证
        public int mExpiration;                 ///< 过期时间
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

    }


    /*
     * @brief 内部设备信息
     */
    public static class DevInfo {
        public String mAppUserId;           ///< 用户Id
        public String mUserType;             ///< 用户角色：1--所有者; 2--管理员; 3--成员

        public String mProductId;           ///< 产品Id
        public String mProductKey;        ///< 产品名
        public String mDeviceId;            ///< 设备唯一的Id
        public String mDeviceName;        ///< 设备名
        public String mDeviceMac;         ///< 设备MAC地址

        public long mCreateTime = -1;     ///< 创建时间戳，-1表示未设置
        public long mUpdateTime = -1;     ///< 最后一次更新时间戳，-1表示未设置

        public boolean mConnected = false; ///< 是否在线

        public String mSharer;            ///< 分享人的用户Id，如果自己配网则是 0
        public int mShareCount = -1;      ///< 当前分享个数，-1表示未设置
        public int mShareType = -1;       ///< 共享类型，-1表示未设置
    }



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "AgoraLowService";
    private static final int HTTP_TIMEOUT = 8000;


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static AgoraLowService mInstance = null;

    ///< 服务器请求站点
    private String mServerBaseUrl = "https://un2nfllop5.execute-api.cn-north-1.amazonaws.com.cn/Prod";
    private String mAppShadowProductKey = "EJImmKSK6m54R5l";  ///< APP影子虚拟设备


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public static AgoraLowService getInstance() {
        if(mInstance == null) {
            synchronized (AgoraLowService.class) {
                if(mInstance == null) {
                    mInstance = new AgoraLowService();
                }
            }
        }
        return mInstance;
    }

    public void setBaseUrl(final String baseUrl) {
        mServerBaseUrl = baseUrl;
        ALog.getInstance().e(TAG, "<setBaseUrl> mServerBaseUrl=" + mServerBaseUrl);
    }

    /*
     * @brief 获取邮箱注册验证码
     * @param clientId : 品牌标识
     * @param account : 要注册的邮箱账号
     * @param type : 请求类型（【REGISTER】注册、【LOGIN】登录、【PWE_RESET】重置密码）
     * @return 错误码，0表示成功
     */
    public int getEmailCode(final long clientId, final String account, final String type) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/email/code/get";

        // 请求参数
        try {
            body.put("merchantId", Long.toString(clientId));
            body.put("email", account);
            body.put("type", type);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<getEmailCode> failure set JSON object!");
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST", null, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<getEmailCode> failure with no response!");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<getEmailCode> failure, mErrorCode=" + responseObj.mErrorCode);
            return ErrCode.XERR_ACCOUNT_GETCODE;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<getEmailCode> failure, mRespCode="
                    + responseObj.mRespCode);
            return mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_ACCOUNT_GETCODE);
        }

        ALog.getInstance().d(TAG, "<getEmailCode> successful"
                + ", email=" + account + ", type=" + type);
        return ErrCode.XOK;
    }

    /*
     * @brief 获取手机验证码
     * @param clientId : 品牌标识
     * @param phoneNumber : 手机号码
     * @param type : 请求类型（【REGISTER】注册、【LOGIN】登录、【PWE_RESET】重置密码）
     * @return 错误码，0表示成功
     */
    public int getPhoneCode(final long clientId, final String phoneNumber, final String type) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        if ((phoneNumber == null) || (phoneNumber.length() <= 4)) {
            ALog.getInstance().e(TAG, "<getPhoneCode> failure, phoneNumber=" + phoneNumber);
            return ErrCode.XERR_INVALID_PARAM;
        }

        // 请求URL
        String requestUrl = mServerBaseUrl + "/sms/code/get";

        // 请求参数
        try {
            body.put("merchantId", Long.toString(clientId));
            String phone = phoneNumber;
            String phoneHead = phoneNumber.substring(0, 3);
            if (phoneHead.compareToIgnoreCase("+86") != 0) {
                phone = "+86" + phoneNumber;
            }
            body.put("phone", phone);
            body.put("type", type);

        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<getPhoneCode> failure set JSON object!");
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST", null, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<getPhoneCode> failure with no response!");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<getPhoneCode> failure, mErrorCode=" + responseObj.mErrorCode);
            return ErrCode.XERR_ACCOUNT_GETCODE;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<getPhoneCode> failure, mRespCode="
                    + responseObj.mRespCode);
            return mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_ACCOUNT_GETCODE);
        }

        ALog.getInstance().d(TAG, "<getPhoneCode> successful"
                + ", phoneNumber=" + phoneNumber + ", type=" + type);
        return ErrCode.XOK;
    }


    /*
     * @brief 注册用户账号
     * @param clientId : 品牌标识
     * @param account : 要注册的账号
     * @param password : 账号密码
     * @param code : 账号注册验证码
     * @param phoneNumber: 手机号码，必须以+86开头
     * @param eMail: 邮箱，与手机号码 二选一
     * @return 错误码，0表示成功
     */
    public int accountRegister(long clientId, final String account, final String password,
                               final String code,
                               final String phoneNumber, final String eMail) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/register";

        // 请求参数
        try {
            body.put("merchantId", Long.toString(clientId));
            body.put("account", account);
            body.put("password", password);
            if (!code.isEmpty()) {
                body.put("code", code);
            }

            // 手机号码 与 邮箱 只能二选一，并且手机号码必须加"+86"开头
            if ((phoneNumber != null) && (phoneNumber.length() > 4)) {
                String phone = phoneNumber;
                String phoneHead = phoneNumber.substring(0, 3);
                if (phoneHead.compareToIgnoreCase("+86") != 0) {
                    phone = "+86" + phoneNumber;
                }
                body.put("mobilephone", phone);

            } else if ((eMail != null) && (eMail.length() > 2)) {
                body.put("email", eMail);
            }
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<accountRegister> failure set JSON object!");
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST", null, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<accountRegister> failure with no response!");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountRegister> failure, mErrorCode=" + responseObj.mErrorCode);
            return ErrCode.XERR_ACCOUNT_REGISTER;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountRegister> failure, mRespCode="
                    + responseObj.mRespCode);
            if (responseObj.mRespCode == 1) {  // 该账号已经注册
                return ErrCode.XERR_ACCOUNT_ALREADY_EXIST;
            }
            return mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_ACCOUNT_REGISTER);
        }

        ALog.getInstance().d(TAG, "<accountRegister> successful"
                + ", account=" + account + ", password=" + password);
        return ErrCode.XOK;
    }

    /*
     * @brief 注销用户账号
     * @return 错误码，0表示成功
     */
    public int accountUnregister(final String srvToken) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/cancel";

        // 请求参数

        // 发送HTTP请求
        ResponseObj responseObj = requestToServer(requestUrl, "POST", srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<accountUnregister> failure with no response!");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountUnregister> failure, mErrorCode="
                    + responseObj.mErrorCode);
            return ErrCode.XERR_ACCOUNT_UNREGISTER;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountUnregister> failure, mRespCode="
                    + responseObj.mRespCode);
            return mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_ACCOUNT_UNREGISTER);
        }

        ALog.getInstance().d(TAG, "<accountUnregister> successful");
        return ErrCode.XOK;
    }

    /*
     * @brief  根据账号和密码 登录用户账号
     * @param account : 要登录的账号
     * @param password : 要登录的账号密码
     * @return 账号信息，如果返回null表示登录失败
     */
    public static class LoginResult {
        public int mErrCode = ErrCode.XOK;
        public AccountInfo mAccountInfo;
    }
    public LoginResult accountLogin(final String account, final String password) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        LoginResult result = new LoginResult();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/login";

        // 请求参数
        try {
            body.put("account", account);
            body.put("password", password);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<accountLogin> failure set JSON object!");
            result.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return result;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST", null, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<accountLogin> failure with no response!");
            result.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return result;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountLogin> failure!");
            result.mErrCode = ErrCode.XERR_ACCOUNT_LOGIN;
            return result;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountLogin> failure, mRespCode="
                    + responseObj.mRespCode);
            result.mErrCode = mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_ACCOUNT_LOGIN);
            return result;
        }
        if (responseObj.mRespJsonObj == null) {
            ALog.getInstance().e(TAG, "<accountLogin> NO response object!");
            result.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return result;
        }

        // 解析账号信息
        result.mAccountInfo = new AccountInfo();
        try {
            JSONObject infoObj = responseObj.mRespJsonObj.getJSONObject("info");
            result.mAccountInfo.mAccount = infoObj.getString("account");
            result.mAccountInfo.mEndpoint = infoObj.getString("endpoint");
            result.mAccountInfo.mRegion = infoObj.getString("region");
            result.mAccountInfo.mExpiration = infoObj.getInt("expiration");
            result.mAccountInfo.mPlatformToken= infoObj.getString("granwin_token");

            JSONObject poolObj = infoObj.getJSONObject("pool");
            result.mAccountInfo.mPoolIdentifier = poolObj.getString("identifier");
            result.mAccountInfo.mPoolIdentityId = poolObj.getString("identityId");
            result.mAccountInfo.mIdentityPoolId = poolObj.getString("identityPoolId");
            result.mAccountInfo.mPoolToken = poolObj.getString("token");

            JSONObject proofObj = infoObj.getJSONObject("proof");
            result.mAccountInfo.mProofAccessKeyId = proofObj.getString("accessKeyId");
            result.mAccountInfo.mProofSecretKey = proofObj.getString("secretKey");
            result.mAccountInfo.mProofSessionToken = proofObj.getString("sessionToken");
            result.mAccountInfo.mProofSessionExpiration = proofObj.getLong("sessionExpiration");

            // 拼接user映射的虚拟设备thing name
            result.mAccountInfo.mInventDeviceName = queryInventDeviceName(result.mAccountInfo.mPlatformToken);

            result.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<accountLogin> [JSONException], error=" + e);
            result.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return result;
        }

        ALog.getInstance().d(TAG, "<accountLogin> successful"
                + ", account=" + account + ", password=" + password);
        return result;
    }

    /*
     * @brief  更改用户账号的密码
     * @param account : 当前登录的账号
     * @param newPassword : 要重置的密码
     * @param code : 验证码
     * @return 错误代码，0表示成功
     */
    public int accountResetPassword(final String account, final String srvToken,
                                    final String newPassword, final String code) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/password/reset";

        // 请求参数
        try {
            body.put("account", account);
            body.put("newPassword", newPassword);
            body.put("code", code);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<accountResetPassword> failure set JSON object!");
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                null, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<accountResetPassword> failure with no response!");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountResetPassword> failure"
                    + ", account=" + account
                    + ", newPassword=" + newPassword
                    + ", code=" + code
                    + ", mErrorCode=" + responseObj.mErrorCode);
            return ErrCode.XERR_ACCOUNT_RESETPSWD;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountChgPassword> failure"
                    + ", account=" + account
                    + ", newPassword=" + newPassword
                    + ", code=" + code
                    + ", mRespCode=" + responseObj.mRespCode);
            return mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_ACCOUNT_RESETPSWD);
        }

        ALog.getInstance().d(TAG, "<accountResetPassword> successful"
                + ", account=" + account
                + ", newPassword=" + newPassword
                + ", code=" + code);
        return ErrCode.XOK;
    }

    /*
     * @brief  更改用户账号的密码
     * @param account : 要登录的账号
     * @param password : 要登录的账号密码
     * @return 错误代码，0表示成功
     */
    public int accountChgPassword(final String srvToken, final String oldPassword,
                                  final String newPassword) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/password/update";

        // 请求参数
        try {
            body.put("oldPassword", oldPassword);
            body.put("newPassword", newPassword);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<accountResetPassword> failure set JSON object!");
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<accountChgPassword> failure with no response!");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountChgPassword> failure"
                    + ", oldPassword=" + oldPassword
                    + ", newPassword=" + newPassword
                    + ", mErrorCode=" + responseObj.mErrorCode);
            return ErrCode.XERR_ACCOUNT_CHGPSWD;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountChgPassword> failure"
                    + ", oldPassword=" + oldPassword
                    + ", newPassword=" + newPassword
                    + ", mRespCode=" + responseObj.mRespCode);
            return mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_ACCOUNT_CHGPSWD);
        }

        ALog.getInstance().d(TAG, "<accountChgPassword> successful"
                + ", oldPassword=" + oldPassword
                + ", newPassword=" + newPassword    );
        return ErrCode.XOK;
    }

    /*
     * @brief 查询用户信息
     * @param srvToken : 账号Token
     * @return 查询到的用户信息，如果失败返回null
     */
    public static class UsrInfQueryResult {
        public int mErrCode = ErrCode.XOK;
        public IAccountMgr.UserInfo mUserInfo;
    }
    public UsrInfQueryResult accountUserInfoQuery(final String srvToken) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        UsrInfQueryResult queryResult = new UsrInfQueryResult();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/info/get";

        // 请求参数

        // 发送HTTP请求
        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<accountUserInfoQuery> failure with no response");
            queryResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return queryResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountUserInfoQuery> failure"
                    + ", mErrorCode=" + responseObj.mErrorCode);
            queryResult.mErrCode = responseObj.mErrorCode;
            return queryResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountChgPassword> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            queryResult.mErrCode = mapRespErrCode(responseObj.mErrorCode,
                                    ErrCode.XERR_ACCOUNT_USRINFO_QUERY);
            return queryResult;
        }

        // 解析用户信息
        IAccountMgr.UserInfo userInfo = new IAccountMgr.UserInfo();
        try {
            JSONObject userObj = responseObj.mRespJsonObj.getJSONObject("info");
            if (userObj == null) {
                queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
                return queryResult;
            }

            userInfo.mId = parseJsonLongValue(userObj, "id", -1);
            userInfo.mAccount = parseJsonStringValue(userObj, "account", null);
            userInfo.mEmail = parseJsonStringValue(userObj, "email", null);
            userInfo.mPhoneNumber = parseJsonStringValue(userObj, "phone", null);

            userInfo.mStatus = parseJsonIntValue(userObj, "status", -1);
            userInfo.mDeleted = parseJsonIntValue(userObj, "deleted", -1);
            userInfo.mIdentityId = parseJsonStringValue(userObj, "identityId", null);
            userInfo.mIdentityPoolId = parseJsonStringValue(userObj, "identityPoolId", null);
            userInfo.mMerchantId = parseJsonLongValue(userObj, "merchantId", -1);
            userInfo.mMerchantName = parseJsonStringValue(userObj, "merchantName", null);
            userInfo.mCreateBy = parseJsonLongValue(userObj, "createBy", -1);
            userInfo.mCreateTime = parseJsonLongValue(userObj, "createTime", -1);
            userInfo.mUpdateBy = parseJsonLongValue(userObj, "updateBy", -1);
            userInfo.mUpdateTime = parseJsonLongValue(userObj, "updateTime", -1);
            userInfo.mParam = parseJsonStringValue(userObj, "param", null);

            userInfo.mName = parseJsonStringValue(userObj, "name", null);
            userInfo.mAvatar = parseJsonStringValue(userObj, "avatar", null);
            userInfo.mSex = parseJsonIntValue(userObj, "sex", -1);
            userInfo.mAge = parseJsonIntValue(userObj, "age", -1);
            userInfo.mBirthday = parseJsonLongValue(userObj, "birthday", -1);
            userInfo.mHeight = parseJsonIntValue(userObj, "height", -1);
            userInfo.mWeight = parseJsonIntValue(userObj, "weight", -1);
            userInfo.mBackground = parseJsonStringValue(userObj, "background", null);

            userInfo.mCountry = parseJsonStringValue(userObj, "country", null);
            userInfo.mCountryId = parseJsonStringValue(userObj, "countryId", null);
            userInfo.mProvinceId = parseJsonStringValue(userObj, "provinceId", null);
            userInfo.mProvince = parseJsonStringValue(userObj, "province", null);
            userInfo.mCityId = parseJsonStringValue(userObj, "cityId", null);
            userInfo.mCity = parseJsonStringValue(userObj, "city", null);
            userInfo.mAreaId = parseJsonStringValue(userObj, "areaId", null);
            userInfo.mArea = parseJsonStringValue(userObj, "area", null);
            userInfo.mAddress = parseJsonStringValue(userObj, "address", null);

            queryResult.mUserInfo = userInfo;

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<accountUserInfoQuery> [JSONException], error=" + e);
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return queryResult;
        }

        ALog.getInstance().d(TAG, "<accountUserInfoQuery> successful"
                + ", userInfo=" + userInfo.toString());
        return queryResult;
    }

    /*
     * @brief 更新用户信息到服务器
     * @param srvToken : 账号Token
     * @param userInfo : 要更新的用户信息
     * @return 错误码
     */
    public int accountUserInfoUpdate(final String srvToken,
                                     final IAccountMgr.UserInfo updatedUserInfo) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/info/update";

        // 请求参数
        try {
            if (updatedUserInfo.mName != null) {
                body.put("name", updatedUserInfo.mName);
            }
            if (updatedUserInfo.mAvatar != null) {
                body.put("avatar", updatedUserInfo.mAvatar);
            }
            if (updatedUserInfo.mSex >= 0) {
                body.put("sex", String.valueOf(updatedUserInfo.mSex));
            }
            if (updatedUserInfo.mAge > 0) {
                body.put("age", String.valueOf(updatedUserInfo.mAge));
            }
            if (updatedUserInfo.mBirthday > 0) {
                body.put("birthday", String.valueOf(updatedUserInfo.mBirthday));
            }
            if (updatedUserInfo.mHeight > 0) {
                body.put("height", String.valueOf(updatedUserInfo.mHeight));
            }
            if (updatedUserInfo.mWeight > 0) {
                body.put("weight", String.valueOf(updatedUserInfo.mWeight));
            }
            if (updatedUserInfo.mBackground != null) {
                body.put("background", updatedUserInfo.mBackground);
            }

            if (updatedUserInfo.mCountryId != null) {
                body.put("countryId", updatedUserInfo.mCountryId);
            }
            if (updatedUserInfo.mCountry != null) {
                body.put("country", updatedUserInfo.mCountry);
            }
            if (updatedUserInfo.mProvinceId != null) {
                body.put("provinceId", updatedUserInfo.mProvinceId);
            }
            if (updatedUserInfo.mProvince != null) {
                body.put("province", updatedUserInfo.mProvince);
            }
            if (updatedUserInfo.mCityId != null) {
                body.put("cityId", updatedUserInfo.mCityId);
            }
            if (updatedUserInfo.mCity != null) {
                body.put("city", updatedUserInfo.mCity);
            }
            if (updatedUserInfo.mAreaId != null) {
                body.put("areaId", updatedUserInfo.mAreaId);
            }
            if (updatedUserInfo.mArea != null) {
                body.put("area", updatedUserInfo.mArea);
            }
            if (updatedUserInfo.mAddress != null) {
                body.put("address", updatedUserInfo.mAddress);
            }

        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<accountUserInfoUpdate> failure set JSON object!");
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }


        //
        // 发送HTTP请求
        //
        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<accountUserInfoUpdate> failure with no response");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountUserInfoUpdate> failure"
                    + ", mErrorCode=" + responseObj.mErrorCode);
            return ErrCode.XERR_ACCOUNT_USRINFO_UPDATE;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountUserInfoUpdate> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            return mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_ACCOUNT_USRINFO_UPDATE);
        }

        ALog.getInstance().d(TAG, "<accountUserInfoUpdate> successful"
                + ", updateUserInfo=" + updatedUserInfo.toString());
        return ErrCode.XOK;
    }

    /*
     * @brief 查询指定账号名下绑定的设备列表
     */
    public static class DeviceQueryResult {
        public int mErrCode = ErrCode.XOK;
        public ArrayList<DevInfo> mDeviceList = new ArrayList<>();
    }

    public DeviceQueryResult deviceQuery(final AccountInfo accountInfo) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        DeviceQueryResult queryResult = new DeviceQueryResult();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/device/list";

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                accountInfo.mPlatformToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<deviceQuery> failure with no response");
            queryResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return queryResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<deviceQuery> failure, mErrorCode="
                    + responseObj.mErrorCode);
            queryResult.mErrCode = ErrCode.XERR_DEVMGR_QUEYR;
            return queryResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<deviceQuery> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            queryResult.mErrCode = mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_QUEYR);
            return queryResult;
        }

        // 解析设备列表信息
        try {
            JSONArray listObj = responseObj.mRespJsonObj.getJSONArray("info");
            if (listObj == null) {
                queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
                return queryResult;
            }

            for (int i = 0; i < listObj.length(); i++) {
                JSONObject deviceObj = listObj.getJSONObject(i);
                DevInfo devInfo = new DevInfo();

                devInfo.mAppUserId = deviceObj.getString("appuserId");
                devInfo.mUserType = deviceObj.getString("uType");

                devInfo.mProductId = deviceObj.getString("productId");
                devInfo.mProductKey = deviceObj.getString("productKey");
                devInfo.mDeviceId = deviceObj.getString("deviceId");
                devInfo.mDeviceName = deviceObj.getString("deviceNickname");
                devInfo.mDeviceMac = deviceObj.getString("mac");
                devInfo.mSharer = deviceObj.getString("sharer");

                devInfo.mCreateTime = deviceObj.getLong("createTime");
                devInfo.mUpdateTime = deviceObj.getLong("updateTime");

                devInfo.mConnected = deviceObj.getBoolean("connect");

                if (devInfo.mProductKey.compareToIgnoreCase(mAppShadowProductKey) == 0) {
                    continue;   // 跳过 APP影子虚拟设备
                }

                queryResult.mDeviceList.add(devInfo);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<deviceQuery> [JSONException], error=" + e);
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return queryResult;
        }

        ALog.getInstance().d(TAG, "<deviceQuery> successful, token=" + accountInfo.mPlatformToken
                + ", deviceCount=" + queryResult.mDeviceList.size());
        return queryResult;
    }

    /*
     * @brief 绑定IoT设备
     */
    public int deviceBind(final AccountInfo accountInfo, final String productKey,
                          final String deviceMac) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/device/bind";

        // 请求参数
        try {
            body.put("productKey", productKey);
            body.put("mac", deviceMac);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<deviceBind> failure set JSON object!");
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                accountInfo.mPlatformToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<deviceBind> failure with no response");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<deviceBind> failure, token=" + accountInfo.mPlatformToken
                    + ", productKey=" + productKey + ", mac=" + deviceMac);
            return responseObj.mErrorCode;
        }
        if (responseObj.mRespCode != 0) {
            ALog.getInstance().d(TAG, "<deviceBind> failure, token=" + accountInfo.mPlatformToken
                    + ", productKey=" + productKey  + ", mac=" + deviceMac
                    + ", mRespCode=" + responseObj.mRespCode);
            return mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_ADD);
        }

        ALog.getInstance().d(TAG, "<deviceBind> successful, token=" + accountInfo.mPlatformToken
                + ", productKey=" + productKey  + ", mac=" + deviceMac);
        return ErrCode.XOK;
    }

    /*
     * @brief 解绑IoT设备
     */
    public int deviceUnbind(final AccountInfo accountInfo, String deviceId) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/device/unbind";

        // 请求参数
        try {
            body.put("deviceId", deviceId);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<deviceUnbind> failure set JSON object!");
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                accountInfo.mPlatformToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<deviceUnbind> failure with no response");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<deviceUnbind> failure, token=" + accountInfo.mPlatformToken
                    + ", deviceId=" + deviceId);
            return responseObj.mErrorCode;
        }
        if (responseObj.mRespCode != 0) {
            ALog.getInstance().e(TAG, "<deviceUnbind> failure, token=" + accountInfo.mPlatformToken
                    + ", deviceId=" + deviceId + ", mRespCode=" + responseObj.mRespCode);
            return mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_DEL);
        }

        ALog.getInstance().d(TAG, "<deviceUnbind> successful, token=" + accountInfo.mPlatformToken
                + ", deviceId=" + deviceId);
        return ErrCode.XOK;
    }

    /*
     * @brief 重命名IoT设备
     */
    public int deviceRename(final AccountInfo accountInfo, String deviceId, String name) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/device/update";

        // 请求参数
        try {
            body.put("deviceId", deviceId);
            body.put("deviceNickName", name);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<deviceRename> failure set JSON object!");
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                accountInfo.mPlatformToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<deviceRename> failure with no response");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<deviceRename> failure, token=" + accountInfo.mPlatformToken
                    + ", deviceId=" + deviceId);
            return responseObj.mErrorCode;
        }
        if (responseObj.mRespCode != 0) {
            ALog.getInstance().e(TAG, "<deviceRename> failure, token=" + accountInfo.mPlatformToken
                    + ", deviceId=" + deviceId + ", mRespCode=" + responseObj.mRespCode);
            return mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_RENAME);
        }

        ALog.getInstance().d(TAG, "<deviceRename> successful, token=" + accountInfo.mPlatformToken
                + ", deviceId=" + deviceId);
        return ErrCode.XOK;
    }


    /*
     * @brief 查询产品列表信息
     * @param srvToken : 参数
     */
    public IDeviceMgr.ProductQueryResult productQuery(final String srvToken,
                                                      final IDeviceMgr.ProductQueryParam queryParam ) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        IDeviceMgr.ProductQueryResult queryResult = new IDeviceMgr.ProductQueryResult();
        queryResult.mQueryParam = queryParam;

        // 请求URL
        String requestUrl = mServerBaseUrl + "/app/product/list";

        // 请求参数
        if (queryParam.mPageNo >= 0) {
            params.put("pageNo", String.valueOf(queryParam.mPageNo));
        }
        if (queryParam.mPageSize > 0) {
            params.put("pageSize", String.valueOf(queryParam.mPageSize));
        }
        if (queryParam.mProductTypeId > 0) {
            params.put("productTypeId", String.valueOf(queryParam.mProductTypeId));
        }
        if (queryParam.mConnectType >= 0) {
            params.put("connectType", String.valueOf(queryParam.mConnectType));
        }
        if (queryParam.mBlurry != null) {
            params.put("blurry", queryParam.mBlurry);
        }

        // 发送HTTP请求
        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<productQuery> failure with no response");
            queryResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return queryResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<productQuery> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            queryResult.mErrCode = mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_PRODUCT_QUERY);
            return queryResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<productQuery> failure, mErrorCode="
                    + responseObj.mErrorCode);
            queryResult.mErrCode = ErrCode.XERR_DEVMGR_PRODUCT_QUERY;
            return queryResult;
        }

        // 解析产品列表信息
        try {
            //
            // 解析产品列表
            //
            JSONArray listObj = responseObj.mRespJsonObj.getJSONArray("list");
            if (listObj == null) {
                ALog.getInstance().e(TAG, "<productQuery> failure, list Object is NULL");
                queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
                return queryResult;
            }
            for (int i = 0; i < listObj.length(); i++) {
                JSONObject productObj = listObj.getJSONObject(i);
                IDeviceMgr.ProductInfo productInfo = new IDeviceMgr.ProductInfo();

                productInfo.mId = parseJsonLongValue(productObj, "id", 0);
                productInfo.mName = parseJsonStringValue(productObj,"name", null);
                productInfo.mAlias = parseJsonStringValue(productObj,"alias", null);
                productInfo.mImgSmall = parseJsonStringValue(productObj,"imgSmall", null);
                productInfo.mImgBig = parseJsonStringValue(productObj,"imgBig", null);

                productInfo.mProductNumber = parseJsonStringValue(productObj,"productKey", null);
                productInfo.mProductTypeId = parseJsonLongValue(productObj, "productTypeId", 0);
                productInfo.mProductTypeName = parseJsonStringValue(productObj,"productTypeName", null);

                productInfo.mMerchantId = parseJsonLongValue(productObj, "merchantId", 0);
                productInfo.mMerchantName = parseJsonStringValue(productObj,"merchantName", null);

                productInfo.mStatus = parseJsonIntValue(productObj, "status", 0);
                productInfo.mDeleted = parseJsonIntValue(productObj, "deleted", 0);
                productInfo.mBindType = parseJsonIntValue(productObj, "deleted", 0);
                productInfo.mConnectType = parseJsonIntValue(productObj, "connectType", 0);

                productInfo.mCreateBy = parseJsonLongValue(productObj, "createBy", 0);
                productInfo.mCreateTime = parseJsonLongValue(productObj, "createTime", 0);
                productInfo.mUpdateBy = parseJsonLongValue(productObj, "updateBy", 0);
                productInfo.mUpdateTime = parseJsonLongValue(productObj, "updateTime", 0);


                queryResult.mProductList.add(productInfo);
            }

            //
            // 解析翻页信息
            //
            JSONObject pageTurnObj = responseObj.mRespJsonObj.getJSONObject("pageTurn");
            if (pageTurnObj == null) {
                ALog.getInstance().e(TAG, "<productQuery> failure, pageTurn Object is NULL");
                queryResult.mErrCode = ErrCode.XERR_DEVMGR_PRODUCT_QUERY;
                return queryResult;
            }
            queryResult.mPageTurn.mFirstPage = parseJsonIntValue(pageTurnObj, "firstPage", 0);
            queryResult.mPageTurn.mCurrentPage = parseJsonIntValue(pageTurnObj, "currentPage", 0);
            queryResult.mPageTurn.mPage = parseJsonIntValue(pageTurnObj, "page", 0);
            queryResult.mPageTurn.mPrevPage = parseJsonIntValue(pageTurnObj, "prevPage", 0);
            queryResult.mPageTurn.mNextPage = parseJsonIntValue(pageTurnObj, "nextPage", 0);
            queryResult.mPageTurn.mPageCount = parseJsonIntValue(pageTurnObj, "pageCount", 0);
            queryResult.mPageTurn.mPageSize = parseJsonIntValue(pageTurnObj, "pageSize", 0);

            queryResult.mPageTurn.mRowCount = parseJsonIntValue(pageTurnObj, "rowCount", 0);
            queryResult.mPageTurn.mStart = parseJsonIntValue(pageTurnObj, "start", 0);
            queryResult.mPageTurn.mEnd = parseJsonIntValue(pageTurnObj, "end", 0);
            queryResult.mPageTurn.mStartIndex = parseJsonIntValue(pageTurnObj, "startIndex", 0);

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<productQuery> [JSONException], error=" + e);
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return queryResult;
        }

        ALog.getInstance().d(TAG, "<productQuery> successful"
                + ", productListCount=" + queryResult.mProductList.size());
        return queryResult;
    }


    /*
     * @brief 分享设备
     * @param srvToken : Token参数
     * @param force : 是否强制分享，强制分享无需对端账号接受
     */
    public int shareDevice(final String srvToken, boolean force,
                           final String deviceId, final String sharingAccount, int permission) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String cmd = force ? ("/user/device/share/touser") : ("/device/share/push/add");
        String requestUrl = mServerBaseUrl + cmd;

        // 请求参数
        try {
            body.put("deviceId", deviceId);
            body.put("type", permission);
            body.put("userId", sharingAccount);

        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<shareDevice> failure set JSON object!");
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        // 发送HTTP请求
        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<shareDevice> failure with no response");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<shareDevice> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            if (responseObj.mRespCode == 1) {  // 绑定的用户账号不存在
                return ErrCode.XERR_DEVMGR_NO_BIND_USER;
            }
            return mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_SHARE);
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<shareDevice> failure, mErrorCode="
                    + responseObj.mErrorCode);
            return ErrCode.XERR_DEVMGR_SHARE;
        }

        ALog.getInstance().d(TAG, "<shareDevice> successful"
                + ", deviceId=" + deviceId
                + ", sharingAccount=" + sharingAccount
                + ", permission=" + permission      );
        return ErrCode.XOK;
    }

    /*
     * @brief 取消设备分享
     * @param srvToken : Token参数
     * @param deviceId : 设备Id
     * @param desharingAccount : 要解除的对端账号
     */
    public int deshareDevice(final String srvToken, final String deviceId,
                             final String desharingAccount) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/device/member/remove";

        // 请求参数
        try {
            body.put("deviceId", deviceId);
            body.put("userId", desharingAccount);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<deshareDevice> failure set JSON object!");
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        // 发送HTTP请求
        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<deshareDevice> failure with no response");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<deshareDevice> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            return mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_DESHARE);
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<deshareDevice> failure, mErrorCode="
                    + responseObj.mErrorCode);
            return ErrCode.XERR_DEVMGR_DESHARE;
        }

        ALog.getInstance().d(TAG, "<deshareDevice> successful"
                + ", deviceId=" + deviceId
                + ", desharingAccount=" + desharingAccount  );
        return ErrCode.XOK;
    }

    /*
     * @brief 接受分享过来的设备
     * @param srvToken : Token参数
     * @param deviceName : 设备名字
     * @param order : 分享口令
     */
    public int acceptDevice(final String srvToken, final String deviceName,
                             final String order) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/device/accept";

        // 请求参数
        try {
            body.put("deviceNickname", deviceName);
            body.put("order", order);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<acceptDevice> failure set JSON object!");
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        // 发送HTTP请求
        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<acceptDevice> failure with no response");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<acceptDevice> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            return mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_ACCEPT);
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<acceptDevice> failure, mErrorCode="
                    + responseObj.mErrorCode);
            return ErrCode.XERR_DEVMGR_ACCEPT;
        }

        ALog.getInstance().d(TAG, "<acceptDevice> successful"
                + ", deviceName=" + deviceName
                + ", order=" + order  );
        return ErrCode.XOK;
    }


    /*
     * @brief 查询可以分享出去的设备列表
     */
    public DeviceQueryResult querySharableDevList(final String srvToken) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        DeviceQueryResult queryResult = new DeviceQueryResult();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/device/own/devices";

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<querySharableDevList> failure with no response");
            queryResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return queryResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<querySharableDevList> failure, mErrorCode="
                    + responseObj.mErrorCode);
            queryResult.mErrCode = ErrCode.XERR_DEVMGR_QUEYR;
            return queryResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<querySharableDevList> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            queryResult.mErrCode = mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_QUEYR);
            return queryResult;
        }

        // 解析设备列表信息
        try {
            JSONArray listObj = responseObj.mRespJsonObj.getJSONArray("info");
            if (listObj == null) {
                queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
                return queryResult;
            }

            for (int i = 0; i < listObj.length(); i++) {
                JSONObject deviceObj = listObj.getJSONObject(i);
                DevInfo devInfo = new DevInfo();
                devInfo.mDeviceName = deviceObj.getString("nickName");
                devInfo.mDeviceId = deviceObj.getString("deviceId");
                devInfo.mDeviceMac = deviceObj.getString("mac");
                devInfo.mCreateTime = deviceObj.getLong("time");
                devInfo.mShareCount = deviceObj.getInt("count");

                queryResult.mDeviceList.add(devInfo);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<querySharableDevList> [JSONException], error=" + e);
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return queryResult;
        }

        ALog.getInstance().d(TAG, "<querySharableDevList> successful"
                + ", deviceCount=" + queryResult.mDeviceList.size());
        return queryResult;
    }

    /*
     * @brief 查询分享出去的账号和设备信息
     */
    public static class OutSharerQueryResult {
        public int mErrCode = ErrCode.XOK;
        public ArrayList<IotOutSharer> mOutSharerList = new ArrayList<>();
    }

    public OutSharerQueryResult queryOutSharerList(final String srvToken,
                                                    final String deviceId) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        OutSharerQueryResult queryResult = new OutSharerQueryResult();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/device/share/cancel/list";

        // 请求参数
        try {
            body.put("deviceId", deviceId);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<queryOutSharerList> failure set JSON object!");
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return queryResult;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<queryOutSharerList> failure with no response");
            queryResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return queryResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryOutSharerList> failure, mErrorCode="
                    + responseObj.mErrorCode);
            queryResult.mErrCode = ErrCode.XERR_DEVMGR_QUEYR;
            return queryResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryOutSharerList> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            queryResult.mErrCode = mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_QUEYR);
            return queryResult;
        }

        // 解析设备列表信息
        try {
            JSONArray listObj = responseObj.mRespJsonObj.getJSONArray("info");
            if (listObj == null) {
                queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
                return queryResult;
            }

            for (int i = 0; i < listObj.length(); i++) {
                JSONObject deviceObj = listObj.getJSONObject(i);
                IotOutSharer outSharer = new IotOutSharer();

                outSharer.mSharer = parseJsonStringValue(deviceObj,"sharer", null);
                outSharer.mShareType = parseJsonIntValue(deviceObj,"uType", -1);
                outSharer.mPhone = parseJsonStringValue(deviceObj,"phone", null);
                outSharer.mEmail = parseJsonStringValue(deviceObj,"email", null);
                outSharer.mUsrNickName = parseJsonStringValue(deviceObj,"nickName", null);
                outSharer.mAppUserId = parseJsonStringValue(deviceObj,"appuserId", null);
                outSharer.mAvatar = parseJsonStringValue(deviceObj,"avatar", null);

                outSharer.mProductNumber = parseJsonStringValue(deviceObj,"productId", null);
                outSharer.mProductID = parseJsonStringValue(deviceObj,"productKey", null);
                outSharer.mDeviceNumber = parseJsonStringValue(deviceObj,"deviceId", null);
                outSharer.mDeviceID = parseJsonStringValue(deviceObj,"mac", null);
                outSharer.mDevNickName = parseJsonStringValue(deviceObj,"deviceNickname", null);
                outSharer.mConnected = parseJsonBoolValue(deviceObj,"connect", false);

                outSharer.mCreateTime = parseJsonLongValue(deviceObj,"createTime", -1);
                outSharer.mUpdateTime = parseJsonLongValue(deviceObj,"updateTime", -1);

                queryResult.mOutSharerList.add(outSharer);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<queryOutSharerList> [JSONException], error=" + e);
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return queryResult;
        }

        ALog.getInstance().d(TAG, "<queryOutSharerList> successful"
                + ", sharerCount=" + queryResult.mOutSharerList.size());
        return queryResult;
    }

    /*
     * @brief 查询分享尽量的设备列表
     */
    public DeviceQueryResult queryFromsharingDevList(final String srvToken) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        DeviceQueryResult queryResult = new DeviceQueryResult();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/user/device/share/withme";

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<queryFromsharingDevList> failure with no response");
            queryResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return queryResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryFromsharingDevList> failure, mErrorCode="
                    + responseObj.mErrorCode);
            queryResult.mErrCode = ErrCode.XERR_DEVMGR_QUEYR;
            return queryResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryFromsharingDevList> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            queryResult.mErrCode = mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_QUEYR);
            return queryResult;
        }

        // 解析设备列表信息
        try {
            JSONArray listObj = responseObj.mRespJsonObj.getJSONArray("info");
            if (listObj == null) {
                queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
                return queryResult;
            }

            for (int i = 0; i < listObj.length(); i++) {
                JSONObject deviceObj = listObj.getJSONObject(i);
                DevInfo devInfo = new DevInfo();
                devInfo.mDeviceName = deviceObj.getString("nickName");
                devInfo.mDeviceId = deviceObj.getString("deviceId");
                devInfo.mDeviceMac = deviceObj.getString("mac");
                devInfo.mCreateTime = deviceObj.getLong("time");

                queryResult.mDeviceList.add(devInfo);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<queryFromsharingDevList> [JSONException], error=" + e);
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return queryResult;
        }

        ALog.getInstance().d(TAG, "<queryFromsharingDevList> successful"
                + ", deviceCount=" + queryResult.mDeviceList.size());
        return queryResult;
    }

    /*
     * @brief 分页查询设备推送消息列表
     */
    public static class ShareMsgPageQueryResult {
        public int mErrCode = ErrCode.XOK;
        public IotShareMsgPage mPageInfo = new IotShareMsgPage();
    }

    public ShareMsgPageQueryResult queryShareMsgByPage(final String srvToken, int pageNumber,
                                                       int pageSize, int auditStatus) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        ShareMsgPageQueryResult queryResult = new ShareMsgPageQueryResult();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/device/share/push/list";


        // 请求参数
        try {
            if (pageNumber >= 0) {
                body.put("pageNo", pageNumber);
            }

            if (pageSize >= 0) {
                body.put("pageSize", pageSize);
            }

            if (auditStatus == 1 || auditStatus == 2) {
                body.put("auditStatus", (auditStatus == 1) ? true : false);
            }

        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<queryOutSharerList> failure set JSON object!");
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return queryResult;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<queryShareMsgByPage> failure with no response");
            queryResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return queryResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryShareMsgByPage> failure, mErrorCode="
                    + responseObj.mErrorCode);
            queryResult.mErrCode = ErrCode.XERR_DEVMGR_QUEYR;
            return queryResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryShareMsgByPage> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            queryResult.mErrCode = mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_QUEYR);
            return queryResult;
        }

        // 解析设备列表信息
        try {
            JSONArray listObj = responseObj.mRespJsonObj.getJSONArray("list");
            if (listObj == null) {
                queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
                return queryResult;
            }

            for (int i = 0; i < listObj.length(); i++) {
                JSONObject messageObj = listObj.getJSONObject(i);
                IotShareMessage shareMessage = new IotShareMessage();

                shareMessage.mMessageId = parseJsonLongValue(messageObj, "id", 0);
                shareMessage.mMessageType = parseJsonIntValue(messageObj, "msgType", -1);
                shareMessage.mStatus = parseJsonIntValue(messageObj, "status", -1);
                shareMessage.mAuditStatus = parseJsonBoolValue(messageObj, "auditStatus", false);

                shareMessage.mPushType = parseJsonIntValue(messageObj, "type", -1);
                shareMessage.mPushTime = parseJsonLongValue(messageObj, "pushTime", 0);

                shareMessage.mParam = parseJsonStringValue(messageObj, "para", null);
                shareMessage.mPermission = parseJsonIntValue(messageObj, "permission", -1);
                shareMessage.mTitle= parseJsonStringValue(messageObj, "title", null);
                shareMessage.mContent = parseJsonStringValue(messageObj, "content", null);
                shareMessage.mRecvUserId = parseJsonLongValue(messageObj, "userId", 0);

                shareMessage.mDeviceNumber = parseJsonStringValue(messageObj, "deviceId", null);
                shareMessage.mMerchantId = parseJsonStringValue(messageObj, "merchantId", null);
                shareMessage.mMerchantName = parseJsonStringValue(messageObj, "merchantName", null);
                shareMessage.mProductImgUrl = parseJsonStringValue(messageObj, "img", null);

                shareMessage.mDeleted = parseJsonBoolValue(messageObj, "deleted", false);
                shareMessage.mCreatedBy = parseJsonLongValue(messageObj, "createBy", -1);
                shareMessage.mCreateTime = parseJsonLongValue(messageObj, "createTime", -1);
                shareMessage.mUpdatedBy = parseJsonLongValue(messageObj, "updateBy", -1);
                shareMessage.mUpdateTime= parseJsonLongValue(messageObj, "updateTime", -1);

                queryResult.mPageInfo.mShareMsgList.add(shareMessage);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<queryShareMsgByPage> [JSONException], error=" + e);
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return queryResult;
        }

        ALog.getInstance().d(TAG, "<queryShareMsgByPage> successful"
                + ", mShareMsgList=" + queryResult.mPageInfo.mShareMsgList.size());
        return queryResult;
    }

    /*
     * @brief 查询单个分享消息
     */
    public static class ShareMsgInfoResult {
        public int mErrCode = ErrCode.XOK;
        public IotShareMessage mShareMsg = new IotShareMessage();
    }
    public ShareMsgInfoResult queryShareMsgDetail(final String srvToken, long messageId) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        ShareMsgInfoResult queryResult = new ShareMsgInfoResult();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/device/share/push/details";

        // 请求参数
        try {
            body.put("id", messageId);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<queryShareMsgDetail> failure set JSON object!");
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return queryResult;
        }


        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<queryShareMsgDetail> failure with no response");
            queryResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return queryResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryShareMsgDetail> failure, mErrorCode="
                    + responseObj.mErrorCode);
            queryResult.mErrCode = ErrCode.XERR_DEVMGR_QUERY_SHAREDETAIL;
            return queryResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryShareMsgDetail> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            queryResult.mErrCode = mapRespErrCode(responseObj.mRespCode,
                    ErrCode.XERR_DEVMGR_QUERY_SHAREDETAIL);
            return queryResult;
        }

        // 解析设备列表信息
        try {
            JSONObject infoObj = responseObj.mRespJsonObj.getJSONObject("info");
            if (infoObj == null) {
                queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
                return queryResult;
            }

            queryResult.mShareMsg.mMessageId = parseJsonLongValue(infoObj, "id", 0);
            queryResult.mShareMsg.mMessageType = parseJsonIntValue(infoObj, "msgType", -1);
            queryResult.mShareMsg.mStatus = parseJsonIntValue(infoObj, "status", -1);
            queryResult.mShareMsg.mAuditStatus = parseJsonBoolValue(infoObj, "auditStatus", false);

            queryResult.mShareMsg.mPushType = parseJsonIntValue(infoObj, "type", -1);
            queryResult.mShareMsg.mPushTime = parseJsonLongValue(infoObj, "pushTime", 0);

            queryResult.mShareMsg.mParam = parseJsonStringValue(infoObj, "para", null);
            queryResult.mShareMsg.mPermission = parseJsonIntValue(infoObj, "permission", -1);
            queryResult.mShareMsg.mTitle= parseJsonStringValue(infoObj, "title", null);
            queryResult.mShareMsg.mContent = parseJsonStringValue(infoObj, "content", null);
            queryResult.mShareMsg.mRecvUserId = parseJsonLongValue(infoObj, "userId", 0);

            queryResult.mShareMsg.mDeviceNumber = parseJsonStringValue(infoObj, "deviceId", null);
            queryResult.mShareMsg.mMerchantId = parseJsonStringValue(infoObj, "merchantId", null);
            queryResult.mShareMsg.mMerchantName = parseJsonStringValue(infoObj, "merchantName", null);
            queryResult.mShareMsg.mProductImgUrl = parseJsonStringValue(infoObj, "img", null);

            queryResult.mShareMsg.mDeleted = parseJsonBoolValue(infoObj, "deleted", false);
            queryResult.mShareMsg.mCreatedBy = parseJsonLongValue(infoObj, "createBy", -1);
            queryResult.mShareMsg.mCreateTime = parseJsonLongValue(infoObj, "createTime", -1);
            queryResult.mShareMsg.mUpdatedBy = parseJsonLongValue(infoObj, "updateBy", -1);
            queryResult.mShareMsg.mUpdateTime= parseJsonLongValue(infoObj, "updateTime", -1);

            queryResult.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<queryShareMsgDetail> [JSONException], error=" + e);
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return queryResult;
        }

        ALog.getInstance().d(TAG, "<queryShareMsgDetail> successful"
                + ", queryResult.mShareMsg=" + queryResult.mShareMsg.toString());
        return queryResult;
    }


    /*
     * @brief 删除单个分享消息
     */
    public int deleteShareMsg(final String srvToken, long messageId) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        ShareMsgInfoResult queryResult = new ShareMsgInfoResult();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/device/share/push/del";

        // 请求参数
        try {
            body.put("id", messageId);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<deleteShareMsg> failure set JSON object!");
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        // 发送HTTP请求
        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<deleteShareMsg> failure with no response");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<deleteShareMsg> failure, mErrorCode="
                    + responseObj.mErrorCode);
            return ErrCode.XERR_DEVMGR_QUEYR;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<deleteShareMsg> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            return mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_QUEYR);
        }

        ALog.getInstance().d(TAG, "<deleteShareMsg> successful, messageId=" + messageId);
        return ErrCode.XOK;
    }


    /**
     * @brief 查询到的MCU版本信息
     */
    public static class McuVersionResult {
        public int mErrCode = ErrCode.XOK;
        public IDeviceMgr.McuVersionInfo mMcuVersion = new IDeviceMgr.McuVersionInfo();
    }

    /**
     * @brief 获取最新的固件版本信息
     * @param srvToken : Token参数
     * @param deviceId : 设备Id
     */
    public McuVersionResult getMcuVersion(final String srvToken,
                                          final String deviceId) {
        McuVersionResult result = new McuVersionResult();
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String cmd = "/user/device/mcuota/get";
        String requestUrl = mServerBaseUrl + cmd;

        // 请求参数
        try {
            body.put("deviceId", deviceId);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<getMcuVersion> failure set JSON object!");
            result.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return result;
        }

        // 发送HTTP请求
        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<getMcuVersion> failure with no response");
            result.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return result;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<getMcuVersion> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            result.mErrCode = mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_GET_MCUVER);
            return result;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<getMcuVersion> failure, mErrorCode="
                    + responseObj.mErrorCode);
            result.mErrCode = ErrCode.XERR_DEVMGR_GET_MCUVER;
            return result;
        }


        // 解析固件版本信息
        try {
            JSONObject infoObj = responseObj.mRespJsonObj.getJSONObject("info");
            if (infoObj == null) {
                result.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
                return result;
            }

            long deviceNumber = parseJsonLongValue(infoObj, "deviceId", -1);
            result.mMcuVersion.mDeviceNumber = Long.toString(deviceNumber);
            result.mMcuVersion.mDeviceID = parseJsonStringValue(infoObj, "mac", null);
            result.mMcuVersion.mUpgradeId = parseJsonLongValue(infoObj, "upgradeId", -1);
            result.mMcuVersion.mIsupgradable = parseJsonBoolValue(infoObj, "isUpgrade", false);
            result.mMcuVersion.mCurrVersion = parseJsonStringValue(infoObj, "currentVersion", null);
            result.mMcuVersion.mUpgradeVersion = parseJsonStringValue(infoObj, "upgradeVersion", null);
            result.mMcuVersion.mRemark = parseJsonStringValue(infoObj, "remark", null);
            result.mMcuVersion.mReleasedTime = parseJsonLongValue(infoObj, "releaseTime", -1);
            result.mMcuVersion.mSize = parseJsonLongValue(infoObj, "size", -1);

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<accountUserInfoQuery> [JSONException], error=" + e);
            result.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return result;
        }

        ALog.getInstance().d(TAG, "<getMcuVersion> successful"
                + ", deviceId=" + deviceId
                + ", mcuVersion=" + result.mMcuVersion.toString());
        return result;
    }

    /**
     * @brief 升级固件版本信息
     * @param srvToken : Token参数
     * @param deviceId : 设备Id
     * @param upgradeId : 升级Id
     * @param decide : 升级方式
     */
    public int upgradeMcuVersion(final String srvToken,
                                 final String deviceId,
                                 final long upgradeId,
                                 final int decide           ) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String cmd = "/user/device/mcuota/decide";
        String requestUrl = mServerBaseUrl + cmd;

        // 请求参数
        try {
            body.put("deviceId", deviceId);
            body.put("upgradeId", upgradeId);
            body.put("decide", decide);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<upgradeMcuVersion> failure set JSON object!");
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        // 发送HTTP请求
        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<upgradeMcuVersion> failure with no response");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<upgradeMcuVersion> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            return mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_UPGRADE_MCUVER);
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<upgradeMcuVersion> failure, mErrorCode="
                    + responseObj.mErrorCode);
            return ErrCode.XERR_DEVMGR_UPGRADE_MCUVER;
        }

        ALog.getInstance().d(TAG, "<upgradeMcuVersion> successful"
                + ", deviceId=" + deviceId  + ", upgradeId=" + upgradeId
                + ", decide=" + decide);
        return ErrCode.XOK;
    }

    /**
     * @brief 固件升级状态信息
     */
    public static class McuUpgradeProgress {
        public int mErrCode = ErrCode.XOK;
        public IDeviceMgr.McuUpgradeStatus mPrgoress = new IDeviceMgr.McuUpgradeStatus();
    }

    /**
     * @brief 获取固件升级状态
     * @param srvToken : 服务Token参数
     * @param upgradeId : 升级Id
     */
    public McuUpgradeProgress getMcuUpgradeStatus(final String srvToken, final long upgradeId) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        McuUpgradeProgress result = new McuUpgradeProgress();

        // 请求URL
        String cmd = "/user/device/mcuota/query";
        String requestUrl = mServerBaseUrl + cmd;

        // 请求参数
        try {
            body.put("upgradeId", upgradeId);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
            ALog.getInstance().e(TAG, "<getMcuUpgradeStatus> failure set JSON object!");
            result.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return result;
        }

        // 发送HTTP请求
        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<getMcuUpgradeStatus> failure with no response");
            result.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return result;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<getMcuUpgradeStatus> failure"
                    + ", mRespCode=" + responseObj.mRespCode);
            result.mErrCode = mapRespErrCode(responseObj.mRespCode, ErrCode.XERR_DEVMGR_UPGRADE_MCUVER);
            return result;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<getMcuUpgradeStatus> failure, mErrorCode="
                    + responseObj.mErrorCode);
            result.mErrCode = ErrCode.XERR_DEVMGR_UPGRADE_MCUVER;
            return result;
        }

        // 解析固件升级信息
        try {
            JSONObject infoObj = responseObj.mRespJsonObj.getJSONObject("info");
            if (infoObj == null) {
                result.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
                return result;
            }

            long deviceNumber = parseJsonLongValue(infoObj, "deviceId", -1);
            result.mPrgoress.mDeviceNumber = String.valueOf(deviceNumber);
            result.mPrgoress.mDeviceName = parseJsonStringValue(infoObj, "deviceName", null);
            result.mPrgoress.mDeviceID = parseJsonStringValue(infoObj, "mac", null);
            result.mPrgoress.mCurrVersion = parseJsonStringValue(infoObj, "currentVersion", null);
            result.mPrgoress.mStatus = parseJsonIntValue(infoObj, "status", -1);

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<getMcuUpgradeStatus> [JSONException], error=" + e);
            result.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return result;
        }

        ALog.getInstance().d(TAG, "<getMcuUpgradeStatus> successful"
                + ", progress=" + result.mPrgoress.toString());
        return result;
    }


    ////////////////////////////////////////////////////////////////////////
    ///////////////////////////// Inner Methods ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief 给服务器发送HTTP请求，并且等待接收回应数据
     *        该函数是阻塞等待调用，因此最好是在工作线程中执行
     */
    private synchronized ResponseObj requestToServer(String baseUrl, String method, String token,
                                                     Map<String, String> params, JSONObject body) {

        ResponseObj responseObj = new ResponseObj();

        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            responseObj.mErrorCode = ErrCode.XERR_HTTP_URL;
            ALog.getInstance().e(TAG, "<requestToServer> Invalid url=" + baseUrl);
            return responseObj;
        }

        // 拼接URL和请求参数生成最终URL
        String realURL = baseUrl;
        if (!params.isEmpty()) {
            Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
            Map.Entry<String, String> entry =  it.next();
            realURL += "?" + entry.getKey() + "=" + entry.getValue();
            while (it.hasNext()) {
                entry =  it.next();
                realURL += "&" + entry.getKey() + "=" + entry.getValue();
            }
        }

        // 支持json格式消息体
        String realBody = String.valueOf(body);

        ALog.getInstance().d(TAG, "<requestToServer> requestUrl=" + realURL
                + ", requestBody="  + realBody.toString());

        //开启子线程来发起网络请求
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder();


        //同步方式请求HTTP，因此请求操作最好放在工作线程中进行
        try {
            java.net.URL url = new URL(realURL);
            connection = (HttpURLConnection) url.openConnection();
            // 设置token
            if ((token != null) && (!token.isEmpty())) {
                connection.setRequestProperty("token", token);
            }

            switch (method) {
                case "GET":
                    connection.setRequestMethod("GET");
                    break;

                case "POST":
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                    DataOutputStream os = new DataOutputStream(connection.getOutputStream());
                    os.write(realBody.getBytes());  // 必须是原始数据流，否则中文乱码
                    os.flush();
                    os.close();
                    break;

                case "SET":
                    connection.setRequestMethod("SET");
                    break;

                case "DELETE":
                    connection.setRequestMethod("DELETE");
                    break;

                default:
                    ALog.getInstance().e(TAG, "<requestToServer> Invalid method=" + method);
                    responseObj.mErrorCode = ErrCode.XERR_HTTP_METHOD;
                    return responseObj;
            }
            connection.setReadTimeout(HTTP_TIMEOUT);
            connection.setConnectTimeout(HTTP_TIMEOUT);
            responseObj.mRespCode = connection.getResponseCode();
            if (responseObj.mRespCode != HttpURLConnection.HTTP_OK) {
                responseObj.mErrorCode = ErrCode.XERR_HTTP_RESP_CODE + responseObj.mRespCode;
                ALog.getInstance().e(TAG, "<requestToServer> Error response code="
                        + responseObj.mRespCode + ", errMessage=" + connection.getResponseMessage());
                return responseObj;
            }

            // 读取回应数据包
            InputStream inputStream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JSONObject data = null;
            try {
                responseObj.mRespJsonObj = new JSONObject(response.toString());
                responseObj.mRespCode = responseObj.mRespJsonObj.getInt("code");
                responseObj.mTip = responseObj.mRespJsonObj.getString("tip");

            } catch (JSONException e) {
                e.printStackTrace();
                ALog.getInstance().e(TAG, "<requestToServer> Invalied json=" + response);
                responseObj.mErrorCode = ErrCode.XERR_HTTP_RESP_DATA;
                responseObj.mRespJsonObj = null;
            }

            ALog.getInstance().d(TAG, "<requestToServer> finished, response="  + response.toString());
            return responseObj;

        } catch (Exception e) {
            e.printStackTrace();
            responseObj.mErrorCode = ErrCode.XERR_HTTP_CONNECT;
            return responseObj;

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    /*
     * @brief  查询user对应的虚拟设备（首次调用创建虚拟设备）
     * @param accountInfo : 登录的账号信息
     * @return 获取到的虚拟设备thing name
     */
    private String queryInventDeviceName(final String srvToken) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mServerBaseUrl + "/device/invent/certificate/get";

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                srvToken, params, body);
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryInventDeviceName> failure"
                    + ", toke=" + srvToken);
            return null;
        }

        // 解析虚拟设备名称
        String deviceName = "";
        try {
            JSONObject infoObj = responseObj.mRespJsonObj.getJSONObject("info");
            deviceName = infoObj.getString("thingName");
        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<queryInventDeviceName> [JSONException], error=" + e);
            return null;
        }

        ALog.getInstance().d(TAG, "<queryInventDeviceName> done"
                + ", toke=" + srvToken
                + ", device name =" + deviceName);
        return deviceName;
    }


    int parseJsonIntValue(JSONObject jsonState, String fieldName, int defVal) {
        try {
            int value = jsonState.getInt(fieldName);
            return value;

        } catch (JSONException e) {
            ALog.getInstance().e(TAG, "<parseJsonIntValue> "
                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return defVal;
        }
    }

    long parseJsonLongValue(JSONObject jsonState, String fieldName, long defVal) {
        try {
            long value = jsonState.getLong(fieldName);
            return value;

        } catch (JSONException e) {
            ALog.getInstance().e(TAG, "<parseJsonLongValue> "
                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return defVal;
        }
    }

    String parseJsonStringValue(JSONObject jsonState, String fieldName, String defVal) {
        try {
            String value = jsonState.getString(fieldName);
            return value;

        } catch (JSONException e) {
            ALog.getInstance().e(TAG, "<parseJsonIntValue> "
                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return defVal;
        }
    }

    Boolean parseJsonBoolValue(JSONObject jsonState, String fieldName, boolean defVal) {
        try {
            Boolean value = jsonState.getBoolean(fieldName);
            return value;

        } catch (JSONException e) {
            ALog.getInstance().e(TAG, "<parseJsonBoolValue> "
                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return defVal;
        }
    }


    /*
     * @brief 将response code映射成返回给应用层的错误代码
     */
    int mapRespErrCode(int respCode, int defErrCode) {
        int retCode = defErrCode;

        switch (respCode) {
            case RESP_OK:
                retCode = ErrCode.XOK;
                break;

            case RESP_PARAM_INVALID:
                retCode = ErrCode.XERR_INVALID_PARAM;
                break;

            case RESP_SYS_EXCEPTION:
                retCode = ErrCode.XERR_SYSTEM;
                break;

            case RESP_TOKEN_INVALID:
                retCode = ErrCode.XERR_TOKEN_INVALID;
                break;

            case RESP_VERYCODE_ERR:
                retCode = ErrCode.XERR_ACCOUNT_VERYCODE;
                break;

            case RESP_USER_ALREADY_EXIST:
                retCode = ErrCode.XERR_ACCOUNT_ALREADY_EXIST;
                break;

            case RESP_USER_NOT_EXIST:
                retCode = ErrCode.XERR_ACCOUNT_NOT_EXIST;
                break;

            case RESP_PASSWORD_ERR:
                retCode = ErrCode.XERR_ACCOUNT_PASSWORD_ERR;
                break;

            case RESP_NOT_ALLOW_OPT:
                retCode = ErrCode.XERR_DEVMGR_NOT_ALLOW;
                break;

            case RESP_DEV_ALREADY_SHARED:
                retCode = ErrCode.XERR_DEVMGR_ALREADY_SHARED;
                break;

            case RESP_UPGRADE_INVALID:
                retCode = ErrCode.XERR_DEVMGR_UPGRADE_INVALID;
                break;
        }

        return retCode;
    }
}
