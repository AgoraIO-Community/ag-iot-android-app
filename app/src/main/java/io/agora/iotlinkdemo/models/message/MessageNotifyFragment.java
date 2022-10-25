package io.agora.iotlinkdemo.models.message;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import io.agora.iotlinkdemo.base.BaseViewBindingFragment;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.FagmentMessageNotifyBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.message.adapter.MessageNotifyAdapter;
import io.agora.iotlink.IotAlarm;
import io.agora.iotlink.IotDevMessage;
import io.agora.iotlink.IotDevMsgPage;
import com.alibaba.android.arouter.facade.annotation.Route;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知消息
 */
@Route(path = PagePathConstant.pageMessageNotify)
public class MessageNotifyFragment extends BaseViewBindingFragment<FagmentMessageNotifyBinding> {
    private final String TAG = "IOTLINK/MsgNotifyFrag";

    private NotificationViewModel mNotificationViewModel;
    private MessageNotifyAdapter messageNotifyAdapter;
    private ArrayList<IotDevMessage> mMessages = new ArrayList<>();  ///< 当前所有通知消息列表
    private int mUnreadedCount = 0;

    @NonNull
    @Override
    protected FagmentMessageNotifyBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FagmentMessageNotifyBinding.inflate(inflater);
    }

    @Override
    public void initView() {
        mNotificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        mNotificationViewModel.setLifecycleOwner(this);
        messageNotifyAdapter = new MessageNotifyAdapter(mMessages);
        getBinding().rlMsgList.setAdapter(messageNotifyAdapter);
        getBinding().rlMsgList.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    @Override
    public void initListener() {
        mNotificationViewModel.setISingleCallback((type, data) -> {
            if (type == Constant.CALLBACK_TYPE_MESSAGE_NOTIFY_QUERY_RESULT) {   // 查询所有通知消息成功
                if (data instanceof IotDevMsgPage) {
                    mMessages.clear();
                    mMessages.addAll(((IotDevMsgPage) data).mDevMsgList);
                    List<Long> list = new ArrayList<>();
                    for (IotDevMessage iotDevMessage : mMessages) {
                        if (iotDevMessage.mStatus == 0) {
                            list.add(iotDevMessage.mMessageId);
                        }
                    }

                    // 设置所有未读通知的数量
                    refreshUnreadDisplay();

                    // 刷新通知列表显示
                    getBinding().rlMsgList.post(() -> {
                        messageNotifyAdapter.notifyDataSetChanged();
                        if (mMessages.isEmpty()) {
                            getBinding().rlMsgList.setVisibility(View.GONE);
                            getBinding().btnEdit.setVisibility(View.GONE);
                            getBinding().tvMessageNo.setVisibility(View.VISIBLE);
                        } else {
                            getBinding().rlMsgList.setVisibility(View.VISIBLE);
                            getBinding().btnEdit.setVisibility(View.GONE);
                            getBinding().tvMessageNo.setVisibility(View.GONE);
                        }
                    });
                }

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_NOTIFY_QUERY_FAIL) {   // 查询所有通知消息失败
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int errCode = (Integer)data;
                        popupMessage("查询通知消息失败, 错误码=" + errCode);
                    }
                });

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_MARK_NOTIFY_MSG_FAIL) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int errCode = (Integer) data;
                        //popupMessage("标记通知消息已读失败, 错误码=" + errCode);
                    }
                });

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_MARK_NOTIFY_MSG_SUCCESS) {
                List<Long> markedIdList = (List<Long>)data;

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        // 将通知列表中相应的通知标记为已读
                        int markedCount = markedIdList.size();
                        int msgCount = mMessages.size();
                        for (int i = 0; i < markedCount; i++) {
                            long markedId = markedIdList.get(i);

                            for (int j = 0; j < msgCount; j++) {  // 找到相应的通知，标记为已读
                                IotDevMessage iotDevMessage = mMessages.get(j);
                                if (iotDevMessage.mMessageId == markedId) {
                                    iotDevMessage.mStatus = 1;
                                    mMessages.set(j, iotDevMessage);
                                    break;
                                }
                            }
                        }

                        // 刷新未读通知数量
                        refreshUnreadDisplay();
                    }
                });
            }
        });

        messageNotifyAdapter.setMRVItemClickListener((view, position, data) -> {
            IotDevMessage iotDevMessage = (IotDevMessage)data;
            Log.d(TAG, "<setMRVItemClickListener>");
            if (position < 0) {
                return;
            }
            if (iotDevMessage.mStatus == 1) {       // 消息已读
                return;
            }

            List<Long> msgIdList = new ArrayList<>();
            msgIdList.add(iotDevMessage.mMessageId);
            mNotificationViewModel.markNotifyMessage(msgIdList);

        });

        // 查询所有通知消息
        mNotificationViewModel.queryAllNotifications();
    }

    @Override
    public void onStart() {
        super.onStart();
        mNotificationViewModel.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mNotificationViewModel.onStop();
    }

    public boolean onBtnBack() {
        return false;
    }


    void refreshUnreadDisplay() {
        long unreadedCount = 0;
        for (IotDevMessage iotDevMessage : mMessages) {
            if (iotDevMessage.mStatus == 0) {
                unreadedCount++;
            }
        }
        // 设置所有未读通知的数量
        if (getActivity() instanceof MessageActivity) {
            ((MessageActivity)getActivity()).setNotificationCount(unreadedCount);
            Log.d(TAG, "<refreshUnreadDisplay> unreadedCount=" + unreadedCount);
        }
    }

}
