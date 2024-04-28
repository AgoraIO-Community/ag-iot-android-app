package io.agora.falcondemo.models.player;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agora.baselibrary.base.BaseAdapter;

import io.agora.falcondemo.models.home.DeviceInfo;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.falcondemo.R;
import io.agora.iotlink.IConnectionMgr;
import io.agora.iotlink.IConnectionObj;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TransStatusListAdapter extends BaseAdapter<FileTransStatus> {
    private static final String TAG = "IOTLINK/TransListAdpt";


    private DevPreviewActivity mOwner;
    private RecyclerView mRecycleView;


    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////// Public Methods /////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    public TransStatusListAdapter(List<FileTransStatus> statusList) {
        super(statusList);
    }

    void setOwner(DevPreviewActivity ownerActivity) {
        mOwner = ownerActivity;
    }

    /**
     * @brief 清除所有列表项
     */
    void clear() {
        List<FileTransStatus> emptyList = new ArrayList<>();
        setDatas(emptyList);
        this.notifyDataSetChanged();
    }

    /**
     * @brief 获取传输状态列表项
     */
    public FileTransStatus getItem(int position) {
        return getDatas().get(position);
    }

    /**
     * @brief 设置传输状态列表项，同时更新相应控件显示
     */
    public void setItem(int position, final FileTransStatus transStatus) {
        getDatas().set(position, transStatus);
        updateUiWgt(transStatus);
    }

    /**
     * @brief 根据设备信息，更新 DeviceItem 控件显示
     */
    private void updateUiWgt(final FileTransStatus transStatus) {
        if (transStatus.mViewHolder == null) {
            return;
        }
        // 状态文本信息
        transStatus.mViewHolder.setText(R.id.tvTransStatus, getDisplayText(transStatus));
    }

    /**
     * @brief 增加新的状态到开头
     */
    public void addNewItem(final FileTransStatus transStatus) {
        List<FileTransStatus> statusList = getDatas();
        statusList.add(0, transStatus);
        setDatas(statusList);
        this.notifyDataSetChanged();
    }


    @Override
    public int getLayoutId(int viewType) {
        return R.layout.item_filetrans_status;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder holder, int position) {
        FileTransStatus transStatus = getDatas().get(position);
        if (transStatus == null) {
            return;
        }
        transStatus.mViewHolder = holder;

        // 状态文本信息
        transStatus.mViewHolder.setText(R.id.tvTransStatus, getDisplayText(transStatus));
    }


    /**
     * @brief 根据状态类型显示不同的内容
     */
    String getDisplayText(final FileTransStatus transStatus) {
        String displayText = " ";

        switch (transStatus.mType) {
            case FileTransStatus.TYPE_START:
                displayText = transStatus.mTimestamp + " [SEND_CMD_START] " + transStatus.mInfo;
                break;
            case FileTransStatus.TYPE_STOP:
                displayText = transStatus.mTimestamp + " [SEND_CMD_STOP] " + transStatus.mInfo;
                break;
            case FileTransStatus.TYPE_FILE_BEGIN:
                displayText = transStatus.mTimestamp + " [ONE_FILE_BEGIN] " + transStatus.mInfo;
                break;
            case FileTransStatus.TYPE_FILE_DATA:
                displayText = transStatus.mTimestamp + " [ONE_FILE_DATA] size=" + transStatus.mDataSize;
                break;
            case FileTransStatus.TYPE_FILE_END:
                String eofText = transStatus.mEOF ? " [EOF] " : " ";
                String recvText = (transStatus.mRecvSuccess) ? " [SUCCESS]" : " [FAILURE]";
                displayText = transStatus.mTimestamp + " [ONE_FILE_END] " + transStatus.mInfo
                        + eofText + recvText;
                break;
        }

        return displayText;
    }



}
