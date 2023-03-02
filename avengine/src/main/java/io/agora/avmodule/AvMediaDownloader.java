package io.agora.avmodule;


import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * @brief 云录视频下载转换器
 *        音视频流来源于加密的网络ts流，转码成本地文件
 */
public class AvMediaDownloader extends AvCompBase
        implements IAvCompDecoder.IAvCompDecoderCallback, IAvCompEncoder.IAvCompEncoderCallback  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////
    /////////////////////////// Constant Definition ////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "AVMODULE/MediaDnloader";
    private static final String COMP_NAME = "MediaDnloader";

    private static final String DNLOAD_VIDEO_CODEC = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final String DNLOAD_AUDIO_CODEC = MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int AUDIO_SAMPLE_FMT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AUDIO_CHANNELS = 2;
    private static final int AUDIO_SAMPLE_RATE = 44100;


    /**
     * @brief 定义下载状态
     */
    public static final int DOWNLOAD_STATE_INVALID = 0x0000;      ///< 当前下载还未初始化
    public static final int DOWNLOAD_STATE_PREPARING = 0x0001;    ///< 初始化成功，正在请求云媒体文件信息
    public static final int DOWNLOAD_STATE_ONGOING = 0x0002;      ///< 正常下载转换过程中
    public static final int DOWNLOAD_STATE_PAUSED = 0x0003;       ///< 暂停
    public static final int DOWNLOAD_STATE_DONE = 0x0004;         ///< 转换完成状态
    public static final int DOWNLOAD_STATE_ERROR = 0x0005;        ///< 错误状态,不能再继续

    //
    // The mesage Id
    //
    protected static final int MSG_ID_DOWNLOAD = 0x1001;          ///< 下载转码处理






    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private final Object mDataLock = new Object();    ///< 同步访问锁,类中相应变量需要进行加锁处理
    private volatile int mState = DOWNLOAD_STATE_INVALID;
    private AvDownloaderParam mInitParam;             ///< 初始化参数
    private IAvCompDecoder mAvDecoder;      ///< 解码器组件
    private IAvCompEncoder mAvEncoder;      ///< 编码器组件
    private AvMediaInfo mMediaInfo;         ///< 原始媒体文件信息


    private int mDecErrCount = 0;           ///< 解码错误统计

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 初始化录像，准备输出媒体文件和编码
     *        初始化成功后，状态机切换为 DOWNLOAD_STATE_IDLE
     * @param initParam : 录像的初始化参数
     * @return 返回错误代码，0：表示成功打开；其他值：表示打开文件失败
     */
    public int initialize(AvDownloaderParam initParam) {
        int ret;
        mInitParam = initParam;
        synchronized (mDataLock) {
            mMediaInfo = null;
        }

        // 创建解码器组件
         ret = decodeCompCreate();
        if (ret != ErrCode.XERR_NONE) {
            Log.e(TAG, "<initialize> fail to create decoder component");
            release();
            return ret;
        }

        // 启动组件线程
        ret = runStart(COMP_NAME);
        if (ret != ErrCode.XERR_NONE) {
            Log.e(TAG, "<initialize> fail to start component");
            release();
            return ret;
        }

        // 设置状态机为准备中
        setState(DOWNLOAD_STATE_PREPARING);

        // 发送处理消息
        mWorkHandler.removeMessages(MSG_ID_DOWNLOAD);
        mWorkHandler.sendEmptyMessage(MSG_ID_DOWNLOAD);

        Log.d(TAG, "<initialize> done");
        return ErrCode.XOK;
    }


    /**
     * @brief 关闭录像，释放所有的编解码器，关闭输入输出文件
     *        释放完成后，状态机切换为 DOWNLOAD_STATE_INVALID
     * @return 返回错误代码
     */
    public int release() {
        Log.d(TAG, "<release> [BEGIN] mState=" + mState);

        // 停止组件线程
        runStop();

        // 释放解码器组件
        decodeCompDestroy();

        // 释放编码器组件
        encodeCompDestroy();

        // 设置无效状态机
        setState(DOWNLOAD_STATE_INVALID);

        synchronized (mDataLock) {
            mMediaInfo = null;
        }
        Log.d(TAG, "<release> [END] mState=" + mState);
        return ErrCode.XOK;
    }

    /**
     * @brief 获取当前录像的状态
     * @return 返回状态机
     */
    public int getState() {
        synchronized (mDataLock) {
            return mState;
        }
    }

    /*
     * @brief 设置新的状态机，仅供内部调用
     * @param newState ：新状态机
     */
    private void setState(int newState) {
        synchronized (mDataLock) {
            mState = newState;
        }
    }


    /**
     * @brief 暂停当前处理
     * @return 错误代码
     */
    public int downloadPause() {
        if (getState() != DOWNLOAD_STATE_ONGOING) {
            Log.e(TAG, "<downloadPause> [ERROR] bad state, mState=" + mState);
            return ErrCode.XERR_BAD_STATE;
        }

        if (mAvDecoder != null) {
            mAvDecoder.pause();
        }
        if (mAvEncoder != null) {
            mAvEncoder.pause();
        }

        setState(DOWNLOAD_STATE_PAUSED);
        Log.d(TAG, "<downloadPause> done");
        return ErrCode.XOK;
    }

    /**
     * @brief 恢复当前处理
     * @return 错误代码
     */
    public int downloadResume() {
        if (getState() != DOWNLOAD_STATE_PAUSED) {
            Log.e(TAG, "<downloadResume> [ERROR] bad state, mState=" + mState);
            return ErrCode.XERR_BAD_STATE;
        }

        if (mAvDecoder != null) {
            mAvDecoder.resume();
        }
        if (mAvEncoder != null) {
            mAvEncoder.resume();
        }

        setState(DOWNLOAD_STATE_ONGOING);
        mWorkHandler.removeMessages(MSG_ID_DOWNLOAD);
        mWorkHandler.sendEmptyMessage(MSG_ID_DOWNLOAD);
        Log.d(TAG, "<downloadResume> done");
        return ErrCode.XOK;
    }


    /**
     * @brief 获取当前视频编码进度
     * @return 当前视频帧时间戳
     */
    public long getVideoTimestamp() {
        int state = getState();
        if (state == DOWNLOAD_STATE_INVALID || state == DOWNLOAD_STATE_PREPARING) {
            return 0;
        }
        if (mAvEncoder == null) {
            return 0;
        }

        return mAvEncoder.getVideoTimestamp();
    }

    /**
     * @brief 获取当前音频编码进度
     * @return 当前视频帧时间戳
     */
    public long getAudioTimestamp() {
        int state = getState();
        if (state == DOWNLOAD_STATE_INVALID || state == DOWNLOAD_STATE_PREPARING) {
            return 0;
        }
        if (mAvEncoder == null) {
            return 0;
        }

        return mAvEncoder.getAudioTimestamp();
    }


    ///////////////////////////////////////////////////////////////////////////////////
    ///////////////////////// Override AvCompBase Methods /////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void removeAllMessages() {
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(MSG_ID_DOWNLOAD);
            Log.d(TAG, "<removeAllMessages> done");
        }
    }

    @Override
    protected void processWorkMessage(Message msg) {
        switch (msg.what) {
            case MSG_ID_DOWNLOAD:
                doMessageDownloading();
                break;
        }
    }

    /**
     * @brief 下载转码处理
     */
    void doMessageDownloading() {
        int state = getState();
        int ret;

        switch (state) {
            case DOWNLOAD_STATE_PREPARING: {    // 正在请求媒体信息
                //Log.d(TAG, "<doMessageDownloading> state=DOWNLOAD_STATE_PREPARING");
                AvMediaInfo mediaInfo;
                synchronized (mDataLock) {
                    mediaInfo = mMediaInfo;
                 }

                 if (mediaInfo == null) {   // 没有获取到媒体信息，继续50ms轮询一次
                     mWorkHandler.removeMessages(MSG_ID_DOWNLOAD);
                     mWorkHandler.sendEmptyMessageDelayed(MSG_ID_DOWNLOAD, 50);
                     return;
                 }

                // 创建编码器
                ret = encodeCompCreate(mediaInfo.mVideoWidth, mediaInfo.mVideoHeight, mediaInfo.mFrameRate);
                if (ret != ErrCode.XOK) {
                    Log.e(TAG, "<doMessageDownloading> fail to create encode comp");
                    if (mInitParam.mCallback != null) {
                        mInitParam.mCallback.onDownloaderError(mInitParam, ErrCode.XERR_CODEC_OPEN);
                    }
                    setState(DOWNLOAD_STATE_ERROR);  // 进入错误状态，并且不再继续
                    return;
                }


                setState(DOWNLOAD_STATE_ONGOING);  // 切换到正常处理流程
                if (mInitParam.mCallback != null) {
                    mInitParam.mCallback.onDownloaderPrepared(mInitParam, mediaInfo);
                }

                 mWorkHandler.removeMessages(MSG_ID_DOWNLOAD);
                 mWorkHandler.sendEmptyMessage(MSG_ID_DOWNLOAD);
            } break;

            case DOWNLOAD_STATE_ONGOING: {  // 正常解码和编码处理流程
                //Log.d(TAG, "<doMessageDownloading> state=DOWNLOAD_STATE_ONGOING");
                processInVideoFrame();
                processInAudioFrame();
                mWorkHandler.removeMessages(MSG_ID_DOWNLOAD);
                mWorkHandler.sendEmptyMessage(MSG_ID_DOWNLOAD);

            } break;

            default: {  // 其他状态下不做任何处理
                Log.d(TAG, "<doMessageDownloading> state=" + state);
            } break;
        }
    }


    /**
     * @brief 在这里处理接收到的解码后的视频帧，帧数据存放在 mInVideoQueue 队列中
     */
    void processInVideoFrame() {
        // 从解码器中提取视频帧
        AvVideoFrame videoFrame = mAvDecoder.dequeueVideoFrame();
        if (videoFrame == null) {
            return;
        }
        Log.d(TAG, "<processInVideoFrame> timestamp=" + videoFrame.mTimestamp
                + ", keyFrame=" + videoFrame.mKeyFrame
                + ", lastFrame=" + videoFrame.mLastFrame);

        // 送入编码器进行编码
        mAvEncoder.inputVideoFrame(videoFrame);
    }

    /**
     * @brief 在这里处理接收到的解码后的音频帧，帧数据存放在 mInAudioQueue 队列中
     */
    void processInAudioFrame() {
        ArrayList<AvAudioFrame> outFrameList = new ArrayList<>();

        for (;;) {
            // 从解码器中提取音频帧
            AvAudioFrame audioFrame = mAvDecoder.dequeueAudioFrame();
            if (audioFrame == null) {
                break;
            }
            Log.d(TAG, "<processInAudioFrame> timestamp=" + audioFrame.mTimestamp
                    + ", keyFrame=" + audioFrame.mKeyFrame
                    + ", lastFrame=" + audioFrame.mLastFrame);

            // 送入编码器进行编码
            mAvEncoder.inputAudioFrame(audioFrame);
        }
    }



    ///////////////////////////////////////////////////////////////////////////////////
    /////////////////// Override IAvCompDecoderCallback Methods ///////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onMediaInfoDecoded(AvMediaInfo mediaInfo) {
        Log.d(TAG, "<onMediaInfoDecoded> mediaInfo=" + mediaInfo.toString());
        synchronized (mDataLock) {
            if (mState == DOWNLOAD_STATE_PREPARING) {
                mMediaInfo = mediaInfo;
            }
        }
    }

    private int mDumpFrameIdx = 0;

    @Override
    public void onVideoFrameDecoded(AvMediaInfo mediaInfo, AvVideoFrame videoFrame) {
//        Log.d(TAG, "<onVideoFrameDecoded> timestamp=" + videoFrame.mTimestamp
//            + ", keyFrame=" + videoFrame.mKeyFrame
//            + ", lastFrame=" + videoFrame.mLastFrame );

//        String dumpFilePath = String.format("/sdcard/zdump/%dx%d_%06d.I420",
//            videoFrame.mWidth, videoFrame.mHeight, mDumpFrameIdx);
//        AvUtility.saveBytesToFile(videoFrame.mDataBuffer, dumpFilePath);
//        mDumpFrameIdx++;
    }

    @Override
    public void onAudioFrameDecoded(AvMediaInfo mediaInfo, AvAudioFrame audioFrame) {
//        Log.d(TAG, "<onAudioFrameDecoded> timestamp=" + audioFrame.mTimestamp
//                + ", keyFrame=" + audioFrame.mKeyFrame
//                + ", lastFrame=" + audioFrame.mLastFrame );

    }

    @Override
    public void onDecodeError(AvMediaInfo mediaInfo, int errCode) {
        mDecErrCount++;
        Log.e(TAG, "<onDecoderError> errCode=" + errCode + ", mDecErrCount=" + mDecErrCount);

        if (mDecErrCount > 10) {
            if (mInitParam.mCallback != null) { // 回调转码错误错误
                mInitParam.mCallback.onDownloaderError(mInitParam, ErrCode.XERR_CODEC_DECODING);
            }
            mDecErrCount = 0;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////
    /////////////////// Override IAvCompEncoderCallback Methods ///////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onEncodeDone(IAvCompEncoder.CompEncodeParam encodeParam, int errCode) {
        Log.d(TAG, "<onEncodeDone> errCode=" + errCode);

        // 下载转码完成状态机
        setState(DOWNLOAD_STATE_DONE);

        if (mInitParam.mCallback != null) { // 回调整个下载完成
            mInitParam.mCallback.onDownloaderDone(mInitParam);
        }
    }

    @Override
    public void onEncodeError(IAvCompEncoder.CompEncodeParam encodeParam, int errCode) {
        Log.e(TAG, "<onEncodeError> errCode=" + errCode);

        if (mInitParam.mCallback != null) {
            mInitParam.mCallback.onDownloaderError(mInitParam, errCode);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////// Inner Methods ////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 创建解码器组件
     */
    int decodeCompCreate() {
        IAvCompDecoder.CompDecodeParam decodeParam = new IAvCompDecoder.CompDecodeParam();
        decodeParam.mContext = mInitParam.mContext;
        decodeParam.mCallback = this;
        decodeParam.mDecParam = new AvDecParam();
        decodeParam.mDecParam.mContext = mInitParam.mContext;
        decodeParam.mDecParam.mInFileUrl = mInitParam.mInFileUrl;
        decodeParam.mDecParam.mOutVidFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        decodeParam.mDecParam.mOutVidWidth = 0;     // 0: 表示使用原始大小
        decodeParam.mDecParam.mOutVidHeight = 0;
        decodeParam.mDecParam.mOutAudSampleFmt = AUDIO_SAMPLE_FMT;
        decodeParam.mDecParam.mOutAudChannels = AUDIO_CHANNELS;
        decodeParam.mDecParam.mOutAudSampleRate = AUDIO_SAMPLE_RATE;

        mAvDecoder = new AvCompSwDecoder();
        int ret = mAvDecoder.initialize(decodeParam);
        if (ret != ErrCode.XERR_NONE) {
            Log.e(TAG, "<decodeCompCreate> fail to create decoder component");
            return ret;
        }

        ret = mAvDecoder.start();
        if (ret != ErrCode.XERR_NONE) {
            Log.e(TAG, "<decodeCompCreate> fail to start decoder component");
            mAvDecoder.release();
            mAvDecoder = null;
            return ret;
        }

        return ErrCode.XOK;
    }

    /**
     * @brief 释放解码器组件
     */
    void decodeCompDestroy() {
        if (mAvDecoder != null) {
            mAvDecoder.stop();
            mAvDecoder.release();
            mAvDecoder = null;
        }
    }



    /**
     * @brief 创建编码器组件
     */
    int encodeCompCreate(int width, int height, int frameRate) {
        IAvCompEncoder.CompEncodeParam encodeParam = new IAvCompEncoder.CompEncodeParam();
        encodeParam.mContext = mInitParam.mContext;
        encodeParam.mCallback = this;
        encodeParam.mEncParam = new AvEncParam();
        encodeParam.mEncParam.mOutFilePath = mInitParam.mOutFilePath;

        encodeParam.mEncParam.mVideoCodec = DNLOAD_VIDEO_CODEC;
        encodeParam.mEncParam.mColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        encodeParam.mEncParam.mVideoWidth = width;
        encodeParam.mEncParam.mVideoHeight = height;
        encodeParam.mEncParam.mRotation = 0;
        encodeParam.mEncParam.mFrameRate = frameRate;
        encodeParam.mEncParam.mGopFrame = frameRate;        // 每秒1个GOP
        encodeParam.mEncParam.mVideoBitRate = calcVideoBitrate(width, height, frameRate);
        encodeParam.mEncParam.mVBitRateMode = calcVideoBitrateMode(encodeParam.mEncParam.mVideoBitRate);

        encodeParam.mEncParam.mAudioCodec = DNLOAD_AUDIO_CODEC;
        encodeParam.mEncParam.mSampleFmt = AUDIO_SAMPLE_FMT;
        encodeParam.mEncParam.mChannels = AUDIO_CHANNELS;
        encodeParam.mEncParam.mSampleRate = AUDIO_SAMPLE_RATE;
        encodeParam.mEncParam.mAudioBitRate = calcAudioBitrate(AUDIO_CHANNELS, AUDIO_SAMPLE_RATE);

        mAvEncoder = new AvCompHwEncoder();
        int ret = mAvEncoder.initialize(encodeParam);
        if (ret != ErrCode.XERR_NONE) {
            Log.e(TAG, "<encodeCompCreate> fail to create encoder component");
            return ret;
        }

        ret = mAvEncoder.start();
        if (ret != ErrCode.XERR_NONE) {
            Log.e(TAG, "<encodeCompCreate> fail to start encoder component");
            mAvEncoder.release();
            mAvEncoder = null;
            return ret;
        }

        return ErrCode.XOK;
    }

    /**
     * @brief 释放编码器组件
     */
    void encodeCompDestroy() {
        if (mAvEncoder != null) {
            mAvEncoder.stop();
            mAvEncoder.release();
            mAvEncoder = null;
        }
    }

    /**
     * @brief 启动编码器组件线程
     */
    int encodeCompStart() {
        synchronized (mDataLock) {
            if (mAvEncoder != null) {
                int ret = mAvEncoder.start();
                return ret;
            }
        }
        return ErrCode.XERR_BAD_STATE;
    }

    /**
     * @brief 停止编码器组件线程
     */
    int encodeCompStop() {
        synchronized (mDataLock) {
            if (mAvEncoder != null) {
                int ret = mAvEncoder.stop();
                return ret;
            }
        }
        return ErrCode.XERR_BAD_STATE;
    }


    ////////////////////////////////////////////////////////////////////////////
    /////////////////////// Methods for Encoding Paramters /////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private static class VideoBitrateCfg {
        public int mWidth;
        public int mHeight;
        public int mFrameRate;
        public int mBaseBitrate;
        public int mLiveBitrate;

        public VideoBitrateCfg(int width, int height, int frameRate,
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

    private final static VideoBitrateCfg[] mVideoBitrateArray = {
            new VideoBitrateCfg(160, 120, 15, 65, 130),
            new VideoBitrateCfg(120, 120, 15, 50 , 100),
            new VideoBitrateCfg(320, 180, 15, 140, 280),
            new VideoBitrateCfg(180, 180, 15, 100, 200),
            new VideoBitrateCfg(240, 180, 15, 120, 240),
            new VideoBitrateCfg(320, 240, 15, 200, 400),
            new VideoBitrateCfg(240, 240, 15, 140, 280),
            new VideoBitrateCfg(424, 240, 15, 220, 440),
            new VideoBitrateCfg(640, 360, 15, 400, 800),
            new VideoBitrateCfg(360, 360, 15, 260, 520),
            new VideoBitrateCfg(640, 360, 30, 600, 1200),
            new VideoBitrateCfg(360, 360, 30, 400, 800),
            new VideoBitrateCfg(480, 360, 15, 320, 640),
            new VideoBitrateCfg(480, 360, 30, 490, 980),
            new VideoBitrateCfg(640, 480, 15, 500, 1000),
            new VideoBitrateCfg(480, 480, 15, 400, 800),
            new VideoBitrateCfg(640, 480, 30, 750, 1500),
            new VideoBitrateCfg(480, 480, 30, 600, 1200),
            new VideoBitrateCfg(848, 480, 15, 610, 1220),
            new VideoBitrateCfg(848, 480, 30, 930, 1860),
            new VideoBitrateCfg(640, 480, 10, 400, 800),
            new VideoBitrateCfg(1280, 720, 15, 1130, 2260),
            new VideoBitrateCfg(1280, 720, 30, 1710, 3420),
            new VideoBitrateCfg(960, 720, 15, 910, 1820),
            new VideoBitrateCfg(960, 720, 30, 1380, 2760),
            new VideoBitrateCfg(1920, 1080, 15, 2080, 4160),
            new VideoBitrateCfg(1920, 1080, 30, 3150, 6300),
            new VideoBitrateCfg(1920, 1080, 60, 4780, 6500),
            new VideoBitrateCfg(2560, 1440, 30, 4850, 6500),
            new VideoBitrateCfg(2560, 1440, 60, 6500, 6500),
            new VideoBitrateCfg(3840, 2160, 30, 6500, 6500),
            new VideoBitrateCfg(3840, 2160, 60, 6500, 6500)
    };

    /**
     * @brief 根据视频帧宽高和帧率，找到最接近的 RTC 视频码率
     *        帧率相同，宽高面积正好大于指定宽高面积的
     */
    int calcVideoBitrate(int width, int height, int frameRate) {
        int inArea = width * height;
        int bitrate = 0;
        int i;

        for (i = 0; i < mVideoBitrateArray.length; i++) {
            VideoBitrateCfg bitrateCfg = mVideoBitrateArray[i];
            if (frameRate != bitrateCfg.mFrameRate) {
                continue;
            }

            if ((bitrateCfg.mWidth * bitrateCfg.mHeight) > inArea) {
                bitrate = bitrateCfg.mBaseBitrate * 1024;
                Log.d(TAG, "<calcVideoBitrate> found config="
                        + bitrateCfg.toString() + ", bitrate=" + bitrate);
            }
        }

        if (bitrate == 0) { // 没有找到匹配的码率，直接计算
            bitrate = (int) (width * height * frameRate * 0.15f);
        }

        // 判断码率范围并且进行调整
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AvCapability.VideoCaps videoCaps = AvCapability.getVideoCapability(DNLOAD_VIDEO_CODEC);
            int bitrateLower = videoCaps.mBitrateRange.getLower();
            int bitrateUpper = videoCaps.mBitrateRange.getUpper();
            int bitrateAdjust = (width*height);
            if (bitrate < bitrateLower) { // 码率小于下限
                while (bitrate < bitrateLower) {
                    bitrate += bitrateAdjust;
                }
                Log.d(TAG, "<calcVideoBitrate> videobitrate lower, Adjust videoBitrate=" + bitrate);

            } else if (bitrate > bitrateUpper) { // 码率大于上限
                while (bitrate > bitrateUpper) {
                    bitrate -= bitrateAdjust;
                }
                Log.d(TAG, "<calcVideoBitrate> videobitrate upper, Adjust videoBitrate=" + bitrate);
            }
        }

        Log.d(TAG, "<calcVideoBitrate> calculated, width=" + width
                + ", height=" + height + ", frameRate=" + frameRate
                + ", bitrate=" + bitrate);
        return bitrate;
    }

    /**
     * @brief 根据码率
     *        帧率相同，宽高面积正好大于指定宽高面积的
     */
    int calcVideoBitrateMode(int bitrate) {
        int bitRateMode = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AvCapability.VideoCaps videoCaps = AvCapability.getVideoCapability(DNLOAD_VIDEO_CODEC);
            String videoCapsText = videoCaps.toString();
            Log.d(TAG, "<calcVideoBitrateMode> videoCapability=" + videoCapsText);
            if (videoCaps.mBitrateCqSupported) {
                bitRateMode = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ;
                Log.d(TAG, "<recordingStart> BITRATE_MODE_CQ, mVideoBitRate=" + bitrate);
            } else {
                bitRateMode = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR;
                Log.d(TAG, "<calcVideoBitrateMode> BITRATE_MODE_VBR, mVideoBitRate=" + bitrate);
            }
        }

        return bitRateMode;
    }


    /**
     * @brief 根据音频通道数和采样率，计算音频码率
     *        帧率相同，宽高面积正好大于指定宽高面积的
     */
    int calcAudioBitrate(int channels, int sampleRate) {
        int bitrate = (int)(sampleRate * channels * 2 * 0.2f);
        Log.d(TAG, "<calcAudioBitrate> bitrate=" + bitrate);
        return bitrate;
    }

}