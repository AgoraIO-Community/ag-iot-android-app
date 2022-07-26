package io.agora.iotlinkdemo.models.device.add;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.listener.ISingleCallback;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.common.GlideApp;
import io.agora.iotlinkdemo.databinding.ActivityDeviceAddingBinding;
import io.agora.iotlinkdemo.event.ResetAddDeviceEvent;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.device.DeviceViewModel;
import com.alibaba.android.arouter.facade.annotation.Route;

import org.greenrobot.eventbus.EventBus;

/**
 * 正在添加设备
 * <p>
 * 添加设备第五步
 */
@Route(path = PagePathConstant.pageDeviceAdding)
public class DeviceAddStep5AddingActivity extends BaseViewBindingActivity<ActivityDeviceAddingBinding> {
    /**
     * 设备模块统一ViewModel
     */
    private DeviceViewModel deviceViewModel;

    @Override
    protected ActivityDeviceAddingBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDeviceAddingBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        deviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        deviceViewModel.setLifecycleOwner(this);
        GlideApp.with(this).asGif().load(R.mipmap.loading).into(getBinding().progressAdding);
        deviceViewModel.initHandler();
    }

    @Override
    public void initListener() {
        deviceViewModel.setISingleCallback((var1, var2) -> {
            if (var1 == Constant.CALLBACK_TYPE_EXIT_STEP) {
                mHealthActivityManager.finishActivityByClass("DeviceAddStep5AddingActivity");
            } else if (var1 == Constant.CALLBACK_TYPE_DEVICE_ADD_SUCCESS) {
                //成功添加
                PagePilotManager.pageAddResult(true);
                exitActivity();
                EventBus.getDefault().post(new ResetAddDeviceEvent());
            } else if (var1 == Constant.CALLBACK_TYPE_DEVICE_ADD_FAIL) {
                //超时
                PagePilotManager.pageWifiTimeOut();
                exitActivity();
                EventBus.getDefault().post(new ResetAddDeviceEvent());
            }
        });
        getBinding().titleView.setRightIconClick(view -> mHealthActivityManager.popActivity());
        getBinding().btnFail.setOnClickListener(view -> {
            PagePilotManager.pageAddResult(false);
            exitActivity();
            EventBus.getDefault().post(new ResetAddDeviceEvent());
        });
        getBinding().btnSuccess.setOnClickListener(view -> {
            PagePilotManager.pageAddResult(true);
            exitActivity();
            EventBus.getDefault().post(new ResetAddDeviceEvent());
        });
        getBinding().btnFailWifi.setOnClickListener(view -> {
            PagePilotManager.pageWifiTimeOut();
            exitActivity();
            EventBus.getDefault().post(new ResetAddDeviceEvent());
        });
    }

    private void exitActivity() {
        getBinding().btnFail.postDelayed(() -> mHealthActivityManager.finishActivityByClass("DeviceAddStep5AddingActivity"), 500);
    }

    @Override
    protected void onStart() {
        super.onStart();
        deviceViewModel.onStart();
        deviceViewModel.startTimer(getBinding().txTimeRun);
    }

    @Override
    protected void onStop() {
        super.onStop();
        deviceViewModel.stopTimer();
        deviceViewModel.onStop();
    }

}
