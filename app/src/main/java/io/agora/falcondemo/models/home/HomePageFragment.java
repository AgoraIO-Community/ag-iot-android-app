package io.agora.falcondemo.models.home;

import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.agora.baselibrary.base.BaseDialog;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import io.agora.falcondemo.dialog.DialogInputCommand;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.ICallkitMgr;
import io.agora.falcondemo.R;
import io.agora.falcondemo.base.BaseViewBindingFragment;
import io.agora.falcondemo.base.PermissionHandler;
import io.agora.falcondemo.base.PermissionItem;
import io.agora.falcondemo.base.PushApplication;
import io.agora.falcondemo.databinding.FragmentHomePageBinding;
import io.agora.falcondemo.dialog.DialogNewDevice;
import io.agora.falcondemo.models.player.DevPreviewActivity;
import io.agora.falcondemo.utils.AppStorageUtil;
import io.agora.falcondemo.utils.FileUtils;
import io.agora.iotlink.rtcsdk.TalkingEngine;


public class HomePageFragment extends BaseViewBindingFragment<FragmentHomePageBinding>
        implements PermissionHandler.ICallback, ICallkitMgr.ICallback  {
    private static final String TAG = "IOTLINK/HomePageFrag";


    private PermissionHandler mPermHandler;             ///< 权限申请处理

    private MainActivity mMainActivity;
    private DeviceListAdapter mDevListAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private AlertDialog mAnswerRjectDlg = null;


    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseFragment /////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    @Override
    protected FragmentHomePageBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomePageBinding.inflate(inflater);
    }

    @Override
    public void initView() {
        mMainActivity = (MainActivity)getActivity();
        //
        // 初始化设备列表
        //
        List<DeviceInfo> deviceList = deviceListLoad();

        if (mDevListAdapter == null) {
            mDevListAdapter = new DeviceListAdapter(deviceList);
            mDevListAdapter.setOwner(this);
            mDevListAdapter.setRecycleView(getBinding().rvDeviceList);
            getBinding().rvDeviceList.setLayoutManager(new LinearLayoutManager(getActivity()));
            getBinding().rvDeviceList.setAdapter(mDevListAdapter);
            mDevListAdapter.setMRVItemClickListener((view, position, data) -> {
            });
        }
        mSwipeRefreshLayout = getBinding().srlDevList;

        getBinding().titleView.hideLeftImage();

        //
        //
        // Microphone权限判断处理
        //
        int[] permIdArray = new int[1];
        permIdArray[0] = PermissionHandler.PERM_ID_RECORD_AUDIO;
        mPermHandler = new PermissionHandler(getActivity(), this, permIdArray);
        if (!mPermHandler.isAllPermissionGranted()) {
            Log.d(TAG, "<initView> requesting permission...");
            mPermHandler.requestNextPermission();
        } else {
            Log.d(TAG, "<initView> permission ready");

        }

        Log.d(TAG, "<initView> done");
    }

    @Override
    public void initListener() {
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(false));
        });

        getBinding().titleView.setRightIconClick(view -> {
            onBtnDeviceMgr(view);
        });

        getBinding().cbAllSelect.setOnClickListener(view -> {
            onBtnSelectAll(view);
        });

        getBinding().btnDoDelete.setOnClickListener(view -> {
            onBtnDelete(view);
        });

        Log.d(TAG, "<initListener> done");
    }

    public void onFragRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "<onFragRequestPermissionsResult> requestCode=" + requestCode);
        if (mPermHandler != null) {
            mPermHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public void onAllPermisonReqDone(boolean allGranted, final PermissionItem[] permItems) {
        Log.d(TAG, "<onAllPermisonReqDone> allGranted = " + allGranted);

        if (permItems[0].requestId == PermissionHandler.PERM_ID_CAMERA) {  // Camera权限结果
            if (allGranted) {
                //PagePilotManager.pageDeviceAddScanning();
            } else {
                popupMessage(getString(R.string.no_permission));
            }

        } else if (permItems[0].requestId == PermissionHandler.PERM_ID_RECORD_AUDIO) { // 麦克风权限结果
            if (allGranted) {
            //    doCallDial(mSelectedDev);
            } else {
                popupMessage(getString(R.string.no_permission));
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "<onStart> ");

        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        callkitMgr.registerListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "<onStop> ");

        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        callkitMgr.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "<onResume> ");

        // 当从全屏播放界面返回来时，要重新设置各个设备的视频播放控件
        UUID sessionId = PushApplication.getInstance().getFullscrnSessionId();
        if (sessionId != null) {
            resetDeviceDisplayView(sessionId);
            PushApplication.getInstance().setFullscrnSessionId(null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "<onPause> ");
    }

    /**
     * @brief 响应 Back按键处理
     * @return 如果 Fragment已经处理返回 true; 否则返回false
     */
    boolean onBackKeyEvent() {
        if (mDevListAdapter.isInSelectMode()) {
            // 切换回非选择模式
            switchSelectMode(false);
            return true;
        }

        return false;
    }

    /**
     * @brief 选择模式 / 非选择模式 相互切换
     */
    void switchSelectMode(boolean selectMode) {

        if (selectMode) {
            // 切换到选择模式
            mDevListAdapter.switchSelectMode(true);
            getBinding().cbAllSelect.setChecked(false);
            mMainActivity.setNavigatebarVisibility(View.GONE);
            getBinding().clBottomDel.setVisibility(View.VISIBLE);
            getBinding().clBottomDel.invalidate();

        } else {
            // 切换到非选择模式
            mDevListAdapter.switchSelectMode(false);
            getBinding().cbAllSelect.setChecked(false);
            mMainActivity.setNavigatebarVisibility(View.VISIBLE);
            getBinding().clBottomDel.setVisibility(View.GONE);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////// Event & Widget Methods  ///////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * @brief 设备管理
     */
    void onBtnDeviceMgr(View view) {
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        ICallkitMgr.AudioEffectId currAudioEffectId = callkitMgr.getAudioEffect();
        String effectName = getAudioEffectName(currAudioEffectId);

        PopupMenu deviceMenu = new PopupMenu(getActivity(), view);
        getActivity().getMenuInflater().inflate(R.menu.menu_device, deviceMenu.getMenu());
        MenuItem menuItem = deviceMenu.getMenu().getItem(2);
        String title = "通话音效 (" + effectName + ")";
        menuItem.setTitle(title);

        deviceMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.m_device_add:
                        onMenuAddDevice();
                        break;

                    case R.id.m_device_remove:
                        onMenuRemoveDevice();
                        break;

                    case R.id.m_talk_effect:
                        onMenuTalkEffect();
                        break;

                }
                return true;
            }
        });
        deviceMenu.show();
    }

    /**
     * @brief 全选按钮点击事件
     */
    void onBtnSelectAll(View view) {
        boolean allChecked = getBinding().cbAllSelect.isChecked();
        if (allChecked) {
            mDevListAdapter.setAllItemsSelectStatus(true);
        } else {
            mDevListAdapter.setAllItemsSelectStatus(false);
        }
    }


    /**
     * @brief 删除按钮点击事件
     */
    void onBtnDelete(View view) {
        List<DeviceInfo> selectedList = mDevListAdapter.getSelectedItems();
        int selectedCount = selectedList.size();
        if (selectedCount <= 0) {
            popupMessage("Please select one device at least!");
            return;
        }

        showLoadingView();

        // 挂断所有要删除的通话
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        for (int i = 0; i < selectedCount; i++) {
            DeviceInfo deviceInfo = selectedList.get(i);
            if (deviceInfo.mSessionId != null) {    // 要删除的设备进行挂断操作
                callkitMgr.callHangup(deviceInfo.mSessionId);
            }
        }

        // 获取所有剩余的设备列表
        int deleteCount = mDevListAdapter.deleteSelectedItems();

        // 切换回 非选择模式
        switchSelectMode(false);

        // 保存新的设备列表
        List<DeviceInfo> deviceInfoList = mDevListAdapter.getDatas();
        deviceListStore(deviceInfoList);

        hideLoadingView();

        popupMessage("Total " + selectedCount + " devices already deleted!");
    }


    /**
     * @brief 新增一个设备
     */
    void onMenuAddDevice() {
        DialogNewDevice newDevDlg = new DialogNewDevice(this.getActivity());
        newDevDlg.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
            @Override
            public void onLeftButtonClick() {
            }

            @Override
            public void onRightButtonClick() {
            }
        });

        newDevDlg.mSingleCallback = (integer, obj) -> {
            if (integer == 0) {
                String nodeId = (String)obj;
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemByDevNodeId(nodeId);
                if (findResult.mDevInfo != null) {
                    popupMessage("Device :" + nodeId + " already exist!");
                    return;
                }

                DeviceInfo newDevice = new DeviceInfo();
                newDevice.mNodeId = nodeId;
                mDevListAdapter.addNewItem(newDevice);

                List<DeviceInfo> deviceInfoList = mDevListAdapter.getDatas();
                deviceListStore(deviceInfoList);
            }
        };
        newDevDlg.setCanceledOnTouchOutside(false);
        newDevDlg.show();
    }

    /**
     * @brief 进入编辑模式删除设备
     */
    void onMenuRemoveDevice() {
//        List<DeviceInfo> deviceInfoList = mDevListAdapter.getDatas();
//        int devCount = deviceInfoList.size();
//        int i;
//        for (i = 0; i < devCount; i++) {
//            DeviceInfo deviceInfo = deviceInfoList.get(i);
//            if (deviceInfo.mSessionId != null) {
//                popupMessage("There are some devices in talking, should hangup all devices!");
//                return;
//            }
//        }

        // 切换到选择模式
        switchSelectMode(true);
    }

    /**
     * @brief 设置通话音效
     */
    void onMenuTalkEffect() {
        PopupMenu effectMenu = new PopupMenu(getActivity(), getBinding().titleView);
        getActivity().getMenuInflater().inflate(R.menu.menu_audio_effect, effectMenu.getMenu());

        effectMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.m_audeffect_normal: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.NORMAL);
                    } break;
                    case R.id.m_audeffect_ktv: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.KTV);
                    } break;
                    case R.id.m_audeffect_concert: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.CONCERT);
                    } break;
                    case R.id.m_audeffect_studio: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.STUDIO);
                    } break;
                    case R.id.m_audeffect_photograph: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.PHONOGRAPH);
                    } break;
                    case R.id.m_audeffect_virtualstereo: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.VIRTUALSTEREO);
                    } break;
                    case R.id.m_audeffect_spacial: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.SPACIAL);
                    } break;
                    case R.id.m_audeffect_ethereal: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.ETHEREAL);
                    } break;
                    case R.id.m_audeffect_voice3d: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.VOICE3D);
                    } break;
                    case R.id.m_audeffect_uncle: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.UNCLE);
                    } break;
                    case R.id.m_audeffect_oldman: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.OLDMAN);
                    } break;
                    case R.id.m_audeffect_boy: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.BOY);
                    } break;
                    case R.id.m_audeffect_sister: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.SISTER);
                    } break;
                    case R.id.m_audeffect_girl: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.GIRL);
                    } break;
                    case R.id.m_audeffect_pigking: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.PIGKING);
                    } break;
                    case R.id.m_audeffect_hulk: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.HULK);
                    } break;
                    case R.id.m_audeffect_rnb: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.RNB);
                    } break;
                    case R.id.m_audeffect_popular: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.POPULAR);
                    } break;
                    case R.id.m_audeffect_pitchcorrection: {
                        onMenuAudioEffect(ICallkitMgr.AudioEffectId.PITCHCORRECTION);
                    } break;
                }
                return true;
            }
        });
        effectMenu.show();
    }

    void onMenuAudioEffect(ICallkitMgr.AudioEffectId effectId) {
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        callkitMgr.setAudioEffect(effectId);
    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////// Session Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @brief 呼叫挂断 按钮点击事件
     */
    void onDevItemDialHangupClick(View view, int position, DeviceInfo deviceInfo) {
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }

        if (deviceInfo.mSessionId == null) {
            int sdkState = AIotAppSdkFactory.getInstance().getStateMachine();
            if (sdkState != IAgoraIotAppSdk.SDK_STATE_RUNNING) {
                popupMessage("Network is reconnecting, call dial later...");
                return;
            }

            // 呼叫操作
            String attachMsg = "Call_" + deviceInfo.mNodeId + "_at_" + getTimestamp();
            ICallkitMgr.DialParam dialParam = new ICallkitMgr.DialParam();
            dialParam.mPeerNodeId = deviceInfo.mNodeId;
            dialParam.mAttachMsg = attachMsg;
            dialParam.mPubLocalAudio = false;
            ICallkitMgr.DialResult dialResult = callkitMgr.callDial(dialParam);
            if (dialResult.mErrCode != ErrCode.XOK) {
                popupMessage("Call device: " + deviceInfo.mNodeId + " failure, errCode=" + dialResult.mErrCode);
                return;
            }


            // 更新 sessionId 和 提示信息
            ICallkitMgr.SessionInfo sessionInfo = callkitMgr.getSessionInfo(dialResult.mSessionId);

            deviceInfo.mSessionId = dialResult.mSessionId;
            deviceInfo.mTips = "Dial Requesting...";
            deviceInfo.mUserCount = sessionInfo.mUserCount;
            mDevListAdapter.setItem(position, deviceInfo);

            // 设置设备显示控件
            if (deviceInfo.mVideoView != null) {
                deviceInfo.mVideoView.setVisibility(View.VISIBLE);
            }
            callkitMgr.setPeerVideoView(deviceInfo.mSessionId, deviceInfo.mVideoView);

        } else {
            // 挂断操作
            int errCode = callkitMgr.callHangup(deviceInfo.mSessionId);
            if (errCode != ErrCode.XOK) {
                popupMessage("Hangup device: " + deviceInfo.mNodeId + " failure, errCode=" + errCode);
                return;
            }

            // 更新设备状态信息
            deviceInfo.clear();
            mDevListAdapter.setItem(position, deviceInfo);
            popupMessage("Hangup device: " + deviceInfo.mNodeId + " successful!");
        }
    }

    /**
     * @brief 静音 按钮点击事件
     */
    void onDevItemMuteAudioClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mSessionId == null) {
            return;
        }
        boolean devMute = (!deviceInfo.mDevMute);

        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        ICallkitMgr.SessionInfo sessionInfo = callkitMgr.getSessionInfo(deviceInfo.mSessionId);
        if (sessionInfo.mState != ICallkitMgr.SESSION_STATE_TALKING) {  // 只在通话时操作
            Log.d(TAG, "<onDevItemMuteAudioClick> not in talking, state=" + sessionInfo.mState);
            return;
        }

        int errCode = callkitMgr.mutePeerAudio(deviceInfo.mSessionId, devMute);
        if (errCode != ErrCode.XOK) {
            popupMessage("Mute or unmute device: " + deviceInfo.mNodeId + " failure, errCode=" + errCode);
            return;
        }

        deviceInfo.mDevMute = devMute;
        mDevListAdapter.setItem(position, deviceInfo);
    }

    /**
     * @brief 录像 按钮点击事件
     */
    void onDevItemRecordClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mSessionId == null) {
            return;
        }

        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        ICallkitMgr.SessionInfo sessionInfo = callkitMgr.getSessionInfo(deviceInfo.mSessionId);
        if (sessionInfo.mState != ICallkitMgr.SESSION_STATE_TALKING) {  // 只在通话时操作
            Log.d(TAG, "<onDevItemRecordClick> not in talking, state=" + sessionInfo.mState);
            return;
        }

        boolean recording = callkitMgr.isTalkingRecording(deviceInfo.mSessionId);
        if (recording) {
            // 停止录像
            int errCode = callkitMgr.talkingRecordStop(deviceInfo.mSessionId);
            if (errCode != ErrCode.XOK) {
                popupMessage("Device: " + deviceInfo.mNodeId + " stop recording failure, errCode=" + errCode);
                return;
            }

            popupMessage("Device: " + deviceInfo.mNodeId + " recording stopped!");
            deviceInfo.mRecording = false;
            mDevListAdapter.setItem(position, deviceInfo);

        } else {
            // 启动录像
            String strSavePath = FileUtils.getFileSavePath(deviceInfo.mNodeId, false);
            int errCode = callkitMgr.talkingRecordStart(deviceInfo.mSessionId, strSavePath);
            if (errCode != ErrCode.XOK) {
                popupMessage("Device: " + deviceInfo.mNodeId + " start recording failure, errCode=" + errCode);
                return;
            }

            popupMessage("Device: " + deviceInfo.mNodeId + " start recording......");
            deviceInfo.mRecording = true;
            mDevListAdapter.setItem(position, deviceInfo);
        }
    }

    /**
     * @brief 通话 按钮点击事件
     */
    void onDevItemMicClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mSessionId == null) {
            return;
        }
        boolean micPush = (!deviceInfo.mMicPush);

        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        ICallkitMgr.SessionInfo sessionInfo = callkitMgr.getSessionInfo(deviceInfo.mSessionId);
        if (sessionInfo.mState != ICallkitMgr.SESSION_STATE_TALKING) {  // 只在通话时操作
            Log.d(TAG, "<onDevItemMicClick> not in talking, state=" + sessionInfo.mState);
            return;
        }

        int errCode = callkitMgr.muteLocalAudio(deviceInfo.mSessionId, (!micPush));
        if (errCode != ErrCode.XOK) {
            popupMessage("Voice or unvoice device: " + deviceInfo.mNodeId + " failure, errCode=" + errCode);
            return;
        }

        deviceInfo.mMicPush = micPush;
        mDevListAdapter.setItem(position, deviceInfo);
    }

    /**
     * @brief 全屏 按钮点击事件
     */
    void onDevItemFullscrnClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mSessionId == null) {
            return;
        }

        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        ICallkitMgr.SessionInfo sessionInfo = callkitMgr.getSessionInfo(deviceInfo.mSessionId);
        if (sessionInfo.mState != ICallkitMgr.SESSION_STATE_TALKING) {  // 只在通话时操作
            Log.d(TAG, "<onDevItemFullscrnClick> not in talking, state=" + sessionInfo.mState);
            return;
        }

        PushApplication.getInstance().setFullscrnSessionId(deviceInfo.mSessionId);
        gotoDevPreviewActivity();
    }


    /**
     * @brief 截屏 按钮点击事件
     */
    void onDevItemShotCaptureClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mSessionId == null) {
            return;
        }

        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        ICallkitMgr.SessionInfo sessionInfo = callkitMgr.getSessionInfo(deviceInfo.mSessionId);
        if (sessionInfo.mState != ICallkitMgr.SESSION_STATE_TALKING) {  // 只在通话时操作
            Log.d(TAG, "<onDevItemShotCaptureClick> not in talking, state=" + sessionInfo.mState);
            return;
        }

        String strSavePath = FileUtils.getFileSavePath(deviceInfo.mNodeId, true);
        int errCode = callkitMgr.capturePeerVideoFrame(deviceInfo.mSessionId, strSavePath);
        if (errCode != ErrCode.XOK) {
            popupMessage("Device: " + deviceInfo.mNodeId + " shot capture failure, errCode=" + errCode);
            return;
        }

    }


    /**
     * @brief 发送命令 按钮点击事件
     */
    void onDevItemCommandClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mSessionId == null) {
            return;
        }

        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        ICallkitMgr.SessionInfo sessionInfo = callkitMgr.getSessionInfo(deviceInfo.mSessionId);
        if (sessionInfo.mState != ICallkitMgr.SESSION_STATE_TALKING) {  // 只在通话时操作
            Log.d(TAG, "<onDevItemCommandClick> not in talking, state=" + sessionInfo.mState);
            return;
        }

        boolean signalReady = AIotAppSdkFactory.getInstance().isSignalingReady();
        if (!signalReady) {
            popupMessage("Signaling not ready, cannot send command!");
            return;
        }

        DialogInputCommand inputCmdDlg = new DialogInputCommand(this.getActivity());
        inputCmdDlg.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
            @Override
            public void onLeftButtonClick() {
            }

            @Override
            public void onRightButtonClick() {
            }
        });

        inputCmdDlg.mSingleCallback = (integer, obj) -> {
            if (integer == 0) {
                String commandData = (String)obj;

                int errCode = callkitMgr.sendCommand(deviceInfo.mSessionId, commandData, new ICallkitMgr.OnCmdSendListener() {
                    @Override
                    public void onCmdSendDone(int errCode) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (errCode != ErrCode.XOK) {
                                    popupMessage("Send command: " + commandData + " failed, errCode=" + errCode);
                                } else {
                                    popupMessage("Send command: " + commandData + " successful!");
                                }
                            }
                        });
                    }
                });

                if (errCode != ErrCode.XOK) {
                    popupMessage("Send command: " + commandData + " to device failure, errCode=" + errCode);
                    return;
                }
            }
        };
        inputCmdDlg.setCanceledOnTouchOutside(false);
        inputCmdDlg.show();
    }


    /**
     * @brief 选择 按钮点击事件
     */
    void onDevItemCheckBox(CompoundButton compoundButton, int position, final DeviceInfo deviceInfo,
                           boolean selected) {
        // 处理全选按钮
        boolean selectAll = mDevListAdapter.isAllItemsSelected();
        getBinding().cbAllSelect.setChecked(selectAll);
    }

    void onDevItemCheckBoxClick(View view, int position, DeviceInfo deviceInfo) {
        boolean selected = (!deviceInfo.mSelected);
        deviceInfo.mSelected = selected;
        mDevListAdapter.setItemSelectStatus(position, deviceInfo);

        // 处理全选按钮
        boolean selectAll = mDevListAdapter.isAllItemsSelected();
        getBinding().cbAllSelect.setChecked(selectAll);

    }

    String getTimestamp() {
        String time_txt = "";
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) ;
        int month = calendar.get(Calendar.MONTH);
        int date = calendar.get(Calendar.DATE);
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int ms = calendar.get(Calendar.MILLISECOND);

        time_txt = String.format(Locale.getDefault(), "%d-%02d-%02d %02d:%02d:%02d.%d",
                year, month, date, hour,minute, second, ms);
        return time_txt;
    }

    void gotoDevPreviewActivity() {
        Intent intent = new Intent(getActivity(), DevPreviewActivity.class);
        startActivity(intent);
    }

    /**
     * @brief 重新设置所有设备的视频显示控件
     */
    void resetDeviceDisplayView(final UUID sessionId) {
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        List<DeviceInfo> deviceList = mDevListAdapter.getDatas();
        if (deviceList == null) {
            return;
        }
        int deviceCount = deviceList.size();
        for (int i = 0; i < deviceCount; i++) {
            DeviceInfo deviceInfo = deviceList.get(i);
            if (deviceInfo.mSessionId == null) {
                continue;
            }
            if (sessionId.compareTo(deviceInfo.mSessionId) == 0) {
                callkitMgr.setPeerVideoView(deviceInfo.mSessionId, deviceInfo.mVideoView);
                Log.d(TAG, "<resetDeviceDisplayView> sessionId=" + sessionId
                        + ", mNodeId=" + deviceInfo.mNodeId);
                return;
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////// Override Methods of ICallkitMgr.ICallback  ///////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onPeerIncoming(final UUID sessionId, final String peerNodeId,
                                final String attachMsg) {
        Log.d(TAG, "<onPeerIncoming> [IOTSDK/] sessionId=" + sessionId
                + ", peerNodeId=" + peerNodeId + ", attachMsg=" + attachMsg);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemByDevNodeId(peerNodeId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onPeerIncoming> NOT found device, peerNodeId=" + peerNodeId);
                    return;
                }

                // 更新设备状态信息
                findResult.mDevInfo.mSessionId = sessionId;
                findResult.mDevInfo.mSessionType = 2;
                findResult.mDevInfo.mTips = "Incoming...";
                mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);

                {   // 设置来电设备的视频显示控件
                    ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
                    if (findResult.mDevInfo.mVideoView != null) {
                        findResult.mDevInfo.mVideoView.setVisibility(View.VISIBLE);
                    }
                    callkitMgr.setPeerVideoView(sessionId, findResult.mDevInfo.mVideoView);
                }


                // 弹框显示是否接听
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Incoming Call");
                builder.setMessage("Incoming call from device: " + findResult.mDevInfo.mNodeId + " ...");
                builder.setCancelable(false);
                builder.setPositiveButton("Answer", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mAnswerRjectDlg = null;

                        // 接听操作
                        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();

                        int sdkState = AIotAppSdkFactory.getInstance().getStateMachine();
                        if (sdkState != IAgoraIotAppSdk.SDK_STATE_RUNNING) {
                            callkitMgr.callHangup(sessionId); // 挂断操作
                            // 更新设备状态信息
                            findResult.mDevInfo.clear();
                            mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);

                            popupMessage("Network is reconnecting, answer later...");
                            return;
                        }

                        callkitMgr.callAnswer(sessionId, false);

                        // 更新设备状态信息
                        findResult.mDevInfo.mTips = "Talking";
                        mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);
                    }
                });
                builder.setNegativeButton("Reject", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mAnswerRjectDlg = null;

                        // 挂断操作
                        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
                        callkitMgr.callHangup(sessionId);

                        // 更新设备状态信息
                        findResult.mDevInfo.clear();
                        mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);
                    }
                });

                mAnswerRjectDlg = builder.show();
                mAnswerRjectDlg.setCanceledOnTouchOutside(false);

            }
        });
    }

    @Override
    public void onDialDone(final UUID sessionId, final String peerNodeId, int errCode) {
        Log.d(TAG, "<onDialDone> [IOTSDK/] sessionId=" + sessionId
                + ", peerNodeId=" + peerNodeId + ", errCode=" + errCode);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemBySessionId(sessionId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onDialDone> NOT found session, sessionId=" + sessionId);
                    return;
                }

                if (errCode != ErrCode.XOK) {  // 呼叫失败
                    popupMessage("Call device: " + findResult.mDevInfo.mNodeId + " error, errCode=" + errCode);

                    // 更新设备状态信息
                    findResult.mDevInfo.clear();
                    mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);
                    return;
                }

                // 更新设备状态信息
                findResult.mDevInfo.mTips = "Dialing...";
                mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);
            }
        });
    }

    @Override
    public void onPeerAnswer(final UUID sessionId, final String peerNodeId) {
        Log.d(TAG, "<onPeerAnswer> [IOTSDK/] sessionId=" + sessionId
                + ", peerNodeId=" + peerNodeId);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemBySessionId(sessionId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onPeerAnswer> NOT found session, sessionId=" + sessionId);
                    return;
                }

                // 更新设备状态信息
                findResult.mDevInfo.mTips = "Talking...";
                mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);
            }
        });
    }

    @Override
    public void onPeerHangup(final UUID sessionId, final String peerNodeId) {
        Log.d(TAG, "<onPeerHangup> [IOTSDK/] sessionId=" + sessionId
                + ", peerNodeId=" + peerNodeId);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemBySessionId(sessionId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onPeerHangup> NOT found session, sessionId=" + sessionId);
                    return;
                }

                popupMessage("Peer device: " + findResult.mDevInfo.mNodeId + " hangup!");

                // 更新设备状态信息
                findResult.mDevInfo.clear();
                mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);

                if (mAnswerRjectDlg != null) {
                    mAnswerRjectDlg.cancel();
                    mAnswerRjectDlg = null;
                }

            }
        });
    }

    @Override
    public void onPeerTimeout(final UUID sessionId, final String peerNodeId) {
        Log.d(TAG, "<onPeerTimeout> [IOTSDK/] sessionId=" + sessionId
                + ", peerNodeId=" + peerNodeId);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemBySessionId(sessionId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onPeerTimeout> NOT found session, sessionId=" + sessionId);
                    return;
                }

                popupMessage("Call device: " + findResult.mDevInfo.mNodeId + " timeout and hangup!");

                // 更新设备状态信息
                findResult.mDevInfo.clear();
                mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);

                if (mAnswerRjectDlg != null) {
                    mAnswerRjectDlg.cancel();
                    mAnswerRjectDlg = null;
                }
            }
        });
    }

    @Override
    public void onPeerFirstVideo(final UUID sessionId, int videoWidth, int videoHeight) {
        Log.d(TAG, "<onPeerFirstVideo> [IOTSDK/] sessionId=" + sessionId
                + ", videoWidth=" + videoWidth + ", videoHeight=" + videoHeight);
    }

    @Override
    public void onOtherUserOnline(final UUID sessionId, int uid, int onlineUserCount) {
        Log.d(TAG, "<onOtherUserOnline> [IOTSDK/] sessionId=" + sessionId
                + ", onlineUserCount=" + onlineUserCount);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemBySessionId(sessionId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onOtherUserOnline> NOT found session, sessionId=" + sessionId);
                    return;
                }

                // 更新设备状态信息
                findResult.mDevInfo.mUserCount = onlineUserCount;
                mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);
            }
        });
    }

    @Override
    public void onOtherUserOffline(final UUID sessionId, int uid, int onlineUserCount) {
        Log.d(TAG, "<onOtherUserOffline> [IOTSDK/] sessionId=" + sessionId
                + ", onlineUserCount=" + onlineUserCount);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemBySessionId(sessionId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onOtherUserOffline> NOT found session, sessionId=" + sessionId);
                    return;
                }

                // 更新设备状态信息
                findResult.mDevInfo.mUserCount = onlineUserCount;
                mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);
            }
        });
    }

    @Override
    public void onSessionError(final UUID sessionId, int errCode) {
        Log.d(TAG, "<onSessionError> [IOTSDK/] sessionId=" + sessionId
                + ", errCode=" + errCode);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemBySessionId(sessionId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onOtherUserOffline> NOT found session, sessionId=" + sessionId);
                    return;
                }

                popupMessage("Talking " + findResult.mDevInfo.mNodeId + " error, errCode=" + errCode);

                // 更新设备状态信息
                findResult.mDevInfo.clear();
                mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);

                if (mAnswerRjectDlg != null) {
                    mAnswerRjectDlg.cancel();
                    mAnswerRjectDlg = null;
                }
            }
        });
    }

    @Override
    public void onRecordingError(final UUID sessionId, int errCode) {
        Log.d(TAG, "<onRecordingError> [IOTSDK/] sessionId=" + sessionId
                + ", errCode=" + errCode);
    }

    @Override
    public void onCaptureFrameDone(final UUID sessionId, int errCode,
                                   final String filePath, int width, int height) {
        Log.d(TAG, "<onCaptureFrameDone> [IOTSDK/] sessionId=" + sessionId
                + ", errCode=" + errCode + ", filePath=" + filePath);

        popupMessage("Capture successful, save to file=" + filePath);
    }

    @Override
    public void onReceivedCommand(final UUID sessionId, final String recvedCmd) {
        Log.d(TAG, "<onReceivedCommand> [IOTSDK/] sessionId=" + sessionId
                + ", recvedCmd=" + recvedCmd);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemBySessionId(sessionId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onReceivedCommand> NOT found session, sessionId=" + sessionId);
                    return;
                }

                popupMessage("Recv message: " + recvedCmd + " from devNodeId=" + findResult.mDevInfo.mNodeId);
            }
        });

    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of DeviceList Storage  ///////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * @brief 从本地存储读取设备列表信息
     */
    private List<DeviceInfo> deviceListLoad() {
        List<DeviceInfo> deviceInfoList = new ArrayList<>();

        String localUserId = AIotAppSdkFactory.getInstance().getLocalUserId();
        String keyDevCount = localUserId + "_device_count";

        int devCount = AppStorageUtil.queryIntValue(keyDevCount, 0);
        for (int i = 0; i < devCount; i++) {
            DeviceInfo deviceInfo = new DeviceInfo();
            String keyNodeId = localUserId + "_device_index_" + i;
            deviceInfo.mNodeId = AppStorageUtil.queryValue(keyNodeId, null);
            if (deviceInfo.mNodeId == null) {
                Log.e(TAG, "<deviceListLoad> fail to read key: " + keyNodeId);
                continue;
            }
            deviceInfoList.add(deviceInfo);
        }

        Log.d(TAG, "<deviceListLoad> stored device list, devCount=" + devCount);
        return deviceInfoList;
    }


    /**
     * @brief 将设备列表存储到本地
     */
    private void deviceListStore(final List<DeviceInfo> deviceInfoList) {
        int devCount = deviceInfoList.size();

        String localUserId = AIotAppSdkFactory.getInstance().getLocalUserId();
        String keyDevCount = localUserId + "_device_count";

        AppStorageUtil.keepShared(keyDevCount, devCount);
        for (int i = 0; i < devCount; i++) {
            DeviceInfo deviceInfo = deviceInfoList.get(i);
            String keyNodeId = localUserId + "_device_index_" + i;
            AppStorageUtil.keepShared(keyNodeId, deviceInfo.mNodeId);
        }

        Log.d(TAG, "<deviceListStore> stored device list, devCount=" + devCount);
    }

    ///////////////////////////////////////////////////////////////////////
    //////////////////// Methods for Audio Effect  ///////////////////////
    ///////////////////////////////////////////////////////////////////////
    final static ICallkitMgr.AudioEffectId[] mAudEffectIdArray = {
            ICallkitMgr.AudioEffectId.NORMAL,
            ICallkitMgr.AudioEffectId.KTV,
            ICallkitMgr.AudioEffectId.CONCERT,
            ICallkitMgr.AudioEffectId.STUDIO,
            ICallkitMgr.AudioEffectId.PHONOGRAPH,
            ICallkitMgr.AudioEffectId.VIRTUALSTEREO,
            ICallkitMgr.AudioEffectId.SPACIAL,
            ICallkitMgr.AudioEffectId.ETHEREAL,
            ICallkitMgr.AudioEffectId.VOICE3D,

            ICallkitMgr.AudioEffectId.UNCLE,
            ICallkitMgr.AudioEffectId.OLDMAN,
            ICallkitMgr.AudioEffectId.BOY,
            ICallkitMgr.AudioEffectId.SISTER,
            ICallkitMgr.AudioEffectId.GIRL,
            ICallkitMgr.AudioEffectId.PIGKING,
            ICallkitMgr.AudioEffectId.HULK,

            ICallkitMgr.AudioEffectId.RNB,
            ICallkitMgr.AudioEffectId.POPULAR,
            ICallkitMgr.AudioEffectId.PITCHCORRECTION
    };

    final static String[] mAudEffectNameArray = {
            "原声", "KTV", "演唱会", "录音棚", "留声机", "虚拟立体声", "空旷", "空灵", "3D人声",
            "大叔", "老男人", "男孩", "少女", "女孩", "猪八戒", "绿巨人",
            "R&B", "流行", "电音"
    };

    String getAudioEffectName(ICallkitMgr.AudioEffectId effectId) {
        int count = mAudEffectIdArray.length;
        for (int i = 0; i < count; i++) {
            if (effectId == mAudEffectIdArray[i]) {
                return mAudEffectNameArray[i];
            }
        }

        return mAudEffectNameArray[0];
    }
}
