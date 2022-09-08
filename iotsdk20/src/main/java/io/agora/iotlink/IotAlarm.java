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
 * @brief 告警信息
 */
public class IotAlarm {

    public long mAlarmId;           ///< 告警信息Id，是告警信息唯一标识

    public String mProductID;       ///< 产品Id
    public String mDeviceID;        ///< 设备Id
    public String mDeviceName;      ///< 设备名

    public int mMessageType;        ///< 消息类型，{0:声音检测, 1:移动侦测，2: PIR红外侦测； 3：按钮警报；}
    public String mDescription;     ///< 消息内容
    public int mStatus;             ///< 消息状态，  {0：未读, 1：已读}
    public long mTriggerTime;       ///< 告警消息触发时间戳，通常告警事件录制60秒

    public String mImageId;         ///< 告警图片Id
    public String mImageUrl;        ///< 告警图片路径Url
    public String mVideoUrl;        ///< 告警视频路径Url，分页查询时不返回，只有单个告警信息查询时返回
    public long mVideoBeginTime;    ///< 告警视频开始时间戳
    public long mVideoEndTime;      ///< 告警视频结束时间戳

    public boolean mDeleted;        ///< 是否已删除
    public long mCreatedBy;         ///< 创建人 ID
    public long mCreatedDate;       ///< 创建日期
    public long mChangedBy;         ///< 更新人 ID
    public long mChangedDate;       ///< 更新日期


    @Override
    public String toString() {
        String infoText = "{ mAlarmId=" + mAlarmId + ", mProductID=" + mProductID
                + ", mDeviceID=" + mDeviceID + ", mDeviceName=" + mDeviceName
                + ", mMessageType=" + mMessageType + ", mDescription=" + mDescription
                + ", mStatus=" + mStatus + ", mTriggerTime=" + mTriggerTime
                + ", mImageId=" + mImageId + ", mImageUrl=" + mImageUrl
                + ", mVideoUrl=" + mVideoUrl
                + ", mVideoBeginTime=" + mVideoBeginTime + ", mVideoEndTime=" + mVideoEndTime
                + ", mDeleted=" + mDeleted + ", mCreatedBy=" + mCreatedBy
                + ", mCreatedDate=" + mCreatedDate + ", mChangedBy=" + mChangedBy
                + ", mChangedDate=" + mChangedDate + " }";
        return infoText;
    }

}
