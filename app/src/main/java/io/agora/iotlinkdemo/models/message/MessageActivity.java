package io.agora.iotlinkdemo.models.message;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.databinding.ActivityMessageBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.models.message.adapter.MessageViewPagerAdapter;
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
}
