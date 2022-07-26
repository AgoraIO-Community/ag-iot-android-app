package io.agora.iotlinkdemo.models.usercenter;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;

import com.agora.baselibrary.base.BaseViewModel;
import com.agora.baselibrary.utils.SPUtil;
import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.event.UserLoginChangeEvent;
import io.agora.iotlinkdemo.utils.FileUtils;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAccountMgr;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class UserInfoViewModel extends BaseViewModel implements IAccountMgr.ICallback {

    public static IAccountMgr.UserInfo userInfo;

    public UserInfoViewModel() {
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onCleared() {
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(@Nullable UserLoginChangeEvent event) {
        if (getISingleCallback() != null) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_USER_STATUS_CHANGE, null);
        }
    }

    public void onStart() {
        // 注册账号管理监听
        AIotAppSdkFactory.getInstance().getAccountMgr().registerListener(this);
    }

    public void onStop() {
        // 注册账号管理监听
        AIotAppSdkFactory.getInstance().getAccountMgr().unregisterListener(this);
    }

    /**
     * 获取用户信息
     */
    public void requestUserInfo() {
        int ret = AIotAppSdkFactory.getInstance().getAccountMgr().queryUserInfo();
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能查询用户信息, 错误码: " + ret);
        }
    }

    /**
     * 获取用户信息 回调
     */
    @Override
    public void onQueryUserInfoDone(int errCode, IAccountMgr.UserInfo userInfo) {
        if (userInfo == null) return;
        if (UserInfoViewModel.userInfo != null) {
            String userAvatar = UserInfoViewModel.userInfo.mAvatar;
            UserInfoViewModel.userInfo = userInfo;
            if (TextUtils.isEmpty(UserInfoViewModel.userInfo.mAvatar)) {
                UserInfoViewModel.userInfo.mAvatar = userAvatar;
            }
        } else {
            UserInfoViewModel.userInfo = userInfo;
        }
        if (TextUtils.isEmpty(UserInfoViewModel.userInfo.mAvatar)) {
            UserInfoViewModel.userInfo.mAvatar = SPUtil.Companion.getInstance(AgoraApplication.getInstance()).getString("AVATAR", null);
        }
        if (getISingleCallback() != null) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_USER_GET_USERINFO, userInfo);
        }
    }

    /**
     * 修改用户昵称
     */
    public void requestSetUsername(IAccountMgr.UserInfo userInfo, String newUsername) {
        userInfo.mName = newUsername;
        int ret = AIotAppSdkFactory.getInstance().getAccountMgr().updateUserInfo(userInfo);
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能设置用户信息, 错误码: " + ret);
        }
    }

    /**
     * 修改完成回调
     */
    @Override
    public void onUpdateUserInfoDone(int errCode, IAccountMgr.UserInfo userInfo) {
        if (errCode != ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_USER_EDIT_USERINFO_FAIL, null);
            ToastUtils.INSTANCE.showToast("修改用户信息失败, 错误码: " + errCode);
        } else {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_USER_EDIT_USERINFO_SUCCESS, userInfo);
        }
    }

    /**
     * 上传图像图片到服务器，触发 onUploadPortraitDone() 回调
     */
    public void uploadPortrait(String tempPhotoPath) {
        int ret = AIotAppSdkFactory.getInstance().getAccountMgr().uploadPortrait(FileUtils.file2byte(new File(tempPhotoPath)));
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能设置用户信息, 错误码: " + ret);
        }
    }

    /**
     * 上传图像图片到服务器，触发 onUploadPortraitDone() 回调
     */
    public void uploadPortrait(Drawable drawable) {
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        int ret = AIotAppSdkFactory.getInstance().getAccountMgr().uploadPortrait(stream.toByteArray());
        if (ret != ErrCode.XOK) {
            ToastUtils.INSTANCE.showToast("不能设置用户信息, 错误码: " + ret);
        }
    }

    @Override
    public void onUploadPortraitDone(int errCode, byte[] fileContent, String cloudFilePath) {
        if (errCode != ErrCode.XOK) {
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_USER_UPLOAD_AVATAR_FAIL, null);
            ToastUtils.INSTANCE.showToast("上传头像失败, 错误码: " + errCode);
        } else {
            UserInfoViewModel.userInfo.mAvatar = cloudFilePath;
            AIotAppSdkFactory.getInstance().getAccountMgr().updateUserInfo(UserInfoViewModel.userInfo);
            SPUtil.Companion.getInstance(AgoraApplication.getInstance()).putString("AVATAR", cloudFilePath);
            getISingleCallback().onSingleCallback(Constant.CALLBACK_TYPE_USER_UPLOAD_AVATAR_SUCCESS, cloudFilePath);
            ToastUtils.INSTANCE.showToast("上传头像成功");
        }
    }
}