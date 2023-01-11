package io.agora.iotlinkdemo.models.message;

import android.content.Context;
import android.util.Log;

import com.agora.baselibrary.base.BaseViewModel;
import com.agora.baselibrary.utils.ToastUtils;

import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.IotAlarmImage;
import io.agora.iotlink.IotAlarmVideo;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAlarmMgr;
import io.agora.iotlink.IDevMessageMgr;
import io.agora.iotlink.IotAlarm;
import io.agora.iotlink.IotAlarmPage;
import io.agora.iotlink.IotDevMsgPage;
import io.agora.iotlink.IotDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageViewModel extends BaseViewModel
        implements IAlarmMgr.ICallback, IDevMessageMgr.ICallback, ICallkitMgr.ICallback {
    private final String TAG = "IOTLINK/MsgViewModel";


    public static class CustomDate {
        public int year;
        public int month;
        public int day;
    }


    private IAlarmMgr.QueryParam mQueryParam;     ///< 当前告警信息查询过滤条件


    ////////////////////////////////////////////////////////////////////////
    /////////////////////////// Public Methods ////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public MessageViewModel() {

        // 默认查询当天的所有告警信息
        mQueryParam = new IAlarmMgr.QueryParam();
        mQueryParam.mPageIndex = 1;
        mQueryParam.mPageSize = Constant.MAX_RECORD_CNT;
        mQueryParam.mMsgType = -1;       // 默认查询所有类型的告警消息
        mQueryParam.mMsgStatus = -1;     // 默认查询 已读和未读 所有告警信息
        mQueryParam.mDeviceID = null;    // 默认查询所有设备的
        setQueryBeginDate(new CustomDate());
        setQueryEndDate(new CustomDate());
    }

    public void onStart() {
        AIotAppSdkFactory.getInstance().getAlarmMgr().registerListener(this);
        AIotAppSdkFactory.getInstance().getDevMessageMgr().registerListener(this);
        AIotAppSdkFactory.getInstance().getCallkitMgr().registerListener(this);
    }

    public void onStop() {
        AIotAppSdkFactory.getInstance().getAlarmMgr().unregisterListener(this);
        AIotAppSdkFactory.getInstance().getDevMessageMgr().unregisterListener(this);
        AIotAppSdkFactory.getInstance().getCallkitMgr().unregisterListener(this);
    }

    /**
     * @brief 设置查询条件：开始日期
     */
    public void setQueryBeginDate(CustomDate beginDate) {
        String text = String.format(Locale.getDefault(), "%d-%02d-%02d 00:00:00",
                beginDate.year, beginDate.month, beginDate.day);
        mQueryParam.mBeginDate = text;
        Log.d(TAG, "<setQueryBeginDate> mBeginDate=" + mQueryParam.mBeginDate);
    }

    /**
     * @brief 设置查询条件：结束日期
     */
    public void setQueryEndDate(CustomDate endDate) {
        String text = String.format(Locale.getDefault(), "%d-%02d-%02d 23:59:59",
                endDate.year, endDate.month, endDate.day);
        mQueryParam.mEndDate = text;
        Log.d(TAG, "<setQueryEndDate> mEndDate=" + mQueryParam.mEndDate);
    }

    /**
     * @brief 设置查询条件：消息类型
     */
    public void setQueryMsgType(int msgType) {
        mQueryParam.mMsgType = msgType;
        Log.d(TAG, "<setQueryMsgType> msgType=" + msgType);
    }

    /**
     * @brief 设置查询条件：设备ID
     */
    public void setQueryDeviceID(final String deviceID) {
        mQueryParam.mDeviceID = deviceID;
        Log.d(TAG, "<setQueryDeviceID> deviceID=" + deviceID);
    }

    /**
     * @brief 根据当前过滤条件，查询告警消息
     */
    public void queryAlarmsByFilter() {
        int ret = AIotAppSdkFactory.getInstance().getAlarmMgr().queryByPage(mQueryParam);
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能查询告警消息, 错误码: " + ret);
        }
    }

    @Override
    public void onAlarmPageQueryDone(int errCode, IAlarmMgr.QueryParam queryParam, IotAlarmPage alarmPage) {
        if (errCode != ErrCode.XOK && errCode != ErrCode.XERR_HTTP_JSON_PARSE) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_ALARM_QUERY_FAIL, (Integer)errCode);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_ALARM_QUERY_RESULT, alarmPage);
        }
    }



    /**
     * @breif 查询全部未读告警消息数量
     */
    public void queryUnreadedAlarmCount() {
        IAlarmMgr.QueryParam queryParam = new IAlarmMgr.QueryParam();
        queryParam.mPageIndex = 1;
        queryParam.mPageSize = Constant.MAX_RECORD_CNT;
        queryParam.mMsgType = -1;       // 默认查询所有类型的告警消息
        queryParam.mMsgStatus = 0;      // 仅查询未读的告警消息
        queryParam.mDeviceID = null;    // 默认查询所有设备的
        queryParam.mBeginDate = null;   // 查询所有时间的
        queryParam.mEndDate = null;
        int ret = AIotAppSdkFactory.getInstance().getAlarmMgr().queryNumber(queryParam);
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能查询全部告警未读消息数量, 错误码=" + ret);
        }
    }

    @Override
    public void onAlarmNumberQueryDone(int errCode, IAlarmMgr.QueryParam queryParam, long alarmNumber) {
        Log.d(TAG,"<onAlarmNumberQueryDone> errCode=" + errCode
                + ", alarmNumber=" + alarmNumber);

        if (errCode != ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_UNREAD_ALARM_COUNT, (long)(-1));
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_UNREAD_ALARM_COUNT, alarmNumber);
        }
    }



    /**
     * @brief 标记多个告警信息为已读，触发 onAlarmMarkDone() 回调
     * @param alarmIdList : 要标记已读的告警信息Id列表
     * @return 错误码
     */
    public void markAlarmMessage(List<Long> alarmIdList) {
        int ret = AIotAppSdkFactory.getInstance().getAlarmMgr().mark(alarmIdList);
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("要标记告警已读已读, 错误码: " + ret);
        }

    }

    @Override
    public void onAlarmMarkDone(int errCode, List<Long> markedIdList) {
        Log.d(TAG,"<onAlarmMarkDone> errCode=" + errCode
                + ", markedIdCount=" + markedIdList.size());

        if (errCode != ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_MARK_ALARM_MSG_FAIL, (Integer)errCode);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_MARK_ALARM_MSG, null);
            // queryUnreadedAlarmCount();
        }
    }





    /**
     * 删除告警消息
     */
    public void requestDeleteAlarmMgr(List<Long> alarmIds) {
        int ret = AIotAppSdkFactory.getInstance().getAlarmMgr().delete(alarmIds);
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能删除告警消息, 错误码: " + ret);
        }
    }

    @Override
    public void onAlarmDeleteDone(int errCode, List<Long> deletedIdList) {
        Log.d(TAG,"<onAlarmDeleteDone> errCode=" + errCode
                + ", deletedIdCount=" + deletedIdList.size());

        if (errCode != ErrCode.XOK) {
        //    ToastUtils.INSTANCE.showToast("删除告警消息, 错误码: " + errCode);
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_ALARM_DELETE_FAIL, (Integer)errCode);
        } else {
        //    ToastUtils.INSTANCE.showToast("删除告警消息成功");
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_ALARM_DELETE_RESULT, null);
        }
    }


    /**
     * 获取 告警消息 详情  onAlarmInfoQueryDone
     */
    public void requestAlarmMgrDetailById(long alarmId) {
        int ret = AIotAppSdkFactory.getInstance().getAlarmMgr().queryById(alarmId);
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能获取 告警消息详情, 错误码: " + ret);
        }
    }

    @Override
    public void onAlarmInfoQueryDone(int errCode, IotAlarm iotAlarm) {
        Log.d(TAG,"<onAlarmInfoQueryDone> errCode=" + errCode
                + ", iotAlarm=" + iotAlarm.toString());
        if (errCode != ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_ALARM_DETAIL_FAIL, (Integer)errCode);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_ALARM_DETAIL_RESULT, iotAlarm);
        }
    }




    /////////////////////////////////////////////////////////////////////////////////
    //////////////////////////// 通知消息管理方法 /////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 查询当前所有未读通知的数量
     */
    public int queryUnreadedNotifyCount() {
        IDevMessageMgr.QueryParam notifyQueryParam = new IDevMessageMgr.QueryParam();
        List<IotDevice> bindDevList = AIotAppSdkFactory.getInstance().getDeviceMgr().getBindDevList();
        List<String> deviceIdList = new ArrayList<>();
        for (int i = 0; i < bindDevList.size(); i++) {
            IotDevice device = bindDevList.get(i);
            deviceIdList.add(device.mDeviceID);
        }

        // 开始查询
        notifyQueryParam.mDevIDList.addAll(deviceIdList);
        notifyQueryParam.mMsgType = -1;         // 查询所有通知
        notifyQueryParam.mMsgStatus = 0;        // 仅查询未读通知
        notifyQueryParam.mPageIndex = 1;
        notifyQueryParam.mBeginDate = null;    // 查询所有时间的
        notifyQueryParam.mEndDate = null;
        notifyQueryParam.mPageSize = Constant.MAX_RECORD_CNT;
        int errCode = AIotAppSdkFactory.getInstance().getDevMessageMgr().queryNumber(notifyQueryParam);
        Log.d(TAG,"<queryUnreadedNotifyCount> errCode=" + errCode);
        return errCode;
    }


    @Override
    public void onDevMessageNumberQueryDone(int errCode, IDevMessageMgr.QueryParam queryParam,
                                            long devMsgNumber) {
        Log.d(TAG,"<onDevMessageNumberQueryDone> errCode=" + errCode
                + ", devMsgNumber=" + devMsgNumber);

        if (errCode != ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_UNREAD_NOTIFIY_COUNT, (long)(-1));
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_UNREAD_NOTIFIY_COUNT, devMsgNumber);
        }
    }



    @Override
    public void onPeerIncoming(IotDevice iotDevice, String attachMsg) {
        Log.d(TAG,"<onPeerIncoming> iotDevice=" + iotDevice.toString()
                + ", attachMsg=" + attachMsg);

        getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_INCOMING, iotDevice);
    }

}