package io.agora.sdkwayang.constant;

import android.os.Environment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.agora.sdkwayang.util.AppUtil;


public class ConstantApp {
    public static final String APP_BUILD_DATE = "today";

    public static boolean IS_API_ACTION_IN_OTHER_THREAD = true;
    public static final boolean ENABLE_SUPER_GOD_MODE = true;

    public static final String APP_DIRECTORY =  Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/io.agora.wayangdemo/files";

    public static final int BASE_VALUE_PERMISSION = 0X0001;
    public static final int PERMISSION_REQ_ID_RECORD_AUDIO = BASE_VALUE_PERMISSION + 1;
    public static final int PERMISSION_REQ_ID_CAMERA = BASE_VALUE_PERMISSION + 2;
    public static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = BASE_VALUE_PERMISSION + 3;
    public static final int PERMISSION_REQ_ID_READ_EXTERNAL_STORAGE = BASE_VALUE_PERMISSION + 4;


    public static final int DEFAULT_PROFILE_IDX = 2; // default use 240P

    public static class PrefManager {
        public static final String PREF_PROPERTY_PROFILE_IDX = "pref_profile_index";
        public static final String PREF_PROPERTY_UID = "pOCXx_uid";
        public static final String PREF_PROPERTY_USE_DYNAMIC_KEY = "PREF_USE_dynamic_ile";
        public static final String PREF_PROPERTY_CHANNEL_PROFILE = "PREF_channel_profile";
        public static final String PREF_PROPERTY_WEB_INTEROPERABILITY = "PREF_ch_WEB_interop";
    }

    public static final String ACTION_KEY_CROLE = "C_Role";
    public static final String CRASH_FLAG = "C_rash";
    public static final String ACTION_KEY_CHANNEL_NAME = "ecHANEL";
    public static final String ACTION_KEY_ENCRYPTION_KEY = "xdL_encr_key_";
    public static final String ACTION_KEY_ENCRYPTION_MODE = "tOK_edsx_Mode";

    public static class AppError {
        public static final int JOIN_CHANNEL_ERROR = 2;
        public static final int NO_NETWORK_CONNECTION = 3;
        public static final int UNSTABLE_NETWORK_CONNECTION = 4;
        public static final int GET_DYNAMIC_ACCESS_TOKEN_FAILED = 5;
    }

    public static class AppMixingAudio {
        public static final int MIXING_AUDIO_FINISHED = 1;
    }

    public static final int REQUESTED_VIDEO_STREAM_TYPE_NONE = -1;
    public static final int REQUESTED_VIDEO_STREAM_TYPE_AUTO = 0;
    public static final int REQUESTED_VIDEO_STREAM_TYPE_LOW = 1;

    public static class MicEchoTestType {
        public static final String TYPE_DATA_SOURCE_MIC = "Microphone";
        public static final String TYPE_DATA_SOURCE_FILE = "File";
        public static final String TYPE_DATA_SOURCE_LOOPBACK = "LoopBack";
    }

    public static final int LAYOUT_TYPE_STYLE_MATRIX = 1;
    public static final int LAYOUT_TYPE_STYLE_TILE = 2;
    public static final int LAYOUT_TYPE_STYLE_FLOAT = 3;
    public static final int LAYOUT_TYPE_STYLE_SCROLL = 4;
    public static final int LAYOUT_TYPE_STYLE_FULL = 5;


    public static final int MAX_SMALL_VIEW_WIDTH = 150;


    public static final String NORMAL_LOG_FILE = AppUtil.APP_DIRECTORY + File.separator + "log" + File.separator+"normalLogs.log";
    public static final String CRASH_JAVA_LOG_FILE = AppUtil.APP_DIRECTORY + File.separator + "log" + File.separator+"JAVA_crashLogs";
    public static final String ARN_JAVA_LOG_FILE = AppUtil.APP_DIRECTORY + File.separator + "log" + File.separator+"JAVA_anrLogs";
    public static final String CRASH_CPP_DEVICE_LOG_FILE = AppUtil.APP_DIRECTORY + File.separator + "log" + File.separator+"CPP_DEVICE_crashLogs";
    public static final String CRASH_CPP_LOGCAT_LOG_FILE = AppUtil.APP_DIRECTORY + File.separator + "log" + File.separator+"CPP_LOGCAT_crashLogs";
    public static final String CRASH_CPP_TRACE_LOG_FILE = AppUtil.APP_DIRECTORY + File.separator + "log" + File.separator+"CPP_TRACE_crashLogs";
    public static final String CRASH_CPP_BUGREPORT_LOG_FILE = AppUtil.APP_DIRECTORY + File.separator + "log" + File.separator+"CPP_MEMORY_crashLogs";
    public static final String CRASH_CPP_LOG_ZIP = AppUtil.APP_DIRECTORY + File.separator + "log" + File.separator+"CPP_ZIP_crashLogs";
    public static final String CRASH_JAVA_LOG_ZIP = AppUtil.APP_DIRECTORY + File.separator + "log" + File.separator+"JAVA_ZIP_crashLogs";




    public static final int MAX_MESSAGE_COUNT = 200;
    public static final String MSG_SERVER = "Server";
    public static final String MSG_CLIENT= "Client";
    public static final String MSG_HEART_BEAT = "ping";

    public static boolean OFFLINE_MODE = false;
    public static String OFFLINE_FILD_PATH = null;

    public static String TOAST_SERVER_URL_INVALID = "wayang server url can not be null";
    public static String TOAST_DEVICE_ID_INVALID = "device id can not be null";
    public static String TOAST_ON_CONNECT_WAYANG_SERVER_SUCCESS = "wayang server connected success";
    public static String TOAST_ON_CONNECT_WAYANG_SERVER_FAILED = "wayang server connected failed";
    public static String TOAST_ON_CONNECTING_WAYANG_SERVER = "wayang server connecting";

    public static final int WEBSOCKET_RECONNECT_WAITTING_INTERVAL = 4000;
    public static final int OKHTTPCLIENT_READ_TIMEOUT = 3;
    public static final int OKHTTPCLIENT_WRITE_TIMEOUT = 3;
    public static final int OKHTTPCLIENT_CONNECT_TIMEOUT = 3;

    public static String WEBSOCKET_URL_BASE = "ws://114.236.93.153:8083/iov/websocket/dual?topic=";
    public static String DEVICE_NAME_BASE = null;
    public static String WEBSOCKET_URL_SHAREPREFERENCE = "WEBSOCKET_URL_SHAREPREFERENCE";
    public static String DEVICE_NAME_SHAREPREFERENCE = "DEVICE_NAME_SHAREPREFERENCE";
}
