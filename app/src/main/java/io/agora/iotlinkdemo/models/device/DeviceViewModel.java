package io.agora.iotlinkdemo.models.device;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatTextView;

import com.agora.baselibrary.base.BaseViewModel;
import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.event.ResetAddDeviceEvent;
import io.agora.iotlinkdemo.manager.DevicesListManager;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAccountMgr;
import io.agora.iotlink.IDeviceMgr;
import io.agora.iotlink.IotDevice;
import io.agora.iotlink.IotOutSharer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 设备添加连接相关viewModel
 */
public class DeviceViewModel extends BaseViewModel implements IDeviceMgr.ICallback, IAccountMgr.ICallback {

    public DeviceViewModel() {
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onCleared() {
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(@Nullable ResetAddDeviceEvent event) {
        if (getISingleCallback() != null) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_EXIT_STEP, null);
        }
    }

    public void onStart() {
        // 注册设备管理监听
        IDeviceMgr deviceMgr = AIotAppSdkFactory.getInstance().getDeviceMgr();
        if (deviceMgr != null) {
            deviceMgr.registerListener(this);
        }
    }

    public void onStop() {
        // 注销设备管理监听
        IDeviceMgr deviceMgr = AIotAppSdkFactory.getInstance().getDeviceMgr();
        if (deviceMgr != null) {
            deviceMgr.unregisterListener(this);
        }
    }

    public void editDeviceName(IotDevice device, String newName) {
        IDeviceMgr deviceMgr = AIotAppSdkFactory.getInstance().getDeviceMgr();
        if (deviceMgr != null) {
            deviceMgr.renameDevice(device, newName);
        }
    }

    @Override
    public void onDeviceRenameDone(int errCode, IotDevice iotDevice, String newName) {
        if (errCode == ErrCode.XOK) {
            //修改名称成功
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_EDIT_NAME_SUCCESS, newName);
        } else {
            //修改名称失败
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_EDIT_NAME_FAIL, null);
        }
    }

    /**
     * 源自sdk 处理逻辑
     * 先保存原设备列表 然后再通过对新的设备列表进行对比来得到新添加的设备信息
     */
    @Override
    public void onAllDevicesQueryDone(int errCode, List<IotDevice> deviceList) {
//        Log.d("cwtsw", "获取数据列表" + deviceList);
        DevicesListManager.devicesList.clear();
        DevicesListManager.devicesList.addAll(deviceList);
        Log.d("cwtsw", "mBeforeBindDevList " + mBeforeBindDevList);
        if (deviceList.size() > mBeforeBindDevList.size()) {
            //停止计时
            stopTimer();
            // find new device
            for (int i = 0; i < deviceList.size(); i++) {
                int j = 0;
                for (j = 0; j < mBeforeBindDevList.size(); j++) {
                    if (deviceList.get(i).mDeviceNumber.equals(mBeforeBindDevList.get(j).mDeviceNumber)) {
                        break;
                    }
                }
                // not found device in mBeforBindDevList, it's new device
                if (j == mBeforeBindDevList.size()) {
                    mNewDevice = deviceList.get(i);
                }
            }
            //跳转成功
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_ADD_SUCCESS, null);
        }
    }

    public String getUserId() {
        return AIotAppSdkFactory.getInstance().getAccountMgr().getQRCodeUserId();
    }

    /**
     * 移除设备，触发 onDeviceRemoveDone() 回调
     *
     * @param removingDev : 要移除的设备信息
     */
    public void removeDevice(IotDevice removingDev) {
        int ret = AIotAppSdkFactory.getInstance().getDeviceMgr().removeDevice(removingDev);
        if (ret != ErrCode.XOK) {
            Log.d("cwtsw", "要移除设备失败, 错误码: " + ret);
        }
    }

    @Override
    public void onDeviceRemoveDone(int errCode, IotDevice delDevice, List<IotDevice> deviceList) {
        if (errCode == ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_REMOVE_SUCCESS, null);
            ToastUtils.INSTANCE.showToast("移除设备成功");
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_REMOVE_FAIL, null);
            ToastUtils.INSTANCE.showToast("移除设备失败");
            Log.d("cwtsw", "移除设备失败, 错误码: " + errCode);
        }
    }

    /*
     * @brief 分享设备给其他人，需要对方接受，触发 onShareDeviceDone() 回调
     * @param iotDevice : 将要分享出去的设备
     * @param sharingAccount : 分享的目标账号
     * @param permission : 分享的权限：2--可以传导分享； 3--不能传导分享
     * @param needPeerAgree : 是否需要对端同意，为false时表示强制分享，无需对端同意
     * @return 错误码
     */
    public void shareDevice(IotDevice iotDevice, String sharingAccount, int permission, boolean needPeerAgree) {
        int ret = AIotAppSdkFactory.getInstance().getDeviceMgr().shareDevice(iotDevice, sharingAccount, permission, needPeerAgree);
        if (ret != ErrCode.XOK) {
            Log.d("cwtsw", "要分享备失败, 错误码: " + ret);
        }
    }

    /*
     * @brief 设备分享完成事件
     * @param errCode : 错误码
     * @param force : 是否强制分享
     * @param iotDevice : 将要分享出去的设备
     * @param sharingAccount : 分享的目标账号
     * @param permission : 分享的权限：2--可以传导分享； 3--不能传导分享
     */
    @Override
    public void onShareDeviceDone(int errCode, boolean force, final IotDevice iotDevice,
                                  final String sharingAccount, int permission) {
        if (errCode == ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_SHARE_TO_SUCCESS, null);
            ToastUtils.INSTANCE.showToast("设备分享成功");
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_SHARE_TO_FAIL, null);
            ToastUtils.INSTANCE.showToast("设备分享失败 errCode = " + errCode);
        }
    }

    /*
     * @brief 查询可取消分享的设备列表(该描述来自sdk)，触发 onQueryOutSharerListDone() 回调
     * @return 错误码
     */
    public void queryOutSharerList(final String deviceId) {
        int ret = AIotAppSdkFactory.getInstance().getDeviceMgr().queryOutSharerList(deviceId);
        if (ret != ErrCode.XOK) {
            Log.d("cwtsw", "要查询可取消分享的设备列表, 错误码: " + ret);
        }
    }

    @Override
    public void onQueryOutSharerListDone(int errCode, String
            deviceId, List<IotOutSharer> outSharerList) {
        if (errCode == ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_SHARE_TO_LIST_SUCCESS, outSharerList);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_SHARE_TO_LIST_FAIL, null);
            ToastUtils.INSTANCE.showToast("查询可取消分享的设备列表");
        }
    }

    /*
     * @brief 取消设备的分享权限，触发 onDeshareDeviceDone() 回调
     * @param outSharer : 要取消的已经分享的设备
     * @param unsharingAccount : 要取消权限的账号
     * @return 错误码
     */
    public void deshareDevice(final IotOutSharer outSharer) {
        int ret = AIotAppSdkFactory.getInstance().getDeviceMgr().deshareDevice(outSharer);
        if (ret != ErrCode.XOK) {
            Log.d("cwtsw", "要取消的已经分享失败, 错误码: " + ret);
        }
    }

    /*
     * @brief 完成设备的分享取消
     * @param errCode : 错误码
     * @param IotOutSharer : 要取消的分享
     */
    @Override
    public void onDeshareDeviceDone(int errCode, final IotOutSharer outSharer) {
        if (errCode == ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_SHARE_CANCEL_SUCCESS, null);
            ToastUtils.INSTANCE.showToast("取消分享成功");
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_SHARE_CANCEL_FAIL, null);
            ToastUtils.INSTANCE.showToast("取消分享失败");
        }
    }


    /**
     * 以下源自sdk ======== ========
     */
    private static IotDevice mNewDevice = null;

    public static IotDevice getNewDevice() {
        return mNewDevice;
    }

    private long mBaseTimer = 0;
    private Timer mTimer = null;
    public static final int MSGID_REFRESH_LIST = 0x1001;
    public static final int MSGID_TIMER_1S = 0x1002;
    private Handler mMsgHandler = null;
    private ArrayList<IotDevice> mBeforeBindDevList = new ArrayList<>();

    public void initHandler() {
        mBeforeBindDevList.clear();
        for (IotDevice device : DevicesListManager.devicesList) {
            if (!TextUtils.isEmpty(device.mDeviceNumber)) {
                mBeforeBindDevList.add(device);
            }
        }
        mMsgHandler = new Handler(Looper.myLooper()) {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case MSGID_TIMER_1S:
                        onMsgTimer1s(msg);
                        break;
                    case MSGID_REFRESH_LIST:
                        onMsgRefreshDevList(msg);
                        break;
                }
            }
        };
    }

    public void startTimer(AppCompatTextView txTimeRun) {
        mBaseTimer = SystemClock.elapsedRealtime();
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int time = (int) ((SystemClock.elapsedRealtime() - mBaseTimer) / 1000);
                Message msg = mMsgHandler.obtainMessage();
                msg.what = MSGID_TIMER_1S;
                msg.arg1 = time;
                msg.obj = txTimeRun;
                mMsgHandler.sendMessage(msg);
            }
        }, 0, 1000L);
    }

    public void stopTimer() {
        mTimer.cancel();
    }

    private void onMsgTimer1s(Message msg) {
        TextView txTimeRun = (TextView) msg.obj;
        String mm = new DecimalFormat("00").format(msg.arg1 % 3600 / 60);
        String ss = new DecimalFormat("00").format(msg.arg1 % 60);
        String timeFormat = mm + ":" + ss;
        txTimeRun.setText(timeFormat);
        if (msg.arg1 > 30) {
            mTimer.cancel();
            //超时
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_DEVICE_ADD_FAIL, null);
        } else {
            if (msg.arg1 % 4 == 0) {
                Message newMsg = mMsgHandler.obtainMessage();
                newMsg.what = MSGID_REFRESH_LIST;
                mMsgHandler.sendMessageDelayed(newMsg, 0);
            }
        }
    }

    private void onMsgRefreshDevList(Message msg) {
        int ret = AIotAppSdkFactory.getInstance().getDeviceMgr().queryAllDevices();
        if (ret != ErrCode.XOK) {
            Log.d("cwtsw", "查询绑定设备失败, 错误码: " + ret);
        }
    }
}
