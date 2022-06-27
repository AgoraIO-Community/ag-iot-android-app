package com.agora.iotlink.models.message.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.agora.baselibrary.base.BaseAdapter;
import com.agora.baselibrary.utils.StringUtils;
import com.agora.iotlink.R;
import com.agora.iotsdk20.IotDevMessage;
import com.agora.iotsdk20.IotNotification;

import java.util.ArrayList;

/**
 * 通知消息 Adapter
 */
public class MessageNotifyAdapter extends BaseAdapter<IotDevMessage> {

    public MessageNotifyAdapter(ArrayList<IotDevMessage> messages) {
        super(messages);
    }

    /**
     * 是否处于编辑模式
     */
    public boolean isEdit = false;

    @Override
    public int getLayoutId(int viewType) {
        return R.layout.item_notify_message;
    }

    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder holder, int position) {
        IotDevMessage iotDevMessage = getDatas().get(position);
        if (iotDevMessage != null) {
            holder.setText(R.id.tvMsgTitle, iotDevMessage.mDescription);
//            holder.setText(R.id.tvMsgDesc, iotDevMessage.mDescription);
            holder.setText(R.id.tvMsgTime, StringUtils.INSTANCE.getDetailTime("yyyy-MM-dd HH:mm:ss", Long.parseLong(iotDevMessage.mCreatedDate) / 1000));
        }
        if (isEdit) {
            holder.setVisible(R.id.cbSelect, View.VISIBLE);
        } else {
            holder.setVisible(R.id.cbSelect, View.GONE);
        }
//        ((AppCompatCheckBox) holder.getView(R.id.cbSelect)).setOnCheckedChangeListener((compoundButton, b) -> {
//            if (b) {
//                msgNotification.mMarkFlag = 1;
//            } else {
//                msgNotification.mMarkFlag = 0;
//            }
//        });
    }
}
