package io.agora.iotlink.base;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.logger.ALog;


/*
 * @brief 基本事件
 */
public class BaseEvent {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/BaseEvent";

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private final Object mEventObj = new Object();
    private int mAttachValue = 0;

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 设置事件
     * @param attachValue : 附加信息值
     */
    public void setEvent(int attachValue) {
        synchronized (mEventObj) {
            mAttachValue = attachValue;
            mEventObj.notifyAll();
        }
    }

    /**
     * @brief 阻塞等待事件
     * @param timeout : 等待超时，单位毫秒
     * @return 返回 XOK表示等待成功；否则表示等待超时
     */
    public int waitEvent(long timeout)   {

        synchronized (mEventObj) {
            try {
                mEventObj.wait(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
                ALog.getInstance().e(TAG, "<waitEvent> [EXCEPTION] timeout=" + timeout
                        + ", exp=" + e.toString());
                return ErrCode.XERR_TIMEOUT;
            }
        }

        return ErrCode.XOK;
    }

    /**
     * @brief 获取附加信息值
     * @return 返回附加信息值
     */
    public int getAttachValue() {
        synchronized (mEventObj) {
            return mAttachValue;
        }
    }
}