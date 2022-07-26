package io.agora.iotlinkdemo.manager;

import android.app.Activity;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.agora.baselibrary.utils.GsonUtil;
import com.agora.baselibrary.utils.StringUtils;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlink.IotAlarm;
import io.agora.iotlink.IotOutSharer;
import com.alibaba.android.arouter.launcher.ARouter;

public class PagePilotManager {
    public static void pageMainHome() {
        ARouter.getInstance()
                .build(PagePathConstant.pageMainHome)
                .navigation();
        ;
    }

    /**
     * 手机号登录
     */
    public static void pagePhoneLogin() {
        ARouter.getInstance()
                .build(PagePathConstant.pagePhoneLogin)
                .navigation();
    }

    /**
     * 手机号注册
     */
    public static void pagePhoneRegister(boolean isForgePassword) {
        ARouter.getInstance()
                .build(PagePathConstant.pagePhoneRegister)
                .withBoolean(Constant.IS_FORGE_PASSWORD, isForgePassword)
                .navigation();
    }

    /**
     * 获取fragment
     */
    public static Fragment getFragmentPage(String uri, Bundle bundle) {
        return (Fragment) ARouter.getInstance().build(uri)
                .with(bundle)
                .navigation();
    }

    /**
     * 添加设备首页
     */
    public static void pageAddDeviceMain() {
        ARouter.getInstance()
                .build(PagePathConstant.pageAddDeviceMain)
                .navigation();
    }

    /**
     * 重置设备
     */
    public static void pageResetDevice() {
        ARouter.getInstance()
                .build(PagePathConstant.pageResetDevice)
                .navigation();
    }

    /**
     * 设备扫码
     */
    public static void pageDeviceQR() {
        ARouter.getInstance()
                .build(PagePathConstant.pageDeviceQR)
                .navigation();
    }

    /**
     * 设置wifi
     */
    public static void pageSetDeviceWifi() {
        ARouter.getInstance()
                .build(PagePathConstant.pageSetDeviceWifi)
                .navigation();
    }

    /**
     * 正在添加设备
     */
    public static void pageDeviceAdding() {
        ARouter.getInstance()
                .build(PagePathConstant.pageDeviceAdding)
                .navigation();
    }

    /**
     * wifi列表
     */
    public static void pageWifiList(Activity activity) {
        ARouter.getInstance()
                .build(PagePathConstant.pageWifiList)
                .navigation(activity, 100);
    }

    /**
     * wifi列表
     */
    public static void pageWifiTimeOut() {
        ARouter.getInstance()
                .build(PagePathConstant.pageWifiTimeOut)
                .navigation();
    }

    /**
     * 添加设备成功/失败
     *
     * @param isSuccess true 成功
     */
    public static void pageAddResult(boolean isSuccess) {
        ARouter.getInstance()
                .build(PagePathConstant.pageAddResult)
                .withBoolean(Constant.IS_SUCCESS, isSuccess)
                .navigation();
    }

    /**
     * 添加设备扫码页
     */
    public static void pageDeviceAddScanning() {
        ARouter.getInstance()
                .build(PagePathConstant.pageDeviceAddScanning)
                .navigation();
    }

    /**
     * 设备设置
     */
    public static void pageDeviceSetting() {
        ARouter.getInstance()
                .build(PagePathConstant.pageDeviceSetting)
                .navigation();
    }

    /**
     * 固件升级
     */
    public static void pageDeviceFirmwareUpgrade() {
        ARouter.getInstance()
                .build(PagePathConstant.pageDeviceFirmwareUpgrade)
                .navigation();
    }

    /**
     * 设备共享出去的用户列表
     */
    public static void pageDeviceShareToUserList() {
        ARouter.getInstance()
                .build(PagePathConstant.pageDeviceShareToUserList)
                .navigation();
    }

    /**
     * 设备共享出去的用户详情
     */
    public static void pageDeviceShareToUserDetail(IotOutSharer iotOutSharer) {
        ARouter.getInstance()
                .build(PagePathConstant.pageDeviceShareToUserDetail)
                .withString(Constant.OBJECT, GsonUtil.Companion.getInstance().toJson(iotOutSharer))
                .navigation();
    }

    /**
     * 添加共享给其他用户
     */
    public static void pageDeviceShareToUserAdd() {
        ARouter.getInstance()
                .build(PagePathConstant.pageDeviceShareToUserAdd)
                .navigation();
    }

    /**
     * 共享的设备设置
     */
    public static void pageShareDeviceSetting() {
        ARouter.getInstance()
                .build(PagePathConstant.pageShareDeviceSetting)
                .navigation();
    }

    /**
     * 设备信息设置
     */
    public static void pageDeviceInfoSetting() {
        ARouter.getInstance()
                .build(PagePathConstant.pageDeviceInfoSetting)
                .navigation();
    }

    /**
     * 设备基本功能设置
     */
    public static void pageDeviceBaseSetting() {
        ARouter.getInstance()
                .build(PagePathConstant.pageDeviceBaseSetting)
                .navigation();
    }

    /**
     * 预览 播放页
     */
    public static void pagePreviewPlay() {
        ARouter.getInstance()
                .build(PagePathConstant.pagePreviewPlay)
                .navigation();
    }

    /**
     * 消息播放页
     */
    public static void pagePlayMessage(IotAlarm iotAlarm) {
        String title;
        switch (iotAlarm.mMessageType) {
            case 0: {
                title = "声音检测";
                break;
            }
            case 1: {
                title = "有人通过";
                break;
            }
            case 2: {
                title = "移动侦测";
                break;
            }
            case 3: {
                title = "语音告警";
                break;
            }
            default: {
                title = "未知类型";
                break;
            }
        }
        ARouter.getInstance()
                .build(PagePathConstant.pagePlayMessage)
                .withString(Constant.FILE_URL, iotAlarm.mFileUrl)
                .withString(Constant.FILE_DESCRIPTION, iotAlarm.mDescription)
                .withString(Constant.MESSAGE_TITLE, title)
                .withString(
                        Constant.MESSAGE_TIME,
                        StringUtils.INSTANCE.getDetailTime(
                                "yyyy-MM-dd HH:mm:ss",
                                Long.parseLong(iotAlarm.mCreatedDate) / 1000
                        )
                )
                .navigation();
    }

    /**
     * 选择国家
     */
    public static void pageSelectCountry(Activity activity) {
//        ARouter.getInstance()
//                .build(PagePathConstant.pageSelectCountry)
//                .navigation(activity, 100);
    }

    /**
     * 输入验证码
     */
    public static void pageInputVCode(String account, String type, Boolean isForgePassword) {
        ARouter.getInstance()
                .build(PagePathConstant.pageInputVCode)
                .withString(Constant.ACCOUNT, account)
                .withString(Constant.TYPE, type)
                .withBoolean(Constant.IS_FORGE_PASSWORD, isForgePassword)
                .navigation();
    }

    /**
     * 设置密码
     */
    public static void pageSetPwd(String account, String code, Boolean isForgePassword) {
        ARouter.getInstance()
                .build(PagePathConstant.pageSetPwd)
                .withString(Constant.ACCOUNT, account)
                .withBoolean(Constant.IS_FORGE_PASSWORD, isForgePassword)
                .withString(Constant.CODE, code)
                .navigation();
    }

    /**
     * 关于
     */
    public static void pageAbout() {
        ARouter.getInstance()
                .build(PagePathConstant.pageAbout)
                .navigation();
    }

    /**
     * 关于
     */
    public static void pageMessage() {
        ARouter.getInstance()
                .build(PagePathConstant.pageMessage)
                .navigation();
    }

    /**
     * 修改密码
     */
    public static void pageChangePassword() {
        ARouter.getInstance()
                .build(PagePathConstant.pageChangePassword)
                .navigation();
    }

    /**
     * 相册
     */
    public static void pageAlbum() {
        ARouter.getInstance()
                .build(PagePathConstant.pageAlbum)
                .navigation();
    }

    /**
     * 相册 图片预览
     */
    public static void pageAlbumViewPhoto(String fileUrl, String time) {
        ARouter.getInstance()
                .build(PagePathConstant.pageAlbumViewPhoto)
                .withString(Constant.FILE_URL, fileUrl)
                .withString(Constant.TIME, time)
                .navigation();
    }

    /**
     * 被叫
     */
    public static void pageCalled() {
        ARouter.getInstance()
                .build(PagePathConstant.pageCalled)
                .navigation();
    }

    /**
     * 个人资料
     */
    public static void pageUserInfo() {
        ARouter.getInstance()
                .build(PagePathConstant.pageUserInfo)
                .navigation();
    }

    /**
     * 修改昵称
     */
    public static void pageUserEditNickname(String name) {
        ARouter.getInstance()
                .build(PagePathConstant.pageUserEditNickname)
                .withString(Constant.USER_NAME, name)
                .navigation();
    }

    /**
     * 通用设置
     */
    public static void pageGeneralSettings() {
        ARouter.getInstance()
                .build(PagePathConstant.pageGeneralSettings)
                .navigation();
    }

    /**
     * 消息推送设置
     */
    public static void pageMessagePushSetting() {
        ARouter.getInstance()
                .build(PagePathConstant.pageMessagePushSetting)
                .navigation();
    }

    /**
     * 系统权限设置
     */
    public static void pageSystemPermissionSetting() {
        ARouter.getInstance()
                .build(PagePathConstant.pageSystemPermissionSetting)
                .navigation();
    }

    /**
     * 应用更新
     */
    public static void pageAppUpdate() {
        ARouter.getInstance()
                .build(PagePathConstant.pageAppUpdate)
                .navigation();
    }

    /**
     * 账号安全
     */
    public static void pageAccountSecurity() {
        ARouter.getInstance()
                .build(PagePathConstant.pageAccountSecurity)
                .navigation();
    }

    public static void pageWebView(String url) {
        ARouter.getInstance()
                .build(PagePathConstant.pageWebView)
                .withString(Constant.URL, url)
                .navigation();
    }
}
