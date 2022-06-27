package com.agora.iotlink.models.device.add;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.common.Constant;
import com.agora.iotlink.databinding.ActivityWifiListBinding;
import com.agora.iotlink.manager.PagePathConstant;
import com.agora.iotlink.models.device.add.adapter.WifiAdapter;
import com.agora.iotlink.utils.WifiUtils;
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
