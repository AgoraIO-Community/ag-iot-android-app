package io.agora.iotlinkdemo.models.message;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingFragment;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.FagmentMessageAlarmBinding;
import io.agora.iotlinkdemo.dialog.DeleteMediaTipDialog;
import io.agora.iotlinkdemo.dialog.SelectVideoTypeDialog;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.message.adapter.MessageAlarmAdapter;
import io.agora.iotlink.IotAlarm;
import io.agora.iotlink.IotAlarmPage;
import com.alibaba.android.arouter.facade.annotation.Route;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 告警消息
 */
@Route(path = PagePathConstant.pageMessageAlarm)
public class MessageAlarmFragment extends BaseViewBindingFragment<FagmentMessageAlarmBinding> {
    private static final String TAG = "IOTLINK/MsgAlarmFrag";
    /**
     * 消息ViewModel
     */
    private MessageViewModel messageViewModel;

    private MessageViewModel.CustomDate customDate = new MessageViewModel.CustomDate();

    private MessageAlarmAdapter messageAlarmAdapter;
    private ArrayList<IotAlarm> mMessages = new ArrayList<>();
    /**
     * 筛选视频类型话框
     */
    private SelectVideoTypeDialog selectVideoTypeDialog;

    @NonNull
    @Override
    protected FagmentMessageAlarmBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FagmentMessageAlarmBinding.inflate(inflater);
    }

    @Override
    public void initView() {
        messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        messageViewModel.setLifecycleOwner(this);
        messageAlarmAdapter = new MessageAlarmAdapter(mMessages);
        messageAlarmAdapter.setMRVItemClickListener((view, position, data) -> {
            Log.d(TAG, "<setMRVItemClickListener> [DBGCLICK]");
            if (position == -1) {
                getBinding().btnEdit.performClick();
            } else {
                 messageViewModel.requestAlarmMgrDetailById(data.mAlarmId);
            }
        });
        getBinding().calendarView.setMaxDate(System.currentTimeMillis());
        getBinding().rlMsgList.setAdapter(messageAlarmAdapter);
        getBinding().rlMsgList.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    @Override
    public void initListener() {
        getBinding().btnEdit.setOnClickListener(view -> {
            changeEditStatus(!messageAlarmAdapter.isEdit);
        });
        getBinding().btnSelectDevice.setOnClickListener(view -> {
            ToastUtils.INSTANCE.showToast(R.string.function_not_open);
        });
        getBinding().btnDoDelete.setOnClickListener(view -> {
            showDeleteMediaTipDialog();
        });
        getBinding().btnSelectType.setOnClickListener(view -> {
            showSelectVideoTypeDialog();
        });
        getBinding().cbAllSelect.setOnCheckedChangeListener((compoundButton, b) -> {
            for (IotAlarm iotAlarm : messageAlarmAdapter.getDatas()) {
                iotAlarm.mDeleted = b;
            }
            messageAlarmAdapter.notifyDataSetChanged();
        });
        messageViewModel.setISingleCallback((type, data) -> {
            if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_QUERY_FAIL) {  // 查询告警消息失败
                getBinding().rlMsgList.post(() -> {
                    int errCode = (Integer)data;
                    hideLoadingView();
                    popupMessage("查询告警消息失败, 错误码=" + errCode);
                });

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_QUERY_RESULT) {  // 查询告警消息成功
                if (data instanceof IotAlarmPage) {
                    // 刷新当前告警消息列表
                    mMessages.clear();
                    mMessages.addAll(((IotAlarmPage) data).mAlarmList);

                    getBinding().rlMsgList.post(() -> {
                        hideLoadingView();
                        messageAlarmAdapter.notifyDataSetChanged();
                        if (mMessages.isEmpty()) {
                            getBinding().rlMsgList.setVisibility(View.GONE);
                            getBinding().btnEdit.setVisibility(View.GONE);
                            getBinding().tvMessageNo.setVisibility(View.VISIBLE);
                        } else {
                            getBinding().rlMsgList.setVisibility(View.VISIBLE);
                            getBinding().btnEdit.setVisibility(View.VISIBLE);
                            getBinding().tvMessageNo.setVisibility(View.GONE);
                        }
                    });
                }

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_UNREAD_NOTIFIY_COUNT) {  // 查询到未读通知数量
                if (getActivity() instanceof MessageActivity) {
                    long count = (Long)data;
                    if (count >= 0) {  // 查询成功时才更新
                        ((MessageActivity) getActivity()).setNotificationCount(count);
                    }
                }

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_UNREAD_ALARM_COUNT) {  // 查询到未读告警消息数量
                if (getActivity() instanceof MessageActivity) {
                    long count = (Long)data;
                    if (count >= 0) { // 查询成功时才更新
                        ((MessageActivity) getActivity()).setAlarmCount(count);
                    }
                }

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_DETAIL_RESULT) {  // 单个告警消息查询成功
                if (data instanceof IotAlarm) {
                    if (((IotAlarm) data).mVideoUrl == null) {
                        getBinding().rlMsgList.post(() -> {
                            popupMessage("没有告警云录视频!");
                        });

                    } else {
                        PagePilotManager.pagePlayMessage((IotAlarm) data);
                    }
                    if (((IotAlarm) data).mStatus == 0) {
                        List<Long> list = new ArrayList<>();
                        list.add(((IotAlarm) data).mAlarmId);
                        messageViewModel.markAlarmMessage(list);  // 标记告警消息已读
                    }
                }

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_DETAIL_FAIL) {  // 单个告警消息查询失败
                getBinding().rlMsgList.post(() -> {
                    hideLoadingView();
                    int errCode = (Integer) data;
                    popupMessage("查询告警详情失败, 错误码: " + errCode);
                });

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_DELETE_RESULT) {  // 告警消息删除成功
                getBinding().rlMsgList.post(() -> {
                    hideLoadingView();
                    popupMessage("删除告警消息成功!");
                });

                // 重新查询当前告警信息
                requestData();

                // 重新查询未读告警信息数量
                messageViewModel.queryUnreadedAlarmCount();

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_MARK_ALARM_MSG_FAIL) {  // 告警消息删除失败
                getBinding().rlMsgList.post(() -> {
                    hideLoadingView();
                    int errCode = (Integer)data;
                    popupMessage("删除告警信息失败, 错误码=" + errCode);
                });

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_MARK_ALARM_MSG) {  // 标记已读成功
                // 重新查询未读告警信息数量
                messageViewModel.queryUnreadedAlarmCount();

            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_MARK_ALARM_MSG_FAIL) {  // 标记已读失败

            }

        });
        getBinding().calendarView.setOnDateChangeListener((calendarView, year, month, dayOfMonth) -> {
            customDate.year = year;
            customDate.month = month + 1;
            customDate.day = dayOfMonth;
            getBinding().btnSelectDate.setText(year + "-" + (month + 1) + "-" + dayOfMonth);
            getBinding().selectBg.setVisibility(View.GONE);
            requestData();
        });
        getBinding().btnSelectDate.setOnClickListener(view -> {
            getBinding().selectBg.setVisibility(View.VISIBLE);
        });

        messageViewModel.queryUnreadedAlarmCount();   // 查询所有未读告警消息数量
        messageViewModel.queryUnreadedNotifyCount();  // 查询所有未读通知数量
    }

    @Override
    public void requestData() {
        Log.d(TAG, "<requestData>");
        showLoadingView();
        getSelectDate();
        messageViewModel.queryAlarmsByFilter();       // 查询当天告警消息
    }


    /**
     * 删除对话框
     */
    private DeleteMediaTipDialog deleteMediaTipDialog;

    private void showDeleteMediaTipDialog() {
        if (deleteMediaTipDialog == null) {
            deleteMediaTipDialog = new DeleteMediaTipDialog(getActivity());
            deleteMediaTipDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {

                }

                @Override
                public void onRightButtonClick() {
                    doDelete();
                }
            });
        }
        deleteMediaTipDialog.show();
    }

    private void doDelete() {
        List<Long> deletes = new ArrayList<>();
        for (IotAlarm iotAlarm : messageAlarmAdapter.getDatas()) {
            if (iotAlarm.mDeleted) {
                deletes.add(iotAlarm.mAlarmId);
            }
        }
        if (deletes.isEmpty()) {
            deleteMediaTipDialog.dismiss();
            ToastUtils.INSTANCE.showToast("请选择要删除的消息");
        } else {
            messageViewModel.requestDeleteAlarmMgr(deletes);
            messageAlarmAdapter.notifyDataSetChanged();
            changeEditStatus(false);
        }
    }

    private void changeEditStatus(boolean toEdit) {
        if (!toEdit) {
            messageAlarmAdapter.isEdit = false;
            getBinding().btnSelectDate.setVisibility(View.VISIBLE);
            getBinding().btnSelectType.setVisibility(View.VISIBLE);
            getBinding().btnSelectDevice.setVisibility(View.VISIBLE);
            getBinding().bgBottomDel.setVisibility(View.GONE);
            getBinding().btnEdit.setText("编辑");
        } else {
            getBinding().btnSelectDate.setVisibility(View.INVISIBLE);
            getBinding().btnSelectType.setVisibility(View.INVISIBLE);
            getBinding().btnSelectDevice.setVisibility(View.INVISIBLE);
            getBinding().bgBottomDel.setVisibility(View.VISIBLE);
            messageAlarmAdapter.isEdit = true;
            getBinding().btnEdit.setText("完成");
        }
        messageAlarmAdapter.notifyDataSetChanged();
    }



    private void getSelectDate() {
        if (getBinding().btnSelectDate.getText().equals("今天")) {
            Calendar calendar = Calendar.getInstance();
            customDate.year = calendar.get(Calendar.YEAR);
            customDate.month = calendar.get(Calendar.MONTH) + 1;
            customDate.day = calendar.get(Calendar.DAY_OF_MONTH);
        }
        messageViewModel.setQueryBeginDate(customDate);
        messageViewModel.setQueryEndDate(customDate);
    }

    private void showSelectVideoTypeDialog() {
        if (selectVideoTypeDialog == null) {
            selectVideoTypeDialog = new SelectVideoTypeDialog(getActivity());
            selectVideoTypeDialog.iSingleCallback = (type, var2) -> {
                if (type == 1) {
                    getBinding().btnSelectType.setText(getString(R.string.all_type));
                    messageViewModel.setQueryMsgType(-1);
                } else if (type == 2) {
                    getBinding().btnSelectType.setText(getString(R.string.sound_detection));
                    messageViewModel.setQueryMsgType(0);
                } else if (type == 3) {
                    getBinding().btnSelectType.setText(getString(R.string.motion_detection));
                    messageViewModel.setQueryMsgType(2);
                } else if (type == 4) {
                    getBinding().btnSelectType.setText(getString(R.string.human_infrared_detection));
                    messageViewModel.setQueryMsgType(1);
                } else if (type == 5) {
                    getBinding().btnSelectType.setText(getString(R.string.call_button));
                    messageViewModel.setQueryMsgType(3);
                }
                requestData();
            };
        }
        selectVideoTypeDialog.show();
    }

    @Override
    public void onStart() {
        Log.d(TAG, "<onStart>");
        super.onStart();
        messageViewModel.onStart();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "<onStop>");
        super.onStop();
        messageViewModel.onStop();
    }

    public boolean onBtnBack() {
        if (messageAlarmAdapter == null) {
            return false;
        }
        if (messageAlarmAdapter.isEdit) {
            changeEditStatus(false);
            return true;
        }

        return false;
    }

}
