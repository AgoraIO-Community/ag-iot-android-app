package com.agora.iotlink.models.device.add.adapter;

import android.net.wifi.ScanResult;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseAdapter;
import com.agora.iotlink.R;

import java.util.ArrayList;

public class WifiAdapter extends BaseAdapter<ScanResult> {

    public WifiAdapter(ArrayList<ScanResult> scanResults) {
        super(scanResults);
    }

    @Override
    public int getLayoutId(int viewType) {
        return R.layout.item_wifi_list;
    }

    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder holder, int position) {
        ScanResult scanResult = getDatas().get(position);
        if (scanResult != null) {
            holder.setText(R.id.tvWifiName, scanResult.SSID);
            holder.itemView.setOnClickListener(view ->
                    getMRVItemClickListener().onItemClick(view, position, scanResult));
        }

    }
}
