package io.agora.iotlinkdemo.models.home.homemine;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.utils.NetUtils;
import com.agora.baselibrary.utils.StringUtils;

import io.agora.avmodule.AvMediaInfo;
import io.agora.iotlink.AlarmVideoDownloader;
import io.agora.iotlink.ErrCode;
import io.agora.iotlinkdemo.base.BaseViewBindingFragment;
import io.agora.iotlinkdemo.databinding.FragmentHomeMineBinding;
import io.agora.iotlinkdemo.manager.DevicesListManager;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.usercenter.UserInfoViewModel;
import io.agora.iotlinkdemo.thirdpartyaccount.ThirdAccountMgr;


public class MineFragment extends BaseViewBindingFragment<FragmentHomeMineBinding>
        implements AlarmVideoDownloader.ICallback {
    private static final String TAG = "LINK/MineFragment";
    private static final String DEF_PROGRESS_TEXT = "00:00:00 / 00:00:00";
    private static final long REFRESH_PROGRESS_TIMER = 200;     // 每隔200ms刷新一次转换进度

    //
    // message Id
    //
    private static final int MSGID_DNLOAD_PROGRESS = 0x1001;   ///< 下载转换进度


    /**
     * 用户相关viewModel
     */
    private UserInfoViewModel userInfoViewModel;

    private Handler mMsgHandler = null;                 ///< 主线程中的消息处理
    private AvMediaInfo mMediaInfo;


    @NonNull
    @Override
    protected FragmentHomeMineBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomeMineBinding.inflate(inflater);
    }

    @Override
    public void initView() {
        userInfoViewModel = new ViewModelProvider(this).get(UserInfoViewModel.class);
        userInfoViewModel.setLifecycleOwner(this);
        getBinding().ivToEdit.setVisibility(View.INVISIBLE);
        getBinding().vToEdit.setVisibility(View.INVISIBLE);
    }

    @Override
    public void initListener() {

        String accountName = ThirdAccountMgr.getInstance().getLoginAccountName();
        getBinding().tvUserMobile.setText(accountName);

        getBinding().vToEdit.setOnClickListener(view -> {
            if (NetUtils.INSTANCE.isNetworkConnected()) {
            //    PagePilotManager.pageUserInfo();
            }
        });
        getBinding().tvGeneralSettings.setOnClickListener(view -> {
            if (NetUtils.INSTANCE.isNetworkConnected()) {
                PagePilotManager.pageGeneralSettings();
            }
        });
        getBinding().tvMsgCenter.setOnClickListener(view -> {
            if (NetUtils.INSTANCE.isNetworkConnected()) {
                PagePilotManager.pageMessage();
            }
        });

        getBinding().tvAbout.setOnClickListener(view -> PagePilotManager.pageAbout());
        setUserInfo();

//        getBinding().btnStartStop.setOnClickListener(view -> {
//            onBtnStartStop();
//        });
//
//        getBinding().btnPauseResume.setOnClickListener(view -> {
//            onBtnPauseResume();
//        });
     }

    private void setUserInfo() {
        getBinding().tvUserMobile.post(() -> {
            String accountName = ThirdAccountMgr.getInstance().getLoginAccountName();
            String accountId = ThirdAccountMgr.getInstance().getLoginAccountId();
            String txtName = accountName + "\n (" + accountId + ")";
            getBinding().tvUserMobile.setText(txtName);

            int count = DevicesListManager.deviceSize;
            getBinding().tvDeviceCount.setText(count + " 台设备");
         });

    }

    @Override
    public void requestData() {
        if (NetUtils.INSTANCE.isNetworkConnected()) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        userInfoViewModel.onStart();

        // 创建主线程消息处理
        mMsgHandler = new Handler(getActivity().getMainLooper())
        {
            @SuppressLint("HandlerLeak")
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case MSGID_DNLOAD_PROGRESS:
                        onMsgDnloadProgress(msg);
                        break;
                }
            }
        };
    }

    @Override
    public void onStop() {
        super.onStop();
        userInfoViewModel.onStop();

        if (mMsgHandler != null) {
            mMsgHandler.removeMessages(MSGID_DNLOAD_PROGRESS);
            mMsgHandler = null;
        }
    }





    AlarmVideoDownloader mVideoDnloader = null;

    void onBtnStartStop() {
        if (mVideoDnloader == null) {   // 启动下载流程
            downloadStart();

        } else {    // 停止正在进行的下载
            downloadStop();
            popupMessage("Stopped media file downloading!");
        }
    }

    void onBtnPauseResume() {
        if (mVideoDnloader == null) {
            return;
        }
        if (mMediaInfo == null) {
            popupMessage("Downloading not prepared!");
            return;
        }

        int state = mVideoDnloader.getState();
        int ret;

        if (state == AlarmVideoDownloader.STATE_PAUSED) {
            ret = mVideoDnloader.resume();
            if (ret == ErrCode.XOK) {
                getBinding().btnPauseResume.setText("Pause");
            } else {
                Log.e(TAG, "<onBtnPauseResume> resume error, ret=" + ret);
            }

        } else if (state == AlarmVideoDownloader.STATE_ONGOING) {
            ret = mVideoDnloader.pause();
            if (ret == ErrCode.XOK) {
                getBinding().btnPauseResume.setText("Resume");
            } else {
                Log.e(TAG, "<onBtnPauseResume> pause error, ret=" + ret);
            }

        } else {
            popupMessage("Bad state, state=" + state);
        }
    }


    /**
     * @brief 启动下载处理
     */
    void downloadStart() {
        String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        //String cloudVideoUrl = storagePath + "/daughter.mp4";
        String cloudVideoUrl = "xxxxxxxxx.m3u8";
        String localPath = storagePath + "/download_out.mp4";
        getBinding().pbDownloading.setProgress(0);
        getBinding().tvProgress.setText(DEF_PROGRESS_TEXT);
        mVideoDnloader = new AlarmVideoDownloader();
        int ret = mVideoDnloader.start(cloudVideoUrl, localPath, this);
        if (ret == ErrCode.XOK) {
            popupMessage("Start downloading successful...");
            getBinding().btnStartStop.setText("Stop");
        } else {
            mVideoDnloader = null;
            popupMessage("Start downloading failure!");
        }
    }

    /**
     * @brief 结束下载处理
     */
    void downloadStop() {
        if (mVideoDnloader != null) {
            mVideoDnloader.stop();
            mVideoDnloader = null;
        }
        mMediaInfo = null;

        getBinding().btnStartStop.setText("Start");
        getBinding().btnPauseResume.setText("Pause");
        getBinding().pbDownloading.setProgress(0);
        getBinding().tvProgress.setText(DEF_PROGRESS_TEXT);

        if (mMsgHandler != null) {
            mMsgHandler.removeMessages(MSGID_DNLOAD_PROGRESS);
        }
    }

    /**
     * @brief 定时消息，刷新下载进度
     */
    void onMsgDnloadProgress(Message msg) {
        if (mVideoDnloader == null) {
            Log.e(TAG, "<onMsgDnloadProgress> invalid downloader");
            return;
        }
        if (mMediaInfo == null) {
            Log.e(TAG, "<onMsgDnloadProgress> invalid media info");
            return;
        }
        if (mMediaInfo.mVideoDuration <= 0) {
            mMsgHandler.removeMessages(MSGID_DNLOAD_PROGRESS);
            mMsgHandler.sendEmptyMessageDelayed(MSGID_DNLOAD_PROGRESS, REFRESH_PROGRESS_TIMER);
            return;
        }

        long videoDuration = mMediaInfo.mVideoDuration;
        long videoTimestamp = mVideoDnloader.getVideoTimestamp();
        if (videoTimestamp > videoDuration) {
            videoTimestamp = videoDuration;
        }

        // 进度条调整
        int progress = (int)(videoTimestamp * 1000 / videoDuration);
        getBinding().pbDownloading.setProgress(progress);

        // 进度时间显示
        String txtTimestamp = timestampToString(videoTimestamp);
        String txtDuration = timestampToString(videoDuration);
        String txtProgress = txtTimestamp + " / " + txtDuration;
        getBinding().tvProgress.setText(txtProgress);

        // 定时刷新一次
        mMsgHandler.removeMessages(MSGID_DNLOAD_PROGRESS);
        mMsgHandler.sendEmptyMessageDelayed(MSGID_DNLOAD_PROGRESS, REFRESH_PROGRESS_TIMER);
        Log.d(TAG, "<onMsgDnloadProgress> done, progress=" + progress
                    + ", txtProgress=" + txtProgress
                    + ", videoTimestamp=" + videoTimestamp
                    + ", videoDuration=" + videoDuration       );
    }



    @Override
    public void onDownloadPrepared(final String videoUrl, final AvMediaInfo mediaInfo) {
        mMediaInfo = mediaInfo;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 开始刷新进度
                if (mMsgHandler != null) {
                    mMsgHandler.sendEmptyMessage(MSGID_DNLOAD_PROGRESS);
                }
            }
        });
    }

    @Override
    public void onDownloadDone(final String videoUrl)  {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                downloadStop();
                popupMessage("Downloading complete!");
            }
        });
    }

    @Override
    public void onDownloadError(final String videoUrl, int errCode) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                downloadStop();
                popupMessage("Downloading error, errCode=" + errCode);
            }
        });
    }


    String timestampToString(long timestamp) {
        String txtTime = "00:00:00";
        long timeSeconds = (timestamp / 1000) / 1000;

        long hour = timeSeconds / 3600;
        long minute = (timeSeconds - hour*3600) / 60;
        long second = timeSeconds - hour*3600 - minute*60;
        txtTime = String.format("%02d:%02d:%02d", hour, minute, second);
        return txtTime;
    }
}
