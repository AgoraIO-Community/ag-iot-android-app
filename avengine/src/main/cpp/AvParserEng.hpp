/**
 * @file AvParserEng.hpp
 * @brief This file define the parser engine
 * @author xiaohua.lu
 * @email 2489186909@qq.com    luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2023-02-05
 * @license Copyright (C) 2021 LuXiaoHua. All rights reserved.
 */
#ifndef __AVPARSER_ENG_H__
#define __AVPARSER_ENG_H__

#include "comtypedef.hpp"
#include "AvParseProgress.hpp"


class CAvParserEng final
{
public:
    CAvParserEng();
    virtual ~CAvParserEng();


    static int32_t distillMediaInfo(std::string file_path, AvMediaInfo* out_media_info);

    int32_t Open(SharePtr<AvDecParam> dec_param);
    int32_t Close();
    const AvMediaInfo* GetMediaInfoPtr();

    /*
     * @brief 从数据流中读取并送入一个数据包到音视频解码器
     * @param pkt_type : 表明送入的数据包类型； 0--没有送如包；  1--送入视频包； 2--送入音频包
     * @return 错误值
     */
    int32_t InputPacket(int32_t& pkt_type);


    /*
     * @brief 从解码器中解码一帧视频帧
     * @param 无
     * @return 返回的错误码, XERR_CODEC_INDATA 可以继续送入数据；
     *                    XERR_CODEC_DEC_EOS 视频流解码完成；
     *                    XERR_CODEC_DECODING 解码出现问题，不能再继续
     */
    int32_t DecodeVideoFrame();

    /*
     * @brief 返回最后一次解码的视频帧
     */
    AvVideoFrame* GetVideoFrame();



    /*
     * @brief 从解码器中解码一帧音频帧
     * @param 无
     * @return 返回的错误码, XERR_CODEC_INDATA 可以继续送入数据；
     *                    XERR_CODEC_DEC_EOS 视频流解码完成；
     *                    XERR_CODEC_DECODING 解码出现问题，不能再继续
     */
    int32_t DecodeAudioFrame();

    /*
     * @brief 返回最后一次解码的音频帧
     */
    AvAudioFrame* GetAudioFrame();



protected:
    int32_t VideoFrameConvert(const AVFrame* in_frame);
    int32_t AudioFrameConvert(const AVFrame* in_frame);

private:
    SharePtr<AvDecParam> dec_param_ = nullptr;
    AVFormatOpenContextPtr format_ctx_  = nullptr;
    AVCodecContextPtr      video_codec_ctx_ = nullptr;
    SwsContextPtr          video_sws_ctx_ = nullptr;
    AVCodecContextPtr      audio_codec_ctx_ = nullptr;
    SwrContextPtr          audio_sws_ctx_  = nullptr;
    AvMediaInfoPtr         media_info_ = nullptr;

    CAvParseProgress       video_progress_;
    CAvParseProgress       audio_progress_;
    AvVideoFramePtr        video_frame_ = nullptr;        ///< 当前解码出来的视频帧
    UniquePtr<uint8_t[]>   yuv_buffer_= nullptr;          ///< 解码后的YUV缓冲区
    AvAudioFramePtr        audio_frame_ = nullptr;        ///< 当前解码出来的音频帧

    int64_t                start_timestamp_ = 0;          ///< 初始时间戳
    int64_t                audio_duration_ = 0;           ///< 当前视频时长


};

#endif // __AVPARSER_ENG_H__
