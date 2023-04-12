package io.agora.iotlinkdemo.models.home;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.agora.baselibrary.base.BaseViewModel;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.event.UserLogoutEvent;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.player.called.CalledInComingActivity;
import io.agora.iotlinkdemo.models.player.living.PlayerPreviewActivity;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.IAccountMgr;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.IDeviceMgr;
import io.agora.iotlink.IotDevice;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * 主页接收消息 viewModel
 */
public class MainViewModel extends BaseViewModel implements ICallkitMgr.ICallback, IAccountMgr.ICallback {
    private final static String TAG = "LINK/MainViewModel";

    public void onStart() {
        // 注册管理监听
        ICallkitMgr callkitMgr= AIotAppSdkFactory.getInstance().getCallkitMgr();
        if (callkitMgr != null) {
            callkitMgr.registerListener(this);
        }
        IAccountMgr accountMgr = AIotAppSdkFactory.getInstance().getAccountMgr();
        if (accountMgr != null) {
            accountMgr.registerListener(this);
        }
    }

    public void onStop() {
        // 注销管理监听
        ICallkitMgr callkitMgr= AIotAppSdkFactory.getInstance().getCallkitMgr();
        if (callkitMgr != null) {
            callkitMgr.unregisterListener(this);
        }
        IAccountMgr accountMgr = AIotAppSdkFactory.getInstance().getAccountMgr();
        if (accountMgr != null) {
            accountMgr.unregisterListener(this);
        }
    }

    /**
     * 对端设备来电事件
     *
     * @param iotDevice  : 来电的设备
     * @param attachMsg: 来电时附带信息
     */
    @Override
    public void onPeerIncoming(IotDevice iotDevice, String attachMsg) {
        Log.d(TAG, "<onPeerIncoming> iotDevice=" + iotDevice.mDeviceName
                    + ", attachMsg=" + attachMsg);
        AgoraApplication.getInstance().setLivingDevice(iotDevice);
        getISingleCallback().onSingleCallback(0, null);
    }

    public void makeMainTaskToFront(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTasks = manager.getRunningTasks(20);
        for (ActivityManager.RunningTaskInfo taskInfo : runningTasks) {
            Log.d(TAG, "<makeMainTaskToFront> taskInfo.baseActivity.getPackageName() = " + taskInfo.baseActivity.getPackageName());
            //判断是否是相同的包名
            if (taskInfo.baseActivity.getPackageName().equals(AgoraApplication.mInstance.getPackageName())) {
                int taskId;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    taskId = taskInfo.taskId;
                } else {
                    taskId = taskInfo.id;
                }
                manager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME);
                Intent intent = new Intent(context, CalledInComingActivity.class);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setAction(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(intent);
//                Intent intent = new Intent();
//                intent.setComponent(new ComponentName(AgoraApplication.mInstance.getPackageName(),
//                        "io.agora.iotlinkdemo.models.player.called.CalledInComingActivity"));
//                context.startActivity(intent);
                Log.d(TAG, "<makeMainTaskToFront> taskId =" + taskId);
                return;
            }
        }
    }

    @Override
    public void onLoginOtherDevice(String account) {
        Log.d(TAG, "<onLoginOtherDevice> account = " + account);
        if (account.equals(AIotAppSdkFactory.getInstance().getAccountMgr().getLoggedAccount())) {
            EventBus.getDefault().post(new UserLogoutEvent());
            getISingleCallback().onSingleCallback(999, null);
            PagePilotManager.pagePhoneLogin();
        }
    }

    @Override
    public void onTokenInvalid() {
        EventBus.getDefault().post(new UserLogoutEvent());
        getISingleCallback().onSingleCallback(999, null);
        PagePilotManager.pagePhoneLogin();
    }

    @Override
    public void onMqttStateChanged(int mqttState) {
        boolean bMqttReady = AIotAppSdkFactory.getInstance().isAwsMqttReady();
        Log.d(TAG, "<onMqttStateChanged> mqttState=" + mqttState
                + ", bMqttReady=" + bMqttReady);
    }

    @Override
    public void onMqttError(final String errMessage) {
        Log.d(TAG, "<onMqttError> errMessage=" + errMessage);
    }


}
