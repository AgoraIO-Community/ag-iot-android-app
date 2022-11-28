/**
 * @file ThirdAccountMgr.java
 * @brief 这个文件是客户应用层实现自己的账号注册、账号登逻辑
 *        在实际业务场景中客户可以根据自己的账号系统做调整，但是一定要返回 IAccountMgr.LoginParam 这些信息
 *        进行灵隼平台的登录调用
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-08-08
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */

package io.agora.iotlinkdemo.thirdpartyaccount;


import android.text.TextUtils;
import android.util.Log;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAccountMgr;
import io.agora.iotlink.utils.RSAUtils;


public class ThirdAccountMgr {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////
    public interface IRegisterCallback{
        void onThirdAccountRegisterDone(int errCode, final String errMessage,
                                        final String account, final String password);
    }

    public interface IUnregisterCallback{
        void onThirdAccountUnregisterDone(int errCode, final String errMessage,
                                          final String account, final String password);
    }

    public interface ILoginCallback{
        void onThirdAccountLoginDone(int errCode, final String errMessage,
                                     final String account, final String password,
                                     final String rsaPublicKey,
                                     final IAccountMgr.LoginParam loginParam);
    }

    public interface IQueryIdCallback{
        void onThirdAccountQueryIdDone( int errCode, final String errMessage,
                                        final String accountName, final String accountId);
    }

    public interface IReqVerifyCodeCallback{
        void onThirdAccountReqVCodeDone( int errCode, final String errMessage,
                                         final String phoneNumber);
    }

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/ThridAccountMgr";
    private static final int HTTP_TIMEOUT = 8000;



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static ThirdAccountMgr mInstance = null;
    private ExecutorService mExecSrv = Executors.newSingleThreadExecutor();

    ///< 服务器请求站点
    private String mThirdBaseUrl = "https://third-user.sh3.agoralab.co/third-party";
    //private String mThirdBaseUrl = "https://third-user.la3.agoralab.co/third-party";  // 针对北美区域
    //private String mThirdBaseUrl = "https://third-user.sh.agoralab.co/third-party";  // 测试环境

    private String mLoginAccountName;       ///< 当前已经登录的账号名称
    private String mLoginAccountId;         ///< 当前已登录账号Id


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public static ThirdAccountMgr getInstance() {
        if (mInstance == null) {
            synchronized (ThirdAccountMgr.class) {
                if (mInstance == null) {
                    mInstance = new ThirdAccountMgr();
                }
            }
        }
        return mInstance;
    }


    public void setAccountServerUrl(final String serverUrl) {
        mThirdBaseUrl = serverUrl;
    }

    class AccountMgrCallable implements Callable<Integer> {
        private String mAccount;
        private String mPassword;
        private String mVerifyCode;
        private String mRsaPublicKey;
        private int mOperation;
        private IRegisterCallback mRegCallback;
        private IUnregisterCallback mUnregCallback;
        private ILoginCallback mLoginCallback;
        private IQueryIdCallback mQueryIdCallback;
        private IReqVerifyCodeCallback mReqVCodeCallback;

        public AccountMgrCallable(final String account, final String password,
                                  final String verifyCode, IRegisterCallback callback) {
            mAccount = account;
            mPassword = password;
            mVerifyCode = verifyCode;
            mOperation = 1;
            mRegCallback = callback;
        }

        public AccountMgrCallable(final String account, final String password,
                                  IUnregisterCallback callback) {
            mAccount = account;
            mPassword = password;
            mOperation = 2;
            mUnregCallback = callback;
        }

        public AccountMgrCallable(final String account, final String password,
                                  final String rsaPublicKey, ILoginCallback callback) {
            mAccount = account;
            mPassword = password;
            mRsaPublicKey = rsaPublicKey;
            mOperation = 3;
            mLoginCallback = callback;
        }

        public AccountMgrCallable(final String account, IQueryIdCallback callback) {
            mAccount = account;
            mOperation = 4;
            mQueryIdCallback = callback;
        }

        public AccountMgrCallable(final String phoneNumber, IReqVerifyCodeCallback callback) {
            mAccount = phoneNumber;
            mOperation = 5;
            mReqVCodeCallback = callback;
        }

        @Override
        public Integer call() {
            int errCode = ErrCode.XOK;

            switch (mOperation) {
                case 1: {
                    CommonResult result = accountRegister(mAccount, mPassword, mVerifyCode);
                    if (mRegCallback != null) {
                        mRegCallback.onThirdAccountRegisterDone(result.mErrCode, result.mMessage,
                                                                mAccount, mPassword);
                    }
                } break;

                case 2: {
                    CommonResult result = accountUnregister(mAccount, mPassword);
                    if (mUnregCallback != null) {
                        mUnregCallback.onThirdAccountUnregisterDone(result.mErrCode, result.mMessage,
                                                                    mAccount, mPassword);
                    }
                } break;

                case 3: {
                    LoginResult result = accountLogin(mAccount, mPassword, mRsaPublicKey);
                    if (mLoginCallback != null) {
                        mLoginCallback.onThirdAccountLoginDone(result.mErrCode, result.mMessage,
                                                                mAccount, mPassword, mRsaPublicKey,
                                                                result.mLoginParam);
                    }
                    errCode = result.mErrCode;
                } break;

                case 4: {
                    QueryAccountIdResult result = queryAccountIdByName(mAccount);
                    if (mQueryIdCallback != null) {
                        mQueryIdCallback.onThirdAccountQueryIdDone(result.mErrCode, result.mMessage,
                                mAccount, result.mAccountId);
                    }
                    errCode = result.mErrCode;
                } break;

                case 5: {
                    CommonResult result = requestVerifyCode(mAccount);
                    if (mReqVCodeCallback != null) {
                        mReqVCodeCallback.onThirdAccountReqVCodeDone(result.mErrCode, result.mMessage,
                                mAccount);
                    }
                    errCode = result.mErrCode;
                } break;
            }

            return errCode;
        }
    }

    /**
     * @brief 在独立线程中执行第三方用户注册
     * @param accoutName : 要注册的用户账号
     * @param password : 要注册的账号密码
     * @param verifyCode : 手机验证码
     * @return 错误码
     */
    public int register(final String accoutName, final String password,
                        final String verifyCode, IRegisterCallback callback) {
        Future<Integer> future = mExecSrv.submit(new AccountMgrCallable(accoutName, password,
                                    verifyCode, callback));
        return ErrCode.XOK;
    }

    /**
     * @brief 在独立线程中执行第三方用户注销
     * @param accoutName : 要注册的用户账号
     * @return 错误码
     */
    public int unregister(final String accoutName, final String password, IUnregisterCallback callback) {
        Future<Integer> future = mExecSrv.submit(new AccountMgrCallable(accoutName, password, callback));
        return ErrCode.XOK;
    }

    /**
     * @brief 在独立线程中执行第三方用户登录
     * @param accoutName : 要注册的用户账号
     * @param password : 要注册的账号密码
     * @return 错误码
     */
    public int login(final String accoutName, final String password,
                     final String rsaPublicKey, ILoginCallback callback) {
        Future<Integer> future = mExecSrv.submit(new AccountMgrCallable(accoutName, password, rsaPublicKey, callback));
        return ErrCode.XOK;
    }

    /**
     * @brief 在独立线程中执行查询用户Id
     * @param accoutName : 要查询的用户账号
     * @return 错误码
     */
    public int queryId(final String accoutName, IQueryIdCallback callback) {
        Future<Integer> future = mExecSrv.submit(new AccountMgrCallable(accoutName, callback));
        return ErrCode.XOK;
    }

    /**
     * @brief 在独立线程中执行 请求手机验证码
     * @param phoneNumber : 要请求的手机号码
     * @return 错误码
     */
    public int requestPhoneVCode(final String phoneNumber, IReqVerifyCodeCallback callback) {
        Future<Integer> future = mExecSrv.submit(new AccountMgrCallable(phoneNumber, callback));
        return ErrCode.XOK;
    }


    /**
     * @brief 获取当前已经登录的账号名称
     * @return 登录的账号名称
     */
    public String getLoginAccountName() {
        return mLoginAccountName;
    }

    /**
     * @brief 获取当前已经登录的账号ID
     * @return 登录的账号ID
     */
    public String getLoginAccountId() {
        return mLoginAccountId;
    }

    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////// Methods for Account Register ////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    /**
     * @brief HTTP请求后，服务器回应数据
     */
    private static class ResponseObj {
        public int mErrorCode;              ///< 错误码
        public int mRespCode;               ///< 回应数据包中HTTP代码
        public String mTip;                 ///< 回应数据
        public JSONObject mRespJsonObj;     ///< 回应包中的JSON对象
    }

    private static class CommonResult {
        public int mErrCode = ErrCode.XOK;
        public String mMessage;
    }

    private static class LoginResult {
        public int mErrCode = ErrCode.XOK;
        public String mMessage;
        public IAccountMgr.LoginParam mLoginParam = new IAccountMgr.LoginParam();
    }

    private static class QueryAccountIdResult {
        public int mErrCode = ErrCode.XOK;
        public String mMessage;
        public String mAccountName;
        public String mAccountId;
    }

    /**
     * @brief 请求手机验证码
     * @param phoneNumber : 请求的手机号
     * @return 错误码
     */
    public CommonResult requestVerifyCode(final String phoneNumber)  {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        CommonResult result = new CommonResult();
        Log.d(TAG, "<requestVerifyCode> [Enter] phoneNumber=" + phoneNumber);

        // 请求URL
        String requestUrl = mThirdBaseUrl + "/sys-verification-code/v1/sendRegisterCode";

        // param内容
        params.put("mobile", phoneNumber);

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                null, params, body);
        if (responseObj == null) {
            Log.e(TAG, "<requestVerifyCode> [EXIT] failure with no response!");
            result.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return result;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            Log.e(TAG, "<requestVerifyCode> [EXIT] failure, mRespCode="
                    + responseObj.mRespCode);
            if ((responseObj.mRespCode == 9999) && (responseObj.mTip != null) &&
                    responseObj.mTip.contains("上一个验证码仍然有效")) {
                result.mErrCode = ErrCode.XERR_VCODE_VALID;
            } else {
                result.mErrCode = ErrCode.XERR_HTTP_RESP_CODE;
            }

            result.mMessage = responseObj.mTip;
            return result;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            Log.e(TAG, "<requestVerifyCode> [EXIT] failure, mErrorCode="
                    + responseObj.mErrorCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_DATA;
            result.mMessage = responseObj.mTip;
            return result;
        }

        Log.d(TAG, "<requestVerifyCode> [EXIT] successful");
        return result;
    }


    /**
     * @brief 第三方用户注册
     * @param accountName : 要注册的用户账号
     * @param password : 要注册的账号密码
     * @return 错误码
     */
    public CommonResult accountRegister(final String accountName, final String password,
                                        final String verifyCode)  {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        CommonResult result = new CommonResult();
        Log.d(TAG, "<accountRegister> [Enter] accoutName=" + accountName
                    + ", password=" + password);

        // 请求URL
        String requestUrl = mThirdBaseUrl + "/auth/register2";

        // body内容
        try {
            body.put("username", accountName);
            body.put("password", password);
            if (verifyCode != null) {
                body.put("verificationCode", verifyCode);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "<accountRegister> [Exit] failure with JSON exp!");
            result.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return result;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                null, params, body);
        if (responseObj == null) {
            Log.e(TAG, "<accountRegister> [EXIT] failure with no response!");
            result.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return result;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            Log.e(TAG, "<accountRegister> [EXIT] failure, mRespCode="
                    + responseObj.mRespCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_CODE;
            result.mMessage = responseObj.mTip;
            return result;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            Log.e(TAG, "<accountRegister> [EXIT] failure, mErrorCode="
                    + responseObj.mErrorCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_DATA;
            result.mMessage = responseObj.mTip;
            return result;
        }

        Log.d(TAG, "<accountRegister> [EXIT] successful");
        return result;
    }


    /**
     * @brief 第三方用户注销
     * @param accountName : 要注册的用户账号
     * @return 错误码
     */
    public CommonResult accountUnregister(final String accountName, final String password)  {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        CommonResult result = new CommonResult();
        Log.d(TAG, "<accountUnregister> [Enter] accoutName=" + accountName);


        // 请求URL
        String requestUrl = mThirdBaseUrl + "/auth/removeAccount";

        // body内容
        try {
            body.put("username", accountName);
            body.put("password", password);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "<accountUnregister> [Exit] failure with JSON exp!");
            result.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return result;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                null, params, body);
        if (responseObj == null) {
            Log.e(TAG, "<accountUnregister> [EXIT] failure with no response!");
            result.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return result;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            Log.e(TAG, "<accountUnregister> [EXIT] failure, mRespCode="
                    + responseObj.mRespCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_CODE;
            result.mMessage = responseObj.mTip;
            return result;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            Log.e(TAG, "<accountUnregister> [EXIT] failure, mErrorCode="
                    + responseObj.mErrorCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_DATA;
            result.mMessage = responseObj.mTip;
            return result;
        }

        Log.d(TAG, "<accountUnregister> [EXIT] successful");
        return result;
    }

    /**
     * @brief 第三方用户登录
     * @return 返回登录的信息
     */
    public LoginResult accountLogin(final String accountName,
                                    final String password,
                                    final String rsaPublicKey   )  {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        LoginResult result = new LoginResult();
        Log.d(TAG, "<accountLogin> [Enter] accoutName=" + accountName
                + ", password=" + password + ", rsaPublicKey=" + rsaPublicKey);

        // 请求URL
        String requestUrl = mThirdBaseUrl + "/auth/login";

        // body内容
        try {
            body.put("username", accountName);
            body.put("password", password);
            if (!TextUtils.isEmpty(rsaPublicKey)) {
                body.put("publicKey", rsaPublicKey);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "<accountLogin> [Exit] failure with JSON exp!");
            result.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return result;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                null, params, body);
        if (responseObj == null) {
            Log.e(TAG, "<accountLogin> [EXIT] failure with no response!");
            result.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return result;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            Log.e(TAG, "<accountLogin> [EXIT] failure, mRespCode="
                    + responseObj.mRespCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_CODE;
            result.mMessage = responseObj.mTip;
            return result;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            Log.e(TAG, "<accountLogin> [EXIT] failure, mErrorCode="
                    + responseObj.mErrorCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_DATA;
            result.mMessage = responseObj.mTip;
            return result;
        }

        // 解析账号信息
        try {
            JSONObject dataObj = responseObj.mRespJsonObj.getJSONObject("data");
            if (dataObj == null) {
                Log.e(TAG, "<accountLogin> [EXIT] failure, no data Obj");
                result.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
                return result;
            }
            JSONObject lsTokenObj = dataObj.getJSONObject("lsToken");
            if (lsTokenObj == null) {
                Log.e(TAG, "<accountLogin> [EXIT] failure, no lsToken");
                result.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
                return result;
            }
            JSONObject gyTokenObj = dataObj.getJSONObject("gyToken");
            if (gyTokenObj == null) {
                Log.e(TAG, "<accountLogin> [EXIT] failure, no gyToken");
                result.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
                return result;
            }

            //
            // 解析底层服务账号信息
            //
            result.mLoginParam.mAccount = parseJsonStringValue(gyTokenObj, "account", null);
            result.mLoginParam.mEndpoint = parseJsonStringValue(gyTokenObj,"endpoint", null);
            result.mLoginParam.mRegion = parseJsonStringValue(gyTokenObj,"region", null);
            result.mLoginParam.mExpiration = parseJsonIntValue(gyTokenObj, "expiration", -1);
            result.mLoginParam.mPlatformToken = parseJsonStringValue(gyTokenObj,"granwin_token", null);

            JSONObject poolObj = gyTokenObj.getJSONObject("pool");
            result.mLoginParam.mPoolIdentifier = parseJsonStringValue(poolObj,"identifier", null);
            result.mLoginParam.mPoolIdentityId = parseJsonStringValue(poolObj,"identityId", null);
            result.mLoginParam.mIdentityPoolId = parseJsonStringValue(poolObj,"identityPoolId", null);
            result.mLoginParam.mPoolToken = parseJsonStringValue(poolObj,"token", null);

            JSONObject proofObj = gyTokenObj.getJSONObject("proof");
            result.mLoginParam.mProofAccessKeyId = parseJsonStringValue(proofObj,"accessKeyId", null);
            result.mLoginParam.mProofSecretKey = parseJsonStringValue(proofObj,"secretKey", null);
            result.mLoginParam.mProofSessionToken = parseJsonStringValue(proofObj,"sessionToken", null);
            result.mLoginParam.mProofSessionExpiration = parseJsonLongValue(proofObj,"sessionExpiration", -1);

            result.mLoginParam.mInventDeviceName = parseInventDevName(result.mLoginParam.mPoolIdentifier);

            //
            // 解析灵隼账号信息
            //
            result.mLoginParam.mLsAccessToken = parseJsonStringValue(lsTokenObj,"access_token", null);
            result.mLoginParam.mLsTokenType = parseJsonStringValue(lsTokenObj,"token_type", null);
            result.mLoginParam.mLsRefreshToken = parseJsonStringValue(lsTokenObj,"refresh_token", null);
            result.mLoginParam.mLsExpiresIn = parseJsonLongValue(lsTokenObj,"expires_in", -1);
            result.mLoginParam.mLsScope = parseJsonStringValue(lsTokenObj,"scope", null);

            mLoginAccountName = accountName;
            String [] splitArray = result.mLoginParam.mInventDeviceName.split("-");
            if ((splitArray != null) && (splitArray.length >= 2)) {
                mLoginAccountId = splitArray[1];
            }
            result.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "<accountLogin> [JSONException], error=" + e);
            result.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return result;
        }

        Log.d(TAG, "<accountLogin> [EXIT] successful");
        return result;
    }

    /**
     * @brief 根据 账号名称 查询 账号Id
     */
    public QueryAccountIdResult queryAccountIdByName(final String accountName) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        QueryAccountIdResult result = new QueryAccountIdResult();
        Log.d(TAG, "<queryAccountIdByName> [Enter] accountName=" + accountName);

        // 请求URL
        String requestUrl = mThirdBaseUrl + "/auth/getUidByUsername";

        // body内容
        try {
            body.put("username", accountName);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "<queryAccountIdByName> [Exit] failure with JSON exp!");
            result.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return result;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                null, params, body);
        if (responseObj == null) {
            Log.e(TAG, "<queryAccountIdByName> [EXIT] failure with no response!");
            result.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return result;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            Log.e(TAG, "<queryAccountIdByName> [EXIT] failure, mRespCode="
                    + responseObj.mRespCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_CODE;
            result.mMessage = responseObj.mTip;
            return result;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            Log.e(TAG, "<queryAccountIdByName> [EXIT] failure, mErrorCode="
                    + responseObj.mErrorCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_DATA;
            result.mMessage = responseObj.mTip;
            return result;
        }

        // 解析账号信息
        try {
            result.mAccountId = responseObj.mRespJsonObj.getString("data");
            result.mAccountName = accountName;
            result.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "<queryAccountIdByName> [JSONException], error=" + e);
            result.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return result;
        }

        Log.d(TAG, "<queryAccountIdByName> [EXIT] successful, mAccountId=" + result.mAccountId);
        return result;
    }


    /**
     * @brief 根据 pool_identifier 来解析虚拟设备影子名称
     */
    String parseInventDevName(final String poolIdentifier) {
        if (TextUtils.isEmpty(poolIdentifier)) {
            return "";
        }

        Pattern pattern = Pattern.compile("_");
        final String[] segments = pattern.split(poolIdentifier);
        if (segments == null) {
            Log.e(TAG, "<parseInventDevName> invalid poolIdentifier=" + poolIdentifier);
            return "";
        }
        if (segments.length < 3) {
            Log.e(TAG, "<parseInventDevName> less poolIdentifier=" + poolIdentifier);
            return "";
        }

        String inventDevName = segments[0] + "-" + segments[2];
        return inventDevName;
    }





    ////////////////////////////////////////////////////////////////////////
    ///////////////////////////// Inner Methods ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 给服务器发送HTTP请求，并且等待接收回应数据
     *        该函数是阻塞等待调用，因此最好是在工作线程中执行
     */
    private synchronized ThirdAccountMgr.ResponseObj requestToServer(String baseUrl, String method, String token,
                                                                    Map<String, String> params, JSONObject body) {

        ThirdAccountMgr.ResponseObj responseObj = new ThirdAccountMgr.ResponseObj();

        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            responseObj.mErrorCode = ErrCode.XERR_HTTP_URL;
            Log.e(TAG, "<requestToServer> Invalid url=" + baseUrl);
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

        Log.d(TAG, "<requestToServer> requestUrl=" + realURL
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
                connection.setRequestProperty("authorization", "Bearer " + token);
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
                    Log.e(TAG, "<requestToServer> Invalid method=" + method);
                    responseObj.mErrorCode = ErrCode.XERR_HTTP_METHOD;
                    return responseObj;
            }
            connection.setReadTimeout(HTTP_TIMEOUT);
            connection.setConnectTimeout(HTTP_TIMEOUT);
            responseObj.mRespCode = connection.getResponseCode();
            if (responseObj.mRespCode != HttpURLConnection.HTTP_OK) {
                responseObj.mErrorCode = ErrCode.XERR_HTTP_RESP_CODE + responseObj.mRespCode;
                Log.e(TAG, "<requestToServer> Error response code="
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
                responseObj.mTip = responseObj.mRespJsonObj.getString("msg");

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "<requestToServer> Invalied json=" + response);
                responseObj.mErrorCode = ErrCode.XERR_HTTP_RESP_DATA;
                responseObj.mRespJsonObj = null;
            }

            Log.d(TAG, "<requestToServer> finished, response="  + response.toString());
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
     * @brief 发送HTTP请求上传文件处理，并且等待接收回应数据
     *        该函数是阻塞等待调用，因此最好是在工作线程中执行
     */
    private synchronized ThirdAccountMgr.ResponseObj requestFileToServer(String baseUrl,
                                                                      String token,
                                                                      String fileName,
                                                                      String fileDir,
                                                                      boolean rename,
                                                                      byte[] fileContent ) {

        ThirdAccountMgr.ResponseObj responseObj = new ThirdAccountMgr.ResponseObj();

        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            responseObj.mErrorCode = ErrCode.XERR_HTTP_URL;
            Log.e(TAG, "<requestFileToServer> Invalid url=" + baseUrl);
            return responseObj;
        }

        // 拼接URL和请求参数生成最终URL
        String realURL = baseUrl;
        Log.d(TAG, "<requestFileToServer> requestUrl=" + realURL);


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
                connection.setRequestProperty("authorization", "Bearer " + token);
            }


            final String NEWLINE = "\r\n";
            final String PREFIX = "--";
            final String BOUNDARY = "########";


            // 调用HttpURLConnection对象setDoOutput(true)、setDoInput(true)、setRequestMethod("POST")；
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");

            // 设置Http请求头信息；（Accept、Connection、Accept-Encoding、Cache-Control、Content-Type、User-Agent）
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

            // 调用HttpURLConnection对象的connect()方法，建立与服务器的真实连接；
            connection.connect();

            // 调用HttpURLConnection对象的getOutputStream()方法构建输出流对象；
            DataOutputStream os = new DataOutputStream(connection.getOutputStream());

            //
            // 写入文件头的键值信息
            //
            String fileKey = "file";
            String fileContentType = "image/jpeg";
            String fileHeader = "Content-Disposition: form-data; name=\"" + fileKey
                                + "\"; filename=\"" + fileName + "\"" + NEWLINE;
            String contentType = "Content-Type: " + fileContentType + NEWLINE;
            String encodingType = "Content-Transfer-Encoding: binary" + NEWLINE;
            os.writeBytes(PREFIX + BOUNDARY + NEWLINE);
            os.writeBytes(fileHeader);
            os.writeBytes(contentType);
            os.writeBytes(encodingType);
            os.writeBytes(NEWLINE);

            //
            // 写入文件内容
            //
            os.write(fileContent);
            os.writeBytes(NEWLINE);

            //
            // 写入其他参数数据
            //
            Map<String, String> params = new HashMap<String, String>();
            params.put("fileName", fileName);
            params.put("fileDir", fileDir);
            params.put("renameFile", (rename ? "true" : "false"));

            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = params.get(key);

                os.writeBytes(PREFIX + BOUNDARY + NEWLINE);
                os.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + NEWLINE);
                os.writeBytes(NEWLINE);

                os.write(value.getBytes());
                os.writeBytes(NEWLINE);
            }

            //
            // 写入整体结束所有的数据
            //
            os.writeBytes(PREFIX + BOUNDARY + PREFIX + NEWLINE);
            os.flush();
            os.close();

            connection.setReadTimeout(HTTP_TIMEOUT);
            connection.setConnectTimeout(HTTP_TIMEOUT);
            responseObj.mRespCode = connection.getResponseCode();
            if (responseObj.mRespCode != HttpURLConnection.HTTP_OK) {
                responseObj.mErrorCode = ErrCode.XERR_HTTP_RESP_CODE + responseObj.mRespCode;
                Log.e(TAG, "<requestFileToServer> Error response code="
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
                responseObj.mTip = responseObj.mRespJsonObj.getString("timestamp");

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "<requestFileToServer> Invalied json=" + response);
                responseObj.mErrorCode = ErrCode.XERR_HTTP_RESP_DATA;
                responseObj.mRespJsonObj = null;
            }

            Log.d(TAG, "<requestFileToServer> finished, response="  + response.toString());
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

    int parseJsonIntValue(JSONObject jsonState, String fieldName, int defVal) {
        try {
            int value = jsonState.getInt(fieldName);
            return value;

        } catch (JSONException e) {
            Log.e(TAG, "<parseJsonIntValue> "
                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return defVal;
        }
    }

    long parseJsonLongValue(JSONObject jsonState, String fieldName, long defVal) {
        try {
            long value = jsonState.getLong(fieldName);
            return value;

        } catch (JSONException e) {
            Log.e(TAG, "<parseJsonLongValue> "
                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return defVal;
        }
    }

    boolean parseJsonBoolValue(JSONObject jsonState, String fieldName, boolean defVal) {
        try {
            boolean value = jsonState.getBoolean(fieldName);
            return value;

        } catch (JSONException e) {
            Log.e(TAG, "<parseJsonBoolValue> "
                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return defVal;
        }
    }

    String parseJsonStringValue(JSONObject jsonState, String fieldName, String defVal) {
        try {
            String value = jsonState.getString(fieldName);
            return value;

        } catch (JSONException e) {
            Log.e(TAG, "<parseJsonIntValue> "
                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return defVal;
        }
    }

    JSONArray parseJsonArray(JSONObject jsonState, String fieldName) {
        try {
            JSONArray jsonArray = jsonState.getJSONArray(fieldName);
            return jsonArray;

        } catch (JSONException e) {
            Log.e(TAG, "<parseJsonArray> "
                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return null;
        }
    }



}
