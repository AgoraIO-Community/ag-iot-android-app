package io.agora.avmodule;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


public class AvSoftDecoder {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "AVMODULE/SoftDecoder";


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////

    AvMediaInfo mMediaInfo = new AvMediaInfo();
    private AvDecParam mDecParam;        ///< 编解码器初始化参数
    private long mParserHandler = 0;

    static {
        System.loadLibrary("SoftDecoder");
    }


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Utility Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public AvMediaInfo distillMediaInfo(String filePath) {
        if (filePath == null) {
            Log.e(TAG, "<distillMediaInfo> [ERROR] invalid parameter");
            return null;
        }

        AvMediaInfo mediaInfo = new AvMediaInfo();
        int ret = native_distillMediaInfo(filePath, mediaInfo);
        Log.d(TAG, "<distillMediaInfo> done, ret=" + ret
                + ", mediaInfo=" + mediaInfo.toString());
        return mediaInfo;
    }



    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 打开要解析的文件
     */
    public int parserOpen(AvDecParam decParam) {
        if (mParserHandler != 0) {
            return ErrCode.XERR_BAD_STATE;
        }

        mParserHandler = native_parserOpen(decParam);
        if (mParserHandler == 0) {
            Log.e(TAG, "<parserOpen> [ERROR] fail to open file=" + decParam.mInFileUrl);
            return ErrCode.XERR_FILE_OPEN;
        }
        int ret = native_getInMediaInfo(mParserHandler, mMediaInfo);

        Log.d(TAG, "<parserOpen> done, decParam=" + decParam.toString()
                + ", mParserHandler=" + mParserHandler + ", ret=" + ret);
        return ret;
    }

    /**
     * @brief 打开要解析的文件
     */
    public int parserClose() {
        if (mParserHandler == 0) {
            return ErrCode.XOK;
        }

        native_parserClose(mParserHandler);
        mParserHandler = 0;
        Log.d(TAG, "<parserClose> done");
        return ErrCode.XOK;
    }

    /**
     * @brief 获取解析后的文件信息
     */
    public AvMediaInfo parserGetMediaInfo() {
        return mMediaInfo;
    }

    /**
     * @brief 驱动解码器送入一个数据包进行处理
     * @param outPktType : outPktType[0] 输出送入的包类型，
     *                   1--送入了视频包； 2--送入了音频包； other--没有数据包送入
     */
    public int parserInputPacket(int[] outPktType) {
        if (mParserHandler == 0) {
            Log.e(TAG, "<parserInputPacket> [ERROR] bad state");
            return ErrCode.XERR_BAD_STATE;
        }

        int ret = native_parserInputPacket(mParserHandler, outPktType);
//        Log.d(TAG, "<parserInputPacket> done, ret=" + ret + ", pktType=" + outPktType[0]);
        return ret;
    }

    /**
     * @brief 解码一帧视频帧
     * @return Integer : 错误码，XERR_CODEC_DECODING 表示解码失败，并且不能继续
     *                         XERR_CODEC_DEC_EOS 表示所有的解码都已经完成
     *         AvVideoFrame : 如果解码成功，返回解码输出的视频帧数据
     */
    public Pair<Integer, AvVideoFrame> parserDecVideoFrame() {
        if (mParserHandler == 0) {
            Log.e(TAG, "<parserDecVideoFrame> [ERROR] bad state");
            return new Pair(ErrCode.XERR_BAD_STATE, null);
        }

        AvVideoFrame videoFrame = new AvVideoFrame();
        int retDec = native_parserDecVideoFrame(mParserHandler, videoFrame);
        if (videoFrame.mLastFrame) {
            Log.d(TAG, "<parserDecVideoFrame> done, last frame, retDec=" + retDec
                    + ", timestamp=" + videoFrame.mTimestamp
                    + ", width=" + videoFrame.mWidth + ", height=" + videoFrame.mHeight
                    + ", flags=" + videoFrame.mFlags );
            return  new Pair(retDec, videoFrame);
        }
        if (videoFrame.mWidth <= 0 || videoFrame.mHeight <= 0) {
            return  new Pair(retDec, null);
        }

        int dataSize = (videoFrame.mWidth * videoFrame.mHeight * 3 / 2);
        videoFrame.mDataBuffer = new byte[dataSize];
        int retGet = native_parserGetVideoFrame(mParserHandler, videoFrame.mDataBuffer);
//        Log.d(TAG, "<parserDecVideoFrame> done, retGet=" + retGet
//                + ", timestamp=" + videoFrame.mTimestamp
//                + ", width=" + videoFrame.mWidth + ", height=" + videoFrame.mHeight
//                + ", flags=" + videoFrame.mFlags + ", lastFrame=" + videoFrame.mLastFrame );
        return (new Pair(retGet, videoFrame));
    }

    /**
     * @brief 解码一帧视频帧
     * @return Integer : 错误码，XERR_CODEC_DECODING 表示解码失败，并且不能继续
     *                         XERR_CODEC_DEC_EOS 表示所有的解码都已经完成
     *         AvVideoFrame : 如果解码成功，返回解码输出的视频帧数据
     */
    public Pair<Integer, AvAudioFrame> parserDecAudioFrame() {
        if (mParserHandler == 0) {
            Log.e(TAG, "<parserDecAudioFrame> [ERROR] bad state");
            return new Pair(ErrCode.XERR_BAD_STATE, null);
        }

        AvAudioFrame audioFrame = new AvAudioFrame();
        int retDec = native_parserDecAudioFrame(mParserHandler, audioFrame);
        if (audioFrame.mLastFrame) {
            Log.d(TAG, "<parserDecAudioFrame> done, last frame, retDec=" + retDec
                    + ", timestamp=" + audioFrame.mTimestamp
                    + ", sampleFormat=" + audioFrame.mSampleFormat
                    + ", channels=" + audioFrame.mChannels
                    + ", sampleRate=" + audioFrame.mSampleRate
                    + ", flags=" + audioFrame.mFlags );
            return  new Pair(retDec, audioFrame);
        }
        if (audioFrame.mSampleNumber <= 0) {
            return  new Pair(retDec, null);
        }

        int dataSize = (audioFrame.mBytesPerSample * audioFrame.mChannels * audioFrame.mSampleNumber);
        audioFrame.mDataBuffer = new byte[dataSize];
        int retGet = native_parserGetAudioFrame(mParserHandler, audioFrame.mDataBuffer);
//        Log.d(TAG, "<parserDecAudioFrame> done, ret=" + retGet
//                + ", timestamp=" + audioFrame.mTimestamp
//                + ", sampleFormat=" + audioFrame.mSampleFormat
//                + ", channels=" + audioFrame.mChannels
//                + ", sampleRate=" + audioFrame.mSampleRate
//                + ", flags=" + audioFrame.mFlags
//                + ", lastFrame=" + audioFrame.mLastFrame);
        return (new Pair(retGet, audioFrame));
    }




    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Image Engine Methods //////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief 将YUV图像数据转换成Bitmap对象
     * @param yuvData : 输入的 Y U V 数据
     * @param yuvFormat : yuv像素格式
     * @param width, height : 图像大小
     * @param outBmp 是输出的图像数据，调用该函数前需要先分配好对象
     * @return 错误码
     */
    public int yuvToBitmap(byte[] yuvData, int yuvFormat, int width, int height, int stride, Bitmap outBmp)
    {
        int ret = native_yuvToBitmap(yuvData, yuvFormat, width, height, stride, outBmp);
        return ret;
    }

    /*
     * @brief 将Bitmap对象转换成YUV数据
     * @param inBmp 是输入的图像数据，调用该函数前需要先分配好对象
     * @param outYuvFmt : 输出yuv像素格式
     * @param outYuvBuffer : 输出的yuv数据缓冲区
     * @return 错误码
     */
    public int bitmapToYuv(Bitmap inBmp, int outYuvFmt, byte[] outYuvBuffer)
    {
        int ret = native_bitmapToYuv(inBmp, outYuvFmt, outYuvBuffer);
        return ret;
    }


    /*
     * @brief 进行图像旋转操作
     * @param srcBmp : 原始输入图像
     * @param rotateAngle : 旋转角度
     * @return 返回旋转后的图像
     */
    static public Bitmap rotateBmp(Bitmap srcBmp, int rotateAngle)
    {
        float targetX, targetY;
        int   dstWidth, dstHeight;
        Matrix mtx = new Matrix();

        if (90 == rotateAngle || 270 == rotateAngle)
        {
            dstWidth = srcBmp.getHeight();
            dstHeight= srcBmp.getWidth();
        }
        else
        {
            dstWidth = srcBmp.getWidth();
            dstHeight= srcBmp.getHeight();
        }

        Bitmap dstBmp    = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888);
        Canvas dstCanvas = new Canvas(dstBmp);
        mtx.reset();
        if (90 == rotateAngle)
        {
            mtx.setRotate(rotateAngle, (float) srcBmp.getWidth() / 2, (float) srcBmp.getHeight() / 2);
            final float[] values = new float[9];
            mtx.getValues(values);
            float x1 = values[Matrix.MTRANS_X];
            float y1 = values[Matrix.MTRANS_Y];
            mtx.postTranslate(srcBmp.getHeight() - x1, -y1);
        }
        else if (180 == rotateAngle)
        {
            mtx.setRotate(rotateAngle, (float) srcBmp.getWidth() / 2, (float) srcBmp.getHeight() / 2);
        }
        else if (270 == rotateAngle)
        {
            mtx.setRotate(rotateAngle, (float) srcBmp.getWidth() / 2, (float) srcBmp.getHeight() / 2);
            final float[] values = new float[9];
            mtx.getValues(values);
            float x1 = values[Matrix.MTRANS_X];
            float y1 = values[Matrix.MTRANS_Y];
            mtx.postTranslate(-x1, srcBmp.getWidth() - y1);
        }
        else
        {
        }
        dstCanvas.drawBitmap(srcBmp, mtx, null);
        dstCanvas = null;

        return dstBmp;
    }

    /*
     * @brief save bitmap to local file
     */
    public boolean saveBmpToFile(Bitmap bmp, String fileName)
    {
        File f = new File(fileName);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "<saveBmpToFile> file not found: " + fileName);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "<saveBmpToFile> IO exception");
            return false;
        }

        return true;
    }


    /*
     * @brief save bitmap to local file
     */
    public boolean saveBmpToBmpFile(Bitmap bmp, String fileName)
    {
        int ret = native_bmpSaveToFile(bmp, fileName);
        return (ret == 0) ? true : false;
    }



    /**************************************************************/
    /********************  Native JNI Define *********************/
    /*************************************************************/
    public native int native_distillMediaInfo(String filePath, AvMediaInfo outMediaInfo);

    public native long native_parserOpen(AvDecParam decParam);
    public native int native_parserClose(long hParser);
    public native int native_getInMediaInfo(long hParser, AvMediaInfo outMediaInfo);
    public native int native_parserInputPacket(long hParser, int[] trackId);
    public native int native_parserDecVideoFrame(long hParser, AvVideoFrame outVideoFrame);
    public native int native_parserGetVideoFrame(long hParser, byte[] videoFrame);
    public native int native_parserDecAudioFrame(long hDecoder, AvAudioFrame outAudioFrame);
    public native int native_parserGetAudioFrame(long hParser, byte[] audioFrame);

    public native int native_yuvToBitmap(byte[] yuvData, int yuvFormat, int width, int height, int stride, Object outBmp);
    public native int native_bitmapToYuv(Object inBmp, int outYuvFmt, byte[] outYuvBuffer);

    public native int native_bmpSaveToFile(Object inBmp, String outFile);

}