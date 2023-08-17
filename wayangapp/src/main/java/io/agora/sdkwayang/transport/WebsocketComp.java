package io.agora.sdkwayang.transport;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.base.BaseEvent;
import io.agora.iotlink.base.BaseThreadComp;
import io.agora.iotlink.ErrCode;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import io.agora.iotlink.logger.ALog;
import io.agora.sdkwayang.logger.WLog;
import io.agora.sdkwayang.protocol.BaseData;
import io.agora.sdkwayang.util.JsonUtil;
import io.agora.sdkwayang.util.ToolUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import androidx.annotation.NonNull;


/**
 * @brief WebSocket通信组件，有独立的运行线程
 * @author luxiaohua
 * @date 2023/06/07
 */
public class WebsocketComp extends BaseThreadComp {


    //
    // The state machine
    //
    public static final int STATE_DISCONNECTED = 0x0000;    ///< 已经断开连接
    public static final int STATE_CONNECTING = 0x0001;      ///< 正在连接中
    public static final int STATE_CONNECTED = 0x0002;       ///< 连接成功
    public static final int STATE_DISCONNECTING = 0x0003;   ///< 正在断开连接

    /**
     * @brief 回调接口
     */
    public interface ICallback {

        /**
         * @brief 状态变化事件
         * @param serverUrl : 服务器URL地址
         * @param deviceInfo : 设备信息
         * @param state : 状态机
         */
        default void onWsStateChanged(final String serverUrl, final String deviceInfo, int state) { }

        /**
         * @brief 接收到消息事件
         * @param serverUrl : 服务器URL地址
         * @param deviceInfo : 设备信息
         * @param recvData : 接收到的命令数据
         */
        default void onWsRecvMessage(final String serverUrl, final String deviceInfo,
                                   final BaseData recvData) { }

    }



    /**
     * @brief 组件初始化参数
     */
    public static class InitParam {
        public String mServerUrl = null;
        public String mDeviceInfo = null;

        @Override
        public String toString() {
            String infoText = "{ mServerUrl=" + mServerUrl
                    + ", mDeviceInfo=" + mDeviceInfo + " }";
            return infoText;
        }
    }

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTWY/WebsocketComp";
    private static final String PKT_HEART_BEAT = "ping";
    private static final long HEART_BEAT_INTERVAL = 20000;          ///< 定时20秒发一次心跳包

    private static final int NORMAL_CLOSURE_STATUS = 1000;
    public static final int WEBSOCKET_RECONNECT_WAITTING_INTERVAL = 4000;
    public static final int OKHTTPCLIENT_CONNECT_TIMEOUT = 3;       ///< 客户端连接超时 3秒
    public static final int OKHTTPCLIENT_READ_TIMEOUT = 3;          ///< 客户端读取超时 3秒
    public static final int OKHTTPCLIENT_WRITE_TIMEOUT = 3;         ///< 客户端写入超时 3秒


    //
    // The message Id
    //
    private static final int MSGID_WEBSOCKET_INIT = 0x1001;
    private static final int MSGID_WEBSOCKET_SEND = 0x1002;
    private static final int MSGID_WEBSOCKET_RECVED = 0x1003;
    private static final int MSGID_WEBSOCKET_RECONNECT = 0x1004;        ///< 错误时WebSocket重连处理
    private static final int MSGID_WEBSOCKET_HEARTBEAT = 0x1005;        ///< 定时10秒发送心跳包保活


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final Object mDataLock = new Object();       ///< 同步访问锁,类中所有变量需要进行加锁处理
    private static WebsocketComp mInstance = null;

    private InitParam mInitParam;
    private ICallback mCallback;
    private int mState = STATE_DISCONNECTED;

    private OkHttpClient mOkHttpClient;
    private WebSocket mSocket;
    private TransWyPktQueue mSendPktQueue = new TransWyPktQueue();      ///< 发送数据包队列


    ////////////////////////////////////////////////////////////////////
    /////////////////////// Public Methods ////////////////////////////
    ////////////////////////////////////////////////////////////////////
    public WebsocketComp() {
    }

    /**
     * @brief 获取唯一的实例对象
     */
    public static WebsocketComp getInstance() {
        if (mInstance == null) {
            synchronized (WebsocketComp.class) {
                if (mInstance == null) {
                    mInstance = new WebsocketComp();
                }
            }
        }
        return mInstance;
    }

    /**
     * @brief 初始化WebSocket组件
     */
    public int initialize(final InitParam initParam, final ICallback callback) {
        mInitParam = initParam;
        mCallback = callback;

        // 切换到正在连接状态
        setState(STATE_CONNECTING);

        mSendPktQueue.clear();
        runStart(TAG);

        sendSingleMessage(MSGID_WEBSOCKET_INIT, 0, 0, null, 0);
        WLog.getInstance().d(TAG, "<initialize> done, initParam=" + initParam);
        return ErrCode.XOK;
    }


    /**
     * @brief 释放WebSocket组件
     */
    public void release() {

        runStop();

        // 切换到断开连接状态
        setState(STATE_DISCONNECTED);

        mSendPktQueue.clear();

        WLog.getInstance().d(TAG, "<release> done");
    }

    /**
     * @brief 获取当前状态机
     */
    public int getState() {
        synchronized (mDataLock) {
            return mState;
        }
    }

    /**
     * @brief 设置当前状态机，只能内部调用
     * @param newState : 要设置的新状态机
     */
    private void setState(int newState) {
        synchronized (mDataLock) {
            mState = newState;
        }
    }

    /**
     * @brief 发送数据
     */
    public int sendPacket(final String pktData) {
        if (getState() != STATE_CONNECTED) {
            WLog.getInstance().e(TAG, "<sendData> bad state, state=" + getState());
            return ErrCode.XERR_BAD_STATE;
        }

        // 插入发送队列
        TransWyPacket sendPacket = new TransWyPacket();
        sendPacket.mContent = pktData;
        mSendPktQueue.inqueue(sendPacket);

        // 消息通知组件线程进行发送
        sendSingleMessage(MSGID_WEBSOCKET_SEND, 0, 0, null, 0);

        return ErrCode.XOK;
    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////// Methods for Override BaseThreadComp ///////////////////////
    //////////////////////////////////////////////////////////////////////////
    @Override
    protected void processWorkMessage(Message msg)   {
        switch (msg.what) {
            case MSGID_WEBSOCKET_INIT:
                onMessageWebsocketInit(msg);
                break;

            case MSGID_WEBSOCKET_SEND:
                onMessageWebsocketSend(msg);
                break;

            case MSGID_WEBSOCKET_RECVED:
                onMessageWebsocketRecved(msg);
                break;

            case MSGID_WEBSOCKET_RECONNECT:     // WebSocket重连
                onMessageWebsocketReconnect(msg);
                break;

            case MSGID_WEBSOCKET_HEARTBEAT:     // 定时发送心跳包
                onMessageWebsocketHeartBeat(msg);
                break;
        }

    }

    @Override
    protected void removeAllMessages() {
        synchronized (mMsgQueueLock) {
            mWorkHandler.removeMessages(MSGID_WEBSOCKET_INIT);
            mWorkHandler.removeMessages(MSGID_WEBSOCKET_SEND);
            mWorkHandler.removeMessages(MSGID_WEBSOCKET_RECVED);
            mWorkHandler.removeMessages(MSGID_WEBSOCKET_RECONNECT);
        }
    }

    @Override
    protected void processTaskFinsh() {

        websocketDestroy();
        ALog.getInstance().d(TAG, "<processTaskFinsh> done!");
    }



    ///////////////////////////////////////////////////////////////////////////
    //////////////// Methods for thread message handler ///////////////////////
    //////////////////////////////////////////////////////////////////////////
    /**
     * @brief 组件线程消息处理：初始化 websocket
     */
    void onMessageWebsocketInit(Message msg) {
        websocketCreate();
        WLog.getInstance().d(TAG, "<onMessageWebsocketInit> done");
    }

    /**
     * @brief 组件线程消息处理：WebSocket 发送数据
     */
    void onMessageWebsocketSend(Message msg) {
        if (getState() != STATE_CONNECTED) {
            ALog.getInstance().e(TAG, "<onMessageWebsocketSend> bad state, state=" + getState());
            return;
        }

        //
        // 将发送队列中的数据包依次发送出去
        //
        for (;;) {
            TransWyPacket sendingPkt = mSendPktQueue.dequeue();
            if (sendingPkt == null) {  // 发送队列为空，没有要发送的数据包了
                break;
            }

            boolean sendRslt = mSocket.send(sendingPkt.mContent);
            if (!sendRslt){
                WLog.getInstance().d(TAG, "<onMessageWebsocketSend> send packet failure, pkt="
                        + sendingPkt.mContent);

                // 重新插入到队列头，等下次重新发送
                mSendPktQueue.inqueueHead(sendingPkt);
                return;
            }
        }

        WLog.getInstance().d(TAG, "<onMessageWebsocketSend> done");
    }

    /**
     * @brief 组件线程消息处理：WebSocket接收到数据
     */
    void onMessageWebsocketRecved(Message msg) {
        WLog.getInstance().d(TAG, "<onMessageWebsocketRecved> done");
    }


    /**
     * @brief 组件线程消息处理：WebSocket重连操作
     */
    void onMessageWebsocketReconnect(Message msg) {
        WLog.getInstance().d(TAG, "<onMessageWebsocketReconnect> ==>Enter");

        websocketDestroy(); // 释放旧的websocket

        ThreadSleep(1000);

        websocketCreate();  // 创建新的websocket
    }

    /**
     * @brief 组件线程消息处理：定时发送心跳包
     */
    void onMessageWebsocketHeartBeat(Message msg) {
        sendPacket(PKT_HEART_BEAT);

        // 触发下一个定时器
        sendSingleMessage(MSGID_WEBSOCKET_HEARTBEAT, 0, 0, null, HEART_BEAT_INTERVAL);
    }


    void ThreadSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptExp) {
            interruptExp.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////
    /////////////////////// Internal Methods ////////////////////////////
    ////////////////////////////////////////////////////////////////////

    private boolean websocketCreate() {
        if (mSocket != null) {
            WLog.getInstance().d(TAG, "<websocketCreate> already initialized");
            return true;
        }


        try {
            mOkHttpClient = new OkHttpClient.Builder()
                    .readTimeout(OKHTTPCLIENT_READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(OKHTTPCLIENT_WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .connectTimeout(OKHTTPCLIENT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder().url(mInitParam.mServerUrl + mInitParam.mDeviceInfo).build();
            EchoWebSocketListener socketListener = new EchoWebSocketListener();
            mOkHttpClient.newWebSocket(request, socketListener);
            mOkHttpClient.dispatcher().executorService().shutdown();

        } catch (Exception exp) {
            exp.printStackTrace();
            WLog.getInstance().e(TAG, "<websocketCreate> [EXCEPTION] exp=" + exp);
            mOkHttpClient = null;
            mSocket = null;
            return false;
        }

        WLog.getInstance().d(TAG, "<websocketCreate> done");
        return true;
    }


    private synchronized void websocketDestroy() {
        if (mSocket != null) {
            mSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye!");
            mSocket = null;
        }

        if (mOkHttpClient != null) {
            mOkHttpClient.dispatcher().executorService().shutdown();
            mOkHttpClient = null;
            WLog.getInstance().d(TAG, "<websocketDestroy> done");
        }
    }


    private final class EchoWebSocketListener extends WebSocketListener {

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            WLog.getInstance().d(TAG, "<Listener.onOpen>");
            super.onOpen(webSocket, response);

            mSocket = webSocket;

            setState(STATE_CONNECTED);  // 切换到 已经连接 状态

            // 触发心跳包定时器
            sendSingleMessage(MSGID_WEBSOCKET_HEARTBEAT, 0, 0, null, HEART_BEAT_INTERVAL);

            if (mCallback != null) {
                mCallback.onWsStateChanged(mInitParam.mServerUrl, mInitParam.mDeviceInfo, STATE_CONNECTED);
            }
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            WLog.getInstance().d(TAG, "<Listener.onClosed> code=" + code + ", reason=" + reason);
            super.onClosed(webSocket, code, reason);
            mSocket = null;

            setState(STATE_DISCONNECTED);  // 切换到 断开连接 状态
            if (mCallback != null) {
                mCallback.onWsStateChanged(mInitParam.mServerUrl, mInitParam.mDeviceInfo, STATE_DISCONNECTED);
            }
        }

        @Override
        public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            WLog.getInstance().d(TAG, "<Listener.onClosing> code=" + code + ", reason=" + reason);
            super.onClosing(webSocket, code, reason);

            setState(STATE_DISCONNECTING);  // 切换到 正在断连 状态
            if (mCallback != null) {
                mCallback.onWsStateChanged(mInitParam.mServerUrl, mInitParam.mDeviceInfo, STATE_DISCONNECTING);
            }
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @NonNull Response response) {
            WLog.getInstance().d(TAG, "<Listener.onFailure> t=" + t + ", response=" + response);
            super.onFailure(webSocket, t, response);
            mSocket = null;

            setState(STATE_DISCONNECTED);  // 切换到 断开连接 状态
            if (mCallback != null) {
                mCallback.onWsStateChanged(mInitParam.mServerUrl, mInitParam.mDeviceInfo, STATE_DISCONNECTED);
            }

            // 四秒后进行 WebSocket重连操作
            sendSingleMessage(MSGID_WEBSOCKET_RECONNECT, 0, 0, null, 4000);
        }


        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
            super.onMessage(webSocket, bytes);
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String reciveInfo) {
            WLog.getInstance().d(TAG, "<Listener.onMessage> reciveInfo=" + reciveInfo);
            super.onMessage(webSocket, reciveInfo);
            if (reciveInfo.length() <= 0) {
                return;
            }
            BaseData baseData = null;
            try {
                baseData = JsonUtil.packageToBaseData(reciveInfo);
            } catch (Exception exp) {
                exp.printStackTrace();
                WLog.getInstance().e(TAG, "<Listener.onMessage> exp=" + exp);
                sendFormatError(reciveInfo);
                return;
            }
            if (baseData == null) {
                return;
            }

            if (baseData.getDevice() != null) {
                if (!mInitParam.mDeviceInfo.equals(baseData.getDevice())) {
                    return;
                }
            }
            int type = baseData.getType();
            if (type != 1 && type != 2 && type != 3 && type != 8 &&type != 15) {
                WLog.getInstance().e(TAG, "<Listener.onMessage> [ERROR] type=" + type);
                return;
            }

            // 回调给上层
            if (mCallback != null) {
                mCallback.onWsRecvMessage(mInitParam.mServerUrl, mInitParam.mDeviceInfo, baseData);
            }
        }

    }

    /**
     * @brief 发送 格式错误的数据包 给服务器
     */
    private void sendFormatError(String originMsg) {
        ConcurrentHashMap<String, Object> extra = new ConcurrentHashMap<>(1);
        extra.put("formatError", "Message Format is error");
        extra.put("originMsg", originMsg);
        String formatErrorResult = ToolUtil.assembleFormateErrorCallback(
                mInitParam.mDeviceInfo, null, null, extra);

        // 发送数据包
        sendPacket(formatErrorResult);
    }

//
//    private final Runnable mFrameProducer = new Runnable() {
//        @Override
//        public void run() {
//            if (isThreadStarted) {
//                sendSocketMessage(ConstantApp.MSG_HEART_BEAT);
//                mHeartHandler.postDelayed(mFrameProducer, mFrameProducerIntervalMillis);
//            }
//        }
//    };
//
//    public void sendCaseInfo(Context context, String caseInfo) {
//        if (caseInfo != null){
//            CrashReport.putUserData(context, "crashInfo", caseInfo);
//        }
//    }
}
