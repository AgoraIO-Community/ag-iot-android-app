package io.agora.iotlinkdemo.models.player;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
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
    private final String TAG = "IOTLINK/BasePlayerFrag";

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


    private boolean isPause = true;

    private void initPlayer() {
        gsyPlayer.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
                gsyPlayer.startWindowFullscreen(getActivity(), true, true);
            }
        });

        mGsyVideoOption.setThumbImageView(null)
                .setIsTouchWiget(true)
                .setRotateViewAuto(false)
                .setLooping(false)
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
                        onPlayerPrepared(url);
                    }

                    @Override
                    public void onQuitFullscreen(String url, Object... objects) {
                        super.onQuitFullscreen(url, objects);
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
        Log.d(TAG, "<onConfigurationChanged> ");
        super.onConfigurationChanged(newConfig);
        //如果旋转了就全屏
//        if (!isPause) {
//            gsyPlayer.onConfigurationChanged(getActivity(), newConfig, null, true, true);
//        }
    }

//    @Override
//    public void onBackPressed() {
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
