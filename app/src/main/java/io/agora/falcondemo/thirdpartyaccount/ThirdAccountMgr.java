package io.agora.falcondemo.thirdpartyaccount;



import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.Base64;
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
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.logger.ALog;


public class ThirdAccountMgr {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 创建用户的参数
     */
    public static class UserCreateParam {
        public String mAppId;
        public String mUserId;
        public int mClientType;
        public String mAuthKey;
        public String mAuthSecret;
        public  String mRegion;

        @Override
        public String toString() {
            String infoText = "{ mAppId=" + mAppId
                    + ", mAuthKey=" + mAuthKey
                    + ", mAuthSecret=" + mAuthSecret
                    + ", mUserId=" + mUserId
                    + ", mClientType=" + mClientType
                    + ", mRegion=" + mRegion  + " }";
            return infoText;
        }
    }

    /**
     * @brief 创建用户的结果
     */
    public static class UserCreateResult {
        public int mErrCode;                ///< 错误码
        public int mRespCode;               ///< 回应数据包中HTTP代码
        public String mMessage;             ///< 回应数据
        public long mTimestamp;             ///< 回应时间戳
        public String mTraceId;             ///< 跟踪的traceId，对于创建链接回应是空
        public String mNodeId;              ///< 创建的 用户账号 nodeId
        public int mRegion;                 ///< 用户所在区域

        @Override
        public String toString() {
            String infoText = "{ mErrCode=" + mErrCode
                    + ", mRespCode=" + mRespCode + ", mMessage=" + mMessage
                    + ", mTimestamp=" + mTimestamp + ", mTraceId=" + mTraceId
                    + ", mNodeId=" + mNodeId + ", mRegion=" + mRegion  + " }";
            return infoText;
        }
    }

    /**
     * @brief 创建用户的回调函数
     */
    public interface IUserCreateCallback{
        void onThirdAccountUserCreateDone(final UserCreateParam createParam,
                                          final UserCreateResult createResult);
    }




    /**
     * @brief 激活用户的参数
     */
    public static class UserActiveParam {
        public String mAppId;
        public String mUserId;
        public int mClientType;
        public String mPusherId;
        public String mAuthKey;
        public String mAuthSecret;
        public  String mRegion;

        @Override
        public String toString() {
            String infoText = "{ mAppId=" + mAppId
                    + ", mAuthKey=" + mAuthKey
                    + ", mAuthSecret=" + mAuthSecret
                    + ", mUserId=" + mUserId
                    + ", mClientType=" + mClientType
                    + ", mPusherId=" + mPusherId
                    + ", mRegion=" + mRegion  + " }";
            return infoText;
        }
    }

    /**
     * @brief 激活用户的结果
     */
    public static class UserActiveResult {
        public int mErrCode;                ///< 错误码
        public int mRespCode;               ///< 回应数据包中HTTP代码
        public String mMessage;             ///< 回应数据
        public long mTimestamp;             ///< 回应时间戳
        public String mTraceId;             ///< 跟踪的traceId，对于创建链接回应是空

        public String mNodeId;              ///< 创建的 用户账号 nodeId
        public int mNodeRegion;             ///< 用户所在区域
        public String mNodeToken;           ///< 节点token

        @Override
        public String toString() {
            String infoText = "{ mErrCode=" + mErrCode
                    + ", mRespCode=" + mRespCode + ", mMessage=" + mMessage
                    + ", mTimestamp=" + mTimestamp + ", mTraceId=" + mTraceId
                    + ", mNodeId=" + mNodeId + ", mNodeRegion=" + mNodeRegion
                    + ", mNodeToken=" + mNodeToken + " }";
            return infoText;
        }
    }

    /**
     * @brief 激活用户的回调函数
     */
    public interface IUserActiveCallback{
        void onThirdAccountUserActiveDone(final UserActiveParam activeParam,
                                          final UserActiveResult activeResult);
    }






    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/ThridAccountMgr";
    private static final int HTTP_TIMEOUT = 8000;
    /*
    private static final String BASE_URL = "http://api.sd-rtn.com/cn/iot/link";
    private static final String BASE_URL = "http://api-test-huzhou1.agora.io/cn/iot/link";
     */
    private static final String BASE_URL_HEAD = "http://api.sd-rtn.com/";
    private static final String BASE_URL_TAIL = "/iot/link";

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static ThirdAccountMgr mInstance = null;
    private ExecutorService mExecSrv = Executors.newSingleThreadExecutor();
    private UserActiveResult mActiveResult;

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


    /**
     * @brief 在独立线程中执行第三方用户创建
     * @param createParam : 要创建的用户账号信息
     * @param callback : 异步回调函数
     * @return 异步Future
     */
    public Future<Integer> userCreate(final UserCreateParam createParam, IUserCreateCallback callback) {
        Future<Integer> future = mExecSrv.submit(new AccountMgrCallable(createParam, callback));
        return future;
    }

    /**
     * @brief 在独立线程中执行第三方用户激活
     * @param activeParam : 要激活的用户账号信息
     * @param callback : 异步回调函数
     * @return 异步Future
     */
    public Future<Integer> userActive(final UserActiveParam activeParam, IUserActiveCallback callback) {
        Future<Integer> future = mExecSrv.submit(new AccountMgrCallable(activeParam, callback));
        return future;
    }

    /**
     * @brief 第三方用户非激活，将已经激活的用户信息清除即可
     */
    public void userDeactive() {
        mActiveResult = null;
    }

    /**
     * @brief 获取当前已经登录的 本地NodeId
     */
    public String getLocalNodeId() {
        if (mActiveResult == null) {
            return null;
        }
        return mActiveResult.mNodeId;
    }

    /**
     * @brief 获取HTTP请求的基本URL
     */
//    public String getRequestBaseUrl() {
//        return BASE_URL;
//    }



    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////// Internal Methods ///////////////////////////
    //////////////////////////////////////////////////////////////////////////
    /*
     * @brief HTTP请求后，服务器回应数据
     */
    private static class ResponseObj {
        public int mErrorCode;              ///< 错误码
        public int mRespCode;               ///< 回应数据包中HTTP代码
        public String mTip;                 ///< 回应数据
        public JSONObject mRespJsonObj;     ///< 回应包中的JSON对象
    }

    /**
     * @brief 线程中执行的账号管理
     */
    private class AccountMgrCallable implements Callable<Integer> {
        private int mOperation = 1;    // 1: 创建用户；  2: 激活用户

        private UserCreateParam mCreateParam;
        private IUserCreateCallback mCreateCallback;

        private UserActiveParam mActiveParam;
        private IUserActiveCallback mActiveCallback;

        public AccountMgrCallable(final UserCreateParam createParam, IUserCreateCallback createCallback) {
            mCreateParam = createParam;
            mCreateCallback = createCallback;
            mOperation = 1;
        }

        public AccountMgrCallable(final UserActiveParam activeParam, IUserActiveCallback activeCallback) {
            mActiveParam = activeParam;
            mActiveCallback = activeCallback;
            mOperation = 2;
        }


        @Override
        public Integer call() {
            int errCode = ErrCode.XOK;

            switch (mOperation) {
                case 1: {   // 创建用户
                    UserCreateResult result = userAccountCreate(mCreateParam);
                    if (mCreateCallback != null) {
                        mCreateCallback.onThirdAccountUserCreateDone(mCreateParam, result);
                    }
                    errCode = result.mErrCode;
                } break;

                case 2: {   // 激活用户
                    UserActiveResult result = userAccountActive(mActiveParam);
                    mActiveResult = result;
                    if (mActiveCallback != null) {
                        mActiveCallback.onThirdAccountUserActiveDone(mActiveParam, result);
                    }
                    errCode = result.mErrCode;
                } break;
            }

            return errCode;
        }
    }

    /**
     * @brief 创建用户操作
     * @param createParam : 要创建的信息
     * @return 创建结果
     */
    private UserCreateResult userAccountCreate(final UserCreateParam createParam) {
        UserCreateResult result = new UserCreateResult();
        ALog.getInstance().d(TAG, "<userAccountCreate> [Enter] createParam=" + createParam);

        String serverUrl = BASE_URL_HEAD + getRegionText(createParam.mRegion) + BASE_URL_TAIL;
        // 请求URL
        String requestUrl = serverUrl + "/open-api/v2/iot-core/secret-node/user/create";

        // body内容
        JSONObject body = new JSONObject();
        try {
            body.put("appId", createParam.mAppId);
            body.put("userId", createParam.mUserId);
            body.put("clientType", createParam.mClientType);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "<userAccountCreate> [Exit] failure with JSON exp!");
            result.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            result.mMessage = "Fail_JSON_request";
            return result;
        }

        String basicAuth = generateBasicAuth(createParam.mAuthKey, createParam.mAuthSecret);
        ResponseObj responseObj = requestToServer(requestUrl, basicAuth, null, body);
        if (responseObj.mRespCode != ErrCode.XOK) {
            Log.e(TAG, "<userAccountCreate> [EXIT] failure, mRespCode=" + responseObj.mRespCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_CODE;
            result.mRespCode = responseObj.mRespCode;
            result.mMessage = responseObj.mTip;
            return result;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            Log.e(TAG, "<userAccountCreate> [EXIT] failure, mErrorCode=" + responseObj.mErrorCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_DATA;
            result.mRespCode = responseObj.mRespCode;
            result.mMessage = responseObj.mTip;
            return result;
        }

        try {
            result.mTimestamp =  parseJsonLongValue(responseObj.mRespJsonObj, "timestamp", -1);
            result.mTraceId =  parseJsonStringValue(responseObj.mRespJsonObj, "traceId", null);
            JSONObject dataObj = responseObj.mRespJsonObj.getJSONObject("data");
            result.mNodeId = parseJsonStringValue(dataObj, "nodeId", null);
            result.mRegion = parseJsonIntValue(dataObj, "region", -1);

            result.mRespCode = ErrCode.XOK;
            result.mErrCode = ErrCode.XOK;
            result.mMessage = responseObj.mTip;

        } catch (JSONException jsonExp) {
            jsonExp.printStackTrace();
            Log.e(TAG, "<userAccountCreate> [JSON_EXP] jsonExp=" + jsonExp);
            result.mErrCode =  ErrCode.XERR_HTTP_JSON_PARSE;
            result.mMessage = "Invalid_JSON_response";
            return result;
        }

        Log.d(TAG, "<userAccountCreate> [EXIT] successful, result=" + result);
        return result;
    }


    /**
     * @brief 激活用户操作
     * @param activeParam : 要激活的信息
     * @return 激活结果
     */
    private UserActiveResult userAccountActive(final UserActiveParam activeParam) {
        UserActiveResult result = new UserActiveResult();
        ALog.getInstance().d(TAG, "<userAccountActive> [Enter] activeParam=" + activeParam);

        String serverUrl = BASE_URL_HEAD + getRegionText(activeParam.mRegion) + BASE_URL_TAIL;
        // 请求URL
        String requestUrl = serverUrl + "/open-api/v2/iot-core/secret-node/user/activate";

        // body内容
        JSONObject body = new JSONObject();
        try {
            body.put("appId", activeParam.mAppId);
            body.put("userId", activeParam.mUserId);
            body.put("clientType", activeParam.mClientType);
            body.put("pusherId", activeParam.mPusherId);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "<userAccountActive> [Exit] failure with JSON exp!");
            result.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            result.mMessage = "Fail_JSON_request";
            return result;
        }

        String basicAuth = generateBasicAuth(activeParam.mAuthKey, activeParam.mAuthSecret);
        ResponseObj responseObj = requestToServer(requestUrl, basicAuth, null, body);
        if (responseObj.mRespCode != ErrCode.XOK) {
            Log.e(TAG, "<userAccountActive> [EXIT] failure, mRespCode=" + responseObj.mRespCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_CODE;
            result.mRespCode = responseObj.mRespCode;
            result.mMessage = responseObj.mTip;
            return result;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            Log.e(TAG, "<userAccountActive> [EXIT] failure, mErrorCode=" + responseObj.mErrorCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_DATA;
            result.mRespCode = responseObj.mRespCode;
            result.mMessage = responseObj.mTip;
            return result;
        }


        try {
            result.mTimestamp =  parseJsonLongValue(responseObj.mRespJsonObj, "timestamp", -1);
            result.mTraceId =  parseJsonStringValue(responseObj.mRespJsonObj, "traceId", null);

            JSONObject dataObj = responseObj.mRespJsonObj.getJSONObject("data");
            result.mNodeId = parseJsonStringValue(dataObj, "nodeId", null);
            result.mNodeRegion = parseJsonIntValue(dataObj, "nodeRegion", -1);
            result.mNodeToken = parseJsonStringValue(dataObj, "nodeToken", null);

            result.mRespCode = ErrCode.XOK;
            result.mErrCode = ErrCode.XOK;
            result.mMessage = responseObj.mTip;

        } catch (JSONException jsonExp) {
            jsonExp.printStackTrace();
            Log.e(TAG, "<userAccountActive> [JSON_EXP] jsonExp=" + jsonExp);
            result.mErrCode =  ErrCode.XERR_HTTP_JSON_PARSE;
            result.mMessage = "Invalid_JSON_response";
            return result;
        }

        Log.d(TAG, "<userAccountActive> [EXIT] successful, result=" + result);
        return result;
    }



    ////////////////////////////////////////////////////////////////////////
    ///////////////////////////// Inner Methods ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    String generateBasicAuth(final String key, final String secret) {
        String auth = key + ":" + secret;
        byte[] authBytes = auth.getBytes(Charset.forName("UTF-8"));
        String authHeader = "Basic " + Base64.encodeToString(authBytes, Base64.NO_WRAP);
        return authHeader;
    }

    private String getRegionText(String mRegion){
        String textRegion = "";
        switch (mRegion) {
            case "CN": // 中国大陆
                textRegion = "cn";
                break;
            case "NA": // 北美
                textRegion = "na";
                break;
            case "AP": // 亚太
                textRegion = "ap";
                break;
            case "EU": // 欧洲
                textRegion = "eu";
                break;
        }
        return  textRegion;
    }


    /**
     * @brief 给服务器发送HTTP请求，并且等待接收回应数据
     *        该函数是阻塞等待调用，因此最好是在工作线程中执行
     */
    private synchronized ResponseObj requestToServer(String baseUrl, String basicAuth,
                                                     Map<String, String> params, JSONObject body) {
        ResponseObj responseObj = new ResponseObj();

        if ((!baseUrl.startsWith("http://")) && (!baseUrl.startsWith("https://"))) {
            responseObj.mErrorCode = ErrCode.XERR_HTTP_URL;
            responseObj.mTip = "Invalid_URL";
            ALog.getInstance().e(TAG, "<requestToServer> Invalid url=" + baseUrl);
            return responseObj;
        }

        // 拼接URL和请求参数生成最终URL
        String realURL = baseUrl;
        if ((params != null) && (!params.isEmpty())) {
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
                + ", requestBody="  + realBody.toString()
                + ", basicAuth=" + basicAuth);

        //开启子线程来发起网络请求
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder();


        //同步方式请求HTTP，因此请求操作最好放在工作线程中进行
        try {
            java.net.URL url = new URL(realURL);
            connection = (HttpURLConnection) url.openConnection();
            // 设置 basic auth
            if (!TextUtils.isEmpty(basicAuth)) {
                connection.setRequestProperty("Authorization", basicAuth);
            }
            connection.setReadTimeout(HTTP_TIMEOUT);
            connection.setConnectTimeout(HTTP_TIMEOUT);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            DataOutputStream os = new DataOutputStream(connection.getOutputStream());
            os.write(realBody.getBytes());  // 必须是原始数据流，否则中文乱码
            os.flush();
            os.close();

            responseObj.mRespCode = connection.getResponseCode();
            if (responseObj.mRespCode != HttpURLConnection.HTTP_OK) {
                responseObj.mErrorCode = ErrCode.XERR_HTTP_RESP_CODE + responseObj.mRespCode;
                responseObj.mTip = connection.getResponseMessage();
                if (responseObj.mRespCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    responseObj.mTip = "Invalid_appId";
                }
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
                responseObj.mTip = responseObj.mRespJsonObj.getString("msg");

            } catch (JSONException e) {
                e.printStackTrace();
                ALog.getInstance().e(TAG, "<requestToServer> Invalied json=" + response);
                responseObj.mErrorCode = ErrCode.XERR_HTTP_RESP_DATA;
                responseObj.mTip = "Invalid_JSON_response";
                responseObj.mRespJsonObj = null;
            }

            ALog.getInstance().d(TAG, "<requestToServer> finished, response="  + response.toString());
            return responseObj;

        } catch (Exception e) {
            e.printStackTrace();
            responseObj.mErrorCode = ErrCode.XERR_HTTP_CONNECT;
            responseObj.mTip = "Fail_to_connect_server";
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
