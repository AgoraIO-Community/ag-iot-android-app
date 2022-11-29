package io.agora.iotlinkdemo.models.device.add;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.base.PermissionHandler;
import io.agora.iotlinkdemo.base.PushApplication;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.common.GlideApp;
import io.agora.iotlinkdemo.databinding.ActivityDeviceBtConfigBinding;
import io.agora.iotlinkdemo.deviceconfig.DeviceBtCfg;
import io.agora.iotlinkdemo.event.ResetAddDeviceEvent;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.device.DeviceViewModel;

import com.agora.baselibrary.utils.SPUtil;
import com.alibaba.android.arouter.facade.annotation.Route;

import org.greenrobot.eventbus.EventBus;

import java.util.List;


/**
 * 正在使用蓝牙配置设备
 * <p>
 * 添加设备第五步
 */
@Route(path = PagePathConstant.pageDeviceBtCfg)
public class DeviceAddStepBtCfgActivity extends BaseViewBindingActivity<ActivityDeviceBtConfigBinding>
        implements DeviceBtCfg.IBtCfgCallback   {
    private final String TAG = "IOTLINK/DevBtCfgAct";

    //
    // UI state machine
    //
    public static final int UISATE_CFG_IDLE = 0x00010;
    public static final int UISATE_CFG_PREPARING = 0x0001;
    public static final int UISATE_CFG_CONNECTED = 0x0002;
    public static final int UISATE_CFG_NETWORK = 0x0003;
    public static final int UISATE_CFG_CUSTOMDATA = 0x0004;
    public static final int UISATE_CFG_DEVRESTART = 0x0005;

    private static final int COLOR_BLACK = Color.argb(255, 0,0,0);
    private static final int COLOR_GRAY = Color.argb(255, 128,128,128);


    //
    // message Id
    //
    public static final int MSGID_CFG_PROGRESS = 0x1001;    ///< 配置进度
    public static final int MSGID_CFG_DONE = 0x1002;        ///< 配置完成


    private volatile boolean mForeground = false;       ///< 当前界面是否前景
    private Handler mMsgHandler = null;                 ///< 主线程中的消息处理

    private DeviceViewModel mDeviceViewModel;
    private int mUiState = UISATE_CFG_IDLE;
    private boolean mDestroyed = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "<onCreate>");

        mMsgHandler = new Handler(getMainLooper()) {
            @SuppressLint("HandlerLeak")
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case MSGID_CFG_PROGRESS: {
                        onMsgConfigProgress(msg.arg1);
                    } break;

                    case MSGID_CFG_DONE: {
                        onMsgConfigDone(msg.arg1);
                    } break;
                }
            }
        };


        DeviceBtCfg.getInstance().registerListener(this);  // 注册当前界面回调

        String ssid = SPUtil.Companion.getInstance(this).getString(Constant.WIFI_NAME, "");
        String wifiPswd = SPUtil.Companion.getInstance(this).getString(Constant.WIFI_PWD, "");
        String productId = SPUtil.Companion.getInstance(this).getString(Constant.FROM_QR_K, "");
        String userId = AIotAppSdkFactory.getInstance().getAccountMgr().getQRCodeUserId();;

        BluetoothDevice btDevice = PushApplication.getInstance().getCfgingBtDevice();
        DeviceBtCfg.BtCfgParam cfgParam = new DeviceBtCfg.BtCfgParam();
        cfgParam.mSsid = ssid;
        cfgParam.mPassword = wifiPswd;
        cfgParam.mProductId = productId;
        cfgParam.mUserId = userId;
        DeviceBtCfg.getInstance().deviceCfgStart(btDevice, cfgParam);

        mForeground = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "<onDestroy>");

        mForeground = false;
        if (mMsgHandler != null) {
            mMsgHandler.removeMessages(MSGID_CFG_PROGRESS);
            mMsgHandler.removeMessages(MSGID_CFG_DONE);
            mMsgHandler = null;
        }
        DeviceBtCfg.getInstance().unregisterListener(this);  // 注销当前界面回调
        DeviceBtCfg.getInstance().deviceCfgStop();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "<onStart>");
        super.onStart();
        mDeviceViewModel.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "<onStop>");
        super.onStop();
        mDeviceViewModel.stopTimer();
        mDeviceViewModel.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "<onResume>");
        mForeground = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "<onPause>");
        mForeground = false;
    }


    @Override
    protected ActivityDeviceBtConfigBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDeviceBtConfigBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        mDeviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        mDeviceViewModel.setLifecycleOwner(this);
        mDeviceViewModel.initHandler();
        updateCfgStatusUI();
    }

    @Override
    public void initListener() {
        mDeviceViewModel.setISingleCallback((var1, var2) -> {
            if (var1 == Constant.CALLBACK_TYPE_EXIT_STEP) {
                mHealthActivityManager.finishActivityByClass("DeviceAddStepBtCfgActivity");

            } else if (var1 == Constant.CALLBACK_TYPE_DEVICE_ADD_SUCCESS) {
                Log.d(TAG, "<initListener.setISingleCallback> DEVICE_ADD_SUCCESS");
                if (mDestroyed) {
                    return;
                }
                //成功添加
                PagePilotManager.pageAddResult(true);
                exitActivity();
                EventBus.getDefault().post(new ResetAddDeviceEvent());

            } else if (var1 == Constant.CALLBACK_TYPE_DEVICE_ADD_NOTIFY) {
                Log.d(TAG, "<initListener.setISingleCallback> DEVICE_ADD_NOTIFY");
                if (mDestroyed) {
                    return;
                }
                //成功添加
                PagePilotManager.pageAddResult(true);
                exitActivity();
                EventBus.getDefault().post(new ResetAddDeviceEvent());

            } else if (var1 == Constant.CALLBACK_TYPE_DEVICE_ADD_FAIL) {
                Log.d(TAG, "<initListener.setISingleCallback> DEVICE_ADD_FAIL");
                if (mDestroyed) {
                    return;
                }
                //超时
                PagePilotManager.pageDeviceBtCfgResult();
                exitActivity();
                EventBus.getDefault().post(new ResetAddDeviceEvent());
            }
        });

        getBinding().titleView.setRightIconClick(view -> mHealthActivityManager.popActivity());
        updateCfgStatusUI();
    }

    void onMsgConfigProgress(int cfgStatus) {
        Log.d(TAG, "<onMsgConfigProgress> cfgStatus=" + cfgStatus);

        switch (cfgStatus) {
            case DeviceBtCfg.CFG_STATUS_IDLE: {
                mUiState = UISATE_CFG_IDLE;
            } break;

            case DeviceBtCfg.CFG_STATUS_PREPARING: {
                mUiState = UISATE_CFG_PREPARING;
            } break;

            case DeviceBtCfg.CFG_STATUS_CONNECTED: {
                mUiState = UISATE_CFG_CONNECTED;
            } break;

            case DeviceBtCfg.CFG_STATUS_NETWORK: {
                mUiState = UISATE_CFG_NETWORK;
            } break;

            case DeviceBtCfg.CFG_STATUS_CUSTOMDATA: {
                mUiState = UISATE_CFG_CUSTOMDATA;
            } break;

            case DeviceBtCfg.CFG_STATUS_DONE: {
                mUiState = UISATE_CFG_DEVRESTART;
            } break;
        }
        updateCfgStatusUI();
    }

    void onMsgConfigDone(int errCode) {
        Log.d(TAG, "<onMsgConfigDone> errCode=" + errCode);
        DeviceBtCfg.getInstance().deviceCfgStop();

        if (errCode == 0) {
            //popupMessage("蓝牙设备配置成功!");
            mUiState = UISATE_CFG_DEVRESTART;
            updateCfgStatusUI();
            mDeviceViewModel.startTimer(null);

        } else {
            //popupMessage("蓝牙设备配置失败, 错误码=" + errCode);
            PagePilotManager.pageDeviceBtCfgResult();
            exitActivity();
            EventBus.getDefault().post(new ResetAddDeviceEvent());
        }
    }

    private void exitActivity() {
        mDestroyed = true;
        getBinding().titleView.postDelayed(() -> {
            mHealthActivityManager.finishActivityByClass("DeviceAddStepBtScanActivity");
            mHealthActivityManager.finishActivityByClass("DeviceAddStepBtCfgActivity");
        }, 500);
    }

    /**
     * @brief 根据当前配置进度更新UI显示
     */
    void updateCfgStatusUI() {
        switch (mUiState) {
            case UISATE_CFG_IDLE:
            case UISATE_CFG_PREPARING:{
                getBinding().tvBtCfgTips1.setTextColor(COLOR_BLACK);
                getBinding().tvBtCfgTips2.setTextColor(COLOR_BLACK);
                getBinding().tvBtCfgTips3.setTextColor(COLOR_BLACK);
                getBinding().tvBtCfgTips4.setTextColor(COLOR_BLACK);
                getBinding().tvBtCfgTips5.setTextColor(COLOR_BLACK);
            } break;

            case UISATE_CFG_CONNECTED: {
                getBinding().tvBtCfgTips1.setTextColor(COLOR_GRAY);
                getBinding().tvBtCfgTips2.setTextColor(COLOR_BLACK);
                getBinding().tvBtCfgTips3.setTextColor(COLOR_BLACK);
                getBinding().tvBtCfgTips4.setTextColor(COLOR_BLACK);
                getBinding().tvBtCfgTips5.setTextColor(COLOR_BLACK);
            } break;

            case UISATE_CFG_NETWORK: {
                getBinding().tvBtCfgTips1.setTextColor(COLOR_GRAY);
                getBinding().tvBtCfgTips2.setTextColor(COLOR_GRAY);
                getBinding().tvBtCfgTips3.setTextColor(COLOR_BLACK);
                getBinding().tvBtCfgTips4.setTextColor(COLOR_BLACK);
                getBinding().tvBtCfgTips5.setTextColor(COLOR_BLACK);
            } break;

            case UISATE_CFG_CUSTOMDATA: {
                getBinding().tvBtCfgTips1.setTextColor(COLOR_GRAY);
                getBinding().tvBtCfgTips2.setTextColor(COLOR_GRAY);
                getBinding().tvBtCfgTips3.setTextColor(COLOR_GRAY);
                getBinding().tvBtCfgTips4.setTextColor(COLOR_BLACK);
                getBinding().tvBtCfgTips5.setTextColor(COLOR_BLACK);
            } break;

            case UISATE_CFG_DEVRESTART: {
                getBinding().tvBtCfgTips1.setTextColor(COLOR_GRAY);
                getBinding().tvBtCfgTips2.setTextColor(COLOR_GRAY);
                getBinding().tvBtCfgTips3.setTextColor(COLOR_GRAY);
                getBinding().tvBtCfgTips4.setTextColor(COLOR_GRAY);
                getBinding().tvBtCfgTips5.setTextColor(COLOR_BLACK);
            } break;
        }
    }

    /////////////////////////////////////////////////////////////////////////////////
    ////////////////// Override methods of DeviceBtCfg.IBtCfgCallback ///////////////
    /////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onConfigProgress(int cfgStatus) {
        Log.d(TAG, "<onConfigProgress> cfgStatus=" + cfgStatus);
        if (mMsgHandler != null) {
            Message msg = new Message();
            msg.what = MSGID_CFG_PROGRESS;
            msg.arg1 = cfgStatus;
            mMsgHandler.removeMessages(MSGID_CFG_PROGRESS);
            mMsgHandler.sendMessage(msg);
        }
    }


    @Override
    public void onConfigDone(int errCode) {
        Log.d(TAG, "<onConfigDone> errCode=" + errCode);
        if (mMsgHandler != null) {
            Message msg = new Message();
            msg.what = MSGID_CFG_DONE;
            msg.arg1 = errCode;
            mMsgHandler.removeMessages(MSGID_CFG_DONE);
            mMsgHandler.sendMessage(msg);
        }
    }

}
