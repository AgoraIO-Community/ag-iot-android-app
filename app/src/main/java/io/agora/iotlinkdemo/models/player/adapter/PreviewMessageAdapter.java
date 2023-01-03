package io.agora.iotlinkdemo.models.player.adapter;

import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;

import com.agora.baselibrary.base.BaseAdapter;
import com.agora.baselibrary.utils.StringUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.common.GlideApp;
import io.agora.iotlink.IotAlarm;
import io.agora.iotlinkdemo.manager.PagePilotManager;

import java.util.ArrayList;

public class PreviewMessageAdapter extends BaseAdapter<IotAlarm> {
    /**
     * 是否处于编辑模式
     */
    public boolean isEdit = false;
    /**
     * 上一次播放的item位置
     */
    public int oldPlayPosition = -1;

    public int newPosition = -1;

    public PreviewMessageAdapter(ArrayList<IotAlarm> messages) {
        super(messages);
    }

    @Override
    public int getLayoutId(int viewType) {
        return R.layout.item_preview_msg_list;
    }

    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder holder, int position) {
        IotAlarm iotAlarm = getDatas().get(position);
        if (iotAlarm != null) {
            if (!TextUtils.isEmpty(iotAlarm.mImageUrl)) {
                GlideApp.with(getMContext()).load(iotAlarm.mImageUrl).placeholder(R.mipmap.icon_deft).
                        into((AppCompatImageView) holder.getView(R.id.ivMessageCover));
            } else {
                GlideApp.with(getMContext()).load(iotAlarm.mVideoUrl).placeholder(R.mipmap.icon_deft).
                        into((AppCompatImageView) holder.getView(R.id.ivMessageCover));
            }
            String msgTitle = PagePilotManager.getAlarmMessageTitle(iotAlarm.mMessageType);
            holder.setText(R.id.tvMsgTitle, msgTitle);
            holder.setText(R.id.tvMsgDesc, iotAlarm.mDescription);
            holder.setText(R.id.tvMsgTime, StringUtils.INSTANCE.getDetailTime("yyyy-MM-dd HH:mm:ss", iotAlarm.mTriggerTime / 1000));
            if (isEdit) {
                holder.setVisible(R.id.cbSelect, View.VISIBLE);
            } else {
                holder.setVisible(R.id.cbSelect, View.GONE);
            }
            ((AppCompatCheckBox) holder.getView(R.id.cbSelect)).setChecked(iotAlarm.mDeleted);
            holder.itemView.setOnClickListener(view -> {
                if (isEdit) {
                    iotAlarm.mDeleted = !iotAlarm.mDeleted;
                    holder.getView(R.id.cbSelect).performClick();
                } else {
                    getMRVItemClickListener().onItemClick(view, position, iotAlarm);
                }
            });
            if (position == newPosition) {
                holder.setVisible(R.id.tvPlaying, View.VISIBLE);
            }else{
                holder.setVisible(R.id.tvPlaying, View.GONE);
            }
        }
    }
}
