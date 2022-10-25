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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NotificationViewModel extends BaseViewModel implements IDevMessageMgr.ICallback {
    private final String TAG = "IOTLINK/NotifyViewModel";



    public NotificationViewModel() {
    }


    public void onStart() {
        AIotAppSdkFactory.getInstance().getDevMessageMgr().registerListener(this);
    }

    public void onStop() {
        AIotAppSdkFactory.getInstance().getDevMessageMgr().unregisterListener(this);
    }

    private String beginDateToString(MessageViewModel.CustomDate beginDate) {
        String text = String.format(Locale.getDefault(), "%d-%02d-%02d 00:00:00",
                beginDate.year, beginDate.month, beginDate.day);
        return text;
    }

    private String endDateToString(MessageViewModel.CustomDate endDate) {
        String text = String.format(Locale.getDefault(), "%d-%02d-%02d 23:59:59",
                endDate.year, endDate.month, endDate.day);
        return text;
    }

    /**
     * @brief 查询当前设备列表中所有的通知消息
     */
    public void queryAllNotifications() {
        IDevMessageMgr.QueryParam notifyQueryParam = new IDevMessageMgr.QueryParam();
        List<IotDevice> bindDevList = AIotAppSdkFactory.getInstance().getDeviceMgr().getBindDevList();
        List<String> deviceIdList = new ArrayList<>();
        for (int i = 0; i < bindDevList.size(); i++) {
            IotDevice device = bindDevList.get(i);
            deviceIdList.add(device.mDeviceID);
        }

//        // 取今天 ~ 之前七天的通知消息
//        MessageViewModel.CustomDate beginDay = new MessageViewModel.CustomDate();
//        MessageViewModel.CustomDate endDay = new MessageViewModel.CustomDate();
//        Calendar calendar = Calendar.getInstance();
//        endDay.year = calendar.get(Calendar.YEAR);
//        endDay.month = calendar.get(Calendar.MONTH) + 1;
//        endDay.day = calendar.get(Calendar.DAY_OF_MONTH);
//
//        calendar.add(Calendar.DAY_OF_MONTH, -7);
//        beginDay.year = calendar.get(Calendar.YEAR);
//        beginDay.month = calendar.get(Calendar.MONTH) + 1;
//        beginDay.day = calendar.get(Calendar.DAY_OF_MONTH);

        // 开始查询
        notifyQueryParam.mDevIDList.addAll(deviceIdList);
        notifyQueryParam.mMsgType = -1;        // 查询所有通知
        notifyQueryParam.mPageIndex = 1;
        notifyQueryParam.mPageSize = Constant.MAX_RECORD_CNT;
//        notifyQueryParam.mBeginDate = beginDateToString(beginDay);
//        notifyQueryParam.mEndDate = endDateToString(endDay);
        int errCode = AIotAppSdkFactory.getInstance().getDevMessageMgr().queryByPage(notifyQueryParam);
        Log.d(TAG,"<queryAllNotifications> errCode=" + errCode);
//                    + ", mBeginDate=" + notifyQueryParam.mBeginDate
//                    + ", mEndDate=" + notifyQueryParam.mEndDate  );
        if (errCode != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能查询全部通知未读消息数量, 错误码=" + errCode);
        }
    }

    /**
     * @brief 分页查询到完成事件
     * @param errCode : 结果错误码，0表示成功
     * @param devMsgPage : 查询到的设备消息页面
     */
    @Override
    public void onDevMessagePageQueryDone(int errCode, final IDevMessageMgr.QueryParam queryParam,
                                          final IotDevMsgPage devMsgPage) {
        Log.d(TAG,"<onDevMessagePageQueryDone> errCode=" + errCode
                + ", notificationCount=" + devMsgPage.mDevMsgList.size());

        if (errCode != ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_NOTIFY_QUERY_FAIL, (Integer)errCode);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_NOTIFY_QUERY_RESULT, devMsgPage);
        }
    }

    /*
     * @brief 标记多个设备消息为已读，触发 onDevMessageMarkDone() 回调
     * @param devMsgIdList : 要标记已读的设备消息Id列表
     * @return 错误码
     */
    public int markNotifyMessage(List<Long> devMsgIdList) {
        int errCode = AIotAppSdkFactory.getInstance().getDevMessageMgr().mark(devMsgIdList);
        Log.d(TAG,"<markNotifyMessage> errCode=" + errCode);
        return errCode;
    }

    @Override
    public void onDevMessageMarkDone(int errCode, List<Long> markedIdList) {
        Log.d(TAG,"<onDevMessageMarkDone> errCode=" + errCode
                    + ", markedIdCount=" + markedIdList.size());

        if (errCode != ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_MARK_NOTIFY_MSG_FAIL, (Integer)errCode);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_MESSAGE_MARK_NOTIFY_MSG_SUCCESS, markedIdList);
        }
    }
}