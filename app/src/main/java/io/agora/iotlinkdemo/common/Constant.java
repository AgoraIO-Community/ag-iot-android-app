package io.agora.iotlinkdemo.common;

public class Constant {
    public static final String IS_CONNECT_SUCCESS = "isConnectSuccess";

    public static final String CURRENT_USER = "current_user";
    public static final String CURRENT_MEMBER_ID = "current_member_id";
    public static final String FIRST_REPORT = "first_report";
    public static final String USER_ID = "user_id";
    public static final String SCAN_RESULT = "scan_result";
    public static final String DEVICE_ID = "device_id";
    public static final String WIFI_NAME = "wifi_name";
    public static final String WIFI_PWD = "wifi_pwd";
    public static final String FROM_QR_C = "from_qr_c";
    public static final String FROM_QR_K = "from_qr_k";
    public static final String ID = "id";
    public static final String SSID = "ssid";

    public static final String TIME = "time";

    public static final String CODE = "code";

    public static final String USER_NAME = "user_name";

    public static final String OBJECT = "object";
    public static final String PHONE = "phone";
    public static final String URL = "url";

    public static final String IS_FORGE_PASSWORD = "is_forge_password";

    public static final String COUNTRY = "country";

    public static final String IS_SUCCESS = "is_success";

    public static final String IS_FIRST = "is_first";

    public static final String FILE_URL = "file_url";

    public static final String FILE_DESCRIPTION = "file_description";

    public static final String IS_AGREE = "is_agree";

    public static final String MESSAGE_TITLE = "message_title";

    public static final String MESSAGE_TIME = "message_time";

    public static final String ACCOUNT = "account";

    public static final String TYPE = "type";

    public static final String AVATAR = "avatar";

    public static final String PASSWORD = "password";

    public static final String COUNTRY_NAME = "country_name";

    public static final String BT_CFG_DEVICE = "bt_cfg_device";

    /* 每页最大值 */
    public static final int MAX_RECORD_CNT = 4096;

    /* 退出流程回调 */
    public static final int CALLBACK_TYPE_EXIT_STEP = -1;


    /* 设备名称修改成功 */
    public static final int CALLBACK_TYPE_DEVICE_EDIT_NAME_SUCCESS = 10;
    /* 设备名称修改失败 */
    public static final int CALLBACK_TYPE_DEVICE_EDIT_NAME_FAIL = 11;
    /* 移除设备成功 */
    public static final int CALLBACK_TYPE_DEVICE_REMOVE_SUCCESS = 12;
    /* 移除设备失败 */
    public static final int CALLBACK_TYPE_DEVICE_REMOVE_FAIL = 13;
    /* 添加设备成功 */
    public static final int CALLBACK_TYPE_DEVICE_ADD_SUCCESS = 14;
    /* 添加设备失败 */
    public static final int CALLBACK_TYPE_DEVICE_ADD_FAIL = 15;
    /* 设备连接中 */
    public static final int CALLBACK_TYPE_DEVICE_CONNING = 16;
    /* 设备浏览中 */
    public static final int CALLBACK_TYPE_DEVICE_BROWSE = 17;
    /* 设备分享成功 */
    public static final int CALLBACK_TYPE_DEVICE_SHARE_TO_SUCCESS = 18;
    /* 设备分享失败 */
    public static final int CALLBACK_TYPE_DEVICE_SHARE_TO_FAIL = 19;
    /* 获取设备分享的用户列表成功 */
    public static final int CALLBACK_TYPE_DEVICE_SHARE_TO_LIST_SUCCESS = 20;
    /* 获取设备分享的用户列表失败 */
    public static final int CALLBACK_TYPE_DEVICE_SHARE_TO_LIST_FAIL = 21;
    /* 取消分享成功 */
    public static final int CALLBACK_TYPE_DEVICE_SHARE_CANCEL_SUCCESS = 22;
    /* 取消分享失败 */
    public static final int CALLBACK_TYPE_DEVICE_SHARE_CANCEL_FAIL = 23;
    /* 设备查询成功 */
    public static final int CALLBACK_TYPE_DEVICE_QUERY_SUCCESS = 24;
    /* 设备查询失败 */
    public static final int CALLBACK_TYPE_DEVICE_QUERY_FAIL = 25;
    /* 蓝牙添加设备成功 */
    public static final int CALLBACK_TYPE_DEVICE_BTADD_SUCCESS = 26;
    /* 蓝牙添加设备失败 */
    public static final int CALLBACK_TYPE_DEVICE_BTADD_FAIL = 27;
    /* 添加设备MQTT消息 */
    public static final int CALLBACK_TYPE_DEVICE_ADD_NOTIFY = 28;

    /* 设备接收速率 */
    public static final int CALLBACK_TYPE_DEVICE_NET_RECEIVING_SPEED = 30;
    /* 设备延迟 */
    public static final int CALLBACK_TYPE_DEVICE_LAST_MILE_DELAY = 31;
    /* 视频第一帧显示 */
    public static final int CALLBACK_TYPE_DEVICE_PEER_FIRST_VIDEO = 32;
    /* 设备端接听 */
    public static final int CALLBACK_TYPE_DEVICE_ANSWER = 33;
    /* 呼叫设备完成 */
    public static final int CALLBACK_TYPE_DEVICE_DIAL_DONE = 34;
    /* 呼叫设备超时未接 */
    public static final int CALLBACK_TYPE_DEVICE_DIAL_TIMEOUT = 35;
    /* 设备挂断 */
    public static final int CALLBACK_TYPE_DEVICE_HANGUP = 36;
    /* 设备来电 */
    public static final int CALLBACK_TYPE_DEVICE_INCOMING = 37;

    /* 用户登录状态改变*/
    public static final int CALLBACK_TYPE_USER_STATUS_CHANGE = 100;
    /* 用户信息回调 */
    public static final int CALLBACK_TYPE_USER_GET_USERINFO = 101;
    /* 用户修改信息成功 */
    public static final int CALLBACK_TYPE_USER_EDIT_USERINFO_SUCCESS = 102;
    /* 用户修改信息失败 */
    public static final int CALLBACK_TYPE_USER_EDIT_USERINFO_FAIL = 103;
    /* 上传用户头像成功 */
    public static final int CALLBACK_TYPE_USER_UPLOAD_AVATAR_SUCCESS = 104;
    /* 上传用户头像失败 */
    public static final int CALLBACK_TYPE_USER_UPLOAD_AVATAR_FAIL = 105;

    /* 获取全部告警消息 */
    public static final int CALLBACK_TYPE_MESSAGE_ALARM_QUERY_RESULT = 200;
    /* 获取告警消息详情 */
    public static final int CALLBACK_TYPE_MESSAGE_ALARM_DETAIL_RESULT = 201;
    /* 获取全部通知消息 */
    public static final int CALLBACK_TYPE_MESSAGE_NOTIFY_QUERY_RESULT = 202;
    /* 删除告警消息 */
    public static final int CALLBACK_TYPE_MESSAGE_ALARM_DELETE_RESULT = 203;
    /* 删除通知消息 */
    public static final int CALLBACK_TYPE_MESSAGE_NOTIFY_DELETE_RESULT = 204;
    /* 未读告警消息数量 */
    public static final int CALLBACK_TYPE_MESSAGE_ALARM_COUNT_RESULT = 205;
    /* 未读通知消息数量 */
    public static final int CALLBACK_TYPE_MESSAGE_NOTIFY_COUNT_RESULT = 206;
    /* 标记告警已读 */
    public static final int CALLBACK_TYPE_MESSAGE_MARK_ALARM_MSG = 207;
    /* 标记通知已读 */
    public static final int CALLBACK_TYPE_MESSAGE_MARK_NOTIFY_MSG = 208;

    /* 获取全部告警消息失败 */
    public static final int CALLBACK_TYPE_MESSAGE_ALARM_QUERY_FAIL = 210;
    /* 获取告警消息详情失败 */
    public static final int CALLBACK_TYPE_MESSAGE_ALARM_DETAIL_FAIL = 211;
    /* 获取全部通知消息失败 */
    public static final int CALLBACK_TYPE_MESSAGE_NOTIFY_QUERY_FAIL = 212;
    /* 删除告警消息失败 */
    public static final int CALLBACK_TYPE_MESSAGE_ALARM_DELETE_FAIL = 213;
    /* 未读告警消息数量失败 */
    public static final int CALLBACK_TYPE_MESSAGE_ALARM_COUNT_FAIL = 215;
    /* 未读通知消息数量失败 */
    public static final int CALLBACK_TYPE_MESSAGE_NOTIFY_COUNT_FAIL = 216;
    /* 标记告警已读失败 */
    public static final int CALLBACK_TYPE_MESSAGE_MARK_ALARM_MSG_FAIL = 217;
    /* 标记通知已读失败 */
    public static final int CALLBACK_TYPE_MESSAGE_MARK_NOTIFY_MSG_FAIL = 218;
    /* 标记通知已读成功 */
    public static final int CALLBACK_TYPE_MESSAGE_MARK_NOTIFY_MSG_SUCCESS = 219;
    /* 查询到未读告警消息数量 */
    public static final int CALLBACK_TYPE_MESSAGE_UNREAD_ALARM_COUNT = 220;
    /* 查询到未读通知消息数量 */
    public static final int CALLBACK_TYPE_MESSAGE_UNREAD_NOTIFIY_COUNT = 221;

    /* 对方挂断 */
    public static final int CALLBACK_TYPE_PLAYER_CALL_HANG_UP = 300;
    /* 转为竖屏 */
    public static final int CALLBACK_TYPE_PLAYER_TO_PORTRAIT = 301;
    /* 转为横屏 */
    public static final int CALLBACK_TYPE_PLAYER_TO_LANDSCAPE = 302;
    /* 更新设备属性 */
    public static final int CALLBACK_TYPE_PLAYER_UPDATE_PROPERTY = 303;
    /* 保存截图成功 */
    public static final int CALLBACK_TYPE_PLAYER_SAVE_SCREENSHOT = 304;

    /* 播放器返回当前进度 */
    public static final int CALLBACK_TYPE_PLAYER_CURRENT_PROGRESS = 305;
    /* 播放器返回当前缓冲进度 */
    public static final int CALLBACK_TYPE_PLAYER_SEC_PROGRESS = 306;
    /* 播放器返回当前播放时间 */
    public static final int CALLBACK_TYPE_PLAYER_CURRENT_TIME = 307;
    /* 播放器返回总播放时间 */
    public static final int CALLBACK_TYPE_PLAYER_TOTAL_TIME = 308;

    /* 固件版本信息获取 */
    public static final int CALLBACK_TYPE_FIRM_GETVERSION = 309;
    /* 固件版本升级 */
    public static final int CALLBACK_TYPE_FIRM_UPGRADE = 310;

    /* 查询到属性描述符列表 */
    public static final int CALLBACK_TYPE_PLAYER_UPDATEPROPDESC = 311;

    /* 其他用户上线 */
    public static final int CALLBACK_TYPE_USER_ONLINE = 312;
    /* 其他用户下线 */
    public static final int CALLBACK_TYPE_USER_OFFLINE = 312;

    /* 第三方账号注册完成 */
    public static final int CALLBACK_TYPE_THIRD_REGISTER_DONE = 401;
    /* 第三方账号注销完成 */
    public static final int CALLBACK_TYPE_THIRD_UNREGISTER_DONE = 402;
    /* 第三方账号登录完成 */
    public static final int CALLBACK_TYPE_THIRD_LOGIN_DONE = 403;
    /* 第三方账号登出完成 */
    public static final int CALLBACK_TYPE_THIRD_LOGOUT_DONE = 404;
    /* 第三方账号请求验证码完成 */
    public static final int CALLBACK_TYPE_THIRD_REQVCODE_DONE = 405;
    /* 第三方账号重置密码完成 */
    public static final int CALLBACK_TYPE_THIRD_RESETPSWD_DONE = 406;

    /* RTM联接完成 */
    public static final int CALLBACK_TYPE_RTM_CONNECT_DONE = 501;
    /* RTM发送消息完成 */
    public static final int CALLBACK_TYPE_RTM_SEND_DONE = 502;
    /* RTM接收到消息 */
    public static final int CALLBACK_TYPE_RTM_RECVED = 503;


    /* 录像错误 */
    public static final int CALLBACK_TYPE_RECORDING_ERROR = 601;

}
