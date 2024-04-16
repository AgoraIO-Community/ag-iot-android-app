package io.agora.falcondemo.models.devstream;


import android.view.View;

import com.agora.baselibrary.base.BaseAdapter;

import java.util.UUID;

import io.agora.iotlink.IConnectionObj;

/**
 * @brief 设备流信息
 */
public class DevStreamInfo {


    public IConnectionObj.STREAM_ID mStreamId;
    public BaseAdapter.CommonViewHolder mViewHolder;    ///< 设备显示的 ViewHolder
    public View         mVideoView;                     ///< 视频帧显示控件

    @Override
    public String toString() {
        String infoText = "{ mStreamId=" + mStreamId  + " }";
        return infoText;
    }

    public void clear() {
        if (mVideoView != null) {
            mVideoView.setVisibility(View.INVISIBLE);
        }
    }

}
