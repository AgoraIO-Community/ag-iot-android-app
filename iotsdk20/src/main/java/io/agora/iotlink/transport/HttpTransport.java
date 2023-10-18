package io.agora.iotlink.transport;


import android.util.Log;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;


public class HttpTransport {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief HTTP请求后，服务器回应数据
     */
    private static class ResponseObj {
        public int mErrorCode;              ///< 错误码
        public int mRespCode;               ///< 回应数据包中HTTP代码
        public String mTip;                 ///< 回应数据
        public JSONObject mRespJsonObj;     ///< 回应包中的JSON对象
    }

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/HttpTransport";
    private static final int HTTP_TIMEOUT = 8000;



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static HttpTransport mInstance = null;

    ///< 服务器请求站点
    private String mBaseUrl = "https://iot-api-gateway.sh.agoralab.co/api";


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public static HttpTransport getInstance() {
        if (mInstance == null) {
            synchronized (HttpTransport.class) {
                if (mInstance == null) {
                    mInstance = new HttpTransport();
                }
            }
        }
        return mInstance;
    }

    public void setBaseUrl(final String baseUrl) {
        mBaseUrl = baseUrl;
        ALog.getInstance().d(TAG, "<setBaseUrl> mBaseUrl=" + mBaseUrl);
    }


    //////////////////////////////////////////////////////////////////////////////////
    ////////////////////////// Methods for Node Manager Module ////////////////////////
    //////////////////////////////////////////////////////////////////////////////////

    public static class NodeActiveResult {
        public int mErrCode = ErrCode.XOK;
        public String mMessage;

        public String mAppId;
        public String mUserId;
        public int mClientType;
        public String mPusherId;

        public String mNodeId;
        public String mNodeRegion;
        public String mNodeToken;

        public String mMqttServer;
        public int mMqttPort;
        public String mMqttUserName;
        public String mMqttSalt;

        @Override
        public String toString() {
            String infoText = "{ mErrCode=" + mErrCode + ", mMessage=" + mMessage
                    + ", mAppId=" + mAppId + ", mUserId=" + mUserId + ", mClientType=" + mClientType
                    + ", mNodeId=" + mNodeId + ", mNodeRegion=" + mNodeRegion
                    + ", mNodeToken=" + mNodeToken + ", mPUsherId=" + mPusherId
                    + ", mMqttServer=" + mMqttServer + ", mMqttPort=" + mMqttPort
                    + ", mMqttUserName=" + mMqttUserName
                    + ", mMqttSalt=" + mMqttSalt  + " }";
            return infoText;
        }
    }

    /**
     * @brief 节点激活操作
     * @param prepareParam : 要激活的节点信息
     * @return 注册结果信息
     */
    public NodeActiveResult nodeActive(final String appId,
                                       final IAgoraIotAppSdk.LoginParam prepareParam)  {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        NodeActiveResult result = new NodeActiveResult();
        ALog.getInstance().d(TAG, "<nodeActive> [Enter] prepareParam=" + prepareParam.toString());

        // 请求URL
        String requestUrl = mBaseUrl + "/iot-core/v2/secret-node/user/activate";

        // body内容
        try {
            JSONObject header = new JSONObject();
            header.put("traceId", UUID.randomUUID().toString() );
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);

            JSONObject payloadObj = new JSONObject();
            payloadObj.put("masterAppId", appId);
            payloadObj.put("userId", prepareParam.mUserId);
            payloadObj.put("clientType", prepareParam.mClientType);
            payloadObj.put("pusherId", "00000000");
            body.put("payload", payloadObj);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "<nodeActive> [Exit] failure with JSON exp!");
            result.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return result;
        }

        ResponseObj responseObj = requestToServer(requestUrl, "POST",
                null, params, body);
        if (responseObj == null) {
            Log.e(TAG, "<nodeActive> [EXIT] failure with no response!");
            result.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return result;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            Log.e(TAG, "<nodeActive> [EXIT] failure, mRespCode=" + responseObj.mRespCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_CODE;
            result.mMessage = responseObj.mTip;
            return result;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            Log.e(TAG, "<accountRegister> [EXIT] failure, mErrorCode=" + responseObj.mErrorCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_DATA;
            result.mMessage = responseObj.mTip;
            return result;
        }

        // 解析呼叫请求返回结果
        result.mAppId = appId;
        result.mUserId = prepareParam.mUserId;
        result.mClientType = prepareParam.mClientType;
        result.mPusherId = "00000000";

        try {
            JSONObject dataObj = responseObj.mRespJsonObj.getJSONObject("data");
            result.mNodeId = parseJsonStringValue(dataObj, "nodeId", null);
            result.mNodeRegion = parseJsonStringValue(dataObj, "nodeRegion", null);
            result.mNodeToken = parseJsonStringValue(dataObj, "nodeToken", null);

            result.mMqttServer = parseJsonStringValue(dataObj, "mqttServer", null);
            result.mMqttPort = parseJsonIntValue(dataObj, "mqttPort", 18085);
            result.mMqttUserName = parseJsonStringValue(dataObj, "mqttUsername", null);
            result.mMqttSalt = parseJsonStringValue(dataObj, "mqttSalt", null);

            result.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "<nodeActive> JSONException=" + e.toString());
            result.mErrCode =  ErrCode.XERR_HTTP_JSON_PARSE;
            return result;
        }

        Log.d(TAG, "<nodeActive> [EXIT] successful, result=" + result);
        return result;
    }




    ////////////////////////////////////////////////////////////////////////
    ///////////////////////////// Inner Methods ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief 给服务器发送HTTP请求，并且等待接收回应数据
     *        该函数是阻塞等待调用，因此最好是在工作线程中执行
     */
    private synchronized HttpTransport.ResponseObj requestToServer(String baseUrl, String method, String token,
                                                                    Map<String, String> params, JSONObject body) {

        HttpTransport.ResponseObj responseObj = new HttpTransport.ResponseObj();

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
                connection.setRequestProperty("authorization", "Bearer " + token);
            }
            connection.setReadTimeout(HTTP_TIMEOUT);
            connection.setConnectTimeout(HTTP_TIMEOUT);
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
                responseObj.mTip = responseObj.mRespJsonObj.getString("timestamp");

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
     * @brief 发送HTTP请求上传文件处理，并且等待接收回应数据
     *        该函数是阻塞等待调用，因此最好是在工作线程中执行
     */
    private synchronized HttpTransport.ResponseObj requestFileToServer(String baseUrl,
                                                                      String token,
                                                                      String fileName,
                                                                      String fileDir,
                                                                      boolean rename,
                                                                      byte[] fileContent ) {

        HttpTransport.ResponseObj responseObj = new HttpTransport.ResponseObj();

        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            responseObj.mErrorCode = ErrCode.XERR_HTTP_URL;
            ALog.getInstance().e(TAG, "<requestFileToServer> Invalid url=" + baseUrl);
            return responseObj;
        }

        // 拼接URL和请求参数生成最终URL
        String realURL = baseUrl;
        ALog.getInstance().d(TAG, "<requestFileToServer> requestUrl=" + realURL);


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
                ALog.getInstance().e(TAG, "<requestFileToServer> Error response code="
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
                ALog.getInstance().e(TAG, "<requestFileToServer> Invalied json=" + response);
                responseObj.mErrorCode = ErrCode.XERR_HTTP_RESP_DATA;
                responseObj.mRespJsonObj = null;
            }

            ALog.getInstance().d(TAG, "<requestFileToServer> finished, response="  + response.toString());
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

    boolean parseJsonBoolValue(JSONObject jsonState, String fieldName, boolean defVal) {
        try {
            boolean value = jsonState.getBoolean(fieldName);
            return value;

        } catch (JSONException e) {
            ALog.getInstance().e(TAG, "<parseJsonBoolValue> "
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

    JSONArray parseJsonArray(JSONObject jsonState, String fieldName) {
        try {
            JSONArray jsonArray = jsonState.getJSONArray(fieldName);
            return jsonArray;

        } catch (JSONException e) {
            ALog.getInstance().e(TAG, "<parseJsonArray> "
                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return null;
        }
    }



}
