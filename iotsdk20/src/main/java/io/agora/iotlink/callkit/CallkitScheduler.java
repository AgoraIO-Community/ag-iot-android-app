
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

/*
 * @brief 处理呼叫系统中异步的任务
 */
public class CallkitScheduler {

    /**
     * @brief 活动通话的信息
     */
    public static class ActiveTalkInfo {
        public String mTalkId;
        public String mSessionId;

        @Override
        public String toString() {
            String infoText = "{ mTalkId=" + mTalkId
                    + ", mSessionId=" + mSessionId + " }";
            return infoText;
        }
    }


    /**
     * @brief 呼叫的异步回调
     */
    public static interface IAsyncDialCallback {
        default void onAsyncDialDone(final AgoraService.CallReqResult callReqResult) {}
    }

    /**
     * @brief 挂断的异步回调
     */
    public static interface IAsyncHangupCallback {
        default void onAsyncHangupDone(int errCode) {}
    }

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/CallkitTask";
    private static final int EXIT_WAIT_TIMEOUT = 3000;

    private static long mCmd_Sequence = 1;          ///< 进行操作命令的累加


    //
    // The mesage Id
    //
    private static final int MSGID_CALLTASK_BASE = 0x9000;
    private static final int MSGID_CALLTASK_EXECUTE = 0x9001;  ///< 处理命令执行
    private static final int MSGID_CALLTASK_EXIT = 0x90FF;


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private final Object mWorkExitEvent = new Object();
    private HandlerThread mWorkThread;      ///< 呼叫系统使用内部自己独立的工作线程
    private Handler mWorkHandler;           ///< 呼叫系统工作线程处理器

    private AgoraIotAppSdk mSdkInstance;                        ///< 由外部输入的
    private static final Object mDataLock = new Object();       ///< 同步访问锁,类中所有变量需要进行加锁处理
    private CallCmdQueue mCmdQueue = new CallCmdQueue();        ///< 执行命令队列

    private ActiveTalkInfo mActiveTalkInfo = new ActiveTalkInfo();
    private CallkitContext mLastCallCtx;    ///< 最后一次呼叫的信息


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public int initialize(AgoraIotAppSdk sdkInstance) {
        mSdkInstance = sdkInstance;
        workThreadCreate();

        mCmdQueue.clear();

        return ErrCode.XOK;
    }

    public void release() {
        workThreadDestroy();

        mCmdQueue.clear();
    }

    /**
     * @brief 判断是否是活动sessionId
     */
    public boolean isActiveSessionId(final String sessionId) {
        if (sessionId == null) {
            return false;
        }

        synchronized (mDataLock) {
            if (mActiveTalkInfo.mSessionId == null) {
                return false;
            }

            if (mActiveTalkInfo.mSessionId.compareToIgnoreCase(sessionId) == 0) {
                return true;
            }

            return false;
        }
    }

    public String getActiveTalkId() {
        synchronized (mDataLock) {
            return mActiveTalkInfo.mTalkId;
        }
    }

    public String getActiveSessionId() {
        synchronized (mDataLock) {
            return mActiveTalkInfo.mSessionId;
        }
    }

    /**
     * @brief 对AWS事件数据包进行过滤
     * @return 如果该数据包要丢弃，则返回0； 否则返回true
     */
    public boolean filterAwsEvent(JSONObject jsonState) {
        if (!jsonState.has("callStatus")) {
            ALog.getInstance().e(TAG, "<filterAwsEvent> no field: callStatus");
            return false;
        }
        String sessionId = parseJsonStringValue(jsonState,"sessionId", null);
        ALog.getInstance().d(TAG, "<filterAwsEvent> jsonState=" + jsonState.toString() );

        if (sessionId != null) {  // 对端接听的回应包中没有sessionId字段，这种情况不过滤了
            if (!isActiveSessionId(sessionId)) {
                ALog.getInstance().e(TAG, "<filterAwsEvent> NOT matched active sessionId"
                        + ", activeSessionId=" + getActiveSessionId()
                        + ", awsEventSessionId=" + sessionId);
                return true;
            }
        }

        return false;
    }

    /**
     * @brief 来电操作
     */
    public int incoming(final CallkitContext incomingCallCtx) {
        int cmdCount = mCmdQueue.size();
        if (cmdCount > 0) {
            ALog.getInstance().d(TAG, "<incoming> bad state, cmdCount=" + cmdCount);
            return ErrCode.XERR_BAD_STATE;
        }


        // 设置新的 通话Id 为 活动Id
        String cmdTalkId = generateTalkId(false);
        setActiveTalkInfo(cmdTalkId, incomingCallCtx.sessionId);

        // 设置最后的会话上下文
        setLastCallCtx(incomingCallCtx);

        ALog.getInstance().d(TAG, "<incoming> done, activeTalkInfo=" + mActiveTalkInfo.toString());
        return ErrCode.XOK;
    }

    /**
     * @brief 呼叫操作
     */
    public int dial(final String token, final String appId, final String identityId,
                    final String peerId, final String attachMsg,
                    final IAsyncDialCallback callback) {


        CallkitCmd dialCmd = new CallkitCmd();
        dialCmd.mType = CallkitCmd.CMD_TYPE_DIAL;
        dialCmd.mTalkId = generateTalkId(true);        // 生成新的talkId
        dialCmd.mToken = token;
        dialCmd.mAppId = appId;
        dialCmd.mIdentityId = identityId;
        dialCmd.mPeerId = peerId;
        dialCmd.mAttachMsg = attachMsg;
        dialCmd.mDialCallbk = callback;
        mCmdQueue.inqueue(dialCmd);

        // 设置新的 通话Id 为 活动Id
        setActiveTalkInfo(dialCmd.mTalkId, null);

        ALog.getInstance().d(TAG, "<dial> inqueue dial command"
                + ", hangupCmd=" + dialCmd.toString()
                + ", activeTalkInfo=" + mActiveTalkInfo.toString()
                + ", cmdQueueSize=" + mCmdQueue.size());
        sendMessage(MSGID_CALLTASK_EXECUTE, 0, 0, null);
        return ErrCode.XOK;
    }

    /**
     * @brief 挂断操作
     */
    public int hangup() {
        CallkitContext lastCallCtx = getLastCallCtx();
        if (lastCallCtx != null) {
            ALog.getInstance().d(TAG, "<hangup> NO last call context, already hangup!");
            return ErrCode.XOK;
        }

        // 从队列中参数最近的呼叫命令
        CallkitCmd lastDialCmd = mCmdQueue.removeLastDialCmd();
        if (lastDialCmd != null) {
            ALog.getInstance().d(TAG, "<hangup> remove dial cmd from queue, cmd=" + lastDialCmd.toString());
            return ErrCode.XOK;
        }

        CallkitCmd hangupCmd = new CallkitCmd();
        hangupCmd.mType = CallkitCmd.CMD_TYPE_HANGUP;
        hangupCmd.mTalkId = getActiveTalkId();     // 直接使用当前talkId
        mCmdQueue.inqueue(hangupCmd);

        // 清除当前 活动通话Id，表示当前上层是挂断状态
        setActiveTalkInfo(null, null);

        ALog.getInstance().d(TAG, "<hangup> inqueue hangup command"
                + ", hangupCmd=" + hangupCmd.toString()
                + ", activeTalkInfo=" + mActiveTalkInfo.toString()
                + ", cmdQueueSize=" + mCmdQueue.size());
        sendMessage(MSGID_CALLTASK_EXECUTE, 0, 0, null);
        return ErrCode.XOK;
    }



    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Thread Methods  ///////////////////////////
    ///////////////////////////////////////////////////////////////////////
    void workThreadCreate() {
        mWorkThread = new HandlerThread("CallkitScheduler");
        mWorkThread.start();
        mWorkHandler = new Handler(mWorkThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                workThreadProcessMessage(msg);
            }
        };
    }

    void workThreadDestroy() {
        if (mWorkHandler != null) {
            // 清除所有消息队列中消息
            workThreadClearMessage();

            // 同步等待线程中所有任务处理完成后，才能正常退出线程
            mWorkHandler.sendEmptyMessage(MSGID_CALLTASK_EXIT);
            synchronized (mWorkExitEvent) {
                try {
                    mWorkExitEvent.wait(EXIT_WAIT_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    ALog.getInstance().e(TAG, "<release> exception=" + e.getMessage());
                }
            }
            mWorkHandler = null;
        }

        if (mWorkThread != null) {
            mWorkThread.quit();
            mWorkThread = null;
        }
    }

    void workThreadProcessMessage(Message msg) {
        switch (msg.what) {
            case MSGID_CALLTASK_EXECUTE:
                DoExecuteCmd(msg);
                break;

            case MSGID_CALLTASK_EXIT:  // 工作线程退出消息
                synchronized (mWorkExitEvent) {
                    mWorkExitEvent.notify();    // 事件通知
                }
                break;
        }
    }

    void workThreadClearMessage() {
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(MSGID_CALLTASK_EXECUTE);
            mWorkHandler = null;
        }
    }

    void sendMessage(int what, int arg1, int arg2, Object obj) {
        Message msg = new Message();
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = obj;
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(what);
            mWorkHandler.sendMessage(msg);
        }
    }


    /**
     * @brief 消息线程执行：依次处理队列中的命令
     */
    void DoExecuteCmd(Message msg) {

        CallkitCmd cmd = mCmdQueue.dequeue();
        if (cmd == null) {  // 队列中已经没有命令不用再执行了
            ALog.getInstance().d(TAG, "<DoExecuteCmd> no command in queue!");
        }

        if (cmd.mType == CallkitCmd.CMD_TYPE_DIAL) {
            DoExecuteDial(cmd);

        } else if (cmd.mType == CallkitCmd.CMD_TYPE_HANGUP) {
            DoExecuteHangup(cmd);

        } else {
            ALog.getInstance().e(TAG, "<DoExecuteCmd> [ERROR] invalid cmd type!");
        }

        if (mCmdQueue.size() > 0) {  // 队列中还有其他命令，要继续触发执行
            sendMessage(MSGID_CALLTASK_EXECUTE, 0, 0, null);
        }
    }

    /**
     * @brief 消息线程执行：执行呼叫HTTP操作
     */
    void DoExecuteDial(final CallkitCmd cmd) {
        AccountMgr.AccountInfo accountInfo = mSdkInstance.getAccountInfo();
        if (accountInfo == null) {
            ALog.getInstance().d(TAG, "<accountInfo> [ERROR] accountInfo is NONE");
            return;
        }
        ALog.getInstance().d(TAG, "<DoExecuteDial> ==>BEGIN" + cmd.toString());

        // 执行一次呼叫操作
        AgoraService.CallReqResult callReqResult = AgoraService.getInstance().makeCall(
                cmd.mToken, cmd.mAppId, cmd.mIdentityId, cmd.mPeerId, cmd.mAttachMsg);

        if (callReqResult.mErrCode == ErrCode.XERR_CALLKIT_LOCAL_BUSY) { // 本地端忙
            // 使用最后一次的通话信息执行一次挂断操作
            CallkitContext lastCallCtx = getLastCallCtx();
            if (lastCallCtx != null) {
                AgoraService.getInstance().makeAnswer(accountInfo.mAgoraAccessToken,
                        lastCallCtx.sessionId, lastCallCtx.callerId, lastCallCtx.calleeId,
                        accountInfo.mInventDeviceName, false);
            }

            // 然后重新执行一次呼叫操作
            callReqResult = AgoraService.getInstance().makeCall(
                    cmd.mToken, cmd.mAppId, cmd.mIdentityId, cmd.mPeerId, cmd.mAttachMsg);
        }

        // 更新最后一次呼叫的信息，如果呼叫失败，callReqResult.mCallkitCtx会为空
        setLastCallCtx(callReqResult.mCallkitCtx);

        if (isActiveTalkId(cmd.mTalkId)) { // 回调给上层
            if (callReqResult.mCallkitCtx != null) {  // 设置当前活动的sessionId
                setActiveTalkInfo(cmd.mTalkId, callReqResult.mCallkitCtx.sessionId);
                ALog.getInstance().d(TAG, "<DoExecuteDial> update active sessionId"
                            + ", activeTalkInfo=" + mActiveTalkInfo.toString());
            }

            ALog.getInstance().d(TAG, "<DoExecuteDial> <==END, callback, cmdTalkId=" + cmd.mTalkId);
            cmd.mDialCallbk.onAsyncDialDone(callReqResult);

        } else {
            ALog.getInstance().d(TAG, "<DoExecuteDial> <== END, cmdTalkId=" + cmd.mTalkId);
        }
    }


    /**
     * @brief 消息线程执行：执行挂断HTTP操作
     */
    void DoExecuteHangup(final CallkitCmd cmd) {
        AccountMgr.AccountInfo accountInfo = mSdkInstance.getAccountInfo();
        if (accountInfo == null) {
            ALog.getInstance().d(TAG, "<DoExecuteHangup> [ERROR] accountInfo is NONE");
            return;
        }

        // 使用最后一次的通话信息执行一次挂断操作
        CallkitContext lastCallCtx = getLastCallCtx();
        if (lastCallCtx != null) {
            int errCode = AgoraService.getInstance().makeAnswer(accountInfo.mAgoraAccessToken,
                                    lastCallCtx.sessionId, lastCallCtx.callerId, lastCallCtx.calleeId,
                                    accountInfo.mInventDeviceName, false);
            if (errCode == ErrCode.XOK) {  // 最后一次呼叫信息清空
                setLastCallCtx(null);
            }
            ALog.getInstance().d(TAG, "<DoExecuteHangup> hangup done, errCode=" + errCode
                            + ", cmdTalkId=" + cmd.mTalkId);

        } else {
            ALog.getInstance().d(TAG, "<DoExecuteHangup> do nothing done, cmdTalkId=" + cmd.mTalkId);
        }
    }


    /**
     * @brief 设置/获取 最后一次呼叫信息
     */
    void setLastCallCtx(final CallkitContext callCtx) {
        synchronized (mDataLock) {
            mLastCallCtx = callCtx;
        }
    }

    CallkitContext getLastCallCtx() {
        synchronized (mDataLock) {
            return mLastCallCtx;
        }
    }


    /**
     * @brief 设置/获取 活动通话信息
     */
    void setActiveTalkInfo(final String talkId, final String sessionId) {
        synchronized (mDataLock) {
            mActiveTalkInfo.mTalkId = talkId;
            mActiveTalkInfo.mSessionId = sessionId;
        }
    }

    /**
     * @brief 判断 talkId 是否是当前活动通话Id
     */
    boolean isActiveTalkId(final String talkId) {
        synchronized (mDataLock) {
            if (mActiveTalkInfo.mTalkId == null) {
                return false;
            }

            if (talkId.compareToIgnoreCase(mActiveTalkInfo.mTalkId) == 0) {
                return true;
            }
            return false;
        }
    }

    /**
     * @brief 根据操作类型设置 talkId
     */
    String generateTalkId(boolean bDial) {
        String talkId = UUID.randomUUID().toString();
        talkId = talkId + (bDial ? "-Dial-" : "-Income-") + mCmd_Sequence;
        mCmd_Sequence++;
        return talkId;
    }

    int parseJsonIntValue(JSONObject jsonState, String fieldName, int defVal) {
        try {
            int value = jsonState.getInt(fieldName);
            return value;

        } catch (JSONException e) {
//            ALog.getInstance().e(TAG, "<parseJsonIntValue> "
//                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return defVal;
        }
    }

    String parseJsonStringValue(JSONObject jsonState, String fieldName, String defVal) {
        try {
            String value = jsonState.getString(fieldName);
            return value;

        } catch (JSONException e) {
//            ALog.getInstance().e(TAG, "<parseJsonIntValue> "
//                    + ", fieldName=" + fieldName + ", exp=" + e.toString());
            return defVal;
        }
    }

}