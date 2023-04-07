
package io.agora.iotlink.callkit;


import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.view.SurfaceView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

import io.agora.avmodule.AvBaseFrame;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.IotDevice;
import io.agora.iotlink.aws.AWSUtils;
import io.agora.iotlink.callkit.AgoraService;
import io.agora.iotlink.callkit.CallkitContext;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.rtcsdk.TalkingEngine;
import io.agora.iotlink.sdkimpl.AccountMgr;
import io.agora.iotlink.sdkimpl.AgoraIotAppSdk;
import io.agora.rtc2.Constants;

/**
 * @brief 呼叫命令队列
 */
public class CallCmdQueue {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/CallCmdQueue";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private ArrayList<CallkitCmd> mCmdQueue = new ArrayList<>();  ///< 通话列表

    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public void inqueue(final CallkitCmd cmd) {
        synchronized (mCmdQueue) {
            mCmdQueue.add(cmd);
        }
    }

    public CallkitCmd dequeue() {
        synchronized (mCmdQueue) {
            int count = mCmdQueue.size();
            if (count <= 0) {
                return null;
            }
            CallkitCmd cmd = mCmdQueue.remove(0);
            return cmd;
        }
    }

    public void innqueueHead(final CallkitCmd cmd) {
        synchronized (mCmdQueue) {
            mCmdQueue.add(0, cmd);
        }
    }

    /**
     * @brief 判断队列最后一个是否呼叫命令，如果是呼叫命令则从队列中移除
     * @return 返回移除的呼叫命令, null表示没有呼叫命令
     */
    public CallkitCmd removeLastDialCmd() {
        synchronized (mCmdQueue) {
            int count = mCmdQueue.size();
            if (count <= 0) {
                return null;
            }

            CallkitCmd cmd = mCmdQueue.get(count-1);
            if (cmd.mType == CallkitCmd.CMD_TYPE_DIAL) {
                mCmdQueue.remove(count-1);
                return cmd;
            }

            return null;
        }
    }



    public int size() {
        synchronized (mCmdQueue) {
            int count = mCmdQueue.size();
            return count;
        }
    }

    public void clear() {
        synchronized (mCmdQueue) {
            mCmdQueue.clear();
        }
    }




}