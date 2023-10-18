package io.agora.falcondemo.dialog;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.listener.ISingleCallback;
import com.agora.baselibrary.utils.ScreenUtils;

import io.agora.falcondemo.databinding.DialogInputAppidBinding;
import io.agora.falcondemo.databinding.DialogInputCommandBinding;


public class DialogInputCommand extends BaseDialog<DialogInputCommandBinding> {
    public DialogInputCommand(@NonNull Context context) {
        super(context);
    }

    public ISingleCallback<Integer, Object>  mSingleCallback;

    @NonNull
    @Override
    protected DialogInputCommandBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogInputCommandBinding.inflate(inflater);
    }

    @Override
    protected void initView() {

        getBinding().btnConfirm.setOnClickListener(view -> {
            String inputAppId = getBinding().etCommand.getText().toString();
            if (TextUtils.isEmpty(inputAppId)) {
                popupMessage("发送的命令不能为空!");
                return;
            }

            mSingleCallback.onSingleCallback(0, inputAppId);

            getOnButtonClickListener().onRightButtonClick();
            dismiss();
        });
    }


    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ScreenUtils.dp2px(300),
                ScreenUtils.dp2px(230)
        );
        getWindow().getAttributes().gravity = Gravity.CENTER;
    }

    protected void popupMessage(String message)
    {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
