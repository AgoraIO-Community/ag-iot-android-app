package io.agora.wayangdemo.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ScreenUtils;
import io.agora.wayangdemo.databinding.DialogCommonBinding;

public class CommonDialog extends BaseDialog<DialogCommonBinding> {
    public CommonDialog(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected DialogCommonBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogCommonBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        getBinding().btnLeft.setOnClickListener(view -> {
            getOnButtonClickListener().onLeftButtonClick();
            dismiss();
        });
        getBinding().btnRight.setOnClickListener(view -> {
            getOnButtonClickListener().onRightButtonClick();
            dismiss();
        });
    }

    public void setDialogTitle(String title) {
        getBinding().tvTitle.setText(title);
    }

    public void setDescText(String desc) {
        getBinding().tvDesc.setText(desc);
        getBinding().tvDesc.setVisibility(View.VISIBLE);
        getWindow().setLayout(
                ScreenUtils.dp2px(300),
                ScreenUtils.dp2px(227)
        );
    }

    public void setDialogBtnText(String leftText, String rightText) {
        getBinding().btnLeft.setText(leftText);
        getBinding().btnRight.setText(rightText);
    }

    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ScreenUtils.dp2px(300),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        getWindow().getAttributes().gravity = Gravity.CENTER;
    }
}
