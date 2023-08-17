package io.agora.falcondemo.dialog;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.listener.ISingleCallback;
import com.agora.baselibrary.utils.ScreenUtils;

import io.agora.falcondemo.databinding.DialogAddDeviceBinding;


public class DialogNewDevice extends BaseDialog<DialogAddDeviceBinding> {
    public DialogNewDevice(@NonNull Context context) {
        super(context);
    }

    public ISingleCallback<Integer, Object>  mSingleCallback;

    @NonNull
    @Override
    protected DialogAddDeviceBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogAddDeviceBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        getBinding().btnCancel.setOnClickListener(view -> {
            getOnButtonClickListener().onLeftButtonClick();
            dismiss();
        });
        getBinding().btnDefine.setOnClickListener(view -> {
            String nodeId = getBinding().etNodeId.getText().toString();
            if (TextUtils.isEmpty(nodeId)) {
                return;
            }

            mSingleCallback.onSingleCallback(0, nodeId);

            getOnButtonClickListener().onRightButtonClick();
            dismiss();
        });
    }


    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ScreenUtils.dp2px(300),
                ScreenUtils.dp2px(230)
        );
        getWindow().getAttributes().gravity = Gravity.CENTER;
    }
}
