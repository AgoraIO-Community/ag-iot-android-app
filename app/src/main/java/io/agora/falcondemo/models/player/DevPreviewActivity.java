package io.agora.falcondemo.models.player;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

import io.agora.iotlink.AIotAppSdkFactory;
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
