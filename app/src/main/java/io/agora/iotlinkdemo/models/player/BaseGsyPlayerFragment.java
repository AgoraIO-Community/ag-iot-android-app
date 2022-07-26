package io.agora.iotlinkdemo.models.player;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import io.agora.iotlinkdemo.base.BaseViewBindingFragment;
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder;
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

public abstract class BaseGsyPlayerFragment<T extends ViewBinding> extends BaseViewBindingFragment<T> {

    private StandardGSYVideoPlayer gsyPlayer;
    private final GSYVideoOptionBuilder mGsyVideoOption = new GSYVideoOptionBuilder();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void initView() {
        gsyPlayer = getStandardGSYVideoPlayer();
        initPlayer();
    }

    protected abstract StandardGSYVideoPlayer getStandardGSYVideoPlayer();


    private OrientationUtils orientationUtils;
    private boolean isPause = true;

    private void initPlayer() {
        //外部辅助的旋转，帮助全屏
        orientationUtils = new OrientationUtils(getActivity(), gsyPlayer);
        //初始化不打开外部的旋转
        orientationUtils.setEnable(false);
        gsyPlayer.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //直接横屏
                orientationUtils.resolveByClick();
                //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
                gsyPlayer.startWindowFullscreen(getActivity(), true, true);
            }
        });

        mGsyVideoOption.setThumbImageView(null)
                .setIsTouchWiget(true)
                .setRotateViewAuto(false)
                .setLooping(true)
                .setLockLand(false)
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
                    }

                    @Override
                    public void onQuitFullscreen(String url, Object... objects) {
                        super.onQuitFullscreen(url, objects);
                        if (orientationUtils != null) {
                            orientationUtils.backToProtVideo();
                        }
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
        gsyPlayer.post(() -> {
            mGsyVideoOption.build(gsyPlayer);
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //如果旋转了就全屏
        if (!isPause) {
            gsyPlayer.onConfigurationChanged(getActivity(), newConfig, orientationUtils, true, true);
        }
    }

//    @Override
//    public void onBackPressed() {
//        if (orientationUtils != null) {
//            orientationUtils.backToProtVideo();
//        }
//        if (GSYVideoManager.backFromWindowFull(this)) {
//            return;
//        }
//        super.onBackPressed();
//    }

    @Override
    public void onPause() {
        gsyPlayer.getCurrentPlayer().onVideoPause();
        super.onPause();
        isPause = true;
    }

    @Override
    public void onResume() {
        gsyPlayer.getCurrentPlayer().onVideoResume(false);
        super.onResume();
        isPause = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        gsyPlayer.getCurrentPlayer().release();
        if (orientationUtils != null)
            orientationUtils.releaseListener();
    }
}
