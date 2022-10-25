package io.agora.iotlinkdemo.models.home.homeindex.adapter;

import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.agora.baselibrary.base.BaseAdapter;
import com.agora.baselibrary.utils.StringUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlink.IotDevice;
import io.agora.iotlinkdemo.common.GlideApp;
import io.agora.iotlinkdemo.manager.DevicesListManager;

import java.util.ArrayList;

public class DevicesAdapter extends BaseAdapter<IotDevice> {

    public DevicesAdapter(ArrayList<IotDevice> devices) {
        super(devices);
    }

    @Override
    public int getLayoutId(int viewType) {
        if (viewType == 199) {
            return R.layout.item_device_title;
        } else {
            return R.layout.item_device_list;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (getDatas().get(position).mDeviceID.equals("199")) {
            return Integer.parseInt(getDatas().get(position).mDeviceID);
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder holder, int position) {
        IotDevice device = getDatas().get(position);
        if (!getDatas().get(position).mDeviceID.equals("199")) {
            if (device != null) {
                // 查询产品图片并显示
                String smallImgUrl = DevicesListManager.getInstance().getProductSmallImg(device.mProductID);
                if (!TextUtils.isEmpty(smallImgUrl)) {
                    GlideApp.with(getMContext()).load(smallImgUrl).placeholder(R.mipmap.img1).
                            into((AppCompatImageView) holder.getView(R.id.ivDeviceIcon));
                }

                holder.setText(R.id.tvDeviceName, StringUtils.INSTANCE.getBase64String(device.mDeviceName));
                holder.itemView.setOnClickListener(view -> getMRVItemClickListener().onItemClick(view, position, device));
                if (device.mConnected) {
                    holder.setTextColor(R.id.tvDeviceStatus, ContextCompat.getColor(getMContext(), R.color.black_50_percent));
                    holder.setVisible(R.id.tvDeviceStatus, View.GONE);
//                    holder.setText(R.id.tvDeviceStatus, "移动侦测 " + StringUtils.INSTANCE.getDetailTime("MM-dd HH:mm", device.mUpdateTime * 1000));
                } else {
                    holder.setTextColor(R.id.tvDeviceStatus, ContextCompat.getColor(getMContext(), R.color.yellow_f7));
                    holder.setVisible(R.id.tvDeviceStatus, View.VISIBLE);
                    holder.setText(R.id.tvDeviceStatus, getMContext().getString(R.string.offline));
                }
            }
        }
    }
}
