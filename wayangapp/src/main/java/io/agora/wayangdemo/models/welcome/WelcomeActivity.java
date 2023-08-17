package io.agora.wayangdemo.models.welcome;

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

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.wayangdemo.R;
import io.agora.wayangdemo.base.AgoraApplication;
import io.agora.wayangdemo.base.BaseViewBindingActivity;
import io.agora.wayangdemo.base.PermissionHandler;
import io.agora.wayangdemo.base.PermissionItem;
import io.agora.wayangdemo.base.PushApplication;
import io.agora.wayangdemo.common.Constant;
import io.agora.wayangdemo.databinding.ActivityWelcomeBinding;
import io.agora.wayangdemo.huanxin.EmAgent;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.wayangdemo.models.home.MainActivity;
import io.agora.wayangdemo.utils.AppStorageUtil;


public class WelcomeActivity extends BaseViewBindingActivity<ActivityWelcomeBinding>
        implements PermissionHandler.ICallback  {
    private static final String TAG = "IOTWY/WelcomeAct";

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
    private static final int MSGID_PERMISSION_READY = 0x1001;       ///< 权限已经申请成功
    private static final int MSGID_PERMISSION_FAILED = 0x1002;      ///< 权限申请失败


    ///////////////////////////////////////////////////////////////////////////
    ///////////////////// Variable Definition /////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    private Handler mMsgHandler = null;                 ///< 主线程中的消息处理
    private PermissionHandler mPermHandler;             ///< 权限申请处理



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
                    case MSGID_PERMISSION_READY:
                        onMessagePermissionReady();
                        break;

                    case MSGID_PERMISSION_FAILED:
                        onMessagePermissionFail();
                        break;
                }
            }
        };


        //
        //
        // Microphone权限判断处理
        //
        int[] permIdArray = new int[1];
        permIdArray[0] = PermissionHandler.PERM_ID_RECORD_AUDIO;
        mPermHandler = new PermissionHandler(this, this, permIdArray);
        if (!mPermHandler.isAllPermissionGranted()) {
            Log.d(TAG, "<initView> requesting permission...");
            mPermHandler.requestNextPermission();
        } else {
            Log.d(TAG, "<initView> permission ready");
            mMsgHandler.sendEmptyMessageDelayed(MSGID_PERMISSION_READY, 0);
        }

        Log.d(TAG, "<onCreate> <==Exit");
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "<onStart>");
        super.onStart();
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
            mMsgHandler.removeMessages(MSGID_PERMISSION_READY);
            mMsgHandler = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d(TAG, "<onKeyDown> Exit application");
            mHealthActivityManager.popAllActivity();
            System.exit(0);  // 直接退出应用
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }




    @Override
    public void onAllPermisonReqDone(boolean allGranted, final PermissionItem[] permItems) {
        Log.d(TAG, "<onAllPermisonReqDone> allGranted = " + allGranted);

        if (permItems[0].requestId == PermissionHandler.PERM_ID_CAMERA) {  // Camera权限结果
            if (allGranted) {
                mMsgHandler.sendEmptyMessageDelayed(MSGID_PERMISSION_READY, 0);
            } else {
                mMsgHandler.sendEmptyMessageDelayed(MSGID_PERMISSION_FAILED, 0);
            }

        } else if (permItems[0].requestId == PermissionHandler.PERM_ID_RECORD_AUDIO) { // 麦克风权限结果
            if (allGranted) {
                mMsgHandler.sendEmptyMessageDelayed(MSGID_PERMISSION_READY, 0);
            } else {
                mMsgHandler.sendEmptyMessageDelayed(MSGID_PERMISSION_FAILED, 0);
            }
        }
    }

    //////////////////////////////////////////////////////////////////
    //////////////////////// Methods for Login ///////////////////////
    //////////////////////////////////////////////////////////////////
    void onMessagePermissionReady() {
        gotoMainActivity();
    }

    void onMessagePermissionFail() {
        popupMessage(getString(R.string.no_permission));

        mMsgHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "<onMessagePermissionFail> Exit application");
                        mHealthActivityManager.popAllActivity();
                        System.exit(0);  // 直接退出应用
                    }
                }, 3000);
    }


    void gotoMainActivity() {
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // 清除Activity堆栈
        startActivity(intent);
    }

}
