
package io.agora.avmodule;



/**
 * @brief 帧数据基类
 */
public class AvBaseFrame {
    //
    // 帧类型定义
    //
    public static final int FRAME_TYPE_VIDEO = 0;
    public static final int FRAME_TYPE_AUDIO = 1;

    public int mFrameType;                  ///< 帧类型：视频帧还是音频帧
    public int mFrameIndex;                 ///< 当前帧索引
    public byte[] mDataBuffer;              ///< 帧数据缓冲区，作为最后一帧有可能为空
    public long mTimestamp;                 ///< 帧数据对应时间戳
    public boolean mKeyFrame;               ///< 当前帧是否是关键帧
    public boolean mLastFrame;              ///< 当前帧是最后一帧
    public int mFlags;                      ///< 帧信息标记
}