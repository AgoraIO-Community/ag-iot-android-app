package io.agora.falcondemo.models.devstream;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agora.baselibrary.base.BaseAdapter;

import io.agora.falcondemo.utils.DevStreamUtils;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.falcondemo.R;
import io.agora.iotlink.IConnectionMgr;
import io.agora.iotlink.IConnectionObj;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DevStreamListAdapter extends BaseAdapter<DevStreamInfo> {
    private static final String TAG = "IOTLINK/StreamListAdpt";
    private DevStreamActivity mOwner;
    public IConnectionObj mConnectObj;      ///< 当前链接实例，null表示当前无通话
    public boolean      mPubAudio = false;  ///< 是否正在推送APP端音频

    private Context context;

    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////// Public Methods /////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    public DevStreamListAdapter(Context context, List<DevStreamInfo> deviceList, IConnectionObj connectObj) {
        super(deviceList);
        this.context = context;
        mConnectObj = connectObj;
    }

    void setOwner(DevStreamActivity ownerActivity) {
        mOwner = ownerActivity;
    }

    void setPublishAudio(boolean pubAudio) {
        mPubAudio = pubAudio;
    }


    /**
     * @brief 获取设备流列表项
     */
    public DevStreamInfo getItem(int position) {
        DevStreamInfo devStream = getDatas().get(position);
        return devStream;
    }

    /**
     * @brief 设置设备列表项，同时更新相应控件显示
     */
    public void setItem(int position, final DevStreamInfo devStream) {
        getDatas().set(position, devStream);
        updateUiWgt(devStream);
    }

    /**
     * @brief 根据设备信息，更新 DeviceItem 控件显示
     */
    private void updateUiWgt(final DevStreamInfo devStream) {
        if (devStream.mViewHolder == null) {
            return;
        }

        Button btnPreview = devStream.mViewHolder.getView(R.id.btn_stream_preview);
        Button btnMute = devStream.mViewHolder.getView(R.id.btn_mute_streamaudio);
        Button btnRecord = devStream.mViewHolder.getView(R.id.btn_stream_record);

        // 设备流名
        String streamName = DevStreamUtils.getStreamName(devStream.mStreamId);
        devStream.mViewHolder.setText(R.id.tvStreamName, streamName);

        if (mConnectObj == null) {  // 连接已经断开
            devStream.mViewHolder.setText(R.id.tvStreamTips, "Disconnected");
            btnPreview.setText(context.getString(R.string.preview));
            btnMute.setText(context.getString(R.string.unmute));
            btnRecord.setText(context.getString(R.string.record));
            View displayView = devStream.mViewHolder.getView(R.id.svStreamView);
            displayView.setVisibility(View.INVISIBLE);
            return;
        }

        // 获取流的状态信息
        int connectState = IConnectionObj.STATE_DISCONNECTED;
        boolean audioPublishing = false;
        boolean subscribed = false;
        boolean muteAudio = true;
        boolean recording = false;
        IConnectionObj.ConnectionInfo connectInfo = mConnectObj.getInfo();
        connectState = connectInfo.mState;
        audioPublishing = connectInfo.mAudioPublishing;
        if (devStream.mStreamId != null) {
            IConnectionObj.StreamStatus streamStatus = mConnectObj.getStreamStatus(devStream.mStreamId);
            subscribed = streamStatus.mSubscribed;
            muteAudio = streamStatus.mAudioMute;
            recording = streamStatus.mRecording;
        }

        // 设置设备显示控件
        View displayView = devStream.mViewHolder.getView(R.id.svStreamView);
        if (subscribed) {
            displayView.setVisibility(View.VISIBLE);
            mConnectObj.setVideoDisplayView(devStream.mStreamId, devStream.mVideoView);
        } else {
            displayView.setVisibility(View.INVISIBLE);
        }

        // 提示文字
        if (mConnectObj == null) {
            devStream.mViewHolder.setText(R.id.tvStreamTips, "Disconnected");  // 未连接
        } else {
            if (connectState != IConnectionObj.STATE_CONNECTED) {
                devStream.mViewHolder.setText(R.id.tvStreamTips, "Connecting...");
            } else if (recording) {
                devStream.mViewHolder.setText(R.id.tvStreamTips, "Recording...");
            } else if (subscribed) {
                devStream.mViewHolder.setText(R.id.tvStreamTips, "Subscribed"); // 已订阅
            } else {
                devStream.mViewHolder.setText(R.id.tvStreamTips, "Connected");
            }
        }

        // 预览停止
        btnPreview.setText(subscribed ? context.getString(R.string.stop_record)  : context.getString(R.string.preview) );

        // 静音播音按钮
        btnMute.setText(muteAudio? context.getString(R.string.unmute)  : context.getString(R.string.mute) );

        // 录像按钮
        btnRecord.setText(recording ? context.getString(R.string.stop_record)  : context.getString(R.string.record) );
    }

    /**
     * @brief 增加设备列表项
     */
    public void addNewItem(final DevStreamInfo devStream) {
        getDatas().add(devStream);
        this.notifyDataSetChanged();
    }


    @Override
    public int getLayoutId(int viewType) {
        return R.layout.item_devstream_info;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder holder, int position) {
        DevStreamInfo devStream = getDatas().get(position);
        if (devStream == null) {
            return;
        }

        devStream.mVideoView = holder.getView(R.id.svStreamView);
        devStream.mViewHolder = holder;
        getDatas().set(position, devStream);   // 更新控件信息

        Button btnPreview = devStream.mViewHolder.getView(R.id.btn_stream_preview);
        Button btnMute = devStream.mViewHolder.getView(R.id.btn_mute_streamaudio);
        Button btnRecord = devStream.mViewHolder.getView(R.id.btn_stream_record);

        // 设备流名
        String streamName = DevStreamUtils.getStreamName(devStream.mStreamId);
        devStream.mViewHolder.setText(R.id.tvStreamName, streamName);

        if (mConnectObj == null) {  // 连接已经断开
            devStream.mViewHolder.setText(R.id.tvStreamTips, "Disconnected");
            btnPreview.setText(context.getString(R.string.preview));
            btnMute.setText(context.getString(R.string.unmute));
            btnRecord.setText(context.getString(R.string.record));
            View displayView = devStream.mViewHolder.getView(R.id.svStreamView);
            displayView.setVisibility(View.INVISIBLE);
            return;
        }

        // 获取流的状态信息
        int connectState = IConnectionObj.STATE_DISCONNECTED;
        boolean audioPublishing = false;
        boolean subscribed = false;
        boolean muteAudio = true;
        boolean recording = false;
        IConnectionObj.ConnectionInfo connectInfo = mConnectObj.getInfo();
        connectState = connectInfo.mState;
        audioPublishing = connectInfo.mAudioPublishing;
        if (devStream.mStreamId != null) {
            IConnectionObj.StreamStatus streamStatus = mConnectObj.getStreamStatus(devStream.mStreamId);
            subscribed = streamStatus.mSubscribed;
            muteAudio = streamStatus.mAudioMute;
            recording = streamStatus.mRecording;
        }

        // 设置设备显示控件
        View displayView = devStream.mViewHolder.getView(R.id.svStreamView);
        if (subscribed) {
            displayView.setVisibility(View.VISIBLE);
            mConnectObj.setVideoDisplayView(devStream.mStreamId, devStream.mVideoView);
        } else {
            displayView.setVisibility(View.INVISIBLE);
        }

        // 提示文字
        if (mConnectObj == null) {
            devStream.mViewHolder.setText(R.id.tvStreamTips, "Disconnected");  // 未连接
        } else {
            if (connectState != IConnectionObj.STATE_CONNECTED) {
                devStream.mViewHolder.setText(R.id.tvStreamTips, "Connecting...");
            } else if (recording) {
                devStream.mViewHolder.setText(R.id.tvStreamTips, "Recording...");
            } else if (subscribed) {
                devStream.mViewHolder.setText(R.id.tvStreamTips, "Subscribed"); // 已订阅
            } else {
                devStream.mViewHolder.setText(R.id.tvStreamTips, "Connected");
            }
        }

        // 预览停止
        btnPreview.setText(subscribed ? context.getString(R.string.stop_record)  : context.getString(R.string.preview) );
        btnPreview.setOnClickListener(view -> {
            mOwner.onDevItemPreviewClick(view, position, devStream);
        });

        // 静音播音按钮
        btnMute.setText(muteAudio ? context.getString(R.string.unmute)  : context.getString(R.string.mute) );
        btnMute.setOnClickListener(view -> {
            mOwner.onDevItemMuteAudioClick(view, position, devStream);
        });

        // 录像按钮
        btnRecord.setText(recording ? context.getString(R.string.stop_record)  : context.getString(R.string.record) );
        btnRecord.setOnClickListener(view -> {
            mOwner.onDevItemRecordClick(view, position, devStream);
        });

        // 截屏按钮
        Button btnShotCapture = holder.getView(R.id.btn_stream_shot);
        btnShotCapture.setOnClickListener(view -> {
            mOwner.onDevItemShotCaptureClick(view, position, devStream);
        });

    }

}
