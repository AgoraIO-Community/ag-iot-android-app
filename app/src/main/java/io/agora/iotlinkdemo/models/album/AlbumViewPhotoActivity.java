package io.agora.iotlinkdemo.models.album;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.common.GlideApp;
import io.agora.iotlinkdemo.databinding.ActivityAlbumViewPhotoBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.widget.MakeUpZoomImage;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import kotlin.jvm.JvmField;

/**
 * 相册 截图预览
 */
@Route(path = PagePathConstant.pageAlbumViewPhoto)
public class AlbumViewPhotoActivity extends BaseViewBindingActivity<ActivityAlbumViewPhotoBinding> {
    @JvmField
    @Autowired(name = Constant.FILE_URL)
    String fileUrl;

    @JvmField
    @Autowired(name = Constant.TIME)
    String time;

    @JvmField
    @Autowired(name = Constant.TYPE)
    int mMediaType = 0;         // 0: 图片；   1：视频

    @Override
    protected ActivityAlbumViewPhotoBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityAlbumViewPhotoBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        MakeUpZoomImage.attach(this);
        ARouter.getInstance().inject(this);

        if (mMediaType == 1) {  // 视频播放
            getBinding().ivPhoto.setVisibility(View.GONE);
            getBinding().tvPhotoTime.setVisibility(View.GONE);
            getBinding().vvMedia.setVisibility(View.VISIBLE);

            try {
                getBinding().vvMedia.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        finish();
                        popupMessage("播放完成!");
                    }
                });

                getBinding().vvMedia.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean isPlaying = getBinding().vvMedia.isPlaying();
                        if (isPlaying) {
                            getBinding().vvMedia.pause();
                        } else {
                            getBinding().vvMedia.start();
                        }
                    }
                });

                getBinding().vvMedia.setVideoPath(fileUrl);
                getBinding().vvMedia.start();

            } catch (Exception exp) {
                exp.printStackTrace();
            }


        } else {    // 图片显示
            getBinding().ivPhoto.setVisibility(View.VISIBLE);
            getBinding().tvPhotoTime.setVisibility(View.VISIBLE);
            getBinding().vvMedia.setVisibility(View.GONE);

            GlideApp.with(this).load(fileUrl).placeholder(R.mipmap.icon_deft).into(getBinding().ivPhoto);
            getBinding().tvPhotoTime.setText(time);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MakeUpZoomImage.get().release();
    }
}
