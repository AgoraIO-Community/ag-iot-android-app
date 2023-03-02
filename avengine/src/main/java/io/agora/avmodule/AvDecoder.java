package io.agora.avmodule;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;



public class AvDecoder {
    private final static int TIMEOUT_US = 10000;




    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "AVMODULE/AvDecoder";

    //
    // 当前处理的流的类型
    //
    public static final int STREAM_TYPE_VIDEO = 0;
    public static final int STREAM_TYPE_AUDIO = 1;



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private Context mContext;
    private Uri mFileUri;                       ///< 文件Uri
    private String mFilePath;                   ///< 文件名
    private int mStreamType = STREAM_TYPE_VIDEO;   ///< 当前是处理视频流还是音频流
    private String mMimeType;                   ///< 根据 mDecStreamType 来区分

    private MediaExtractor mExtractor = null;   ///< 码流解析器
    private MediaFormat mAvFormat = null;       ///< 从媒体信息解析出来的音视频格式信息
    private MediaFormat mOutputFormat = null;   ///< 从数据流解析出来的音视频信息，与mAvForamt可能会不一样
    private MediaCodec  mAvDecoder = null;      ///< 音视频解码器
    private ByteBuffer[] mInputBuffers;         ///< 解码输入缓冲区
    private ByteBuffer[] mOutputBuffers;        ///< 解码输出缓冲区
    private int mAvTrackIndex = -1;             ///< 流轨道索引
    private int mFrameIndex = 0;                ///< 解码的帧索引
    private AvMediaInfo mMediaInfo = new AvMediaInfo();
    private String mStorageRootPath;

    //
    // 仅对视频流解码有效
    //
    private int mColorFormat;           ///< 输出视频帧色彩格式
    private int mDataWidth = 0;        ///< 输出视频帧数据原始宽度
    private int mDataHeight = 0;       ///< 输出视频帧数据原始高度
    private int mStrideWidth = 0;      ///< 输出视频帧数据行长，如果没有则使用原始宽度
    private int mStrideHeight = 0;     ///< 输出视频帧数据高度，如果没有则使用原始高度
    private Rect mFrameCrop = new Rect();   ///< 输出视频帧裁剪区域（基于原始数据大小）
    private int mDisplayWidth = 0;      ///< 显示的视频帧高度
    private int mDisplayHeight= 0;      ///< 显示的适配帧宽度
    private int mRotation = 0;
    private Bitmap mOriginalBmp;        ///< 存放原始YUV数据转换后的图像，宽高(mDataWidth,mDataHeight)
    private Matrix mRotMatrix;          ///< 旋转变换矩阵

    //
    // 仅对音频流解码有效
    //
    private int mSapmleFormat = AudioFormat.ENCODING_PCM_16BIT;  ///< 采样格式
    private int mBytesPerSample = 2;            ///< 每个采样的字节数
    private int mChannels = 2;                  ///< 音频通道数，Android固定只支持双通道
    private int mSampleRate = 48000;            ///< 采样率


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////

    /*
     * @brief 初始化解码器
     * @param filePath : 要解码的文件路径
     * @param audioDecoder : 是否解码音频流，还是视频流
     * @return error code
     */
    public int initialize(Context ctx, Uri fileUri, String filePath, int decStreamType) {
        mStorageRootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        mContext = ctx;
        mFileUri = fileUri;
        mFilePath = filePath;
        mStreamType = decStreamType;
        if (mStreamType == STREAM_TYPE_AUDIO) {
            mMimeType = GlobalConst.MIME_TYPE_AUDIO;
        } else {
            mMimeType = GlobalConst.MIME_TYPE_VIDEO;
        }

        int ret = open();
        if (ret != ErrCode.XOK) {
            Log.e(TAG, "<initialize> fail to open()");
            return ret;
        }

        try {
            //
            // 获取 音频/视频 流基本信息
            //
            if (mStreamType == STREAM_TYPE_AUDIO) {
                mSapmleFormat = AudioFormat.ENCODING_PCM_16BIT;
                mBytesPerSample = 2;
                mChannels = mAvFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                mSampleRate = mAvFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);

            } else {
                getVideoInfoFromOpen(mAvFormat);    // 从打开的媒体格式获取视频帧信息
            }

            //
           // 创建相应的解码器
            //
            mAvDecoder = MediaCodec.createDecoderByType(mAvFormat.getString(MediaFormat.KEY_MIME));
            //mAvFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            mAvDecoder.configure(mAvFormat, null, null, 0);
            mAvDecoder.start();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mInputBuffers = mAvDecoder.getInputBuffers();
                mOutputBuffers = mAvDecoder.getOutputBuffers();
                Log.i(TAG, "<initialize>: inputBufferSize=" + mInputBuffers.length
                        + ", outputBuffersSize=" + mOutputBuffers.length);
            }

        } catch (IOException ioExcept) {
            ioExcept.printStackTrace();
            Log.e(TAG, "<initialize> IOException");
            release();
            return ErrCode.XERR_CODEC_OPEN;

        } catch (Exception except) {
            except.printStackTrace();
            Log.e(TAG, "<initialize> Exception");
            release();
            return ErrCode.XERR_CODEC_OPEN;
        }

        Log.d(TAG, "<initialize> done");
        return ErrCode.XOK;
    }

    /*
     * @brief 释放解码器
     */
    public void release()   {
        if (mAvDecoder != null) {
            try {
                mAvDecoder.stop();
            } catch (IllegalStateException illegalExp) {
                illegalExp.printStackTrace();
            }
            mAvDecoder.release();
            mAvDecoder = null;
            Log.d(TAG, "<release> done");
        }

    }

    /*
     * @brief 获取当前流的媒体文件信息
     */
    public AvMediaInfo getMediaInfo()   {
        return mMediaInfo;
    }

    /*
     * @brief 获取当前流的格式信息
     */
    public MediaFormat getMediaFormat()   {
        return mAvFormat;
    }


    /*
     * @brief 送入解码帧数据
     * @param None
     * @return 错误代码, XERR_CODEC_DEC_EOS 表示已经送入最后一帧数据，后续没有数据送入了
     */
    public int inputFrame() {
        int ret = ErrCode.XOK;
        try {
            // 获取解码器输入缓冲区
            int inputBufferIndex = mAvDecoder.dequeueInputBuffer(TIMEOUT_US);
            if (inputBufferIndex < 0) {  // 当前没有空闲的解码输入缓冲区
                //Log.e(TAG, "<inputFrame> " + mMimeType + " NO input buffer");
                return ErrCode.XERR_CODEC_NOBUFFER;
            }
            ByteBuffer inputBuffer;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                inputBuffer = (mInputBuffers != null) ? mInputBuffers[inputBufferIndex] : null;
            } else {
                inputBuffer = mAvDecoder.getInputBuffer(inputBufferIndex);
            }
            if (inputBuffer == null) {
                Log.e(TAG, "<inputFrame> " + mMimeType + " fail to get input buffer");
                return ErrCode.XERR_CODEC_NOBUFFER;
            }

            // 通过Extractor读取一帧数据到输入缓冲区，并且送入解码器进行解码
            int sampleSize = mExtractor.readSampleData(inputBuffer, 0);
            long timestamp = mExtractor.getSampleTime();
            if (sampleSize >= 0) {
                boolean haveNext = mExtractor.advance();    // 驱动读取下一帧
                if (!haveNext) {   // 当前已经是最后一帧了
                    mAvDecoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, timestamp,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    ret = ErrCode.XERR_CODEC_DEC_EOS;
                } else {
                    mAvDecoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, timestamp, 0);
                }
            } else {
                // 直接更新解码缓冲区
                mAvDecoder.queueInputBuffer(inputBufferIndex, 0, 0, 0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }
//            Log.d(TAG, "<inputFrame> sampleSize=" + sampleSize + ", timestamp=" + timestamp
//                + ", inputBufferIndex=" + inputBufferIndex);

        } catch (Exception except) {
            except.printStackTrace();
            Log.e(TAG, "<inputFrame> " + mMimeType + " except=" + except.toString());
            return ErrCode.XERR_CODEC_DECODING;
        }

        return ret;
    }

    /*
     * @brief 视频帧解码操作，仅对视频流轨道有效
     * @param None
     * @return Integer：错误码，XERR_CODEC_DEC_EOS 表示解码是最后一帧了，后续没有要解码数据了
     *         AvVideoFrame : 返回解码后的视频帧
     */
    public Pair<Integer, AvVideoFrame> decodeVideoFrame() {
        AvVideoFrame outFrame = null;
        int ret = ErrCode.XOK;

        try {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mAvDecoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
            switch (outputBufferIndex) {
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    mOutputFormat = mAvDecoder.getOutputFormat();
                    getVideoInfoFromOut(mOutputFormat);    // 从实际解码处理的媒体格式获取视频帧信息
                    ret = ErrCode.XERR_CODEC_MOREINDATA;  // 需要更多数据
                    break;

                case MediaCodec.INFO_TRY_AGAIN_LATER: // 当前没有解码数据输出，需要继续解码
                    //Log.d(TAG, "<decodeVideoFrame> TRY_AGAIN_LATER");
                    ret = ErrCode.XERR_CODEC_MOREINDATA;    // 需要更多数据
                    break;

                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d(TAG, "<decodeVideoFrame> BUFFERS_CHANGED");
                    mOutputBuffers = mAvDecoder.getOutputBuffers();
                    ret = ErrCode.XERR_CODEC_MOREINDATA;    // 需要更多数据
                    break;

                default: {
                    // 获取解码后的数据
                    ByteBuffer outputBuffer;
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        outputBuffer = (mOutputBuffers != null) ? mOutputBuffers[outputBufferIndex] : null;
                    } else {
                        outputBuffer = mAvDecoder.getOutputBuffer(outputBufferIndex);
                    }
                    if (outputBuffer == null) {
                        Log.e(TAG, "<decodeVideoFrame> outputBuffer is NULL");
                        return (new Pair(ErrCode.XERR_CODEC_MOREINDATA, null));
                    }
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                    if (bufferInfo.size > 0) {
                        outFrame = new AvVideoFrame();
                        outFrame.mDataBuffer = new byte[bufferInfo.size];
                        outputBuffer.get(outFrame.mDataBuffer, 0, bufferInfo.size);
                        outFrame.mFrameBmp = convertYuvToBitmap(outFrame.mDataBuffer);  // YUV==>Bitmap
                        outFrame.mDataBuffer = null;  // 用完即释放缓解内存
                    } else {
                        outFrame = new AvVideoFrame();
                    }
                    outFrame.mFrameIndex = mFrameIndex++;
                    outFrame.mTimestamp = bufferInfo.presentationTimeUs;
                    outFrame.mFlags = bufferInfo.flags;
                    outFrame.mColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_Format32bitARGB8888;
                    if (outFrame.mFrameBmp != null) {
                        outFrame.mWidth = outFrame.mFrameBmp.getWidth();
                        outFrame.mHeight = outFrame.mFrameBmp.getHeight();
                    }

                    // 这里直接将解码后的YUV数据转换成ARGB格式
                    mAvDecoder.releaseOutputBuffer(outputBufferIndex, false);
                    Log.d(TAG, "<decodeVideoFrame> decodedSize=" + bufferInfo.size
                            + ", mTimestamp=" + outFrame.mTimestamp
                            + ", mFlags=" + outFrame.mFlags);

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {   // 判断帧类型
                        outFrame.mKeyFrame = true;
                    }

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {   // 解码缓冲区完成
                        Log.d(TAG, "<decodeVideoFrame> BUFFER_FLAG_EOS");
                        ret = ErrCode.XERR_CODEC_DEC_EOS;
                        outFrame.mLastFrame = true;
                    } else {
                        ret = ErrCode.XOK;
                    }
                }  break;
            }

        } catch (Exception except) {
            except.printStackTrace();
            Log.e(TAG, "<decodeVideoFrame> except=" + except.toString());
            return (new Pair(ErrCode.XERR_CODEC_DECODING, null));
        }

        return (new Pair(ret, outFrame));
    }

    /*
     * @brief 音频帧解码操作，仅对音频流轨道有效
     * @param None
     * @return Integer：错误码，XERR_CODEC_DEC_EOS 表示解码是最后一帧了，后续没有要解码数据了
     *         AvAudioFrame : 返回解码后的音频帧
     */
    public Pair<Integer, AvAudioFrame> decodeAudioFrame() {
        AvAudioFrame outFrame = null;
        int ret = ErrCode.XOK;

        try {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mAvDecoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
            switch (outputBufferIndex) {
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    Log.d(TAG, "<decodeAudioFrame> FORMAT_CHANGED");
                    ret = ErrCode.XERR_CODEC_MOREINDATA;  // 需要更多数据
                    break;

                case MediaCodec.INFO_TRY_AGAIN_LATER: // 当前没有解码数据输出，需要继续解码
                    //Log.d(TAG, "<decodeAudioFrame> TRY_AGAIN_LATER");
                    ret = ErrCode.XERR_CODEC_MOREINDATA;    // 需要更多数据
                    break;

                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d(TAG, "<decodeAudioFrame> BUFFERS_CHANGED");
                    mOutputBuffers = mAvDecoder.getOutputBuffers();
                    ret = ErrCode.XERR_CODEC_MOREINDATA;    // 需要更多数据
                    break;

                default: {
                    // 获取解码后的数据
                    ByteBuffer outputBuffer;
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        outputBuffer = (mOutputBuffers != null) ? mOutputBuffers[outputBufferIndex] : null;
                    } else {
                        outputBuffer = mAvDecoder.getOutputBuffer(outputBufferIndex);
                    }
                    if (outputBuffer == null) {
                        Log.e(TAG, "<decodeAudioFrame> outputBuffer is NULL");
                        return (new Pair(ErrCode.XERR_CODEC_MOREINDATA, null));
                    }
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                    if (bufferInfo.size > 0)  {
                        outFrame = new AvAudioFrame();
                        outFrame.mDataBuffer = new byte[bufferInfo.size];
                        outputBuffer.get(outFrame.mDataBuffer, 0, bufferInfo.size);
                    } else {
                        outFrame = new AvAudioFrame();
                        Log.d(TAG, "<decodeAudioFrame> No audio data is decoded!");
                    }
                    outFrame.mFrameIndex = mFrameIndex++;
                    outFrame.mTimestamp = bufferInfo.presentationTimeUs;
                    outFrame.mFlags = bufferInfo.flags;
                    outFrame.mSampleFormat = mSapmleFormat;
                    outFrame.mBytesPerSample = mBytesPerSample;
                    outFrame.mChannels = mChannels;
                    outFrame.mSampleRate = mSampleRate;
                    mAvDecoder.releaseOutputBuffer(outputBufferIndex, false);
                    Log.d(TAG, "<decodeAudioFrame> decodedSize=" + bufferInfo.size
                            + ", mTimestamp=" + outFrame.mTimestamp
                            + ", mFlags=" + outFrame.mFlags);

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {   // 判断帧类型
                        outFrame.mKeyFrame = true;
                    }

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {   // 解码缓冲区完成
                        Log.d(TAG, "<decodeAudioFrame> BUFFER_FLAG_EOS");
                         ret = ErrCode.XERR_CODEC_DEC_EOS;
                         outFrame.mLastFrame = true;
                    } else {
                        ret = ErrCode.XOK;
                    }
                }  break;
            }

        } catch (Exception except) {
            except.printStackTrace();
            Log.e(TAG, "<decodeAudioFrame> except=" + except.toString());
            return (new Pair(ErrCode.XERR_CODEC_DECODING, null));
        }

        return (new Pair(ret, outFrame));
    }


    /////////////////////////////////////////////////////////////////////////
    ////////////////////////// Internal Methods /////////////////////////////
    /////////////////////////////////////////////////////////////////////////
    /*
     * @brief 打开媒体文件，解析文信息
     * @retrun error code, 0表示成功
     */
    private int open()  {
        Log.d(TAG, "<open> BEGIN, file=" + mFilePath);
        mAvTrackIndex = -1;
        mAvFormat = null;

        try {
            int ret;
            mExtractor = new MediaExtractor();
            if ((mFilePath != null) && (mFilePath.length() > 0)) {
                mExtractor.setDataSource(mFilePath);
            } else {
                mExtractor.setDataSource(mContext, mFileUri, null);
            }

            // 查询视频轨道信息
            int trackCount = mExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mExtractor.getTrackFormat(i);
                if (trackFormat.getString(MediaFormat.KEY_MIME).startsWith(mMimeType)) {
                    mAvTrackIndex = i;
                    mAvFormat = trackFormat;
                    break;  // 只找第一个对应的流轨道
                }
            }

            if (mAvTrackIndex < 0) {  // 可以找到相应的轨道
                Log.e(TAG, "<open> [ERROR] cannot found stream track, mMimeType=" + mMimeType);
                close();
                return ErrCode.XERR_FILE_OPEN;
            }

            // 获取媒体文件相关信息
            if (mStreamType == STREAM_TYPE_VIDEO) {    // 视频流轨道获取视频信息
                mMediaInfo.mVideoTrackId = mAvTrackIndex;
                mMediaInfo.mVideoCodec = mAvFormat.getString(MediaFormat.KEY_MIME);
                mMediaInfo.mColorFormat = AvUtility.getAvMediaInteger(mAvFormat, MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
                mMediaInfo.mDataWidth = AvUtility.getAvMediaInteger(mAvFormat, MediaFormat.KEY_WIDTH, -1);
                mMediaInfo.mDataHeight = AvUtility.getAvMediaInteger(mAvFormat, MediaFormat.KEY_HEIGHT, -1);
                mMediaInfo.mDisplayWidth = AvUtility.getAvMediaInteger(mAvFormat, GlobalConst.KEY_DISPLAY_WIDTH, -1);
                mMediaInfo.mDisplayHeight = AvUtility.getAvMediaInteger(mAvFormat, GlobalConst.KEY_DISPLAY_HEIGHT, -1);
                mMediaInfo.mVideoWidth = (mMediaInfo.mDataWidth > 0) ? mMediaInfo.mDataWidth : mMediaInfo.mDisplayWidth;
                mMediaInfo.mVideoHeight = (mMediaInfo.mDataHeight > 0) ? mMediaInfo.mDataHeight: mMediaInfo.mDisplayHeight;
                mMediaInfo.mRotation = AvUtility.getAvMediaInteger(mAvFormat, MediaFormat.KEY_ROTATION, 0);
                mMediaInfo.mFrameRate = AvUtility.getAvMediaInteger(mAvFormat, MediaFormat.KEY_FRAME_RATE, 30);
                mMediaInfo.mVideoBitrate = AvUtility.getAvMediaInteger(mAvFormat, MediaFormat.KEY_BIT_RATE, 0);
                mMediaInfo.mVideoMaxBitrate = AvUtility.getAvMediaInteger(mAvFormat, GlobalConst.KEY_MAX_BIT_RATE, 0);
                mMediaInfo.mVideoDuration = mAvFormat.getLong(MediaFormat.KEY_DURATION);
            }

            if  (mStreamType == STREAM_TYPE_AUDIO) {   // 音频流轨道获取音频流信息
                mMediaInfo.mAudioTrackId = mAvTrackIndex;
                mMediaInfo.mAudioCodec = mAvFormat.getString(MediaFormat.KEY_MIME);
                mMediaInfo.mSampleFmt = AvUtility.getAvMediaInteger(mAvFormat, MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT);
                mMediaInfo.mChannels = AvUtility.getAvMediaInteger(mAvFormat, MediaFormat.KEY_CHANNEL_COUNT, 2);
                mMediaInfo.mSampleRate = AvUtility.getAvMediaInteger(mAvFormat, MediaFormat.KEY_SAMPLE_RATE, 44100);
                mMediaInfo.mAudioBitrate = AvUtility.getAvMediaInteger(mAvFormat, MediaFormat.KEY_BIT_RATE, 0);
                mMediaInfo.mAudioMaxBitrate = AvUtility.getAvMediaInteger(mAvFormat, GlobalConst.KEY_MAX_BIT_RATE, 0);
                mMediaInfo.mAudioDuration = mAvFormat.getLong(MediaFormat.KEY_DURATION);
            }
            mMediaInfo.mFilePath = mFilePath;
            mMediaInfo.mContext = mContext;
            mMediaInfo.mFileUri = mFileUri;
            if (mMediaInfo.mVideoDuration > mMediaInfo.mAudioDuration) {
                mMediaInfo.mFileDuration = mMediaInfo.mVideoDuration;
            } else {
                mMediaInfo.mFileDuration = mMediaInfo.mAudioDuration;
            }

            mExtractor.selectTrack(mAvTrackIndex);


        } catch (IOException ioExcept) {
            ioExcept.printStackTrace();
            Log.e(TAG, "<open> IOException");
            mExtractor = null;
            mFilePath = "";
            mAvTrackIndex = -1;
            mAvFormat = null;
            mMediaInfo = null;
            return ErrCode.XERR_FILE_OPEN;

        } catch (Exception except) {
            except.printStackTrace();
            Log.e(TAG, "<open> Exception");
            mExtractor = null;
            mFilePath = "";
            mAvTrackIndex = -1;
            mAvFormat = null;
            mMediaInfo = null;
            return ErrCode.XERR_FILE_OPEN;
        }

        Log.d(TAG, "<open> END");
        return ErrCode.XERR_NONE;
    }


    /*
     * @brief 关闭媒体文件，释放所有解码器和相关资源
     * @param None
     * @retrun error code, 0表示成功
     */
    private int close()  {
        if (mExtractor != null) {   // 关闭码流解析器
            mExtractor.unselectTrack(mAvTrackIndex);
            mExtractor.release();
            mExtractor = null;
            Log.d(TAG, "<close> done");
        }

        mFilePath = "";
        mAvTrackIndex = -1;
        mAvFormat = null;
        mMediaInfo = null;

        return ErrCode.XOK;
    }


    /*
     * @brief 从打开后的媒体格式提取视频帧信息
     *       重点是 原始数据宽高、显示宽高、裁剪区域等
     */
    private void getVideoInfoFromOpen(MediaFormat avFmt) {

        mColorFormat = AvUtility.getAvMediaInteger(avFmt, MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);

        // 视频帧原始数据宽高
        mDataWidth = AvUtility.getAvMediaInteger(avFmt, MediaFormat.KEY_WIDTH, 0);
        mDataHeight = AvUtility.getAvMediaInteger(avFmt, MediaFormat.KEY_HEIGHT, 0);
        mStrideWidth = AvUtility.getAvMediaInteger(avFmt, MediaFormat.KEY_STRIDE, mDataWidth);
        mStrideHeight = AvUtility.getAvMediaInteger(avFmt, MediaFormat.KEY_SLICE_HEIGHT, mDataHeight);

        // 显示宽高如果没有提取到，默认就是原始数据宽高
        mDisplayWidth = AvUtility.getAvMediaInteger(avFmt, GlobalConst.KEY_DISPLAY_WIDTH, mDataWidth);
        mDisplayHeight = AvUtility.getAvMediaInteger(avFmt, GlobalConst.KEY_DISPLAY_HEIGHT, mDataHeight);

        // 旋转角度如果没有提取到，默认就是0度，不旋转
        mRotation = AvUtility.getAvMediaInteger(avFmt, MediaFormat.KEY_ROTATION, 0);

        // 裁剪区域如果没有提取到，默认就是原始数据全部
        mFrameCrop.left = AvUtility.getAvMediaInteger(avFmt, GlobalConst.KEY_CROP_LEFT, 0);
        mFrameCrop.right = AvUtility.getAvMediaInteger(avFmt, GlobalConst.KEY_CROP_RIGHT, mDataWidth-1);
        mFrameCrop.top = AvUtility.getAvMediaInteger(avFmt, GlobalConst.KEY_CROP_TOP, 0);
        mFrameCrop.bottom = AvUtility.getAvMediaInteger(avFmt, GlobalConst.KEY_CROP_BOTTOM, mDataHeight-1);

        Log.d(TAG, "<getVideoInfoFromOpen> colorFmt=" + mColorFormat
                + ", wdith=" + mDataWidth + ", height=" + mDataHeight
                + ", mStrideWidth=" + mStrideWidth + ", mStrideHeigh=" + mStrideHeight
                + ", displayWidth=" + mDisplayWidth + ", displayHeight=" + mDisplayHeight
                + ", rotation=" + mRotation
                + ", cropLeft=" + mFrameCrop.left + ", cropRight=" + mFrameCrop.right
                + ", cropTop=" + mFrameCrop.top + ", cropBottom=" + mFrameCrop.bottom      );
    }


    /*
     * @brief 从解析后的媒体格式提取视频帧信息（注意：有些信息之前已经提取到了，就保留旧值）
     *       重点是 原始数据宽高、显示宽高、裁剪区域等
     */
    private void getVideoInfoFromOut(MediaFormat outAvFmt) {
        // 色彩格式，如果没有提取到则保留旧值
        mColorFormat = AvUtility.getAvMediaInteger(outAvFmt, MediaFormat.KEY_COLOR_FORMAT, mColorFormat);

        // 视频帧原始数据宽高
        mDataWidth = AvUtility.getAvMediaInteger(outAvFmt, MediaFormat.KEY_WIDTH, mDataWidth);
        mDataHeight = AvUtility.getAvMediaInteger(outAvFmt, MediaFormat.KEY_HEIGHT, mDataHeight);
        mStrideWidth = AvUtility.getAvMediaInteger(outAvFmt, MediaFormat.KEY_STRIDE, mStrideWidth);
        mStrideHeight = AvUtility.getAvMediaInteger(outAvFmt, MediaFormat.KEY_SLICE_HEIGHT, mStrideHeight);


        // 显示宽高如果没有提取到，默认保留旧值
        mDisplayWidth = AvUtility.getAvMediaInteger(outAvFmt, GlobalConst.KEY_DISPLAY_WIDTH, mDisplayWidth);
        mDisplayHeight = AvUtility.getAvMediaInteger(outAvFmt, GlobalConst.KEY_DISPLAY_HEIGHT, mDisplayHeight);

        // 旋转角度如果没有提取到，默认保留旧值
        mRotation = AvUtility.getAvMediaInteger(outAvFmt, MediaFormat.KEY_ROTATION, mRotation);

        // 裁剪区域如果没有提取到，默认保留旧值
        mFrameCrop.left = AvUtility.getAvMediaInteger(outAvFmt, GlobalConst.KEY_CROP_LEFT, mFrameCrop.left);
        mFrameCrop.right = AvUtility.getAvMediaInteger(outAvFmt, GlobalConst.KEY_CROP_RIGHT, mFrameCrop.right);
        mFrameCrop.top = AvUtility.getAvMediaInteger(outAvFmt, GlobalConst.KEY_CROP_TOP, mFrameCrop.top);
        mFrameCrop.bottom = AvUtility.getAvMediaInteger(outAvFmt, GlobalConst.KEY_CROP_BOTTOM, mFrameCrop.bottom);

        Log.d(TAG, "<getVideoInfoFromOut> colorFmt=" + mColorFormat
                + ", wdith=" + mDataWidth + ", height=" + mDataHeight
                + ", mStrideWidth=" + mStrideWidth + ", mStrideHeigh=" + mStrideHeight
                + ", displayWidth=" + mDisplayWidth + ", displayHeight=" + mDisplayHeight
                + ", rotation=" + mRotation
                + ", cropLeft=" + mFrameCrop.left + ", cropRight=" + mFrameCrop.right
                + ", cropTop=" + mFrameCrop.top + ", cropBottom=" + mFrameCrop.bottom      );

        // 校验display信息 和 crop对齐，如果两者不一致，以display信息重新计算裁剪区域

        if (mFrameCrop.left < 0) {
            mFrameCrop.left = 0;
        }

        if (mFrameCrop.right >= mStrideWidth) {
            mFrameCrop.right = mStrideWidth-1;
        }

        if (mFrameCrop.top < 0) {
            mFrameCrop.top = 0;
        }

        if (mFrameCrop.bottom >= mStrideHeight) {
            mFrameCrop.bottom = mStrideHeight-1;
        }
    }


    /*
     * @brief 将YUV视频帧数据转换成Bitmap对象
     *       特别要注意：mDataWidth & mDataHeight 与 mDisplayWidth & mDisplayHeight 时的处理
     *                  此时通常由 mFrameCrop来控制实际的裁剪区域
     * @param yuvBuffer : 原始YUV数据缓冲区，宽高由 mStrideWidth & mStrideHeight 决定
     */
    int dumpImgCnt = 1;
    private Bitmap convertYuvToBitmap(byte[] yuvBuffer) {

//        if ((yuvBuffer != null) && (dumpImgCnt < 100)) {
//            String fileName = String.format(Locale.getDefault(), "%dx%d_%06d.I420",
//                    mDataWidth, mDataHeight, dumpImgCnt);
//            String saveFilePath = mStorageRootPath + "/zdump/" + fileName;
//            AvUtility.saveBytesToFile(yuvBuffer, saveFilePath);
//            dumpImgCnt++;
//        }

        //
        // 创建原始图像解码的缓冲区
        //
        if ((mOriginalBmp == null) ||
            (mOriginalBmp.getWidth() != mStrideWidth) ||
            (mOriginalBmp.getHeight() != mStrideHeight)) {
            mOriginalBmp = AvUtility.createBitmap(mStrideWidth, mStrideHeight);
            if (mOriginalBmp == null) {
                Log.e(TAG, "<convertYuvToBitmap> fail to createBitmap() original");
                return null;
            }
        }

        // 原始YUV数据转换成Bitmap
//        int ret =  AvSoftCodec.getInstance().yuvToBitmap(yuvBuffer, mColorFormat,
//                mStrideWidth, mStrideHeight, mStrideWidth, mOriginalBmp);

//        if (dumpImgCnt < 180) {  // dump前180帧图像
//            String fileName = String.format(Locale.getDefault(), "org_%06d.bmp", dumpImgCnt);
//            String saveFilePath = mStorageRootPath + "/zdump/" + fileName;
//            AvSoftCodec.getInstance().saveBmpToBmpFile(mOriginalBmp, saveFilePath);
//            dumpImgCnt++;
//        }

        //
        // 根据裁剪区域进行裁剪
        //
        Bitmap cropBmp = AvUtility.copyBitmap(mOriginalBmp, mFrameCrop);
        if (cropBmp == null) {
            Log.e(TAG, "<convertYuvToBitmap> fail to copyBitmap() from original");
            return null;
        }
        if (mRotation == 0) {  // 没有旋转直接返回
            return cropBmp;
        }

        //
        // 进行旋转处理
        //
        if (mRotMatrix == null) {
            mRotMatrix = new Matrix();
            float centerX = (float) (cropBmp.getWidth() / 2);
            float centerY = (float) (cropBmp.getHeight() / 2);
            mRotMatrix.setRotate(mRotation, centerX, centerY);
        }
        Bitmap rotateBmp = null;
        try {
            rotateBmp = Bitmap.createBitmap(cropBmp, 0, 0, cropBmp.getWidth(), cropBmp.getHeight(),
                                            mRotMatrix, true);
        } catch (IllegalArgumentException argueExp) {
            argueExp.printStackTrace();
            Log.e(TAG, "<convertYuvToBitmap> rotate bmp, argueExp=" + argueExp);
            return null;

        } catch (Exception exp) {
            exp.printStackTrace();
            Log.e(TAG, "<convertYuvToBitmap> rotate bmp, exp=" + exp);
            return null;
        }

        return rotateBmp;
    }


    /*
     * @brief 尝试视频帧解码操作，仅对视频流轨道有效
     * @param None
     * @return Integer：错误码，XERR_CODEC_DEC_EOS 表示解码是最后一帧了，后续没有要解码数据了
     *         AvVideoFrame : 返回解码后的视频帧
     */
    public Pair<Integer, AvVideoFrame> tryDecVideoFrame() {
        AvVideoFrame outFrame = null;
        int ret = ErrCode.XOK;

        try {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mAvDecoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
            switch (outputBufferIndex) {
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    mOutputFormat = mAvDecoder.getOutputFormat();
                    getVideoInfoFromOut(mOutputFormat);    // 从实际解码处理的媒体格式获取视频帧信息
                    ret = ErrCode.XERR_CODEC_MOREINDATA;  // 需要更多数据
                    break;

                case MediaCodec.INFO_TRY_AGAIN_LATER: // 当前没有解码数据输出，需要继续解码
                    //Log.d(TAG, "<tryDecVideoFrame> TRY_AGAIN_LATER");
                    ret = ErrCode.XERR_CODEC_MOREINDATA;    // 需要更多数据
                    break;

                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d(TAG, "<tryDecVideoFrame> BUFFERS_CHANGED");
                    mOutputBuffers = mAvDecoder.getOutputBuffers();
                    ret = ErrCode.XERR_CODEC_MOREINDATA;    // 需要更多数据
                    break;

                default: {
                    // 获取解码后的数据
                    ByteBuffer outputBuffer;
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        outputBuffer = (mOutputBuffers != null) ? mOutputBuffers[outputBufferIndex] : null;
                    } else {
                        outputBuffer = mAvDecoder.getOutputBuffer(outputBufferIndex);
                    }
                    if (outputBuffer == null) {
                        Log.e(TAG, "<tryDecVideoFrame> outputBuffer is NULL");
                        return (new Pair(ErrCode.XERR_CODEC_MOREINDATA, null));
                    }
                    //outputBuffer.position(bufferInfo.offset);
                    //outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                    // 这里不需要获取解码后的YUV数据，只需要视频帧信息
                    outFrame = new AvVideoFrame();
                    outFrame.mDataBuffer = null;
                    outFrame.mFrameIndex = mFrameIndex++;
                    outFrame.mTimestamp = bufferInfo.presentationTimeUs;
                    outFrame.mFlags = bufferInfo.flags;
                    outFrame.mColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_Format32bitARGB8888;
                    if (outFrame.mFrameBmp != null) {
                        outFrame.mWidth = outFrame.mFrameBmp.getWidth();
                        outFrame.mHeight = outFrame.mFrameBmp.getHeight();
                    }

                    mAvDecoder.releaseOutputBuffer(outputBufferIndex, false);
                    Log.d(TAG, "<tryDecVideoFrame> decodedSize=" + bufferInfo.size
                            + ", mTimestamp=" + outFrame.mTimestamp
                            + ", mFlags=" + outFrame.mFlags);

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {   // 判断帧类型
                        outFrame.mKeyFrame = true;
                    }

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {   // 解码缓冲区完成
                        Log.d(TAG, "<tryDecVideoFrame> BUFFER_FLAG_EOS");
                        ret = ErrCode.XERR_CODEC_DEC_EOS;
                        outFrame.mLastFrame = true;
                    } else {
                        ret = ErrCode.XOK;
                    }
                }  break;
            }

        } catch (Exception except) {
            except.printStackTrace();
            Log.e(TAG, "<tryDecVideoFrame> except=" + except.toString());
            return (new Pair(ErrCode.XERR_CODEC_DECODING, null));
        }

        return (new Pair(ret, outFrame));
    }

}