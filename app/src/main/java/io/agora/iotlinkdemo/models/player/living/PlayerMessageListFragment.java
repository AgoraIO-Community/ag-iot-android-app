package io.agora.iotlinkdemo.models.player.living;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.util.Log;
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
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.base.PermissionHandler;
import io.agora.iotlinkdemo.base.PermissionItem;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.FagmentPlayerMessageBinding;
import io.agora.iotlinkdemo.dialog.DeleteMediaTipDialog;
import io.agora.iotlinkdemo.dialog.SelectVideoTypeDialog;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.message.MessageViewModel;
import io.agora.iotlinkdemo.models.player.BaseGsyPlayerFragment;
import io.agora.iotlinkdemo.models.player.adapter.PreviewMessageAdapter;
import io.agora.iotlinkdemo.utils.FileUtils;
import io.agora.iotlink.IotAlarm;
import io.agora.iotlink.IotAlarmPage;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 播放页功能列表
 */
@Route(path = PagePathConstant.pagePlayerMessage)
public class PlayerMessageListFragment extends BaseGsyPlayerFragment<FagmentPlayerMessageBinding>
        implements PermissionHandler.ICallback {
    private final String TAG = "IOTLINK/PlayMsgFrag";
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

    private PermissionHandler mPermHandler;             ///< 权限申请处理

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
        Log.d(TAG, "<initView> done");
    }

    @Override
    protected StandardGSYVideoPlayer getStandardGSYVideoPlayer() {
        return getBinding().gsyPlayer;
    }

    public void requestMsgData() {
        getSelectDate();
        messageViewModel.queryAlarmsByFilter();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "<onResume>");
        requestMsgData();
    }

    private void getSelectDate() {
        if (getBinding().btnSelectDate.getText().equals("今天")) {
            Calendar calendar = Calendar.getInstance();
            customDate.year = calendar.get(Calendar.YEAR);
            customDate.month = calendar.get(Calendar.MONTH) + 1;
            customDate.day = calendar.get(Calendar.DAY_OF_MONTH);
        }
        messageViewModel.setQueryDeviceID(AgoraApplication.getInstance().getLivingDevice().mDeviceID);
        messageViewModel.setQueryBeginDate(customDate);
        messageViewModel.setQueryEndDate(customDate);
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

                        IotAlarmPage alarmPage = (IotAlarmPage)data;
                        if (alarmPage.mAlarmList.size() <= 0) {
                            // 没有告警消息，播放组件不能用
                            getBinding().cbChangeSound.setEnabled(false);
                            getBinding().pbPlayProgress.setEnabled(false);
                            getBinding().pbPlayProgressFull.setEnabled(false);
                            getBinding().ivChangeScreen.setEnabled(false);
                            getBinding().ivDownload.setEnabled(false);
                            getBinding().ivPlaying.setEnabled(false);
                            getBinding().ivClip.setEnabled(false);
                            getBinding().ivDelete.setEnabled(false);
                            getBinding().btnEdit.setEnabled(false);

                        } else {
                            // 有告警消息，播放组件可以使用
                            getBinding().cbChangeSound.setEnabled(true);
                            getBinding().pbPlayProgress.setEnabled(true);
                            getBinding().pbPlayProgressFull.setEnabled(true);
                            getBinding().ivChangeScreen.setEnabled(true);
                            getBinding().ivDownload.setEnabled(true);
                            getBinding().ivPlaying.setEnabled(true);
                            getBinding().ivClip.setEnabled(true);
                            getBinding().ivDelete.setEnabled(true);
                            getBinding().btnEdit.setEnabled(true);
                        }
                    });
                    if (!mMessages.isEmpty()) {
                        messageViewModel.requestAlarmMgrDetailById(mMessages.get(0).mAlarmId);
                    }
                }
            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_DETAIL_RESULT) {
                if (data instanceof IotAlarm) {
                    currentIotAlarm = (IotAlarm) data;
                    setGsyPlayerInfo(((IotAlarm) data).mVideoUrl, "");
                    Log.d(TAG, "<initListener.setISingleCallback> [CALLBACK_TYPE_MESSAGE_ALARM_DETAIL_RESULT]"
                                + ", url=" + ((IotAlarm) data).mVideoUrl);

                    boolean muted;
                    if (mIsOrientLandscape) {
                        muted = !(getBinding().cbChangeSoundFull.isChecked());
                    } else {
                        muted = !(getBinding().cbChangeSound.isChecked());
                    }
                    getBinding().gsyPlayer.setMute(muted);
                    getBinding().gsyPlayer.startPlay();

                    getBinding().ivPlaying.post(() -> {
                        getBinding().ivPlaying.setSelected(true);
                        if (previewMessageAdapter.oldPlayPosition == -1) {
                            changePlayItem(0);
                        }
                    });
                    isPlaying = true;
                }

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_QUERY_FAIL) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int errCode = (Integer)data;
                        popupMessage("查询告警消息失败, 错误码: " + errCode);
                    }
                });

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_DETAIL_FAIL) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int errCode = (Integer) data;
                        popupMessage("查询告警详情失败, 错误码: " + errCode);
                    }
                });

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_DELETE_RESULT) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        popupMessage("删除告警消息成功!");
                    }
                });
                requestMsgData();

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_DELETE_FAIL) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int errCode = (Integer)data;
                        popupMessage("删除告警信息失败, 错误码: " + errCode);
                    }
                });
            }
        });
        previewMessageAdapter.setMRVItemClickListener((view, position, data) -> {
            messageViewModel.requestAlarmMgrDetailById(data.mAlarmId);
            changePlayItem(position);

        });
        getBinding().gsyPlayer.iSingleCallback = (type, data) -> {
            if (type == Constant.CALLBACK_TYPE_PLAYER_CURRENT_PROGRESS) {
                long progress = (Long)data;
                getBinding().pbPlayProgress.setProgress((int) progress);
                getBinding().pbPlayProgressFull.setProgress((int) progress);

            } else if (type == Constant.CALLBACK_TYPE_PLAYER_CURRENT_TIME) {
                getBinding().tvCurrentTime.setText(StringUtils.INSTANCE.getDurationTimeSS((long) data / 1000));
                getBinding().tvCurrentTimeFull.setText(StringUtils.INSTANCE.getDurationTimeSS((long) data / 1000));

            } else if (type == Constant.CALLBACK_TYPE_PLAYER_TOTAL_TIME) {
                getBinding().tvTotalTime.setText(StringUtils.INSTANCE.getDurationTimeSS((long) data / 1000));
                getBinding().tvTotalTimeFull.setText(StringUtils.INSTANCE.getDurationTimeSS((long) data / 1000));

            }
        };

        getBinding().cbChangeSound.setOnCheckedChangeListener((compoundButton, b) -> getBinding().gsyPlayer.setMute(!b));
        getBinding().cbChangeSoundFull.setOnCheckedChangeListener((compoundButton, b) -> getBinding().gsyPlayer.setMute(!b));

        getBinding().ivPlaying.setOnClickListener(view -> changePlayingStatus());
        getBinding().ivPlayingFull.setOnClickListener(view -> changePlayingStatus());


        getBinding().saveBg.setOnClickListener(view -> PagePilotManager.pageAlbum());

        getBinding().ivClip.setOnClickListener(view -> onBtnScreenshot());
        getBinding().ivClipFull.setOnClickListener(view -> onBtnScreenshot());

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
        getBinding().ivChangeScreen.setOnClickListener(view -> onBtnLandscape());
        getBinding().ivBack.setOnClickListener(view -> onBtnLandscape());
        getBinding().ivDownload.setOnClickListener(view -> startDownload());
        getBinding().ivDownloadFull.setOnClickListener(view -> startDownload());
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
        getBinding().landscapeLayout.setOnClickListener(view -> showLandScapeLayout());
        Log.d(TAG, "<initListener> done");
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
        getBinding().ivPlayingFull.setSelected(isPlaying);
        boolean bSound = (!getBinding().gsyPlayer.isMute());
        getBinding().cbChangeSoundFull.setChecked(bSound);
    }

    public boolean onBtnBack() {
        if (mIsOrientLandscape) { // 退回到 portrait竖屏显示
            onBtnLandscape();
            return true;
        }

        if (previewMessageAdapter.isEdit) {
            changeEditStatus(false);
            return true;
        }

        return false;
    }

    /**
     * 当前是否正在横屏显示
     */
    private boolean mIsOrientLandscape = false;
    private int uiOptionsOld = 0;

    /**
     * 横屏切换
     */
    public void onBtnLandscape() {
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) getBinding().gsyPlayer.getLayoutParams();
        if (mIsOrientLandscape) {
            // 设置为竖屏显示,恢复原先显示标记
            View decorView = getActivity().getWindow().getDecorView();
            decorView.setSystemUiVisibility(uiOptionsOld);
            ((PlayerPreviewActivity) getActivity()).showTitle();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            // 隐藏横屏布局，显示竖屏控件
            getBinding().landscapeLayout.setVisibility(View.GONE);
            getBinding().btnSelectLegibility.setVisibility(View.VISIBLE);
            getBinding().tvCurrentTime.setVisibility(View.VISIBLE);
            getBinding().pbPlayProgress.setVisibility(View.VISIBLE);
            getBinding().tvTotalTime.setVisibility(View.VISIBLE);
            getBinding().ivChangeScreen.setVisibility(View.VISIBLE);
            getBinding().ivDownload.setVisibility(View.VISIBLE);
            getBinding().ivPlaying.setVisibility(View.VISIBLE);
            getBinding().ivClip.setVisibility(View.VISIBLE);
            getBinding().ivDelete.setVisibility(View.VISIBLE);
            ((ConstraintLayout.LayoutParams) getBinding().saveBg.getLayoutParams()).rightMargin = ScreenUtils.dp2px(15);
            getBinding().ivPlaying.setSelected(isPlaying);
            boolean bSound = (!getBinding().gsyPlayer.isMute());
            getBinding().cbChangeSound.setChecked(bSound);

            // 调整播放器显示控件大小
            if (lp == null) {
                lp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ScreenUtils.dp2px(200));
            } else {
                lp.width = me.jessyan.autosize.utils.ScreenUtils.getScreenSize(getActivity())[1];
                lp.height = ScreenUtils.dp2px(200);
            }
            lp.topMargin = ScreenUtils.dp2px(55);
            getBinding().gsyPlayer.setLayoutParams(lp);

        } else {
            // 调整为横屏全屏显示
            View decorView = getActivity().getWindow().getDecorView();
            uiOptionsOld = decorView.getSystemUiVisibility();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            ((PlayerPreviewActivity) getActivity()).hideTitle();

            // 隐藏竖屏控件，显示横屏布局
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getBinding().landscapeLayout.setVisibility(View.VISIBLE);
            getBinding().btnSelectLegibility.setVisibility(View.GONE);
            getBinding().tvCurrentTime.setVisibility(View.GONE);
            getBinding().pbPlayProgress.setVisibility(View.GONE);
            getBinding().tvTotalTime.setVisibility(View.GONE);
            getBinding().ivChangeScreen.setVisibility(View.GONE);
            getBinding().ivDownload.setVisibility(View.GONE);
            getBinding().ivPlaying.setVisibility(View.GONE);
            getBinding().ivClip.setVisibility(View.GONE);
            getBinding().ivDelete.setVisibility(View.GONE);
            ((ConstraintLayout.LayoutParams) getBinding().saveBg.getLayoutParams()).rightMargin = ScreenUtils.dp2px(90);
            getBinding().ivPlayingFull.setSelected(isPlaying);
            boolean bSound = (!getBinding().gsyPlayer.isMute());
            getBinding().cbChangeSoundFull.setChecked(bSound);

            // 调整播放器显示控件大小
            if (lp == null) {
                lp = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
            } else {
                lp.width = me.jessyan.autosize.utils.ScreenUtils.getScreenSize(getActivity())[1];
                lp.height = me.jessyan.autosize.utils.ScreenUtils.getScreenSize(getActivity())[0];
            }
            lp.topMargin = ScreenUtils.dp2px(0);
            getBinding().gsyPlayer.setLayoutParams(lp);
        }
        mIsOrientLandscape = !mIsOrientLandscape;
    }

    private void changePlayingStatus() {
        if (currentIotAlarm == null) {
            return;
        }

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

    /**
     * @brief 云录播放视频帧截图
     */
    private void onBtnScreenshot() {
//        //
//        // 截图写存储 权限判断处理
//        //
//        int[] permIdArray = new int[1];
//        permIdArray[0] = PermissionHandler.PERM_ID_WRITE_STORAGE;
//        mPermHandler = new PermissionHandler(getActivity(), this, permIdArray);
//        if (!mPermHandler.isAllPermissionGranted()) {
//            Log.d(TAG, "<onBtnScreenshot> requesting permission...");
//            mPermHandler.requestNextPermission();
//        } else {
//            Log.d(TAG, "<onBtnScreenshot> permission ready");
//            saveShot();
//        }

        saveShot();
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
                saveShot();
            } else {
                popupMessage(getString(R.string.no_permission));
            }
        }
    }


    private void saveShot() {
        getBinding().gsyPlayer.taskShotPic(bitmap -> {
            getBinding().ivCover.setImageBitmap(bitmap);
            boolean ret = FileUtils.saveScreenshotToSD(bitmap,
                    FileUtils.getFileSavePath(AgoraApplication.getInstance().getLivingDevice().mDeviceID, true));
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
        ToastUtils.INSTANCE.showToast(getString(R.string.function_not_open));
//        showLoadingView();
//        ToastUtils.INSTANCE.showToast(getString(R.string.start_download));
//        getBinding().ivDownload.postDelayed(() -> stopDownload(), 2000);
    }

    private void stopDownload() {
//        hideLoadingView();
//        ToastUtils.INSTANCE.showToast(getString(R.string.stop_download));
//        showSaveTip(true);
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
                    messageViewModel.setQueryMsgType(-1);
                } else if (type == 2) {
                    getBinding().btnSelectType.setText(getString(R.string.sound_detection));
                    messageViewModel.setQueryMsgType(0);
                } else if (type == 3) {
                    getBinding().btnSelectType.setText(getString(R.string.motion_detection));
                    messageViewModel.setQueryMsgType(2);
                } else if (type == 4) {
                    getBinding().btnSelectType.setText(getString(R.string.human_infrared_detection));
                    messageViewModel.setQueryMsgType(1);
                } else if (type == 5) {
                    getBinding().btnSelectType.setText(getString(R.string.call_button));
                    messageViewModel.setQueryMsgType(3);
                }
                requestMsgData();
            };
        }
        selectVideoTypeDialog.show();
    }


    @Override
    public void onStart() {
        Log.d(TAG, "<onStart>");
        super.onStart();
        messageViewModel.onStart();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "<onStop>");
        super.onStop();
        messageViewModel.onStop();
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

        // 将播放按钮和进度条复位
        isPlaying = false;
        getBinding().ivPlaying.setSelected(false);
        getBinding().ivPlayingFull.setSelected(false);
        getBinding().pbPlayProgress.setProgress(0);
        getBinding().pbPlayProgressFull.setProgress(0);
        getBinding().tvCurrentTime.setText(StringUtils.INSTANCE.getDurationTimeSS(0));
        getBinding().tvCurrentTimeFull.setText(StringUtils.INSTANCE.getDurationTimeSS(0));
        getBinding().tvTotalTime.setText(StringUtils.INSTANCE.getDurationTimeSS(0));
        getBinding().tvTotalTimeFull.setText(StringUtils.INSTANCE.getDurationTimeSS(0));
        currentIotAlarm = null;
        changePlayItem(-1);

        getBinding().gsyPlayer.releaseAll();
    }
}
