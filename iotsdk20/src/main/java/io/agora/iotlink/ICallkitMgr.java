/**
 * @file IAccountMgr.java
 * @brief This file define the interface of call kit and RTC management
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink;



import android.view.View;
import java.util.UUID;


/*
 * @brief 呼叫系统接口
 */
public interface ICallkitMgr {

    /**
     * @brief 会话类型
     */
    public static final int SESSION_TYPE_UNKNOWN = 0x0000;           ///< 会话类型：未知
    public static final int SESSION_TYPE_DIAL = 0x0001;              ///< 会话类型：主叫
    public static final int SESSION_TYPE_INCOMING = 0x0002;          ///< 会话类型：被叫

    /**
     * @brief 音效属性
     */
    public enum AudioEffectId {
        NORMAL, OLDMAN, BABYBOY, BABYGIRL, ZHUBAJIE, ETHEREAL, HULK
    }

    /**
     * @brief 会话的状态机
     */
    public static final int SESSION_STATE_IDLE = 0x0000;           ///< 空闲状态
    public static final int SESSION_STATE_DIAL_REQING = 0x0001;    ///< 正在发送拨号请求
    public static final int SESSION_STATE_DIALING = 0x0002;        ///< 本地已经进入频道，等待对端响应
    public static final int SESSION_STATE_TALKING = 0x0003;        ///< 正在通话中
    public static final int SESSION_STATE_INCOMING = 0x0004;       ///< 设备端来电中



    /**
     * @brief RTC状态信息
     */
    public static class RtcNetworkStatus {
        public int totalDuration;
        public int txBytes;
        public int rxBytes;
        public int txKBitRate;
        public int txAudioBytes;
        public int rxAudioBytes;
        public int txVideoBytes;
        public int rxVideoBytes;
        public int rxKBitRate;
        public int txAudioKBitRate;
        public int rxAudioKBitRate;
        public int txVideoKBitRate;
        public int rxVideoKBitRate;
        public int lastmileDelay;
        public double cpuTotalUsage;
        public double cpuAppUsage;
        public int users;
        public int connectTimeMs;
        public int txPacketLossRate;
        public int rxPacketLossRate;
        public double memoryAppUsageRatio;
        public double memoryTotalUsageRatio;
        public int memoryAppUsageInKbytes;
    }

    /**
     * @brief 会话信息
     */
    public static class SessionInfo {
        public UUID mSessionId;         ///< 会话的唯一标识
        public String mLocalUserId;     ///< 当前用户的 UserId
        public String mLocalNodeId;     ///< 当前用户的 NodeId
        public String mPeerNodeId;      ///< 对端设备的 NodeId
        public int mState;              ///< 当前会话状态
        public int mType;               ///< 会话类型
        public int mUserCount;          ///< 在线用户数量

        @Override
        public String toString() {
            String infoText = "{ mSessionId=" + mSessionId
                    + ", mLocalUserId=" + mLocalUserId
                    + ", mLocalNodeId=" + mLocalNodeId
                    + ", mPeerNodeId=" + mPeerNodeId
                    + ", mState=" + mState
                    + ", mType=" + mType
                    + ", mUserCount=" + mUserCount + " }";
            return infoText;
        }
    }

    /**
     * @brief 账号管理回调接口
     */
    public static interface ICallback {

        /**
         * @brief 对端设备来电事件
         * @param sessionId : 会话唯一标识
         * @param peerNodeId : 对端设备 NodeId
         * @param attachMsg : 来电附加信息
         */
        default void onPeerIncoming(final UUID sessionId, final String peerNodeId,
                                    final String attachMsg) {}

        /**
         * @brief 呼叫请求完成回调事件
         * @param sessionId : 会话唯一标识
         * @param peerNodeId : 对端设备 NodeId
         * @param errCode : 错误代码，0表示呼叫请求成功
         */
        default void onDialDone(final UUID sessionId, final String peerNodeId, int errCode) {}

        /**
         * @brief 对端接听回调事件
         * @param sessionId : 会话唯一标识
         * @param peerNodeId : 对端设备 NodeId
         */
        default void onPeerAnswer(final UUID sessionId, final String peerNodeId) {}

        /**
         * @brief 对端挂断回调事件，此时再次通过sessionId查询会话对端已经查询不到了
         * @param sessionId : 会话唯一标识
         * @param peerNodeId : 对端设备 NodeId
         */
        default void onPeerHangup(final UUID sessionId, final String peerNodeId) {}

        /**
         * @brief 呼叫对端超时无响应回调事件
         * @param sessionId : 会话唯一标识
         * @param peerNodeId : 对端设备 NodeId
         */
        default void onPeerTimeout(final UUID sessionId, final String peerNodeId) {}

        /**
         * @brief 对端首帧出图
         * @param sessionId : 会话唯一标识
         * @param videoWidth : 首帧视频宽度
         * @param videoHeight : 首帧视频高度
         */
        default void onPeerFirstVideo(final UUID sessionId, int videoWidth, int videoHeight) {}

        /**
         * @brief 有其他用户上线进入通话事件 (非当前用户)
         * @param uid : 上线用户的 RTC userId
         * @param onlineUserCount : 当前在线的总用户数量
         */
        default void onOtherUserOnline(final UUID sessionId, int uid, int onlineUserCount) {}

        /**
         * @brief 有其他用户退出进入通话事件 （非当前用户）
         * @param uid : 退出用户的 RTC userId
         * @param onlineUserCount : 当前仍然在线的总用户数量
         */
        default void onOtherUserOffline(final UUID sessionId, int uid, int onlineUserCount) {}

        /**
         * @brief 错误事件，在呼叫系统中遇到任意错误时发生
         * @param sessionId : 会话唯一标识
         * @param errCode : 错误代码
         */
        default void onSessionError(final UUID sessionId, int errCode) {}

        /**
         * @brief 录像错误事件
         * @param sessionId : 会话唯一标识
         * @param errCode : 错误码
         */
        default void onRecordingError(final UUID sessionId, int errCode) {}

        /**
         * @brief 设备端截图完成事件
         * @param sessionId : 会话唯一标识
         * @param errCode : 错误码：0表示截图成功；-1表示写入文件失败或JPEG编码失败；
         *                  -2表示方法调用后 1秒内没有收到指定用户的视频帧；-3表示截图调用过于频繁
         * @param filePath : 截图保存的路径
         * @param width : 截图宽度
         * @param height : 截图高度
         */
        default void onCaptureFrameDone(final UUID sessionId, int errCode,
                                        final String filePath, int width, int height) {}
    }

    /**
     * @brief 主叫参数
     */
    public static class DialParam {
        public String mPeerNodeId;          ///< 要呼叫的对端设备 NodeId
        public String mAttachMsg;           ///< 主叫呼叫附带信息
        public boolean mPubLocalAudio;      ///< 设备端接听后是否立即推送本地音频流

        @Override
        public String toString() {
            String infoText = "{\n mPeerNodeId=" + mPeerNodeId
                    + ",\n mAttachMsg=" + mAttachMsg
                    + ",\n mPubLocalAudio=" + mPubLocalAudio + " }";
            return infoText;
        }
    }

    /**
     * @brief 主叫请求返回结果
     */
    public static class DialResult {
        public UUID mSessionId;         ///< 主叫请求后，分配的唯一的 sessionId
        public int mErrCode;            ///< 请求错误码，有如下的值：
                                        ///<  XOK--表示成功；
                                        ///<  XERR_NETWORK-- SDK当前非 RUNNING状态
                                        ///<  XERR_BAD_STATE-- 相应对端已经在通话中
                                        ///<  XERR_JSON_WRITE--请求数据组包失败
    }


    ////////////////////////////////////////////////////////////////////////
    //////////////////////////// Public Methods ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 注册回调接口
     * @param callback : 回调接口
     * @return 错误码
     */
    int registerListener(ICallkitMgr.ICallback callback);

    /**
     * @brief 注销回调接口
     * @param callback : 回调接口
     * @return 错误码
     */
    int unregisterListener(ICallkitMgr.ICallback callback);


    /**
     * @brief 根据 sessionId 获取会话状态信息
     * @param sessionId : 会话唯一标识
     * @return 返回会话信息，如果没有查询到会话，则返回null
     */
    SessionInfo getSessionInfo(final UUID sessionId);


    /**
     * @brief 呼叫设备
     * @param dialParam : 呼叫参数信息
     * @return 会话的 sessionId 和 错误码，如果呼叫成功则返回唯一的sessionId
     */
    DialResult callDial(final DialParam dialParam);

    /**
     * @brief 挂断指定的通话
     * @param sessionId : 会话唯一标识
     * @return 错误码，目前总是返回 XOK，如果找不到该会话，清除相关信息，也返回 XOK
     */
    int callHangup(final UUID sessionId);

    /**
     * @brief 接听当前来电
     * @param sessionId : 会话唯一标识
     * @param pubLocalAudio : 接听后是否立即推送本地音频
     * @return 错误码，XOK--接听成功；XERR_INVALID_PARAM--没有找到该会话；XERR_BAD_STATE--该会话类型不是来电会话；
     */
    int callAnswer(final UUID sessionId, boolean pubLocalAudio);

    /**
     * @brief 设置对端视频显示控件，如果不设置则不显示对端视频
     * @param sessionId : 会话的唯一标识
     * @param peerView: 设备端视频显示控件
     * @return 错误码，XOK--设置成功； XERR_INVALID_PARAM--没有找到该会话； XERR_UNSUPPORTED--设置失败
     */
    int setPeerVideoView(final UUID sessionId, final View peerView);

    /**
     * @brief 禁止/启用 本地音频推流到对端
     * @param sessionId : 会话唯一标识
     * @param mute: 是否禁止
     * @return 错误码，XOK--设置成功； XERR_INVALID_PARAM--没有找到该会话； XERR_UNSUPPORTED--设置失败
     */
    int muteLocalAudio(final UUID sessionId, boolean mute);

    /**
     * @brief 禁止/启用 拉流对端视频
     * @param sessionId : 会话唯一标识
     * @param mute: 是否禁止
     * @return 错误码，XOK--设置成功； XERR_INVALID_PARAM--没有找到该会话； XERR_UNSUPPORTED--设置失败
     */
    int mutePeerVideo(final UUID sessionId, boolean mute);

    /**
     * @brief 禁止/启用 拉流对端音频
     * @param sessionId : 会话唯一标识
     * @param mute: 是否禁止
     * @return 错误码，XOK--设置成功； XERR_INVALID_PARAM--没有找到该会话； XERR_UNSUPPORTED--设置失败
     */
    int mutePeerAudio(final UUID sessionId, boolean mute);

    /**
     * @brief 截屏对端视频帧图像请求，该函数是异步调用，截图完成后会触发 onCaptureFrameDone() 回调
     * @param sessionId : 会话唯一标识
     * @param saveFilePath : 保存的文件（应用层确保文件有可写权限）
     * @return 错误码，XOK--截图请求成功； XERR_INVALID_PARAM--没有找到该会话； XERR_UNSUPPORTED--设置失败
     */
    int capturePeerVideoFrame(final UUID sessionId, final String saveFilePath);

    /**
     * @brief 开始录制当前通话（包括音视频流），仅在通话状态下才能调用
     *         同一时刻只能启动一路录像功能
     * @param sessionId : 会话唯一标识
     * @param outFilePath : 输出保存的视频文件路径
     * @return 错误码，XOK--开始录制成功； XERR_INVALID_PARAM--没有找到该会话；
     */
    int talkingRecordStart(final UUID sessionId, final String outFilePath);

    /**
     * @brief 停止录制当前通话，仅在通话状态下才能调用
     * @param sessionId : 会话唯一标识
     * @return 错误码，XOK--开始录制成功； XERR_INVALID_PARAM--没有找到该会话；
     */
    int talkingRecordStop(final UUID sessionId);

    /**
     * @brief 判断当前是否正在本地录制
     * @param sessionId : 会话唯一标识
     * @return true 表示正在本地录制频道； false: 不在录制
     */
    boolean isTalkingRecording(final UUID sessionId);





    /**
     * @brief 获取当前网络状态
     * @return 返回RTC网络状态信息，如果当前没有任何一个会话，则返回null
     */
    RtcNetworkStatus getNetworkStatus();

    /**
     * @brief 设置本地播放所有混音后音频的音量
     * @param volumeLevel: 音量级别
     * @return 错误码，XOK--设置成功； XERR_UNSUPPORTED--设置失败
     */
    int setPlaybackVolume(int volumeLevel);

    /**
     * @brief 设置音效效果（通常是变声等音效），如果本地推音频流，会影响推送音频效果
     * @param effectId: 音效Id
     * @return 错误码，XOK--设置成功； XERR_UNSUPPORTED--设置失败
     */
    int setAudioEffect(final AudioEffectId effectId);

    /**
     * @brief 获取指定会话的当前音效
     * @return 返回当前设置的音效
     */
    AudioEffectId getAudioEffect();

    /**
     * @brief 设置RTC私有参数
     * @param privateParam : 要设置的私参
     * @return 错误码，XOK--设置成功； XERR_UNSUPPORTED--设置失败
     */
    int setRtcPrivateParam(String privateParam);


}
