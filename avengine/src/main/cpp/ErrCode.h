#ifndef __ERRCODE_HPP__
#define __ERRCODE_HPP__


//
// 0: 表示正确
//
#define XERR_NONE                   0
#define XOK                         0

//
// 通用错误码
//
#define XERR_BASE                   -1
#define XERR_UNKNOWN                -1          ///< 未知错误
#define XERR_INVALID_PARAM          -2          ///< 参数错误
#define XERR_UNSUPPORTED            -3          ///< 当前操作不支持
#define XERR_NO_MEMORY              -4          ///< 内存不足
#define XERR_BAD_STATE              -5          ///< 当前操作状态不正确
#define XERR_BUFFER_OVERFLOW        -6          ///< 缓冲区中数据不足
#define XERR_BUFFER_UNDERFLOW       -7          ///< 缓冲区中数据过多放不下
#define XERR_COMPONENT_NOT_EXIST    -8          ///< 相应的组件模块不存在
#define XERR_TIMEOUT                -9          ///< 操作超时
#define XERR_HW_NOT_FOUND           -10         ///< 未找硬件设备

//
// 文件操作错误码
//
#define XERR_FILE_BASE              -20000
#define XERR_FILE_NOT_EXIST         -20001
#define XERR_FILE_ALREADY_EXIST     -20002
#define XERR_FILE_OPEN              -20003
#define XERR_FILE_EOF               -20004
#define XERR_FILE_FULL              -20005
#define XERR_FILE_SEEK              -20006
#define XERR_FILE_READ              -20007
#define XERR_FILE_WRITE             -20008
#define XERR_FILE_NO_STREAM         -20009


//
// 编解码器错误码
//
#define XERR_CODEC_BASE             -100000
#define XERR_CODEC_OPEN             -100001
#define XERR_CODEC_CLOSE            -100002
#define XERR_CODEC_INDATA           -100003
#define XERR_CODEC_OUTDATA          -100004
#define XERR_CODEC_NOBUFFER         -100005
#define XERR_CODEC_DECODING         -100006
#define XERR_CODEC_ENCODING         -100007
#define XERR_CODEC_MOREINDATA       -100008
#define XERR_CODEC_DEC_EOS          -100009
#define XERR_CODEC_OUTFMT_READY     -100010



#endif  // __ERRCODE_HPP__
