/**
 * @file EncodedVideoFrame.java
 * @brief This file implement read video frame from local resource file
 *
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2021-10-18
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.rtcsdk;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import io.agora.rtc2.Constants;
import io.agora.rtc2.video.EncodedVideoFrameInfo;


public class EncodedVideoFrame {
    private final static String TAG = "IOTSDK/EncVideoFrame";

    public static final String ENCODED_IMAGE_FILE_NAME = "stefan.h264";
    public static final int ENCODED_IMAGE_GOP_FRAME_COUNT = 10;
    public static final int ENCODED_IMAGE_WDITH = 352;
    public static final int ENCODED_IMAGE_HEIGHT = 288;
    public static final int ENCODED_FRAME_TOTAL = 90;

    private static final int[] encodedFrameSize = {
        17418, 4569, 4638, 3680, 3628, 3768, 4305, 4305, 4602, 4705,
        20574, 4633, 4931, 4650, 4940, 5279, 5070, 5009, 5307, 5248,
        24628, 5334, 4798, 5116, 3939, 3053, 3261, 4113, 2416, 1638,
        24223, 3163, 4304, 4802, 3843, 5095, 4467, 4836, 4935, 4812,
        25064, 5511, 4821, 4431, 3747, 3350, 2975, 4159, 4742, 4628,
        24824, 2930, 3481, 3151, 3247, 3466, 3408, 3479, 3631, 4227,
        24486, 3660, 4482, 4180, 4203, 4366, 4393, 4496, 4569, 4578,
        24573, 3481, 4359, 4428, 4374, 4479, 4531, 4353, 4403, 4562,
        24598, 4077, 4231, 3927, 4382, 4255, 4182, 4129, 3275, 2344,
    };
    private static int[] encodedFrameOffset = new int[ENCODED_FRAME_TOTAL];
    private static ByteBuffer[] encodedFrame = new ByteBuffer[ENCODED_FRAME_TOTAL];
    private static int encodedFrameMaxSize;
    private static byte[] tempBuffer;

    static {
        int offset = 0;
        encodedFrameMaxSize = 0;
        for (int i = 0; i < encodedFrameOffset.length; i++) {
            encodedFrameOffset[i] = offset;
            offset += encodedFrameSize[i];
            if (encodedFrameSize[i] > encodedFrameMaxSize) encodedFrameMaxSize = encodedFrameSize[i];
        }
        tempBuffer = new byte[encodedFrameMaxSize];
    }

    private int currentFrameIndex;
    private InputStream is;
    private Context ctx;

    public EncodedVideoFrame(Context ctx) {
        this.ctx = ctx;
        currentFrameIndex = 0;
    }

    public int init() {
        if (is != null) {
            Log.d(TAG, "init:don't init repeatly!");
            return 0;
        }
        Resources resources = ctx.getResources();
        AssetManager assetManager = resources.getAssets();
        try {
            is = assetManager.open(ENCODED_IMAGE_FILE_NAME);
        } catch (IOException e) {
            Log.e(TAG, "init:e={" + e.toString() + "}");
            return -1;
        }
        return 0;
    }

    public int deinit() {
        if (is != null) {
            try {
                is.close();
            } catch (Exception a) {
            }
            is = null;
        }
        return 0;
    }

    public ByteBuffer nextFrame(EncodedVideoFrameInfo info) {
        if (is == null) {
            Log.e(TAG, "<nextFrame> file not opened!");
            return null;
        }
        ByteBuffer data;
        if (encodedFrame[currentFrameIndex] == null) {
            data = ByteBuffer.allocateDirect(encodedFrameSize[currentFrameIndex]);
            try {
                int len = is.read(tempBuffer, 0, data.capacity());
                data.put(tempBuffer, 0, data.capacity());
            } catch (IOException e) {
                Log.e(TAG, "<nextFrame> currentFrameIndex=" + currentFrameIndex
                        + "error=" + e);
                deinit();
                return null;
            }
            encodedFrame[currentFrameIndex] = data;
        } else {
            data = encodedFrame[currentFrameIndex];
            Log.d(TAG, "<nextFrame> frame has been readed, currentFrameIndex=" + currentFrameIndex);
        }
        if (currentFrameIndex % ENCODED_IMAGE_GOP_FRAME_COUNT == 0) {
            info.frameType = Constants.VIDEO_FRAME_TYPE_KEY_FRAME;
        } else {
            info.frameType = Constants.VIDEO_FRAME_TYPE_DELTA_FRAME;
        }
        ++currentFrameIndex;
        if (currentFrameIndex >= ENCODED_FRAME_TOTAL) {
            currentFrameIndex -= ENCODED_FRAME_TOTAL;
        }
        return data;
    }
}
