package io.agora.iotlinkdemo.models.usercenter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.NetUtils;
import com.agora.baselibrary.utils.SPUtil;
import io.agora.iotlinkdemo.BuildConfig;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.CenterCropRoundCornerTransform;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.common.GlideApp;
import io.agora.iotlinkdemo.databinding.ActivityUserInfoBinding;
import io.agora.iotlinkdemo.dialog.ChangeAvatarDialog;
import io.agora.iotlinkdemo.dialog.ChangeOfVoiceDialog;
import io.agora.iotlinkdemo.dialog.SelectPhotoFromDialog;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.utils.FileUtils;
import io.agora.iotlinkdemo.utils.ImageCompressUtil;
import io.agora.iotlinkdemo.utils.UriUtils;
import io.agora.iotlink.IAccountMgr;
import io.agora.iotlink.ICallkitMgr;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.net.URI;

/**
 * 个人资料
 */
@Route(path = PagePathConstant.pageUserInfo)
public class UserInfoActivity extends BaseViewBindingActivity<ActivityUserInfoBinding> {
    private static final int CHOOSE_PHOTO = 100;
    private static final int TAKE_PHOTO = 101;
    private SelectPhotoFromDialog selectPhotoFromDialog;
    /**
     * 用户相关viewModel
     */
    private UserInfoViewModel userInfoViewModel;

    private ChangeAvatarDialog changeAvatarDialog;

    @Override
    protected ActivityUserInfoBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityUserInfoBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        userInfoViewModel = new ViewModelProvider(this).get(UserInfoViewModel.class);
        userInfoViewModel.setLifecycleOwner(this);
    }


    @Override
    public void initListener() {
        getBinding().tvAvatar.setOnClickListener(view -> {
            if (NetUtils.INSTANCE.isNetworkConnected()) {
                showChangeAvatarDialog();
            }
        });
        getBinding().tvNickname.setOnClickListener(view -> {
            if (NetUtils.INSTANCE.isNetworkConnected()) {
                PagePilotManager.pageUserEditNickname("");
            }
        });
        userInfoViewModel.setISingleCallback((type, var2) -> {
            if (type == Constant.CALLBACK_TYPE_USER_GET_USERINFO) {
             setUserInfo();
            } else if (type == Constant.CALLBACK_TYPE_USER_UPLOAD_AVATAR_SUCCESS) {
                hideLoadingView();
//                SPUtil.Companion.getInstance(this).putString("AVATAR", (String) var2);
            }
        });
    }

    private void showChangeAvatarDialog() {
        if (changeAvatarDialog == null) {
            changeAvatarDialog = new ChangeAvatarDialog(this);
            changeAvatarDialog.iSingleCallback = (type, var2) -> {
            };
        }
        changeAvatarDialog.show();
    }

    private void setUserInfo() {

    }

    @Override
    public void requestData() {

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
