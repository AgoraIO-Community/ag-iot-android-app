package io.agora.iotlinkdemo.models.device.add;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityAddDeviceResultBinding;
import io.agora.iotlinkdemo.databinding.ActivityDeviceBtCfgResultBinding;
import io.agora.iotlinkdemo.dialog.EditNameDialog;
import io.agora.iotlinkdemo.event.ResetAddDeviceEvent;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.models.device.DeviceViewModel;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import org.greenrobot.eventbus.EventBus;

import kotlin.jvm.JvmField;

/**
 * 添加成功/失败
 */
@Route(path = PagePathConstant.pageDeviceBtCfgRslt)
public class DeviceAddStepBtCfgResultActivity extends BaseViewBindingActivity<ActivityDeviceBtCfgResultBinding> {


    @Override
    protected ActivityDeviceBtCfgResultBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDeviceBtCfgResultBinding.inflate(inflater);
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
