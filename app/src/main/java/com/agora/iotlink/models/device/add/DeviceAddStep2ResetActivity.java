package com.agora.iotlink.models.device.add;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.common.Constant;
import com.agora.iotlink.databinding.ActivityResetDeviceBinding;
import com.agora.iotlink.manager.PagePathConstant;
import com.agora.iotlink.manager.PagePilotManager;
import com.agora.iotlink.models.device.DeviceViewModel;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 重置设备
 * <p>
 * 添加设备第二步
 */
@Route(path = PagePathConstant.pageResetDevice)
public class DeviceAddStep2ResetActivity extends BaseViewBindingActivity<ActivityResetDeviceBinding> {
    /**
     * 设备模块统一ViewModel
     */
    private DeviceViewModel deviceViewModel;

    @Override
    protected ActivityResetDeviceBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityResetDeviceBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        deviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        deviceViewModel.setLifecycleOwner(this);
        deviceViewModel.setISingleCallback((var1, var2) -> {
            if (var1 == Constant.CALLBACK_TYPE_EXIT_STEP) {
                mHealthActivityManager.finishActivityByClass("DeviceAddStep1ScanningActivity");
                mHealthActivityManager.finishActivityByClass("DeviceAddStep2ResetActivity");
            }
        });
    }

    @Override
    public void initListener() {
        getBinding().titleView.setRightIconClick(view -> mHealthActivityManager.popActivity());
        getBinding().cbConfirm.setOnCheckedChangeListener((compoundButton, b) -> {
            getBinding().btnNextStep.setEnabled(b);
        });
        getBinding().btnNextStep.setOnClickListener(view -> {
            PagePilotManager.pageSetDeviceWifi();
        });
    }
}
