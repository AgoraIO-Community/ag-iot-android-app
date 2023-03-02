package io.agora.avmodule;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Pair;




/*
 * @brief 媒体文件解码组件软件实现
 */
public class AvCompSwDecoder extends AvCompBase implements IAvCompDecoder {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "AVMODULE/CompSwDecoder";

    private static final long EXIT_WAIT_TIMEOUT = 3000;    ///< 线程结束等待超时3秒
    private static final long SYNCOPT_TIMEOUT = 3000;      ///< 同步操作超时3秒

    //
    // The state machine of Player Engine
    //
    public static final int STATE_INVALID = 0x0000;         ///< 组件还未初始化
    public static final int STATE_READY = 0x0001;           ///< 媒体文件已经打开，但未解码处理
    public static final int STATE_DECODING = 0x0002;        ///< 正在解码过程中
    public static final int STATE_PAUSED = 0x0003;          ///< 暂停解码处理状态
    public static final int STATE_DONE = 0x0004;            ///< 整个解码处理已经完成

    //
    // The mesage Id
    //
    private static final int MSGID_DECODING = 0x1001;



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private final Object mDataLock = new Object();    ///< 同步访问锁,类中相应变量需要进行加锁处理

    private CompDecodeParam mInitParam;
    private volatile int mStateMachine = STATE_INVALID; ///< 当前状态机

    private AvSoftDecoder mDecoder = null;              ///< 软解码器
    private int[] mInputPktType = new int[4];
    private boolean mInputEos = false;                  ///< 数据包送入是否已经完成
    private boolean mVideoDecodeEos = false;            ///< 视频帧是否已经全部解码完成
    private boolean mAudioDecodeEos = false;            ///< 音频帧是否已经全部送入完成

    private AvMediaInfo mMediaInfo = new AvMediaInfo(); ///< 媒体文件信息
    private AvFrameQueue mVideoQueue = new AvFrameQueue();  ///< 解码后的视频帧队列
    private AvFrameQueue mAudioQueue = new AvFrameQueue();  ///< 解码后的音频帧队列



    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 初始化SDK
     * @param initParam : 解码初始化参数
     * @return error code
     */
    @Override
    public int initialize(CompDecodeParam initParam) {
        int ret;
        mInitParam = initParam;
        mInputEos = false;
        mVideoDecodeEos = false;
        mAudioDecodeEos = false;
        mMediaInfo.mVideoTrackId = -1;
        mMediaInfo.mAudioTrackId = -1;
        mVideoQueue.clear();
        mAudioQueue.clear();

        setState(STATE_READY);
        Log.d(TAG, "<initialize> done");
        return ErrCode.XOK;
    }

    /**
     * @brief 引擎释放操作
     */
    public void release()   {

        if (mDecoder != null) {  // 关闭软解码器
            mDecoder.parserClose();
            mDecoder = null;
            Log.d(TAG, "<release> done");
        }

        mVideoQueue.clear();
        mAudioQueue.clear();
        setState(STATE_INVALID);
    }

    /**
     * @brief 获取Sdk当前状态
     * @return 返回状态机值
     */
    public int getState() {
        synchronized (mDataLock) {
            return mStateMachine;
        }
    }

    /**
     * @brief 设置新的状态机值
     * @param newState : 要设置的新状态机值
     */
    private void setState(int newState) {
        synchronized (mDataLock) {
            mStateMachine = newState;
        }
    }

    /**
     * @brief 获取媒体文件信息
     * @return 返回解码后的媒体文件信息
     */
    public AvMediaInfo getMediaInfo() {
        return mMediaInfo;
    }

    /*
     * @brief 启动解码线程进行解码处理
     * @param None
     * @retrun error code, 0表示成功
     */
    @Override
    public int start()  {
        Log.d(TAG, "<start> [BEGIN] mStateMachine=" + mStateMachine);

        setState(STATE_DECODING);

        // 启动组件线程
        runStart("CompSwDecoder");

        mWorkHandler.removeMessages(MSGID_DECODING);
        mWorkHandler.sendEmptyMessage(MSGID_DECODING);

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

        setState(STATE_READY);

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
        } else if (state != STATE_DECODING) {
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
        if (state == STATE_DECODING) {
            return ErrCode.XOK;
        } else if (state != STATE_PAUSED) {
            Log.e(TAG, "<resume> bad state, mStateMachine=" + mStateMachine);
            return ErrCode.XERR_BAD_STATE;
        }

        setState(STATE_DECODING);
        return ErrCode.XOK;
    }


    /*
     * @brief 从当前解码器中提取解码的一帧视频帧
     * @param None
     * @retrun 返回解码到的视频帧，如果队列为空则返回null
     */
    public AvVideoFrame dequeueVideoFrame()  {
        AvVideoFrame videoFrame = (AvVideoFrame)mVideoQueue.dequeue();
        return videoFrame;
    }

    /*
     * @brief 从当前解码器中提取解码的一帧音频
     * @param None
     * @retrun 返回解码到的音频帧，如果队列为空则返回null
     */
    public AvAudioFrame dequeueAudioFrame()  {
        AvAudioFrame audioFrame = (AvAudioFrame)mAudioQueue.dequeue();
        return audioFrame;
    }

    /*
     * @brief 获取输出队列视频帧个数
     * @param None
     * @retrun 视频帧个数
     */
    public int getVideoFrameCount() {
        int frameCnt = mVideoQueue.size();
        return frameCnt;
    }

    /*
     * @brief 获取输出队列音频帧个数
     * @param None
     * @retrun 音频帧个数
     */
    public int getAudioFrameCount() {
        int frameCnt = mAudioQueue.size();
        return frameCnt;
    }

    /////////////////////////////////////////////////////////////////////////
    ////////////////////////// Internal Methods /////////////////////////////
    /////////////////////////////////////////////////////////////////////////
    @Override
    protected void removeAllMessages() {
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(MSGID_DECODING);
            Log.d(TAG, "<removeAllMessages> done");
        }
    }

    @Override
    protected void processWorkMessage(Message msg)
    {
        switch (msg.what)
        {
            case MSGID_DECODING: {  // 正常循环编码消息
                doMessageDecoding();
            } break;

            default:
                break;
        }
    }


    /*
     * @brief 整体的解码操作
     */
    private void doMessageDecoding() {
        int state = getState();
        if (state == STATE_READY || state == STATE_PAUSED) {  // 停止解码，则延迟10ms后再检测
            mWorkHandler.removeMessages(MSGID_DECODING);
            mWorkHandler.sendEmptyMessageDelayed(MSGID_DECODING, 10);
            return;
        }
        if (state == STATE_INVALID || state == STATE_DONE) {  // 不再进行解码处理
            mWorkHandler.removeMessages(MSGID_DECODING);
            return;
        }

        //
        // 在解码线程中打开云文件
        //
        if (mDecoder == null) {
            mDecoder = new AvSoftDecoder();
            int ret = mDecoder.parserOpen(mInitParam.mDecParam);
            if (ret != ErrCode.XOK) {
                Log.e(TAG, "<doMessageDecoding> fail to open parser");
                if (mInitParam.mCallback != null) {
                    mInitParam.mCallback.onDecodeError(mMediaInfo, ErrCode.XERR_FILE_OPEN);
                }
                return;
            }
            mMediaInfo = mDecoder.parserGetMediaInfo();

            if (mMediaInfo.mVideoTrackId < 0) {  // 没有视频流
                mVideoDecodeEos = true;

            } else if (mMediaInfo.mAudioTrackId < 0) {  // 没有音频流
                mAudioDecodeEos = true;
            }

            if (mInitParam.mCallback != null) {
                mInitParam.mCallback.onMediaInfoDecoded(mMediaInfo);
            }
            Log.d(TAG, "<doMessageDecoding> open file done, mediaInfo=" + mMediaInfo.toString());
        }


        int videoCount = mVideoQueue.size();
        int audioCount = mAudioQueue.size();
        if (videoCount > 4 || audioCount > 16) {  // 解码的帧缓冲区队列过多，延迟解码
            mWorkHandler.removeMessages(MSGID_DECODING);
            mWorkHandler.sendEmptyMessageDelayed(MSGID_DECODING, 30);
//            Log.d(TAG, "<doMessageDecoding> delay 30ms, videoCount=" + videoCount
//                    + ", audioCount=" + audioCount);
            return;
        }
        mInputPktType[0] = 0;



        // 送入解码的数据包
        int retInput = doInputPacket();

        // 解码视频帧
        int decVideo = doVideoDecoding();

        // 解码音视频帧
        int decAudio = doAudioDecoding();


        if (mVideoDecodeEos && mAudioDecodeEos) {
            // 全部解码完成了
            setState(STATE_DONE);
            mWorkHandler.removeMessages(MSGID_DECODING);
            Log.d(TAG, "<doMessageDecoding> All streams decoding done!");

        } else {
            // 立即进行下一次解码操作
            mWorkHandler.removeMessages(MSGID_DECODING);
            mWorkHandler.sendEmptyMessage(MSGID_DECODING);
        }
    }

    /*
     * @brief 读取音视频数据包送入解码器
     */
    private int doInputPacket() {
        if (getState() != STATE_DECODING) {
            Log.e(TAG, "<doInputPacket> bad state, mStateMachine=" + mStateMachine);
            return ErrCode.XERR_BAD_STATE;
        }
        if (mDecoder == null) {
            Log.e(TAG, "<doInputPacket> mDecoder not ready");
            return ErrCode.XERR_BAD_STATE;
        }

        if (mInputEos) { // 读取包送入已经完成了
            return ErrCode.XERR_CODEC_DEC_EOS;
        }

        int ret = mDecoder.parserInputPacket(mInputPktType);
        if (ret == ErrCode.XERR_CODEC_DEC_EOS) {  // 视频帧已经送入完成
            Log.d(TAG, "<doInputPacket> feeding EOS done!");
            mInputEos = true;

        } else if (ret != ErrCode.XOK) {
            Log.e(TAG, "<doInputPacket> fail to input frame");
        }

        return ret;
    }

    /*
     * @brief 进行视频帧解码操作
     */
    private int doVideoDecoding() {
        if (getState() != STATE_DECODING) {
            Log.e(TAG, "<doVideoDecoding> bad state, mStateMachine=" + mStateMachine);
            return ErrCode.XERR_BAD_STATE;
        }
        if (mDecoder == null) {
            Log.e(TAG, "<doVideoDecoding> mDecoder not ready");
            return ErrCode.XERR_BAD_STATE;
        }

        if (mVideoDecodeEos) { // 视频帧的解码都完成了
            return ErrCode.XERR_CODEC_DEC_EOS;
        }


        //
        // 如果可以解码成功则反复进行操作，直到返回错误 或者 XERR_CODEC_DEC_EOS 或者 XERR_CODEC_INDATA
        // 解码后的数据输出会输出到 outFrame中
        //
        int ret = ErrCode.XOK;
        for (;;) {

            Pair<Integer, AvVideoFrame> pair = mDecoder.parserDecVideoFrame();
            ret = pair.first;
            AvVideoFrame videoFrame = pair.second;
            if (videoFrame != null) {
                mVideoQueue.inqueue(videoFrame);
                if (mInitParam.mCallback != null) {  // 回调解码出来的视频帧
                    mInitParam.mCallback.onVideoFrameDecoded(mMediaInfo, videoFrame);
                }
            }

            if (ret == ErrCode.XERR_CODEC_DECODING) { // 解码失败
                Log.e(TAG, "<doVideoDecoding> decoding error");
                if (mInitParam.mCallback != null) {  // 回调解码错误
                    mInitParam.mCallback.onDecodeError(mMediaInfo, ErrCode.XERR_CODEC_DECODING);
                }
                break;  // 跳出循环

            } else if (ret == ErrCode.XERR_CODEC_DEC_EOS) {  // 所有解码都已经完成
                Log.d(TAG, "<doVideoDecoding> decoding EOS done!");
                mVideoDecodeEos = true;
                break;  // 跳出循环

            } else if (ret < 0) {
                //Log.d(TAG, "<doVideoDecoding> error, ret=" + ret);
                break;  // 跳出循环

            } else { // 可以继续进行解码操作
            }
        }

        return ret;
    }

    /*
     * @brief 进行音频帧解码操作
     */
    private int doAudioDecoding() {
        if (getState() != STATE_DECODING) {
            Log.e(TAG, "<doAudioDecoding> bad state, mStateMachine=" + mStateMachine);
            return ErrCode.XERR_BAD_STATE;
        }
        if (mDecoder == null) {
            Log.e(TAG, "<doAudioDecoding> mDecoder not ready");
            return ErrCode.XERR_BAD_STATE;
        }

        if (mAudioDecodeEos) { // 音频帧的解码都完成了
            return ErrCode.XERR_CODEC_DEC_EOS;
        }


        //
        // 如果可以解码成功则反复进行操作，直到返回错误 或者 XERR_CODEC_DEC_EOS 或者 XERR_CODEC_INDATA
        // 解码后的数据输出会输出到 outFrame中
        //
        int ret = ErrCode.XOK;
        for (;;) {

            Pair<Integer, AvAudioFrame> pair = mDecoder.parserDecAudioFrame();
            ret = pair.first;
            AvAudioFrame audioFrame = pair.second;
            if (audioFrame != null) {
                mAudioQueue.inqueue(audioFrame);
                if (mInitParam.mCallback != null) {  // 回调解码出来的视频帧
                    mInitParam.mCallback.onAudioFrameDecoded(mMediaInfo, audioFrame);
                }
            }

            if (ret == ErrCode.XERR_CODEC_DECODING) { // 解码失败
                Log.e(TAG, "<doAudioDecoding> decoding error");
                if (mInitParam.mCallback != null) {  // 回调解码错误
                    mInitParam.mCallback.onDecodeError(mMediaInfo, ErrCode.XERR_CODEC_DECODING);
                }
                break;  // 跳出循环

            } else if (ret == ErrCode.XERR_CODEC_DEC_EOS) {  // 所有解码都已经完成
                Log.d(TAG, "<doAudioDecoding> decoding EOS done!");
                mAudioDecodeEos = true;
                break;  // 跳出循环

            } else if (ret < 0) {
                //Log.d(TAG, "<doAudioDecoding> error, ret=" + ret);
                break;  // 跳出循环

            } else { // 可以继续进行解码操作
            }
        }

        return ret;
    }


}