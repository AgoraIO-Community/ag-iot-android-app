/**
 * @file AvParseProgress.hpp
 * @brief This file define the parser progress
 * @author xiaohua.lu   luxiaohua@agora.io
 * @email 2489186909@qq.com
 * @version 1.0.0.1
 * @date 2022-02-02
 * @license Copyright (C) 2021 LuXiaoHua. All rights reserved.
 */
#ifndef __AVPARSE_PROGRESS_H__
#define __AVPARSE_PROGRESS_H__

#include "comtypedef.hpp"





class CAvParseProgress final
{
public:
    CAvParseProgress();
    virtual ~CAvParseProgress();

    void Clone(CAvParseProgress* src_progress);

    void ResetAll();

    void SetInputPts(int64_t pkt_pts);
    int64_t GetInputPts();
    int64_t GetLastPtkPts();
    void SetInputEos();

    void IncreaseDecodeCount();
    void ResetDecodeCount();
    void SetDecodedPts(int64_t pts);
    int64_t GetDecodedPts();
    bool IsDecodeEos();
    bool IsDecodeFail();

    void SetVideoParam(int32_t format, int32_t width, int32_t height);
    void GetVideoParam(int32_t& format, int32_t& width, int32_t& height);

    void SetAudioParam(int32_t format, int32_t channels, int32_t smpl_rate);
    void GetAudioParam(int32_t& format, int32_t& channels, int32_t smpl_rate);

private:
    int64_t input_pkt_pts_ = 0;             ///< 当前已经送入数据包的时间戳
    int64_t last_pkt_pts_ = -1;             ///< 最后一个数据包的时间戳, -1表示还未读取到最后
    int64_t decoded_pts_ = 0;               ///< 当前已经解码帧的时间戳
    int32_t dec_retry_cnt_ = 0;             ///< 解码尝试次数

    //
    // 视频参数数据
    //
    int32_t color_format_ = 0;
    int32_t video_width_ = 0;
    int32_t video_height_ = 0;

    //
    // 音频参数数据
    //
    int32_t sample_foramt_ = 0;
    int32_t channels_ = 0;
    int32_t sample_rate_ = 0;

};

#endif // __AVPARSE_PROGRESS_H__
