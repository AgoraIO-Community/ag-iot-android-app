package com.agora.iotlink.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ScreenUtils;
import com.agora.iotlink.databinding.DialogCheckUpdateBinding;

public class CheckUpdateDialog extends BaseDialog<DialogCheckUpdateBinding> {
    public CheckUpdateDialog(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected DialogCheckUpdateBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogCheckUpdateBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        getBinding().btnCancel.setOnClickListener (view->{
            getOnButtonClickListener().onLeftButtonClick();
            dismiss();
        });
        getBinding().btnUpdate.setOnClickListener (view->{
            getOnButtonClickListener().onRightButtonClick();
            dismiss();
        });
    }
    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ScreenUtils.dp2px(300),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        getWindow().getAttributes().gravity = Gravity.BOTTOM;
    }
}
