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
import io.agora.falcondemo.databinding.DialogNightVisionBinding;

/**
 * 红外夜视 对话框
 */
public class SelectNightVisionDialog extends BaseDialog<DialogNightVisionBinding> {
    public SelectNightVisionDialog(@NonNull Context context) {
        super(context);
    }

    private Drawable hasCheckDrawable;

    public ISingleCallback<Integer, Object> iSingleCallback;

    @NonNull
    @Override
    protected DialogNightVisionBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogNightVisionBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        setCanceledOnTouchOutside(true);
        getWindow().setWindowAnimations(R.style.popup_window_style_bottom);
        hasCheckDrawable = ContextCompat.getDrawable(getContext(), R.mipmap.albumselected);
        hasCheckDrawable.setBounds(0, 0, hasCheckDrawable.getMinimumWidth(), hasCheckDrawable.getMinimumHeight());

        getBinding().btnNvAuto.setOnClickListener(view -> {
            //自动
            getBinding().btnNvAuto.setCompoundDrawables(null, null, hasCheckDrawable, null);
            getBinding().btnNvClose.setCompoundDrawables(null, null, null, null);
            getBinding().btnNvOpen.setCompoundDrawables(null, null, null, null);
            iSingleCallback.onSingleCallback(0, null);
            dismiss();
        });
        getBinding().btnNvClose.setOnClickListener(view -> {
            //关闭
            getBinding().btnNvAuto.setCompoundDrawables(null, null, null, null);
            getBinding().btnNvClose.setCompoundDrawables(null, null, hasCheckDrawable, null);
            getBinding().btnNvOpen.setCompoundDrawables(null, null, null, null);
            iSingleCallback.onSingleCallback(1, null);
            dismiss();
        });
        getBinding().btnNvOpen.setOnClickListener(view -> {
            //开启
            getBinding().btnNvAuto.setCompoundDrawables(null, null, null, null);
            getBinding().btnNvClose.setCompoundDrawables(null, null, null, null);
            getBinding().btnNvOpen.setCompoundDrawables(null, null, hasCheckDrawable, null);
            iSingleCallback.onSingleCallback(2, null);
            dismiss();
        });
    }

    /**
     * 设置默认选择 ps 目前只支持开与关
     */
    public void setSelect(int type) {
        if (type == 0) {
            getBinding().btnNvAuto.performClick();
        } else if (type == 2){
            getBinding().btnNvOpen.performClick();
        } else {
            getBinding().btnNvClose.performClick();
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
