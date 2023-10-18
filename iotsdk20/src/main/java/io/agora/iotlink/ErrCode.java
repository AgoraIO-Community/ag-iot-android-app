/**
 * @file ErrCode.java
 * @brief This file define the common error code for SDK
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink;


/*
 * @brief 全局错误码定义
 */
public class ErrCode {

    // 0: 表示正确
    public static final int XERR_NONE = 0;
    public static final int XOK = 0;

    //
    // 通用错误码
    //
    public static final int XERR_BASE = -10000;
    public static final int XERR_UNKNOWN = -10001;              ///< 未知错误
    public static final int XERR_INVALID_PARAM = -10002;        ///< 参数错误
    public static final int XERR_UNSUPPORTED = -10003;          ///< 当前操作不支持
    public static final int XERR_BAD_STATE = -10004;            ///< 当前状态不正确，不能操作
    public static final int XERR_NOT_FOUND = -10005;            ///< 没有找到相关数据
    public static final int XERR_NO_MEMORY = -10006;            ///< 内存不足
    public static final int XERR_BUFFER_OVERFLOW = -10007;      ///< 缓冲区中数据不足
    public static final int XERR_BUFFER_UNDERFLOW = -10008;     ///< 缓冲区中数据过多放不下
    public static final int XERR_TIMEOUT = -10009;              ///< 操作超时
    public static final int XERR_COMPONENT_NOT_EXIST = -10010;  ///< 相应的组件模块不存在
    public static final int XERR_HW_NOT_FOUND = -10011;         ///< 未找硬件设备
    public static final int XERR_NETWORK = -10012;              ///< 网络错误
    public static final int XERR_SERVICE = -10013;              ///< 服务连接失败
    public static final int XERR_EOF = -10014;                  ///< 已经结束
    public static final int XERR_TOKEN_INVALID = -10015;        ///< Token无线
    public static final int XERR_SYSTEM = -10016;               ///< 系统错误
    public static final int XERR_APPID_INVALID = -10017;        ///< AppId不支持
    public static final int XERR_NODEID_INVALID = -10018;       ///< NodeId无效
    public static final int XERR_NOT_AUTHORIZED = -10019;       ///< 未认证
    public static final int XERR_INVOKE_TOO_OFTEN = -10020;     ///< 调用太频繁
    public static final int XERR_THREADPOOL_EXEC = -10021;      ///< 线程执行失败
    public static final int XERR_JSON_READ = -10022;            ///< JSON解析错误
    public static final int XERR_JSON_WRITE = -10023;           ///< JSON写入错误

    //
    // 文件操作错误码
    //
    public static final int XERR_FILE_BASE = -11000;
    public static final int XERR_FILE_NOT_EXIST = -11001;       ///< 文件不存在
    public static final int XERR_FILE_ALREADY_EXIST = -11002;   ///< 文件已经存在
    public static final int XERR_FILE_OPEN = -11003;            ///< 文件打开错误
    public static final int XERR_FILE_EOF = -11004;             ///< 文件已经尾部
    public static final int XERR_FILE_FULL = -11005;            ///< 文件已满
    public static final int XERR_FILE_SEEK = -11006;            ///< 文件SEEK错误
    public static final int XERR_FILE_READ = -11007;            ///< 文件读取错误
    public static final int XERR_FILE_WRITE = -11008;           ///< 文件写入错误
    public static final int XERR_FILE_NO_STREAM = -11009;       ///< 文件没有数据流

    //
    // HTTP 操作错误码
    //
    public static final int XERR_HTTP_BASE = -12000;
    public static final int XERR_HTTP_URL = -12001;             ///< URL地址错误
    public static final int XERR_HTTP_PARAM = -12002;           ///< 参数错误
    public static final int XERR_HTTP_BODY = -12003;            ///< body错误
    public static final int XERR_HTTP_METHOD = -12004;          ///< Method方法错误
    public static final int XERR_HTTP_CONNECT = -12005;         ///< 连接错误
    public static final int XERR_HTTP_NO_RESPONSE = -12005;     ///< 服务器无响应
    public static final int XERR_HTTP_RESP_DATA = -12006;       ///< 回应数据包中，"data"自动错误
    public static final int XERR_HTTP_RESP_CODE = -12007;       ///< HTTP回应错误
    public static final int XERR_HTTP_JSON_PARSE = -12008;      ///< 回应数据包中，JSON解析错误
    public static final int XERR_HTTP_JSON_WRITE = -12009;      ///< 请求数据包中，JSON写入错误

    //
    // 播放相关的错误码
    //
    public static final int XERR_PLAYER_BASE = -13000;
    public static final int XERR_PLAYER_OPEN = -13001;          ///< 播放器打开失败
    public static final int XERR_PLAYER_CLOSE = -13002;         ///< 播放器关闭失败
    public static final int XERR_PLAYER_PLAY = -13003;          ///< 播放器播放失败
    public static final int XERR_PLAYER_PAUSE = -13004;         ///< 播放器暂停失败
    public static final int XERR_PLAYER_STOP = -13005;          ///< 播放器停止失败
    public static final int XERR_PLAYER_SEEK = -13006;          ///< 播放器SEEK失败

    //
    // 编解码器错误码
    //
    public static final int XERR_CODEC_BASE = -14000;
    public static final int XERR_CODEC_OPEN = -14001;           ///< Codec打开失败
    public static final int XERR_CODEC_CLOSE = -14002;          ///< Codec关闭失败
    public static final int XERR_CODEC_INDATA = -14003;         ///< 需要更多的输入数据
    public static final int XERR_CODEC_OUTDATA = -14004;        ///< 还有更多的输出数据
    public static final int XERR_CODEC_NOBUFFER = -14005;       ///< 没有缓冲区用于编解码
    public static final int XERR_CODEC_DECODING = -14006;       ///< 解码错误
    public static final int XERR_CODEC_ENCODING = -14007;       ///< 编码错误
    public static final int XERR_CODEC_MOREINDATA = -14008;     ///< 需要更多的数据进行编解码
    public static final int XERR_CODEC_DEC_EOS = -14009;        ///< 编解码缓冲区也已经完成
    public static final int XERR_CODEC_OUTFMT_READY = -14010;   ///< 输出格式就绪


    //
    // SDK相关错误
    //
    public static final int XERR_SDK_BASE = -20000;
    public static final int XERR_SDK_NOT_READY = -20001;          ///< SDK未就绪


    //
    // 呼叫系统相关错误
    //
    public static final int XERR_CALLKIT_BASE = -30000;
    public static final int XERR_CALLKIT_TIMEOUT = -30001;          ///< 呼叫超时无响应
    public static final int XERR_CALLKIT_DIAL = -30002;             ///< 呼叫拨号失败
    public static final int XERR_CALLKIT_HANGUP = -30003;           ///< 呼叫挂断失败
    public static final int XERR_CALLKIT_ANSWER = -30004;           ///< 呼叫接听失败
    public static final int XERR_CALLKIT_REJECT = -30005;           ///< 呼叫拒绝失败
    public static final int XERR_CALLKIT_PEER_BUSY = -30006;        ///< 对端忙
    public static final int XERR_CALLKIT_PEERTIMEOUT = -30007;      ///< 对端超时无响应
    public static final int XERR_CALLKIT_LOCAL_BUSY = -30008;       ///< 本地端忙
    public static final int XERR_CALLKIT_ERR_OPT = -30009;          ///< 不支持的错误操作
    public static final int XERR_CALLKIT_PEER_UNREG = -30010;       ///< 对端未注册

    //
    // RTM模块相应的错误
    //
    public static final int XERR_RTMMGR_BASE = -120000;
    public static final int XERR_RTMMGR_LOGIN_UNKNOWN = -121001;            ///< RTM登录失败
    public static final int XERR_RTMMGR_LOGIN_REJECTED = -121002;           ///< RTM登录被拒绝
    public static final int XERR_RTMMGR_LOGIN_INVALID_ARGUMENT = -121003;   ///< RTM登录时参数错误
    public static final int XERR_RTMMGR_LOGIN_INVALID_APP_ID = -121004;     ///< RTM登录时appId错误
    public static final int XERR_RTMMGR_LOGIN_INVALID_TOKEN = -121005;      ///< RTM登录时token错误
    public static final int XERR_RTMMGR_LOGIN_TOKEN_EXPIRED = -121006;      ///< RTM登录时token过期
    public static final int XERR_RTMMGR_LOGIN_NOT_AUTHORIZED = -121007;     ///< RTM登录时鉴权失败
    public static final int XERR_RTMMGR_LOGIN_ALREADY_LOGIN = -121008;      ///< RTM已经登录
    public static final int XERR_RTMMGR_LOGIN_TIMEOUT = -121009;            ///< RTM登录超时
    public static final int XERR_RTMMGR_LOGIN_TOO_OFTEN = -121010;          ///< RTM登录太频繁
    public static final int XERR_RTMMGR_LOGIN_NOT_INITIALIZED = -121011;    ///< RTM未初始化
    public static final int XERR_RTMMGR_MSG_FAILURE = -122001;              ///< 发送RTM消息失败
    public static final int XERR_RTMMGR_MSG_TIMEOUT = -122002;              ///< 发送RTM消息超时
    public static final int XERR_RTMMGR_MSG_PEER_UNREACHABLE = -122003;     ///< 消息不可到达
    public static final int XERR_RTMMGR_MSG_CACHED_BY_SERVER = -122004;     ///< 消息未发送被缓存了
    public static final int XERR_RTMMGR_MSG_TOO_OFTEN = -122005;;           ///< 消息发送太频繁
    public static final int XERR_RTMMGR_MSG_INVALID_USERID = -122006;       ///< RTM用户账号无效
    public static final int XERR_RTMMGR_MSG_INVALID_MESSAGE = -122007;      ///< RTM消息无效
    public static final int XERR_RTMMGR_MSG_IMCOMPATIBLE_MESSAGE = -122008; ///< 消息不兼容
    public static final int XERR_RTMMGR_MSG_NOT_INITIALIZED = -122101;      ///< RTM未初始化发消息
    public static final int XERR_RTMMGR_MSG_USER_NOT_LOGGED_IN = -122102;   ///< RTM未登录发消息
    public static final int XERR_RTMMGR_LOGOUT_REJECT = -123001;            ///< RTM登出被拒绝
    public static final int XERR_RTMMGR_LOGOUT_NOT_INITIALIZED = -123101;   ///< RTM未初始化登出
    public static final int XERR_RTMMGR_LOGOUT_NOT_LOGGED_IN = -123102;     ///< RTM未登录就登出
    public static final int XERR_RTMMGR_RENEW_FAILURE = -124001;            ///< RTM Renew token失败
    public static final int XERR_RTMMGR_RENEW_INVALID_ARGUMENT = -124002;   ///< RTM Renew参数错误
    public static final int XERR_RTMMGR_RENEW_REJECTED = -124003;           ///< RTM Renew被拒绝
    public static final int XERR_RTMMGR_RENEW_TOO_OFTEN = -124004;          ///< RTM Renew太频繁
    public static final int XERR_RTMMGR_RENEW_TOKEN_EXPIRED = -124005;      ///< RTM Renew过期
    public static final int XERR_RTMMGR_RENEW_INVALID_TOKEN = -124006;      ///< RTM Renew无效
    public static final int XERR_RTMMGR_RENEW_NOT_INITIALIZED = -124101;    ///< RTM未初始化Renew
    public static final int XERR_RTMMGR_RENEW_NOT_LOGGED_IN = -124102;      ///< RTM未登录就Renew


}