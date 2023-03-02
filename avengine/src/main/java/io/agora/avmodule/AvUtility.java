package io.agora.avmodule;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


/*
 * @brief 辅助函数类库
 */
public class AvUtility {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "AVMODULE/AvUtility";
    private static final String MIME_TYPE_VIDEO = "video/";
    private static final String MIME_TYPE_AUDIO = "audio/";


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief 硬解码解析媒体文件相关的信息
     *        当前不考虑多音频轨道或者多视频轨道情况
     * @param mediaFilePath : 媒体文件路径
     * @retrun 媒体文件信息类
     */
    static public AvMediaInfo hwParseMediaInfo(String mediaFilePath)  {
        //Log.d(TAG, "<hwParseMediaInfo> BEGIN, file=" + mediaFilePath);

        MediaFormat videoFormat = null;
        MediaFormat audioFormat = null;
        int videoTrackIndex = -1;
        int audioTrackIndex = -1;
        AvMediaInfo mediaInfo = new AvMediaInfo();
        mediaInfo.mVideoTrackId = -1;
        mediaInfo.mAudioTrackId = -1;

        try {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(mediaFilePath);

            // 查询视频轨道信息
            int trackCount = extractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = extractor.getTrackFormat(i);
                if (trackFormat.getString(MediaFormat.KEY_MIME).startsWith(MIME_TYPE_VIDEO)) {
                    videoTrackIndex = i;
                    videoFormat = trackFormat;
                } else if (trackFormat.getString(MediaFormat.KEY_MIME).startsWith(MIME_TYPE_AUDIO)) {
                    audioTrackIndex = i;
                    audioFormat = trackFormat;
                }
            }


            // 获取媒体文件相关信息
            if (videoFormat != null) {
                mediaInfo.mVideoTrackId = videoTrackIndex;
                mediaInfo.mVideoCodec = videoFormat.getString(MediaFormat.KEY_MIME);
                mediaInfo.mColorFormat = AvUtility.getAvMediaInteger(videoFormat, MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
                mediaInfo.mDataWidth = AvUtility.getAvMediaInteger(videoFormat, MediaFormat.KEY_WIDTH, -1);
                mediaInfo.mDataHeight = AvUtility.getAvMediaInteger(videoFormat, MediaFormat.KEY_HEIGHT, -1);
                mediaInfo.mDisplayWidth = AvUtility.getAvMediaInteger(videoFormat, GlobalConst.KEY_DISPLAY_WIDTH, -1);
                mediaInfo.mDisplayHeight = AvUtility.getAvMediaInteger(videoFormat, GlobalConst.KEY_DISPLAY_HEIGHT, -1);
                mediaInfo.mVideoWidth = (mediaInfo.mDataWidth > 0) ? mediaInfo.mDataWidth : mediaInfo.mDisplayWidth;
                mediaInfo.mVideoHeight = (mediaInfo.mDataHeight > 0) ? mediaInfo.mDataHeight: mediaInfo.mDisplayHeight;
                mediaInfo.mRotation = AvUtility.getAvMediaInteger(videoFormat, MediaFormat.KEY_ROTATION, 0);
                mediaInfo.mFrameRate = AvUtility.getAvMediaInteger(videoFormat, MediaFormat.KEY_FRAME_RATE, 30);
                mediaInfo.mVideoBitrate = AvUtility.getAvMediaInteger(videoFormat, MediaFormat.KEY_BIT_RATE, 0);
                mediaInfo.mVideoMaxBitrate = AvUtility.getAvMediaInteger(videoFormat, GlobalConst.KEY_MAX_BIT_RATE, 0);
                mediaInfo.mVideoDuration = videoFormat.getLong(MediaFormat.KEY_DURATION);
            }

            if (audioFormat != null) {
                mediaInfo.mAudioTrackId = audioTrackIndex;
                mediaInfo.mAudioCodec = audioFormat.getString(MediaFormat.KEY_MIME);
                mediaInfo.mSampleFmt = AvUtility.getAvMediaInteger(audioFormat, MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT);
                mediaInfo.mChannels = AvUtility.getAvMediaInteger(audioFormat, MediaFormat.KEY_CHANNEL_COUNT, 2);
                mediaInfo.mSampleRate = AvUtility.getAvMediaInteger(audioFormat, MediaFormat.KEY_SAMPLE_RATE, 44100);
                mediaInfo.mAudioBitrate = AvUtility.getAvMediaInteger(audioFormat, MediaFormat.KEY_BIT_RATE, 0);
                mediaInfo.mAudioMaxBitrate = AvUtility.getAvMediaInteger(audioFormat, GlobalConst.KEY_MAX_BIT_RATE, 0);
                mediaInfo.mAudioDuration = audioFormat.getLong(MediaFormat.KEY_DURATION);
            }

            mediaInfo.mFilePath = mediaFilePath;
            if (mediaInfo.mVideoDuration > mediaInfo.mAudioDuration) {
                mediaInfo.mFileDuration = mediaInfo.mVideoDuration;
            } else {
                mediaInfo.mFileDuration = mediaInfo.mAudioDuration;
            }
            extractor.release();
        } catch (IOException ioExcept) {
            ioExcept.printStackTrace();
            Log.e(TAG, "<hwParseMediaInfo> IOException");
            return null;

        } catch (Exception except) {
            except.printStackTrace();
            Log.e(TAG, "<hwParseMediaInfo> Exception");
            return null;
        }

        //Log.d(TAG, "<hwParseMediaInfo> END");
        return mediaInfo;
    }

    /*
     * @brief 探测是否支持硬解码
     * @param mediaFilePath : 媒体文件路径
     * @retrun true : 表示支持硬解码；  false : 表示不支持硬解码
     */
    static public boolean detectHwDecoder(Context ctx, String mediaFilePath)  {
        Log.d(TAG, "<detectHwDecoder> BEGIN, file=" + mediaFilePath);

        AvDecoder videoDecoder = new AvDecoder();
        int ret = videoDecoder.initialize(ctx, null, mediaFilePath, AvDecoder.STREAM_TYPE_VIDEO);
        if (ret != ErrCode.XOK) {
            Log.e(TAG, "<detectHwDecoder> fail to open video decoder, ret=" + ret);
            return false;
        }

        boolean videoInputEos = false;
        boolean videoDecodeEos = false;
        boolean supportHwDecoder = true;
        int tryCount = 0;           // 解码尝试次数
        int decodedFrameCnt = 0;    // 已经解码视频帧数

        while ((tryCount < 50) && (supportHwDecoder)) {

            if (decodedFrameCnt >= 2) { // 已经解码到两帧视频帧了
                break;
            }


            //
            // 读取原始码视频流数据，送入到解码器中
            //
            ret = ErrCode.XOK;
            if (!videoInputEos) {
                ret = videoDecoder.inputFrame();
                if (ret == ErrCode.XERR_CODEC_DEC_EOS) {  // 视频帧已经送入完成
                    Log.d(TAG, "<detectHwDecoder> feeding EOS done!");
                    videoInputEos = true;
                } else if (ret == ErrCode.XERR_CODEC_DECODING) { // 解码器失败
                    Log.e(TAG, "<detectHwDecoder> input frame failure");
                    supportHwDecoder = false;
                }
            }

            //
            // 进行解码操作，如果有解码后的数据输出会输出到 outFrame中
            //
            if (!videoDecodeEos) {
                Pair<Integer, AvVideoFrame> pair = videoDecoder.tryDecVideoFrame();
                ret = pair.first;
                AvVideoFrame videoFrame = pair.second;
                if (videoFrame != null) {
                }

                if (ret == ErrCode.XERR_CODEC_DECODING) { // 解码失败
                    Log.e(TAG, "<detectHwDecoder> decoding error");
                    supportHwDecoder = false;

                } else if (ret == ErrCode.XERR_CODEC_DEC_EOS) {  // 所有解码都已经完成
                    Log.d(TAG, "<detectHwDecoder> decoding EOS done!");
                    videoDecodeEos = true;

                } else if (ret == ErrCode.XERR_NONE) { // 有视频帧输出，可以继续进行解码操作
                    decodedFrameCnt++;

                } else { // 可以继续进行解码操作

                }
            }
        }

        videoDecoder.release();
        videoDecoder = null;

        Log.d(TAG, "<detectHwDecoder> END, supportHwDecoder=" + supportHwDecoder);
        return supportHwDecoder;
    }


    /*
     * @brief 软解码解析媒体文件相关的信息
     *        当前不考虑多音频轨道或者多视频轨道情况
     * @param mediaFilePath : 媒体文件路径
     * @retrun 媒体文件信息类
     */
    static public AvMediaInfo swParseMediaInfo(String mediaFilePath)  {
        //Log.d(TAG, "<swParseMediaInfo> BEGIN, file=" + mediaFilePath);
        //AvMediaInfo mediaInfo = AvSoftCodec.getInstance().distillMediaInfo(mediaFilePath);
        //Log.d(TAG, "<swParseMediaInfo> END");
        //return mediaInfo;
        return null;
    }



    /*
     * @brief 自动解码解析媒体文件相关的信息，并判断是否支持硬解码
     *        当前不考虑多音频轨道或者多视频轨道情况
     * @param mediaFilePath : 媒体文件路径
     * @retrun Integer 返回解码的Codec类型
     *         AvMediaInfo 返回解码到的媒体信息
     *
     */
    static public Pair<Integer, AvMediaInfo> autoParseMediaInfo(String mediaFilePath) {
        int videoBitrate = 0;
        int videoMaxBitrate = 0;
        int audioBitrate = 0;
        int audioMaxBitrate = 0;

        AvMediaInfo swMediaInfo = swParseMediaInfo(mediaFilePath);
        AvMediaInfo hwMediaInfo = hwParseMediaInfo(mediaFilePath);
        if (hwMediaInfo != null) {
            videoBitrate = swMediaInfo.mVideoBitrate;
            videoMaxBitrate = swMediaInfo.mVideoMaxBitrate;
            audioBitrate = swMediaInfo.mAudioBitrate;
            audioMaxBitrate = swMediaInfo.mAudioMaxBitrate;

            if (videoBitrate > 0) {
                hwMediaInfo.mVideoBitrate = videoBitrate;
            }
            if (videoMaxBitrate > 0) {
                hwMediaInfo.mVideoMaxBitrate = videoMaxBitrate;
            }
            if (audioBitrate > 0) {
                hwMediaInfo.mAudioBitrate = audioBitrate;
            }
            if (audioMaxBitrate > 0) {
                hwMediaInfo.mAudioMaxBitrate = audioMaxBitrate;
            }
        }

        if (swMediaInfo != null) {
            if (swMediaInfo.mColorRange != 0 || swMediaInfo.mColorSpace != 0) {
                String manufacturer = android.os.Build.MANUFACTURER; // 制造商
                String model = android.os.Build.MODEL; // 型号

                if ((manufacturer.compareToIgnoreCase("OPPO") == 0) &&
                        (model.compareToIgnoreCase("K9") == 0)) {
                    // 走软解码
                    Log.d(TAG, "<autoParseMediaInfo> unsupport color range, file=" + mediaFilePath
                            + ", mediaInfo=" + swMediaInfo.toString());
                    return new Pair<>(GlobalConst.CODEC_TYPE_SW, swMediaInfo);
                }
            }
        }

        int smplFmt = AudioFormat.ENCODING_PCM_16BIT;
        if ((hwMediaInfo != null) && (hwMediaInfo.mAudioTrackId >= 0)) {
            smplFmt = hwMediaInfo.mSampleFmt;
        }
        if ((hwMediaInfo != null) && (smplFmt == AudioFormat.ENCODING_PCM_16BIT)
            && (hwMediaInfo.mVideoTrackId >= 0)) {
            // 支持硬解码
            Log.d(TAG, "<autoParseMediaInfo> support HW decoder, file=" + mediaFilePath
                    + ", mediaInfo=" + hwMediaInfo.toString());
            return new Pair<>(GlobalConst.CODEC_TYPE_HW, hwMediaInfo);
        }

        if ((swMediaInfo != null) && (swMediaInfo.mVideoTrackId >= 0)) {
            // 支持软解码
            Log.d(TAG, "<autoParseMediaInfo> support SW decoder 3, file=" + mediaFilePath
                    + ", mediaInfo=" + swMediaInfo.toString());
            return new Pair<>(GlobalConst.CODEC_TYPE_SW, swMediaInfo);
        }

        // 解码不支持
        Log.e(TAG, "<autoParseMediaInfo> don't support decoder, file=" + mediaFilePath);
        return new Pair<>(GlobalConst.CODEC_TYPE_NONE, null);
    }




    /*
     * @brief 删除文件
     * @param filePath : 要删除的文件路径
     * @retrun 媒体文件信息类
     */
    static public boolean deleteFile(String filePath)  {
        File delFile = new File(filePath);
        boolean ret = true;
        if (delFile.exists()) {
            ret = delFile.delete();
        }

        Log.d(TAG, "<deleteFile> file=" + filePath + ", ret=" + ret);
        return ret;

    }

    /*
     * @brief 从MediaFormat中取值
     * @param avForamt : 输入的媒体信息数据
     * @param key : 键值
     * @param defaultVal : 如果没有取到时返回的默认值
     * @retrun 返回取到的值
     */
    static public int getAvMediaInteger(MediaFormat avForamt, String key, int defaultVal) {
        int value = defaultVal;

        try {
            if (!avForamt.containsKey(key)) {
                return defaultVal;
            }
            value = avForamt.getInteger(key);

        } catch (NullPointerException nullExp) {
            nullExp.printStackTrace();
            return defaultVal;

        } catch (ClassCastException castExp) {
            castExp.printStackTrace();
            return defaultVal;

        } catch (Exception exp) {
            exp.printStackTrace();
            return defaultVal;
        }

        return value;
    }


    /*
     * @brief 创建ARGB格式的图像
     * @param width : 图像宽度
     * @param height : 图像高度
     * @retrun 返回创建的Bitmap对象，如果创建失败则返回null
     */
    static public Bitmap createBitmap(int width, int height) {
        Bitmap bmp= null;
        try {
            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } catch (IllegalArgumentException argueExp) {
            argueExp.printStackTrace();
            Log.e(TAG, "<createBitmap> [ARGUE_EXP] fail to create bitmap");
            return null;
        }
        return bmp;
    }

    /*
     * @brief 拷贝部分图像数据
     * @param srcBmp : 源图像
     * @param copyRect : 需要拷贝的图像数据，如果为null表示直接整个克隆
     * @retrun 返回拷贝后的Bitmap对象，如果创建失败则返回null
     */
    static public Bitmap copyBitmap(Bitmap srcBmp, Rect copyRect) {
        if (srcBmp == null) {
            return null;
        }

        Bitmap dstBmp= null;
        try {
            Bitmap.Config cfg = srcBmp.getConfig();
            if (copyRect == null) {
                dstBmp = srcBmp.copy(cfg, false);
            } else {
                int x = 0;
                int y = 0;
                int width = (copyRect.right+1) - copyRect.left;
                int height = (copyRect.bottom+1) - copyRect.top;
                dstBmp = Bitmap.createBitmap(srcBmp, x, y, width, height, null, false);
            }

        } catch (IllegalArgumentException argueExp) {
            argueExp.printStackTrace();
            Log.e(TAG, "<copyBitmap> [ARGUE_EXP] fail to create bitmap");
            return null;
        }
        return dstBmp;
    }


    /*
     * @brief 保存字节流到本地文件
     * @param buffer : 要保存的数据流
     * @param saveFilePath : 要保存的文件
     * @retrun 返回是否保存成功
     */
    static public boolean saveBytesToFile(byte[] buffer, String saveFilePath)  {
        try {
            FileOutputStream os = new FileOutputStream(saveFilePath);
            os.write(buffer);
            os.close();

        } catch (SecurityException secExp) {
            secExp.printStackTrace();
            return false;

        } catch (FileNotFoundException foundExp) {
            foundExp.printStackTrace();
            return false;

        } catch (UnsupportedEncodingException encodExp) {
            encodExp.printStackTrace();
            return false;

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }



    /*
     * @brief 根据相应的编解码信息
     * @param mimType : 编解码器的mime
     * @retrun 返回编解码信息，如果没有找到则返回null
     */
    public static MediaCodecInfo getCodecInfo(String mimeType) {
        int codecCount = MediaCodecList.getCodecCount();
        for (int i = 0; i < codecCount; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }

        return null;
    }


    /*
     * @brief 根据相应的编解码信息
     * @param mimType : 编解码器的mime
     * @retrun 返回编解码信息，如果没有找到则返回null
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static MediaCodecInfo.CodecCapabilities getAvCodecCapabilities(String mimeType) {

        MediaCodecInfo codecInfo = getCodecInfo(mimeType);
        if (codecInfo == null) {
            return null;
        }

        MediaCodecInfo.CodecCapabilities capabilities = null;
        try {
            capabilities = codecInfo.getCapabilitiesForType(mimeType);
            capabilities.getVideoCapabilities().getSupportedFrameRates();


        } catch (IllegalArgumentException argueExpt) {
            argueExpt.printStackTrace();
            Log.e(TAG, "<CreateVideoEncoder> [EXCEPTION] fail to get capabilites");
            return null;
        }

        return capabilities;
    }


    /*
     * @brief 图像旋转操作
     * @param srcBmp : 原始图像
     * @param orientation : 旋转角度
     * @retrun 返回旋转后的图像
     */
    static public Bitmap rotateBmp(Bitmap srcBmp, int orientation)
    {
        float targetX, targetY;
        int   dstWidth, dstHeight;
        Matrix mtx = new Matrix();

        if (90 == orientation || 270 == orientation)
        {
            dstWidth = srcBmp.getHeight();
            dstHeight= srcBmp.getWidth();
        }
        else
        {
            dstWidth = srcBmp.getWidth();
            dstHeight= srcBmp.getHeight();
        }

        Bitmap dstBmp    = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888);
        Canvas dstCanvas = new Canvas(dstBmp);
        mtx.reset();
        if (90 == orientation)
        {
            mtx.setRotate(orientation, (float) srcBmp.getWidth() / 2, (float) srcBmp.getHeight() / 2);
            final float[] values = new float[9];
            mtx.getValues(values);
            float x1 = values[Matrix.MTRANS_X];
            float y1 = values[Matrix.MTRANS_Y];
            mtx.postTranslate(srcBmp.getHeight() - x1, -y1);
        }
        else if (180 == orientation)
        {
            mtx.setRotate(orientation, (float) srcBmp.getWidth() / 2, (float) srcBmp.getHeight() / 2);
        }
        else if (270 == orientation)
        {
            mtx.setRotate(orientation, (float) srcBmp.getWidth() / 2, (float) srcBmp.getHeight() / 2);
            final float[] values = new float[9];
            mtx.getValues(values);
            float x1 = values[Matrix.MTRANS_X];
            float y1 = values[Matrix.MTRANS_Y];
            mtx.postTranslate(-x1, srcBmp.getWidth() - y1);
        }
        else
        {
        }
        dstCanvas.drawBitmap(srcBmp, mtx, null);
        dstCanvas = null;

        return dstBmp;
    }



    /**
     * @brief 复制创建缓冲区数据
     * @param srcBuffer : 源缓冲区数据
     * @retrun 返回新创建并且拷贝的缓冲区数据
     */
    static public byte[] bufferClone(byte[] srcBuffer) {
        if (srcBuffer == null) {
            return null;
        }
        if (srcBuffer.length <= 0) {
            return null;
        }

        byte[] dstBuffer = new byte[srcBuffer.length];
        System.arraycopy(srcBuffer, 0, dstBuffer, 0, srcBuffer.length);
        return dstBuffer;
    }

}