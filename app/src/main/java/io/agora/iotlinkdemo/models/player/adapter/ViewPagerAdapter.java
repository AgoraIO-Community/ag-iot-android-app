package io.agora.iotlinkdemo.models.player.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;

import java.util.ArrayList;

public class ViewPagerAdapter extends FragmentStateAdapter {
    public ArrayList<Fragment> registeredFragments = new ArrayList<>();

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        registeredFragments.add(PagePilotManager.getFragmentPage(PagePathConstant.pagePlayerFunction, null));
        registeredFragments.add(PagePilotManager.getFragmentPage(PagePathConstant.pagePlayerMessage, null));
        registeredFragments.add(PagePilotManager.getFragmentPage(PagePathConstant.pagePlayerRtm, null));
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
