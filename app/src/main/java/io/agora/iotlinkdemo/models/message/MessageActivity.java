package io.agora.iotlinkdemo.models.message;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.databinding.ActivityMessageBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.models.message.adapter.MessageViewPagerAdapter;
import io.agora.iotlinkdemo.models.player.adapter.ViewPagerAdapter;
import io.agora.iotlinkdemo.models.player.living.PlayerFunctionListFragment;
import io.agora.iotlinkdemo.models.player.living.PlayerMessageListFragment;
import io.agora.iotlinkdemo.models.player.living.PlayerRtcFragment;
import io.agora.iotlinkdemo.models.player.living.PlayerRtmFragment;
import io.agora.iotlinkdemo.utils.AnimUtils;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 消息
 */
@Route(path = PagePathConstant.pageMessage)
public class MessageActivity extends BaseViewBindingActivity<ActivityMessageBinding> {

    @Override
    protected ActivityMessageBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityMessageBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        getBinding().viewPager.setAdapter(new MessageViewPagerAdapter(this));
        AnimUtils.radioGroupLineAnim(getBinding().rBtnAlarm, getBinding().lineCheck);
        getBinding().rBtnAlarm.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                getBinding().viewPager.setCurrentItem(0);
            }
        });
        getBinding().rBtnNotification.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                getBinding().viewPager.setCurrentItem(1);
            }
        });
        getBinding().viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == 0) {
                    getBinding().rBtnAlarm.setChecked(true);
                    AnimUtils.radioGroupLineAnim(getBinding().rBtnAlarm, getBinding().lineCheck);
                } else {
                    getBinding().rBtnNotification.setChecked(true);
                    AnimUtils.radioGroupLineAnim(getBinding().rBtnNotification, getBinding().lineCheck);
                }
            }
        });
    }

    public void setAlarmCount(final long count) {
        getBinding().tvAlbumCount.post(() -> {
            if (count > 0) {
                getBinding().tvAlbumCount.setVisibility(View.VISIBLE);
                if (count > 99) {
                    getBinding().tvAlbumCount.setText(String.valueOf(99));
                } else {
                    getBinding().tvAlbumCount.setText(String.valueOf(count));
                }

            } else {
                getBinding().tvAlbumCount.setVisibility(View.GONE);
            }
        });

    }

    public void setNotificationCount(final long count) {
        getBinding().tvNotificationCount.post(() -> {
            if (count > 0) {
                getBinding().tvNotificationCount.setVisibility(View.VISIBLE);
                if (count > 99) {
                    getBinding().tvNotificationCount.setText(String.valueOf(99));
                } else {
                    getBinding().tvNotificationCount.setText(String.valueOf(count));
                }
            } else {
                getBinding().tvNotificationCount.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            boolean bHookBackKey = false;
            if (getBinding().viewPager.getCurrentItem() == 0) {
                bHookBackKey = ((MessageAlarmFragment) ((MessageViewPagerAdapter) getBinding().viewPager.getAdapter())
                        .registeredFragments.get(0)).onBtnBack();
            } else if (getBinding().viewPager.getCurrentItem() == 1) {
                bHookBackKey = ((MessageNotifyFragment) ((MessageViewPagerAdapter) getBinding().viewPager.getAdapter())
                        .registeredFragments.get(1)).onBtnBack();
            }
            if (bHookBackKey) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
