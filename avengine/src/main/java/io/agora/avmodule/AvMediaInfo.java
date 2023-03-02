package io.agora.avmodule;


import android.content.Context;
import android.net.Uri;

/*
 * @brief 媒体文件信息
 */
public class AvMediaInfo {
    public Context mContext;
    public Uri mFileUri;            ///< 媒体文件Uri，与mFilePath只能二选一
    public String mFilePath;        ///< 媒体文件绝对路径
    public long mFileDuration;      ///< 媒体文件时长, Max(mVideoDuration, mAudioDuration)

    public int mVideoTrackId = -1;  ///< 视频流所在trackIndex
    public long mVideoDuration;     ///< 视频流时长
    public String mVideoCodec = ""; ///< 视频编码方式
    public int mColorFormat;        ///< 视频帧色彩格式
    public int mColorRange = 0;     ///< 色彩范围
    public int mColorSpace = 0;     ///< 色彩空间
    public int mDataWidth;          ///< 视频帧原始宽度
    public int mDataHeight;         ///< 视频帧原始高度
    public int mDisplayWidth;       ///< 视频帧显示宽度
    public int mDisplayHeight;      ///< 视频帧显示高度
    public int mVideoWidth;         ///< 视频帧宽度（以显示宽度为准）
    public int mVideoHeight;        ///< 视频帧高度（以显示高度为准）
    public int mRotation;           ///< 旋转角度
    public int mFrameRate;          ///< 帧率
    public int mVideoBitrate;       ///< 视频的码率，如果没有获取到则为0
    public int mVideoMaxBitrate;    ///< 视频的最大码率，如果没有获取到则为0

    public int mAudioTrackId = -1;  ///< 音频流所在trackIndex
    public long mAudioDuration;     ///< 音频流时长
    public String mAudioCodec = ""; ///< 音频编码方式
    public int mSampleFmt;          ///< 音频采样格式
    public int mChannels;           ///< 音频频道数量
    public int mSampleRate;         ///< 采样率
    public int mAudioBitrate;       ///< 音频的码率，如果没有获取到则为0
    public int mAudioMaxBitrate;    ///< 音频的最大码率，如果没有获取到则为0

    @Override
    public String toString() {
        String strInfo = "{ videoTrackId=" + mVideoTrackId + ", videoCodec=" + mVideoCodec
                + ", colorfmt=" + mColorFormat + ", width=" + mVideoWidth + ", height=" + mVideoHeight
                + ", fps=" + mFrameRate + ", rotation=" + mRotation
                + ", vBitRate=" + mVideoBitrate + ", maxVBitrate=" + mVideoMaxBitrate
                + ", vDuration=" + mVideoDuration + " }\n"

                + "{ audioTrackId=" + mAudioTrackId  + ", audioCodec=" + mAudioCodec
                + ", smplFmt=" + mSampleFmt + ", mChannels=" + mChannels + ", smplRate=" + mSampleRate
                + ", aBitRate=" + mAudioBitrate + ", maxABitrate=" + mAudioMaxBitrate
                + ", aDuration=" + mAudioDuration + " }\n";

        return strInfo;
    }

}