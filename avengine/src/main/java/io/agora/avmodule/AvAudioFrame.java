
package io.agora.avmodule;



/**
 * @brief 音频帧数据
 */
public class AvAudioFrame extends AvBaseFrame {

    public int mSampleFormat;               ///< 采样格式, 默认 AudioFormat.ENCODING_PCM_16BIT
    public int mBytesPerSample;             ///< 每个采样的字节数，默认是2
    public int mChannels;                   ///< 音频通道数，Android固定只支持双通道
    public int mSampleNumber;               ///< 样本数量
    public int mSampleRate;                 ///< 采样率

    public AvAudioFrame() {
        mFrameType = FRAME_TYPE_AUDIO;
    }
}