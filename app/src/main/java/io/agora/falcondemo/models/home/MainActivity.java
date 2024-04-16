package io.agora.falcondemo.models.home;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.navigation.ActivityKt;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.ui.BottomNavigationViewKt;

import com.agora.baselibrary.base.BaseDialog;

import io.agora.falcondemo.R;
import io.agora.falcondemo.base.AgoraApplication;
import io.agora.falcondemo.base.BaseViewBindingActivity;
import io.agora.falcondemo.base.PushApplication;
import io.agora.falcondemo.common.Constant;
import io.agora.falcondemo.databinding.ActivityMainBinding;
import io.agora.falcondemo.dialog.CommonDialog;
import io.agora.falcondemo.models.login.AccountLoginActivity;
import io.agora.falcondemo.models.settings.AccountSecurityActivity;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.logger.ALog;


public class MainActivity extends BaseViewBindingActivity<ActivityMainBinding> {
    private static final String TAG = "IOTLINK/MainActivity";
    private NavController navController;


    private boolean mOverlyWndSetted = false;           ///< 是否已经显示过悬浮窗设置
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

        navController = ActivityKt.findNavController(this, R.id.nav_host_fragment_activity_main);
        BottomNavigationViewKt.setupWithNavController(getBinding().navView, navController);

        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {
                switch (navDestination.getId()) {
                    case R.id.navigation_home:
                        Log.d(TAG, "<initView> navigation_home");
                        PushApplication.getInstance().setUiPage(Constant.UI_PAGE_HOME); // 切换回主界面
                        break;
                    case R.id.navigation_mine:
                        Log.d(TAG, "<initView> navigation_mine");
                        PushApplication.getInstance().setUiPage(Constant.UI_PAGE_MINE); // 切换到我的界面
                        break;
                }
            }
        });

        AgoraApplication appInstance = (AgoraApplication) getApplication();
        appInstance.setMainActivity(this);

        // TODO: 不再需要检测悬浮窗权限
        //checkOverlayWndPermission();
    }

    @Override
    protected boolean isCanExit() {
        return true;
    }

    @Override
    public void initListener() {
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
        AgoraApplication appInstance = (AgoraApplication) getApplication();
        appInstance.setMainActivity(null);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "<onRequestPermissionsResult> requestCode=" + requestCode);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        HomePageFragment homePageFragment = (HomePageFragment)getFragment(HomePageFragment.class);
        if (homePageFragment != null) {
            homePageFragment.onFragRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            HomePageFragment homePageFragment = (HomePageFragment)getFragment(HomePageFragment.class);
            if (homePageFragment != null) {
                boolean respKeyDown = homePageFragment.onBackKeyEvent();
                if (respKeyDown) {  // 已经响应了 Back按键处理
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    /**
     * @brief 检测当前悬浮窗权限
     */
    void checkOverlayWndPermission() {
        if (AgoraApplication.getInstance().isChkedOverlayWnd()) {
            Log.d(TAG, "<checkOverlayWndPermission> already checked overlay window permission!");
            return;
        }

        // TODO: 检测悬浮窗权限，这个权限是否开启不影响主界面的业务流程
        AgoraApplication.getInstance().SetChkedOverlayWnd(true);
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


    /**
     * @brief 控制是否显示底部标签页栏
     */
    void setNavigatebarVisibility(int visibility) {
        getBinding().navView.setVisibility(visibility);
    }



}
