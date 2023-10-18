package io.agora.falcondemo.models.home;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agora.baselibrary.base.BaseAdapter;

import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ICallkitMgr;
import io.agora.falcondemo.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeviceListAdapter extends BaseAdapter<DeviceInfo> {

    private HomePageFragment mOwner;
    private RecyclerView mRecycleView;
    private boolean mSelectMode = false;            ///< 是否选择模式下

    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////// Public Methods /////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    public DeviceListAdapter(List<DeviceInfo> deviceList) {
        super(deviceList);
        mSelectMode = false;
    }

    void setOwner(HomePageFragment ownerFragment) {
        mOwner = ownerFragment;
    }

    void setRecycleView(final RecyclerView recycleView) {
        mRecycleView = recycleView;
    }

    /**
     * @brief 切换选择模式
     */
    void switchSelectMode(boolean selectMode) {
        mSelectMode = selectMode;

        // 所有设备项都设置为：还未选择
        List<DeviceInfo> deviceInfoList = getDatas();
        int devCount = deviceInfoList.size();
        int i;
        for (i = 0; i < devCount; i++) {
            DeviceInfo deviceInfo = deviceInfoList.get(i);
            deviceInfo.mSelected = false;
            deviceInfoList.set(i, deviceInfo);

            // 控制选择按钮是否显示
            if (deviceInfo.mViewHolder != null) {
                CheckBox cbSelect = deviceInfo.mViewHolder.getView(R.id.cb_dev_select);
                cbSelect.setVisibility( selectMode ? View.VISIBLE : View.INVISIBLE);
            }
        }
        setDatas(deviceInfoList);  // 更新到内部数据
    }

    boolean isInSelectMode() {
        return mSelectMode;
    }

    /**
     * @brief 设置所有设备项 全选/非全选 状态
     */
    void setAllItemsSelectStatus(boolean selected) {
        if (!mSelectMode) {  // 非选择模式下不做处理
            return;
        }

        List<DeviceInfo> deviceInfoList = getDatas();
        for (int i = 0; i < deviceInfoList.size(); i++) {
            DeviceInfo deviceInfo = deviceInfoList.get(i);
            deviceInfo.mSelected = selected;
            deviceInfoList.set(i, deviceInfo);

            // 控制选择按钮是否显示
            if (deviceInfo.mViewHolder != null) {
                CheckBox cbSelect = deviceInfo.mViewHolder.getView(R.id.cb_dev_select);
                cbSelect.setChecked(selected);
            }
        }
        setDatas(deviceInfoList);
    }

    /**
     * @brief 设置某个设备项 全选/非全选 状态
     */
    void setItemSelectStatus(int position, final DeviceInfo deviceInfo) {
        if (!mSelectMode) {  // 非选择模式下不做处理
            return;
        }

        // 控制选择按钮是否显示
        if (deviceInfo.mViewHolder != null) {
            CheckBox cbSelect = deviceInfo.mViewHolder.getView(R.id.cb_dev_select);
            cbSelect.setChecked(deviceInfo.mSelected);
        }

        getDatas().set(position, deviceInfo);
    }


    /**
     * @brief 判断是否所有设备项都被选中了
     */
    boolean isAllItemsSelected() {
        List<DeviceInfo> deviceInfoList = getDatas();
        for (int i = 0; i < deviceInfoList.size(); i++) {
            DeviceInfo deviceInfo = deviceInfoList.get(i);
            if (!deviceInfo.mSelected) {
                return false;
            }
        }

        return true;
    }

    /**
     * @brief 获取当前所有选择设备项
     */
    List<DeviceInfo> getSelectedItems() {
        List<DeviceInfo> selectedList = new ArrayList<>();

        List<DeviceInfo> deviceInfoList = getDatas();
        for (int i = 0; i < deviceInfoList.size(); i++) {
            DeviceInfo deviceInfo = deviceInfoList.get(i);
            if (deviceInfo.mSelected) {
                selectedList.add(deviceInfo);
            }
        }

        return selectedList;
    }

    /**
     * @brief 删除所有选中的设备项
     * @retrun 返回删除的数量
     */
    int deleteSelectedItems() {
        int oldCount = getDatas().size();
        List<DeviceInfo> unselectedList = new ArrayList<>();

        List<DeviceInfo> deviceInfoList = getDatas();
        for (int i = 0; i < deviceInfoList.size(); i++) {
            DeviceInfo deviceInfo = deviceInfoList.get(i);
            if (!deviceInfo.mSelected) {
                unselectedList.add(deviceInfo);
            }
        }

        setDatas(unselectedList);
        this.notifyDataSetChanged();

        return (oldCount - unselectedList.size());
    }

    /**
     * @brief 查询结果
     */
    public static class FindResult {
        public int mPosition = -1;      ///< 查询到的设备在列表中索引值, -1 表示没有查询到
        public DeviceInfo mDevInfo;     ///< 查询到的设备信息
    }

    /**
     * @brief 根据 非空的sessionId 找到设备项
     */
    public FindResult findItemBySessionId(final UUID sessionId) {
        FindResult findResult = new FindResult();
        findResult.mPosition = -1;
        if (sessionId == null) {
            return findResult;
        }

        List<DeviceInfo> deviceList = getDatas();
        for (int i = 0; i < deviceList.size(); i++) {
            DeviceInfo deviceInfo = deviceList.get(i);
            if (deviceInfo.mSessionId == null) {
                continue;
            }
            if (sessionId.compareTo(deviceInfo.mSessionId) == 0) {
                findResult.mPosition = i;
                findResult.mDevInfo = deviceInfo;
                return findResult;
            }
        }

        return findResult;
    }

    /**
     * @brief 根据 非空的设备 nodeId 找到设备项
     */
    public FindResult findItemByDevNodeId(final String devNodeId) {
        FindResult findResult = new FindResult();
        findResult.mPosition = -1;
        if (devNodeId == null) {
            return findResult;
        }

        List<DeviceInfo> deviceList = getDatas();
        for (int i = 0; i < deviceList.size(); i++) {
            DeviceInfo deviceInfo = deviceList.get(i);
            if (deviceInfo.mNodeId == null) {
                continue;
            }
            if (devNodeId.compareToIgnoreCase(deviceInfo.mNodeId) == 0) {
                findResult.mPosition = i;
                findResult.mDevInfo = deviceInfo;
                return findResult;
            }
        }

        return findResult;
    }


    /**
     * @brief 获取设备列表项
     */
    public DeviceInfo getItem(int position) {
        DeviceInfo deviceInfo = getDatas().get(position);
        return deviceInfo;
    }

    /**
     * @brief 设置设备列表项，同时更新相应控件显示
     */
    public void setItem(int position, final DeviceInfo deviceInfo) {
        getDatas().set(position, deviceInfo);
        updateUiWgt(deviceInfo);
    }

    /**
     * @brief 根据设备信息，更新 DeviceItem 控件显示
     */
    private void updateUiWgt(final DeviceInfo deviceInfo) {
        if (deviceInfo.mViewHolder == null) {
            return;
        }
        // 设备名
        deviceInfo.mViewHolder.setText(R.id.tvDeviceName, deviceInfo.mNodeId);

        // 在线用户数量
        String userCountTxt = "User: " + deviceInfo.mUserCount;
        deviceInfo.mViewHolder.setText(R.id.tvUserCount, userCountTxt);

        // 提示文字
        if (deviceInfo.mTips != null) {
            deviceInfo.mViewHolder.setText(R.id.tvDeviceTips, deviceInfo.mTips);
        } else {
            deviceInfo.mViewHolder.setText(R.id.tvDeviceTips, "Device Closed");
        }

        // 呼叫挂断按钮
        Button btnDialHangup = deviceInfo.mViewHolder.getView(R.id.btn_dial_hangup);
        if (deviceInfo.mSessionId == null) {
            btnDialHangup.setText("呼叫");
        } else {
            btnDialHangup.setText("挂断");
        }

        // 静音播音按钮
        Button btnMute = deviceInfo.mViewHolder.getView(R.id.btn_mute_audio);
        btnMute.setText(deviceInfo.mDevMute ? "播放" : "静音");

        // 录像按钮
        Button btnRecord = deviceInfo.mViewHolder.getView(R.id.btn_record);
        btnRecord.setText(deviceInfo.mRecording ? "停止" : "录像");

        // 通话禁音按钮
        Button btnMic = deviceInfo.mViewHolder.getView(R.id.btn_mic);
        btnMic.setText(deviceInfo.mMicPush ? "禁麦" : "通话");
    }

    /**
     * @brief 增加设备列表项
     */
    public void addNewItem(final DeviceInfo deviceInfo) {
        getDatas().add(deviceInfo);
        this.notifyDataSetChanged();
    }


    @Override
    public int getLayoutId(int viewType) {
        return R.layout.item_device_info;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder holder, int position) {
        DeviceInfo deviceInfo = getDatas().get(position);
        if (deviceInfo == null) {
            return;
        }

        deviceInfo.mVideoView = holder.getView(R.id.svDeviceView);
        deviceInfo.mViewHolder = holder;
        getDatas().set(position, deviceInfo);   // 更新控件信息


        // 设置设备显示控件
        View displayView = holder.getView(R.id.svDeviceView);
        if (deviceInfo.mSessionId != null) {
            displayView.setVisibility(View.VISIBLE);
            ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
            callkitMgr.setPeerVideoView(deviceInfo.mSessionId, displayView);
        } else {
            displayView.setVisibility(View.INVISIBLE);
        }

        // 设备名
        holder.setText(R.id.tvDeviceName, deviceInfo.mNodeId);

        // 在线用户数量
        String userCountTxt = "User: " + deviceInfo.mUserCount;
        holder.setText(R.id.tvUserCount, userCountTxt);

        // 提示文字
        if (deviceInfo.mTips != null) {
            holder.setText(R.id.tvDeviceTips, deviceInfo.mTips);
        } else {
            holder.setText(R.id.tvDeviceTips, "Device Closed");
        }

        // 呼叫挂断按钮
        Button btnDialHangup = holder.getView(R.id.btn_dial_hangup);
        if (deviceInfo.mSessionId == null) {
            btnDialHangup.setText("呼叫");
        } else {
            btnDialHangup.setText("挂断");
        }
        btnDialHangup.setOnClickListener(view -> {
            mOwner.onDevItemDialHangupClick(view, position, deviceInfo);
        });

        // 静音播音按钮
        Button btnMute = holder.getView(R.id.btn_mute_audio);
        btnMute.setText(deviceInfo.mDevMute ? "播放" : "静音");
        btnMute.setOnClickListener(view -> {
            mOwner.onDevItemMuteAudioClick(view, position, deviceInfo);
        });

        // 录像按钮
        Button btnRecord = holder.getView(R.id.btn_record);
        btnRecord.setText(deviceInfo.mRecording ? "停止" : "录像");
        btnRecord.setOnClickListener(view -> {
            mOwner.onDevItemRecordClick(view, position, deviceInfo);
        });

        // 通话禁音按钮
        Button btnMic = holder.getView(R.id.btn_mic);
        btnMic.setText(deviceInfo.mMicPush ? "禁麦" : "通话");
        btnMic.setOnClickListener(view -> {
            mOwner.onDevItemMicClick(view, position, deviceInfo);
        });


        // 全屏按钮
        Button btnFullscreen = holder.getView(R.id.btn_fullscreen);
        btnFullscreen.setOnClickListener(view -> {
            mOwner.onDevItemFullscrnClick(view, position, deviceInfo);
        });

        // 截屏按钮
        Button btnShotCapture = holder.getView(R.id.btn_shotcapture);
        btnShotCapture.setOnClickListener(view -> {
            mOwner.onDevItemShotCaptureClick(view, position, deviceInfo);
        });

        // 命令按钮
        Button btnCommand = holder.getView(R.id.btn_command);
        btnCommand.setOnClickListener(view -> {
            mOwner.onDevItemCommandClick(view, position, deviceInfo);
        });

        // 选择按钮
        CheckBox cbSlect = holder.getView(R.id.cb_dev_select);
        cbSlect.setChecked(deviceInfo.mSelected);
        if (mSelectMode) {
            cbSlect.setVisibility(View.VISIBLE);
        } else {
            cbSlect.setVisibility(View.INVISIBLE);
        }
        cbSlect.setOnClickListener(view -> {
            mOwner.onDevItemCheckBoxClick(view, position, deviceInfo);
        });

//        holder.itemView.setOnClickListener(view -> {
//            getMRVItemClickListener().onItemClick(view, position, deviceInfo);
//        });
    }


}
