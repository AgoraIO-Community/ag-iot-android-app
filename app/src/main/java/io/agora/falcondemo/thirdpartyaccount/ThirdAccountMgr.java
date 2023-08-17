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

package io.agora.falcondemo.thirdpartyaccount;



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
import io.agora.iotlink.ErrCode;


public class ThirdAccountMgr {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////
    public static class RegisterParam {
        public String mUserId;
        public int mClientType;
        public String mMasterAppId;

        @Override
        public String toString() {
            String infoText = "{ mUserId=" + mUserId
                    + ", mClientType=" + mClientType
                    + ", mMasterAppId=" + mMasterAppId + " }";
            return infoText;
        }
    }


    public interface IRegisterCallback{
        void onThirdAccountRegisterDone(int errCode, final String errMsg,
                                        final RegisterParam registerParam,
                                        final String retrievedNodeId, final String region);
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
    private String mThirdBaseUrl = "https://iot-api-gateway.sh.agoralab.co/api";



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
        private int mOperation = 1;
        private RegisterParam mRegParam;
        private IRegisterCallback mRegCallback;


        public AccountMgrCallable(final RegisterParam registerParam, IRegisterCallback callback) {
            mRegParam = registerParam;
            mRegCallback = callback;
        }

        @Override
        public Integer call() {
            int errCode = ErrCode.XOK;

            switch (mOperation) {
                case 1: {
                    RegsiterResult result = accountRegister(mRegParam);
                    if (mRegCallback != null) {
                        mRegCallback.onThirdAccountRegisterDone(result.mErrCode, result.mMessage,
                                                                mRegParam,
                                                                result.mNodeId, result.mRegion);
                    }
                    errCode = result.mErrCode;
                } break;

            }

            return errCode;
        }
    }

    /**
     * @brief 在独立线程中执行第三方用户注册
     * @param registerParam : 要注册的用户账号
     * @param callback : 异步回调函数
     * @return 错误码
     */
    public int register(final RegisterParam registerParam, IRegisterCallback callback) {
        Future<Integer> future = mExecSrv.submit(new AccountMgrCallable(registerParam, callback));
        return ErrCode.XOK;
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

    private static class RegsiterResult {
        public int mErrCode = ErrCode.XOK;
        public String mMessage;
        public String mNodeId;
        public String mRegion;

        @Override
        public String toString() {
            String infoText = "{ mErrCode=" + mErrCode
                    + ", mMessage=" + mMessage
                    + ", mNodeId=" + mNodeId
                    + ", mRegion=" + mRegion + " }";
            return infoText;
        }
    }



    /**
     * @brief 第三方用户注册
     * @param registerParam : 要注册的用户账号信息
     * @return 注册结果信息
     */
    private RegsiterResult accountRegister(final RegisterParam registerParam)  {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        RegsiterResult result = new RegsiterResult();
        Log.d(TAG, "<accountRegister> [Enter] registerParam=" + registerParam.toString());

        // 请求URL
        String requestUrl = mThirdBaseUrl + "/iot-core/v2/secret-node/user/create";

        // body内容
        try {
            JSONObject header = new JSONObject();
            header.put("traceId", registerParam.mMasterAppId + "-" + registerParam.mUserId );
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);

            JSONObject payloadObj = new JSONObject();
            payloadObj.put("masterAppId", registerParam.mMasterAppId);
            payloadObj.put("userId", registerParam.mUserId);
            payloadObj.put("clientType", registerParam.mClientType);
            body.put("payload", payloadObj);

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

        // 解析呼叫请求返回结果
        try {
            JSONObject dataObj = responseObj.mRespJsonObj.getJSONObject("data");
            result.mNodeId = parseJsonStringValue(dataObj, "nodeId", null);
            result.mRegion = parseJsonStringValue(dataObj, "region", null);
            result.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "<getRtcPlayToken> JSONException=" + e.toString());
            result.mErrCode =  ErrCode.XERR_HTTP_JSON_PARSE;
            return result;
        }

        Log.d(TAG, "<accountRegister> [EXIT] successful, result=" + result);
        return result;
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
