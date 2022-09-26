package io.agora.iotlinkdemo.models.device.setting.mydevice;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.agora.baselibrary.listener.ISingleCallback;
import com.agora.baselibrary.utils.NetUtils;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityDeviceShareToUserListBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.device.DeviceViewModel;
import io.agora.iotlinkdemo.models.device.setting.adapter.ShareToUserAdapter;
import io.agora.iotlink.IAccountMgr;
import io.agora.iotlink.IotOutSharer;
import com.alibaba.android.arouter.facade.annotation.Route;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备共享出去的用户列表
 * <p>
 * 交付测试前sdk 未提供此功能
 */
@Route(path = PagePathConstant.pageDeviceShareToUserList)
public class DeviceShareToUserListActivity extends BaseViewBindingActivity<ActivityDeviceShareToUserListBinding> {

    /**
     * 设备模块统一ViewModel
     */
    private DeviceViewModel deviceViewModel;

    private ShareToUserAdapter shareToUserAdapter;
    private ArrayList<IotOutSharer> devices = new ArrayList<>();

    @Override
    protected ActivityDeviceShareToUserListBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDeviceShareToUserListBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        deviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        deviceViewModel.setLifecycleOwner(this);
        getBinding().tvAddShare.setOnClickListener(view -> PagePilotManager.pageDeviceShareToUserAdd());
        shareToUserAdapter = new ShareToUserAdapter(devices);
        getBinding().rvUserList.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvUserList.setAdapter(shareToUserAdapter);
    }

    @Override
    public void initListener() {
        deviceViewModel.setISingleCallback((type, data) -> {
            devices.clear();
            if (type == Constant.CALLBACK_TYPE_DEVICE_SHARE_TO_LIST_SUCCESS) {
                devices.addAll((List<IotOutSharer>) data);
                getBinding().rvUserList.post(() -> {
                    if (devices.isEmpty()) {
                        getBinding().tvShareTips1.setText("可视门铃暂未共享给其他人");
                        getBinding().tvShareTips2.setVisibility(View.VISIBLE);
                    } else {
                        getBinding().tvShareTips1.setText("可视门铃已共享给");
                        getBinding().tvShareTips2.setVisibility(View.GONE);
                    }
                    shareToUserAdapter.notifyItemChanged(devices.size());
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (NetUtils.INSTANCE.isNetworkConnected()) {
            deviceViewModel.queryOutSharerList(AgoraApplication.getInstance().getLivingDevice().mDeviceID);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        deviceViewModel.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        deviceViewModel.onStop();
    }
}
