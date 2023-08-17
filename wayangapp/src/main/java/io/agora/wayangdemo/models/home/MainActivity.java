package io.agora.wayangdemo.models.home;


import android.content.Intent;
import android.os.Bundle;

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
import io.agora.wayangdemo.databinding.ActivityMainBinding;
import io.agora.wayangdemo.models.config.ConfigActivity;
import io.agora.wayangdemo.models.welcome.WelcomeActivity;

public class MainActivity extends BaseViewBindingActivity<ActivityMainBinding> {
    private static final String TAG = "IOTWY/MainActivity";


    private boolean isStop = false;



    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseActivity /////////////////////
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected ActivityMainBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityMainBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        // 设置视频帧显示控件
        SdkWayangFactory.getInstance().setDisplayView(getBinding().svDeviceView);
    }

    @Override
    protected boolean isCanExit() {
        return true;
    }

    @Override
    public void initListener() {
        getBinding().titleView.setRightIconClick(view -> {
            gotoConfigActivity();
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

//        HomePageFragment homePageFragment = (HomePageFragment)getFragment(HomePageFragment.class);
//        if (homePageFragment != null) {
//            homePageFragment.onFragRequestPermissionsResult(requestCode, permissions, grantResults);
//        }
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


    void gotoConfigActivity() {
        Intent intent = new Intent(MainActivity.this, ConfigActivity.class);
        startActivity(intent);
    }
}
