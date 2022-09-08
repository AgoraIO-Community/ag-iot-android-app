/**
 * @file IRtmMgr.java
 * @brief This file define the interface of RTM management
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-08-11
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink;


/**
 * @brief RTM消息管理模块
 */
public interface IRtmMgr  {


    //
    // RtmMgr 模块的状态机
    //
    public static final int RTMMGR_STATE_DISCONNECTED = 0x0001;    ///< 当前无联接
    public static final int RTMMGR_STATE_CONNECTING = 0x0002;      ///< 正在联接中
    public static final int RTMMGR_STATE_CONNECTED = 0x0003;       ///< 已经正常联接
    public static final int RTMMGR_STATE_RECONNECTING = 0x0004;    ///< 网络问题，正在重联接
    public static final int RTMMGR_STATE_ABORTED = 0x0005;         ///< RTM无效


    /**
     * @brief RTM消息管理回调接口
     */
    public static interface ICallback {

        /**
         * @brief 联接设备完成事件
         * @param errCode : 错误代码
         * @param iotDevice : 要联接的设备
         */
        default void onConnectDone(int errCode, final IotDevice iotDevice) {}

        /**
         * @brief 联接状态变化事件
         * @param state : 当前新状态机
         */
        default void onConnectionStateChanged(int state) { };

        /**
         * @brief 接收到设备端数据事件
         * @param messageData : 接收到的数据
         */
        default void onMessageReceived(byte[] messageData) { }
    }




    ////////////////////////////////////////////////////////////////////////
    //////////////////////////// Public Methods ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief 获取当前RtmMgr状态机
     * @return 返回状态机
     */
    int getStateMachine();

    /*
     * @brief 注册回调接口
     * @param callback : 回调接口
     * @return 错误码
     */
    int registerListener(IRtmMgr.ICallback callback);

    /*
     * @brief 注销回调接口
     * @param callback : 回调接口
     * @return 错误码
     */
    int unregisterListener(IRtmMgr.ICallback callback);

    /**
     * @brief 联接到要控制的设备，触发 onConnectDone() 事件
     * @param iotDevice : 要控制的设备
     * @param
     * @return 错误码
     */
    int connect(final IotDevice iotDevice);

    /**
     * @brief 与当前控制的设备断开联接，同步调用函数
     * @return 错误码
     */
    int disconnect();

    /**
     * @brief 发送消息回调
     */
    public static interface ISendCallback<T> {
        void onSendDone(int errCode);
    }

    /**
     * @brief 发送消息到对端设备
     * @param messageData : 要发送的数据流
     * @param sendCallback : 消息发送回调
     * @return 错误码
     */
    int sendMessage(byte[] messageData, final ISendCallback sendCallback);



}
