package io.agora.iotlinkdemo.models.player.living;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IRtcPlayer;
import io.agora.iotlinkdemo.base.BaseViewBindingFragment;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.FagmentPlayerRtcBinding;
import io.agora.iotlinkdemo.databinding.FagmentPlayerRtmBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.models.player.RtmViewModel;
import com.alibaba.android.arouter.facade.annotation.Route;
import java.io.UnsupportedEncodingException;


/**
 * @brief RTC频道播放页面
 */
@Route(path = PagePathConstant.pagePlayerRtc)
public class PlayerRtcFragment extends BaseViewBindingFragment<FagmentPlayerRtcBinding>
    implements IRtcPlayer.ICallback {

    private final String TAG = "IOTLINK/PlayRtcFrag";

    //
    // message Id
    //
    public static final int MSGID_RTM_TIMER_SEND = 0x1002;  ///< 定时发送RTM消息


    private Handler mMsgHandler = null;             ///< 主线程中的消息处理





    @NonNull
    @Override
    protected FagmentPlayerRtcBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FagmentPlayerRtcBinding.inflate(inflater);
    }

    @Override
    public void initView() {
        Log.d(TAG, "<initView>");
        super.initView();
        getBinding().btnRtcPlay.setOnClickListener(view -> onBtnPlay());


        mMsgHandler = new Handler(getActivity().getMainLooper())
        {
            @SuppressLint("HandlerLeak")
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case MSGID_RTM_TIMER_SEND:
                        break;
                    default:
                        break;
                }
            }
        };

        // 不管如何，当前呼叫要先挂断
        AIotAppSdkFactory.getInstance().getCallkitMgr().callHangup();
    }

    @Override
    public void initListener() {
        Log.d(TAG, "<initListener>");

    }


    @Override
    public void onDestroyView() {
        Log.d(TAG, "<onDestroyView>");
        super.onDestroyView();

        if (mMsgHandler != null) {
            mMsgHandler.removeMessages(MSGID_RTM_TIMER_SEND);
            mMsgHandler = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "<onStart>");

        AIotAppSdkFactory.getInstance().getRtcPlayer().registerListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "<onStop>");

        AIotAppSdkFactory.getInstance().getRtcPlayer().unregisterListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "<onPause>");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "<onResume>");
    }


    /**
     * @brief 播放和停止控制按钮
     */
    void onBtnPlay() {
        IRtcPlayer rtcPlayer = AIotAppSdkFactory.getInstance().getRtcPlayer();
        int playingState = rtcPlayer.getStateMachine();

        if (playingState == IRtcPlayer.RTCPLAYER_STATE_STOPPED) {  // 当前处于停止状态
            String channelName = getBinding().etRtcChannelName.getText().toString();
            if (TextUtils.isEmpty(channelName)) {
                popupMessage("请输入要播放的频道");
                return;
            }

            // 加入频道启动播放
            int errCode = rtcPlayer.start(channelName, getBinding().playerView);
            if (errCode != ErrCode.XOK) {
                popupMessage("播放频道失败，错误码=" + errCode);
                return;
            }

            getBinding().btnRtcPlay.setText("停止");


        } else {  // 当前处于播放状态

            // 加入频道启动播放
            int errCode = rtcPlayer.stop();
            if (errCode != ErrCode.XOK) {
                popupMessage("停止播放失败，错误码=" + errCode);
                return;
            }

            getBinding().btnRtcPlay.setText("开始");
        }
    }

    public boolean onBtnBack() {
        return false;
    }



}
