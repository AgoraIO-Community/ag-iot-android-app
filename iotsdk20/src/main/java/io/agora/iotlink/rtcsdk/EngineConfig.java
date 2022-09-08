/**
 * @file EngineConfig.java
 * @brief This file define the global configuration of RTC engine
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2021-10-18
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.rtcsdk;

import io.agora.rtc2.RtcConnection;
import io.agora.rtc2.video.VideoEncoderConfiguration;

public class EngineConfig {
    public int mClientRole;

    public VideoEncoderConfiguration.VideoDimensions mVideoDimension;;

    public int mUid;

    public int mUid2;

    public String mChannel;

    public String mChannel2;

    public void reset() {
        mChannel = null;
    }

    EngineConfig() {
    }
}
