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
 * @brief 分页的告警信息
 */
public class IotAlarmPage {

    public ArrayList<IotAlarm> mAlarmList = new ArrayList<>();  ///< 当前页的告警记录列表
    public int mPageIndex;                  ///< 当前页索引，从1开始
    public int mPageSize;                   ///< 当前页最多告警数量
    public int mTotalPage;                  ///< 总页数


    @Override
    public String toString() {
        String infoText = "{ mAlarmListSize=" + mAlarmList.size()
                + ", mPageIndex=" + mPageIndex
                + ", mPageSize=" + mPageSize
                + ", mTotalPage=" + mTotalPage + " }";
        return infoText;
    }

}
