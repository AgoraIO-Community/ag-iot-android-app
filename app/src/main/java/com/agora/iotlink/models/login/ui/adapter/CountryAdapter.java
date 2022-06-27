package com.agora.iotlink.models.login.ui.adapter;

import android.view.View;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseAdapter;
import com.agora.iotlink.R;
import com.agora.iotlink.api.bean.CountryBean;

import java.util.ArrayList;

public class CountryAdapter extends BaseAdapter<CountryBean> {

    /**
     * 记录历史选择
     */
    private int oldSelectPosition = -1;

    public CountryAdapter(ArrayList<CountryBean> devices) {
        super(devices);
    }

    @Override
    public int getLayoutId(int viewType) {
        return R.layout.item_country;
    }

    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder holder, int position) {
        CountryBean aCountry = getDatas().get(position);
        holder.setText(R.id.tvCountryName, aCountry.countryName);
        if (aCountry.isSelect) {
            holder.setVisible(R.id.ivSelectStatus, View.VISIBLE);
        } else {
            holder.setVisible(R.id.ivSelectStatus, View.GONE);
        }
        holder.itemView.setTag(position);
        holder.itemView.setOnClickListener(view -> {
            int pos = (int) holder.itemView.getTag();
            holder.setVisible(R.id.ivSelectStatus, View.VISIBLE);
            aCountry.isSelect = true;
            notifyItemChanged(pos);
            if (oldSelectPosition != -1) {
                getDatas().get(oldSelectPosition).isSelect = false;
                notifyItemChanged(oldSelectPosition);
            }
            oldSelectPosition = pos;
            getMRVItemClickListener().onItemClick(holder.itemView, pos, aCountry);
        });
    }
}
