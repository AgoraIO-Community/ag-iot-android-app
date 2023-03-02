package io.agora.avmodule;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import java.io.IOException;



/*
 * @brief 媒体文件解码组件接口
 */
public interface IAvCompDecoder {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief 解码组件的回调事件接口
     */
    public static interface IAvCompDecoderCallback {

        /*
         * @brief 媒体文件信息解析到事件
         * @param mediaInfo : 当前媒体文件信息
         */
        void onMediaInfoDecoded(AvMediaInfo mediaInfo);

        /*
         * @brief 解码到一帧视频帧事件
         * @param mediaInfo : 当前媒体文件信息
         * @param videoFrame : 解码到的视频帧，根据 mLastFrame字段判断是否最后一帧
         */
        void onVideoFrameDecoded(AvMediaInfo mediaInfo, AvVideoFrame videoFrame);

        /*
         * @brief 解码到一帧音频帧事件
         * @param mediaInfo : 当前媒体文件信息
         * @param videoFrame : 解码到的视频帧，根据 mLastFrame字段判断是否最后一帧
         */
        void onAudioFrameDecoded(AvMediaInfo mediaInfo, AvAudioFrame audioFrame);

        /*
         * @brief 解码过程中出现了不能继续的错误
         * @param mediaInfo : 当前媒体文件信息
         * @param errCode : 错误代码
         */
        void onDecodeError(AvMediaInfo mediaInfo, int errCode);

    }

    /*
     * @brief 解码组件初始化参数
     */
    public static class CompDecodeParam {
        public IAvCompDecoderCallback mCallback;
        public Context mContext;
        public AvDecParam mDecParam;
    }



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    //
    // The state machine of Player Engine
    //
    public static final int STATE_INVALID = 0x0000;         ///< 组件还未初始化
    public static final int STATE_IDLE = 0x0001;            ///< 媒体文件已经打开，但未解码
    public static final int STATE_DECODING = 0x0002;        ///< 正在解码过程中
    public static final int STATE_DONE = 0x0003;            ///< 整个解码处理已经完成





    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief 初始化SDK
     * @param context : 相关的上下文信息
     * @return error code
     */
    public int initialize(CompDecodeParam initParam);


    /*
     * @brief 引擎释放操作
     */
    public void release();


    /*
     * @brief 获取Sdk当前状态
     * @return 返回状态机值
     */
    public int getState();

    /*
     * @brief 获取媒体文件信息
     * @return 返回解码后的媒体文件信息
     */
    public AvMediaInfo getMediaInfo();



    /*
     * @brief 启动解码线程进行解码处理
     * @param None
     * @retrun error code, 0表示成功
     */
    public int start();

    /*
     * @brief 停止解码线程进行解码处理，若要恢复需要调用start()
     * @param None
     * @retrun error code, 0表示成功
     */
    public int stop();

    /*
     * @brief 暂停解码线程进行解码处理，若要恢复需要调用resume()
     * @param None
     * @retrun error code, 0表示成功
     */
    public int pause();

    /*
     * @brief 恢复解码线程进行解码处理
     * @param None
     * @retrun error code, 0表示成功
     */
    public int resume();

    /*
     * @brief 从当前解码器中提取解码的一帧视频帧
     * @param None
     * @retrun 返回解码到的视频帧，如果队列为空则返回null
     */
    public AvVideoFrame dequeueVideoFrame();

    /*
     * @brief 从当前解码器中提取解码的一帧音频
     * @param None
     * @retrun 返回解码到的音频帧，如果队列为空则返回null
     */
    public AvAudioFrame dequeueAudioFrame();

    /*
     * @brief 获取输出队列视频帧个数
     * @param None
     * @retrun 视频帧个数
     */
    public int getVideoFrameCount();

    /*
     * @brief 获取输出队列音频帧个数
     * @param None
     * @retrun 音频帧个数
     */
    public int getAudioFrameCount();




}