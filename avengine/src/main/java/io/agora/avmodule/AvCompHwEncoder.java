package io.agora.avmodule;


import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Message;
import android.util.Log;
import java.io.IOException;



/*
 * @brief 媒体文件硬编码组件
 */
public class AvCompHwEncoder extends AvCompBase implements IAvCompEncoder {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////




    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "AVMODULE/CompHwEncoder";

    private static final long SYNCOPT_TIMEOUT = 3000;      ///< 同步操作超时3秒

    //
    // The state for muxer
    //
    public static final int MUXSTATE_INIT = 0x0000;         ///< 混流器组件还未初始化
    public static final int MUXSTATE_PREPARE_AUD = 0x0001;  ///< 混流器正在初始化音频轨道
    public static final int MUXSTATE_PREPARE_VID = 0x0002;  ///< 混流器正在初始化视频轨道
    public static final int MUXSTATE_WRITTING = 0x0004;     ///< 混流器正常写文件操作


    //
    // The mesage Id
    //
    private static final int MSGID_ENCODING = 0x1001;



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////

    private final Object mDataLock = new Object();    ///< 同步访问锁,类中相应变量需要进行加锁处理

    private CompEncodeParam mInitParam;
    private volatile int mStateMachine = STATE_INVALID; ///< 当前状态机
    private volatile int mMuxState = MUXSTATE_INIT;     ///< 混流器状态机

    private final Object mEncMuxDoneEvent = new Object(); ///< 所有音视频编码及混流完成事件
    private AvEncoder mVideoEncoder = null;              ///< 视频编码器
    private AvEncoder mAudioEncoder = null;             ///< 音频编码器
    private MediaMuxer mAvMuxer = null;                 ///< 音视频混流器
    private int mVideoTrackIndex = -1;                  ///< 视频混流轨道索引
    private int mAudioTrackIndex = -1;                  ///< 音频混流轨道索引
    private boolean mVideoInputEos = false;             ///< 视频帧是否已经全部送入完成
    private boolean mVideoEncodeEos = false;            ///< 视频帧是否已经全部编码完成
    private boolean mAudioInputEos = false;             ///< 音频帧是否已经全部送入完成
    private boolean mAudioEncodeEos = false;            ///< 音频帧是否已经全部送入完成
    private long mVideoTimestamp = 0;                   ///< 当前编码视频帧时间戳
    private long mAudioTimestamp = 0;                   ///< 当前编码音频帧时间戳


    private AvFrameQueue mInVideoQueue = new AvFrameQueue();    ///< 要编码的视频帧队列
    private AvFrameQueue mInAudioQueue = new AvFrameQueue();    ///< 要编码的音频帧队列



    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////// Override Methods of IAvCompEncoder //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    /*
     * @brief 组件初始化
     * @param context : 相关的上下文信息
     * @return error code
     */
    @Override
    public int initialize(CompEncodeParam initParam) {
        int ret;
        mInitParam = initParam;
        mVideoTimestamp = 0;
        mAudioTimestamp = 0;
        mVideoInputEos = false;
        mVideoEncodeEos = false;
        mAudioInputEos = false;
        mAudioEncodeEos = false;
        mMuxState = MUXSTATE_INIT;
        mInVideoQueue.clear();
        mInAudioQueue.clear();

        //
        // 打开视频编码器
        //
        if (mInitParam.mEncParam.mVideoCodec.isEmpty())  {  // 不需要进行视频编码
            mVideoInputEos = true;
            mVideoEncodeEos = true;
            mVideoEncoder = null;

        } else {  // 视频编码器初始化
            mVideoEncoder = new AvEncoder();
            ret = mVideoEncoder.initialize(mInitParam.mEncParam, AvEncoder.ENCODER_TYPE_VIDEO);
            if (ret != ErrCode.XOK) {
                Log.e(TAG, "<initialize> fail to open video encoder, ret=" + ret);
                mVideoEncoder = null;
                return ErrCode.XERR_CODEC_OPEN;
            }
        }


        //
        // 打开音频编码器
        //
        if  (mInitParam.mEncParam.mAudioCodec.isEmpty()) {  // 不需要进行音频编码
            mAudioInputEos = true;
            mAudioEncodeEos = true;
            mAudioEncoder = null;

        } else {  // 音频编码器初始化
            mAudioEncoder = new AvEncoder();
            ret = mAudioEncoder.initialize(mInitParam.mEncParam, AvEncoder.ENCODER_TYPE_AUDIO);
            if (ret != ErrCode.XOK) {
                Log.e(TAG, "<initialize> fail to open audio encoder, ret=" + ret);
                mAudioEncoder = null;
                mVideoEncoder.release();
                mVideoEncoder = null;
                return ErrCode.XERR_CODEC_OPEN;
            }
        }

        //
        // 初始化混流器
        //
        ret = muxerInitialize();
        if (ret != ErrCode.XOK) {
            Log.e(TAG, "<initialize> fail to open muxerInitialize(), ret=" + ret);
            mAudioEncoder.release();
            mVideoEncoder.release();
            mAudioEncoder = null;
            mVideoEncoder = null;
            return ErrCode.XERR_CODEC_OPEN;
        }

        // 设置混流器
        if (mVideoEncoder != null) {
            mVideoEncoder.setMediaMuxer(mAvMuxer);
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.setMediaMuxer(mAvMuxer);
        }
        mMuxState = MUXSTATE_PREPARE_AUD;

        synchronized (mDataLock) {
            mStateMachine = STATE_IDLE;
        }


        Log.d(TAG, "<initialize> done");
        return ErrCode.XOK;
    }

    /*
     * @brief 组件释放操作
     */
    @Override
    public void release()   {
        Log.d(TAG, "<release> [BEGIN] mStateMachine=" + mStateMachine);

        if (mAudioEncoder != null) {  // 关闭音频编码器
            mAudioEncoder.release();
            mAudioEncoder = null;
        }

        if (mVideoEncoder != null) {  // 关闭视频编码器
            mVideoEncoder.release();
            mVideoEncoder = null;
        }

        // 释放混流器
        muxerRelease();

        synchronized (mDataLock) {
            mStateMachine = STATE_INVALID;
        }

        Log.d(TAG, "<release> [END] mStateMachine=" + mStateMachine);
    }

    /*
     * @brief 获取组件当前状态
     * @return 返回状态机值
     */
    @Override
    public int getState() {
        synchronized (mDataLock) {
            return mStateMachine;
        }
    }

    private void setState(int newState) {
        synchronized (mDataLock) {
            mStateMachine = newState;
        }
    }

    /*
     * @brief 送入要编码的视频帧数据
     * @return 返回编码后的媒体文件信息
     */
    @Override
    public int inputVideoFrame(AvVideoFrame videoFrame) {
        mInVideoQueue.inqueue(videoFrame);
        return ErrCode.XOK;
    }

    /*
     * @brief 送入要编码的视频帧数据
     * @return 返回编码后的媒体文件信息
     */
    @Override
    public int inputAudioFrame(AvAudioFrame audioFrame) {
        mInAudioQueue.inqueue(audioFrame);
        return ErrCode.XOK;
    }

    /*
     * @brief 获取输入视频帧数量
     * @return 队列中等待编码的视频帧数量
     */
    @Override
    public int getInVideoCount() {
        return mInVideoQueue.size();
    }

    /*
     * @brief 获取输入音频帧数量
     * @return 队列中等待编码的音频帧数量
     */
    @Override
    public int getInAudioCount() {
        return mInAudioQueue.size();
    }

    /*
     * @brief 启动编码线程进行编码处理
     * @param None
     * @retrun error code, 0表示成功
     */
    @Override
    public int start()  {
        Log.d(TAG, "<start> [BEGIN] mStateMachine=" + mStateMachine);

        synchronized (mDataLock) {
            mStateMachine = STATE_ENCODING;
        }

        // 启动组件线程
        runStart("CompHwEncoder");

        mWorkHandler.removeMessages(MSGID_ENCODING);
        mWorkHandler.sendEmptyMessage(MSGID_ENCODING);

        Log.d(TAG, "<start> [END] mStateMachine=" + mStateMachine);
        return ErrCode.XOK;
    }

    /*
     * @brief 停止编码线程进行编码处理，若要恢复需要调用start()
     * @param None
     * @retrun error code, 0表示成功
     */
    @Override
    public int stop()  {
        Log.d(TAG, "<stop> [BEGIN] mStateMachine=" + mStateMachine);

        // 停止线程处理
        runStop();

        synchronized (mDataLock) {
            mStateMachine = STATE_IDLE;
        }

        Log.d(TAG, "<stop> [END] mStateMachine=" + mStateMachine);
        return ErrCode.XOK;
    }

    /*
     * @brief 暂停当前处理
     * @param None
     * @retrun error code, 0表示成功
     */
    @Override
    public int pause() {
        int state = getState();
        if (state == STATE_PAUSED) {
            return ErrCode.XOK;
        } else if (state != STATE_ENCODING) {
            Log.e(TAG, "<pause> bad state, mStateMachine=" + mStateMachine);
            return ErrCode.XERR_BAD_STATE;
        }

        setState(STATE_PAUSED);
        return ErrCode.XOK;
    }

    /*
     * @brief 恢复暂停处理
     * @param None
     * @retrun error code, 0表示成功
     */
    @Override
    public int resume() {
        int state = getState();
        if (state == STATE_ENCODING) {
            return ErrCode.XOK;
        } else if (state != STATE_PAUSED) {
            Log.e(TAG, "<resume> bad state, mStateMachine=" + mStateMachine);
            return ErrCode.XERR_BAD_STATE;
        }

        setState(STATE_ENCODING);
        return ErrCode.XOK;
    }


    @Override
    public long getVideoTimestamp() {
        synchronized (mDataLock) {
            return mVideoTimestamp;
        }
    }

    @Override
    public long getAudioTimestamp() {
        synchronized (mDataLock) {
            return mAudioTimestamp;
        }
    }

    /////////////////////////////////////////////////////////////////////////
    //////////////////// Media Muxering Methods /////////////////////////////
    /////////////////////////////////////////////////////////////////////////
    /*
     * @brief 初始化混流器
     */
    int muxerInitialize() {

        try {
            //AvUtility.deleteFile(mInitParam.mEncParam.mOutFilePath);
            mAvMuxer = new MediaMuxer(mInitParam.mEncParam.mOutFilePath,
                                        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        } catch (IllegalArgumentException illegalExp) {
            illegalExp.printStackTrace();
            Log.e(TAG, "<muxerInitialize> [IllegalException], illegalExp=" + illegalExp.toString());
            if (mVideoEncoder != null)  mVideoEncoder.release();
            if (mAudioEncoder != null)  mAudioEncoder.release();
            return ErrCode.XERR_CODEC_OPEN;

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
            Log.e(TAG, "<muxerInitialize> [IOException], ioExp=" + ioExp.toString());
            if (mVideoEncoder != null)  mVideoEncoder.release();
            if (mAudioEncoder != null)  mAudioEncoder.release();
            return ErrCode.XERR_CODEC_OPEN;

        } catch (Exception exp) {
            exp.printStackTrace();
            Log.e(TAG, "<muxerInitialize> [Exception], exp=" + exp.toString());
            if (mVideoEncoder != null)  mVideoEncoder.release();
            if (mAudioEncoder != null)  mAudioEncoder.release();
            return ErrCode.XERR_CODEC_OPEN;
        }

        Log.d(TAG, "<muxerInitialize> done, outFile=" + mInitParam.mEncParam.mOutFilePath);
        return ErrCode.XOK;
    }

    /*
     * @brief 释放混流器
     */
    int muxerRelease() {
        if (mAvMuxer != null) {
            try {
                mAvMuxer.stop();
                mAvMuxer.release();
            } catch (IllegalStateException stateExp) {  // 混流器可能还未star()会导致该异常，这是正常的
                stateExp.printStackTrace();
            }
            mAvMuxer = null;
            Log.d(TAG, "<muxerRelease> done");
        }
        return ErrCode.XOK;
    }

    /*
     * @brief 添加视频轨道信息
     * @param videoFormat : 视频信息
     * @return 添加的视频轨道
     */
    public int muxerAddVideoTrack(MediaFormat videoFormat) {
        mVideoTrackIndex = mAvMuxer.addTrack(videoFormat);
        return mVideoTrackIndex;
    }

    /*
     * @brief 添加音频轨道信息到混流器
     * @param videoFormat : 视频信息
     * @return 添加的音频轨道
     */
    public int muxerAddAudioTrack(MediaFormat audioFormat) {
        mAudioTrackIndex = mAvMuxer.addTrack(audioFormat);
        return mAudioTrackIndex;
    }


    /*
     * @brief 根据轨道信息决定是否启动混流器
     * @param videoFormat : 视频信息
     * @return 添加的音频轨道
     */
    public int muxerStart() {
        if (mAudioTrackIndex < 0 || mVideoTrackIndex < 0) {
            return ErrCode.XERR_BAD_STATE;
        }

        try {
            mAvMuxer.start();
        } catch (IllegalStateException illegalExp) {
            illegalExp.printStackTrace();
            Log.e(TAG, "<muxerStart> [illegalExp]");
            return ErrCode.XERR_BAD_STATE;
        }

        Log.d(TAG, "<muxerStart> done");
        return ErrCode.XOK;
    }


    /////////////////////////////////////////////////////////////////////////
    ////////////////////////// Internal Methods /////////////////////////////
    /////////////////////////////////////////////////////////////////////////
    @Override
    protected void removeAllMessages() {
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(MSGID_ENCODING);
            Log.d(TAG, "<removeAllMessages> done");
        }
    }

    @Override
    protected void processWorkMessage(Message msg)
    {
        switch (msg.what)
        {
            case MSGID_ENCODING: {  // 正常循环编码消息
                doMessageEncoding();
            } break;

            default:
                break;
        }
    }

    /*
     * @brief 整体的编码操作
     */
    private void doMessageEncoding() {
        int state = getState();
        if (state == STATE_IDLE || state == STATE_PAUSED) {  // 停止编码，则延迟10ms后再检测
            mWorkHandler.removeMessages(MSGID_ENCODING);
            mWorkHandler.sendEmptyMessageDelayed(MSGID_ENCODING, 10);
            return;
        }
        if (state == STATE_INVALID) {
            return;
        }


        int retVideo = ErrCode.XOK;
        int retAudio = ErrCode.XOK;

        switch (mMuxState) {
            case MUXSTATE_PREPARE_AUD: { // 正在初始化音频轨道
                if (mInitParam.mEncParam.mAudioCodec.isEmpty()) {
                    // 无音频流要编码
                    mMuxState = MUXSTATE_PREPARE_VID;
                    Log.d(TAG, "<doMessageEncoding> [MUX_STATE] NO AUDIO, AUDIO-->VIDEO");

                } else {
                    // 处理音频流编码
                    retAudio = doAudioEncoding();
                    mAudioTrackIndex = mAudioEncoder.getTrackIndex();
                    if (mAudioTrackIndex != -1) {   // 混流器已经设置音频轨道
                        mMuxState = MUXSTATE_PREPARE_VID;
                        Log.d(TAG, "<doMessageEncoding> [MUX_STATE] AUDIO-->VIDEO");
                    }
                }
            } break;

            case MUXSTATE_PREPARE_VID: { // 正在初始化视频轨道
                if (mInitParam.mEncParam.mVideoCodec.isEmpty()) {
                    // 无视频流编码
                    mMuxState = MUXSTATE_WRITTING;
                    Log.d(TAG, "<doMessageEncoding> [MUX_STATE] NO VIDEO, VIDEO-->WRITTING");
                    try {
                        mAvMuxer.start();
                        mMuxState = MUXSTATE_WRITTING;

                    } catch (IllegalStateException stateExpt) {
                        stateExpt.printStackTrace();
                    }

                } else {
                    // 处理视频流编码
                    retVideo = doVideoEncoding();
                    mVideoTrackIndex = mVideoEncoder.getTrackIndex();
                    if (mVideoTrackIndex != -1) {   // 混流器已经设置视频轨道
                        try {
                            mAvMuxer.start();
                            mMuxState = MUXSTATE_WRITTING;
                            Log.d(TAG, "<doMessageEncoding> [MUX_STATE] VIDEO-->WRITTING");

                        } catch (IllegalStateException stateExpt) {
                            stateExpt.printStackTrace();
                            Log.e(TAG, "<doMessageEncoding> fail to start muxer");
                        }

                    }
                }
            } break;

            case MUXSTATE_WRITTING: {   // 正常解码读取
                retVideo = doVideoEncoding();  // 编码视频帧
                retAudio = doAudioEncoding();  // 编码音频帧
            } break;
        }

        if ((ErrCode.XERR_CODEC_DEC_EOS == retVideo) && (ErrCode.XERR_CODEC_DEC_EOS == retAudio)) {
            // 全部编码完成了
            Log.d(TAG, "<doMessageEncoding> All streams encoding done!");
            synchronized (mEncMuxDoneEvent) {
                mEncMuxDoneEvent.notify();    // 事件通知
            }

            if (mInitParam.mCallback != null) {  // 回调编码完成
                mInitParam.mCallback.onEncodeDone(mInitParam, ErrCode.XOK);
            }

        } else {
            // 立即进行下一次编码操作
            mWorkHandler.removeMessages(MSGID_ENCODING);
            mWorkHandler.sendEmptyMessage(MSGID_ENCODING);
        }
    }

    /*
     * @brief 进行视频帧编码操作
     */
    private int doVideoEncoding() {
        synchronized (mDataLock) {
            if (mStateMachine != STATE_ENCODING) {
                Log.e(TAG, "<doVideoEncoding> bad state, mStateMachine=" + mStateMachine);
                return ErrCode.XERR_BAD_STATE;
            }
        }
        if (mVideoEncoder == null) {  // 没有视频帧要编码
            return ErrCode.XERR_CODEC_DEC_EOS;
        }
        if (mVideoInputEos && mVideoEncodeEos) { // 视频帧的送入和编码都完成了
            return ErrCode.XERR_CODEC_DEC_EOS;
        }


        //
        // 从视频帧队列中取视频帧数据，送入到编码器中
        //
        int ret = ErrCode.XOK;
        if (!mVideoInputEos) {
            AvVideoFrame inVideoFrame = (AvVideoFrame)mInVideoQueue.dequeue();
            if (inVideoFrame != null) {
                ret = mVideoEncoder.inputFrame(inVideoFrame);
                if (ret == ErrCode.XERR_CODEC_DEC_EOS) {  // 视频帧已经送入完成
                    Log.d(TAG, "<doVideoEncoding> feeding EOS done!");
                    mVideoInputEos = true;

                } else if (ret == ErrCode.XERR_CODEC_NOBUFFER) {
                    mInVideoQueue.inqueueHead(inVideoFrame);  // 没有送入缓冲区了，重新插到队列头

                } else if (ret != ErrCode.XOK) {
                    //Log.e(TAG, "<doVideoEncoding> fail to input frame");
                }
            }
        }

        //
        // 进行编码操作，如果有编码后的数据输出会输出到 outFrame中
        //
        if (!mVideoEncodeEos) {
            ret = mVideoEncoder.encodeVideoFrame();
            if (ret == ErrCode.XERR_CODEC_ENCODING) { // 编码失败
                Log.e(TAG, "<doVideoEncoding> encoding error");
                if (mInitParam.mCallback != null) {  // 回调编码错误
                    mInitParam.mCallback.onEncodeError(mInitParam, ErrCode.XERR_CODEC_ENCODING);
                }

            } else if (ret == ErrCode.XERR_CODEC_DEC_EOS) {  // 所有编码都已经完成
                Log.d(TAG, "<doVideoEncoding> encoding EOS done!");
                mVideoEncodeEos = true;

            } else { // 可以继续进行编码操作或者轨道已经就绪
                //    Log.d(TAG, "<doVideoEncoding> decoded one frame, timestamp=" + outFrame.mTimestamp);
            }
        }

        // 获取当前视频帧编码时间戳
        synchronized (mDataLock) {
            mVideoTimestamp = mVideoEncoder.getVideoTimestamp();
        }
        return ret;
    }

    /*
     * @brief 进行音频帧编码操作
     */
    private int doAudioEncoding() {
        synchronized (mDataLock) {
            if (mStateMachine != STATE_ENCODING) {
                Log.e(TAG, "<doAudioEncoding> bad state, mStateMachine=" + mStateMachine);
                return ErrCode.XERR_BAD_STATE;
            }
        }
        if (mAudioEncoder == null) {  // 没有音频流要编码
            return ErrCode.XERR_CODEC_DEC_EOS;
        }
        if (mAudioInputEos && mAudioEncodeEos) { // 音频帧的送入和编码都完成了
            return ErrCode.XERR_CODEC_DEC_EOS;
        }

        //
        // 从音频帧队列中取音频帧数据，送入到编码器中
        //
        int ret = ErrCode.XOK;
        if (!mAudioInputEos) {
            AvAudioFrame inAudioFrame = (AvAudioFrame)mInAudioQueue.dequeue();
            if (inAudioFrame != null) {
                ret = mAudioEncoder.inputFrame(inAudioFrame);
                if (ret == ErrCode.XERR_CODEC_DEC_EOS) {  // 音频帧已经送入完成
                    Log.d(TAG, "<doAudioEncoding> feeding EOS done!");
                    mAudioInputEos = true;

                } else if (ret == ErrCode.XERR_CODEC_NOBUFFER) {
                    mInAudioQueue.inqueueHead(inAudioFrame);  // 没有送入缓冲区了，重新插到队列头

                } else if (ret != ErrCode.XOK) {
                    //Log.e(TAG, "<doAudioEncoding> fail to input frame");
                }
            }
        }


        //
        // 进行编码操作，如果有编码后的数据输出会输出到 outFrame中
        //
        if (!mAudioEncodeEos) {
            ret = mAudioEncoder.encodeAudioFrame();
            if (ret == ErrCode.XERR_CODEC_ENCODING) { // 编码失败
                Log.e(TAG, "<doAudioEncoding> encoding error");
                if (mInitParam.mCallback != null) {  // 回调编码错误
                    mInitParam.mCallback.onEncodeError(mInitParam, ErrCode.XERR_CODEC_ENCODING);
                }

            } else if (ret == ErrCode.XERR_CODEC_DEC_EOS) {  // 所有编码都已经完成
                Log.d(TAG, "<doAudioEncoding> encoding EOS done!");
                mAudioEncodeEos = true;

            } else { // 可以继续进行编码操作或者轨道已经就绪
                //    Log.d(TAG, "<doAudioEncoding> decoded one frame, timestamp=" + outFrame.mTimestamp);
            }
        }

        // 获取当前音频帧编码时间戳
        synchronized (mDataLock) {
            mAudioTimestamp = mAudioEncoder.getAudioTimestamp();
        }
        return ret;
    }


}