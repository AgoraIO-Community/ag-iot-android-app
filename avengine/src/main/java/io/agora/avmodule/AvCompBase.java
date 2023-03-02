package io.agora.avmodule;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;



/*
 * @brief 组件基类，包含一个消息队列线程
 */
public class AvCompBase {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    protected static final String TAG = "AVMODULE/AvCompBase";
    protected static final long EXIT_WAIT_TIMEOUT = 3000;    ///< 线程结束等待超时3秒


    //
    // The mesage Id
    //
    protected static final int MSGID_WORK_EXIT = 0xFFFF;




    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    protected final Object mWorkExitEvent = new Object();
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
                        synchronized (mWorkExitEvent) {
                            mWorkExitEvent.notify();    // 事件通知
                        }
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
            synchronized (mWorkExitEvent) {
                try {
                    mWorkExitEvent.wait(EXIT_WAIT_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.e(TAG, "<release> exception=" + e.getMessage());
                }
            }

            mWorkHandler = null;
        }

        if (mWorkThread != null) {
            mWorkThread.quit();
            mWorkThread = null;
            Log.d(TAG, "<runStop> done");
        }

    }


    /*
     * @brief 退出前清空消息队列，子类应该重写该方法
     */
    protected void removeAllMessages() {

    }

    /*
     * @brief 消息处理，子类可以重写该方法实现自己的消息处理
     */
    protected void processWorkMessage(Message msg)   {

    }


}