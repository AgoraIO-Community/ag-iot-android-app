package io.agora.avmodule;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import java.io.IOException;



/*
 * @brief 媒体文件编码组件接口
 */
public interface IAvCompEncoder {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief 编码组件的回调事件接口
     */
    public static interface IAvCompEncoderCallback {
        /*
         * @brief 编码正常完成
         * @param encodeParam : 当前编码参数
         * @param errCode : 错误代码
         */
        void onEncodeDone(CompEncodeParam compEncodeParam, int errCode);

        /*
         * @brief 编码过程中出现了不能继续的错误
         * @param encodeParam : 当前编码参数
         * @param errCode : 错误代码
         */
        void onEncodeError(CompEncodeParam compEncodeParam, int errCode);
    }

    /*
     * @brief 编码组件初始化参数
     */
    public static class CompEncodeParam {
        public IAvCompEncoderCallback mCallback;
        public Context mContext;
        public AvEncParam mEncParam;
    }


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    //
    // The state machine of encoder component
    //
    public static final int STATE_INVALID = 0x0000;         ///< 组件还未初始化
    public static final int STATE_IDLE = 0x0001;            ///< 媒体文件已经打开，但未编码
    public static final int STATE_ENCODING = 0x0002;        ///< 正在编码过程中
    public static final int STATE_PAUSED = 0x0003;          ///< 暂停状态


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////




    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief 组件初始化
     * @param context : 相关的上下文信息
     * @return error code
     */
    public int initialize(CompEncodeParam initParam);

    /*
     * @brief 组件释放操作
     */
    public void release();

    /*
     * @brief 获取组件当前状态
     * @return 返回状态机值
     */
    public int getState();

    /*
     * @brief 送入要编码的视频帧数据
     * @return 返回编码后的媒体文件信息
     */
    public int inputVideoFrame(AvVideoFrame videoFrame);

    /*
     * @brief 送入要编码的视频帧数据
     * @return 返回编码后的媒体文件信息
     */
    public int inputAudioFrame(AvAudioFrame audioFrame);

    /*
     * @brief 获取输入视频帧数量
     * @return 队列中等待编码的视频帧数量
     */
    public int getInVideoCount();

    /*
     * @brief 获取输入音频帧数量
     * @return 队列中等待编码的音频帧数量
     */
    public int getInAudioCount();

    /*
     * @brief 启动编码线程进行编码处理
     * @param None
     * @retrun error code, 0表示成功
     */
    public int start();

    /*
     * @brief 停止编码线程进行编码处理，若要恢复需要调用start()
     * @param None
     * @retrun error code, 0表示成功
     */
    public int stop();

    /*
     * @brief 暂停编码线程进行解码处理，若要恢复需要调用resume()
     * @param None
     * @retrun error code, 0表示成功
     */
    public int pause();

    /*
     * @brief 恢复编码线程进行解码处理
     * @param None
     * @retrun error code, 0表示成功
     */
    public int resume();


    /*
     * @brief 获取视频编码进度
     * @retrun 返回当前视频帧时间戳
     */
    public long getVideoTimestamp();

    /*
     * @brief 获取音频编码进度
     * @retrun 返回当前音频帧时间戳
     */
    public long getAudioTimestamp();

}