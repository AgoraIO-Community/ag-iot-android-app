/**
 * @file DeviceMgr.java
 * @brief This file implement the devices management
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.sdkimpl;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.IDeviceMgr;
import io.agora.iotlink.IotDevice;
import io.agora.iotlink.IotOutSharer;
import io.agora.iotlink.IotPropertyDesc;
import io.agora.iotlink.IotShareMessage;
import io.agora.iotlink.IotShareMsgPage;
import io.agora.iotlink.aws.AWSUtils;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.lowservice.AgoraLowService;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



/*
 * @brief 设备管理器
 */
public class DeviceMgr implements IDeviceMgr {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/DeviceMgr";
    private static final int EXIT_WAIT_TIMEOUT = 3000;

    //
    // The mesage Id
    //
    private static final int MSGID_DEVMGR_BASE = 0x2000;
    private static final int MSGID_DEVMGR_QUERY = 0x2001;
    private static final int MSGID_DEVMGR_BIND = 0x2002;
    private static final int MSGID_DEVMGR_UNBIND = 0x2003;
    private static final int MSGID_DEVMGR_SETPROP = 0x2004;
    private static final int MSGID_DEVMGR_GETPROP = 0x2005;
    private static final int MSGID_DEVMGR_RECVSHADOW = 0x2006;
    private static final int MSGID_DEVMGR_RENAME = 0x2007;
    private static final int MSGID_DEVMGR_ON_OFF_LINE = 0x2008;
    private static final int MSGID_DEVMGR_ACTION_UPDATE = 0x2009;
    private static final int MSGID_DEVMGR_PROPERTY_UPDATE = 0x200A;
    private static final int MSGID_DEVMGR_PRODUCT_QUERY = 0x200B;
    private static final int MSGID_DEVMGR_SHARE = 0x200D;
    private static final int MSGID_DEVMGR_DESHARE = 0x200E;
    private static final int MSGID_DEVMGR_ACCEPT = 0x200F;
    private static final int MSGID_DEVMGR_QUERY_SHARABLE = 0x2010;
    private static final int MSGID_DEVMGR_QUERY_OUTSHARER = 0x2011;
    private static final int MSGID_DEVMGR_QUERY_SHAREDIN = 0x2012;
    private static final int MSGID_DEVMGR_QUERYMSG_BYPAGE = 0x2013;
    private static final int MSGID_DEVMGR_QUERYMSG_BYID = 0x2014;
    private static final int MSGID_DEVMGR_DELMSG = 0x2015;
    private static final int MSGID_DEVMGR_GET_MCUVER = 0x2016;
    private static final int MSGID_DEVMGR_UPGRADE_MCUVER = 0x2017;
    private static final int MSGID_DEVMGR_UPGRADE_GETSTATUS = 0x2018;
    private static final int MSGID_DEVMGR_QUERY_PROPDESC = 0x2019;
    private static final int MSGID_DEVMGR_EXIT = 0x2099;

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private ArrayList<IDeviceMgr.ICallback> mCallbackList = new ArrayList<>();
    private AgoraIotAppSdk mSdkInstance;                        ///< 由外部输入的
    private HandlerThread mWorkThread;                          ///< 设备管理的工作线程
    private Handler mWorkHandler;                               ///< 工作线程Handler，从SDK获取到
    private final Object mWorkExitEvent = new Object();

    private static final Object mDataLock = new Object();       ///< 同步访问锁,类中所有变量需要进行加锁处理
    private volatile int mStateMachine = DEVMGR_STATE_IDLE;     ///< 当前状态机

    private ArrayList<IotDevice> mBindDevList = new ArrayList<>();  ///< 当前已经绑定的设备
    private IotDevice mBindingDev;


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    int initialize(AgoraIotAppSdk sdkInstance) {
        mSdkInstance = sdkInstance;
        //mWorkHandler = sdkInstance.getWorkHandler();

        mWorkThread = new HandlerThread("AppSdk");
        mWorkThread.start();
        mWorkHandler = new Handler(mWorkThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                workThreadProcessMessage(msg);
            }
        };

        mStateMachine = DEVMGR_STATE_IDLE;
        return ErrCode.XOK;
    }

    void release() {
        workThreadClearMessage();

        synchronized (mCallbackList) {
            mCallbackList.clear();
        }
    }

    void workThreadProcessMessage(Message msg) {
        switch (msg.what) {
            case MSGID_DEVMGR_QUERY: {
                DoDeviceListQuery(msg);
            } break;

            case MSGID_DEVMGR_BIND: {
                DoDeviceBind(msg);
            } break;

            case MSGID_DEVMGR_UNBIND: {
                DoDeviceUnbind(msg);
            } break;

            case MSGID_DEVMGR_RENAME: {
                DoDeviceRename(msg);
            } break;

            case MSGID_DEVMGR_QUERY_PROPDESC: {
                DoQueryAllPropDesc(msg);
            } break;

            case MSGID_DEVMGR_SETPROP: {
                DoDeviceSetProp(msg);
            } break;

            case MSGID_DEVMGR_GETPROP: {
                DoDeviceGetProp(msg);
            } break;

            case MSGID_DEVMGR_RECVSHADOW: {
                DoRecvShadowProperty(msg);
            } break;

            case MSGID_DEVMGR_ON_OFF_LINE: {
                DoDeviceOnOffLine(msg);
            } break;

            case MSGID_DEVMGR_ACTION_UPDATE: {
                DoDeviceActionUpdate(msg);
            } break;

            case MSGID_DEVMGR_PROPERTY_UPDATE: {
                DoDevicePropertyUpdate(msg);
            } break;

            case MSGID_DEVMGR_PRODUCT_QUERY: {
                DoProductListQuery(msg);
            } break;

            case MSGID_DEVMGR_SHARE: {
                DoShareDevice(msg);
            } break;

            case MSGID_DEVMGR_DESHARE: {
                DoDeshareDevice(msg);
            } break;

            case MSGID_DEVMGR_ACCEPT: {
                DoAcceptDevice(msg);
            } break;

            case MSGID_DEVMGR_QUERY_SHARABLE: {
                DoQuerySharable(msg);
            } break;

            case MSGID_DEVMGR_QUERY_OUTSHARER: {
                DoQueryOutSharers(msg);
            } break;

            case MSGID_DEVMGR_QUERY_SHAREDIN: {
                DoQuerySharedIn(msg);
            } break;

            case MSGID_DEVMGR_QUERYMSG_BYPAGE: {
                DoQueryShareMsgByPage(msg);
            } break;

            case MSGID_DEVMGR_QUERYMSG_BYID: {
                DoQueryShareMsgById(msg);
            } break;

            case MSGID_DEVMGR_DELMSG: {
                DoDeleteShareMsg(msg);
            } break;

            case MSGID_DEVMGR_GET_MCUVER: {
                DoGetMcuVersion(msg);
            } break;

            case MSGID_DEVMGR_UPGRADE_MCUVER: {
                DoUpgradeMcuVersion(msg);
            } break;

            case MSGID_DEVMGR_UPGRADE_GETSTATUS: {
                DoGetMcuUpgradingStatus(msg);
            } break;

        }
    }

    void workThreadClearMessage() {
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(MSGID_DEVMGR_QUERY);
            mWorkHandler.removeMessages(MSGID_DEVMGR_BIND);
            mWorkHandler.removeMessages(MSGID_DEVMGR_UNBIND);
            mWorkHandler.removeMessages(MSGID_DEVMGR_RENAME);
            mWorkHandler.removeMessages(MSGID_DEVMGR_SETPROP);
            mWorkHandler.removeMessages(MSGID_DEVMGR_GETPROP);
            mWorkHandler.removeMessages(MSGID_DEVMGR_QUERY_PROPDESC);
            mWorkHandler.removeMessages(MSGID_DEVMGR_RECVSHADOW);
            mWorkHandler.removeMessages(MSGID_DEVMGR_ON_OFF_LINE);
            mWorkHandler.removeMessages(MSGID_DEVMGR_ACTION_UPDATE);
            mWorkHandler.removeMessages(MSGID_DEVMGR_PROPERTY_UPDATE);
            mWorkHandler.removeMessages(MSGID_DEVMGR_SHARE);
            mWorkHandler.removeMessages(MSGID_DEVMGR_DESHARE);
            mWorkHandler.removeMessages(MSGID_DEVMGR_ACCEPT);
            mWorkHandler.removeMessages(MSGID_DEVMGR_QUERY_SHARABLE);
            mWorkHandler.removeMessages(MSGID_DEVMGR_QUERY_OUTSHARER);
            mWorkHandler.removeMessages(MSGID_DEVMGR_QUERY_SHAREDIN);
            mWorkHandler.removeMessages(MSGID_DEVMGR_QUERYMSG_BYPAGE);
            mWorkHandler.removeMessages(MSGID_DEVMGR_QUERYMSG_BYID);
            mWorkHandler.removeMessages(MSGID_DEVMGR_DELMSG);
            mWorkHandler.removeMessages(MSGID_DEVMGR_GET_MCUVER);
            mWorkHandler.removeMessages(MSGID_DEVMGR_UPGRADE_MCUVER);
            mWorkHandler.removeMessages(MSGID_DEVMGR_UPGRADE_GETSTATUS);

            // 同步等待线程中所有任务处理完成后，才能正常退出线程
            mWorkHandler.sendEmptyMessage(MSGID_DEVMGR_EXIT);
            synchronized (mWorkExitEvent) {
                try {
                    mWorkExitEvent.wait(EXIT_WAIT_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    ALog.getInstance().e(TAG, "<workThreadClearMessage> exception=" + e.getMessage());
                }
            }

            mWorkHandler = null;
        }

        if (mWorkThread != null) {
            mWorkThread.quit();
            mWorkThread = null;
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

    void sendMessageDelay(int what, int arg1, int arg2, Object obj, long delayTime) {
        Message msg = new Message();
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = obj;
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(what);
            mWorkHandler.sendMessageDelayed(msg, delayTime);
        }
    }


    /*
     * @brief 根据设备MAC，在绑定设备列表中找到相应的设备
     *          仅被呼叫系统模块，在其工作线程中调用
     */
    IotDevice findBindDeviceByDevMac(String deviceMac)
    {
        synchronized (mDataLock) {
            int devCount = mBindDevList.size();
            for (int i = 0; i < devCount; i++) {
                IotDevice iotDevice = mBindDevList.get(i);
                if (iotDevice.mDeviceID.compareToIgnoreCase(deviceMac) == 0) {
                    return iotDevice;
                }
            }
        }
        return null;
    }


    ///////////////////////////////////////////////////////////////////////
    /////////////////// Override Methods of IDeviceMgr ///////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public int getStateMachine() {
        synchronized (mDataLock) {
            return mStateMachine;
        }
    }

    @Override
    public int registerListener(IDeviceMgr.ICallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.add(callback);
        }
        return ErrCode.XOK;
    }

    @Override
    public int unregisterListener(IDeviceMgr.ICallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.remove(callback);
        }
        return ErrCode.XOK;
    }

    @Override
    public int queryAllDevices() {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<queryAllDevices> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        sendMessage(MSGID_DEVMGR_QUERY, 0, 0, null);
        ALog.getInstance().d(TAG, "<queryAllDevices> ");
        return ErrCode.XOK;
    }

    @Override
    public List<IotDevice> getBindDevList() {
        synchronized (mDataLock) {
            return mBindDevList;
        }
    }

    @Override
    public int addDevice(String productKey, String deviceMac) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<addDevice> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        mBindingDev = new IotDevice();
        mBindingDev.mProductID = productKey;
        mBindingDev.mDeviceID = deviceMac;
        sendMessage(MSGID_DEVMGR_BIND, 0, 0, mBindingDev);
        ALog.getInstance().d(TAG, "<addDevice> ");
        return ErrCode.XOK;
    }

    @Override
    public int removeDevice(IotDevice removingDevice) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<removeDevice> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        sendMessage(MSGID_DEVMGR_UNBIND, 0, 0, removingDevice);
        ALog.getInstance().d(TAG, "<removeDevice> removingDevice=" + removingDevice.toString());
        return ErrCode.XOK;
    }

    @Override
    public int renameDevice(IotDevice iotDevice, String newName) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<renameDevice> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        DevRenameParam renameParam = new DevRenameParam();
        renameParam.mIotDevice = iotDevice;
        renameParam.mNewName = newName;
        sendMessage(MSGID_DEVMGR_RENAME, 0, 0, renameParam);
        ALog.getInstance().d(TAG, "<renameDevice> iotDevice=" + iotDevice.toString()
                                + ", newName=" + newName);
        return ErrCode.XOK;
    }

    @Override
    public int queryAllPropertyDesc(final String deviceID, final String productNumber) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<queryAllPropertyDesc> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        QueryPropDescParam queryParam = new QueryPropDescParam();
        queryParam.mDeviceID = deviceID;
        queryParam.mProductNumber = productNumber;
        sendMessage(MSGID_DEVMGR_QUERY_PROPDESC, 0, 0, queryParam);
        ALog.getInstance().d(TAG, "<queryAllPropertyDesc> deviceID=" + deviceID
                + ", productNumber=" + productNumber);
        return ErrCode.XOK;
    }

    @Override
    public int setDeviceProperty(IotDevice iotDevice, Map<String, Object> properties) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<setDeviceProperty> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        SetPropParam setPropParam = new SetPropParam();
        setPropParam.mIotDevice = iotDevice;
        setPropParam.mProperties = properties;
        sendMessage(MSGID_DEVMGR_SETPROP, 0, 0, setPropParam);
        ALog.getInstance().d(TAG, "<setDeviceProperty> iotDevice=" + iotDevice.toString()
                + ", properties=" + properties.toString());
        return ErrCode.XOK;
    }

    @Override
    public int getDeviceProperty(IotDevice iotDevice) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<getDeviceProperty> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        sendMessage(MSGID_DEVMGR_GETPROP, 0, 0, iotDevice);
        ALog.getInstance().d(TAG, "<getDeviceProperty> iotDevice=" + iotDevice.toString());
        return ErrCode.XOK;
    }

    @Override
    public int getMcuVersionInfo(IotDevice iotDevice) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<getMcuVersionInfo> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        sendMessage(MSGID_DEVMGR_GET_MCUVER, 0, 0, iotDevice);
        ALog.getInstance().d(TAG, "<getMcuVersionInfo> iotDevice=" + iotDevice.toString());
        return ErrCode.XOK;
    }

    @Override
    public int upgradeMcuVersion(IotDevice iotDevice, long upgradeId, int decide) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<upgradeMcuVersion> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        McuUpgradeVerParam upgradeParam = new McuUpgradeVerParam();
        upgradeParam.mIotDevice = iotDevice;
        upgradeParam.mUpgradeId = upgradeId;
        upgradeParam.mDecide = decide;
        sendMessage(MSGID_DEVMGR_UPGRADE_MCUVER, 0, 0, upgradeParam);
        ALog.getInstance().d(TAG, "<upgradeMcuVersion> iotDevice=" + iotDevice.toString()
                    + ", upgradeId=" + upgradeId + ", decide=" + decide);
        return ErrCode.XOK;
    }

    @Override
    public int getMcuUpgradeStatus(IotDevice iotDevice, long upgradeId) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<upgradeMcuVersion> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        McuUpgradeVerParam upgradeParam = new McuUpgradeVerParam();
        upgradeParam.mIotDevice = iotDevice;
        upgradeParam.mUpgradeId = upgradeId;
        sendMessage(MSGID_DEVMGR_UPGRADE_GETSTATUS, 0, 0, upgradeParam);
        ALog.getInstance().d(TAG, "<getMcuUpgradeStatus> iotDevice=" + iotDevice.mDeviceName
                + ", upgradeId=" + upgradeId);
        return ErrCode.XOK;
    }

    @Override
    public int queryProductList(final ProductQueryParam queryParam) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<queryProductList> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        sendMessage(MSGID_DEVMGR_PRODUCT_QUERY, 0, 0, queryParam);
        ALog.getInstance().d(TAG, "<queryProductList> queryParam=" + queryParam.toString());
        return ErrCode.XOK;
    }

    @Override
    public int shareDevice(final IotDevice iotDevice, final String sharingAccount,
                           int permission, boolean needPeerAgree) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<shareDevice> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        ShareDevOptParam shareParam = new ShareDevOptParam();
        shareParam.mIotDevice = iotDevice;
        shareParam.mAccount = sharingAccount;
        shareParam.mPermission = permission;
        shareParam.mForce = (needPeerAgree ? false : true);
        sendMessage(MSGID_DEVMGR_SHARE, 0, 0, shareParam);
        ALog.getInstance().d(TAG, "<shareDevice> deviceID=" + iotDevice.mDeviceID
                + ", sharingAccount=" + sharingAccount
                + ", permission=" + permission + ", needPeerAgree=" + needPeerAgree);
        return ErrCode.XOK;
    }

    @Override
    public int deshareDevice(final IotOutSharer outSharer) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<deshareDevice> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        ShareDevOptParam deshareParam = new ShareDevOptParam();
        deshareParam.mOutSharer = outSharer;
        sendMessage(MSGID_DEVMGR_DESHARE, 0, 0, deshareParam);
        ALog.getInstance().d(TAG, "<deshareDevice> mDeviceID=" + outSharer.mDeviceID);
        return ErrCode.XOK;
    }

    @Override
    public int acceptDevice(final String deviceName, final String order) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<acceptDevice> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        ShareDevAcceptParam accpetParam = new ShareDevAcceptParam();
        accpetParam.mDeviceName = deviceName;
        accpetParam.mOrder = order;
        sendMessage(MSGID_DEVMGR_ACCEPT, 0, 0, accpetParam);
        ALog.getInstance().d(TAG, "<acceptDevice> deviceName=" + deviceName
                + ", order=" + order);
        return ErrCode.XOK;
    }

    @Override
    public int querySharableDevList() {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<querySharableDevList> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        sendMessage(MSGID_DEVMGR_QUERY_SHARABLE, 0, 0, null);
        ALog.getInstance().d(TAG, "<querySharableDevList> ");
        return ErrCode.XOK;
    }

    @Override
    public int queryOutSharerList(final String deviceID) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<queryOutSharerList> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        ShareDevOptParam deshareParam = new ShareDevOptParam();
        deshareParam.mIotDevice = new IotDevice();
        deshareParam.mIotDevice.mDeviceID = deviceID;
        sendMessage(MSGID_DEVMGR_QUERY_OUTSHARER, 0, 0, deshareParam);
        ALog.getInstance().d(TAG, "<queryOutSharerList> deviceID=" + deviceID);
        return ErrCode.XOK;
    }

    @Override
    public int queryInSharedDevList() {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<queryInSharedDevList> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        sendMessage(MSGID_DEVMGR_QUERY_SHAREDIN, 0, 0, null);
        ALog.getInstance().d(TAG, "<queryInSharedDevList> ");
        return ErrCode.XOK;
    }


    @Override
    public int queryShareMsgByPage(int pageNumber, int pageSize, int auditStatus) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<queryShareMsgByPage> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        ShareMsgOptParam queryParam = new ShareMsgOptParam();
        queryParam.mPageNumber = pageNumber;
        queryParam.mPageSize = pageSize;
        queryParam.mAuditStatus = auditStatus;
        sendMessage(MSGID_DEVMGR_QUERYMSG_BYPAGE, 0, 0, queryParam);
        ALog.getInstance().d(TAG, "<queryShareMsgByPage> pageNumber=" + pageNumber
                    + ", pageSize=" + pageSize + ", auditStatus=" + auditStatus);
        return ErrCode.XOK;
    }

    @Override
    public int queryShareMsgById(long messageId) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<queryShareMsgById> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        ShareMsgOptParam queryParam = new ShareMsgOptParam();
        queryParam.mMessageId = messageId;
        sendMessage(MSGID_DEVMGR_QUERYMSG_BYID, 0, 0, queryParam);
        ALog.getInstance().d(TAG, "<queryShareMsgById> messageId=" + messageId);
        return ErrCode.XOK;
    }


    @Override
    public int deleteShareMsg(long messageId) {
        if (!mSdkInstance.isAccountReady()) {
            ALog.getInstance().e(TAG, "<deleteShareMsg> bad state, sdkState="
                    + mSdkInstance.getStateMachine());
            return ErrCode.XERR_BAD_STATE;
        }

        ShareMsgOptParam deleteParam = new ShareMsgOptParam();
        deleteParam.mMessageId = messageId;
        sendMessage(MSGID_DEVMGR_DELMSG, 0, 0, deleteParam);
        ALog.getInstance().d(TAG, "<deleteShareMsg> messageId=" + messageId);
        return ErrCode.XOK;
    }





    //////////////////////////////////////////////////////////////
    ///////////////////////// AWS线程中回调事件 ////////////////////
    //////////////////////////////////////////////////////////////
    /*
     * @brief 运行在AWS回调中，接收到shadow数据更新事件
     */
    void onReceiveShadow(String things_name, JSONObject jsonObject) {
        RecvShadowParam recvShadowParam = new RecvShadowParam();
        recvShadowParam.mThingsName = things_name;
        recvShadowParam.mJsonObj = jsonObject;

        if (mWorkHandler != null) {
            Message msg = new Message();
            msg.what = MSGID_DEVMGR_RECVSHADOW;
            msg.obj = recvShadowParam;
            mWorkHandler.sendMessage(msg);
        }
    }

    /*
     * @brief 运行在AWS回调中，接收到设备上下线事件
     */
    void onDevOnlineChanged(String deviceMac, String deviceId, boolean online) {
        DevOnOffLineParam onOffLineParam = new DevOnOffLineParam();
        onOffLineParam.mDeviceID = deviceMac;
        onOffLineParam.mOnline = online;

        if (mWorkHandler != null) {
            Message msg = new Message();
            msg.what = MSGID_DEVMGR_ON_OFF_LINE;
            msg.obj = onOffLineParam;
            mWorkHandler.sendMessage(msg);
        }
    }

    /*
     * @brief 运行在AWS回调中，接收到设备Action事件
     */
    void onDevActionUpdated(String deviceMac, String actionType) {
        if (actionType.compareToIgnoreCase("add") != 0) {   // 仅处理设备绑定完成事件
            return;
        }

        DevActionUpdateParam actionUpdateParam = new DevActionUpdateParam();
        actionUpdateParam.mDeviceID = deviceMac;
        actionUpdateParam.mActionType = actionType;

        if (mWorkHandler != null) {
            Message msg = new Message();
            msg.what = MSGID_DEVMGR_ACTION_UPDATE;
            msg.obj = actionUpdateParam;
            mWorkHandler.sendMessage(msg);
        }
    }

    /*
     * @brief 运行在AWS回调中，接收到设备属性更新
     */
    void onDevPropertyUpdated(String deviceMac, String deviceId,
                              Map<String, Object> properties) {
        DevPropertyUpdateParam propertyUpdateParam = new DevPropertyUpdateParam();
        propertyUpdateParam.mDeviceID = deviceMac;
        propertyUpdateParam.mProperties = properties;

        if (mWorkHandler != null) {
            Message msg = new Message();
            msg.what = MSGID_DEVMGR_PROPERTY_UPDATE;
            msg.obj = propertyUpdateParam;
            mWorkHandler.sendMessage(msg);
        }
    }



    //////////////////////////////////////////////////////////////
    ///////////////////////// 工作线程中处理 //////////////////////
    //////////////////////////////////////////////////////////////
    /*
     * @brief 工作线程中处理设 绑定设备列表查询
     */
    void DoDeviceListQuery(Message msg) {
        ArrayList<IotDevice> devList = new ArrayList<>();

        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoDeviceListQuery> cannot get account");
            CallbackQueryDevListDone(ErrCode.XERR_DEVMGR_QUEYR, devList);
            return;
        }

        AgoraLowService.AccountInfo gyAccount = convertToLowServiceAccount(account);
        AgoraLowService.DeviceQueryResult queryResult;
        queryResult = AgoraLowService.getInstance().deviceQuery(gyAccount);
        if (queryResult.mDeviceList != null) {
            int devCount = queryResult.mDeviceList.size();
            for (int i = 0; i < devCount; i++) {
                AgoraLowService.DevInfo devInfo = queryResult.mDeviceList.get(i);

                IotDevice iotDevice = new IotDevice();
                iotDevice.mAppUserId = devInfo.mAppUserId;
                iotDevice.mUserType = devInfo.mUserType;
                iotDevice.mProductNumber = devInfo.mProductId;
                iotDevice.mProductID = devInfo.mProductKey;
                iotDevice.mDeviceName = devInfo.mDeviceName;
                iotDevice.mDeviceID = devInfo.mDeviceMac;
                iotDevice.mSharer = devInfo.mSharer;
                iotDevice.mCreateTime = devInfo.mCreateTime;
                iotDevice.mUpdateTime = devInfo.mUpdateTime;
                iotDevice.mConnected = devInfo.mConnected;

                devList.add(iotDevice);
            }
        }

        synchronized (mDataLock) {
            mBindDevList.clear();
            mBindDevList.addAll(devList); // 更新绑定设备列表
        }

        ALog.getInstance().d(TAG, "<DoDeviceListQuery> mAccount =" + account.mAccount
                + ", bindDeviceCount=" + devList.size());
        processTokenErrCode(queryResult.mErrCode);  // Token过期统一处理
        CallbackQueryDevListDone(queryResult.mErrCode, devList);
    }

    void CallbackQueryDevListDone(int errCode, List<IotDevice> bindedDevList) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onAllDevicesQueryDone(errCode, bindedDevList);
            }
        }
    }


    /*
     * @brief 工作线程中处理设备绑定
     */
    void DoDeviceBind(Message msg)
    {
        IotDevice bindingDev = (IotDevice)(msg.obj);
        ArrayList<IotDevice> bindedDevList = new ArrayList<>();

        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoDeviceBind> cannot get account");
            CallbackBindDeviceDone(ErrCode.XERR_DEVMGR_ADD, bindingDev, bindedDevList);
            return;
        }

        //
        // 进行设备绑定操作
        //
        AgoraLowService.AccountInfo gyAccount = convertToLowServiceAccount(account);
        int errCode = AgoraLowService.getInstance().deviceBind(gyAccount,
                bindingDev.mProductID, bindingDev.mDeviceID);

        //
        // 查询已经绑定的设备列表
        //
        AgoraLowService.DeviceQueryResult queryResult;
        queryResult = AgoraLowService.getInstance().deviceQuery(gyAccount);
        if (queryResult.mDeviceList != null) {
            int devCount = queryResult.mDeviceList.size();
            for (int i = 0; i < devCount; i++) {
                AgoraLowService.DevInfo devInfo = queryResult.mDeviceList.get(i);
                IotDevice iotDevice = new IotDevice();
                iotDevice.mAppUserId = devInfo.mAppUserId;
                iotDevice.mUserType = devInfo.mUserType;
                iotDevice.mProductID = devInfo.mProductKey;
                iotDevice.mProductNumber = devInfo.mProductId;
                iotDevice.mDeviceName = devInfo.mDeviceName;
                iotDevice.mDeviceID = devInfo.mDeviceMac;
                iotDevice.mSharer = devInfo.mSharer;
                iotDevice.mCreateTime = devInfo.mCreateTime;
                iotDevice.mUpdateTime = devInfo.mUpdateTime;
                iotDevice.mConnected = devInfo.mConnected;

                if (bindingDev.mDeviceID.compareToIgnoreCase(devInfo.mDeviceMac) == 0) {
                    // 更新新绑定设备的信息
                    bindingDev.mAppUserId = devInfo.mAppUserId;
                    bindingDev.mUserType = devInfo.mUserType;
                    bindingDev.mDeviceName = devInfo.mDeviceName;
                    bindingDev.mSharer = devInfo.mSharer;
                    bindingDev.mCreateTime = devInfo.mCreateTime;
                    bindingDev.mUpdateTime = devInfo.mUpdateTime;
                    bindingDev.mConnected = devInfo.mConnected;
                }

                bindedDevList.add(iotDevice);
            }
        }

        synchronized (mDataLock) {
            mBindDevList.clear();
            mBindDevList.addAll(bindedDevList); // 更新绑定设备列表
        }

        ALog.getInstance().d(TAG, "<DoDeviceBind> mAccount =" + account.mAccount
                + ", bindDeviceCount=" + bindedDevList.size());
        processTokenErrCode(errCode);  // Token过期统一处理
        CallbackBindDeviceDone(errCode, bindingDev, bindedDevList);
    }

    void CallbackBindDeviceDone(int errCode, IotDevice bindingDevice,
                                List<IotDevice> bindedDevList) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onDeviceAddDone(errCode, bindingDevice, bindedDevList);
            }
        }
    }


    /*
     * @brief 工作线程中处理设备解绑
     */
    void DoDeviceUnbind(Message msg)
    {
        IotDevice unbindingDev = (IotDevice)(msg.obj);
        ArrayList<IotDevice> bindedDevList = new ArrayList<>();

        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoDeviceUnbind> cannot get account");
            CallbackUnbindDeviceDone(ErrCode.XERR_DEVMGR_DEL, unbindingDev, bindedDevList);
            return;
        }

        //
        // 进行设备解绑操作
        //
        AgoraLowService.AccountInfo gyAccount = convertToLowServiceAccount(account);
        int errCode = AgoraLowService.getInstance().deviceUnbind(gyAccount, unbindingDev.mDeviceID);

        //
        // 查询已经绑定的设备列表
        //
        AgoraLowService.DeviceQueryResult queryResult;
        queryResult = AgoraLowService.getInstance().deviceQuery(gyAccount);
        if (queryResult.mDeviceList != null) {
            int devCount = queryResult.mDeviceList.size();
            for (int i = 0; i < devCount; i++) {
                AgoraLowService.DevInfo devInfo = queryResult.mDeviceList.get(i);
                IotDevice iotDevice = new IotDevice();
                iotDevice.mAppUserId = devInfo.mAppUserId;
                iotDevice.mUserType = devInfo.mUserType;
                iotDevice.mProductID = devInfo.mProductKey;
                iotDevice.mProductNumber = devInfo.mProductId;
                iotDevice.mDeviceName = devInfo.mDeviceName;
                iotDevice.mDeviceID = devInfo.mDeviceMac;
                iotDevice.mSharer = devInfo.mSharer;
                iotDevice.mCreateTime = devInfo.mCreateTime;
                iotDevice.mUpdateTime = devInfo.mUpdateTime;
                iotDevice.mConnected = devInfo.mConnected;

                bindedDevList.add(iotDevice);
            }
        }

        synchronized (mDataLock) {
            mBindDevList.clear();
            mBindDevList.addAll(bindedDevList); // 更新绑定设备列表
        }

        ALog.getInstance().d(TAG, "<DoDeviceUnbind> mAccount =" + account.mAccount
                + ", bindDeviceCount=" + bindedDevList.size());
        processTokenErrCode(errCode);  // Token过期统一处理
        CallbackUnbindDeviceDone(errCode, unbindingDev, bindedDevList);
    }

    void CallbackUnbindDeviceDone(int errCode, IotDevice unbindingDevice,
                                List<IotDevice> bindedDevList) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onDeviceRemoveDone(errCode, unbindingDevice, bindedDevList);
            }
        }
    }


    /*
     * @brief 工作线程中处理设备重命名
     */
    void DoDeviceRename(Message msg)
    {
        DevRenameParam renameParam = (DevRenameParam)(msg.obj);

        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoDeviceRename> cannot get account");
            CallbackRenameDone(ErrCode.XERR_DEVMGR_DEL, renameParam.mIotDevice, renameParam.mNewName);
            return;
        }

        //
        // 进行设备解绑操作
        //
        AgoraLowService.AccountInfo gyAccount = convertToLowServiceAccount(account);
        int errCode = AgoraLowService.getInstance().deviceRename(gyAccount,
                            renameParam.mIotDevice.mDeviceID, renameParam.mNewName);

        ALog.getInstance().d(TAG, "<DoDeviceRename> iotDevice =" + renameParam.mIotDevice
                + ", newName=" + renameParam.mNewName);
        processTokenErrCode(errCode);  // Token过期统一处理
        CallbackRenameDone(errCode, renameParam.mIotDevice, renameParam.mNewName);
    }

    void CallbackRenameDone(int errCode, IotDevice iotDevice, String newName) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onDeviceRenameDone(errCode, iotDevice, newName);
            }
        }
    }

    /*
     * @brief 工作线程中处理 查询产品列表
     */
    void DoProductListQuery(Message msg) {
        ProductQueryParam queryParam = (ProductQueryParam)(msg.obj);

        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoProductListQuery> cannot get account");
            ProductQueryResult result = new ProductQueryResult();
            result.mErrCode = ErrCode.XERR_DEVMGR_PRODUCT_QUERY;
            CallbackProductQueryDone(result);
            return;
        }

        IAgoraIotAppSdk.InitParam sdkInitParam = mSdkInstance.getInitParam();
        queryParam.mBlurry = sdkInitParam.mProjectID;
        ProductQueryResult queryResult = AgoraLowService.getInstance().productQuery(
                account.mPlatformToken, queryParam);

        ALog.getInstance().d(TAG, "<DoProductListQuery> done, errCode=" + queryResult.mErrCode);
        processTokenErrCode(queryResult.mErrCode);  // Token过期统一处理
        CallbackProductQueryDone(queryResult);
    }

    void CallbackProductQueryDone(ProductQueryResult result) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onQueryProductDone(result);
            }
        }
    }

    /*
     * @brief 工作线程中处理 查询所有属性描述符
     */
    void DoQueryAllPropDesc(Message msg) {
        QueryPropDescParam queryParam = (QueryPropDescParam)(msg.obj);

        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoQueryAllPropDesc> cannot get account");
            List<IotPropertyDesc> propDescList = new ArrayList<>();
            CallbackQueryPropDescDone(ErrCode.XERR_DEVMGR_QUERY_PROPDESC, queryParam.mDeviceID,
                    queryParam.mProductNumber, propDescList);
            return;
        }

        //
        // 查询属性描述符
        //
        AgoraLowService.PropertyDescResult queryResult;
        queryResult = AgoraLowService.getInstance().queryPropertyDesc(account.mPlatformToken,
                queryParam.mDeviceID,  queryParam.mProductNumber);

        ALog.getInstance().d(TAG, "<DoQueryAllPropDesc> done, mAccount =" + account.mAccount
                + ", errCode=" + queryResult.mErrCode
                + ", propDescCount=" + queryResult.mPropDescList.size());
        CallbackQueryPropDescDone(queryResult.mErrCode, queryParam.mDeviceID,
                queryParam.mProductNumber, queryResult.mPropDescList);
    }

    void CallbackQueryPropDescDone(int errCode,
                                   final String deviceID,
                                   final String productNumber,
                                   final List<IotPropertyDesc> propDescList) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onQueryAllPropertyDescDone(errCode, deviceID, productNumber, propDescList);
            }
        }
    }


    /*
     * @brief 工作线程中处理 设置设备属性值
     */
    void DoDeviceSetProp(Message msg) {
        SetPropParam setPropParam = (SetPropParam)(msg.obj);
        IotDevice iotDevice = setPropParam.mIotDevice;
        Map<String, Object> properties = setPropParam.mProperties;

        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoDeviceSetProp> cannot get account");
            CallbackSetPropertyDone(ErrCode.XERR_DEVMGR_SETPROPERTY, iotDevice, properties);
            return;
        }

        //
        // 进行设备属性设置
        //
        AgoraLowService.AccountInfo gyAccount = convertToLowServiceAccount(account);
        AWSUtils.getInstance().setDeviceStatus(gyAccount.mAccount, iotDevice.mProductID,
                iotDevice.mDeviceID, properties);

        ALog.getInstance().d(TAG, "<DoDeviceSetProp> done, mAccount =" + account.mAccount
                + ", iotDevice=" + iotDevice.toString()
                + ", properties=" + properties.toString());
        CallbackSetPropertyDone(ErrCode.XOK, iotDevice, properties);
    }

    void CallbackSetPropertyDone(int errCode, IotDevice iotDevice, Map<String, Object> properties) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onSetPropertyDone(errCode, iotDevice, properties);
            }
        }
    }


    /*
     * @brief 工作线程中处理 设置设备属性值
     */
    void DoDeviceGetProp(Message msg) {
        IotDevice iotDevice = (IotDevice)(msg.obj);

        //
        // 进行设备属性设置
        //
        AWSUtils.getInstance().getDeviceStatus(iotDevice.mDeviceID);

        ALog.getInstance().d(TAG, "<DoDeviceGetProp> iotDevice=" + iotDevice.toString());
        CallbackGetPropertyDone(ErrCode.XOK, iotDevice);
    }

    void CallbackGetPropertyDone(int errCode, IotDevice iotDevice) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onGetPropertyDone(errCode, iotDevice);
            }
        }
    }

    /**
     * @brief 工作线程中处理 获取固件版本信息
     */
    void DoGetMcuVersion(Message msg) {
        IotDevice iotDevice = (IotDevice)(msg.obj);

        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoGetMcuVersion> cannot get account");
            CallbackGetMcuVersionDone(ErrCode.XERR_DEVMGR_GET_MCUVER, iotDevice, null);
            return;
        }

        //
        // 获取固件版本
        //
        AgoraLowService.McuVersionResult result = AgoraLowService.getInstance().getMcuVersion(
                account.mPlatformToken,  iotDevice.mDeviceID);

        ALog.getInstance().d(TAG, "<DoGetMcuVersion> errCode=" + result.mErrCode
                + ", mMcuVersion=" + result.mMcuVersion);
        processTokenErrCode(result.mErrCode);  // Token过期统一处理
        CallbackGetMcuVersionDone(result.mErrCode, iotDevice, result.mMcuVersion);
    }

    void CallbackGetMcuVersionDone(int errCode, IotDevice iotDevice,
                                   final McuVersionInfo mcuVerInfo) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onGetMcuVerInfoDone(errCode, iotDevice, mcuVerInfo);
            }
        }
    }

    /**
     * @brief 工作线程中处理 升级固件版本
     */
    void DoUpgradeMcuVersion(Message msg) {
        McuUpgradeVerParam upgradeParam = (McuUpgradeVerParam)(msg.obj);

        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoUpgradeMcuVersion> cannot get account");
            CallbackUpgradeMcuVerDone(ErrCode.XERR_DEVMGR_UPGRADE_MCUVER,
                    upgradeParam.mIotDevice, upgradeParam.mUpgradeId, upgradeParam.mDecide);
            return;
        }

        //
        // 升级固件版本
        //
        int errCode = AgoraLowService.getInstance().upgradeMcuVersion(account.mPlatformToken,
                upgradeParam.mUpgradeId, upgradeParam.mDecide);

        ALog.getInstance().d(TAG, "<DoUpgradeMcuVersion> errCode=" + errCode
                + ", mUpgradeId=" + upgradeParam.mUpgradeId
                + ", decide=" + upgradeParam.mDecide);
        processTokenErrCode(errCode);  // Token过期统一处理
        CallbackUpgradeMcuVerDone(errCode, upgradeParam.mIotDevice,
                upgradeParam.mUpgradeId, upgradeParam.mDecide);
    }

    void CallbackUpgradeMcuVerDone(int errCode, IotDevice iotDevice,
                                   long upgradeId, int decide) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onUpgradeMcuVerDone(errCode, iotDevice, upgradeId, decide);
            }
        }
    }

    /**
     * @brief 工作线程中处理 获取固件升级状态
     */
    void DoGetMcuUpgradingStatus(Message msg) {
        McuUpgradeVerParam upgradeParam = (McuUpgradeVerParam)(msg.obj);

        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoGetMcuUpgradingStatus> cannot get account");
            CallbackGetMcuUpgradeStatus(ErrCode.XERR_DEVMGR_UPGRADE_GETSTATUS,
                    upgradeParam.mIotDevice, null);
            return;
        }

        //
        // 获取固件升级状态
        //
        AgoraLowService.McuUpgradeProgress progress = AgoraLowService.getInstance().getMcuUpgradeStatus(
                account.mPlatformToken, upgradeParam.mUpgradeId);

        ALog.getInstance().d(TAG, "<DoGetMcuUpgradingStatus> errCode=" + progress.mErrCode
                + ", progress=" + progress.mPrgoress.toString());
        processTokenErrCode(progress.mErrCode);  // Token过期统一处理
        CallbackGetMcuUpgradeStatus(progress.mErrCode, upgradeParam.mIotDevice, progress.mPrgoress);
    }

    void CallbackGetMcuUpgradeStatus(int errCode, IotDevice iotDevice,
                                     final IDeviceMgr.McuUpgradeStatus status) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onUpgradeStatusDone(errCode, iotDevice, status);
            }
        }
    }



    /**
     * @brief 工作线程中处理 设置设备属性值
     */
    void DoRecvShadowProperty(Message msg) {
        RecvShadowParam recvShadowParam = (RecvShadowParam)(msg.obj);

        ALog.getInstance().d(TAG, "<DoRecvShadowProperty> mThingsName=" + recvShadowParam.mThingsName
                + ", mJsonObj=" + recvShadowParam.mJsonObj.toString());
        Map<String, Object> properties = parseShadowProperties(recvShadowParam.mJsonObj);

        int deviceCount = mBindDevList.size();
        int i;
        for (i = 0; i < deviceCount; i++) {
            IotDevice iotDevice = mBindDevList.get(i);
            if (iotDevice.mDeviceID.compareToIgnoreCase(recvShadowParam.mThingsName) == 0) {
                ALog.getInstance().d(TAG, "<DoRecvShadowProperty> mDeviceID=" + iotDevice.mDeviceID
                        + ", properties=" + properties.toString());
                CallbackRecvShadowProperty(ErrCode.XOK, iotDevice, properties);
                return;
            }
        }

        ALog.getInstance().e(TAG, "<DoGetDeviceProperty> NOT found matched device");
    }

    void CallbackRecvShadowProperty(int errCode, IotDevice iotDevice, Map<String, Object> properties) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onReceivedDeviceProperty(iotDevice, properties);
            }
        }
    }


    /*
     * @brief 工作线程中处理 设备上下线通知
     */
    void DoDeviceOnOffLine(Message msg) {
        DevOnOffLineParam onOffLineParam = (DevOnOffLineParam)(msg.obj);
        ALog.getInstance().d(TAG, "<DoDeviceOnOffLine> deviceID=" + onOffLineParam.mDeviceID
                + ", online=" + onOffLineParam.mOnline);

        //
        // 更新绑定设备列表中的状态字段
        //
        IotDevice callbackDevice = null;
        synchronized (mDataLock) {
            for (int i = 0; i < mBindDevList.size(); i++) {
                IotDevice tempIotDev = mBindDevList.get(i);
                if (tempIotDev.mDeviceID.compareToIgnoreCase(onOffLineParam.mDeviceID) == 0) {
                    tempIotDev.mConnected = onOffLineParam.mOnline;
                    mBindDevList.set(i, tempIotDev);
                    callbackDevice = tempIotDev;
                    ALog.getInstance().d(TAG, "<DoDeviceOnOffLine> updated device"
                            + ", iotDevice=" + callbackDevice.toString());
                    break;
                }
            }
        }
        if (callbackDevice == null) {  // 没有找到相应的设备MAC，忽略当前这个事件
            ALog.getInstance().e(TAG, "<DoDeviceOnOffLine> can Not found device, devID="
                            + onOffLineParam.mDeviceID);
            return;
        }

        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onDeviceOnOffLine(callbackDevice, onOffLineParam.mOnline, mBindDevList);
            }
        }
    }

    /*
     * @brief 工作线程中处理 设备Action更新通知，这里仅处理了 设备绑定完成事件
     */
    void DoDeviceActionUpdate(Message msg) {
        DevActionUpdateParam actionUpdateParam = (DevActionUpdateParam)(msg.obj);

        // 查询当前绑定的设备列表
        ArrayList<IotDevice> bindDevList = queryDevList();
        synchronized (mDataLock) {
            mBindDevList.clear();
            mBindDevList.addAll(bindDevList); // 更新绑定设备列表
        }

        // 绑定成功事件，一定应该可以查询到绑定的设备
        IotDevice iotDevice = findBindDeviceByDevMac(actionUpdateParam.mDeviceID);
        if (iotDevice == null) {
            ALog.getInstance().e(TAG, "<DoDeviceActionUpdate> can Not found device, devID="
                    + actionUpdateParam.mDeviceID);
            return;
        }


        ALog.getInstance().d(TAG, "<DoDeviceActionUpdate> iotDevice=" + iotDevice.toString());
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onDeviceAddDone(ErrCode.XOK, iotDevice, bindDevList);
            }
        }
    }

    /*
     * @brief 工作线程中处理 设备属性更新通知
     */
    void DoDevicePropertyUpdate(Message msg) {
        DevPropertyUpdateParam propertyUpdateParam = (DevPropertyUpdateParam)(msg.obj);
        IotDevice iotDevice = findBindDeviceByDevMac(propertyUpdateParam.mDeviceID);
        if (iotDevice == null) {
            ALog.getInstance().e(TAG, "<DoDevicePropertyUpdate> can Not found device, devID="
                    + propertyUpdateParam.mDeviceID);
            return;
        }

        ALog.getInstance().d(TAG, "<DoDevicePropertyUpdate> deviceID=" + iotDevice.mDeviceID
                + ", mProperties=" + propertyUpdateParam.mProperties);

        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onDevicePropertyUpdate(iotDevice, propertyUpdateParam.mProperties);
            }
        }
    }


     /*
     * @brief 工作线程中处理 共享设备操作
     */
    void DoShareDevice(Message msg) {
        ShareDevOptParam shareParam = (ShareDevOptParam)(msg.obj);

        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoShareDevice> cannot get account");
            CallbackDevShareDone(ErrCode.XERR_DEVMGR_SHARE, shareParam);
            return;
        }

        //
        // 进行设备共享操作
        //
        int errCode = AgoraLowService.getInstance().shareDevice(account.mPlatformToken,
                shareParam.mForce, shareParam.mIotDevice.mDeviceID,
                shareParam.mAccount, shareParam.mPermission);

        ALog.getInstance().d(TAG, "<DoShareDevice> errCode=" + errCode
                + ", deviceID=" + shareParam.mIotDevice.mDeviceID
                + ", account=" + shareParam.mAccount
                + ", permission=" + shareParam.mPermission
                + ", force=" + shareParam.mForce       );
        processTokenErrCode(errCode);  // Token过期统一处理
        CallbackDevShareDone(errCode, shareParam);
    }

    void CallbackDevShareDone(int errCode, final ShareDevOptParam shareParam) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onShareDeviceDone(errCode, shareParam.mForce, shareParam.mIotDevice,
                        shareParam.mAccount, shareParam.mPermission);
            }
        }
    }


    /*
     * @brief 工作线程中处理 解除设备共享
     */
    void DoDeshareDevice(Message msg) {
        ShareDevOptParam deshareParam = (ShareDevOptParam)(msg.obj);

        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoDeshareDevice> cannot get account");
            CallbackDevDeshareDone(ErrCode.XERR_DEVMGR_DESHARE, deshareParam.mOutSharer);
            return;
        }

        //
        // 进行撤销共享操作
        //
        String deviceID = deshareParam.mOutSharer.mDeviceID;
        String deshareAccount = deshareParam.mOutSharer.mAppUserId;
        int errCode = AgoraLowService.getInstance().deshareDevice(account.mPlatformToken,
                deviceID, deshareAccount);

        ALog.getInstance().d(TAG, "<DoDeshareDevice> errCode=" + errCode
                + ", deviceNumber =" + deviceID
                + ", deshareAccount=" + deshareAccount        );
        processTokenErrCode(errCode);  // Token过期统一处理
        CallbackDevDeshareDone(errCode, deshareParam.mOutSharer);
    }

    void CallbackDevDeshareDone(int errCode, final IotOutSharer outSharer) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onDeshareDeviceDone(errCode, outSharer);
            }
        }
    }


    /*
     * @brief 工作线程中处理 接受共享过来的设备
     */
    void DoAcceptDevice(Message msg) {
        ShareDevAcceptParam acceptParam = (ShareDevAcceptParam)(msg.obj);

        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoAcceptDevice> cannot get account");
            CallbackDevAcceptDone(ErrCode.XERR_DEVMGR_ACCEPT, acceptParam);
            return;
        }

        //
        // 进行接受共享操作
        //
        int errCode = AgoraLowService.getInstance().acceptDevice(account.mPlatformToken,
                acceptParam.mDeviceName, acceptParam.mOrder);

        ALog.getInstance().d(TAG, "<DoAcceptDevice> errCode=" + errCode
                + ", mDeviceName =" + acceptParam.mDeviceName
                + ", order=" + acceptParam.mOrder       );
        processTokenErrCode(errCode);  // Token过期统一处理
        CallbackDevAcceptDone(errCode, acceptParam);
    }

    void CallbackDevAcceptDone(int errCode, final ShareDevAcceptParam acceptParam) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onAcceptDeviceDone(errCode, acceptParam.mDeviceName,
                        acceptParam.mOrder);
            }
        }
    }


    /*
     * @brief 工作线程中处理 查询可以共享出去的设备列表
     */
    void DoQuerySharable(Message msg) {
        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoQuerySharable> cannot get account");
            CallbackQuerySharableDone(ErrCode.XERR_DEVMGR_QUERY_SHARABLE, null);
            return;
        }

        //
        // 查询可共享设备
        //
        ArrayList<IotDevice> sharableDevList = new ArrayList<>();
        AgoraLowService.DeviceQueryResult queryResult;
        queryResult = AgoraLowService.getInstance().querySharableDevList(account.mPlatformToken);
        if (queryResult.mDeviceList != null) {
            int devCount = queryResult.mDeviceList.size();
            for (int i = 0; i < devCount; i++) {
                AgoraLowService.DevInfo devInfo = queryResult.mDeviceList.get(i);

                IotDevice iotDevice = findBindDeviceByDevMac(devInfo.mDeviceMac);
                if (iotDevice == null) {    // 不应该在绑定设备列表中没有找到
                    Log.e(TAG, "<DoQuerySharable> NOT found device by mac=" + devInfo.mDeviceMac);
                    continue;
                }

                iotDevice.mSharer = devInfo.mSharer;
                iotDevice.mShareCount = devInfo.mShareCount;
                iotDevice.mShareType = devInfo.mShareType;
                sharableDevList.add(iotDevice);
            }
        }

        ALog.getInstance().d(TAG, "<DoQuerySharable> errCode=" + queryResult.mErrCode
                + ", deviceCount =" + queryResult.mDeviceList.size());
        processTokenErrCode(queryResult.mErrCode);  // Token过期统一处理
        CallbackQuerySharableDone(queryResult.mErrCode, sharableDevList);
    }

    void CallbackQuerySharableDone(int errCode, final List<IotDevice> deviceList) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onQuerySharableDevListDone(errCode, deviceList);
            }
        }
    }


    /*
     * @brief 工作线程中处理 查询可以解除共享的设备列表
     */
    void DoQueryOutSharers(Message msg) {
        ShareDevOptParam optParam = (ShareDevOptParam)(msg.obj);
        String deviceID = optParam.mIotDevice.mDeviceID;

        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoQueryOutSharers> cannot get account");
            CallbackQueryDesharableDone(ErrCode.XERR_DEVMGR_QUERY_DESHARABLE, deviceID,null);
            return;
        }

        //
        // 查询分享出去的账号设备信息
        //
        ArrayList<IotDevice> desharableDevList = new ArrayList<>();
        AgoraLowService.OutSharerQueryResult queryResult;
        queryResult = AgoraLowService.getInstance().queryOutSharerList(account.mPlatformToken, deviceID);

        ALog.getInstance().d(TAG, "<DoQuerySharable> errCode=" + queryResult.mErrCode
                + ", outSharerCount=" + queryResult.mOutSharerList.size());
        processTokenErrCode(queryResult.mErrCode);  // Token过期统一处理
        CallbackQueryDesharableDone(queryResult.mErrCode, deviceID, queryResult.mOutSharerList);
    }

    void CallbackQueryDesharableDone(int errCode, final String deviceID,
                                     final List<IotOutSharer> sharerList) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onQueryOutSharerListDone(errCode, deviceID, sharerList);
            }
        }
    }

    /*
     * @brief 工作线程中处理 查询共享进来的设备列表
     */
    void DoQuerySharedIn(Message msg) {
        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoQuerySharedIn> cannot get account");
            CallbackQuerySharedinDone(ErrCode.XERR_DEVMGR_QUERY_SHARAEDIN, null);
            return;
        }

        //
        // 查询共享过来的设备
        //
        ArrayList<IotDevice> sharedInDevList = new ArrayList<>();
        AgoraLowService.DeviceQueryResult queryResult;
        queryResult = AgoraLowService.getInstance().queryFromsharingDevList(account.mPlatformToken);
        if (queryResult.mDeviceList != null) {
            int devCount = queryResult.mDeviceList.size();
            for (int i = 0; i < devCount; i++) {
                AgoraLowService.DevInfo devInfo = queryResult.mDeviceList.get(i);

                IotDevice iotDevice = findBindDeviceByDevMac(devInfo.mDeviceMac);
                if (iotDevice == null) {    // 不应该在绑定设备列表中没有找到
                    Log.e(TAG, "<DoQuerySharedIn> NOT found device by mac=" + devInfo.mDeviceMac);
                    continue;
                }

                iotDevice.mSharer = devInfo.mSharer;
                iotDevice.mShareCount = devInfo.mShareCount;
                iotDevice.mShareType = devInfo.mShareType;
                sharedInDevList.add(iotDevice);
            }
        }

        ALog.getInstance().d(TAG, "<DoQuerySharedIn> errCode=" + queryResult.mErrCode
                + ", deviceCount =" + queryResult.mDeviceList.size());
        processTokenErrCode(queryResult.mErrCode);  // Token过期统一处理
        CallbackQuerySharedinDone(queryResult.mErrCode, sharedInDevList);
    }

    void CallbackQuerySharedinDone(int errCode, final List<IotDevice> deviceList) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onQueryInSharedDevList(errCode, deviceList);
            }
        }
    }

    /*
     * @brief 工作线程中处理 分页查询共享消息
     */
    void DoQueryShareMsgByPage(Message msg) {
        ShareMsgOptParam optParam = (ShareMsgOptParam)(msg.obj);
        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoQueryShareMsgByPage> cannot get account");
            CallbackQueryShareMsgPageDone(ErrCode.XERR_DEVMGR_QUERY_SHAREMSG, null);
            return;
        }

        //
        // 分页查询共享消息
        //
        AgoraLowService.ShareMsgPageQueryResult queryResult;
        queryResult = AgoraLowService.getInstance().queryShareMsgByPage(account.mPlatformToken,
                    optParam.mPageNumber, optParam.mPageSize, optParam.mAuditStatus);

        ALog.getInstance().d(TAG, "<DoQueryShareMsgByPage> errCode=" + queryResult.mErrCode
                + ", mPageInfo=" + queryResult.mPageInfo.toString());
        processTokenErrCode(queryResult.mErrCode);  // Token过期统一处理
        CallbackQueryShareMsgPageDone(queryResult.mErrCode, queryResult.mPageInfo);
    }

    void CallbackQueryShareMsgPageDone(int errCode, final IotShareMsgPage shareMsgPage) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onQueryShareMsgPageDone(errCode, shareMsgPage);
            }
        }
    }


    /*
     * @brief 工作线程中处理 查询单个共享消息
     */
    void DoQueryShareMsgById(Message msg) {
        ShareMsgOptParam optParam = (ShareMsgOptParam)(msg.obj);
        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoQueryShareMsgById> cannot get account");
            CallbackQueryShareMsgDone(ErrCode.XERR_DEVMGR_QUERY_SHAREDETAIL, null);
            return;
        }

        //
        // 查询单个共享消息详情
        //
        AgoraLowService.ShareMsgInfoResult queryResult;
        queryResult = AgoraLowService.getInstance().queryShareMsgDetail(account.mPlatformToken,
                                                optParam.mMessageId);

        ALog.getInstance().d(TAG, "<DoQueryShareMsgById> errCode=" + queryResult.mErrCode
                + ", queryResult.mShareMsg=" + queryResult.mShareMsg.toString());
        processTokenErrCode(queryResult.mErrCode);  // Token过期统一处理
        CallbackQueryShareMsgDone(queryResult.mErrCode, queryResult.mShareMsg);
    }

    void CallbackQueryShareMsgDone(int errCode, final IotShareMessage shareMessage) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onQueryShareMsgDetailDone(errCode, shareMessage);
            }
        }
    }


    /*
     * @brief 工作线程中处理 删除单个共享消息
     */
    void DoDeleteShareMsg(Message msg) {
        ShareMsgOptParam optParam = (ShareMsgOptParam)(msg.obj);
        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<DoDeleteShareMsg> cannot get account");
            CallbackDeleteShareMsgDone(ErrCode.XERR_DEVMGR_DEL_SHAREMSG, optParam.mMessageId);
            return;
        }

        //
        // 删除单个共享消息详情
        //
        int errCode = AgoraLowService.getInstance().deleteShareMsg(account.mPlatformToken,
                optParam.mMessageId);

        ALog.getInstance().d(TAG, "<DoDeleteShareMsg> errCode=" + errCode
                + ", mMessageId=" + optParam.mMessageId);
        processTokenErrCode(errCode);  // Token过期统一处理
        CallbackDeleteShareMsgDone(errCode, optParam.mMessageId);
    }

    void CallbackDeleteShareMsgDone(int errCode, long messageId) {
        synchronized (mCallbackList) {
            for (IDeviceMgr.ICallback listener : mCallbackList) {
                listener.onDeleteShareMsgDone(errCode, messageId);
            }
        }
    }



    ///////////////////////////////////////////////////////////////////////
    ///////////////////// Inner Data Structure and Methods ////////////////
    ///////////////////////////////////////////////////////////////////////
    /*
     * @brief 统一处理Token过期错误码
     */
    void processTokenErrCode(int errCode) {
        if (errCode == ErrCode.XERR_TOKEN_INVALID)    {
            AccountMgr accountMgr = (AccountMgr)(mSdkInstance.getAccountMgr());
            accountMgr.onTokenInvalid();
        }
    }


    /*
     * @brief 解析设备属性值
     */
    private Map<String, Object> parseShadowProperties(JSONObject jsonObject) {
        Map<String, Object> properties = new HashMap<>();

        try {
            Iterator<String> iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                Object value = jsonObject.get(key);
                properties.put(key, value);
            }
        } catch (JSONException jsonExp) {
            jsonExp.printStackTrace();
        }

        return properties;
    }

    AgoraLowService.AccountInfo convertToLowServiceAccount(AccountMgr.AccountInfo account) {
        AgoraLowService.AccountInfo gyAccount = new AgoraLowService.AccountInfo();
        gyAccount.mAccount = account.mAccount;
        gyAccount.mEndpoint = account.mEndpoint;
        gyAccount.mRegion = account.mRegion;
        gyAccount.mPlatformToken = account.mPlatformToken;
        gyAccount.mExpiration = account.mExpiration;
        gyAccount.mRefresh = account.mRefresh;
        gyAccount.mPoolIdentifier = account.mPoolIdentifier;
        gyAccount.mPoolIdentityId = account.mPoolIdentityId;
        gyAccount.mPoolToken = account.mPoolToken;
        gyAccount.mIdentityPoolId = account.mIdentityPoolId;
        gyAccount.mProofAccessKeyId = account.mProofAccessKeyId;
        gyAccount.mProofSecretKey = account.mProofSecretKey;
        gyAccount.mProofSessionToken = account.mProofSessionToken;
        gyAccount.mProofSessionExpiration = account.mProofSessionExpiration;
        return gyAccount;
    }

    /*
     * @brief 纯粹的查询设备列表
     */
    ArrayList<IotDevice> queryDevList() {
        AccountMgr.AccountInfo account = mSdkInstance.getAccountInfo();
        if (account == null) {
            ALog.getInstance().e(TAG, "<queryDevList> cannot get account");
            return null;
        }

        AgoraLowService.AccountInfo gyAccount = convertToLowServiceAccount(account);
        AgoraLowService.DeviceQueryResult queryResult;
        queryResult = AgoraLowService.getInstance().deviceQuery(gyAccount);
        ArrayList<IotDevice> devList = new ArrayList<>();
        if (queryResult.mDeviceList != null) {
            int devCount = queryResult.mDeviceList.size();
            for (int i = 0; i < devCount; i++) {
                AgoraLowService.DevInfo devInfo = queryResult.mDeviceList.get(i);

                IotDevice iotDevice = new IotDevice();
                iotDevice.mAppUserId = devInfo.mAppUserId;
                iotDevice.mUserType = devInfo.mUserType;
                iotDevice.mProductID = devInfo.mProductKey;
                iotDevice.mProductNumber = devInfo.mProductId;
                iotDevice.mDeviceName = devInfo.mDeviceName;
                iotDevice.mDeviceID = devInfo.mDeviceMac;
                iotDevice.mSharer = devInfo.mSharer;
                iotDevice.mCreateTime = devInfo.mCreateTime;
                iotDevice.mUpdateTime = devInfo.mUpdateTime;
                iotDevice.mConnected = devInfo.mConnected;

                devList.add(iotDevice);
            }
        }

        synchronized (mDataLock) {
            mBindDevList.clear();
            mBindDevList.addAll(devList); // 更新绑定设备列表
        }

        ALog.getInstance().d(TAG, "<queryDevList> bindDeviceCount=" + devList.size());
        return devList;
    }


    /*
     * @brief 属性设置参数
     */
    private class SetPropParam {
        IotDevice mIotDevice;
        Map<String, Object> mProperties;
    }

    /*
     * brief 接收到影子设备中属性
     */
    private class RecvShadowParam {
        String mThingsName;
        JSONObject mJsonObj;
    }

    /*
     * brief 设备重命名信息
     */
    private class DevRenameParam {
        IotDevice mIotDevice;
        String mNewName;
    }

    /*
     * brief 设备上下线信息
     */
    private class DevOnOffLineParam {
        String mDeviceID;
        boolean mOnline;
    }

    /*
     * brief 设备Action更新信息
     */
    private class DevActionUpdateParam {
        String mDeviceID;
        String mActionType;
    }

    /*
     * brief 设备属性更新信息
     */
    private class DevPropertyUpdateParam {
        String mDeviceID;
        Map<String, Object> mProperties;
    }

    /*
     * brief 设备消息操作参数
     */
    private class ShareMsgOptParam {
        int mPageNumber;
        int mPageSize;
        int mAuditStatus;
        long mMessageId;
    }

    /*
     * brief 设备共享操作参数
     */
    private class ShareDevOptParam {
        IotDevice mIotDevice;
        String mAccount;
        int mPermission;
        boolean mForce;

        IotOutSharer mOutSharer;
    }

    /*
     * brief 设备共享接受参数
     */
    private class ShareDevAcceptParam {
        String mDeviceName;
        String mOrder;
    }

    /**
     * @brief 设备固件升级参数
     */
    private class McuUpgradeVerParam {
        IotDevice mIotDevice;
        long   mUpgradeId;
        int    mDecide;
    }

    /**
     * @brief 查询属性描述符参数
     */
    private class QueryPropDescParam {
        String mDeviceID;
        String mProductNumber;
    }
}
