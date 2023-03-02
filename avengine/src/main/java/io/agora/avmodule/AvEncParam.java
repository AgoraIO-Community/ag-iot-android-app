package io.agora.avmodule;


/*
 * @param 音视频编码参数
 */
public class AvEncParam {

    public String mOutFilePath;     ///< 输出媒体文件全路径

    public String mVideoCodec;      ///< 输出文件视频视频编码格式，默认是 AVC
    public int mColorFormat;        ///< 输出视频帧色彩格式
    public int mVideoWidth;         ///< 输出文件视频帧的宽度
    public int mVideoHeight;        ///< 输出文件视频帧的高度
    public int mRotation;           ///< 输出文件视频帧的旋转角度
    public int mFrameRate;          ///< 输出文件视频帧率
    public int mGopFrame;           ///< 输出文件关键帧间隔帧数，必须是 mFrameRate的倍数
    public int mVideoBitRate;       ///< 输出文件视频编码码率，详细根据不同机型做配置
    public int mVBitRateMode;       ///< 输出文件视频编码码率模式，是可变码率还是固定码率

    public String mAudioCodec;      ///< 输出文件音频编码格式，默认可以沿用输入文件的
    public int mSampleFmt;          ///< 输出文件要编码的音频采样格式，默认可以沿用输入文件的
    public int mChannels;           ///< 输出文件要编码的音频通道数量，Android固定2
    public int mSampleRate;         ///< 输出文件要编码的音频采样率，默认可以沿用输入文件的
    public int mAudioBitRate;       ///< 输出文件音频编码码率，默认可以沿用输入文件的

    @Override
    public String toString() {
        String strInfo = "{ videoCodec=" + mVideoCodec
                + ", colorfmt=" + mColorFormat + ", width=" + mVideoWidth + ", height=" + mVideoHeight
                + ", fps=" + mFrameRate + ", rotation=" + mRotation + ", gopFrame=" + mGopFrame
                + ", videoBitrate=" + mVideoBitRate + ", videoBitMode=" + mVBitRateMode + " }\n"

                + "{ audioCodec=" + mAudioCodec
                + ", smplFmt=" + mSampleFmt + ", mChannels=" + mChannels + ", smplRate=" + mSampleRate
                + ", audioBitrate=" + mAudioBitRate + " }\n";
        return strInfo;
    }
}