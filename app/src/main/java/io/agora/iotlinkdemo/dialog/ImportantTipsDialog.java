package io.agora.iotlinkdemo.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ScreenUtils;
import io.agora.iotlinkdemo.databinding.DialogImportantTipsBinding;

public class ImportantTipsDialog extends BaseDialog<DialogImportantTipsBinding> {
    public ImportantTipsDialog(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected DialogImportantTipsBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogImportantTipsBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        getBinding().btnCancel.setOnClickListener(view -> {
            getOnButtonClickListener().onLeftButtonClick();
            dismiss();
        });
        getBinding().btnUpdate.setOnClickListener(view -> {
            getOnButtonClickListener().onRightButtonClick();
            dismiss();
        });
    }

    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ScreenUtils.dp2px(300),
                ScreenUtils.dp2px(258)
        );
        getWindow().getAttributes().gravity = Gravity.CENTER;
    }
}
