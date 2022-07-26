package io.agora.iotlinkdemo.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.listener.ISingleCallback;
import com.agora.baselibrary.utils.ScreenUtils;
import io.agora.iotlinkdemo.databinding.DialogEditDeviceNameBinding;

public class EditNameDialog extends BaseDialog<DialogEditDeviceNameBinding> {
    public EditNameDialog(@NonNull Context context) {
        super(context);
    }

    public ISingleCallback<Integer, Object> iSingleCallback;

    @NonNull
    @Override
    protected DialogEditDeviceNameBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogEditDeviceNameBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        getBinding().btnCancel.setOnClickListener(view -> {
            getOnButtonClickListener().onLeftButtonClick();
            dismiss();
        });
        getBinding().btnDefine.setOnClickListener(view -> {
            iSingleCallback.onSingleCallback(0, getBinding().etDeviceName.getText().toString());
            getOnButtonClickListener().onRightButtonClick();
            dismiss();
        });
    }

    /**
     * 设置title
     */
    public void setDialogTitle(String title) {
        getBinding().tvTitle.setText(title);
    }

    /**
     * 输入提示
     */
    public void setDialogInputHint(String title) {
        getBinding().tvTitle.setText(title);
        getBinding().etDeviceName.setHint(title);
    }

    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ScreenUtils.dp2px(300),
                ScreenUtils.dp2px(227)
        );
        getWindow().getAttributes().gravity = Gravity.CENTER;
    }
}
