package io.agora.sdkwayang.transport;

import java.util.ArrayList;
import java.util.UUID;

/*
 * @brief 线程安全的 传输数据包队列
 */
public class TransWyPktQueue {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTWY/WyPktQueue";


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private ArrayList<TransWyPacket> mPktList = new ArrayList<>();  ///< 帧列表


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 将数据包插入队列尾部
     * @param packet : 要插入的数据包
     * @return None
     */
    public void inqueue(TransWyPacket packet) {
        synchronized (mPktList) {
            mPktList.add(packet);
//            Log.d(TAG, "<inqueue> packet=" + packet);
        }
    }

    /**
     * @brief 从队列头提取一个数据包对象
     * @return 返回提取到的数据包，如果队列为空则返回null
     */
    public TransWyPacket dequeue() {
        synchronized (mPktList) {
            int count = mPktList.size();
            if (count <= 0) {
                return null;
            }
            TransWyPacket packet = mPktList.remove(0);
//            Log.d(TAG, "<dequeue> packet=" + packet);
            return packet;
        }
    }

    /**
     * @brief 将数据包插入队列头部
     * @param packet : 要插入的数据包
     * @return None
     */
    public void inqueueHead(TransWyPacket packet) {
        synchronized (mPktList) {
            mPktList.add(0, packet);
//            Log.d(TAG, "<inqueueHead> packet=" + packet);
        }
    }


    /**
     * @brief 获取当前队列大小
     * @return 队列帧数量
     */
    public int size() {
        synchronized (mPktList) {
            int count = mPktList.size();
            return count;
        }
    }

    /**
     * @brief 清空队列
     * @return None
     */
    public void clear() {
        synchronized (mPktList) {
            mPktList.clear();
        }
    }


}