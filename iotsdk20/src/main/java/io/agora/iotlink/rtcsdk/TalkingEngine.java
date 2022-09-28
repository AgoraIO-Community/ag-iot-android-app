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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Environment;
import android.preference.PreferenceManager;

import android.util.Log;
import android.view.SurfaceView;

import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.utils.ImageConvert;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Random;

import io.agora.base.VideoFrame;
import io.agora.base.internal.CalledByNative;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcConnection;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineEx;
import io.agora.rtc2.video.EncodedVideoFrameInfo;
//import io.agora.rtc2.video.IVideoEncodedImageReceiver;
import io.agora.rtc2.video.IVideoFrameObserver;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;



public class TalkingEngine implements AGEventHandler, IVideoFrameObserver {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 通话引擎回调接口
     */
    public static interface ICallback {

        /**
         * @brief 本地加入频道成功
         */
        default void onTalkingJoinDone(String channel, int localUid) { }

        /**
         * @brief 本地离开频道成功
         */
        default void onTalkingLeftDone() {  }


        /**
         * @brief 通话对端RTC上线
         */
        default void onTalkingPeerJoined(int localUid, int peerUid) {  }

        /**
         * @brief 通话对端RTC下线
         */
        default void onTalkingPeerLeft(int localUid, int peerUid) {  }

        /**
         * @brief 对端首帧出图
         */
        default void onPeerFirstVideoDecoded(int peerUid, int videoWidth, int videoHeight) { }


        /**
         * @brief 用户上线
         */
        default void onUserOnline(int uid) {  }

        /**
         * @brief 用户下线
         */
        default void onUserOffline(int uid) {  }
    }


    ///////////////////////////////////////////////////////////////////////////
    ///////////////////// Constant Definition /////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private final String TAG = "IOTSDK/TalkingEngine";
    private static final int WAIT_OPT_TIMEOUT = 3000;

    /*
     * @brief 通话引擎初始化参数
     */
    public class InitParam {
        public Context mContext;
        public String mAppId;
        public ICallback mCallback;
        public boolean mPublishAudio = true;        ///< 通话时是否推流本地音频
        public boolean mPublishVideo = true;        ///< 通话时是否推流本地视频
        public boolean mSubscribeAudio = true;      ///< 通话时是否订阅对端音频
        public boolean mSubscribeVideo = true;      ///< 通话时是否订阅对端视频
    }



    ///////////////////////////////////////////////////////////////////////////
    ///////////////////// Variable Definition /////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private final Object mDumpVideoEvent = new Object();
    private InitParam mInitParam;           ///< TalkingEngine初始化参数
    private EngineConfig mRtcEngCfg;        ///< RtcEngine相关配置参数
    private MyEngineEventHandler mRtcEngEventHandler;   ///< RtcEngine事件处理器
    private RtcEngineEx mRtcEngine;         ///< RtcEngine实例对象
    private int mLocalUid = 0;              ///< 本地端加入频道时的Uid
    private int mPeerUid = 0;               ///< 对端通话的Uid

    private ICallkitMgr.RtcNetworkStatus mRtcStatus = new ICallkitMgr.RtcNetworkStatus();
    private volatile boolean mDumpVideoFrame = false;
    private volatile Bitmap mDumpBitmap = null;


    ///////////////////////////////////////////////////////////////////////////
    //////////////////////// Public Methods ///////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    public synchronized boolean initialize(InitParam initParam) {
        mInitParam = initParam;

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
            mRtcEngine = (RtcEngineEx) RtcEngine.create(mInitParam.mContext, mInitParam.mAppId,
                    mRtcEngEventHandler.mRtcEventHandler);
            ALog.getInstance().d(TAG, "<initialize> mAppId=" + mInitParam.mAppId);
        } catch (Exception e) {
            e.printStackTrace();
            ALog.getInstance().e(TAG, "<initialize> " + Log.getStackTraceString(e));
            return false;
        }
        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        mRtcEngine.enableVideo();  // 启动音视频采集和推流处理
        mRtcEngine.enableAudio();
        mRtcEngine.muteLocalVideoStream(!mInitParam.mPublishVideo);
        mRtcEngine.muteLocalAudioStream(!mInitParam.mPublishAudio);

        String log_file_path = Environment.getExternalStorageDirectory()
                + File.separator + mInitParam.mContext.getPackageName() + "/log/agora-rtc.log";
        mRtcEngine.setLogFile(log_file_path);
        // Warning: only enable dual stream mode if there will be more than one broadcaster in the channel
        mRtcEngine.enableDualStreamMode(false);

        // 设置视频编码参数
        mRtcEngine.setVideoEncoderConfiguration(
                new VideoEncoderConfiguration(mRtcEngCfg.mVideoDimension,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE));

        // 设置广播模式
        mRtcEngine.setClientRole(mRtcEngCfg.mClientRole);

        // 设置私参：音频默认G722 编码
        String param = "{\"che.audio.custom_payload_type\":9}";
        int ret = mRtcEngine.setParameters(param);

        mRtcEngine.registerVideoFrameObserver(this);

        ALog.getInstance().d(TAG, "<initialize> done");
        return true;
    }

    public synchronized void release()
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

    public synchronized int setParameters(String param) {
        int ret = mRtcEngine.setParameters(param);
        ALog.getInstance().d(TAG, "<setParameter> param=" + param + " , ret=" + ret);
        return ret;
    }

    public synchronized int getVideoWidth() {
        return mRtcEngCfg.mVideoDimension.width;
    }

    public synchronized int getVideoHeight() {
        return mRtcEngCfg.mVideoDimension.height;
    }

    public synchronized int getFrameRate() {
        return 15;
    }

    public synchronized int getBitrate() {
        return 1000;
    }


    public synchronized ICallkitMgr.RtcNetworkStatus getNetworkStatus() {
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

    public synchronized boolean joinChannel(String channel, String token,
                                            int localUid   ) {
        ALog.getInstance().d(TAG, "<joinChannel> Enter, channel=" + channel
                + ", token=" + token + ", localUid=" + localUid);
        mLocalUid = localUid;

        // 加入频道
        int ret = mRtcEngine.joinChannel(token, channel, "AgoraCallKit", mLocalUid);
        if (ret != Constants.ERR_OK) {
            ALog.getInstance().e(TAG, "<joinChannel> Exit with error, ret=" + ret);
            return false;
        }

        ret = mRtcEngine.muteLocalVideoStream(!mInitParam.mPublishVideo);
        if (ret != Constants.ERR_OK) {
            ALog.getInstance().e(TAG, "<joinChannel> muteLocalVideoStream() error, ret=" + ret);
        }

        ret = mRtcEngine.muteLocalAudioStream(!mInitParam.mPublishAudio);
        if (ret != Constants.ERR_OK) {
            ALog.getInstance().e(TAG, "<joinChannel> muteLocalAudioStream() error, ret=" + ret);
        }

        ALog.getInstance().d(TAG, "<joinChannel> Exit");
        return true;
    }

    public synchronized boolean leaveChannel()
    {
        if (mLocalUid == 0 && mPeerUid == 0) {   // 已经离开频道了
            return true;
        }
        ALog.getInstance().d(TAG, "<leaveChannel> Enter");

        // 退出频道
        mLocalUid = 0;
        mPeerUid = 0;
        int ret = mRtcEngine.leaveChannel();
        if (ret != Constants.ERR_OK) {
            ALog.getInstance().e(TAG, "<leaveChannel> Exit with error, ret=" + ret);
            return false;
        }

        ALog.getInstance().d(TAG, "<leaveChannel> Exit");
        return true;
    }

    public synchronized boolean isInChannel()
    {
        return (mLocalUid != 0);
    }

    public synchronized boolean setLocalVideoView(SurfaceView localView, int localUid)
    {
        if (localView != null) {
            VideoCanvas videoCanvas = new VideoCanvas(localView, VideoCanvas.RENDER_MODE_FIT, localUid);
            mRtcEngine.setupLocalVideo(videoCanvas);
            mRtcEngine.startPreview();
            ALog.getInstance().d(TAG, "<setLocalVideoView> localView=" + localView + ", localUid=" + localUid);

        } else {
            mRtcEngine.stopPreview();
            ALog.getInstance().d(TAG, "<setLocalVideoView> stop preview");
        }

        return true;
    }

    public synchronized void setPeerUid(int peerUid) {
        mPeerUid = peerUid;
        ALog.getInstance().d(TAG, "<setPeerUid> peerUid=" + peerUid);
    }

    public synchronized boolean setRemoteVideoView(SurfaceView remoteView)
    {
        VideoCanvas videoCanvas = new VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_ADAPTIVE, mPeerUid);
        int ret = mRtcEngine.setupRemoteVideo(videoCanvas);
        ALog.getInstance().d(TAG, "<setRemoteVideoView> remoteView=" + remoteView
                + ", mPeerUid=" + mPeerUid + ", ret=" + ret);
        return true;
    }


    public synchronized boolean muteLocalVideoStream(boolean mute) {
        int ret = mRtcEngine.muteLocalVideoStream(mute);
        ALog.getInstance().d(TAG, "<muteLocalVideoStream> mute=" + mute + ", ret=" + ret);
        return (ret == Constants.ERR_OK);
    }

    public synchronized boolean muteLocalAudioStream(boolean mute) {
        int ret = mRtcEngine.muteLocalAudioStream(mute);
        ALog.getInstance().d(TAG, "<muteLocalAudioStream> mute=" + mute + ", ret=" + ret);
        return (ret == Constants.ERR_OK);
    }

    public synchronized boolean mutePeerVideoStream(boolean mute) {
        int ret = mRtcEngine.muteAllRemoteVideoStreams(mute);
        ALog.getInstance().d(TAG, "<mutePeerVideoStream> mute=" + mute + ", ret=" + ret);
        return (ret == Constants.ERR_OK);
    }

    public synchronized boolean mutePeerAudioStream(boolean mute) {
        int ret = mRtcEngine.muteAllRemoteAudioStreams(mute);
        ALog.getInstance().d(TAG, "<mutePeerAudioStream> mute=" + mute + ", ret=" + ret);
        return (ret == Constants.ERR_OK);
    }

    public synchronized boolean setAudioEffect(int voice_changer) {
        int ret = mRtcEngine.setAudioEffectPreset(voice_changer);
        ALog.getInstance().d(TAG, "<setAudioEffect> voice_changer=" + voice_changer + ", ret=" + ret);
        return (ret == Constants.ERR_OK);
    }

    public synchronized Bitmap capturePeerVideoFrame()
    {
        synchronized (mDumpVideoEvent) {
            mDumpVideoFrame = true;
            mDumpBitmap = null;
        }

        synchronized (mDumpVideoEvent) {
            try {
                mDumpVideoEvent.wait(WAIT_OPT_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
                ALog.getInstance().e(TAG, "<release> exception=" + e.getMessage());
            }
        }


        synchronized (mDumpVideoEvent) {
            mDumpVideoFrame = false;
            return mDumpBitmap;
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
    }

    @Override
    public void onFirstRemoteVideoFrame(int uid, int width, int height, int elapsed)
    {
        ALog.getInstance().d(TAG, "<onFirstRemoteVideoFrame> uid=" + uid
                + ", width=" + width + ", height=" + height
                + ", elapsed=" + elapsed);

        if (uid == mPeerUid) {  // 对端首帧出图
            if (mInitParam.mCallback != null) {
                mInitParam.mCallback.onPeerFirstVideoDecoded(mPeerUid, width, height);
            }
        }
    }

    @Override
    public void onFirstLocalVideoFrame(int width, int height, int elapsed)
    {
        ALog.getInstance().d(TAG, "<onFirstLocalVideoFrame> width=" + width + ", height=" + height
                + ", elapsed=" + elapsed);
    }

    @Override
    public void onJoinChannelSuccess(String channel, int uid, int elapsed)
    {
        ALog.getInstance().d(TAG, "<onJoinChannelSuccess> channel=" + channel
                + ", uid=" + uid + ", elapsed=" + elapsed);

        if (mInitParam.mCallback != null) {
            mInitParam.mCallback.onTalkingJoinDone(channel, uid);
        }
    }

    @Override
    public void onLeaveChannelSuccess()
    {
        Log.d(TAG, "<onLeaveChannelSuccess> ");

        if (mInitParam.mCallback != null) {
            mInitParam.mCallback.onTalkingLeftDone();
        }
    }

    @Override
    public void onUserJoined(int uid, int elapsed)
    {
        ALog.getInstance().d(TAG, "<onUserJoined> uid=" + uid + ", elapsed=" + elapsed);

        if (uid == mPeerUid) {  // 对端加入频道
            if (mInitParam.mCallback != null) {
                mInitParam.mCallback.onTalkingPeerJoined(mLocalUid, mPeerUid);
            }
        }

        if (mInitParam.mCallback != null) {
            mInitParam.mCallback.onUserOnline(uid);
        }
    }

    @Override
    public void onUserOffline(int uid, int reason)
    {
        ALog.getInstance().d(TAG, "<onUserOffline> uid=" + uid + ", reason=" + reason);

        if (uid == mPeerUid) {
            if (mInitParam.mCallback != null) {
                mInitParam.mCallback.onTalkingPeerLeft(mLocalUid, mPeerUid);
            }
        }

        if (mInitParam.mCallback != null) {
            mInitParam.mCallback.onUserOffline(uid);
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
    //////////////////// Override IVideoFrameObserver Methods //////////////////
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onCaptureVideoFrame(VideoFrame videoFrame)   {
        return false;
    }

    @Override
    public boolean onScreenCaptureVideoFrame(VideoFrame videoFrame)  {
        return false;
    }

    @Override
    public boolean onMediaPlayerVideoFrame(VideoFrame videoFrame, int var2)  {
        return false;
    }

    @Override
    public boolean onRenderVideoFrame(String channelId, int uid, VideoFrame videoFrame)  {

        synchronized (mDumpVideoEvent) {
            if (!mDumpVideoFrame) {
                return false;
            }
        }

        long t1 = System.currentTimeMillis();
        int rotation = videoFrame.getRotation();
        VideoFrame.Buffer buffer = videoFrame.getBuffer();
        VideoFrame.I420Buffer I420Buffer = buffer.toI420();
        int width = buffer.getWidth();
        int height = buffer.getHeight();
        Bitmap bmp = I420ToBitmap(I420Buffer);
        Bitmap dumpBmp = bmp;
        if (rotation > 0) {
            dumpBmp = ImageConvert.rotateBmp(bmp, rotation);
        }
        long t2 = System.currentTimeMillis();

        ALog.getInstance().d(TAG, "<onRenderVideoFrame> channelId=" + channelId + ", uid=" + uid
                + ", width=" + width + ", heigh=" + height + "rotate=" + rotation
                + ", costTime=" + (t2-t1) );

        synchronized (mDumpVideoEvent) {
            mDumpBitmap = dumpBmp;
            mDumpVideoFrame = false;
            mDumpVideoEvent.notify();
        }
        return false;
    }

    @Override
    public boolean onPreEncodeScreenVideoFrame(VideoFrame videoFrame) {
        return false;
    }

    @Override
    public boolean onPreEncodeVideoFrame(VideoFrame videoFrame) {
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
    //////////////////////////// Internal Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    /*
     * @brief 将I420数据转换成Bitmap对象
     * @
     */
    Bitmap I420ToBitmap(VideoFrame.I420Buffer I420Buffer) {

        ByteBuffer yBuffer = I420Buffer.getDataY();
        ByteBuffer uBuffer = I420Buffer.getDataU();
        ByteBuffer vBuffer = I420Buffer.getDataV();
        int yStride = I420Buffer.getStrideY();
        int uStride = I420Buffer.getStrideU();
        int vStride = I420Buffer.getStrideV();
        int width = I420Buffer.getWidth();
        int height = I420Buffer.getHeight();

        byte[] yBytes = new byte[yBuffer.capacity()];
        yBuffer.position(0);
        yBuffer.get(yBytes);

        byte[] uBytes = new byte[uBuffer.capacity()];
        uBuffer.position(0);
        uBuffer.get(uBytes);

        byte[] vBytes = new byte[vBuffer.capacity()];
        vBuffer.position(0);
        vBuffer.get(vBytes);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int ret = ImageConvert.getInstance().I420ToRgba(yBytes, uBytes, vBytes, width, height, bmp);
        if (ret != 0) {
            bmp.recycle();
            return null;
        }

        return bmp;
    }


}