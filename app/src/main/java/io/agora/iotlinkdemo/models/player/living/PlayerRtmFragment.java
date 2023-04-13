package io.agora.iotlinkdemo.models.player.living;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IRtmMgr;
import io.agora.iotlink.IotAlarm;
import io.agora.iotlink.IotAlarmPage;
import io.agora.iotlinkdemo.base.BaseViewBindingFragment;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.FagmentPlayerRtmBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.models.message.MessageViewModel;
import io.agora.iotlinkdemo.models.player.BaseGsyPlayerFragment;
import io.agora.iotlinkdemo.models.player.RtmViewModel;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import java.io.UnsupportedEncodingException;


/**
 * 播放页功能列表
 */
@Route(path = PagePathConstant.pagePlayerRtm)
public class PlayerRtmFragment extends BaseViewBindingFragment<FagmentPlayerRtmBinding> {
    private final String TAG = "IOTLINK/PlayRtmFrag";

    //
    // message Id
    //
    public static final int MSGID_RTM_TIMER_SEND = 0x1002;  ///< 定时发送RTM消息


    private Handler mMsgHandler = null;             ///< 主线程中的消息处理
    private RtmViewModel mRtmViewModel;
    private volatile boolean mContinueRtmSend = false;  ///< 是否连续性的RTM发送
    private volatile long mRtmSendCount = 0;        ///< 连续发送次数统计
    private String mContinueSendMessage;

    @NonNull
    @Override
    protected FagmentPlayerRtmBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FagmentPlayerRtmBinding.inflate(inflater);
    }


    @Override
    public void initView() {
        Log.d(TAG, "<initView>");
        super.initView();
        mRtmViewModel = new ViewModelProvider(this).get(RtmViewModel.class);
        mRtmViewModel.setLifecycleOwner(this);
        getBinding().btnRtmConnect.setOnClickListener(view -> onBtnConnect());
        getBinding().btnRtmSend.setOnClickListener(view -> onBtnSend());
        getBinding().btnRtmTimersend.setOnClickListener(view -> onBtnTimerSend());
        getBinding().etRtmMessage.setEnabled(true);
        getBinding().tvRtmRecvedmsg.setMovementMethod(ScrollingMovementMethod.getInstance());

        mMsgHandler = new Handler(getActivity().getMainLooper())
        {
            @SuppressLint("HandlerLeak")
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case MSGID_RTM_TIMER_SEND:
                        onMsgTimerRtmSend();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    @Override
    public void initListener() {
        Log.d(TAG, "<initListener>");
        mRtmViewModel.setISingleCallback((type, data) -> {

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (type == Constant.CALLBACK_TYPE_RTM_CONNECT_DONE) {
                        int errCode = (Integer)data;
                        if (errCode != ErrCode.XOK) {
                            popupMessage("设备联接失败，不能发送消息，错误码=" + errCode);
                        } else {
                            popupMessage("设备联接成功，可以发送消息" + errCode);
                        }

                    } else if (type == Constant.CALLBACK_TYPE_RTM_SEND_DONE) {
                        int errCode = (Integer)data;
                        if (errCode != ErrCode.XOK) {
                            popupMessage("发送消息失败，错误码=" + errCode);
                        } else {
                            popupMessage("发送消息成功！");
                        }

                    } else if (type == Constant.CALLBACK_TYPE_RTM_RECVED) {
                        byte[] messageData = (byte[])data;

                        // SDK支持接收原始字节数据，具体是否转换成字符串要看设备端发送内容
                        try {
                            String message = new String(messageData, "UTF-8");
                            getBinding().tvRtmRecvedmsg.append(message);   // 添加到接收消息框中
                            String endline = "\n";
                            getBinding().tvRtmRecvedmsg.append(endline);
                        } catch (UnsupportedEncodingException encodeExp) {
                            encodeExp.printStackTrace();
                        }

                    }
                }
            });
        });
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
        Log.d(TAG, "<onStart>");
        super.onStart();
        mRtmViewModel.onStart();
 //       mRtmViewModel.connect();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "<onStop>");
        super.onStop();
        mRtmViewModel.onStop();
        mRtmViewModel.disconnect();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "<onPause>");
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "<onResume>");
        super.onResume();
    }


    public boolean onBtnBack() {
        return false;
    }

    /**
     * @brief 联接/断开 操作
     */
    void onBtnConnect() {
        int rtmState = mRtmViewModel.getRtmState();

        mRtmViewModel.connect2();

//        if (rtmState == IRtmMgr.RTMMGR_STATE_CONNECTED) {
//            mRtmViewModel.disconnect();
//            getBinding().btnRtmConnect.setText("联接");
//
//        } else if (rtmState == IRtmMgr.RTMMGR_STATE_DISCONNECTED) {
//            mRtmViewModel.connect();
//            getBinding().btnRtmConnect.setText("断开");
//        }
    }

    /**
     * @brief 发送单条RTM消息
     */
    void onBtnSend() {
        String messageData = getBinding().etRtmMessage.getText().toString();
        if (TextUtils.isEmpty(messageData)) {
            popupMessage("请输入要发送的内容");
            return;
        }

        mRtmViewModel.sendMessage(messageData.getBytes());
        getBinding().etRtmMessage.setText("");
    }

    /**
     * @brief 连续发送RTM消息
     */
    void onBtnTimerSend() {
        if (mContinueRtmSend) {
            mContinueRtmSend = false;
            if (mMsgHandler != null) {
                mMsgHandler.removeMessages(MSGID_RTM_TIMER_SEND);
            }
            getBinding().btnRtmTimersend.setText("开始发送");

        } else {
            mContinueSendMessage = getBinding().etRtmMessage.getText().toString();
            if (TextUtils.isEmpty(mContinueSendMessage)) {
                popupMessage("请输入要发送的内容");
                return;
            }

            mRtmSendCount = 0;
            mContinueRtmSend = true;
            if (mMsgHandler != null) {
                mMsgHandler.sendEmptyMessage(MSGID_RTM_TIMER_SEND);
            }
            getBinding().btnRtmTimersend.setText("停止发送");
        }
    }

    /**
     * @brief 定时发送RTM消息
     */
    void onMsgTimerRtmSend() {
        if (!mContinueRtmSend) {
            return;
        }

        mRtmSendCount++;
        String sendMessage = "[" + mRtmSendCount + "] " + mContinueSendMessage;
        mRtmViewModel.sendMessageWithoutCallback(sendMessage.getBytes());

        if (mContinueRtmSend) {
            if (mMsgHandler != null) {
                mMsgHandler.sendEmptyMessageDelayed(MSGID_RTM_TIMER_SEND, 16);
            }
        }
    }

}
