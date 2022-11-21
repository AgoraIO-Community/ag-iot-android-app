package io.agora.iotlinkdemo.dialog;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.listener.ISingleCallback;
import com.agora.baselibrary.utils.ScreenUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.databinding.DialogPirDetectionBinding;
import io.agora.iotlinkdemo.databinding.DialogSelectDevcfgModeBinding;

/**
 * @brief 选择设备配网模式 对话框
 */
public class SelectDevCfgModeDialog extends BaseDialog<DialogSelectDevcfgModeBinding> {
    public SelectDevCfgModeDialog(@NonNull Context context) {
        super(context);
    }

    private Drawable hasCheckDrawable;

    public ISingleCallback<Integer, Object> iSingleCallback;

    @NonNull
    @Override
    protected DialogSelectDevcfgModeBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogSelectDevcfgModeBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        setCanceledOnTouchOutside(true);
        getWindow().setWindowAnimations(R.style.popup_window_style_bottom);
        getBinding().tvCfgModeCamera.setOnClickListener(view -> {
            iSingleCallback.onSingleCallback(0, null);
            dismiss();
        });
        getBinding().tvCfgModeBluetooth.setOnClickListener(view -> {
            iSingleCallback.onSingleCallback(1, null);
            dismiss();
        });
    }

    public void setSelect(int type) {
        if (type == 0) {
            getBinding().tvCfgModeCamera.performClick();
        } else if (type == 1) {
            getBinding().tvCfgModeBluetooth.performClick();
        }
    }

    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ScreenUtils.dp2px(336)
        );
        getWindow().getAttributes().gravity = Gravity.BOTTOM;
    }
}
