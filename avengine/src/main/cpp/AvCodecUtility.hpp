/**
 * @file AvParserEng.hpp
 * @brief This file define the utility functions
 * @author xiaohua.lu
 * @email 2489186909@qq.com    luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-02-03
 * @license Copyright (C) 2021 LuXiaoHua. All rights reserved.
 */
#ifndef __AVCODEC_UTILITY_H__
#define __AVCODEC_UTILITY_H__

#include "comtypedef.hpp"


//
// Media Codec中定义色彩格式
//
#define COLOR_Format24bitRGB888             11
#define COLOR_Format24bitBGR888             12
#define COLOR_Format32bitBGRA8888           15
#define COLOR_Format32bitARGB8888           16
#define COLOR_FormatYUV411Planar            17
#define COLOR_FormatYUV411PackedPlanar      18
#define COLOR_FormatYUV420Planar            19
#define COLOR_FormatYUV420PackedPlanar      20
#define COLOR_FormatYUV420SemiPlanar        21
#define COLOR_FormatYUV422Planar            22
#define COLOR_FormatYUV422PackedPlanar      23
#define COLOR_FormatYUV422SemiPlanar        24
#define COLOR_FormatYCbYCr                  25
#define COLOR_FormatYCrYCb                  26
#define COLOR_FormatCbYCrY                  27
#define COLOR_FormatCrYCbY                  28
#define COLOR_FormatYUV444Interleaved       29
#define COLOR_FormatYUV420PackedSemiPlanar  39
#define COLOR_FormatYUV422PackedSemiPlanar  40
#define COLOR_Format18BitBGR666             41
#define COLOR_Format24BitARGB6666           42
#define COLOR_Format24BitABGR6666           43
#define COLOR_TI_FormatYUV420PackedSemiPlanar 0x7f000100
#define COLOR_Format32bitABGR8888             0x7F00A000
#define COLOR_FormatYUV420Flexible            0x7F420888
#define COLOR_FormatYUV422Flexible            0x7F422888
#define COLOR_FormatYUV444Flexible            0x7F444888
#define COLOR_FormatRGBFlexible               0x7F36B888
#define COLOR_FormatRGBAFlexible              0x7F36A888
#define COLOR_QCOM_FormatYUV420SemiPlanar     0x7fa30c00



//
// Android中音频采样格式
//
#define ENCODING_PCM_16BIT      2
#define ENCODING_PCM_8BIT       3
#define ENCODING_PCM_FLOAT      4
#define ENCODING_AC3            5
#define ENCODING_E_AC3          6
#define ENCODING_DTS            7
#define ENCODING_DTS_HD         8
#define ENCODING_MP3            9
#define ENCODING_AAC_LC         10
#define ENCODING_AAC_HE_V1      11
#define ENCODING_AAC_HE_V2      12
#define ENCODING_IEC61937       13
#define ENCODING_DOLBY_TRUEHD   14
#define ENCODING_AAC_ELD        15
#define ENCODING_AAC_XHE        16
#define ENCODING_AC4            17
#define ENCODING_E_AC3_JOC      18
#define ENCODING_DOLBY_MAT      19
#define ENCODING_OPUS           20
#define ENCODING_PCM_24BIT_PACKED   21;
#define ENCODING_PCM_32BIT      22



//
// Android中定义的Codec
//
#define MIMETYPE_VIDEO_VP8            "video/x-vnd.on2.vp8"
#define MIMETYPE_VIDEO_VP9            "video/x-vnd.on2.vp9"
#define MIMETYPE_VIDEO_AV1            "video/av01"
#define MIMETYPE_VIDEO_AVC            "video/avc"
#define MIMETYPE_VIDEO_HEVC           "video/hevc"
#define MIMETYPE_VIDEO_MPEG4          "video/mp4v-es"
#define MIMETYPE_VIDEO_H263           "video/3gpp"
#define MIMETYPE_VIDEO_MPEG2          "video/mpeg2"
#define MIMETYPE_VIDEO_RAW            "video/raw"
#define MIMETYPE_VIDEO_DOLBY_VISION   "video/dolby-vision"
#define MIMETYPE_VIDEO_SCRAMBLED      "video/scrambled"

#define MIMETYPE_AUDIO_AMR_NB         "audio/3gpp"
#define MIMETYPE_AUDIO_AMR_WB         "audio/amr-wb"
#define MIMETYPE_AUDIO_MPEG           "audio/mpeg"
#define MIMETYPE_AUDIO_AAC            "audio/mp4a-latm"
#define MIMETYPE_AUDIO_QCELP          "audio/qcelp"
#define MIMETYPE_AUDIO_VORBIS         "audio/vorbis"
#define MIMETYPE_AUDIO_OPUS           "audio/opus"
#define MIMETYPE_AUDIO_G711_ALAW      "audio/g711-alaw"
#define MIMETYPE_AUDIO_G711_MLAW      "audio/g711-mlaw"
#define MIMETYPE_AUDIO_RAW            "audio/raw"
#define MIMETYPE_AUDIO_FLAC           "audio/flac"
#define MIMETYPE_AUDIO_MSGSM          "audio/gsm"
#define MIMETYPE_AUDIO_AC3            "audio/ac3"
#define MIMETYPE_AUDIO_EAC3           "audio/eac3"
#define MIMETYPE_AUDIO_EAC3_JOC       "audio/eac3-joc"
#define MIMETYPE_AUDIO_AC4            "audio/ac4"
#define MIMETYPE_  AUDIO_SCRAMBLED    "audio/scrambled"





class CAvCodecUtility final
{
public:

    static int32_t MapFfmpegColorFormat(int32_t android_format, int32_t& out_ffmpeg_format);
    static int32_t MapAndroidColorFormat(int32_t ffmpeg_format, int32_t& out_android_format);

    static int32_t MapFfmpegSampleFormat(int32_t android_format, int32_t& out_ffmpeg_format);
    static int32_t MapAndroidSampleFormat(int32_t ffmpeg_format, int32_t& out_android_format);

    static int32_t MapFfmpegCodec(std::string android_codec, int32_t& out_ffmpeg_codec);
    static int32_t MapAndroidCodec(int32_t ffmpeg_codec, std::string& out_android_codec);

};

#endif // __AVCODEC_UTILITY_H__
