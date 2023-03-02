package io.agora.avmodule;



import android.media.AudioFormat;
import android.media.Image;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.util.Log;
import android.util.Range;

import androidx.annotation.RequiresApi;


/**
 * @brief 音视频能力类
 */
public class AvCapability {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////

    /*
     * @brief 视频编解码器能力
     */
    public static class VideoCaps {
        public int[] mColorFormats;                 ///< 支持的视频帧色彩格式
        public Range<Integer> mWidthRange;          ///< 视频宽度范围
        public Range<Integer> mHeightRange;         ///< 视频高度范围
        public Range<Integer> mFrameRateRange;      ///< 帧率范围
        public Range<Integer> mBitrateRange;        ///< 码率范围
        public boolean mBitrateVbrSupported;        ///< 是否支持 VBR码率模式
        public boolean mBitrateCbrSupported;        ///< 是否支持 CBR码率模式
        public boolean mBitrateCqSupported;         ///< 是否支持 CQ码率模式

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public String toString() {
            String widthInfo = "width[" + mWidthRange.getLower() + ", " + mWidthRange.getUpper() + "] ";
            String heightInfo = "height[" + mHeightRange.getLower() + ", " + mHeightRange.getUpper() + "] ";
            String fpsInfo = "fps[" + mFrameRateRange.getLower() + ", " + mFrameRateRange.getUpper() + "] ";
            String bitrateInfo = "bitrate[" + mBitrateRange.getLower() + ", " + mBitrateRange.getUpper() + "] ";
            String vbrSupported = mBitrateVbrSupported ? "[VBR support]" : "[VBR unsupport]";
            String cbrSupported = mBitrateCbrSupported ? "[CBR support]" : "[CBR unsupport]";
            String cqSupported = mBitrateCqSupported ? "[CQ support]" : "[CQ unsupport]";

            String text = "{ " + widthInfo + heightInfo + fpsInfo + bitrateInfo
                            + vbrSupported + cbrSupported + cqSupported + " }";
            return text;
        }
    }

    /*
     * @brief 音频编解码器能力
     */
    public static class AudioCaps {
        public int mSampleFormat;                   ///< 音频采样格式
        public int[] mSampleRateArray;              ///< 支持的采样率
        public int mMaxChannelCount;                ///< 最大支持的通道数
        public Range<Integer> mBitrateRange;        ///< 码率范围
        public boolean mBitrateVbrSupported;        ///< 是否支持 VBR码率模式
        public boolean mBitrateCbrSupported;        ///< 是否支持 CBR码率模式
        public boolean mBitrateCqSupported;         ///< 是否支持 CQ码率模式

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public String toString() {
            String formatInfo = "mSampleFormat=" + mSampleFormat + ", ";
            String channelInfo = "mMaxChannelCount=" + mMaxChannelCount + ", ";
            String bitrateInfo = "bitrate[" + mBitrateRange.getLower() + ", " + mBitrateRange.getUpper() + "] ";
            String text = "{ " + formatInfo + channelInfo + bitrateInfo + " }";
            return text;
        }
    }


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "AVMODULE/Capability";
    private static final String MIME_TYPE_VIDEO = "video/";
    private static final String MIME_TYPE_AUDIO = "audio/";


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
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
     * @brief 获取VideoCodec的能力
     * @param mimType : 编解码器的mime
     * @retrun 返回编解码信息，如果没有找到则返回null
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static VideoCaps getVideoCapability(String mimeType) {
        MediaCodecInfo codecInfo = getCodecInfo(mimeType);
        if (codecInfo == null) {
            Log.e(TAG, "<getVideoCapability> fail to get codec info, mimeType=" + mimeType);
            return null;
        }

        VideoCaps videoCapability = new VideoCaps();
        try {
            MediaCodecInfo.CodecCapabilities caps = codecInfo.getCapabilitiesForType(mimeType);
            MediaCodecInfo.VideoCapabilities videoCaps = caps.getVideoCapabilities();

            videoCapability.mWidthRange = videoCaps.getSupportedWidths();
            videoCapability.mHeightRange = videoCaps.getSupportedHeights();
            videoCapability.mFrameRateRange = videoCaps.getSupportedFrameRates();
            videoCapability.mBitrateRange = videoCaps.getBitrateRange();
            videoCapability.mColorFormats = new int[caps.colorFormats.length];
            System.arraycopy(caps.colorFormats, 0, videoCapability.mColorFormats, 0,
                    caps.colorFormats.length);

            MediaCodecInfo.EncoderCapabilities encodeCaps = caps.getEncoderCapabilities();
            videoCapability.mBitrateVbrSupported = encodeCaps.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            videoCapability.mBitrateCbrSupported = encodeCaps.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
            videoCapability.mBitrateCqSupported = encodeCaps.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);

        } catch (IllegalArgumentException argueExpt) {
            argueExpt.printStackTrace();
            Log.e(TAG, "<getVideoCapability> [argueExpt] fail to get capabilites");
            return null;
        }

        return videoCapability;
    }

    /*
     * @brief 获取AudioCodec的能力
     * @param mimType : 编解码器的mime
     * @retrun 返回编解码信息，如果没有找到则返回null
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static AudioCaps getAudioCapability(String mimeType) {
        MediaCodecInfo codecInfo = getCodecInfo(mimeType);
        if (codecInfo == null) {
            Log.e(TAG, "<getAudioCapability> fail to get codec info, mimeType=" + mimeType);
            return null;
        }

        AudioCaps audioCapability = new AudioCaps();
        try {
            MediaCodecInfo.CodecCapabilities caps = codecInfo.getCapabilitiesForType(mimeType);
            MediaCodecInfo.AudioCapabilities audioCaps = caps.getAudioCapabilities();

            audioCapability.mSampleFormat = AudioFormat.ENCODING_PCM_16BIT;  ///< Android仅支持的采样格式
            audioCapability.mSampleRateArray = audioCaps.getSupportedSampleRates();
            audioCapability.mBitrateRange = audioCaps.getBitrateRange();
            audioCapability.mMaxChannelCount = audioCaps.getMaxInputChannelCount();

            MediaCodecInfo.EncoderCapabilities encodeCaps = caps.getEncoderCapabilities();
            audioCapability.mBitrateVbrSupported = encodeCaps.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            audioCapability.mBitrateCbrSupported = encodeCaps.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
            audioCapability.mBitrateCqSupported = encodeCaps.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);

        } catch (IllegalArgumentException argueExpt) {
            argueExpt.printStackTrace();
            Log.e(TAG, "<getAudioCapability> [argueExpt] fail to get capabilites");
            return null;
        }

        return audioCapability;
    }



}