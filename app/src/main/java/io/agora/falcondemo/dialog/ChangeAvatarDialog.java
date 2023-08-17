package io.agora.falcondemo.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.listener.ISingleCallback;
import com.agora.baselibrary.utils.ScreenUtils;
import io.agora.falcondemo.R;
import io.agora.falcondemo.databinding.DialogChangeAvatarBinding;

/**
 * 选择头像
 */
public class ChangeAvatarDialog extends BaseDialog<DialogChangeAvatarBinding> {
    public ChangeAvatarDialog(@NonNull Context context) {
        super(context);
    }

    public ISingleCallback<Integer, Object> iSingleCallback;

    /**
     * 当前选中
     */
    private int currentPosition = 1;

    @NonNull
    @Override
    protected DialogChangeAvatarBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogChangeAvatarBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        setCanceledOnTouchOutside(true);
        getWindow().setWindowAnimations(R.style.popup_window_style_bottom);
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
