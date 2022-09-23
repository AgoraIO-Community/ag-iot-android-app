/**
 * @file IotAlarm.java
 * @brief This file define the data structure of alarm information
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 *
 * 可读写属性：
 *     OSD水印     100    	  true/false
 *     红外夜视    101   	  0: 自动；1：关；2：开
 *     移动侦测    102    	  true/false
 *     PRI灵敏度   103    	  0: 关；1：3m；2：1.5m;  3: 0.8m
 *     音量        104    	  1~100，默认50
 *     强拆警报    105   	  true/false
 *     功耗模式    1000      0：切换中；1：低功耗；2：正常
 *     视频清晰度  107       1: 标清；2：高清
 *     指示灯      108       true/false
 *     TF卡格式化  112       1: 开始格式化；2：取消格式化
 *     预览时长    113       1~3600，默认600
 *     声音检测    115       true/false
 *
 * 只读属性：
 *     电池电量	  106         1~100，默认50
 *     WIFI SSID   501         字符串
 *     IP地址      502         字符串
 *     MAC地址     503         字符串
 *     时区        504         字符串
 *     固件版本号  109         字符串
 *     TF状态      110        6
 *     TF剩余空间  111       0~65535
 *
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.agora.rtc2.Constants;

/*
 * @brief 设备属性点描述符
 */
public class IotPropertyDesc {

    public long mId;           ///< 属性唯一标识
    public int mIndex;         ///< 排序键（值越小排越前）

    public String mProductID;  ///<	产品ID
    public String mPointName;  ///<	数据点名称
    public int mPointType;     ///< 数据点类型：【1】整型、【2】布尔值、【3】枚举、【4】字符串、【5】浮点型、【6】bit类型、【7】raw类型
    public String mMarkName;    ///<	数据点标识
    public int mReadType;      ///<	读写类型：【1】只读、【2】读写
    public String mMaxValue;   ///<	最大值
    public String mMinValue;   ///<	最小值
    public String mParams;     ///<	类型参数值
    public String mRemark;     ///<	备注
    public int  mStatus;       ///<	状态：【1】启用、【2】停用

    public long mCreateBy;     ///<	创建人ID
    public long mCreateTime;   ///<	创建时间
    public int mDeleted;       ///< 	删除标记：【0】未删除、【1】已删除

    @Override
    public String toString() {
        String infoText = "{ mId=" + mId + ", mIndex=" + mIndex
                + ", mProductID=" + mProductID + ", mPointName=" + mPointName
                + ", mPointType=" + mPointType + ", mMarkName=" + mMarkName
                + ", mReadType=" + mReadType + ", mRemark=" + mRemark
                + ", mMaxValue=" + mMaxValue + ", mMinValue=" + mMinValue
                + ", mParams=" + mParams + ", mStatus=" + mStatus
                + ", mCreateBy=" + mCreateBy + ", mCreateTime=" + mCreateTime
                + ", mDeleted=" + mDeleted + "}";
        return infoText;
    }

}
