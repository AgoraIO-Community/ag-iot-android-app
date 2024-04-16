package io.agora.falcondemo.models.devstream;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.agora.baselibrary.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.agora.falcondemo.common.Constant;
import io.agora.falcondemo.databinding.ActivityDevStreammgrBinding;
import io.agora.falcondemo.models.home.DeviceInfo;
import io.agora.falcondemo.models.home.DeviceListAdapter;
import io.agora.falcondemo.thirdpartyaccount.ThirdAccountMgr;
import io.agora.falcondemo.utils.AppStorageUtil;
import io.agora.falcondemo.utils.DevStreamUtils;
import io.agora.falcondemo.utils.FileUtils;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.falcondemo.R;
import io.agora.falcondemo.base.BaseViewBindingActivity;
import io.agora.falcondemo.base.PushApplication;
import io.agora.falcondemo.databinding.ActivityDevPreviewBinding;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.IConnectionMgr;
import io.agora.iotlink.IConnectionObj;


public class DevStreamActivity extends BaseViewBindingActivity<ActivityDevStreammgrBinding>
    implements IConnectionMgr.ICallback, IConnectionObj.ICallback {
    private static final String TAG = "IOTLINK/DevStreamAct";


    private IConnectionObj mConnectObj = null;
    private String mDeviceNodeId;
    private DevStreamListAdapter mStreamListAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;



    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseActivity /////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected ActivityDevStreammgrBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDevStreammgrBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        mConnectObj = PushApplication.getInstance().getFullscrnConnectionObj();
        IConnectionObj.ConnectionInfo connectInfo = mConnectObj.getInfo();
        mDeviceNodeId = connectInfo.mPeerNodeId;

        // 添加固定的17个Stream，第一个必须是 PUBLIC_STREAM_1，交错添加剩余的 Private 和 Public
        IConnectionObj.STREAM_ID[] streamIdArray = {
            IConnectionObj.STREAM_ID.PUBLIC_STREAM_1, IConnectionObj.STREAM_ID.PRIVATE_STREAM_1,
            IConnectionObj.STREAM_ID.PUBLIC_STREAM_2, IConnectionObj.STREAM_ID.PRIVATE_STREAM_2,
            IConnectionObj.STREAM_ID.PUBLIC_STREAM_3, IConnectionObj.STREAM_ID.PRIVATE_STREAM_3,
            IConnectionObj.STREAM_ID.PUBLIC_STREAM_4, IConnectionObj.STREAM_ID.PRIVATE_STREAM_4,
            IConnectionObj.STREAM_ID.PUBLIC_STREAM_5, IConnectionObj.STREAM_ID.PRIVATE_STREAM_5,
            IConnectionObj.STREAM_ID.PUBLIC_STREAM_6, IConnectionObj.STREAM_ID.PRIVATE_STREAM_6,
            IConnectionObj.STREAM_ID.PUBLIC_STREAM_7, IConnectionObj.STREAM_ID.PRIVATE_STREAM_7,
            IConnectionObj.STREAM_ID.PUBLIC_STREAM_8, IConnectionObj.STREAM_ID.PRIVATE_STREAM_8,
            IConnectionObj.STREAM_ID.PUBLIC_STREAM_9, IConnectionObj.STREAM_ID.PRIVATE_STREAM_9
        };
        List<DevStreamInfo> devStreamList = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            if ((streamIdArray[i]) == IConnectionObj.STREAM_ID.PRIVATE_STREAM_1) { // 跳过 PRIVATE_1
                continue;
            }
            DevStreamInfo devStream = new DevStreamInfo();
            devStream.mStreamId = streamIdArray[i];
            devStreamList.add(devStream);
        }


        // 设置列表显示控件
        mStreamListAdapter = new DevStreamListAdapter(devStreamList, mConnectObj);
        mStreamListAdapter.setOwner(this);
        mStreamListAdapter.setPublishAudio(false);
        getBinding().rvDevStreamList.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvDevStreamList.setAdapter(mStreamListAdapter);
        getBinding().rvDevStreamList.setItemViewCacheSize(20);
        mStreamListAdapter.setMRVItemClickListener((view, position, data) -> {
        });

        mSwipeRefreshLayout = getBinding().srlStreamList;
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });

        Log.d(TAG, "<initView> ");
    }

    @Override
    protected boolean isCanExit() {
        return false;
    }

    @Override
    public void initListener() {
        getBinding().titleView.setLeftClick(view -> {
            onBtnBack(false);
        });

        getBinding().titleView.setRightIconClick(view -> {
            onBtnBack(false);
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        // 注册回调
        AIotAppSdkFactory.getInstance().getConnectionMgr().registerListener(this);
        mConnectObj = PushApplication.getInstance().getFullscrnConnectionObj();
        if (mConnectObj != null) {
            mConnectObj.registerListener(this);
        }

        Log.d(TAG, "<onStart> mConnectObj=" + mConnectObj);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "<onStop> ");

        // 注销回调
        AIotAppSdkFactory.getInstance().getConnectionMgr().unregisterListener(this);
        if (mConnectObj != null) {
            mConnectObj.unregisterListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "<onResume> ");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "<onPause> ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "<onDestroy> ");
        mConnectObj = null;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBtnBack(false);
        }
        return super.onKeyDown(keyCode, event);
    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////// Events and Message Handle Methods ///////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * @brief 处理返回到主界面
     * @param  disconnected : 设备是否已经断连
     */
    void onBtnBack(boolean disconnected) {
        PushApplication.getInstance().setUiPage(Constant.UI_PAGE_HOME); // 切换回主界面
        if (disconnected) {
            finish();
            return;
        }

        // 当前流的处理
        IConnectionObj.STREAM_ID[] streamIdArray = {
            IConnectionObj.STREAM_ID.PUBLIC_STREAM_1,  IConnectionObj.STREAM_ID.PUBLIC_STREAM_2,
            IConnectionObj.STREAM_ID.PUBLIC_STREAM_3,  IConnectionObj.STREAM_ID.PUBLIC_STREAM_4,
            IConnectionObj.STREAM_ID.PUBLIC_STREAM_5,  IConnectionObj.STREAM_ID.PUBLIC_STREAM_6,
            IConnectionObj.STREAM_ID.PUBLIC_STREAM_7,  IConnectionObj.STREAM_ID.PUBLIC_STREAM_8,
            IConnectionObj.STREAM_ID.PUBLIC_STREAM_9,
            IConnectionObj.STREAM_ID.PRIVATE_STREAM_2, IConnectionObj.STREAM_ID.PRIVATE_STREAM_3,
            IConnectionObj.STREAM_ID.PRIVATE_STREAM_4, IConnectionObj.STREAM_ID.PRIVATE_STREAM_5,
            IConnectionObj.STREAM_ID.PRIVATE_STREAM_6, IConnectionObj.STREAM_ID.PRIVATE_STREAM_7,
            IConnectionObj.STREAM_ID.PRIVATE_STREAM_8, IConnectionObj.STREAM_ID.PRIVATE_STREAM_9
        };
        int i;
        for (i =0; i < 17; i++) {
            IConnectionObj.StreamStatus streamStatus = mConnectObj.getStreamStatus(streamIdArray[i]);
            String streamName = DevStreamUtils.getStreamName(streamIdArray[i]);

            if (streamStatus.mRecording) { // 停止录制
                mConnectObj.streamRecordStop(streamIdArray[i]);
            }

            if (streamStatus.mSubscribed) {  // 设置流显示控件为空
                mConnectObj.setVideoDisplayView(streamIdArray[i], null);
            }
        }

        finish();
    }


    /**
     * @brief 连接预览按钮点击事件
     */
    void onDevItemPreviewClick(View view, int position, DevStreamInfo devStream) {
        if (mConnectObj == null) {
            return;
        }
        IConnectionObj.StreamStatus streamStatus = mConnectObj.getStreamStatus(devStream.mStreamId);
        String streamName = DevStreamUtils.getStreamName(devStream.mStreamId);
        int errCode;

        if (!streamStatus.mSubscribed) {
            // 开始订阅音视频流
            errCode = mConnectObj.streamSubscribeStart(devStream.mStreamId, "xxxxx");
            if (errCode != ErrCode.XOK) {
                popupMessage("Subscribe stream: " +  streamName + " failure, errCode=" + errCode);
                return;
            }

            errCode = mConnectObj.setVideoDisplayView(devStream.mStreamId, devStream.mVideoView);

            // 更新流信息
            mStreamListAdapter.setItem(position, devStream);

        } else {
            // 停止订阅
            errCode = mConnectObj.streamSubscribeStop(devStream.mStreamId);
            if (errCode != ErrCode.XOK) {
                popupMessage("Unsubscribe stream: " +  streamName + " failure, errCode=" + errCode);
                return;
            }

            // 更新流信息
            mStreamListAdapter.setItem(position, devStream);
            popupMessage("Stop stream: " + streamName + "preview!");
        }
    }

    /**
     * @brief 静音 按钮点击事件
     */
    void onDevItemMuteAudioClick(View view, int position, DevStreamInfo devStream) {
        if (mConnectObj == null) {
            return;
        }
        IConnectionObj.StreamStatus streamStatus = mConnectObj.getStreamStatus(devStream.mStreamId);
        if (!streamStatus.mSubscribed) {
            popupMessage("The stream have NOT subscribed, please subscribe firstly");
            return;
        }
        String streamName = DevStreamUtils.getStreamName(devStream.mStreamId);
        int errCode;


        if (!streamStatus.mAudioMute) {
            // 音频禁音操作
            errCode = mConnectObj.muteAudioPlayback(devStream.mStreamId,  true);
            if (errCode != ErrCode.XOK) {
                popupMessage("Mute audio: " +  streamName + " failure, errCode=" + errCode);
                return;
            }

            // 更新流信息
            mStreamListAdapter.setItem(position, devStream);

        } else {
            // 音频音放操作
            errCode = mConnectObj.muteAudioPlayback(devStream.mStreamId, false);
            if (errCode != ErrCode.XOK) {
                popupMessage("Unmute audio: " +  streamName + " failure, errCode=" + errCode);
                return;
            }

            // 更新流信息
            mStreamListAdapter.setItem(position, devStream);
        }
    }

    /**
     * @brief 录像 按钮点击事件
     */
    void onDevItemRecordClick(View view, int position, DevStreamInfo devStream) {
        if (mConnectObj == null) {
            return;
        }
        IConnectionObj.StreamStatus streamStatus = mConnectObj.getStreamStatus(devStream.mStreamId);
        if (!streamStatus.mSubscribed) {
            popupMessage("The stream have NOT subscribed, please subscribe firstly");
            return;
        }
        String streamName = DevStreamUtils.getStreamName(devStream.mStreamId);
        int errCode;

        if (!mConnectObj.isStreamRecording(devStream.mStreamId)) {
            // 开始录制
            String strSavePath = FileUtils.getFileSavePath(mConnectObj.getInfo().mPeerNodeId, devStream.mStreamId,false);
            errCode = mConnectObj.streamRecordStart(devStream.mStreamId,  strSavePath);
            if (errCode != ErrCode.XOK) {
                popupMessage("Recording " +  streamName + " to file: " + strSavePath + " failure, errCode=" + errCode);
                return;
            }

            // 更新流信息
            mStreamListAdapter.setItem(position, devStream);
            popupMessage("Recording " +  streamName + " to file: " + strSavePath + " ......");

        } else {
            // 停止录制
            errCode = mConnectObj.streamRecordStop(devStream.mStreamId);
            if (errCode != ErrCode.XOK) {
                popupMessage("Stop recording: " +  streamName + " failure, errCode=" + errCode);
                return;
            }

            // 更新流信息
            mStreamListAdapter.setItem(position, devStream);
            popupMessage("Stop recording: " + streamName + "successful!");
        }
    }


    /**
     * @brief 截屏 按钮点击事件
     */
    void onDevItemShotCaptureClick(View view, int position, DevStreamInfo devStream) {
        if (mConnectObj == null) {
            return;
        }
        IConnectionObj.StreamStatus streamStatus = mConnectObj.getStreamStatus(devStream.mStreamId);
        if (!streamStatus.mSubscribed) {
            popupMessage("The stream have NOT subscribed, please subscribe firstly");
            return;
        }
        String streamName = DevStreamUtils.getStreamName(devStream.mStreamId);
        int errCode;

        String strSavePath = FileUtils.getFileSavePath(mConnectObj.getInfo().mPeerNodeId, devStream.mStreamId, true);
        errCode = mConnectObj.streamVideoFrameShot(devStream.mStreamId, strSavePath);
        if (errCode != ErrCode.XOK) {
            popupMessage("Stream: " + streamName + " shot capture failure, errCode=" + errCode);
            return;
        }
    }



    //////////////////////////////////////////////////////////////////////////////////
    //////////////// Override Methods of IConnectionMgr.ICallback  ///////////////////
    ///////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onPeerDisconnected(final IConnectionObj connectObj, int errCode) {
        String peerNodeId = connectObj.getInfo().mPeerNodeId;
        Log.d(TAG, "<onPeerDisconnected> [IOTSDK/] connectObj=" + connectObj
                + ", peerNodeId=" + peerNodeId + ", errCode=" + errCode);
        if (mDeviceNodeId.compareTo(peerNodeId) != 0) {  // 不是当前设备
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PushApplication.getInstance().setFullscrnConnectionObj(null);
                onBtnBack(true);
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
        Log.d(TAG, "<onStreamVideoFrameShotDone> [IOTSDK/] connectObj=" + connectObj
                + ", subStreamId=" + DevStreamUtils.getStreamName(subStreamId)
                + ", errCode=" + errCode + ", filePath=" + filePath);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String streamName = DevStreamUtils.getStreamName(subStreamId);
                if (errCode != ErrCode.XOK) {
                    popupMessage("Stream: " + streamName + " frame shot failure, errCode=" + errCode);
                    return;
                }

                popupMessage("Stream: " + streamName + " frame shot successful, save to file: " + filePath);
            }
        });
    }

    @Override
    public void onStreamError(final IConnectionObj connectObj, final IConnectionObj.STREAM_ID subStreamId,
                              int errCode) {
        Log.d(TAG, "<onStreamError> [IOTSDK/] connectObj=" + connectObj
                + ", subStreamId=" + DevStreamUtils.getStreamName(subStreamId)
                + ", errCode=" + errCode);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String streamName = DevStreamUtils.getStreamName(subStreamId);
                popupMessage("Stream: " + streamName + " failure, errCode=" + errCode);
            }
        });

    }

    @Override
    public void onMessageSendDone(final IConnectionObj connectObj, int errCode,
                                  UUID messageId, final byte[] messageData) {
    }

    @Override
    public void onMessageReceived(final IConnectionObj connectObj, final byte[] recvedMsgData) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                popupMessage("Recv message: " + recvedMsgData + " from device!");
            }
        });
    }

}
