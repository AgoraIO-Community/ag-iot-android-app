package io.agora.iotlinkdemo.models.device.add;


import android.annotation.SuppressLint;
import android.bluetooth.le.ScanResult;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.agora.baselibrary.utils.SPUtil;
import com.agora.baselibrary.utils.ScreenUtils;

import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.base.PermissionHandler;
import io.agora.iotlinkdemo.base.PermissionItem;
import io.agora.iotlinkdemo.base.PushApplication;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityDeviceBtScanBinding;
import io.agora.iotlinkdemo.deviceconfig.DeviceBtCfg;
import io.agora.iotlinkdemo.event.ResetAddDeviceEvent;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.device.DeviceViewModel;
import io.agora.iotlinkdemo.models.device.add.adapter.BtDevListAdapter;
import io.agora.iotlinkdemo.utils.ZXingUtils;
import com.alibaba.android.arouter.facade.annotation.Route;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 扫描蓝牙设备
 * <p>
 * 添加设备第四步
 */
@Route(path = PagePathConstant.pageDeviceBtScan)
public class DeviceAddStepBtScanActivity extends BaseViewBindingActivity<ActivityDeviceBtScanBinding>
        implements DeviceBtCfg.IBtCfgCallback, PermissionHandler.ICallback   {
    private final String TAG = "IOTLINK/DevBtScanAct";

    //
    // UI state machine
    //
    public static final int UISATE_IDLE = 0x0000;
    public static final int UISATE_SCANNING = 0x0001;
    public static final int UISATE_SCAN_DONE = 0x0002;
    public static final int UISATE_SCAN_ERROR = 0x0003;


    //
    // message Id
    //
    public static final int MSGID_CHECK_BT_PERMISSION = 0x1001;    ///< 检测蓝牙权限
    public static final int MSGID_SCAN_UPDATE = 0x1002;            ///< 扫描定时更新
    public static final int MSGID_SCAN_DONE = 0x1003;              ///< 扫描超时时间到,扫描完成
    public static final int MSGID_SCAN_ERROR = 0x1004;             ///< 扫描出错


    private volatile boolean mForeground = false;       ///< 当前界面是否前景

    private PermissionHandler mPermHandler;             ///< 权限申请处理
    private Handler mMsgHandler = null;                 ///< 主线程中的消息处理
    private int mUIState = UISATE_IDLE;

    private BtDevListAdapter mBtDevListAdapter;
    private ArrayList<ScanResult> mBtDeviceList = new ArrayList<>();  ///< 当前扫描到的所有设备
    android.bluetooth.le.ScanResult mSelectedResult = null;




    ////////////////////////////////////////////////////////////////////////////
    ///////////////////////////// Methods for Activity UI //////////////////////
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "<onCreate>");
        mForeground = false;

        mMsgHandler = new Handler(getMainLooper()) {
            @SuppressLint("HandlerLeak")
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case MSGID_CHECK_BT_PERMISSION: {
                        onMsgCheckBtPermission();
                    } break;

                    case MSGID_SCAN_UPDATE: {
                        onMsgScanUpdate();
                    } break;

                    case MSGID_SCAN_DONE: {
                        onMsgScanDone();
                    } break;

                    case MSGID_SCAN_ERROR: {
                        onMsgScanError(msg.arg1);
                    } break;
                }
            }
        };


        DeviceBtCfg.getInstance().registerListener(this);  // 注册当前界面回调

        List<android.bluetooth.le.ScanResult> scannedList = DeviceBtCfg.getInstance().getScannedDevices();
        synchronized (mBtDeviceList) {
            mBtDeviceList.clear();
            mBtDeviceList.addAll(scannedList);
        }
        mBtDevListAdapter = new BtDevListAdapter(this, mBtDeviceList);
        getBinding().rvScannedDev.setAdapter(mBtDevListAdapter);
        getBinding().rvScannedDev.setLayoutManager(new LinearLayoutManager(this));

        mBtDevListAdapter.setMRVItemClickListener((view, position, data) -> {
            onDevItemClick(view, position, data);
        });
        mSelectedResult = null;
        mUIState = UISATE_IDLE;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "<onDestroy>");

        mForeground = false;
        if (mMsgHandler != null) {
            mMsgHandler.removeMessages(MSGID_CHECK_BT_PERMISSION);
            mMsgHandler.removeMessages(MSGID_SCAN_UPDATE);
            mMsgHandler.removeMessages(MSGID_SCAN_DONE);
            mMsgHandler.removeMessages(MSGID_SCAN_ERROR);
            mMsgHandler = null;
        }
        DeviceBtCfg.getInstance().unregisterListener(this);  // 注销当前界面回调
        DeviceBtCfg.getInstance().scanStop(); // 如果当前正在扫描设备,则立即停止
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "<onStart>");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "<onStop>");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "<onResume>");

        if (mMsgHandler != null) {
            mUIState = UISATE_IDLE;
            mMsgHandler.removeMessages(MSGID_CHECK_BT_PERMISSION);
            mMsgHandler.sendEmptyMessage(MSGID_CHECK_BT_PERMISSION);
        }

        mForeground = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "<onPause>");
        DeviceBtCfg.getInstance().scanStop(); // 如果当前正在扫描设备,则立即停止
        mUIState = UISATE_IDLE;
        mForeground = false;
    }

    @Override
    protected ActivityDeviceBtScanBinding getViewBinding(@NonNull LayoutInflater inflater) {
        Log.d(TAG, "<getViewBinding>");
        return ActivityDeviceBtScanBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "<initView>");
        updateScanUI();
    }

    @Override
    public void requestData() {
        Log.d(TAG, "<requestData>");
    }

    @Override
    public void initListener() {
        Log.d(TAG, "<initListener>");
        getBinding().titleView.setRightIconClick(view -> mHealthActivityManager.popActivity());
        getBinding().btnNextStep.setOnClickListener(view -> {
            onBtnNext();
        });
    }


    /**
     * @brief 检测蓝牙扫描权限
     */
    void onMsgCheckBtPermission() {
        //
        // 蓝牙 权限判断处理
        //
        int[] permIdArray;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permIdArray = new int[3];
            permIdArray[0] = PermissionHandler.PERM_ID_FINELOCAL;
            permIdArray[1] = PermissionHandler.PERM_ID_BLUETOOTH_SCAN;
            permIdArray[2] = PermissionHandler.PERM_ID_BLUETOOTH_CONNECT;
        } else {
            permIdArray = new int[1];
            permIdArray[0] = PermissionHandler.PERM_ID_FINELOCAL;
        }
        mPermHandler = new PermissionHandler(this, this, permIdArray);
        if (!mPermHandler.isAllPermissionGranted()) {
            Log.d(TAG, "<onMsgCheckBtPermission> requesting permission...");
            mPermHandler.requestNextPermission();
        } else {
            Log.d(TAG, "<onMsgCheckBtPermission> permission ready");
            onStartBtDevScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "<onFragRequestPermissionsResult> requestCode=" + requestCode);
        if (mPermHandler != null) {
            mPermHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onAllPermisonReqDone(boolean allGranted, final PermissionItem[] permItems) {
        Log.d(TAG, "<onAllPermisonReqDone> allGranted = " + allGranted);

        if (permItems[0].requestId == PermissionHandler.PERM_ID_WRITE_STORAGE) {  // 截图权限
            if (allGranted) {
                onStartBtDevScan();
            } else {
                popupMessage(getString(R.string.no_permission));
            }
        }
    }

    void onStartBtDevScan() {
        Log.d(TAG, "<onStartBtDevScan>");

        mUIState = UISATE_SCANNING;
        if (!DeviceBtCfg.getInstance().isScanning()) {
            DeviceBtCfg.getInstance().scanStart();
        }
    }

    void onBtnNext() {
        int scannedCount = DeviceBtCfg.getInstance().getScannedDevCount();
        if (scannedCount <= 0) {
            Log.e(TAG, "<onBtnNext> no scanned device list");
            return;
        }

        if (mSelectedResult == null) {
            Log.e(TAG, "<onBtnNext> no scan result selected");
            return;
        }

        DeviceBtCfg.getInstance().scanStop();

        String devName = "";
        String devAddress = "";
        try {
            devName = mSelectedResult.getDevice().getName();
            devAddress = mSelectedResult.getDevice().getAddress();
        } catch (SecurityException securityExp) {
            securityExp.printStackTrace();
        }
        Log.d(TAG, "<onBtnNext> devName=" + devName);
        PushApplication.getInstance().setCfgingBtDevice(mSelectedResult.getDevice());
        PagePilotManager.pageDeviceBtConfig();
    }

    /**
     * @brief 选择某个设备，开始进行蓝牙配网操作
     */
    void onDevItemClick(View view, int position,
                        android.bluetooth.le.ScanResult btScanRslt) {

        int currSelectPos = mBtDevListAdapter.getSelectPosition();
        if (currSelectPos == position) {    // 当前Item已经选中,设置为未选中
            mSelectedResult = null;
            mBtDevListAdapter.setSelectPosition(-1);
            mBtDevListAdapter.notifyDataSetChanged();

        } else {
            mSelectedResult = btScanRslt;
            mBtDevListAdapter.setSelectPosition(position);
            mBtDevListAdapter.notifyDataSetChanged();
        }
    }


    void onMsgScanUpdate() {
        updateScanUI();
        refreshDevList();
    }

    void onMsgScanDone() {
        DeviceBtCfg.getInstance().scanStop();

        mUIState = UISATE_SCAN_DONE;
        if (DeviceBtCfg.getInstance().getScannedDevCount() <= 0) {  // 没有扫描到设备
            Log.d(TAG, "<onMsgScanDone> no scanned devices");

            if (mForeground) {
                PagePilotManager.pageDeviceBtScanResult();
                exitActivity();
                EventBus.getDefault().post(new ResetAddDeviceEvent());
            }
            return;
        }

        updateScanUI();
        refreshDevList();
    }

    void onMsgScanError(int errCode) {
        DeviceBtCfg.getInstance().scanStop();

        mUIState = UISATE_SCAN_ERROR;
        if (DeviceBtCfg.getInstance().getScannedDevCount() <= 0) {  // 没有扫描到设备
            Log.d(TAG, "<onMsgScanError> no scanned devices");
            if (mForeground) {
                PagePilotManager.pageDeviceBtScanResult();
                exitActivity();
                EventBus.getDefault().post(new ResetAddDeviceEvent());
            }
            return;
        }

        updateScanUI();
        refreshDevList();
    }

    private void exitActivity() {
        getBinding().titleView.postDelayed(() -> mHealthActivityManager.finishActivityByClass("DeviceAddStepBtScanActivity"), 500);
    }

    void refreshDevList() {
        List<android.bluetooth.le.ScanResult> scannedList = DeviceBtCfg.getInstance().getScannedDevices();
        synchronized (mBtDeviceList) {
            mBtDeviceList.clear();
            mBtDeviceList.addAll(scannedList);
        }
        mBtDevListAdapter.notifyDataSetChanged();
    }

    void updateScanUI() {
        if (mUIState == UISATE_IDLE) {  // 界面空闲状态 (正在检测权限)
            getBinding().pbBtScanProgress.setVisibility(View.INVISIBLE);
            getBinding().rvScannedDev.setVisibility(View.INVISIBLE);
            getBinding().btnNextStep.setEnabled(false);
            return;
        }

        if (mUIState == UISATE_SCANNING) {  // 正在扫描状态
            int scannedCount = DeviceBtCfg.getInstance().getScannedDevCount();
            if (scannedCount <= 0) {    // 还没有扫描到设备
                getBinding().tvBtScanTips.setText(getString(R.string.device_bt_scanning_tips));
                getBinding().pbBtScanProgress.setVisibility(View.VISIBLE);
                getBinding().rvScannedDev.setVisibility(View.INVISIBLE);
                getBinding().btnNextStep.setEnabled(false);

            } else {    // 至少扫描到一个蓝牙设备
                getBinding().tvBtScanTips.setText(getString(R.string.device_bt_scannedone_tips));
                getBinding().pbBtScanProgress.setVisibility(View.INVISIBLE);
                getBinding().rvScannedDev.setVisibility(View.VISIBLE);
                getBinding().btnNextStep.setEnabled(true);
            }
            return;
        }


        if (mUIState == UISATE_SCAN_DONE || mUIState == UISATE_SCAN_ERROR) {  // 扫描完成/错误状态
            int scannedCount = DeviceBtCfg.getInstance().getScannedDevCount();
            if (scannedCount <= 0) {    // 还没有扫描到设备
                getBinding().tvBtScanTips.setText(getString(R.string.device_bt_scanning_tips));
                getBinding().pbBtScanProgress.setVisibility(View.VISIBLE);
                getBinding().rvScannedDev.setVisibility(View.INVISIBLE);
                getBinding().btnNextStep.setEnabled(false);

            } else {    // 至少扫描到一个蓝牙设备
                getBinding().tvBtScanTips.setText(getString(R.string.device_bt_scannedone_tips));
                getBinding().pbBtScanProgress.setVisibility(View.INVISIBLE);
                getBinding().rvScannedDev.setVisibility(View.VISIBLE);
                getBinding().btnNextStep.setEnabled(true);
            }
            return;
        }
    }



    /////////////////////////////////////////////////////////////////////////////////
    ////////////////// Override methods of DeviceBtCfg.IBtCfgCallback ///////////////
    /////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onScanProgress(List<android.bluetooth.le.ScanResult> devices) {
        Log.d(TAG, "<onScanProgress> devices=" + devices.size());
        if (mMsgHandler != null) {
            mMsgHandler.removeMessages(MSGID_SCAN_UPDATE);
            mMsgHandler.sendEmptyMessage(MSGID_SCAN_UPDATE);
        }
    }

    @Override
    public void onScanDone(List<android.bluetooth.le.ScanResult> devices) {
        Log.d(TAG, "<onScanDone> devices=" + devices.size());
        if (mMsgHandler != null) {
            mMsgHandler.removeMessages(MSGID_SCAN_DONE);
            mMsgHandler.sendEmptyMessage(MSGID_SCAN_DONE);
        }
    }

    @Override
    public void onScanError(int errCode) {
        Log.d(TAG, "<onScanError> errCode=" + errCode);
        if (mMsgHandler != null) {
            Message msg = new Message();
            msg.what = MSGID_SCAN_ERROR;
            msg.arg1 = errCode;
            mMsgHandler.removeMessages(MSGID_SCAN_ERROR);
            mMsgHandler.sendMessage(msg);
        }
    }
}
