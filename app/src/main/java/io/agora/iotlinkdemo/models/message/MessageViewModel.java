package io.agora.iotlinkdemo.models.message;

import android.content.Context;
import android.util.Log;

import com.agora.baselibrary.base.BaseViewModel;
import com.agora.baselibrary.utils.ToastUtils;

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

public class MessageViewModel extends BaseViewModel implements IAlarmMgr.ICallback, IDevMessageMgr.ICallback {

    private final String TAG = "IOTLINK/MsgViewModel";

    public IAlarmMgr.QueryParam queryParam;
    public IDevMessageMgr.QueryParam queryDevParam;

    public MessageViewModel() {
        queryParam = new IAlarmMgr.QueryParam();
        queryParam.mPageIndex = 1;
        queryParam.mPageSize = Constant.MAX_RECORD_CNT;
        //消息类型 -1 全部 0 声音检测 1 移动检测
        queryParam.mMsgType = -1;
        queryParam.mBeginDate = beginDateToString(new CustomDate());
        queryParam.mEndDate = endDateToString(new CustomDate());
        //消息类型
        queryParam.mMsgStatus = -1;

        queryDevParam = new IDevMessageMgr.QueryParam();
        List<IotDevice> mBindDevList = AIotAppSdkFactory.getInstance().getDeviceMgr().getBindDevList();
        List<String> mDeviceIDList = new ArrayList<>();
        for (int i = 0; i < mBindDevList.size(); i++) {
            IotDevice device = mBindDevList.get(i);
            mDeviceIDList.add(device.mDeviceID);
        }
        queryDevParam.mDevIDList.addAll(mDeviceIDList);
        queryDevParam.mPageIndex = 1;
        queryDevParam.mMsgType = -1;
        queryDevParam.mPageSize = Constant.MAX_RECORD_CNT;
        //消息类型
        queryDevParam.mMsgStatus = -1;
    }

    public static class CustomDate {
        public int year;
        public int month;
        public int day;
    }

    public String beginDateToString(CustomDate beginDate) {
        String text = String.format(Locale.getDefault(), "%d-%02d-%02d 00:00:00",
                beginDate.year, beginDate.month, beginDate.day);
        return text;
    }

    public String endDateToString(CustomDate endDate) {
        String text = String.format(Locale.getDefault(), "%d-%02d-%02d 23:59:59",
                endDate.year, endDate.month, endDate.day);
        return text;
    }

    public void onStart() {
        AIotAppSdkFactory.getInstance().getAlarmMgr().registerListener(this);
        AIotAppSdkFactory.getInstance().getDevMessageMgr().registerListener(this);
    }

    public void onStop() {
        AIotAppSdkFactory.getInstance().getAlarmMgr().unregisterListener(this);
        AIotAppSdkFactory.getInstance().getDevMessageMgr().unregisterListener(this);
    }

    /**
     * 获取全部告警未读消息数量
     */
    public void requestAlarmMgrCount() {
        int ret = AIotAppSdkFactory.getInstance().getAlarmMgr().queryNumber(getQueryParam());
        if (ret != ErrCode.XOK && ret != -10004) {
            ToastUtils.INSTANCE.showToast("不能查询全部告警未读消息数量, 错误码: " + ret);
        }
    }

    /**
     * 获取全部通知未读消息
     */
    public void requestNotifyMgrCount() {
        int ret = AIotAppSdkFactory.getInstance().getDevMessageMgr().queryNumber(queryDevParam);
        if (ret != ErrCode.XOK && ret != -10004) {
            ToastUtils.INSTANCE.showToast("不能查询全部通知未读消息数量, 错误码: " + ret);
        }
    }

    private IAlarmMgr.QueryParam getQueryParam() {
        IAlarmMgr.QueryParam queryParam = new IAlarmMgr.QueryParam();
        queryParam.mPageIndex = 1;
        queryParam.mPageSize = Constant.MAX_RECORD_CNT;
        //消息类型 -1 全部 0 声音检测 1 移动检测
        queryParam.mMsgType = -1;
        //消息类型 0未读
        queryParam.mMsgStatus = 0;
        return queryParam;
    }

    private IDevMessageMgr.QueryParam getQueryDevParam() {
        IDevMessageMgr.QueryParam queryParam = new IDevMessageMgr.QueryParam();
        queryParam.mPageIndex = 1;
        queryParam.mPageSize = Constant.MAX_RECORD_CNT;
        //消息类型 -1 全部 0 声音检测 1 移动检测
        queryParam.mMsgType = -1;
        //消息类型 0未读
        queryParam.mMsgStatus = 0;
        return queryParam;
    }

    /*
     * @brief 标记多个告警信息为已读，触发 onAlarmMarkDone() 回调
     * @param alarmIdList : 要标记已读的告警信息Id列表
     * @return 错误码
     */
    public void markAlarmMessage(List<Long> msg) {
        int ret = AIotAppSdkFactory.getInstance().getAlarmMgr().mark(msg);
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("要标记告警已读已读, 错误码: " + ret);
        }

    }

    @Override
    public void onAlarmMarkDone(int errCode, List<Long> markedIdList) {
        if (errCode != ErrCode.XOK) {
            //ToastUtils.INSTANCE.showToast("标记告警已读失败, 错误码: " + errCode);
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_MARK_ALARM_MSG_FAIL, (Integer)errCode);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_MARK_ALARM_MSG, null);
            requestAlarmMgrCount();
        }
    }

    /*
     * @brief 标记多个设备消息为已读，触发 onDevMessageMarkDone() 回调
     * @param devMsgIdList : 要标记已读的设备消息Id列表
     * @return 错误码
     */
    public void markNotifyMessage(List<Long> msg) {
        AIotAppSdkFactory.getInstance().getAlarmMgr().mark(msg);
    }

    @Override
    public void onDevMessageMarkDone(int errCode, List<Long> markedIdList) {
        if (errCode != ErrCode.XOK) {
//            ToastUtils.INSTANCE.showToast("标记通知已读失败, 错误码: " + errCode);
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_MARK_NOTIFY_MSG_FAIL, (Integer)errCode);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_MARK_NOTIFY_MSG, null);
//            requestNotifyMgrCount();
        }
    }

    @Override
    public void onAlarmNumberQueryDone(int errCode, IAlarmMgr.QueryParam queryParam, long alarmNumber) {
        if (errCode != ErrCode.XOK) {
        //    ToastUtils.INSTANCE.showToast("查询告警消息数量失败, 错误码: " + errCode);
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_ALARM_COUNT_FAIL, (Integer)errCode);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_ALARM_COUNT_RESULT, alarmNumber);
        }
    }

    @Override
    public void onDevMessageNumberQueryDone(int errCode, IDevMessageMgr.QueryParam queryParam, long devMsgNumber) {
        if (errCode != ErrCode.XOK && errCode != -110005) {
        //    ToastUtils.INSTANCE.showToast("查询通知消息数量失败, 错误码: " + errCode);
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_NOTIFY_COUNT_FAIL, (Integer)errCode);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_NOTIFY_COUNT_RESULT, devMsgNumber);
        }
    }

    /**
     * 获取全部告警消息
     */
    public void requestAllAlarmMgr() {
        int ret = AIotAppSdkFactory.getInstance().getAlarmMgr().queryByPage(queryParam);
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能查询告警消息, 错误码: " + ret);
        }
    }

    @Override
    public void onAlarmPageQueryDone(int errCode, IAlarmMgr.QueryParam queryParam, IotAlarmPage alarmPage) {
        if (errCode != ErrCode.XOK && errCode != ErrCode.XERR_HTTP_JSON_PARSE) {
        //    ToastUtils.INSTANCE.showToast("查询告警消息失败, 错误码: " + errCode);
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_ALARM_QUERY_FAIL, (Integer)errCode);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_ALARM_QUERY_RESULT, alarmPage);
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
        if (errCode != ErrCode.XOK) {
        //    ToastUtils.INSTANCE.showToast("删除告警消息, 错误码: " + errCode);
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_ALARM_DELETE_FAIL, (Integer)errCode);
        } else {
        //    ToastUtils.INSTANCE.showToast("删除告警消息成功");
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_ALARM_DELETE_RESULT, null);
        }
    }

    /**
     * 查询指定页面的设备列表，触发 onDevMessagePageQueryDone() 回调
     */
    public void requestAllNotificationMgr() {
        int ret = AIotAppSdkFactory.getInstance().getDevMessageMgr().queryByPage(queryDevParam);
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能查询所有设备信息, 错误码: " + ret);
        }
    }

    /*
     * @brief 分页查询到的设备消息
     * @param errCode : 结果错误码，0表示成功
     * @param alarmPage : 查询到的设备消息页面
     */
    @Override
    public void onDevMessagePageQueryDone(int errCode, final IDevMessageMgr.QueryParam queryParam,
                                          final IotDevMsgPage devMsgPage) {
        if (errCode != ErrCode.XOK) {
        //    ToastUtils.INSTANCE.showToast("查询通知消息失败, 错误码: " + errCode);
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_NOTIFY_QUERY_FAIL, (Integer)errCode);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_NOTIFY_QUERY_RESULT, devMsgPage);
        }
    }

    /**
     * 删除通知消息
     */
    public void requestDeleteNotifyMgr(List<String> ids) {
//        int ret = AIotAppSdkFactory.getInstance().getDevMessageMgr().(ids);
//        if (ret != ErrCode.XOK) {
//            ToastUtils.INSTANCE.showToast("不能删除告警消息, 错误码: " + ret);
//        }
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
        if (errCode != ErrCode.XOK) {
        //    ToastUtils.INSTANCE.showToast("获取 告警消息 详情失败, 错误码: " + errCode);
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_ALARM_DETAIL_FAIL, (Integer)errCode);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_ALARM_DETAIL_RESULT, iotAlarm);
        }
    }


    /**
     * @brief 根据ImageId 查询告警图片信息
     */
    public void queryAlarmImage(final String imageId) {
        // String imageId = "IVFESSRUKM3ESSZUIVETKLLMPBUDAMBR_1664349292588_1303698194";
        int errCode = AIotAppSdkFactory.getInstance().getAlarmMgr().queryImageById(imageId);
        if (errCode != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("查询告警图片失败，错误码=" + errCode);
        }
    }

    @Override
    public void onAlarmImageQueryDone(int errCode, final String imageId,
                                       final IotAlarmImage alarmImage) {
        Log.d(TAG, "<onAlarmImageQueryDone> errCode=" + errCode
                + ", imageId=" + imageId + ", alarmImage=" + alarmImage.toString());
    }


    /**
     * @brief 根据时间戳 查询告警云录视频信息
     */
    public void queryAlarmVideo(final String deviceID, long timestamp) {
        int errCode = AIotAppSdkFactory.getInstance().getAlarmMgr().queryVideoByTimestamp(
                        deviceID, null, timestamp);
        if (errCode != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("查询告警云录视频失败，错误码=" + errCode);
        }
    }

    @Override
    public void onAlarmVideoQueryDone(int errCode, final String deviceID, long timestamp,
                                       final IotAlarmVideo alarmVideo) {
        Log.d(TAG, "<onAlarmVideoQueryDone> errCode=" + errCode
                + ", deviceID=" + deviceID
                + ", timestamp=" + timestamp
                + ", alarmVideo=" + alarmVideo.toString());
    }


}