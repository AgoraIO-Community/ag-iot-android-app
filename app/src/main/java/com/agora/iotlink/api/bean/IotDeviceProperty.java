package com.agora.iotlink.api.bean;

import com.agora.iotsdk20.IotDevProperty;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//
/////////////////////////////////////////////////////
/////////////////////// 可读写属性 ///////////////////
/////////////////////////////////////////////////////
//public boolean mOsdWatermark = false;       ///< OSD水印
//public int mNightView = 1;                  ///< 红外夜视
//public boolean mMotionDetect = false;       ///< 移动侦测
//public int mPirSensitive = 0;               ///< PIR灵敏度
//public int mVolume = 50;                    ///< 音量
//public boolean mForceAlarm = false;         ///< 强拆警报
//public int mPowerMode = 2;                  ///< 功耗模式
//public int mVideoQuality = 1;               ///< 视频清晰度
//public boolean mLed = false;                ///< 指示灯
//public int mTfCardFormat = 2;               ///< TF卡格式化
//public int mPreviewDuration = 600;          ///< 预览时长
//public boolean mVoiceDetect = false;        ///< 声音检测
//
/////////////////////////////////////////////////////
/////////////////////// 只读属性 ////////////////////
/////////////////////////////////////////////////////
//public int mQuantity = 50;                  ///< 电池电量
//public String mWifiSsid;                    ///< WIFI SSID
//public String mIpAddress;                   ///< IP地址
//public String mDevMac;                      ///< MAC地址
//public String mTimeZone;                    ///< 时区
//public String mVersion;                     ///< 固件版本号
//public int mTfCardState = 6;                ///< TF卡状态
//public int mTfCardSpace = 0;                ///< TF卡剩余空间
public class IotDeviceProperty extends IotDevProperty {
//    /*
//     * 根据Map属性中的列表更新
//     */
//    @Override
//    public void update(Map<String, Object> properties) {
//        Iterator iterator = properties.entrySet().iterator();
//        while (iterator.hasNext()) {
//            Map.Entry mapEntry = (Map.Entry) iterator.next();
//            String key = mapEntry.getKey().toString();
//            switch (key) {
//                case "100":     //OSD水印开关
//                    mOsdWatermark = (Boolean) (mapEntry.getValue());
//                    break;
//
//                case "101":     //红外夜视
//                    mNightView = (Integer) (mapEntry.getValue());
//                    break;
//
//                case "102":     //移动报警开关
//                    mMotionDetect = (Boolean) (mapEntry.getValue());
//                    break;
//
//                case "103":     //PIR开关及灵敏度
//                    mPowerMode = (Integer) (mapEntry.getValue());
//                    break;
//
//                case "104":     //设备音量控制
//                    mVolume = (Integer) (mapEntry.getValue());
//                    break;
//
//                case "105":     //强拆警报
//                    mForceAlarm = (Boolean) (mapEntry.getValue());
//                    break;
//
//                case "106":     //电池电量
//                    mQuantity = (Integer) (mapEntry.getValue());
//                    break;
//
//                case "107":     //视频清晰度
//                    mVideoQuality = (Integer) (mapEntry.getValue());
//                    break;
//
//                case "108":     //工作指示灯开关
//                    mLed = (Boolean) (mapEntry.getValue());
//                    break;
//
//                case "115":     //声音检测
//                    mVoiceDetect = (Boolean) (mapEntry.getValue());
//                    break;
//
//                case "1000":    //低功耗状态
//                    mPowerMode = (Integer) (mapEntry.getValue());
//                    break;
//            }
//        }
//    }

    /**
     * 指示灯
     *
     * @param isOpen true 开启  false 关闭
     */
    public Map<String, Object> setLedSwitch(boolean isOpen) {
        Map<String, Object> properties = new HashMap<>();
        mLed = isOpen;
        properties.put("108", isOpen);//? 1 : 0
        return properties;
    }

    /**
     * 红外 夜视功能开关
     *
     * @param type 0 自动 1关闭 2开启
     */
    public Map<String, Object> setNightView(int type) {
        Map<String, Object> properties = new HashMap<>();
        mNightView = type;
        properties.put("101", type);
        return properties;
    }

    /**
     * 声音检测
     *
     * @param isOpen true 开启
     */
    public Map<String, Object> setSoundDetection(boolean isOpen) {
        Map<String, Object> properties = new HashMap<>();
        mVoiceDetect = isOpen;
        properties.put("115", mVoiceDetect);//? 1 : 0
        return properties;
    }

    /**
     * 移动检测 些处sdk经常修改 由boolean 变int 又变boolean 需要注意
     *
     * @param isOpen true 开启
     */
    public Map<String, Object> setMotionAlarm(boolean isOpen) {
        Map<String, Object> properties = new HashMap<>();
        mMotionDetect = isOpen;
        properties.put("102", mMotionDetect);
        return properties;
    }

    /**
     * Pir
     *
     * @param type 0 关闭 1 开启 2 低灵敏度 4 中灵敏度 5 高灵敏度
     */
    public Map<String, Object> setPirSwitch(int type) {
        Map<String, Object> properties = new HashMap<>();
        mPirSensitive = type;
        properties.put("103", mPirSensitive);
        return properties;
    }

    /**
     * 清晰度
     *
     * @param type 1 标清 2 高清
     */
    public Map<String, Object> setVideoQuality(int type) {
        Map<String, Object> properties = new HashMap<>();
        mVideoQuality = type;
        properties.put("107", mVideoQuality);
        return properties;
    }

    /**
     * 音量
     *
     * @param value 0-100
     */
    public Map<String, Object> setVolume(int value) {
        Map<String, Object> properties = new HashMap<>();
        mVolume = value;
        properties.put("104", mVolume);
        return properties;
    }

}
