
package io.agora.iotlink;


/*
 * @brief 设备共享消息
 */
public class IotShareMessage {

    public long mMessageId = 0;     ///< 设备消息Id，是设备消息唯一标识
    public int mMessageType = -1;   ///< 消息类型：1--分享消息
    public int mStatus = -1;        ///< 状态：1--发送成功；2--发送失败；  3--待发送
    public boolean mAuditStatus;    ///< 是否已经处理

    public int mPushType = -1;      ///< 推送类型：1--App消息； 2--短信消息； 3--邮箱消息
    public long mPushTime = -1;     ///< 推送时间

    public String mParam;           ///< 消息参数：对于分享消息就是分享口令
    public int mPermission = -1;    ///< 分享权限：2--管理员权限； 3--成员权限
    public String mTitle;           ///< 消息标题
    public String mContent;         ///< 消息内容
    public long mRecvUserId = -1;   ///< 被分享人Id

    public String mProductName;     ///< 产品名字
    public String mDeviceNumber;    ///< 设备唯一数字
    public String mProductImgUrl;   ///< 产品图片路径
    public String mMerchantId;      ///< 厂商Id
    public String mMerchantName;    ///< 厂商名称

    public boolean mDeleted;        ///< 是否已删除
    public long mCreatedBy = -1;    ///< 创建人 ID
    public long mCreateTime;        ///< 创建日期
    public long mUpdatedBy = -1;    ///< 修改人 ID
    public long mUpdateTime;        ///< 修改日期

    @Override
    public String toString() {
        String infoText = "{ mMessageId=" + mMessageId
                + ", mMessageType=" + mMessageType + ", mStatus=" + mStatus
                + ", mPushType=" + mPushType + ", mPushTime=" + mPushTime
                + ", mParam=" + mParam + ", mPermission=" + mPermission
                + ", mTitle=" + mTitle + ", mContent=" + mContent + ", mRecvUserId=" + mRecvUserId
                + ", mProductName=" + mProductName + ", mDeviceNumber=" + mDeviceNumber
                + ", mProductImgUrl=" + mProductImgUrl
                + ", mMerchantId=" + mMerchantId + ", mMerchantName=" + mMerchantName
                + ", mDeleted=" + mDeleted + ", mCreatedBy=" + mCreatedBy
                + ", mCreateTime=" + mCreateTime + " }";
        return infoText;
    }

}
