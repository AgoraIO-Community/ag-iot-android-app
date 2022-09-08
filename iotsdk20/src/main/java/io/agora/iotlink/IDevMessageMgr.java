
package io.agora.iotlink;

import java.util.ArrayList;
import java.util.List;


/*
 * @brief 设备消息管理接口
 */
public interface IDevMessageMgr  {

    //
    // 设备消息管理系统的状态机
    //
    public static final int DEVMSGMGR_STATE_IDLE = 0x0001;       ///< 当前 空闲状态
    public static final int DEVMSGMGR_STATE_MARKING = 0x0001;    ///< 正在标记设备消息已读
    public static final int DEVMSGMGR_STATE_QUERYING = 0x0002;    ///< 正在查询中


    /*
     * @brief 设备消息页查询参数
     */
    public static class QueryParam {

        public List<String> mDevIDList = new ArrayList<>();    ///< 设备Number列表
        public String mProductID;   ///< 要查询的产品ID
        public int mMsgType;        ///< 消息类型
        public int mMsgStatus;      ///< 消息状态
        public String mBeginDate;   ///< 开始时间
        public String mEndDate;     ///< 结束事件
        public int mPageIndex;      ///< 要查询的页面索引，从1开始
        public int mPageSize;       ///< 页面告警数量大小
        public boolean mAscSort;    ///< 是否升序排序

        @Override
        public String toString() {
            String infoText = "{ mDevIDCount=" + mDevIDList.size()
                    + ", mProductID=" + mProductID
                    + ", mMsgType=" + mMsgType
                    + ", mMsgStatus=" + mMsgStatus
                    + ", mBeginDate=" + mBeginDate
                    + ", mEndDate=" + mEndDate
                    + ", mPageIndex=" + mPageIndex
                    + ", mPageSize=" + mPageSize
                    + ", mAscSort=" + mAscSort + "}";
            return infoText;
        }
    };

    /*
     * @brief 设备消息回调接口
     */
    public static interface ICallback {

        /*
         * @brief 接收到一个新的设备消息
         * @param devMessage : 设备消息
         */
        default void onDevMessageReceived(IotDevMessage devMessage) {}

        /*
         * @brief 设备消息标记完成回调
         * @param errCode : 结果错误码，0表示成功
         * @param deletedIdList : 标记的设备消息Id列表
         */
        default void onDevMessageMarkDone(int errCode, List<Long> markedIdList) {}

        /*
         * @brief 根据Id查询到的设备消息
         * @param errCode : 结果错误码，0表示成功
         * @param iotAlarm : 查询到的设备消息
         */
        default void onDevMessageInfoQueryDone(int errCode, final IotDevMessage devMessage) {}

        /*
         * @brief 分页查询到的设备消息
         * @param errCode : 结果错误码，0表示成功
         * @param alarmPage : 查询到的设备消息页面
         */
        default void onDevMessagePageQueryDone(int errCode, final QueryParam queryParam,
                                               final IotDevMsgPage devMsgPage) {}

        /*
         * @brief 查询到的设备消息数量
         * @param errCode : 结果错误码，0表示成功
         * @param queryParam : 查询参数
         * @param alarmNumber : 查询到的告警数量
         */
        default void onDevMessageNumberQueryDone(int errCode, final QueryParam queryParam,
                                                 long devMsgNumber) {}

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
    int registerListener(IDevMessageMgr.ICallback callback);

    /*
     * @brief 注销回调接口
     * @param callback : 回调接口
     * @return 错误码
     */
    int unregisterListener(IDevMessageMgr.ICallback callback);

    /*
     * @brief 标记多个设备消息为已读，触发 onDevMessageMarkDone() 回调
     * @param devMsgIdList : 要标记已读的设备消息Id列表
     * @return 错误码
     */
    int mark(List<Long> devMsgIdList);

    /*
     * @brief 根据消息Id查询详细的设备消息，触发 onDevMessageInfoQueryDone() 回调
     * @param devMsgId : 要查询的设备消息Id
     * @return 错误码
     */
    int queryById(long devMsgId);

    /*
     * @brief 查询指定页面的设备列表，触发 onDevMessagePageQueryDone() 回调
     * @param queryParam : 相应的查询参数
     * @return 错误码
     */
    int queryByPage(QueryParam queryParam);

    /*
     * @brief 根据条件查询所有设备消息，触发 onDevMessageNumberQueryDone() 回调
     * @param queryParam : 相应的查询参数，忽略 mPageIndex；mPageSize；mAscSort；三个字段
     * @return 错误码
     */
    int queryNumber(QueryParam queryParam);


}
