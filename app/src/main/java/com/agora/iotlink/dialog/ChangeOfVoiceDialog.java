package com.agora.iotlink.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.listener.ISingleCallback;
import com.agora.baselibrary.utils.ScreenUtils;
import com.agora.iotlink.R;
import com.agora.iotlink.databinding.DialogChangeOfVoiceBinding;

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

    @NonNull
    @Override
    protected DialogChangeOfVoiceBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogChangeOfVoiceBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        setCanceledOnTouchOutside(true);
        getWindow().setWindowAnimations(R.style.popup_window_style_bottom);
        getBinding().cbVoice1.setSelected(true);
        getBinding().cbVoice1.setOnClickListener(view -> {
            currentPosition = 1;
            setCheckStatus();
            dismiss();
        });
        getBinding().cbVoice2.setOnClickListener(view -> {
            currentPosition = 2;
            setCheckStatus();
            dismiss();
        });
        getBinding().cbVoice3.setOnClickListener(view -> {
            currentPosition = 3;
            setCheckStatus();
            dismiss();
        });
        getBinding().cbVoice4.setOnClickListener(view -> {
            currentPosition = 4;
            setCheckStatus();
            dismiss();
        });

    }

    public void setCheckStatus() {
        iSingleCallback.onSingleCallback(currentPosition, null);
        switch (currentPosition) {
            case 2: {
                getBinding().cbVoice1.setSelected(false);
                getBinding().cbVoice2.setSelected(true);
                getBinding().cbVoice3.setSelected(false);
                getBinding().cbVoice4.setSelected(false);
                break;
            }
            case 3: {
                getBinding().cbVoice1.setSelected(false);
                getBinding().cbVoice2.setSelected(false);
                getBinding().cbVoice3.setSelected(true);
                getBinding().cbVoice4.setSelected(false);
                break;
            }
            case 4: {
                getBinding().cbVoice1.setSelected(false);
                getBinding().cbVoice2.setSelected(false);
                getBinding().cbVoice3.setSelected(false);
                getBinding().cbVoice4.setSelected(true);
                break;
            }
            default: {
                getBinding().cbVoice1.setSelected(true);
                getBinding().cbVoice2.setSelected(false);
                getBinding().cbVoice3.setSelected(false);
                getBinding().cbVoice4.setSelected(false);
                break;
            }
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
