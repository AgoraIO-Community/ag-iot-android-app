/**
 * @file IAgoraIotAppSdk.java
 * @brief This file define the SDK interface for Agora Iot AppSdk 2.0
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink;


import android.content.Context;
import android.os.Bundle;



/*
 * @brief SDK引擎接口
 */

public interface IAgoraIotAppSdk  {

    //
    // SDK 状态机
    //
    public static final int SDK_STATE_INVALID = 0x0000;             ///< SDK未初始化
    public static final int SDK_STATE_INITIALIZED = 0x0001;         ///< SDK初始化完成，但还未就绪
    public static final int SDK_STATE_PREPARING = 0x0002;           ///< SDK正在就绪中
    public static final int SDK_STATE_RUNNING = 0x0003;             ///< SDK就绪完成，可以正常使用
    public static final int SDK_STATE_RECONNECTING = 0x0004;        ///< SDK正在内部重连中，暂时不可用
    public static final int SDK_STATE_UNPREPARING = 0x0005;         ///< SDK正在注销处理，完成后切换到初始化完成状态


    //
    // SDK 状态机变化原因
    //
    public static final int SDK_REASON_NONE = 0x0000;               ///< 未指定
    public static final int SDK_REASON_PREPARE = 0x0001;            ///< 节点prepare失败
    public static final int SDK_REASON_NETWORK = 0x0002;            ///< 网络状态原因
    public static final int SDK_REASON_ABORT = 0x0003;              ///< 节点被抢占激活

    /**
     * @brief SDK状态监听器
     */
    public interface OnSdkStateListener {

        /**
         * @brief 在SDK状态机变化时回调
         * @param oldSdkState : 原先的SDK状态机，参考 @SDK_STATE_XXXX
         * @param newSdkState : 新的SDK状态机，参考 @SDK_STATE_XXXX
         * @param reason : 状态变化原因，参考 @SDK_REASON_XXXX
         */
        void onSdkStateChanged(int oldSdkState, int newSdkState, int reason);
    }

    /**
     * @brief SDK初始化参数
     */
    public static class InitParam {
        public Context mContext;
        public String mAppId;                       ///< 项目的 AppId
        public String mLogFilePath;                 ///< 日志文件路径，不设置则日志不输出到本地文件
        public String mServerUrl;                   ///< 服务器URL
        public OnSdkStateListener mStateListener;   ///< SDK状态变化监听器

        @Override
        public String toString() {
            String infoText = "{ mAppId=" + mAppId
                    + ", mLogFilePath=" + mLogFilePath
                    + ", mServerUrl=" + mServerUrl + " }";
            return infoText;
        }
    }


    /**
     * @brief SDK就绪监听器，errCode=0表示就绪成功
     */
    public interface OnPrepareListener {
        void onSdkPrepareDone(final PrepareParam paramParam, int errCode);
    }

    /**
     * @brief SDK就绪参数，其中 mClientType值如下：
     *        1: Web;  2: Phone;  3: Pad;  4: TV;  5: PC;  6: Mini_app
     */
    public static class PrepareParam {
        public String mUserId;
        public int mClientType;

        @Override
        public String toString() {
            String infoText = "{ mUserId=" + mUserId + ", mClientType=" + mClientType + " }";
            return infoText;
        }
    }




    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 初始化Sdk
     * @param initParam : 初始化参数
     * @retrun 返回错误码，XOK--初始化成功，SDK状态会切换到 SDK_STATE_INITIALIZED
     *                   XERR_INVALID_PARAM--参数有错误；
     */
    int initialize(final InitParam initParam);

    /**
     * @brief 释放SDK所有资源，所有的组件模块也会被释放
     *        调用该函数后，SDK状态会切换到 SDK_STATE_INVALID
     */
    void release();


    /**
     * @brief 获取SDK当前状态机
     * @return 返回当前 SDK状态机值，如：SDK_STATE_XXXX
     */
    int getStateMachine();


    /**
     * @brief 就绪准备操作，仅在 SDK_STATE_INITIALIZED 状态下才能调用，异步调用，
     *        异步操作完成后，通过 onSdkPrepareDone() 回调就绪操作结果
     *        如果就绪操作成功，则 SDK状态切换到 SDK_STATE_RUNNING 状态
     *        如果就绪操作失败，则 SDK状态切换回 SDK_STATE_INITIALIZED
     * @param prepareParam : 就绪操作的参数
     * @param prepareListener : 就绪操作监听器
     * @return 返回错误码，XOK--就绪操作请求成功，SDK状态会切换到 SDK_STATE_PREPARING 开始异步就绪操作
     *                   XERR_BAD_STATE-- 当前 非SDK_STATE_INITIALIZED 状态下调用本函数
     */
    int prepare(final PrepareParam prepareParam, final OnPrepareListener prepareListener);


    /**
     * @brief 逆就绪停止运行操作，仅在 SDK_STATE_RUNNING 或者 SDK_STATE_RECONNECTING 或者 SDK_STATE_PREPARING
     *         这三种状态下才能调用，同步调用
     *         该函数会触发SDK状态先切换到 SDK_STATE_UNPREPARING 状态，然后切换到 SDK_STATE_INITIALIZED 状态
     * @return 返回错误码， XOK--逆就绪操作请求成功，SDK状态会切换到 SDK_STATE_INITIALIZED
     *                   XERR_BAD_STATE-- 当前SDK状态不是 SDK_STATE_RUNNING 或者 SDK_STATE_RUNNING
     */
    int unprepare();

    /**
     * @brief 获取就绪后的本地 userId
     * @return 返回当前就绪的 userId，如果当前还未就绪，则返回null
     */
    String getLocalUserId();

    /**
     * @brief 获取就绪后的本地 NodeId
     * @return 返回当前就绪的 NodeId，如果当前还未就绪，则返回null
     */
    String getLocalNodeId();


    /**
     * @brief 获取呼叫系统组件接口
     * @return 返回呼叫组件接口，如果当前还未进行初始化，则返回null
     */
    ICallkitMgr getCallkitMgr();


}
