#  Agora Link Ver2.1.0

#### 应用说明
该应用是灵隼系统Android端Demo程序
必须要使用配套的 device_sdk_ver2.1.0 版本的设备SDK使用

#### 软件架构
1. app目录         ：应用层demo程序代码


#### 功能列表
2.1.0是一个极致简化版本，只包含了最基本的链接管理功能
1. 设备管理：在主界面可以浏览多个设备，并且收发消息
2. 流的操作：可以对于任意一路订阅的流，进行预览、音放禁音、录像、截图操作

#### 编译调试
1. 安装最新版本的 Android Studio开发工具，尽量使用 Dolphin | 2021.3.1 Patch1 及其以后版本

2. 在'灵隼'官网平台申请相应的开发者账号，获取相关信息，主要是 appId的信息
   https://docs-preprod.agora.io/cn/iot-apaas/landing-page

3. 使用Android Studio打开整个项目工程，进行编译和调试


#### APP使用说明
1. 首次运行时，需要输入appId

2. 设备管理：
   在主界面上通过输入输入设备的 NodeId可以进行设备添加；
   设备列表界面，对应的总是 PUBLIC_STREAM_1 这路流操作

3. 设备列表的提示信息:
   设备没有连接："Disconnected"
   设备正在连接中: "Connecting..."
   设备已经连接，但是还没有订阅："Connected"
   设备已经连接，已经订阅播放视频："Subscribed"
   设备已经连接和订阅，正在录像："Recording..."


