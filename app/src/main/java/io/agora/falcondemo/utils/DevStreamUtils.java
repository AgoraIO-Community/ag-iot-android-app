package io.agora.falcondemo.utils;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.agora.baselibrary.base.BaseApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.agora.iotlink.IConnectionObj;

/**
 * DevStream 工具类
 */
public class DevStreamUtils {

    public static String getStreamName(final IConnectionObj.STREAM_ID streamId) {
        String streamName = "UNKOWN_STREAM";

        switch (streamId) {
            case BROADCAST_STREAM_1:
                streamName = "BROADCAST_STREAM_1";
                break;
            case BROADCAST_STREAM_2:
                streamName = "BROADCAST_STREAM_2";
                break;
            case BROADCAST_STREAM_3:
                streamName = "BROADCAST_STREAM_3";
                break;
            case BROADCAST_STREAM_4:
                streamName = "BROADCAST_STREAM_4";
                break;
            case BROADCAST_STREAM_5:
                streamName = "BROADCAST_STREAM_5";
                break;
            case BROADCAST_STREAM_6:
                streamName = "BROADCAST_STREAM_6";
                break;
            case BROADCAST_STREAM_7:
                streamName = "BROADCAST_STREAM_7";
                break;
            case BROADCAST_STREAM_8:
                streamName = "BROADCAST_STREAM_8";
                break;
            case BROADCAST_STREAM_9:
                streamName = "BROADCAST_STREAM_9";
                break;

            case UNICAST_STREAM_1:
                streamName = "UNICAST_STREAM_1";
                break;
            case UNICAST_STREAM_2:
                streamName = "UNICAST_STREAM_2";
                break;
            case UNICAST_STREAM_3:
                streamName = "UNICAST_STREAM_3";
                break;
            case UNICAST_STREAM_4:
                streamName = "UNICAST_STREAM_4";
                break;
            case UNICAST_STREAM_5:
                streamName = "UNICAST_STREAM_5";
                break;
            case UNICAST_STREAM_6:
                streamName = "UNICAST_STREAM_6";
                break;
            case UNICAST_STREAM_7:
                streamName = "UNICAST_STREAM_7";
                break;
            case UNICAST_STREAM_8:
                streamName = "UNICAST_STREAM_8";
                break;
            case UNICAST_STREAM_9:
                streamName = "UNICAST_STREAM_9";
                break;
        }

        return streamName;

    }

}
