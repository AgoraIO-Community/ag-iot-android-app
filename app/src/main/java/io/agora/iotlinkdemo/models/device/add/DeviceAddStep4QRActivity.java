package io.agora.iotlinkdemo.models.device.add;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.utils.SPUtil;
import com.agora.baselibrary.utils.ScreenUtils;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityDeviceQrScannerBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.device.DeviceViewModel;
import io.agora.iotlinkdemo.utils.ZXingUtils;
import com.alibaba.android.arouter.facade.annotation.Route;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 设备扫码
 * <p>
 * 添加设备第四步
 */
@Route(path = PagePathConstant.pageDeviceQR)
public class DeviceAddStep4QRActivity extends BaseViewBindingActivity<ActivityDeviceQrScannerBinding> {
    /**
     * 设备模块统一ViewModel
     */
    private DeviceViewModel deviceViewModel;

    @Override
    protected ActivityDeviceQrScannerBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDeviceQrScannerBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        deviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        deviceViewModel.setLifecycleOwner(this);
        deviceViewModel.setISingleCallback((var1, var2) -> {
            if (var1 == Constant.CALLBACK_TYPE_EXIT_STEP) {
                mHealthActivityManager.finishActivityByClass("DeviceAddStep4QRActivity");
            }
        });
    }

    @Override
    public void requestData() {
        String wifi_ssid = SPUtil.Companion.getInstance(this).getString(Constant.WIFI_NAME, "");
        String wifi_pws = SPUtil.Companion.getInstance(this).getString(Constant.WIFI_PWD, "");
        String k = SPUtil.Companion.getInstance(this).getString(Constant.FROM_QR_K, "");
        String userId = deviceViewModel.getUserId();
        //构建二维码内容，json格式
        JSONObject body = new JSONObject();
        try {
            body.put("s", wifi_ssid);
            body.put("p", wifi_pws);
            body.put("u", userId);
            body.put("k", k);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("CWTSW", "Cannot create QRcode !");
        }
        String qrCode_content = String.valueOf(body);
        Bitmap bitmap = ZXingUtils.createQRImage(qrCode_content, ScreenUtils.dp2px(310), ScreenUtils.dp2px(310));
        getBinding().ivQR.setImageBitmap(bitmap);
    }

    @Override
    public void initListener() {
        getBinding().titleView.setRightIconClick(view -> mHealthActivityManager.popActivity());
        getBinding().btnNextStep.setOnClickListener(view -> {
            PagePilotManager.pageDeviceAdding();
        });
    }
}
