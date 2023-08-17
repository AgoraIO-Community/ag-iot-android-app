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
import io.agora.falcondemo.databinding.DialogChangeOfVoiceBinding;

/**
 * 变声 对话框
 */
public class ChangeOfVoiceDialog extends BaseDialog<DialogChangeOfVoiceBinding> {
    public ChangeOfVoiceDialog(@NonNull Context context) {
        super(context);
    }

    public ISingleCallback<Integer, Object> iSingleCallback;

    /**
     * 当前选中
     */
    private int currentPosition = 1;
    private Drawable hasCheckDrawable;

    @NonNull
    @Override
    protected DialogChangeOfVoiceBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogChangeOfVoiceBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        setCanceledOnTouchOutside(true);
        getWindow().setWindowAnimations(R.style.popup_window_style_bottom);
        hasCheckDrawable = ContextCompat.getDrawable(getContext(), R.mipmap.albumselected);
        hasCheckDrawable.setBounds(0, 0, hasCheckDrawable.getMinimumWidth(), hasCheckDrawable.getMinimumHeight());

        getBinding().btnVoiceOrg.setOnClickListener(view -> {
            currentPosition = 0;
            setCheckStatus();
            iSingleCallback.onSingleCallback(currentPosition, null);
            dismiss();
        });
        getBinding().btnVoiceOldman.setOnClickListener(view -> {
            currentPosition = 1;
            setCheckStatus();
            iSingleCallback.onSingleCallback(currentPosition, null);
            dismiss();
        });
        getBinding().btnVoiceBoy.setOnClickListener(view -> {
            currentPosition = 2;
            setCheckStatus();
            iSingleCallback.onSingleCallback(currentPosition, null);
            dismiss();
        });
        getBinding().btnVoiceGirl.setOnClickListener(view -> {
            currentPosition = 3;
            setCheckStatus();
            iSingleCallback.onSingleCallback(currentPosition, null);
            dismiss();
        });
        getBinding().btnVoiceZhubajie.setOnClickListener(view -> {
            currentPosition = 4;
            setCheckStatus();
            iSingleCallback.onSingleCallback(currentPosition, null);
            dismiss();
        });
        getBinding().btnVoiceEthereal.setOnClickListener(view -> {
            currentPosition = 5;
            setCheckStatus();
            iSingleCallback.onSingleCallback(currentPosition, null);
            dismiss();
        });
        getBinding().btnVoiceHulu.setOnClickListener(view -> {
            currentPosition = 6;
            setCheckStatus();
            iSingleCallback.onSingleCallback(currentPosition, null);
            dismiss();
        });
    }

    public void setSelect(int position) {
        currentPosition = position;
        switch (currentPosition) {
            case 0: {
                getBinding().btnVoiceOrg.performClick();
            } break;

            case 1: {
                getBinding().btnVoiceOldman.performClick();
            } break;

            case 2: {
                getBinding().btnVoiceBoy.performClick();
            } break;

            case 3: {
                getBinding().btnVoiceGirl.performClick();
            } break;

            case 4: {
                getBinding().btnVoiceZhubajie.performClick();
            } break;

            case 5: {
                getBinding().btnVoiceEthereal.performClick();
            } break;

            case 6: {
                getBinding().btnVoiceHulu.performClick();
            } break;
        }
    }

    private void setCheckStatus() {
        switch (currentPosition) {
            case 0: {
                getBinding().btnVoiceOrg.setCompoundDrawables(null, null, hasCheckDrawable, null);
                getBinding().btnVoiceOldman.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceBoy.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceGirl.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceZhubajie.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceEthereal.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceHulu.setCompoundDrawables(null, null, null, null);
            } break;

            case 1: {
                getBinding().btnVoiceOrg.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceOldman.setCompoundDrawables(null, null, hasCheckDrawable, null);
                getBinding().btnVoiceBoy.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceGirl.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceZhubajie.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceEthereal.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceHulu.setCompoundDrawables(null, null, null, null);
            } break;

            case 2: {
                getBinding().btnVoiceOrg.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceOldman.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceBoy.setCompoundDrawables(null, null, hasCheckDrawable, null);
                getBinding().btnVoiceGirl.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceZhubajie.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceEthereal.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceHulu.setCompoundDrawables(null, null, null, null);
            } break;

            case 3: {
                getBinding().btnVoiceOrg.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceOldman.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceBoy.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceGirl.setCompoundDrawables(null, null, hasCheckDrawable, null);
                getBinding().btnVoiceZhubajie.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceEthereal.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceHulu.setCompoundDrawables(null, null, null, null);
            } break;

            case 4: {
                getBinding().btnVoiceOrg.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceOldman.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceBoy.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceGirl.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceZhubajie.setCompoundDrawables(null, null, hasCheckDrawable, null);
                getBinding().btnVoiceEthereal.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceHulu.setCompoundDrawables(null, null, null, null);
            } break;

            case 5: {
                getBinding().btnVoiceOrg.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceOldman.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceBoy.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceGirl.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceZhubajie.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceEthereal.setCompoundDrawables(null, null, hasCheckDrawable, null);
                getBinding().btnVoiceHulu.setCompoundDrawables(null, null, null, null);
            } break;

            case 6: {
                getBinding().btnVoiceOrg.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceOldman.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceBoy.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceGirl.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceZhubajie.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceEthereal.setCompoundDrawables(null, null, null, null);
                getBinding().btnVoiceHulu.setCompoundDrawables(null, null, hasCheckDrawable, null);
            } break;
        }
    }

    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ScreenUtils.dp2px(380)
        );
        getWindow().getAttributes().gravity = Gravity.BOTTOM;
    }
}
