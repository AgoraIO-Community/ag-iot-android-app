/**
 * @file IAgoraIotAppSdk.java
 * @brief This file define the SDK interface for Agora Iot AppSdk 2.0
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 2.0.0.1
 * @date 2023-04-12
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.sdkimpl;


import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.util.UUID;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.base.BaseThreadComp;
import io.agora.iotlink.callkit.SessionCtx;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.rtmsdk.RtmMgrComp;
import io.agora.iotlink.rtmsdk.RtmPacket;
import io.agora.iotlink.transport.HttpTransport;
import io.agora.iotlink.transport.MqttTransport;
import io.agora.iotlink.transport.TransPacket;
import io.agora.iotlink.transport.TransPktQueue;




/**
 * @brief SDK引擎接口
 */
public class AgoraIotAppSdk extends BaseThreadComp
        implements IAgoraIotAppSdk, MqttTransport.ICallback, RtmMgrComp.IRtmMgrCallback {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/AgoraIotAppSdk";
    private static final int MQTT_TIMEOUT = 10000;
    private static final int LOGOUT_WAIT_TIMEOUT = 3500;



    //
    // The message Id
    //
    private static final int MSGID_LOGIN_NODEACTIVE = 0x0001;
    private static final int MSGID_LOGIN_INIT_DONE = 0x0002;
    private static final int MSGID_MQTT_STATE_CHANGED = 0x0003;
    private static final int MSGID_PACKET_SEND = 0x0004;
    private static final int MSGID_PACKET_RECV = 0x0005;
    private static final int MSGID_LOGOUT = 0x0006;


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private final Object mLogoutEvent = new Object();
    private InitParam mInitParam;
    private CallkitMgr mCallkitMgr;


    public static final Object mDataLock = new Object();    ///< 同步访问锁,类中所有变量需要进行加锁处理
    private LocalNode mLocalNode = new LocalNode();
    private volatile int mStateMachine = AgoraIotAppSdk.SDK_STATE_INVALID;     ///< 当前呼叫状态机

    private LoginParam mLoginParam;
    private OnLoginListener mLoginListener;
    private MqttTransport mMqttTransport = new MqttTransport();

    private String mMqttTopicSub;                                   ///< MQTT订阅的主题
    private String mMqttTopicPub;                                   ///< MQTT发布的主题
    private TransPktQueue mRecvPktQueue = new TransPktQueue();      ///< 接收数据包队列
    private TransPktQueue mSendPktQueue = new TransPktQueue();      ///< 发送数据包队列

    private RtmMgrComp mRtmComp;                                ///< RTM组件

    ///////////////////////////////////////////////////////////////////////
    //////////////// Override Methods of IAgoraIotAppSdk //////////////////
    ///////////////////////////////////////////////////////////////////////

    @Override
    public int initialize(InitParam initParam) {
        if (TextUtils.isEmpty(initParam.mAppId) ||
            TextUtils.isEmpty(initParam.mServerUrl) ||
            (initParam.mContext == null))      {
            Log.e(TAG, "<initialize > [ERROR] invalid parameter");
            return ErrCode.XERR_INVALID_PARAM;
        }

        mInitParam = initParam;

        synchronized (mDataLock) {
            mLocalNode.mReady = false;
        }

        // 初始化日志系统
        if ((initParam.mLogFilePath != null) && (!initParam.mLogFilePath.isEmpty())) {
            boolean logRet = ALog.getInstance().initialize(initParam.mLogFilePath);
            if (!logRet) {
                Log.e(TAG, "<initialize > [ERROR] fail to initialize logger");
            }
        }

        // 设置 HTTP服务器地址
        HttpTransport.getInstance().setBaseUrl(initParam.mServerUrl);

        // 启动组件线程
        runStart(TAG);
        mSendPktQueue.clear();
        mRecvPktQueue.clear();

        // 创建组件实例对象
        mCallkitMgr = new CallkitMgr();
        mCallkitMgr.initialize(this);

        // SDK初始化完成，状态机切换到 初始化完成状态
        setStateMachine(SDK_STATE_INITIALIZED);

        ALog.getInstance().d(TAG, "<initialize> done");
        return ErrCode.XOK;
    }

    @Override
    public void release() {
        // 停止组件线程
        runStop();
        synchronized (mDataLock) {
            mLocalNode.mReady = false;
        }
        mSendPktQueue.clear();
        mRecvPktQueue.clear();

        // 销毁组件对象
        if (mCallkitMgr != null) {
            mCallkitMgr.release();
            mCallkitMgr = null;
        }

        // 状态机切换到 无效状态
        setStateMachine(SDK_STATE_INVALID);

        ALog.getInstance().d(TAG, "<release> done");
        ALog.getInstance().release();
    }

    @Override
    public int getStateMachine() {
        synchronized (mDataLock) {
            return mStateMachine;
        }
    }

    private void setStateMachine(int newState) {
        synchronized (mDataLock) {
            mStateMachine = newState;
        }
    }

    @Override
    public boolean isSignalingReady() {
        int rtmState = mRtmComp.getState();
        boolean ready = false;
        if ((rtmState == RtmMgrComp.RTM_STATE_RUNNING) || (rtmState == RtmMgrComp.RTM_STATE_RENEWING)) {
            ready = true;
        }

        return ready;
    }


    @Override
    public int login(final LoginParam loginParam, final OnLoginListener loginListener) {
        int state = getStateMachine();
        if (state != SDK_STATE_INITIALIZED) {
            ALog.getInstance().e(TAG, "<login> bad status, state=" + state);
            return ErrCode.XERR_BAD_STATE;
        }

        setStateMachine(SDK_STATE_LOGIN_ONGOING);    // 设置状态机正在准备操作

        // 回调状态机变化
        CallbackStateChanged(SDK_STATE_INITIALIZED, SDK_STATE_LOGIN_ONGOING, SDK_REASON_NONE);

        // 发送消息进行操作
        synchronized (mDataLock) {
            mLoginParam = loginParam;
            mLoginListener = loginListener;

            // 设置本地节点信息
            mLocalNode.mReady = false;
            mLocalNode.mUserId = loginParam.mUserId;
        }
        sendSingleMessage(MSGID_LOGIN_NODEACTIVE, 0, 0, null, 0);

        ALog.getInstance().d(TAG, "<login> loginParam=" + loginParam.toString());
        return ErrCode.XOK;
    }

    @Override
    public int logout() {
        int state = getStateMachine();
        if (state == SDK_STATE_INVALID) {
            ALog.getInstance().e(TAG, "<logout> bad status, state=" + state);
            return ErrCode.XERR_BAD_STATE;
        }
        if (state == SDK_STATE_INITIALIZED) {
            ALog.getInstance().e(TAG, "<logout> already unprepared!");
            return ErrCode.XOK;
        }
        ALog.getInstance().d(TAG, "<logout> ==>Enter");

        // 设置状态机到 注销状态
        setStateMachine(SDK_STATE_LOGOUT_ONGOING);

        // 回调状态机变化
        CallbackStateChanged(state, SDK_STATE_LOGOUT_ONGOING, SDK_REASON_NONE);


        // 释放和重新初始化呼叫模块
        mCallkitMgr.release();
        mCallkitMgr.initialize(this);

        // 删除队列中所有消息, 仅发送注销消息
        removeAllMessages();
        sendSingleMessage(MSGID_LOGOUT, 0, 0, null, 0);

        synchronized (mLogoutEvent) {
            try {
                mLogoutEvent.wait(LOGOUT_WAIT_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
                ALog.getInstance().e(TAG, "<logout> exception=" + e.getMessage());
            }
        }

        synchronized (mDataLock) {
            mLocalNode.mReady = false;
            mLocalNode.mUserId = null;
            mLocalNode.mNodeId = null;
            mLocalNode.mRegion = null;
            mLocalNode.mToken = null;
        }
        removeAllMessages();

        // 设置状态机到 初始化完成
        setStateMachine(SDK_STATE_INITIALIZED);

        // 回调状态机变化
        CallbackStateChanged(SDK_STATE_LOGOUT_ONGOING, SDK_STATE_INITIALIZED, SDK_REASON_NONE);

        ALog.getInstance().d(TAG, "<logout> <==Exit");
        return ErrCode.XOK;
    }

    @Override
    public ICallkitMgr getCallkitMgr() {
        return mCallkitMgr;
    }

    @Override
    public String getLocalUserId() {
        synchronized (mDataLock) {
            return mLocalNode.mUserId;
        }
    }

    @Override
    public String getLocalNodeId() {
        synchronized (mDataLock) {
            return mLocalNode.mNodeId;
        }
    }


    /**
     * @brief 获取SDK的初始化参数
     */
    IAgoraIotAppSdk.InitParam getInitParam() {
        return mInitParam;
    }

    /**
     * @brief 获取本地节点信息
     */
    LocalNode getLoalNode() {
        synchronized (mDataLock) {
            return mLocalNode;
        }
    }

    /**
     * @brief RTM连接到设备
     */
    int rtmConnectToDevice(final String localUserId, final String rtmToken) {
        int ret = mRtmComp.connectToDevice(localUserId, rtmToken);
        return ret;
    }

    /**
     * @brief RTM发生数据包
     */
    int rtmSendPacketToDev(final SessionCtx sessionCtx, final String cmdData,
                           final ICallkitMgr.OnCmdSendListener sendListener     ) {
        int ret = mRtmComp.sendPacketToDev(sessionCtx, cmdData, sendListener);
        return ret;
    }

    /**
     * @brief 发送数据包，被其他组件模块调用
     */
    int sendPacket(final TransPacket sendPacket) {
        // 插入发送队列
        mSendPktQueue.inqueue(sendPacket);

        // 消息通知组件线程进行发送
        sendSingleMessage(MSGID_PACKET_SEND, 0, 0, null, 0);

        //ALog.getInstance().d(TAG, "<sendPacket> done, queueSize=" + mSendPktQueue.size());
        return ErrCode.XOK;
    }

    /**
     * @brief 根据 sessionId 从队列中删除要发送的数据包
     * @param sessionId : 查找的sessionId
     * @return 返回删除的 packet，如果没有对应sessionId的发送包则返回null
     */
    TransPacket removePacketBySessionId(final UUID sessionId) {
        // 从发送队列中进行删除
        TransPacket packet = mSendPktQueue.removeBySessionId(sessionId);
        return packet;
    }

    /**
     * @brief 获取MQTT订阅的主题
     */
    String getMqttSubscribedTopic() {
        return mMqttTopicSub;
    }

    /**
     * @brief 获取MQTT发布的主题
     */
    String getMqttPublishTopic() {
        return mMqttTopicPub;
    }



    ///////////////////////////////////////////////////////////////////////////
    //////////////// Methods for Override BaseThreadComp ///////////////////////
    //////////////////////////////////////////////////////////////////////////
    @Override
    protected void processWorkMessage(Message msg)   {
        switch (msg.what) {
            case MSGID_LOGIN_NODEACTIVE:
                    onMessageLoginNodeActive(msg);
                break;

            case MSGID_LOGIN_INIT_DONE:
                    onMessageInitDone(msg);
                break;

            case MSGID_MQTT_STATE_CHANGED:
                    onMessageMqttStateChanged(msg);
                break;

            case MSGID_PACKET_SEND:
                    onMessagePacketSend(msg);
                break;

            case MSGID_PACKET_RECV:
                    onMessagePacketRecv(msg);
                break;

            case MSGID_LOGOUT:
                    onMessageUnprepare(msg);
                break;
        }

         mMqttTransport.processWorkMessage(msg);
    }

    @Override
    protected void removeAllMessages() {
        synchronized (mMsgQueueLock) {
            mWorkHandler.removeMessages(MSGID_LOGIN_NODEACTIVE);
            mWorkHandler.removeMessages(MSGID_LOGIN_INIT_DONE);
            mWorkHandler.removeMessages(MSGID_MQTT_STATE_CHANGED);
            mWorkHandler.removeMessages(MSGID_PACKET_SEND);
            mWorkHandler.removeMessages(MSGID_PACKET_RECV);
            mWorkHandler.removeMessages(MSGID_LOGOUT);
        }

        if (mMqttTransport != null) {
            mMqttTransport.removeAllMessages();
        }
    }

    @Override
    protected void processTaskFinsh() {
        if (mMqttTransport != null) {
            mMqttTransport.release();
        }
        ALog.getInstance().d(TAG, "<processTaskFinsh> done!");
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////////// Methods for thread message handler ///////////////////////
    //////////////////////////////////////////////////////////////////////////
    /**
     * @brief 组件线程消息处理：Node节点激活
     */
    void onMessageLoginNodeActive(Message msg) {
        int sdkState = getStateMachine();
        if (sdkState != SDK_STATE_LOGIN_ONGOING) {
            ALog.getInstance().e(TAG, "<onMessageLoginNodeActive> bad state, sdkState=" + sdkState);
            return;
        }
        removeMessage(MSGID_LOGIN_NODEACTIVE);

        // 激活节点
        LoginParam loginParam;
        synchronized (mDataLock) {
            loginParam = mLoginParam;
        }

        HttpTransport.NodeActiveResult result = HttpTransport.getInstance().nodeActive(mInitParam.mAppId, loginParam);
        if (result.mErrCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<onMessageLoginNodeActive> fail to nodeActive, ret=" + result.mErrCode);
            sendSingleMessage(MSGID_LOGIN_INIT_DONE, result.mErrCode, 0, null, 0);
            return;
        }
        if (getStateMachine() != SDK_STATE_LOGIN_ONGOING) {
            ALog.getInstance().e(TAG, "<onMessageLoginNodeActive> bad state2, sdkState=" + getStateMachine());
            return;
        }

            // 更新本地节点信息
        synchronized (mDataLock) {
            mLocalNode.mReady = true;
            mLocalNode.mUserId = loginParam.mUserId;
            mLocalNode.mNodeId = result.mNodeId;
            mLocalNode.mRegion = result.mNodeRegion;
            mLocalNode.mToken = result.mNodeToken;
        }

        // 设置 MQTT订阅和发布的主题
        mMqttTopicSub = "$falcon/callkit/" + result.mNodeId + "/sub";
        mMqttTopicPub = "$falcon/callkit/" + result.mNodeId + "/pub";


        //
        // 创建并初始化 Mqtt Transport
        //
        MqttTransport.InitParam mqttParam = new MqttTransport.InitParam();
        mqttParam.mContext = mInitParam.mContext;
        mqttParam.mThreadComp = this;
        mqttParam.mCallback = this;
        mqttParam.mClientId = result.mNodeId;
        mqttParam.mServerUrl = String.format("ssl://%s:%d", result.mMqttServer, result.mMqttPort);
        mqttParam.mUserName = result.mMqttUserName;
        mqttParam.mPassword = result.mNodeId + "/" + result.mUserId + "/" + result.mMqttSalt;
        mqttParam.mHasCaCertify = true;


        mqttParam.mSubTopicArray = new String[1];
        mqttParam.mSubTopicArray[0] = mMqttTopicSub;
        mqttParam.mSubQosArray = new int[1];
        mqttParam.mSubQosArray[0] = 2;

        int ret = mMqttTransport.initialize(mqttParam);
        if (ret != ErrCode.XOK) {
            synchronized (mDataLock) {
                mLocalNode.mReady = false;
                mLocalNode.mUserId = null;
                mLocalNode.mNodeId = null;
            }
            mMqttTransport.release();
            ALog.getInstance().e(TAG, "<onMessageLoginNodeActive> fail to mqtt init, ret=" + ret);
            sendSingleMessage(MSGID_LOGIN_INIT_DONE, ret, 0, null, 0);
            return;
        }

        // 创建 RTM组件
        RtmMgrComp.InitParam rtmInitParam = new RtmMgrComp.InitParam();
        rtmInitParam.mContext = mInitParam.mContext;
        rtmInitParam.mRtmAppId = mInitParam.mAppId;
        rtmInitParam.mSdkInstance = this;
        rtmInitParam.mCallback = this;
        mRtmComp = new RtmMgrComp();
        mRtmComp.initialize(rtmInitParam);

        // 发送一个超时延时的事件，这样如果没有 MQTT初始化回调回来，也能继续后续处理
        sendSingleMessage(MSGID_LOGIN_INIT_DONE, ErrCode.XERR_TIMEOUT, 0, null, MQTT_TIMEOUT);
        ALog.getInstance().d(TAG, "<onMessageLoginNodeActive> done");
    }


    /**
     * @brief 组件线程消息处理：MQTT初始化完成
     */
    void onMessageInitDone(Message msg) {
        int sdkState = getStateMachine();
        if (sdkState != SDK_STATE_LOGIN_ONGOING) {
            ALog.getInstance().e(TAG, "<onMessageInitDone> bad state, sdkState=" + sdkState);
            return;
        }
        removeMessage(MSGID_LOGIN_NODEACTIVE);
        removeMessage(MSGID_LOGIN_INIT_DONE);

        // 获取回调数据
        LoginParam loginParam;
        OnLoginListener loginListener;
        synchronized (mDataLock) {
            loginParam = mLoginParam;
            loginListener = mLoginListener;
        }

        if (msg.arg1 != ErrCode.XOK) {  // prepare() 操作有错误
            // 清除数据
            synchronized (mDataLock) {
                mLocalNode.mReady = false;
                mLocalNode.mNodeId = loginParam.mUserId;
            }
            if (mMqttTransport != null) {
                mMqttTransport.release();
            }
            setStateMachine(SDK_STATE_INITIALIZED);  // 设置状态机到初始化状态

            // 回调状态机变化
            CallbackStateChanged(sdkState, SDK_STATE_INITIALIZED, SDK_REASON_PREPARE);

        } else {
            setStateMachine(SDK_STATE_RUNNING);  // 设置状态机到正常运行状态

            // 回调状态机变化
            CallbackStateChanged(sdkState, SDK_STATE_RUNNING, SDK_REASON_NONE);
        }

        ALog.getInstance().d(TAG, "<onMessageInitDone> done, errCode=" + msg.arg1);
        loginListener.onSdkLoginDone(loginParam, msg.arg1);
    }

    /**
     * @brief 组件线程消息处理：MQTT状态发生变化
     */
    void onMessageMqttStateChanged(Message msg) {
        int sdkState = getStateMachine();
        if (sdkState == SDK_STATE_LOGOUT_ONGOING || sdkState == SDK_STATE_INITIALIZED){
            ALog.getInstance().e(TAG, "<onMessageMqttStateChanged> bad state");
            return;
        }
        int newMqttState = msg.arg1;
        ALog.getInstance().d(TAG, "<onMessageMqttStateChanged> newMqttState=" + newMqttState);

        if (newMqttState == MqttTransport.MQTT_TRANS_STATE_CONNECTED) {
            setStateMachine(SDK_STATE_RUNNING);
            synchronized (mDataLock) {
                mLocalNode.mReady = true;  // 更新当前本地节点就绪状态
            }

            // 回调状态机变化
            CallbackStateChanged(sdkState, SDK_STATE_RUNNING, SDK_REASON_NONE);

        } else if (newMqttState == MqttTransport.MQTT_TRANS_STATE_RECONNECTING) { // MQTT正在重连
            setStateMachine(SDK_STATE_RECONNECTING);
            synchronized (mDataLock) {
                mLocalNode.mReady = false;  // 更新当前本地节点就绪状态
            }

            // 回调状态机变化
            CallbackStateChanged(sdkState, SDK_STATE_RECONNECTING, SDK_REASON_NETWORK);

        } else if (newMqttState == MqttTransport.MQTT_TRANS_STATE_ABORT) {  // MQTT被抢占了
            ALog.getInstance().d(TAG, "<onMessageMqttStateChanged> release all");

            // 释放和重新初始化呼叫模块
            mCallkitMgr.release();
            mCallkitMgr.initialize(this);

            // 释放MQTT对象和所有MQTT数据包队列
            if (mMqttTransport != null) {
                mMqttTransport.release();
            }
            mRecvPktQueue.clear();
            mSendPktQueue.clear();

            // 删除队列中所有消息
            removeAllMessages();

            synchronized (mDataLock) {
                mLocalNode.mReady = false;
                mLocalNode.mUserId = null;
                mLocalNode.mNodeId = null;
                mLocalNode.mRegion = null;
                mLocalNode.mToken = null;
            }

            // 设置状态机到 初始化完成
            setStateMachine(SDK_STATE_INITIALIZED);

            // 回调状态机变化
            CallbackStateChanged(sdkState, SDK_STATE_INITIALIZED, SDK_REASON_ABORT);
        }
    }

    /**
     * @brief 组件线程消息处理：发送数据包
     */
    void onMessagePacketSend(Message msg) {
        int sdkState = getStateMachine();
        if (sdkState == SDK_STATE_INVALID) {
            ALog.getInstance().e(TAG, "<onMessagePacketSend> INVALID state");
            return;
        }
        if (sdkState != SDK_STATE_RUNNING) {
            // 2秒后再处理
            ALog.getInstance().e(TAG, "<onMessagePacketSend> bad state, sdkState=" + sdkState);
            sendSingleMessage(MSGID_PACKET_SEND, 0, 0, null, 2000);
            return;
        }


        //
        // 将发送队列中的数据包依次发送出去
        //
        for (;;) {
            TransPacket sendingPkt = mSendPktQueue.dequeue();
            if (sendingPkt == null) {  // 发送队列为空，没有要发送的数据包了
                break;
            }

            mMqttTransport.sendPacket(sendingPkt);
        }

    }

    /**
     * @brief 组件线程消息处理：处理接收到的数据包
     */
    void onMessagePacketRecv(Message msg) {
        int sdkState = getStateMachine();
        if (sdkState == SDK_STATE_INVALID) {
            ALog.getInstance().e(TAG, "<onMessagePacketRecv> invalid state");
            return;
        }
        if (sdkState != SDK_STATE_RUNNING) {
            // 2秒后再处理
            ALog.getInstance().e(TAG, "<onMessagePacketRecv> bad state, sdkState=" + sdkState);
            sendSingleMessage(MSGID_PACKET_SEND, 0, 0, null, 2000);
            return;
        }

        //
        // 处理队列中接收包
        //
        for (;;) {
            TransPacket recvedPkt = mRecvPktQueue.dequeue();
            if (recvedPkt == null) {
                break;
            }

            // TODO: 根据数据包不同的topic进行分发处理，当前全部都是呼叫系统的数据包
            mCallkitMgr.inqueueRecvPkt(recvedPkt);
        }
    }


    /**
     * @brief 组件线程消息处理：unprepare操作
     */
    void onMessageUnprepare(Message msg) {
        ALog.getInstance().d(TAG, "<onMessageUnprepare> BEGIN");
        if (mMqttTransport != null) {
            mMqttTransport.release();
        }
        mRecvPktQueue.clear();
        mSendPktQueue.clear();

        // 销毁RTM组件
        if (mRtmComp != null) {
            mRtmComp.release();
            mRtmComp = null;
        }

        ALog.getInstance().d(TAG, "<onMessageUnprepare> END");

        synchronized (mLogoutEvent) {
            mLogoutEvent.notify();    // 事件通知
        }
    }

    /**
     * @brief 回调SDK状态机变化
     */
    void CallbackStateChanged(int oldState, int newState, int reason) {
        ALog.getInstance().d(TAG, "<CallbackStateChanged> oldState=" + oldState
                    + ", newState=" + newState + ", reason=" + reason );
        if (oldState == newState) {
            return;
        }
        synchronized (mDataLock) {
            if (mInitParam.mStateListener == null) {
                return;
            }
            mInitParam.mStateListener.onSdkStateChanged(oldState, newState, reason);
        }
    }

    /**
     * @brief 回调RTM状态机变化
     */
    void CallbackRtmStateChanged(int newState) {
        ALog.getInstance().d(TAG, "<CallbackRtmStateChanged> newState=" + newState);

        synchronized (mDataLock) {
            if (mInitParam.mStateListener == null) {
                return;
            }

            if ((newState == RtmMgrComp.RTM_STATE_RUNNING) || (newState == RtmMgrComp.RTM_STATE_RENEWING)) {
                mInitParam.mStateListener.onSignalingStateChanged(true);
            } else {
                mInitParam.mStateListener.onSignalingStateChanged(false);
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Override Methods of IRtmCallback ////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onRtmRecvedPacket(final RtmPacket recvedPacket) {
        if (mCallkitMgr != null) {
            mCallkitMgr.onRtmRecvedPacket(recvedPacket);
        }
    }

    @Override
    public void onRtmStateChanged(int newState) {
        ALog.getInstance().d(TAG, "<onRtmStateChanged> newState=" + newState);
        CallbackRtmStateChanged(newState);
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////// Methods for Override MqttTransport.ICallback ////////////////
    //////////////////////////////////////////////////////////////////////////
    public void onMqttTransInitDone(int errCode) {
        int sdkState = getStateMachine();
        if (sdkState != SDK_STATE_LOGIN_ONGOING) {
            ALog.getInstance().d(TAG, "<onMqttTransInitDone> bad state, sdkState=" + sdkState);
            return;
        }

        removeMessage(MSGID_LOGIN_INIT_DONE);  // 移除已有的超时消息
        sendSingleMessage(MSGID_LOGIN_INIT_DONE, errCode, 0, null, 0);
    }

    public void onMqttTransStateChanged(int newState) {
        sendSingleMessage(MSGID_MQTT_STATE_CHANGED, newState, 0, null, 0);
    }

    public void onMqttTransReceived(final TransPacket transPacket) {
        // 插入数据包到接收队列中
        mRecvPktQueue.inqueue(transPacket);

        // 消息通知组件线程进行接收包分发处理
        sendSingleMessage(MSGID_PACKET_RECV, 0, 0, null, 0);
    }

    public void onMqttTransError(int errCode) {
        ALog.getInstance().e(TAG, "<onMqttTransError> errCode=" + errCode);
    }



}
