
package io.agora.iotlink.transport;


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
 * @brief Http命令队列
 */
public class HttpCmdQueue {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/HttpCmdQueue";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private ArrayList<HttpReqCmd> mCmdQueue = new ArrayList<>();  ///< 通话列表

    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public void inqueue(final HttpReqCmd cmd) {
        synchronized (mCmdQueue) {
            mCmdQueue.add(cmd);
        }
    }

    public HttpReqCmd dequeue() {
        synchronized (mCmdQueue) {
            int count = mCmdQueue.size();
            if (count <= 0) {
                return null;
            }
            HttpReqCmd cmd = mCmdQueue.remove(0);
            return cmd;
        }
    }

    public void innqueueHead(final HttpReqCmd cmd) {
        synchronized (mCmdQueue) {
            mCmdQueue.add(0, cmd);
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