package io.agora.iotlinkdemo.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ScreenUtils;
import io.agora.iotlinkdemo.databinding.DialogNeedPowerBinding;

/**
 * 电量不足对话框 对话框
 */
public class NoPowerDialog extends BaseDialog<DialogNeedPowerBinding> {
    public NoPowerDialog(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected DialogNeedPowerBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogNeedPowerBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        getBinding().btnHasKnow.setOnClickListener(view -> {
            dismiss();
        });
    }

    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ScreenUtils.dp2px(300),
                ScreenUtils.dp2px(176)
        );
        getWindow().getAttributes().gravity = Gravity.CENTER;
    }
}
