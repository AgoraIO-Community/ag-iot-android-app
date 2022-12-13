package io.agora.iotlinkdemo.models.device.add;

import android.content.Intent;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModelProvider;

import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.base.PermissionHandler;
import io.agora.iotlinkdemo.base.PermissionItem;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityResetDeviceBinding;
import io.agora.iotlinkdemo.dialog.CommonDialog;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.device.DeviceViewModel;
import io.agora.iotlinkdemo.utils.ZXingUtils;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ToastUtils;
import com.alibaba.android.arouter.facade.annotation.Route;

import java.io.IOException;

/**
 * 重置设备
 * <p>
 * 添加设备第二步
 */
@Route(path = PagePathConstant.pageResetDevice)
public class DeviceAddStep2ResetActivity extends BaseViewBindingActivity<ActivityResetDeviceBinding>
    implements PermissionHandler.ICallback  {

    private static final String TAG = "LINK/DevAddStep2Act";
    private static final int REQUEST_CODE_LOCATION_SRV = 1001;

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
        boolean enableLocation = isLocationProviderEnabled();
        if (!enableLocation) {
            showRequestLocationDialog();
            return;
        }

        requestAFLPermission();
    }

    /**
     * @brief ACCESS_FINE_LOCATION 权限判断处理
     */
    void requestAFLPermission() {
        int[] permIdArray = new int[1];
        permIdArray[0] = PermissionHandler.PERM_ID_FINELOCAL;
        mPermHandler = new PermissionHandler(this, this, permIdArray);
        if (!mPermHandler.isAllPermissionGranted()) {
            Log.d(TAG, "<requestAFLPermission> requesting permission...");
            mPermHandler.requestNextPermission();

        } else {
            Log.d(TAG, "<requestAFLPermission> permission granted, goto WIFI activity");
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



    /**
     * @brief 判断是否开启了GPS或网络定位开关
     */
    public boolean isLocationProviderEnabled() {
        boolean result = false;
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            result = true;
        }
        return result;
    }


    /**
     * @brief 显示跳转到 定位服务-->位置信息 系统设置的提示
     */
    public void showRequestLocationDialog() {
        Log.d(TAG, "<showRequestLocationDialog> request FloatWnd permission");
        if (commonDialog == null) {
            commonDialog = new CommonDialog(this);
            commonDialog.setDialogTitle("请给软件设置定位服务位置信息权限，否则不能获取WIFI信息");
            commonDialog.setDialogBtnText(getString(R.string.cancel), getString(R.string.confirm));
            commonDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {
                    Log.d(TAG, "<showRequestLocationDialog> onLeftButtonClick");
                }

                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onRightButtonClick() {
                    Log.d(TAG, "<showRequestLocationDialog> onRightButtonClick");

                    // 跳转到系统界面
                    Intent intentLocalSrv = new Intent();
                    intentLocalSrv.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intentLocalSrv, REQUEST_CODE_LOCATION_SRV);
                }
            });
        }
        commonDialog.show();
    }

    /**
     * Handle the return results from the album.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "<onActivityResult> requestCode=" + requestCode
                + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_CODE_LOCATION_SRV) {
            boolean enableLocation = isLocationProviderEnabled();
            if (enableLocation) {   // 已经打开 "位置信息"
                requestAFLPermission();
            }
        }
    }

}
