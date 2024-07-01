package io.agora.falcondemo.models.home;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.agora.baselibrary.base.BaseDialog;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import io.agora.falcondemo.common.Constant;
import io.agora.falcondemo.dialog.DialogInputCommand;
import io.agora.falcondemo.models.devstream.DevStreamActivity;
import io.agora.falcondemo.models.player.FileTransStatus;
import io.agora.falcondemo.thirdpartyaccount.ThirdAccountMgr;
import io.agora.falcondemo.utils.DevStreamUtils;
import io.agora.falcondemo.utils.FileTransferMgr;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.falcondemo.R;
import io.agora.falcondemo.base.BaseViewBindingFragment;
import io.agora.falcondemo.base.PermissionHandler;
import io.agora.falcondemo.base.PermissionItem;
import io.agora.falcondemo.base.PushApplication;
import io.agora.falcondemo.databinding.FragmentHomePageBinding;
import io.agora.falcondemo.dialog.DialogNewDevice;
import io.agora.falcondemo.models.player.DevPreviewActivity;
import io.agora.falcondemo.utils.AppStorageUtil;
import io.agora.falcondemo.utils.FileUtils;
import io.agora.iotlink.IConnectionMgr;
import io.agora.iotlink.IConnectionObj;
import io.agora.iotlink.base.BaseEvent;
import io.agora.iotlink.logger.ALog;


public class HomePageFragment extends BaseViewBindingFragment<FragmentHomePageBinding>
        implements PermissionHandler.ICallback, IConnectionMgr.ICallback, IConnectionObj.ICallback  {
    private static final String TAG = "IOTLINK/HomePageFrag";


    //
    // message Id
    //
    private static final int MSGID_HOMEPAGE_CONNECTALL = 0x2001;       ///< 连接所有设备
    private static final int MSGID_HOMEPAGE_REFRESH = 0x2002;          ///< 刷新所有UI状态显示





    private volatile boolean mFragmentForeground = false;        ///< 界面是否在前台
    private PermissionHandler mPermHandler;             ///< 权限申请处理
    private Handler mMsgHandler = null;                 ///< 主线程中的消息处理

    private MainActivity mMainActivity;
    private HomePageFragment mHomeFragment;
    private DeviceListAdapter mDevListAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;


    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseFragment /////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    @Override
    protected FragmentHomePageBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomePageBinding.inflate(inflater);
    }

    @Override
    public void initView() {
        mMainActivity = (MainActivity)getActivity();
        mHomeFragment = this;
        mFragmentForeground = false;

        //
        // 初始化设备列表
        //
        List<DeviceInfo> deviceList = deviceListLoad();

        if (mDevListAdapter == null) {
            mDevListAdapter = new DeviceListAdapter(deviceList);
            mDevListAdapter.setOwner(this);
            mDevListAdapter.setRecycleView(getBinding().rvDeviceList);
            getBinding().rvDeviceList.setLayoutManager(new LinearLayoutManager(getActivity()));
            getBinding().rvDeviceList.setAdapter(mDevListAdapter);
            getBinding().rvDeviceList.setItemViewCacheSize(15);
            mDevListAdapter.setMRVItemClickListener((view, position, data) -> {
            });
        }
        mSwipeRefreshLayout = getBinding().srlDevList;

        getBinding().titleView.hideLeftImage();

        // 创建主线程消息处理
        mMsgHandler = new Handler(getActivity().getMainLooper())
        {
            @SuppressLint("HandlerLeak")
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case MSGID_HOMEPAGE_CONNECTALL:
                        onMsgConnectAll(msg);
                        break;

                    case MSGID_HOMEPAGE_REFRESH:
                        onMsgRefreshAll(msg);
                        break;
                }
            }
        };


        //
        //
        // Microphone权限判断处理
        //
        int[] permIdArray = new int[1];
        permIdArray[0] = PermissionHandler.PERM_ID_RECORD_AUDIO;
        mPermHandler = new PermissionHandler(getActivity(), this, permIdArray);
        if (!mPermHandler.isAllPermissionGranted()) {
            Log.d(TAG, "<initView> requesting permission...");
            mPermHandler.requestNextPermission();
        } else {
            Log.d(TAG, "<initView> permission ready");
            mMsgHandler.sendEmptyMessage(MSGID_HOMEPAGE_CONNECTALL);
        }

        FileTransferMgr.getInstance();
        Log.d(TAG, "<initView> done");
    }

    @Override
    public void initListener() {
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(false));
        });

        getBinding().titleView.setRightIconClick(view -> {
            onBtnDeviceMgr(view);
        });

        getBinding().cbAllSelect.setOnClickListener(view -> {
            onBtnSelectAll(view);
        });

        getBinding().btnDoDelete.setOnClickListener(view -> {
            onBtnDelete(view);
        });

        Log.d(TAG, "<initListener> done");
    }

    public void onFragRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "<onFragRequestPermissionsResult> requestCode=" + requestCode);
        if (mPermHandler != null) {
            mPermHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public void onAllPermisonReqDone(boolean allGranted, final PermissionItem[] permItems) {
        Log.d(TAG, "<onAllPermisonReqDone> allGranted = " + allGranted);

        if (permItems[0].requestId == PermissionHandler.PERM_ID_CAMERA) {  // Camera权限结果
            if (allGranted) {
                //PagePilotManager.pageDeviceAddScanning();
            } else {
                popupMessage(getString(R.string.no_permission));
            }

        } else if (permItems[0].requestId == PermissionHandler.PERM_ID_RECORD_AUDIO) { // 麦克风权限结果
            if (allGranted) {
                mMsgHandler.sendEmptyMessage(MSGID_HOMEPAGE_CONNECTALL);
            } else {
                popupMessage(getString(R.string.no_permission));
            }
        }
    }



    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "<onStart> ");

        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
        connectMgr.registerListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "<onStop> ");

        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
        connectMgr.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "<onResume> ");

        int currentUiPage = PushApplication.getInstance().getUiPage();
        if (currentUiPage == Constant.UI_PAGE_HOME) {  // 当前界面
            mFragmentForeground = true;

            // 当从全屏播放界面返回来时，要重新设置各个设备的视频播放控件
            IConnectionObj connectObj = PushApplication.getInstance().getFullscrnConnectionObj();
            if (connectObj != null) {
                resetDeviceDisplayView(connectObj);
            }

            // 刷新所有设备界面显示
            mMsgHandler.sendEmptyMessage(MSGID_HOMEPAGE_REFRESH);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "<onPause> ");
        mFragmentForeground = false;
    }

    /**
     * @brief 响应 Back按键处理
     * @return 如果 Fragment已经处理返回 true; 否则返回false
     */
    boolean onBackKeyEvent() {
        if (mDevListAdapter.isInSelectMode()) {
            // 切换回非选择模式
            switchSelectMode(false);
            return true;
        }

        return false;
    }

    /**
     * @brief 选择模式 / 非选择模式 相互切换
     */
    void switchSelectMode(boolean selectMode) {

        if (selectMode) {
            // 切换到选择模式
            mDevListAdapter.switchSelectMode(true);
            getBinding().cbAllSelect.setChecked(false);
            mMainActivity.setNavigatebarVisibility(View.GONE);
            getBinding().clBottomDel.setVisibility(View.VISIBLE);
            getBinding().clBottomDel.invalidate();

        } else {
            // 切换到非选择模式
            mDevListAdapter.switchSelectMode(false);
            getBinding().cbAllSelect.setChecked(false);
            mMainActivity.setNavigatebarVisibility(View.VISIBLE);
            getBinding().clBottomDel.setVisibility(View.GONE);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////// Event & Widget Methods  ///////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * @brief 连接所有设备
     */
    void onMsgConnectAll(Message msg) {
        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
        List<DeviceInfo> deviceList = mDevListAdapter.getDatas();
        int i;
        for (i = 0; i < deviceList.size(); i++) {
            DeviceInfo deviceInfo = deviceList.get(i);
            if (deviceInfo.mConnectObj != null) {
                continue;
            }

            // 连接操作
            String attachMsg = "Call_" + deviceInfo.mNodeId + "_at_" + getTimestamp();
            IConnectionMgr.ConnectCreateParam createParam = new IConnectionMgr.ConnectCreateParam();
            createParam.mPeerNodeId = deviceInfo.mNodeId;
            createParam.mAttachMsg = attachMsg;
            createParam.mEncrypt = true;
            IConnectionObj connectObj = connectMgr.connectionCreate(createParam);
            if (connectObj == null) {
                popupMessage("Connect device: " + deviceInfo.mNodeId + " failure!");
                return;
            }

            // 更新 sessionId 和 提示信息
            connectObj.registerListener(mHomeFragment);  // 注册回调函数

            deviceInfo.mConnectObj = connectObj;
            mDevListAdapter.setItem(i, deviceInfo);
        }
    }

    /**
     * @brief 刷新所有设备状态显示
     */
    void onMsgRefreshAll(Message msg) {
        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
        List<IConnectionObj> connectList = connectMgr.getConnectionList();

        List<DeviceInfo> deviceList = mDevListAdapter.getDatas();
        int i;
        for (i = 0; i < deviceList.size(); i++) {
            DeviceInfo deviceInfo = deviceList.get(i);
            IConnectionObj connectObj = findConnectObjByDevNodeId(connectList, deviceInfo.mNodeId);
            if (connectObj != deviceInfo.mConnectObj) {
                Log.d(TAG, "<onMsgRefreshAll> devNodeId=" + deviceInfo.mNodeId
                    + ", oldConnectObj=" + deviceInfo.mConnectObj + ", newConnectObj" + connectObj);
                deviceInfo.mConnectObj = connectObj;
                mDevListAdapter.setItem(i, deviceInfo);
            }
        }
    }

    /**
     * @brief 根据设备 NodeId，找到相应的链接对象
     */
    IConnectionObj findConnectObjByDevNodeId(final List<IConnectionObj> connectList,
                                             final String devNodeId) {
        for (int i = 0; i < connectList.size(); i++) {
            IConnectionObj connectObj = connectList.get(i);

            String peerNodeId = connectObj.getInfo().mPeerNodeId;
            if (devNodeId.compareToIgnoreCase(peerNodeId) == 0) {
                return connectObj;
            }
        }
        return null;
    }


    /**
     * @brief 设备管理
     */
    void onBtnDeviceMgr(View view) {
        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();

        PopupMenu deviceMenu = new PopupMenu(getActivity(), view);
        getActivity().getMenuInflater().inflate(R.menu.menu_device, deviceMenu.getMenu());
//        MenuItem menuItem = deviceMenu.getMenu().getItem(2);
//        String title = "通话音效 (" + effectName + ")";
//        menuItem.setTitle(title);

        deviceMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.m_device_add:
                        onMenuAddDevice();
                        break;

                    case R.id.m_device_remove:
                        onMenuRemoveDevice();
                        break;

                    case R.id.m_talk_effect:
                        onMenuTalkEffect();
                        break;
                }
                return true;
            }
        });
        deviceMenu.show();
    }

    /**
     * @brief 全选按钮点击事件
     */
    void onBtnSelectAll(View view) {
        boolean allChecked = getBinding().cbAllSelect.isChecked();
        if (allChecked) {
            mDevListAdapter.setAllItemsSelectStatus(true);
        } else {
            mDevListAdapter.setAllItemsSelectStatus(false);
        }
    }


    /**
     * @brief 删除按钮点击事件
     */
    void onBtnDelete(View view) {
        List<DeviceInfo> selectedList = mDevListAdapter.getSelectedItems();
        int selectedCount = selectedList.size();
        if (selectedCount <= 0) {
            popupMessage("Please select one device at least!");
            return;
        }

        showLoadingView();

        // 挂断所有要删除的链接
        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
        for (int i = 0; i < selectedCount; i++) {
            DeviceInfo deviceInfo = selectedList.get(i);
            if (deviceInfo.mConnectObj != null) {    // 要删除的设备进行断连操作
                connectMgr.connectionDestroy(deviceInfo.mConnectObj);
            }
        }

        // 获取所有剩余的设备列表
        int deleteCount = mDevListAdapter.deleteSelectedItems();

        // 切换回 非选择模式
        switchSelectMode(false);

        // 保存新的设备列表
        List<DeviceInfo> deviceInfoList = mDevListAdapter.getDatas();
        deviceListStore(deviceInfoList);

        hideLoadingView();

        popupMessage("Total " + selectedCount + " devices already deleted!");
    }


    /**
     * @brief 新增一个设备
     */
    void onMenuAddDevice() {
        DialogNewDevice newDevDlg = new DialogNewDevice(this.getActivity());
        newDevDlg.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
            @Override
            public void onLeftButtonClick() {
            }

            @Override
            public void onRightButtonClick() {
            }
        });

        newDevDlg.mSingleCallback = (integer, obj) -> {
            if (integer == 0) {
                String nodeId = (String)obj;
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemByDevNodeId(nodeId);
                if (findResult.mDevInfo != null) {
                    popupMessage("Device :" + nodeId + " already exist!");
                    return;
                }

                DeviceInfo newDevice = new DeviceInfo();
                newDevice.mNodeId = nodeId;
                mDevListAdapter.addNewItem(newDevice);

                List<DeviceInfo> deviceInfoList = mDevListAdapter.getDatas();
                deviceListStore(deviceInfoList);
            }
        };
        newDevDlg.setCanceledOnTouchOutside(false);
        newDevDlg.show();
    }

    /**
     * @brief 进入编辑模式删除设备
     */
    void onMenuRemoveDevice() {
//        List<DeviceInfo> deviceInfoList = mDevListAdapter.getDatas();
//        int devCount = deviceInfoList.size();
//        int i;
//        for (i = 0; i < devCount; i++) {
//            DeviceInfo deviceInfo = deviceInfoList.get(i);
//            if (deviceInfo.mConnectionId != null) {
//                popupMessage("There are some devices in talking, should hangup all devices!");
//                return;
//            }
//        }

        // 切换到选择模式
        switchSelectMode(true);
    }

    /**
     * @brief 设置通话音效
     */
    void onMenuTalkEffect() {
        PopupMenu effectMenu = new PopupMenu(getActivity(), getBinding().titleView);
        getActivity().getMenuInflater().inflate(R.menu.menu_audio_effect, effectMenu.getMenu());

        effectMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.m_audeffect_normal: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.NORMAL);
                    } break;
                    case R.id.m_audeffect_ktv: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.KTV);
                    } break;
                    case R.id.m_audeffect_concert: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.CONCERT);
                    } break;
                    case R.id.m_audeffect_studio: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.STUDIO);
                    } break;
                    case R.id.m_audeffect_photograph: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.PHONOGRAPH);
                    } break;
                    case R.id.m_audeffect_virtualstereo: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.VIRTUALSTEREO);
                    } break;
                    case R.id.m_audeffect_spacial: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.SPACIAL);
                    } break;
                    case R.id.m_audeffect_ethereal: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.ETHEREAL);
                    } break;
                    case R.id.m_audeffect_voice3d: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.VOICE3D);
                    } break;
                    case R.id.m_audeffect_uncle: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.UNCLE);
                    } break;
                    case R.id.m_audeffect_oldman: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.OLDMAN);
                    } break;
                    case R.id.m_audeffect_boy: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.BOY);
                    } break;
                    case R.id.m_audeffect_sister: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.SISTER);
                    } break;
                    case R.id.m_audeffect_girl: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.GIRL);
                    } break;
                    case R.id.m_audeffect_pigking: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.PIGKING);
                    } break;
                    case R.id.m_audeffect_hulk: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.HULK);
                    } break;
                    case R.id.m_audeffect_rnb: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.RNB);
                    } break;
                    case R.id.m_audeffect_popular: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.POPULAR);
                    } break;
                    case R.id.m_audeffect_pitchcorrection: {
                        onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.PITCHCORRECTION);
                    } break;
                }
                return true;
            }
        });
        effectMenu.show();
    }

    void onMenuAudioEffect(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE effectId) {
        AIotAppSdkFactory.getInstance().setPublishAudioEffect(effectId);
    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////// Session Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @brief 连接断开 按钮点击事件
     */
    void onDevItemDialHangupClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mConnectObj == null) {
            handelDevItemDialEncryptChoice(view,position,deviceInfo);
        } else {
            handelDevItemHangupAction(view,position,deviceInfo);
        }


        /*
        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }

        if (deviceInfo.mConnectObj == null) {
            // 连接操作
            String attachMsg = "Call_" + deviceInfo.mNodeId + "_at_" + getTimestamp();
            IConnectionMgr.ConnectCreateParam createParam = new IConnectionMgr.ConnectCreateParam();
            createParam.mPeerNodeId = deviceInfo.mNodeId;
            createParam.mAttachMsg = attachMsg;
            ALog.getInstance().d(TAG, "<onDevItemDialHangupClick> [IOTSDK] connect device: " + deviceInfo
                                    + ", position=" + position);
            IConnectionObj connectObj = connectMgr.connectionCreate(createParam);
            if (connectObj == null) {
                popupMessage("Connect device: " + deviceInfo.mNodeId + " failure!");
                return;
            }

            // 注册回调函数
            connectObj.registerListener(mHomeFragment);

            // 更新 sessionId 和 提示信息
            deviceInfo.mConnectObj = connectObj;
            mDevListAdapter.setItem(position, deviceInfo);

        } else {
            // 断开操作
            ALog.getInstance().d(TAG, "<onDevItemDialHangupClick> [IOTSDK] disconnect device: " + deviceInfo
                    + ", position=" + position);
            int errCode = connectMgr.connectionDestroy(deviceInfo.mConnectObj);
            if (errCode != ErrCode.XOK) {
                popupMessage("Disconnect device: " + deviceInfo.mNodeId + " failure, errCode=" + errCode);
                return;
            }

            // 更新设备状态信息
            deviceInfo.clear();
            mDevListAdapter.setItem(position, deviceInfo);
            popupMessage("Disconnect device: " + deviceInfo.mNodeId + " successful!");
        }
         */
    }

    void handelDevItemDialEncryptChoice(View view, int position, DeviceInfo deviceInfo) {
        new AlertDialog.Builder(getContext())
        .setTitle("是否加密？")
        .setMessage("")
        .setPositiveButton("加密", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                handelDevItemDialAction(view,position,deviceInfo,true);
                Log.d(TAG, "<handelItemDialHangupClick> encrypt is true");
            }
        })
        .setNegativeButton("不加密", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                handelDevItemDialAction(view,position,deviceInfo,false);
                Log.d(TAG, "<handelItemDialHangupClick> encrypt is false");
            }
        })
        .show();
    }

    void handelDevItemDialAction(View view, int position, DeviceInfo deviceInfo,boolean mEncrypt) {

        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        // 连接操作
        String attachMsg = "Call_" + deviceInfo.mNodeId + "_at_" + getTimestamp();
        IConnectionMgr.ConnectCreateParam createParam = new IConnectionMgr.ConnectCreateParam();
        createParam.mPeerNodeId = deviceInfo.mNodeId;
        createParam.mAttachMsg = attachMsg;
        createParam.mEncrypt = mEncrypt;
        ALog.getInstance().d(TAG, "<onDevItemDialHangupClick> [IOTSDK] connect device: " + deviceInfo
                + ", position=" + position);
        IConnectionObj connectObj = connectMgr.connectionCreate(createParam);
        if (connectObj == null) {
            popupMessage("Connect device: " + deviceInfo.mNodeId + " failure!");
            return;
        }

        // 注册回调函数
        connectObj.registerListener(mHomeFragment);

        // 更新 sessionId 和 提示信息
        deviceInfo.mConnectObj = connectObj;
        mDevListAdapter.setItem(position, deviceInfo);

    }

    void handelDevItemHangupAction(View view, int position, DeviceInfo deviceInfo) {
        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
        // 断开操作
        ALog.getInstance().d(TAG, "<onDevItemDialHangupClick> [IOTSDK] disconnect device: " + deviceInfo
                + ", position=" + position);
        int errCode = connectMgr.connectionDestroy(deviceInfo.mConnectObj);
        if (errCode != ErrCode.XOK) {
            popupMessage("Disconnect device: " + deviceInfo.mNodeId + " failure, errCode=" + errCode);
            return;
        }

        // 更新设备状态信息
        deviceInfo.clear();
        mDevListAdapter.setItem(position, deviceInfo);
        popupMessage("Disconnect device: " + deviceInfo.mNodeId + " successful!");
    }

    /**
     * @brief 订阅 按钮点击事件
     */
    void onDevItemPreviewClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mConnectObj == null) {
            return;
        }
        IConnectionObj.ConnectionInfo connectInfo = deviceInfo.mConnectObj.getInfo();
        if (connectInfo.mState != IConnectionObj.STATE_CONNECTED) {  // 只在连接成功后操作
            Log.d(TAG, "<onDevItemPreviewClick> not connected, state=" + connectInfo.mState);
            return;
        }
        IConnectionObj.StreamStatus streamStatus = deviceInfo.mConnectObj.getStreamStatus(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1);
        int errCode;

        if (!streamStatus.mSubscribed) {
            // 订阅流 PUBLIC_1
            ALog.getInstance().d(TAG, "<onDevItemDialHangupClick> [IOTSDK] subscribe device: " + deviceInfo
                    + ", position=" + position);
            errCode = deviceInfo.mConnectObj.streamSubscribeStart(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1, "xxxx");
            if (errCode != ErrCode.XOK) {
                popupMessage("Subscribe PUB_1 stream of: " + deviceInfo.mNodeId + " failure, errCode=" + errCode);
                return;
            }

            errCode = deviceInfo.mConnectObj.setVideoDisplayView(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1, deviceInfo.mVideoView);

        } else {
            // 取消 流 PUBLIC_1的订阅
            ALog.getInstance().d(TAG, "<onDevItemDialHangupClick> [IOTSDK] unsubscribe device: " + deviceInfo
                    + ", position=" + position);
            errCode = deviceInfo.mConnectObj.streamSubscribeStop(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1);
            if (errCode != ErrCode.XOK) {
                popupMessage("Unsubscribe PUB_1 stream of: " + deviceInfo.mNodeId + " failure, errCode=" + errCode);
                return;
            }
        }

        mDevListAdapter.setItem(position, deviceInfo);
    }

    /**
     * @brief 静音 按钮点击事件
     */
    void onDevItemMuteAudioClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mConnectObj == null) {
            return;
        }
        IConnectionObj.ConnectionInfo connectInfo = deviceInfo.mConnectObj.getInfo();
        if (connectInfo.mState != IConnectionObj.STATE_CONNECTED) {  // 只在连接成功后操作
            Log.d(TAG, "<onDevItemMuteAudioClick> not connected, state=" + connectInfo.mState);
            return;
        }

        IConnectionObj.StreamStatus streamStatus = deviceInfo.mConnectObj.getStreamStatus(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1);
        if (!streamStatus.mSubscribed) {
            popupMessage("The stream have NOT subscribed, please subscribe firstly");
            return;
        }

        boolean audioMute = streamStatus.mAudioMute;
        int errCode = deviceInfo.mConnectObj.muteAudioPlayback(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1, (!audioMute));
        if (errCode != ErrCode.XOK) {
            popupMessage("Mute or unmute device: " + deviceInfo.mNodeId + " failure, errCode=" + errCode);
            return;
        }

        mDevListAdapter.setItem(position, deviceInfo);
    }

    /**
     * @brief 录像 按钮点击事件
     */
    void onDevItemRecordClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mConnectObj == null) {
            return;
        }
        IConnectionObj.ConnectionInfo connectInfo = deviceInfo.mConnectObj.getInfo();
        if (connectInfo.mState != IConnectionObj.STATE_CONNECTED) {  // 只在连接成功后操作
            Log.d(TAG, "<onDevItemRecordClick> not connected, state=" + connectInfo.mState);
            return;
        }
        IConnectionObj.StreamStatus streamStatus = deviceInfo.mConnectObj.getStreamStatus(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1);
        if (!streamStatus.mSubscribed) {
            popupMessage("The stream have NOT subscribed, please subscribe firstly");
            return;
        }

        boolean recording = deviceInfo.mConnectObj.isStreamRecording(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1);
        if (recording) {
            // 停止录像
            int errCode = deviceInfo.mConnectObj.streamRecordStop(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1);
            if (errCode != ErrCode.XOK) {
                popupMessage("Device: " + deviceInfo.mNodeId + " stop recording failure, errCode=" + errCode);
                return;
            }

            popupMessage("Device: " + deviceInfo.mNodeId + " recording stopped, save to file: " + deviceInfo.mRcdFilePath);
            mDevListAdapter.setItem(position, deviceInfo);

        } else {
            // 启动录像
            String strSavePath = FileUtils.getFileSavePath(deviceInfo.mNodeId, IConnectionObj.STREAM_ID.BROADCAST_STREAM_1, false);
            int errCode = deviceInfo.mConnectObj.streamRecordStart(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1, strSavePath);
            if (errCode != ErrCode.XOK) {
                popupMessage("Device: " + deviceInfo.mNodeId + " start recording failure, errCode=" + errCode);
                return;
            }

            popupMessage("Device: " + deviceInfo.mNodeId + " start recording......");
            deviceInfo.mRcdFilePath = strSavePath;
            mDevListAdapter.setItem(position, deviceInfo);
        }
    }

    /**
     * @brief 通话 按钮点击事件
     */
    void onDevItemMicClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mConnectObj == null) {
            return;
        }
        IConnectionObj.ConnectionInfo connectInfo = deviceInfo.mConnectObj.getInfo();
        if (connectInfo.mState != IConnectionObj.STATE_CONNECTED) {  // 只在连接成功后操作
            Log.d(TAG, "<onDevItemMicClick> not connected, state=" + connectInfo.mState);
            return;
        }
        boolean audioPublishing = connectInfo.mAudioPublishing;

        int errCode = deviceInfo.mConnectObj.publishAudioEnable(!audioPublishing, IConnectionObj.AUDIO_CODEC_TYPE.G722);
        if (errCode != ErrCode.XOK) {
            popupMessage("Voice or unvoice device: " + deviceInfo.mNodeId + " failure, errCode=" + errCode);
            return;
        }

        mDevListAdapter.setItem(position, deviceInfo);
    }

    /**
     * @brief 全屏 按钮点击事件
     */
    void onDevItemFullscrnClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mConnectObj == null) {
            return;
        }
        IConnectionObj.ConnectionInfo connectInfo = deviceInfo.mConnectObj.getInfo();
        if (connectInfo.mState != IConnectionObj.STATE_CONNECTED) {  // 只在连接成功后操作
            Log.d(TAG, "<onDevItemFullscrnClick> not connected, state=" + connectInfo.mState);
            return;
        }

        PushApplication.getInstance().setFullscrnConnectionObj(deviceInfo.mConnectObj);
        gotoDevPreviewActivity();
    }


    /**
     * @brief 截屏 按钮点击事件
     */
    void onDevItemShotCaptureClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mConnectObj == null) {
            return;
        }
        IConnectionObj.ConnectionInfo connectInfo = deviceInfo.mConnectObj.getInfo();
        if (connectInfo.mState != IConnectionObj.STATE_CONNECTED) {  // 只在连接成功后操作
            Log.d(TAG, "<onDevItemShotCaptureClick> not connected, state=" + connectInfo.mState);
            return;
        }
        IConnectionObj.StreamStatus streamStatus = deviceInfo.mConnectObj.getStreamStatus(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1);
        if (!streamStatus.mSubscribed) {
            popupMessage("The stream have NOT subscribed, please subscribe firstly");
            return;
        }

        String strSavePath = FileUtils.getFileSavePath(deviceInfo.mNodeId, IConnectionObj.STREAM_ID.BROADCAST_STREAM_1, true);
        int errCode = deviceInfo.mConnectObj.streamVideoFrameShot(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1, strSavePath);
        if (errCode != ErrCode.XOK) {
            popupMessage("Device: " + deviceInfo.mNodeId + " shot capture failure, errCode=" + errCode);
            return;
        }
    }


    /**
     * @brief 发送命令 按钮点击事件
     */
    void onDevItemCommandClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mConnectObj == null) {
            return;
        }
        IConnectionObj.ConnectionInfo connectInfo = deviceInfo.mConnectObj.getInfo();
        if (connectInfo.mState != IConnectionObj.STATE_CONNECTED) {  // 只在连接成功后操作
            Log.d(TAG, "<onDevItemCommandClick> not connected, state=" + connectInfo.mState);
            return;
        }

        DialogInputCommand inputCmdDlg = new DialogInputCommand(this.getActivity());
        inputCmdDlg.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
            @Override
            public void onLeftButtonClick() {
            }

            @Override
            public void onRightButtonClick() {
            }
        });

        inputCmdDlg.mSingleCallback = (integer, obj) -> {
            if (integer == 0) {
                String commandData = (String)obj;
                deviceInfo.mConnectObj.sendMessageData(commandData.getBytes(StandardCharsets.UTF_8));
            }
        };
        inputCmdDlg.setCanceledOnTouchOutside(false);
        inputCmdDlg.show();
    }


    /**
     * @brief 流管理 按钮点击事件
     */
    void onDevItemStreamMgrClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mConnectObj == null) {
            return;
        }
        IConnectionObj.ConnectionInfo connectInfo = deviceInfo.mConnectObj.getInfo();
        if (connectInfo.mState != IConnectionObj.STATE_CONNECTED) {  // 只在连接成功后操作
            Log.d(TAG, "<onDevItemStreamMgrClick> not connected, state=" + connectInfo.mState);
            return;
        }

        // 界面跳转
        PushApplication.getInstance().setFullscrnConnectionObj(deviceInfo.mConnectObj);
        Intent intent = new Intent(getActivity(), DevStreamActivity.class);
        startActivity(intent);
        PushApplication.getInstance().setUiPage(Constant.UI_PAGE_STREAM_MGR);
    }


    /**
     * @brief 选择 按钮点击事件
     */
    void onDevItemCheckBox(CompoundButton compoundButton, int position, final DeviceInfo deviceInfo,
                           boolean selected) {
        // 处理全选按钮
        boolean selectAll = mDevListAdapter.isAllItemsSelected();
        getBinding().cbAllSelect.setChecked(selectAll);
    }

    void onDevItemCheckBoxClick(View view, int position, DeviceInfo deviceInfo) {
        boolean selected = (!deviceInfo.mSelected);
        deviceInfo.mSelected = selected;
        mDevListAdapter.setItemSelectStatus(position, deviceInfo);

        // 处理全选按钮
        boolean selectAll = mDevListAdapter.isAllItemsSelected();
        getBinding().cbAllSelect.setChecked(selectAll);

    }

    String getTimestamp() {
        String time_txt = "";
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) ;
        int month = calendar.get(Calendar.MONTH);
        int date = calendar.get(Calendar.DATE);
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int ms = calendar.get(Calendar.MILLISECOND);

        time_txt = String.format(Locale.getDefault(), "%d-%02d-%02d %02d:%02d:%02d.%d",
                year, month, date, hour,minute, second, ms);
        return time_txt;
    }

    void gotoDevPreviewActivity() {
        Intent intent = new Intent(getActivity(), DevPreviewActivity.class);
        startActivity(intent);
        PushApplication.getInstance().setUiPage(Constant.UI_PAGE_FULLSCRN);
    }

    /**
     * @brief 重新设置所有设备的视频显示控件
     */
    void resetDeviceDisplayView(final IConnectionObj connectObj) {
        String devNodeId = connectObj.getInfo().mPeerNodeId;

        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
        List<DeviceInfo> deviceList = mDevListAdapter.getDatas();
        if (deviceList == null) {
            return;
        }
        int deviceCount = deviceList.size();
        for (int i = 0; i < deviceCount; i++) {
            DeviceInfo deviceInfo = deviceList.get(i);
            if (deviceInfo.mConnectObj == null) {
                continue;
            }
            if (deviceInfo.mNodeId.compareTo(devNodeId) == 0) {
                 mDevListAdapter.setItem(i, deviceInfo);  // 刷新显示
                Log.d(TAG, "<resetDeviceDisplayView> connectObj=" + connectObj
                        + ", mNodeId=" + deviceInfo.mNodeId);
                return;
            }
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////// Override Methods of IConnectionMgr.ICallback  ///////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onConnectionCreateDone(final IConnectionObj connectObj, int errCode) {
        String peerNodeId = connectObj.getInfo().mPeerNodeId;
        Log.d(TAG, "<onConnectionCreateDone> [IOTSDK/] connectObj=" + connectObj
                + ", errCode=" + errCode);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemByDevNodeId(peerNodeId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onConnectionCreateDone> NOT found connection, peerNodeId=" + peerNodeId);
                    return;
                }
                if (errCode != ErrCode.XOK) {  // 连接设备失败
                    popupMessage("Connect device: " + findResult.mDevInfo.mNodeId + " failure, errCode=" + errCode);
                    findResult.mDevInfo.clear();  // 重置设备信息
                    mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);
                    return;
                }

                // 更新设备状态信息
                 mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);
            }
        });
    }

    @Override
    public void onPeerAnswerOrReject(final IConnectionObj connectObj, boolean answer) {
        String peerNodeId = connectObj.getInfo().mPeerNodeId;
        Log.d(TAG, "<onPeerAnswerOrReject> [IOTSDK/] connectObj=" + connectObj
                + ", answer=" + answer);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemByDevNodeId(peerNodeId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onPeerAnswerOrReject> NOT found connection, peerNodeId=" + peerNodeId);
                    return;
                }
                if (!mFragmentForeground) {  // 在后台时不提示了
                    return;
                }

                String strAnswer = (answer) ? " answer " : " reject ";
                popupMessage("Peer device: " + findResult.mDevInfo.mNodeId + strAnswer + "connection!");
            }
        });
    }

    @Override
    public void onPeerDisconnected(final IConnectionObj connectObj, int errCode) {
        String peerNodeId = connectObj.getInfo().mPeerNodeId;
        ALog.getInstance().d(TAG, "<onPeerDisconnected> [IOTSDK/] connectObj=" + connectObj
                + ", peerNodeId=" + connectObj.getInfo().mPeerNodeId + ", errCode=" + errCode);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemByDevNodeId(peerNodeId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onPeerDisconnected> NOT found connection, peerNodeId=" + peerNodeId);
                    return;
                }
                // 更新设备状态信息
                findResult.mDevInfo.clear();
                mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);

                if (!mFragmentForeground) {  // 在后台时不提示了
                    return;
                }
                popupMessage("Peer device: " + findResult.mDevInfo.mNodeId + " disconnected!");
            }
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////
    //////////////// Override Methods of IConnectionObj.ICallback  ////////////////////
    ///////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onStreamFirstFrame(final IConnectionObj connectObj, IConnectionObj.STREAM_ID subStreamId,
                                     int videoWidth, int videoHeight) {
        Log.d(TAG, "<onStreamFirstFrame> [IOTSDK/] connectObj=" + connectObj
                + ", subStreamId=" + DevStreamUtils.getStreamName(subStreamId)
                + ", videoWidth=" + videoWidth + ", videoHeight=" + videoHeight);
    }

    @Override
    public void onStreamVideoFrameShotDone(final IConnectionObj connectObj, IConnectionObj.STREAM_ID subStreamId, int errCode,
                                       final String filePath, int width, int height) {
        String peerNodeId = connectObj.getInfo().mPeerNodeId;
        Log.d(TAG, "<onStreamVideoFrameShotDone> [IOTSDK/] connectObj=" + connectObj
                + ", subStreamId=" + DevStreamUtils.getStreamName(subStreamId)
                + ", errCode=" + errCode + ", filePath=" + filePath);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemByDevNodeId(peerNodeId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onPeerHangup> NOT found connection, peerNodeId=" + peerNodeId);
                    return;
                }
                if (!mFragmentForeground) {  // 在后台时不提示了
                    return;
                }

                if (errCode != ErrCode.XOK) {
                    popupMessage("Device: " + findResult.mDevInfo.mNodeId + " frame shot failure, errCode=" + errCode);
                    return;
                }

                popupMessage("Device: " + findResult.mDevInfo.mNodeId + " frame shot successful, save to file: " + filePath);
            }
        });
    }

    @Override
    public void onStreamError(final IConnectionObj connectObj,
                              final IConnectionObj.STREAM_ID subStreamId, int errCode) {
        Log.d(TAG, "<onStreamError> [IOTSDK/] connectObj=" + connectObj
                + ", subStreamId=" + DevStreamUtils.getStreamName(subStreamId)
                + ", errCode=" + errCode);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String deviceId = connectObj.getInfo().mPeerNodeId;
                String streamName = DevStreamUtils.getStreamName(subStreamId);
                popupMessage("Device: " + deviceId + "  Stream: " + streamName + " failure, errCode=" + errCode);
            }
        });
    }

    @Override
    public void onMessageSendDone(final IConnectionObj connectObj, int errCode,
                                  UUID signalId, final byte[] messageData) {
        String peerNodeId = connectObj.getInfo().mPeerNodeId;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemByDevNodeId(peerNodeId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onSignalSendDone> NOT found session, peerNodeId=" + peerNodeId);
                    return;
                }

                if (errCode == ErrCode.XOK) {
                    popupMessage("Send signal data to device: " + findResult.mDevInfo.mNodeId + " successful!");
                } else {
                    popupMessage("Send signal data to device: " + findResult.mDevInfo.mNodeId + " failure, errCode=" + errCode);
                }
             }
        });
    }

    @Override
    public void onMessageReceived(final IConnectionObj connectObj, final byte[] recvedMsgData) {
        String peerNodeId = connectObj.getInfo().mPeerNodeId;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemByDevNodeId(peerNodeId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onSignalRecved> NOT found session, peerNodeId=" + peerNodeId);
                    return;
                }
                if (!mFragmentForeground) {  // 在后台时不提示了
                    return;
                }

                String recvText = new String(recvedMsgData, StandardCharsets.UTF_8);
                popupMessageLongTime("Recv message: " + recvText + " from devNodeId=" + findResult.mDevInfo.mNodeId);
            }
        });
    }


    public void onFileTransRecvStart(final IConnectionObj connectObj, final byte[] startDescrption) {
        String peerNodeId = connectObj.getInfo().mPeerNodeId;
        Log.d(TAG, "<onFileTransRecvStart> [IOTSDK/] connectObj=" + connectObj
                + ", startDescrption.length=" + startDescrption.length);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FileTransferMgr.getInstance().onFileTransRecvStart(connectObj, startDescrption);
            }
        });
    }

    public void onFileTransRecvData(final IConnectionObj connectObj, final byte[] recvedData) {
        String peerNodeId = connectObj.getInfo().mPeerNodeId;
        Log.d(TAG, "<onFileTransRecvData> [IOTSDK/] peerNodeId=" + peerNodeId
                + ", recvedData.length=" + recvedData.length);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FileTransferMgr.getInstance().onFileTransRecvData(connectObj, recvedData);
            }
        });
    }

    public void onFileTransRecvDone(final IConnectionObj connectObj, boolean transferEnd,
                                    final byte[] doneDescrption) {
        String peerNodeId = connectObj.getInfo().mPeerNodeId;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FileTransferMgr.getInstance().onFileTransRecvDone(connectObj, transferEnd, doneDescrption);
            }
        });
    }



    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of DeviceList Storage  ///////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * @brief 从本地存储读取设备列表信息
     */
    private List<DeviceInfo> deviceListLoad() {
        List<DeviceInfo> deviceInfoList = new ArrayList<>();

        String localUserId = ThirdAccountMgr.getInstance().getLocalNodeId();
        String keyDevCount = localUserId + "_device_count";

        int devCount = AppStorageUtil.queryIntValue(keyDevCount, 0);
        for (int i = 0; i < devCount; i++) {
            DeviceInfo deviceInfo = new DeviceInfo();
            String keyNodeId = localUserId + "_device_index_" + i;
            deviceInfo.mNodeId = AppStorageUtil.queryValue(keyNodeId, null);
            if (deviceInfo.mNodeId == null) {
                Log.e(TAG, "<deviceListLoad> fail to read key: " + keyNodeId);
                continue;
            }
            deviceInfoList.add(deviceInfo);
        }

        Log.d(TAG, "<deviceListLoad> stored device list, devCount=" + devCount);
        return deviceInfoList;
    }


    /**
     * @brief 将设备列表存储到本地
     */
    private void deviceListStore(final List<DeviceInfo> deviceInfoList) {
        int devCount = deviceInfoList.size();

        String localUserId = ThirdAccountMgr.getInstance().getLocalNodeId();
        String keyDevCount = localUserId + "_device_count";

        AppStorageUtil.keepShared(keyDevCount, devCount);
        for (int i = 0; i < devCount; i++) {
            DeviceInfo deviceInfo = deviceInfoList.get(i);
            String keyNodeId = localUserId + "_device_index_" + i;
            AppStorageUtil.keepShared(keyNodeId, deviceInfo.mNodeId);
        }

        Log.d(TAG, "<deviceListStore> stored device list, devCount=" + devCount);
    }

    ///////////////////////////////////////////////////////////////////////
    //////////////////// Methods for Audio Effect  ///////////////////////
    ///////////////////////////////////////////////////////////////////////
    final static IAgoraIotAppSdk.AUDIO_EFFECT_TYPE[] mAudEffectIdArray = {
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.NORMAL,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.KTV,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.CONCERT,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.STUDIO,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.PHONOGRAPH,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.VIRTUALSTEREO,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.SPACIAL,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.ETHEREAL,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.VOICE3D,

            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.UNCLE,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.OLDMAN,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.BOY,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.SISTER,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.GIRL,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.PIGKING,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.HULK,

            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.RNB,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.POPULAR,
            IAgoraIotAppSdk.AUDIO_EFFECT_TYPE.PITCHCORRECTION
    };

    final static String[] mAudEffectNameArray = {
            "原声", "KTV", "演唱会", "录音棚", "留声机", "虚拟立体声", "空旷", "空灵", "3D人声",
            "大叔", "老男人", "男孩", "少女", "女孩", "猪八戒", "绿巨人",
            "R&B", "流行", "电音"
    };

    String getAudioEffectName(IAgoraIotAppSdk.AUDIO_EFFECT_TYPE effectId) {
        int count = mAudEffectIdArray.length;
        for (int i = 0; i < count; i++) {
            if (effectId == mAudEffectIdArray[i]) {
                return mAudEffectNameArray[i];
            }
        }

        return mAudEffectNameArray[0];
    }
}
