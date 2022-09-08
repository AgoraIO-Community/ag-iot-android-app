/**
 * @file IotNotification.java
 * @brief This file define the data structure of notification information
 *        The notification come from device or server
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
 * @brief 通知信息
 */
public class IotNotification {

    public String mNotificationId;  ///< 通知信息Id，是通知信息唯一标识
    public Date mOccurTime;         ///< 通知时间
    public String mEvent;           ///< 通知事件
    public int mMarkFlag;           ///< 标记信息
}
