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
import io.agora.iotlinkdemo.dialog.EditNameDialog;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.models.device.DeviceViewModel;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import kotlin.jvm.JvmField;

/**
 * 添加成功/失败
 */
@Route(path = PagePathConstant.pageAddResult)
public class DeviceAddStep6ResultActivity extends BaseViewBindingActivity<ActivityAddDeviceResultBinding> {
    /**
     * 设备模块统一ViewModel
     */
    private DeviceViewModel deviceViewModel;

    /**
     * 是否添加成功
     */
    @JvmField
    @Autowired(name = Constant.IS_SUCCESS)
    boolean isSuccess = false;

    /**
     * 修改名称
     */
    private EditNameDialog editNameDialog;

    @Override
    protected ActivityAddDeviceResultBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityAddDeviceResultBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        ARouter.getInstance().inject(this);
        deviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        deviceViewModel.setLifecycleOwner(this);
        if (isSuccess) {
            getBinding().titleView.setTitle(getString(R.string.add_success));
            getBinding().titleView.setRightText(getString(R.string.finish));
            getBinding().tvAddStatus.setText(getString(R.string.add_success));
            getBinding().titleView.setRightIconClick(view -> mHealthActivityManager.popActivity());
        } else {
            getBinding().titleView.setTitle(getString(R.string.add_fail));
            getBinding().tvAddStatus.setTextColor(
                    ContextCompat.getColor(
                            this,
                            R.color.red_e0
                    )
            );
            getBinding().ivAddStatus.setImageResource(R.mipmap.fail);
            getBinding().tvAddStatus.setText(getString(R.string.add_fail));
            getBinding().ivEditName.setVisibility(View.GONE);
            getBinding().tvTips1.setVisibility(View.VISIBLE);
            getBinding().tvTips2.setVisibility(View.VISIBLE);
            getBinding().tvContactCustomerService.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void initListener() {
        deviceViewModel.setISingleCallback((var1, var2) -> {
            if (var1 == Constant.CALLBACK_TYPE_EXIT_STEP) {
                mHealthActivityManager.finishActivityByClass("DeviceAddStep6ResultActivity");
            } else if (var1 == Constant.CALLBACK_TYPE_DEVICE_EDIT_NAME_SUCCESS) {
                ToastUtils.INSTANCE.showToast("修改成功");
                getBinding().tvDeviceName.post(() -> {
                    getBinding().tvDeviceName.setText((String) var2);
                });
            } else if (var1 == Constant.CALLBACK_TYPE_DEVICE_EDIT_NAME_FAIL) {
                ToastUtils.INSTANCE.showToast("修改失败");
            }
        });
        getBinding().ivEditName.setOnClickListener(view -> showEditNameDialog());
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
                        deviceViewModel.editDeviceName(deviceViewModel.getNewDevice(), (String) o);
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
