package io.agora.iotlinkdemo.models.device.setting.mydevice;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.StringUtils;
import com.agora.baselibrary.utils.ToastUtils;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IDeviceMgr;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityDeviceSettingBinding;
import io.agora.iotlinkdemo.dialog.CommonDialog;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.device.DeviceViewModel;
import io.agora.iotlinkdemo.models.player.living.PlayerPreviewActivity;

import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 设备设置
 */
@Route(path = PagePathConstant.pageDeviceSetting)
public class DeviceSettingActivity extends BaseViewBindingActivity<ActivityDeviceSettingBinding> {
    private final String TAG = "IOTLINK/DevSettingAct";
    /**
     * 设备模块统一ViewModel
     */
    private DeviceViewModel deviceViewModel;

    @Override
    protected ActivityDeviceSettingBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDeviceSettingBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        deviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        deviceViewModel.setLifecycleOwner(this);
    }

    @Override
    public void initListener() {
        deviceViewModel.setISingleCallback((var1, var2) -> {
            hideLoadingView();
            if (var1 == Constant.CALLBACK_TYPE_DEVICE_REMOVE_SUCCESS) {
                mHealthActivityManager.finishActivityByClass("PlayerPreviewActivity");
                mHealthActivityManager.popActivity();

            } else if (var1 == Constant.CALLBACK_TYPE_FIRM_GETVERSION) {  // 获取固件版本
                IDeviceMgr.McuVersionInfo mcuVersionInfo = (IDeviceMgr.McuVersionInfo)var2;
                Log.d(TAG, "<ISingleCallback> mcuVerInfo=" + mcuVersionInfo.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateMcuVersionInfo();
                    }
                });
            }

        });
        getBinding().tvRemoveDevice.setOnClickListener(view -> {
            showRemoveDialog();
        });
        getBinding().tvDeviceName.setOnClickListener(view -> {
            PagePilotManager.pageDeviceInfoSetting();
        });
        getBinding().tvBaseSetting.setOnClickListener(view -> {
            PagePilotManager.pageDeviceBaseSetting();
        });
        getBinding().tvShareDevice.setOnClickListener(view -> {
            PagePilotManager.pageDeviceShareToUserList();
        });
        getBinding().tvDetectionAlarmSetting.setOnClickListener(view -> {
            ToastUtils.INSTANCE.showToast(getString(R.string.function_not_open));
        });

        updateMcuVersionInfo();
        getBinding().tvDeviceFirmwareUpgrade.setOnClickListener(view -> {
            IDeviceMgr.McuVersionInfo mcuVersion = AgoraApplication.getInstance().getLivingMcuVersion();
            if (mcuVersion == null) {   // 还未获取到固件版本信息
                Log.e(TAG, "<initListener> NOT get mcu version");
                return;
            }
            if ((mcuVersion != null) && (mcuVersion.mIsupgradable) &&
                 (mcuVersion.mUpgradeId > 0) && (!TextUtils.isEmpty(mcuVersion.mUpgradeVersion))) {
                PagePilotManager.pageDeviceFirmwareUpgrade();
            } else {
                popupLastVersionDlg(mcuVersion.mCurrVersion);
            }
        });

        getBinding().tvRebootDevice.setOnClickListener(view -> {
            ToastUtils.INSTANCE.showToast(getString(R.string.function_not_open));
        });
    }

    public void showRemoveDialog() {
        if (commonDialog == null) {
            commonDialog = new CommonDialog(this);
            commonDialog.setDialogTitle("确定移除设备吗？");
            commonDialog.setDialogBtnText(getString(R.string.cancel), getString(R.string.confirm));
            commonDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {

                }

                @Override
                public void onRightButtonClick() {
                    showLoadingView();
                    deviceViewModel.removeDevice(AgoraApplication.getInstance().getLivingDevice());
                }
            });
        }
        commonDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getBinding().tvDeviceName.setText(StringUtils.INSTANCE.getBase64String(AgoraApplication.getInstance().getLivingDevice().mDeviceName));

        // 延迟调用查询当前MCU固件版本版本
        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    public void run() {
                        deviceViewModel.queryMcuVersion();
                    }
                },
                300);
    }

    @Override
    protected void onStart() {
        super.onStart();
        deviceViewModel.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        deviceViewModel.onStop();
    }

    /**
     * @brief 更新版本信息显示
     */
    void updateMcuVersionInfo() {
        IDeviceMgr.McuVersionInfo mcuVersionInfo = AgoraApplication.getInstance().getLivingMcuVersion();
        if ((mcuVersionInfo != null) && (mcuVersionInfo.mIsupgradable) &&
                (mcuVersionInfo.mUpgradeId > 0) && (!TextUtils.isEmpty(mcuVersionInfo.mUpgradeVersion)) ) {
            getBinding().tvIsLastVersion.setText("有最新版本: " + mcuVersionInfo.mUpgradeVersion);
            getBinding().ivNeedUpgrade.setVisibility(View.VISIBLE);
        } else {
            getBinding().tvIsLastVersion.setText("已是最新版本");
            getBinding().ivNeedUpgrade.setVisibility(View.GONE);
        }
    }

    /**
     * @brief 显示当前已经是最新版本
     */
    void popupLastVersionDlg(final String version) {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View upgradeView = inflater.inflate(R.layout.dialog_firmware_lastversion, null);
        final TextView tvTitle = (TextView) upgradeView.findViewById(R.id.tvFirmLastVerTitle);

        String newVersion = "当前已是最新版本，版本号: " + version;
        tvTitle.setText(newVersion);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("")
                .setView(upgradeView)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }});
        AlertDialog dlg = builder.show();
        dlg.setCanceledOnTouchOutside(false);
    }

}
