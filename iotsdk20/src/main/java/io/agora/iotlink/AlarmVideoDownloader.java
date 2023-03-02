/**
 * @file IAlarmMgr.java
 * @brief This file define the interface of alarm management
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink;

import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.util.List;

import io.agora.avmodule.AvCapability;
import io.agora.avmodule.AvDownloaderParam;
import io.agora.avmodule.AvMediaDownloader;
import io.agora.avmodule.AvMediaInfo;
import io.agora.avmodule.AvRecorderParam;
import io.agora.avmodule.IAvDownloaderCallback;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.rtcsdk.TalkingEngine;


/*
 * @brief 告警云录视频下载器
 */
public class AlarmVideoDownloader implements IAvDownloaderCallback {
    private static final String TAG = "IOTSDK/VideoDnloader";
    private static final String DNLOAD_VIDEO_CODEC = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final String DNLOAD_AUDIO_CODEC = MediaFormat.MIMETYPE_AUDIO_AAC;

    /**
     * @brief 告警云录视频下载回调
     */
    public static interface ICallback {

        /**
         * @brief 下载准备完成事件，可以获取云媒体文件信息
         * @param videoUrl : 要下载的云录视频Url，包含签名信息
         * @param mediaInfo : 云媒体文件信息
         */
        default void onDownloadPrepared(final String videoUrl, final AvMediaInfo mediaInfo) {}

        /**
         * @brief 下载完成事件
         * @param videoUrl : 要下载的云录视频Url，包含签名信息
         */
        default void onDownloadDone(final String videoUrl) {}

        /**
         * @brief 下载错误事件，不能再继续
         * @param videoUrl : 要下载的云录视频Url，包含签名信息
         */
        default void onDownloadError(final String videoUrl, int errCode) {}
    }


    /**
     * @brief 定义下载状态
     */
    public static final int STATE_INVALID = 0x0000;      ///< 当前下载还未初始化
    public static final int STATE_PREPARING = 0x0001;    ///< 初始化成功，正在请求云媒体文件信息
    public static final int STATE_ONGOING = 0x0002;      ///< 正常下载转换过程中
    public static final int STATE_PAUSED = 0x0003;       ///< 暂停
    public static final int STATE_DONE = 0x0004;         ///< 转换完成状态
    public static final int STATE_ERROR = 0x0005;        ///< 错误状态,不能再继续

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private AvDownloaderParam mDownloaderParam = null;
    private AvMediaDownloader   mVideoDownloader = null;
    private String mCloudVideoUrl;
    private String mLocalFilePath;
    private ICallback mCallback;


    ////////////////////////////////////////////////////////////////////////
    //////////////////////////// Public Methods ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 启动下载流程
     * @param cloudVideoUrl : 要下载的云视频文件路径
     * @param localFilePath : 下载到本地保存的文件路径
     * @param callback : 下载回调
     * @return 错误码
     */
    public int start(final String cloudVideoUrl, final String localFilePath, ICallback callback) {
        int ret;

        mCloudVideoUrl = cloudVideoUrl;
        mLocalFilePath = localFilePath;
        mCallback = callback;

        deleteFile(mLocalFilePath);

        mDownloaderParam = new AvDownloaderParam();
        mDownloaderParam.mContext = null;
        mDownloaderParam.mInFileUrl = cloudVideoUrl;
        mDownloaderParam.mOutFilePath = localFilePath;
        mDownloaderParam.mCallback = this;

        //
        // 创建并且初始化下载器
        //
        mVideoDownloader = new AvMediaDownloader();
        ret = mVideoDownloader.initialize(mDownloaderParam);
        if (ret != ErrCode.XOK) {
            Log.e(TAG, "<start> fail to initialize downloader");
            return ret;
        }

        Log.d(TAG, "<start> done, ret=" + ret);
        return ret;
    }

    /**
     * @brief 停止当前下载流程
     * @return 错误码
     */
    public int stop() {
        if (mVideoDownloader != null) {
            int ret = mVideoDownloader.release();
            mVideoDownloader = null;
            Log.d(TAG, "<stop> done, ret=" + ret);
        }
        return ErrCode.XOK;
    }

    /**
     * @brief 获取当前状态
     * @return 返回状态机
     */
    public int getState() {
        if (mVideoDownloader == null) {
            return STATE_INVALID;
        }
        return mVideoDownloader.getState();
    }


    /**
     * @brief 暂停当前下载流程
     * @return 错误码
     */
    public int pause() {
        if (mVideoDownloader == null) {
            return ErrCode.XERR_BAD_STATE;
        }

        int ret = mVideoDownloader.downloadPause();
        Log.d(TAG, "<pause> done, ret=" + ret);
        return ret;
    }

    /**
     * @brief 恢复当前下载流程，仅在暂停时可以调用
     * @return 错误码
     */
    public int resume() {
        if (mVideoDownloader == null) {
            return ErrCode.XERR_BAD_STATE;
        }

        int ret = mVideoDownloader.downloadResume();
        Log.d(TAG, "<resume> done, ret=" + ret);
        return ret;
    }

    /**
     * @brief 获取当前视频编码进度
     * @return 当前视频帧时间戳
     */
    public long getVideoTimestamp() {
        if (mVideoDownloader == null) {
            return 0;
        }

        return mVideoDownloader.getVideoTimestamp();
    }

    /**
     * @brief 获取当前音频编码进度
     * @return 当前视频帧时间戳
     */
    public long getAudioTimestamp() {
        if (mVideoDownloader == null) {
            return 0;
        }

        return mVideoDownloader.getAudioTimestamp();
    }

    ///////////////////////////////////////////////////////////////////////////////
    //////////////////////// Override Methods of IAvDownloaderCallback ////////////
    ///////////////////////////////////////////////////////////////////////////////
    @Override
    public void onDownloaderPrepared(final AvDownloaderParam downloaderParam, final AvMediaInfo mediaInfo) {
        Log.d(TAG, "<onDownloaderPrepared> mediaInfo=" + mediaInfo.toString());
        if (mCallback != null) {
            mCallback.onDownloadPrepared(mCloudVideoUrl, mediaInfo);
        }
    }

    @Override
    public void onDownloaderDone(final AvDownloaderParam downloaderParam) {
        Log.d(TAG, "<onDownloaderDone> ");

        if (mCallback != null) {
            mCallback.onDownloadDone(mCloudVideoUrl);
        }
    }

    @Override
    public void onDownloaderError(final AvDownloaderParam downloaderParam, int errCode) {
        Log.d(TAG, "<onDownloaderError> errCode=" + errCode);
        if (mCallback != null) {
            mCallback.onDownloadError(mCloudVideoUrl, errCode);
        }
    }



    ////////////////////////////////////////////////////////////////////////////
    //////////////////////////// Internal Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    boolean deleteFile(final String filePath)  {
        File delFile = new File(filePath);
        boolean ret = true;
        if (delFile.exists()) {
            ret = delFile.delete();
        }

        Log.d(TAG, "<deleteFile> file=" + filePath + ", ret=" + ret);
        return ret;
    }
}
