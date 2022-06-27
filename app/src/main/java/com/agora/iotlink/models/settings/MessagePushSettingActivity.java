package com.agora.iotlink.models.settings;

import android.view.LayoutInflater;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;

import com.agora.baselibrary.utils.ToastUtils;
import com.agora.iotlink.R;
import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.databinding.ActivityMessagePushBinding;
import com.agora.iotlink.manager.PagePathConstant;
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
