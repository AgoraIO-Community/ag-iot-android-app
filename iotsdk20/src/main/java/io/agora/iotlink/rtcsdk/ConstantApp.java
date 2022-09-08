/**
 * @file ConstantApp.java
 * @brief This file define the global constant variable
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2021-10-18
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.rtcsdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;


import io.agora.iotlink.logger.ALog;

import io.agora.rtc2.Constants;
import io.agora.rtc2.video.VideoEncoderConfiguration;


public class ConstantApp {
    private final static String TAG = "IOTSDK/ConstantApp";

    public static final String APP_BUILD_DATE = "today";

    public static final int BASE_VALUE_PERMISSION = 0X0001;
    public static final int PERMISSION_REQ_ID_RECORD_AUDIO = BASE_VALUE_PERMISSION + 1;
    public static final int PERMISSION_REQ_ID_CAMERA = BASE_VALUE_PERMISSION + 2;
    public static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = BASE_VALUE_PERMISSION + 3;

    public static final int MAX_PEER_COUNT = 3;

    public static VideoEncoderConfiguration.VideoDimensions[] VIDEO_DIMENSIONS = new VideoEncoderConfiguration.VideoDimensions[] {
            VideoEncoderConfiguration.VD_160x120,
            VideoEncoderConfiguration.VD_320x180,
            VideoEncoderConfiguration.VD_320x240,
            VideoEncoderConfiguration.VD_640x360,
            VideoEncoderConfiguration.VD_640x480,
            VideoEncoderConfiguration.VD_1280x720
    };

    public static final int DEFAULT_PROFILE_IDX = 4; // default use 480P

    public static class PrefManager {
        public static final String PREF_PROPERTY_PROFILE_IDX = "pref_profile_index";
        public static final String PREF_PROPERTY_UID = "pOCXx_uid";
        public static final String PREF_PROPERTY_UID2 = "pOCXx_uid2";
        public static final String PREF_LOCAL_VIDEO_SOURCE_TYPE = "pref_local_video_source_type";
        public static final String PREF_LOCAL_VIDEO_SINK_TYPE = "pref_local_video_sink_type";
    }

    public static final String ACTION_KEY_CROLE = "C_Role";
    public static final String ACTION_KEY_ROOM_NAME = "ecHANEL";

    public static final int LOCAL_VIDEO_SOURCE_TYPE_FRONT_CAMERA = 0;
    public static final int LOCAL_VIDEO_SOURCE_TYPE_BACK_CAMERA = 1;
    public static final int LOCAL_VIDEO_SOURCE_TYPE_SCREEN_CAPTURE = 2;
    public static final int LOCAL_VIDEO_SOURCE_TYPE_YUV_FRAME = 3;
    public static final int LOCAL_VIDEO_SOURCE_TYPE_ENCODED_FRAME = 4;
    public static final int LOCAL_VIDEO_SOURCE_TYPE_DEFAULT = LOCAL_VIDEO_SOURCE_TYPE_FRONT_CAMERA;

    public static int getLocalVideoSourceType(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        int type = pref.getInt(PrefManager.PREF_LOCAL_VIDEO_SOURCE_TYPE, ConstantApp.LOCAL_VIDEO_SOURCE_TYPE_DEFAULT);
        ALog.getInstance().d(TAG, "<getLocalVideoSourceType> type=" + type);
        return type;
    }

    public static void setLocalVideoSourceType(Context context, int type) {
        int oldType = getLocalVideoSourceType(context);
        if (type == oldType) {
            ALog.getInstance().d(TAG, "<setLocalVideoSourceType> same with old, type=" + type);
            return;
        }
        ALog.getInstance().d(TAG, "<setLocalVideoSourceType> type=" + type);

        switch (type) {
            case LOCAL_VIDEO_SOURCE_TYPE_FRONT_CAMERA:
            case LOCAL_VIDEO_SOURCE_TYPE_BACK_CAMERA:
            case LOCAL_VIDEO_SOURCE_TYPE_SCREEN_CAPTURE:
            case LOCAL_VIDEO_SOURCE_TYPE_YUV_FRAME:
            case LOCAL_VIDEO_SOURCE_TYPE_ENCODED_FRAME:
                break;
            default: throw new IllegalArgumentException("unknown local video source type " + type);
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(PrefManager.PREF_LOCAL_VIDEO_SOURCE_TYPE, type);
        editor.apply();
    }

    public static boolean localVideoSourceIsCamera(Context context) {
        int type = getLocalVideoSourceType(context);
        return type == LOCAL_VIDEO_SOURCE_TYPE_FRONT_CAMERA || type == LOCAL_VIDEO_SOURCE_TYPE_BACK_CAMERA;
    }

    public static boolean localVideoSourceIsScreen(Context context) {
        int type = getLocalVideoSourceType(context);
        return type == LOCAL_VIDEO_SOURCE_TYPE_SCREEN_CAPTURE;
    }

    public static boolean localVideoSourceIsExternal(Context context) {
        int type = getLocalVideoSourceType(context);
        return type == LOCAL_VIDEO_SOURCE_TYPE_YUV_FRAME || type == LOCAL_VIDEO_SOURCE_TYPE_ENCODED_FRAME;
    }

    public static boolean localVideoSourceIsYuv(Context context) {
        int type = getLocalVideoSourceType(context);
        return type == LOCAL_VIDEO_SOURCE_TYPE_YUV_FRAME;
    }

    public static boolean localVideoSourceIsEncoded(Context context) {
        int type = getLocalVideoSourceType(context);
        return type == LOCAL_VIDEO_SOURCE_TYPE_ENCODED_FRAME;
    }

    public static final int LOCAL_VIDEO_SINK_TYPE_DISPLAY = 0;
    public static final int LOCAL_VIDEO_SINK_TYPE_YUV_FRAME_OBSERVER = 1;
    public static final int LOCAL_VIDEO_SINK_TYPE_ENCODED_FRAME_RECEIVER = 2;
    public static final int LOCAL_VIDEO_SINK_TYPE_DEFAULT = LOCAL_VIDEO_SINK_TYPE_DISPLAY;

    public static int getLocalVideoSinkType(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        int type = pref.getInt(PrefManager.PREF_LOCAL_VIDEO_SINK_TYPE, ConstantApp.LOCAL_VIDEO_SINK_TYPE_DEFAULT);
        ALog.getInstance().d(TAG, "<getLocalVideoSinkType> type=" + type);
        return type;
    }

    public static void setLocalVideoSinkType(Context context, int type) {
        int oldType = getLocalVideoSinkType(context);
        if (type == oldType) {
            ALog.getInstance().d(TAG, "<setLocalVideoSinkType> same with old, type=" + type);
            return;
        }
        ALog.getInstance().d(TAG, "<setLocalVideoSinkType> type=" + type);
        switch (type) {
            case LOCAL_VIDEO_SINK_TYPE_DISPLAY:
            case LOCAL_VIDEO_SINK_TYPE_YUV_FRAME_OBSERVER:
            case LOCAL_VIDEO_SINK_TYPE_ENCODED_FRAME_RECEIVER:
                break;
            default: throw new IllegalArgumentException("unknown local video sink type " + type);
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(PrefManager.PREF_LOCAL_VIDEO_SINK_TYPE, type);
        editor.apply();
    }

    public static boolean localVideoSinkIsDisplay(Context context) {
        int type = getLocalVideoSinkType(context);
        return type == LOCAL_VIDEO_SINK_TYPE_DISPLAY;
    }

    public static boolean localVideoSinkIsExternal(Context context) {
        int type = getLocalVideoSinkType(context);
        return type == LOCAL_VIDEO_SINK_TYPE_YUV_FRAME_OBSERVER || type == LOCAL_VIDEO_SINK_TYPE_ENCODED_FRAME_RECEIVER;
    }

    public static boolean localVideoSinkIsYuv(Context context) {
        int type = getLocalVideoSinkType(context);
        return type == LOCAL_VIDEO_SINK_TYPE_YUV_FRAME_OBSERVER;
    }

    public static boolean localVideoSinkIsEncoded(Context context) {
        int type = getLocalVideoSinkType(context);
        return type == LOCAL_VIDEO_SINK_TYPE_ENCODED_FRAME_RECEIVER;
    }
}
