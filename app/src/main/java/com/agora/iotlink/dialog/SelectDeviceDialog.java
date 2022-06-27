package com.agora.iotlink.dialog;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ScreenUtils;
import com.agora.iotlink.R;
import com.agora.iotlink.databinding.DialogSelectLegibilityBinding;

/**
 * 选择 设备 对话框
 */
public class SelectDeviceDialog extends BaseDialog<DialogSelectLegibilityBinding> {
    public SelectDeviceDialog(@NonNull Context context) {
        super(context);
    }

    private Drawable hasCheckDrawable;

    @NonNull
    @Override
    protected DialogSelectLegibilityBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogSelectLegibilityBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        hasCheckDrawable = ContextCompat.getDrawable(getContext(), R.mipmap.albumselected);
        hasCheckDrawable.setBounds(0, 0, hasCheckDrawable.getMinimumWidth(), hasCheckDrawable.getMinimumHeight());
        setCanceledOnTouchOutside(true);
        getWindow().setWindowAnimations(R.style.popup_window_style_bottom);
        getBinding().btnHD.setOnClickListener(view -> {
            //高清
            getBinding().btnHD.setCompoundDrawables(null, null, hasCheckDrawable, null);
            getBinding().btnSD.setCompoundDrawables(null, null, null, null);
            getOnButtonClickListener().onLeftButtonClick();
            dismiss();
        });
        getBinding().btnSD.setOnClickListener(view -> {
            //标清
            getBinding().btnHD.setCompoundDrawables(null, null, null, null);
            getBinding().btnSD.setCompoundDrawables(null, null, hasCheckDrawable, null);
            getOnButtonClickListener().onRightButtonClick();
            dismiss();
        });
    }

    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ScreenUtils.dp2px(176)
        );
        getWindow().getAttributes().gravity = Gravity.BOTTOM;
    }
}
