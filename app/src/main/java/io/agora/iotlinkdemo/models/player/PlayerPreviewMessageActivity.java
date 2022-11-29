package io.agora.iotlinkdemo.models.player;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ScreenUtils;
import com.agora.baselibrary.utils.StringUtils;
import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.PermissionHandler;
import io.agora.iotlinkdemo.base.PermissionItem;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityPlayerMessageBinding;
import io.agora.iotlinkdemo.dialog.DeleteMediaTipDialog;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.message.MessageViewModel;
import io.agora.iotlinkdemo.models.player.adapter.ViewPagerAdapter;
import io.agora.iotlinkdemo.models.player.living.PlayerFunctionListFragment;
import io.agora.iotlinkdemo.models.player.living.PlayerMessageListFragment;
import io.agora.iotlinkdemo.models.player.living.PlayerPreviewActivity;
import io.agora.iotlinkdemo.utils.FileUtils;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import java.util.ArrayList;
import java.util.List;

import kotlin.jvm.JvmField;

/**
 * 消息播放页
 */
@Route(path = PagePathConstant.pagePlayMessage)
public class PlayerPreviewMessageActivity extends BaseGsyPlayerActivity<ActivityPlayerMessageBinding>
        implements PermissionHandler.ICallback {
    private final String TAG = "IOTLINK/PlayPrevMsgAct";

    private final int UI_STATE_PORTRAIT = 0x0000;           ///< 竖屏显示
    private final int UI_STATE_FULL = 0x0001;               ///< 全屏显示，横屏无控件显示
    private final int UI_STATE_LANDSCAPE = 0x0002;          ///< 横屏带控件显示


    private MessageViewModel messageViewModel;              ///< 消息ViewModel
    private PermissionHandler mPermHandler;                 ///< 权限申请处理

    private DeleteMediaTipDialog deleteMediaTipDialog;      ///< 删除对话框

    private volatile boolean mIsOrientLandscape = false;    ///< 是否横屏状态
    private volatile int mUiState = UI_STATE_PORTRAIT;      ///< 当前UI状态机
    private int mUiOptionsOld = 0;

    private boolean isPlaying = false;

    @JvmField
    @Autowired(name = Constant.FILE_URL)
    String mFileUrl;

    @JvmField
    @Autowired(name = Constant.FILE_DESCRIPTION)
    String mFileDescription;

    @JvmField
    @Autowired(name = Constant.MESSAGE_TITLE)
    String mMessageTitle;

    @JvmField
    @Autowired(name = Constant.MESSAGE_TIME)
    String mMessageTime;

    @JvmField
    @Autowired(name = Constant.ID)
    long mAlarmId;






    ////////////////////////////////////////////////////////////////////////////////////
    /////////////////////// Methods for BaseGsyPlayerActivity Override /////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected ActivityPlayerMessageBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityPlayerMessageBinding.inflate(inflater);
    }

    @Override
    public boolean isBlackDarkStatus() {
        return false;
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "<onStart>");
        super.onStart();
        messageViewModel.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "<onStop>");
        super.onStop();
        messageViewModel.onStop();
    }


    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        ARouter.getInstance().inject(this);
        getWindow().setBackgroundDrawableResource(R.color.black);
        messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        messageViewModel.setLifecycleOwner(this);
        messageViewModel.setISingleCallback((type, data) -> {
            if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_DELETE_RESULT) {
                mHealthActivityManager.popActivity();
            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_DELETE_FAIL) {
                popupMessage("删除告警视频失败!");
            }
        });


    }

    @Override
    public void requestData() {
        getBinding().tvMsgTitle.setText(mMessageTitle);
        getBinding().tvMsgDesc.setText(mFileDescription);
        getBinding().tvMsgTime.setText(mMessageTime);
        setGsyPlayerInfo(mFileUrl, "");
        getBinding().gsyPlayer.setMute(false);
        getBinding().gsyPlayer.startPlay();
        isPlaying = true;
        getBinding().ivPlaying.setSelected(true);
    }


    @Override
    public void initListener() {
        getBinding().gsyPlayer.iSingleCallback = (type, data) -> {
            getBinding().gsyPlayer.post(() -> {
                if (type == Constant.CALLBACK_TYPE_PLAYER_CURRENT_PROGRESS) {
                    long progress = (Long)data;
                    getBinding().pbPlayProgress.setProgress((int) progress);
                    getBinding().pbPlayProgressFull.setProgress((int) progress);
                } else if (type == Constant.CALLBACK_TYPE_PLAYER_CURRENT_TIME) {
                    getBinding().tvCurrentTime.setText(StringUtils.INSTANCE.getDurationTimeSS((long)data / 1000));
                } else if (type == Constant.CALLBACK_TYPE_PLAYER_TOTAL_TIME) {
                    getBinding().tvTotalTime.setText(StringUtils.INSTANCE.getDurationTimeSS((long)data / 1000));
                }
            });
        };

        getBinding().saveBg.setOnClickListener(view -> PagePilotManager.pageAlbum());

        getBinding().btnAlbum.setOnClickListener(view -> PagePilotManager.pageAlbum());
        getBinding().btnSelectLegibilityFull.setOnClickListener(view -> PagePilotManager.pageAlbum());

        getBinding().cbChangeSound.setOnCheckedChangeListener((compoundButton, b) -> {
            getBinding().gsyPlayer.setMute(!b);
        });
        getBinding().cbChangeSoundFull.setOnCheckedChangeListener((compoundButton, b) -> {
            getBinding().gsyPlayer.setMute(!b);
        });

        getBinding().ivDownload.setOnClickListener(view -> ToastUtils.INSTANCE.showToast(getString(R.string.function_not_open)));
        getBinding().ivDownloadFull.setOnClickListener(view -> ToastUtils.INSTANCE.showToast(getString(R.string.function_not_open)));

        getBinding().ivPlaying.setOnClickListener(view -> {
            if (isPlaying) {
                getBinding().ivPlaying.setSelected(false);
                getBinding().gsyPlayer.pausePlay();
            } else {
                getBinding().gsyPlayer.resumePlay();
                getBinding().ivPlaying.setSelected(true);
            }
            isPlaying = !isPlaying;
        });
        getBinding().ivPlayingFull.setOnClickListener(view -> {
            if (isPlaying) {
                getBinding().ivPlayingFull.setSelected(false);
                getBinding().gsyPlayer.pausePlay();
            } else {
                getBinding().gsyPlayer.resumePlay();
                getBinding().ivPlayingFull.setSelected(true);
            }
            isPlaying = !isPlaying;
        });

        getBinding().ivClip.setOnClickListener(view -> {
            onBtnScreenshot();
        });
        getBinding().ivClipFull.setOnClickListener(view -> {
            onBtnScreenshot();
        });

        getBinding().ivDelete.setOnClickListener(view -> showDeleteMediaTipDialog());
        getBinding().ivDeleteFull.setOnClickListener(view -> showDeleteMediaTipDialog());

        getBinding().pbPlayProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long time = seekBar.getProgress() * getBinding().gsyPlayer.getDuration() / 100;
                getBinding().gsyPlayer.getGSYVideoManager().seekTo(time);
            }
        });
        getBinding().pbPlayProgressFull.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long time = seekBar.getProgress() * getBinding().gsyPlayer.getDuration() / 100;
                getBinding().gsyPlayer.getGSYVideoManager().seekTo(time);
            }
        });

        getBinding().ivChangeScreen.setOnClickListener(view -> onBtnLandscape());
        getBinding().ivBackFull.setOnClickListener(view -> onBtnLandscape());

        getBinding().landscapeLayout.setOnClickListener(view -> onTapLandscapeLayout());
    }

    /**
     * @brief 旋转按钮，切换横竖屏显示
     */
    public void onBtnLandscape() {
        ConstraintLayout.LayoutParams lpPlayer = (ConstraintLayout.LayoutParams) getBinding().gsyPlayer.getLayoutParams();
        if (mIsOrientLandscape) {
            // 设置为竖屏显示,恢复原先显示标记
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(mUiOptionsOld);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            // 显示相应的控件
            mUiState = UI_STATE_PORTRAIT;
            showUiWidgets();

            // 调整播放器控件显示大小
            if (lpPlayer == null) {
                lpPlayer = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ScreenUtils.dp2px(200));
            } else {
                lpPlayer.width = me.jessyan.autosize.utils.ScreenUtils.getScreenSize(this)[1];
                lpPlayer.height = ScreenUtils.dp2px(200);
            }
            lpPlayer.topMargin = ScreenUtils.dp2px(260);
            getBinding().gsyPlayer.setLayoutParams(lpPlayer);

        } else {
            // 调整为横屏显示
            View decorView = getWindow().getDecorView();
            mUiOptionsOld = decorView.getSystemUiVisibility();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            // 显示相应的控件
            mUiState = UI_STATE_LANDSCAPE;
            showUiWidgets();

            // 调整播放器控件显示大小
            if (lpPlayer == null) {
                lpPlayer = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
            } else {
                lpPlayer.width = me.jessyan.autosize.utils.ScreenUtils.getScreenSize(this)[1];
                lpPlayer.height = me.jessyan.autosize.utils.ScreenUtils.getScreenSize(this)[0];
            }
            lpPlayer.topMargin = ScreenUtils.dp2px(0);
            getBinding().gsyPlayer.setLayoutParams(lpPlayer);
        }

        mIsOrientLandscape = !mIsOrientLandscape;
    }

    /**
     * @brief 横屏状态下，Touch事件触发相应控件显示或者隐藏
     */
    void onTapLandscapeLayout() {
        if (!mIsOrientLandscape) {
            return;
        }
        if (mUiState == UI_STATE_PORTRAIT) {
            return;
        }

        if (mUiState == UI_STATE_FULL) {
            Log.d(TAG, "<onPlaygerWgtTap> UI_STATE_FULL ==> UI_STATE_LANDSCAPE");
            mUiState = UI_STATE_LANDSCAPE;
            showUiWidgets();

        } else {
            Log.d(TAG, "<onPlaygerWgtTap> UI_STATE_LANDSCAPE ==> UI_STATE_FULL");
            mUiState = UI_STATE_FULL;
            showUiWidgets();
        }
    }

    /**
     * @brief 全根据当前不同的UI状态，显示相应的控件
     */
    void showUiWidgets() {
        switch (mUiState) {
            case UI_STATE_FULL: {      // 全屏显示
                // 隐藏所有竖屏控件
                showPortraitWgts(View.GONE);

                // 显示横屏布局
                getBinding().landscapeLayout.setVisibility(View.VISIBLE);
                showLandScapeWgts(View.GONE);
            } break;

            case UI_STATE_LANDSCAPE: {  // 横屏显示
                // 隐藏所有竖屏控件
                showPortraitWgts(View.GONE);

                // 显示横屏布局
                getBinding().landscapeLayout.setVisibility(View.VISIBLE);
                showLandScapeWgts(View.VISIBLE);
            } break;

            default: {
                // 隐藏横屏布局
                getBinding().landscapeLayout.setVisibility(View.GONE);

                // 显示所有竖屏控件
                showPortraitWgts(View.VISIBLE);
            } break;
        }
    }

    /**
     * @brief 横屏控件的显示控制
     */
    private void showLandScapeWgts(int visibility) {
        getBinding().ivPowerBgFull.setVisibility(visibility);
        getBinding().pbPowerValueFull.setVisibility(visibility);
        getBinding().cbChangeSoundFull.setVisibility(visibility);
        getBinding().ivDownloadFull.setVisibility(visibility);
        getBinding().ivPlayingFull.setVisibility(visibility);
        getBinding().ivDeleteFull.setVisibility(visibility);
        getBinding().ivDeleteFull.setVisibility(visibility);
        getBinding().ivClipFull.setVisibility(visibility);
        getBinding().tvCurrentTimeFull.setVisibility(visibility);
        getBinding().pbPlayProgressFull.setVisibility(visibility);
        getBinding().tvTotalTimeFull.setVisibility(visibility);
        getBinding().ivBackFull.setVisibility(visibility);

        getBinding().ivPlayingFull.setSelected(isPlaying);
        boolean bSound = (!getBinding().gsyPlayer.isMute());
        getBinding().cbChangeSoundFull.setChecked(bSound);
    }

    /**
     * @brief 竖屏控件的显示控制
     */
    private void showPortraitWgts(int visibility) {
        getBinding().titleView.setVisibility(visibility);

        getBinding().btnAlbum.setVisibility(visibility);
        getBinding().cbChangeSound.setVisibility(visibility);

        getBinding().tvCurrentTime.setVisibility(visibility);
        getBinding().pbPlayProgress.setVisibility(visibility);
        getBinding().tvTotalTime.setVisibility(visibility);

        getBinding().bgMessage.setVisibility(visibility);
        getBinding().tvMsgTitle.setVisibility(visibility);
        getBinding().tvMsgDesc.setVisibility(visibility);
        getBinding().tvMsgTime.setVisibility(visibility);

        getBinding().ivButtonBg.setVisibility(visibility);
        getBinding().ivChangeScreen.setVisibility(visibility);
        getBinding().ivDownload.setVisibility(visibility);
        getBinding().ivPlaying.setVisibility(visibility);
        getBinding().ivClip.setVisibility(visibility);
        getBinding().ivDelete.setVisibility(visibility);

        getBinding().ivPlaying.setSelected(isPlaying);
        boolean bSound = (!getBinding().gsyPlayer.isMute());
        getBinding().cbChangeSound.setChecked(bSound);
    }

    /**
     * @brief 播放视频帧截图保存
     *
     */
    private void onBtnScreenshot() {
//        //
//        // 截图写存储 权限判断处理
//        //
//        int[] permIdArray = new int[1];
//        permIdArray[0] = PermissionHandler.PERM_ID_WRITE_STORAGE;
//        mPermHandler = new PermissionHandler(this, this, permIdArray);
//        if (!mPermHandler.isAllPermissionGranted()) {
//            Log.d(TAG, "<onBtnScreenshot> requesting permission...");
//            mPermHandler.requestNextPermission();
//        } else {
//            Log.d(TAG, "<onBtnScreenshot> permission ready");
//            captureFrame();
//        }
        captureFrame();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

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
                captureFrame();
            } else {
                popupMessage(getString(R.string.no_permission));
            }
        }
    }

    private void captureFrame() {
        getBinding().gsyPlayer.taskShotPic(bitmap -> {
            getBinding().ivCover.setImageBitmap(bitmap);
            boolean ret = FileUtils.saveScreenshotToSD(bitmap,
                    FileUtils.getFileSavePath(String.valueOf(System.currentTimeMillis()), true));
            if (ret) {
                showSaveTip(false);
            } else {
                ToastUtils.INSTANCE.showToast("保存截图失败");
            }
        });
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

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mIsOrientLandscape) {
                onBtnLandscape();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected CustomStandardGSYVideoPlayer getStandardGSYVideoPlayer() {
        return getBinding().gsyPlayer;
    }

    private void showDeleteMediaTipDialog() {
        if (deleteMediaTipDialog == null) {
            deleteMediaTipDialog = new DeleteMediaTipDialog(this);
            deleteMediaTipDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {

                }

                @Override
                public void onRightButtonClick() {
                    List<Long> list = new ArrayList<>();
                    list.add(mAlarmId);
                    messageViewModel.requestDeleteAlarmMgr(list);
                }
            });
        }
        deleteMediaTipDialog.show();
    }



    @Override
    protected void onPlayerPrepared(final String url) {
        Log.d(TAG, "<onPlayerPrepared> url=" + url);
    }

    @Override
    protected void onPlayerAutoComplete(final String url) {
        Log.d(TAG, "<onPlayerAutoComplete> url=" + url);
        getBinding().gsyPlayer.post(() -> {
            onMsgPlayerCompleted();
        });
    }

    @Override
    protected void onPlayerComplete(final String url) {
        Log.d(TAG, "<onPlayerComplete> url=" + url);
    }

    /**
     * @brief 播放完成事件
     */
    private void onMsgPlayerCompleted() {
        Log.d(TAG, "<onMsgPlayerCompleted> ");

        if (mIsOrientLandscape) {
            // 退出全屏显示
            onBtnLandscape();
        }

        // 退出当前界面
        finish();
    }

}
