package io.agora.iotlinkdemo.models.welcome;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.SPUtil;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityWelcomeBinding;
import io.agora.iotlinkdemo.dialog.CommonDialog;
import io.agora.iotlinkdemo.dialog.UserAgreementDialog;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.login.LoginViewModel;
import io.agora.iotlink.AIotAppSdkFactory;

public class WelcomeActivity extends BaseViewBindingActivity<ActivityWelcomeBinding> {
    private static final String TAG = "LINK/WelcomeAct";

    //
    // 界面流程状态
    //
    private static final int UI_STATE_IDLE = 0x0000;        ///< 空闲状态
    private static final int UI_STATE_USRAGREE = 0x0001;    ///< 隐私协议
    private static final int UI_STATE_OVERLAYWND = 0x0002;  ///< 检测悬浮窗权限
    private static final int UI_STATE_LOGIN = 0x0003;       ///< 处理登录


    //
    // message Id
    //
    private static final int MSGID_CHECK_USRAGREE = 0x1001;    ///< 检测隐私协议是否同意
    private static final int MSGID_CHECK_OVERLAYWND = 0x1002;  ///< 检测悬浮窗权限
    private static final int MSGID_HANDLE_LOGIN = 0x1003;      ///< 处理登录



    ///////////////////////////////////////////////////////////////////////////
    ///////////////////// Variable Definition /////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private UserAgreementDialog userAgreementDialog;    ///< 隐私协议对话框
    private LoginViewModel phoneLoginViewModel;         ///< 登录模块统一ViewModel

    private Handler mMsgHandler = null;                 ///< 主线程中的消息处理
    private int mUiState = UI_STATE_IDLE;               ///< 当前UI处理状态机
    private boolean mOverlyWndSetted = false;           ///< 是否已经显示过悬浮窗设置




    ////////////////////////////////////////////////////////////////////////////
    ///////////////// Override BaseViewBindingActivity Methods /////////////////
    ////////////////////////////////////////////////////////////////////////////
     @Override
    protected ActivityWelcomeBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityWelcomeBinding.inflate(inflater);
    }

    @Override
    public boolean isBlackDarkStatus() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "<onCreate>");

        // 初始化SDK引擎
        AgoraApplication appInstance = (AgoraApplication) getApplication();
        appInstance.initializeEngine();

        // 创建登录处理模型
        phoneLoginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        phoneLoginViewModel.setLifecycleOwner(this);
        phoneLoginViewModel.setISingleCallback((var1, var2) -> {
            hideLoadingView();
            onCallbackLoginDone(var1, (String)var2);
        });

        // 创建主线程消息处理
        mMsgHandler = new Handler(getMainLooper())
        {
            @SuppressLint("HandlerLeak")
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case MSGID_CHECK_USRAGREE:
                        onMsgCheckUserAgreement();
                        break;

                    case MSGID_CHECK_OVERLAYWND:
                        onMsgCheckOverlayWnd();
                        break;

                    case MSGID_HANDLE_LOGIN:
                        onMsgHandleLogin();
                        break;
                }
            }
        };

        // 开始处理用户同意
        mUiState = UI_STATE_IDLE;
        mMsgHandler.sendEmptyMessageDelayed(MSGID_CHECK_USRAGREE, 200);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "<onStart>");
        super.onStart();
        phoneLoginViewModel.onStart();

        if ((mUiState == UI_STATE_OVERLAYWND) && (mOverlyWndSetted)) {
            mMsgHandler.sendEmptyMessage(MSGID_HANDLE_LOGIN);
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "<onStop>");
        super.onStop();
        phoneLoginViewModel.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "<onDestory>");
        super.onDestroy();

        if (mMsgHandler != null) {
            mMsgHandler.removeMessages(MSGID_CHECK_USRAGREE);
            mMsgHandler.removeMessages(MSGID_CHECK_OVERLAYWND);
            mMsgHandler.removeMessages(MSGID_HANDLE_LOGIN);
            mMsgHandler = null;
        }
    }


    ////////////////////////////////////////////////////////////////////
    ////////////////// Methods for User Agreement /////////////////////
    ///////////////////////////////////////////////////////////////////
    void onMsgCheckUserAgreement() {
        mUiState = UI_STATE_USRAGREE;
        if (!SPUtil.Companion.getInstance(WelcomeActivity.this).getBoolean(Constant.IS_AGREE, false)) {
            showUserAgreementDialog();
        } else {
            mMsgHandler.sendEmptyMessage(MSGID_CHECK_OVERLAYWND);
        }
    }

    /**
     * @brief 显示用户协议 隐私政策对话框
     */
    private void showUserAgreementDialog() {
        if (userAgreementDialog == null) {
            userAgreementDialog = new UserAgreementDialog(this);
            userAgreementDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {   // 不同意
                    userAgreementDialog.dismiss();
                    mHealthActivityManager.popAllActivity();
                    System.exit(0);  // 直接退出应用
                }

                @Override
                public void onRightButtonClick() {  // 同意
                    userAgreementDialog.dismiss();
                    SPUtil.Companion.getInstance(WelcomeActivity.this).putBoolean(Constant.IS_AGREE, true);
                    mMsgHandler.sendEmptyMessage(MSGID_CHECK_OVERLAYWND);
                }
            });
        }
        userAgreementDialog.show();
    }

    //////////////////////////////////////////////////////////////////
    /////////////// Methods for Overlay Wnd Permission ///////////////
    //////////////////////////////////////////////////////////////////
    @RequiresApi(api = Build.VERSION_CODES.M)
    void onMsgCheckOverlayWnd() {
        // TODO: 只有需要离线推送功能时，才需要申请悬浮窗权限，否则可以跳过

//        mUiState = UI_STATE_OVERLAYWND;
//        mOverlyWndSetted = false;
//        if (!Settings.canDrawOverlays(this)) {
//            showRequestSuspensionDialog();
//        } else {
//            mMsgHandler.sendEmptyMessage(MSGID_HANDLE_LOGIN);
//        }

        mMsgHandler.sendEmptyMessage(MSGID_HANDLE_LOGIN);
    }

    /**
     * @brief 获取悬浮窗权限提示
     */
    public void showRequestSuspensionDialog() {
        if (commonDialog == null) {
            commonDialog = new CommonDialog(this);
            commonDialog.setDialogTitle("请给软件设置悬浮窗权限，否则收不到被叫通知！");
            commonDialog.setDialogBtnText(getString(R.string.cancel), getString(R.string.confirm));
            commonDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {
                    mMsgHandler.sendEmptyMessage(MSGID_HANDLE_LOGIN);
                }

                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onRightButtonClick() {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    startActivity(intent);
                    mOverlyWndSetted = true;
                }
            });
        }
        commonDialog.show();
    }


    //////////////////////////////////////////////////////////////////
    //////////////////////// Methods for Login ///////////////////////
    //////////////////////////////////////////////////////////////////
    void onMsgHandleLogin() {
        mUiState = UI_STATE_LOGIN;

        String account = AIotAppSdkFactory.getInstance().getAccountMgr().getLoggedAccount();
        if (!TextUtils.isEmpty(account)) {  // 当前账号已经登录，直接跳转到主界面
            Log.d(TAG, "<onMsgHandleLogin> account already login, goto pageMainHome");
            PagePilotManager.pageMainHome();
            return;
        }

        String storedAccount = SPUtil.Companion.getInstance(this).getString(Constant.ACCOUNT, null);
        String storedPassword = SPUtil.Companion.getInstance(this).getString(Constant.PASSWORD, null);
        if (TextUtils.isEmpty(storedAccount) || TextUtils.isEmpty(storedPassword)) { // 没有历史登录信息
            Log.d(TAG, "<onMsgHandleLogin> No login history, goto pagePhoneLogin");
            PagePilotManager.pagePhoneLogin();
            return;
        }


        phoneLoginViewModel.requestLogin(storedAccount, storedPassword);


    }

    void onCallbackLoginDone(final int errCode, final String account) {
        Log.d(TAG, "<onCallbackLoginDone> errCode=" + errCode);
        if (errCode == Constant.CALLBACK_TYPE_LOGIN_REQUEST_LOGIN_SUCCESS) {
            // 自动登录成功直接跳转到主界面
            PagePilotManager.pageMainHome();

        } else if (errCode == Constant.CALLBACK_TYPE_LOGIN_REQUEST_LOGIN_FAIL) {
            // 自动登录失败跳转到登录界面
            PagePilotManager.pagePhoneLogin();
        }
    }







//    @RequiresApi(api = Build.VERSION_CODES.M)
//    @Override
//    public void getPermissions() {
//        AgoraApplication appInstance = (AgoraApplication) getApplication();
//        appInstance.initializeEngine();
//        phoneLoginViewModel.onStart();
//        if (!Settings.canDrawOverlays(this)) {
//            showRequestSuspensionDialog();
//        } else {
//            checkStatusToStart();
//        }
//    }

//    private void checkStatusToStart() {
//        if (!SPUtil.Companion.getInstance(WelcomeActivity.this).getBoolean(Constant.IS_AGREE, false)) {
//            showUserAgreementDialog();
//        } else {
//            startMainActivity();
//        }
//    }
//



//
//    private void startMainActivity() {
//        String account = AIotAppSdkFactory.getInstance().getAccountMgr().getLoggedAccount();
//        if (TextUtils.isEmpty(account)) {
//            account = SPUtil.Companion.getInstance(this).getString(Constant.ACCOUNT, null);
//            if (!TextUtils.isEmpty(account)) {
//                String password = SPUtil.Companion.getInstance(this).getString(Constant.PASSWORD, null);
//                if (!TextUtils.isEmpty(password)) {
//                    phoneLoginViewModel.requestLogin(account, password);
//                }
//            } else {
//                PagePilotManager.pagePhoneLogin();
//            }
//        } else {
//            PagePilotManager.pageMainHome();
//        }
//
//        phoneLoginViewModel.onStop();
//    }



}
