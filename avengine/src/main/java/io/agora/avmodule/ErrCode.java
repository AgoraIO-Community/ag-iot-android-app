package io.agora.avmodule;


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
    public static final int XERR_SERVICE = -10013;              ///< 服务错误
    public static final int XERR_EOF = -10014;                  ///< 已经结束
    public static final int XERR_TOKEN_INVALID = -10015;        ///< Token过期
    public static final int XERR_SYSTEM = -10016;               ///< 系统错误
    public static final int XERR_APPID_INVALID = -10017;        ///< AppId不支持
    public static final int XERR_USERID_INVALID = -10018;       ///< UserId无效
    public static final int XERR_NOT_AUTHORIZED = -10019;       ///< 未认证
    public static final int XERR_INVOKE_TOO_OFTEN = -10020;     ///< 调用太频繁
    public static final int XERR_MQTT_DISCONNECT = -10021;      ///< MQTT未联接
    public static final int XERR_VCODE_VALID = -10022;          ///< 上一个验证码依然有效

    //
    // 文件操作错误码
    //
    public static final int XERR_FILE_BASE = -20000;
    public static final int XERR_FILE_NOT_EXIST = -20001;
    public static final int XERR_FILE_ALREADY_EXIST = -20002;
    public static final int XERR_FILE_OPEN = -20003;
    public static final int XERR_FILE_EOF = -20004;
    public static final int XERR_FILE_FULL = -20005;
    public static final int XERR_FILE_SEEK = -20006;
    public static final int XERR_FILE_READ = -20007;
    public static final int XERR_FILE_WRITE = -20008;
    public static final int XERR_FILE_NO_STREAM = -20009;

    //
    // HTTP请求响应相关的错误
    //
    public static final int XERR_HTTP_BASE = -80000;
    public static final int XERR_HTTP_URL = -80001;                 ///< URL地址错误
    public static final int XERR_HTTP_PARAM = -80002;               ///< 参数错误
    public static final int XERR_HTTP_BODY = -80003;                ///< body错误
    public static final int XERR_HTTP_METHOD = -80004;              ///< Method方法错误
    public static final int XERR_HTTP_CONNECT = -80005;             ///< 连接错误
    public static final int XERR_HTTP_NO_RESPONSE = -80005;         ///< 服务器无响应
    public static final int XERR_HTTP_RESP_DATA = -80006;           ///< 回应数据包内容有错误
    public static final int XERR_HTTP_RESP_CODE = -80007;           ///< HTTP回应错误
    public static final int XERR_HTTP_JSON_PARSE = -80008;          ///< 回应数据包中，JSON解析错误
    public static final int XERR_HTTP_JSON_WRITE = -80009;          ///< 请求数据包中，JSON写入错误

    //
    // 播放相关的错误
    //
    public static final int XERR_PLAYER_BASE = -90000;
    public static final int XERR_PLAYER_OPEN = -90001;              ///< 打开失败
    public static final int XERR_PLAYER_CLOSE = -90002;              ///< 关闭失败
    public static final int XERR_PLAYER_PLAY = -90003;              ///< 播放失败
    public static final int XERR_PLAYER_PAUSE = -90004;             ///< 暂停失败
    public static final int XERR_PLAYER_STOP = -90005;              ///< 停止失败
    public static final int XERR_PLAYER_SEEK = -90006;              ///< SEEK失败

    //
    // 编解码器错误码
    //
    public static final int XERR_CODEC_BASE = -100000;
    public static final int XERR_CODEC_OPEN = -100001;
    public static final int XERR_CODEC_CLOSE = -100002;
    public static final int XERR_CODEC_INDATA = -100003;
    public static final int XERR_CODEC_OUTDATA = -100004;
    public static final int XERR_CODEC_NOBUFFER = -100005;
    public static final int XERR_CODEC_DECODING = -100006;
    public static final int XERR_CODEC_ENCODING = -100007;
    public static final int XERR_CODEC_MOREINDATA = -100008;     ///< 需要更多的数据进行编解码
    public static final int XERR_CODEC_DEC_EOS = -100009;        ///< 编解码缓冲区也已经完成
    public static final int XERR_CODEC_OUTFMT_READY = -100010;   ///< 输出格式就绪



}