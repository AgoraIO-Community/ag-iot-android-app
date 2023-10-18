package io.agora.iotlink.rtmsdk;


import android.content.Context;
import android.os.Message;
import android.text.TextUtils;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.base.AtomicInteger;
import io.agora.iotlink.base.BaseThreadComp;
import io.agora.iotlink.callkit.SessionCtx;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.sdkimpl.AgoraIotAppSdk;
import io.agora.iotlink.sdkimpl.CallkitMgr;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmMessage;
import io.agora.rtm.RtmMessageType;
import io.agora.rtm.RtmStatusCode;
import io.agora.rtm.SendMessageOptions;


/**
 * @brief RTM消息管理组件，有独立的运行线程
 */
public class RtmMgrComp extends BaseThreadComp {

    /**
     * @brief RTM组件回调
     */
    public static interface IRtmMgrCallback {
        /**
         * @brief 接收到设备端的RTM数据包事件
         * @param recvedPacket : 接收到的数据包
         */
        default void onRtmRecvedPacket(final RtmPacket recvedPacket) {  }

        /**
         * @brief RTM状态发生变化时回调
         * @param newState : 当前新的RTM状态
         */
        void onRtmStateChanged(int newState);
    }




    /**
     * @brief RTM组件初始化参数
     */
    public static class InitParam {
        public Context mContext;
        public String mRtmAppId;
        public AgoraIotAppSdk mSdkInstance;
        public IRtmMgrCallback mCallback;
    }



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/RtmMgrComp";
    private static final long TIMER_INTERVAL = 120000;               ///< 定时器间隔 2分钟
    private static final long HEARTBEAT_INTVAL = 300000;             ///< 心跳包定时5分钟发送一次
    private static final String HEARTBEAT_CONTENT = "{ }";
    private static final long RETRY_DELAY_TIME = 10000;              ///< 登录失败后延迟10秒后重新登录

    //
    // RTM的状态机
    //
    public static final int RTM_STATE_IDLE = 0x0000;               ///< 还未登录状态
    public static final int RTM_STATE_LOGINING = 0x0001;           ///< 正在登录中
    public static final int RTM_STATE_RENEWING = 0x0002;           ///< 正在RenewToken中
    public static final int RTM_STATE_LOGOUTING = 0x0003;          ///< 正在登出中
    public static final int RTM_STATE_RUNNING = 0x0004;            ///< 正常运行状态

    //
    // The message Id
    //
    private static final int MSGID_RTM_BASE = 0x2000;
    private static final int MSGID_RTM_SEND_PKT = 0x2001;           ///< 处理数据包接收
    private static final int MSGID_RTM_RECV_PKT = 0x2002;           ///< 处理数据包接收
    private static final int MSGID_RTM_CONNECT_DEV = 0x2003;        ///< 连接到设备
    private static final int MSGID_RTM_LOGIN_DONE = 0x2004;         ///< 登录完成消息
    private static final int MSGID_RTM_LOGOUT_DONE = 0x2005;        ///< 登出完成消息（暂时用不到）
    private static final int MSGID_RTM_RENEWTOKEN_DONE = 0x2006;    ///< token刷新完成消息
    private static final int MSGID_RTM_TIMER = 0x2009;              ///< 定时广播消息，防止无消息退出
    private static final int MSGID_RTM_STATE_ABORT = 0x200A;        ///<

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final Object mDataLock = new Object();       ///< 同步访问锁,类中所有变量需要进行加锁处理
    private InitParam mInitParam;                               ///< 初始化参数
    private RtmClient mRtmClient;                               ///< RTM客户端实例
    private AtomicInteger mState = new AtomicInteger();         ///< RTM状态机
    private SendMessageOptions mSendMsgOptions;                 ///< RTM消息配置
    private long mHeartbeatTimestamp = 0;                       ///< 上次发送心跳包的时间戳

    private RtmPktQueue mRecvPktQueue = new RtmPktQueue();  ///< 接收数据包队列
    private RtmPktQueue mSendPktQueue = new RtmPktQueue();  ///< 发送数据包队列






    ///////////////////////////////////////////////////////////////////////
    /////////////////////////// Public Methods ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    /**
     * @brief 初始化 RTM组件
     */
    public int initialize(final InitParam initParam) {
        long t1 = System.currentTimeMillis();
        mInitParam = initParam;

        mRecvPktQueue.clear();
        mSendPktQueue.clear();

        int ret = rtmEngCreate();
        if (ret != ErrCode.XOK) {
            return ret;
        }
        mState.setValue(RTM_STATE_IDLE);  // 未登录状态
        mHeartbeatTimestamp = System.currentTimeMillis();

        // 启动组件线程
        runStart(TAG);

        // 启动定时器消息
        sendSingleMessage(MSGID_RTM_TIMER, 0, 0, null, TIMER_INTERVAL);

        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<initialize> done, costTime=" + (t2-t1));
        return ErrCode.XOK;
    }

    /**
     * @brief 销毁 RTM组件
     */
    public void release() {
        long t1 = System.currentTimeMillis();
        // 停止组件线程
        runStop();

        rtmEngDestroy();
        mRecvPktQueue.clear();
        mSendPktQueue.clear();
        mState.setValue(RTM_STATE_IDLE);  // 未登录状态

        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<release> done, costTime=" + (t2-t1));
    }

    /**
     * @brief 获取RTM组件状态
     */
    public int getState() {
        return mState.getValue();
    }

    /**
     * @brief 连接到某个设备
     */
    public int connectToDevice(final String localUserId, final String rtmToken) {
        // 发送消息处理
        Object[] params = { localUserId, rtmToken};
        sendSingleMessage(MSGID_RTM_CONNECT_DEV, 0, 0, params, 0);

        ALog.getInstance().d(TAG, "<connectToDevice> localUserId=" + localUserId
                + ", rtmToken=" + rtmToken);
        return ErrCode.XOK;
    }


    /**
     * @brief 发送命令到设备
     */
    public int sendPacketToDev(final SessionCtx sessionCtx, final String cmdData,
                               final ICallkitMgr.OnCmdSendListener sendListener) {
        int rtmState = mState.getValue();
        if ((rtmState != RTM_STATE_RUNNING) && (rtmState != RTM_STATE_RENEWING)) {
            ALog.getInstance().e(TAG, "<sendPacketToDev> bad state, rtmState=" + rtmState);
            return ErrCode.XERR_BAD_STATE;
        }

        // 发送消息处理
        RtmPacket packet = new RtmPacket();
        packet.mPeerId = sessionCtx.mDevNodeId;
        packet.mPktData = cmdData;
        packet.mIsRecvPkt = false;
        packet.mSessionId = sessionCtx.mSessionId;
        packet.mSendTimestamp = System.currentTimeMillis();
        packet.mSendListener = sendListener;
        mSendPktQueue.inqueue(packet);
        sendSingleMessage(MSGID_RTM_SEND_PKT, 0, 0, null, 0);

        ALog.getInstance().d(TAG, "<sendPacketToDev> packet=" + packet);
        return ErrCode.XOK;
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////////// Methods for Override BaseThreadComp //////////////////////
    //////////////////////////////////////////////////////////////////////////
    @Override
    public void processWorkMessage(Message msg) {
        switch (msg.what) {
            case MSGID_RTM_SEND_PKT:
                onMessageSendPkt(msg);
                break;

            case MSGID_RTM_RECV_PKT:
                onMessageRecvPkt(msg);
                break;

            case MSGID_RTM_CONNECT_DEV:
                onMessageConnectToDev(msg);
                break;

            case MSGID_RTM_LOGIN_DONE:
                onMessageLoginDone(msg);
                break;

            case MSGID_RTM_RENEWTOKEN_DONE:
                onMessageRenewTokenDone(msg);
                break;

            case MSGID_RTM_STATE_ABORT:
                onMessageStateAbort(msg);
                break;

            case MSGID_RTM_TIMER:
                onMessageTimer(msg);
                break;
        }
    }

    @Override
    protected void removeAllMessages() {
        synchronized (mMsgQueueLock) {
            mWorkHandler.removeMessages(MSGID_RTM_SEND_PKT);
            mWorkHandler.removeMessages(MSGID_RTM_RECV_PKT);
            mWorkHandler.removeMessages(MSGID_RTM_CONNECT_DEV);
            mWorkHandler.removeMessages(MSGID_RTM_LOGIN_DONE);
            mWorkHandler.removeMessages(MSGID_RTM_RENEWTOKEN_DONE);
            mWorkHandler.removeMessages(MSGID_RTM_TIMER);
        }
        ALog.getInstance().d(TAG, "<removeAllMessages> done");
    }

    @Override
    protected void processTaskFinsh() {
        ALog.getInstance().d(TAG, "<processTaskFinsh> done");
    }


    /**
     * @brief 工作线程中运行，连接到设备
     */
    void onMessageConnectToDev(Message msg) {
        Object[] params = (Object[])msg.obj;
        String localUserId = (String)params[0];
        String rtmToken = (String)params[1];


        int state = mState.getValue();
        if (state == RTM_STATE_IDLE) {  // RTM还没有进行登录
            mState.setValue(RTM_STATE_LOGINING);  // 切换到正在登录状态
            if (mInitParam.mCallback != null) {
                mInitParam.mCallback.onRtmStateChanged(RTM_STATE_LOGINING);
            }

            rtmEngLogin(rtmToken, localUserId);
            ALog.getInstance().d(TAG, "<onMessageConnectToDev> done, login with token");

        } else {  // 已经登录，进行Token更新操作
            if (!TextUtils.isEmpty(rtmToken)) {
                mState.setValue(RTM_STATE_RENEWING);  // 切换到正在RenewToking状态
                if (mInitParam.mCallback != null) {
                    mInitParam.mCallback.onRtmStateChanged(RTM_STATE_RENEWING);
                }

                rtmEngRenewToken(rtmToken);
                ALog.getInstance().d(TAG, "<onMessageConnectToDev> done, renew token");
            } else {
                ALog.getInstance().d(TAG, "<onMessageConnectToDev> done, need NOT renew token");
            }
        }
    }

    /**
     * @brief 工作线程中运行，登录完成
     */
    void onMessageLoginDone(Message msg) {
        int errCode = msg.arg1;
        Object[] params = (Object[])msg.obj;
        ALog.getInstance().d(TAG, "<onMessageLoginDone> done, errCode=" + errCode);

        if (errCode != ErrCode.XOK) {
            mState.setValue(RTM_STATE_IDLE);  // 登录失败，切换到 未登录状态
            if (mInitParam.mCallback != null) {
                mInitParam.mCallback.onRtmStateChanged(RTM_STATE_IDLE);
            }

            // 延迟一段时间后重新登录
            ALog.getInstance().d(TAG, "<onMessageLoginDone> retry login...");
            sendSingleMessage(MSGID_RTM_CONNECT_DEV, 0, 0, params, RETRY_DELAY_TIME);

        } else {
            mState.setValue(RTM_STATE_RUNNING);  // 登录成功，切换到 运行状态
            if (mInitParam.mCallback != null) {
                mInitParam.mCallback.onRtmStateChanged(RTM_STATE_RUNNING);
            }
        }
    }

    /**
     * @brief 工作线程中运行，RenewToken完成
     */
    void onMessageRenewTokenDone(Message msg) {
        int errCode = msg.arg1;

        if (errCode != ErrCode.XOK) {
            mState.setValue(RTM_STATE_RUNNING);  // Renew失败，切换到 运行状态

        } else {
            mState.setValue(RTM_STATE_RUNNING);  // Renew成功，切换到 运行状态
        }
        if (mInitParam.mCallback != null) {
            mInitParam.mCallback.onRtmStateChanged(RTM_STATE_RUNNING);
        }

        ALog.getInstance().d(TAG, "<onMessageRenewTokenDone> done, errCode=" + errCode);
    }

    /**
     * @brief 工作线程中运行，处理发送RTM数据包
     */
    void onMessageSendPkt(Message msg) {
        RtmPacket sendPkt = mSendPktQueue.dequeue();
        if (sendPkt == null) {  // 发送队列为空，没有必要处理发送消息了
            return;
        }

        // 发送数据包
        rtmEngSendData(sendPkt);

        // 队列中还有数据包，放到下次发送消息中处理
        if (mSendPktQueue.size() > 0) {
            sendSingleMessage(MSGID_RTM_SEND_PKT, 0, 0, null, 0);
        }
    }

    /**
     * @brief 工作线程中运行，处理接收RTM数据包
     */
    void onMessageRecvPkt(Message msg) {
        for (;;) {
            RtmPacket recvedPkt = mRecvPktQueue.dequeue();
            if (recvedPkt == null) {  // 接收队列为空，没有要接收到的数据包要分发了
                return;
            }

            //
            // 回调给上一层接收到数据包
            //
            if (mInitParam.mCallback != null) {
                mInitParam.mCallback.onRtmRecvedPacket(recvedPkt);
            }
        }
    }

    /**
     * @brief 工作线程中运行，RTM账号被踢
     */
    void onMessageStateAbort(Message msg) {
        ALog.getInstance().d(TAG, "<onMessageStateAbort> ");
        mState.setValue(RTM_STATE_IDLE);
        if (mInitParam.mCallback != null) {
            mInitParam.mCallback.onRtmStateChanged(RTM_STATE_IDLE);
        }
    }

    /**
     * @brief 工作线程中运行，定时处理消息
     */
    void onMessageTimer(Message msg) {

        //
        // TODO: 不再定时给所有设备发送心跳空包，这部分属于业务逻辑，放到业务层实现
        //
        if (mState.getValue() != RTM_STATE_RUNNING) { // 如果当前RMT没有ready，则等10秒后再看
            sendSingleMessage(MSGID_RTM_TIMER, 0, 0, null, 10000L);
            return;
        }
//        long interval = System.currentTimeMillis() - mHeartbeatTimestamp;
//        if (interval > HEARTBEAT_INTVAL) {
//            mHeartbeatTimestamp = System.currentTimeMillis();
//
//            // 轮询正在会话的各个设备，依次发送心跳处理包
//            CallkitMgr callkitMgr = (CallkitMgr) mInitParam.mSdkInstance.getCallkitMgr();
//            List<SessionCtx> sessionList = callkitMgr.getAllSessionList();
//            for (SessionCtx sessionCtx: sessionList) {
//                RtmPacket packet = new RtmPacket();
//                packet.mPeerId = sessionCtx.mDevNodeId;
//                packet.mPktData = HEARTBEAT_CONTENT;
//                packet.mIsRecvPkt = false;
//                packet.mSessionId = sessionCtx.mSessionId;
//                packet.mSendTimestamp = System.currentTimeMillis();
//                packet.mSendListener = null;
//                mSendPktQueue.inqueue(packet);
//            }
//
//            sendSingleMessage(MSGID_RTM_SEND_PKT, 0, 0, null, 0);
//        }

        // 下次定时器处理
        sendSingleMessage(MSGID_RTM_TIMER, 0, 0, null, TIMER_INTERVAL);
    }



    ///////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Methods for Rtm SDK ////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 初始化 RtmSdk
     */
    private int rtmEngCreate() {
        mSendMsgOptions = new SendMessageOptions();

        RtmClientListener rtmListener = new RtmClientListener() {
            @Override
            public void onConnectionStateChanged(int state, int reason) {   //连接状态改变
                ALog.getInstance().d(TAG, "<rtmEngCreate.onConnectionStateChanged> state=" + state
                        + ", reason=" + reason);
                if (state == RtmStatusCode.ConnectionState.CONNECTION_STATE_ABORTED) {
                    sendSingleMessage(MSGID_RTM_STATE_ABORT, 0, 0, null, 0);
                }
            }

            @Override
            public void onMessageReceived(RtmMessage rtmMessage, String peerId) {   // 收到RTM消息
                int rtmMsgType = rtmMessage.getMessageType();
                String messageText = null;
                if (rtmMsgType == RtmMessageType.TEXT) {  // 文本格式
                    messageText = rtmMessage.getText();

                } else if (rtmMsgType == RtmMessageType.RAW) {  // 数据流格式
                    byte[] rawMessage = rtmMessage.getRawMessage();
                    try {
                        messageText = new String(rawMessage, "UTF-8");
                    } catch (UnsupportedEncodingException encExp) {
                        encExp.printStackTrace();
                        ALog.getInstance().e(TAG, "<rtmEngCreate.onMessageReceived> [EXP] encExp=" + encExp);
                    }

                } else {
                    ALog.getInstance().e(TAG, "<rtmEngCreate.onMessageReceived> rtmMsgType=" + rtmMsgType);
                    return;
                }
                ALog.getInstance().d(TAG, "<rtmEngCreate.onMessageReceived> messageText=" + messageText
                        + ", peerId=" + peerId);

                RtmPacket packet = new RtmPacket();
                packet.mPeerId = peerId;
                packet.mPktData = messageText;
                packet.mIsRecvPkt = true;
                packet.mRecvTimestamp = System.currentTimeMillis();
                mRecvPktQueue.inqueue(packet);
                sendSingleMessage(MSGID_RTM_RECV_PKT, 0, 0, null, 0);
            }

            @Override
            public void onTokenExpired() {
                ALog.getInstance().d(TAG, "<rtmEngCreate.onTokenExpired>");
             }

            @Override
            public void onTokenPrivilegeWillExpire() {
                ALog.getInstance().d(TAG, "<rtmEngCreate.onTokenPrivilegeWillExpire>");
            }

            @Override
            public void onPeersOnlineStatusChanged(Map<String, Integer> peersStatus) {
                ALog.getInstance().d(TAG, "<rtmEngCreate.onPeersOnlineStatusChanged> peersStatus=" + peersStatus);
            }
        };

        try {
            mRtmClient = RtmClient.createInstance(mInitParam.mContext, mInitParam.mRtmAppId, rtmListener);
        } catch (Exception exp) {
            exp.printStackTrace();
            ALog.getInstance().e(TAG, "<rtmEngCreate> [EXCEPTION] create rtmp, exp=" + exp.toString());
            return ErrCode.XERR_UNSUPPORTED;
        }

        ALog.getInstance().d(TAG, "<rtmEngCreate> done");
        return ErrCode.XOK;
    }

    /**
     * @brief 释放 RtmSDK
     */
    private void rtmEngDestroy()
    {
        if (mRtmClient != null) {
            mRtmClient.release();
            mRtmClient = null;
            ALog.getInstance().d(TAG, "<rtmEngDestroy> done");
        }
    }


    /**
     * @brief 登录用户账号
     */
    private int rtmEngLogin(final String token, final String userId)
    {
        if (mRtmClient == null) {
            return ErrCode.XERR_BAD_STATE;
        }

        mRtmClient.login(token, userId, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void responseInfo) {
                ALog.getInstance().d(TAG, "<rtmEngLogin.onSuccess> success");
                Object[] params = { userId, token};
                sendSingleMessage(MSGID_RTM_LOGIN_DONE, ErrCode.XOK, 0, params, 0);
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                ALog.getInstance().i(TAG, "<rtmEngLogin.onFailure> failure"
                        + ", errInfo=" + errorInfo.getErrorCode()
                        + ", errDesc=" + errorInfo.getErrorDescription());
                int errCode = mapRtmLoginErrCode(errorInfo.getErrorCode());
                Object[] params = { userId, token};
                sendSingleMessage(MSGID_RTM_LOGIN_DONE, errCode, 0, params, 0);
            }
        });

        ALog.getInstance().d(TAG, "<rtmEngLogin> done, token=" + token + ", userId=" + userId);
        return ErrCode.XOK;
    }

    /**
     * @brief 更新token
     */
    private int rtmEngRenewToken(final String token)
    {
        if (mRtmClient == null) {
            return ErrCode.XERR_BAD_STATE;
        }

        mRtmClient.renewToken(token, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void responseInfo) {
                ALog.getInstance().d(TAG, "<rtmEngRenewToken.onSuccess> success");
                sendSingleMessage(MSGID_RTM_RENEWTOKEN_DONE, ErrCode.XOK, 0, null, 0);
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                ALog.getInstance().i(TAG, "<rtmEngRenewToken.onFailure> failure"
                        + ", errInfo=" + errorInfo.getErrorCode()
                        + ", errDesc=" + errorInfo.getErrorDescription());
                int errCode = mapRtmRenewErrCode(errorInfo.getErrorCode());
                sendSingleMessage(MSGID_RTM_RENEWTOKEN_DONE, errCode, 0, null, 0);
            }
        });

        ALog.getInstance().d(TAG, "<rtmEngRenewToken> done, token=" + token);
        return ErrCode.XOK;
    }


    /**
     * @brief 发送消息到对端
     */
    private int rtmEngSendData(final RtmPacket rtmPacket) {
        if (mRtmClient == null) {
            return ErrCode.XERR_BAD_STATE;
        }

        RtmMessage rtmMsg = mRtmClient.createMessage(rtmPacket.mPktData.getBytes(StandardCharsets.UTF_8));
        mRtmClient.sendMessageToPeer(rtmPacket.mPeerId, rtmMsg, mSendMsgOptions, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                ALog.getInstance().d(TAG, "<rtmEngSendData.onSuccess>");

                if (rtmPacket.mSendListener != null) {
                    rtmPacket.mSendListener.onCmdSendDone(ErrCode.XOK);
                }
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                ALog.getInstance().i(TAG, "<rtmEngSendData.onFailure> failure"
                        + ", errInfo=" + errorInfo.getErrorCode()
                        + ", errDesc=" + errorInfo.getErrorDescription());
                int errCode = mapRtmMsgErrCode(errorInfo.getErrorCode());

                if (rtmPacket.mSendListener != null) {
                    rtmPacket.mSendListener.onCmdSendDone(errCode);
                }
            }
        });

        ALog.getInstance().d(TAG, "<sendMessage> done, rtmPacket=" + rtmPacket);
        return ErrCode.XOK;
    }



    ///////////////////////////////////////////////////////////////////////////
    ////////////////////// Methods for Mapping Error Code /////////////////////
    ////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 映射RTM登录的错误码到全局统一的错误码
     */
    private int mapRtmLoginErrCode(int rtmErrCode) {
        switch (rtmErrCode) {
            case RtmStatusCode.LoginError.LOGIN_ERR_UNKNOWN:
                return ErrCode.XERR_RTMMGR_LOGIN_UNKNOWN;

            case RtmStatusCode.LoginError.LOGIN_ERR_REJECTED:
                return ErrCode.XERR_RTMMGR_LOGIN_REJECTED;

            case RtmStatusCode.LoginError.LOGIN_ERR_INVALID_ARGUMENT:
                return ErrCode.XERR_RTMMGR_LOGIN_INVALID_ARGUMENT;

            case RtmStatusCode.LoginError.LOGIN_ERR_INVALID_APP_ID:
                return ErrCode.XERR_RTMMGR_LOGIN_INVALID_ARGUMENT;

            case RtmStatusCode.LoginError.LOGIN_ERR_INVALID_TOKEN:
                return ErrCode.XERR_RTMMGR_LOGIN_INVALID_TOKEN;

            case RtmStatusCode.LoginError.LOGIN_ERR_TOKEN_EXPIRED:
                return ErrCode.XERR_RTMMGR_LOGIN_TOKEN_EXPIRED;

            case RtmStatusCode.LoginError.LOGIN_ERR_NOT_AUTHORIZED:
                return ErrCode.XERR_RTMMGR_LOGIN_NOT_AUTHORIZED;

            case RtmStatusCode.LoginError.LOGIN_ERR_ALREADY_LOGIN:
                return ErrCode.XERR_RTMMGR_LOGIN_ALREADY_LOGIN;

            case RtmStatusCode.LoginError.LOGIN_ERR_TIMEOUT:
                return ErrCode.XERR_RTMMGR_LOGIN_TIMEOUT;

            case RtmStatusCode.LoginError.LOGIN_ERR_TOO_OFTEN:
                return ErrCode.XERR_RTMMGR_LOGIN_TOO_OFTEN;

            case RtmStatusCode.LoginError.LOGIN_ERR_NOT_INITIALIZED:
                return ErrCode.XERR_RTMMGR_LOGIN_NOT_INITIALIZED;
        }

        return ErrCode.XOK;
    }

    /**
     * @brief 映射RTM登出的错误码到全局统一的错误码
     */
    private int mapRtmLogoutErrCode(int rtmErrCode) {
        switch (rtmErrCode) {
            case RtmStatusCode.LogoutError.LOGOUT_ERR_REJECTED:
                return ErrCode.XERR_RTMMGR_LOGOUT_REJECT;

            case RtmStatusCode.LogoutError.LOGOUT_ERR_NOT_INITIALIZED:
                return ErrCode.XERR_RTMMGR_LOGOUT_NOT_INITIALIZED;

            case RtmStatusCode.LogoutError.LOGOUT_ERR_USER_NOT_LOGGED_IN:
                return ErrCode.XERR_RTMMGR_LOGOUT_NOT_LOGGED_IN;
        }

        return ErrCode.XOK;
    }

    /**
     * @brief 映射RTM Renew token的错误码到全局统一的错误码
     */
    private int mapRtmRenewErrCode(int rtmErrCode) {
        switch (rtmErrCode) {
            case RtmStatusCode.RenewTokenError.RENEW_TOKEN_ERR_FAILURE:
                return ErrCode.XERR_RTMMGR_RENEW_FAILURE;

            case RtmStatusCode.RenewTokenError.RENEW_TOKEN_ERR_INVALID_ARGUMENT:
                return ErrCode.XERR_RTMMGR_RENEW_INVALID_ARGUMENT;

            case RtmStatusCode.RenewTokenError.RENEW_TOKEN_ERR_REJECTED:
                return ErrCode.XERR_RTMMGR_RENEW_REJECTED;

            case RtmStatusCode.RenewTokenError.RENEW_TOKEN_ERR_TOO_OFTEN:
                return ErrCode.XERR_RTMMGR_RENEW_TOO_OFTEN;

            case RtmStatusCode.RenewTokenError.RENEW_TOKEN_ERR_TOKEN_EXPIRED:
                return ErrCode.XERR_RTMMGR_RENEW_TOKEN_EXPIRED;

            case RtmStatusCode.RenewTokenError.RENEW_TOKEN_ERR_INVALID_TOKEN:
                return ErrCode.XERR_RTMMGR_RENEW_INVALID_TOKEN;

            case RtmStatusCode.RenewTokenError.RENEW_TOKEN_ERR_NOT_INITIALIZED:
                return ErrCode.XERR_RTMMGR_RENEW_NOT_INITIALIZED;

            case RtmStatusCode.RenewTokenError.RENEW_TOKEN_ERR_USER_NOT_LOGGED_IN:
                return ErrCode.XERR_RTMMGR_RENEW_NOT_LOGGED_IN;
        }

        return ErrCode.XOK;
    }


    /**
     * @brief 映射RTM的消息错误码到全局统一的错误码
     */
    private int mapRtmMsgErrCode(int msgErrCode) {
        switch (msgErrCode) {
            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_FAILURE:
                return ErrCode.XERR_RTMMGR_MSG_FAILURE;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_TIMEOUT:
                return ErrCode.XERR_RTMMGR_MSG_TIMEOUT;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_PEER_UNREACHABLE:
                return ErrCode.XERR_RTMMGR_MSG_PEER_UNREACHABLE;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_CACHED_BY_SERVER:
                return ErrCode.XERR_RTMMGR_MSG_CACHED_BY_SERVER;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_TOO_OFTEN:
                return ErrCode.XERR_RTMMGR_MSG_TOO_OFTEN;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_INVALID_USERID:
                return ErrCode.XERR_RTMMGR_MSG_INVALID_USERID;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_INVALID_MESSAGE:
                return ErrCode.XERR_RTMMGR_MSG_INVALID_MESSAGE;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_IMCOMPATIBLE_MESSAGE:
                return ErrCode.XERR_RTMMGR_MSG_IMCOMPATIBLE_MESSAGE;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_NOT_INITIALIZED:
                return ErrCode.XERR_RTMMGR_MSG_NOT_INITIALIZED;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_USER_NOT_LOGGED_IN:
                return ErrCode.XERR_RTMMGR_MSG_USER_NOT_LOGGED_IN;
        }

        return ErrCode.XOK;
    }

}
