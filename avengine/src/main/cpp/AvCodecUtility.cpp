/**
 * @file AvParserEng.hpp
 * @brief This file implement the utility functions
 * @author xiaohua.lu
 * @email 2489186909@qq.com   luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-02-03
 * @license Copyright (C) 2021 LuXiaoHua. All rights reserved.
 */
#include "comtypedef.hpp"
#include "AvCodecUtility.hpp"
#include "string.h"



///////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////// Global Definition /////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////


//
// 色彩格式映射
//
int32_t kMap_Color_Format[][2] = {
    {COLOR_Format24bitRGB888, AV_PIX_FMT_RGB24},
    {COLOR_Format24bitBGR888, AV_PIX_FMT_BGR24},

    {COLOR_Format32bitBGRA8888, AV_PIX_FMT_BGRA},
    {COLOR_Format32bitARGB8888, AV_PIX_FMT_ARGB},

    {COLOR_FormatYUV411Planar, AV_PIX_FMT_YUV411P},
  //  {COLOR_FormatYUV411PackedPlanar, AV_PIX_FMT_UYYVYY411},

    {COLOR_FormatYUV420Planar, AV_PIX_FMT_YUV420P},
    //{COLOR_FormatYUV420PackedPlanar, AV_PIX_FMT_YV12},

    {COLOR_FormatYUV420SemiPlanar, AV_PIX_FMT_NV12},
    {COLOR_FormatYUV420PackedSemiPlanar, AV_PIX_FMT_NV21}
};



//
// 音频采样格式映射
//
int32_t kMap_Sample_Format[][2] = {
    {ENCODING_PCM_8BIT, AV_SAMPLE_FMT_U8},
    {ENCODING_PCM_16BIT, AV_SAMPLE_FMT_S16},
    {ENCODING_PCM_FLOAT, AV_SAMPLE_FMT_FLT},
    {ENCODING_PCM_32BIT, AV_SAMPLE_FMT_S32},
    {ENCODING_PCM_FLOAT, AV_SAMPLE_FMT_DBL},

    {ENCODING_PCM_8BIT, AV_SAMPLE_FMT_U8P},
    {ENCODING_PCM_16BIT, AV_SAMPLE_FMT_S16P},
    {ENCODING_PCM_FLOAT, AV_SAMPLE_FMT_FLTP},
    {ENCODING_PCM_32BIT, AV_SAMPLE_FMT_S32P},
    {ENCODING_PCM_FLOAT, AV_SAMPLE_FMT_DBLP},
    {ENCODING_PCM_FLOAT, AV_SAMPLE_FMT_S64},
    {ENCODING_PCM_FLOAT, AV_SAMPLE_FMT_S64P}
};



//
// 编解码器映射
//
struct CodecMap {
    std::string android_codec;
    int32_t ffmpeg_codec;
};

CodecMap kMap_Codec[] = {
    {MIMETYPE_VIDEO_MPEG2, AV_CODEC_ID_MPEG2VIDEO},
    {MIMETYPE_VIDEO_MPEG4, AV_CODEC_ID_MPEG4},
    {MIMETYPE_VIDEO_RAW, AV_CODEC_ID_RAWVIDEO},
    {MIMETYPE_VIDEO_H263, AV_CODEC_ID_H263},
    {MIMETYPE_VIDEO_AVC, AV_CODEC_ID_H264},
    {MIMETYPE_VIDEO_HEVC, AV_CODEC_ID_H265},
    {MIMETYPE_VIDEO_AV1, AV_CODEC_ID_AV1},
    {"video/x-vnd.on2.vp7", AV_CODEC_ID_VP7},
    {MIMETYPE_VIDEO_VP8, AV_CODEC_ID_VP8},
    {MIMETYPE_VIDEO_VP9, AV_CODEC_ID_VP9},
    {MIMETYPE_VIDEO_MPEG4, AV_CODEC_ID_MPEG4},
    {MIMETYPE_VIDEO_AVC, AV_CODEC_ID_H264},
    {MIMETYPE_VIDEO_HEVC, AV_CODEC_ID_H265},
    {"video/wmv1", AV_CODEC_ID_WMV1},
    {"video/wmv1", AV_CODEC_ID_WMV2},
    {"video/flv1", AV_CODEC_ID_FLV1},
    {"video/mjpeg", AV_CODEC_ID_MJPEG},

    {"audio/mp2", AV_CODEC_ID_MP2},
    {MIMETYPE_AUDIO_MPEG, AV_CODEC_ID_MP3},
    {MIMETYPE_AUDIO_AAC, AV_CODEC_ID_AAC},
    {MIMETYPE_AUDIO_AC3, AV_CODEC_ID_AC3},
    {"audio/dts", AV_CODEC_ID_DTS},
    {MIMETYPE_AUDIO_VORBIS, AV_CODEC_ID_VORBIS},
    {MIMETYPE_AUDIO_MPEG, AV_CODEC_ID_DVAUDIO},
    {"audio/mwav1", AV_CODEC_ID_WMAV1},
    {"audio/mwav2", AV_CODEC_ID_WMAV2},
    {"audio/mace3", AV_CODEC_ID_MACE3},
    {"audio/mace6", AV_CODEC_ID_MACE6},
    {"audio/vmd",   AV_CODEC_ID_VMDAUDIO},
    {MIMETYPE_AUDIO_FLAC, AV_CODEC_ID_FLAC},
    {MIMETYPE_AUDIO_MPEG, AV_CODEC_ID_MP3ADU},
    {MIMETYPE_AUDIO_MPEG, AV_CODEC_ID_MP3ON4},
    {"audio/shorten", AV_CODEC_ID_SHORTEN},
    {"audio/alac", AV_CODEC_ID_ALAC},
    {"audio/westwood_snd1", AV_CODEC_ID_WESTWOOD_SND1},
    {MIMETYPE_AUDIO_MSGSM, AV_CODEC_ID_GSM},
    {MIMETYPE_AUDIO_MSGSM, AV_CODEC_ID_GSM_MS},
    {"audio/qdm2", AV_CODEC_ID_QDM2},
    {"audio/cook", AV_CODEC_ID_COOK},
    {"audio/truesppech", AV_CODEC_ID_TRUESPEECH},
    {"audio/tta", AV_CODEC_ID_TTA},
    {"audio/smack", AV_CODEC_ID_SMACKAUDIO},
    {MIMETYPE_AUDIO_QCELP, AV_CODEC_ID_QCELP},
    {MIMETYPE_AUDIO_RAW, AV_CODEC_ID_WAVPACK},
    {"audio/discina", AV_CODEC_ID_DSICINAUDIO},
    {"audio/imc", AV_CODEC_ID_IMC},
    {"audio/musepack7", AV_CODEC_ID_MUSEPACK7},
    {"audio/mlp", AV_CODEC_ID_MLP},
    {"audio/atrac3", AV_CODEC_ID_ATRAC3},
    {"audio/ape", AV_CODEC_ID_APE},
    {"audio/nellymoser", AV_CODEC_ID_NELLYMOSER},
    {"audio/musepack8", AV_CODEC_ID_MUSEPACK8},
    {"audio/speex", AV_CODEC_ID_SPEEX},
    {"audio/wmavoice", AV_CODEC_ID_WMAVOICE},
    {"audio/wmapro", AV_CODEC_ID_WMAPRO},
    {"audio/wmalossless", AV_CODEC_ID_WMALOSSLESS},
    {"audio/atrac3p", AV_CODEC_ID_ATRAC3P},
    {"audio/eac3", AV_CODEC_ID_EAC3},
    {"audio/sipr", AV_CODEC_ID_SIPR},
    {"audio/mp1", AV_CODEC_ID_MP1},
    {"audio/twinvq", AV_CODEC_ID_TWINVQ},
    {"audio/truehd", AV_CODEC_ID_TRUEHD},
    {"audio/mp4als", AV_CODEC_ID_MP4ALS},
    {"audio/atrac1", AV_CODEC_ID_ATRAC1},
    {"audio/bink_rdft", AV_CODEC_ID_BINKAUDIO_RDFT},
    {"audio/bink_dct", AV_CODEC_ID_BINKAUDIO_DCT},
    {"audio/wmapro", AV_CODEC_ID_WMAPRO},
    {MIMETYPE_AUDIO_AMR_NB, AV_CODEC_ID_AMR_NB},
    {MIMETYPE_AUDIO_AMR_WB, AV_CODEC_ID_AMR_WB},
    {MIMETYPE_AUDIO_AAC, AV_CODEC_ID_AAC_LATM},
    {MIMETYPE_AUDIO_QCELP, AV_CODEC_ID_QCELP},
    {MIMETYPE_AUDIO_OPUS, AV_CODEC_ID_OPUS},
    {MIMETYPE_AUDIO_RAW, AV_CODEC_ID_PCM_F16LE},
    {MIMETYPE_AUDIO_FLAC, AV_CODEC_ID_FLAC},
    {MIMETYPE_AUDIO_MSGSM, AV_CODEC_ID_GSM_MS},
    {MIMETYPE_AUDIO_EAC3, AV_CODEC_ID_EAC3},
    {"audio/mp3", AV_CODEC_ID_MP3},
    {"audio/G722", AV_CODEC_ID_ADPCM_G722},
    {MIMETYPE_AUDIO_G711_ALAW, AV_CODEC_ID_PCM_ALAW},
    {MIMETYPE_AUDIO_G711_MLAW, AV_CODEC_ID_PCM_MULAW}

};

///////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////// Public Methods ////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////

int32_t CAvCodecUtility::MapFfmpegColorFormat(int32_t android_format, int32_t& out_ffmpeg_format) {
    int32_t map_count = sizeof(kMap_Color_Format) / sizeof(kMap_Color_Format[0]);
    int32_t i;

    for (i= 0; i < map_count; i++) {
        if (android_format == kMap_Color_Format[i][0]) {
            out_ffmpeg_format = kMap_Color_Format[i][1];
            return XOK;
        }
    }

    LOGE("<CAvCodecUtility::MapFfmpegColorFormat> [ERROR] not found, android_format=%d\n", android_format);
    assert(0);
    return XERR_UNSUPPORTED;
}

int32_t CAvCodecUtility::MapAndroidColorFormat(int32_t ffmpeg_format, int32_t& out_android_format) {
    int32_t map_count = sizeof(kMap_Color_Format) / sizeof(kMap_Color_Format[0]);
    int32_t i;

    for (i= 0; i < map_count; i++) {
        if (ffmpeg_format == kMap_Color_Format[i][1]) {
            out_android_format = kMap_Color_Format[i][0];
            return XOK;
        }
    }

    LOGE("<CAvCodecUtility::MapAndroidColorFormat> [ERROR] not found, ffmpeg_format=%d\n", ffmpeg_format);
    assert(0);
    return XERR_UNSUPPORTED;
}

int32_t CAvCodecUtility::MapFfmpegSampleFormat(int32_t android_format, int32_t& out_ffmpeg_format)
{
    int32_t map_count = sizeof(kMap_Sample_Format) /  sizeof(kMap_Sample_Format[0]);
    int32_t i;

    for (i= 0; i < map_count; i++) {
        if (android_format == kMap_Sample_Format[i][0]) {
            out_ffmpeg_format = kMap_Sample_Format[i][1];
            return XOK;
        }
    }

    LOGE("<CAvCodecUtility::MapFfmpegSampleFormat> [ERROR] not found, android_format=%d\n", android_format);
    assert(0);
    return XERR_UNSUPPORTED;
}

int32_t CAvCodecUtility::MapAndroidSampleFormat(int32_t ffmpeg_format, int32_t& out_android_format)
{
    int32_t map_count = sizeof(kMap_Sample_Format) /  sizeof(kMap_Sample_Format[0]);
    int32_t i;

    for (i= 0; i < map_count; i++) {
        if (ffmpeg_format == kMap_Sample_Format[i][1]) {
            out_android_format = kMap_Sample_Format[i][0];
            return XOK;
        }
    }

    LOGE("<CAvCodecUtility::MapAndroidSampleFormat> [ERROR] not found, ffmpeg_format=%d\n", ffmpeg_format);
    assert(0);
    return XERR_UNSUPPORTED;
}


int32_t CAvCodecUtility::MapFfmpegCodec(std::string android_codec, int32_t& out_ffmpeg_codec)
{
    int32_t map_count = sizeof(kMap_Codec) / sizeof(kMap_Codec[0]);
    int32_t i;

    for (i= 0; i < map_count; i++) {
        if (strcmp(kMap_Codec[i].android_codec.c_str(), android_codec.c_str()) == 0) {
            out_ffmpeg_codec = kMap_Codec[i].ffmpeg_codec;
            return XOK;
        }
    }

    LOGE("<CAvCodecUtility::MapFfmpegCodec> [ERROR] not found, android_format=%s\n", android_codec.c_str());
    assert(0);
    return XERR_UNSUPPORTED;
}

int32_t CAvCodecUtility::MapAndroidCodec(int32_t ffmpeg_codec, std::string& out_android_codec)
{
    int32_t map_count = sizeof(kMap_Codec) / sizeof(kMap_Codec[0]);
    int32_t i;

    for (i= 0; i < map_count; i++) {
        if (ffmpeg_codec == kMap_Codec[i].ffmpeg_codec) {
            out_android_codec = kMap_Codec[i].android_codec;
            return XOK;
        }
    }

    LOGE("<CAvCodecUtility::MapAndroidCodec> [ERROR] not found, ffmpeg_format=%d\n", ffmpeg_codec);
    assert(0);
    return XERR_UNSUPPORTED;
}







