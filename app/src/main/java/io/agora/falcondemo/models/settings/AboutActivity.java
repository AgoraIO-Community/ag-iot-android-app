package io.agora.falcondemo.models.settings;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.baselibrary.base.BaseBindingActivity;
import com.agora.baselibrary.base.BaseDialog;
import io.agora.falcondemo.BuildConfig;
import io.agora.falcondemo.R;
import io.agora.falcondemo.databinding.ActivityAboutBinding;
import io.agora.falcondemo.dialog.UserAgreementDialog;



public class AboutActivity extends BaseBindingActivity<ActivityAboutBinding> {

    private UserAgreementDialog userAgreementDialog;

    @Override
    protected ActivityAboutBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityAboutBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {

        getBinding().tvPrivacyPolicy.setOnClickListener(view -> {
            showPolicyDialog();
        });

        getBinding().tvUserAgreement.setOnClickListener(view -> {
            showPolicyDialog();
        });

    }

    @Override
    public void initListener() {
        getBinding().tvVersion.setText(getString(R.string.version_is, BuildConfig.VERSION_NAME));
    }


    void showPolicyDialog() {
        if (userAgreementDialog == null) {
            userAgreementDialog = new UserAgreementDialog(this);
            userAgreementDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {
                    userAgreementDialog.dismiss();
                }

                @Override
                public void onRightButtonClick() {
                    userAgreementDialog.dismiss();
                }
            });
        }
        userAgreementDialog.show();
    }
}
