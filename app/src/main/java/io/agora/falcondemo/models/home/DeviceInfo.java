package io.agora.falcondemo.models.home;


import android.view.View;

import com.agora.baselibrary.base.BaseAdapter;

import java.util.UUID;

import io.agora.iotlink.IConnectionObj;

/**
 * @brief 设备信息
 */
public class DeviceInfo {

    public String       mNodeId;            ///< 设备的 NodeId

    public IConnectionObj mConnectObj;      ///< 当前链接对象
    public int          mConnectType;       ///< 当前链接类型
    public String       mRcdFilePath;       ///< 录像保存文件

    public BaseAdapter.CommonViewHolder mViewHolder;    ///< 设备显示的 ViewHolder
    public View         mVideoView;         ///< 视频帧显示控件

    public boolean      mSelected;          ///< 选择模式下是否被选中


    @Override
    public String toString() {
        String infoText = "{ mNodeId=" + mNodeId
                + ", mConnectObj=" + mConnectObj
                + ", mConnectType=" + mConnectType + " }";
        return infoText;
    }

    public void clear() {
        mConnectObj = null;
        if (mVideoView != null) {
            mVideoView.setVisibility(View.INVISIBLE);
        }
    }

}
