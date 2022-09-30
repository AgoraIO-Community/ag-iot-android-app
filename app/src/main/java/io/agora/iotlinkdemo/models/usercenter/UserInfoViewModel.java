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


}