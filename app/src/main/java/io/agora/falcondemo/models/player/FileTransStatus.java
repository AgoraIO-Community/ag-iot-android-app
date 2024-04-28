package io.agora.falcondemo.models.player;



import com.agora.baselibrary.base.BaseAdapter;


/**
 * @brief 文件传输状态信息
 */
public class FileTransStatus {
    /**
     * @brief 状态的类型
     */
    public static final int TYPE_INVALID = 0x0000;
    public static final int TYPE_START = 0x0001;      ///< 发送了开始传输命令
    public static final int TYPE_STOP = 0x0002;       ///< 发送了停止传输命令
    public static final int TYPE_FILE_BEGIN = 0x0003; ///< 单个文件开始
    public static final int TYPE_FILE_DATA = 0x0004;  ///< 单个文件数据
    public static final int TYPE_FILE_END = 0x0005;   ///< 单个文件结束


    public BaseAdapter.CommonViewHolder mViewHolder;    ///< 显示的 ViewHolder

    public int          mType;                          ///< 状态的类型
    public String       mTimestamp;                     ///< 状态时间
    public String       mInfo;                          ///< 状态的信息: START STOP BEGIN END 类型用到
    public int          mDataSize;                      ///< 数据大小
    public boolean      mEOF;                           ///< 是否整体结束
    public String       mMd5Text;                       ///< 单个文件的Md5值
    public boolean      mRecvSuccess;                   ///< 接收是否成功

    @Override
    public String toString() {
        String infoText = "{ mType=" + mType
                + ", mTimestamp=" + mTimestamp
                + ", mInfo=" + mInfo
                + ", mDataSize=" + mDataSize
                + ", mEOF=" + mEOF
                + ", mMd5Text=" + mMd5Text + " }";
        return infoText;
    }

}
