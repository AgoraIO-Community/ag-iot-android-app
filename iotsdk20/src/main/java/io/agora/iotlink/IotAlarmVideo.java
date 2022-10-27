/**
 * @file IotAlarm.java
 * @brief This file define the data structure of alarm information
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink;

import android.accounts.Account;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.agora.rtc2.Constants;

/*
 * @brief 告警云录视频信息
 */
public class IotAlarmVideo {

    public long mVideoRecordId; ///< 视频记录id
    public int mRecordType;     ///<  录像类型，0表示计划录像，1表示报警录像，2表示主动录像，99表示所有录像
    public String mAccountId;   ///< 用户账号Id
    public String mProductID;   ///< 产品id
    public String mDeviceID;    ///< 设备id
    public String mDeviceName;  ///< 设备名称

    public long mBeginTime;     ///< 录像开始时间戳
    public long mEndTime;       ///< 录像结束时间戳
    public String mFileName;    ///< 录像文件名（包括前面的目录）
    public String mVideoUrl;    ///< 录像文件路径URL，已经包含签名了
    public String mVideoSecret; ///< 视频加密信息
    public String mBucket;      ///< 图片文件存储桶
    public String mRemark;      ///< 备注

    public boolean mDeleted;    ///< 是否已删除，0-未删除，1-已删除
    public long mCreatedBy;     ///< 创建人id
    public long mCreateTime;    ///< 创建时间戳

    @Override
    public String toString() {
        String infoText = "{ mVideoRecordId=" + mVideoRecordId + ", mRecordType=" + mRecordType
                + ", mAccountId=" + mAccountId +  ", mProductID=" + mProductID
                + ", mDeviceID=" + mDeviceID + ", mDeviceName=" + mDeviceName
                + ", mBeginTime=" + mBeginTime + ", mEndTime=" + mEndTime
                + ", mFileName=" + mFileName + ", mVideoUrl=" + mVideoUrl + ", mVideoSecret=" + mVideoSecret
                + ", mBucket=" + mBucket + ", mRemark=" + mRemark
                + ", mDeleted=" + mDeleted + ", mCreatedBy=" + mCreatedBy
                + ", mCreateTime=" + mCreateTime + " }";
        return infoText;
    }

}
