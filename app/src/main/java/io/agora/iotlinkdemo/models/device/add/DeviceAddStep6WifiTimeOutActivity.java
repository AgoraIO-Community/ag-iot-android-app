package io.agora.iotlinkdemo.models.device.add;

import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.databinding.ActivityWifiTimeOutBinding;
import io.agora.iotlinkdemo.event.ResetAddDeviceEvent;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import com.alibaba.android.arouter.facade.annotation.Route;

import org.greenrobot.eventbus.EventBus;

/**
 * 配网失败
 * <p>
 * 添加设备第六步中的一种
 */
@Route(path = PagePathConstant.pageWifiTimeOut)
public class DeviceAddStep6WifiTimeOutActivity extends BaseViewBindingActivity<ActivityWifiTimeOutBinding> {

    @Override
    protected ActivityWifiTimeOutBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityWifiTimeOutBinding.inflate(inflater);
    }

    @Override
    public void initListener() {
        getBinding().titleView.setRightIconClick(view -> resetAddDevice());
        getBinding().btnNextStep.setOnClickListener(view -> resetAddDevice());
    }

    /**
     * 回到首页重新开始流程
     */
    private void resetAddDevice() {
        EventBus.getDefault().post(new ResetAddDeviceEvent());
        mHealthActivityManager.popActivity();
    }
}
