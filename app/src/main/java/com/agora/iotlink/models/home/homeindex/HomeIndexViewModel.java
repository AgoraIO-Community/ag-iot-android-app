package com.agora.iotlink.models.home.homeindex;

import android.util.Log;

import com.agora.baselibrary.base.BaseViewModel;
import com.agora.baselibrary.utils.StringUtils;
import com.agora.baselibrary.utils.ToastUtils;
import com.agora.iotlink.BuildConfig;
import com.agora.iotlink.base.AgoraApplication;
import com.agora.iotlink.event.UserLogoutEvent;
import com.agora.iotlink.manager.PagePilotManager;
import com.agora.iotlink.utils.ErrorToastUtils;
import com.agora.iotsdk20.AIotAppSdkFactory;
import com.agora.iotsdk20.ErrCode;
import com.agora.iotsdk20.ICallkitMgr;
import com.agora.iotsdk20.IDeviceMgr;
import com.agora.iotsdk20.IotDevice;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 设备相关viewModel
 */
public class HomeIndexViewModel extends BaseViewModel implements IDeviceMgr.ICallback, ICallkitMgr.ICallback {
    public HomeIndexViewModel() {
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onCleared() {
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(@Nullable UserLogoutEvent event) {
        if (getISingleCallback() != null) {
            getISingleCallback().onSingleCallback(999, null);
        }
    }

    /**
     * 请求sdk 获取设备列表 onDeviceQueryAllDone
     */
    public void requestDeviceList() {
        IDeviceMgr deviceMgr= AIotAppSdkFactory.getInstance().getDeviceMgr();
        if (deviceMgr != null) {
            deviceMgr.queryAllDevices();
        }
    }

    public void onStart() {
        // 注册账号管理监听
        IDeviceMgr deviceMgr= AIotAppSdkFactory.getInstance().getDeviceMgr();
        ICallkitMgr callkitMgr= AIotAppSdkFactory.getInstance().getCallkitMgr();
        if (deviceMgr != null) {
            deviceMgr.registerListener(this);
        }
        if (callkitMgr != null) {
            callkitMgr.registerListener(this);
        }
    }

    public void onStop() {
        // 注销账号管理监听
        IDeviceMgr deviceMgr= AIotAppSdkFactory.getInstance().getDeviceMgr();
        ICallkitMgr callkitMgr= AIotAppSdkFactory.getInstance().getCallkitMgr();
        if (deviceMgr != null) {
            deviceMgr.unregisterListener(this);
        }
        if (callkitMgr != null) {
            callkitMgr.unregisterListener(this);
        }
    }

    @Override
    public void onAllDevicesQueryDone(int errCode, List<IotDevice> deviceList) {
        Log.d("cwtsw", "deviceList = " + deviceList);
        getISingleCallback().onSingleCallback(2, deviceList);
//        DevicesListManager.Companion.getINSTANCE().addAllDevice(deviceList);
    }

    /**
     * 设备上下线事件
     *
     * @param iotDevice     : 相应的设备信息
     * @param online        : 上线还是下线事件
     * @param bindedDevList : 更新状态后的当前绑定设备列表
     */
    @Override
    public void onDeviceOnOffLine(IotDevice iotDevice, boolean online,
                                  List<IotDevice> bindedDevList) {
        requestDeviceList();
    }

    /**
     * 呼叫设备 即连接设备
     */
    public int callDial(IotDevice iotDevice, String attachMsg) {
        return AIotAppSdkFactory.getInstance().getCallkitMgr().callDial(iotDevice, attachMsg);
    }

    @Override
    public void onDialDone(int errCode, IotDevice iotDevice) {
        getISingleCallback().onSingleCallback(0, null);
        if (errCode != ErrCode.XOK) {
            ErrorToastUtils.showCallError(errCode);
            ToastUtils.INSTANCE.showToast("呼叫:" + StringUtils.INSTANCE.getBase64String(iotDevice.mDeviceName) + " 错误，错误码：" + errCode);
            if (BuildConfig.DEBUG) {
                AgoraApplication.getInstance().setLivingDevice(iotDevice);
                PagePilotManager.pagePreviewPlay();
            }
        } else {
            AgoraApplication.getInstance().setLivingDevice(iotDevice);
            PagePilotManager.pagePreviewPlay();
        }
    }
}
