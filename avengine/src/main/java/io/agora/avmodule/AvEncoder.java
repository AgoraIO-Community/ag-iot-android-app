package io.agora.avmodule;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;
import java.nio.ByteBuffer;



public class AvEncoder {
    private final static int TIMEOUT_US = 10000;
    public static final int BUFFER_FLAG_SW_EOS = 256;       // 软解码的EOS空帧

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "AVMODULE/AvEncoder";

    //
    // 编码器的类型
    //
    public static final int ENCODER_TYPE_VIDEO = 0;
    public static final int ENCODER_TYPE_AUDIO = 1;



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private AvEncParam mInitParam;              ///< 编码器初始化参数
    private int mEncodeType = ENCODER_TYPE_VIDEO;   ///< 当前是音频编码还是视频编码
    private String mCodecType;                  ///< 编码器格式
    private MediaFormat mAvFormat = null;       ///< 音视频格式信息
    private MediaMuxer mAvMuxer;                ///< 混流器，由外部设置进来
    private int mAvTrackIndex = -1;             ///< 当前轨道索引，由外部设置进来

    private MediaCodec  mAvEncoder = null;      ///< 音视频编码器
    private ByteBuffer[] mInputBuffers;         ///< 编码输入缓冲区
    private ByteBuffer[] mOutputBuffers;        ///< 编码输出缓冲区
    private MediaFormat mOutputFormat = null;

    private long mEncodingVidTimestamp = 0;     ///< 当前编码视频帧时间戳
    private long mEncodingAudTimestamp = 0;     ///< 当前编码音频帧时间戳



    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////

    /**
     * @brief 初始化解码器
     * @param strMineType : 选择的mine type
     * @return error code
     */
    private static MediaCodecInfo SelectCodec(String strMineType) {
        int nNum = MediaCodecList.getCodecCount();
        for (int i = 0; i < nNum; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(strMineType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    public int initialize(AvEncParam initParam, int encodeType) {
        mInitParam = initParam;
        mEncodeType = encodeType;
        mOutputFormat = null;

        if (mEncodeType == ENCODER_TYPE_AUDIO) {
            setupAudioFormat();
        } else {
            setupVideoFormat();
        }

        mEncodingVidTimestamp = 0;
        mEncodingAudTimestamp = 0;

        try {
            mAvEncoder = MediaCodec.createEncoderByType(mCodecType);
            mAvEncoder.configure(mAvFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        } catch (IllegalStateException illegalExp) {
            illegalExp.printStackTrace();
            Log.e(TAG, "<initialize> [illegalEXCEPT] faile to configure()");
            return ErrCode.XERR_UNSUPPORTED;

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "<initialize> [EXCEPT] faile to configure()");
            return ErrCode.XERR_UNSUPPORTED;
        }

        try {
            mAvEncoder.start();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mInputBuffers = mAvEncoder.getInputBuffers();
                mOutputBuffers = mAvEncoder.getOutputBuffers();
                Log.i(TAG, "<initialize>: inputBufferSize=" + mInputBuffers.length
                        + ", outputBuffersSize=" + mOutputBuffers.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "<initialize> [EXCEPT] faile to start()");
            return ErrCode.XERR_UNSUPPORTED;
        }

        Log.d(TAG, "<initialize> END");
        return ErrCode.XERR_NONE;
    }

    /*
     * @brief 释放编码器
     */
    public void release()   {
        if (mAvEncoder != null) {
            try {
                mAvEncoder.stop();
                mAvEncoder.release();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                Log.e(TAG, "<release> [IllegalEXCEPT] excetpion of state: " + e);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "<release> [EXCEPT] exception of other: " + e);
            }
            mAvEncoder = null;
            Log.d(TAG, "<release> done");
        }

        mInputBuffers = null;
        mOutputBuffers = null;
        mOutputFormat = null;
    }

    /*
     * @brief 获取当前编码器输出音视频格式
     */
    public MediaFormat getOutputFormat() {
        return mOutputFormat;
    }

    /*
     * @brief 获取当前轨道索引
     */
    public int getTrackIndex() {
        return mAvTrackIndex;
    }

    /*
     * @brief 设置混流器
     */
    public void setMediaMuxer(MediaMuxer muxer) {
        mAvMuxer = muxer;
    }


    /*
     * @brief 送入要编码的帧数据
     * @param inputFrame : 要编码的帧数据，mLastFrame字段用来标记是否最后一帧
     * @return 错误码，0是成功
     */
    public int inputFrame(AvBaseFrame inputFrame) {
        if (mAvEncoder == null) {
            Log.e(TAG, "<inputFrame> bad state, mEncodeType=" + mEncodeType);
            return ErrCode.XERR_BAD_STATE;
        }
        if (inputFrame.mDataBuffer == null) {
            Log.d(TAG, "<inputFrame> mEncodeType=" + mEncodeType
                    + ", timestamp=" + inputFrame.mTimestamp
                    + ", flags=" + inputFrame.mFlags
                    + ", lastFrame=" + inputFrame.mLastFrame
                    + ", No data");

            if (mEncodeType == ENCODER_TYPE_VIDEO) {
                AvVideoFrame videoFrame = (AvVideoFrame)inputFrame;
                //inputFrame.mDataBuffer = new byte[videoFrame.mWidth*videoFrame.mHeight*3/2];
            }
        }
        else {
            Log.d(TAG, "<inputFrame> mEncodeType=" + mEncodeType
                    + ", timestamp=" + inputFrame.mTimestamp
                    + ", flags=" + inputFrame.mFlags
                    + ", lastFrame=" + inputFrame.mLastFrame
                    + ", frameLength=" + inputFrame.mDataBuffer.length);
        }

        int ret = ErrCode.XOK;
        int flags = 0;
        if (inputFrame.mLastFrame) {  // 是否送入最后一帧数据
            flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            if ((inputFrame.mFlags & BUFFER_FLAG_SW_EOS) != 0) {  // 软解码的EOS空帧
                flags += BUFFER_FLAG_SW_EOS;
            }
            ret = ErrCode.XERR_CODEC_DEC_EOS;
        }

        try {
            // 获取编码器输入缓冲区
            int inputBufferIndex = mAvEncoder.dequeueInputBuffer(TIMEOUT_US);
            if (inputBufferIndex < 0) {  // 当前没有空闲的编码输入缓冲区
                Log.e(TAG, "<inputFrame> " + mCodecType + " NO input buffer");
                return ErrCode.XERR_CODEC_NOBUFFER;
            }
            ByteBuffer inputBuffer;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                inputBuffer = (mInputBuffers != null) ? mInputBuffers[inputBufferIndex] : null;
            } else {
                inputBuffer = mAvEncoder.getInputBuffer(inputBufferIndex);
            }
            if (inputBuffer == null) {
                Log.e(TAG, "<inputFrame> " + mCodecType + " fail to get input buffer");
                return ErrCode.XERR_CODEC_NOBUFFER;
            }

            // 填充输入缓冲区，并且送入解码器进行解码
            if (inputFrame.mDataBuffer != null) {
                inputBuffer.clear();
                inputBuffer.put(inputFrame.mDataBuffer);
                mAvEncoder.queueInputBuffer(inputBufferIndex, 0, inputFrame.mDataBuffer.length,
                        inputFrame.mTimestamp, flags);
                Log.d(TAG, "<inputFrame> dataSize=" + inputFrame.mDataBuffer.length
                      + ", timestamp=" + inputFrame.mTimestamp
                    + ", inputBufferIndex=" + inputBufferIndex);
                inputFrame.mDataBuffer = null;

            } else {
                inputBuffer.clear();
                mAvEncoder.queueInputBuffer(inputBufferIndex, 0, 0, inputFrame.mTimestamp, flags);
            }

        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.e(TAG, "<inputFrame> [EXCEPT] illegalExcetpion: " + e.toString());
            ret = ErrCode.XERR_CODEC_INDATA;

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "<inputFrame> [EXCEPT] Exceptioin: " + e.toString());
            ret = ErrCode.XERR_CODEC_INDATA;
        }

        return ret;
    }

    /*
     * @brief 视频帧编码操作
     * @param None
     * @return 错误码，XERR_CODEC_DEC_EOS 表示编码是最后一帧了，后续没有要编码的数据了
     */
    public int encodeVideoFrame() {
        int ret = ErrCode.XOK;

        try {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mAvEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
            switch (outputBufferIndex) {
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED: {
                    Log.d(TAG, "<encodeVideoFrame> FORMAT_CHANGED");
                    mOutputFormat = mAvEncoder.getOutputFormat();
                    mAvTrackIndex = mAvMuxer.addTrack(mOutputFormat);
                    ret = ErrCode.XERR_CODEC_OUTFMT_READY;  // 输出格式就绪
                } break;

                case MediaCodec.INFO_TRY_AGAIN_LATER: // 当前没有解码数据输出，需要继续解码
                    Log.d(TAG, "<encodeVideoFrame> TRY_AGAIN_LATER");
                    ret = ErrCode.XERR_CODEC_MOREINDATA;    // 需要更多数据
                    break;

                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d(TAG, "<encodeVideoFrame> BUFFERS_CHANGED");
                    mOutputBuffers = mAvEncoder.getOutputBuffers();
                    ret = ErrCode.XERR_CODEC_MOREINDATA;    // 需要更多数据
                    break;

                default: {
                    // 获取编码后的数据
                    ByteBuffer outputBuffer;
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        outputBuffer = (mOutputBuffers != null) ? mOutputBuffers[outputBufferIndex] : null;
                    } else {
                        outputBuffer = mAvEncoder.getOutputBuffer(outputBufferIndex);
                    }
                    if (outputBuffer == null) {
                        Log.e(TAG, "<encodeVideoFrame> outputBuffer is NULL");
                        return ErrCode.XERR_CODEC_MOREINDATA;
                    }
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    Log.d(TAG, "<encodeVideoFrame> offset=" + bufferInfo.offset
                            + ", size=" + bufferInfo.size
                            + ", timestamp=" + bufferInfo.presentationTimeUs
                            + ", flags=" + bufferInfo.flags     );

                    // 编码后的视频帧通过混流器写入文件
                    if (bufferInfo.size >= 0 && bufferInfo.presentationTimeUs >= 0) {
                        if ((bufferInfo.flags & BUFFER_FLAG_SW_EOS) != 0) {  // 软解码的EOS空帧
                            Log.d(TAG, "<encodeVideoFrame> SW decoder EOS frame");
                        } else {
                            mAvMuxer.writeSampleData(mAvTrackIndex, outputBuffer, bufferInfo);
                        }
                    }

                    if ((bufferInfo.presentationTimeUs >= 0) && (bufferInfo.size > 0)) {
                        mEncodingVidTimestamp = bufferInfo.presentationTimeUs;
                    }
                    mAvEncoder.releaseOutputBuffer(outputBufferIndex, false);
                    Log.d(TAG, "<encodeVideoFrame> encodeSize=" + bufferInfo.size
                            + ", timestamp=" + bufferInfo.presentationTimeUs
                            + ", flags=" + bufferInfo.flags);

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {   // 解码缓冲区完成
                        Log.d(TAG, "<encodeVideoFrame> BUFFER_FLAG_EOS");
                        ret = ErrCode.XERR_CODEC_DEC_EOS;
                    } else {
                        ret = ErrCode.XOK;
                    }
                }  break;
            }

        } catch (IllegalArgumentException agrueExpt) {
            agrueExpt.printStackTrace();
            Log.e(TAG, "<encodeVideoFrame> [ArgueExpt] agrueExpt=" + agrueExpt.toString());
            return ErrCode.XERR_CODEC_ENCODING;

        } catch (IllegalStateException stateExpt) {
            stateExpt.printStackTrace();
            Log.e(TAG, "<encodeVideoFrame> [StateExpt] stateExpt=" + stateExpt.toString());
            return ErrCode.XERR_CODEC_ENCODING;

        } catch (Exception except) {
            except.printStackTrace();
            Log.e(TAG, "<encodeVideoFrame> [EXPT] except=" + except.toString());
            return ErrCode.XERR_CODEC_ENCODING;
        }

        return ret;
    }

    /*
     * @brief 音频帧解码操作，仅对音频流轨道有效
     * @param None
     * @return Integer：错误码，XERR_CODEC_DEC_EOS 表示解码是最后一帧了，后续没有要解码数据了
     */
    public int encodeAudioFrame() {
        int ret = ErrCode.XOK;

        try {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mAvEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
            switch (outputBufferIndex) {
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED: {
                    Log.d(TAG, "<encodeAudioFrame> FORMAT_CHANGED");
                    mOutputFormat = mAvEncoder.getOutputFormat();
                    mAvTrackIndex = mAvMuxer.addTrack(mOutputFormat);
                    ret = ErrCode.XERR_CODEC_OUTFMT_READY;  // 输出格式就绪
                } break;

                case MediaCodec.INFO_TRY_AGAIN_LATER: // 当前没有解码数据输出，需要继续解码
                    Log.d(TAG, "<encodeAudioFrame> TRY_AGAIN_LATER");
                    ret = ErrCode.XERR_CODEC_MOREINDATA;    // 需要更多数据
                    break;

                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d(TAG, "<encodeAudioFrame> BUFFERS_CHANGED");
                    mOutputBuffers = mAvEncoder.getOutputBuffers();
                    ret = ErrCode.XERR_CODEC_MOREINDATA;    // 需要更多数据
                    break;

                default: {
                    // 获取解码后的数据
                    ByteBuffer outputBuffer;
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        outputBuffer = (mOutputBuffers != null) ? mOutputBuffers[outputBufferIndex] : null;
                    } else {
                        outputBuffer = mAvEncoder.getOutputBuffer(outputBufferIndex);
                    }
                    Log.d(TAG, "<encodeAudioFrame> offset=" + bufferInfo.offset
                            + ", size=" + bufferInfo.size
                            + ", timestamp=" + bufferInfo.presentationTimeUs
                            + ", flags=" + bufferInfo.flags     );

                    // 编码后的视频帧通过混流器写入文件
                    if (bufferInfo.size >= 0 && bufferInfo.presentationTimeUs >= 0) {
                        if ((bufferInfo.flags & BUFFER_FLAG_SW_EOS) != 0) {  // 软解码的EOS空帧
                            Log.d(TAG, "<encodeAudioFrame> SW decoder EOS frame");
                        } else {
                            mAvMuxer.writeSampleData(mAvTrackIndex, outputBuffer, bufferInfo);
                        }
                    }

                    if ((bufferInfo.presentationTimeUs >= 0) && (bufferInfo.size > 0)) {
                        mEncodingAudTimestamp = bufferInfo.presentationTimeUs;
                    }
                    mAvEncoder.releaseOutputBuffer(outputBufferIndex, false);
                    Log.d(TAG, "<encodeAudioFrame> encodeSize=" + bufferInfo.size
                            + ", timestamp=" + bufferInfo.presentationTimeUs
                            + ", flags=" + bufferInfo.flags);

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {   // 解码缓冲区完成
                        Log.d(TAG, "<encodeAudioFrame> BUFFER_FLAG_EOS");
                        ret = ErrCode.XERR_CODEC_DEC_EOS;
                    } else {
                        ret = ErrCode.XOK;
                    }
                }  break;
            }

        } catch (IllegalArgumentException agrueExpt) {
            agrueExpt.printStackTrace();
            Log.e(TAG, "<encodeAudioFrame> [ARGUEEXPT] agrueExpt=" + agrueExpt.toString());
            return ErrCode.XERR_CODEC_ENCODING;

        } catch (IllegalStateException stateExpt) {
            stateExpt.printStackTrace();
            Log.e(TAG, "<encodeAudioFrame> [STATEEXPT] stateExpt=" + stateExpt.toString());
            return ErrCode.XERR_CODEC_ENCODING;

        } catch (Exception except) {
            except.printStackTrace();
            Log.e(TAG, "<encodeAudioFrame> except=" + except.toString());
            return ErrCode.XERR_CODEC_ENCODING;
        }

        return ret;
    }

    long getVideoTimestamp() {
        return mEncodingVidTimestamp;
    }

    long getAudioTimestamp() {
        return mEncodingAudTimestamp;
    }

    /////////////////////////////////////////////////////////////////////////
    ////////////////////////// Internal Methods /////////////////////////////
    /////////////////////////////////////////////////////////////////////////

    /*
     * @brief 设置视频编码器参数
     */
    boolean setupVideoFormat() {
        mCodecType = mInitParam.mVideoCodec;
        Log.d(TAG, "<setupVideoFormat> codecType=" + mInitParam.mVideoCodec
                + ", wdith=" + mInitParam.mVideoWidth
                + ", height=" + mInitParam.mVideoHeight
                + ", colorFormat=" + mInitParam.mColorFormat
                + ", frameRate=" + mInitParam.mFrameRate
                + ", gopFrames=" + mInitParam.mGopFrame
                + ", rotation=" + mInitParam.mRotation
                + ", videoBitrate=" + mInitParam.mVideoBitRate
                + ", bitrateMode=" + mInitParam.mVBitRateMode        );

        try {
            mAvFormat = MediaFormat.createVideoFormat(mInitParam.mVideoCodec,
                    mInitParam.mVideoWidth, mInitParam.mVideoHeight);

            mAvFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mInitParam.mColorFormat); // 色彩格式
            mAvFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mInitParam.mFrameRate); // 帧率
            int intervalSeconds = (mInitParam.mGopFrame / mInitParam.mFrameRate);
            mAvFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, intervalSeconds);  // 关键帧间隔秒数
            mAvFormat.setInteger(MediaFormat.KEY_BIT_RATE, mInitParam.mVideoBitRate);  // 码率
            mAvFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, mInitParam.mVBitRateMode);  // 码率模式

        } catch (Exception exp) {
            exp.printStackTrace();
            Log.e(TAG, "<setupVideoFormat> [ERROR] exp=" + exp.toString());
            return false;
        }

        return true;
    }

    /*
     * @brief 设置音频编码器参数
     */
    boolean setupAudioFormat() {
        mCodecType = mInitParam.mAudioCodec;
        int minBufSize = 0;
        try {
            mAvFormat = MediaFormat.createAudioFormat(mInitParam.mAudioCodec,
                    mInitParam.mSampleRate, mInitParam.mChannels);

            mAvFormat.setInteger(MediaFormat.KEY_BIT_RATE, mInitParam.mAudioBitRate);
            minBufSize = AudioRecord.getMinBufferSize(mInitParam.mSampleRate,
                    mInitParam.mChannels, mInitParam.mSampleFmt);
            mAvFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufSize * 4);

        } catch (Exception exp) {
            exp.printStackTrace();
            Log.e(TAG, "<setupAudioFormat> [ERROR] exp=" + exp.toString());
            return false;
        }

        Log.d(TAG, "<setupAudioFormat> codecType=" + mInitParam.mAudioCodec
                + ", mSampleRate=" + mInitParam.mSampleRate
                + ", mChannels=" + mInitParam.mChannels
                + ", audioBitrate=" + mInitParam.mAudioBitRate
                + ", minBufSize=" + minBufSize       );
        return true;
    }



}