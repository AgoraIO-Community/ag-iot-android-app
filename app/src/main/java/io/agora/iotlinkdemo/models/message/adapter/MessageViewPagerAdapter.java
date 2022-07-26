package io.agora.iotlinkdemo.models.message.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;

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
