package io.agora.falcondemo.models.login;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.falcondemo.R;
import io.agora.falcondemo.base.BaseViewBindingActivity;
import io.agora.falcondemo.databinding.ActivityWebviewBinding;


public class WebViewActivity extends BaseViewBindingActivity<ActivityWebviewBinding> {

    String url = "https://agoralink.sd-rtn.com/terms/termsofuse";


    @Override
    protected ActivityWebviewBinding getViewBinding(@NonNull LayoutInflater layoutInflater) {
        return ActivityWebviewBinding.inflate(layoutInflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        if (url.contains("termsofuse")) {
            getBinding().titleView.setTitle(getString(R.string.user_agreement));
        } else {
            getBinding().titleView.setTitle(getString(R.string.privacy_policy));
        }
        getBinding().webView.loadUrl(url);
    }
}
