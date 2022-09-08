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
 * @brief 设备属性
 */
public class IotDevProperty {

    ///////////////////////////////////////////////////
    ///////////////////// 可读写属性 ///////////////////
    ///////////////////////////////////////////////////
    public boolean mOsdWatermark = false;       ///< OSD水印
    public int mNightView = 1;                  ///< 红外夜视
    public boolean mMotionDetect = false;       ///< 移动侦测
    public int mPirSensitive = 0;               ///< PIR灵敏度
    public int mVolume = 50;                    ///< 音量
    public boolean mForceAlarm = false;         ///< 强拆警报
    public int mPowerMode = 2;                  ///< 功耗模式
    public int mVideoQuality = 1;               ///< 视频清晰度
    public boolean mLed = false;                ///< 指示灯
    public int mTfCardFormat = 2;               ///< TF卡格式化
    public int mPreviewDuration = 600;          ///< 预览时长
    public boolean mVoiceDetect = false;        ///< 声音检测
    public boolean mSirenSwitch = false;        ///< 设备警笛开关

    ///////////////////////////////////////////////////
    ///////////////////// 只读属性 ////////////////////
    ///////////////////////////////////////////////////
    public int mQuantity = 50;                  ///< 电池电量
    public String mWifiSsid;                    ///< WIFI SSID
    public String mIpAddress;                   ///< IP地址
    public String mDevMac;                      ///< MAC地址
    public String mTimeZone;                    ///< 时区
    public String mVersion;                     ///< 固件版本号
    public int mTfCardState = 6;                ///< TF卡状态
    public int mTfCardSpace = 0;                ///< TF卡剩余空间

    /*
     * @brief 根据Map属性中的列表更新
     */
    public void update(Map<String, Object> properties) {
        Iterator iterator = properties.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry  mapEntry = (Map.Entry) iterator.next();
            String key = mapEntry.getKey().toString();

            switch (key) {
                ///////////////////////////////////////
                //////////////// 读写属性 /////////////
                ///////////////////////////////////////
                case "100":     //OSD水印开关
                    mOsdWatermark = (Boolean)(mapEntry.getValue());
                    break;

                case "101":     //红外夜视
                    mNightView = (Integer)(mapEntry.getValue());
                    break;

                case "102":     //移动报警开关
                    mMotionDetect = (Boolean)(mapEntry.getValue());
                    break;

                case "103":     //PIR开关及灵敏度
                    mPirSensitive = (Integer)(mapEntry.getValue());
                    break;

                case "104":     //设备音量控制
                    mVolume = (Integer)(mapEntry.getValue());
                    break;

                case "105":     //强拆警报
                    mForceAlarm = (Boolean)(mapEntry.getValue());
                    break;

                case "1000":     //功耗模式
                    mPowerMode = (Integer) (mapEntry.getValue());
                    break;

                case "107":     //视频清晰度
                    mVideoQuality = (Integer) (mapEntry.getValue());
                    break;

                case "108":     //指示灯
                    mLed = (Boolean) (mapEntry.getValue());
                    break;

                case "112":     //TF卡格式化
                    mTfCardFormat = (Integer) (mapEntry.getValue());
                    break;

                case "113":     //预览时长
                    mPreviewDuration = (Integer) (mapEntry.getValue());
                    break;

                case "114":     //设备警笛
                    mSirenSwitch = (Boolean) (mapEntry.getValue());
                    break;

                case "115":     //声音检测
                    mVoiceDetect = (Boolean) (mapEntry.getValue());
                    break;


                ///////////////////////////////////////
                //////////////// 只读属性 /////////////
                ///////////////////////////////////////
                case "106":     //电池电量
                    mQuantity = (Integer)(mapEntry.getValue());
                    break;

                case "501":    //WIFI SSID
                    mWifiSsid = (String) (mapEntry.getValue());
                    break;

                case "502":    //IP Address
                    mIpAddress = (String) (mapEntry.getValue());
                    break;

                case "503":    //MAC Address
                    mDevMac = (String) (mapEntry.getValue());
                    break;

                case "504":    //时区
                    mTimeZone = (String) (mapEntry.getValue());
                    break;

                case "109":    //固件版本号
                    mVersion = (String) (mapEntry.getValue());
                    break;

                case "110":    //TF卡状态
                    mTfCardState = (Integer) (mapEntry.getValue());
                    break;

                case "111":    //TF卡剩余空间
                    mTfCardSpace = (Integer) (mapEntry.getValue());
                    break;
            }
        }
    }

    @Override
    public String toString() {
        String infoText = "{ mOsdWatermark=" + mOsdWatermark
                + ", mNightView=" + mNightView + ", mMotionDetect=" + mMotionDetect
                + ", mPirSensitive=" + mPirSensitive + ", mVolume=" + mVolume
                + ", mForceAlarm=" + mForceAlarm + ", mPowerMode=" + mPowerMode
                + ", mVideoQuality=" + mVideoQuality + ", mLed=" + mLed
                + ", mTfCardFormat=" + mTfCardFormat + ", mPreviewDuration=" + mLed
                + ", mVoiceDetect=" + mVoiceDetect
                + ", mQuantity=" + mQuantity + ", mWifiSsid=" + mWifiSsid
                + ", mIpAddress=" + mIpAddress + ", mDevMac=" + mDevMac
                + ", mTimeZone=" + mTimeZone + ", mVersion=" + mVersion
                + ", mTfCardState=" + mTfCardState + ", mTfCardSpace=" + mTfCardSpace + "}";
        return infoText;
    }

}
