package io.agora.avmodule;


import android.media.MediaCodecInfo;
import android.os.Message;
import android.util.Log;



/**
 * @brief 音视频流订阅的录像机
 *        音视频流来源于 RTC SDK 拉流，转码成本地文件
 */
public class AvMediaRecorder extends AvCompBase
                            implements IAvCompEncoder.IAvCompEncoderCallback  {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final int HIG_RESOLUTION_SIZE = 6000000;

    /*
     * @brief 定义录像状态
     */
    public static final int RECORD_STATE_INVALID = 0x0000;      ///< 当前录像还未初始化
    public static final int RECORD_STATE_IDLE = 0x0001;         ///< 录像初始化成功，可以开始转换
    public static final int RECORD_STATE_ONGOING = 0x0002;      ///< 正在转换过程中

    //
    // The mesage Id
    //
    protected static final int MSG_ID_RECORD = 0x1001;          ///< 音视频帧录像处理




    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "AVMODULE/MediaRecorder";
    private static final String COMP_NAME = "MediaRecorder";


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private final Object mDataLock = new Object();    ///< 同步访问锁,类中相应变量需要进行加锁处理
    private volatile int mState = RECORD_STATE_INVALID;
    private AvRecorderParam mInitParam;      ///< 初始化参数

    private IAvCompEncoder mAvEncoder;      ///< 编码器组件

    private long mVideoFrameSpan = 0;       ///< 视频帧间隔时间
    private long mVideoReadTimestamp = 0;   ///< 最后一帧视频帧读取时刻点

    private AvFrameQueue mInVideoQueue = new AvFrameQueue();  ///< 输入视频帧队列
    private AvFrameQueue mInAudioQueue = new AvFrameQueue();  ///< 输入音频帧队列


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 初始化录像，准备输出媒体文件和编码
     *        初始化成功后，状态机切换为 RECORD_STATE_IDLE
     * @param initParam : 录像的初始化参数
     * @return 返回错误代码，0：表示成功打开；其他值：表示打开文件失败
     */
    public int initialize(AvRecorderParam initParam) {
        int ret;
        mInitParam = initParam;

        //
        // 创建编码器组件
        //
        IAvCompEncoder.CompEncodeParam encodeParam = new IAvCompEncoder.CompEncodeParam();
        encodeParam.mContext = mInitParam.mContext;
        encodeParam.mCallback = this;
        encodeParam.mEncParam = new AvEncParam();
        encodeParam.mEncParam.mOutFilePath = mInitParam.mOutFilePath;
        encodeParam.mEncParam.mVideoCodec = mInitParam.mVideoCodec;
        encodeParam.mEncParam.mColorFormat = mInitParam.mColorFormat;
        encodeParam.mEncParam.mVideoWidth = mInitParam.mVideoWidth;
        encodeParam.mEncParam.mVideoHeight = mInitParam.mVideoHeight;
        encodeParam.mEncParam.mRotation = mInitParam.mRotation;
        encodeParam.mEncParam.mFrameRate = mInitParam.mFrameRate;
        encodeParam.mEncParam.mGopFrame = mInitParam.mGopFrame;
        encodeParam.mEncParam.mVideoBitRate = mInitParam.mVideoBitRate;
        encodeParam.mEncParam.mVBitRateMode = mInitParam.mVBitRateMode;
        encodeParam.mEncParam.mAudioCodec = mInitParam.mAudioCodec;
        encodeParam.mEncParam.mSampleFmt = mInitParam.mSampleFmt;
        encodeParam.mEncParam.mChannels = mInitParam.mChannels;
        encodeParam.mEncParam.mSampleRate = mInitParam.mSampleRate;
        encodeParam.mEncParam.mAudioBitRate = mInitParam.mAudioBitRate;
        mAvEncoder = new AvCompHwEncoder();
        ret = mAvEncoder.initialize(encodeParam);
        if (ret != ErrCode.XERR_NONE) {
            Log.e(TAG, "<initialize> fail to create encoder component");
            release();
            return ret;
        }

        mVideoFrameSpan = (1000 / mInitParam.mFrameRate);   // 视频帧间隔时间


        //
        // 启动录像组件线程
        //
        ret = runStart(COMP_NAME);
        if (ret != ErrCode.XERR_NONE) {
            Log.e(TAG, "<initialize> fail to start component");
            release();
            return ret;
        }

        synchronized (mDataLock) {
            mState = RECORD_STATE_IDLE;
        }

        Log.d(TAG, "<initialize> done");
        return ErrCode.XOK;
    }


    /**
     * @brief 关闭录像，释放所有的编解码器，关闭输入输出文件
     *        释放完成后，状态机切换为 RECORD_STATE_INVALID
     * @return 返回错误代码
     */
    public int release() {
        Log.d(TAG, "<release> [BEGIN] mState=" + mState);

        // 释放编码器组件
        if (mAvEncoder != null) {
            mAvEncoder.release();
            mAvEncoder = null;
        }
        mInVideoQueue.clear();
        mInAudioQueue.clear();

        synchronized (mDataLock) {
            mState = RECORD_STATE_INVALID;
        }

        Log.d(TAG, "<release> [END] mState=" + mState);
        return ErrCode.XOK;
    }

    /*
     * @brief 获取当前录像的状态
     * @param None
     * @return 返回状态机
     */
    public int getRecordingState() {
        synchronized (mDataLock) {
            return mState;
        }
    }

    /*
     * @brief 录像启动内部线程开始转换处理
     *        启动成功后，状态机切换为 RECORD_STATE_ONGOING
     * @param convertCallback ：转换过程中的回调接口
     * @return 错误代码，0表示正常启动
     */
    public int recordingStart() {
        synchronized (mDataLock) {
            if (mState != RECORD_STATE_IDLE) {
                Log.e(TAG, "<recordingStart> [ERROR] bad state, mState=" + mState);
                return ErrCode.XERR_BAD_STATE;
            }
        }

        Log.d(TAG, "<recordingStart> [BEGIN] mState=" + mState);

        mVideoReadTimestamp = System.currentTimeMillis();
        mInVideoQueue.clear();
        mInAudioQueue.clear();
        synchronized (mDataLock) {
            mState = RECORD_STATE_ONGOING;
        }

        // 启动编码器组件线程
        int ret = mAvEncoder.start();
        if (ret != ErrCode.XOK) {
            Log.e(TAG, "<recordingStart> fail to encoder start, ret=" + ret);
            return ret;
        }

        // 启动录像组件线程
        ret = runStart(COMP_NAME);
        if (ret != ErrCode.XOK) {
            Log.e(TAG, "<recordingStart> fail to component start, ret=" + ret);
            return ret;
        }
        mWorkHandler.removeMessages(MSG_ID_RECORD);
        mWorkHandler.sendEmptyMessage(MSG_ID_RECORD);

        Log.d(TAG, "<recordingStart> [END] mState=" + mState + ", ret=" + ret);
        return ret;
    }

    /*
     * @brief 直接强制停止当前所有的转换处理线程，通常在转换成功后才会调用
     *        停止成功后，状态机切换为 RECORD_STATE_IDLE
     * @return 错误代码，0表示正常恢复转换
     */
    public int recordingStop() {
        synchronized (mDataLock) {
            if (mState != RECORD_STATE_ONGOING) {
                Log.e(TAG, "<recordingStop> [ERROR] bad state, mState=" + mState);
                return ErrCode.XERR_BAD_STATE;
            }
        }
        Log.d(TAG, "<recordingStop> [BEGIN] mState=" + mState);

        // 停止录像组件线程
        runStop();

        // 停止编码器组件线程
        int ret = mAvEncoder.stop();
        if (ret != ErrCode.XOK) {
            Log.e(TAG, "<recordingStop> fail to encoder stop, ret=" + ret);
        }

        synchronized (mDataLock) {
            mState = RECORD_STATE_IDLE;
        }

        Log.d(TAG, "<recordingStop> [END] mState=" + mState + ", ret=" + ret);
        return ret;
    }

    ///////////////////////////////////////////////////////////////////////////////////
    ///////////////////////// Override AvCompBase Methods /////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void removeAllMessages() {
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(MSG_ID_RECORD);
            Log.d(TAG, "<removeAllMessages> done");
        }
    }

    @Override
    protected void processWorkMessage(Message msg) {
        switch (msg.what) {
            case MSG_ID_RECORD:
                doMessageRecording();
                break;
        }
    }

    /*
     * @brief 定时对解码后的音视频帧进行处理
     */
    void doMessageRecording() {
        //
        // 读取视频帧数据，送入编码器组件
        //
        long currTimestamp = System.currentTimeMillis();
        if ((currTimestamp - mVideoReadTimestamp) >= mVideoFrameSpan) {  // 按照特定帧率来读取视频帧
            AvVideoFrame videoFrame = mInitParam.mAvReader.onReadVideoFrame();
            if (videoFrame != null) {
                mAvEncoder.inputVideoFrame(videoFrame);
                mVideoReadTimestamp = System.currentTimeMillis();
            }
        }

        //
        // 不断读取音频帧数据，送入编码器组件
        //
        for (;;) {
            AvAudioFrame audioFrame = mInitParam.mAvReader.onReadAudioFrame();
            if (audioFrame == null) {
                break;
            }
            mAvEncoder.inputAudioFrame(audioFrame);
        }

        // 定时进行下一次处理
        mWorkHandler.removeMessages(MSG_ID_RECORD);
        mWorkHandler.sendEmptyMessageDelayed(MSG_ID_RECORD, (mVideoFrameSpan/3));
        Log.d(TAG, "<doMessageRecording> done");
    }


    ///////////////////////////////////////////////////////////////////////////////////
    /////////////////// Override IAvCompEncoderCallback Methods ///////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onEncodeDone(IAvCompEncoder.CompEncodeParam encodeParam, int errCode) {
        Log.d(TAG, "<onEncodeDone> errCode=" + errCode);

    }

    @Override
    public void onEncodeError(IAvCompEncoder.CompEncodeParam encodeParam, int errCode) {
        Log.e(TAG, "<onEncodeError> errCode=" + errCode);

        if (mInitParam.mCallback != null) {
            mInitParam.mCallback.onRecorderError(mInitParam, errCode);
        }
    }

}