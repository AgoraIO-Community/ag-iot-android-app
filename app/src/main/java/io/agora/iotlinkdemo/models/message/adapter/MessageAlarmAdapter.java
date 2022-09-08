package io.agora.iotlinkdemo.models.message.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;

import com.agora.baselibrary.base.BaseAdapter;
import com.agora.baselibrary.utils.StringUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.common.GlideApp;
import io.agora.iotlink.IotAlarm;

import java.util.ArrayList;

/**
 * 告警消息 Adapter
 */
public class MessageAlarmAdapter extends BaseAdapter<IotAlarm> {
    /**
     * 是否处于编辑模式
     */
    public boolean isEdit = false;

    public MessageAlarmAdapter(ArrayList<IotAlarm> messages) {
        super(messages);
    }

    @Override
    public int getLayoutId(int viewType) {
        return R.layout.item_alarm_message;
    }

    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder holder, int position) {
        IotAlarm iotAlarm = getDatas().get(position);
        if (iotAlarm != null) {
            GlideApp.with(getMContext()).load(iotAlarm.mVideoUrl).placeholder(R.mipmap.icon_deft).into((AppCompatImageView) holder.getView(R.id.ivMessageCover));
            if (iotAlarm.mMessageType == 0) {
                holder.setText(R.id.tvMsgTitle, "声音检测");
            } else if (iotAlarm.mMessageType == 1) {
                holder.setText(R.id.tvMsgTitle, "有人通过");
            } else if (iotAlarm.mMessageType == 2) {
                holder.setText(R.id.tvMsgTitle, "移动侦测");
            } else if (iotAlarm.mMessageType == 3) {
                holder.setText(R.id.tvMsgTitle, "语音告警");
            }
            holder.setText(R.id.tvMsgDesc, iotAlarm.mDescription);
            holder.setText(R.id.tvMsgTime, StringUtils.INSTANCE.getDetailTime("yyyy-MM-dd HH:mm:ss", iotAlarm.mTriggerTime / 1000));
            holder.setText(R.id.tvMsgFrom, "来自设备 " + StringUtils.INSTANCE.getBase64String(iotAlarm.mDeviceName));
            if (isEdit) {
                holder.setVisible(R.id.cbSelect, View.VISIBLE);
            } else {
                holder.setVisible(R.id.cbSelect, View.GONE);
            }
            ((AppCompatCheckBox) holder.getView(R.id.cbSelect)).setChecked(iotAlarm.mDeleted);
            holder.itemView.setOnLongClickListener(view -> {
                iotAlarm.mDeleted = !iotAlarm.mDeleted;
                holder.getView(R.id.cbSelect).performClick();
                getMRVItemClickListener().onItemClick(view, -1, iotAlarm);
                return false;
            });
            holder.itemView.setOnClickListener(view -> {
                if (isEdit) {
                    iotAlarm.mDeleted = !iotAlarm.mDeleted;
                    holder.getView(R.id.cbSelect).performClick();
                } else {
                    getMRVItemClickListener().onItemClick(view, position, iotAlarm);
                }
            });
        }
    }
}
