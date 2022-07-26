package io.agora.iotlinkdemo.models.device.setting.mydevice;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.StringUtils;
import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityDeviceInfoSettingBinding;
import io.agora.iotlinkdemo.dialog.EditNameDialog;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.models.device.DeviceViewModel;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 设备信息设置
 */
@Route(path = PagePathConstant.pageDeviceInfoSetting)
public class DeviceInfoSettingActivity extends BaseViewBindingActivity<ActivityDeviceInfoSettingBinding> {
    /**
     * 修改名称
     */
    private EditNameDialog editNameDialog;
    /**
     * 设备模块统一ViewModel
     */
    private DeviceViewModel deviceViewModel;

    @Override
    protected ActivityDeviceInfoSettingBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDeviceInfoSettingBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        deviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        deviceViewModel.setLifecycleOwner(this);
    }

    @Override
    public void requestData() {
        getBinding().tvDeviceName.setText(AgoraApplication.getInstance().getLivingDevice().mDeviceName);
        getBinding().tvDeviceIDValue.setText(AgoraApplication.getInstance().getLivingDevice().mDeviceNumber);
    }

    @Override
    public void initListener() {
        deviceViewModel.setISingleCallback((var1, var2) -> {
            getBinding().tvDeviceName.post(new Runnable() {
                @Override
                public void run() {
                    if (var1 == Constant.CALLBACK_TYPE_DEVICE_EDIT_NAME_SUCCESS) {
                        ToastUtils.INSTANCE.showToast("修改成功");
                        AgoraApplication.getInstance().getLivingDevice().mDeviceName = (String) var2;
                        getBinding().tvDeviceName.setText(StringUtils.INSTANCE.getBase64String(AgoraApplication.getInstance().getLivingDevice().mDeviceName));
                    } else if (var1 == Constant.CALLBACK_TYPE_DEVICE_EDIT_NAME_FAIL) {
                        ToastUtils.INSTANCE.showToast("修改失败");
                    }
                }
            });
        });
        getBinding().tvDeviceName.setOnClickListener(view -> {
            showEditNameDialog();
        });
    }

    private void showEditNameDialog() {
        if (editNameDialog == null) {
            editNameDialog = new EditNameDialog(this);
            editNameDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {

                }

                @Override
                public void onRightButtonClick() {
                }
            });
            editNameDialog.iSingleCallback = (integer, o) -> {
                if (integer == 0) {
                    if (o instanceof String) {
                        //修改名称
                        deviceViewModel.editDeviceName(AgoraApplication.getInstance().getLivingDevice(), (String) o);
                    }
                }
            };
        }
        editNameDialog.show();
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
