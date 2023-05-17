#  Agora Link Ver1.5

#### 应用说明
该应用是灵隼系统Android端Demo程序
必须要使用配套的 device_sdk_ver1.5 版本的设备SDK使用

#### 软件架构
1. app目录         ：应用层demo程序代码
2. iotsdk20目录 ：SDK代码
3. avengine目录：录像功能SDK目录

#### 功能列表

1. 账号管理：包括用户账号的注册、注销、登录、登出、用户信息更新等

2. 设备管理：IoT设备的绑定、解绑、查询、实时状态更新、设备控制、分享、录像等

3. 呼叫系统：APP端与设备端的主动呼叫、来电接听、通话处理、变声音效等

4. 告警管理：设备端的声音检测、移动侦测、红外侦测、按钮报警记录的管理

5. 云录管理:  告警云录视频查询、播放、本地下载等

6. 设备系统消息管理：设备上线、下线、绑定、解绑等系统事件管理

#### 编译调试

1. 安装最新版本的 Android Studio开发工具，尽量使用 Chipmunk版本

2. 在'灵隼'官网平台申请相应的开发者账号，获取相关信息
   https://docs-preprod.agora.io/cn/iot-apaas/landing-page
    
3. 使用获取到的开发者账号信息，更新 /app/src/main/res/values/config.xml 配置文件

4. 使用Android Studio打开整个项目工程，进行编译和调试

5. 




