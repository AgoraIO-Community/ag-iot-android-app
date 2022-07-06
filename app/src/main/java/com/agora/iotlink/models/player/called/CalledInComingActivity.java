package com.agora.iotlink.models.player.called;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.utils.StringUtils;
import com.agora.baselibrary.utils.ToastUtils;
import com.agora.iotlink.R;
import com.agora.iotlink.base.AgoraApplication;
import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.base.PermissionHandler;
import com.agora.iotlink.base.PermissionItem;
import com.agora.iotlink.common.Constant;
import com.agora.iotlink.databinding.ActivityCalledBinding;
import com.agora.iotlink.dialog.ChangeOfVoiceDialog;
import com.agora.iotlink.manager.PagePathConstant;
import com.agora.iotlink.manager.PagePilotManager;
import com.agora.iotsdk20.ICallkitMgr;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 被叫
 */
@Route(path = PagePathConstant.pageCalled)
public class CalledInComingActivity extends BaseViewBindingActivity<ActivityCalledBinding>
    implements PermissionHandler.ICallback  {
    private static final String TAG = "LINK/CallInComeAct";
    private CalledInComingViewModel calledInComingViewModel;

    /**
     * 显示变声选择对话框
     */
    private ChangeOfVoiceDialog changeOfVoiceDialog;

    private boolean isCallHangup = false;

    private PermissionHandler mPermHandler;             ///< 权限申请处理

    @Override
    protected ActivityCalledBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityCalledBinding.inflate(inflater);
    }


    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        calledInComingViewModel = new ViewModelProvider(this).get(CalledInComingViewModel.class);
        calledInComingViewModel.setLifecycleOwner(this);
        calledInComingViewModel.setPeerVideoView(getBinding().svDeviceView);
        getBinding().titleView.setTitle(StringUtils.INSTANCE.getBase64String(AgoraApplication.getInstance().getLivingDevice().mDeviceName));
        getBinding().tvDeviceNameValue.setText(AgoraApplication.getInstance().getLivingDevice().mDeviceName);
    }

    @Override
    public void initListener() {
        calledInComingViewModel.setISingleCallback((type, data) -> {
            if (type == Constant.CALLBACK_TYPE_PLAYER_CALL_HANG_UP) {
                ToastUtils.INSTANCE.showToast("对方挂断");
                mHealthActivityManager.popActivity();
            }
        });
        getBinding().tvAnswer.setOnClickListener(view -> {
            onBtnAnswer();
        });
        getBinding().tvRingOff.setOnClickListener(view -> {
            isCallHangup = true;
            calledInComingViewModel.callHangup();
            mHealthActivityManager.popActivity();
        });
        getBinding().tvChangeVoice.setOnClickListener(view -> {
            showChangeOfVoiceDialog();
        });
    }

    private void showChangeOfVoiceDialog() {
        if (changeOfVoiceDialog == null) {
            changeOfVoiceDialog = new ChangeOfVoiceDialog(this);
            changeOfVoiceDialog.iSingleCallback = (type, var2) -> {
                if (type == 1) {
                    calledInComingViewModel.setAudioEffect(ICallkitMgr.AudioEffectId.NORMAL);
                    getBinding().tvChangeVoice.setSelected(false);
                } else {
                    if (type == 2) {
                        calledInComingViewModel.setAudioEffect(ICallkitMgr.AudioEffectId.OLDMAN);
                    } else if (type == 3) {
                        calledInComingViewModel.setAudioEffect(ICallkitMgr.AudioEffectId.BABYBOY);
                    } else if (type == 4) {
                        calledInComingViewModel.setAudioEffect(ICallkitMgr.AudioEffectId.BABYGIRL);
                    }
                    getBinding().tvChangeVoice.setSelected(true);
                }
            };
        }
        changeOfVoiceDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        calledInComingViewModel.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isCallHangup) {
            calledInComingViewModel.callHangup();
        }
        calledInComingViewModel.onStop();
    }


    void onBtnAnswer() {
        //
        // RECORD_AUDIO 权限判断处理
        //
        int[] permIdArray = new int[1];
        permIdArray[0] = PermissionHandler.PERM_ID_RECORD_AUDIO;
        mPermHandler = new PermissionHandler(this, this, permIdArray);
        if (!mPermHandler.isAllPermissionGranted()) {
            Log.d(TAG, "<onBtnAnswer> requesting permission...");
            mPermHandler.requestNextPermission();

        } else {
            Log.d(TAG, "<onBtnAnswer> permission granted, goto WIFI activity");
            isCallHangup = true;
            calledInComingViewModel.callAnswer();
            PagePilotManager.pagePreviewPlay();
            mHealthActivityManager.popActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "<onRequestPermissionsResult> requestCode=" + requestCode);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mPermHandler != null) {
            mPermHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onAllPermisonReqDone(boolean allGranted, final PermissionItem[] permItems) {
        Log.d(TAG, "<onAllPermisonReqDone> allGranted = " + allGranted);

        if (allGranted) {
            isCallHangup = true;
            calledInComingViewModel.callAnswer();
            PagePilotManager.pagePreviewPlay();
            mHealthActivityManager.popActivity();

        } else {
            popupMessage(getString(R.string.no_permission));
        }
    }
}
