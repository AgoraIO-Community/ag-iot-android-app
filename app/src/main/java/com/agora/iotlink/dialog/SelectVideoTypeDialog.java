package com.agora.iotlink.dialog;

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
import com.agora.iotlink.R;
import com.agora.iotlink.databinding.DialogSelectVideoBinding;

/**
 * 选择 视频类型 对话框
 */
public class SelectVideoTypeDialog extends BaseDialog<DialogSelectVideoBinding> {
    public SelectVideoTypeDialog(@NonNull Context context) {
        super(context);
    }

    private Drawable hasCheckDrawable;

    public ISingleCallback<Integer, Object> iSingleCallback;

    @NonNull
    @Override
    protected DialogSelectVideoBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogSelectVideoBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        hasCheckDrawable = ContextCompat.getDrawable(getContext(), R.mipmap.albumselected);
        hasCheckDrawable.setBounds(0, 0, hasCheckDrawable.getMinimumWidth(), hasCheckDrawable.getMinimumHeight());
        setCanceledOnTouchOutside(true);
        getWindow().setWindowAnimations(R.style.popup_window_style_bottom);
        getBinding().btnAll.setOnClickListener(view -> {
            setButtonCheckStatus(1);
        });
        getBinding().btnSoundDetection.setOnClickListener(view -> {
            setButtonCheckStatus(2);
        });
        getBinding().btnMotionDetection.setOnClickListener(view -> {
            setButtonCheckStatus(3);
        });
        getBinding().btnHumanInfraredDetection.setOnClickListener(view -> {
            setButtonCheckStatus(4);
        });
        getBinding().btnCallButton.setOnClickListener(view -> {
            setButtonCheckStatus(5);
        });
    }

    private void setButtonCheckStatus(int type) {
        iSingleCallback.onSingleCallback(type, null);
        getBinding().btnAll.setCompoundDrawables(null, null, null, null);
        getBinding().btnSoundDetection.setCompoundDrawables(null, null, null, null);
        getBinding().btnMotionDetection.setCompoundDrawables(null, null, null, null);
        getBinding().btnHumanInfraredDetection.setCompoundDrawables(null, null, null, null);
        getBinding().btnCallButton.setCompoundDrawables(null, null, null, null);
        switch (type) {
            case 1: {
                getBinding().btnAll.setCompoundDrawables(null, null, hasCheckDrawable, null);
                break;
            }
            case 2: {
                getBinding().btnSoundDetection.setCompoundDrawables(null, null, hasCheckDrawable, null);
                break;
            }
            case 3: {
                getBinding().btnMotionDetection.setCompoundDrawables(null, null, hasCheckDrawable, null);
                break;
            }
            case 4: {
                getBinding().btnHumanInfraredDetection.setCompoundDrawables(null, null, hasCheckDrawable, null);
                break;
            }
            case 5: {
                getBinding().btnCallButton.setCompoundDrawables(null, null, hasCheckDrawable, null);
                break;
            }
        }
        dismiss();
    }

    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ScreenUtils.dp2px(316)
        );
        getWindow().getAttributes().gravity = Gravity.BOTTOM;
    }
}
