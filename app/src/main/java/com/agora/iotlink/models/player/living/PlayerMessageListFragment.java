package com.agora.iotlink.models.player.living;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ScreenUtils;
import com.agora.baselibrary.utils.StringUtils;
import com.agora.baselibrary.utils.ToastUtils;
import com.agora.iotlink.R;
import com.agora.iotlink.base.AgoraApplication;
import com.agora.iotlink.common.Constant;
import com.agora.iotlink.databinding.FagmentPlayerMessageBinding;
import com.agora.iotlink.dialog.DeleteMediaTipDialog;
import com.agora.iotlink.dialog.SelectVideoTypeDialog;
import com.agora.iotlink.manager.PagePathConstant;
import com.agora.iotlink.manager.PagePilotManager;
import com.agora.iotlink.models.message.MessageViewModel;
import com.agora.iotlink.models.player.BaseGsyPlayerFragment;
import com.agora.iotlink.models.player.adapter.PreviewMessageAdapter;
import com.agora.iotlink.utils.FileUtils;
import com.agora.iotsdk20.IotAlarm;
import com.agora.iotsdk20.IotAlarmPage;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 播放页功能列表
 */
@Route(path = PagePathConstant.pagePlayerMessage)
public class PlayerMessageListFragment extends BaseGsyPlayerFragment<FagmentPlayerMessageBinding> {

    /**
     * 消息ViewModel
     */
    private MessageViewModel messageViewModel;

    /**
     * 删除对话框
     */
    private DeleteMediaTipDialog deleteMediaTipDialog;
    /**
     * 筛选视频类型话框
     */
    private SelectVideoTypeDialog selectVideoTypeDialog;

    /**
     * 筛选视频类型话框
     */
    private PreviewMessageAdapter previewMessageAdapter;
    /**
     * 消息列表
     */
    private ArrayList<IotAlarm> mMessages = new ArrayList<>();

    private MessageViewModel.CustomDate customDate = new MessageViewModel.CustomDate();

    private boolean isDeleteCurrent = false;

    /**
     * 当前播放的IotAlarm
     */
    private IotAlarm currentIotAlarm = null;

    private boolean isPlaying = false;

    private LinearLayoutManager linearLayoutManager;

    @NonNull
    @Override
    protected FagmentPlayerMessageBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FagmentPlayerMessageBinding.inflate(inflater);
    }

    @Override
    public void initView() {
        super.initView();
        messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        messageViewModel.setLifecycleOwner(this);
        getBinding().gsyPlayer.findViewById(R.id.back).setVisibility(View.GONE);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        getBinding().rvMsgList.setLayoutManager(linearLayoutManager);
        previewMessageAdapter = new PreviewMessageAdapter(mMessages);
        getBinding().rvMsgList.setAdapter(previewMessageAdapter);
        getBinding().calendarView.setMaxDate(System.currentTimeMillis());
    }

    @Override
    protected StandardGSYVideoPlayer getStandardGSYVideoPlayer() {
        return getBinding().gsyPlayer;
    }

    public void requestMsgData() {
        getSelectDate();
        messageViewModel.requestAllAlarmMgr();
    }

    @Override
    public void onResume() {
        super.onResume();
        requestMsgData();
    }

    private void getSelectDate() {
        if (getBinding().btnSelectDate.getText().equals("今天")) {
            Calendar calendar = Calendar.getInstance();
            customDate.year = calendar.get(Calendar.YEAR);
            customDate.month = calendar.get(Calendar.MONTH) + 1;
            customDate.day = calendar.get(Calendar.DAY_OF_MONTH);
        }
//        messageViewModel.queryParam.mDeviceId = AgoraApplication.getInstance().getLivingDevice().mDeviceId;
        messageViewModel.queryParam.mBeginDate = messageViewModel.beginDateToString(customDate);
        messageViewModel.queryParam.mEndDate = messageViewModel.endDateToString(customDate);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void initListener() {
        messageViewModel.setISingleCallback((type, data) -> {
            if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_QUERY_RESULT) {
                if (data instanceof IotAlarmPage) {
                    mMessages.clear();
                    mMessages.addAll(((IotAlarmPage) data).mAlarmList);
                    getBinding().rvMsgList.post(() -> {
                        previewMessageAdapter.notifyDataSetChanged();
                    });
                    if (!mMessages.isEmpty()) {
                        messageViewModel.requestAlarmMgrDetailById(mMessages.get(0).mAlarmId);
                    }
                }
            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_DETAIL_RESULT) {
                if (data instanceof IotAlarm) {
                    currentIotAlarm = (IotAlarm) data;
                    setGsyPlayerInfo(((IotAlarm) data).mFileUrl, "");
                    getBinding().gsyPlayer.startPlay();
                    getBinding().ivPlaying.post(() -> {
                        getBinding().ivPlaying.setSelected(true);
                        if (previewMessageAdapter.oldPlayPosition == -1) {
                            changePlayItem(0);
                        }
                    });
                    isPlaying = true;
                }
            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_DELETE_RESULT) {
                requestMsgData();
            }
        });
        previewMessageAdapter.setMRVItemClickListener((view, position, data) -> {
            messageViewModel.requestAlarmMgrDetailById(data.mAlarmId);
            changePlayItem(position);

        });
        getBinding().gsyPlayer.iSingleCallback = (type, data) -> {
            if (type == Constant.CALLBACK_TYPE_PLAYER_CURRENT_PROGRESS) {
                getBinding().pbPlayProgress.setProgress((int) data);
                getBinding().pbPlayProgressFull.setProgress((int) data);
            } else if (type == Constant.CALLBACK_TYPE_PLAYER_CURRENT_TIME) {
                getBinding().tvCurrentTime.setText(StringUtils.INSTANCE.getDurationTimeSS((int) data / 1000));
                getBinding().tvCurrentTimeFull.setText(StringUtils.INSTANCE.getDurationTimeSS((int) data / 1000));
            } else if (type == Constant.CALLBACK_TYPE_PLAYER_TOTAL_TIME) {
                getBinding().tvTotalTime.setText(StringUtils.INSTANCE.getDurationTimeSS((int) data / 1000));
                getBinding().tvTotalTimeFull.setText(StringUtils.INSTANCE.getDurationTimeSS((int) data / 1000));
            }
        };

        getBinding().cbChangeSound.setOnCheckedChangeListener((compoundButton, b) -> getBinding().gsyPlayer.setMute(b));
        getBinding().cbChangeSoundFull.setOnCheckedChangeListener((compoundButton, b) -> getBinding().gsyPlayer.setMute(b));

        getBinding().ivPlaying.setOnClickListener(view -> changePlayingStatus());
        getBinding().ivPlayingFull.setOnClickListener(view -> changePlayingStatus());


        getBinding().saveBg.setOnClickListener(view -> PagePilotManager.pageAlbum());

        getBinding().ivClip.setOnClickListener(view -> saveShot());
        getBinding().ivClipFull.setOnClickListener(view -> saveShot());

        getBinding().ivDelete.setOnClickListener(view -> {
            isDeleteCurrent = true;
            showDeleteMediaTipDialog();
        });
        getBinding().ivDeleteFull.setOnClickListener(view -> {
            isDeleteCurrent = true;
            showDeleteMediaTipDialog();
        });


        getBinding().btnEdit.setOnClickListener(view -> {
            changeEditStatus(!previewMessageAdapter.isEdit);
        });
        getBinding().btnDoDelete.setOnClickListener(view -> {
            showDeleteMediaTipDialog();
        });
        getBinding().cbAllSelect.setOnCheckedChangeListener((compoundButton, b) -> {
            for (IotAlarm iotAlarm : previewMessageAdapter.getDatas()) {
                iotAlarm.mDeleted = b;
            }
            previewMessageAdapter.notifyDataSetChanged();
        });
        getBinding().calendarView.setOnDateChangeListener((calendarView, year, month, dayOfMonth) -> {
            getBinding().btnSelectDate.setText(year + "-" + (month + 1) + "-" + dayOfMonth);
            getBinding().selectBg.setVisibility(View.GONE);
            customDate.year = year;
            customDate.month = month + 1;
            customDate.day = dayOfMonth;
            requestMsgData();
        });
        getBinding().btnSelectDate.setOnClickListener(view -> {
            getBinding().selectBg.setVisibility(View.VISIBLE);
        });
        getBinding().btnSelectType.setOnClickListener(view -> {
            showSelectVideoTypeDialog();
        });
        getBinding().ivClip.setOnClickListener(view -> {
            showSaveTip(false);
        });
        getBinding().ivChangeScreen.setOnClickListener(view -> onBtnLandscape());
        getBinding().ivBack.setOnClickListener(view -> onBtnLandscape());
        getBinding().ivDownload.setOnClickListener(view -> startDownload());
        getBinding().pbPlayProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int time = seekBar.getProgress() * getBinding().gsyPlayer.getDuration() / 100;
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
                int time = seekBar.getProgress() * getBinding().gsyPlayer.getDuration() / 100;
                getBinding().gsyPlayer.getGSYVideoManager().seekTo(time);
            }
        });
        getBinding().landscapeLayout.setOnClickListener(view -> showLandScapeLayout());
    }

    private void showLandScapeLayout() {
        getBinding().ivPowerBgFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().pbPowerValueFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().cbChangeSoundFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().ivDownloadFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().ivPlayingFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().ivDeleteFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().ivDeleteFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().ivClipFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().tvCurrentTimeFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().pbPlayProgressFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().tvTotalTimeFull.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        getBinding().ivBack.setVisibility(getBinding().ivBack.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    public void onBtnBack() {
        if (mIsOrientLandscape) { // 退回到 portrait竖屏显示
            onBtnLandscape();
            getBinding().gsyPlayer.onBackFullscreen();
        }
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
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) getBinding().gsyPlayer.getLayoutParams();
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
            getBinding().tvCurrentTime.setVisibility(View.VISIBLE);
            getBinding().pbPlayProgress.setVisibility(View.VISIBLE);
            getBinding().tvTotalTime.setVisibility(View.VISIBLE);
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

//            View decorView = getActivity().getWindow().getDecorView();
//            uiOptionsOld = decorView.getSystemUiVisibility();
//            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
//            decorView.setSystemUiVisibility(uiOptions);
            ((PlayerPreviewActivity) getActivity()).hideTitle();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getBinding().landscapeLayout.setVisibility(View.VISIBLE);
            getBinding().btnSelectLegibility.setVisibility(View.GONE);
            getBinding().tvCurrentTime.setVisibility(View.GONE);
            getBinding().pbPlayProgress.setVisibility(View.GONE);
            getBinding().tvTotalTime.setVisibility(View.GONE);
            ((ConstraintLayout.LayoutParams) getBinding().saveBg.getLayoutParams()).rightMargin = ScreenUtils.dp2px(90);
        }
        mIsOrientLandscape = !mIsOrientLandscape;
        getBinding().gsyPlayer.setLayoutParams(lp);
    }

    private void changePlayingStatus() {
        if (isPlaying) {
            getBinding().gsyPlayer.pausePlay();
            getBinding().ivPlaying.setSelected(false);
            getBinding().ivPlayingFull.setSelected(false);
        } else {
            getBinding().gsyPlayer.resumePlay();
            getBinding().ivPlaying.setSelected(true);
            getBinding().ivPlayingFull.setSelected(true);
        }
        isPlaying = !isPlaying;
    }

    private void saveShot() {
        getBinding().gsyPlayer.taskShotPic(bitmap -> {
            getBinding().ivCover.setImageBitmap(bitmap);
            boolean ret = FileUtils.saveScreenshotToSD(bitmap,
                    FileUtils.getFileSavePath(AgoraApplication.getInstance().getLivingDevice().mDeviceNumber, true));
            if (ret) {
                showSaveTip(false);
            } else {
                ToastUtils.INSTANCE.showToast("保存截图失败");
            }
        });
    }

    /**
     * 设置item播放状态
     */
    private void changePlayItem(int position) {
        if (previewMessageAdapter.oldPlayPosition != position) {
            previewMessageAdapter.newPosition = position;
            if (previewMessageAdapter.oldPlayPosition != -1) {
                previewMessageAdapter.notifyItemChanged(previewMessageAdapter.oldPlayPosition);
            }
            previewMessageAdapter.notifyItemChanged(position);
            previewMessageAdapter.oldPlayPosition = position;
        }
    }

    private void changeEditStatus(boolean toEdit) {
        if (!toEdit) {
            previewMessageAdapter.isEdit = false;
            getBinding().btnSelectDate.setVisibility(View.VISIBLE);
            getBinding().btnSelectType.setVisibility(View.VISIBLE);
            getBinding().bgBottomDel.setVisibility(View.GONE);
            getBinding().btnEdit.setText("编辑");
        } else {
            getBinding().btnSelectDate.setVisibility(View.INVISIBLE);
            getBinding().btnSelectType.setVisibility(View.INVISIBLE);
            getBinding().bgBottomDel.setVisibility(View.VISIBLE);
            previewMessageAdapter.isEdit = true;
            getBinding().btnEdit.setText("完成");
        }
        previewMessageAdapter.notifyDataSetChanged();
    }

    private void startDownload() {
        showLoadingView();
        ToastUtils.INSTANCE.showToast(getString(R.string.start_download));
        getBinding().ivDownload.postDelayed(() -> stopDownload(), 2000);
    }

    private void stopDownload() {
        hideLoadingView();
        ToastUtils.INSTANCE.showToast(getString(R.string.stop_download));
        showSaveTip(true);
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

    private void showDeleteMediaTipDialog() {
        if (deleteMediaTipDialog == null) {
            deleteMediaTipDialog = new DeleteMediaTipDialog(getActivity());
            deleteMediaTipDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {

                }

                @Override
                public void onRightButtonClick() {
                    doDelete();
                }
            });
        }
        deleteMediaTipDialog.show();
    }

    /**
     * 执行删除
     */
    private void doDelete() {
        List<Long> deletes = new ArrayList<>();
        if (!isDeleteCurrent) {
            deletes.add(currentIotAlarm.mAlarmId);
        } else {
            for (IotAlarm iotAlarm : mMessages) {
                if (iotAlarm.mDeleted) {
                    deletes.add(iotAlarm.mAlarmId);
                }
            }
        }
        if (deletes.isEmpty()) {
            deleteMediaTipDialog.dismiss();
            ToastUtils.INSTANCE.showToast("请选择要删除的消息");
        } else {
            messageViewModel.requestDeleteAlarmMgr(deletes);
            changeEditStatus(false);
        }
    }

    private void showSelectVideoTypeDialog() {
        if (selectVideoTypeDialog == null) {
            selectVideoTypeDialog = new SelectVideoTypeDialog(getActivity());
            selectVideoTypeDialog.iSingleCallback = (type, var2) -> {
                if (type == 1) {
                    getBinding().btnSelectType.setText(getString(R.string.all_type));
                    messageViewModel.queryParam.mMsgType = -1;
                } else if (type == 2) {
                    getBinding().btnSelectType.setText(getString(R.string.sound_detection));
                    messageViewModel.queryParam.mMsgType = 0;
                } else if (type == 3) {
                    getBinding().btnSelectType.setText(getString(R.string.motion_detection));
                    messageViewModel.queryParam.mMsgType = 2;
                } else if (type == 4) {
                    getBinding().btnSelectType.setText(getString(R.string.human_infrared_detection));
                    messageViewModel.queryParam.mMsgType = 1;
                } else if (type == 5) {
                    getBinding().btnSelectType.setText(getString(R.string.call_button));
                    messageViewModel.queryParam.mMsgType = 3;
                }
                requestMsgData();
            };
        }
        selectVideoTypeDialog.show();
    }


    @Override
    public void onStart() {
        super.onStart();
        messageViewModel.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        messageViewModel.onStop();
    }
}
