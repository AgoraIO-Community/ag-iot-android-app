package io.agora.iotlink.transport;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;


import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.android.service.MqttTraceHandler;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.base.BaseEvent;
import io.agora.iotlink.base.BaseThreadComp;
import io.agora.iotlink.logger.ALog;


public class MqttTransport implements MqttSdkClient.ICallback {

    //
    // The state machine of MQTT transport
    //
    public static final int MQTT_TRANS_STATE_IDLE = 0x0000;
    public static final int MQTT_TRANS_STATE_CONNECTING = 0x0001;
    public static final int MQTT_TRANS_STATE_CONNECTED = 0x0002;
    public static final int MQTT_TRANS_STATE_RECONNECTING = 0x0003;
    public static final int MQTT_TRANS_STATE_ABORT = 0x0004;        ///< 被抢占了



    /**
     * @brief MQTT 客户端回调接口
     */
    public static interface ICallback {

        /**
         * @brief 初始化完成事件
         * @param errCode : 错误码
         */
        default void onMqttTransInitDone(int errCode) {}


        /**
         * @brief 状态变化事件
         * @param newState : 错误码
         */
        default void onMqttTransStateChanged(int newState) {}

        /**
         * @brief 消息到来事件
         * @param transPacket : 消息数据
         */
        default void onMqttTransReceived(final TransPacket transPacket) {}

        /**
         * @brief 错误事件
         * @param errCode : 错误码
         */
        default void onMqttTransError(int errCode) {}

    }

    /**
     * @brief MQTT 初始化参数
     */
    public static class InitParam {
        public Context mContext;
        public BaseThreadComp mThreadComp;
        public ICallback mCallback;

        public String mServerUrl;                   ///< 服务器地址
        public String mUserName;
        public String mPassword;
        public String mClientId;

        public String[] mSubTopicArray;             ///< 要订阅的主题
        public int[] mSubQosArray;                  ///< 订阅的QoS

        public boolean mHasCaCertify;               ///< 是否有CA校验

        @Override
        public String toString() {
            String infoText = "{ mServerUrl=" + mServerUrl
                    + ", mUserName=" + mUserName
                    + ", mPassword=" + mPassword
                    + ", mClientId=" + mClientId + " }";
            return infoText;
        }
    }


    ////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////
    ////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/MQTTTRANS";
    private static final long STATE_DETECT_INTERVAL = 2000;         ///< 状态检测，定时2秒
    private static final long MAX_RECONNECT_DELAY = 16000;          ///< 最大重连延时16秒


    //
    // Message Id
    //
    private static final int MSGID_CONNECT_DONE = 0x0101;
    private static final int MSGID_SUBSCRIBE_DONE = 0x0102;
    private static final int MSGID_STATE_DETECT = 0x0103;
    private static final int MSGID_RECONNECT = 0x0104;

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private final Object mDataLock = new Object();


    private InitParam mInitParam;
    private MqttSdkClient mMqttSdkClient = null;
    private int mState = MQTT_TRANS_STATE_IDLE;
    private volatile long mReconnectDelay = 1000;           ///< 重连延时



    ////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////// Methods of Public /////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 设置当前状态机
     */
    void setState(int newState) {
        synchronized (mDataLock) {
            mState = newState;
        }
    }

    /**
     * @brief 返回当前状态机
     */
    public int getState() {
        synchronized (mDataLock) {
            return mState;
        }
    }

    /**
     * @brief 初始化MQTT，连接到服务器
     * @param initParam : 初始化参数
     * @return 返回错误码
     */
    public int initialize(final InitParam initParam) {
        if ((initParam.mCallback == null) || TextUtils.isEmpty(initParam.mServerUrl)) {
            ALog.getInstance().d(TAG, "<initialize> invalid parameter!");
            return ErrCode.XERR_INVALID_PARAM;
        }
        mInitParam = initParam;


        //
        // 创建并初始化 MQTT客户端
        //
        MqttSdkClient.InitParam mqttParam = new MqttSdkClient.InitParam();
        mqttParam.mContext = initParam.mContext;
        mqttParam.mCallback = this;
        mqttParam.mServerUrl = initParam.mServerUrl;
        mqttParam.mUserName = initParam.mUserName;
        mqttParam.mPassword = initParam.mPassword;
        mqttParam.mClientId = initParam.mClientId;
        mqttParam.mHasCaCertify = initParam.mHasCaCertify;

        setState(MQTT_TRANS_STATE_CONNECTING);
        mMqttSdkClient = new MqttSdkClient();
        int ret = mMqttSdkClient.initialize(mqttParam);
        if (ret != ErrCode.XOK) {
            setState(MQTT_TRANS_STATE_IDLE);
            mMqttSdkClient = null;
            return ret;
        }

        return ErrCode.XOK;
    }


    /**
     * @brief 同步释放MQTT客户端，阻塞等待
     * @return 返回错误码
     */
    public int release() {
        if (mMqttSdkClient == null) {
            return ErrCode.XOK;
        }
        removeAllMessages();

        mMqttSdkClient.release();
        mMqttSdkClient = null;
        return ErrCode.XOK;
    }
    /**
     * @brief MQTT发送数据包
     * @param sendingPkt : 要发送的数据包
     */
    public int sendPacket(final TransPacket sendingPkt) {
        if (mMqttSdkClient == null) {
            return ErrCode.XERR_BAD_STATE;
        }

        int ret = mMqttSdkClient.sendPacket(sendingPkt);
        return ret;
     }

    /**
     * @brief 消息处理方法，在消息组件中被调用
     */
    public void processWorkMessage(Message msg) {
        if (mMqttSdkClient == null) {
            return;
        }
        switch (msg.what) {
            case MSGID_CONNECT_DONE:
                onMessageConnectDone(msg);
                break;

            case MSGID_SUBSCRIBE_DONE:
                onMessageSubscribeDone(msg);
                break;

            case MSGID_STATE_DETECT:
                onMessageStateDetect(msg);
                break;

            case MSGID_RECONNECT:
                onMessageReconnect(msg);
                break;
        }
    }

    /**
     * @brief 删除所有消息，在消息组件中被调用
     */
    public void removeAllMessages() {
        mInitParam.mThreadComp.removeMessage(MSGID_CONNECT_DONE);
        mInitParam.mThreadComp.removeMessage(MSGID_SUBSCRIBE_DONE);
        mInitParam.mThreadComp.removeMessage(MSGID_STATE_DETECT);
        mInitParam.mThreadComp.removeMessage(MSGID_RECONNECT);
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////// Methods for Override MqttSdkClient.ICallback ////////////////
    //////////////////////////////////////////////////////////////////////////
    @Override
    public void onMqttConnectDone(int errCode) {
        if (mMqttSdkClient == null) {
            return;
        }
        mInitParam.mThreadComp.sendSingleMessage(MSGID_CONNECT_DONE, errCode, 0,null, 0);
    }

    @Override
    public void onMqttReconnectDone(int errCode) {
        if (mMqttSdkClient == null) {
            return;
        }

        if (errCode == ErrCode.XOK) { // 重连成功
            ALog.getInstance().d(TAG, "<onMqttReconnectDone> reconnect successful!");
            mReconnectDelay = 1000; // 重置延时时间

        } else if (errCode == ErrCode.XERR_NOT_AUTHORIZED) { // 未鉴权,被抢占了，不再重连
            ALog.getInstance().e(TAG, "<onMqttReconnectDone> reconnect abort!");

            mInitParam.mThreadComp.removeMessage(MSGID_STATE_DETECT); // 停止状态检测
            setState(MQTT_TRANS_STATE_ABORT);  // 设置状态机
            mInitParam.mCallback.onMqttTransStateChanged(MQTT_TRANS_STATE_ABORT); // 回调给上层

        } else { // 其他原因重连失败，延时后重连
            ALog.getInstance().e(TAG, "<onMqttReconnectDone> reconnect failure, try later!");
            mReconnectDelay *= 2;  // 双倍增加延时时间
            mReconnectDelay = Math.min(mReconnectDelay, MAX_RECONNECT_DELAY);
            mInitParam.mThreadComp.sendSingleMessage(MSGID_RECONNECT, 0, 0, null, mReconnectDelay);
        }

    }

    @Override
    public void onMqttSubscribeDone(int errCode) {
        if (mMqttSdkClient == null) {
            return;
        }
        mInitParam.mThreadComp.sendSingleMessage(MSGID_SUBSCRIBE_DONE, errCode, 0,null, 0);
    }

    @Override
    public void onMqttMsgReceived(final TransPacket transPacket) {
        if (mMqttSdkClient == null) {
            return;
        }
        // 直接回调给上层
        mInitParam.mCallback.onMqttTransReceived(transPacket);
    }

    @Override
    public void onMqttConnectionLost(final String cause) {
        if (mMqttSdkClient == null) {
            return;
        }
        int oldState = getState();
        setState(MQTT_TRANS_STATE_RECONNECTING);  // 进入重连状态

        if (oldState != MQTT_TRANS_STATE_RECONNECTING) { // 回调状态变化
            mInitParam.mCallback.onMqttTransStateChanged(MQTT_TRANS_STATE_RECONNECTING);
        }

        // 1秒后立即进行重连操作
        mReconnectDelay = 1000;  // 重置重连延时
        mInitParam.mThreadComp.sendSingleMessage(MSGID_RECONNECT, 0, 0,null, mReconnectDelay);

        // 开始定时2秒检测状态
        mInitParam.mThreadComp.sendSingleMessage(MSGID_STATE_DETECT, 0, 0,null, STATE_DETECT_INTERVAL);
    }

    /////////////////////////////////////////////////////////////
    ///////////////////////// 消息处理方法 //////////////////////////
    /////////////////////////////////////////////////////////////
    /**
     * @brief 连接服务器完成消息
     */
    void onMessageConnectDone(Message msg) {
        int errCode = msg.arg1;
        ALog.getInstance().d(TAG, "<onMessageConnectDone> errCode=" + errCode);

        if (errCode != ErrCode.XOK) {  // 连接服务器失败，直接回调给上层
            mInitParam.mCallback.onMqttTransInitDone(errCode);
            return;
        }

        // 订阅MQTT消息
        int ret = mMqttSdkClient.subscribe(mInitParam.mSubTopicArray, mInitParam.mSubQosArray);
        if (ret != ErrCode.XOK) {
            mInitParam.mCallback.onMqttTransInitDone(ret);  // 回调初始化完成事件
            return;
        }
    }

    void onMessageSubscribeDone(Message msg) {
        int errCode = msg.arg1;
        int state = getState();
        ALog.getInstance().d(TAG, "<onMessageSubscribeDone> errCode=" + errCode + ", state=" + state);

        if (state == MQTT_TRANS_STATE_CONNECTING) {     // 正在初始化时的订阅
            // 回调初始化完成事件
            ALog.getInstance().d(TAG, "<onMessageSubscribeDone> callback init done event!");
            mInitParam.mCallback.onMqttTransInitDone(errCode);

            // 回调状态机变化事件
            if (errCode == ErrCode.XOK) {
                ALog.getInstance().d(TAG, "<onMessageSubscribeDone> callback state change to CONNECTED!");
                setState(MQTT_TRANS_STATE_CONNECTED);
                mInitParam.mCallback.onMqttTransStateChanged(MQTT_TRANS_STATE_CONNECTED);

            } else {
                // 释放MQTT对象，下次重新连接
                mMqttSdkClient.release();
                mMqttSdkClient = null;

                ALog.getInstance().d(TAG, "<onMessageSubscribeDone> callback state change to IDLE!");
                setState(MQTT_TRANS_STATE_IDLE);
                mInitParam.mCallback.onMqttTransStateChanged(MQTT_TRANS_STATE_IDLE);
            }


        } else if (state == MQTT_TRANS_STATE_RECONNECTING) {    // 自动重联时的订阅

            if (errCode == ErrCode.XOK) {
                // 回调状态机变化事件
                ALog.getInstance().d(TAG, "<onMessageSubscribeDone> callback state change to CONNECTED!");
                setState(MQTT_TRANS_STATE_CONNECTED);
                mInitParam.mCallback.onMqttTransStateChanged(MQTT_TRANS_STATE_CONNECTED);

            } else {
                // 过段时间重新检测状态和订阅
                mInitParam.mThreadComp.sendSingleMessage(MSGID_STATE_DETECT, 0, 0,null, STATE_DETECT_INTERVAL);
            }
        }
    }

    void onMessageStateDetect(Message msg) {
        if (mMqttSdkClient == null) {
            return;
        }
        boolean isConnected = mMqttSdkClient.isConnected();
        if (!isConnected) {
            ALog.getInstance().e(TAG, "<onMessageStateDetect> not connected, try it later!");
            mInitParam.mThreadComp.sendSingleMessage(MSGID_STATE_DETECT, 0, 0,null, STATE_DETECT_INTERVAL);
            return;
        }

        // 订阅MQTT消息
        int ret = mMqttSdkClient.subscribe(mInitParam.mSubTopicArray, mInitParam.mSubQosArray);
        if (ret != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<onMessageStateDetect> fail to subscribe, try it later!");
            mInitParam.mThreadComp.sendSingleMessage(MSGID_STATE_DETECT, 0, 0,null, STATE_DETECT_INTERVAL);
            return;
        }

        ALog.getInstance().d(TAG, "<onMessageStateDetect> subscribing topics...");
    }


    void onMessageReconnect(Message msg) {
        if (mMqttSdkClient == null) {
            return;
        }
        boolean isConnected = mMqttSdkClient.isConnected();
        if (isConnected) {
            ALog.getInstance().e(TAG, "<onMessageReconnect> already connected!");
            return;
        }

        // 重连操作
        int ret = mMqttSdkClient.reconnect();
        if (ret != ErrCode.XOK) {
            // 重连失败，延时后继续
            ALog.getInstance().e(TAG, "<onMessageReconnect> fail to subscribe, try after " + mReconnectDelay);
            mInitParam.mThreadComp.sendSingleMessage(MSGID_RECONNECT, 0, 0,null, mReconnectDelay);
            return;
        }

        ALog.getInstance().d(TAG, "<onMessageReconnect> done");
    }


}