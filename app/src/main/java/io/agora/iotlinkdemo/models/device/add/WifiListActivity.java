package io.agora.iotlinkdemo.models.device.add;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityWifiListBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.models.device.add.adapter.WifiAdapter;
import io.agora.iotlinkdemo.utils.WifiUtils;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 可用wifi列表
 */
@Route(path = PagePathConstant.pageWifiList)
public class WifiListActivity extends BaseViewBindingActivity<ActivityWifiListBinding> {
    private WifiAdapter wifiAdapter;

    @Override
    protected ActivityWifiListBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityWifiListBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        wifiAdapter = new WifiAdapter(WifiUtils.getWifiList());
        getBinding().rvWifiList.setAdapter(wifiAdapter);
        getBinding().rvWifiList.setLayoutManager(new LinearLayoutManager(this));
        wifiAdapter.setMRVItemClickListener((view, position, data) -> {
            Intent intent = new Intent();
            intent.putExtra(Constant.SSID, data.SSID);
            setResult(RESULT_OK, intent);
            mHealthActivityManager.popActivity();
        });
    }
}
