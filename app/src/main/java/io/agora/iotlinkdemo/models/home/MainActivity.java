package io.agora.iotlinkdemo.models.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.ActivityKt;
import androidx.navigation.NavController;
import androidx.navigation.ui.BottomNavigationViewKt;

import com.agora.baselibrary.listener.ISingleCallback;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.databinding.ActivityMainBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.home.homeindex.HomeIndexFragment;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 主页容器
 */
@Route(path = PagePathConstant.pageMainHome)
public class MainActivity extends BaseViewBindingActivity<ActivityMainBinding> {
    private static final String TAG = "LINK/MainActivity";
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
                    Log.d(TAG, "<setISingleCallback> [INCOMING] makeMainTaskToFront");
                    getWindow().getDecorView().post(() -> {
                        mainViewModel.makeMainTaskToFront(this);
                    });
                } else {
                    Log.d(TAG, "<setISingleCallback> [INCOMING] siwtch to incoming page");
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "<onRequestPermissionsResult> requestCode=" + requestCode);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        HomeIndexFragment devListFrag = (HomeIndexFragment)getFragment(HomeIndexFragment.class);
        if (devListFrag != null) {
            devListFrag.onFragRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
