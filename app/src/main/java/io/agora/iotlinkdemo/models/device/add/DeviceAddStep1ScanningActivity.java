package io.agora.iotlinkdemo.models.device.add;



import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.agora.baselibrary.utils.GsonUtil;
import com.agora.baselibrary.utils.SPUtil;
import com.agora.baselibrary.utils.ToastUtils;

import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.api.bean.QRBean;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.base.PermissionHandler;
import io.agora.iotlinkdemo.base.PermissionItem;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityAddDeviceBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.utils.ZXingUtils;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;


/**
 * 描码添加设备
 * <p>
 * 添加设备第一步
 */
@Route(path = PagePathConstant.pageDeviceAddScanning)
public class DeviceAddStep1ScanningActivity extends BaseViewBindingActivity<ActivityAddDeviceBinding>
    implements PermissionHandler.ICallback, CameraPreview.ICameraScanCallback  {
    private static final String TAG = "LINK/DevAddStep1Act";
    private static final int REQUEST_CODE_PHOTO = 1000;

    private PermissionHandler mPermHandler;             ///< 权限申请处理
    private volatile boolean mQRCodeChecking = false;   ///< 是否正在检测二维码正确性

    @Override
    protected ActivityAddDeviceBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityAddDeviceBinding.inflate(inflater);
    }



    @Override
    public void onQRCodeParsed(final String textQRCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mQRCodeChecking) {
                    return;
                }
                mQRCodeChecking = true;
                goNextStep(textQRCode);
                mQRCodeChecking = false;
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "<onCreate>");
        super.onCreate(savedInstanceState);

        CameraPreview camView = getBinding().camView;
        int camDisplayWidth, camDisplayHeight;
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        float wndDisplayRatio = (float)(metric.heightPixels) / (float)(metric.widthPixels);
        if (wndDisplayRatio > 1.40f) {      // 按照16:9计算
            camDisplayWidth = metric.widthPixels;
            camDisplayHeight = camDisplayWidth * 16 / 9;
        } else {    // 按照 4:3 计算
            camDisplayWidth = metric.widthPixels;
            camDisplayHeight = camDisplayWidth * 4 / 3;
        }

        camView.querySupportedCamera();  // query supported camera
        Size widgetSize = camView.calculateMatchedSize(camDisplayWidth, camDisplayHeight);

        ConstraintLayout.LayoutParams camLayoutParam = (ConstraintLayout.LayoutParams)camView.getLayoutParams();
        camLayoutParam.width = widgetSize.getHeight();      // 竖屏要旋转宽高
        camLayoutParam.height = widgetSize.getWidth();
        camView.setLayoutParams(camLayoutParam);
        camView.setPreviewSize(widgetSize);
        camView.setScanCallback(this);

        Log.d(TAG, "<onCreate> done, camDisplay={" + camDisplayWidth + ", " + camDisplayHeight + "}"
                + ", widgetSize={" + camLayoutParam.width + ", " + camLayoutParam.height + "}");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "<onDestroy>");
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "<onStart>");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "<onStop>");
        super.onStop();
        getBinding().camView.scaningStop();
        getBinding().cbLight.setChecked(false);
    }


    @Override
    protected void onResume() {
        Log.d(TAG, "<onResume>");
        super.onResume();
        getBinding().camView.scaningStart();
        boolean isOpened = getBinding().camView.isTorchOpened();
        getBinding().cbLight.setChecked(isOpened);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "<onPause>");
        super.onPause();
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        getBinding().btnNextStep.setOnClickListener(view -> PagePilotManager.pageResetDevice());
        getBinding().titleView.setRightIconClick(view -> mHealthActivityManager.popActivity());
    }

    @Override
    public void initListener() {

        getBinding().tvAlbum.setOnClickListener(view -> {
            onBtnGallery();
        });

        getBinding().cbLight.setOnClickListener(view -> {
            boolean opened = getBinding().camView.isTorchOpened();
            boolean bRet = getBinding().camView.turnTorch(!opened);
            boolean isOpened = getBinding().camView.isTorchOpened();
            getBinding().cbLight.setChecked(isOpened);
        });
    }

    void onBtnGallery() {
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
                String textQRCode = ZXingUtils.parseQRCodeByBmp(bitmap);
                if (textQRCode == null) {
                    ToastUtils.INSTANCE.showToast("二维码不正确");
                    return;
                }
                goNextStep(textQRCode);
                return;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void goNextStep(final String textQRCode) {
        Log.d(TAG, "<goNextStep> textQRCode=" + textQRCode);
        try {
            QRBean qrBean = GsonUtil.Companion.getInstance().fromJson(textQRCode, QRBean.class);
            if (TextUtils.isEmpty(qrBean.c) || TextUtils.isEmpty(qrBean.k)) {
                ToastUtils.INSTANCE.showToast("二维码不正确");
                return;
            }

            getBinding().camView.scaningStop();
            SPUtil.Companion.getInstance(this).putString(Constant.FROM_QR_C, qrBean.c);
            SPUtil.Companion.getInstance(this).putString(Constant.FROM_QR_K, qrBean.k);
            PagePilotManager.pageResetDevice();
            return;

        } catch (JsonSyntaxException jsonExp) {
            jsonExp.printStackTrace();
            Log.d(TAG, "<goNextStep> [EXCEPTION] jsonExp=" + jsonExp);
            ToastUtils.INSTANCE.showToast("二维码不正确");
        }
    }





}
