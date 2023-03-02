#ifndef __COMTYPEDEF_HPP__
#define __COMTYPEDEF_HPP__

#include <jni.h>

#include "BaseInclude.hpp"
#include "ErrCode.h"
#include "JNIHelper.hpp"
#include "JNIPublic.h"



#ifndef MAX
#define MAX(x, y)							( ((x) > (y)) ? (x) : (y))
#endif

#ifndef MIN
#define MIN(x, y)							( ((x) < (y)) ? (x) : (y))
#endif

#ifndef ABS
#define ABS(value)							(((value) > 0) ? (value) : -(value))
#endif



/**
 * @brief 媒体文件解码参数
 */
struct AvDecParam final {

    DELETE_COPY(AvDecParam);

    std::string in_file_path_;      ///< 输入媒体文件全路径

    int32_t color_format_;        ///< 输出视频帧色彩格式，默认固定 AV_PIX_FMT_NV12
    int32_t video_width_;         ///< 输出文件视频帧的宽度
    int32_t video_height_;        ///< 输出文件视频帧的高度

    int32_t sample_format_;      ///< 输出文件要编码的音频采样格式，Android上默认16位采样
    int32_t channels_;           ///< 输出文件要编码的音频通道数量，Android 默认2
    int32_t sample_rate_;        ///< 输出文件要编码的音频采样率，Android 默认44100
};


/**
 * @brief 媒体文件信息结构
 */
struct AvMediaInfo final
{
    DELETE_COPY(AvMediaInfo);

    std::string file_path_;          ///< 媒体文件Uri
    int64_t file_duration_ = 0;      ///< 媒体文件时长, Max(mVideoDuration, mAudioDuration)

    int32_t video_track_index_ = -1; ///< 视频流所在trackIndex
    int64_t video_duration_ = 0;     ///< 视频流时长
    int32_t video_codec_;            ///< 视频编码方式
    int32_t color_format_;           ///< 视频帧色彩格式
    int32_t color_range_;            ///< 色域空间范围
    int32_t color_space_;            ///< 色彩空间
    int32_t video_width_ = 0;        ///< 视频帧宽度
    int32_t video_height_ = 0;       ///< 视频帧高度
    int32_t rotation_ = 0;           ///< 旋转角度
    int32_t frame_rate_ = 30;        ///< 帧率
    int32_t video_bitrate_ = 0;      ///< 视频的码率，如果没有获取到则为0
    int32_t video_max_bitrate = 0;   ///< 视频的最大码率，如果没有获取到则为0

    int32_t audio_track_index_= -1;  ///< 音频流所在trackIndex
    int64_t audio_duration_ = 0;     ///< 音频流时长
    int32_t audio_codec_;            ///< 音频编码方式
    int32_t sample_foramt_;          ///< 音频采样格式
    int32_t bytes_per_sample_ = 2;   ///< 每个采样的字节数
    int32_t channels_ = 2;           ///< 音频频道数量
    int32_t sample_rate_ = 48000;    ///< 采样率
    int32_t audio_bitrate_ = 0;      ///< 音频的码率，如果没有获取到则为0
    int32_t audio_max_bitrate = 0;   ///< 音频的最大码率，如果没有获取到则为0

};



/**
 * @brief  音视频帧类
 */
struct AvBaseFrame
{
    DELETE_COPY(AvBaseFrame);

    int32_t frame_index_ = 0;            ///< 当前帧索引
    int64_t timestamp_ = 0;              ///< 帧数据对应时间戳
    bool key_frame_ = false;             ///< 当前帧是否是关键帧
    bool last_frame_ = false;            ///< 当前帧是最后一帧
    int32_t flags_ = 0;                  ///< 帧信息标记
    bool frame_valid_ = false;           ///< 当前帧是否有效
};

struct AvVideoFrame : public AvBaseFrame
{
    DELETE_COPY(AvVideoFrame);

    int32_t color_fmt_;                  ///< 帧数据格式
    int32_t width_;                      ///< 视频帧宽度
    int32_t height_;                     ///< 视频帧高度
    UniquePtr<uint8_t[]> frame_data_;    ///< 解码后的图像数据
};

struct AvAudioFrame : public AvBaseFrame
{
    DELETE_COPY(AvAudioFrame);

    int32_t sample_fmt_;               ///< 采样格式, 默认 S16
    int32_t bytes_per_sample_;         ///< 每个采样的字节数，默认是2
    int32_t channels_;                 ///< 音频通道数，Android固定只支持双通道
    int32_t sample_rate_;              ///< 采样率
    UniquePtr<uint8_t[]> sample_data_; ///< 解码后的PCM采样数据
    int32_t sample_number_;            ///< 样本数量
};

using AvDecParamPtr = std::unique_ptr<AvDecParam>;
using AvMediaInfoPtr = std::unique_ptr<AvMediaInfo>;
using AvVideoFramePtr = std::unique_ptr<AvVideoFrame>;
using AvAudioFramePtr = std::unique_ptr<AvAudioFrame>;



// copy from https://www.fluentcpp.com/2017/10/27/function-aliases-cpp
#define ALIAS_TEMPLATE_FUNCTION(highLevelF, lowLevelF)                                             \
  template<typename... Args>                                                                       \
  static inline auto highLevelF(Args &&... args)->decltype(lowLevelF(std::forward<Args>(args)...)) \
  {                                                                                                \
    return lowLevelF(std::forward<Args>(args)...);                                                 \
  }


// time like 1s, 1ms, 1us, 1ns.
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wheader-hygiene"
using namespace std::chrono_literals;
#pragma clang diagnostic pop
ALIAS_TEMPLATE_FUNCTION(SLEEP, std::this_thread::sleep_for)





#endif  // __COMTYPEDEF_HPP__
