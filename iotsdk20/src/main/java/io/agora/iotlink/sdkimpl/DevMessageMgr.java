
package io.agora.iotlink.sdkimpl;



import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IDevMessageMgr;
import io.agora.iotlink.IotDevMessage;
import io.agora.iotlink.IotDevMsgPage;
import io.agora.iotlink.callkit.AgoraService;
import io.agora.iotlink.logger.ALog;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * @brief 设备消息管理器
 */
public class DevMessageMgr implements IDevMessageMgr {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/DevMsgMgr";


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private ArrayList<IDevMessageMgr.ICallback> mCallbackList = new ArrayList<>();
    private AgoraIotAppSdk mSdkInstance;        ///< 由外部输入的
    private ThreadPoolExecutor mThreadPool;     ///< 执行操作的线程池，由外部输入

    private static final Object mDataLock = new Object();       ///< 同步访问锁,类中所有变量需要进行加锁处理

    private volatile int mStateMachine = DEVMSGMGR_STATE_IDLE;   ///< 当前呼叫状态机





    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    int initialize(AgoraIotAppSdk sdkInstance) {
        mSdkInstance = sdkInstance;
        mThreadPool = sdkInstance.getThreadPool();
        mStateMachine = DEVMSGMGR_STATE_IDLE;
        return ErrCode.XOK;
    }

    void release() {
        synchronized (mCallbackList) {
            mCallbackList.clear();
        }
        mThreadPool = null;
    }


    ///////////////////////////////////////////////////////////////////////
    /////////////////// Override Methods of IDevMessageMgr /////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public int getStateMachine() {
        synchronized (mDataLock) {
            return mStateMachine;
        }
    }

    @Override
    public int registerListener(IDevMessageMgr.ICallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.add(callback);
        }
        return ErrCode.XOK;
    }

    @Override
    public int unregisterListener(IDevMessageMgr.ICallback callback){
        synchronized (mCallbackList) {
            mCallbackList.remove(callback);
        }
        return ErrCode.XOK;
    }


    @Override
    public int mark(List<Long> devMsgIdList) {
        ALog.getInstance().d(TAG, "<mark> idList=" + idListToString(devMsgIdList));
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
                if (account == null) {
                    ALog.getInstance().e(TAG, "<mark> failure, cannot get account");
                    CallbackMarkDone(ErrCode.XERR_ALARM_DEL, devMsgIdList);
                    return;
                }

                int errCode = AgoraService.getInstance().devMsgMarkRead(account.mAgoraAccessToken,
                        account.mAccount, devMsgIdList);
                ALog.getInstance().d(TAG, "<mark> done, errCode=" + errCode
                        + ", alarmIdList=" + idListToString(devMsgIdList));
                CallbackMarkDone(errCode, devMsgIdList);
            }
        });
        return ErrCode.XOK;
    }

    @Override
    public int queryById(long devMsgId) {
        ALog.getInstance().d(TAG, "<queryById> devMsgId=" + devMsgId);
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
                if (account == null) {
                    ALog.getInstance().e(TAG, "<queryById> failure, cannot get account");
                    CallbackQueryInfoDone(ErrCode.XERR_DEVMSG_GETINFO, null);
                    return;
                }

                AgoraService.DevMsgInfoResult infoResult = AgoraService.getInstance().queryDevMsgInfoById(
                        account.mAgoraAccessToken, account.mAccount, devMsgId);
                ALog.getInstance().d(TAG, "<queryById> done, errCode=" + infoResult.mErrCode
                        + ", mDevMessage=" + infoResult.mDevMessage.toString());
                CallbackQueryInfoDone(infoResult.mErrCode, infoResult.mDevMessage);
            }
        });
        return ErrCode.XOK;
    }

    @Override
    public int queryByPage(QueryParam queryParam) {
        ALog.getInstance().d(TAG, "<queryByPage> queryParam=" + queryParam.toString());
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
                if (account == null) {
                    ALog.getInstance().e(TAG, "<queryByPage> failure, cannot get account");
                    CallbackQueryPageDone(ErrCode.XERR_DEVMSG_PAGEQUERY, queryParam, null);
                    return;
                }

                AgoraService.DevMsgPageResult pageResult = AgoraService.getInstance().queryDevMsgByPage(
                        account.mAgoraAccessToken, account.mAccount, account.mInventDeviceName, queryParam);
                ALog.getInstance().d(TAG, "<queryByPage> done, errCode=" + pageResult.mErrCode
                        + ", mDevMsgPage=" + pageResult.mDevMsgPage.toString());
                CallbackQueryPageDone(pageResult.mErrCode, queryParam, pageResult.mDevMsgPage);
            }
        });

        return ErrCode.XOK;
    }

    @Override
    public int queryNumber(QueryParam queryParam) {

        ALog.getInstance().d(TAG, "<queryNumber> queryParam=" + queryParam.toString());
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
                if (account == null) {
                    ALog.getInstance().e(TAG, "<queryNumber> failure, cannot get account");
                    CallbackQueryNumberDone(ErrCode.XERR_DEVMSG_NUMBER, queryParam, 0);
                    return;
                }

                AgoraService.DevMsgNumberResult numberResult = AgoraService.getInstance().queryDevMsgNumber(
                        account.mAgoraAccessToken, account.mAccount, account.mInventDeviceName, queryParam);
                ALog.getInstance().d(TAG, "<queryNumber> done, errCode=" + numberResult.mErrCode
                        + ", mDevMsgNumber=" + numberResult.mDevMsgNumber);
                CallbackQueryNumberDone(numberResult.mErrCode, queryParam, numberResult.mDevMsgNumber);
            }
        });
        return ErrCode.XOK;
    }



    ///////////////////////////////////////////////////////////////////////
    ///////////////////// Methods for all Callback ////////////////////////
    ///////////////////////////////////////////////////////////////////////
    void CallbackMarkDone(int errCode, final List<Long> devMsgIdList) {
        synchronized (mCallbackList) {
            for (IDevMessageMgr.ICallback listener : mCallbackList) {
                listener.onDevMessageMarkDone(errCode, devMsgIdList);
            }
        }
    }

    void CallbackQueryInfoDone(int errCode, final IotDevMessage devMessage) {
        synchronized (mCallbackList) {
            for (IDevMessageMgr.ICallback listener : mCallbackList) {
                listener.onDevMessageInfoQueryDone(errCode, devMessage);
            }
        }
    }

    void CallbackQueryPageDone(int errCode, final IDevMessageMgr.QueryParam queryParam,
                               final IotDevMsgPage devMsgPage) {
        synchronized (mCallbackList) {
            for (IDevMessageMgr.ICallback listener : mCallbackList) {
                listener.onDevMessagePageQueryDone(errCode, queryParam, devMsgPage);
            }
        }
    }

    void CallbackQueryNumberDone(int errCode, final IDevMessageMgr.QueryParam queryParam, long number) {
        synchronized (mCallbackList) {
            for (IDevMessageMgr.ICallback listener : mCallbackList) {
                listener.onDevMessageNumberQueryDone(errCode, queryParam, number);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////
    ///////////////////// Inner Data Structure and Methods ////////////////
    ///////////////////////////////////////////////////////////////////////
    String idListToString(List<Long> idList) {
        String text_info = "( ";
        for (int i = 0; i < idList.size(); i++) {
            String idText = String.valueOf(idList.get(i));
            text_info = text_info + idText + ", ";
        }
        text_info = text_info + " )";
        return text_info;
    }

}
