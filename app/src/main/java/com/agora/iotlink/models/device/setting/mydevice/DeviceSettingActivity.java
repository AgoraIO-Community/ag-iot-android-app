package com.agora.iotlink.models.device.setting.mydevice;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.StringUtils;
import com.agora.baselibrary.utils.ToastUtils;
import com.agora.iotlink.R;
import com.agora.iotlink.base.AgoraApplication;
import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.common.Constant;
import com.agora.iotlink.databinding.ActivityDeviceSettingBinding;
import com.agora.iotlink.dialog.CommonDialog;
import com.agora.iotlink.manager.PagePathConstant;
import com.agora.iotlink.manager.PagePilotManager;
import com.agora.iotlink.models.device.DeviceViewModel;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 设备设置
 */
@Route(path = PagePathConstant.pageDeviceSetting)
public class DeviceSettingActivity extends BaseViewBindingActivity<ActivityDeviceSettingBinding> {
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
        getBinding().tvDeviceFirmwareUpgrade.setOnClickListener(view -> {
            ToastUtils.INSTANCE.showToast(getString(R.string.function_not_open));
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
}
