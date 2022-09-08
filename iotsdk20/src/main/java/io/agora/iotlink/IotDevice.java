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
 * @brief 设备信息
 */
public class IotDevice {

    public String mAppUserId;           ///< 用户Id
    public String mUserType;           ///< 用户角色：1--所有者; 2--管理员; 3--成员

    public String mProductNumber;      ///< 产品唯一数字
    public String mProductID;         ///< 产品密钥
    public String mDeviceNumber;      ///< 设备唯一的数字
    public String mDeviceName;        ///< 设备名
    public String mDeviceID;          ///< 设备唯一ID(就是设备MAC)
    public String mSharer;            ///< 分享人的用户Id，如果自己配网则是 0
    public int mShareCount = -1;      ///< 当前分享个数，-1表示未设置
    public int mShareType = -1;       ///< 共享类型，-1表示未设置

    public long mCreateTime;          ///< 创建时间戳
    public long mUpdateTime;          ///< 最后一次更新时间戳

    public boolean mConnected;        ///< 是否在线


    @Override
    public String toString() {
        String infoText = "{ mAppUserId=" + mAppUserId + ", mUserType=" + mUserType
                + ", mProductNumber=" + mProductNumber + ", mProductID=" + mProductID
                + ", mDeviceNumber=" + mDeviceNumber + ", mDeviceName=" + mDeviceName
                + ", mDeviceID=" + mDeviceID + ", mSharer=" + mSharer + " }";
        return infoText;
    }

    protected IotDevice clone() throws CloneNotSupportedException {
        return (IotDevice)super.clone();
    }
}
