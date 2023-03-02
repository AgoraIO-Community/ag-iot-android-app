
package io.agora.avmodule;

import android.content.Context;



/*
 * @param 下载参数
 */
public class AvDownloaderParam {


    public Context mContext;                ///< 下载器引擎上下文
    public String mInFileUrl;               ///< 要下载的文件路径
    public String mOutFilePath;             ///< 输出媒体文件全路径
    public IAvDownloaderCallback mCallback; ///< 下载回调接口




    @Override
    public String toString() {
        String strInfo = "{ mInFileUrl=" + mInFileUrl
                + ", mOutFilePath=" + mOutFilePath + " }\n";
        return strInfo;
    }
}