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
            case PUBLIC_STREAM_1:
                streamName = "PUBLIC_STREAM_1";
                break;
            case PUBLIC_STREAM_2:
                streamName = "PUBLIC_STREAM_2";
                break;
            case PUBLIC_STREAM_3:
                streamName = "PUBLIC_STREAM_3";
                break;
            case PUBLIC_STREAM_4:
                streamName = "PUBLIC_STREAM_4";
                break;
            case PUBLIC_STREAM_5:
                streamName = "PUBLIC_STREAM_5";
                break;
            case PUBLIC_STREAM_6:
                streamName = "PUBLIC_STREAM_6";
                break;
            case PUBLIC_STREAM_7:
                streamName = "PUBLIC_STREAM_7";
                break;
            case PUBLIC_STREAM_8:
                streamName = "PUBLIC_STREAM_8";
                break;
            case PUBLIC_STREAM_9:
                streamName = "PUBLIC_STREAM_9";
                break;

            case PRIVATE_STREAM_1:
                streamName = "PRIVATE_STREAM_1";
                break;
            case PRIVATE_STREAM_2:
                streamName = "PRIVATE_STREAM_2";
                break;
            case PRIVATE_STREAM_3:
                streamName = "PRIVATE_STREAM_3";
                break;
            case PRIVATE_STREAM_4:
                streamName = "PRIVATE_STREAM_4";
                break;
            case PRIVATE_STREAM_5:
                streamName = "PRIVATE_STREAM_5";
                break;
            case PRIVATE_STREAM_6:
                streamName = "PRIVATE_STREAM_6";
                break;
            case PRIVATE_STREAM_7:
                streamName = "PRIVATE_STREAM_7";
                break;
            case PRIVATE_STREAM_8:
                streamName = "PRIVATE_STREAM_8";
                break;
            case PRIVATE_STREAM_9:
                streamName = "PRIVATE_STREAM_9";
                break;
        }

        return streamName;

    }

}
