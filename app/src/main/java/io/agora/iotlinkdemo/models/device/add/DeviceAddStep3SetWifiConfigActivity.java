package io.agora.iotlinkdemo.models.device.add;

import android.annotation.SuppressLint;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.NetUtils;
import com.agora.baselibrary.utils.SPUtil;
import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivitySetDeviceWifiBinding;
import io.agora.iotlinkdemo.deviceconfig.DeviceBtCfg;
import io.agora.iotlinkdemo.dialog.CommonDialog;
import io.agora.iotlinkdemo.dialog.SelectDevCfgModeDialog;
import io.agora.iotlinkdemo.dialog.SelectPirDialog;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.device.DeviceViewModel;
import io.agora.iotlinkdemo.models.device.add.adapter.BtDevListAdapter;

import com.alibaba.android.arouter.facade.annotation.Route;

import java.util.List;

/**
 * 设置设备wifi
 * <p>
 * 添加设备第三步
 */
@Route(path = PagePathConstant.pageSetDeviceWifi)
public class DeviceAddStep3SetWifiConfigActivity extends BaseViewBindingActivity<ActivitySetDeviceWifiBinding> {
    private final String TAG = "IOTLINK/DevAdd3Act";

    /**
     * 设备模块统一ViewModel
     */
    private DeviceViewModel deviceViewModel;

    private SelectDevCfgModeDialog mSelectCfgModeDlg;


    @Override
    protected ActivitySetDeviceWifiBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivitySetDeviceWifiBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        deviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        deviceViewModel.setLifecycleOwner(this);
        deviceViewModel.setISingleCallback((var1, var2) -> {
            if (var1 == Constant.CALLBACK_TYPE_EXIT_STEP) {
                mHealthActivityManager.finishActivityByClass("DeviceAddStep3SetWifiConfigActivity");
            }
        });

        getBinding().titleView.setRightIconClick(view -> mHealthActivityManager.popActivity());
        getBinding().tvSelectWifi.setOnClickListener(view -> PagePilotManager.pageWifiList(this));
        getBinding().btnNextStep.setOnClickListener(view -> {
            onBtnNext();
        });
        getBinding().etInputPWD.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!TextUtils.isEmpty(getBinding().tvSelectWifi.getText())) {
                    if (editable.length() >= 8) {
                        getBinding().btnNextStep.setEnabled(true);
                    } else {
                        getBinding().btnNextStep.setEnabled(false);
                    }
                } else {
                    getBinding().btnNextStep.setEnabled(false);
                }
            }
        });
        if (!NetUtils.INSTANCE.isWifiConnected(this)) {
            showNoConnectWifiDialog();
        } else {
            getBinding().tvSelectWifi.setText(getWifiName());
        }
        getBinding().btnNextStep.setEnabled(true);
    }

    /**
     * 获取悬浮窗权限提示
     */
    public void showNoConnectWifiDialog() {
        if (commonDialog == null) {
            commonDialog = new CommonDialog(this);
            commonDialog.setDialogTitle("目前手机没有连接WiFi");
            commonDialog.setDescText("手机连接WiFi后才能与设备联网");
            commonDialog.setDialogBtnText(getString(R.string.cancel), getString(R.string.connect));
            commonDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {
                }

                @Override
                public void onRightButtonClick() {
                    startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                }
            });
        }
        commonDialog.show();
    }

    private String getWifiName() {
        WifiManager wifi_service = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifi_service.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            String ssid = data.getStringExtra(Constant.SSID);
            getBinding().tvSelectWifi.setText(ssid);
            getBinding().etInputPWD.setEnabled(true);
            getBinding().etInputPWD.setText("");
        }
    }

    void onBtnNext() {
        String wifiName = getBinding().tvSelectWifi.getText().toString();
        String wifiPWD = getBinding().etInputPWD.getText().toString();
        if (TextUtils.isEmpty(wifiName) || TextUtils.isEmpty(wifiPWD)) {
            ToastUtils.INSTANCE.showToast("请选择wifi 并填入密码");
            return;
        }
        if (!NetUtils.INSTANCE.isWifiConnected(this)) {
            showNoConnectWifiDialog();
            return;
        }

        SPUtil.Companion.getInstance(this).putString(Constant.WIFI_NAME, wifiName);
        SPUtil.Companion.getInstance(this).putString(Constant.WIFI_PWD, wifiPWD);
        if (mSelectCfgModeDlg == null) {
            mSelectCfgModeDlg = new SelectDevCfgModeDialog(this);
            mSelectCfgModeDlg.iSingleCallback = (type, data) -> {
                if (type == 0) {    // Camera配网模式
                    PagePilotManager.pageDeviceQR();
                } else if (type == 1) {  // 蓝牙配网模式
                    PagePilotManager.pageDeviceBtScan();
                }
            };
        }
        mSelectCfgModeDlg.show();
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "<onCreate>");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "<onDestroy>");
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
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "<onPause>");
    }
}
