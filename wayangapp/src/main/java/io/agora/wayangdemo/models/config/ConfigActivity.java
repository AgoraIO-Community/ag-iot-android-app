package io.agora.wayangdemo.models.config;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.ActivityKt;
import androidx.navigation.NavController;
import androidx.navigation.ui.BottomNavigationViewKt;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.listener.ISingleCallback;

import io.agora.iotlink.SdkWayangFactory;
import io.agora.wayangdemo.R;
import io.agora.wayangdemo.base.AgoraApplication;
import io.agora.wayangdemo.base.BaseViewBindingActivity;
import io.agora.wayangdemo.databinding.ActivityConfigBinding;
import io.agora.wayangdemo.databinding.ActivityMainBinding;
import io.agora.wayangdemo.utils.AppStorageUtil;

public class ConfigActivity extends BaseViewBindingActivity<ActivityConfigBinding> {
    private static final String TAG = "IOTWY/CfgActivity";


    private boolean isStop = false;
    private ConfigActivity mActivity;


    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseActivity /////////////////////
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected ActivityConfigBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityConfigBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        mActivity = this;
        String serverUrl = AppStorageUtil.safeGetString(this, AppStorageUtil.KEY_SRVURL, null);
        String deviceInfo = AppStorageUtil.safeGetString(this, AppStorageUtil.KEY_DEVINFO, null);

        if (!TextUtils.isEmpty(serverUrl)) {
            getBinding().etServerUrl.setText(serverUrl);
        } else {
            getBinding().etServerUrl.setText("");
        }

        if (!TextUtils.isEmpty(deviceInfo)) {
            getBinding().etWsDevInfo.setText(deviceInfo);
        } else {
            getBinding().etWsDevInfo.setText("");
        }
    }

    @Override
    protected boolean isCanExit() {
        return false;
    }

    @Override
    public void initListener() {
        getBinding().btnCfgConfirm.setOnClickListener(view -> {
            onBtnConfirm();
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        isStop = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isStop = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "<onRequestPermissionsResult> requestCode=" + requestCode);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            HomePageFragment homePageFragment = (HomePageFragment)getFragment(HomePageFragment.class);
//            if (homePageFragment != null) {
//                boolean respKeyDown = homePageFragment.onBackKeyEvent();
//                if (respKeyDown) {  // 已经响应了 Back按键处理
//                    return true;
//                }
//            }
        }
        return super.onKeyDown(keyCode, event);
    }


    /**
     * @brief 确认按钮
     */
    void onBtnConfirm() {
        String etServerUrl = getBinding().etServerUrl.getText().toString();
        if (TextUtils.isEmpty(etServerUrl)) {
            popupMessage("服务器地址不能为空!");
            return;
        }

        String deviceInfo = getBinding().etWsDevInfo.getText().toString();
        if (TextUtils.isEmpty(deviceInfo)) {
            popupMessage("设备信息不能为空!");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(ConfigActivity.this);
        builder.setIcon(null);
        builder.setTitle("配置提醒");
        builder.setMessage("重新配置后，需要重启APP才能生效，确定配置吗？");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                AppStorageUtil.safePutString(mActivity, AppStorageUtil.KEY_SRVURL, etServerUrl);
                AppStorageUtil.safePutString(mActivity, AppStorageUtil.KEY_DEVINFO, deviceInfo);
                popupMessage("配置成功!");

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mHealthActivityManager.popAllActivity();
                        System.exit(0);
                    }
                }, 2000L);
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        }).show();
    }



}
