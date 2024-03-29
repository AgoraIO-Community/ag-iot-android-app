package io.agora.falcondemo.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ScreenUtils;

import io.agora.falcondemo.databinding.DialogUserAgreementBinding;


public class UserAgreementDialog extends BaseDialog<DialogUserAgreementBinding> {
    public UserAgreementDialog(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected DialogUserAgreementBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogUserAgreementBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        getBinding().btnDisagree.setOnClickListener(view -> {
            getOnButtonClickListener().onLeftButtonClick();
        });
        getBinding().btnAgree.setOnClickListener(view -> {
            getOnButtonClickListener().onRightButtonClick();
        });
        getBinding().tvPrivacyPolicy.setOnClickListener(view -> {
            //PagePilotManager.pageWebView("https://agoralink.sd-rtn.com/terms/privacypolicy");
        });
        getBinding().tvUserAgreement.setOnClickListener(view -> {
            //PagePilotManager.pageWebView("https://agoralink.sd-rtn.com/terms/termsofuse");
        });
    }

    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ScreenUtils.dp2px(300),
                ScreenUtils.dp2px(478)
        );
        getWindow().getAttributes().gravity = Gravity.CENTER;
    }
}
