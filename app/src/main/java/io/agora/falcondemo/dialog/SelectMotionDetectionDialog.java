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
import io.agora.falcondemo.databinding.DialogMotionDetectionBinding;

/**
 * 移动侦测 对话框
 */
public class SelectMotionDetectionDialog extends BaseDialog<DialogMotionDetectionBinding> {
    public SelectMotionDetectionDialog(@NonNull Context context) {
        super(context);
    }

    private Drawable hasCheckDrawable;

    public ISingleCallback<Integer, Object> iSingleCallback;

    @NonNull
    @Override
    protected DialogMotionDetectionBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogMotionDetectionBinding.inflate(inflater);
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
            getBinding().btnHighSensitivity.setCompoundDrawables(null, null, null, null);
            getBinding().btnOpen.setCompoundDrawables(null, null, null, null);
            iSingleCallback.onSingleCallback(0, null);
            dismiss();
        });
        getBinding().btnHighSensitivity.setOnClickListener(view -> {
            //高灵敏度
            getBinding().btnClose.setCompoundDrawables(null, null, null, null);
            getBinding().btnHighSensitivity.setCompoundDrawables(null, null, hasCheckDrawable, null);
            getBinding().btnOpen.setCompoundDrawables(null, null, null, null);
            iSingleCallback.onSingleCallback(1, null);
            dismiss();
        });
        getBinding().btnOpen.setOnClickListener(view -> {
            //开启
            getBinding().btnClose.setCompoundDrawables(null, null, null, null);
            getBinding().btnHighSensitivity.setCompoundDrawables(null, null, null, null);
            getBinding().btnOpen.setCompoundDrawables(null, null, hasCheckDrawable, null);
            iSingleCallback.onSingleCallback(2, null);
            dismiss();
        });
    }

    /**
     * 设置默认选择 ps 目前只支持开与关
     */
    public void setSelect(int type) {
        if (type != 0) {
            getBinding().btnOpen.performClick();
        }else{
            getBinding().btnClose.performClick();
        }
    }

    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ScreenUtils.dp2px(222)
        );
        getWindow().getAttributes().gravity = Gravity.BOTTOM;
    }
}
