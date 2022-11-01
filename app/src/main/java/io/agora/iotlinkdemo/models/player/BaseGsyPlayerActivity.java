package io.agora.iotlinkdemo.models.player;

import android.content.res.Configuration;
import android.util.Log;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder;
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;

public abstract class BaseGsyPlayerActivity<T extends ViewBinding> extends BaseViewBindingActivity<T> {
    private final String TAG = "IOTLINK/BasePlayerAct";

    private CustomStandardGSYVideoPlayer gsyPlayer;
    private final GSYVideoOptionBuilder mGsyVideoOption = new GSYVideoOptionBuilder();

    @Override
    protected void init() {
        super.init();
        gsyPlayer = getStandardGSYVideoPlayer();
        initPlayer();
    }

    protected abstract CustomStandardGSYVideoPlayer getStandardGSYVideoPlayer();


    private OrientationUtils orientationUtils;
    private boolean isPause = true;

    private void initPlayer() {
        //外部辅助的旋转，帮助全屏
        orientationUtils = new OrientationUtils(this, gsyPlayer);
        //初始化不打开外部的旋转
        orientationUtils.setEnable(false);

        gsyPlayer.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //直接横屏
                orientationUtils.resolveByClick();
                //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
                gsyPlayer.startWindowFullscreen(BaseGsyPlayerActivity.this, true, true);
            }
        });

        mGsyVideoOption.setThumbImageView(null)
                .setIsTouchWiget(true)
                .setRotateViewAuto(false)
                .setLockLand(false)
                .setLooping(false)
                .setAutoFullWithSize(true)
                .setShowFullAnimation(false)
                .setNeedLockFull(true)
                .setCacheWithPlay(false)
                .setVideoAllCallBack(new GSYSampleCallBack() {
                    @Override
                    public void onPrepared(String url, Object... objects) {
                        super.onPrepared(url, objects);
                        //开始播放了才能旋转和全屏
                        orientationUtils.setEnable(true);
                        onPlayerPrepared(url);
                    }

                    @Override
                    public void onQuitFullscreen(String url, Object... objects) {
                        super.onQuitFullscreen(url, objects);
                        if (orientationUtils != null) {
                            orientationUtils.backToProtVideo();
                        }
                    }

                    @Override
                    public void onAutoComplete(String url, Object... objects) {
                        onPlayerAutoComplete(url);
                    }

                    @Override
                    public void onComplete(String url, Object... objects) {
                        onPlayerComplete(url);
                    }

                }).setLockClickListener((view, lock) -> {
            if (orientationUtils != null) {
                //配合下方的onConfigurationChanged
                orientationUtils.setEnable(!lock);
            }
        });

    }

    protected void setGsyPlayerInfo(String fileUrl, String fileDescription) {
        mGsyVideoOption.setUrl(fileUrl);
//        mGsyVideoOption,setVideoTitle(fileDescription);
        mGsyVideoOption.build(gsyPlayer);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //如果旋转了就全屏
        if (!isPause) {
            gsyPlayer.onConfigurationChanged(this, newConfig, orientationUtils, true, true);
        }
    }

    public void startPlay() {
        gsyPlayer.startPlay();
    }

    @Override
    public void onBackPressed() {
        if (orientationUtils != null) {
            orientationUtils.backToProtVideo();
        }
        if (GSYVideoManager.backFromWindowFull(this)) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        gsyPlayer.getCurrentPlayer().onVideoPause();
        super.onPause();
        isPause = true;
    }

    @Override
    protected void onResume() {
        gsyPlayer.getCurrentPlayer().onVideoResume(false);
        super.onResume();
        isPause = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gsyPlayer.getCurrentPlayer().release();
        if (orientationUtils != null)
            orientationUtils.releaseListener();
    }

    protected void onPlayerPrepared(final String url) {
        Log.d(TAG, "<onPlayerPrepared> url=" + url);
    }

    protected void onPlayerAutoComplete(final String url) {
        Log.d(TAG, "<onPlayerAutoComplete> url=" + url);
    }

    protected void onPlayerComplete(final String url) {
        Log.d(TAG, "<onPlayerComplete> url=" + url);
    }
}
