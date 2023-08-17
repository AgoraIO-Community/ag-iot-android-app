package io.agora.falcondemo.dialog;

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
import io.agora.falcondemo.R;
import io.agora.falcondemo.databinding.DialogPirDetectionBinding;

/**
 * pir 对话框
 */
public class SelectPirDialog extends BaseDialog<DialogPirDetectionBinding> {
    public SelectPirDialog(@NonNull Context context) {
        super(context);
    }

    private Drawable hasCheckDrawable;

    public ISingleCallback<Integer, Object> iSingleCallback;

    @NonNull
    @Override
    protected DialogPirDetectionBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogPirDetectionBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        setCanceledOnTouchOutside(true);
        getWindow().setWindowAnimations(R.style.popup_window_style_bottom);
        hasCheckDrawable = ContextCompat.getDrawable(getContext(), R.mipmap.albumselected);
        hasCheckDrawable.setBounds(0, 0, hasCheckDrawable.getMinimumWidth(), hasCheckDrawable.getMinimumHeight());
        getBinding().btnClose.setOnClickListener(view -> {
            //关闭
            getBinding().btnClose.setCompoundDrawables(null, null, hasCheckDrawable, null);
            getBinding().btnOpen.setCompoundDrawables(null, null, null, null);
            getBinding().btnLowSensitivity.setCompoundDrawables(null, null, null, null);
            getBinding().btnMiddleSensitivity.setCompoundDrawables(null, null, null, null);
            getBinding().btnHighSensitivity.setCompoundDrawables(null, null, null, null);
            iSingleCallback.onSingleCallback(1, 0);
            dismiss();
        });
        getBinding().btnOpen.setOnClickListener(view -> {
            //开启
            getBinding().btnClose.setCompoundDrawables(null, null, null, null);
            getBinding().btnOpen.setCompoundDrawables(null, null, hasCheckDrawable, null);
            getBinding().btnLowSensitivity.setCompoundDrawables(null, null, null, null);
            getBinding().btnMiddleSensitivity.setCompoundDrawables(null, null, null, null);
            getBinding().btnHighSensitivity.setCompoundDrawables(null, null, null, null);
            iSingleCallback.onSingleCallback(1, 1);
            dismiss();
        });
        getBinding().btnLowSensitivity.setOnClickListener(view -> {
            //低灵敏度
            getBinding().btnClose.setCompoundDrawables(null, null, null, null);
            getBinding().btnOpen.setCompoundDrawables(null, null, null, null);
            getBinding().btnLowSensitivity.setCompoundDrawables(null, null, hasCheckDrawable, null);
            getBinding().btnMiddleSensitivity.setCompoundDrawables(null, null, null, null);
            getBinding().btnHighSensitivity.setCompoundDrawables(null, null, null, null);
            iSingleCallback.onSingleCallback(1, 2);
            dismiss();
        });
        getBinding().btnMiddleSensitivity.setOnClickListener(view -> {
            //中灵敏度
            getBinding().btnClose.setCompoundDrawables(null, null, null, null);
            getBinding().btnOpen.setCompoundDrawables(null, null, null, null);
            getBinding().btnLowSensitivity.setCompoundDrawables(null, null, null, null);
            getBinding().btnMiddleSensitivity.setCompoundDrawables(null, null, hasCheckDrawable, null);
            getBinding().btnHighSensitivity.setCompoundDrawables(null, null, null, null);
            iSingleCallback.onSingleCallback(1, 3);
            dismiss();
        });
        getBinding().btnHighSensitivity.setOnClickListener(view -> {
            //高灵敏度
            getBinding().btnClose.setCompoundDrawables(null, null, null, null);
            getBinding().btnOpen.setCompoundDrawables(null, null, null, null);
            getBinding().btnLowSensitivity.setCompoundDrawables(null, null, null, null);
            getBinding().btnMiddleSensitivity.setCompoundDrawables(null, null, null, null);
            getBinding().btnHighSensitivity.setCompoundDrawables(null, null, hasCheckDrawable, null);
            iSingleCallback.onSingleCallback(1, 4);
            dismiss();
        });
    }

    public void setSelect(int type) {
        if (type == 0) {
            getBinding().btnClose.performClick();
        } else if (type == 1) {
            getBinding().btnOpen.performClick();
        } else if (type == 2) {
            getBinding().btnLowSensitivity.performClick();
        } else if (type == 3) {
            getBinding().btnMiddleSensitivity.performClick();
        } else if (type == 4) {
            getBinding().btnHighSensitivity.performClick();
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
