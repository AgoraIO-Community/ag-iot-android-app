package com.agora.iotlink.models.device.add;


import static com.huawei.hms.hmsscankit.RemoteView.REQUEST_CODE_PHOTO;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.agora.baselibrary.utils.GsonUtil;
import com.agora.baselibrary.utils.SPUtil;
import com.agora.baselibrary.utils.ToastUtils;
import com.agora.iotlink.R;
import com.agora.iotlink.api.bean.QRBean;
import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.base.PermissionHandler;
import com.agora.iotlink.base.PermissionItem;
import com.agora.iotlink.common.Constant;
import com.agora.iotlink.databinding.ActivityAddDeviceBinding;
import com.agora.iotlink.manager.PagePathConstant;
import com.agora.iotlink.manager.PagePilotManager;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.huawei.hms.hmsscankit.RemoteView;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

import java.io.IOException;

/**
 * 描码添加设备
 * <p>
 * 添加设备第一步
 */
@Route(path = PagePathConstant.pageDeviceAddScanning)
public class DeviceAddStep1ScanningActivity extends BaseViewBindingActivity<ActivityAddDeviceBinding>
    implements PermissionHandler.ICallback  {
    private static final String TAG = "LINK/DevAddStep1Act";

    private RemoteView remoteView;
    private PermissionHandler mPermHandler;             ///< 权限申请处理

    @Override
    protected ActivityAddDeviceBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityAddDeviceBinding.inflate(inflater);
    }

    private boolean isLight = false;

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        getBinding().btnNextStep.setOnClickListener(view -> PagePilotManager.pageResetDevice());
        getBinding().titleView.setRightIconClick(view -> mHealthActivityManager.popActivity());
        remoteView = new RemoteView.Builder().setContext(this).setFormat(HmsScan.ALL_SCAN_TYPE).build();
        remoteView.setOnResultCallback(this::goNextStep);
        remoteView.onCreate(savedInstanceState);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        getBinding().qrLayout.addView(remoteView, params);
    }

    @Override
    public void initListener() {
        remoteView.setOnLightVisibleCallback(b -> isLight = b);
        getBinding().tvAlbum.setOnClickListener(view -> {
            onBtnGallery();
        });

        getBinding().cbLight.setOnCheckedChangeListener((compoundButton, b) -> {
            remoteView.switchLight();
        });
    }

    void onBtnGallery() {
        // requestAppPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        //
        // Gallery权限判断处理
        //
        int[] permIdArray = new int[1];
        permIdArray[0] = PermissionHandler.PERM_ID_READ_STORAGE;
        mPermHandler = new PermissionHandler(this, this, permIdArray);
        if (!mPermHandler.isAllPermissionGranted()) {
            Log.d(TAG, "<onBtnGallery> requesting permission...");
            mPermHandler.requestNextPermission();
        } else {
            Log.d(TAG, "<onBtnGallery> permission granted, openAlbum");
            openAlbum();
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
            openAlbum();
        } else {
            popupMessage(getString(R.string.no_permission));
        }
    }

    private void openAlbum() {
        Intent intentToPickPic = new Intent(Intent.ACTION_PICK, null);
        intentToPickPic.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intentToPickPic, REQUEST_CODE_PHOTO);
    }

    /**
     * Handle the return results from the album.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_PHOTO) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                HmsScan[] hmsScans = ScanUtil.decodeWithBitmap(DeviceAddStep1ScanningActivity.this, bitmap,
                        new HmsScanAnalyzerOptions.Creator().setPhotoMode(true).create());

                goNextStep(hmsScans);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void goNextStep(HmsScan[] hmsScans) {
        Log.d("cwtsw", "goNextStep hmsScans = " + hmsScans);
        try {
            if (hmsScans != null && hmsScans.length > 0 && hmsScans[0] != null && !TextUtils.isEmpty(hmsScans[0].getOriginalValue())) {
                QRBean qrBean = GsonUtil.Companion.getInstance().fromJson(hmsScans[0].showResult, QRBean.class);
                if (TextUtils.isEmpty(qrBean.c) || TextUtils.isEmpty(qrBean.k)) {
                    ToastUtils.INSTANCE.showToast("二维码不正确");
                    return;
                }
                SPUtil.Companion.getInstance(this).putString(Constant.FROM_QR_C, qrBean.c);
                SPUtil.Companion.getInstance(this).putString(Constant.FROM_QR_K, qrBean.k);
                PagePilotManager.pageResetDevice();
            } else {
                ToastUtils.INSTANCE.showToast("二维码不正确");
            }
        } catch (Exception e) {
            if (hmsScans != null && hmsScans.length > 0 && hmsScans[0] != null) {
                ToastUtils.INSTANCE.showToast("二维码不正确 " + hmsScans[0].showResult);
            } else {
                ToastUtils.INSTANCE.showToast("二维码不正确");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        remoteView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        remoteView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        remoteView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        remoteView.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        remoteView.onStop();
        getBinding().cbLight.setChecked(false);
    }
}
