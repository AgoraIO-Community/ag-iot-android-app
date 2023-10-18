package io.agora.falcondemo.models.player;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.agora.baselibrary.utils.ScreenUtils;

import java.util.UUID;

import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.ICallkitMgr;
import io.agora.falcondemo.R;
import io.agora.falcondemo.base.BaseViewBindingActivity;
import io.agora.falcondemo.base.PushApplication;
import io.agora.falcondemo.databinding.ActivityDevPreviewBinding;
import io.agora.falcondemo.databinding.ActivityMainBinding;


public class DevPreviewActivity extends BaseViewBindingActivity<ActivityDevPreviewBinding>
    implements ICallkitMgr.ICallback {
    private static final String TAG = "IOTLINK/DevPrevAct";


    private UUID mSessionId = null;
    private ICallkitMgr.VideoQualityParam mVideoQuality = null;

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

    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseActivity /////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected ActivityDevPreviewBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDevPreviewBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        mSessionId = PushApplication.getInstance().getFullscrnSessionId();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        callkitMgr.setPeerVideoView(mSessionId, getBinding().svDeviceView);

        ICallkitMgr.SessionInfo sessionInfo = callkitMgr.getSessionInfo(mSessionId);
        getBinding().tvNodeId.setText(sessionInfo.mPeerNodeId);
        mVideoQuality = sessionInfo.mVideoQuality;

        getBinding().btnPanel.setOnClickListener(view -> {
            onBtnPannel(view);
        });


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

        mSessionId = PushApplication.getInstance().getFullscrnSessionId();
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        callkitMgr.registerListener(this);

        Log.d(TAG, "<onStart> mSessionId=" + mSessionId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "<onStop> ");

        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        callkitMgr.unregisterListener(this);
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
        mSessionId = null;
    }



    ///////////////////////////////////////////////////////////////////////////
    //////////////// Events and Message Handle Methods ///////////////////////
    ///////////////////////////////////////////////////////////////////////////
    void onBtnPannel(View parentView) {

        if ((mCtrlPnlView == null) || (mCtrlPnlWnd == null)) {
            mCtrlPnlView = LayoutInflater.from(this).inflate(R.layout.layout_fullscrn_ctrl, null);
            mBtnDefault = (Button)mCtrlPnlView.findViewById(R.id.btnDefault);
            mBtnSr100 = (Button)mCtrlPnlView.findViewById(R.id.btnSr100);
            mBtnSr133 = (Button)mCtrlPnlView.findViewById(R.id.btnSr133);
            mBtnSr150 = (Button)mCtrlPnlView.findViewById(R.id.btnSr150);
            mBtnSr200 = (Button)mCtrlPnlView.findViewById(R.id.btnSr200);
            mBtnSi = (Button)mCtrlPnlView.findViewById(R.id.btnSi);
            mSbSiDegree = (SeekBar)mCtrlPnlView.findViewById(R.id.sbSiDegree);
            mTvSiDegree = (TextView)mCtrlPnlView.findViewById(R.id.tvSiDegree);

            mBtnDefault.setOnClickListener(view -> {
                onBtnDefault(view);
            });

            mBtnSr100.setOnClickListener(view -> {
                onBtnSr100(view);
            });

            mBtnSr133.setOnClickListener(view -> {
                onBtnSr133(view);
            });

            mBtnSr150.setOnClickListener(view -> {
                onBtnSr150(view);
            });

            mBtnSr200.setOnClickListener(view -> {
                onBtnSr200(view);
            });

            mBtnSi.setOnClickListener(view -> {
                onBtnSi(view);
            });

            mTvSiDegree.setText("画质深度: 256");

            mSbSiDegree.setMax(256);
            mSbSiDegree.setProgress(mVideoQuality.mSiDegree);
            mSbSiDegree.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    onSiDegreeChanged(seekBar.getProgress(), seekBar.getMax());
                }
            });


            mCtrlPnlWnd = new PopupWindow(this);
            mCtrlPnlWnd.setContentView(mCtrlPnlView);
            mCtrlPnlWnd.setWidth(ScreenUtils.dp2px(460));
            mCtrlPnlWnd.setHeight(ScreenUtils.dp2px(240));
        }

        mCtrlPnlWnd.setFocusable(true);
        mCtrlPnlWnd.setOutsideTouchable(true);
        mCtrlPnlWnd.showAtLocation(mCtrlPnlView, Gravity.LEFT|Gravity.BOTTOM, 10, 10);
    }

    void onBtnDefault(View view) {
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        mVideoQuality.mQualityType = ICallkitMgr.VIDEOQUALITY_TYPE_DEFAULT;
        int errCode = callkitMgr.setPeerVideoQuality(mSessionId, mVideoQuality);
        if (errCode == ErrCode.XOK) {
            popupMessage("Set video quality to defalut successful!");
        } else {
            popupMessage("Set video quality to defalut failure, errCode=" + errCode);
        }
    }

    void onBtnSr100(View view) {
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        mVideoQuality.mQualityType = ICallkitMgr.VIDEOQUALITY_TYPE_SR;
        mVideoQuality.mSiDegree = ICallkitMgr.SR_DEGREE_100;
        int errCode = callkitMgr.setPeerVideoQuality(mSessionId, mVideoQuality);
        if (errCode == ErrCode.XOK) {
            popupMessage("Set video quality to SR_1X successful!");
        } else {
            popupMessage("Set video quality to SR_1X failure, errCode=" + errCode);
        }
    }
    void onBtnSr133(View view) {
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        mVideoQuality.mQualityType = ICallkitMgr.VIDEOQUALITY_TYPE_SR;
        mVideoQuality.mSiDegree = ICallkitMgr.SR_DEGREE_133;
        int errCode = callkitMgr.setPeerVideoQuality(mSessionId, mVideoQuality);
        if (errCode == ErrCode.XOK) {
            popupMessage("Set video quality to SR_1.33X successful!");
        } else {
            popupMessage("Set video quality to SR_1.33X failure, errCode=" + errCode);
        }
    }
    void onBtnSr150(View view) {
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        mVideoQuality.mQualityType = ICallkitMgr.VIDEOQUALITY_TYPE_SR;
        mVideoQuality.mSiDegree = ICallkitMgr.SR_DEGREE_150;
        int errCode = callkitMgr.setPeerVideoQuality(mSessionId, mVideoQuality);
        if (errCode == ErrCode.XOK) {
            popupMessage("Set video quality to SR_1.5X successful!");
        } else {
            popupMessage("Set video quality to SR_1.5X failure, errCode=" + errCode);
        }
    }

    void onBtnSr200(View view) {
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        mVideoQuality.mQualityType = ICallkitMgr.VIDEOQUALITY_TYPE_SR;
        mVideoQuality.mSiDegree = ICallkitMgr.SR_DEGREE_200;
        int errCode = callkitMgr.setPeerVideoQuality(mSessionId, mVideoQuality);
        if (errCode == ErrCode.XOK) {
            popupMessage("Set video quality to SR_2X successful!");
        } else {
            popupMessage("Set video quality to SR_2X failure, errCode=" + errCode);
        }
    }

    void onBtnSi(View view) {
        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        mVideoQuality.mQualityType = ICallkitMgr.VIDEOQUALITY_TYPE_SI;
        mVideoQuality.mSiDegree = mSbSiDegree.getProgress();
        int errCode = callkitMgr.setPeerVideoQuality(mSessionId, mVideoQuality);
        if (errCode == ErrCode.XOK) {
            popupMessage("Set video quality to SI " + mVideoQuality.mSiDegree + " successful!");
        } else {
            popupMessage("Set video quality to SI " + mVideoQuality.mSiDegree + " failure, errCode=" + errCode);
        }
    }

    void onSiDegreeChanged(int sbProgress, int sbMax) {
        Log.d(TAG, "<onSiDegreeChanged> sbProgress=" + sbProgress + ", sbMax=" + sbMax);

        mTvSiDegree.setText("画质深度: " + sbProgress);

        ICallkitMgr callkitMgr = AIotAppSdkFactory.getInstance().getCallkitMgr();
        mVideoQuality.mQualityType = ICallkitMgr.VIDEOQUALITY_TYPE_SI;
        mVideoQuality.mSiDegree = mSbSiDegree.getProgress();
        int errCode = callkitMgr.setPeerVideoQuality(mSessionId, mVideoQuality);
        if (errCode == ErrCode.XOK) {
            popupMessage("Set video quality to SI " + mVideoQuality.mSiDegree + " successful!");
        } else {
            popupMessage("Set video quality to SI " + mVideoQuality.mSiDegree + " failure, errCode=" + errCode);
        }
    }



    ///////////////////////////////////////////////////////////////////////////
    //////////////// Override Methods of ICallkitMgr.ICallback  ///////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onPeerIncoming(final UUID sessionId, final String peerNodeId,
                               final String attachMsg) {
        Log.d(TAG, "<onPeerIncoming> [IOTSDK/] sessionId=" + sessionId
                + ", peerNodeId=" + peerNodeId + ", attachMsg=" + attachMsg);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void onDialDone(final UUID sessionId, final String peerNodeId, int errCode) {
        Log.d(TAG, "<onDialDone> [IOTSDK/] sessionId=" + sessionId
                + ", peerNodeId=" + peerNodeId + ", errCode=" + errCode);
        if (sessionId.compareTo(mSessionId) != 0) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void onPeerAnswer(final UUID sessionId, final String peerNodeId) {
        Log.d(TAG, "<onPeerAnswer> [IOTSDK/] sessionId=" + sessionId
                + ", peerNodeId=" + peerNodeId);
        if (sessionId.compareTo(mSessionId) != 0) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void onPeerHangup(final UUID sessionId, final String peerNodeId) {
        Log.d(TAG, "<onPeerHangup> [IOTSDK/] sessionId=" + sessionId
                + ", peerNodeId=" + peerNodeId);
        if (sessionId.compareTo(mSessionId) != 0) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    @Override
    public void onPeerTimeout(final UUID sessionId, final String peerNodeId) {
        Log.d(TAG, "<onPeerTimeout> [IOTSDK/] sessionId=" + sessionId
                + ", peerNodeId=" + peerNodeId);
        if (sessionId.compareTo(mSessionId) != 0) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    @Override
    public void onPeerFirstVideo(final UUID sessionId, int videoWidth, int videoHeight) {
        Log.d(TAG, "<onPeerFirstVideo> [IOTSDK/] sessionId=" + sessionId
                + ", videoWidth=" + videoWidth + ", videoHeight=" + videoHeight);
        if (sessionId.compareTo(mSessionId) != 0) {
            return;
        }
    }

    @Override
    public void onOtherUserOnline(final UUID sessionId, int uid, int onlineUserCount) {
        Log.d(TAG, "<onOtherUserOnline> [IOTSDK/] sessionId=" + sessionId
                + ", onlineUserCount=" + onlineUserCount);
        if (sessionId.compareTo(mSessionId) != 0) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void onOtherUserOffline(final UUID sessionId, int uid, int onlineUserCount) {
        Log.d(TAG, "<onOtherUserOffline> [IOTSDK/] sessionId=" + sessionId
                + ", onlineUserCount=" + onlineUserCount);
        if (sessionId.compareTo(mSessionId) != 0) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void onSessionError(final UUID sessionId, int errCode) {
        Log.d(TAG, "<onSessionError> [IOTSDK/] sessionId=" + sessionId
                + ", errCode=" + errCode);
        if (sessionId.compareTo(mSessionId) != 0) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    @Override
    public void onRecordingError(final UUID sessionId, int errCode) {
        Log.d(TAG, "<onRecordingError> [IOTSDK/] sessionId=" + sessionId
                + ", errCode=" + errCode);
        if (sessionId.compareTo(mSessionId) != 0) {
            return;
        }
    }
}
