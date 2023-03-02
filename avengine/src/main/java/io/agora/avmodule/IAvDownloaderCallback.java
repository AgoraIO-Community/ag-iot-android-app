package io.agora.avmodule;

import java.util.List;


/*
 * @breief 录像回调接口
 */
public interface IAvDownloaderCallback {

    /**
     * @breief 下载准备完成事件，可以获取到云媒体文件信息
     * @param downloaderParam : 下载参数
     * @param mediaInfo   : 媒体文件信息
     */
    void onDownloaderPrepared(final AvDownloaderParam downloaderParam, final AvMediaInfo mediaInfo);

    /**
     * @breief 整个下载完成
     * @param downloaderParam : 下载参数
     * @return None
     */
    void onDownloaderDone(final AvDownloaderParam downloaderParam);

    /**
     * @breief 下载过程中产生错误，并且不能再继续
     * @param downloaderParam : 下载参数
     * @param errCode      ：转换结果错误代码，0表示正常
     * @return None
     */
    void onDownloaderError(final AvDownloaderParam downloaderParam, int errCode);


}