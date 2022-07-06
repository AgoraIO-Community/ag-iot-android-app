package com.agora.iotlink.models.device.add;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.iotlink.R;
import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.base.PermissionHandler;
import com.agora.iotlink.base.PermissionItem;
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
public class DeviceAddStep2ResetActivity extends BaseViewBindingActivity<ActivityResetDeviceBinding>
    implements PermissionHandler.ICallback  {

    private static final String TAG = "LINK/DevAddStep2Act";

    /**
     * 设备模块统一ViewModel
     */
    private DeviceViewModel deviceViewModel;
    private PermissionHandler mPermHandler;             ///< 权限申请处理

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
            onBtnNext();
        });
    }

    void onBtnNext() {
        //
        // ACCESS_FINE_LOCATION 权限判断处理
        //
        int[] permIdArray = new int[1];
        permIdArray[0] = PermissionHandler.PERM_ID_FINELOCAL;
        mPermHandler = new PermissionHandler(this, this, permIdArray);
        if (!mPermHandler.isAllPermissionGranted()) {
            Log.d(TAG, "<onBtnNext> requesting permission...");
            mPermHandler.requestNextPermission();

        } else {
            Log.d(TAG, "<onBtnNext> permission granted, goto WIFI activity");
            PagePilotManager.pageSetDeviceWifi();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "<onRequestPermissionsResult> requestCode=" + requestCode);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mPermHandler != null) {
            mPermHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onAllPermisonReqDone(boolean allGranted, final PermissionItem[] permItems) {
        Log.d(TAG, "<onAllPermisonReqDone> allGranted = " + allGranted);

        if (allGranted) {
            PagePilotManager.pageSetDeviceWifi();
        } else {
            popupMessage(getString(R.string.no_permission));
        }
    }
}
