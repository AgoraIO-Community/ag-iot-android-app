package io.agora.iotlinkdemo.models.device.setting.adapter;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.agora.baselibrary.base.BaseAdapter;
import com.agora.baselibrary.utils.StringUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.common.GlideApp;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlink.IotOutSharer;

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
        String userName = accountInfo.mAppUserId;
        if (!TextUtils.isEmpty(accountInfo.mPhone)) {
            userName = StringUtils.INSTANCE.formatAccount(accountInfo.mPhone);
        } else if (!TextUtils.isEmpty(accountInfo.mEmail))  {
            userName = accountInfo.mEmail;
        }
        holder.setText(R.id.tvUsername, userName);
        holder.itemView.setOnClickListener(view -> {
            PagePilotManager.pageDeviceShareToUserDetail(accountInfo);
        });
    }
}
