
#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <android/log.h>
#include "AvSoftDecoder_jni.h"
#include "ErrCode.h"
#include "AvParserEng.hpp"
#include "JNIPublic.h"
#include "JNIHelper.hpp"
#include "AvCodecUtility.hpp"

//
// 定义Android层的色彩格式
//
#define COLOR_FormatYUV420Planar        19      ///< 对应I420
#define COLOR_FormatYUV420PackedPlanar  20      ///< 对应YUV12
#define COLOR_FormatYUV420SemiPlanar    21      ///< 对应NV21


#define BUFFER_FLAG_SW_EOS              256     ///< 软解码的EOS空帧


//
// Handler for media parser
//
typedef struct _AvParserEngHandler {
    CAvParserEng*   pParserEng;

    JavaVM*       pJavaVM;
    jclass        JavaCtrlClass;
    jobject       JavaCtrlObj;

}AVPARSERENG_HANDLER, *LPAVPARSERENG_HANDLER;


//////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////// JNI Interface Implementation ///////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
/*
 * Class:     io_agora_avmodule_AvSoftDecoder
 * Method:    native_distillMediaInfo
 * Signature: (Ljava/lang/String;Lio/agora/avmodule/AvMediaInfo;)I
 */
JNIEXPORT jint JNICALL Java_io_agora_avmodule_AvSoftDecoder_native_1distillMediaInfo
    (JNIEnv *env, jobject thiz, jstring jstr_filePath, jobject jobj_mediaInfo)
{
    auto autoFilePath = getRAIIJString(env, jstr_filePath);
    if (autoFilePath == nullptr) {
        LOGE("<native_1distillMediaInfo> [ERROR] invalid parameter\n");
        return XERR_INVALID_PARAM;
    }
    std::string in_file_path = autoFilePath.get();
    if (in_file_path.empty()) {
        LOGE("<native_1distillMediaInfo> [ERROR] invalid parameter\n");
        return XERR_INVALID_PARAM;
    }


    //
    // 获取类对象的各个字段的 Field Id
    //
    jclass jclass_media_info = env->GetObjectClass(jobj_mediaInfo);
    jfieldID fid_file_path = env->GetFieldID(jclass_media_info, "mFilePath", kTypeString);
    jfieldID fid_duration = env->GetFieldID(jclass_media_info, "mFileDuration", kTypeLong);

    jfieldID fid_video_track = env->GetFieldID(jclass_media_info, "mVideoTrackId", kTypeInt);
    jfieldID fid_video_duration = env->GetFieldID(jclass_media_info, "mVideoDuration", kTypeLong);
    jfieldID fid_video_codec = env->GetFieldID(jclass_media_info, "mVideoCodec", kTypeString);
    jfieldID fid_color_format = env->GetFieldID(jclass_media_info, "mColorFormat", kTypeInt);
    jfieldID fid_color_range = env->GetFieldID(jclass_media_info, "mColorRange", kTypeInt);
    jfieldID fid_color_space = env->GetFieldID(jclass_media_info, "mColorSpace", kTypeInt);
    jfieldID fid_video_width = env->GetFieldID(jclass_media_info, "mVideoWidth", kTypeInt);
    jfieldID fid_video_height = env->GetFieldID(jclass_media_info, "mVideoHeight", kTypeInt);
    jfieldID fid_rotation = env->GetFieldID(jclass_media_info, "mRotation", kTypeInt);
    jfieldID fid_frame_rate = env->GetFieldID(jclass_media_info, "mFrameRate", kTypeInt);
    jfieldID fid_video_bitrate = env->GetFieldID(jclass_media_info, "mVideoBitrate", kTypeInt);
    jfieldID fid_video_max_bitrate = env->GetFieldID(jclass_media_info, "mVideoMaxBitrate", kTypeInt);

    jfieldID fid_audio_track = env->GetFieldID(jclass_media_info, "mAudioTrackId", kTypeInt);
    jfieldID fid_audio_duration = env->GetFieldID(jclass_media_info, "mAudioDuration", kTypeLong);
    jfieldID fid_audio_codec = env->GetFieldID(jclass_media_info, "mAudioCodec", kTypeString);
    jfieldID fid_sample_format = env->GetFieldID(jclass_media_info, "mSampleFmt", kTypeInt);
    jfieldID fid_channels = env->GetFieldID(jclass_media_info, "mChannels", kTypeInt);
    jfieldID fid_samle_rate = env->GetFieldID(jclass_media_info, "mSampleRate", kTypeInt);

    jfieldID fid_audio_bitrate = env->GetFieldID(jclass_media_info, "mAudioBitrate", kTypeInt);
    jfieldID fid_audio_max_bitrate = env->GetFieldID(jclass_media_info, "mAudioMaxBitrate", kTypeInt);

    //
    // 媒体文件信息提取
    //
    AvMediaInfo out_media_info;
    int32_t ret = CAvParserEng::distillMediaInfo(in_file_path, &out_media_info);


    //
    // 设置字段值
    //
    jstring  file_path = env->NewStringUTF((const char *)(in_file_path.c_str()));
    env->SetObjectField(jobj_mediaInfo, fid_file_path, file_path);
    env->SetLongField(jobj_mediaInfo, fid_duration, (jlong)(out_media_info.file_duration_));

    env->SetIntField(jobj_mediaInfo, fid_video_track, (jint)(out_media_info.video_track_index_));
    if (out_media_info.video_track_index_ >= 0) {  // 有视频流信息
        env->SetLongField(jobj_mediaInfo, fid_video_duration,
                          (jlong) (out_media_info.video_duration_));
        std::string android_video_codec = "";
        CAvCodecUtility::MapAndroidCodec(out_media_info.video_codec_, android_video_codec);
        jstring video_codec = env->NewStringUTF((const char *) (android_video_codec.c_str()));
        env->SetObjectField(jobj_mediaInfo, fid_video_codec, video_codec);
        env->SetIntField(jobj_mediaInfo, fid_color_format, (jint) (out_media_info.color_format_));
        env->SetIntField(jobj_mediaInfo, fid_color_space, (jint) (out_media_info.color_space_));
        env->SetIntField(jobj_mediaInfo, fid_color_range, (jint) (out_media_info.color_range_));
        env->SetIntField(jobj_mediaInfo, fid_video_width, (jint) (out_media_info.video_width_));
        env->SetIntField(jobj_mediaInfo, fid_video_height, (jint) (out_media_info.video_height_));
        env->SetIntField(jobj_mediaInfo, fid_rotation, (jint) (out_media_info.rotation_));
        env->SetIntField(jobj_mediaInfo, fid_frame_rate, (jint) (out_media_info.frame_rate_));
        env->SetIntField(jobj_mediaInfo, fid_video_bitrate,
                         (jint) (out_media_info.video_bitrate_));
        env->SetIntField(jobj_mediaInfo, fid_video_max_bitrate,
                         (jint) (out_media_info.video_max_bitrate));
    }

    env->SetIntField(jobj_mediaInfo, fid_audio_track, (jint) (out_media_info.audio_track_index_));
    if (out_media_info.audio_track_index_ >= 0) {  // 有音频流信息
        env->SetLongField(jobj_mediaInfo, fid_audio_duration,
                          (jlong) (out_media_info.audio_duration_));
        std::string android_audio_codec = "";
        CAvCodecUtility::MapAndroidCodec(out_media_info.audio_codec_, android_audio_codec);
        jstring audio_codec = env->NewStringUTF((const char *) (android_audio_codec.c_str()));
        env->SetObjectField(jobj_mediaInfo, fid_audio_codec, audio_codec);

        int32_t android_format = ENCODING_PCM_16BIT;
        CAvCodecUtility::MapAndroidSampleFormat(out_media_info.sample_foramt_, android_format);
        env->SetIntField(jobj_mediaInfo, fid_sample_format, (jint) (android_format));
        env->SetIntField(jobj_mediaInfo, fid_channels, (jint) (out_media_info.channels_));
        env->SetIntField(jobj_mediaInfo, fid_samle_rate, (jint) (out_media_info.sample_rate_));
        env->SetIntField(jobj_mediaInfo, fid_frame_rate, (jint) (out_media_info.frame_rate_));
        env->SetIntField(jobj_mediaInfo, fid_audio_bitrate,
                         (jint) (out_media_info.audio_bitrate_));
        env->SetIntField(jobj_mediaInfo, fid_audio_max_bitrate,
                         (jint) (out_media_info.audio_max_bitrate));
    }

    //
    // 释放相应的值
    //
    env->DeleteLocalRef(jclass_media_info);

    LOGD("<native_1distillMediaInfo> done\n");
    return XOK;
}


/*
 * Class:     io_agora_avmodule_AvSoftDecoder
 * Method:    native_parserOpen
 * Signature: (Lio/agora/avmodule/AvDecParam;)J
 */
JNIEXPORT jlong JNICALL Java_io_agora_avmodule_AvSoftDecoder_native_1parserOpen(
    JNIEnv *env,
    jobject thiz,
    jobject jobj_decParam      )
{
    AVPARSERENG_HANDLER* pEngHandler = nullptr;
    int res;

    pEngHandler = (AVPARSERENG_HANDLER*)malloc(sizeof(AVPARSERENG_HANDLER));
    assert(pEngHandler);
    if (nullptr == pEngHandler) {
        return 0;
    }
    memset(pEngHandler, 0, sizeof(AVPARSERENG_HANDLER));

    env->GetJavaVM(&(pEngHandler->pJavaVM));
    if( NULL == pEngHandler->pJavaVM )
    {
        LOGE("<native_1parserOpen> [ERROR] GetJavaVM failed\n");
        free(pEngHandler);
        return 0;
    }
    pEngHandler->JavaCtrlClass = (jclass)env->NewGlobalRef( env->GetObjectClass(thiz) );
    pEngHandler->JavaCtrlObj = env->NewGlobalRef(thiz);


    pEngHandler->pParserEng = new CAvParserEng();
    SharePtr<AvDecParam> dec_param = MakeSharePtr<AvDecParam>();

    // 解析输入文件路径
    auto autInFilePath = getRAIIJString(env, GetJStringField(env, jobj_decParam, "mInFileUrl").get());
    if (autInFilePath) {
        dec_param->in_file_path_ = autInFilePath.get();
    } else {
        LOGE("<native_1parserOpen> [ERROR] not mInFilePath value\n");
        return XERR_INVALID_PARAM;
    }

    // 解析视频参数
    int32_t android_color_format = (int32_t) (GetJNIFieid<jint>(env, jobj_decParam,"mOutVidFormat"));
    CAvCodecUtility::MapFfmpegColorFormat(android_color_format, dec_param->color_format_);
    dec_param->video_width_ = (int32_t) (GetJNIFieid<jint>(env, jobj_decParam, "mOutVidWidth"));
    dec_param->video_height_ = (int32_t) (GetJNIFieid<jint>(env, jobj_decParam,"mOutVidHeight"));

    // 解析音频参数
    int32_t android_sample_format = (int32_t) (GetJNIFieid<jint>(env, jobj_decParam,"mOutAudSampleFmt"));
    CAvCodecUtility::MapFfmpegSampleFormat(android_sample_format, dec_param->sample_format_);
    dec_param->channels_ = (int32_t) (GetJNIFieid<jint>(env, jobj_decParam, "mOutAudChannels"));
    dec_param->sample_rate_ = (int32_t) (GetJNIFieid<jint>(env, jobj_decParam, "mOutAudSampleRate"));

    // 打开媒体文件进行解析
    int32_t ret = pEngHandler->pParserEng->Open(dec_param);
    if (ret != XOK) {
        LOGD("<native_1parserOpen> [ERROR] fail to open input file: %s\n", dec_param->in_file_path_.c_str());
        free(pEngHandler);
        return 0;
    }

    LOGD("<native_1parserOpen> done, filePath=%s, pEngHandler=%p",
         dec_param->in_file_path_.c_str(), pEngHandler);
    return (jlong)pEngHandler;
}

/*
 * Class:     io_agora_avmodule_AvSoftDecoder
 * Method:    native_parserClose
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_io_agora_avmodule_AvSoftDecoder_native_1parserClose
    (JNIEnv *env, jobject thiz, jlong jlEng)
{
    AVPARSERENG_HANDLER* pEngHandler = (AVPARSERENG_HANDLER*)jlEng;

    if (nullptr != pEngHandler)
    {
        if (pEngHandler->pParserEng)
        {
            pEngHandler->pParserEng->Close();
            delete pEngHandler->pParserEng;
            pEngHandler->pParserEng = nullptr;
        }

        if (pEngHandler->JavaCtrlObj)
        {
            env->DeleteGlobalRef(pEngHandler->JavaCtrlObj);
            pEngHandler->JavaCtrlObj = nullptr;
        }

        if (pEngHandler->JavaCtrlClass)
        {
            env->DeleteGlobalRef(pEngHandler->JavaCtrlClass);
            pEngHandler->JavaCtrlClass = nullptr;
        }

        free(pEngHandler);
        LOGD("<native_1parserClose> done");
    }

    return XOK;
}

/*
 * Class:     io_agora_avmodule_AvSoftDecoder
 * Method:    native_getInMediaInfo
 * Signature: (JLio/agora/avmodule/AvMediaInfo;)I
 */
JNIEXPORT jint JNICALL Java_io_agora_avmodule_AvSoftDecoder_native_1getInMediaInfo
    (JNIEnv *env, jobject thiz, jlong jlEng, jobject jobj_mediaInfo)
{
    AVPARSERENG_HANDLER* pEngHandler = (AVPARSERENG_HANDLER*)jlEng;

    if (nullptr == pEngHandler || nullptr == pEngHandler->pParserEng) {
        LOGE("<native_1getInMediaInfo> [ERROR] invalid parameter\n");
        return XERR_INVALID_PARAM;
    }

    //
    // 获取类对象的各个字段的 Field Id
    //
    jclass jclass_media_info = env->GetObjectClass(jobj_mediaInfo);
    jfieldID fid_file_path = env->GetFieldID(jclass_media_info, "mFilePath", kTypeString);
    jfieldID fid_duration = env->GetFieldID(jclass_media_info, "mFileDuration", kTypeLong);

    jfieldID fid_video_track = env->GetFieldID(jclass_media_info, "mVideoTrackId", kTypeInt);
    jfieldID fid_video_duration = env->GetFieldID(jclass_media_info, "mVideoDuration", kTypeLong);
    jfieldID fid_video_codec = env->GetFieldID(jclass_media_info, "mVideoCodec", kTypeString);
    jfieldID fid_color_format = env->GetFieldID(jclass_media_info, "mColorFormat", kTypeInt);
    jfieldID fid_color_range = env->GetFieldID(jclass_media_info, "mColorRange", kTypeInt);
    jfieldID fid_color_space = env->GetFieldID(jclass_media_info, "mColorSpace", kTypeInt);
    jfieldID fid_video_width = env->GetFieldID(jclass_media_info, "mVideoWidth", kTypeInt);
    jfieldID fid_video_height = env->GetFieldID(jclass_media_info, "mVideoHeight", kTypeInt);
    jfieldID fid_rotation = env->GetFieldID(jclass_media_info, "mRotation", kTypeInt);
    jfieldID fid_frame_rate = env->GetFieldID(jclass_media_info, "mFrameRate", kTypeInt);
    jfieldID fid_video_bitrate = env->GetFieldID(jclass_media_info, "mVideoBitrate", kTypeInt);
    jfieldID fid_video_max_bitrate = env->GetFieldID(jclass_media_info, "mVideoMaxBitrate", kTypeInt);

    jfieldID fid_audio_track = env->GetFieldID(jclass_media_info, "mAudioTrackId", kTypeInt);
    jfieldID fid_audio_duration = env->GetFieldID(jclass_media_info, "mAudioDuration", kTypeLong);
    jfieldID fid_audio_codec = env->GetFieldID(jclass_media_info, "mAudioCodec", kTypeString);
    jfieldID fid_sample_format = env->GetFieldID(jclass_media_info, "mSampleFmt", kTypeInt);
    jfieldID fid_channels = env->GetFieldID(jclass_media_info, "mChannels", kTypeInt);
    jfieldID fid_samle_rate = env->GetFieldID(jclass_media_info, "mSampleRate", kTypeInt);

    jfieldID fid_audio_bitrate = env->GetFieldID(jclass_media_info, "mAudioBitrate", kTypeInt);
    jfieldID fid_audio_max_bitrate = env->GetFieldID(jclass_media_info, "mAudioMaxBitrate", kTypeInt);

    //
    // 设置字段值
    //
    const AvMediaInfo* media_info_ptr = pEngHandler->pParserEng->GetMediaInfoPtr();
    jstring  file_path = env->NewStringUTF((const char *)(media_info_ptr->file_path_.c_str()));
    env->SetObjectField(jobj_mediaInfo, fid_file_path, file_path);
    env->SetLongField(jobj_mediaInfo, fid_duration, (jlong)(media_info_ptr->file_duration_));

    env->SetIntField(jobj_mediaInfo, fid_video_track, (jint)(media_info_ptr->video_track_index_));
    if (media_info_ptr->video_track_index_ >= 0) {  // 有视频流信息
        env->SetLongField(jobj_mediaInfo, fid_video_duration,
                          (jlong) (media_info_ptr->video_duration_));
        std::string android_video_codec = "";
        CAvCodecUtility::MapAndroidCodec(media_info_ptr->video_codec_, android_video_codec);
        jstring video_codec = env->NewStringUTF((const char *) (android_video_codec.c_str()));
        env->SetObjectField(jobj_mediaInfo, fid_video_codec, video_codec);
        env->SetIntField(jobj_mediaInfo, fid_color_format, (jint) (media_info_ptr->color_format_));
        env->SetIntField(jobj_mediaInfo, fid_color_space, (jint) (media_info_ptr->color_space_));
        env->SetIntField(jobj_mediaInfo, fid_color_range, (jint) (media_info_ptr->color_range_));
        env->SetIntField(jobj_mediaInfo, fid_video_width, (jint) (media_info_ptr->video_width_));
        env->SetIntField(jobj_mediaInfo, fid_video_height, (jint) (media_info_ptr->video_height_));
        env->SetIntField(jobj_mediaInfo, fid_rotation, (jint) (media_info_ptr->rotation_));
        env->SetIntField(jobj_mediaInfo, fid_frame_rate, (jint) (media_info_ptr->frame_rate_));
        env->SetIntField(jobj_mediaInfo, fid_video_bitrate,
                         (jint) (media_info_ptr->video_bitrate_));
        env->SetIntField(jobj_mediaInfo, fid_video_max_bitrate,
                         (jint) (media_info_ptr->video_max_bitrate));
    }

    env->SetIntField(jobj_mediaInfo, fid_audio_track, (jint) (media_info_ptr->audio_track_index_));
    if (media_info_ptr->audio_track_index_ >= 0) {  // 有音频流信息
        env->SetLongField(jobj_mediaInfo, fid_audio_duration,
                          (jlong) (media_info_ptr->audio_duration_));
        std::string android_audio_codec = "";
        CAvCodecUtility::MapAndroidCodec(media_info_ptr->audio_codec_, android_audio_codec);
        jstring audio_codec = env->NewStringUTF((const char *) (android_audio_codec.c_str()));
        env->SetObjectField(jobj_mediaInfo, fid_audio_codec, audio_codec);

        int32_t android_format = ENCODING_PCM_16BIT;
        CAvCodecUtility::MapAndroidSampleFormat(media_info_ptr->sample_foramt_, android_format);
        env->SetIntField(jobj_mediaInfo, fid_sample_format, (jint) (android_format));
        env->SetIntField(jobj_mediaInfo, fid_channels, (jint) (media_info_ptr->channels_));
        env->SetIntField(jobj_mediaInfo, fid_samle_rate, (jint) (media_info_ptr->sample_rate_));
        env->SetIntField(jobj_mediaInfo, fid_frame_rate, (jint) (media_info_ptr->frame_rate_));
        env->SetIntField(jobj_mediaInfo, fid_audio_bitrate,
                         (jint) (media_info_ptr->audio_bitrate_));
        env->SetIntField(jobj_mediaInfo, fid_audio_max_bitrate,
                         (jint) (media_info_ptr->audio_max_bitrate));
    }

    //
    // 释放相应的值
    //
    env->DeleteLocalRef(jclass_media_info);

    LOGD("<native_1getInMediaInfo> done\n");
    return XOK;
}


/*
 * Class:     io_agora_avmodule_AvSoftDecoder
 * Method:    native_parserInputPacket
 * Signature: (J[I)I
 */
JNIEXPORT jint JNICALL Java_io_agora_avmodule_AvSoftDecoder_native_1parserInputPacket
    (JNIEnv *env, jobject thiz, jlong jlEng, jintArray outPktType)
{
    AVPARSERENG_HANDLER* pEngHandler = (AVPARSERENG_HANDLER*)jlEng;

    if (nullptr == pEngHandler || nullptr == pEngHandler->pParserEng) {
        LOGE("<native_1parserInputPacket> [ERROR] invalid parameter\n");
        return XERR_INVALID_PARAM;
    }

    // Lock packet type buffer
    jint * out_pkt_type =  env->GetIntArrayElements(outPktType, 0);
    if (NULL == out_pkt_type) {
        LOGE("<native_1parserInputPacket> fail to lock pkt type");
        return XERR_INVALID_PARAM;
    }

    int32_t pkt_type = 0;
    int32_t ret = pEngHandler->pParserEng->InputPacket(pkt_type);
    out_pkt_type[0] = (jint)pkt_type;

    // Unlock  packet type buffer
    env->ReleaseIntArrayElements(outPktType, out_pkt_type, 0);

    LOGD("<native_1parserInputPacket> done, ret=%d, pkt_type=%d\n", ret, pkt_type);
    return ret;
}

/*
 * Class:     io_agora_avmodule_AvSoftDecoder
 * Method:    native_parserDecVideoFrame
 * Signature: (JLio/agora/avmodule/AvVideoFrame;)I
 */
JNIEXPORT jint JNICALL Java_io_agora_avmodule_AvSoftDecoder_native_1parserDecVideoFrame
    (JNIEnv *env, jobject thiz, jlong jlEng, jobject jobj_videoFrame)
{
    AVPARSERENG_HANDLER* pEngHandler = (AVPARSERENG_HANDLER*)jlEng;

    if (nullptr == pEngHandler || nullptr == pEngHandler->pParserEng) {
        LOGE("<native_1parserDecVideoFrame> [ERROR] invalid parameter\n");
        return XERR_INVALID_PARAM;
    }

    //
    // 获取类对象的各个字段的 Field Id
    //
    jclass jclass_video_frame = env->GetObjectClass(jobj_videoFrame);

    jfieldID fid_frame_index = env->GetFieldID(jclass_video_frame, "mFrameIndex", kTypeInt);
    jfieldID fid_timestamp = env->GetFieldID(jclass_video_frame, "mTimestamp", kTypeLong);
    jfieldID fid_key_frame = env->GetFieldID(jclass_video_frame, "mKeyFrame", kTypeBool);
    jfieldID fid_last_frame = env->GetFieldID(jclass_video_frame, "mLastFrame", kTypeBool);
    jfieldID fid_flags = env->GetFieldID(jclass_video_frame, "mFlags", kTypeInt);
    jfieldID fid_color_format = env->GetFieldID(jclass_video_frame, "mColorFormat", kTypeInt);
    jfieldID fid_width = env->GetFieldID(jclass_video_frame, "mWidth", kTypeInt);
    jfieldID fid_height = env->GetFieldID(jclass_video_frame, "mHeight", kTypeInt);


   //
    // 解码视频帧
    //
    int32_t ret = pEngHandler->pParserEng->DecodeVideoFrame();
    AvVideoFrame* video_frame = pEngHandler->pParserEng->GetVideoFrame();

    if (ret == XERR_CODEC_DEC_EOS) { // 所有视频帧解码完成
        env->SetBooleanField(jobj_videoFrame, fid_last_frame, (jboolean)(JNI_TRUE));

        const AvMediaInfo* pMeidaInfo = pEngHandler->pParserEng->GetMediaInfoPtr();
        int64_t time_span = (int64_t)(1000 * 1000 * 4) / (int64_t)(pMeidaInfo->frame_rate_);

        env->SetIntField(jobj_videoFrame, fid_frame_index, (jint)(video_frame->frame_index_));
        env->SetLongField(jobj_videoFrame, fid_timestamp, (jlong)(video_frame->timestamp_ + time_span));
        env->SetBooleanField(jobj_videoFrame, fid_key_frame, (jboolean)(video_frame->key_frame_ ? JNI_TRUE: JNI_FALSE));
        env->SetIntField(jobj_videoFrame, fid_flags, (jint)(video_frame->flags_));

        env->SetIntField(jobj_videoFrame, fid_color_format, (jint)(COLOR_FormatYUV420SemiPlanar)); // 固定输出NV12
        env->SetIntField(jobj_videoFrame, fid_width, (jint)(video_frame->width_));
        env->SetIntField(jobj_videoFrame, fid_height, (jint)(video_frame->height_));

    } else if (ret == XERR_CODEC_INDATA) { // 本次解码无视频帧输出

    } else if (ret == XERR_CODEC_DECODING) { // 解码失败

    }


    if (video_frame->frame_valid_) {    // 当前解码到视频帧
        env->SetIntField(jobj_videoFrame, fid_frame_index, (jint)(video_frame->frame_index_));
        env->SetLongField(jobj_videoFrame, fid_timestamp, (jlong)(video_frame->timestamp_));
        env->SetBooleanField(jobj_videoFrame, fid_key_frame, (jboolean)(video_frame->key_frame_ ? JNI_TRUE: JNI_FALSE));
        env->SetIntField(jobj_videoFrame, fid_flags, (jint)(video_frame->flags_));

        env->SetIntField(jobj_videoFrame, fid_color_format, (jint)(COLOR_FormatYUV420SemiPlanar)); // 固定输出NV12
        env->SetIntField(jobj_videoFrame, fid_width, (jint)(video_frame->width_));
        env->SetIntField(jobj_videoFrame, fid_height, (jint)(video_frame->height_));
    }

    if (ret == XERR_CODEC_DEC_EOS) { // 解码完成
        jint flags = video_frame->flags_ + BUFFER_FLAG_SW_EOS;
        env->SetIntField(jobj_videoFrame, fid_flags, (jint) (flags));
    }

    //
    // 释放相应的类对象
    //
    env->DeleteLocalRef(jclass_video_frame);

    LOGD("<native_1parserDecVideoFrame> done, ret=%d\n", ret);
    return (jint)ret;
}

/*
 * Class:     io_agora_avmodule_AvSoftDecoder
 * Method:    native_parserGetVideoFrame
 * Signature: (J[B)I
 */
JNIEXPORT jint JNICALL Java_io_agora_avmodule_AvSoftDecoder_native_1parserGetVideoFrame
    (JNIEnv *env, jobject thiz, jlong jlEng, jbyteArray jobj_videoBuffer)
{
    AVPARSERENG_HANDLER* pEngHandler = (AVPARSERENG_HANDLER*)jlEng;

    if (nullptr == pEngHandler || nullptr == pEngHandler->pParserEng) {
        LOGE("<native_1parserGetVideoFrame> [ERROR] invalid parameter\n");
        return XERR_INVALID_PARAM;
    }

    AvVideoFrame* video_frame = pEngHandler->pParserEng->GetVideoFrame();
    if (!video_frame->frame_valid_) {
        LOGE("<native_1parserGetVideoFrame> [ERROR] no video frame\n");
        return XERR_CODEC_DECODING;
    }

    // Lock video bytes buffer
    jbyte* out_vidbuffer =  env->GetByteArrayElements(jobj_videoBuffer, 0);
    if (NULL == out_vidbuffer) {
        LOGE("<native_1parserGetVideoFrame> fail to lock video buffer");
        return XERR_INVALID_PARAM;
    }

    // Copy the data
    int32_t data_size = (video_frame->width_ * video_frame->height_ * 3 / 2);
    memcpy(out_vidbuffer, video_frame->frame_data_.get(), data_size);


    // Unlock video bytes buffer
    env->ReleaseByteArrayElements(jobj_videoBuffer, out_vidbuffer, 0);

    LOGD("<native_1parserGetVideoFrame> done\n");
    return XOK;
}

/*
 * Class:     io_agora_avmodule_AvSoftDecoder
 * Method:    native_parserDecAudioFrame
 * Signature: (JLio/agora/avmodule/AvAudioFrame;)I
 */
JNIEXPORT jint JNICALL Java_io_agora_avmodule_AvSoftDecoder_native_1parserDecAudioFrame
    (JNIEnv *env, jobject thiz, jlong jlEng, jobject jobj_audioFrame)
{
    AVPARSERENG_HANDLER* pEngHandler = (AVPARSERENG_HANDLER*)jlEng;

    if (nullptr == pEngHandler || nullptr == pEngHandler->pParserEng) {
        LOGE("<native_1parserDecAudioFrame> [ERROR] invalid parameter\n");
        return XERR_INVALID_PARAM;
    }

    //
    // 获取类对象的各个字段的 Field Id
    //
    jclass jclass_audio_frame = env->GetObjectClass(jobj_audioFrame);

    jfieldID fid_frame_index = env->GetFieldID(jclass_audio_frame, "mFrameIndex", kTypeInt);
    jfieldID fid_timestamp = env->GetFieldID(jclass_audio_frame, "mTimestamp", kTypeLong);
    jfieldID fid_key_frame = env->GetFieldID(jclass_audio_frame, "mKeyFrame", kTypeBool);
    jfieldID fid_last_frame = env->GetFieldID(jclass_audio_frame, "mLastFrame", kTypeBool);
    jfieldID fid_flags = env->GetFieldID(jclass_audio_frame, "mFlags", kTypeInt);
    jfieldID fid_smpl_format = env->GetFieldID(jclass_audio_frame, "mSampleFormat", kTypeInt);
    jfieldID fid_bytes_per_smpl = env->GetFieldID(jclass_audio_frame, "mBytesPerSample", kTypeInt);
    jfieldID fid_channels = env->GetFieldID(jclass_audio_frame, "mChannels", kTypeInt);
    jfieldID fid_smpl_number = env->GetFieldID(jclass_audio_frame, "mSampleNumber", kTypeInt);
    jfieldID fid_smpl_rate = env->GetFieldID(jclass_audio_frame, "mSampleRate", kTypeInt);

    //
    // 解码音频帧
    //
    int32_t ret = pEngHandler->pParserEng->DecodeAudioFrame();
    AvAudioFrame* audio_frame = pEngHandler->pParserEng->GetAudioFrame();

    if (ret == XERR_CODEC_DEC_EOS) { // 解码完成
        env->SetBooleanField(jobj_audioFrame, fid_last_frame, (jboolean)(JNI_TRUE));

        int64_t time_span = 4*20*1000;

        env->SetIntField(jobj_audioFrame, fid_frame_index, (jint)(audio_frame->frame_index_));
        env->SetLongField(jobj_audioFrame, fid_timestamp, (jlong)(audio_frame->timestamp_ + time_span));
        env->SetBooleanField(jobj_audioFrame, fid_key_frame, (jboolean)(audio_frame->key_frame_ ? JNI_TRUE: JNI_FALSE));
        env->SetIntField(jobj_audioFrame, fid_flags, (jint)(audio_frame->flags_));

        int32_t android_format = ENCODING_PCM_16BIT;
        CAvCodecUtility::MapAndroidSampleFormat(audio_frame->sample_fmt_, android_format);
        env->SetIntField(jobj_audioFrame, fid_smpl_format, (jint)(android_format));
        env->SetIntField(jobj_audioFrame, fid_bytes_per_smpl, (jint)(audio_frame->bytes_per_sample_));
        env->SetIntField(jobj_audioFrame, fid_channels, (jint)(audio_frame->channels_));
        env->SetIntField(jobj_audioFrame, fid_smpl_number, (jint)(0));
        env->SetIntField(jobj_audioFrame, fid_smpl_rate, (jint)(audio_frame->sample_rate_));

    } else if (ret == XERR_CODEC_INDATA) { // 本次解码无视频帧输出

    } else if (ret == XERR_CODEC_DECODING) { // 解码失败

    }

    if (audio_frame->frame_valid_) {    // 当前解码到音频帧

        env->SetIntField(jobj_audioFrame, fid_frame_index, (jint)(audio_frame->frame_index_));
        env->SetLongField(jobj_audioFrame, fid_timestamp, (jlong)(audio_frame->timestamp_));
        env->SetBooleanField(jobj_audioFrame, fid_key_frame, (jboolean)(audio_frame->key_frame_ ? JNI_TRUE: JNI_FALSE));
        env->SetIntField(jobj_audioFrame, fid_flags, (jint)(audio_frame->flags_));

        int32_t android_format = ENCODING_PCM_16BIT;
        CAvCodecUtility::MapAndroidSampleFormat(audio_frame->sample_fmt_, android_format);
        env->SetIntField(jobj_audioFrame, fid_smpl_format, (jint)(android_format));
        env->SetIntField(jobj_audioFrame, fid_bytes_per_smpl, (jint)(audio_frame->bytes_per_sample_));
        env->SetIntField(jobj_audioFrame, fid_channels, (jint)(audio_frame->channels_));
        env->SetIntField(jobj_audioFrame, fid_smpl_number, (jint)(audio_frame->sample_number_));
        env->SetIntField(jobj_audioFrame, fid_smpl_rate, (jint)(audio_frame->sample_rate_));
    }

    if (ret == XERR_CODEC_DEC_EOS) { // 解码完成
        jint flags = audio_frame->flags_ + BUFFER_FLAG_SW_EOS;
        env->SetIntField(jobj_audioFrame, fid_flags, (jint) (flags));
    }

        //
    // 释放相应的类对象
    //
    env->DeleteLocalRef(jobj_audioFrame);

    LOGD("<native_1parserDecAudioFrame> done, ret=%d\n", ret);
    return (jint)ret;
}

/*
 * Class:     io_agora_avmodule_AvSoftDecoder
 * Method:    native_parserGetAudioFrame
 * Signature: (J[B)I
 */
JNIEXPORT jint JNICALL Java_io_agora_avmodule_AvSoftDecoder_native_1parserGetAudioFrame
    (JNIEnv *env, jobject thiz, jlong jlEng, jbyteArray jb_audioBuffer)
{
    AVPARSERENG_HANDLER* pEngHandler = (AVPARSERENG_HANDLER*)jlEng;

    if (nullptr == pEngHandler || nullptr == pEngHandler->pParserEng) {
        LOGE("<native_1parserGetAudioFrame> [ERROR] invalid parameter\n");
        return XERR_INVALID_PARAM;
    }

    AvAudioFrame* audio_frame = pEngHandler->pParserEng->GetAudioFrame();
    if ((!audio_frame->frame_valid_) || (audio_frame->sample_number_  <= 0)) {
        LOGE("<native_1parserGetAudioFrame> [ERROR] no audio frame\n");
        return XERR_CODEC_DECODING;
    }

    // Lock audio bytes buffer
    jbyte* out_audbuffer =  env->GetByteArrayElements(jb_audioBuffer, 0);
    if (NULL == out_audbuffer) {
        LOGE("<native_1parserGetAudioFrame> fail to lock audio buffer");
        return XERR_INVALID_PARAM;
    }

    // Copy the data
    int32_t data_size = (audio_frame->sample_number_ * audio_frame->channels_ * audio_frame->bytes_per_sample_);
    memcpy(out_audbuffer, audio_frame->sample_data_.get(), data_size);


    // Unlock audio bytes buffer
    env->ReleaseByteArrayElements(jb_audioBuffer, out_audbuffer, 0);


    LOGD("<native_1parserGetAudioFrame> done\n");
    return XOK;
}


/*
 * Class:     com_luxiaohua_avmodule_AvSoftCodec
 * Method:    native_yuvToBitmap
 * Signature: ([BIIILjava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_io_agora_avmodule_AvSoftDecoder_native_1yuvToBitmap(
    JNIEnv*     env,
    jobject     thiz,
    jbyteArray  jb_yuvData,
    jint 		yuvFormat,
    jint 	    width,
    jint		height,
    jint        stride,
    jobject		jobj_outBmp    )
{
    return -1;
}



/*
 * Class:     com_luxiaohua_avmodule_AvSoftCodec
 * Method:    native_bitmapToYuv
 * Signature: (Ljava/lang/Object;I[B)I
 */
JNIEXPORT jint JNICALL Java_io_agora_avmodule_AvSoftDecoder_native_1bitmapToYuv(
    JNIEnv*     env,
    jobject     thiz,
    jobject		jobj_inBmp,
    jint 		yuvFormat,
    jbyteArray  jb_outYuvBuffer )
{
    return -1;
}

/*
 * Class:     com_luxiaohua_avmodule_AvSoftCodec
 * Method:    native_bmpSaveToFile
 * Signature: (Ljava/lang/Object;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_io_agora_avmodule_AvSoftDecoder_native_1bmpSaveToFile
        (JNIEnv* env, jobject thiz, jobject jobj_inBmp, jstring jstr_outFile)
{
    return -1;
}
