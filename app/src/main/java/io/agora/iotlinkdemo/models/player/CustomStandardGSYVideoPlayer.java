package io.agora.iotlinkdemo.models.player;

import android.content.Context;
import android.media.AudioManager;
import android.util.AttributeSet;

import com.agora.baselibrary.listener.ISingleCallback;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import io.agora.iotlinkdemo.common.Constant;

public class CustomStandardGSYVideoPlayer extends StandardGSYVideoPlayer {
    public ISingleCallback<Integer, Object> iSingleCallback;

    public CustomStandardGSYVideoPlayer(Context context, Boolean fullFlag) {
        super(context, fullFlag);
        initView();
    }

    public CustomStandardGSYVideoPlayer(Context context) {
        super(context);
        initView();
    }

    public CustomStandardGSYVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        hideAllWidget();
        setViewShowState(mBottomProgressBar, GONE);
        setMute(true);
//        mBrightnessDialogTv.setVisibility(View.GONE);
//        mDialogSeekTime.setVisibility(View.GONE);
//        mDialogTotalTime.setVisibility(View.GONE);
//        mDialogIcon.setVisibility(View.GONE);
//        mBrightnessDialogTv.setVisibility(View.GONE);
//        mBrightnessDialogTv.setVisibility(View.GONE);
//        mBackButton.setVisibility(View.GONE);
//        mLockScreen.setVisibility(View.GONE);
    }

    @Override
    protected void changeUiToNormal() {
    }

    @Override
    protected void changeUiToPreparingShow() {
    }

    @Override
    protected void changeUiToPlayingShow() {
    }

    @Override
    protected void changeUiToPauseShow() {
    }

    @Override
    protected void changeUiToPlayingBufferingShow() {
    }

    @Override
    protected void changeUiToCompleteShow() {
    }

    @Override
    protected void changeUiToError() {
    }

    @Override
    protected void changeUiToPlayingClear() {
    }

    @Override
    protected void changeUiToPauseClear() {
    }

    /**
     * 是否静音 true 静音 false 不静音
     */
    public void setMute(boolean isMute) {
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, isMute);
    }

    @Override
    protected void setProgressAndTime(long progress, long secProgress, long currentTime, long totalTime, boolean forceChange) {
        if (iSingleCallback != null) {
            iSingleCallback.onSingleCallback(Constant.CALLBACK_TYPE_PLAYER_CURRENT_PROGRESS, progress);
            iSingleCallback.onSingleCallback(Constant.CALLBACK_TYPE_PLAYER_SEC_PROGRESS, secProgress);
            iSingleCallback.onSingleCallback(Constant.CALLBACK_TYPE_PLAYER_CURRENT_TIME, currentTime);
            iSingleCallback.onSingleCallback(Constant.CALLBACK_TYPE_PLAYER_TOTAL_TIME, totalTime);
        }
    }

    public void startPlay() {
        post(this::prepareVideo);
    }


    public void resumePlay() {
        getGSYVideoManager().start();
    }

    public void pausePlay() {
        getGSYVideoManager().pause();
    }
}
