package com.agora.iotlink.models.home;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.ActivityKt;
import androidx.navigation.NavController;
import androidx.navigation.ui.BottomNavigationViewKt;

import com.agora.baselibrary.listener.ISingleCallback;
import com.agora.iotlink.R;
import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.databinding.ActivityMainBinding;
import com.agora.iotlink.manager.PagePathConstant;
import com.agora.iotlink.manager.PagePilotManager;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 主页容器
 */
@Route(path = PagePathConstant.pageMainHome)
public class MainActivity extends BaseViewBindingActivity<ActivityMainBinding> {
    private NavController navController;
    /**
     * 主页接收消息
     */
    private MainViewModel mainViewModel;

    private boolean isStop = false;

    @Override
    protected ActivityMainBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityMainBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mainViewModel.setLifecycleOwner(this);
        navController = ActivityKt.findNavController(this, R.id.nav_host_fragment_activity_main);
        BottomNavigationViewKt.setupWithNavController(getBinding().navView, navController);
        mainViewModel.setISingleCallback(new ISingleCallback<Integer, Object>() {
            @Override
            public void onSingleCallback(Integer type, Object data) {
                if (type == 999) {
                    mHealthActivityManager.popAllActivity();
                }
            }
        });
    }

    @Override
    protected boolean isCanExit() {
        return true;
    }

    @Override
    public void initListener() {
        mainViewModel.setISingleCallback((type, data) -> {
            if (type == 0) {
                if (isStop) {
                    getWindow().getDecorView().post(() -> {
                        mainViewModel.makeMainTaskToFront(this);
                    });
                } else {
                    PagePilotManager.pageCalled();
                }
            }
        });
        getBinding().navView.setItemIconTintList(null);
    }

//    @RequiresApi(api = Build.VERSION_CODES.M)
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        if (hasFocus) {
//            if (!Settings.canDrawOverlays(this)) {
//                ToastUtils.INSTANCE.showToast("请给软件设置悬浮窗权限，否则收不到被叫通知！");
//                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
//                startActivity(intent);
//            }
//        }
//    }

    @Override
    protected void onStart() {
        super.onStart();
        mainViewModel.onStart();
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
        mainViewModel.onStop();
    }
}
