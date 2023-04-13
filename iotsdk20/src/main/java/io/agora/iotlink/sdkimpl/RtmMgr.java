/**
 * @file RtmMgr.java
 * @brief This file implement the RTM management
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-08-11
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.sdkimpl;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.IRtmMgr;
import io.agora.iotlink.IotDevice;
import io.agora.iotlink.callkit.AgoraService;
import io.agora.iotlink.logger.ALog;

import java.util.ArrayList;
import java.util.Map;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmFileMessage;
import io.agora.rtm.RtmImageMessage;
import io.agora.rtm.RtmMediaOperationProgress;
import io.agora.rtm.RtmMessage;
import io.agora.rtm.RtmStatusCode;
import io.agora.rtm.SendMessageOptions;

/**
 * @brief RTM系统管理器
 */
public class RtmMgr implements IRtmMgr {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/RtmMgr";

    private static final int CONNECTION_STATE_DISCONNECTED = 1;
    private static final int CONNECTION_STATE_CONNECTING = 2;
    private static final int CONNECTION_STATE_CONNECTED = 3;
    private static final int CONNECTION_STATE_RECONNECTING = 4;
    private static final int CONNECTION_STATE_ABORTED = 5;


    //
    // The mesage Id
    //
    private static final int MSGID_RTMMGR_BASE = 0x6000;
    private static final int MSGID_RTMMGR_CONNECT = 0x6001;
    private static final int MSGID_RTMMGR_CONNECT_DONE = 0x6002;
    private static final int MSGID_RTMMGR_DISCONNECT = 0x6003;


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private ArrayList<IRtmMgr.ICallback> mCallbackList = new ArrayList<>();
    private AgoraIotAppSdk mSdkInstance;                        ///< 由外部输入的
    private Handler mWorkHandler;                               ///< 工作线程Handler，从SDK获取到
    private Handler mEntryHandler;                              ///< 入口调用者线程Handler
    IAgoraIotAppSdk.InitParam mSdkInitParam;

    private static final Object mDataLock = new Object();       ///< 同步访问锁,类中所有变量需要进行加锁处理
    private RtmClient mRtmClient;                               ///< RTM Client SDK
    private IotDevice mPeerDevice;
    private SendMessageOptions mSendMsgOptions;                 ///< RTM消息配置
    private volatile int mStateMachine = RTMMGR_STATE_DISCONNECTED;  ///< 当前呼叫状态机

    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    int initialize(AgoraIotAppSdk sdkInstance) {
        mSdkInstance = sdkInstance;
        mWorkHandler = sdkInstance.getWorkHandler();

        mSdkInitParam = sdkInstance.getInitParam();
        mSendMsgOptions = new SendMessageOptions();
        return ErrCode.XOK;
    }

    void release() {
        disconnect();
        workThreadClearMessage();
    }

    void workThreadProcessMessage(Message msg) {
        switch (msg.what) {
            case MSGID_RTMMGR_CONNECT: {
                DoTaskConnect(msg);
            } break;

            case MSGID_RTMMGR_CONNECT_DONE: {
                DoTaskConnectDone(msg);
            } break;

            case MSGID_RTMMGR_DISCONNECT: {
                DoTaskDisconnect(msg);
            } break;
        }
    }

    void workThreadClearMessage() {
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(MSGID_RTMMGR_CONNECT);
            mWorkHandler.removeMessages(MSGID_RTMMGR_CONNECT_DONE);
            mWorkHandler.removeMessages(MSGID_RTMMGR_DISCONNECT);
            mWorkHandler = null;
        }
    }

    void sendTaskMessage(int what, int arg1, int arg2, Object obj) {
        Message msg = new Message();
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = obj;
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(what);
            mWorkHandler.sendMessage(msg);
        }
    }


    ///////////////////////////////////////////////////////////////////////
    /////////////////// Override Methods of IRtmMgr ///////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public int getStateMachine() {
        synchronized (mDataLock) {
            return mStateMachine;
        }
    }

    void setStateMachine(int newStateMachine) {
        synchronized (mDataLock) {
            mStateMachine = newStateMachine;
        }
    }

    @Override
    public int registerListener(IRtmMgr.ICallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.add(callback);
        }
        return ErrCode.XOK;
    }

    @Override
    public int unregisterListener(IRtmMgr.ICallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.remove(callback);
        }
        return ErrCode.XOK;
    }


    @Override
    public int connect(final IotDevice iotDevice) {
        long t1 = System.currentTimeMillis();
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<connect> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }
        int state = getStateMachine();
        if ((state == RTMMGR_STATE_CONNECTING) || (state == RTMMGR_STATE_CONNECTED)) {
            ALog.getInstance().e(TAG, "<connect> connected or connecting, state=" + state);
            return ErrCode.XERR_RTMMGR_ALREADY_CONNECTED;
        }

        setStateMachine(RTMMGR_STATE_CONNECTING);  // 设置状态机：正在连接
        sendTaskMessage(MSGID_RTMMGR_CONNECT, 0, 0, iotDevice);

        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<connect> finished, costTime=" + (t2-t1));
        return ErrCode.XOK;
    }

    @Override
    public int disconnect() {
        long t1 = System.currentTimeMillis();
        if (getStateMachine() != RTMMGR_STATE_DISCONNECTED) {
            sendTaskMessage(MSGID_RTMMGR_DISCONNECT, 0, 0, null);
        }
        setStateMachine(RTMMGR_STATE_DISCONNECTED);   // 设置状态机：已经断开
        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<disconnect> finished, costTime=" + (t2-t1));
        return ErrCode.XOK;
    }

    @Override
    public int sendMessage(byte[] messageData, final IRtmMgr.ISendCallback sendCallback) {
        if (getStateMachine() != RTMMGR_STATE_CONNECTED) {
            ALog.getInstance().e(TAG, "<sendMessage> NOT ready");
            return ErrCode.XERR_BAD_STATE;
        }

        RtmMessage rtmMsg = mRtmClient.createMessage(messageData);
        mRtmClient.sendMessageToPeer(mPeerDevice.mDeviceID, rtmMsg, mSendMsgOptions, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                ALog.getInstance().d(TAG, "<sendMessage.onSuccess>");
                sendCallback.onSendDone(ErrCode.XOK);
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                int rtmMsgErrCode = errorInfo.getErrorCode();
                int errCode = mapRtmMsgErrCode(rtmMsgErrCode);
                ALog.getInstance().d(TAG, "<sendMessage.onFailure> rtmMsgErrCode=" + rtmMsgErrCode);
                sendCallback.onSendDone(errCode);
            }
        });

        ALog.getInstance().d(TAG, "<sendMessage> messageDataSize=" + messageData.length);
        return ErrCode.XOK;
    }


    ///////////////////////////////////////////////////////////////////////
    /////////////////// Internal Methods of IRtmMgr ///////////////////////
    ///////////////////////////////////////////////////////////////////////

    /**
     * @brief 在工作线程中执行，请求Token信息
     */
    void DoTaskConnect(Message msg) {
        IotDevice iotDevice = (IotDevice)msg.obj;

        //
        // 向服务器请求 RTM的token信息
        //
        AccountMgr.AccountInfo accountInfo = mSdkInstance.getAccountInfo();
        String controllerId = accountInfo.mInventDeviceName;
        String controlledId = iotDevice.mDeviceID;
        AgoraService.RtmAccountInfo rtmAccountInfo = AgoraService.getInstance().reqRtmAccount(
                accountInfo.mAgoraAccessToken, mSdkInitParam.mRtcAppId, controllerId, controlledId);
        if (rtmAccountInfo.mErrCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<DoTaskConnect> fail to request token");
            setStateMachine(RTMMGR_STATE_DISCONNECTED);  // 设置状态机：断开

            // 回调连接失败
            synchronized (mCallbackList) {
                for (IRtmMgr.ICallback listener : mCallbackList) {
                    listener.onConnectDone(rtmAccountInfo.mErrCode, iotDevice);
                }
            }
            return;
        }

        //
        // 在入口调用线程中，创建RTM联接并且登录
        //
        String rtmToken = rtmAccountInfo.mToken;
        int errCode = rtmEngCreate(rtmToken, accountInfo.mInventDeviceName, iotDevice);
        if (errCode != ErrCode.XOK) {
            sendTaskMessage(MSGID_RTMMGR_CONNECT_DONE, errCode, 0, iotDevice);
        }

        ALog.getInstance().d(TAG, "<DoTaskConnect> finished"
                + ", mUid=" + accountInfo.mInventDeviceName + ", mToken=" + rtmToken);
    }

    /**
     * @brief 在工作线程中执行，联接请求完成处理，成功或者失败
     */
    void DoTaskConnectDone(Message msg) {
        int errCode = msg.arg1;
        int state = (errCode == ErrCode.XOK) ? RTMMGR_STATE_CONNECTED : RTMMGR_STATE_DISCONNECTED;
        setStateMachine(state);  // 根据连接是否成功 来设置状态机
        ALog.getInstance().d(TAG, "<DoTaskConnectDone> errCode=" + errCode);

        synchronized (mCallbackList) {
            for (IRtmMgr.ICallback listener : mCallbackList) {
                listener.onConnectDone(errCode, mPeerDevice);
            }
        }
    }

    /**
     * @brief 在工作线程中执行，执行断开连接处理
     */
    void DoTaskDisconnect(Message msg) {
        long t1 = System.currentTimeMillis();
        rtmEngDestroy();
        setStateMachine(RTMMGR_STATE_DISCONNECTED);
        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<DoTaskDisconnect> done, costTime=" + (t2-t1));
    }


    /**
     * @brief 在调用主线程中执行，创建RTM联接并且登录
     */
    int rtmEngCreate(final String token, final String localUserId, final IotDevice iotDevice) {
        long t1 = System.currentTimeMillis();
        mPeerDevice = iotDevice;
        RtmClientListener rtmListener = new RtmClientListener() {
            @Override
            public void onConnectionStateChanged(int state, int reason) {   //连接状态改变
                ALog.getInstance().d(TAG, "<onConnectionStateChanged> state=" + state
                        + ", reason=" + reason);

                switch (state) {
                    case CONNECTION_STATE_DISCONNECTED:
                        break;

                    case CONNECTION_STATE_CONNECTING:
                        break;

                    case CONNECTION_STATE_CONNECTED:
                        break;

                    case CONNECTION_STATE_RECONNECTING:
                        break;

                    case CONNECTION_STATE_ABORTED:
                        setStateMachine(RTMMGR_STATE_ABORTED);
                        break;
                }

                synchronized (mCallbackList) {
                    for (IRtmMgr.ICallback listener : mCallbackList) {
                        listener.onConnectionStateChanged(state);
                    }
                }
            }

            @Override
            public void onMessageReceived(RtmMessage rtmMessage, String peerId) {   // 收到RTM消息
                synchronized (mCallbackList) {
                    for (IRtmMgr.ICallback listener : mCallbackList) {
                        listener.onMessageReceived(rtmMessage.getRawMessage());
                    }
                }
            }

            @Override
            public void onImageMessageReceivedFromPeer(final RtmImageMessage rtmImageMessage,
                                                       final String peerId) {
            }

            @Override
            public void onFileMessageReceivedFromPeer(RtmFileMessage rtmFileMessage, String s) {
            }

            @Override
            public void onMediaUploadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress,
                                                 long l) {
            }

            @Override
            public void onMediaDownloadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {
            }

            @Override
            public void onTokenExpired() {  //token过期，需要刷新RTM token
            }

            @Override
            public void onTokenPrivilegeWillExpire() {
            }

            @Override
            public void onPeersOnlineStatusChanged(Map<String, Integer> status) {   //对端用户在线状态改变
            }
        };


        try {
            mRtmClient = RtmClient.createInstance(mSdkInitParam.mContext,
                    mSdkInitParam.mRtcAppId, rtmListener);
        } catch (Exception exp) {
            exp.printStackTrace();
            ALog.getInstance().e(TAG, "<rtmLogin> [EXCEPTION] exp=" + exp.toString());
            sendTaskMessage(MSGID_RTMMGR_CONNECT_DONE, ErrCode.XERR_UNSUPPORTED, 0, iotDevice);
            return ErrCode.XERR_UNSUPPORTED;
        }

        mRtmClient.login(token, localUserId, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void responseInfo) {
                ALog.getInstance().d(TAG, "<rtmEngCreate.login.onSuccess> success");
                sendTaskMessage(MSGID_RTMMGR_CONNECT_DONE, ErrCode.XOK, 0, iotDevice);
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                ALog.getInstance().i(TAG, "<rtmEngCreate.login.onFailure> failure"
                        + ", errInfo=" + errorInfo.getErrorCode()
                        + ", errDesc=" + errorInfo.getErrorDescription());
                int errCode = mapErrCode(errorInfo.getErrorCode());
                sendTaskMessage(MSGID_RTMMGR_CONNECT_DONE, errCode, 0, iotDevice);
            }
        });

        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<rtmEngCreate> finished, costTime=" + (t2-t1));
        return ErrCode.XOK;
    }


    /**
     * @brief 在调用主线程中执行，销毁RTM引擎
     */
    void rtmEngDestroy() {
        if (mRtmClient != null) {
            //mRtmClient.logout(null);
            mRtmClient.release();
            mRtmClient = null;
            ALog.getInstance().d(TAG, "<rtmEngDestroy> done");
        }
    }






    /**
     * @brief 映射RTM的错误码到全局统一的错误码
     */
    int mapErrCode(int rtmErrCode) {
        switch (rtmErrCode) {
            case RtmStatusCode.LoginError.LOGIN_ERR_UNKNOWN:
                return ErrCode.XERR_UNKNOWN;

            case RtmStatusCode.LoginError.LOGIN_ERR_REJECTED:
                return ErrCode.XERR_RTMMGR_REJECT;

            case RtmStatusCode.LoginError.LOGIN_ERR_INVALID_ARGUMENT:
                return ErrCode.XERR_INVALID_PARAM;

            case RtmStatusCode.LoginError.LOGIN_ERR_INVALID_APP_ID:
                return ErrCode.XERR_APPID_INVALID;

            case RtmStatusCode.LoginError.LOGIN_ERR_INVALID_TOKEN:
                return ErrCode.XERR_TOKEN_INVALID;

            case RtmStatusCode.LoginError.LOGIN_ERR_TOKEN_EXPIRED:
                return ErrCode.XERR_TOKEN_INVALID;

            case RtmStatusCode.LoginError.LOGIN_ERR_NOT_AUTHORIZED:
                return ErrCode.XERR_NOT_AUTHORIZED;

            case RtmStatusCode.LoginError.LOGIN_ERR_ALREADY_LOGIN:
                return ErrCode.XERR_RTMMGR_ALREADY_CONNECTED;

            case RtmStatusCode.LoginError.LOGIN_ERR_TIMEOUT:
                return ErrCode.XERR_TIMEOUT;

            case RtmStatusCode.LoginError.LOGIN_ERR_TOO_OFTEN:
                return ErrCode.XERR_INVOKE_TOO_OFTEN;

            case RtmStatusCode.LoginError.LOGIN_ERR_NOT_INITIALIZED:
                return ErrCode.XERR_BAD_STATE;
        }

        return ErrCode.XOK;
    }



    /**
     * @brief 映射RTM的消息错误码到全局统一的错误码
     */
    int mapRtmMsgErrCode(int msgErrCode) {
        switch (msgErrCode) {
            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_FAILURE:
                return ErrCode.XERR_RTMMGR_MSG_FAILURE;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_TIMEOUT:
                return ErrCode.XERR_RTMMGR_MSG_TIMEOUT;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_PEER_UNREACHABLE:
                return ErrCode.XERR_RTMMGR_MSG_UNREACHABLE;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_CACHED_BY_SERVER:
                return ErrCode.XERR_RTMMGR_MSG_CACHED_BY_SERVER;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_TOO_OFTEN:
                return ErrCode.XERR_RTMMGR_MSG_TOO_OFTEN;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_INVALID_USERID:
                return ErrCode.XERR_USERID_INVALID;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_INVALID_MESSAGE:
                return ErrCode.XERR_RTMMGR_MSG_INVALID;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_IMCOMPATIBLE_MESSAGE:
                return ErrCode.XERR_RTMMGR_MSG_IMCOMPATIBLE;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_NOT_INITIALIZED:
                return ErrCode.XERR_BAD_STATE;

            case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_USER_NOT_LOGGED_IN:
                return ErrCode.XERR_BAD_STATE;
        }

        return ErrCode.XOK;
    }



}
