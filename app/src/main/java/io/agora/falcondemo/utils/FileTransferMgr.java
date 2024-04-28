package io.agora.falcondemo.utils;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatImageView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IConnectionMgr;
import io.agora.iotlink.IConnectionObj;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.sdkimpl.ConnectionObj;


public class FileTransferMgr {


    /**
     * @brief 链接操作的回调接口
     */
    public static interface ICallback {

        /**
         * @brief 传输接收单个文件开始回调
         * @param connectObj : 当前连接对象
         * @param startDescrption : 启动描述
         */
        default void onTransMgrRecvStart(final IConnectionObj connectObj, final byte[] startDescrption) { }

        /**
         * @brief 传输接收单个文件数据回调
         * @param connectObj : 当前连接对象
         * @param recvedData : 接收到的数据内容
         */
        default void onTransMgrRecvData(final IConnectionObj connectObj, final byte[] recvedData) { }

        /**
         * @brief 传输接收单个文件完成回调
         * @param connectObj : 当前连接对象
         * @param transferEnd : 是否整个传输都结束
         * @param doneDescrption: 结束描述
         */
        default void onTransMgrRecvDone(final IConnectionObj connectObj, boolean transferEnd,
                                         final byte[] doneDescrption, final byte[] md5Value) { }

        /**
         * @brief 传输过程中遇到问题，本次传输失败（可以重新进行下一次传输）
         * @param connectObj : 当前连接对象
         * @param errCode : 传输错误码，ERR_NETWORK----表示网络问题导致传输失败
         */
        default void onTransMgrError(final IConnectionObj connectObj, int errCode) { }
    }


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/FileTransfer";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private ArrayList<ICallback> mCallbackList = new ArrayList<>();
    private HashMap<IConnectionObj, MessageDigest> mVerifyMap = new HashMap<>();  ///< 校验映射表
    private static FileTransferMgr mInstance = null;

    ///////////////////////////////////////////////////////////////
    /////////////////////// Public Methods ////////////////////////
    ///////////////////////////////////////////////////////////////
    public static FileTransferMgr getInstance() {
        if (mInstance == null) {
            synchronized (ALog.class) {
                if(mInstance == null) {
                    mInstance = new FileTransferMgr();
                }
            }
        }
        return mInstance;
    }

    public int registerListener(ICallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.add(callback);
        }
        return ErrCode.XOK;
    }

    public int unregisterListener(ICallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.remove(callback);
        }
        return ErrCode.XOK;
    }

    /**
     * @brief 当开始一个传输，则添加一个校验器
     */
    public int transferStart(final IConnectionObj connectObj, final String startMessage) {
        MessageDigest md5Digest;
        try {
            md5Digest = MessageDigest.getInstance("MD5");
            md5Digest.reset();
        } catch (NoSuchAlgorithmException noAlgorithmExp) {
            noAlgorithmExp.printStackTrace();
            return ErrCode.XERR_UNSUPPORTED;
        }

        synchronized (mVerifyMap) {
            mVerifyMap.put(connectObj, md5Digest);
        }

        int ret = connectObj.fileTransferStart(startMessage);

        ALog.getInstance().d(TAG, "<transferStart> done, connectObj=" + connectObj
                + ", startMessage=" + startMessage + ", ret=" + ret);
        return ret;
    }

    /**
     * @brief 当结束一个传输，则删除一个校验器
     */
    public void transferStop(final IConnectionObj connectObj) {

        connectObj.fileTransferStop();

        MessageDigest md5Digest;
        synchronized (mVerifyMap) {
            md5Digest = mVerifyMap.remove(connectObj);
        }

        ALog.getInstance().d(TAG, "<transferStop> done, connectObj=" + connectObj);
    }

    /**
     * @brief 单个文件传输开始回调
     */
    public void onFileTransRecvStart(final IConnectionObj connectObj, final byte[] startDescrption) {
        // 找到相应的校验器
        MessageDigest md5Digest;
        synchronized (mVerifyMap) {
            md5Digest = mVerifyMap.get(connectObj);
        }
        if (md5Digest == null) {
            ALog.getInstance().e(TAG, "<onFileTransRecvStart> NOT found, connectObj=" + connectObj);
            return;
        }

        // 重置校验器
        md5Digest.reset();
        ALog.getInstance().d(TAG, "<onFileTransRecvStart> done, connectObj=" + connectObj);

        // 回调给上层
        synchronized (mCallbackList) {
            for (ICallback listener : mCallbackList) {
                listener.onTransMgrRecvStart(connectObj, startDescrption);
            }
        }
    }

    /**
     * @brief 单个文件传输内容回调
     */
    public void onFileTransRecvData(final IConnectionObj connectObj, final byte[] recvedData) {
        // 找到相应的校验器
        MessageDigest md5Digest;
        synchronized (mVerifyMap) {
            md5Digest = mVerifyMap.get(connectObj);
        }
        if (md5Digest == null) {
            ALog.getInstance().e(TAG, "<onFileTransRecvData> NOT found, connectObj=" + connectObj);
            return;
        }

        // 更新校验数据
        md5Digest.update(recvedData);

        // 回调给上层
        synchronized (mCallbackList) {
            for (ICallback listener : mCallbackList) {
                listener.onTransMgrRecvData(connectObj, recvedData);
            }
        }
    }

    /**
     * @brief 单个文件传输完成回调
     */
    public void onFileTransRecvDone(final IConnectionObj connectObj, boolean transferEnd,
                                    final byte[] doneDescrption) {
        // 找到相应的校验器
        MessageDigest md5Digest;
        synchronized (mVerifyMap) {
            md5Digest = mVerifyMap.get(connectObj);
        }
        if (md5Digest == null) {
            ALog.getInstance().e(TAG, "<onFileTransRecvDone> NOT found, connectObj=" + connectObj);
            return;
        }

        // 获取校验数据
        byte[] md5Value = md5Digest.digest();

        // 回调给上层
        synchronized (mCallbackList) {
            for (ICallback listener : mCallbackList) {
                listener.onTransMgrRecvDone(connectObj, transferEnd, doneDescrption, md5Value);
            }
        }
    }


}