package io.agora.iotlinkdemo.models.settings;

import android.view.LayoutInflater;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;

import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.databinding.ActivityMessagePushBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 消息推送设置
 */
@Route(path = PagePathConstant.pageMessagePushSetting)
public class MessagePushSettingActivity extends BaseViewBindingActivity<ActivityMessagePushBinding> {

    @Override
    protected ActivityMessagePushBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityMessagePushBinding.inflate(inflater);
    }

    @Override
    public void initListener() {
        getBinding().tvAlarmMessage.setOnCheckedChangeListener((compoundButton, b) -> {
            ToastUtils.INSTANCE.showToast(R.string.function_not_open);
            compoundButton.setChecked(false);
        });
        getBinding().tvNotificationMessage.setOnCheckedChangeListener((compoundButton, b) -> {
            ToastUtils.INSTANCE.showToast(R.string.function_not_open);
            compoundButton.setChecked(false);
        });
    }
}
