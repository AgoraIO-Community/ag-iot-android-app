/**
 * @file IotDevice.java
 * @brief This file define the data structure of IoT device
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink;



/*
 * @brief 设备共享出去的信息
 */
public class IotOutSharer {

    public String mSharer;              ///< 被共享的Id
    public int mShareType = -1;         ///< 共享权限 2--管理员权限； 3--成员权限
    public String mPhone;               ///< 被共享用户的手机
    public String mEmail;               ///< 被共享用户的邮箱
    public String mUsrNickName;         ///< 被共享的用户名称
    public String mAppUserId;           ///< 用户Id
    public String mAvatar;              ///< 头像

    public String mProductNumber;       ///< 产品唯一Number
    public String mProductID;           ///< 产品ID
    public String mDeviceID;            ///< 设备ID(MAC地址)
    public String mDevNickName;         ///< 设备名
    public boolean mConnected;          ///< 是否在线

    public long mCreateTime = -1;       ///< 创建时间戳
    public long mUpdateTime = -1;       ///< 最后一次更新时间戳


    @Override
    public String toString() {
        String infoText = "{ mSharer=" + mSharer + ", mShareType=" + mShareType
                + "mPhone=" + mPhone + ", mEmail=" + mEmail
                + "mUsrNickName=" + mUsrNickName + ", mAppUserId=" + mAppUserId
                + "mAvatar=" + mAvatar + ", mAvatar=" + mAvatar
                + "mAppUserId=" + mAppUserId
                + ", mProductNumber=" + mProductNumber + ", mProductID=" + mProductID
                + ", mDeviceID=" + mDeviceID
                + ", mDevNickName=" + mDevNickName + ", mConnected=" + mConnected
                + ", mCreateTime=" + mCreateTime + ", mUpdateTime=" + mUpdateTime + " }";

        return infoText;
    }

    protected IotOutSharer clone() throws CloneNotSupportedException {
        return (IotOutSharer)super.clone();
    }
}
