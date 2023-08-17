/**
 * @file IAgoraIotAppSdk.java
 * @brief This file define the SDK interface for Agora Iot AppSdk 2.0
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink;


import io.agora.iotlink.sdkimpl.AgoraIotAppSdk;
import io.agora.sdkwayang.SdkWayangComp;


/*
 * @brief Wayang接口
 */

public class SdkWayangFactory  {

    private static SdkWayangComp mWayangInstance = null;

    public static SdkWayangComp getInstance() {
        if(mWayangInstance == null) {
            synchronized (AgoraIotAppSdk.class) {
                if(mWayangInstance == null) {
                    mWayangInstance = new SdkWayangComp();
                }
            }
        }

        return mWayangInstance;
    }

}
