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
    // 账号相关错误
    //
    public static final int XERR_ACCOUNT_BASE = -30000;
    public static final int XERR_ACCOUNT_NOT_EXIST = -30001;        ///< 账号不存在
    public static final int XERR_ACCOUNT_ALREADY_EXIST = -30002;    ///< 账号已经存在
    public static final int XERR_ACCOUNT_PASSWORD_ERR = -30003;     ///< 密码错误
    public static final int XERR_ACCOUNT_NOT_LOGIN = -30004;        ///< 账号未登录
    public static final int XERR_ACCOUNT_REGISTER = -30005;         ///< 账号注册失败
    public static final int XERR_ACCOUNT_UNREGISTER = -30006;       ///< 账号注销失败
    public static final int XERR_ACCOUNT_LOGIN = -30007;            ///< 账号登录失败
    public static final int XERR_ACCOUNT_LOGOUT = -30008;           ///< 账号登出失败
    public static final int XERR_ACCOUNT_CHGPSWD = -30009;          ///< 账号更换密码失败
    public static final int XERR_ACCOUNT_RESETPSWD = -30010;        ///< 账号重置密码失败
    public static final int XERR_ACCOUNT_GETCODE = -30011;          ///< 获取验证码失败
    public static final int XERR_ACCOUNT_USRINFO_QUERY = -30013;    ///< 查询用户信息失败
    public static final int XERR_ACCOUNT_USRINFO_UPDATE = -30014;   ///< 更新用户信息失败
    public static final int XERR_ACCOUNT_VERYCODE = -30015;         ///< 验证码错误

    //
    // 呼叫系统相关错误
    //
    public static final int XERR_CALLKIT_BASE = -40000;
    public static final int XERR_CALLKIT_TIMEOUT = -40001;          ///< 呼叫超时无响应
    public static final int XERR_CALLKIT_DIAL = -40002;             ///< 呼叫拨号失败
    public static final int XERR_CALLKIT_HANGUP = -40003;           ///< 呼叫挂断失败
    public static final int XERR_CALLKIT_ANSWER = -40004;           ///< 呼叫接听失败
    public static final int XERR_CALLKIT_REJECT = -40005;           ///< 呼叫拒绝失败
    public static final int XERR_CALLKIT_PEER_BUSY = -40006;        ///< 对端忙
    public static final int XERR_CALLKIT_PEERTIMEOUT = -40007;      ///< 对端超时无响应
    public static final int XERR_CALLKIT_LOCAL_BUSY = -40008;       ///< 本地端忙
    public static final int XERR_CALLKIT_ERR_OPT = -40009;          ///< 不支持的错误操作
    public static final int XERR_CALLKIT_PEER_UNREG = -40010;       ///< 对端未注册
    public static final int XERR_CALLKIT_RESET = -40011;            ///< 对端未注册

    //
    // 设备管理相关错误
    //
    public static final int XERR_DEVMGR_BASE = -50000;
    public static final int XERR_DEVMGR_NO_DEVICE = -50001;         ///< 没有找到设备
    public static final int XERR_DEVMGR_ONLINE = -50002;            ///< 设已解决在线
    public static final int XERR_DEVMGR_OFFLINE = -50003;           ///< 设备不在线
    public static final int XERR_DEVMGR_QUEYR = -50004;             ///< 设备查询失败
    public static final int XERR_DEVMGR_ADD = -50005;               ///< 设备添加失败
    public static final int XERR_DEVMGR_DEL = -50006;               ///< 设备删除失败
    public static final int XERR_DEVMGR_SETPROPERTY = -50007;       ///< 设备属性设置失败
    public static final int XERR_DEVMGR_GETPROPERTY = -50008;       ///< 设备属性获取失败
    public static final int XERR_DEVMGR_RENAME = -50009;            ///< 设备重命名失败
    public static final int XERR_DEVMGR_SHARE = -50010;             ///< 设备共享失败
    public static final int XERR_DEVMGR_DESHARE = -50011;           ///< 解除共享失败
    public static final int XERR_DEVMGR_ACCEPT = -50012;            ///< 接受共享失败
    public static final int XERR_DEVMGR_QUERY_SHARABLE = -50013;    ///< 查询可共享设备列表失败
    public static final int XERR_DEVMGR_QUERY_DESHARABLE = -50014;  ///< 查询可解除设备列表失败
    public static final int XERR_DEVMGR_QUERY_SHARAEDIN = -50015;   ///< 查询共享进来的设备列表失败
    public static final int XERR_DEVMGR_QUERY_SHAREMSG = -50016;    ///< 查询共享消息列表失败
    public static final int XERR_DEVMGR_QUERY_SHAREDETAIL = -50017; ///< 查询共享消息详情败
    public static final int XERR_DEVMGR_DEL_SHAREMSG = -50018;      ///< 删除共享消息失败
    public static final int XERR_DEVMGR_PRODUCT_QUERY = -50019;     ///< 查询产品列表失败
    public static final int XERR_DEVMGR_ALREADY_SHARED = -50020;    ///< 查询产品列表失败
    public static final int XERR_DEVMGR_NO_BIND_USER = -50021;      ///< 绑定的用户不存在
    public static final int XERR_DEVMGR_GET_MCUVER = -50022;        ///< 获取固件版本失败
    public static final int XERR_DEVMGR_UPGRADE_MCUVER = -50023;    ///< 升级固件版本失败
    public static final int XERR_DEVMGR_UPGRADE_INVALID = -50024;   ///< 升级信息不存在或无效
    public static final int XERR_DEVMGR_UPGRADE_GETSTATUS = -50025; ///< 获取升级信状态失败
    public static final int XERR_DEVMGR_NOT_ALLOW = -50026;         ///< 不允许的操作
    public static final int XERR_DEVMGR_QUERY_PROPDESC = -50027;    ///< 查询属性描述符失败

    //
    // 告警管理相关错误
    //
    public static final int XERR_ALARM_BASE = -60000;
    public static final int XERR_ALARM_NOT_FOUND = -60001;          ///< 没有找到告警信息
    public static final int XERR_ALARM_QUERY = -60002;              ///< 告警查询失败
    public static final int XERR_ALARM_DEL = -60003;                ///< 告警删除失败
    public static final int XERR_ALARM_MARK = -60004;               ///< 告警标记失败
    public static final int XERR_ALARM_ADD = -60005;                ///< 告警添加失败
    public static final int XERR_ALARM_GETINFO = -60006;            ///< 获取告警信息
    public static final int XERR_ALARM_PAGEQUERY = -60007;          ///< 查询一页告警信息失败
    public static final int XERR_ALARM_NUMBER = -60008;             ///< 查询告警信息数量失败
    public static final int XERR_ALARM_IMAGE = -60009;              ///< 查询告警图片信息失败
    public static final int XERR_ALARM_VIDEO = -60010;              ///< 查询告警云录视频失败

    //
    // 通知管理相关错误
    //
    public static final int XERR_NOTIFICATION_BASE = -70000;
    public static final int XERR_NOTIFICATION_NOT_FOUND = -70001;    ///< 没有找到通知信息
    public static final int XERR_NOTIFICATION_DEL = -70002;          ///< 通知删除失败
    public static final int XERR_NOTIFICATION_MARK = -70003;         ///< 通知标记失败


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


    //
    // 设备消息管理相关错误
    //
    public static final int XERR_DEVMSG_BASE = -110000;
    public static final int XERR_DEVMSG_QUERY = -110001;              ///< 设备消息查询失败
    public static final int XERR_DEVMSG_MARK = -110002;               ///< 设备消息标记失败
    public static final int XERR_DEVMSG_GETINFO = -110003;            ///< 获取设备消息
    public static final int XERR_DEVMSG_PAGEQUERY = -110004;          ///< 查询一页设备消息
    public static final int XERR_DEVMSG_NUMBER = -110005;             ///< 查询设备消息数量

    //
    // RTM模块相应的错误
    //
    public static final int XERR_RTMMGR_BASE = -120000;
    public static final int XERR_RTMMGR_CONNECT = -120001;              ///< 联接错误
    public static final int XERR_RTMMGR_REJECT = -120002;               ///< 联接被拒
    public static final int XERR_RTMMGR_ALREADY_CONNECTED = -120003;    ///< 已经联接
    public static final int XERR_RTMMGR_SRV_NO_RESPONSE = -120004;      ///< 服务器无响应
    public static final int XERR_RTMMGR_MSG_FAILURE = -120005;          ///< 发送RTM消息失败
    public static final int XERR_RTMMGR_MSG_TIMEOUT = -120006;          ///< 发送RTM消息超时
    public static final int XERR_RTMMGR_MSG_UNREACHABLE = -120007;      ///< 消息不可到达
    public static final int XERR_RTMMGR_MSG_CACHED_BY_SERVER = -120008; ///< 消息未发送被缓存了
    public static final int XERR_RTMMGR_MSG_IMCOMPATIBLE = -120009;     ///< 消息不兼容
    public static final int XERR_RTMMGR_MSG_TOO_OFTEN = -120010;        ///< 消息发送太频繁
    public static final int XERR_RTMMGR_MSG_INVALID = -120011;        ///< 消息发送太频繁


}