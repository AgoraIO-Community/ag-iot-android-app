package com.agora.iotlink.models.device.setting.adapter;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.agora.baselibrary.base.BaseAdapter;
import com.agora.baselibrary.utils.StringUtils;
import com.agora.iotlink.R;
import com.agora.iotlink.common.GlideApp;
import com.agora.iotlink.manager.PagePilotManager;
import com.agora.iotsdk20.IotOutSharer;

import java.util.ArrayList;

public class ShareToUserAdapter extends BaseAdapter<IotOutSharer> {

    public ShareToUserAdapter(ArrayList<IotOutSharer> devices) {
        super(devices);
    }

    @Override
    public int getLayoutId(int viewType) {
        return R.layout.item_share_to_user;
    }

    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder holder, int position) {
        IotOutSharer accountInfo = getDatas().get(position);
        GlideApp.with(getMContext()).load(accountInfo.mAvatar).error(R.mipmap.userimage).into((AppCompatImageView) holder.getView(R.id.ivUserAvatar));
        holder.setText(R.id.tvUserDesc, accountInfo.mUsrNickName);
        if (!TextUtils.isEmpty(accountInfo.mPhone)) {
            holder.setText(R.id.tvUsername, StringUtils.INSTANCE.formatAccount(accountInfo.mPhone));
        } else {
            holder.setText(R.id.tvUsername, accountInfo.mEmail);
        }
        holder.itemView.setOnClickListener(view -> {
            PagePilotManager.pageDeviceShareToUserDetail(accountInfo);
        });
    }
}
