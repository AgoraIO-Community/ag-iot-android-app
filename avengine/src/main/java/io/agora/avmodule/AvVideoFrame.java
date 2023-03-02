package io.agora.avmodule;


import android.graphics.Bitmap;


/**
 * @brief 视频帧数据
 */
public class AvVideoFrame extends AvBaseFrame {
    public int mColorFormat;                ///< 帧数据格式
    public Bitmap mFrameBmp;                ///< 解码后的视频帧图像
    public int mWidth;                      ///< 视频帧宽度
    public int mHeight;                     ///< 视频帧高度

    public AvVideoFrame() {
        mFrameType = FRAME_TYPE_VIDEO;
    }


}