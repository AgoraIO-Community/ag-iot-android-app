package io.agora.iotlinkdemo.models.device.setting.mydevice;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.listener.ISingleCallback;
import com.agora.baselibrary.utils.GsonUtil;
import com.agora.baselibrary.utils.StringUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.common.GlideApp;
import io.agora.iotlinkdemo.databinding.ActivityDeviceShareToUserDetailBinding;
import io.agora.iotlinkdemo.dialog.CommonDialog;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.models.device.DeviceViewModel;
import io.agora.iotlink.IotOutSharer;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import kotlin.jvm.JvmField;

/**
 * 设备共享出去的用户详情
 */
@Route(path = PagePathConstant.pageDeviceShareToUserDetail)
public class DeviceShareUserDetailActivity extends BaseViewBindingActivity<ActivityDeviceShareToUserDetailBinding> {

    @JvmField
    @Autowired(name = Constant.OBJECT)
    String iotOutSharerStr;

    private IotOutSharer iotOutSharer;
    /**
     * 设备模块统一ViewModel
     */
    private DeviceViewModel deviceViewModel;

    @Override
    protected ActivityDeviceShareToUserDetailBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDeviceShareToUserDetailBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        ARouter.getInstance().inject(this);
        deviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        deviceViewModel.setLifecycleOwner(this);
        iotOutSharer = GsonUtil.Companion.getInstance().fromJson(iotOutSharerStr, IotOutSharer.class);
        GlideApp.with(this).load(iotOutSharer.mAvatar).error(R.mipmap.userimage).into(getBinding().ivUserAvatar);
        if (!TextUtils.isEmpty(iotOutSharer.mPhone)) {
            getBinding().tvPhone.setText(StringUtils.INSTANCE.formatAccount(iotOutSharer.mPhone));
        } else {
            getBinding().tvPhone.setText(iotOutSharer.mEmail);
        }
    }

    @Override
    public void initListener() {
        deviceViewModel.setISingleCallback((type, data) -> {
            if (type == Constant.CALLBACK_TYPE_DEVICE_SHARE_CANCEL_SUCCESS) {
                mHealthActivityManager.popActivity();
            }
        });
        getBinding().btnCancelShare.setOnClickListener(view -> {
            showCommonDialog();
        });
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

    public void showCommonDialog() {
        if (commonDialog == null) {
            commonDialog = new CommonDialog(this);
            commonDialog.setDialogTitle("确认取消共享？");
            commonDialog.setDialogBtnText(getString(R.string.cancel), getString(R.string.confirm));
            commonDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {

                }

                @Override
                public void onRightButtonClick() {
                    deviceViewModel.deshareDevice(iotOutSharer);
                }
            });
        }
        commonDialog.show();
    }
}
