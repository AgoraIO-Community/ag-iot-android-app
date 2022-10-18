package io.agora.iotlinkdemo.models.home.homemine;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.utils.NetUtils;
import com.agora.baselibrary.utils.StringUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingFragment;
import io.agora.iotlinkdemo.common.CenterCropRoundCornerTransform;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.common.GlideApp;
import io.agora.iotlinkdemo.databinding.FragmentHomeMineBinding;
import io.agora.iotlinkdemo.manager.DevicesListManager;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.usercenter.UserInfoViewModel;
import io.agora.iotlink.IAccountMgr;
import io.agora.iotlinkdemo.thirdpartyaccount.ThirdAccountMgr;

import com.bumptech.glide.load.engine.DiskCacheStrategy;


public class MineFragment extends BaseViewBindingFragment<FragmentHomeMineBinding> {
    /**
     * 用户相关viewModel
     */
    private UserInfoViewModel userInfoViewModel;

    @NonNull
    @Override
    protected FragmentHomeMineBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomeMineBinding.inflate(inflater);
    }

    @Override
    public void initView() {
        userInfoViewModel = new ViewModelProvider(this).get(UserInfoViewModel.class);
        userInfoViewModel.setLifecycleOwner(this);
        getBinding().ivToEdit.setVisibility(View.INVISIBLE);
        getBinding().vToEdit.setVisibility(View.INVISIBLE);
    }

    @Override
    public void initListener() {

        String accountName = ThirdAccountMgr.getInstance().getLoginAccountName();
        getBinding().tvUserMobile.setText(accountName);

        getBinding().vToEdit.setOnClickListener(view -> {
            if (NetUtils.INSTANCE.isNetworkConnected()) {
            //    PagePilotManager.pageUserInfo();
            }
        });
        getBinding().tvGeneralSettings.setOnClickListener(view -> {
            if (NetUtils.INSTANCE.isNetworkConnected()) {
                PagePilotManager.pageGeneralSettings();
            }
        });
        getBinding().tvMsgCenter.setOnClickListener(view -> {
            if (NetUtils.INSTANCE.isNetworkConnected()) {
                PagePilotManager.pageMessage();
            }
        });
        getBinding().tvAbout.setOnClickListener(view -> PagePilotManager.pageAbout());
        setUserInfo();
     }

    private void setUserInfo() {
        getBinding().tvUserMobile.post(() -> {
            String accountName = ThirdAccountMgr.getInstance().getLoginAccountName();
            String accountId = ThirdAccountMgr.getInstance().getLoginAccountId();
            String txtName = accountName; // + "\n (" + accountId + ")";
            getBinding().tvUserMobile.setText(txtName);

            int count = DevicesListManager.deviceSize;
            getBinding().tvDeviceCount.setText(count + " 台设备");
         });

    }

    @Override
    public void requestData() {
        if (NetUtils.INSTANCE.isNetworkConnected()) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        userInfoViewModel.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        userInfoViewModel.onStop();
    }
}
