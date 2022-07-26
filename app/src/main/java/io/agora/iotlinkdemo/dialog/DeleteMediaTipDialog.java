package io.agora.iotlinkdemo.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ScreenUtils;
import io.agora.iotlinkdemo.databinding.DialogDeleteMediaTipBinding;

/**
 * 删除提示 对话框
 */
public class DeleteMediaTipDialog extends BaseDialog<DialogDeleteMediaTipBinding> {
    public DeleteMediaTipDialog(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected DialogDeleteMediaTipBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogDeleteMediaTipBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        getBinding().btnCancel.setOnClickListener(view -> {
                    getOnButtonClickListener().onLeftButtonClick();
                    dismiss();
                }
        );
        getBinding().btnDelete.setOnClickListener(view -> {
            getOnButtonClickListener().onRightButtonClick();
            dismiss();
        });
    }


    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ScreenUtils.dp2px(300),
                ScreenUtils.dp2px(195)
        );
        getWindow().getAttributes().gravity = Gravity.CENTER;
    }
}
