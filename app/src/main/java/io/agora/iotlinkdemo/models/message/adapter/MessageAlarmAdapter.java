package io.agora.iotlinkdemo.models.message.adapter;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;

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
    private static final String TAG = "IOTLINK/MsgAlarmAdpt";

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
        Log.d(TAG, "<onBindViewHolder> [DBGCLICK] position=" + position);
        IotAlarm iotAlarm = getDatas().get(position);
        if (iotAlarm != null) {
            if (!TextUtils.isEmpty(iotAlarm.mImageUrl)) {
                GlideApp.with(getMContext()).load(iotAlarm.mImageUrl).placeholder(R.mipmap.icon_deft).
                        into((AppCompatImageView) holder.getView(R.id.ivMessageCover));
            } else {
                GlideApp.with(getMContext()).load(iotAlarm.mVideoUrl).placeholder(R.mipmap.icon_deft).
                        into((AppCompatImageView) holder.getView(R.id.ivMessageCover));
            }
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
                Log.d(TAG, "<setOnLongClickListener> [DBGCLICK]");
                iotAlarm.mDeleted = !iotAlarm.mDeleted;
                ((AppCompatCheckBox) holder.getView(R.id.cbSelect)).setChecked(iotAlarm.mDeleted);
                getMRVItemClickListener().onItemClick(view, -1, iotAlarm);
                return false;
            });
            holder.setOnItemClickListener(view -> {
                Log.d(TAG, "<setOnItemClickListener> [DBGCLICK]");
                performItemClick(holder, view, position, iotAlarm);
            });

            holder.getView(R.id.lyAlarmBg).setOnClickListener(view -> {
                Log.d(TAG, "<setOnClickListener> Layout BG [DBGCLICK]");
                performItemClick(holder, view, position, iotAlarm);
            });

            holder.getView(R.id.ivMessageCover).setOnClickListener(view -> {
                Log.d(TAG, "<setOnClickListener> MsgCover [DBGCLICK]");
                performItemClick(holder, view, position, iotAlarm);
            });

            holder.getView(R.id.tvMsgTitle).setOnClickListener(view -> {
                Log.d(TAG, "<setOnClickListener> MsgTitle [DBGCLICK]");
                performItemClick(holder, view, position, iotAlarm);
            });

            holder.getView(R.id.tvMsgDesc).setOnClickListener(view -> {
                Log.d(TAG, "<setOnClickListener> MsgDesc [DBGCLICK]");
                performItemClick(holder, view, position, iotAlarm);
            });

            holder.getView(R.id.tvMsgTime).setOnClickListener(view -> {
                Log.d(TAG, "<setOnClickListener> MsgTime [DBGCLICK]");
                performItemClick(holder, view, position, iotAlarm);
            });

            holder.getView(R.id.tvMsgFrom).setOnClickListener(view -> {
                Log.d(TAG, "<setOnClickListener> MsgFrom [DBGCLICK]");
                performItemClick(holder, view, position, iotAlarm);
            });

            holder.getView(R.id.cbSelect).setOnClickListener(view -> {
                Log.d(TAG, "<setOnClickListener> CHECK BOX [DBGCLICK]");
                performItemClick(holder, view, position, iotAlarm);
            });
        }
    }

    void performItemClick(CommonViewHolder holder, View view, int position, IotAlarm iotAlarm) {
        if (isEdit) {
            iotAlarm.mDeleted = !iotAlarm.mDeleted;
            ((AppCompatCheckBox) holder.getView(R.id.cbSelect)).setChecked(iotAlarm.mDeleted);
        } else {
            getMRVItemClickListener().onItemClick(view, position, iotAlarm);
        }
    }


}
