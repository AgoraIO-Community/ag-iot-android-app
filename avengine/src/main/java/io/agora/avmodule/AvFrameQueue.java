package io.agora.avmodule;

import java.util.ArrayList;

/*
 * @brief 音视频帧队列
 */
public class AvFrameQueue {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "AVMODULE/AvFrameQueue";


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private ArrayList<AvBaseFrame> mFrameList = new ArrayList<>();  ///< 帧列表


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 将帧插入队列尾部
     * @param frame : 要插入的帧对象
     * @return None
     */
    public void inqueue(AvBaseFrame frame) {
        synchronized (mFrameList) {
            mFrameList.add(frame);
//            Log.d(TAG, "<inqueue> mFrameType=" + frame.mFrameType
//                    + ", timestamp=" + frame.mTimestamp
//                    + ", flags=" + frame.mFlags
//                    + ", lastFrame=" + frame.mLastFrame );
        }
    }

    /**
     * @brief 从队列头提取一个帧对象
     * @return 返回提取到的帧对象，如果队列为空则返回null
     */
    public AvBaseFrame dequeue() {
        synchronized (mFrameList) {
            int count = mFrameList.size();
            if (count <= 0) {
                return null;
            }
            AvBaseFrame frame = mFrameList.remove(0);
//            Log.d(TAG, "<dequeue> mFrameType=" + frame.mFrameType
//                    + ", timestamp=" + frame.mTimestamp
//                    + ", flags=" + frame.mFlags
//                    + ", lastFrame=" + frame.mLastFrame );
            return frame;
        }
    }

    /**
     * @brief 将帧插入队列头部
     * @param frame : 要插入的帧对象
     * @return None
     */
    public void inqueueHead(AvBaseFrame frame) {
        synchronized (mFrameList) {
            mFrameList.add(0, frame);
//            Log.d(TAG, "<inqueueHead> mFrameType=" + frame.mFrameType
//                    + ", timestamp=" + frame.mTimestamp
//                    + ", flags=" + frame.mFlags
//                    + ", lastFrame=" + frame.mLastFrame );
        }
    }

    /**
     * @brief 获取当前队列大小
     * @return 队列帧数量
     */
    public int size() {
        synchronized (mFrameList) {
            int count = mFrameList.size();
            return count;
        }
    }

    /**
     * @brief 清空队列
     * @return None
     */
    public void clear() {
        synchronized (mFrameList) {
            mFrameList.clear();
        }
    }


    /**
     * @brief 将队列中视频都清除，保留最先的一帧作为EOS帧
     * @return None
     */
    public void resetToEos() {
        synchronized (mFrameList) {
            int count = mFrameList.size();
            if (count <= 0) {
                return;
            }
            AvBaseFrame frame = mFrameList.remove(0);
            mFrameList.clear();
            frame.mLastFrame = true;
            mFrameList.add(frame);
        }
    }
}