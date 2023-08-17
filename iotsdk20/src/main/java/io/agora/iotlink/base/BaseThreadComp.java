package io.agora.iotlink.base;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import io.agora.iotlink.ErrCode;


/*
 * @brief 组件基类，包含一个消息队列线程
 */
public class BaseThreadComp {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    protected static final String TAG = "IOTSDK/BaseComp";
    protected static final long EXIT_WAIT_TIMEOUT = 3000;    ///< 线程结束等待超时3秒


    //
    // The message Id
    //
    protected static final int MSGID_WORK_EXIT = 0xFFFF;




    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    protected final BaseEvent mWorkExitEvent = new BaseEvent();
    protected final Object mMsgQueueLock = new Object();
    protected HandlerThread mWorkThread;
    protected Handler mWorkHandler;
    protected String mComponentName;


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief 启动组件线程运行
     * @param compName : 组件名称
     * @return error code
     */
    protected int runStart(String compName) {
        mComponentName = compName;

        // 启动工作线程
        mWorkThread = new HandlerThread(compName);
        mWorkThread.start();
        mWorkHandler = new Handler(mWorkThread.getLooper() ){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what)
                {
                    case MSGID_WORK_EXIT: {  // 工作线程退出消息
                        removeAllMessages();
                        processTaskFinsh();
                        mWorkExitEvent.setEvent(0);
                    } break;

                    default: {
                        processWorkMessage(msg);
                    }  break;
                }
            }
        };

        Log.d(TAG, "<runStart> done");
        return ErrCode.XOK;
    }

    /*
     * @brief 停止组件线程运行
     */
    public void runStop()   {
        if (mWorkHandler != null) {
            // 同步等待线程中所有任务处理完成后，才能正常退出线程
            removeAllMessages();
            mWorkHandler.sendEmptyMessage(MSGID_WORK_EXIT);
            mWorkExitEvent.waitEvent(EXIT_WAIT_TIMEOUT);
            mWorkHandler = null;
        }

        if (mWorkThread != null) {
            mWorkThread.quit();
            mWorkThread = null;
            Log.d(TAG, "<runStop> done");
        }
    }

    /**
     * @brief 发送消息，如果队列中有相同的消息则删除
     */
    public void sendSingleMessage(Message msg) {
        synchronized (mMsgQueueLock) {
            mWorkHandler.removeMessages(msg.what);
            mWorkHandler.sendMessage(msg);
        }
    }

    public void sendSingleMessage(int what, int arg1, int arg2, Object obj, long delayTime) {
        Message msg = new Message();
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = obj;

        synchronized (mMsgQueueLock) {
            if (delayTime > 0) {
                mWorkHandler.removeMessages(what);
                mWorkHandler.sendMessageDelayed(msg, delayTime);
            } else {
                mWorkHandler.removeMessages(what);
                mWorkHandler.sendMessage(msg);
            }
        }
    }

    /**
     * @brief 从消息队列中移除相应的消息
     */
    public void removeMessage(int what) {
        synchronized (mMsgQueueLock) {
           mWorkHandler.removeMessages(what);
        }
    }



    /**
     * @brief 退出前清空消息队列，子类应该重写该方法
     */
    protected void removeAllMessages() {
    }

    /**
     * @brief 消息处理，运行在组件线程上，子类可以重写该方法实现自己的消息处理
     */
    protected void processWorkMessage(Message msg)   {
    }

    /**
     * @brief 退出前的释放处理，运行在组件线程上，子类可以重写该方法实现自己的结束处理
     */
    protected void processTaskFinsh()   {
    }

}