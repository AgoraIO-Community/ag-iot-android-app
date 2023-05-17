/**
 * @file MyEngineEventHandler.java
 * @brief This file implement the event callback
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2021-10-18
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.rtcsdk;

import android.content.Context;
import android.util.Log;

import io.agora.iotlink.logger.ALog;

import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;



public class MyEngineEventHandler {

    public MyEngineEventHandler(Context ctx) {
        this.mContext = ctx;
    }

    private final Context mContext;

    private final ConcurrentHashMap<AGEventHandler, Integer> mEventHandlerList = new ConcurrentHashMap<>();

    public void addEventHandler(AGEventHandler handler) {
        this.mEventHandlerList.put(handler, 0);
    }

    public void removeEventHandler(AGEventHandler handler) {
        this.mEventHandlerList.remove(handler);
    }

    public void release() {
        this.mEventHandlerList.clear();
    }

    final IRtcEngineEventHandler mRtcEventHandler = new CallKitEventHandler("default");
    final IRtcEngineEventHandler mRtcEventHandler2 = new CallKitEventHandler("2nd");

    public class CallKitEventHandler extends IRtcEngineEventHandler {
        private final String TAG = "IOTSDK/CallKitEHandler";
        private final String name;

        public CallKitEventHandler(String s) {
            name = s;
        }

        @Override
        public void onFirstRemoteVideoFrame(int uid, int width, int height, int elapsed) {
            String strLog = String.format(Locale.getDefault(), "<onFirstRemoteVideoFrame> uid=%d, width=%d, height=%d, elapsed=%d",
                    uid, width, height, elapsed);
            ALog.getInstance().d(TAG, strLog);

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onFirstRemoteVideoFrame(uid, width, height, elapsed);
            }
        }

        @Override
        public void onFirstLocalVideoFrame(Constants.VideoSourceType source, int width, int height, int elapsed) {
            String strLog = String.format(Locale.getDefault(),"<onFirstLocalVideoFrame> width=%d, height=%d, elapsed=%d",
                    width, height, elapsed);
            ALog.getInstance().d(TAG, strLog);

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onFirstLocalVideoFrame(width, height, elapsed);
            }
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            String strLog = String.format(Locale.getDefault(), "<onUserJoined> uid=%d, elapsed=%d", uid, elapsed);
            ALog.getInstance().d(TAG, strLog);

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onUserJoined(uid, elapsed);
            }
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            String strLog = String.format(Locale.getDefault(),"<onUserOffline> uid=%d, reason=%d", uid, reason);
            ALog.getInstance().d(TAG, strLog);

            // FIXME this callback may return times
            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onUserOffline(uid, reason);
            }
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            ALog.getInstance().d(TAG, "<onLeaveChannel> stats=" + stats);

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onLeaveChannelSuccess();
            }
        }

        @Override
        public void onLastmileQuality(int quality) {
            ALog.getInstance().d(TAG, "<onLastmileQuality> quality=" + quality);

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onLastmileQuality(quality);
            }
        }


        @Override
        public void onFacePositionChanged(int imageWidth, int imageHeight, AgoraFacePositionInfo[] faceRectArr) {

        }

        @Override
        public void onRequestToken() {
            ALog.getInstance().d(TAG, "<onRequestToken>");

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onRequestToken();
            }
        }


        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            String strLog = String.format(Locale.getDefault(),"<onJoinChannelSuccess> channel=%s, uid=%d, elapsed=%d",
                    channel, uid, elapsed);
            ALog.getInstance().d(TAG, strLog);

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onJoinChannelSuccess(channel, uid, elapsed);
            }
        }


        public void onRejoinChannelSuccess(String channel, int uid, int elapsed) {
            String strLog = String.format(Locale.getDefault(),"<onRejoinChannelSuccess> channel=%s, uid=%d, elapsed=%d",
                    channel, uid, elapsed);
            ALog.getInstance().d(TAG, strLog);
        }

//        public void onWarning(int warn) {
//            Log.d(TAG, "<onWarning> warn=" + warn);
//        }

        public void onLocalVideoStats(IRtcEngineEventHandler.LocalVideoStats stats) {
//            String strLog = String.format(Locale.getDefault(),"<onLocalVideoStats> stats=%s, encodedFrameWidth=%d, encodedFrameHeight=%d",
//                    stats.toString(), stats.encodedFrameWidth, stats.encodedFrameHeight);
//            ALog.getInstance().d(TAG, strLog);

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onLocalVideoStats(stats);
            }
        }

        public void onRtcStats(IRtcEngineEventHandler.RtcStats stats) {
 //           ALog.getInstance().d(TAG, "<onRtcStats> stats=" + stats);

             Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onRtcStats(stats);
            }
        }

        public void onNetworkQuality(int uid, int txQuality, int rxQuality) {
//           String strLog = String.format(Locale.getDefault(),"<onNetworkQuality> channel=%s, uid=%d, muted=%d",
//                    uid, txQuality, rxQuality);
//            ALog.getInstance().d(TAG, strLog);

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onNetworkQuality(uid, txQuality, rxQuality);
            }
        }

        public void onRemoteVideoStats(IRtcEngineEventHandler.RemoteVideoStats stats) {
//            String strLog = String.format(Locale.getDefault(),"<onRemoteVideoStats> stats=%s, width=%d, height=%d",
//                    stats.toString(), stats.width, stats.height);
//            ALog.getInstance().d(TAG, strLog);

            Iterator<AGEventHandler> it = mEventHandlerList.keySet().iterator();
            while (it.hasNext()) {
                AGEventHandler handler = it.next();
                handler.onRemoteVideoStats(stats);
            }
        }

    };

}
