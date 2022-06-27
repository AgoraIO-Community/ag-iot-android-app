package com.agora.iotlink.models.device.setting.fromshare;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.StringUtils;
import com.agora.iotlink.R;
import com.agora.iotlink.base.AgoraApplication;
import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.common.Constant;
import com.agora.iotlink.databinding.ActivityDeviceShareSettingBinding;
import com.agora.iotlink.dialog.CommonDialog;
import com.agora.iotlink.manager.PagePathConstant;
import com.agora.iotlink.manager.PagePilotManager;
import com.agora.iotlink.models.device.DeviceViewModel;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 共享的设备设置 与DeviceSettingActivity 是同级的
 * <p>
 * 交付测试前sdk 未提供此功能
 */
@Route(path = PagePathConstant.pageShareDeviceSetting)
public class ShareDeviceSettingActivity extends BaseViewBindingActivity<ActivityDeviceShareSettingBinding> {
    /**
     * 设备模块统一ViewModel
     */
    private DeviceViewModel deviceViewModel;

    @Override
    protected ActivityDeviceShareSettingBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDeviceShareSettingBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        deviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        deviceViewModel.setLifecycleOwner(this);
    }

    @Override
    public void requestData() {
        getBinding().tvDeviceName.setText(AgoraApplication.getInstance().getLivingDevice().mDeviceName);
        getBinding().tvDeviceIdValue.setText(AgoraApplication.getInstance().getLivingDevice().mDeviceNumber);
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
        getBinding().tvDeviceName.setOnClickListener(view -> {
            PagePilotManager.pageDeviceInfoSetting();
        });
        getBinding().tvRemoveShare.setOnClickListener(view -> {
            showRemoveDialog();
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
