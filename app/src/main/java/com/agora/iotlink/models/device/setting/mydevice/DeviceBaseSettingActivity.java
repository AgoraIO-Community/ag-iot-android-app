package com.agora.iotlink.models.device.setting.mydevice;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.common.Constant;
import com.agora.iotlink.databinding.ActivityDeviceBaseSettingBinding;
import com.agora.iotlink.manager.PagePathConstant;
import com.agora.iotlink.models.device.DeviceViewModel;
import com.agora.iotlink.models.player.PlayerViewModel;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 设备基本功能设置
 */
@Route(path = PagePathConstant.pageDeviceBaseSetting)
public class DeviceBaseSettingActivity extends BaseViewBindingActivity<ActivityDeviceBaseSettingBinding> {
    /**
     * 设备模块统一ViewModel
     */
    private PlayerViewModel playerViewModel;

    @Override
    protected ActivityDeviceBaseSettingBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDeviceBaseSettingBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        playerViewModel = new ViewModelProvider(this).get(PlayerViewModel.class);
        playerViewModel.setLifecycleOwner(this);
    }

    @Override
    public void initListener() {
        Log.d("cwtsw", "设备信息 mDevProperty.mLed = " + playerViewModel.mDevProperty.mLed);
        getBinding().cbChangeStatus.setChecked(playerViewModel.mDevProperty.mLed);
//        playerViewModel.setISingleCallback((type, var2) -> {
//            if (type == Constant.CALLBACK_TYPE_PLAYER_UPDATE_PROPERTY) {
//                getBinding().cbChangeStatus.post(() -> {
//                    Log.d("cwtsw", "设备信息 mDevProperty.mLed = " + playerViewModel.mDevProperty.mLed);
//                    getBinding().cbChangeStatus.setChecked(playerViewModel.mDevProperty.mLed);
//                });
//            }
//        });
        getBinding().cbChangeStatus.setOnCheckedChangeListener((compoundButton, b) -> playerViewModel.setLedSwitch(b));
    }

    @Override
    public void requestData() {
//        playerViewModel.requestDeviceProperty();
    }

    @Override
    protected void onStop() {
        super.onStop();
        playerViewModel.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        playerViewModel.onStart();
    }
}
