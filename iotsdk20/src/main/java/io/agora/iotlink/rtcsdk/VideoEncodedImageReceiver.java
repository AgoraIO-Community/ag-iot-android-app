/**
 * @file VideoEncodedImageReceiver.java
 * @brief This file collect the received video frame for saving
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2021-10-18
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.rtcsdk;


import android.os.Environment;

import io.agora.iotlink.logger.ALog;

import io.agora.rtc2.Constants;
import io.agora.rtc2.video.EncodedVideoFrameInfo;
import io.agora.rtc2.video.IVideoEncodedFrameObserver;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;


public class VideoEncodedImageReceiver implements IVideoEncodedFrameObserver {
    private final static String TAG = "IOTSDK/VidEncImgRcver";
    public static final String APP_DIRECTORY = Environment.getExternalStorageDirectory()
                                            + File.separator + "com.agora.agoracallkit";

    private final ConcurrentHashMap<String, File> mSavedDir = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> mSavedFrameIndex = new ConcurrentHashMap<>();

    @Override
    public boolean OnEncodedVideoFrameReceived(ByteBuffer buffer, EncodedVideoFrameInfo info) {
        String strLog = String.format(Locale.getDefault(),
                "<OnEncodedVideoImageReceived> remoteUid=%d, buffer.capacity=%d, info=%s",
                info.uid, buffer.capacity(), info.toString());
        ALog.getInstance().d(TAG, strLog);

        String key = "remoteuid_" + info.uid;
        String dirName = APP_DIRECTORY + File.separator + key;
        File dir  = null;
        int currentIndex = 0;
        if (mSavedDir.containsKey(key)) {
            dir = mSavedDir.get(key);
            currentIndex = mSavedFrameIndex.get(key);
            mSavedFrameIndex.put(key, currentIndex + 1);
        } else {
            if (!createDir(dirName)) return false;
            dir = new File(dirName);
            mSavedDir.put(key, dir);
            mSavedFrameIndex.put(key, currentIndex + 1);
        }
        if (currentIndex >= EncodedVideoFrame.ENCODED_FRAME_TOTAL) return true;
        String baseName = baseName(EncodedVideoFrame.ENCODED_IMAGE_FILE_NAME);
        String extName = extName(EncodedVideoFrame.ENCODED_IMAGE_FILE_NAME);
        String frameType = null;
        switch (info.frameType) {
            case Constants.VIDEO_FRAME_TYPE_BLANK_FRAME: frameType = "E"; break;
            case Constants.VIDEO_FRAME_TYPE_KEY_FRAME: frameType = "I"; break;
            case Constants.VIDEO_FRAME_TYPE_DELTA_FRAME: frameType = "P"; break;
            case Constants.VIDEO_FRAME_TYPE_B_FRAME: frameType = "B"; break;
            case Constants.VIDEO_FRAME_TYPE_UNKNOWN: frameType = "U"; break;
            default: frameType = "X"; break;
        }
        StringBuilder sb = new StringBuilder(dirName);
        sb.append(File.separator);
        sb.append(baseName).append("_");
        sb.append(currentIndex).append("_");
        sb.append(frameType).append("_");
        sb.append(buffer.capacity());
        if (extName != null && !extName.isEmpty()) {
            sb.append(".").append(extName);
        }
        String oneFrameName = sb.toString();
        ALog.getInstance().d(TAG,"<OnEncodedVideoImageReceived> oneFrameName=" + oneFrameName);

        byte[] b = new byte[buffer.capacity()];
        buffer.get(b);
        FileOutputStream dos = null;
        try {
            dos = new FileOutputStream(new File(oneFrameName));
            dos.write(b, 0, b.length);
            dos.close();

        } catch (IOException e) {
            ALog.getInstance().d(TAG, "<OnEncodedVideoImageReceived> write " + oneFrameName + " fail!");
            if (dos != null) {
                try {
                    dos.close();
                } catch (Exception a) {
                }
            }
            return false;
        }
        return true;
    }



    public static boolean createDir(String dir) {
        try {
            File d = new File(dir);
            if (d.exists()) {
                ALog.getInstance().i(TAG, "createDir:" + d.getName() + " has existed, empty it first");
                FilenameFilter filter = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return true;
                    }
                };
                rmFile(dir, filter);
                return true;
            } else {
                if (d.mkdirs()) {
                    ALog.getInstance().i(TAG, "createDir:" + d.getName() + " create success!");
                } else {
                    ALog.getInstance().e(TAG, "createDir:" + d.getName() + " create failed!");
                }
            }
        } catch (SecurityException e) {
            ALog.getInstance().e(TAG, "createDir:" + dir + " create fail! e=" + e);
            return false;
        }
        return true;
    }

    public static boolean rmFile(String dir, FilenameFilter filter) {
        try {
            File d = new File(dir);
            if (!d.exists()) {
                ALog.getInstance().e(TAG, "rmFile:" + d.getName() + " not existed!");
                return true;
            }
            for (File f : d.listFiles(filter)) {
                if (f.isDirectory()) {
                    ALog.getInstance().i(TAG, "rmFile: " + f.getName() + " is directory.");
                    rmFile(dir + File.separator + f.getName(), filter);
                } else {
                    if (f.delete()) {
                        ALog.getInstance().i(TAG, "rmFile: " + f.getName() + " success!");
                    } else {
                        ALog.getInstance().e(TAG, "rmFile: " + f.getName() + " fail!");
                    }
                }
            }
        } catch (SecurityException e) {
            ALog.getInstance().e(TAG, "rmFile:" + dir + " fail! e=" + e);
            return false;
        }
        return true;
    }


    public static String baseName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("fileName is empty!");
        }
        int index = fileName.lastIndexOf(".");
        if (index < 0) {
            ALog.getInstance().i(TAG, "can't find '.' in " + fileName);
            return fileName;
        } else {
            return fileName.substring(0, index);
        }
    }

    public static String extName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("fileName is empty!");
        }
        int index = fileName.lastIndexOf(".");
        if (index < 0) {
            ALog.getInstance().i(TAG, "can't find '.' in " + fileName);
            return "";
        } else if (index == fileName.length() - 1) {
            return "";
        } else {
            return fileName.substring(index + 1);
        }
    }

}
