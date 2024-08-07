package io.agora.falcondemo.models.player;

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

import com.agora.baselibrary.utils.ScreenUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import io.agora.falcondemo.common.Constant;
import io.agora.falcondemo.models.home.DeviceInfo;
import io.agora.falcondemo.models.home.DeviceListAdapter;
import io.agora.falcondemo.utils.DevStreamUtils;
import io.agora.falcondemo.utils.FileTransferMgr;
import io.agora.falcondemo.utils.FileUtils;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.falcondemo.R;
import io.agora.falcondemo.base.BaseViewBindingActivity;
import io.agora.falcondemo.base.PushApplication;
import io.agora.falcondemo.databinding.ActivityDevPreviewBinding;
import io.agora.iotlink.IConnectionMgr;
import io.agora.iotlink.IConnectionObj;
import io.agora.iotlink.logger.ALog;


public class DevPreviewActivity extends BaseViewBindingActivity<ActivityDevPreviewBinding>
    implements IConnectionMgr.ICallback, IConnectionObj.ICallback, FileTransferMgr.ICallback {
    private static final String TAG = "IOTLINK/DevPrevAct";


    private IConnectionObj mConnectObj = null;
    private String mDeviceNodeId;

    private PopupWindow mCtrlPnlWnd = null;
    private View mCtrlPnlView = null;
    private Button mBtnDefault = null;
    private Button mBtnSr100 = null;
    private Button mBtnSr133 = null;
    private Button mBtnSr150 = null;
    private Button mBtnSr200 = null;
    private Button mBtnSi = null;
    private SeekBar mSbSiDegree = null;
    private TextView mTvSiDegree = null;

    private TransStatusListAdapter mStatusListAdapter;


    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseActivity /////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected ActivityDevPreviewBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDevPreviewBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        mConnectObj = PushApplication.getInstance().getFullscrnConnectionObj();
        IConnectionObj.ConnectionInfo connectInfo = mConnectObj.getInfo();
        mDeviceNodeId = connectInfo.mPeerNodeId;

        mConnectObj.setVideoDisplayView(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1, getBinding().svDeviceView);
        getBinding().tvNodeId.setText(connectInfo.mPeerNodeId);


        getBinding().btnPanel.setOnClickListener(view -> {
            onBtnPannel(view);
        });

        getBinding().btnTransfer.setOnClickListener(view -> {
            onBtnTransfer(view);
        });

        getBinding().btnClearStatus.setOnClickListener(view -> {
            // 清除旧的信息
            if (mStatusListAdapter != null) {
                mStatusListAdapter.clear();
            }
        });


        List<FileTransStatus> statusList = new ArrayList<>();

        if (mStatusListAdapter == null) {
            mStatusListAdapter = new TransStatusListAdapter(statusList);
            mStatusListAdapter.setOwner(this);
            getBinding().rvTransStatusList.setLayoutManager(new LinearLayoutManager(this));
            getBinding().rvTransStatusList.setAdapter(mStatusListAdapter);
            getBinding().rvTransStatusList.setItemViewCacheSize(1024);
            mStatusListAdapter.setMRVItemClickListener((view, position, data) -> {
            });
        }

        if (mConnectObj.isFileTransfering()) {
            getBinding().btnTransfer.setText(this.getString(R.string.end_transfer));
        } else {
            getBinding().btnTransfer.setText(this.getString(R.string.start_transfer));
        }




        Log.d(TAG, "<initView> ");
    }

    @Override
    protected boolean isCanExit() {
        return false;
    }

    @Override
    public void initListener() {
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

        FileTransferMgr.getInstance().registerListener(this);
        Log.d(TAG, "<onStart> mConnectObj=" + mConnectObj);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "<onStop> ");

        FileTransferMgr.getInstance().unregisterListener(this);

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

        if (!isConnectExist()) {
            ALog.getInstance().d(TAG, "<onResume> device disconnected!");
            PushApplication.getInstance().setUiPage(Constant.UI_PAGE_HOME); // 切换回主界面
            PushApplication.getInstance().setFullscrnConnectionObj(null);
            finish();
        }

    }

    /**
     * @brief 判断当前连接是否还存在
     */
    boolean isConnectExist() {
        if (mConnectObj == null) {
            return false;
        }
        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
        List<IConnectionObj> connectList = connectMgr.getConnectionList();
        for (int i = 0; i < connectList.size(); i++) {
            IConnectionObj connectObj = connectList.get(i);
            if (connectObj == mConnectObj) {
                return true;
            }
        }
        return false;
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
            PushApplication.getInstance().setUiPage(Constant.UI_PAGE_HOME); // 切回主界面
        }
        return super.onKeyDown(keyCode, event);
    }


    ///////////////////////////////////////////////////////////////////////////
    /////////////////////////// 视频超分处理的方法 ////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    void onBtnPannel(View parentView) {
//
//        if ((mCtrlPnlView == null) || (mCtrlPnlWnd == null)) {
//            mCtrlPnlView = LayoutInflater.from(this).inflate(R.layout.layout_fullscrn_ctrl, null);
//            mBtnDefault = (Button)mCtrlPnlView.findViewById(R.id.btnDefault);
//            mBtnSr100 = (Button)mCtrlPnlView.findViewById(R.id.btnSr100);
//            mBtnSr133 = (Button)mCtrlPnlView.findViewById(R.id.btnSr133);
//            mBtnSr150 = (Button)mCtrlPnlView.findViewById(R.id.btnSr150);
//            mBtnSr200 = (Button)mCtrlPnlView.findViewById(R.id.btnSr200);
//            mBtnSi = (Button)mCtrlPnlView.findViewById(R.id.btnSi);
//            mSbSiDegree = (SeekBar)mCtrlPnlView.findViewById(R.id.sbSiDegree);
//            mTvSiDegree = (TextView)mCtrlPnlView.findViewById(R.id.tvSiDegree);
//
//            mBtnDefault.setOnClickListener(view -> {
//                onBtnDefault(view);
//            });
//
//            mBtnSr100.setOnClickListener(view -> {
//                onBtnSr100(view);
//            });
//
//            mBtnSr133.setOnClickListener(view -> {
//                onBtnSr133(view);
//            });
//
//            mBtnSr150.setOnClickListener(view -> {
//                onBtnSr150(view);
//            });
//
//            mBtnSr200.setOnClickListener(view -> {
//                onBtnSr200(view);
//            });
//
//            mBtnSi.setOnClickListener(view -> {
//                onBtnSi(view);
//            });
//
//            mTvSiDegree.setText("画质深度: 256");
//
//            mSbSiDegree.setMax(256);
//            mSbSiDegree.setProgress(mVideoQuality.mSiDegree);
//            mSbSiDegree.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//                @Override
//                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
//                }
//
//                @Override
//                public void onStartTrackingTouch(SeekBar seekBar) {
//                }
//
//                @Override
//                public void onStopTrackingTouch(SeekBar seekBar) {
//                    onSiDegreeChanged(seekBar.getProgress(), seekBar.getMax());
//                }
//            });
//
//
//            mCtrlPnlWnd = new PopupWindow(this);
//            mCtrlPnlWnd.setContentView(mCtrlPnlView);
//            mCtrlPnlWnd.setWidth(ScreenUtils.dp2px(460));
//            mCtrlPnlWnd.setHeight(ScreenUtils.dp2px(240));
//        }
//
//        mCtrlPnlWnd.setFocusable(true);
//        mCtrlPnlWnd.setOutsideTouchable(true);
//        mCtrlPnlWnd.showAtLocation(mCtrlPnlView, Gravity.LEFT|Gravity.BOTTOM, 10, 10);
    }

    void onBtnDefault(View view) {
//        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
//        IConnectionObj connectObj = connectMgr.getConnectionObj(mConnectionId);
//        if (connectObj == null) {
//            return;
//        }
//
//        mVideoQuality.mQualityType = IConnectionObj.VIDEOQUALITY_TYPE_DEFAULT;
//        int errCode = connectObj.setPreviewVideoQuality(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1, mVideoQuality);
//        if (errCode == ErrCode.XOK) {
//            popupMessage("Set video quality to defalut successful!");
//        } else {
//            popupMessage("Set video quality to defalut failure, errCode=" + errCode);
//        }
    }

    void onBtnSr100(View view) {
//        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
//        IConnectionObj connectObj = connectMgr.getConnectionObj(mConnectionId);
//        if (connectObj == null) {
//            return;
//        }
//
//        mVideoQuality.mQualityType = IConnectionObj.VIDEOQUALITY_TYPE_SR;
//        mVideoQuality.mSiDegree = IConnectionObj.SR_DEGREE_100;
//        int errCode = connectObj.setPreviewVideoQuality(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1, mVideoQuality);
//        if (errCode == ErrCode.XOK) {
//            popupMessage("Set video quality to SR_1X successful!");
//        } else {
//            popupMessage("Set video quality to SR_1X failure, errCode=" + errCode);
//        }
    }

    void onBtnSr133(View view) {
//        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
//        IConnectionObj connectObj = connectMgr.getConnectionObj(mConnectionId);
//        if (connectObj == null) {
//            return;
//        }
//
//        mVideoQuality.mQualityType = IConnectionObj.VIDEOQUALITY_TYPE_SR;
//        mVideoQuality.mSiDegree = IConnectionObj.SR_DEGREE_133;
//        int errCode = connectObj.setPreviewVideoQuality(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1, mVideoQuality);
//        if (errCode == ErrCode.XOK) {
//            popupMessage("Set video quality to SR_1.33X successful!");
//        } else {
//            popupMessage("Set video quality to SR_1.33X failure, errCode=" + errCode);
//        }
    }

    void onBtnSr150(View view) {
//        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
//        IConnectionObj connectObj = connectMgr.getConnectionObj(mConnectionId);
//        if (connectObj == null) {
//            return;
//        }
//
//        mVideoQuality.mQualityType = IConnectionObj.VIDEOQUALITY_TYPE_SR;
//        mVideoQuality.mSiDegree = IConnectionObj.SR_DEGREE_150;
//        int errCode = connectObj.setPreviewVideoQuality(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1, mVideoQuality);
//        if (errCode == ErrCode.XOK) {
//            popupMessage("Set video quality to SR_1.5X successful!");
//        } else {
//            popupMessage("Set video quality to SR_1.5X failure, errCode=" + errCode);
//        }
    }

    void onBtnSr200(View view) {
//        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
//        IConnectionObj connectObj = connectMgr.getConnectionObj(mConnectionId);
//        if (connectObj == null) {
//            return;
//        }
//
//        mVideoQuality.mQualityType = IConnectionObj.VIDEOQUALITY_TYPE_SR;
//        mVideoQuality.mSiDegree = IConnectionObj.SR_DEGREE_200;
//        int errCode = connectObj.setPreviewVideoQuality(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1, mVideoQuality);
//        if (errCode == ErrCode.XOK) {
//            popupMessage("Set video quality to SR_2X successful!");
//        } else {
//            popupMessage("Set video quality to SR_2X failure, errCode=" + errCode);
//        }
    }

    void onBtnSi(View view) {
//        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
//        IConnectionObj connectObj = connectMgr.getConnectionObj(mConnectionId);
//        if (connectObj == null) {
//            return;
//        }
//
//        mVideoQuality.mQualityType = IConnectionObj.VIDEOQUALITY_TYPE_SI;
//        mVideoQuality.mSiDegree = mSbSiDegree.getProgress();
//        int errCode = connectObj.setPreviewVideoQuality(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1, mVideoQuality);
//        if (errCode == ErrCode.XOK) {
//            popupMessage("Set video quality to SI " + mVideoQuality.mSiDegree + " successful!");
//        } else {
//            popupMessage("Set video quality to SI " + mVideoQuality.mSiDegree + " failure, errCode=" + errCode);
//        }
    }

    void onSiDegreeChanged(int sbProgress, int sbMax) {
//        Log.d(TAG, "<onSiDegreeChanged> sbProgress=" + sbProgress + ", sbMax=" + sbMax);
//        mTvSiDegree.setText("画质深度: " + sbProgress);
//
//        IConnectionMgr connectMgr = AIotAppSdkFactory.getInstance().getConnectionMgr();
//        IConnectionObj connectObj = connectMgr.getConnectionObj(mConnectionId);
//        if (connectObj == null) {
//            return;
//        }
//
//        mVideoQuality.mQualityType = IConnectionObj.VIDEOQUALITY_TYPE_SI;
//        mVideoQuality.mSiDegree = mSbSiDegree.getProgress();
//        int errCode = connectObj.setPreviewVideoQuality(IConnectionObj.STREAM_ID.BROADCAST_STREAM_1, mVideoQuality);
//        if (errCode == ErrCode.XOK) {
//            popupMessage("Set video quality to SI " + mVideoQuality.mSiDegree + " successful!");
//        } else {
//            popupMessage("Set video quality to SI " + mVideoQuality.mSiDegree + " failure, errCode=" + errCode);
//        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////////////////////////// 文件传输处理的方法 ////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    void onBtnTransfer(View parentView) {
        if (mConnectObj.isFileTransfering()) {
            // mConnectObj.fileTransferStop();
            FileTransferMgr.getInstance().transferStop(mConnectObj);
            popupMessage("File transfering stopped!");
            getBinding().btnTransfer.setText(this.getString(R.string.start_transfer));

            FileTransStatus newStatus = new FileTransStatus();
            newStatus.mType = FileTransStatus.TYPE_STOP;
            newStatus.mInfo = "";
            newStatus.mTimestamp = getTimestamp();
            mStatusListAdapter.addNewItem(newStatus);

        } else {
            String startMessage = "file1;file2;file3;";
            //int errCode = mConnectObj.fileTransferStart(startMessage);
            int errCode = FileTransferMgr.getInstance().transferStart(mConnectObj, startMessage);
            if (errCode != ErrCode.XOK) {
                popupMessage("Start file transfering failure, it is ongoing!");
                return;
            }
            popupMessage("File transfering started...");
            getBinding().btnTransfer.setText(this.getString(R.string.end_transfer));

            FileTransStatus newStatus = new FileTransStatus();
            newStatus.mType = FileTransStatus.TYPE_START;
            newStatus.mInfo = startMessage;
            newStatus.mTimestamp = getTimestamp();
            mStatusListAdapter.addNewItem(newStatus);
        }
    }


    String getTimestamp() {
        String time_txt = "";
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int date = calendar.get(Calendar.DATE);
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int ms = calendar.get(Calendar.MILLISECOND);

        time_txt = String.format(Locale.getDefault(), "%d-%02d-%02d %02d:%02d:%02d.%d",
                year, month, date, hour,minute, second, ms);
        return time_txt;
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////// Override Methods of IConnectionMgr.ICallback  ///////////////////
    ///////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onPeerDisconnected(final IConnectionObj connectObj, int errCode) {
        String peerDevNodeId = connectObj.getInfo().mPeerNodeId;
        ALog.getInstance().d(TAG, "<onPeerDisconnected> [IOTSDK/] connectObj=" + connectObj
                + ", peerDevNodeId=" + peerDevNodeId + ", errCode=" + errCode);
        if (mDeviceNodeId.compareTo(peerDevNodeId) != 0) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ALog.getInstance().d(TAG, "<onPeerDisconnected> device disconnected!");
                PushApplication.getInstance().setUiPage(Constant.UI_PAGE_HOME); // 切换回主界面
                PushApplication.getInstance().setFullscrnConnectionObj(null);
                finish();
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////////////
    //////////////// Override Methods of IConnectionObj.ICallback  ////////////////////
    ///////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onStreamError(final IConnectionObj connectObj, final IConnectionObj.STREAM_ID subStreamId,
                              int errCode) {
        if (connectObj != mConnectObj) {  // 不是当前链接的
            return;
        }
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

    ///////////////////////////////////////////////////////////////////////////////////
    //////////////// Override Methods of FileTransferMgr.ICallback  ////////////////////
    ///////////////////////////////////////////////////////////////////////////////////
    public void onTransMgrRecvStart(final IConnectionObj connectObj, final byte[] startDescrption) {
        if (connectObj != mConnectObj) {  // 不是当前链接的
            return;
        }
        String peerNodeId = connectObj.getInfo().mPeerNodeId;
        Log.d(TAG, "<onTransMgrRecvStart> [IOTSDK/] peerNodeId=" + peerNodeId
                + ", startDescrption=" + new String(startDescrption));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FileTransStatus newStatus = new FileTransStatus();
                newStatus.mType = FileTransStatus.TYPE_FILE_BEGIN;
                newStatus.mInfo = new String(startDescrption);
                newStatus.mTimestamp = getTimestamp();
                mStatusListAdapter.addNewItem(newStatus);
            }
        });
    }

    public void onTransMgrRecvData(final IConnectionObj connectObj, final byte[] recvedData) {
        if (connectObj != mConnectObj) {  // 不是当前链接的
            return;
        }
        String peerNodeId = connectObj.getInfo().mPeerNodeId;
        Log.d(TAG, "<onTransMgrRecvData> [IOTSDK/] peerNodeId=" + peerNodeId
                + ", recvedData.length=" + recvedData.length);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                FileTransStatus newStatus = new FileTransStatus();
//                newStatus.mType = FileTransStatus.TYPE_FILE_DATA;
//                newStatus.mDataSize = recvedData.length;
//                newStatus.mTimestamp = getTimestamp();
//                mStatusListAdapter.addNewItem(newStatus);
            }
        });
    }

    public void onTransMgrRecvDone(final IConnectionObj connectObj, boolean transferEnd,
                                    final byte[] doneDescrption, final byte[] md5Value) {
        if (connectObj != mConnectObj) {  // 不是当前链接的
            return;
        }
        String peerNodeId = connectObj.getInfo().mPeerNodeId;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 计算MD5值
                String doneInfo = new String(doneDescrption);
                String md5Text = FileUtils.bytesToHexString(md5Value).toLowerCase();
                boolean recvSuccess = true;
                int subStrIdx = doneInfo.indexOf(md5Text);
                if (subStrIdx < 0) {
                    recvSuccess = false;
                }

                FileTransStatus newStatus = new FileTransStatus();
                newStatus.mType = FileTransStatus.TYPE_FILE_END;
                newStatus.mInfo = doneInfo;
                newStatus.mTimestamp = getTimestamp();
                newStatus.mEOF = transferEnd;
                newStatus.mMd5Text = md5Text;
                newStatus.mRecvSuccess = recvSuccess;
                mStatusListAdapter.addNewItem(newStatus);

                String recvText = (recvSuccess) ? "[RECV_FILE_SUCCESS]" : "[RECV_FILE_FAILURE]";
                Log.d(TAG, "<onTransMgrRecvDone> [IOTSDK/] peerNodeId=" + peerNodeId
                        + ", doneDescrption=" + new String(doneDescrption)
                        + ", md5=" + newStatus.mMd5Text + ", " + recvText);

                if (transferEnd) {  // 整个传输完成了
                    popupMessage("File transfering is done!");
                    getBinding().btnTransfer.setText("开始传输");
                }
            }
        });
    }
}
