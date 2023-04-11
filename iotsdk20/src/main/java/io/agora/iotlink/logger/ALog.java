/**
 * @file ALog.java
 * @brief This file implement the Agora logger
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2021-10-21
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.logger;


import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Time;
import java.util.Calendar;
import java.util.Locale;


public final class ALog {
    public static final int ASSERT = 7;
    public static final int DEBUG = 3;
    public static final int ERROR = 6;
    public static final int INFO = 4;
    public static final int VERBOSE = 2;
    public static final int WARN = 5;




    private static ALog mInstance = null;
    private String mLogFile;
    private FileWriter mWriter = null;


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public static ALog getInstance() {
        if (mInstance == null) {
            synchronized (ALog.class) {
                if(mInstance == null) {
                    mInstance = new ALog();
                }
            }
        }
        return mInstance;
    }


    public synchronized boolean initialize(String logFile) {
        mLogFile = logFile;
        return createWriter();
    }

    public synchronized void release() {
        try {
            if (mWriter != null) {
                mWriter.flush();
                mWriter.close();
                mWriter = null;
            }
        } catch (IOException exp) {
            exp.printStackTrace();
        }

        mLogFile = null;
    }


    public synchronized int d(@NonNull String tag, @NonNull String msg) {
        Log.d(tag, msg);
        if (mWriter == null) {  // 不输出日志到文件
            return -1;
        }

        try {
            String log = getTimestamp() + " [DBG] [" + tag + "] " + msg + "\n";
            mWriter.write(log);
            mWriter.flush();

        } catch (IOException exp) {
            exp.printStackTrace();
        }

        return 0;
    }


    public synchronized int i(@NonNull String tag, @NonNull String msg) {
        Log.i(tag, msg);
        if (mWriter == null) {  // 不输出日志到文件
            return -1;
        }

        try {
            String log = getTimestamp() + " [INF] [" + tag + "] " + msg + "\n";
            mWriter.write(log);
            mWriter.flush();

        } catch (IOException exp) {
            exp.printStackTrace();
        }

        return 0;
    }

    public synchronized int w(@NonNull String tag, @NonNull String msg) {
        Log.w(tag, msg);
        if (mWriter == null) {  // 不输出日志到文件
            return -1;
        }


        try {
            String log = getTimestamp() + " [WARN] [" + tag + "] " + msg + "\n";
            mWriter.write(log);
            mWriter.flush();

        } catch (IOException exp) {
            exp.printStackTrace();
        }

        return 0;
    }

    public synchronized int e(@NonNull String tag, @NonNull String msg) {
        Log.e(tag, msg);
        if (mWriter == null) {  // 不输出日志到文件
            return -1;
        }

        try {
            String log = getTimestamp() + " [ERR] [" + tag + "] " + msg + "\n";
            mWriter.write(log);
            mWriter.flush();

        } catch (IOException exp) {
            exp.printStackTrace();
        }

        return 0;
    }

    public synchronized void flush() {
        if (mWriter == null) {
            return;
        }

        try {
            mWriter.flush();
        } catch (IOException exp) {
            exp.printStackTrace();
        }
    }


    private boolean createWriter() {
        if (mWriter == null) {
            try {
                mWriter = new FileWriter(mLogFile, true);
                String log = "\n\n\n=========================================================\n";
                mWriter.write(log);
                mWriter.flush();

            } catch (IOException exp) {
                exp.printStackTrace();
                return false;
            }
        }

        return true;
    }


    private String getTimestamp() {
        String time_txt = "";
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) ;
        int month = calendar.get(Calendar.MONTH) + 1;
        int date = calendar.get(Calendar.DATE);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int ms = calendar.get(Calendar.MILLISECOND);

        time_txt = String.format(Locale.getDefault(), "%d-%02d-%02d %02d:%02d:%02d.%d",
                year, month, date, hour,minute, second, ms);
        return time_txt;
    }





}
