package io.agora.iotlinkdemo.models.album;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;

import com.agora.baselibrary.base.BaseAdapter;
import com.agora.baselibrary.utils.StringUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.api.bean.AlbumBean;
import io.agora.iotlinkdemo.common.CenterCropRoundCornerTransform;
import io.agora.iotlinkdemo.common.GlideApp;

import java.util.ArrayList;

public class AlbumAdapter extends BaseAdapter<AlbumBean> {
    /**
     * 是否处于编辑模式
     */
    public boolean isEdit = false;

    public AlbumAdapter(ArrayList<AlbumBean> albumData) {
        super(albumData);
    }

    @Override
    public int getLayoutId(int viewType) {
        if (viewType == 0) {
            return R.layout.item_alarm_date;
        } else {
            return R.layout.item_alarm_data;
        }

    }

    @Override
    public int getItemViewType(int position) {
        return getDatas().get(position).itemType;
    }

    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder holder, int position) {
        AlbumBean albumBean = getDatas().get(position);
        if (albumBean.itemType == 0) {
            holder.setText(R.id.tvDate, albumBean.date);
        } else {
            GlideApp.with(getMContext()).load(albumBean.mediaCover).placeholder(R.mipmap.icon_deft)
                    .transform(new CenterCropRoundCornerTransform(10))
                    .into((AppCompatImageView) holder.getView(R.id.ivMediaCover));
            holder.setText(R.id.tvMediaTime, albumBean.time);
            if (albumBean.mediaType == 0) {
                holder.setVisible(R.id.tvMediaDuration, View.GONE);
            } else {
                holder.setVisible(R.id.tvMediaDuration, View.VISIBLE);
                holder.setText(R.id.tvMediaDuration, StringUtils.INSTANCE.getDurationTime(albumBean.duration));
            }
            if (isEdit) {
                holder.setVisible(R.id.cbSelect, View.VISIBLE);
            } else {
                holder.setVisible(R.id.cbSelect, View.GONE);
            }
            ((AppCompatCheckBox) holder.getView(R.id.cbSelect)).setChecked(albumBean.isSelect);
            holder.itemView.setOnClickListener(view -> {
                if (isEdit) {
                    albumBean.isSelect = !albumBean.isSelect;
                    holder.getView(R.id.cbSelect).performClick();
                } else {
                    getMRVItemClickListener().onItemClick(view, position, albumBean);
                }
            });
        }
    }
}
