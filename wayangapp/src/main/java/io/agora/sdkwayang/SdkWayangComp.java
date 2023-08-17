package io.agora.sdkwayang;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.base.BaseEvent;
import io.agora.iotlink.base.BaseThreadComp;
import io.agora.iotlink.ErrCode;


import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import io.agora.iotlink.logger.ALog;
import io.agora.sdkwayang.logger.WLog;
import io.agora.sdkwayang.protocol.ApiResult;
import io.agora.sdkwayang.protocol.BaseData;
import io.agora.sdkwayang.protocol.BaseDataQueue;
import io.agora.sdkwayang.transport.TransWyPacket;
import io.agora.sdkwayang.transport.WebsocketComp;
import io.agora.sdkwayang.util.EnumClass;
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
 * @brief SDK的Wayang组件，有独立的运行线程
 *        该组件内部使用 WebSocket组件与服务器通信，进行命令和执行结果的通信
 *        接收服务器发送过来的命令，调用SDK进行执行，并且将执行结果返回给服务器
 *        同时会将回调事件数据也返回给服务器
 * @author luxiaohua@agora.io
 * @date 2023/06/08
 */
public class SdkWayangComp extends BaseThreadComp
        implements WebsocketComp.ICallback {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTWY/SdkWyComp";

    //
    // The message Id
    //
    private static final int MSGID_WYCOMP_STATECHG = 0x0001;        ///< websocket状态变化
    private static final int MSGID_WYCOMP_RECVCMD = 0x0002;         ///< 接收到服务端命令




    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final Object mDataLock = new Object();       ///< 同步访问锁,类中所有变量需要进行加锁处理

    private Context mContext;           ///< 上下文
    private String mServerUrl;          ///< 配置的服务器地址
    private String mDeviceInfo;         ///< 配置的设备信息

    private String mCachePath;          ///< 缓存路径

    private WebsocketComp   mWebSocket;
    private SdkExecutor     mSdkExecutor;
    private BaseDataQueue   mCmdQueue = new BaseDataQueue();


    ////////////////////////////////////////////////////////////////////
    /////////////////////// Public Methods ////////////////////////////
    ////////////////////////////////////////////////////////////////////

    /**
     * @brief 初始化WebSocket组件
     */
    public int initialize(final Context ctx, final String serverUrl, final String devInfo) {
        mContext = ctx;
        mServerUrl = serverUrl;
        mDeviceInfo = devInfo;
        File file = mContext.getExternalFilesDir(null);
        mCachePath = file.getAbsolutePath();

        // 初始化日志系统，并且设置保存路径
        WLog.getInstance().initialize(mCachePath + "/SdkWayang.log");

        mCmdQueue.clear();
        mSdkExecutor = new SdkExecutor(ctx, new SdkExecutor.IEventCallback() {
            @Override
            public void sendCallbackToServer(String cmd, ConcurrentHashMap<String, Object> info,
                                             ConcurrentHashMap<String, Object> extra) {
                // 封装 json格式
                String eventString = ToolUtil.assembleAppActiveCallback(mDeviceInfo, cmd, info, extra);

                // 发送
                if (mWebSocket != null){
                    mWebSocket.sendPacket(eventString);
                }
            }
        });

        // 初始化 WebSocket 组件
        WebsocketComp.InitParam wsInitParam = new WebsocketComp.InitParam();
        wsInitParam.mServerUrl = mServerUrl;
        wsInitParam.mDeviceInfo = mDeviceInfo;
        mWebSocket = WebsocketComp.getInstance();
        int ret = mWebSocket.initialize(wsInitParam, this);

        // 启动组件线程
        runStart(TAG);

        WLog.getInstance().d(TAG, "<initialize> done, serverUrl=" + serverUrl
                + ", devInfo=" + devInfo);
        return ErrCode.XOK;
    }


    /**
     * @brief 释放WebSocket组件
     */
    public void release() {
        if (mWebSocket != null) {
            mWebSocket.release();
            mWebSocket = null;
        }

        // 停止组件线程
        runStop();

        mCmdQueue.clear();

        WLog.getInstance().d(TAG, "<release> done");
        WLog.getInstance().release();
    }

    /**
     * @brief 设置视频帧显示控件
     */
    public void setDisplayView(final View displayView) {
        mSdkExecutor.setDisplayView(displayView);
        WLog.getInstance().d(TAG, "<setDisplayView> done");
    }



    ///////////////////////////////////////////////////////////////////////////
    //////////////// Methods of Override WebsocketComp.ICallback /////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onWsStateChanged(final String serverUrl, final String deviceInfo, int state) {
        WLog.getInstance().d(TAG, "<onWsStateChanged> state=" + state);

        Message msg = new Message();
        msg.what = MSGID_WYCOMP_STATECHG;
        msg.arg1 = state;
        synchronized (mMsgQueueLock) {
            mWorkHandler.sendMessage(msg);
        }
     }

    @Override
    public void onWsRecvMessage(final String serverUrl, final String deviceInfo,
                                final BaseData recvData) {
        WLog.getInstance().d(TAG, "<onWsRecvMessage> recvData=" + recvData);

        mCmdQueue.inqueue(recvData);  // 接收到的命令插入队列中

        sendSingleMessage(MSGID_WYCOMP_RECVCMD, 0, 0, null, 0);
    }




    ///////////////////////////////////////////////////////////////////////////
    //////////////// Methods of Override BaseThreadComp //////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected void processWorkMessage(Message msg)   {
        switch (msg.what) {
            case MSGID_WYCOMP_STATECHG:
                onMessageWsStateChanged(msg);
                break;

            case MSGID_WYCOMP_RECVCMD:
                onMessageRecvCmd(msg);
                break;
        }
    }

    @Override
    protected void removeAllMessages() {
        synchronized (mMsgQueueLock) {
            mWorkHandler.removeMessages(MSGID_WYCOMP_STATECHG);
            mWorkHandler.removeMessages(MSGID_WYCOMP_RECVCMD);
        }
    }

    @Override
    protected void processTaskFinsh() {
        ALog.getInstance().d(TAG, "<processTaskFinsh> done!");
    }





    ///////////////////////////////////////////////////////////////////////////
    //////////////// Methods for thread message handler ///////////////////////
    //////////////////////////////////////////////////////////////////////////
    /**
     * @brief 组件线程消息处理：WebSocket状态变化
     */
    void onMessageWsStateChanged(Message msg) {
        int wsState = msg.arg1;
        WLog.getInstance().d(TAG, "<onMessageWsStateChanged> websocketState=" + wsState);
    }


    /**
     * @brief 组件线程消息处理：接收到服务器端的命令消息
     */
    void onMessageRecvCmd(Message msg) {
        BaseData cmdData = mCmdQueue.dequeue();
        if (cmdData == null) {  // 命令队列为空，没有要执行的命令了
        //    WLog.getInstance().d(TAG, "<onMessageRecvCmd> no more command in queue");
            return;
        }

        // 执行相应的命令
        executeCommand(cmdData);

        // 如何还有其他命令，则在下一次消息中执行
        if (mCmdQueue.size() > 0) {
            sendSingleMessage(MSGID_WYCOMP_RECVCMD, 0, 0, null, 0);
        } else {
        //    WLog.getInstance().d(TAG, "<onMessageRecvCmd> no command for nex execute");
        }
    }


    ////////////////////////////////////////////////////////////////////
    /////////////////////// Internal Methods ////////////////////////////
    ////////////////////////////////////////////////////////////////////
    private void executeCommand(final BaseData receiveData) {
        WLog.getInstance().d(TAG, "<executeCommand> ==>Enter, receiveData=" + receiveData);

        Object[] paramObjs = { receiveData };
        Class<? extends Object>[] ptypes = new Class<?>[]{BaseData.class};
        String methodHead = null;
        String execCallbackString = "";

        try {
            Method sdkMethod = SdkExecutor.class.getDeclaredMethod(receiveData.getCmd(), ptypes);
            ApiResult executeResult = (ApiResult) sdkMethod.invoke(mSdkExecutor, paramObjs);

            execCallbackString = ToolUtil.assembleSdkExecute(
                    mDeviceInfo,
                    receiveData.getCmd(),
                    receiveData.getSequence(),
                    EnumClass.ErrorType.TYPE_0,
                    executeResult.getApiResult(),
                    null);

        } catch (Exception exp) {
            exp.printStackTrace();
            String callback = Log.getStackTraceString(exp);
            WLog.getInstance().e(TAG, "<executeCommand> [EXCEPTION] exp=" + exp);

            int index = callback.indexOf("Caused by");
            if (index == -1) {
                index = 0;
            }
            execCallbackString = ToolUtil.assembleNonSdkAPIExecute(
                    mDeviceInfo,
                    receiveData.getCmd(),
                    receiveData.getSequence(),
                    EnumClass.ErrorType.TYPE_3,
                    callback.substring(index, index + 150),
                    null);
        }

        if (mWebSocket != null){
            mWebSocket.sendPacket(execCallbackString);
        }

        WLog.getInstance().d(TAG, "<executeCommand> <==Exit, execCallbackString=" + execCallbackString);
    }

}
