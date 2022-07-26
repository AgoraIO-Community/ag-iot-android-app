package io.agora.iotlinkdemo.models.device.setting.mydevice;

import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.databinding.ActivityDeviceFirmwareUpgradeBinding;
import io.agora.iotlinkdemo.dialog.CheckUpdateDialog;
import io.agora.iotlinkdemo.dialog.ImportantTipsDialog;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 固件升级
 */
@Route(path = PagePathConstant.pageDeviceFirmwareUpgrade)
public class DeviceFirmwareUpgradeActivity extends BaseViewBindingActivity<ActivityDeviceFirmwareUpgradeBinding> {
    private CheckUpdateDialog checkUpdateDialog;
    private ImportantTipsDialog importantTipsDialog;

    @Override
    protected ActivityDeviceFirmwareUpgradeBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDeviceFirmwareUpgradeBinding.inflate(inflater);
    }

    @Override
    public void requestData() {
    }

    @Override
    public void initListener() {
        getBinding().btnCheckUpdate.setOnClickListener(view -> {
            ToastUtils.INSTANCE.showToast(getString(R.string.function_not_open));
//            showCheckUpdateDialog();
        });
    }

    private void showCheckUpdateDialog() {
        if (checkUpdateDialog == null) {
            checkUpdateDialog = new CheckUpdateDialog(this);
            checkUpdateDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {

                }

                @Override
                public void onRightButtonClick() {
                    showImportantTipsDialog();
                }
            });
        }
        checkUpdateDialog.show();
    }


    private void showImportantTipsDialog() {
        if (importantTipsDialog == null) {
            importantTipsDialog = new ImportantTipsDialog(this);
            importantTipsDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {

                }

                @Override
                public void onRightButtonClick() {
                    getBinding().tvTips1.setVisibility(View.VISIBLE);
                    getBinding().tvTips2.setVisibility(View.GONE);
                    getBinding().btnCheckUpdate.setEnabled(false);
                }
            });
        }
        checkUpdateDialog.show();
    }
}
