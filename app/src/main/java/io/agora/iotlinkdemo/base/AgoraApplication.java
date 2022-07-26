package io.agora.iotlinkdemo.base;

import android.app.Activity;
import android.content.res.Configuration;

import io.agora.iotlinkdemo.BuildConfig;
import com.alibaba.android.arouter.launcher.ARouter;

import me.jessyan.autosize.AutoSizeConfig;
import me.jessyan.autosize.onAdaptListener;
import me.jessyan.autosize.utils.ScreenUtils;

public class AgoraApplication extends PushApplication {

    public void onCreate() {
        super.onCreate();
        initARouter();
        initAutoSize();
    }

    private void initARouter() {
        if (BuildConfig.DEBUG) {
            ARouter.openLog();
            ARouter.openDebug();
        }

        ARouter.init(this);
    }

    private void initAutoSize() {
        AutoSizeConfig.getInstance().setOnAdaptListener(new onAdaptListener() {
            @Override
            public void onAdaptBefore(Object o, Activity activity) {
                AutoSizeConfig.getInstance().setScreenWidth(ScreenUtils.getScreenSize(activity)[0]);
                AutoSizeConfig.getInstance().setScreenHeight(ScreenUtils.getScreenSize(activity)[1]);
                if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    AutoSizeConfig.getInstance()
                            .setDesignWidthInDp(812)
                            .setDesignHeightInDp(375);
                } else {
                    AutoSizeConfig.getInstance()
                            .setDesignWidthInDp(375)
                            .setDesignHeightInDp(812);
                }
            }

            @Override
            public void onAdaptAfter(Object o, Activity activity) {

            }
        });
    }
}