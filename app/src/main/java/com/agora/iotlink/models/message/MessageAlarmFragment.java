package com.agora.iotlink.models.message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ToastUtils;
import com.agora.iotlink.R;
import com.agora.iotlink.base.BaseViewBindingFragment;
import com.agora.iotlink.common.Constant;
import com.agora.iotlink.databinding.FagmentMessageAlarmBinding;
import com.agora.iotlink.dialog.DeleteMediaTipDialog;
import com.agora.iotlink.dialog.SelectVideoTypeDialog;
import com.agora.iotlink.manager.PagePathConstant;
import com.agora.iotlink.manager.PagePilotManager;
import com.agora.iotlink.models.message.adapter.MessageAlarmAdapter;
import com.agora.iotsdk20.IotAlarm;
import com.agora.iotsdk20.IotAlarmPage;
import com.alibaba.android.arouter.facade.annotation.Route;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 告警消息
 */
@Route(path = PagePathConstant.pageMessageAlarm)
public class MessageAlarmFragment extends BaseViewBindingFragment<FagmentMessageAlarmBinding> {
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
            if (type == Constant.CALLBACK_TYPE_MESSAGE_NOTIFY_COUNT_RESULT) {
                if (getActivity() instanceof MessageActivity) {
                    ((MessageActivity) getActivity()).setNotificationCount(((long) data));
                }
            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_COUNT_RESULT) {
                if (getActivity() instanceof MessageActivity) {
                    ((MessageActivity) getActivity()).setAlarmCount(((long) data));
                    messageViewModel.requestNotifyMgrCount();
                }
            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_QUERY_RESULT) {
                messageViewModel.requestAlarmMgrCount();
                if (data instanceof IotAlarmPage) {
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
            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_DETAIL_RESULT) {
                if (data instanceof IotAlarm) {
                    PagePilotManager.pagePlayMessage((IotAlarm) data);
                    if (((IotAlarm) data).mStatus == 0) {
                        List<Long> list = new ArrayList<>();
                        list.add(((IotAlarm) data).mAlarmId);
                        messageViewModel.markAlarmMessage(list);
                    }
                }
            } else if (type == Constant.CALLBACK_TYPE_MESSAGE_ALARM_DELETE_RESULT) {
                requestData();
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

    @Override
    public void requestData() {
        showLoadingView();
        getSelectDate();
        messageViewModel.requestAllAlarmMgr();
    }

    private void getSelectDate() {
        if (getBinding().btnSelectDate.getText().equals("今天")) {
            Calendar calendar = Calendar.getInstance();
            customDate.year = calendar.get(Calendar.YEAR);
            customDate.month = calendar.get(Calendar.MONTH) + 1;
            customDate.day = calendar.get(Calendar.DAY_OF_MONTH);
        }
//        messageViewModel.queryParam.mDeviceId = "686087441870991360";
        messageViewModel.queryParam.mBeginDate = messageViewModel.beginDateToString(customDate);
        messageViewModel.queryParam.mEndDate = messageViewModel.endDateToString(customDate);
    }

    private void showSelectVideoTypeDialog() {
        if (selectVideoTypeDialog == null) {
            selectVideoTypeDialog = new SelectVideoTypeDialog(getActivity());
            selectVideoTypeDialog.iSingleCallback = (type, var2) -> {
                if (type == 1) {
                    getBinding().btnSelectType.setText(getString(R.string.all_type));
                    messageViewModel.queryParam.mMsgType = -1;
                } else if (type == 2) {
                    getBinding().btnSelectType.setText(getString(R.string.sound_detection));
                    messageViewModel.queryParam.mMsgType = 0;
                } else if (type == 3) {
                    getBinding().btnSelectType.setText(getString(R.string.motion_detection));
                    messageViewModel.queryParam.mMsgType = 2;
                } else if (type == 4) {
                    getBinding().btnSelectType.setText(getString(R.string.human_infrared_detection));
                    messageViewModel.queryParam.mMsgType = 1;
                } else if (type == 5) {
                    getBinding().btnSelectType.setText(getString(R.string.call_button));
                    messageViewModel.queryParam.mMsgType = 3;
                }
                requestData();
            };
        }
        selectVideoTypeDialog.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        messageViewModel.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        messageViewModel.onStop();
    }
}
