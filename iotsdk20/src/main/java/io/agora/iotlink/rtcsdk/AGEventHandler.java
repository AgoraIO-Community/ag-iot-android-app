/**
 * @file AGEventHandler.java
 * @brief This file define the interface of event
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2021-10-18
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.rtcsdk;


import io.agora.rtc2.IRtcEngineEventHandler;

public interface AGEventHandler {
    void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed);

    void onFirstRemoteVideoFrame(int uid, int width, int height, int elapsed);

    void onFirstLocalVideoFrame(int width, int height, int elapsed);

    void onJoinChannelSuccess(String channel, int uid, int elapsed);

    void onLeaveChannelSuccess();

    void onUserJoined(int uid, int elapsed);

    void onUserOffline(int uid, int reason);

    void onLastmileQuality(int quality);

    void onLocalVideoStats(IRtcEngineEventHandler.LocalVideoStats stats);

    void onRtcStats(IRtcEngineEventHandler.RtcStats stats);

    void onNetworkQuality(int uid, int txQuality, int rxQuality);

    void onRemoteVideoStats(IRtcEngineEventHandler.RemoteVideoStats stats);

    void onRecorderStateChanged(int state, int code);

    void onRecorderInfoUpdate(Object info);

    void onRequestToken();

}
