
package io.agora.iotlink.transport;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.base.BaseThreadComp;
import io.agora.iotlink.callkit.AgoraService;
import io.agora.iotlink.callkit.CallCmdQueue;
import io.agora.iotlink.callkit.CallkitCmd;
import io.agora.iotlink.callkit.CallkitScheduler;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.sdkimpl.AccountMgr;
import io.agora.iotlink.sdkimpl.AgoraIotAppSdk;

/*
 * @brief 呼叫请求传输通道，
 */
public class HttpReqScheduler extends BaseThreadComp {


    /**
     * @brief 呼叫的异步回调
     */
    public static interface IAsyncDialCallback {
        default void onAsyncDialDone(final AgoraService.CallReqResult dialResult) {}
    }


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/HttpReqScheduler";

    private static long mCmd_Sequence = 1;          ///< 进行操作命令的累加


    //
    // The mesage Id
    //
    private static final int MSGID_HTTPTASK_BASE = 0xA000;
    private static final int MSGID_HTTPTASK_EXECUTE = 0xA001;  ///< 处理命令执行



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private AgoraIotAppSdk mSdkInstance;                        ///< 由外部输入的
    private static final Object mDataLock = new Object();       ///< 同步访问锁,类中所有变量需要进行加锁处理
    private HttpCmdQueue mCmdQueue = new HttpCmdQueue();        ///< 执行命令队列



    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public int initialize(AgoraIotAppSdk sdkInstance) {
        mSdkInstance = sdkInstance;

        mCmdQueue.clear();

        // 启动 HTTP收发线程
        runStart(TAG);

        return ErrCode.XOK;
    }

    public void release() {
        // 停止HTTP收发线程
        runStop();

        mCmdQueue.clear();
    }

    /**
     * @brief 呼叫操作
     */
    public int dial(final String token, final String appId,
                    final String userId, final String deviceId, final String attachMsg,
                    final HttpReqScheduler.IAsyncDialCallback callback) {

        HttpReqCmd dialCmd = new HttpReqCmd();
        dialCmd.mTalkingId = generateTalkId();        // 生成新的talkId

        dialCmd.mAppId = appId;
        dialCmd.mToken = token;
        dialCmd.mUserId = userId;
        dialCmd.mDeviceId = deviceId;
        dialCmd.mAttachMsg = attachMsg;
        dialCmd.mDialCallbk = callback;
        mCmdQueue.inqueue(dialCmd);

        ALog.getInstance().d(TAG, "<dial> inqueue dial command"
                + ", dialCmd=" + dialCmd.toString()
                + ", cmdQueueSize=" + mCmdQueue.size());
        sendSingleMessage(MSGID_HTTPTASK_EXECUTE, 0, 0, null, 0);
        return ErrCode.XOK;
    }

    /**
     * @brief 挂断操作，从队列中删除最近的呼叫命令，如果没有则不做任何处理
     */
    public int hangup() {
        HttpReqCmd lastDialCmd = mCmdQueue.dequeue();
        if (lastDialCmd != null) {
            ALog.getInstance().d(TAG, "<hangup> remove dial cmd from queue"
                    + ", cmd=" + lastDialCmd.toString()
                    + ", cmdQueueSize=" + mCmdQueue.size());
            return ErrCode.XOK;
        }

        return ErrCode.XOK;
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////////// Methods for Override BaseThreadComp //////////////////////
    //////////////////////////////////////////////////////////////////////////
    @Override
    public void processWorkMessage(Message msg) {
        switch (msg.what) {
            case MSGID_HTTPTASK_EXECUTE:
                DoExecuteCmd(msg);
                break;
        }
    }

    @Override
    protected void removeAllMessages() {
        synchronized (mMsgQueueLock) {
            mWorkHandler.removeMessages(MSGID_HTTPTASK_EXECUTE);
        }
    }

    @Override
    protected void processTaskFinsh() {

    }


    void DoExecuteCmd(Message msg) {
        HttpReqCmd cmd = mCmdQueue.dequeue();
        if (cmd == null) {  // 队列中已经没有命令不用再执行了
            ALog.getInstance().d(TAG, "<DoExecuteCmd> no command in queue!");
            return;
        }
        ALog.getInstance().d(TAG, "<DoExecuteCmd> after dequeue, cmdQueueSize=" + mCmdQueue.size());


        // 发送HTTP呼叫请求
        AgoraService.CallReqResult dialResult = AgoraService.getInstance().callDial(
                cmd.mToken, cmd.mAppId, cmd.mUserId, cmd.mDeviceId, cmd.mAttachMsg);
        cmd.mDialCallbk.onAsyncDialDone(dialResult);

        if (mCmdQueue.size() > 0) {  // 队列中还有其他命令，要继续触发执行
            sendSingleMessage(MSGID_HTTPTASK_EXECUTE, 0, 0, null, 0);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    ////////////////////////// Internal Methods //////////////////////////////
    //////////////////////////////////////////////////////////////////////////
    /**
     * @brief 根据操作类型设置 talkId
     */
    String generateTalkId() {
        String talkId = UUID.randomUUID().toString();
        talkId = talkId + "-Dial-" + mCmd_Sequence;
        mCmd_Sequence++;
        return talkId;
    }


}