/**
 * @file TalkingEngine.java
 * @brief This file implement audio and video communication
 *        作为IoT设备端进入通话后，需要将本地的音视频往频道内推流，同时订阅对端的音频流，并进行播放
 *        作为移动应端进入通话后，需要将本地的音频往频道内推流，同时订阅对端的音视频流，并进行播放
 *        因为在服务器端针对每个账户分配了固定的 RtcUserId，因此频道的加入、退出等不用同步等待了
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2021-10-15
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.rtcsdk;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import android.view.View;


import io.agora.avmodule.AvAudioFrame;
import io.agora.avmodule.AvCapability;
import io.agora.avmodule.AvFrameQueue;
import io.agora.avmodule.AvMediaRecorder;
import io.agora.avmodule.AvRecorderParam;
import io.agora.avmodule.AvVideoFrame;
import io.agora.avmodule.IAvRecorderCallback;
import io.agora.base.internal.CalledByNative;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.callkit.SessionCtx;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.utils.ImageConvert;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import io.agora.base.VideoFrame;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IAudioFrameObserver;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcConnection;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineEx;
import io.agora.rtc2.audio.AudioParams;
import io.agora.rtc2.video.IVideoFrameObserver;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;



public class TalkingEngine implements AGEventHandler,
        IVideoFrameObserver, IAudioFrameObserver,
        AvRecorderParam.IAvFrameReader, IAvRecorderCallback {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 通话引擎回调接口
     */
    public interface ICallback {



        /////////////////////////////////////////////////////////////////////////////
        //////////////////// TalkingEngine.ICallback 回调处理 ////////////////////////
        /////////////////////////////////////////////////////////////////////////////
        /**
         * @brief 本地加入频道成功
         */
        default void onTalkingJoinDone(final UUID sessionId, final String channel, int uid) { }

        /**
         * @brief 本地离开频道成功
         */
        default void onTalkingLeftDone(final UUID sessionId) {  }

        /**
         * @brief 用户上线
         */
        default void onUserOnline(final UUID sessionId, int uid, int elapsed) {  }

        /**
         * @brief 用户下线
         */
        default void onUserOffline(final UUID sessionId, int uid, int reason) {  }

        /**
         * @brief 对端首帧出图
         */
        default void onPeerFirstVideoDecoded(final UUID sessionId, int uid,
                                             int videoWidth, int videoHeight) { }

        /**
         * @brief 截图完成回调
         */
        default void onSnapshotTaken(final UUID sessionId, int uid,
                                     final String filePath, int width, int height, int errCode) { }

        /**
         * @brief 录像时产生错误
         */
        default void onRecordingError(final UUID sessionId, int errCode) {  }
    }


    ///////////////////////////////////////////////////////////////////////////
    ///////////////////// Constant Definition /////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private final String TAG = "IOTSDK/TalkingEngine";
    private static final String RECORD_VIDEO_CODEC = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final String RECORD_AUDIO_CODEC = MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int RECORD_TARGET_WIDTH = 1280;
    private static final int RECORD_TARGET_HEIGHT = 720;
    private static final int RECORD_AUDIO_SAMPLERATE = 44100;
    private static final int RECORD_AUDIO_CHANNELS = 2;

    private static final int SET_AUD_CODEC = 0;         ///< 设置音频格式, 9:：G722;  8：G711A； 0：G711U；
    private static final int SET_AUD_SAMPLERATE = 8000; ///< 设置音频采样率
    private static final int SET_AUD_CHANNELS = 1;      ///< 设置音频通道数


    /*
     * @brief 通话引擎初始化参数
     */
    public class InitParam {
        public Context mContext;
        public String mAppId;
        public ICallback mCallback;
    }



    ///////////////////////////////////////////////////////////////////////////
    ///////////////////// Variable Definition /////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private InitParam mInitParam;           ///< TalkingEngine初始化参数
    private EngineConfig mRtcEngCfg;        ///< RtcEngine相关配置参数
    private MyEngineEventHandler mRtcEngEventHandler;   ///< RtcEngine事件处理器
    private RtcEngineEx mRtcEngine;         ///< RtcEngine实例对象

    private final ICallkitMgr.RtcNetworkStatus mRtcStatus = new ICallkitMgr.RtcNetworkStatus();
    private long mRtcInitTime;
    private int mVoiceChanger = Constants.AUDIO_EFFECT_OFF;

    private final Object mVideoDataLock = new Object();
    private byte[] mInVideoYData;           ///< 订阅的视频帧YUV数据
    private byte[] mInVideoUData;
    private byte[] mInVideoVData;
    private volatile int mInVideoWidth;     ///< 订阅的视频帧宽度
    private volatile int mInVideoHeight;    ///< 订阅的视频帧高度
    private volatile int mInVideoRotation;  ///< 订阅的视频旋转角度
    private boolean mCacheVideoFrame = false;   ///< 标记当前是否缓存视频帧

    private final Object mAudioDataLock = new Object();
    private int mInAudioBytesPerSample = 2;               ///< 订阅的音频每个采样字节数
    private int mInAudioChannels = SET_AUD_CHANNELS;      ///< 订阅的音频通道数
    private int mInAudioSampleRate = SET_AUD_SAMPLERATE;  ///< 订阅的音频采样率
    private AvFrameQueue mInAudioFrameQueue = new AvFrameQueue();

    private AvMediaRecorder mRecorder;          ///< 音视频录像器
    private AvRecorderParam  mRecorderParam;    ///< 录像参数
    private int mVideoFrameIndex = 0;           ///< 当前视频帧索引
    private int mAudioFrameIndex = 0;           ///< 当前音频帧索引
    private long mAudioTimestamp = 0;           ///< 音频时长的累计

    private AvCapability.VideoCaps mVideoCaps;
    private int mMaxEncodeWidth = RECORD_TARGET_WIDTH;
    private int mMaxEncodeHeight = RECORD_TARGET_HEIGHT;

    ///////////////////////////////////////////////////////////////////////////
    //////////////////////// Public Methods ///////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 初始化 RtcSDK
     */
    public boolean initialize(InitParam initParam) {
        mInitParam = initParam;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mVideoCaps = AvCapability.getVideoCapability(RECORD_VIDEO_CODEC);
            if (mMaxEncodeWidth > mVideoCaps.mWidthRange.getUpper()) {
                mMaxEncodeWidth = mVideoCaps.mWidthRange.getUpper();
            }
            if (mMaxEncodeHeight > mVideoCaps.mHeightRange.getUpper()) {
                mMaxEncodeHeight = mVideoCaps.mHeightRange.getUpper();
            }
        }

        //
        // 初始RtcEngine配置信息
        //
        mRtcEngCfg = new EngineConfig();
        int prefIndex = ConstantApp.DEFAULT_PROFILE_IDX;  // 480P
        VideoEncoderConfiguration.VideoDimensions dimension = ConstantApp.VIDEO_DIMENSIONS[prefIndex];
        mRtcEngCfg.mClientRole = Constants.CLIENT_ROLE_BROADCASTER;
        mRtcEngCfg.mVideoDimension = dimension;

        //
        // 创建RtcEngine事件回调处理
        //
        mRtcEngEventHandler = new MyEngineEventHandler(mInitParam.mContext);
        mRtcEngEventHandler.addEventHandler(this);

        //
        // 创建 RtcEngine实例对象
        //
        try {
            String appId = mInitParam.mAppId;
            mRtcEngine = (RtcEngineEx) RtcEngine.create(mInitParam.mContext, appId, mRtcEngEventHandler.mRtcEventHandler);
            ALog.getInstance().d(TAG, "<initialize> mAppId=" + mInitParam.mAppId);
        } catch (Exception e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<initialize> " + Log.getStackTraceString(e));
            return false;
        }
        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        mRtcEngine.enableVideo();  // 启动音视频采集和推流处理
        mRtcEngine.enableAudio();

        String log_file_path = Environment.getExternalStorageDirectory()
                + File.separator + mInitParam.mContext.getPackageName() + "/log/agora-rtc.log";
        mRtcEngine.setLogFile(log_file_path);
        // Warning: only enable dual stream mode if there will be more than one broadcaster in the channel
        //mRtcEngine.enableDualStreamMode(false);

        // 设置视频编码参数
        mRtcEngine.setVideoEncoderConfiguration(
                new VideoEncoderConfiguration(mRtcEngCfg.mVideoDimension,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE));

        // 设置广播模式
        mRtcEngine.setClientRole(mRtcEngCfg.mClientRole);

        // 设置私参： 默认G711U格式。 音频G722编码--9;  音频G711U--0;  音频G711A--8
        String codecParam = "{\"che.audio.custom_payload_type\":0}";
        int ret = mRtcEngine.setParameters(codecParam);
        if (ret != 0) {
            ALog.getInstance().e(TAG, "<initialize> fail to set audio codec, ret=" + ret);
        } else {
            ALog.getInstance().w(TAG, "<initialize> set audio codec successful, codecParam=" + codecParam);
        }

        // 设置私参：采样率，G711U是 8kHz
        String smplRate = "{\"che.audio.input_sample_rate\":8000}";
        ret = mRtcEngine.setParameters(smplRate);
        if (ret != 0) {
            ALog.getInstance().e(TAG, "<initialize> fail to set sample rate, ret=" + ret);
        } else {
            ALog.getInstance().w(TAG, "<initialize> set sample rate successful, smplRate=" + smplRate);
        }

//        // 设置私参：使用硬件解码
//        String hwDecoder = "{\"engine.video.enable_hw_decoder\":true}";
//        ret = mRtcEngine.setParameters(hwDecoder);
//        if (ret != 0) {
//            ALog.getInstance().e(TAG, "<initialize> fail to set HW decoder, ret=" + ret);
//        }

        mRtcEngine.registerVideoFrameObserver(this);
        mRtcEngine.registerAudioFrameObserver(this);


        mRtcInitTime = System.currentTimeMillis();
        mRtcStatus.rxAudioKBitRate = (int)(Math.random()*10+1);
        mRtcStatus.rxVideoKBitRate = (int)(Math.random()*90+5);
        mRtcStatus.rxKBitRate = mRtcStatus.rxAudioKBitRate + mRtcStatus.rxVideoKBitRate;

        ALog.getInstance().d(TAG, "<initialize> done, mMaxEncodeWidth=" + mMaxEncodeWidth
                    + ", mMaxEncodeHeight=" + mMaxEncodeHeight);
        return true;
    }

    /**
     * @brief 释放 RtcSDK
     */
    public void release()
    {
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
            RtcEngine.destroy();
            mRtcEngine = null;
        }

        if (mRtcEngEventHandler != null) {
            mRtcEngEventHandler.release();
            mRtcEngEventHandler = null;
            ALog.getInstance().d(TAG, "<release> done");
        }
    }

    /**
     * @brief 判断RtcSDK是否创建就绪
     */
    public boolean isReady() {
        return (mRtcEngine != null);
    }

    /**
     * @brief 私参设置
     */
    public int setParameters(String param) {
        if (mRtcEngine == null) {
            ALog.getInstance().e(TAG, "<setParameters> bad state");
            return Constants.ERR_NOT_READY;
        }
        int ret = mRtcEngine.setParameters(param);
        ALog.getInstance().d(TAG, "<setParameter> param=" + param + " , ret=" + ret);
        return ret;
    }

    /**
     * @brief 获取当前RTC网络状态
     */
    public ICallkitMgr.RtcNetworkStatus getNetworkStatus() {
        ICallkitMgr.RtcNetworkStatus retStatus = new ICallkitMgr.RtcNetworkStatus();
        synchronized (mRtcStatus) {
            retStatus.totalDuration = mRtcStatus.totalDuration;
            retStatus.txBytes = mRtcStatus.txBytes;
            retStatus.rxBytes = mRtcStatus.rxBytes;
            retStatus.txKBitRate = mRtcStatus.txKBitRate;
            retStatus.rxKBitRate = mRtcStatus.rxKBitRate;
            retStatus.txAudioBytes = mRtcStatus.txAudioBytes;
            retStatus.rxAudioBytes = mRtcStatus.rxAudioBytes;
            retStatus.txVideoBytes = mRtcStatus.txVideoBytes;
            retStatus.rxVideoBytes = mRtcStatus.rxVideoBytes;
            retStatus.txAudioKBitRate = mRtcStatus.txAudioKBitRate;
            retStatus.rxAudioKBitRate = mRtcStatus.rxAudioKBitRate;
            retStatus.txVideoKBitRate = mRtcStatus.txVideoKBitRate;
            retStatus.rxVideoKBitRate = mRtcStatus.rxVideoKBitRate;
            retStatus.txPacketLossRate = mRtcStatus.txPacketLossRate;
            retStatus.rxPacketLossRate = mRtcStatus.rxPacketLossRate;
            retStatus.lastmileDelay = mRtcStatus.lastmileDelay;
            retStatus.connectTimeMs = mRtcStatus.connectTimeMs;
            retStatus.cpuAppUsage = mRtcStatus.cpuAppUsage;
            retStatus.cpuTotalUsage = mRtcStatus.cpuTotalUsage;
            retStatus.users = mRtcStatus.users;
            retStatus.memoryAppUsageRatio = mRtcStatus.memoryAppUsageRatio;
            retStatus.memoryTotalUsageRatio = mRtcStatus.memoryTotalUsageRatio;
            retStatus.memoryAppUsageInKbytes = mRtcStatus.memoryAppUsageInKbytes;
        }

        return retStatus;
    }

    /**
     * @brief 根据会话信息，加入指定的频道
     */
    public boolean joinChannel(final SessionCtx sessionCtx) {
        if (mRtcEngine == null) {
            ALog.getInstance().e(TAG, "<joinChannel> bad state");
            return false;
        }
        ALog.getInstance().d(TAG, "<joinChannel> Enter, sessionCtx=" + sessionCtx.toString());

        // 加入频道
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
        options.clientRoleType = mRtcEngCfg.mClientRole;
        options.autoSubscribeAudio = false;     // 不自动订阅音频
        options.autoSubscribeVideo = false;     // 不自动订阅视频
        options.publishCameraTrack = false;
        options.publishMicrophoneTrack = sessionCtx.mPubLocalAudio;

        RtcConnection rtcConnection = new RtcConnection();
        rtcConnection.channelId = sessionCtx.mChnlName;
        rtcConnection.localUid = sessionCtx.mLocalUid;

        RtcChnlEventHandler eventHandler = new RtcChnlEventHandler(this, sessionCtx.mSessionId);
        int ret = mRtcEngine.joinChannelEx(sessionCtx.mRtcToken, rtcConnection, options, eventHandler);
        if (ret != Constants.ERR_OK) {
            ALog.getInstance().e(TAG, "<joinChannel> Exit with error, ret=" + ret);
            return false;
        }

        // 设置私参： 默认G711U格式。 音频G722编码--9;  音频G711U--0;  音频G711A--8
        String codecParam = "{\"che.audio.custom_payload_type\":0}";
        ret = mRtcEngine.setParameters(codecParam);
        if (ret != 0) {
            ALog.getInstance().e(TAG, "<joinChannel> fail to set audio codec, ret=" + ret);
        } else {
            ALog.getInstance().w(TAG, "<joinChannel> set audio codec successful, codecParam=" + codecParam);
        }

        // 设置私参：采样率，G711U是 8kHz
        String smplRate = "{\"che.audio.input_sample_rate\":8000}";
        ret = mRtcEngine.setParameters(smplRate);
        if (ret != 0) {
            ALog.getInstance().e(TAG, "<joinChannel> fail to set sample rate, ret=" + ret);
        } else {
            ALog.getInstance().w(TAG, "<joinChannel> set sample rate successful, smplRate=" + smplRate);
        }

//        // 设置私参：使用硬件解码
//        String hwDecoder = "{\"engine.video.enable_hw_decoder\":true}";
//        ret = mRtcEngine.setParameters(hwDecoder);
//        if (ret != 0) {
//            ALog.getInstance().e(TAG, "<joinChannel> fail to set HW decoder, ret=" + ret);
//        }

        // APP端永远不推视频流
        ret = mRtcEngine.muteLocalVideoStreamEx(true, rtcConnection);
        if (ret != Constants.ERR_OK) {
            ALog.getInstance().e(TAG, "<joinChannel> muteLocalVideoStream() error, ret=" + ret);
        }

        // APP端根据参数，决定是否推音频流
        boolean muteLocalAudio = (!sessionCtx.mPubLocalAudio);
        ret = mRtcEngine.muteLocalAudioStreamEx(muteLocalAudio, rtcConnection);
        if (ret != Constants.ERR_OK) {
            ALog.getInstance().e(TAG, "<joinChannel> muteLocalAudioStream() error, ret=" + ret
                    + ", muteLocalAudio=" + muteLocalAudio);
        }

        // APP端根据参数，决定是否订阅设备端视频流
        boolean muteRemoteVideo = (!sessionCtx.mSubDevVideo);
        ret = mRtcEngine.muteRemoteVideoStreamEx(sessionCtx.mPeerUid, muteRemoteVideo, rtcConnection);
        if (ret != Constants.ERR_OK) {
            ALog.getInstance().e(TAG, "<joinChannel> muteRemoteVideoStreamEx() error, ret=" + ret
                    + ", muteRemoteVideo=" + muteRemoteVideo);
        }

        // APP端根据参数，决定是否订阅设备端音频流
        boolean muteRemoteAudio = (!sessionCtx.mSubDevAudio);
        ret = mRtcEngine.muteRemoteAudioStreamEx(sessionCtx.mPeerUid, muteRemoteAudio, rtcConnection);
        if (ret != Constants.ERR_OK) {
            ALog.getInstance().e(TAG, "<joinChannel> muteRemoteAudioStreamEx() error, ret=" + ret
                    + ", muteRemoteAudio=" + muteRemoteAudio);
        }

        ALog.getInstance().d(TAG, "<joinChannel> Exit");
        return true;
    }

    /**
     * @brief 根据会话信息，退出指定的频道
     */
    public boolean leaveChannel(final SessionCtx sessionCtx)   {
        if (mRtcEngine == null) {
            ALog.getInstance().e(TAG, "<leaveChannel> bad state");
            return false;
        }

        ALog.getInstance().d(TAG, "<leaveChannel> Enter, sessionCtx=" + sessionCtx.toString());

        // 退出频道
        RtcConnection rtcConnection = new RtcConnection();
        rtcConnection.channelId = sessionCtx.mChnlName;
        rtcConnection.localUid = sessionCtx.mLocalUid;
        int ret = mRtcEngine.leaveChannelEx(rtcConnection);
        if (ret != Constants.ERR_OK) {
            ALog.getInstance().e(TAG, "<leaveChannel> Exit with error, ret=" + ret);
            return false;
        }

        ALog.getInstance().d(TAG, "<leaveChannel> Exit");
        return true;
    }

    /**
     * @brief 根据会话信息，设置设备端视频帧显示控件
     */
    public boolean setRemoteVideoView(final SessionCtx sessionCtx, final View remoteView)
    {
        if (mRtcEngine == null) {
            ALog.getInstance().e(TAG, "<setRemoteVideoView> bad state");
            return false;
        }

        RtcConnection rtcConnection = new RtcConnection();
        rtcConnection.channelId = sessionCtx.mChnlName;
        rtcConnection.localUid = sessionCtx.mLocalUid;

        VideoCanvas videoCanvas = new VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_FIT, sessionCtx.mPeerUid);
        int ret = mRtcEngine.setupRemoteVideoEx(videoCanvas, rtcConnection);
        ALog.getInstance().d(TAG, "<setRemoteVideoView> done, remoteView=" + remoteView
                + ", sessionCtx=" + sessionCtx.toString() + ", ret=" + ret);
        return (ret == Constants.ERR_OK);
    }

    /**
     * @brief 设置指定频道的设备端，是否推送视频流
     */
    public boolean mutePeerVideoStream(final SessionCtx sessionCtx) {
        if (mRtcEngine == null) {
            ALog.getInstance().e(TAG, "<mutePeerVideoStream> bad state");
            return false;
        }

        RtcConnection rtcConnection = new RtcConnection();
        rtcConnection.channelId = sessionCtx.mChnlName;
        rtcConnection.localUid = sessionCtx.mLocalUid;

        boolean mutePeerVideo = (!sessionCtx.mSubDevVideo);
        int ret = mRtcEngine.muteRemoteVideoStreamEx(sessionCtx.mPeerUid, mutePeerVideo, rtcConnection);
        ALog.getInstance().d(TAG, "<mutePeerVideoStream> sessionCtx=" + sessionCtx
                    + ", mutePeerVideo=" + mutePeerVideo + ", ret=" + ret);
        return (ret == Constants.ERR_OK);
    }

    /**
     * @brief 设置指定频道的设备端，是否推送音频流
     */
    public boolean mutePeerAudioStream(final SessionCtx sessionCtx) {
        if (mRtcEngine == null) {
            ALog.getInstance().e(TAG, "<mutePeerAudioStream> bad state");
            return false;
        }
        RtcConnection rtcConnection = new RtcConnection();
        rtcConnection.channelId = sessionCtx.mChnlName;
        rtcConnection.localUid = sessionCtx.mLocalUid;

        boolean mutePeerAudio = (!sessionCtx.mSubDevAudio);
        int ret = mRtcEngine.muteRemoteAudioStreamEx(sessionCtx.mPeerUid, mutePeerAudio, rtcConnection);
        ALog.getInstance().d(TAG, "<mutePeerAudioStream> sessionCtx=" + sessionCtx
                + ", mutePeerAudio=" + mutePeerAudio + ", ret=" + ret);
        return (ret == Constants.ERR_OK);
    }

    /**
     * @brief 设置指定频道内设备端视频帧截屏
     */
    public boolean takeSnapshot(final SessionCtx sessionCtx, final String saveFilePath)
    {
        long t1 = System.currentTimeMillis();
        if (mRtcEngine == null) {
            ALog.getInstance().e(TAG, "<capturePeerVideoFrame> bad state");
            return false;
        }
        RtcConnection rtcConnection = new RtcConnection();
        rtcConnection.channelId = sessionCtx.mChnlName;
        rtcConnection.localUid = sessionCtx.mLocalUid;
        int ret = mRtcEngine.takeSnapshotEx(rtcConnection, sessionCtx.mPeerUid, saveFilePath);
        long t2 = System.currentTimeMillis();

        ALog.getInstance().d(TAG, "<takeSnapshot> sessionCtx=" + sessionCtx + ", ret=" + ret
                + ", saveFilePath=" + saveFilePath + ", costTime=" + (t2-t1) );
        return (ret == Constants.ERR_OK);
    }

    /**
     * @brief 设置本地端是否推送音频流
     */
    public boolean muteLocalAudioStream(final SessionCtx sessionCtx) {
        if (mRtcEngine == null) {
            ALog.getInstance().e(TAG, "<muteLocalAudioStream> bad state");
            return false;
        }
        long t1 = System.currentTimeMillis();
        int ret, retLocalAud, retDevVideo, retDevAudio;

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
        options.clientRoleType = mRtcEngCfg.mClientRole;
        options.autoSubscribeAudio = false;  // 不自动订阅音频
        options.autoSubscribeVideo = false;  // 不自动订阅视频
        options.publishCameraTrack = false;
        options.publishMicrophoneTrack = sessionCtx.mPubLocalAudio;

        RtcConnection rtcConnection = new RtcConnection();
        rtcConnection.channelId = sessionCtx.mChnlName;
        rtcConnection.localUid = sessionCtx.mLocalUid;

        boolean muteLocalAudio = (!sessionCtx.mPubLocalAudio);
        ret = mRtcEngine.updateChannelMediaOptionsEx(options, rtcConnection);
        retLocalAud = mRtcEngine.muteLocalAudioStreamEx(muteLocalAudio, rtcConnection);

        boolean mutePeerAudio = (!sessionCtx.mSubDevAudio);
        retDevAudio = mRtcEngine.muteRemoteAudioStreamEx(sessionCtx.mPeerUid, mutePeerAudio, rtcConnection);
        ALog.getInstance().d(TAG, "<muteLocalAudioStream> mutePeerAudio=" + mutePeerAudio
                + ", retDevAudio=" + retDevAudio);

        boolean mutePeerVideo = (!sessionCtx.mSubDevVideo);
        retDevVideo = mRtcEngine.muteRemoteVideoStreamEx(sessionCtx.mPeerUid, mutePeerVideo, rtcConnection);
        ALog.getInstance().d(TAG, "<muteLocalAudioStream> mutePeerVideo=" + mutePeerVideo
                + ", retDevVideo=" + retDevVideo);


        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<muteLocalAudioStream> muteLocalAudio=" + muteLocalAudio
                + ", retLocalAud=" + retLocalAud + ", ret=" + ret + ", costTime=" + (t2-t1));
        return (ret == Constants.ERR_OK);
    }

    /**
     * @brief 设置指定频道的设备端，是否推送音频流
     */
    public boolean setAudioEffect(int voice_changer) {
        if (mRtcEngine == null) {
            ALog.getInstance().e(TAG, "<setAudioEffect> bad state");
            return false;
        }
        mVoiceChanger = voice_changer;
        int ret = mRtcEngine.setAudioEffectPreset(voice_changer);
        ALog.getInstance().d(TAG, "<setAudioEffect> voice_changer=" + voice_changer + ", ret=" + ret);
        return (ret == Constants.ERR_OK);
    }

    public int getAudioEffect() {
        return mVoiceChanger;
    }

    /**
     * @brief 设置播放音量
     */
    public boolean setPlaybackVolume(int volume) {
        if (mRtcEngine == null) {
            ALog.getInstance().e(TAG, "<setPlaybackVolume> bad state");
            return false;
        }
        int ret = mRtcEngine.adjustPlaybackSignalVolume(volume);
        ALog.getInstance().d(TAG, "<setPlaybackVolume> volume=" + volume + ", ret=" + ret);
        return (ret == Constants.ERR_OK);
    }


    /**
     * @brief 开始频道内录像
     * @param outputFile : 录像保存的文件路径
     * @return 返回错误码
     */
    //@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public int recordingStart(final SessionCtx sessionCtx, final String outputFile) {
        if (mRecorder != null) {
            ALog.getInstance().e(TAG, "<recordingStart> recording is ongoing!");
            return ErrCode.XERR_BAD_STATE;
        }

        int videoWidth, videoHeight, videoRotation;
        int bytesPerSample, channels, sampleRate;

        synchronized (mVideoDataLock) {
            videoWidth = mInVideoWidth;
            videoHeight = mInVideoHeight;
            videoRotation = mInVideoRotation;
            mCacheVideoFrame = true;
        }

        synchronized (mAudioDataLock) {
            bytesPerSample = mInAudioBytesPerSample;
            channels = mInAudioChannels;
            sampleRate = mInAudioSampleRate;
        }

        mRecorderParam = new AvRecorderParam();
        mRecorderParam.mContext = mInitParam.mContext;
        mRecorderParam.mAvReader = this;
        mRecorderParam.mOutFilePath = outputFile;
        mRecorderParam.mCallback = this;

        //
        // 设置视频编码参数
        //
        mRecorderParam.mVideoCodec = RECORD_VIDEO_CODEC;
        mRecorderParam.mColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        mRecorderParam.mVideoWidth = videoWidth;
        mRecorderParam.mVideoHeight = videoHeight;
        mRecorderParam.mRotation = 0;
        mRecorderParam.mFrameRate = 15;      // 15FPS，跟RTC保持一致
        mRecorderParam.mGopFrame = mRecorderParam.mFrameRate; // 每秒一个GOP

        // 根据视频宽高和帧率计算的模板视频的码率
        int calcVideoBitRate = calcVideoBitrate(mRecorderParam.mVideoWidth, mRecorderParam.mVideoHeight,
                mRecorderParam.mFrameRate);
        mRecorderParam.mVideoBitRate = calcVideoBitRate;

        // 获取Video编码模式
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AvCapability.VideoCaps videoCaps = AvCapability.getVideoCapability(RECORD_VIDEO_CODEC);
            String videoCapsText = videoCaps.toString();
            Log.d(TAG, "<recordingStart> videoCapability=" + videoCapsText);
            if (videoCaps.mBitrateCqSupported) {
                mRecorderParam.mVBitRateMode = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ;
                Log.d(TAG, "<recordingStart> BITRATE_MODE_CQ, mVideoBitRate=" + mRecorderParam.mVideoBitRate);
            } else {
                mRecorderParam.mVBitRateMode = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR;
                Log.d(TAG, "<recordingStart> BITRATE_MODE_VBR, mVideoBitRate=" + mRecorderParam.mVideoBitRate);
            }

            // 判断码率范围并进行调整
            int bitrateLower = videoCaps.mBitrateRange.getLower();
            int bitrateUpper = videoCaps.mBitrateRange.getUpper();
            int bitrateAdjust = (mRecorderParam.mVideoWidth * mRecorderParam.mVideoHeight);
            if (mRecorderParam.mVideoBitRate < bitrateLower) { // 码率小于下限
                while (mRecorderParam.mVideoBitRate < bitrateLower) {
                    mRecorderParam.mVideoBitRate += bitrateAdjust;
                }
                Log.d(TAG, "<recordingStart> videobitrate lower, Adjust videoBitrate=" + mRecorderParam.mVideoBitRate);

            } else if (mRecorderParam.mVideoBitRate > bitrateUpper) { // 码率大于上限
                while (mRecorderParam.mVideoBitRate > bitrateUpper) {
                    mRecorderParam.mVideoBitRate -= bitrateAdjust;
                }
                Log.d(TAG, "<recordingStart> videobitrate upper, Adjust videoBitrate=" + mRecorderParam.mVideoBitRate);
            }

        } else {
            mRecorderParam.mVBitRateMode = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR;
        }


        //
        // 设置音频流编码
        //
        mRecorderParam.mAudioCodec = RECORD_AUDIO_CODEC;
        mRecorderParam.mSampleFmt = AudioFormat.ENCODING_PCM_16BIT;
        mRecorderParam.mChannels = channels;
        mRecorderParam.mSampleRate = sampleRate;
        int calcAudioBitRate = (int)(sampleRate * channels * 2 * 0.2f);
        mRecorderParam.mAudioBitRate = calcAudioBitRate;

        // 初始化录像变量信息
        mVideoFrameIndex = 0;
        mAudioFrameIndex = 0;
        mAudioTimestamp = 0;
        mInAudioFrameQueue.clear();

        mRecorder = new AvMediaRecorder();
        int ret = mRecorder.initialize(mRecorderParam);
        if (ret != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<recordingStart> initialize() error, ret=" + ret);
            mRecorder = null;
            synchronized (mVideoDataLock) {
                mCacheVideoFrame = false;
            }
            return ret;
        }
        ret = mRecorder.recordingStart();

        ALog.getInstance().d(TAG, "<recordingStart> done, ret=" + ret
            + ", width=" + videoWidth + ", height=" + videoHeight + ", rotation=" + videoRotation
            + ", channels=" + channels + ", bytesPerSmpl=" + bytesPerSample + ", sampleRate=" + sampleRate);
        return ErrCode.XOK;
    }

    /**
     * @brief 停止频道内录像
     * @return 返回错误码
     */
    public int recordingStop(final SessionCtx sessionCtx) {
        if (mRecorder != null) {
            mRecorder.recordingStop();
            mRecorder.release();
            mRecorder = null;
            ALog.getInstance().d(TAG, "<recordingStop> done");
        }
        synchronized (mVideoDataLock) {
            mCacheVideoFrame = false;
        }

        mInAudioFrameQueue.clear();
        return ErrCode.XOK;
    }

    /**
     * @brief 判断当前是否在录像
     * @return true : 当前正在录制； false: 当前不再录制
     */
    public boolean isRecording(final SessionCtx sessionCtx) {
        return (mRecorder != null);
    }

    @Override
    public AvVideoFrame onReadVideoFrame() {
        synchronized (mVideoDataLock) {
            if (mInVideoYData == null || mInVideoUData == null || mInVideoVData == null) {
                return null;
            }

            // YUV 数据格式转换成 NV12 格式
            int yDataSize = mInVideoYData.length;
            int uDataSize = mInVideoUData.length;
            int vDataSize = mInVideoVData.length;
            int yuvDataSize = yDataSize + uDataSize + vDataSize;
            byte[] yuvBuffer = new byte[yuvDataSize];
            ImageConvert.getInstance().ImgCvt_YuvToNv12(mInVideoYData, mInVideoUData, mInVideoVData,
                    mInVideoWidth, mInVideoHeight, yuvBuffer);

            AvVideoFrame videoFrame = new AvVideoFrame();
            videoFrame.mColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
            videoFrame.mDataBuffer = yuvBuffer;
            videoFrame.mFrameIndex = mVideoFrameIndex;
            videoFrame.mTimestamp = (mVideoFrameIndex * 1000L * 1000L / mRecorderParam.mFrameRate);
            videoFrame.mKeyFrame = false;
            videoFrame.mLastFrame = false;
            videoFrame.mFlags = 0;

            mVideoFrameIndex++;
            return videoFrame;
        }
    }

    @Override
    public AvAudioFrame onReadAudioFrame() {
        synchronized (mVideoDataLock) {
            AvAudioFrame audioFrame = (AvAudioFrame) mInAudioFrameQueue.dequeue();
            return audioFrame;
        }
    }

    @Override
    public void onRecorderError(AvRecorderParam recorderParam, int errCode) {
        Log.e(TAG, "<onRecorderError> errCode=" + errCode);

        if (mInitParam.mCallback != null) {
            mInitParam.mCallback.onRecordingError(null, errCode);
        }
    }



    ////////////////////////////////////////////////////////////////////////////
    ////////////////////// Rtc Channel Event Handler Methods ////////////////////
    ////////////////////////////////////////////////////////////////////////////
    void onJoinChannelSuccess(final UUID sessionId, final String channel, int uid, int elapsed)  {
        ALog.getInstance().d(TAG, "<onJoinChannelSuccess> sessionId=" + sessionId
                + ", channel=" + channel  + ", uid=" + uid + ", elapsed=" + elapsed);
        if (mRtcEngine == null) {
            return;
        }

        if (mInitParam.mCallback != null) {
            mInitParam.mCallback.onTalkingJoinDone(sessionId, channel, uid);
        }
    }

    public void onRejoinChannelSuccess(final UUID sessionId, String channel, int uid, int elapsed) {
        ALog.getInstance().d(TAG, "<onRejoinChannelSuccess> sessionId=" + sessionId
                + ", channel=" + channel  + ", uid=" + uid + ", elapsed=" + elapsed);
        if (mRtcEngine == null) {
            return;
        }

    }

    void onLeaveChannelSuccess(final UUID sessionId)  {
        ALog.getInstance().d(TAG, "<onLeaveChannelSuccess> sessionId=" + sessionId);
        if (mRtcEngine == null) {
            return;
        }

        if (mInitParam.mCallback != null) {
            mInitParam.mCallback.onTalkingLeftDone(sessionId);
        }
    }

    void onUserJoined(final UUID sessionId, int uid, int elapsed)  {
        ALog.getInstance().d(TAG, "<onUserJoined> sessionId=" + sessionId
                + ", uid=" + uid + ", elapsed=" + elapsed);
        if (mRtcEngine == null) {
            return;
        }

        if (mInitParam.mCallback != null) {
            mInitParam.mCallback.onUserOnline(sessionId, uid, elapsed);
        }
    }

    void onUserOffline(final UUID sessionId, int uid, int reason)   {
        ALog.getInstance().d(TAG, "<onUserOffline> sessionId=" + sessionId
                + ", uid=" + uid + ", reason=" + reason);
        if (mRtcEngine == null) {
            return;
        }

        if (mInitParam.mCallback != null) {
            mInitParam.mCallback.onUserOffline(sessionId, uid, reason);
        }
    }

    void onFirstLocalVideoFrame(final UUID sessionId, int width, int height, int elapsed)    {
        ALog.getInstance().d(TAG, "<onFirstLocalVideoFrame> sessionId=" + sessionId
                + ", width=" + width + ", height=" + height + ", elapsed=" + elapsed);
        if (mRtcEngine == null) {
            return;
        }
    }

    void onFirstRemoteVideoFrame(final UUID sessionId, int uid, int width, int height, int elapsed) {
        ALog.getInstance().d(TAG, "<onFirstRemoteVideoFrame> sessionId=" + sessionId
                + ", uid=" + uid + ", width=" + width + ", height=" + height + ", elapsed=" + elapsed);
        if (mRtcEngine == null) {
            return;
        }

        if (mInitParam.mCallback != null) {
            mInitParam.mCallback.onPeerFirstVideoDecoded(sessionId, uid, width, height);
        }
    }

    void onLastmileQuality(final UUID sessionId, int quality) {
//        ALog.getInstance().d(TAG, "<onLastmileQuality> sessionId=" + sessionId + ", quality=" + quality);
    }

    void onNetworkQuality(final UUID sessionId, int uid, int txQuality, int rxQuality) {
//        ALog.getInstance().d(TAG,"<onNetworkQuality> sessionId=" + sessionId
//            + ", uid=" + uid + ", txQuality=" + txQuality + ", rxQuality=" + rxQuality);
    }

    void onRtcStats(final UUID sessionId, IRtcEngineEventHandler.RtcStats stats) {
        if (mRtcEngine == null) {
            return;
        }

        if ((System.currentTimeMillis()-mRtcInitTime) < 4000) {
            if ((stats.rxVideoKBitRate <= 0) || (stats.rxAudioKBitRate <= 0)) {
                return;
            }
        }

        synchronized (mRtcStatus) {
            mRtcStatus.totalDuration = stats.totalDuration;
            mRtcStatus.txBytes = stats.txBytes;
            mRtcStatus.rxBytes = stats.rxBytes;
            mRtcStatus.txKBitRate = stats.txKBitRate;
            mRtcStatus.rxKBitRate = stats.rxKBitRate;
            mRtcStatus.txAudioBytes = stats.txAudioBytes;
            mRtcStatus.rxAudioBytes = stats.rxAudioBytes;
            mRtcStatus.txVideoBytes = stats.txVideoBytes;
            mRtcStatus.rxVideoBytes = stats.rxVideoBytes;
            mRtcStatus.txAudioKBitRate = stats.txAudioKBitRate;
            mRtcStatus.rxAudioKBitRate = stats.rxAudioKBitRate;
            mRtcStatus.txVideoKBitRate = stats.txVideoKBitRate;
            mRtcStatus.rxVideoKBitRate = stats.rxVideoKBitRate;
            mRtcStatus.txPacketLossRate = stats.txPacketLossRate;
            mRtcStatus.rxPacketLossRate = stats.rxPacketLossRate;
            mRtcStatus.lastmileDelay = stats.lastmileDelay;
            mRtcStatus.connectTimeMs = stats.connectTimeMs;
            mRtcStatus.cpuAppUsage = stats.cpuAppUsage;
            mRtcStatus.cpuTotalUsage = stats.cpuTotalUsage;
            mRtcStatus.users = stats.users;
            mRtcStatus.memoryAppUsageRatio = stats.memoryAppUsageRatio;
            mRtcStatus.memoryTotalUsageRatio = stats.memoryTotalUsageRatio;
            mRtcStatus.memoryAppUsageInKbytes = stats.memoryAppUsageInKbytes;
        }
    }

    void onLocalVideoStats(final UUID sessionId, Constants.VideoSourceType source,
                                  IRtcEngineEventHandler.LocalVideoStats stats) {
//        ALog.getInstance().d(TAG, "<onLocalVideoStats> sessionId=" + sessionId + ", stats=" + stats);
    }

    void onRemoteVideoStats(final UUID sessionId, IRtcEngineEventHandler.RemoteVideoStats stats) {
//        ALog.getInstance().d(TAG, "<onRemoteVideoStats> sessionId=" + sessionId + ", stats=" + stats);
    }

    void onSnapshotTaken(final UUID sessionId, int uid, String filePath, int width, int height, int errCode) {
        ALog.getInstance().d(TAG, "<onSnapshotTaken> sessionId=" + sessionId
                + ", uid=" + uid + ", filePath=" + filePath
                + ", width=" + width + ", height=" + height + ", errCode=" + errCode);
        if (mRtcEngine == null) {
            return;
        }

        if (mInitParam.mCallback != null) {
            mInitParam.mCallback.onSnapshotTaken(sessionId, uid, filePath, width, height, errCode);
        }
    }



    ////////////////////////////////////////////////////////////////////////////
    ////////////////////// Override AGEventHandler Methods ////////////////////
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed)
    {
        Log.d(TAG, "<onFirstRemoteVideoDecoded> uid=" + uid
                + ", width=" + width + ", height=" + height
                + ", elapsed=" + elapsed);
        if (mRtcEngine == null) {
            return;
        }
    }

    @Override
    public void onFirstRemoteVideoFrame(int uid, int width, int height, int elapsed)
    {
        ALog.getInstance().d(TAG, "<onFirstRemoteVideoFrame> uid=" + uid
                + ", width=" + width + ", height=" + height
                + ", elapsed=" + elapsed);
        if (mRtcEngine == null) {
            return;
        }

    }

    @Override
    public void onFirstLocalVideoFrame(int width, int height, int elapsed)
    {
        ALog.getInstance().d(TAG, "<onFirstLocalVideoFrame> width=" + width + ", height=" + height
                + ", elapsed=" + elapsed);
        if (mRtcEngine == null) {
            return;
        }
    }

    @Override
    public void onJoinChannelSuccess(String channel, int uid, int elapsed)
    {
        ALog.getInstance().d(TAG, "<onJoinChannelSuccess> channel=" + channel
                + ", uid=" + uid + ", elapsed=" + elapsed);
        if (mRtcEngine == null) {
            return;
        }

    }

    @Override
    public void onLeaveChannelSuccess()
    {
        Log.d(TAG, "<onLeaveChannelSuccess> ");
        if (mRtcEngine == null) {
            return;
        }

    }

    @Override
    public void onUserJoined(int uid, int elapsed)
    {
        ALog.getInstance().d(TAG, "<onUserJoined> uid=" + uid + ", elapsed=" + elapsed);
        if (mRtcEngine == null) {
            return;
        }

    }

    @Override
    public void onUserOffline(int uid, int reason)
    {
        ALog.getInstance().d(TAG, "<onUserOffline> uid=" + uid + ", reason=" + reason);
        if (mRtcEngine == null) {
            return;
        }

    }

    @Override
    public void onLastmileQuality(int quality)
    {  }

    @Override
    public void onLocalVideoStats(IRtcEngineEventHandler.LocalVideoStats stats)
    {
    }

    @Override
    public void onRtcStats(IRtcEngineEventHandler.RtcStats stats)
    {
        if (mRtcEngine == null) {
            return;
        }

        if ((System.currentTimeMillis()-mRtcInitTime) < 4000) {
            if ((stats.rxVideoKBitRate <= 0) || (stats.rxAudioKBitRate <= 0)) {
                return;
            }
        }

        synchronized (mRtcStatus) {
            mRtcStatus.totalDuration = stats.totalDuration;
            mRtcStatus.txBytes = stats.txBytes;
            mRtcStatus.rxBytes = stats.rxBytes;
            mRtcStatus.txKBitRate = stats.txKBitRate;
            mRtcStatus.rxKBitRate = stats.rxKBitRate;
            mRtcStatus.txAudioBytes = stats.txAudioBytes;
            mRtcStatus.rxAudioBytes = stats.rxAudioBytes;
            mRtcStatus.txVideoBytes = stats.txVideoBytes;
            mRtcStatus.rxVideoBytes = stats.rxVideoBytes;
            mRtcStatus.txAudioKBitRate = stats.txAudioKBitRate;
            mRtcStatus.rxAudioKBitRate = stats.rxAudioKBitRate;
            mRtcStatus.txVideoKBitRate = stats.txVideoKBitRate;
            mRtcStatus.rxVideoKBitRate = stats.rxVideoKBitRate;
            mRtcStatus.txPacketLossRate = stats.txPacketLossRate;
            mRtcStatus.rxPacketLossRate = stats.rxPacketLossRate;
            mRtcStatus.lastmileDelay = stats.lastmileDelay;
            mRtcStatus.connectTimeMs = stats.connectTimeMs;
            mRtcStatus.cpuAppUsage = stats.cpuAppUsage;
            mRtcStatus.cpuTotalUsage = stats.cpuTotalUsage;
            mRtcStatus.users = stats.users;
            mRtcStatus.memoryAppUsageRatio = stats.memoryAppUsageRatio;
            mRtcStatus.memoryTotalUsageRatio = stats.memoryTotalUsageRatio;
            mRtcStatus.memoryAppUsageInKbytes = stats.memoryAppUsageInKbytes;
        }
    }

    @Override
    public void onNetworkQuality(int uid, int txQuality, int rxQuality)
    {  }

    @Override
    public void onRemoteVideoStats(IRtcEngineEventHandler.RemoteVideoStats stats)
    {  }

    @Override
    public void onRecorderStateChanged(int state, int code)
    {  }

    @Override
    public void onRecorderInfoUpdate(Object info)
    {  }

    ////////////////////////////////////////////////////////////////////////////
    //////////////////// Override Methods of IVideoFrameObserver ///////////////
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onCaptureVideoFrame(int sourceType, VideoFrame videoFrame)   {
        return false;
    }

//    @Override
//    public boolean onScreenCaptureVideoFrame(VideoFrame videoFrame)  {
//        return false;
//    }

    @Override
    public boolean onMediaPlayerVideoFrame(VideoFrame videoFrame, int var2)  {
        return false;
    }

    @Override
    public boolean onRenderVideoFrame(String channelId, int uid, VideoFrame videoFrame)  {
        if (mRtcEngine == null) {
            return false;
        }

        boolean isCacheVideo;
        synchronized (mVideoDataLock) {
            isCacheVideo = mCacheVideoFrame;
        }
        if (isCacheVideo) {
            // 录像时缓存当前预览视频帧
            cacheInVideoFrame(videoFrame);

        } else {
            // 否则只计算可以录像的参数
            VideoFrame.Buffer videoBuffer = videoFrame.getBuffer();
            if (videoBuffer == null) {
                ALog.getInstance().e(TAG, "<onRenderVideoFrame> videoBuffer is NULL");
                return false;
            }
            int frameWidth = videoBuffer.getWidth();
            int frameHeight = videoBuffer.getHeight();
            if ((frameWidth > mMaxEncodeWidth) || (frameHeight > mMaxEncodeHeight)) {
                float scaleRateW = (float)frameWidth / (float)mMaxEncodeWidth;
                float scaleRateH = (float)frameHeight / (float)mMaxEncodeHeight;
                float scaleRate = (scaleRateW > scaleRateH) ? scaleRateW : scaleRateH;
                int scaledWidth = (int)(frameWidth / scaleRate);
                int scaleHeight = (int)(frameHeight / scaleRate);

                // 记录可录像的缩放后参数
                synchronized (mVideoDataLock) {
                    mInVideoWidth = scaledWidth;
                    mInVideoHeight = scaleHeight;
                    mInVideoRotation = videoFrame.getRotation();
                }
                
            } else {
                // 记录可录像的原始参数
                synchronized (mVideoDataLock) {
                    mInVideoWidth = frameWidth;
                    mInVideoHeight = frameHeight;
                    mInVideoRotation = videoFrame.getRotation();
                }
            }
        }

        return false;
    }

//    @Override
//    public boolean onPreEncodeScreenVideoFrame(VideoFrame videoFrame) {
//        return false;
//    }

    @Override
    public boolean onPreEncodeVideoFrame(int sourceType, VideoFrame videoFrame) {
        return false;
    }

    @Override
    public int getVideoFrameProcessMode()   {
        return PROCESS_MODE_READ_ONLY;
    }

    @Override
    public int getVideoFormatPreference()   {
        return 0;
    }

    @Override
    public boolean getRotationApplied() {
        return false;
    }

    @Override
    public boolean getMirrorApplied() {
        return false;
    }

    @Override
    public int getObservedFramePosition() {
        return 0;
    }




    ////////////////////////////////////////////////////////////////////////////
    //////////////////// Override Methods of IAudioFrameObserver ///////////////
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onRecordAudioFrame(String channelId, int type, int samplesPerChannel,
                               int bytesPerSample, int channels, int samplesPerSec,
                               ByteBuffer buffer, long renderTimeMs, int avsync_type) {

        return false;
    }

    @Override
    public boolean onEarMonitoringAudioFrame(int type, int samplesPerChannel, int bytesPerSample,
                                             int channels, int samplesPerSec, ByteBuffer buffer,
                                             long renderTimeMs, int avsync_type) {
        return false;
    }


    @Override
    public boolean onPlaybackAudioFrame(String channelId, int type, int samplesPerChannel,
                                        int bytesPerSample, int channels, int samplesPerSec,
                                        ByteBuffer buffer, long renderTimeMs, int avsync_type) {

//        ALog.getInstance().d(TAG, "<onPlaybackAudioFrame> [1] bytesPerSample=" + bytesPerSample
//                + ", channels=" + channels + ", samplesPerChannel=" + samplesPerChannel
//                + ", renderTimeMs=" + renderTimeMs);

        synchronized (mAudioDataLock) {
            mInAudioBytesPerSample = bytesPerSample;
            mInAudioChannels = channels;
            mInAudioSampleRate = samplesPerSec;
        }

        if (mRecorder != null) {
            // 读取到音频帧，送入编码器组件
            AvAudioFrame audioFrame = new AvAudioFrame();
            audioFrame.mBytesPerSample = mInAudioBytesPerSample;
            audioFrame.mChannels = channels;
            audioFrame.mSampleNumber = (samplesPerChannel*channels);
            audioFrame.mDataBuffer = byteBuffer2ByteArray(buffer);
            audioFrame.mFrameIndex = mAudioFrameIndex;
            audioFrame.mTimestamp = (mAudioTimestamp * 1000L);
            audioFrame.mKeyFrame = true;
            audioFrame.mLastFrame = false;
            audioFrame.mFlags = 0;
            mInAudioFrameQueue.inqueue(audioFrame);

            // 计算当前数据块的时长
            long dataLength = (long)audioFrame.mDataBuffer.length;
            long bytesPerSec = (long)(channels * bytesPerSample * RECORD_AUDIO_SAMPLERATE);
            long bufferDuration = (long)(dataLength * 1000 / bytesPerSec);

            // 累计数据块时长 和 音频帧数
            mAudioTimestamp += bufferDuration;
            mAudioFrameIndex++;

//            ALog.getInstance().d(TAG, "<onPlaybackAudioFrame> bytesPerSample=" + bytesPerSample
//                    + ", channels=" + channels + ", samplesPerChannel=" + samplesPerChannel
//                    + ", mDataBuffer.length=" + audioFrame.mDataBuffer.length
//                    + ", mInAudioFrameQueue.size=" + mInAudioFrameQueue.size()
//                    + ", mAudioFrameIndex=" + mAudioFrameIndex
//                    + ", renderTimeMs=" + renderTimeMs
//                    + ", bufferDuration=" + bufferDuration + ", mAudioTimestamp=" + mAudioTimestamp);
        }

        return false;
    }

    @Override
    public boolean onMixedAudioFrame(String channelId, int type, int samplesPerChannel,
                                     int bytesPerSample, int channels, int samplesPerSec,
                                     ByteBuffer buffer, long renderTimeMs, int avsync_type) {
        return false;
    }

    @Override
    public boolean onPlaybackAudioFrameBeforeMixing(
            String channelId, int userId, int type,
            int samplesPerChannel, int bytesPerSample,
            int channels, int samplesPerSec,
            ByteBuffer buffer, long renderTimeMs, int avsync_type) {
        return false;
    }

    private static final int AgoraAudioFramePositionPlayback = 1 << 0;
    private static final int AgoraAudioFramePositionRecord = 1 << 1;
    private static final int AgoraAudioFramePositionMixed = 1 << 2;
    private static final int AgoraAudioFramePositionBeforeMixing = 1 << 3;

    @Override
    public int getObservedAudioFramePosition() {
        return AgoraAudioFramePositionPlayback;
    }

    @Override
    public AudioParams getRecordAudioParams() {
        AudioParams params = new AudioParams(SET_AUD_SAMPLERATE, SET_AUD_CHANNELS,
                Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, 1024 );
        return params;
    }

    @Override
    public AudioParams getPlaybackAudioParams() {
        // 设置回放的音频格式，这里固定设置：双通道；44100采样率
        // 每次回调个 2646 个样本数据， 正好30ms
        int samplesPerCall = (RECORD_AUDIO_SAMPLERATE * RECORD_AUDIO_CHANNELS * 30) / 1000;
        AudioParams params = new AudioParams(RECORD_AUDIO_SAMPLERATE, RECORD_AUDIO_CHANNELS,
                Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, samplesPerCall );
        return params;
    }

    @Override
    public AudioParams getMixedAudioParams() {
        int samplesPerCall = (RECORD_AUDIO_SAMPLERATE * RECORD_AUDIO_CHANNELS * 30) / 1000;
        AudioParams params = new AudioParams(RECORD_AUDIO_SAMPLERATE, RECORD_AUDIO_CHANNELS,
                Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, samplesPerCall );
        return params;
    }

    @Override
    public AudioParams getEarMonitoringAudioParams() {
        int samplesPerCall = (RECORD_AUDIO_SAMPLERATE * RECORD_AUDIO_CHANNELS * 30) / 1000;
        AudioParams params = new AudioParams(RECORD_AUDIO_SAMPLERATE, RECORD_AUDIO_CHANNELS,
                Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, samplesPerCall );
        return params;
    }

    ////////////////////////////////////////////////////////////////////////////
    //////////////////////////// Internal Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 缓存订阅的视频帧数据
     * @param inVideoFrame : 订阅的视频帧
     */
    boolean cacheInVideoFrame(VideoFrame inVideoFrame) {
        long t1 = System.currentTimeMillis();
        if (inVideoFrame == null) {
            ALog.getInstance().e(TAG, "<cacheInVideoFrame> invalid param");
            return false;
        }
        VideoFrame.Buffer videoBuffer = inVideoFrame.getBuffer();
        if (videoBuffer == null) {
            ALog.getInstance().e(TAG, "<cacheInVideoFrame> videoBuffer is NULL");
            return false;
        }

        VideoFrame.Buffer scaledBuffer = null;
        VideoFrame.I420Buffer i420Buffer = null;

        int frameWidth = videoBuffer.getWidth();
        int frameHeight = videoBuffer.getHeight();
        if ((frameWidth > mMaxEncodeWidth) || (frameHeight > mMaxEncodeHeight)) {
            float scaleRateW = (float)frameWidth / (float)mMaxEncodeWidth;
            float scaleRateH = (float)frameHeight / (float)mMaxEncodeHeight;
            float scaleRate = (scaleRateW > scaleRateH) ? scaleRateW : scaleRateH;
            int scaledWidth = (int)(frameWidth / scaleRate);
            int scaleHeight = (int)(frameHeight / scaleRate);
            scaledBuffer = videoBuffer.cropAndScale(0, 0, frameWidth, frameHeight, scaledWidth, scaleHeight);
            i420Buffer = scaledBuffer.toI420();
//            ALog.getInstance().d(TAG, "<cacheInVideoFrame> scale frame, frameWidth=" + frameWidth
//                        + ", frameHeight=" + frameHeight + ", scaledWidth=" + scaledWidth + ", scaleHeight=" + scaleHeight);
        } else {
            i420Buffer = videoBuffer.toI420();
        }
        if (i420Buffer == null) {
            ALog.getInstance().e(TAG, "<cacheInVideoFrame> i420Buffer is NULL");
            if (scaledBuffer != null) {
                scaledBuffer.release();
            }
            return false;
        }


        ByteBuffer yBuffer = i420Buffer.getDataY();
        ByteBuffer uBuffer = i420Buffer.getDataU();
        ByteBuffer vBuffer = i420Buffer.getDataV();
        int yStride = i420Buffer.getStrideY();
        int uStride = i420Buffer.getStrideU();
        int vStride = i420Buffer.getStrideV();
        int width = i420Buffer.getWidth();
        int height = i420Buffer.getHeight();
        int chromaWidth = (width + 1) / 2;
        int chromaHeight = (height + 1) / 2;
        int yDataSize = width * height;
        int uDataSize = chromaWidth * chromaHeight;
        int vDataSize = uDataSize;
        byte[] yBytes = new byte[yDataSize];
        byte[] uBytes = new byte[uDataSize];
        byte[] vBytes = new byte[vDataSize];
        ImageConvert.getInstance().ImgCvt_YuvToI420(yBuffer, uBuffer, vBuffer, width, height,
                yStride, uStride, vStride, yBytes, uBytes, vBytes);
        
        // 数据拷贝和赋值需要加锁
        synchronized (mVideoDataLock) {
            mInVideoWidth = i420Buffer.getWidth();
            mInVideoHeight = i420Buffer.getHeight();
            mInVideoRotation = inVideoFrame.getRotation();

            if ((mInVideoYData == null) || (mInVideoYData.length != yDataSize)) {
                mInVideoYData = new byte[yDataSize];
            }
            System.arraycopy(yBytes, 0, mInVideoYData, 0, yDataSize);

            if ((mInVideoUData == null) || (mInVideoUData.length != uDataSize)) {
                mInVideoUData = new byte[uDataSize];
            }
            System.arraycopy(uBytes, 0, mInVideoUData, 0, uDataSize);

            if ((mInVideoVData == null) || (mInVideoVData.length != vDataSize)) {
                mInVideoVData = new byte[vDataSize];
            }
            System.arraycopy(vBytes, 0, mInVideoVData, 0, vDataSize);
        }
        
        i420Buffer.release();
        if (scaledBuffer != null) {
            scaledBuffer.release();
        }

        long t2 = System.currentTimeMillis();
        //ALog.getInstance().e(TAG, "<cacheInVideoFrame> done, costTime=" + (t2-t1) + " ms");
        return true;
    }

    /**
     * @brief ByteBuffer 转换到字节数组
     * @param buffer
     * @return
     */
    public byte[] byteBuffer2ByteArray(ByteBuffer buffer) {
        //重置 limit 和postion 值，切换为读模式
//        buffer.flip();

        //获取buffer中有效大小
        int len = buffer.remaining();

        byte [] bytes = new byte[len];
        buffer.get(bytes, 0, len);

        return bytes;
    }


     public static class RtcVideoBitrateCfg {
        public int mWidth;
        public int mHeight;
        public int mFrameRate;
        public int mBaseBitrate;
        public int mLiveBitrate;

        public RtcVideoBitrateCfg(int width, int height, int frameRate,
                                  int baseBitrate, int liveBitrate) {
            mWidth = width;
            mHeight = height;
            mFrameRate = frameRate;
            mBaseBitrate = baseBitrate;
            mLiveBitrate = liveBitrate;
        }

         @Override
         public String toString() {
             String infoText = "{ mWidth=" + mWidth + ", mHeight=" + mHeight
                     + ", mFrameRate=" + mFrameRate + ", mBaseBitrate=" + mBaseBitrate
                     + ", mLiveBitrate=" + mLiveBitrate  + " }";
             return infoText;
         }
    }

    final static RtcVideoBitrateCfg[] mRtcVideoBitrateArray = {
        new RtcVideoBitrateCfg(160, 120, 15, 65, 130),
        new RtcVideoBitrateCfg(120, 120, 15, 50 , 100),
        new RtcVideoBitrateCfg(320, 180, 15, 140, 280),
        new RtcVideoBitrateCfg(180, 180, 15, 100, 200),
        new RtcVideoBitrateCfg(240, 180, 15, 120, 240),
        new RtcVideoBitrateCfg(320, 240, 15, 200, 400),
        new RtcVideoBitrateCfg(240, 240, 15, 140, 280),
        new RtcVideoBitrateCfg(424, 240, 15, 220, 440),
        new RtcVideoBitrateCfg(640, 360, 15, 400, 800),
        new RtcVideoBitrateCfg(360, 360, 15, 260, 520),
        new RtcVideoBitrateCfg(640, 360, 30, 600, 1200),
        new RtcVideoBitrateCfg(360, 360, 30, 400, 800),
        new RtcVideoBitrateCfg(480, 360, 15, 320, 640),
        new RtcVideoBitrateCfg(480, 360, 30, 490, 980),
        new RtcVideoBitrateCfg(640, 480, 15, 500, 1000),
        new RtcVideoBitrateCfg(480, 480, 15, 400, 800),
        new RtcVideoBitrateCfg(640, 480, 30, 750, 1500),
        new RtcVideoBitrateCfg(480, 480, 30, 600, 1200),
        new RtcVideoBitrateCfg(848, 480, 15, 610, 1220),
        new RtcVideoBitrateCfg(848, 480, 30, 930, 1860),
        new RtcVideoBitrateCfg(640, 480, 10, 400, 800),
        new RtcVideoBitrateCfg(1280, 720, 15, 1130, 2260),
        new RtcVideoBitrateCfg(1280, 720, 30, 1710, 3420),
        new RtcVideoBitrateCfg(960, 720, 15, 910, 1820),
        new RtcVideoBitrateCfg(960, 720, 30, 1380, 2760),
        new RtcVideoBitrateCfg(1920, 1080, 15, 2080, 4160),
        new RtcVideoBitrateCfg(1920, 1080, 30, 3150, 6300),
        new RtcVideoBitrateCfg(1920, 1080, 60, 4780, 6500),
        new RtcVideoBitrateCfg(2560, 1440, 30, 4850, 6500),
        new RtcVideoBitrateCfg(2560, 1440, 60, 6500, 6500),
        new RtcVideoBitrateCfg(3840, 2160, 30, 6500, 6500),
        new RtcVideoBitrateCfg(3840, 2160, 60, 6500, 6500)
    };

    /**
     * @brief 根据视频帧宽高和帧率，找到最接近的 RTC 视频码率
     *        帧率相同，宽高面积正好大于指定宽高面积的
     */
    int calcVideoBitrate(int width, int height, int frameRate) {
        int inArea = width * height;
        int bitrate;
        int i;

        for (i = 0; i < mRtcVideoBitrateArray.length; i++) {
            RtcVideoBitrateCfg bitrateCfg = mRtcVideoBitrateArray[i];
            if (frameRate != bitrateCfg.mFrameRate) {
                continue;
            }

            if ((bitrateCfg.mWidth * bitrateCfg.mHeight) > inArea) {
                bitrate = bitrateCfg.mBaseBitrate * 1024;
                ALog.getInstance().d(TAG, "<calcVideoBitrate> found config="
                            + bitrateCfg.toString() + ", bitrate=" + bitrate);
                return bitrate;
            }
        }

        bitrate = (int)(width * height * frameRate * 0.15f);
        ALog.getInstance().d(TAG, "<calcVideoBitrate> calculated, width=" + width
                    + ", height=" + height + ", frameRate=" + frameRate
                    + ", bitrate=" + bitrate);
        return bitrate;
    }

    void ThreadSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptExp) {
            interruptExp.printStackTrace();
        }
    }
}