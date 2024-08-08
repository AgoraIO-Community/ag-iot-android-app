package io.agora.falcondemo.dialog;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.listener.ISingleCallback;
import com.agora.baselibrary.utils.ScreenUtils;

import io.agora.falcondemo.R;
import io.agora.falcondemo.databinding.DialogInputAppidBinding;


public class DialogInputAppId extends BaseDialog<DialogInputAppidBinding> {
    private Button selectedButton;
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

        selectedButton = getBinding().btnCn;
        getBinding().btnCn.setBackgroundResource(R.drawable.selected_button);
        setButtonListeners();

        getBinding().btnConfirm.setOnClickListener(view -> {
            String inputAppId = getBinding().etAppId.getText().toString();
            String inputKey = getBinding().etKey.getText().toString();
            String inputSecret = getBinding().etSecret.getText().toString();
            String selectRegion = selectedButton.getText().toString();
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

            if (TextUtils.isEmpty(selectRegion)) {
                popupMessage("region 不能为空!");
                return;
            }

            String[] params = { inputAppId, inputKey, inputSecret,selectRegion};
            mSingleCallback.onSingleCallback(0, params);

            getOnButtonClickListener().onRightButtonClick();
            dismiss();
        });
    }

    private void setButtonListeners() {
        getBinding().btnCn.setOnClickListener(this::onRegionButtonClick);
        getBinding().btnNa.setOnClickListener(this::onRegionButtonClick);
        getBinding().btnAp.setOnClickListener(this::onRegionButtonClick);
        getBinding().btnEu.setOnClickListener(this::onRegionButtonClick);
    }

    private void onRegionButtonClick(View view) {
        if (selectedButton != null) {
            selectedButton.setBackgroundResource(R.drawable.unselected_button);
            selectedButton.setTextColor(ContextCompat.getColor(getContext(), R.color.def_text_color));
        }
        selectedButton = (Button) view;
        selectedButton.setBackgroundResource(R.drawable.selected_button);
        selectedButton.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
    }

    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ScreenUtils.dp2px(320),
                ScreenUtils.dp2px(420)
        );
        getWindow().getAttributes().gravity = Gravity.CENTER;
    }

    protected void popupMessage(String message)
    {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
