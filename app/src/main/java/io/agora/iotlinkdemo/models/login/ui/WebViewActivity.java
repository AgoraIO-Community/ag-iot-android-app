package io.agora.iotlinkdemo.models.login.ui;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityWebviewBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import kotlin.jvm.JvmField;

@Route(path = PagePathConstant.pageWebView)
public class WebViewActivity extends BaseViewBindingActivity<ActivityWebviewBinding> {
    /**
     * h5地址
     */
    @JvmField
    @Autowired(name = Constant.URL)
    String url = "https://iot-console-web.sh.agoralab.co/terms/termsofuse";


    @Override
    protected ActivityWebviewBinding getViewBinding(@NonNull LayoutInflater layoutInflater) {
        return ActivityWebviewBinding.inflate(layoutInflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        ARouter.getInstance().inject(this);
        if (url.contains("termsofuse")) {
            getBinding().titleView.setTitle(getString(R.string.user_agreement));
        } else {
            getBinding().titleView.setTitle(getString(R.string.privacy_policy));
        }
        getBinding().webView.loadUrl(url);
    }
}
