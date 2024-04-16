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


public class DialogInputAppId extends BaseDialog<DialogInputAppidBinding> {
    public DialogInputAppId(@NonNull Context context) {
        super(context);
    }

    public ISingleCallback<Integer, Object>  mSingleCallback;

    @NonNull
    @Override
    protected DialogInputAppidBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogInputAppidBinding.inflate(inflater);
    }

    @Override
    protected void initView() {

        getBinding().btnConfirm.setOnClickListener(view -> {
            String inputAppId = getBinding().etAppId.getText().toString();
            String inputKey = getBinding().etKey.getText().toString();
            String inputSecret = getBinding().etSecret.getText().toString();
            if (TextUtils.isEmpty(inputAppId)) {
                popupMessage("appId 不能为空!");
                return;
            }
            if (TextUtils.isEmpty(inputKey)) {
                popupMessage("Basic Auth Key 不能为空!");
                return;
            }
            if (TextUtils.isEmpty(inputSecret)) {
                popupMessage("Basic Auth Secret 不能为空!");
                return;
            }

            String[] params = { inputAppId, inputKey, inputSecret};
            mSingleCallback.onSingleCallback(0, params);

            getOnButtonClickListener().onRightButtonClick();
            dismiss();
        });
    }


    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ScreenUtils.dp2px(300),
                ScreenUtils.dp2px(400)
        );
        getWindow().getAttributes().gravity = Gravity.CENTER;
    }

    protected void popupMessage(String message)
    {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
