package io.agora.iotlinkdemo.models.device.setting.mydevice;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.listener.ISingleCallback;
import com.agora.baselibrary.utils.StringUtils;
import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.api.bean.CountryBean;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityDeviceAddUserBinding;
import io.agora.iotlinkdemo.dialog.EditNameDialog;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.device.DeviceViewModel;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 添加共享给其他用户
 *
 */
@Route(path = PagePathConstant.pageDeviceShareToUserAdd)
public class DeviceShareAddUserActivity extends BaseViewBindingActivity<ActivityDeviceAddUserBinding> {
    /**
     * 设备模块统一ViewModel
     */
    private DeviceViewModel deviceViewModel;
    private EditNameDialog editNameDialog;
    /**
     * 当前选择的国家
     */
    private CountryBean countryBean;

    @Override
    protected ActivityDeviceAddUserBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDeviceAddUserBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        deviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        deviceViewModel.setLifecycleOwner(this);
        getBinding().tvDeviceName.setText(AgoraApplication.getInstance().getLivingDevice().mDeviceName);

    }

    @Override
    public void initListener() {
        getBinding().tvCountry.setOnClickListener(view -> {
            PagePilotManager.pageSelectCountry(this);
        });
        getBinding().tvAccount.setOnClickListener(view -> showEditNameDialog());
        getBinding().btnFinish.setOnClickListener(view -> {
            showLoadingView();
            deviceViewModel.shareDevice(AgoraApplication.getInstance().getLivingDevice(),
                getBinding().tvAccountValue.getText().toString(), 2, false);
        });

        deviceViewModel.setISingleCallback((type, data) -> {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideLoadingView();

                    if (type == Constant.CALLBACK_TYPE_DEVICE_SHARE_TO_SUCCESS) {
                        String sharingAccount = (String)data;
                        ToastUtils.INSTANCE.showToast("设备分享成功，对方用户账号Id=" + sharingAccount);
                        mHealthActivityManager.popActivity();

                    } else if (type == Constant.CALLBACK_TYPE_DEVICE_SHARE_TO_FAIL) {
                        DeviceViewModel.ErrInfo errInfo = (DeviceViewModel.ErrInfo)data;
                        if (errInfo.mErrTips != null) {
                            ToastUtils.INSTANCE.showToast("设备分享失败, " + errInfo.mErrTips);
                        } else {
                            ToastUtils.INSTANCE.showToast("设备分享失败, 错误码=" + errInfo.mErrCode);
                        }
                    }
                }
            });
        });
    }

    public void showEditNameDialog() {
        if (editNameDialog == null) {
            editNameDialog = new EditNameDialog(this);
            editNameDialog.setDialogTitle(getString(R.string.account));
            editNameDialog.setDialogInputHint(getString(R.string.please_input_account));
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
                        String account = (String) o;
                        getBinding().tvAccountValue.setText(account);
                        if (!TextUtils.isEmpty(getBinding().tvAccountValue.getText().toString())) {
                            getBinding().btnFinish.setEnabled(true);
                        }
                    }
                }
            };
        }
        editNameDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 最新版本不能调整国家
//        if (requestCode == 100) {
//            if (resultCode == RESULT_OK) {
//                CountryBean countryBean = (CountryBean) data.getSerializableExtra(Constant.COUNTRY);
//                if (countryBean != null) {
//                    this.countryBean = countryBean;
//                    getBinding().tvCountryValue.setText(countryBean.countryName);
//                }
//            }
//        }
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
