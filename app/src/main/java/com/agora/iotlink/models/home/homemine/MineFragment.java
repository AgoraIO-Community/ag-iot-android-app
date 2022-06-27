package com.agora.iotlink.models.home.homemine;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.utils.NetUtils;
import com.agora.baselibrary.utils.StringUtils;
import com.agora.iotlink.R;
import com.agora.iotlink.base.BaseViewBindingFragment;
import com.agora.iotlink.common.CenterCropRoundCornerTransform;
import com.agora.iotlink.common.Constant;
import com.agora.iotlink.common.GlideApp;
import com.agora.iotlink.databinding.FragmentHomeMineBinding;
import com.agora.iotlink.manager.DevicesListManager;
import com.agora.iotlink.manager.PagePilotManager;
import com.agora.iotlink.models.usercenter.UserInfoViewModel;
import com.agora.iotsdk20.IAccountMgr;
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
    }

    @Override
    public void initListener() {
        getBinding().vToEdit.setOnClickListener(view -> {
            if (NetUtils.INSTANCE.isNetworkConnected()) {
                PagePilotManager.pageUserInfo();
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
        userInfoViewModel.setISingleCallback((type, var2) -> {
            if (type == Constant.CALLBACK_TYPE_USER_GET_USERINFO) {
                if (var2 instanceof IAccountMgr.UserInfo) {
                    setUserInfo((IAccountMgr.UserInfo) var2);
                }
            }
        });
    }

    private void setUserInfo(IAccountMgr.UserInfo userInfo) {
        if (userInfo == null) return;
        getBinding().tvUserMobile.post(() -> {
            if (!TextUtils.isEmpty(userInfo.mName)) {
                getBinding().tvUserMobile.setText(userInfo.mName);
            } else if (!TextUtils.isEmpty(userInfo.mPhoneNumber)) {
                getBinding().tvUserMobile.setText(StringUtils.INSTANCE.formatAccount(userInfo.mPhoneNumber));
            } else if (!TextUtils.isEmpty(userInfo.mEmail)) {
                getBinding().tvUserMobile.setText(userInfo.mEmail);
            }
            int count = DevicesListManager.deviceSize;
            getBinding().tvDeviceCount.setText(count + " 台设备");
            GlideApp.with(this).load(userInfo.mAvatar).error(R.mipmap.userimage)
                    .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                    .transform(new CenterCropRoundCornerTransform(100)).into(getBinding().ivUserAvatar);
        });

    }

    @Override
    public void requestData() {
        if (NetUtils.INSTANCE.isNetworkConnected()) {
            userInfoViewModel.requestUserInfo();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (UserInfoViewModel.userInfo != null) {
            setUserInfo(UserInfoViewModel.userInfo);
        }
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
