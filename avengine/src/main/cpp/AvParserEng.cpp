/**
 * @file AvParserEng.cpp
 * @brief This file implement the parser engine
 * @author xiaohua.lu
 * @email 2489186909@qq.com    luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2023-02-05
 * @license Copyright (C) 2021 LuXiaoHua. All rights reserved.
 */
#include "comtypedef.hpp"
#include "AvParserEng.hpp"


#define fftime_to_milliseconds(ts) (av_rescale(ts, 1000*1000, AV_TIME_BASE))
#define milliseconds_to_fftime(ms) (av_rescale(ms, AV_TIME_BASE, 1000*1000))

//
// 用于解码输出的视频帧格式
//
#define OUT_FRAME_FORAMT                AV_PIX_FMT_NV12

int32_t ParseStreamRotateAngle(AVStream *pAvStream);
extern "C" int32_t SaveToBmp(uint8_t * rgba_data, int width, int height, const char* save_bmp_file);


///////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////// Public Methods ////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////
CAvParserEng::CAvParserEng()
{
    /* register all codecs, demux and protocols */
    avcodec_register_all();
#if CONFIG_AVDEVICE
    avdevice_register_all();
#endif
#if CONFIG_AVFILTER
    avfilter_register_all();
#endif
    av_register_all();
    avformat_network_init();
}

CAvParserEng::~CAvParserEng()
{
    avformat_network_deinit();
}


int32_t CAvParserEng::distillMediaInfo(std::string file_path, AvMediaInfo* out_media_info)
{
    int32_t ret;
    int32_t i;

    out_media_info->file_path_ = file_path;
    out_media_info->video_track_index_ = -1;
    out_media_info->audio_track_index_ = -1;

    // 打开媒体文件
    AVFormatOpenContextPtr format_ctx = AVFormatOpenContextPtrCreate(file_path.c_str());
    if (format_ctx == nullptr)
    {
        LOGE("<CAvParserEng::distillMediaInfo> [ERROR] format_ctx is NULL\n");
        return XERR_FILE_OPEN;
    }

    // 查询所有的媒体流
    ret = avformat_find_stream_info(format_ctx.get(), nullptr);
    if (ret < 0)
    {
        LOGE("<CAvParserEng::distillMediaInfo> [ERROR] fail to avformat_find_stream_info(), ret=%d", ret);
        return XERR_FILE_NO_STREAM;
    }

    // 遍历媒体流信息
    const AVCodec* pVideoCodec = nullptr;
    const AVCodec* pAudioCodec = nullptr;
    for (i = 0; i < format_ctx->nb_streams; i++)
    {
        AVStream *pAvStream = format_ctx->streams[i];
        if (pAvStream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO)
        {
            if ((pAvStream->codecpar->width <= 0) || (pAvStream->codecpar->height <= 0))
            {
                LOGE("<CAvParserEng::distillMediaInfo> [ERROR] invalid resolution, streamIndex=%d\n", i);
                continue;
            }

            // 视频解码器
            pVideoCodec = avcodec_find_decoder(pAvStream->codecpar->codec_id);
            if (pVideoCodec == nullptr)
            {
                LOGE("<CAvParserEng::distillMediaInfo> [ERROR] can not find video codecId=%d\n",
                     pAvStream->codecpar->codec_id);
                continue;
            }

            // 解析视频流信息
            out_media_info->video_track_index_ = i;
            out_media_info->video_codec_ = static_cast<int32_t>(pAvStream->codecpar->codec_id);
            out_media_info->video_duration_ =
                    static_cast<int64_t>(pAvStream->duration * 1000 * av_q2d(pAvStream->time_base) * 1000);
            out_media_info->color_format_ = pAvStream->codecpar->format;
            out_media_info->color_range_ = pAvStream->codecpar->color_range;
            out_media_info->color_space_ = pAvStream->codecpar->color_space;
            out_media_info->video_width_  = pAvStream->codecpar->width;
            out_media_info->video_height_ = pAvStream->codecpar->height;
            out_media_info->rotation_ = ParseStreamRotateAngle(pAvStream);
            out_media_info->video_bitrate_ = pAvStream->codecpar->bit_rate;
            if ((pAvStream->r_frame_rate.num > 0) && (pAvStream->r_frame_rate.den > 0)) {
                out_media_info->frame_rate_ = static_cast<int32_t>((float)(pAvStream->r_frame_rate.num) / (float)(pAvStream->r_frame_rate.den) + 0.5f);
            } else if ((pAvStream->avg_frame_rate.num > 0) && (pAvStream->avg_frame_rate.den > 0)) {
                out_media_info->frame_rate_ = static_cast<int32_t>((float)(pAvStream->r_frame_rate.num) / (float)(pAvStream->r_frame_rate.den) + 0.5f);
            }

            LOGD("<CAvParserEng::distillMediaInfo> [VIDEO] idx=%d, codec=%d, fmt=%d, w=%d, h=%d, rotation=%d, fps=%d, duration=%" PRId64 ", bitrate=%d\n",
                 out_media_info->video_track_index_, out_media_info->video_codec_, out_media_info->color_format_,
                 out_media_info->video_width_, out_media_info->video_height_, out_media_info->rotation_,
                 out_media_info->frame_rate_, out_media_info->video_duration_,
                 out_media_info->video_bitrate_);

        } else if (pAvStream->codecpar->codec_type == AVMEDIA_TYPE_AUDIO)
        {
            // 音频解码器
            pAudioCodec = avcodec_find_decoder(pAvStream->codecpar->codec_id);
            if (pAudioCodec == nullptr)
            {
                LOGE("<CAvParserEng::distillMediaInfo> [ERROR] can not find audio codecId=%d\n",
                     pAvStream->codecpar->codec_id);
                continue;
            }

            // 解析音频流信息
            out_media_info->audio_track_index_ = i;
            out_media_info->audio_codec_ = static_cast<int32_t>(pAvStream->codecpar->codec_id);
            out_media_info->audio_duration_ =
                    static_cast<int64_t>(pAvStream->duration * 1000 * av_q2d(pAvStream->time_base) * 1000);
            out_media_info->sample_foramt_ = static_cast<int32_t>(pAvStream->codecpar->format);
            out_media_info->bytes_per_sample_  = av_get_bytes_per_sample((enum AVSampleFormat)(pAvStream->codecpar->format));
            out_media_info->channels_ = pAvStream->codecpar->channels;
            out_media_info->sample_rate_ = pAvStream->codecpar->sample_rate;
            out_media_info->audio_bitrate_ = pAvStream->codecpar->bit_rate;

            LOGD("<CAvParserEng::distillMediaInfo> [AUDIO] idx=%d, codec=%d, fmt=%d, bytesPerSmpl=%d, channels=%d, samplerate=%d, duration=%" PRId64 ", bitrate=%d\n",
                 out_media_info->audio_track_index_, out_media_info->audio_codec_,
                 out_media_info->sample_foramt_, out_media_info->bytes_per_sample_,
                 out_media_info->channels_, out_media_info->sample_rate_,
                 out_media_info->audio_duration_, out_media_info->audio_bitrate_);
        }
    }
    if (out_media_info->video_duration_ > out_media_info->audio_duration_) {
        out_media_info->file_duration_ = out_media_info->video_duration_;
    } else {
        out_media_info->file_duration_ = out_media_info->audio_duration_;
    }

    LOGD("<CAvParserEng::distillMediaInfo> successful!");
    return XOK;
}


int32_t ParseStreamRotateAngle(AVStream *pAvStream)
{
    AVDictionaryEntry* dict_rotate = av_dict_get(pAvStream->metadata, "rotate", NULL, 0);
    double_t theta = (dict_rotate == NULL) ? 0 : atoi(dict_rotate->value);

    uint8_t* display_mtx = av_stream_get_side_data(pAvStream,AV_PKT_DATA_DISPLAYMATRIX, NULL);
    if (display_mtx != NULL) {
        theta = -av_display_rotation_get((int32_t *)display_mtx);
    }

    theta -= 360*floor(theta/360 + 0.9/360);
    if (fabs(theta - 90*round(theta/90)) > 2) {
    }

    int32_t rotation = 0;
    if (fabs(theta - 90) < 1.0)
    {
        rotation = 90;
    }

    else if (fabs(theta - 180) < 1.0 || fabs(theta + 180) < 1.0)
    {
        rotation = 180;
    }
    else if(fabs(theta - 270) < 1.0 || fabs(theta + 90) < 1.0)
    {
        rotation = 270;
    }
    else
    {
    }

    LOGE("<CAvParserEng::ParseStreamRotateAngle> rotation=%d", rotation);
    return rotation;
}

int32_t CAvParserEng::Open(SharePtr<AvDecParam> dec_param)
{
    int32_t ret;
    int32_t i;

    dec_param_ = dec_param;
    media_info_ = MakeUniquePtr<AvMediaInfo>();

    // 打开媒体文件
    format_ctx_ = AVFormatOpenContextPtrCreate(dec_param_->in_file_path_.c_str());
    if (format_ctx_ == nullptr)
    {
        LOGE("<CAvParserEng::Open> [ERROR] format_ctx_ is NULL\n");
        return XERR_FILE_OPEN;
    }

    // 查询所有的媒体流
    ret = avformat_find_stream_info(format_ctx_.get(), nullptr);
    if (ret < 0)
    {
        LOGE("<CAvParserEng::Open> [ERROR] fail to avformat_find_stream_info(), ret=%d", ret);
        return XERR_FILE_NO_STREAM;
    }

    // 遍历媒体流信息
    const AVCodec* pVideoCodec = nullptr;
    const AVCodec* pAudioCodec = nullptr;
    for (i = 0; i < format_ctx_->nb_streams; i++)
    {
        AVStream *pAvStream = format_ctx_->streams[i];
        if (pAvStream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO)
        {
            if ((pAvStream->codecpar->width <= 0) || (pAvStream->codecpar->height <= 0))
            {
                LOGE("<CAvParserEng::Open> [ERROR] invalid resolution, streamIndex=%d\n", i);
                continue;
            }

            // 视频解码器
            pVideoCodec = avcodec_find_decoder(pAvStream->codecpar->codec_id);
            if (pVideoCodec == nullptr)
            {
                LOGE("<CAvParserEng::Open> [ERROR] can not find video codecId=%d\n",
                        pAvStream->codecpar->codec_id);
                continue;
            }

            // 解析视频流信息
            media_info_->video_track_index_ = i;
            media_info_->video_codec_ = static_cast<int32_t>(pAvStream->codecpar->codec_id);
            media_info_->video_duration_ =
                    static_cast<int64_t>(pAvStream->duration * 1000 * av_q2d(pAvStream->time_base) * 1000);
            media_info_->color_format_ = pAvStream->codecpar->format;
            media_info_->color_range_ = pAvStream->codecpar->color_range;
            media_info_->color_space_ = pAvStream->codecpar->color_space;
            media_info_->video_width_  = pAvStream->codecpar->width;
            media_info_->video_height_ = pAvStream->codecpar->height;
            media_info_->rotation_ = ParseStreamRotateAngle(pAvStream);
            if ((pAvStream->r_frame_rate.num > 0) && (pAvStream->r_frame_rate.den > 0)) {
                media_info_->frame_rate_ = static_cast<int32_t>((float)(pAvStream->r_frame_rate.num) / (float)(pAvStream->r_frame_rate.den) + 0.5f);
            } else if ((pAvStream->avg_frame_rate.num > 0) && (pAvStream->avg_frame_rate.den > 0)) {
                media_info_->frame_rate_ = static_cast<int32_t>((float)(pAvStream->r_frame_rate.num) / (float)(pAvStream->r_frame_rate.den) + 0.5f);
            } else {
                media_info_->frame_rate_ = 30;
            }

            if (dec_param_->video_width_ <= 0) {
                dec_param_->video_width_ = pAvStream->codecpar->width;
                LOGD("<CAvParserEng::Open> [VIDEO] output original video width!");
            }
            if (dec_param_->video_height_ <= 0) {
                dec_param_->video_height_ = pAvStream->codecpar->height;
                LOGD("<CAvParserEng::Open> [VIDEO] output original video height!");
            }
            if ((media_info_->video_duration_ <= 0) && (format_ctx_->duration > 0)) {
                media_info_->video_duration_ = format_ctx_->duration;
            }

            LOGD("<CAvParserEng::Open> [VIDEO] idx=%d, codec=%d, fmt=%d, w=%d, h=%d, rotation=%d, fps=%d, duration=%" PRId64 "\n",
                    media_info_->video_track_index_, media_info_->video_codec_, media_info_->color_format_,
                    media_info_->video_width_, media_info_->video_height_, media_info_->rotation_,
                    media_info_->frame_rate_, media_info_->video_duration_);

        } else if (pAvStream->codecpar->codec_type == AVMEDIA_TYPE_AUDIO)
        {
            // 音频解码器
            pAudioCodec = avcodec_find_decoder(pAvStream->codecpar->codec_id);
            if (pAudioCodec == nullptr)
            {
                LOGE("<CAvParserEng::Open> [ERROR] can not find audio codecId=%d\n",
                        pAvStream->codecpar->codec_id);
                continue;
            }

            // 解析音频流信息
            media_info_->audio_track_index_ = i;
            media_info_->audio_codec_ = static_cast<int32_t>(pAvStream->codecpar->codec_id);
            media_info_->audio_duration_ =
                    static_cast<int64_t>(pAvStream->duration * 1000 * av_q2d(pAvStream->time_base) * 1000);
            media_info_->sample_foramt_ = static_cast<int32_t>(pAvStream->codecpar->format);
            media_info_->bytes_per_sample_  = av_get_bytes_per_sample((enum AVSampleFormat)(pAvStream->codecpar->format));
            media_info_->channels_ = pAvStream->codecpar->channels;
            media_info_->sample_rate_ = pAvStream->codecpar->sample_rate;
            if ((media_info_->audio_duration_ <= 0) && (format_ctx_->duration > 0)) {
                media_info_->audio_duration_ = format_ctx_->duration;
            }

            LOGD("<CAvParserEng::Open> [AUDIO] idx=%d, codec=%d, fmt=%d, bytesPerSmpl=%d, channels=%d, samplerate=%d, duration=%" PRId64 "\n",
                    media_info_->audio_track_index_, media_info_->audio_codec_,
                    media_info_->sample_foramt_, media_info_->bytes_per_sample_,
                    media_info_->channels_, media_info_->sample_rate_,
                    media_info_->audio_duration_);
        }
    }
    if (media_info_->video_duration_ > media_info_->audio_duration_) {
        media_info_->file_duration_ = media_info_->video_duration_;
    } else {
        media_info_->file_duration_ = media_info_->audio_duration_;
    }
    media_info_->file_path_ = dec_param_->in_file_path_;


    //
    // 打开 视频解码器 和 视频格式转换器
    //
    if ((pVideoCodec != nullptr) && (media_info_->video_track_index_ >= 0))
    {
        AVStream* pVideoStream = format_ctx_->streams[media_info_->video_track_index_];

        video_codec_ctx_ = AVCodecContextPtrCreate(pVideoCodec);
        if (video_codec_ctx_ == nullptr)
        {
            LOGE("<CAvParserEng::Open> [ERROR] fail to video AVCodecContextPtrCreate()\n");
            Close();
            return XERR_CODEC_OPEN;
        }
        avcodec_parameters_to_context(video_codec_ctx_.get(), pVideoStream->codecpar);

        ret = avcodec_open2(video_codec_ctx_.get(), nullptr, nullptr);
        if (ret < 0)
        {
            LOGE("<CAvParserEng::Open> [ERROR] video fail to avcodec_open2(), ret=%d\n", ret);
            Close();
            return XERR_CODEC_OPEN;
        }
        avcodec_flush_buffers(video_codec_ctx_.get());


        // 视频帧格式转换器，源视频帧和目标视频帧保持一样大小，固定输出ARGB格式
        video_sws_ctx_ = SwsContextPtr(sws_getContext(
                pVideoStream->codecpar->width, pVideoStream->codecpar->height,
                static_cast<AVPixelFormat>(pVideoStream->codecpar->format),
                pVideoStream->codecpar->width, pVideoStream->codecpar->height, OUT_FRAME_FORAMT,
                SWS_BICUBIC, nullptr, nullptr, nullptr));
        if (video_sws_ctx_ == nullptr)
        {
            LOGE("<CAvParserEng::Open> [ERROR] fail to video sws_getContext()\n");
            Close();
            return XERR_CODEC_OPEN;
        }

        // 分配解码后的视频帧缓冲区
        video_frame_ = MakeUniquePtr<AvVideoFrame>();
        video_frame_->color_fmt_ = OUT_FRAME_FORAMT;
        video_frame_->width_ = dec_param_->video_width_;
        video_frame_->height_ = dec_param_->video_height_;
        video_frame_->frame_index_ = 0;
        int32_t buff_size = dec_param_->video_width_ * dec_param_->video_height_ * 4;
        video_frame_->frame_data_ = MakeUniquePtr<uint8_t[]>(buff_size);
        video_frame_->frame_valid_ = false;

        LOGD("<CAvParserEng::Open> Open video decoder and converter successful!");
    }

    //
    // 打开 音频解码器 和 音频格式转换器
    //
    if ((pAudioCodec != nullptr) && (media_info_->audio_track_index_ >= 0)) {
        AVStream* pAudioStream = format_ctx_->streams[media_info_->audio_track_index_];

        audio_codec_ctx_ = AVCodecContextPtrCreate(pAudioCodec);
        if (audio_codec_ctx_ == nullptr)
        {
            LOGE("<CAvParserEng::Open> [ERROR] fail to audio AVCodecContextPtrCreate()\n");
            Close();
            return XERR_CODEC_OPEN;
        }
        avcodec_parameters_to_context(audio_codec_ctx_.get(), pAudioStream->codecpar);

        ret = avcodec_open2(audio_codec_ctx_.get(), nullptr, nullptr);
        if (ret < 0)
        {
            LOGE("<CAvParserEng::Open> [ERROR] audio fail to avcodec_open2(), ret=%d\n", ret);
            Close();
            return XERR_CODEC_OPEN;
        }
        avcodec_flush_buffers(audio_codec_ctx_.get());


        //
        // 创建 音频格式转换器
        //
        int64_t in_channel_layout = av_get_default_channel_layout(audio_codec_ctx_->channels);
        int     out_chnl_layout   = (dec_param_->channels_ == 1) ? AV_CH_LAYOUT_MONO : AV_CH_LAYOUT_STEREO;
        AVSampleFormat out_smpl_fmt= (enum AVSampleFormat)(dec_param_->sample_format_);
        int64_t dst_nb_samples    = av_rescale_rnd(audio_codec_ctx_->frame_size, dec_param_->sample_rate_,
                                                   audio_codec_ctx_->sample_rate, AV_ROUND_UP);
        audio_sws_ctx_  = SwrContextPtrCreate();
        swr_alloc_set_opts( audio_sws_ctx_.get(),
                            out_chnl_layout, out_smpl_fmt, dec_param_->sample_rate_,
                            in_channel_layout, audio_codec_ctx_->sample_fmt, audio_codec_ctx_->sample_rate,
                            0, nullptr);
        ret = swr_init(audio_sws_ctx_.get());
        if (ret < 0)
        {
            LOGE("<CAvParserEng::Open> [ERROR] fail to audio swr_init()\n");
            Close();
            return XERR_CODEC_OPEN;
        }

        // 分配解码后的音频帧缓冲区
        audio_frame_ = MakeUniquePtr<AvAudioFrame>();
        audio_frame_->sample_fmt_ = out_smpl_fmt;
        audio_frame_->bytes_per_sample_ = av_get_bytes_per_sample(out_smpl_fmt);
        audio_frame_->channels_ = dec_param_->channels_;
        audio_frame_->sample_rate_ = dec_param_->sample_rate_;
        audio_frame_->frame_index_ = 0;
        int32_t max_smpl_size = audio_frame_->bytes_per_sample_ * audio_frame_->channels_ * audio_frame_->sample_rate_;
        audio_frame_->sample_data_ = MakeUniquePtr<uint8_t[]>(max_smpl_size);;
        audio_frame_->frame_valid_ = false;

        LOGD("<CAvParserEng::Open> Open audio decoder and converter successful!");
    }

    video_progress_.ResetAll();
    audio_progress_.ResetAll();
    audio_duration_ = 0;
    start_timestamp_ = format_ctx_->start_time;

    LOGD("<CAvParserEng::Open> successful, start_timestamp_=%" PRId64 "\n", start_timestamp_);
    return XOK;
}

int32_t CAvParserEng::Close()
{
    if (video_codec_ctx_ != nullptr) {
        video_codec_ctx_.reset();
    }

    if (video_sws_ctx_ != nullptr) {
        video_sws_ctx_.reset();
    }

    if (audio_codec_ctx_ != nullptr) {
        audio_codec_ctx_.reset();
    }

    if (audio_sws_ctx_ != nullptr) {
        audio_sws_ctx_.reset();
    }


    if (format_ctx_ != nullptr) {
        format_ctx_.reset();
        LOGD("<CAvParserEng::Close> done");
    }

    if (video_frame_ != nullptr) {
        video_frame_->frame_data_.reset();
        video_frame_.reset();
    }

    if (audio_frame_ != nullptr) {
        audio_frame_->sample_data_.reset();
        audio_frame_.reset();
    }
    return XOK;
}

const AvMediaInfo* CAvParserEng::GetMediaInfoPtr() {
    return (media_info_.get());
}


int32_t CAvParserEng::InputPacket(int32_t& pkt_type)
{
    int32_t ret;
    pkt_type = 0;

    //
    // 读取一个数据包
    //
    AVPacketPtr packet = AVPacketPtrCreate();
    ret = av_read_frame(format_ctx_.get(), packet.get());
    if (ret == AVERROR_EOF) {   // 媒体文件读取结束
        video_progress_.SetInputEos();   // 标记最有一帧音视频帧的时间戳
        audio_progress_.SetInputEos();
        LOGE("<CAvParserEng::InputPacket> av_read_frame() EOF, lastVidPst=%"
            PRId64 ", lastAudPts=%" PRId64 "\n",
            video_progress_.GetLastPtkPts(), audio_progress_.GetLastPtkPts() );
        return XERR_CODEC_DEC_EOS;

    } else if (ret < 0) {   // 数据包读取失败
        LOGE("<CAvParserEng::InputPacket> [ERROR] fail to av_read_frame(), ret=%d\n", ret);
    }


    //
    // 将数据包送入相应的解码器
    //
    if (packet->stream_index == media_info_->video_track_index_) {
        video_progress_.SetInputPts(packet->pts);   // 设置当前视频包时间戳
        pkt_type = 1;
        ret = avcodec_send_packet(video_codec_ctx_.get(), packet.get());    // 送入视频解码器
    } else  if (packet->stream_index == media_info_->audio_track_index_) {
        audio_progress_.SetInputPts(packet->pts);   // 设置当前音频包时间戳
        pkt_type = 2;
        ret = avcodec_send_packet(audio_codec_ctx_.get(), packet.get());    // 送入音频解码器
    }
    if (ret == AVERROR(EAGAIN))  { // 没有数据送入,但是可以继续可以从内部缓冲区读取编码后的视频包
        LOGD("<CAvParserEng::InputPacket> avcodec_send_frame() EAGAIN\n");

    } else if (ret == AVERROR_EOF) {  // 数据包送入结束不再送入,但是可以继续可以从内部缓冲区读取编码后的视频包
        LOGE("<CAvParserEng::InputPacket> avcodec_send_frame() EOF\n");
        // LOGD("<DecodePktToFrame> avcodec_send_frame() AVERROR_EOF\n");
        ret = XERR_CODEC_DEC_EOS;

    } else if (ret < 0) { // 送入输入数据包失败
        LOGE("<CAvParserEng::InputPacket> [ERROR] fail to avcodec_send_frame(), ret=%d\n", ret);
    }

    LOGD("<CAvParserEng::InputPacket> done, index=%d, pts=%" PRId64 ", size=%d, flags=%d, ret=%d\n",
            packet->stream_index, packet->pts, packet->size, packet->flags, ret);

    packet.reset();
    return ret;
}

/*
 * @brief 从解码器中解码一帧视频帧
 * @param 无
 * @return 返回的错误码
 */
int32_t CAvParserEng::DecodeVideoFrame()
{
    int ret;

    if (video_codec_ctx_ == nullptr) {
        return XERR_BAD_STATE;
    }

    // 标记视频帧数据无效
    video_frame_->frame_valid_ = false;

    AVFramePtr decoded_frame = AVFramePtrCreate();
    ret = avcodec_receive_frame(video_codec_ctx_.get(), decoded_frame.get());
    if (ret == AVERROR(EAGAIN)) // 当前这次没有解码后的音视频帧输出,需要 avcodec_send_packet()送入更多的数据
    {
        LOGD("<CAvParserEng::DecodeVideoFrame> no data output\n");
        decoded_frame.reset();
        video_progress_.ResetDecodeCount();
        if (video_progress_.IsDecodeEos())  { // 视频解码完成
             LOGD("<CAvParserEng::DecodeVideoFrame> video decoding EOS\n");
            return XERR_CODEC_DEC_EOS;
        }
        return XERR_CODEC_INDATA;

    }
    else if (ret == AVERROR_EOF) // 解码缓冲区已经刷新完成,后续不再有数据输出
    {
        LOGD("<CAvParserEng::DecodeVideoFrame> decoder is EOF\n");
        decoded_frame.reset();
        return XERR_CODEC_DEC_EOS;

    } else if (ret < 0) {
        LOGE("<CAvParserEng::DecodeVideoFrame> [ERROR] fail to avcodec_receive_packet(), ret=%d\n", ret);
        decoded_frame.reset();
        video_progress_.IncreaseDecodeCount();
        if (video_progress_.IsDecodeEos())  { // 视频解码完成
            LOGD("<CAvParserEng::DecodeVideoFrame> video decoding EOS\n");
            return XERR_CODEC_DEC_EOS;
        }
        return XERR_CODEC_DECODING;
    }
    video_progress_.ResetDecodeCount();
    video_progress_.SetDecodedPts(decoded_frame->pts);

    ret = VideoFrameConvert(decoded_frame.get());
    if (ret != XOK) {
        LOGE("<CAvParserEng::DecodeVideoFrame> [ERROR] fail to VideoFrameConvert(), ret=%d\n", ret);
        decoded_frame.reset();
        return ret;
    }
    video_progress_.SetVideoParam(video_frame_->color_fmt_, video_frame_->width_, video_frame_->height_);

    int64_t pts = decoded_frame->pts;
    decoded_frame.reset();
    LOGD("<CAvParserEng::DecodeVideoFrame> decoded, format=%d, w=%d, h=%d, timestamp=%" PRId64 " , flags=%d, pts=%" PRId64 " \n",
         video_frame_->color_fmt_, video_frame_->width_, video_frame_->height_,
         video_frame_->timestamp_, video_frame_->flags_, pts);

    if (video_progress_.IsDecodeEos()) { // 视频解码完成
        LOGD("<CAvParserEng::DecodeVideoFrame> video decoding EOS\n");
        return XERR_CODEC_DEC_EOS;
    }

    return XOK;
}

/*
 * @brief 返回最后一次解码的视频帧
 */
AvVideoFrame* CAvParserEng::GetVideoFrame()
{
    return video_frame_.get();
}


/*
 * @brief 从解码器中解码一帧音频帧
 * @param 无
 * @return 返回的错误码
 */
int32_t CAvParserEng::DecodeAudioFrame()
{
    int ret;

    if (audio_codec_ctx_ == nullptr) {
        return XERR_BAD_STATE;
    }

    // 标记音频帧数据无效
    audio_frame_->frame_valid_ = false;

    AVFramePtr decoded_frame = AVFramePtrCreate();
    ret = avcodec_receive_frame(audio_codec_ctx_.get(), decoded_frame.get());
    if (ret == AVERROR(EAGAIN)) // 当前这次没有解码后的音视频帧输出,需要 avcodec_send_packet()送入更多的数据
    {
        LOGD("<CAvParserEng::DecodeAudioFrame> no data output\n");
        decoded_frame.reset();
        audio_progress_.ResetDecodeCount();
        if (audio_progress_.IsDecodeEos())  { // 音频解码完成
            LOGD("<CAvParserEng::DecodeAudioFrame> audio decoding EOS\n");
            return XERR_CODEC_DEC_EOS;
        }
        return XERR_CODEC_INDATA;

    } else if (ret == AVERROR_EOF) // 解码缓冲区已经刷新完成,后续不再有数据输出
    {
        LOGD("<CAvParserEng::DecodeAudioFrame> decoder is EOF\n");
        decoded_frame.reset();
        return XERR_CODEC_DEC_EOS;

    } else if (ret < 0) {
        LOGE("<CAvParserEng::DecodeAudioFrame> [ERROR] fail to avcodec_receive_packet(), ret=%d\n", ret);
        audio_progress_.IncreaseDecodeCount();
        if (audio_progress_.IsDecodeEos())  { // 音频解码完成
            LOGD("<CAvParserEng::DecodeAudioFrame> audio decoding EOS\n");
            return XERR_CODEC_DEC_EOS;
        }
        return XERR_CODEC_DECODING;
    }
    audio_progress_.ResetDecodeCount();
    audio_progress_.SetDecodedPts(decoded_frame->pts);

    ret = AudioFrameConvert(decoded_frame.get());
    if (ret != XOK) {
        LOGE("<CAvParserEng::DecodeAudioFrame> [ERROR] fail to AudioFrameConvert(), ret=%d\n", ret);
        decoded_frame.reset();
        return ret;
    }
    audio_progress_.SetAudioParam(audio_frame_->sample_fmt_, audio_frame_->channels_, audio_frame_->sample_rate_);


    int64_t pts = decoded_frame->pts;
    decoded_frame.reset();
    LOGD("<CAvParserEng::DecodeAudioFrame> decoded, format=%d, smplrate=%d, samples=%d, timestamp=%" PRId64 " , flags=%d, pts=%" PRId64 " \n",
         audio_frame_->sample_fmt_, audio_frame_->sample_rate_, audio_frame_->sample_number_,
         audio_frame_->timestamp_, audio_frame_->flags_, pts);

    if (audio_progress_.IsDecodeEos()) { // 音频解码完成
        LOGD("<CAvParserEng::DecodeAudioFrame> audio decoding EOS\n");
        return XERR_CODEC_DEC_EOS;
    }

    return XOK;
}

/*
 * @brief 返回最后一次解码的音频帧
 */
AvAudioFrame* CAvParserEng::GetAudioFrame()
{
    return audio_frame_.get();
}


/**
  * @brief 视频帧格式转码，转码结果输出到 video_frame_ 中
  * @param in_frame  ：输入原始视频帧
  * @return 返回错误码, 0: 转码成功;  other: 出错
  */
int32_t CAvParserEng::VideoFrameConvert(const AVFrame* in_frame)
{
    int      out_linesize[4] = {0};
    uint8_t* out_data[4]     = {nullptr};
    int32_t  y_size = video_frame_->width_ * video_frame_->height_;
    int32_t  uv_size = y_size / 4;

    out_linesize[0] = video_frame_->width_;
    out_linesize[1] = video_frame_->width_;
    out_data[0] = video_frame_->frame_data_.get();
    out_data[1] = out_data[0] + y_size;

    memset(out_data[0], 0x00, (y_size + uv_size + uv_size));

    int res = sws_scale(video_sws_ctx_.get(),
                        static_cast<const uint8_t* const*>(in_frame->data),
                        in_frame->linesize,
                        0,
                        video_frame_->height_,
                        out_data,
                        out_linesize);
    if (res < 0)
    {
        LOGE("<CAvParserEng::VideoFrameConvert> [ERROR] fail to sws_scale(), res=%d\n", res);
        return XERR_CODEC_DECODING;
    }


    // 时间戳，Android层的时间戳是微秒，当前视频帧时间时间戳 = (视频帧时间戳 - 启动时间戳)
    AVStream* pVideoStream = format_ctx_->streams[media_info_->video_track_index_];
    int64_t frame_time =  (int64_t)(in_frame->pts * 1000 * av_q2d(pVideoStream->time_base) * 1000);
    int64_t timestamp = frame_time - start_timestamp_;
    LOGD("<CAvParserEng::VideoFrameConvert> pts=%" PRId64 ", start_time=%" PRId64 ", frame_time=%" PRId64 ", timestamp=%" PRId64 " \n",
         in_frame->pts, start_timestamp_, frame_time, timestamp);

    // 填充视频帧信息字段
    video_frame_->frame_index_++;
    video_frame_->timestamp_ = timestamp;
    video_frame_->key_frame_ = in_frame->key_frame;
    video_frame_->last_frame_ = false;
    video_frame_->flags_ = in_frame->flags;
    video_frame_->frame_valid_ = true;          // 视频帧有效

    video_frame_->color_fmt_ = OUT_FRAME_FORAMT;
    video_frame_->width_  = dec_param_->video_width_;
    video_frame_->height_ = dec_param_->video_height_;

#if 0
    char dump_file[100] = {0};
    int time = (int)(timestamp / 1000);
    static int dump_cnt = 0;
    sprintf(dump_file, "/sdcard/zdump/%dx%d_%06d_%d.I420",
            in_frame->width, in_frame->height, dump_cnt, time);
    FILE* fp = fopen(dump_file, "wb");
    if (fp != NULL) {
        int32_t data_size = in_frame->linesize[0] * in_frame->height;
        fwrite(in_frame->data[0], data_size, 1, fp);

        data_size = in_frame->linesize[1] * in_frame->height;
        fwrite(in_frame->data[1], data_size, 1, fp);

        data_size = in_frame->linesize[2] * in_frame->height;
        fwrite(in_frame->data[2], data_size, 1, fp);

        fclose(fp);
    }

    //sprintf(dump_file, "/sdcard/zdump/frm_%06d_%d.bmp", dump_cnt, time);
    //SaveToBmp(data[0], video_frame_->width_, video_frame_->height_, dump_file);
    dump_cnt++;
#endif

    return XOK;
}



/**
  * @brief 音频格式重采样转码，转码结果输出到 audio_frame_ 中
  * @param in_frame  ：入原始音频帧
  * @return 返回错误码, 0: 转码成功;  other: 出错
  */
int32_t CAvParserEng::AudioFrameConvert(const AVFrame* in_frame)
{
    int64_t scale_samples = av_rescale_rnd(in_frame->nb_samples, dec_param_->sample_rate_,
                                           audio_codec_ctx_->sample_rate, AV_ROUND_UP);
    uint8_t* out_buffer[8] = {nullptr};
    out_buffer[0] = audio_frame_->sample_data_.get();

    int32_t cvted_samples = swr_convert(audio_sws_ctx_.get(),
                                        const_cast<uint8_t **>(out_buffer),
                                        (int) scale_samples,
                                        const_cast<const uint8_t **>(in_frame->data),
                                        in_frame->nb_samples);
    if (cvted_samples <= 0) {
        LOGE("<CAvParserEng::AudioFrameConvert> [ERROR] no data for swr_convert()\n");
        return XERR_CODEC_DECODING;
    }


    // 时间戳，Android层的时间戳是微秒，当前视频帧时间时间戳，自己计算还是最准确
    AVStream* pAudioStream = format_ctx_->streams[media_info_->audio_track_index_];
//    int64_t frame_time =  (int64_t)(in_frame->pts * 1000 * av_q2d(pAudioStream->time_base) * 1000);
//    int64_t timestamp = frame_time - start_timestamp_;
//    LOGD("<CAvParserEng::AudioFrameConvert> pts=%" PRId64 ", start_time=%" PRId64 ", frame_time=%" PRId64 ", timestamp=%" PRId64 " \n",
//         in_frame->pts, start_timestamp_, frame_time, timestamp);

    // 填充音频帧信息字段
    audio_frame_->frame_index_++;
    audio_frame_->timestamp_ = audio_duration_;
    audio_frame_->key_frame_ = in_frame->key_frame;
    audio_frame_->last_frame_ = false;
    audio_frame_->flags_ = in_frame->flags;
    audio_frame_->frame_valid_ = true;          // 音频帧有效

    audio_frame_->sample_fmt_ = dec_param_->sample_format_;
    audio_frame_->bytes_per_sample_ = av_get_bytes_per_sample((enum AVSampleFormat)(dec_param_->sample_format_));
    audio_frame_->channels_ = dec_param_->channels_;
    audio_frame_->sample_rate_ = dec_param_->sample_rate_;
    audio_frame_->sample_number_ = cvted_samples;       // 转换后的样本数量

    if (dec_param_->sample_rate_ > 0) {  // 根据数据长度计算音频时间戳
        int64_t frame_duration = (int64_t)cvted_samples * 1000L * 1000L / (int64_t)(dec_param_->sample_rate_);
        audio_duration_ += frame_duration;
        LOGD("<CAvParserEng::AudioFrameConvert> cvted_samples=%d, frame_duration=%" PRId64 ", audio_duration_=%" PRId64 " \n",
             cvted_samples, frame_duration, audio_duration_);
    }

    return XOK;
}



////////////////////////////////////////////////////////////////////////////////////
/////////////////////////// Innternal Utility Functions ////////////////////////////
////////////////////////////////////////////////////////////////////////////////////

//
// Bmp file structure
//
#define XBMP_FILEHDR_LEN				14
typedef struct _tag_XBmpFileHeader
{
    uint16_t			bfType;
    uint32_t			bfSize;
    uint16_t			bfReserved1;
    uint16_t			bfReserved2;
    uint32_t			bfOffBits;
}XBMPFILE_HEADER, *LPXBMPFILE_HEADER;

#define XBMP_INFOHDR_LEN				40
typedef struct _tag_XBMPINFO_HEADER
{
    uint32_t			biSize;
    int32_t				biWidth;
    int32_t				biHeight;
    uint16_t			biPlanes;
    uint16_t			biBitCount;
    uint32_t			biCompression;
    uint32_t			biSizeImage;
    int32_t				biXPelsPerMeter;
    int32_t				biYPelsPerMeter;
    uint32_t			biClrUsed;
    uint32_t			biClrImportant;
} XBMPINFO_HEADER, *LPXBMPINFO_HEADER;

#define XBMP_RGBQUAD_LEN				4
typedef struct _tag_XBmp_RGBQUAD
{
    uint8_t				rgbBlue;
    uint8_t				rgbGreen;
    uint8_t				rgbRed;
    uint8_t				rgbReserved;
} XBMP_RGBQUAD, *LPXBMP_XBMP_RGBQUAD;


extern "C" int32_t SaveToBmp(uint8_t * rgba_data, int width, int height, const char* save_bmp_file)
{
    FILE* file_handler = fopen(save_bmp_file, "wb");
    if (file_handler == nullptr)
    {
        LOGE("<SaveToBmp> fail to fopen(), file=%s", save_bmp_file);
        return XERR_FILE_OPEN;
    }


    // 写入文件头信息
    int32_t pixel_bytes = 4;
    int32_t line_bytes_ = width * pixel_bytes;

    int32_t  bit_count  = pixel_bytes * 8;
    uint16_t temp_16bit = 0x4D42;
    uint32_t temp_32bit = static_cast<uint32_t>(54 + line_bytes_ * height);
    if (bit_count == 8)
        temp_32bit += XBMP_RGBQUAD_LEN * 256;
    else if (bit_count == 16)
        temp_32bit += 16;
    fwrite(&temp_16bit, 1, 2, file_handler);
    fwrite(&temp_32bit, 1, 4, file_handler);

    // 写入bitmap信息
    temp_16bit = 0;
    temp_32bit = 54;
    if (bit_count == 8)
        temp_32bit += XBMP_RGBQUAD_LEN * 256;
    else if (bit_count == 16)
        temp_32bit += 16;
    fwrite(&temp_16bit, 1, 2, file_handler);
    fwrite(&temp_16bit, 1, 2, file_handler);
    fwrite(&temp_32bit, 1, 4, file_handler);

    if (8 == bit_count)
    {
        XBMPINFO_HEADER bi = { 0 };
        bi.biSize = 40;
        bi.biWidth = width;
        bi.biHeight = height;
        bi.biBitCount = (uint16_t)bit_count;
        bi.biPlanes = 1;
        fwrite(&bi, 1, XBMP_INFOHDR_LEN, file_handler);

        XBMP_RGBQUAD rgb[256];
        for (int i = 0; i < 256; i++)
        {
            rgb[i].rgbBlue = (uint8_t)i;
            rgb[i].rgbGreen = (uint8_t)i;
            rgb[i].rgbRed = (uint8_t)i;
            rgb[i].rgbReserved = 0;
        }
        fwrite(rgb, 1, XBMP_RGBQUAD_LEN * 256, file_handler);
    }
    else
    {
        uint32_t		biSize = 40;
        int32_t			biWidth = width;
        int32_t			biHeight = height;
        uint16_t		biPlanes = 1;
        uint16_t		biBitCount = bit_count;
        uint32_t		biCompression = (bit_count == 16) ? 3 : 0;
        uint32_t		biSizeImage = 0;
        int32_t			biXPelsPerMeter = 0;
        int32_t			biYPelsPerMeter = 0;
        uint32_t		biClrUsed = 0;
        uint32_t		biClrImportant = 0;

        fwrite(&biSize, 1, 4, file_handler);
        fwrite(&biWidth, 1, 4, file_handler);
        fwrite(&biHeight, 1, 4, file_handler);
        fwrite(&biPlanes, 1, 2, file_handler);
        fwrite(&biBitCount, 1, 2, file_handler);
        fwrite(&biCompression, 1, 4, file_handler);
        fwrite(&biSizeImage, 1, 4, file_handler);
        fwrite(&biXPelsPerMeter, 1, 4, file_handler);
        fwrite(&biYPelsPerMeter, 1, 4, file_handler);
        fwrite(&biClrUsed, 1, 4, file_handler);
        fwrite(&biClrImportant, 1, 4, file_handler);

        if (bit_count == 16)
        {
            uint32_t mask[16] = { 0 };
            mask[0] = 0xF800;
            mask[1] = 0x7E0;
            mask[2] = 0x1F;
            mask[3] = 0;
            fwrite(mask, 1, 16, file_handler);
        }
    }

    uint8_t* line_buffer = new uint8_t[line_bytes_*2];

    uint8_t* pixel_line = rgba_data + (height - 1) * line_bytes_;
    int i, j;
    for (i = 0; i < height; i++)
    {
        memset(line_buffer, 0, line_bytes_*2);
        uint8_t* pSrc = pixel_line;
        uint8_t* pDst = line_buffer;
        for (j = 0; j < width; j++) {
            pDst[0] = pSrc[2];
            pDst[1] = pSrc[1];
            pDst[2] = pSrc[0];
            pDst[3] = pSrc[3];

            pSrc += 4;
            pDst += 4;
        }

        //fwrite(pixel_line, 1, line_bytes_, file_handler);
        fwrite(line_buffer, 1, line_bytes_, file_handler);
        pixel_line -= line_bytes_;
    }
    fclose(file_handler);

    delete [] line_buffer;
    return XOK;
}
