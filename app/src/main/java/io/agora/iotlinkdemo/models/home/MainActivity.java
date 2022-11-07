package io.agora.iotlinkdemo.models.home;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.ActivityKt;
import androidx.navigation.NavController;
import androidx.navigation.ui.BottomNavigationViewKt;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.listener.ISingleCallback;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.base.PermissionItem;
import io.agora.iotlinkdemo.databinding.ActivityMainBinding;
import io.agora.iotlinkdemo.dialog.CommonDialog;
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
    private boolean mOverlyWndSetted = false;           ///< 是否已经显示过悬浮窗设置
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

        // 检测悬浮窗权限
        checkOverlayWndPermission();
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

    /**
     * @brief 检测当前悬浮窗权限
     */
    void checkOverlayWndPermission() {
        // TODO: 检测悬浮窗权限，这个权限是否开启不影响主界面的业务流程
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mOverlyWndSetted = false;
            if (!Settings.canDrawOverlays(this)) {
                new android.os.Handler(Looper.getMainLooper()).postDelayed(
                        new Runnable() {
                            public void run() {
                                showRequestSuspensionDialog();
                            }
                        },
                        500);
            } else {
                Log.d(TAG, "<CheckOverlayWndPermission> already have overlay permission");
            }
        } else {
            Log.d(TAG, "<CheckOverlayWndPermission> Low Android version");
        }
    }

    /**
     * @brief 获取悬浮窗权限提示
     */
    public void showRequestSuspensionDialog() {
        Log.d(TAG, "<showRequestSuspensionDialog> request FloatWnd permission");
        if (commonDialog == null) {
            commonDialog = new CommonDialog(this);
            commonDialog.setDialogTitle("请给软件设置悬浮窗权限，否则收不到被叫通知！");
            commonDialog.setDialogBtnText(getString(R.string.cancel), getString(R.string.confirm));
            commonDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {
                    Log.d(TAG, "<showRequestSuspensionDialog> onLeftButtonClick");
                    //mMsgHandler.sendEmptyMessage(MSGID_HANDLE_LOGIN);
                }

                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onRightButtonClick() {
                    Log.d(TAG, "<showRequestSuspensionDialog> onRightButtonClick");
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    startActivity(intent);
                    mOverlyWndSetted = true;
                }
            });
        }
        commonDialog.show();
    }

}
