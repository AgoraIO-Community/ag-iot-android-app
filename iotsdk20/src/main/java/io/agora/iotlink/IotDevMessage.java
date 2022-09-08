
package io.agora.iotlink;


/*
 * @brief 设备消息
 */
public class IotDevMessage {

    public long mMessageId;         ///< 设备消息Id，是设备消息唯一标识
    public int mMessageType;        ///< { 1:设备上线，2:设备离线，3:设备绑定，4:设备解绑，99：其他 }
    public String mDescription;     ///< 消息内容
    public int mStatus;             ///< 消息状态，  {0：未读, 1：已读}
    public String mFileUrl;         ///< 云录文件url地址

    public String mProductID;       ///< 产品ID
    public String mDeviceID;        ///< 设备ID
    public String mDeviceName;      ///< 设备名

    public boolean mDeleted;        ///< 是否已删除
    public long mCreatedBy;         ///< 创建人 ID
    public String mCreatedDate;     ///< 创建日期
    public long mChangedBy;         ///< 更新人 ID
    public String mChangedDate;     ///< 更新日期


    @Override
    public String toString() {
        String infoText = "{ mMessageId=" + mMessageId
                + ", mProductID=" + mProductID
                + ", mDeviceID=" + mDeviceID + ", mDeviceName=" + mDeviceName
                + ", mMessageType=" + mMessageType + ", mDescription=" + mDescription
                + ", mStatus=" + mStatus + ", mFileUrl=" + mFileUrl
                + ", mDeleted=" + mDeleted + ", mCreatedBy=" + mCreatedBy
                + ", mCreatedDate=" + mCreatedDate + ", mChangedBy=" + mChangedBy
                + ", mChangedDate=" + mChangedDate + " }";
        return infoText;
    }

}
