package io.agora.iotlinkdemo.models.player.living;

import android.app.Application;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Looper;
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

import io.agora.iotlink.IDeviceMgr;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingFragment;
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
import io.agora.iotlink.ICallkitMgr;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 播放页功能列表
 */
@Route(path = PagePathConstant.pagePlayerFunction)
public class PlayerFunctionListFragment extends BaseViewBindingFragment<FagmentPlayerFunctionBinding> {
    private final String TAG = "IOTLINK/PlayFuncFrag";

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
            getBinding().loadingBG.post(() -> {
                if (type == Constant.CALLBACK_TYPE_PLAYER_UPDATE_PROPERTY) {
                    setSwitchStatus();
                } else if (type == Constant.CALLBACK_TYPE_PLAYER_SAVE_SCREENSHOT) {
                    showSaveTip(false);
                } else if (type == Constant.CALLBACK_TYPE_DEVICE_CONNING) {
                    getBinding().tvTips1.setVisibility(View.VISIBLE);
                    getBinding().tvTips2.setVisibility(View.GONE);
                } else if (type == Constant.CALLBACK_TYPE_DEVICE_NET_RECEIVING_SPEED) {
                    getBinding().loadingBG.setVisibility(View.GONE);
                    getBinding().tvNetSpeed.setText(var2 + "kb");
                    getBinding().tvNetSpeedFull.setText(var2 + "kb");
                } else if (type == Constant.CALLBACK_TYPE_DEVICE_LAST_MILE_DELAY) {
                    getBinding().tvPlaySaveTime.setText(StringUtils.INSTANCE.getDetailTime("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis() / 1000));
                    getBinding().tvPlaySaveTimeFull.setText(StringUtils.INSTANCE.getDetailTime("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis() / 1000));
                } else if (type == Constant.CALLBACK_TYPE_DEVICE_PEER_FIRST_VIDEO) {
                    getBinding().loadingBG.setVisibility(View.GONE);
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
                }
            });
        });
        playerViewModel.initMachine();
//        playerViewModel.initPeerVideo(getBinding().peerView);
        playerViewModel.setMutePeer(true);
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

        getBinding().ivClip.setOnClickListener(view -> getBinding().ivCover.setImageBitmap(playerViewModel.saveScreenshotToSD()));
        getBinding().ivClipFull.setOnClickListener(view -> getBinding().ivCover.setImageBitmap(playerViewModel.saveScreenshotToSD()));
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
            getActivity().getWindow().getDecorView().setSystemUiVisibility(uiOptionsOld);
            ((ConstraintLayout.LayoutParams) getBinding().saveBg.getLayoutParams()).rightMargin = ScreenUtils.dp2px(15);
        } else {
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
            playerViewModel.setMutePeer(false);
        }

        // 延迟调用查询当前MCU固件版本版本
        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    public void run() {
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
            playerViewModel.setMutePeer(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        playerViewModel.release();
    }

    public void onBtnBack() {
        if (mIsOrientLandscape) { // 退回到 portrait竖屏显示
            onBtnLandscape();
        } else {
            playerViewModel.callHangup();
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
                if (type == 1) {
                    getBinding().tvChangeVoiceTip.setVisibility(View.GONE);
                    playerViewModel.setAudioEffect(ICallkitMgr.AudioEffectId.NORMAL);
                    getBinding().ivChangeOfVoice.setSelected(false);
                    getBinding().ivChangeOfVoiceFull.setSelected(false);
                } else {
                    if (type == 2) {
                        getBinding().tvChangeVoiceTip.setText(getString(R.string.change_voice_type2));
                        playerViewModel.setAudioEffect(ICallkitMgr.AudioEffectId.OLDMAN);
                    } else if (type == 3) {
                        getBinding().tvChangeVoiceTip.setText(getString(R.string.change_voice_type3));
                        playerViewModel.setAudioEffect(ICallkitMgr.AudioEffectId.BABYBOY);
                    } else if (type == 4) {
                        getBinding().tvChangeVoiceTip.setText(getString(R.string.change_voice_type4));
                        playerViewModel.setAudioEffect(ICallkitMgr.AudioEffectId.BABYGIRL);
                    }
                    getBinding().tvChangeVoiceTip.setVisibility(View.VISIBLE);
                    getBinding().ivChangeOfVoice.setSelected(true);
                    getBinding().ivChangeOfVoiceFull.setSelected(true);
                }
            };
        }
        changeOfVoiceDialog.show();
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
