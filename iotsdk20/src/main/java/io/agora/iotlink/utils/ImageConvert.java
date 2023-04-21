/**
 * @file ImageConvert.java
 * @brief 进行图像色彩格式转换的类
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2021-11-09
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.utils;


import android.graphics.*;

import java.nio.ByteBuffer;


public class ImageConvert
{
    public static final String TAG = "IOTSDK/ImageConvert";

    private static ImageConvert instance = null;

    static {
        System.loadLibrary("ImgCvt");
    }


    ///////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////// Public Interface ////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////

    public static ImageConvert getInstance() {
        if(instance == null) {
            synchronized (ImageConvert.class) {
                if(instance == null) {
                    instance = new ImageConvert();
                }
            }
        }
        return instance;
    }


    /*
     * @brief 将I420图像数据转换成Bitmap对象
     * @param yData, uData, vData 分别是 Y U V 数据
     * @param width, height 是图像大小
     * @param outBmp 是输出的图像数据，调用该函数前需要先分配好对象
     * @return error code
     */
    public int YuvToI420(
        final ByteBuffer yBuffer,
        final ByteBuffer uBuffer,
        final ByteBuffer vBuffer,
        int width,
        int height,
        byte[] outYbytes,
        byte[] outUbytes,
        byte[] outVbytes        )
    {
        int ret = ImgCvt_YuvToI420(yBuffer, uBuffer, vBuffer, width, height,
                                    outYbytes, outUbytes, outVbytes);
        return ret;
    }

    /*
     * @brief 将I420图像数据转换成Bitmap对象
     * @param yData, uData, vData 分别是 Y U V 数据
     * @param width, height 是图像大小
     * @param outBmp 是输出的图像数据，调用该函数前需要先分配好对象
     * @return error code
     */
    public int I420ToRgba(byte[] yData, byte[] uData, byte[] vData,
                          int width, int height, Bitmap outBmp)
    {
        int ret = ImgCvt_I420ToRgba(yData, uData, vData, width, height, outBmp);
        return ret;
    }



    static public Bitmap rotateBmp(Bitmap srcBmp, long lOrientation)
    {
        float targetX, targetY;
        int   dstWidth, dstHeight;
        Matrix mtx = new Matrix();

        if (90 == lOrientation || 270 == lOrientation)
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
        if (90 == lOrientation)
        {
            mtx.setRotate(lOrientation, (float) srcBmp.getWidth() / 2, (float) srcBmp.getHeight() / 2);
            final float[] values = new float[9];
            mtx.getValues(values);
            float x1 = values[Matrix.MTRANS_X];
            float y1 = values[Matrix.MTRANS_Y];
            mtx.postTranslate(srcBmp.getHeight() - x1, -y1);
        }
        else if (180 == lOrientation)
        {
            mtx.setRotate(lOrientation, (float) srcBmp.getWidth() / 2, (float) srcBmp.getHeight() / 2);
        }
        else if (270 == lOrientation)
        {
            mtx.setRotate(lOrientation, (float) srcBmp.getWidth() / 2, (float) srcBmp.getHeight() / 2);
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


    /**************************************************************/
    /********************  Native JNI Define *********************/
    /*************************************************************/
    public native int ImgCvt_I420ToRgba(byte[] yData, byte[] uData, byte[] vData,
                                             int width, int height, Object outBmp);

    public native int ImgCvt_YuvToNv12(byte[] yData, byte[] uData, byte[] vData,
                                        int width, int height, byte[] nv12Buffer);

    public native int ImgCvt_YuvToI420(Object yBuffer, Object uBuffer, Object vBuffer,
                                       int width, int height,
                                       byte[] dstYbytes, byte[] dstUbytes, byte[] dstVbytes);
}
