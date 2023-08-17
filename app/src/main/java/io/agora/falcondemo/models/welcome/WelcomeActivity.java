package io.agora.falcondemo.models.welcome;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.SPUtil;

import java.util.List;

import io.agora.falcondemo.dialog.DialogInputAppId;
import io.agora.falcondemo.dialog.DialogNewDevice;
import io.agora.falcondemo.models.home.DeviceInfo;
import io.agora.falcondemo.models.home.DeviceListAdapter;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.falcondemo.R;
import io.agora.falcondemo.base.AgoraApplication;
import io.agora.falcondemo.base.BaseViewBindingActivity;
import io.agora.falcondemo.base.PushApplication;
import io.agora.falcondemo.common.Constant;
import io.agora.falcondemo.databinding.ActivityWelcomeBinding;
import io.agora.falcondemo.dialog.CommonDialog;
import io.agora.falcondemo.dialog.UserAgreementDialog;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.falcondemo.models.home.MainActivity;
import io.agora.falcondemo.models.login.AccountLoginActivity;
import io.agora.falcondemo.utils.AppStorageUtil;


public class WelcomeActivity extends BaseViewBindingActivity<ActivityWelcomeBinding> {
    private static final String TAG = "IOTLINK/WelcomeAct";

    //
    // 界面流程状态
    //
    private static final int UI_STATE_IDLE = 0x0000;        ///< 空闲状态
    private static final int UI_STATE_INIT = 0x0001;        ///< 正在初始化
    private static final int UI_STATE_USRAGREE = 0x0002;    ///< 隐私协议
    private static final int UI_STATE_OVERLAYWND = 0x0003;  ///< 检测悬浮窗权限
    private static final int UI_STATE_LOGIN = 0x0004;       ///< 处理登录


    //
    // message Id
    //
    private static final int MSGID_ENGINE_INIT = 0x1001;       ///< 初始化离线推送和整个SDK
    private static final int MSGID_CHECK_USRAGREE = 0x1002;    ///< 检测隐私协议是否同意
    private static final int MSGID_HANDLE_LOGIN = 0x1003;      ///< 处理登录



    ///////////////////////////////////////////////////////////////////////////
    ///////////////////// Variable Definition /////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private UserAgreementDialog userAgreementDialog;    ///< 隐私协议对话框

    private Handler mMsgHandler = null;                 ///< 主线程中的消息处理
    private int mUiState = UI_STATE_IDLE;               ///< 当前UI处理状态机
    private boolean mOverlyWndSetted = false;           ///< 是否已经显示过悬浮窗设置
    private boolean mUserAgreePrivacy = true;           ///< 用户同意隐私协议



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
        Log.d(TAG, "<onCreate> ==>Enter");

        // 创建主线程消息处理
        mMsgHandler = new Handler(getMainLooper())
        {
            @SuppressLint("HandlerLeak")
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case MSGID_ENGINE_INIT:
                        onMsgEngineInitialie();
                        break;

                    case MSGID_CHECK_USRAGREE:
                        onMsgCheckUserAgreement();
                        break;

                    case MSGID_HANDLE_LOGIN:
                        onMsgHandleLogin();
                        break;
                }
            }
        };

        // 开始初始化引擎
        mUiState = UI_STATE_IDLE;
        mMsgHandler.sendEmptyMessageDelayed(MSGID_ENGINE_INIT, 100);

        Log.d(TAG, "<onCreate> <==Exit");
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "<onStart>");
        super.onStart();


        if ((mUiState == UI_STATE_OVERLAYWND) && (mOverlyWndSetted)) {
            mMsgHandler.sendEmptyMessage(MSGID_HANDLE_LOGIN);
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "<onStop>");
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "<onDestory>");
        super.onDestroy();

        if (mMsgHandler != null) {
            mMsgHandler.removeMessages(MSGID_CHECK_USRAGREE);
            mMsgHandler.removeMessages(MSGID_ENGINE_INIT);
            mMsgHandler.removeMessages(MSGID_HANDLE_LOGIN);
            mMsgHandler = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!mUserAgreePrivacy) {
                Log.d(TAG, "<onKeyDown> Exit application");
                mHealthActivityManager.popAllActivity();
                System.exit(0);  // 直接退出应用
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    ////////////////////////////////////////////////////////////////////
    ////////////////// Methods for Engine Initialize ///////////////////
    ///////////////////////////////////////////////////////////////////
    void onMsgEngineInitialie() {
        Log.d(TAG, "<onMsgEngineInitialie> ==>Enter");

        String appId = AppStorageUtil.safeGetString(this, Constant.APP_ID, null);
        if (TextUtils.isEmpty(appId)) {  // 当前没有appId保存，需要用户输入

            DialogInputAppId inAppIdDlg = new DialogInputAppId(this);
            inAppIdDlg.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {
                }

                @Override
                public void onRightButtonClick() {
                }
            });

            inAppIdDlg.mSingleCallback = (integer, obj) -> {
                String inputAppId = (String)obj;
                AppStorageUtil.safePutString(this, Constant.APP_ID, inputAppId);
                doAIotSdkInitialize(inputAppId);
            };
            inAppIdDlg.setCanceledOnTouchOutside(false);
            inAppIdDlg.show();

        } else {
            doAIotSdkInitialize(appId);
        }

        Log.d(TAG, "<onMsgEngineInitialie> <==Exit");
    }

    void doAIotSdkInitialize(final String appId) {
        // 初始化SDK引擎
        AgoraApplication appInstance = (AgoraApplication) getApplication();
        appInstance.initializeEngine(appId);

        // 开始处理隐私协议
        mUiState = UI_STATE_USRAGREE;
        mMsgHandler.sendEmptyMessageDelayed(MSGID_CHECK_USRAGREE, 100);
        Log.d(TAG, "<doAIotSdkInitialize> done");
    }

    ////////////////////////////////////////////////////////////////////
    ////////////////// Methods for User Agreement /////////////////////
    ///////////////////////////////////////////////////////////////////
    void onMsgCheckUserAgreement() {
        mUiState = UI_STATE_USRAGREE;
        if (!SPUtil.Companion.getInstance(WelcomeActivity.this).getBoolean(Constant.IS_AGREE, false)) {
            showUserAgreementDialog();
        } else {
            mMsgHandler.sendEmptyMessage(MSGID_HANDLE_LOGIN);
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
                    mUserAgreePrivacy = false;  // 用户不同意隐私协议
                }

                @Override
                public void onRightButtonClick() {  // 同意
                    userAgreementDialog.dismiss();
                    SPUtil.Companion.getInstance(WelcomeActivity.this).putBoolean(Constant.IS_AGREE, true);
                    mMsgHandler.sendEmptyMessage(MSGID_HANDLE_LOGIN);
                }
            });
        }
        userAgreementDialog.show();
    }


    //////////////////////////////////////////////////////////////////
    //////////////////////// Methods for Login ///////////////////////
    //////////////////////////////////////////////////////////////////
    void onMsgHandleLogin() {
        mUiState = UI_STATE_LOGIN;

//        int sdkState = AIotAppSdkFactory.getInstance().getStateMachine();
//        if (sdkState == IAgoraIotAppSdk.SDK_STATE_RUNNING) {  // 当前账号已经登录，直接跳转到主界面
//            Log.d(TAG, "<onMsgHandleLogin> account already login, goto main");
//            gotoMainActivity();
//            return;
//        }
//
//        String storedAccount = AppStorageUtil.safeGetString(this, Constant.ACCOUNT, null);
//        String storedPassword = AppStorageUtil.safeGetString(this, Constant.PASSWORD, null);
//        if (TextUtils.isEmpty(storedAccount) || TextUtils.isEmpty(storedPassword)) { // 没有历史登录信息
//            Log.d(TAG, "<onMsgHandleLogin> No login history, goto loginActivity");
//            gotoLoginActivity();
//            return;
//        }
//
//
//        // 开始进行登录操作
//        IAgoraIotAppSdk.PrepareParam prepareParam = new IAgoraIotAppSdk.PrepareParam();
//        prepareParam.mUserId = storedAccount;
//        prepareParam.mClientType = 2;
//        int ret = AIotAppSdkFactory.getInstance().prepare(prepareParam, new IAgoraIotAppSdk.OnPrepareListener() {
//            @Override
//            public void onSdkPrepareDone(IAgoraIotAppSdk.PrepareParam prepareParam, int errCode) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        onCallbackLoginDone(errCode);
//                    }
//                });
//            }
//        });
//
//        if (ret != ErrCode.XOK) {   // 登录失败，切换到登录界面
//            Log.e(TAG, "<onMsgHandleLogin> fail to prepare(), ret=" + ret);
//            gotoLoginActivity();
//        }
//
        gotoLoginActivity();
    }

    void onCallbackLoginDone(final int errCode) {
        Log.d(TAG, "<onCallbackLoginDone> errCode=" + errCode);
        if (errCode == ErrCode.XOK) {        // 自动登录成功直接跳转到主界面
            gotoMainActivity();

        } else {   // 自动登录失败跳转到登录界面
            gotoLoginActivity();
        }
    }

    void gotoLoginActivity() {
        Intent intent = new Intent(WelcomeActivity.this, AccountLoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // 清除Activity堆栈
        startActivity(intent);
    }

    void gotoMainActivity() {
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // 清除Activity堆栈
        startActivity(intent);
    }

}
