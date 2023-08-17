/**
 * @file ALog.java
 * @brief This file implement the Agora logger
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2021-10-21
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package org.eclipse.paho.android.service;


import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;


public final class MqttLog {


    /**
     * @brief 外部实现的回调函数
     */
    public static interface IMqttLogger {

        default void onMqttLogger(final String tag, final String content) {  }

    }

    private static IMqttLogger mExtLogger = null;

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public static synchronized void setMqttLogger(final IMqttLogger logger) {
        mExtLogger = logger;
    }

    public static synchronized int d(String tag, String msg) {
        if (mExtLogger != null) {
            mExtLogger.onMqttLogger(tag, msg);
        }
        return 0;
    }


    public static synchronized int i(String tag, String msg) {
        if (mExtLogger != null) {
            mExtLogger.onMqttLogger(tag, msg);
        }
        return 0;
    }

    public static synchronized int w(String tag, String msg) {
        if (mExtLogger != null) {
            mExtLogger.onMqttLogger(tag, msg);
        }
        return 0;
    }

    public static synchronized int e(String tag, String msg) {
        if (mExtLogger != null) {
            mExtLogger.onMqttLogger(tag, msg);
        }
        return 0;
    }


}
