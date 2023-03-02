#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <android/log.h>
#include "ImgCvt_jni.h"


#define  LOG_TAG    "ImgCvt"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

//////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////// JNI Interface Implementation ///////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////

/*
 * Class:     com_agora_agoracallkit_utils_ImageConvert
 * Method:    ImgCvt_I420ToRgba
 * Signature: ([B[B[BIILjava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_io_agora_iotlink_utils_ImageConvert_ImgCvt_1I420ToRgba (
    JNIEnv*			env,
    jobject 		thiz,
    jbyteArray 		jb_yData,
    jbyteArray 		jb_uData,
    jbyteArray		jb_vData,
    jint 	        width,
    jint			height,
    jobject			jobj_outBmp			)
{
	AndroidBitmapInfo bmpInfo = { 0 };
	uint8_t*	rgbaBuffer = NULL;

//    LOGI("<ImgCvt_1I420ToRgba> ==>Enter");

    // get information from output bitmap object
	int res = AndroidBitmap_getInfo(env, jobj_outBmp, &bmpInfo);
	if (ANDROID_BITMAP_RESULT_SUCCESS != res)
	{
		LOGE("<ImgCvt_1I420ToRgba> fail to get out bitmap info, res=0x%x", res);
		AndroidBitmap_unlockPixels(env, jobj_outBmp);
		return -1;
	}
	if (ANDROID_BITMAP_FORMAT_RGBA_8888 != bmpInfo.format)
	{
		LOGE("<ImgCvt_1I420ToRgba> unsupported out bitmap format");
		AndroidBitmap_unlockPixels(env, jobj_outBmp);
		return -2;
	}

	// Lock bitmap buffer
	AndroidBitmap_lockPixels(env, jobj_outBmp, (void**) &rgbaBuffer);
	if (NULL == rgbaBuffer)
	{
		LOGE("<ImgCvt_1I420ToRgba> fail to lock out bitmap pixels");
        AndroidBitmap_unlockPixels(env, jobj_outBmp);
		return -3;
	}

	// Lock YUV buffer
    jbyte* pYBuffer =  env->GetByteArrayElements(jb_yData, 0);
    jbyte* pUBuffer =  env->GetByteArrayElements(jb_uData, 0);
    jbyte* pVBuffer =  env->GetByteArrayElements(jb_vData, 0);
    if (NULL == pYBuffer || NULL == pUBuffer || NULL == pVBuffer) {
        LOGE("<ImgCvt_1I420ToRgba> fail to lock YUV data");
        AndroidBitmap_unlockPixels(env, jobj_outBmp);
        return -4;
    }


    //
    // Converting
    //
    uint8_t* pRgba = (uint8_t*)rgbaBuffer;
    uint8_t* pYData= (uint8_t*)pYBuffer;
    uint8_t* pUData= (uint8_t*)pUBuffer;
    uint8_t* pVData= (uint8_t*)pVBuffer;
    int strideY  = width;
    int strideUV = (width / 2);
    int i, j;
    for (i = 0; i < height; i++)
    {
        int startY = i*strideY;
        int startUV = (i/2) * strideUV;

        for (j = 0; j < width; j++)
        {
            int posY = startY + j;
            int posUV = startUV + (j/2);
            int Y = pYData[posY];
            int U = pUData[posUV];
            int V = pVData[posUV];

            int R = (298*Y + 411*V - 57344) >> 8;
            int G = (298*Y - 101*U - 211*V + 34739) >> 8;
            int B = (298*Y + 519*U- 71117) >> 8;

            if (B < 0)      B = 0;
            if (B > 255)    B = 255;
            if (G < 0)      G = 0;
            if (G > 255)    G = 255;
            if (R < 0)      R = 0;
            if (R > 255)    R = 255;

            pRgba[0] = (uint8_t)R;
            pRgba[1] = (uint8_t)G;
            pRgba[2] = (uint8_t)B;
            pRgba[3] = 0xFF;

            pRgba += 4;
        }
    }

    // Unlock bitmap buffer
    AndroidBitmap_unlockPixels(env, jobj_outBmp);

    // Unlock YUV buffer
    env->ReleaseByteArrayElements(jb_yData, pYBuffer, 0);
    env->ReleaseByteArrayElements(jb_uData, pUBuffer, 0);
    env->ReleaseByteArrayElements(jb_vData, pVBuffer, 0);

    //LOGI("<ImgCvt_1I420ToRgba> <==Exit, width=%d, height=%d", width, height);
    return 0;
}


/*
 * Class:     io_agora_iotlink_utils_ImageConvert
 * Method:    ImgCvt_I420ToRgba
 * Signature: ([B[B[BIILjava/lang/[B;)I
 */
JNIEXPORT jint JNICALL Java_io_agora_iotlink_utils_ImageConvert_ImgCvt_1YuvToNv12(
    JNIEnv*			env,
    jobject thiz,
    jbyteArray 		jb_yData,
    jbyteArray 		jb_uData,
    jbyteArray		jb_vData,
    jint 	        width,
    jint			height,
    jbyteArray		jb_nv21Buffer   )
{
    AndroidBitmapInfo bmpInfo = { 0 };
    uint8_t*	rgbaBuffer = NULL;

//    LOGI("<ImgCvt_1I420ToRgba> ==>Enter");



    // Lock YUV buffer
    jbyte* pYBuffer =  env->GetByteArrayElements(jb_yData, 0);
    jbyte* pUBuffer =  env->GetByteArrayElements(jb_uData, 0);
    jbyte* pVBuffer =  env->GetByteArrayElements(jb_vData, 0);
    jbyte* pNv12Buffer = env->GetByteArrayElements(jb_nv21Buffer, 0);
    if (NULL == pYBuffer || NULL == pUBuffer || NULL == pVBuffer || pNv12Buffer == NULL) {
        LOGE("<ImgCvt_1YuvToNv12> fail to lock YUV data");
        return -1;
    }


    //
    // Converting
    //
    uint8_t* pNv12Data = (uint8_t*)pNv12Buffer;
    uint8_t* pYData= (uint8_t*)pYBuffer;
    uint8_t* pUData= (uint8_t*)pUBuffer;
    uint8_t* pVData= (uint8_t*)pVBuffer;
    int yDataSize = width * height;
    int uvDataSize= (yDataSize / 4);

    // Y 数据直接拷贝
    memcpy(pNv12Buffer, pYData, yDataSize);


    // UV数据要交叉存放
    int pos = yDataSize;
    int i;
    for (i = 0; i < uvDataSize; i++) {
        pNv12Buffer[pos] = pUData[i];
        pNv12Buffer[pos+1] = pVData[i];

        pos += 2;
    }

    // Unlock YUV buffer
    env->ReleaseByteArrayElements(jb_yData, pYBuffer, 0);
    env->ReleaseByteArrayElements(jb_uData, pUBuffer, 0);
    env->ReleaseByteArrayElements(jb_vData, pVBuffer, 0);
    env->ReleaseByteArrayElements(jb_nv21Buffer, pNv12Buffer, 0);

    //LOGI("<ImgCvt_1YuvToNv12> <==Exit, width=%d, height=%d", width, height);
    return 0;
}

