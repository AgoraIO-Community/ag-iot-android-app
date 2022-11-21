package io.agora.iotlinkdemo.models.device.add.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.wifi.ScanResult;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.agora.baselibrary.base.BaseAdapter;
import com.agora.baselibrary.utils.StringUtils;

import io.agora.iotlink.IotDevice;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.common.GlideApp;
import io.agora.iotlinkdemo.manager.DevicesListManager;


import java.util.ArrayList;
import java.util.List;

public class BtDevListAdapter extends BaseAdapter<android.bluetooth.le.ScanResult> {

    private Drawable mSelectDrawable = null;
    private int mSelectPosition = -1;   ///< 当前选中的项目


    public BtDevListAdapter(Context ctx, ArrayList<android.bluetooth.le.ScanResult> btDevices) {
        super(btDevices);
        mSelectDrawable = ContextCompat.getDrawable(ctx, R.mipmap.albumselected);
        mSelectDrawable.setBounds(0, 0, mSelectDrawable.getMinimumWidth(), mSelectDrawable.getMinimumHeight());
    }

    public void setSelectPosition(int selectPosition) {
        mSelectPosition = selectPosition;
    }

    public int getSelectPosition() {
        return mSelectPosition;
    }


    @Override
    public int getLayoutId(int viewType) {
        return R.layout.item_bt_device_list;
    }


    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder holder, int position) {
        android.bluetooth.le.ScanResult btDevice = getDatas().get(position);
        AppCompatTextView devNameView = holder.getView(R.id.tvBtDevName);

        String devName = "";
        String devAddress = "";
        try {
            devName = btDevice.getDevice().getName();
            devAddress = btDevice.getDevice().getAddress();
        } catch (SecurityException securityExp) {
            securityExp.printStackTrace();
        }
        holder.setText(R.id.tvBtDevName, devName);

        if (position == mSelectPosition) {
            devNameView.setCompoundDrawables(null, null, mSelectDrawable, null);
        } else {
            devNameView.setCompoundDrawables(null, null, null, null);
        }

        holder.itemView.setOnClickListener(view -> getMRVItemClickListener().onItemClick(view, position, btDevice));

    }
}
