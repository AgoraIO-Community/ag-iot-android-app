/**
 * @file AvParseProgress.hpp
 * @brief This file implement the parser progress
 * @author xiaohua.lu   luxiaohua@agora.io
 * @email 2489186909@qq.com
 * @version 1.0.0.1
 * @date 2022-02-02
 * @license Copyright (C) 2021 LuXiaoHua. All rights reserved.
 */
#include "comtypedef.hpp"
#include "AvParseProgress.hpp"


#define MAX_DEC_RETRY           20





///////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////// Public Methods ////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////
CAvParseProgress::CAvParseProgress()
{
}

CAvParseProgress::~CAvParseProgress()
{
    ResetAll();
}

void CAvParseProgress::Clone(CAvParseProgress* src_progress)
{
    input_pkt_pts_ = src_progress->input_pkt_pts_;
    last_pkt_pts_ = src_progress->last_pkt_pts_;
    decoded_pts_ = src_progress->decoded_pts_;
    dec_retry_cnt_ = src_progress->dec_retry_cnt_;

    //
    // 视频参数数据
    //
    color_format_ = src_progress->color_format_;
    video_width_ = src_progress->video_width_;
    video_height_ = src_progress->video_height_;

    //
    // 音频参数数据
    //
    sample_foramt_ = src_progress->sample_foramt_;
    channels_ = src_progress->channels_;
    sample_rate_ = src_progress->sample_rate_;

}

void CAvParseProgress::ResetAll()
{
    input_pkt_pts_ = 0;
    last_pkt_pts_ = -1;
    decoded_pts_ = 0;
    dec_retry_cnt_ = 0;
}



void CAvParseProgress::SetInputPts(int64_t pkt_pts)
{
    input_pkt_pts_ = pkt_pts;
}

int64_t CAvParseProgress::GetInputPts()
{
    return input_pkt_pts_;
}

int64_t CAvParseProgress::GetLastPtkPts()
{
    return last_pkt_pts_;
}

void CAvParseProgress::SetInputEos()
{
    last_pkt_pts_ = input_pkt_pts_;
}



void CAvParseProgress::IncreaseDecodeCount() {
    dec_retry_cnt_++;
}

void CAvParseProgress::ResetDecodeCount() {
    dec_retry_cnt_ = 0;
}

void CAvParseProgress::SetDecodedPts(int64_t pts) {
    decoded_pts_ = pts;

}
int64_t CAvParseProgress::GetDecodedPts() {
    return decoded_pts_;
}


bool CAvParseProgress::IsDecodeEos() {
    if ((last_pkt_pts_ >= 0) && (decoded_pts_ >= last_pkt_pts_)) { // 已经解码完最后一帧
        LOGD("<CAvParseProgress::IsDecodeEos> EOS, lastPktPts=%" PRId64 ", decodedPts=%" PRId64 "\n",
                last_pkt_pts_, decoded_pts_);
        return true;
    }

    return false;
}

bool CAvParseProgress::IsDecodeFail() {
    if (dec_retry_cnt_ >= MAX_DEC_RETRY) {  // 尝试超过次数
        return true;
    }

    return false;
}


void CAvParseProgress::SetVideoParam(int32_t format, int32_t width, int32_t height)
{
    color_format_ = format;
    video_width_ = width;
    video_height_ = height;
}

void CAvParseProgress::GetVideoParam(int32_t& format, int32_t& width, int32_t& height)
{
    format = color_format_;
    width = video_width_;
    height = video_height_;
}

void CAvParseProgress::SetAudioParam(int32_t format, int32_t channels, int32_t smpl_rate)
{
    sample_foramt_ = format;
    channels_ = channels;
    sample_rate_ = smpl_rate;
}

void CAvParseProgress::GetAudioParam(int32_t& format, int32_t& channels, int32_t smpl_rate)
{
    format = sample_foramt_;
    channels = channels_;
    smpl_rate = sample_rate_;
}