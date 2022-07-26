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
                if (var2 instanceof IAccountMgr.UserInfo) {
                    setUserInfo((IAccountMgr.UserInfo) var2);
                }
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
                uploadDrawablePath(type);
            };
        }
        changeAvatarDialog.show();
    }

    private void uploadDrawablePath(int type) {
        showLoadingView();
        int drawableId = 0;
        if (type == 1) {
            drawableId = R.mipmap.boy;
        } else if (type == 2) {
            drawableId = R.mipmap.girl;
        } else if (type == 3) {
            drawableId = R.mipmap.dog;
        } else if (type == 4) {
            drawableId = R.mipmap.cat;
        }
        userInfoViewModel.uploadPortrait(ContextCompat.getDrawable(this, drawableId));
        getBinding().ivUserAvatar.setImageResource(drawableId);
    }

    private void setUserInfo(IAccountMgr.UserInfo userInfo) {
        getBinding().tvNickname.post(() -> {
            if (!TextUtils.isEmpty(userInfo.mName)) {
                getBinding().tvNickname.setText(userInfo.mName);
            } else if (!TextUtils.isEmpty(userInfo.mPhoneNumber)) {
                getBinding().tvNickname.setText(userInfo.mPhoneNumber);
            } else if (!TextUtils.isEmpty(userInfo.mEmail)) {
                getBinding().tvNickname.setText(userInfo.mEmail);
            }
            getBinding().tvNickname.post(() -> {
                GlideApp.with(this).load(userInfo.mAvatar).error(R.mipmap.userimage)
                        .transform(new CenterCropRoundCornerTransform(100))
                        .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(getBinding().ivUserAvatar);
            });
        });

    }

    @Override
    public void requestData() {
        userInfoViewModel.requestUserInfo();
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

    private void showSelectPhotoFromDialog() {
        if (selectPhotoFromDialog == null) {
            selectPhotoFromDialog = new SelectPhotoFromDialog(this);
            selectPhotoFromDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {
                    openAlbum();
                }

                @Override
                public void onRightButtonClick() {
                    takePhoto();
                }
            });
        }
        selectPhotoFromDialog.show();
    }

    String mTempPhotoPath = null;

    private void takePhoto() {
        Intent intentToTakePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intentToTakePhoto.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        File fileDir = new File(
                FileUtils.getTempSDPath()
        );
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        File photoFile = new File(fileDir, "photo.jpg");
        mTempPhotoPath = photoFile.getAbsolutePath();
        Uri imageUri = FileProvider.getUriForFile(
                this,
                BuildConfig.APPLICATION_ID + ".fileProvider",
                photoFile
        );
        intentToTakePhoto.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intentToTakePhoto, TAKE_PHOTO);
    }

    private void openAlbum() {
        Intent intentToPickPic = new Intent(Intent.ACTION_PICK, null);
        intentToPickPic.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intentToPickPic, CHOOSE_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CHOOSE_PHOTO) {
                Uri uri = data.getData();
                if (uri != null) {
                    String filePath = UriUtils.INSTANCE.getFilePathByUri(this, uri);
                    if (!TextUtils.isEmpty(filePath)) {
                        setImage(filePath);
                    }
                }
            }
        } else if (requestCode == TAKE_PHOTO) {
            setImage(mTempPhotoPath);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setImage(String filePath) {
        if (filePath == null) return;
        String path = ImageCompressUtil.displayPath(this, filePath);
        if (TextUtils.isEmpty(path) || new File(filePath).length() <= 150000) {
            path = filePath;
        }
        mTempPhotoPath = path;
        UserInfoViewModel.userInfo.mAvatar = mTempPhotoPath;
        userInfoViewModel.uploadPortrait(mTempPhotoPath);

    }
}
