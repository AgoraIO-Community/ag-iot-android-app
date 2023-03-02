package io.agora.avmodule;


import android.content.Context;



/*
 * @param 音视频解码参数
 */
public class AvDecParam {

    public Context mContext;        ///< 转换器引擎上下文
    public String mInFileUrl;       ///< 输入媒体文件路径

    public int mOutVidFormat;       ///< 输出视频帧格式， 0表示固定 NV12
    public int mOutVidWidth;        ///< 输出视频帧宽度，0表示原始宽度
    public int mOutVidHeight;       ///< 输出视频帧高度，0表示原始高度

    public int mOutAudSampleFmt;    ///< 输出音频采样格式
    public int mOutAudChannels;     ///< 输出音频通道数
    public int mOutAudSampleRate;   ///< 输出音频采样率


    @Override
    public String toString() {
        String strInfo = "{ mInFileUrl=" + mInFileUrl
                + ", mOutVidFormat=" + mOutVidFormat
                + ", mOutVidWidth=" + mOutVidWidth
                + ", mOutVidHeight=" + mOutVidHeight
                + ", mOutAudSampleFmt=" + mOutAudSampleFmt
                + ", mOutAudChannels=" + mOutAudChannels
                + ", mOutAudSampleRate=" + mOutAudSampleRate + " }\n";
        return strInfo;
    }
}