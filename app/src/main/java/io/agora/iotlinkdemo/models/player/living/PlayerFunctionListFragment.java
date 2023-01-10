package io.agora.iotlinkdemo.models.player.living;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ScreenUtils;
import com.agora.baselibrary.utils.StringUtils;
import com.agora.baselibrary.utils.ToastUtils;
import com.alibaba.android.arouter.facade.annotation.Route;

import java.util.List;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.IDeviceMgr;
import io.agora.iotlink.IotPropertyDesc;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingFragment;
import io.agora.iotlinkdemo.base.PermissionHandler;
import io.agora.iotlinkdemo.base.PermissionItem;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.FagmentPlayerFunctionBinding;
import io.agora.iotlinkdemo.dialog.ChangeOfVoiceDialog;
import io.agora.iotlinkdemo.dialog.NoPowerDialog;
import io.agora.iotlinkdemo.dialog.SelectLegibilityDialog;
import io.agora.iotlinkdemo.dialog.SelectMotionDetectionDialog;
import io.agora.iotlinkdemo.dialog.SelectNightVisionDialog;
import io.agora.iotlinkdemo.dialog.SelectPirDialog;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.player.PlayerViewModel;

/**
 * 播放页功能列表
 */
@Route(path = PagePathConstant.pagePlayerFunction)
public class PlayerFunctionListFragment extends BaseViewBindingFragment<FagmentPlayerFunctionBinding>
        implements PermissionHandler.ICallback {
    private final String TAG = "IOTLINK/PlayFuncFrag";

    //
    // message Id
    //
    public static final int MSGID_CHECK_STATE = 0x1001;    ///< 检测当前呼叫状态

    private PermissionHandler mPermHandler;             ///< 权限申请处理
    private Handler mMsgHandler = null;                 ///< 主线程中的消息处理

    /**
     * 电量不足提示
     */
    private NoPowerDialog noPowerDialog;

    /**
     * 显示清晰度选择对话框
     */
    private SelectLegibilityDialog selectLegibilityDialog;
    /**
     * 显示选择pir对话框
     */
    private SelectPirDialog selectPirDialog;

    /**
     * 显示变声选择对话框
     */
    private ChangeOfVoiceDialog changeOfVoiceDialog;
    /**
     * 移动侦测对话框
     */
    private SelectMotionDetectionDialog selectMotionDetectionDialog;
    /**
     * 红外夜视对话框
     */
    private SelectNightVisionDialog mNightVisionDialog;

    /**
     * 设备播放等模块统一ViewModel
     */
    private PlayerViewModel playerViewModel;

    @NonNull
    @Override
    protected FagmentPlayerFunctionBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FagmentPlayerFunctionBinding.inflate(inflater);
    }

    @Override
    public void initView() {
        playerViewModel = new ViewModelProvider(this).get(PlayerViewModel.class);
        playerViewModel.setLifecycleOwner(this);
        playerViewModel.initHandler();
        playerViewModel.setISingleCallback((type, var2) -> {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (type == Constant.CALLBACK_TYPE_PLAYER_UPDATE_PROPERTY) {
                        setSwitchStatus();

                    } else if (type == Constant.CALLBACK_TYPE_PLAYER_SAVE_SCREENSHOT) {
                        showSaveTip(false);

                    } else if (type == Constant.CALLBACK_TYPE_DEVICE_CONNING) {  // 正在呼叫中
                        getBinding().tvTips1.setVisibility(View.VISIBLE);
                        getBinding().tvTips2.setVisibility(View.GONE);

                    } else if (type == Constant.CALLBACK_TYPE_DEVICE_NET_RECEIVING_SPEED) {  // 获取到网络信息
                        getBinding().tvNetSpeed.setText(var2 + "kb");
                        getBinding().tvNetSpeedFull.setText(var2 + "kb");

                    } else if (type == Constant.CALLBACK_TYPE_DEVICE_LAST_MILE_DELAY) {
                        getBinding().tvPlaySaveTime.setText(StringUtils.INSTANCE.getDetailTime("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis() / 1000));
                        getBinding().tvPlaySaveTimeFull.setText(StringUtils.INSTANCE.getDetailTime("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis() / 1000));

                    } else if (type == Constant.CALLBACK_TYPE_DEVICE_DIAL_DONE) {  // 呼叫设备完成
                        updateCallWgtStatus();
                        mMsgHandler.removeMessages(MSGID_CHECK_STATE);
                        mMsgHandler.sendEmptyMessageDelayed(MSGID_CHECK_STATE, 10000);

                    } else if (type == Constant.CALLBACK_TYPE_DEVICE_DIAL_TIMEOUT) {  // 呼叫设备超时
                        updateCallWgtStatus();

                    } else if (type == Constant.CALLBACK_TYPE_DEVICE_ANSWER) {  // 设备端接听
                        updateCallWgtStatus();

                    } else if (type == Constant.CALLBACK_TYPE_DEVICE_HANGUP) {  // 设备端挂断
                        updateCallWgtStatus();

                    } else if (type == Constant.CALLBACK_TYPE_DEVICE_PEER_FIRST_VIDEO) {  // 首帧出图
                        updateCallWgtStatus();

                    } else if (type == Constant.CALLBACK_TYPE_FIRM_GETVERSION) {  // 获取固件版本
                        IDeviceMgr.McuVersionInfo mcuVerInfo = (IDeviceMgr.McuVersionInfo)var2;
                        Log.d(TAG, "<ISingleCallback> mcuVerInfo=" + mcuVerInfo.toString());
                        PlayerPreviewActivity playerActivity = (PlayerPreviewActivity)getActivity();
                        if (playerActivity != null) {
                            if ((mcuVerInfo.mIsupgradable) && (mcuVerInfo.mUpgradeId > 0)) {
                                ((PlayerPreviewActivity) getActivity()).updateTitle(true);
                            } else {
                                ((PlayerPreviewActivity) getActivity()).updateTitle(false);
                            }
                        }
                    } else if (type == Constant.CALLBACK_TYPE_PLAYER_UPDATEPROPDESC) { // 查询到属性描述符列表
                        List<IotPropertyDesc> propDescList = (List<IotPropertyDesc>)var2;
                        if (propDescList == null) {  // 查询属性描述列表失败
                            return;
                        }
                        // 打印所有属性描述符信息
                        for (int i = 0; i < propDescList.size(); i++) {
                            IotPropertyDesc propertyDesc = propDescList.get(i);
                            Log.d(TAG, "<ISingleCallback> propDesc[" + i + "] " + propertyDesc.toString());
                        }

                    } else if (type == Constant.CALLBACK_TYPE_USER_ONLINE ||  // 其他用户上下线
                            type == Constant.CALLBACK_TYPE_USER_OFFLINE) {
                        int userCount = (Integer)var2;
                        getBinding().tvUserCount.post(() -> {
                            String str_usercnt = getString(R.string.user_count) + userCount;
                            getBinding().tvUserCount.setText(str_usercnt);
                        });
                    }
                }
            });
        });
        playerViewModel.initMachine();
        updateCallWgtStatus();
        mMsgHandler = new Handler(getActivity().getMainLooper()) {
            @SuppressLint("HandlerLeak")
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case MSGID_CHECK_STATE: {
                        Log.d(TAG, "<handleMessage> MSGID_CHECK_STATE");
                        updateCallWgtStatus();
                    } break;
                }
            }
        };
        mMsgHandler.removeMessages(MSGID_CHECK_STATE);
        mMsgHandler.sendEmptyMessageDelayed(MSGID_CHECK_STATE, 1000);

        getBinding().tvUserCount.setVisibility(View.VISIBLE);
        int userCount = playerViewModel.getOnlineUserCount();
        String str_usercnt = getString(R.string.user_count) + userCount;
        getBinding().tvUserCount.setText(str_usercnt);
    }

    @Override
    public void initListener() {
        getBinding().ivAlbum.setOnClickListener(view -> PagePilotManager.pageAlbum());
        getBinding().cbPIR.setOnClickListener(view -> showSelectPirDialog());

        getBinding().cbChangeSound.setOnCheckedChangeListener((compoundButton, b) -> playerViewModel.setMutePeer(!b));
        getBinding().cbChangeSoundFull.setOnCheckedChangeListener((compoundButton, b) -> playerViewModel.setMutePeer(!b));

        getBinding().btnSelectLegibility.setOnClickListener(view -> showSelectLegibilityDialog());
        getBinding().btnSelectLegibilityFull.setOnClickListener(view -> showSelectLegibilityDialog());

        getBinding().ivCall.setOnClickListener(view -> {
                    playerViewModel.onBtnVoiceTalk();
                    getBinding().ivCall.setSelected(playerViewModel.mSendingVoice);
                }
        );
        getBinding().ivCallFull.setOnClickListener(view -> {
                    playerViewModel.onBtnVoiceTalk();
                    getBinding().ivCallFull.setSelected(playerViewModel.mSendingVoice);
                }
        );
        getBinding().ivChangeOfVoice.setOnClickListener(view -> showChangeOfVoiceDialog());
        getBinding().ivChangeOfVoiceFull.setOnClickListener(view -> showChangeOfVoiceDialog());

        getBinding().ivClip.setOnClickListener(view -> onBtnCapturePeerFrame());
        getBinding().ivClipFull.setOnClickListener(view -> onBtnCapturePeerFrame());
        getBinding().cbRecord.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                ToastUtils.INSTANCE.showToast(R.string.function_not_open);
                compoundButton.setChecked(false);
            }
        });
        getBinding().cbRecordFull.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                ToastUtils.INSTANCE.showToast(R.string.function_not_open);
                compoundButton.setChecked(false);
            }
        });

        getBinding().saveBg.setOnClickListener(view -> PagePilotManager.pageAlbum());
        getBinding().ivChangeScreen.setOnClickListener(view -> onBtnLandscape());
        getBinding().cbNightVision.setOnClickListener(view -> showNightVisionDialog());
        getBinding().cbWDR.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                ToastUtils.INSTANCE.showToast(R.string.function_not_open);
                compoundButton.setChecked(false);
            }
        });
        getBinding().cbSoundDetection.setOnClickListener(view -> {
            getBinding().cbSoundDetection.setSelected(!getBinding().cbSoundDetection.isSelected());
            playerViewModel.setSoundDetection(getBinding().cbSoundDetection.isSelected());
        });
        getBinding().cbMotionDetection.setOnClickListener(view -> showMotionDetectionDialog());
        getBinding().ivBack.setOnClickListener(view -> onBtnLandscape());
        getBinding().landscapeLayout.setOnClickListener(view -> showLandScapeLayout());

        getBinding().tvTips2.setOnClickListener(view -> {
            onBtnRetry();
        });
    }

    private void showLandScapeLayout() {
        getBinding().ivPowerBgFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().pbPowerValueFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().cbChangeSoundFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().btnSelectLegibilityFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().ivChangeOfVoiceFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().ivChangeOfVoiceFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().ivCallFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().cbRecordFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().ivClipFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().tvPlaySaveTimeFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().tvNetSpeedFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().ivBack.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);

    }

    /**
     * 当前是否正在横屏显示
     */
    public boolean mIsOrientLandscape = false;
    public int uiOptionsOld = 0;

    /**
     * 横屏切换
     */
    public void onBtnLandscape() {
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) getBinding().peerView.getLayoutParams();
        if (mIsOrientLandscape) {
            Log.d(TAG, "<onBtnLandscape> switching to portrait display...");
            if (lp == null) {
                lp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ScreenUtils.dp2px(200));
            } else {
                lp.width = me.jessyan.autosize.utils.ScreenUtils.getScreenSize(getActivity())[1];
                lp.height = ScreenUtils.dp2px(200);
            }
            lp.topMargin = ScreenUtils.dp2px(55);
            ((PlayerPreviewActivity) getActivity()).showTitle();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getBinding().landscapeLayout.setVisibility(View.GONE);
            getBinding().btnSelectLegibility.setVisibility(View.VISIBLE);
            getBinding().tvNetSpeed.setVisibility(View.VISIBLE);
            getBinding().tvPlaySaveTime.setVisibility(View.VISIBLE);
            getBinding().loadingBG.setVisibility(View.VISIBLE);
            getActivity().getWindow().getDecorView().setSystemUiVisibility(uiOptionsOld);
            ((ConstraintLayout.LayoutParams) getBinding().saveBg.getLayoutParams()).rightMargin = ScreenUtils.dp2px(15);

        } else {
            Log.d(TAG, "<onBtnLandscape> switching to landscape display...");
            if (lp == null) {
                lp = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
            } else {
                lp.width = me.jessyan.autosize.utils.ScreenUtils.getScreenSize(getActivity())[1];
                lp.height = me.jessyan.autosize.utils.ScreenUtils.getScreenSize(getActivity())[0];
            }
            lp.topMargin = ScreenUtils.dp2px(0);

            View decorView = getActivity().getWindow().getDecorView();
            uiOptionsOld = decorView.getSystemUiVisibility();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            ((PlayerPreviewActivity) getActivity()).hideTitle();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getBinding().landscapeLayout.setVisibility(View.VISIBLE);
            getBinding().btnSelectLegibility.setVisibility(View.GONE);
            getBinding().tvNetSpeed.setVisibility(View.GONE);
            getBinding().tvPlaySaveTime.setVisibility(View.GONE);
            getBinding().loadingBG.setVisibility(View.GONE);
            ((ConstraintLayout.LayoutParams) getBinding().saveBg.getLayoutParams()).rightMargin = ScreenUtils.dp2px(90);
        }
        mIsOrientLandscape = !mIsOrientLandscape;
        getBinding().peerView.setLayoutParams(lp);

    }

    /**
     * 初始化开关状态
     */
    private void setSwitchStatus() {
        getBinding().peerView.post(() -> {
            //电量
            int quantity = playerViewModel.mDevProperty.mQuantity;
            getBinding().pbPowerValue.setProgress(quantity);

            if (quantity < 20) {
//                showNoPowerDialog();
                GradientDrawable progressContent = new GradientDrawable();
                progressContent.setColor(ContextCompat.getColor(getActivity(), R.color.red_e0));
                getBinding().pbPowerValue.setProgressDrawable(new ClipDrawable(progressContent, Gravity.START, ClipDrawable.HORIZONTAL));
            }

            getBinding().btnSelectLegibility.setText(playerViewModel.mDevProperty.mVideoQuality == 1 ? "标清" : "高清");
            getBinding().btnSelectLegibilityFull.setText(playerViewModel.mDevProperty.mVideoQuality == 1 ? "标清" : "高清");

            //红外夜视
            getBinding().cbNightVision.setSelected(playerViewModel.mDevProperty.mNightView == 2);
            //移动侦测
            getBinding().cbMotionDetection.setSelected(playerViewModel.mDevProperty.mMotionDetect);
            //声音侦测
            getBinding().cbSoundDetection.setSelected(playerViewModel.mDevProperty.mVoiceDetect);
            //PIR
            getBinding().cbPIR.setSelected(playerViewModel.mDevProperty.mPirSensitive != 0);
            getBinding().cbSiren.setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) {
                    ToastUtils.INSTANCE.showToast(R.string.function_not_open);
                    compoundButton.setChecked(false);
                }
            });
            getBinding().ivReplay.setOnClickListener(view -> ToastUtils.INSTANCE.showToast(R.string.function_not_open));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        playerViewModel.initPeerVideo(getBinding().peerView);
        if (getBinding().cbChangeSound.isChecked()) {
            Log.d(TAG, "<onResume> unmute peer audio");
            playerViewModel.setMutePeer(false);
        }

        // 延迟调用查询当前MCU固件版本版本
        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    public void run() {
                        updateCallWgtStatus();
                        playerViewModel.queryMcuVersion();
                    }
                },
                300);
    }

    @Override
    public void onPause() {
        super.onPause();
        playerViewModel.pausePlayer();
        if (getBinding().cbChangeSound.isChecked()) {
            if (playerViewModel.getCallStatus() == PlayerViewModel.CALL_STATE_CONNECTED) {
                // 正在通话中才做静音处理
                Log.d(TAG, "<onPause> mute peer audio");
                playerViewModel.setMutePeer(true);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        playerViewModel.release();
        if (mMsgHandler != null) {
            mMsgHandler.removeMessages(MSGID_CHECK_STATE);
            mMsgHandler = null;
        }
    }

    public boolean onBtnBack() {
        if (mIsOrientLandscape) { // 退回到 portrait竖屏显示
            Log.d(TAG, "<onBtnBack> switch to portrait");
            onBtnLandscape();
            return true;
        } else {
            Log.d(TAG, "<onBtnBack> back to home");
            playerViewModel.callHangup();
            return false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        playerViewModel.callHangup();
    }

    @Override
    public void requestData() {
        playerViewModel.requestViewModelData();
        //playerViewModel.queryAllPropDesc();
    }

    /**
     * 开始录音
     */
    private void startRecord() {
        getBinding().tvRECTip.setVisibility(View.VISIBLE);
    }

    /**
     * 停止录音
     */
    private void stopRecord() {
        showSaveTip(true);
        getBinding().tvRECTip.setVisibility(View.GONE);
    }

    /**
     * 显示保存提示
     *
     * @param isSaveVideo true 提示视频保存到相册 false 提示截图保存到相册
     */
    private void showSaveTip(boolean isSaveVideo) {
        if (isSaveVideo) {
            getBinding().tvSaveType.setText(getString(R.string.save_video_tip));
        } else {
            getBinding().tvSaveType.setText(getString(R.string.save_pic_tip));
        }
        getBinding().saveBg.setVisibility(View.VISIBLE);
        getBinding().saveBg.postDelayed(() -> getBinding().saveBg.setVisibility(View.GONE), 2000);
    }

    private void showNoPowerDialog() {
        if (noPowerDialog == null) {
            noPowerDialog = new NoPowerDialog(getActivity());
        }
        noPowerDialog.show();
    }

    private void showSelectLegibilityDialog() {
        if (selectLegibilityDialog == null) {
            selectLegibilityDialog = new SelectLegibilityDialog(getActivity());
            selectLegibilityDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {
                    getBinding().btnSelectLegibility.setText(getString(R.string.HD));
                    playerViewModel.setVideoQuality(2);
                }

                @Override
                public void onRightButtonClick() {
                    getBinding().btnSelectLegibility.setText(getString(R.string.SD));
                    playerViewModel.setVideoQuality(1);
                }
            });
        }
        selectLegibilityDialog.setSelect(playerViewModel.mDevProperty.mVideoQuality);
        selectLegibilityDialog.show();
    }

    private void showSelectPirDialog() {
        if (selectPirDialog == null) {
            selectPirDialog = new SelectPirDialog(getActivity());
            selectPirDialog.iSingleCallback = (type, data) -> {
                if (type == 1) {
                    if ((int) data == 0) {
                        getBinding().cbPIR.setSelected(false);
                    } else {
                        getBinding().cbPIR.setSelected(true);
                    }
                    playerViewModel.setPirSwitch((int) data);
                }
            };
        }
        selectPirDialog.setSelect(playerViewModel.mDevProperty.mPirSensitive);
        selectPirDialog.show();
    }

    private void showChangeOfVoiceDialog() {
        if (changeOfVoiceDialog == null) {
            changeOfVoiceDialog = new ChangeOfVoiceDialog(getActivity());
            changeOfVoiceDialog.iSingleCallback = (type, var2) -> {
                if (type == 0) {
                    getBinding().tvChangeVoiceTip.setVisibility(View.GONE);
                    playerViewModel.setAudioEffect(ICallkitMgr.AudioEffectId.NORMAL);
                    getBinding().ivChangeOfVoice.setSelected(false);
                    getBinding().ivChangeOfVoiceFull.setSelected(false);
                } else {
                    if (type == 1) {
                        getBinding().tvChangeVoiceTip.setText(getString(R.string.change_voice_type1));
                        playerViewModel.setAudioEffect(ICallkitMgr.AudioEffectId.OLDMAN);
                    } else if (type == 2) {
                        getBinding().tvChangeVoiceTip.setText(getString(R.string.change_voice_type2));
                        playerViewModel.setAudioEffect(ICallkitMgr.AudioEffectId.BABYBOY);
                    } else if (type == 3) {
                        getBinding().tvChangeVoiceTip.setText(getString(R.string.change_voice_type3));
                        playerViewModel.setAudioEffect(ICallkitMgr.AudioEffectId.BABYGIRL);
                    } else if (type == 4) {
                        getBinding().tvChangeVoiceTip.setText(getString(R.string.change_voice_type4));
                        playerViewModel.setAudioEffect(ICallkitMgr.AudioEffectId.ZHUBAJIE);
                    } else if (type == 5) {
                        getBinding().tvChangeVoiceTip.setText(getString(R.string.change_voice_type5));
                        playerViewModel.setAudioEffect(ICallkitMgr.AudioEffectId.ETHEREAL);
                    } else if (type == 6) {
                        getBinding().tvChangeVoiceTip.setText(getString(R.string.change_voice_type6));
                        playerViewModel.setAudioEffect(ICallkitMgr.AudioEffectId.HULK);
                    }
                    getBinding().tvChangeVoiceTip.setVisibility(View.VISIBLE);
                    getBinding().ivChangeOfVoice.setSelected(true);
                    getBinding().ivChangeOfVoiceFull.setSelected(true);
                }
            };
        }

        ICallkitMgr.AudioEffectId effectId = playerViewModel.getAudioEffect();
        int position = cvtAudioEffectToPos(effectId);
        changeOfVoiceDialog.setSelect(position);
        changeOfVoiceDialog.show();
    }

    int cvtAudioEffectToPos(ICallkitMgr.AudioEffectId effectId) {
        if (effectId == ICallkitMgr.AudioEffectId.OLDMAN) {
            return 1;
        } else if (effectId == ICallkitMgr.AudioEffectId.BABYBOY) {
            return 2;
        } else if (effectId == ICallkitMgr.AudioEffectId.BABYGIRL) {
            return 3;
        } else if (effectId == ICallkitMgr.AudioEffectId.ZHUBAJIE) {
            return 4;
        } else if (effectId == ICallkitMgr.AudioEffectId.ETHEREAL) {
            return 5;
        } else if (effectId == ICallkitMgr.AudioEffectId.HULK) {
            return 6;
        }
        return 0;
    }

    private void showNightVisionDialog() {
        if (mNightVisionDialog == null) {
            mNightVisionDialog = new SelectNightVisionDialog(getActivity());
            mNightVisionDialog.iSingleCallback = (type, var2) -> {
                playerViewModel.setNightView(type);
                if (type == 2) {
                    getBinding().cbNightVision.setSelected(true);
                } else {
                    getBinding().cbNightVision.setSelected(false);
                }
            };
        }

        mNightVisionDialog.setSelect(playerViewModel.mDevProperty.mNightView);
        mNightVisionDialog.show();
    }

    private void showMotionDetectionDialog() {
        if (selectMotionDetectionDialog == null) {
            selectMotionDetectionDialog = new SelectMotionDetectionDialog(getActivity());
            selectMotionDetectionDialog.iSingleCallback = (type, var2) -> {
                if (type == 0) {
                    getBinding().cbMotionDetection.setSelected(false);
                    playerViewModel.setMotionAlarm(false);
                } else {
                    getBinding().cbMotionDetection.setSelected(true);
                    playerViewModel.setMotionAlarm(true);
                }
            };
        }
        if (playerViewModel.mDevProperty.mMotionDetect) {
            selectMotionDetectionDialog.setSelect(2);
        } else {
            selectMotionDetectionDialog.setSelect(0);
        }
        selectMotionDetectionDialog.show();
    }


    /**
     * @brief 根据呼叫状态刷新相应控件
     */
    private void updateCallWgtStatus() {
        if (mIsOrientLandscape) {
            Log.d(TAG, "<updateCallWgtStatus> OrientLandscape, do nothing");
            return;
        }
        int callkitStatus = playerViewModel.getCallStatus();

        if (callkitStatus == PlayerViewModel.CALL_STATE_CONNECTED) {    // 已经接通
            Log.d(TAG, "<updateCallWgtStatus> CONNECTED");
            getBinding().progressLoading.setVisibility(View.INVISIBLE);
            getBinding().tvTips1.setVisibility(View.INVISIBLE);
            getBinding().tvTips2.setVisibility(View.INVISIBLE);
            getBinding().loadingBG.setVisibility(View.INVISIBLE);

        } else if (callkitStatus == PlayerViewModel.CALL_STATE_CONNECTING) { // 正在接通中
            Log.d(TAG, "<updateCallWgtStatus> CONNECTING...");
            getBinding().progressLoading.setVisibility(View.VISIBLE);
            getBinding().tvTips1.setVisibility(View.VISIBLE);
            getBinding().tvTips2.setVisibility(View.INVISIBLE);
            getBinding().loadingBG.setVisibility(View.VISIBLE);

        } else {  // 断开
            Log.d(TAG, "<updateCallWgtStatus> DISCONNECTED");
            getBinding().progressLoading.setVisibility(View.INVISIBLE);
            getBinding().tvTips1.setVisibility(View.INVISIBLE);
            getBinding().tvTips2.setVisibility(View.VISIBLE);
            getBinding().loadingBG.setVisibility(View.VISIBLE);
        }
    }

    /**
     * @brief 重新呼叫设备端
     */
    void onBtnRetry() {
        int ret = playerViewModel.callDial("This is app");
        if (ret != ErrCode.XOK) {
            return;
        }
        updateCallWgtStatus();
    }

    /**
     * @brief 视频帧截屏处理
     */
    void onBtnCapturePeerFrame() {
//        //
//        // 截图写存储 权限判断处理
//        //
//        int[] permIdArray = new int[1];
//        permIdArray[0] = PermissionHandler.PERM_ID_WRITE_STORAGE;
//        mPermHandler = new PermissionHandler(getActivity(), this, permIdArray);
//        if (!mPermHandler.isAllPermissionGranted()) {
//            Log.d(TAG, "<onBtnCapturePeerFrame> requesting permission...");
//            mPermHandler.requestNextPermission();
//        } else {
//            Log.d(TAG, "<onBtnCapturePeerFrame> permission ready");
//            captureDeviceFrame();
//        }
        captureDeviceFrame();
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

        if (permItems[0].requestId == PermissionHandler.PERM_ID_WRITE_STORAGE) {  // 截图权限
            if (allGranted) {
                captureDeviceFrame();
            } else {
                popupMessage(getString(R.string.no_permission));
            }
        }
    }

    private void captureDeviceFrame() {
        getBinding().ivCover.setImageBitmap(playerViewModel.saveScreenshotToSD());
    }

    @Override
    public void onStart() {
        Log.d(TAG, "<onStart>");
        super.onStart();
        playerViewModel.onStart();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "<onStop>");
        super.onStop();
        playerViewModel.onStop();
    }
}
