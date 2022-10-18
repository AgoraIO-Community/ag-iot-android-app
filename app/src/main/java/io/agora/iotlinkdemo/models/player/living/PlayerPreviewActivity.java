package io.agora.iotlinkdemo.models.player.living;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.agora.baselibrary.utils.StringUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.databinding.ActivityPreviewPlayBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.home.homeindex.HomeIndexFragment;
import io.agora.iotlinkdemo.models.player.adapter.ViewPagerAdapter;
import io.agora.iotlinkdemo.utils.AnimUtils;
import io.agora.iotlink.IotDevice;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

/**
 * 实时预览 播放页
 */
@Route(path = PagePathConstant.pagePreviewPlay)
public class PlayerPreviewActivity extends BaseViewBindingActivity<ActivityPreviewPlayBinding> {
    private static final String TAG = "LINK/PlayerPrevAct";

    @Override
    protected ActivityPreviewPlayBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityPreviewPlayBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        getWindow().setBackgroundDrawableResource(R.color.black);
        getBinding().viewPager.setAdapter(new ViewPagerAdapter(this));
        AnimUtils.radioGroupLineAnim(getBinding().rBtnFunction, getBinding().lineCheck);
    }

    @Override
    public void initListener() {
        getBinding().titleView.setRightIconClick(view -> {
            if ("0".equals(AgoraApplication.getInstance().getLivingDevice().mSharer)) {
                PagePilotManager.pageDeviceSetting();
            } else {
                //分享的设备走这里
                PagePilotManager.pageShareDeviceSetting();
            }
        });
        getBinding().radioGroup.setOnCheckedChangeListener((radioGroup, i) -> {

        });
        getBinding().rBtnFunction.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                getBinding().viewPager.setCurrentItem(0);
            }
        });
        getBinding().rBtnMessage.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                getBinding().viewPager.setCurrentItem(1);
            }
        });
        getBinding().rBtnControl.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                getBinding().viewPager.setCurrentItem(2);
            }
        });
        getBinding().rBtnVideo.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                getBinding().viewPager.setCurrentItem(3);
            }
        });
        getBinding().viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == 0) {
                    getBinding().rBtnFunction.setChecked(true);
                    AnimUtils.radioGroupLineAnim(getBinding().rBtnFunction, getBinding().lineCheck);
                } else if (position == 1) {
                    getBinding().rBtnMessage.setChecked(true);
                    AnimUtils.radioGroupLineAnim(getBinding().rBtnMessage, getBinding().lineCheck);
                } else if (position == 2) {
                    getBinding().rBtnControl.setChecked(true);
                    AnimUtils.radioGroupLineAnim(getBinding().rBtnControl, getBinding().lineCheck);
                } else if (position == 3) {
                    getBinding().rBtnVideo.setChecked(true);
                    AnimUtils.radioGroupLineAnim(getBinding().rBtnVideo, getBinding().lineCheck);
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            boolean bHookBackKey = false;
            if (getBinding().viewPager.getCurrentItem() == 0) {
                bHookBackKey = ((PlayerFunctionListFragment) ((ViewPagerAdapter) getBinding().viewPager.getAdapter())
                         .registeredFragments.get(0)).onBtnBack();
            } else if (getBinding().viewPager.getCurrentItem() == 1) {
                bHookBackKey = ((PlayerMessageListFragment) ((ViewPagerAdapter) getBinding().viewPager.getAdapter())
                        .registeredFragments.get(1)).onBtnBack();
            } else if (getBinding().viewPager.getCurrentItem() == 2) {
                bHookBackKey = ((PlayerRtmFragment) ((ViewPagerAdapter) getBinding().viewPager.getAdapter())
                        .registeredFragments.get(2)).onBtnBack();
            } else if (getBinding().viewPager.getCurrentItem() == 3) {
                bHookBackKey = ((PlayerRtcFragment) ((ViewPagerAdapter) getBinding().viewPager.getAdapter())
                        .registeredFragments.get(3)).onBtnBack();
            }
            if (bHookBackKey) {
                return true;
            }
         }
        return super.onKeyDown(keyCode, event);
    }

    public void showTitle() {
        getBinding().titleView.setVisibility(View.VISIBLE);
        getBinding().viewPager.setUserInputEnabled(true);
        getBinding().radioGroup.setVisibility(View.VISIBLE);
        getBinding().lineCheck.setVisibility(View.VISIBLE);
    }

    public void hideTitle() {
        getBinding().titleView.setVisibility(View.GONE);
        getBinding().viewPager.setUserInputEnabled(false);
        getBinding().radioGroup.setVisibility(View.GONE);
        getBinding().lineCheck.setVisibility(View.GONE);
    }

    /**
     * @brief 根据是否有固件版本 更新标题栏小红点
     */
    public void updateTitle(boolean newFirmwareVersion) {
        int resId;
        if (newFirmwareVersion) {
            resId = R.mipmap.settingnewver;
        } else {
            resId = R.mipmap.setting;
        }

        Resources res = getBaseContext().getResources();
        Bitmap newVerBmp = BitmapFactory.decodeResource(res, resId);
        Drawable drawable = new BitmapDrawable(res, newVerBmp);
        getBinding().titleView.updateRightIcon(drawable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IotDevice livingDevice = AgoraApplication.getInstance().getLivingDevice();
        if (!TextUtils.isEmpty(StringUtils.INSTANCE.getBase64String(livingDevice.mDeviceName))) {
            getBinding().titleView.setTitle(StringUtils.INSTANCE.getBase64String(livingDevice.mDeviceName));
        } else {
            getBinding().titleView.setTitle("");
        }
    }

    @Override
    public boolean isBlackDarkStatus() {
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "<onRequestPermissionsResult> requestCode=" + requestCode);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (getBinding().viewPager.getCurrentItem() == 0) {
            PlayerFunctionListFragment devPlayerFrag;
            devPlayerFrag = ((PlayerFunctionListFragment)((ViewPagerAdapter) getBinding().viewPager.getAdapter()).registeredFragments.get(0));
            if (devPlayerFrag != null) {
                devPlayerFrag.onFragRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }
}
