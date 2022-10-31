package io.agora.iotlinkdemo.models.player;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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

    /**
     * 消息ViewModel
     */
    private MessageViewModel messageViewModel;
    private PermissionHandler mPermHandler;             ///< 权限申请处理

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
    long id;

    /**
     * 删除对话框
     */
    private DeleteMediaTipDialog deleteMediaTipDialog;

    @Override
    protected ActivityPlayerMessageBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityPlayerMessageBinding.inflate(inflater);
    }

    @Override
    public boolean isBlackDarkStatus() {
        return false;
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
            }
        });
    }

    @Override
    public void requestData() {
        getBinding().tvMsgTitle.setText(mMessageTitle);
        getBinding().tvMsgDesc.setText(mFileDescription);
        getBinding().tvMsgTime.setText(mMessageTime);
        setGsyPlayerInfo(mFileUrl, "");
        getBinding().gsyPlayer.startPlay();
        isPlaying = true;
        getBinding().ivPlaying.setSelected(true);
        getBinding().btnSelectLegibility.setOnClickListener(view -> PagePilotManager.pageAlbum());
    }

    private boolean isPlaying = false;

    @Override
    public void initListener() {
        getBinding().gsyPlayer.iSingleCallback = (type, data) -> {
            getBinding().gsyPlayer.post(() -> {
                if (type == Constant.CALLBACK_TYPE_PLAYER_CURRENT_PROGRESS) {
                    long progress = (Long)data;
                    getBinding().pbPlayProgress.setProgress((int) progress);
                } else if (type == Constant.CALLBACK_TYPE_PLAYER_CURRENT_TIME) {
                    getBinding().tvCurrentTime.setText(StringUtils.INSTANCE.getDurationTimeSS((long)data / 1000));
                } else if (type == Constant.CALLBACK_TYPE_PLAYER_TOTAL_TIME) {
                    getBinding().tvTotalTime.setText(StringUtils.INSTANCE.getDurationTimeSS((long)data / 1000));
                }
            });
        };
        getBinding().saveBg.setOnClickListener(view -> PagePilotManager.pageAlbum());
        getBinding().ivDownload.setOnClickListener(view -> ToastUtils.INSTANCE.showToast(getString(R.string.function_not_open)));
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
        getBinding().ivClip.setOnClickListener(view -> {
            onBtnScreenshot();
        });
        getBinding().cbChangeSound.setOnCheckedChangeListener((compoundButton, b) -> {
            getBinding().gsyPlayer.setMute(!b);
        });
        getBinding().ivDelete.setOnClickListener(view -> showDeleteMediaTipDialog());
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
        getBinding().ivChangeScreen.setOnClickListener(view -> onBtnLandscape());
    }

    /**
     * 当前是否正在横屏显示
     */
    public boolean mIsOrientLandscape = false;
    public int uiOptionsOld = 0;

    public void onBtnLandscape() {
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) getBinding().gsyPlayer.getLayoutParams();
        if (mIsOrientLandscape) {
            if (lp == null) {
                lp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ScreenUtils.dp2px(200));
            } else {
                lp.width = me.jessyan.autosize.utils.ScreenUtils.getScreenSize(this)[1];
                lp.height = ScreenUtils.dp2px(200);
            }
            lp.topMargin = ScreenUtils.dp2px(16);
            getWindow().getDecorView().setSystemUiVisibility(uiOptionsOld);
            getBinding().titleView.setVisibility(View.VISIBLE);
            getBinding().btnSelectLegibility.setVisibility(View.VISIBLE);
            getBinding().tvCurrentTime.setVisibility(View.VISIBLE);
            getBinding().pbPlayProgress.setVisibility(View.VISIBLE);
            getBinding().tvTotalTime.setVisibility(View.VISIBLE);
            getBinding().cbChangeSound.setVisibility(View.VISIBLE);

            getBinding().ivButtonBg.setVisibility(View.VISIBLE);
            getBinding().ivChangeScreen.setVisibility(View.VISIBLE);
            getBinding().ivDownload.setVisibility(View.VISIBLE);
            getBinding().ivPlaying.setVisibility(View.VISIBLE);
            getBinding().ivClip.setVisibility(View.VISIBLE);
            getBinding().ivDelete.setVisibility(View.VISIBLE);
            ((ConstraintLayout.LayoutParams) getBinding().saveBg.getLayoutParams()).rightMargin = ScreenUtils.dp2px(15);
        } else {
            if (lp == null) {
                lp = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
            } else {
                lp.width = me.jessyan.autosize.utils.ScreenUtils.getScreenSize(this)[1];
                lp.height = me.jessyan.autosize.utils.ScreenUtils.getScreenSize(this)[0];
            }
            lp.topMargin = ScreenUtils.dp2px(0);
//            View decorView = getWindow().getDecorView();
//            uiOptionsOld = decorView.getSystemUiVisibility();
//            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
//            decorView.setSystemUiVisibility(uiOptions);
            getBinding().titleView.setVisibility(View.GONE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getBinding().btnSelectLegibility.setVisibility(View.GONE);
            getBinding().tvCurrentTime.setVisibility(View.GONE);
            getBinding().pbPlayProgress.setVisibility(View.GONE);
            getBinding().tvTotalTime.setVisibility(View.GONE);
            getBinding().cbChangeSound.setVisibility(View.GONE);

            getBinding().ivButtonBg.setVisibility(View.GONE);
            getBinding().ivChangeScreen.setVisibility(View.GONE);
            getBinding().ivDownload.setVisibility(View.GONE);
            getBinding().ivPlaying.setVisibility(View.GONE);
            getBinding().ivClip.setVisibility(View.GONE);
            getBinding().ivDelete.setVisibility(View.GONE);
            ((ConstraintLayout.LayoutParams) getBinding().saveBg.getLayoutParams()).rightMargin = ScreenUtils.dp2px(90);
        }
        mIsOrientLandscape = !mIsOrientLandscape;
        getBinding().gsyPlayer.setLayoutParams(lp);
    }

    /**
     * @brief 播放视频帧截图保存
     *
     */
    private void onBtnScreenshot() {
        //
        // 截图写存储 权限判断处理
        //
        int[] permIdArray = new int[1];
        permIdArray[0] = PermissionHandler.PERM_ID_WRITE_STORAGE;
        mPermHandler = new PermissionHandler(this, this, permIdArray);
        if (!mPermHandler.isAllPermissionGranted()) {
            Log.d(TAG, "<onBtnScreenshot> requesting permission...");
            mPermHandler.requestNextPermission();
        } else {
            Log.d(TAG, "<onBtnScreenshot> permission ready");
            captureFrame();
        }
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mIsOrientLandscape) {
                getBinding().gsyPlayer.onBackFullscreen();
                onBtnLandscape();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
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
                    list.add(id);
                    messageViewModel.requestDeleteAlarmMgr(list);
                }
            });
        }
        deleteMediaTipDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        messageViewModel.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        messageViewModel.onStop();
    }
}
