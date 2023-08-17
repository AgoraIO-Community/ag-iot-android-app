package io.agora.falcondemo.models.home;


import android.view.View;

import com.agora.baselibrary.base.BaseAdapter;

import java.util.UUID;

/**
 * @brief 设备信息
 */
public class DeviceInfo {

    public String       mNodeId;            ///< 设备的 NodeId

    public UUID         mSessionId;         ///< 当前通话的唯一标识，null表示当前无通话
    public int          mSessionType;       ///< 当前通话类型
    public String       mTips;              ///< 显示到控件上的提示信息
    public int          mUserCount;         ///< 在线用户数量
    public boolean      mDevMute;           ///< 设备是否静音
    public boolean      mRecording;         ///< 设备是否在录像
    public boolean      mMicPush;           ///< 麦克风是否推流

    public BaseAdapter.CommonViewHolder mViewHolder;    ///< 设备显示的 ViewHolder
    public View         mVideoView;         ///< 视频帧显示控件

    public boolean      mSelected;          ///< 选择模式下是否被选中

    @Override
    public String toString() {
        String infoText = "{ mNodeId=" + mNodeId
                + ", mSessionId=" + mSessionId
                + ", mSessionType=" + mSessionType
                + ", mUserCount=" + mUserCount
                + ", mDevMute=" + mDevMute
                + ", mMicPush=" + mMicPush
                + ", mRecording=" + mRecording + " }";
        return infoText;
    }

    public void clear() {
        mSessionId = null;
        mTips = "Device Closed (Hangup)";
        mDevMute = false;
        mRecording = false;
        mUserCount = 0;
        if (mVideoView != null) {
            mVideoView.setVisibility(View.INVISIBLE);
        }
    }

}
