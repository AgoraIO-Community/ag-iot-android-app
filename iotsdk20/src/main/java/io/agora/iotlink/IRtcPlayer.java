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


import android.view.SurfaceView;

/**
 * @brief RTC频道内播放器
 */
public interface IRtcPlayer  {


    //
    // 模块的状态机
    //
    public static final int RTCPLAYER_STATE_STOPPED = 0x0001;    ///< 当前无播放
    public static final int RTCPLAYER_STATE_PREPARING = 0x0002;  ///< 正在准备播放
    public static final int RTCPLAYER_STATE_PLAYING = 0x0003;    ///< 正常播放中



    /**
     * @brief 回调接口
     */
    public static interface ICallback {

        /**
         * @brief 播放启动完成，可以开始正常播放
         * @param errCode : 错误代码
         * @param channelName : 频道名
         * @param localUid : 本地rtc user id
         */
        default void onPrepareDone(int errCode, final String channelName, final int localUid) {}


    }




    ////////////////////////////////////////////////////////////////////////
    //////////////////////////// Public Methods ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 获取当前 RtcPlayer 状态机
     * @return 返回状态机
     */
    int getStateMachine();

    /**
     * @brief 注册回调接口
     * @param callback : 回调接口
     * @return 错误码
     */
    int registerListener(IRtcPlayer.ICallback callback);

    /**
     * @brief 注销回调接口
     * @param callback : 回调接口
     * @return 错误码
     */
    int unregisterListener(IRtcPlayer.ICallback callback);

    /**
     * @brief 进入频道，开始拉流播放，触发 onPrepareDone() 事件
     * @param channelName : 要加入的频道
     * @return 错误码
     */
    int start(final String channelName, final SurfaceView displayView);

    /**
     * @brief 退出频道，停止拉流
     * @return 错误码
     */
    int stop();


}
