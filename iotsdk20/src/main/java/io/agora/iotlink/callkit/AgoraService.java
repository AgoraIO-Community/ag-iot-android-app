package io.agora.iotlink.callkit;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAlarmMgr;
import io.agora.iotlink.IDevMessageMgr;
import io.agora.iotlink.IotAlarm;
import io.agora.iotlink.IotAlarmImage;
import io.agora.iotlink.IotAlarmPage;
import io.agora.iotlink.IotAlarmVideo;
import io.agora.iotlink.IotDevMessage;
import io.agora.iotlink.IotDevMsgPage;
import io.agora.iotlink.IotDevice;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.utils.RSAUtils;

import com.amazonaws.http.HttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;



public class AgoraService {

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
    private static final String TAG = "IOTSDK/AgoraService";
    private static final int HTTP_TIMEOUT = 2500;

    public static final int RESP_CODE_IN_TALKING = 100001;      ///<	对端通话中，无法接听
    public static final int RESP_CODE_ANSWER = 100002;          ///<	未通话，无法接听
    public static final int RESP_CODE_HANGUP = 100003;          ///<	未通话，无法挂断
    public static final int RESP_CODE_ANSWER_TIMEOUT = 100004;  ///< 接听等待超时
    public static final int RESP_CODE_CALL = 100005;            ///< 呼叫中，无法再次呼叫
    public static final int RESP_CODE_INVALID_ANSWER = 100006;  ///< 无效的Answer应答
    public static final int RESP_CODE_PEER_UNREG = 999999;      ///< 被叫端未注册
    public static final int RESP_CODE_SHADOW_UPDATE = 999998;   ///< 影子更新错误
    public static final int RESP_CODE_INVALID_TOKEN = 401;      ///< Token过期

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static AgoraService mInstance = null;

    ///< 服务器请求站点
    private String mBaseUrl = "";
    private String mCallkitBaseUrl= "https://api.agora.io/agoralink/cn/api/call-service/v1";
    private String mAlarmBaseUrl  = "https://api.agora.io/agoralink/cn/api/alert-center/alarm-message/v2";
    private String mAuthBaseUrl   = "https://api.agora.io/agoralink/cn/api/oauth";
    private String mDevMsgBaseUrl = "https://api.agora.io/agoralink/cn/api/alert-center/system-message/v1";
    private String mImgMgrBaseUrl = "https://api.agora.io/agoralink/cn/api/file-system/image/v1";
    private String mRtmBaseUrl    = "https://api.agora.io/agoralink/cn/api/call-service/v1";
    private String mImgBaseUrl    = "https://api.agora.io/agoralink/cn/api/file-system/image-record/v1";
    private String mRcdBaseUrl    = "https://api.agora.io/agoralink/cn/api/cloud-recorder/video-record/v1";


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public static AgoraService getInstance() {
        if (mInstance == null) {
            synchronized (AgoraService.class) {
                if (mInstance == null) {
                    mInstance = new AgoraService();
                }
            }
        }
        return mInstance;
    }

    public void setBaseUrl(final String baseUrl) {
        mBaseUrl = baseUrl;
        mCallkitBaseUrl= baseUrl + "/call-service/v1";
        mAlarmBaseUrl  = baseUrl + "/alert-center/alarm-message/v2";
        mAuthBaseUrl   = baseUrl + "/oauth";
        mDevMsgBaseUrl = baseUrl + "/alert-center/system-message/v1";
        mImgMgrBaseUrl = baseUrl + "/file-system/image/v1";
        mRtmBaseUrl    = baseUrl + "/call-service/v1";
        mImgBaseUrl    = baseUrl + "/file-system/image-record/v1";
        mRcdBaseUrl    = baseUrl + "/cloud-recorder/video-record/v1";

        ALog.getInstance().e(TAG, "<setBaseUrl> mCallkitBaseUrl=" + mCallkitBaseUrl);
        ALog.getInstance().e(TAG, "<setBaseUrl> mAlarmBaseUrl=" + mAlarmBaseUrl);
        ALog.getInstance().e(TAG, "<setBaseUrl> mAuthBaseUrl=" + mAuthBaseUrl);
        ALog.getInstance().e(TAG, "<setBaseUrl> mDevMsgBaseUrl=" + mDevMsgBaseUrl);
        ALog.getInstance().e(TAG, "<setBaseUrl> mImgMgrBaseUrl=" + mImgMgrBaseUrl);
        ALog.getInstance().e(TAG, "<setBaseUrl> mRtmBaseUrl=" + mRtmBaseUrl);
        ALog.getInstance().e(TAG, "<setBaseUrl> mImgBaseUrl=" + mImgBaseUrl);
        ALog.getInstance().e(TAG, "<setBaseUrl> mRcdBaseUrl=" + mRcdBaseUrl);
    }


    //////////////////////////////////////////////////////////////////////////////////
    ////////////////////////// Methods for Callkit Module ////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    public static class RtcPlayTokenResult {
        public int mErrCode;
        public String mChannelName;
        public String mRtcPlayToken;
        public int mLocalUid;
    }

    /**
     * @brief 获取 RTC播放的 token
     * @param token : Agora服务器的鉴权 token
     * @param appid : Agora AppId，来自于声网开发者平台
     * @param channelName : 要播放的频道名称
     * @return 服务端分配的Token信息
     */
    public RtcPlayTokenResult getRtcPlayToken(final String token, final String appid,
                                              final String channelName) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        RtcPlayTokenResult tokenResult = new RtcPlayTokenResult();

        // 请求URL
        String requestUrl = mCallkitBaseUrl + "/getRtcToken";

        // body内容
        JSONObject header = new JSONObject();
        try {
            header.put("traceId", appid + "-" + channelName);
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);

            JSONObject payloadObj = new JSONObject();
            payloadObj.put("appId", appid);
            payloadObj.put("channelName", channelName);
            body.put("payload", payloadObj);

        } catch (JSONException e) {
            e.printStackTrace();
            tokenResult.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return tokenResult;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<getRtcPlayToken> failure with no response!");
            tokenResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return tokenResult;
        }
        ALog.getInstance().d(TAG, "<getRtcPlayToken> responseObj=" + responseObj.toString());
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<getRtcPlayToken> failure, mErrorCode=" + responseObj.mErrorCode);
            tokenResult.mErrCode = ErrCode.XERR_HTTP_RESP_DATA;
            return tokenResult;
        }

        // 解析呼叫请求返回结果
        try {
            JSONObject dataObj = responseObj.mRespJsonObj.getJSONObject("data");
            tokenResult.mChannelName = parseJsonStringValue(dataObj, "channelName", null);
            tokenResult.mRtcPlayToken = parseJsonStringValue(dataObj, "rtcToken", null);
            tokenResult.mLocalUid = parseJsonIntValue(dataObj, "uid", 0);
            tokenResult.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<getRtcPlayToken> JSONException=" + e.toString());
            tokenResult.mErrCode =  ErrCode.XERR_HTTP_JSON_PARSE;
            return tokenResult;
        }

        ALog.getInstance().d(TAG, "<getRtcPlayToken> channelName=" + tokenResult.mChannelName
                    + ", rtcToken=" + tokenResult.mRtcPlayToken
                    + ", uid=" + tokenResult.mLocalUid  );
        return tokenResult;
    }



    /*
     * @brief 发起一个呼叫请求
     * @param appid : Agora AppId，来自于声网开发者平台
     * @param identityId : MQTT的本地client ID，来自于AWS登录账号
     * @param peerId : 呼叫目标client ID
     * @param attachMsg : 附加消息
     * @return 服务端分配的RTC通道信息
     */
    public static class CallReqResult {
        public int mErrCode;
        public CallkitContext mCallkitCtx;
    }

    public CallReqResult makeCall(final String token, final String appid, final String identityId,
                                  final String peerId, final String attachMsg) {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        CallReqResult callReqResult = new CallReqResult();

        // 请求URL
        String requestUrl = mCallkitBaseUrl + "/call";

        // body内容
        JSONObject header = new JSONObject();
        try {
            header.put("traceId", appid + "-" + identityId);
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);
            JSONObject payload = new JSONObject();
            payload.put("appId", appid);
            payload.put("callerId", identityId);
            JSONArray calleeDeviceIds = new JSONArray();
            calleeDeviceIds.put(peerId);                    // TODO：目前不支持一呼多
            payload.put("calleeIds", calleeDeviceIds);
            payload.put("attachMsg", attachMsg);
            body.put("payload", payload);
        } catch (JSONException e) {
            e.printStackTrace();
            callReqResult.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return callReqResult;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<makeCall> failure with no response!");
            callReqResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return callReqResult;
        }
        ALog.getInstance().d(TAG, "<makeCall> responseObj=" + responseObj.toString());
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<makeCall> failure, mErrorCode=" + responseObj.mErrorCode);
            callReqResult.mErrCode = ErrCode.XERR_HTTP_RESP_DATA;
            return callReqResult;
        }

        if (responseObj.mRespCode == RESP_CODE_IN_TALKING) {
            ALog.getInstance().e(TAG, "<makeCall> bad status IN_TALKING, mRespCode="
                    + responseObj.mRespCode);
            callReqResult.mErrCode = ErrCode.XERR_CALLKIT_PEER_BUSY;
            return callReqResult;

        } else if (responseObj.mRespCode == RESP_CODE_ANSWER) {
            ALog.getInstance().e(TAG, "<makeCall> bad status ANSWER");
            callReqResult.mErrCode = ErrCode.XERR_CALLKIT_ANSWER;
            return callReqResult;

        } else if (responseObj.mRespCode == RESP_CODE_HANGUP) {
            ALog.getInstance().e(TAG, "<makeCall> bad status HANGUP");
            callReqResult.mErrCode = ErrCode.XERR_CALLKIT_HANGUP;
            return callReqResult;

        } else if (responseObj.mRespCode == RESP_CODE_ANSWER_TIMEOUT) {
            ALog.getInstance().e(TAG, "<makeCall> bad status ANSWER_TIMEOUT");
            callReqResult.mErrCode = ErrCode.XERR_CALLKIT_TIMEOUT;
            return callReqResult;

        } else if (responseObj.mRespCode == RESP_CODE_CALL) {
            ALog.getInstance().e(TAG, "<makeCall> bad status CALL");
            callReqResult.mErrCode = ErrCode.XERR_CALLKIT_LOCAL_BUSY;
            return callReqResult;

        } else if (responseObj.mRespCode == RESP_CODE_INVALID_ANSWER) {
            ALog.getInstance().e(TAG, "<makeCall> bad status INVALID_ANSWER");
            callReqResult.mErrCode = ErrCode.XERR_CALLKIT_ERR_OPT;
            return callReqResult;

        } else if (responseObj.mRespCode == RESP_CODE_PEER_UNREG) {
            ALog.getInstance().e(TAG, "<makeCall> bad status PEER_UNREG");
            callReqResult.mErrCode = ErrCode.XERR_CALLKIT_PEER_UNREG;
            return callReqResult;

        } else if (responseObj.mRespCode == RESP_CODE_INVALID_TOKEN) {
            ALog.getInstance().e(TAG, "<makeCall> invalid token");
            callReqResult.mErrCode = ErrCode.XERR_TOKEN_INVALID;
            return callReqResult;

        } else if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<makeCall> status failure, mRespCode="
                    + responseObj.mRespCode);
            callReqResult.mErrCode = ErrCode.XERR_HTTP_RESP_CODE;
            return callReqResult;
        }

        // 解析呼叫请求返回结果
        CallkitContext rtcInfo = new CallkitContext();
        try {
            JSONObject dataObj = responseObj.mRespJsonObj.getJSONObject("data");
            rtcInfo.appId = parseJsonStringValue(dataObj, "appId", null);
            rtcInfo.callerId = identityId;
            rtcInfo.calleeId = peerId;
            rtcInfo.channelName = parseJsonStringValue(dataObj, "channelName", null);
            rtcInfo.rtcToken = parseJsonStringValue(dataObj, "rtcToken", null);
            rtcInfo.uid = parseJsonStringValue(dataObj, "uid", null);
            rtcInfo.peerUid = parseJsonStringValue(dataObj, "peerUid", null);
            rtcInfo.deviceAlias = parseJsonStringValue(dataObj, "deviceAlias", null);
            rtcInfo.sessionId = parseJsonStringValue(dataObj, "sessionId", null);
            rtcInfo.callStatus = parseJsonIntValue(dataObj, "callStatus", -1);

            if (rtcInfo.uid != null) {
                rtcInfo.mLocalUid = Integer.valueOf(rtcInfo.uid);
            }
            if (rtcInfo.peerUid != null) {
                rtcInfo.mPeerUid = Integer.valueOf(rtcInfo.peerUid);
            }

            callReqResult.mErrCode = ErrCode.XOK;
            callReqResult.mCallkitCtx = rtcInfo;

        } catch (JSONException e) {
            e.printStackTrace();
            callReqResult.mErrCode =  ErrCode.XERR_HTTP_JSON_PARSE;
            return callReqResult;
        }

        return callReqResult;
    }

    /*
     * @brief 发送呼叫接听响应
     * @param appid : seesionId，呼叫会话的session ID
     * @param callerId : 呼叫会话的发起方ID
     * @param calleeId : 呼叫会话的被叫方ID
     * @param isAccept : true：接听，false：挂断
     * @return 0：成功，<0：失败
     */
    public int makeAnswer(final String token,
                          final String sessionId, final String callerId, final String calleeId,
                          final String localId, final boolean isAccept)  {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mCallkitBaseUrl + "/answer";

        // body内容
        JSONObject header = new JSONObject();
        try {
            header.put("traceId", sessionId + "-" + callerId + "-" + calleeId);
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);
            JSONObject payload = new JSONObject();
            payload.put("callerId", callerId);
            payload.put("calleeId", calleeId);
            payload.put("localId", localId);
            payload.put("sessionId", sessionId);
            payload.put("answer", isAccept ? 0 : 1);
            body.put("payload", payload);
        } catch (JSONException e) {
            e.printStackTrace();
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<makeAnswer> failure with no response!");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<makeAnswer> failure, mErrorCode=" + responseObj.mErrorCode);
            return ErrCode.XERR_CALLKIT_ANSWER;
        }

        if (responseObj.mRespCode == RESP_CODE_SHADOW_UPDATE) { // 影子更新错误，可能需要重试
            ALog.getInstance().e(TAG, "<makeAnswer> RESP_CODE_SHADOW_UPDATE");
            return ErrCode.XERR_CALLKIT_ERR_OPT;
        }

        if (responseObj.mRespCode == RESP_CODE_INVALID_TOKEN) {
            ALog.getInstance().e(TAG, "<makeAnswer> invalid token");
            return ErrCode.XERR_TOKEN_INVALID;
        }

        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<makeAnswer> failure, mRespCode="
                    + responseObj.mRespCode);
            return ErrCode.XERR_CALLKIT_ANSWER;
        }

        return ErrCode.XOK;
    }



    //////////////////////////////////////////////////////////////////////////////////
    ////////////////////////// Methods for Alert Management ////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 新增告警信息
     * @param token : Agora服务器的操作token信息
     * @param account : 当前用户账号
     * @param talentId : 设备账号Id
     * @param insertParam : 要插入的告警信息
     * @return 0：成功，<0：失败
     */
    public int alarmInsert(final String token, final String account, final String talentId,
                           final IAlarmMgr.InsertParam insertParam)  {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mAlarmBaseUrl + "/add";

        // body内容
        try {
            JSONObject header = new JSONObject();
            header.put("traceId", account + "-" + "1" );
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);

            JSONObject payload = new JSONObject();
            payload.put("tenantId", talentId);
            payload.put("beginTime", insertParam.mBeginTime);
            payload.put("imageId", insertParam.mImageId);
            payload.put("productId", insertParam.mProductID);
            payload.put("deviceId", insertParam.mDeviceID);
            payload.put("deviceName", insertParam.mDeviceName);
            payload.put("description", insertParam.mDescription);
            payload.put("status", insertParam.mMsgStatus);
            payload.put("messageType", insertParam.mMsgType);
            body.put("payload", payload);

        } catch (JSONException e) {
            e.printStackTrace();
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<alarmInsert> failure with no response!");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<alarmInsert> failure, mErrorCode=" + responseObj.mErrorCode);
            return ErrCode.XERR_ALARM_ADD;
        }

        if (responseObj.mRespCode == RESP_CODE_INVALID_TOKEN) {
            ALog.getInstance().e(TAG, "<alarmInsert> invalid token");
            return ErrCode.XERR_TOKEN_INVALID;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<alarmInsert> failure, mRespCode="
                    + responseObj.mRespCode);
            return ErrCode.XERR_ALARM_ADD;
        }

        // 解析呼叫请求返回结果
        try {
            long alarmId = responseObj.mRespJsonObj.getLong("data");
            ALog.getInstance().d(TAG, "<alarmInsert> alarmId=" + alarmId);

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<alarmInsert> failure with JSON exception");
            return ErrCode.XERR_HTTP_JSON_PARSE;
        }

        ALog.getInstance().d(TAG, "<alarmInsert> done, account=" + account
                + ", insertParam=" + insertParam.toString());
        return ErrCode.XOK;
    }

    /**
     * @brief 删除告警信息
     * @param token : Agora服务器的操作token信息
     * @param account : 当前用户账号
     * @param alertIdList : 要删除的告警Id列表
     * @return 0：成功，<0：失败
     */
    public int alarmDelete(final String token, final String account, List<Long> alertIdList)  {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mAlarmBaseUrl + "/deleteBatch";

        // body内容
        try {
            JSONObject header = new JSONObject();
            header.put("traceId", account + "-" + "3" );
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);

            JSONArray payload = new JSONArray();
            for (int i = 0; i < alertIdList.size(); i++) {
                long alertId = alertIdList.get(i);
                payload.put(alertId);
            }
            body.put("payload", payload);

        } catch (JSONException e) {
            e.printStackTrace();
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<alarmDelete> failure with no response!");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<alarmDelete> failure, mErrorCode=" + responseObj.mErrorCode);
            return ErrCode.XERR_ALARM_DEL;
        }
        if (responseObj.mRespCode == RESP_CODE_INVALID_TOKEN) {
            ALog.getInstance().e(TAG, "<alarmDelete> invalid token");
            return ErrCode.XERR_TOKEN_INVALID;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<alarmDelete> failure, mRespCode="
                    + responseObj.mRespCode);
            return ErrCode.XERR_ALARM_DEL;
        }

        ALog.getInstance().d(TAG, "<alarmDelete> done, account=" + account
                + ", deletedCount=" + alertIdList.size());
        return ErrCode.XOK;
    }

    /**
     * @brief 标记告警信息为已读状态
     * @param token : Agora服务器的操作token信息
     * @param account : 当前用户账号
     * @param alertIdList : 要删除的告警Id列表
     * @return 0：成功，<0：失败
     */
    public int alarmMarkRead(final String token, final String account, List<Long> alertIdList)  {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mAlarmBaseUrl + "/readMessageBatch";

        // body内容
        try {
            JSONObject header = new JSONObject();
            header.put("traceId", account + "-" + "6" );
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);

            JSONArray payload = new JSONArray();
            for (int i = 0; i < alertIdList.size(); i++) {
                long alertId = alertIdList.get(i);
                payload.put(alertId);
            }
            body.put("payload", payload);

        } catch (JSONException e) {
            e.printStackTrace();
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<alarmMarkRead> failure with no response!");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<alarmMarkRead> failure, mErrorCode=" + responseObj.mErrorCode);
            return ErrCode.XERR_ALARM_MARK;
        }
        if (responseObj.mRespCode == RESP_CODE_INVALID_TOKEN) {
            ALog.getInstance().e(TAG, "<alarmMarkRead> invalid token");
            return ErrCode.XERR_TOKEN_INVALID;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<alarmMarkRead> failure, mRespCode="
                    + responseObj.mRespCode);
            return ErrCode.XERR_ALARM_MARK;
        }

        ALog.getInstance().d(TAG, "<alarmMarkRead> done, account=" + account
                + ", markedCount=" + alertIdList.size());
        return ErrCode.XOK;
    }


    /**
     * @brief 单个告警信息查询结果
     */
    public static class AlarmInfoResult {
        public int mErrCode;
        public IotAlarm mAlarm = new IotAlarm();
    }

    /**
     * @brief 根据告警Id查询告警详细信息
     * @param token : Agora服务器的操作token信息
     * @param account : 当前用户账号
     * @param alarmId : 告警Id
     * @return AlarmInfoResult：包含错误码 和 详细的告警信息
     */
    public AlarmInfoResult queryAlarmInfoById(final String token, final String account,
                                              final byte[] rsaPrivateKey, long alarmId)  {
        AlarmInfoResult queryResult = new AlarmInfoResult();
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mAlarmBaseUrl + "/getById";

        // body内容
        try {
            JSONObject header = new JSONObject();
            header.put("traceId", account + "-" + "7" );
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);

            JSONObject payload = new JSONObject();
            body.put("payload", alarmId);

        } catch (JSONException e) {
            e.printStackTrace();
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return queryResult;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<getAlarmInfoById> failure with no response!");
            queryResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return queryResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<getAlarmInfoById> failure, mErrorCode=" + responseObj.mErrorCode);
            queryResult.mErrCode = ErrCode.XERR_ALARM_GETINFO;
            return queryResult;
        }
        if (responseObj.mRespCode == RESP_CODE_INVALID_TOKEN) {
            ALog.getInstance().e(TAG, "<getAlarmInfoById> failure, invalid token");
            queryResult.mErrCode = ErrCode.XERR_TOKEN_INVALID;
            return queryResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<getAlarmInfoById> failure, mRespCode="
                    + responseObj.mRespCode);
            queryResult.mErrCode = ErrCode.XERR_ALARM_GETINFO;
            return queryResult;
        }

        // 解析呼叫请求返回结果
        try {
            JSONObject dataObj = responseObj.mRespJsonObj.getJSONObject("data");

            queryResult.mAlarm.mAlarmId = dataObj.getLong("alarmMessageId");  // 必须要有
            queryResult.mAlarm.mMessageType = parseJsonIntValue(dataObj, "messageType", 0);
            queryResult.mAlarm.mDescription = parseJsonStringValue(dataObj, "description", null);
            queryResult.mAlarm.mStatus = parseJsonIntValue(dataObj, "status", 0);
            queryResult.mAlarm.mTriggerTime = parseJsonLongValue(dataObj, "beginTime", -1);

            queryResult.mAlarm.mProductID = parseJsonStringValue(dataObj, "productId", null);
            queryResult.mAlarm.mDeviceID = parseJsonStringValue(dataObj, "deviceId", null);
            queryResult.mAlarm.mDeviceName = parseJsonStringValue(dataObj, "deviceName", null);
            queryResult.mAlarm.mOwnerUserId = parseJsonStringValue(dataObj, "tenantId", null);

            queryResult.mAlarm.mImageId = parseJsonStringValue(dataObj, "imageId", null);
            queryResult.mAlarm.mTriggerTime = parseJsonLongValue(dataObj, "beginTime", -1);

            queryResult.mAlarm.mDeleted = parseJsonBoolValue(dataObj, "deleted", false);
            queryResult.mAlarm.mCreatedBy = parseJsonLongValue(dataObj,"createdBy", 0);
            queryResult.mAlarm.mCreatedDate = parseJsonLongValue(dataObj,"createdDate", -1);
            queryResult.mAlarm.mChangedBy = parseJsonLongValue(dataObj, "changedBy", 0);
            queryResult.mAlarm.mChangedDate = parseJsonLongValue(dataObj,"changedDate", -1);

            queryResult.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<getAlarmInfoById> failure with JSON exception");
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return queryResult;
        }

        //
        // 根据ImageId来查询 ImageUrl
        //
        if (queryResult.mAlarm.mImageId != null) {
            AlarmImageResult imgResult = queryAlarmImageInfo(token, account, queryResult.mAlarm.mImageId);
            queryResult.mAlarm.mImageUrl = imgResult.mAlarmImg.mImageUrl;
        }

        //
        // 根据 BeginTime来查询 VideoUrl 等信息
        //
        if (queryResult.mAlarm.mTriggerTime > 0) {
            CloudRecordResult recordResult = queryAlarmRecordInfo(token, account,
                    queryResult.mAlarm.mDeviceID, queryResult.mAlarm.mTriggerTime);

            if (!TextUtils.isEmpty(recordResult.mAlarmVideo.mVideoSecret) && (rsaPrivateKey != null)) { // 视频有加密信息
                // 视频密钥进行Base64转换
                byte[] videoSecret = Base64.decode(recordResult.mAlarmVideo.mVideoSecret, 0);

                // 利用 RSA私钥对 视频密钥进行解码
                byte[] videoKey = RSAUtils.privateDecrypt(videoSecret, rsaPrivateKey);

                if (videoKey != null) {
                    // 解码后的 视频密钥 再转换成 Base64字符串
                    String agora_key = Base64.encodeToString(videoKey, Base64.NO_WRAP);

                    // 视频源 拼接 视频密钥 Base64的字符串
                    queryResult.mAlarm.mVideoUrl = recordResult.mAlarmVideo.mVideoUrl + "&agora-key=" + agora_key;

//                ALog.getInstance().d(TAG, "<getAlarmInfoById> mVideoSecret=" + recordResult.mAlarmVideo.mVideoSecret);
//                ALog.getInstance().d(TAG, "<getAlarmInfoById> videoKey=" + RSAUtils.bytesToString(videoKey));
//                ALog.getInstance().d(TAG, "<getAlarmInfoById> agora_key=" + agora_key);

                } else {
                    ALog.getInstance().e(TAG, "<getAlarmInfoById> fail to decode video secret");
                }

            } else { // 无视频加密，直接使用视频路径进行播放
                queryResult.mAlarm.mVideoUrl = recordResult.mAlarmVideo.mVideoUrl;
            }

            queryResult.mAlarm.mVideoBeginTime = recordResult.mAlarmVideo.mBeginTime;
            queryResult.mAlarm.mVideoEndTime = recordResult.mAlarmVideo.mEndTime;
        }

        ALog.getInstance().d(TAG, "<getAlarmInfoById> done, account=" + account
                + ", alarmId=" + alarmId
                + ", alarm=" + queryResult.mAlarm.toString());
        return queryResult;
    }


    /**
     * @brief 分页查询告警列表结果
     */
    public static class AlarmPageResult {
        public int mErrCode;
        public IotAlarmPage mAlarmPage = new IotAlarmPage();
    }

    /**
     * @brief 分页查询告警列表信息
     * @param token : Agora服务器的操作token信息
     * @param account : 当前用户账号
     * @param queryParam : 查询参数
     * @return AlarmPageResult：包含错误码 和 详细的告警信息
     */
    public AlarmPageResult queryAlarmByPage(final String token,
                                            final String account, final String tenantId,
                                            final IAlarmMgr.QueryParam queryParam)  {
        AlarmPageResult queryResult = new AlarmPageResult();
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        ALog.getInstance().d(TAG, "<queryAlarmByPage> [ENTER] queryParam="
                + queryParam.toString());

        // 请求URL
        String requestUrl = mAlarmBaseUrl + "/getPage";

        // body内容
        try {
            // header
            JSONObject header = new JSONObject();
            header.put("traceId", account + "-" + "8" );
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);

            // payload
            JSONObject payload = new JSONObject();
            payload.put("tenantId", tenantId);


            if ((queryParam.mProductID != null) && (queryParam.mProductID.length() > 0)) {
                payload.put("productId", queryParam.mProductID);
            }
            if ((queryParam.mDeviceID != null) && (queryParam.mDeviceID.length() > 0)) {
                payload.put("deviceId", queryParam.mDeviceID);
            }
            if (queryParam.mMsgType >= 0) {
                payload.put("messageType", queryParam.mMsgType);
            }
            if (queryParam.mMsgStatus >= 0) {
                payload.put("status", queryParam.mMsgStatus);
            }
            if ((queryParam.mBeginDate != null) && (queryParam.mBeginDate.length() > 0)) {
                payload.put("createdDateBegin", queryParam.mBeginDate);
            }
            if ((queryParam.mEndDate != null) && (queryParam.mEndDate.length() > 0)) {
                payload.put("createdDateEnd", queryParam.mEndDate);
            }
            body.put("payload", payload);

            // pageInfo
            JSONObject pageInfo = new JSONObject();
            pageInfo.put("currentPage", queryParam.mPageIndex);
            pageInfo.put("pageSize", queryParam.mPageSize);
            body.put("pageInfo", pageInfo);

            // sort map
            JSONObject sortMap = new JSONObject();
            sortMap.put("alertMessageId", (queryParam.mAscSort ? "asc" : "desc"));
            body.put("sortMap", sortMap);

        } catch (JSONException e) {
            e.printStackTrace();
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            ALog.getInstance().e(TAG, "<queryAlarmByPage> [EXIT] failure with JSON exp!");
            return queryResult;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<queryAlarmByPage> [EXIT] failure with no response!");
            queryResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return queryResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryAlarmByPage> [EXIT] failure, mErrorCode="
                    + responseObj.mErrorCode);
            queryResult.mErrCode = ErrCode.XERR_ALARM_PAGEQUERY;
            return queryResult;
        }
        if (responseObj.mRespCode == RESP_CODE_INVALID_TOKEN) {
            ALog.getInstance().e(TAG, "<queryAlarmByPage> failure, invalid token");
            queryResult.mErrCode = ErrCode.XERR_TOKEN_INVALID;
            return queryResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryAlarmByPage> [EXIT] failure, mRespCode="
                    + responseObj.mRespCode);
            queryResult.mErrCode = ErrCode.XERR_ALARM_PAGEQUERY;
            return queryResult;
        }

        // 解析呼叫请求返回结果
        try {
            JSONObject dataObj = responseObj.mRespJsonObj.getJSONObject("data");
            queryResult.mAlarmPage.mPageSize = dataObj.getInt("pageSize");
            queryResult.mAlarmPage.mPageIndex = dataObj.getInt("currentPage");
            queryResult.mAlarmPage.mTotalPage = dataObj.getInt("totalPage");


            // 解析告警记录列表
            int totalCount = dataObj.getInt("totalCount");
            JSONArray pageResults = parseJsonArray(dataObj,"pageResults");
            if (pageResults != null) {
                for (int i = 0; i < pageResults.length(); i++) {
                    JSONObject alarmObj = pageResults.getJSONObject(i);

                    IotAlarm iotAlarm = new IotAlarm();
                    iotAlarm.mAlarmId = alarmObj.getLong("alarmMessageId");
                    iotAlarm.mMessageType = parseJsonIntValue(alarmObj, "messageType", 0);
                    iotAlarm.mDescription = parseJsonStringValue(alarmObj, "description", null);
                    iotAlarm.mStatus = parseJsonIntValue(alarmObj, "status", 0);
                    iotAlarm.mTriggerTime = parseJsonLongValue(alarmObj, "beginTime", -1);

                    iotAlarm.mProductID = parseJsonStringValue(alarmObj, "productId", null);
                    iotAlarm.mDeviceID = parseJsonStringValue(alarmObj, "deviceId", null);
                    iotAlarm.mDeviceName = parseJsonStringValue(alarmObj, "deviceName", null);
                    iotAlarm.mOwnerUserId = parseJsonStringValue(alarmObj, "tenantId", null);

                    iotAlarm.mImageId = parseJsonStringValue(alarmObj, "imageId", null);
                    iotAlarm.mTriggerTime = parseJsonLongValue(alarmObj, "beginTime", -1);

                    iotAlarm.mDeleted = parseJsonBoolValue(alarmObj, "deleted", false);
                    iotAlarm.mCreatedBy = parseJsonLongValue(alarmObj, "createdBy", 0);
                    iotAlarm.mCreatedDate = parseJsonLongValue(alarmObj, "createdDate", -1);
                    iotAlarm.mChangedBy = parseJsonLongValue(alarmObj, "changedBy", 0);
                    iotAlarm.mChangedDate = parseJsonLongValue(alarmObj, "changedDate", -1);

                    queryResult.mAlarmPage.mAlarmList.add(iotAlarm);
                }
            }

            queryResult.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            ALog.getInstance().e(TAG, "<queryAlarmByPage> [EXIT] failure, exp=" + e.toString());
            return queryResult;
        }


        //
        // 根据ImageId来查询 ImageUrl
        //
        int alarmCount = queryResult.mAlarmPage.mAlarmList.size();
        for (int i = 0; i < alarmCount; i++) {
            IotAlarm iotAlarm = queryResult.mAlarmPage.mAlarmList.get(i);
            if (iotAlarm.mImageId != null) {
                AlarmImageResult imgResult = queryAlarmImageInfo(token, account, iotAlarm.mImageId);
                iotAlarm.mImageUrl = imgResult.mAlarmImg.mImageUrl;
                queryResult.mAlarmPage.mAlarmList.set(i, iotAlarm);
            }
        }

        ALog.getInstance().d(TAG, "<queryAlarmByPage> [EXIT], mAlarmPage="
                + queryResult.mAlarmPage.toString());
        return queryResult;
    }

    /**
     * @brief 根据条件查询告警记录结果
     */
    public static class AlarmNumberResult {
        public int mErrCode;
        public long mAlarmNumber = 0;
    }

    /**
     * @brief 根据条件查询告警信息数量
     * @param token : Agora服务器的操作token信息
     * @param account : 当前用户账号
     * @param queryParam : 查询参数
     * @return AlarmNumberResult：包含错误码 和 查询到的数量
     */
    public AlarmNumberResult queryAlarmNumber(final String token,
                                              final String account, final String tenantId,
                                              final IAlarmMgr.QueryParam queryParam)  {
        AlarmNumberResult queryResult = new AlarmNumberResult();
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        ALog.getInstance().d(TAG, "<queryAlarmNumber> [ENTER] queryParam="
                + queryParam.toString());

        // 请求URL
        String requestUrl = mAlarmBaseUrl + "/count";

        // body内容
        try {
            // header
            JSONObject header = new JSONObject();
            header.put("traceId", account + "-" + "8" );
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);

            // payload
            JSONObject payload = new JSONObject();
            payload.put("tenantId", tenantId);


            if ((queryParam.mProductID != null) && (queryParam.mProductID.length() > 0)) {
                payload.put("productId", queryParam.mProductID);
            }
            if ((queryParam.mDeviceID != null) && (queryParam.mDeviceID.length() > 0)) {
                payload.put("deviceId", queryParam.mDeviceID);
            }
            if (queryParam.mMsgType >= 0) {
                payload.put("messageType", queryParam.mMsgType);
            }
            if (queryParam.mMsgStatus >= 0) {
                payload.put("status", queryParam.mMsgStatus);
            }
            if ((queryParam.mBeginDate != null) && (queryParam.mBeginDate.length() > 0)) {
                payload.put("createdDateBegin", queryParam.mBeginDate);
            }
            if ((queryParam.mEndDate != null) && (queryParam.mEndDate.length() > 0)) {
                payload.put("createdDateEnd", queryParam.mEndDate);
            }
            body.put("payload", payload);

        } catch (JSONException e) {
            e.printStackTrace();
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            ALog.getInstance().e(TAG, "<queryAlarmNumber> [EXIT] failure with JSON exp!");
            return queryResult;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<queryAlarmNumber> [EXIT] failure with no response!");
            queryResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return queryResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryAlarmNumber> [EXIT] failure, mErrorCode="
                    + responseObj.mErrorCode);
            queryResult.mErrCode = ErrCode.XERR_ALARM_NUMBER;
            return queryResult;
        }
        if (responseObj.mRespCode == RESP_CODE_INVALID_TOKEN) {
            ALog.getInstance().e(TAG, "<queryAlarmNumber> failure, invalid token");
            queryResult.mErrCode = ErrCode.XERR_TOKEN_INVALID;
            return queryResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryAlarmNumber> [EXIT] failure, mRespCode="
                    + responseObj.mRespCode);
            queryResult.mErrCode = ErrCode.XERR_ALARM_NUMBER;
            return queryResult;
        }

        // 解析呼叫请求返回结果
        try {
            queryResult.mAlarmNumber = responseObj.mRespJsonObj.getLong("data");
            queryResult.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            ALog.getInstance().e(TAG, "<queryAlarmNumber> [EXIT] failure, exp=" + e.toString());
            return queryResult;
        }

        ALog.getInstance().d(TAG, "<queryAlarmNumber> [EXIT], mAlarmNumber="
                + queryResult.mAlarmNumber);
        return queryResult;
    }


    /**
     * @brief 告警图片信息
     */
    public static class AlarmImageResult {
        public int mErrCode = ErrCode.XOK;
        public IotAlarmImage mAlarmImg = new IotAlarmImage();
    }

    /**
     * @brief 根据 ImageId来查询 告警图片信息
     * @param token : Agora服务器的操作token信息
     * @param account : 当前用户账号
     * @param imageId : 告警Id
     * @return AlarmImageResult：包含错误码 和 详细的告警信息
     */
    public AlarmImageResult queryAlarmImageInfo(final String token, final String account,
                                                final String imageId)  {
        AlarmImageResult result = new AlarmImageResult();
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        ALog.getInstance().d(TAG, "<queryAlarmImageInfo> [ENTER] imageId=" + imageId);

        // 请求URL
        String requestUrl = mImgBaseUrl + "/getByImageId";

        // body内容
        try {
            // header
            JSONObject header = new JSONObject();
            header.put("traceId", account + "-" + "8" );
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);

            // payload
            body.put("payload", imageId);

        } catch (JSONException e) {
            e.printStackTrace();
            result.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            ALog.getInstance().e(TAG, "<queryAlarmImageInfo> [EXIT] failure with JSON exp!");
            return result;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<queryAlarmImageInfo> [EXIT] failure with no response!");
            result.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return result;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryAlarmImageInfo> [EXIT] failure, mErrorCode="
                    + responseObj.mErrorCode);
            result.mErrCode = ErrCode.XERR_ALARM_NUMBER;
            return result;
        }
        if (responseObj.mRespCode == RESP_CODE_INVALID_TOKEN) {
            ALog.getInstance().e(TAG, "<queryAlarmImageInfo> failure, invalid token");
            result.mErrCode = ErrCode.XERR_TOKEN_INVALID;
            return result;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryAlarmImageInfo> [EXIT] failure, mRespCode="
                    + responseObj.mRespCode);
            result.mErrCode = ErrCode.XERR_ALARM_NUMBER;
            return result;
        }

        // 解析呼叫请求返回结果
        try {
            JSONObject dataObj = responseObj.mRespJsonObj.getJSONObject("data");

            result.mAlarmImg.mRecordId = dataObj.getLong("recordId");  // 必须要有
            result.mAlarmImg.mImageId = parseJsonStringValue(dataObj, "imageId", null);
            result.mAlarmImg.mAccountId = parseJsonStringValue(dataObj, "userId", null);
            result.mAlarmImg.mFileName = parseJsonStringValue(dataObj, "fileName", null);
            result.mAlarmImg.mBucket = parseJsonStringValue(dataObj, "bucket", null);
            result.mAlarmImg.mRemark = parseJsonStringValue(dataObj, "remark", null);
            result.mAlarmImg.mImageUrl = parseJsonStringValue(dataObj, "vodUrl", null);

            result.mAlarmImg.mProductID = parseJsonStringValue(dataObj, "productId", null);
            result.mAlarmImg.mDeviceID = parseJsonStringValue(dataObj, "deviceId", null);
            result.mAlarmImg.mDeviceName = parseJsonStringValue(dataObj, "deviceName", null);

            result.mAlarmImg.mDeleted = parseJsonBoolValue(dataObj, "deleted", false);
            result.mAlarmImg.mCreatedBy = parseJsonLongValue(dataObj,"createdBy", 0);
            result.mAlarmImg.mCreateTime = parseJsonLongValue(dataObj,"createdTime", -1);

            result.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<queryAlarmImageInfo> failure with JSON exception");
            result.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return result;
        }

        ALog.getInstance().d(TAG, "<queryAlarmImageInfo> [EXIT], queryResult="
                + result.mAlarmImg.toString());
        return result;
    }


    /**
     * @brief 云录视频信息
     */
    public static class CloudRecordResult {
        public int mErrCode = ErrCode.XOK;
        public IotAlarmVideo mAlarmVideo = new IotAlarmVideo();
    }

    /**
     * @brief 根据时间戳查询告警云录视频
     * @param token : Agora服务器的操作token信息
     * @param account : 当前用户账号
     * @param deviceId : 设备Id
     * @param beginTime : 开始时间
     * @return AlarmImageResult：包含错误码 和 详细的告警信息
     */
    public CloudRecordResult queryAlarmRecordInfo(final String token, final String account,
                                                final String deviceId, long beginTime)  {
        CloudRecordResult result = new CloudRecordResult();
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        ALog.getInstance().d(TAG, "<queryAlarmRecordInfo> [ENTER] deviceId=" + deviceId
                + ", beginTime=" + beginTime);

        // 请求URL
        String requestUrl = mRcdBaseUrl + "/getByTimePoint";

        // body内容
        try {
            // header
            JSONObject header = new JSONObject();
            header.put("traceId", account + "-" + "8" );
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);

            // payload
            JSONObject payload = new JSONObject();
            payload.put("userId", account);
            payload.put("deviceId", deviceId);
            payload.put("beginTime", String.valueOf(beginTime));
            body.put("payload", payload);

        } catch (JSONException e) {
            e.printStackTrace();
            result.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            ALog.getInstance().e(TAG, "<queryAlarmRecordInfo> [EXIT] failure with JSON exp!");
            return result;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<queryAlarmRecordInfo> [EXIT] failure with no response!");
            result.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return result;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryAlarmRecordInfo> [EXIT] failure, mErrorCode="
                    + responseObj.mErrorCode);
            result.mErrCode = ErrCode.XERR_ALARM_NUMBER;
            return result;
        }
        if (responseObj.mRespCode == RESP_CODE_INVALID_TOKEN) {
            ALog.getInstance().e(TAG, "<queryAlarmRecordInfo> failure, invalid token");
            result.mErrCode = ErrCode.XERR_TOKEN_INVALID;
            return result;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryAlarmRecordInfo> [EXIT] failure, mRespCode="
                    + responseObj.mRespCode);
            result.mErrCode = ErrCode.XERR_ALARM_NUMBER;
            return result;
        }

        // 解析呼叫请求返回结果
        try {
            JSONObject dataObj = responseObj.mRespJsonObj.getJSONObject("data");

            result.mAlarmVideo.mVideoRecordId = dataObj.getLong("videoRecordId");  // 必须要有
            result.mAlarmVideo.mRecordType = parseJsonIntValue(dataObj, "type", 0);
            result.mAlarmVideo.mAccountId = parseJsonStringValue(dataObj, "userId", null);
            result.mAlarmVideo.mBeginTime = parseJsonLongValue(dataObj, "beginTime", -1);
            result.mAlarmVideo.mEndTime = parseJsonLongValue(dataObj, "endTime", -1);
            result.mAlarmVideo.mFileName = parseJsonStringValue(dataObj, "fileName", null);
            result.mAlarmVideo.mBucket = parseJsonStringValue(dataObj, "bucket", null);
            result.mAlarmVideo.mRemark = parseJsonStringValue(dataObj, "remark", null);
            result.mAlarmVideo.mVideoUrl = parseJsonStringValue(dataObj, "vodUrl", null);
            result.mAlarmVideo.mVideoSecret = parseJsonStringValue(dataObj, "videoSecretKey", null);

            result.mAlarmVideo.mProductID = parseJsonStringValue(dataObj, "productId", null);
            result.mAlarmVideo.mDeviceID = parseJsonStringValue(dataObj, "deviceId", null);
            result.mAlarmVideo.mDeviceName = parseJsonStringValue(dataObj, "deviceName", null);

            result.mAlarmVideo.mDeleted = parseJsonBoolValue(dataObj, "deleted", false);
            result.mAlarmVideo.mCreatedBy = parseJsonLongValue(dataObj,"createdBy", 0);
            result.mAlarmVideo.mCreateTime = parseJsonLongValue(dataObj,"createdTime", -1);

            result.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<queryAlarmRecordInfo> failure with JSON exception");
            result.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return result;
        }

        ALog.getInstance().d(TAG, "<queryAlarmRecordInfo> [EXIT], queryResult="
                + result.toString());
        return result;
    }



    //////////////////////////////////////////////////////////////////////////////////
    /////////////// Methods for System Message Management ////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    /*
     * @brief 标记设备消息为已读状态
     * @param account : 当前用户账号
     * @param alertIdList : 要标记的设备消息Id列表
     * @return 0：成功，<0：失败
     */
    public int devMsgMarkRead(final String token, final String account, List<Long> devMsgIdList)  {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mDevMsgBaseUrl + "/readMessageBatch";

        // body内容
        try {
            JSONObject header = new JSONObject();
            header.put("traceId", account + "-" + "6" );
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);

            JSONArray payload = new JSONArray();
            for (int i = 0; i < devMsgIdList.size(); i++) {
                long alertId = devMsgIdList.get(i);
                payload.put(alertId);
            }
            body.put("payload", payload);

        } catch (JSONException e) {
            e.printStackTrace();
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<devMsgMarkRead> failure with no response!");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<devMsgMarkRead> failure, mErrorCode=" + responseObj.mErrorCode);
            return ErrCode.XERR_DEVMSG_MARK;
        }
        if (responseObj.mRespCode == RESP_CODE_INVALID_TOKEN) {
            ALog.getInstance().e(TAG, "<devMsgMarkRead> failure, invalid token");
            return ErrCode.XERR_TOKEN_INVALID;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<devMsgMarkRead> failure, mRespCode="
                    + responseObj.mRespCode);
            return ErrCode.XERR_DEVMSG_MARK;
        }

        ALog.getInstance().d(TAG, "<devMsgMarkRead> done, account=" + account
                + ", markedCount=" + devMsgIdList.size());
        return ErrCode.XOK;
    }

    /*
     * @brief 根据设备消息Id查询设备消息详细信息
     * @param account : 当前用户账号
     * @param alarmId : 设备消息Id
     * @return AlarmInfoResult：包含错误码 和 详细的设备消息
     */
    public static class DevMsgInfoResult {
        public int mErrCode;
        public IotDevMessage mDevMessage = new IotDevMessage();
    }
    public DevMsgInfoResult queryDevMsgInfoById(final String token,
                                              final String account, long devMsgId)  {
        DevMsgInfoResult queryResult = new DevMsgInfoResult();
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        // 请求URL
        String requestUrl = mDevMsgBaseUrl + "/getById";

        // body内容
        try {
            JSONObject header = new JSONObject();
            header.put("traceId", account + "-" + "7" );
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);

            JSONObject payload = new JSONObject();
            body.put("payload", devMsgId);

        } catch (JSONException e) {
            e.printStackTrace();
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return queryResult;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<queryDevMsgInfoById> failure with no response!");
            queryResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return queryResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryDevMsgInfoById> failure, mErrorCode=" + responseObj.mErrorCode);
            queryResult.mErrCode = ErrCode.XERR_DEVMSG_GETINFO;
            return queryResult;
        }
        if (responseObj.mRespCode == RESP_CODE_INVALID_TOKEN) {
            ALog.getInstance().e(TAG, "<queryDevMsgInfoById> failure, invalid token");
            queryResult.mErrCode = ErrCode.XERR_TOKEN_INVALID;
            return queryResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryDevMsgInfoById> failure, mRespCode="
                    + responseObj.mRespCode);
            queryResult.mErrCode = ErrCode.XERR_DEVMSG_GETINFO;
            return queryResult;
        }

        // 解析呼叫请求返回结果
        try {
            JSONObject dataObj = responseObj.mRespJsonObj.getJSONObject("data");

            queryResult.mDevMessage.mMessageId = dataObj.getLong("systemMessageId");

            queryResult.mDevMessage.mProductID = parseJsonStringValue(dataObj, "productId", null);
            queryResult.mDevMessage.mDeviceID = parseJsonStringValue(dataObj, "deviceId", null);
            queryResult.mDevMessage.mDeviceName = parseJsonStringValue(dataObj, "deviceName", null);

            queryResult.mDevMessage.mMessageType = parseJsonIntValue(dataObj, "messageType", 0);
            queryResult.mDevMessage.mDescription = parseJsonStringValue(dataObj, "description", null);
            queryResult.mDevMessage.mFileUrl = parseJsonStringValue(dataObj, "fileUrl", null);
            queryResult.mDevMessage.mStatus = parseJsonIntValue(dataObj, "status", 0);

            queryResult.mDevMessage.mDeleted = parseJsonBoolValue(dataObj, "deleted", false);
            queryResult.mDevMessage.mCreatedBy = parseJsonLongValue(dataObj,"createdBy", 0);
            queryResult.mDevMessage.mCreatedDate = parseJsonStringValue(dataObj,"createdDate", null);
            queryResult.mDevMessage.mChangedBy = parseJsonLongValue(dataObj, "changedBy", 0);
            queryResult.mDevMessage.mChangedDate = parseJsonStringValue(dataObj,"changedDate", null);

            queryResult.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<queryDevMsgInfoById> failure with JSON exception");
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return queryResult;
        }

        ALog.getInstance().d(TAG, "<queryDevMsgInfoById> done, account=" + account
                + ", devMsgId=" + devMsgId
                + ", mDevMessage=" + queryResult.mDevMessage.toString());
        return queryResult;
    }


    /*
     * @brief 分页查询设备消息列表信息
     * @param account : 当前用户账号
     * @param queryParam : 查询参数
     * @return DevMsgPageResult：包含错误码 和 详细的设备消息
     */
    public static class DevMsgPageResult {
        public int mErrCode;
        public IotDevMsgPage mDevMsgPage = new IotDevMsgPage();
    }
    public DevMsgPageResult queryDevMsgByPage(final String token,
                                            final String account, final String tenantId,
                                            final IDevMessageMgr.QueryParam queryParam)  {
        DevMsgPageResult queryResult = new DevMsgPageResult();
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        ALog.getInstance().d(TAG, "<queryDevMsgByPage> [ENTER] queryParam="
                + queryParam.toString());

        // 请求URL
        String requestUrl = mDevMsgBaseUrl + "/getPage";

        // body内容
        try {
            // header
            JSONObject header = new JSONObject();
            header.put("traceId", account + "-" + "8" );
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);

            // payload
            JSONObject payload = new JSONObject();

            JSONArray deviceIds = new JSONArray();
            for (int i = 0; i < queryParam.mDevIDList.size(); i++) {
                String deviceId = queryParam.mDevIDList.get(i);
                deviceIds.put(deviceId);
            }
            payload.put("deviceIds", deviceIds);

//            if ((tenantId != null) && (tenantId.length() > 0)) {
//                payload.put("tenantId", tenantId);
//            }
            if ((queryParam.mProductID != null) && (queryParam.mProductID.length() > 0)) {
                payload.put("productID", queryParam.mProductID);
            }
            if (queryParam.mMsgType >= 0) {
                payload.put("messageType", queryParam.mMsgType);
            }
            if (queryParam.mMsgStatus >= 0) {
                payload.put("status", queryParam.mMsgStatus);
            }
            if ((queryParam.mBeginDate != null) && (queryParam.mBeginDate.length() > 0)) {
                payload.put("createdDateBegin", queryParam.mBeginDate);
            }
            if ((queryParam.mEndDate != null) && (queryParam.mEndDate.length() > 0)) {
                payload.put("createdDateEnd", queryParam.mEndDate);
            }
            body.put("payload", payload);

            // pageInfo
            JSONObject pageInfo = new JSONObject();
            pageInfo.put("currentPage", queryParam.mPageIndex);
            pageInfo.put("pageSize", queryParam.mPageSize);
            body.put("pageInfo", pageInfo);

            // sort map
            JSONObject sortMap = new JSONObject();
            sortMap.put("systemMessageId", (queryParam.mAscSort ? "asc" : "desc"));
            body.put("sortMap", sortMap);

        } catch (JSONException e) {
            e.printStackTrace();
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            ALog.getInstance().e(TAG, "<queryDevMsgByPage> [EXIT] failure with JSON exp!");
            return queryResult;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<queryDevMsgByPage> [EXIT] failure with no response!");
            queryResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return queryResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryDevMsgByPage> [EXIT] failure, mErrorCode="
                    + responseObj.mErrorCode);
            queryResult.mErrCode = ErrCode.XERR_DEVMSG_PAGEQUERY;
            return queryResult;
        }
        if (responseObj.mRespCode == RESP_CODE_INVALID_TOKEN) {
            ALog.getInstance().e(TAG, "<queryDevMsgByPage> failure, invalid token");
            queryResult.mErrCode = ErrCode.XERR_TOKEN_INVALID;
            return queryResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryDevMsgByPage> [EXIT] failure, mRespCode="
                    + responseObj.mRespCode);
            queryResult.mErrCode = ErrCode.XERR_DEVMSG_PAGEQUERY;
            return queryResult;
        }

        // 解析呼叫请求返回结果
        try {
            JSONObject dataObj = responseObj.mRespJsonObj.getJSONObject("data");
            queryResult.mDevMsgPage.mPageSize = dataObj.getInt("pageSize");
            queryResult.mDevMsgPage.mPageIndex = dataObj.getInt("currentPage");
            queryResult.mDevMsgPage.mTotalPage = dataObj.getInt("totalPage");

            // 解析告警记录列表
            int totalCount = dataObj.getInt("totalCount");
            JSONArray pageResults = dataObj.getJSONArray("pageResults");
            for (int i = 0; i < pageResults.length(); i++) {
                JSONObject alarmObj = pageResults.getJSONObject(i);

                IotDevMessage devMessage = new IotDevMessage();
                devMessage.mMessageId = alarmObj.getLong("systemMessageId");

                devMessage.mProductID = parseJsonStringValue(alarmObj, "productId", null);
                devMessage.mDeviceID = parseJsonStringValue(alarmObj, "deviceId", null);
                devMessage.mDeviceName = parseJsonStringValue(alarmObj, "deviceName", null);

                devMessage.mMessageType = parseJsonIntValue(alarmObj, "messageType", 0);
                devMessage.mDescription = parseJsonStringValue(alarmObj, "description", null);
                devMessage.mFileUrl = parseJsonStringValue(alarmObj, "fileUrl", null);
                devMessage.mStatus = parseJsonIntValue(alarmObj, "status", 0);

                devMessage.mDeleted = parseJsonBoolValue(alarmObj, "deleted", false);
                devMessage.mCreatedBy = parseJsonLongValue(alarmObj,"createdBy", 0);
                devMessage.mCreatedDate = parseJsonStringValue(alarmObj,"createdDate", null);
                devMessage.mChangedBy = parseJsonLongValue(alarmObj, "changedBy", 0);
                devMessage.mChangedDate = parseJsonStringValue(alarmObj,"changedDate", null);

                queryResult.mDevMsgPage.mDevMsgList.add(devMessage);
            }

            queryResult.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            ALog.getInstance().e(TAG, "<queryDevMsgByPage> [EXIT] failure, exp=" + e.toString());
            return queryResult;
        }

        ALog.getInstance().d(TAG, "<queryDevMsgByPage> [EXIT], mDevMsgPage="
                + queryResult.mDevMsgPage.toString());
        return queryResult;
    }

    /*
     * @brief 根据条件查询设备消息数量
     * @param account : 当前用户账号
     * @param queryParam : 查询参数
     * @return AlarmNumberResult：包含错误码 和 查询到的数量
     */
    public static class DevMsgNumberResult {
        public int mErrCode;
        public long mDevMsgNumber = 0;
    }
    public DevMsgNumberResult queryDevMsgNumber(final String token,
                                              final String account, final String tenantId,
                                              final IDevMessageMgr.QueryParam queryParam)  {
        DevMsgNumberResult queryResult = new DevMsgNumberResult();
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();

        ALog.getInstance().d(TAG, "<queryDevMsgNumber> [ENTER] queryParam="
                + queryParam.toString());

        // 请求URL
        String requestUrl = mDevMsgBaseUrl + "/count";

        // body内容
        try {
            // header
            JSONObject header = new JSONObject();
            header.put("traceId", account + "-" + "8" );
            header.put("timestamp", System.currentTimeMillis());
            body.put("header", header);

            // payload
            JSONObject payload = new JSONObject();

            JSONArray deviceIds = new JSONArray();
            for (int i = 0; i < queryParam.mDevIDList.size(); i++) {
                String deviceId = queryParam.mDevIDList.get(i);
                deviceIds.put(deviceId);
            }
            payload.put("deviceIds", deviceIds);

//            if ((tenantId != null) && (tenantId.length() > 0)) {
//                payload.put("tenantId", tenantId);
//            }
            if ((queryParam.mProductID != null) && (queryParam.mProductID.length() > 0)) {
                payload.put("productId", queryParam.mProductID);
            }
            if (queryParam.mMsgType >= 0) {
                payload.put("messageType", queryParam.mMsgType);
            }
            if (queryParam.mMsgStatus >= 0) {
                payload.put("status", queryParam.mMsgStatus);
            }
            if ((queryParam.mBeginDate != null) && (queryParam.mBeginDate.length() > 0)) {
                payload.put("createdDateBegin", queryParam.mBeginDate);
            }
            if ((queryParam.mEndDate != null) && (queryParam.mEndDate.length() > 0)) {
                payload.put("createdDateEnd", queryParam.mEndDate);
            }
            body.put("payload", payload);

        } catch (JSONException e) {
            e.printStackTrace();
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            ALog.getInstance().e(TAG, "<queryDevMsgNumber> [EXIT] failure with JSON exp!");
            return queryResult;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<queryDevMsgNumber> [EXIT] failure with no response!");
            queryResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return queryResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryDevMsgNumber> [EXIT] failure, mErrorCode="
                    + responseObj.mErrorCode);
            queryResult.mErrCode = ErrCode.XERR_DEVMSG_NUMBER;
            return queryResult;
        }
        if (responseObj.mRespCode == RESP_CODE_INVALID_TOKEN) {
            ALog.getInstance().e(TAG, "<queryDevMsgNumber> failure, invalid token");
            queryResult.mErrCode = ErrCode.XERR_TOKEN_INVALID;
            return queryResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<queryDevMsgNumber> [EXIT] failure, mRespCode="
                    + responseObj.mRespCode);
            queryResult.mErrCode = ErrCode.XERR_DEVMSG_NUMBER;
            return queryResult;
        }

        // 解析呼叫请求返回结果
        try {
            queryResult.mDevMsgNumber = responseObj.mRespJsonObj.getLong("data");
            queryResult.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            queryResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            ALog.getInstance().e(TAG, "<queryDevMsgNumber> [EXIT] failure, exp=" + e.toString());
            return queryResult;
        }

        ALog.getInstance().d(TAG, "<queryDevMsgNumber> [EXIT], mDevMsgNumber="
                + queryResult.mDevMsgNumber);
        return queryResult;
    }


    //////////////////////////////////////////////////////////////////////////////////
    ////////////////////////// Methods for Authorize Module ////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    /*
     * @brief 向服务器注册用户
     * @param account : 当前用户账号
     * @param queryParam : 查询参数
     * @return AlarmPageResult：包含错误码 和 详细的告警信息
     */
    public int accountRegister(final String userName)  {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        ALog.getInstance().d(TAG, "<accountRegister> [Enter] userName=" + userName);

        // 请求URL
        String requestUrl = mAuthBaseUrl + "/register";

        // body内容
        try {
            body.put("username", userName);

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<accountRegister> [Exit] failure with JSON exp!");
            return ErrCode.XERR_HTTP_JSON_WRITE;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                null, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<accountRegister> [EXIT] failure with no response!");
            return ErrCode.XERR_HTTP_NO_RESPONSE;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountRegister> [EXIT] failure, mRespCode="
                    + responseObj.mRespCode);
            return ErrCode.XERR_HTTP_RESP_CODE;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountRegister> [EXIT] failure, mErrorCode="
                    + responseObj.mErrorCode);
            return ErrCode.XERR_HTTP_RESP_DATA;
        }

        ALog.getInstance().d(TAG, "<accountRegister> [EXIT] successful");
        return ErrCode.XOK;
    }


    /*
     * @brief 向服务器请求Token信息
     * @param account : 当前用户账号
     * @param queryParam : 查询参数
     * @return AlarmPageResult：包含错误码 和 详细的告警信息
     */
    public static class RetrieveTokenParam {
        public String mGrantType;
        public String mUserName;
        public String mPassword;
        public String mScope;
        public String mClientId;
        public String mSecretKey;

        @Override
        public String toString() {
            String infoText = "{ mGrantType=" + mGrantType
                    + ", mUserName=" + mUserName + ", mPassword=" + mPassword
                    + ", mScope=" + mScope + ", mClientId=" + mClientId
                    + ", mSecretKey=" + mSecretKey + " }";
            return infoText;
        }
    }

    public static class AccountTokenInfo {
        public int mErrCode;
        public String mScope;
        public String mTokenType;
        public String mAccessToken;
        public String mRefreshToken;
        public long mExpriesIn;

        @Override
        public String toString() {
            String infoText = "{ mErrCode=" + mErrCode
                    + ", mScope=" + mScope + ", mTokenType=" + mTokenType
                    + ", mAccessToken=" + mAccessToken + ", mRefreshToken=" + mRefreshToken
                    + ", mExpriesIn=" + mExpriesIn + " }";
            return infoText;
        }
    }

    public AccountTokenInfo accountGetToken(final RetrieveTokenParam retrieveParam)  {
        AccountTokenInfo retreieveResult = new AccountTokenInfo();
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        ALog.getInstance().d(TAG, "<accountGetToken> [Enter] param=" + retrieveParam.toString());

        // 请求URL
        String requestUrl = mAuthBaseUrl + "/rest-token";

        // body内容
        try {
            if (retrieveParam.mGrantType != null) {
                body.put("grant_type", retrieveParam.mGrantType);
            }

            if (retrieveParam.mUserName != null) {
                body.put("username", retrieveParam.mUserName);
            }

            if (retrieveParam.mPassword != null) {
                body.put("password", retrieveParam.mPassword);
            }

            if (retrieveParam.mScope != null) {
                body.put("scope", retrieveParam.mScope);
            }

            if (retrieveParam.mClientId != null) {
                body.put("client_id", retrieveParam.mClientId);
            }

            if (retrieveParam.mSecretKey != null) {
                body.put("client_secret", retrieveParam.mSecretKey);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            retreieveResult.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            ALog.getInstance().e(TAG, "<accountGetToken> [Exit] failure with JSON exp!");
            return retreieveResult;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                null, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<accountGetToken> [EXIT] failure with no response!");
            retreieveResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return retreieveResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountGetToken> [EXIT] failure, mRespCode="
                    + responseObj.mRespCode);
            retreieveResult.mErrCode = ErrCode.XERR_HTTP_RESP_CODE;
            return retreieveResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<accountGetToken> [EXIT] failure, mErrorCode="
                    + responseObj.mErrorCode);
            retreieveResult.mErrCode = ErrCode.XERR_HTTP_RESP_DATA;
            return retreieveResult;
        }


        // 解析呼叫请求返回结果
        try {
            JSONObject dataObj = responseObj.mRespJsonObj.getJSONObject("data");
            if (dataObj == null) {
                ALog.getInstance().e(TAG, "<accountGetToken> [EXIT] failure, no dataObj");
                retreieveResult.mErrCode = ErrCode.XERR_HTTP_RESP_DATA;
                return retreieveResult;
            }

            retreieveResult.mScope = parseJsonStringValue(dataObj, "scope", null);
            retreieveResult.mTokenType = parseJsonStringValue(dataObj, "token_type", null);
            retreieveResult.mAccessToken = parseJsonStringValue(dataObj, "access_token", null);
            retreieveResult.mRefreshToken = parseJsonStringValue(dataObj, "refresh_token", null);
            retreieveResult.mExpriesIn = parseJsonLongValue(dataObj, "expires_in", -1);
            retreieveResult.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            retreieveResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            ALog.getInstance().e(TAG, "<accountGetToken> [EXIT] failure, exp=" + e.toString());
            return retreieveResult;
        }

        ALog.getInstance().d(TAG, "<accountGetToken> [EXIT] successful, retreieveResult="
                + retreieveResult.toString());
        return retreieveResult;
    }


    /*
     * @brief 向服务器注册用户，并且获取Token信息
     * @param account : 当前用户账号
     * @param queryParam : 查询参数
     * @return AlarmPageResult：包含错误码 和 详细的告警信息
     */
     public AccountTokenInfo accountLogin(final RetrieveTokenParam retrieveParam)  {
        int errCode = accountRegister(retrieveParam.mUserName);
        if (errCode != ErrCode.XOK) {
            AccountTokenInfo registerResult = new AccountTokenInfo();
            registerResult.mErrCode = errCode;
            return registerResult;
        }

         AccountTokenInfo retreieveResult = accountGetToken(retrieveParam);
        return retreieveResult;
    }


    //////////////////////////////////////////////////////////////////////////////////
    ///////////////////// Methods for RTM Management Module ////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    /**
     * @brief RTM 请求到的账号信息
     */
    public static class RtmAccountInfo {
        public int mErrCode = ErrCode.XOK;
        public String mToken;           ///< 分配到的RTM 本地uid
    }

    /**
     * @brief 向服务器请求RTM通道账号
     * @param controllerId : APP控制端账号Id
     * @param controlledId : 被控设备端账号Id
     * @return AlarmPageResult：包含错误码 和 详细的告警信息
     */
    public RtmAccountInfo reqRtmAccount(final String token, final String appId,
                                        final String controllerId, final String controlledId)  {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        RtmAccountInfo result = new RtmAccountInfo();
        ALog.getInstance().d(TAG, "<reqRtmAccount> [Enter]"
                + ", controllerId=" + controllerId
                + ", controlledId=" + controlledId);

        // 请求URL
        String requestUrl = mRtmBaseUrl + "/control/start";

        // body内容
        try {
            JSONObject headerObj = new JSONObject();
            headerObj.put("traceId", appId + "-" + controllerId);
            headerObj.put("timestamp", System.currentTimeMillis());
            body.put("header", headerObj);

            JSONObject payloadObj = new JSONObject();
            payloadObj.put("controllerId", controllerId);
            payloadObj.put("controlledId", controlledId);
            body.put("payload", payloadObj);

        } catch (JSONException e) {
            e.printStackTrace();
            result.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return result;
        }

        AgoraService.ResponseObj responseObj = requestToServer(requestUrl, "POST",
                token, params, body);
        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<reqRtmAccount> [EXIT] failure with no response!");
            result.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return result;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<reqRtmAccount> [EXIT] failure, mRespCode="
                    + responseObj.mRespCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_CODE;
            return result;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<reqRtmAccount> [EXIT] failure, mErrorCode="
                    + responseObj.mErrorCode);
            result.mErrCode = ErrCode.XERR_HTTP_RESP_DATA;
            return result;
        }


        // 解析服务器返回的RTM分配信息
        try {
            JSONObject dataObj = responseObj.mRespJsonObj.getJSONObject("data");
            if (dataObj == null) {
                ALog.getInstance().e(TAG, "<reqRtmAccount> [EXIT] failure, no dataObj");
                result.mErrCode = ErrCode.XERR_HTTP_RESP_DATA;
                return result;
            }

            result.mToken = parseJsonStringValue(dataObj, "rtmToken", null);
            result.mErrCode = ErrCode.XOK;

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<reqRtmAccount> failure with JSON exception");
            result.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return result;
        }

        ALog.getInstance().d(TAG, "<reqRtmAccount> [EXIT] successful"
                + ", token=" + result.mToken);
        return result;
    }



    //////////////////////////////////////////////////////////////////////////////////
    ///////////////////// Methods for Image Management Module ////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    /*
     * @brief 上传图片到服务器
     * @param fileName : 上传后的图片命名
     * @param fileDir : 图片在服务器上的路径
     * @param rename : 是否重命名
     * @param localImgFile : 要上传的本地图片绝对路径
     * @return
     */
    public static class ImgUploadResult {
        public int mErrCode;
        public String mCloudFilePath;

        @Override
        public String toString() {
            String infoText = "{ mErrCode=" + mErrCode + ", mCloudFilePath=" + mCloudFilePath + " }";
            return infoText;
        }
    }

    public ImgUploadResult uploadImage(final String fileName, final String fileDir, boolean rename,
                           final byte[] fileContent    )  {
        Map<String, String> params = new HashMap();
        JSONObject body = new JSONObject();
        ImgUploadResult uploadResult = new ImgUploadResult();
        ALog.getInstance().d(TAG, "<uploadImage> [Enter] fileSize=" + fileContent.length
                + ", fileName=" + fileName + ", fileDir=" + fileDir + ", rename=" + rename);

        // 请求URL
        String requestUrl = mImgMgrBaseUrl + "/uploadFile";

        // body内容
        try {
            body.put("fileName", fileName);
            body.put("fileDir", fileDir);
            body.put("renameFile", rename);

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<uploadImage> [Exit] failure with JSON exp!");
            uploadResult.mErrCode = ErrCode.XERR_HTTP_JSON_WRITE;
            return uploadResult;
        }

        AgoraService.ResponseObj responseObj = requestFileToServer(requestUrl, "POST",
                fileName, fileDir, rename, fileContent);

        if (responseObj == null) {
            ALog.getInstance().e(TAG, "<uploadImage> [EXIT] failure with no response!");
            uploadResult.mErrCode = ErrCode.XERR_HTTP_NO_RESPONSE;
            return uploadResult;
        }
        if (responseObj.mRespCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<uploadImage> [EXIT] failure, mRespCode="
                    + responseObj.mRespCode);
            uploadResult.mErrCode = ErrCode.XERR_HTTP_RESP_CODE;
            return uploadResult;
        }
        if (responseObj.mErrorCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<uploadImage> [EXIT] failure, mErrorCode="
                    + responseObj.mErrorCode);
            uploadResult.mErrCode = ErrCode.XERR_HTTP_RESP_DATA;
            return uploadResult;
        }

        // 解析上传请求返回结果
        try {
            uploadResult.mCloudFilePath = responseObj.mRespJsonObj.getString("data");
            ALog.getInstance().d(TAG, "<uploadImage> cloudFilePath=" + uploadResult.mCloudFilePath);

        } catch (JSONException e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<uploadImage> failure with JSON exception");
            uploadResult.mErrCode = ErrCode.XERR_HTTP_JSON_PARSE;
            return uploadResult;
        }

        ALog.getInstance().d(TAG, "<uploadImage> [EXIT] successful");
        return uploadResult;
    }


    ////////////////////////////////////////////////////////////////////////
    ///////////////////////////// Inner Methods ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief 给服务器发送HTTP请求，并且等待接收回应数据
     *        该函数是阻塞等待调用，因此最好是在工作线程中执行
     */
    private synchronized AgoraService.ResponseObj requestToServer(String baseUrl, String method, String token,
                                                                    Map<String, String> params, JSONObject body) {
        long t1 = System.currentTimeMillis();
        AgoraService.ResponseObj responseObj = new AgoraService.ResponseObj();

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
                responseObj.mTip = responseObj.mRespJsonObj.getString("timestamp");

            } catch (JSONException e) {
                e.printStackTrace();
                ALog.getInstance().e(TAG, "<requestToServer> Invalied json=" + response);
                responseObj.mErrorCode = ErrCode.XERR_HTTP_RESP_DATA;
                responseObj.mRespJsonObj = null;
            }

            long t2 = System.currentTimeMillis();
            ALog.getInstance().d(TAG, "<requestToServer> finished, response=" + response.toString()
                    + ", costTime=" + (t2-t1));
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
    private synchronized AgoraService.ResponseObj requestFileToServer(String baseUrl,
                                                                      String token,
                                                                      String fileName,
                                                                      String fileDir,
                                                                      boolean rename,
                                                                      byte[] fileContent ) {

        AgoraService.ResponseObj responseObj = new AgoraService.ResponseObj();

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
