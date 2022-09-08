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


import android.content.Context;
import android.os.Bundle;
import io.agora.iotlink.sdkimpl.AgoraIotAppSdk;


/*
 * @brief SDK引擎接口
 */

public class AIotAppSdkFactory  {

    private static IAgoraIotAppSdk  mSdkInstance = null;

    public static IAgoraIotAppSdk getInstance() {
        if(mSdkInstance == null) {
            synchronized (AgoraIotAppSdk.class) {
                if(mSdkInstance == null) {
                    mSdkInstance = new AgoraIotAppSdk();
                }
            }
        }

        return mSdkInstance;
    }

}
