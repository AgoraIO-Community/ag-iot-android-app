/**
 * @file IAlarmMgr.java
 * @brief This file define the interface of alarm management
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink;

import java.util.List;


/*
 * @brief 告警信息管理接口
 */
public interface IAlarmMgr  {

    //
    // 告警管理系统的状态机
    //
    public static final int ALARMMGR_STATE_IDLE = 0x0001;       ///< 当前 空闲状态
    public static final int ALARMMGR_STATE_ADDING = 0x0002;     ///< 正在添加告警信息
    public static final int ALARMMGR_STATE_DELETING = 0x0003;   ///< 正在删除告警信息
    public static final int ALARMMGR_STATE_MARKING = 0x0004;    ///< 正在标记告警进行已读
    public static final int ALARMMGR_STATE_QUERYING = 0x0005;   ///< 正在查询中


    /**
     * @brief 告警信息插入参数
     */
    public static class InsertParam {
        public long mBeginTime;     ///< 告警开始时间戳
        public String mImageId;     ///< 图片Id
        public String mProductID;   ///< 产品Id
        public String mDeviceID;    ///< 设备Id
        public String mDeviceName;  ///< 设备名
        public String mDescription; ///< 告警消息内容
        public int mMsgType;        ///< 告警消息类型，{0:声音检测, 1:移动侦测，99：其他}
        public int mMsgStatus;      ///< 默认是0，{0：未读, 1：已读}

        @Override
        public String toString() {
            String infoText = "{ mBeginTime=" + mBeginTime
                    + ", mImageId=" + mImageId
                    + ", mProductId=" + mProductID
                    + ", mDeviceId=" + mDeviceID
                    + ", mDeviceName=" + mDeviceName
                    + ", mMsgType=" + mMsgType
                    + ", mMsgStatus=" + mMsgStatus
                    + ", mDescription=" + mDescription + " }";
            return infoText;
        }
    }


    /*
     * @brief 告警信息页查询参数
     */
    public static class QueryParam {
        public String mProductID;   ///< 要查询的产品Id
        public String mDeviceID;    ///< 要查询的设备Id
        public int mMsgType;        ///< 消息类型
        public int mMsgStatus;      ///< 消息状态
        public String mBeginDate;   ///< 开始时间
        public String mEndDate;     ///< 结束事件
        public int mPageIndex;      ///< 要查询的页面索引，从1开始
        public int mPageSize;       ///< 页面告警数量大小
        public boolean mAscSort;    ///< 是否升序排序

        @Override
        public String toString() {
            String infoText = "{ mProductId=" + mProductID
                    + ", mDeviceId=" + mDeviceID
                    + ", mMsgType=" + mMsgType
                    + ", mMsgStatus=" + mMsgStatus
                    + ", mBeginDate=" + mBeginDate
                    + ", mEndDate=" + mEndDate
                    + ", mPageIndex=" + mPageIndex
                    + ", mPageSize=" + mPageSize + " }";
            return infoText;
        }
    };

    /*
     * @brief 告警信息回调接口
     */
    public static interface ICallback {

        /*
         * @brief 接收到一个新的告警事件
         * @param alarm : 告警消息
         */
        default void onReceivedAlarm(IotAlarm alarm) {}

        /*
         * @brief 插入告警完成事件
         * @param errCode : 结果错误码，0表示成功
         * @param insertParam : 插入的告警参数
         */
        default void onAlarmAddDone(int errCode,  InsertParam insertParam) {}

        /*
         * @brief 告警删除完成回调
         * @param errCode : 结果错误码，0表示成功
         * @param deletedIdList : 删除的告警信息Id列表
         */
        default void onAlarmDeleteDone(int errCode, List<Long> deletedIdList) {}

        /*
         * @brief 告警标记完成回调
         * @param errCode : 结果错误码，0表示成功
         * @param deletedIdList : 标记的告警信息Id列表
         */
        default void onAlarmMarkDone(int errCode, List<Long> markedIdList) {}

        /*
         * @brief 根据Id查询到的告警信息
         * @param errCode : 结果错误码，0表示成功
         * @param iotAlarm : 查询到的告警信息
         */
        default void onAlarmInfoQueryDone(int errCode, final IotAlarm iotAlarm) {}

        /*
         * @brief 分页查询到的告警信息
         * @param errCode : 结果错误码，0表示成功
         * @param alarmPage : 查询到的告警信息页面
         */
        default void onAlarmPageQueryDone(int errCode, final QueryParam queryParam,
                                          final IotAlarmPage alarmPage) {}

        /*
         * @brief 分页查询到的告警信息
         * @param errCode : 结果错误码，0表示成功
         * @param alarmNumber : 查询到的告警数量
         */
        default void onAlarmNumberQueryDone(int errCode, final QueryParam queryParam,
                                              long alarmNumber) {}

    }




    ////////////////////////////////////////////////////////////////////////
    //////////////////////////// Public Methods ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief 获取当前账号管理状态机
     * @return 返回状态机
     */
    int getStateMachine();

    /*
     * @brief 注册回调接口
     * @param callback : 回调接口
     * @return 错误码
     */
    int registerListener(IAlarmMgr.ICallback callback);

    /*
     * @brief 注销回调接口
     * @param callback : 回调接口
     * @return 错误码
     */
    int unregisterListener(IAlarmMgr.ICallback callback);

    /*
     * @brief 插入告警信息，触发 onAlarmAddDone() 回调
     * @param insertParam : 要插入的告警信息
     * @return 错误码
     */
    int insert(InsertParam insertParam);

    /*
     * @brief 删除多个告警信息，触发 onAlarmDeleteDone() 回调
     * @param alarmIdList : 要删除的告警信息Id列表
     * @return 错误码
     */
    int delete(List<Long> alarmIdList);

    /*
     * @brief 标记多个告警信息为已读，触发 onAlarmMarkDone() 回调
     * @param alarmIdList : 要标记已读的告警信息Id列表
     * @return 错误码
     */
    int mark(List<Long> alarmIdList);

    /*
     * @brief 根据告警Id查询详细的告警信息，触发 onAlarmInfoQueryDone() 回调
     * @param alarmId : 要查询的告警信息Id列表
     * @return 错误码
     */
    int queryById(long alarmId);

    /*
     * @brief 查询指定页面的设备列表，触发 onAlarmPageQueryDone() 回调
     * @param queryParam : 相应的查询参数
     * @return 错误码
     */
    int queryByPage(QueryParam queryParam);

    /*
     * @brief 根据条件查询所有告警数量，触发 onAlarmNumberQueryDone() 回调
     * @param queryParam : 相应的查询参数，忽略 mPageIndex；mPageSize；mAscSort；三个字段
     * @return 错误码
     */
    int queryNumber(QueryParam queryParam);


}
