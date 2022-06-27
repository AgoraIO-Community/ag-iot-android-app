package com.agora.iotlink.models.message.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.agora.iotlink.manager.PagePathConstant;
import com.agora.iotlink.manager.PagePilotManager;

import java.util.ArrayList;

public class MessageViewPagerAdapter extends FragmentStateAdapter {
    private ArrayList<Fragment> registeredFragments = new ArrayList<>();

    public MessageViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        registeredFragments.add(PagePilotManager.getFragmentPage(PagePathConstant.pageMessageAlarm, null));
        registeredFragments.add(PagePilotManager.getFragmentPage(PagePathConstant.pageMessageNotify, null));

    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return registeredFragments.get(position);
    }

    @Override
    public int getItemCount() {
        return registeredFragments.size();
    }
}
