/**
 * @file IDeviceMgr.java
 * @brief This file define the interface of devices management
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/*
 * @brief 设备管理接口
 */
public interface IDeviceMgr {

    //
    // 设备管理的状态机
    //
    public static final int DEVMGR_STATE_IDLE = 0x0000;            ///< 当前无设备操作
    public static final int DEVMGR_STATE_QUERYING = 0x0001;        ///< 正在查询设备列表
    public static final int DEVMGR_STATE_BINDING = 0x0002;         ///< 正在绑定设备
    public static final int DEVMGR_STATE_UNBINDING = 0x0003;       ///< 正在解绑设备
    public static final int DEVMGR_STATE_RENAMING = 0x0004;       ///< 正在重命名设备
    public static final int DEVMGR_STATE_SETINGPROP = 0x0005;      ///< 正在设置属性值
    public static final int DEVMGR_STATE_GETINGPROP = 0x0006;      ///< 正在获取属性值
    public static final int DEVMGR_STATE_PRODUCT_QUERYING = 0x0007;  ///< 正在查询产品列表
    public static final int DEVMGR_STATE_SHARING_DEV = 0x0008;     ///< 正在共享设备
    public static final int DEVMGR_STATE_DESHARING_DEV = 0x0009;   ///< 正在解除共享
    public static final int DEVMGR_STATE_ACCEPT_DEV = 0x000A;      ///< 正在接收设备
    public static final int DEVMGR_STATE_SHAREMSG_QUERY = 0x0010;  ///< 正在查询分享消息
    public static final int DEVMGR_STATE_SHAREMSG_DEL = 0x0011;    ///< 正在删除分享消息


    /**
     * @brief 设备固件版本信息
     */
    public static class McuVersionInfo {
        public String mDeviceNumber;        ///< 设备唯一的数字
        public String mDeviceID;            ///< 设备唯一ID(就是设备MAC)
        public long mUpgradeId;             ///< 升级记录Id，-1 表示没有获取到
        public boolean mIsupgradable;       ///< 当前固件是否可以升级
        public String mCurrVersion;         ///< 设备当前固件版本
        public String mUpgradeVersion;      ///< 设备可升级的版本
        public long mReleasedTime;          ///< 版本发布时间
        public long mSize;                  ///< 版本文件大小
        public String mRemark;              ///< 版本备注说明

        @Override
        public String toString() {
            String infoText = "{ mDeviceID=" + mDeviceID
                    + ", mDeviceNumber=" + mDeviceNumber
                    + ", mUpgradeId=" + mUpgradeId
                    + ", mIsupgradable=" + mIsupgradable
                    + ", mCurrVersion=" + mCurrVersion
                    + ", mUpgradeVersion=" + mUpgradeVersion
                    + ", mReleasedTime=" + mReleasedTime
                    + ", mSize=" + mSize
                    + ", mRemark=" + mRemark + " }";
            return infoText;
        }
    }

    /**
     * @brief 设备固件升级状态
     */
    public static class McuUpgradeStatus {
        public String mDeviceNumber;        ///< 设备唯一的数字
        public String mDeviceID;            ///< 设备唯一ID(就是设备MAC)
        public String mDeviceName;          ///< 设备名字
        public long mUpgradeId;             ///< 升级记录Id，-1 表示没有获取到
        public String mCurrVersion;         ///< 当前固件版本号
        public int mStatus;                 ///< 1--完成; 2--失败; 3--取消; 4--待升级; 5--升级中

        @Override
        public String toString() {
            String infoText = "{ mDeviceID=" + mDeviceID
                    + ", mDeviceNumber=" + mDeviceNumber
                    + ", mDeviceName=" + mDeviceName
                    + ", mUpgradeId=" + mUpgradeId
                    + ", mCurrVersion=" + mCurrVersion
                    + ", mStatus=" + mStatus + " }";
            return infoText;
        }
    }

    /*
     * @brief 产品信息
     */
    public static class ProductInfo {
        public long mId;                ///< 产品ID
        public String mName;            ///< 产品名称
        public String mAlias;           ///< 产品别名
        public String mProductNumber;   ///< 产品密钥
        public long mProductTypeId;     ///< 产品型号Id
        public String mProductTypeName; ///< 产品型号名称

        public long mMerchantId;        ///< 商户Id
        public String mMerchantName;    ///< 商户名称

        public String mImgBig;          ///< 产品大图
        public String mImgSmall;        ///< 产品小图

        public int mStatus;             ///< 状态: 1--正常； 0--停用；
        public int mDeleted;            ///< 是否已经删除
        public int mBindType;           ///< 绑定类型：1--可重复绑定； 2--不可重复绑定
        public int mConnectType;        ///< 连接类型：1--WIFI;  2--蜂窝

        public long mCreateBy;          ///< 创建人Id
        public long mCreateTime;        ///< 创建时间戳
        public long mUpdateBy;          ///< 更新人Id
        public long mUpdateTime;        ///< 最后更新时间戳


        @Override
        public String toString() {
            String infoText = "{ mId=" + mId + ", mName=" + mName + ", mAlias=" + mAlias
                    + ", mProductNumber=" + mProductNumber
                    + ", mProductTypeId=" + mProductTypeId
                    + ", mProductTypeName=" + mProductTypeName
                    + ", mMerchantId=" + mMerchantId + ", mMerchantName=" + mMerchantName
                    + ", mImgBig=" + mImgBig + ", mImgSmall=" + mImgSmall
                    + ", mStatus=" + mStatus + ", mDeleted=" + mDeleted
                    + ", mBindType=" + mBindType + ", mConnectType=" + mConnectType
                    + ", mCreateBy=" + mCreateBy + ", mCreateTime=" + mCreateTime
                    + ", mUpdateBy=" + mUpdateBy + ", mUpdateTime=" + mUpdateTime + " }";
            return infoText;
        }
    }

    /*
     * @brief 产品查询参数
     */
    public static class ProductQueryParam {
        public int mPageNo = -1;            ///< 查询的页码，<0 表示不设置该参数
        public int mPageSize = -1;          ///< 分页大小，<0 表示不设置该参数
        public long mProductTypeId = -1;    ///< 产品型号Id, <0 表示不设置该参数
        public int mConnectType = -1;       ///< 连接类型：1--WIFI;  2--蜂窝；<0 表示不设置该参数
        public String mBlurry;              ///< 保留字段不用设置

        @Override
        public String toString() {
            String infoText = "{ mPageNo=" + mPageNo + ", mPageSize=" + mPageSize
                    + ", mProductTypeId=" + mProductTypeId
                    + ", mConnectType=" + mConnectType
                    + ", mBlurry=" + mBlurry + " }";
            return infoText;
        }
    }

    /*
     * @brief 产品查询翻页信息
     */
    public static class ProductPageTurn {
        public int mFirstPage;
        public int mCurrentPage;
        public int mPage;
        public int mPrevPage;
        public int mNextPage;
        public int mPageCount;
        public int mPageSize;

        public int mRowCount;
        public int mStart;
        public int mEnd;
        public int mStartIndex;

        @Override
        public String toString() {
            String infoText = "{ mFirstPage=" + mFirstPage
                    + ", mCurrentPage=" + mCurrentPage + ", mPage=" + mPage
                    + ", mPrevPage=" + mPrevPage + ", mNextPage=" + mNextPage
                    + ", mPageCount=" + mPageCount + ", mPageSize=" + mPageSize
                    + ", mRowCount=" + mRowCount + ", mStart=" + mStart + ", mEnd=" + mEnd
                    + ", mStartIndex=" + mStartIndex + " }";
            return infoText;
        }
    }

    /*
     * @brief 产品查询结果
     */
    public static class ProductQueryResult {
        public int mErrCode;                        ///< 错误码
        public ProductQueryParam mQueryParam;       ///< 查询参数
        public List<ProductInfo> mProductList = new ArrayList<>();      ///< 查询到的产品列表
        public ProductPageTurn  mPageTurn = new ProductPageTurn();      ///< 查询结果翻页信息
    }

    /*
     * @brief 设备管理回调接口
     */
    public static interface ICallback {

        /*
         * @brief 查询所有设备完成事件
         * @param errCode : 查询结果错误码，0表示查询成功
         * @param deviceList : 返回查询到的绑定设备列表
         */
        default void onAllDevicesQueryDone(int errCode, List<IotDevice> deviceList) {}

        /*
         * @brief 添加设备完成事件
         * @param errCode : 添加结果错误码，0表示添加成功
         * @param addedDevice : 新增加的设备信息
         * @param bindDevList : 当前最新的绑定设备列表
         */
       default void onDeviceAddDone(int errCode, IotDevice addDevice,
                                    List<IotDevice> bindDevList) { }

        /*
         * @brief 移除设备完成事件
         * @param errCode : 移除结果错误码，0表示移除成功
         * @param delDevice : 移除的设备信息
         * @param bindDevList : 当前最新的绑定设备列表
         */
        default void onDeviceRemoveDone(int errCode, IotDevice delDevice,
                                        List<IotDevice> bindDevList) { }

        /*
         * @brief 重命名设备完成事件
         * @param errCode : 重命名结果错误码，0表示重命名成功
         * @param iotDevice : 要重命名的设备
         * @param newName : 新的名字
         */
        default void onDeviceRenameDone(int errCode, IotDevice iotDevice, String newName) { }

        /*
         * @brief 设备端属性值设置完成
         * @param device : 相应的设备信息，
         * @properties : 要设置的属性值
         */
        default void onSetPropertyDone(int errCode, IotDevice iotDevice,
                                       Map<String, Object> properties) {}

        /*
         * @brief 设备端属性值获取完成，属性值之后通过 onReceivedDeviceProperty() 回调上来
         * @param device : 相应的设备信息
         */
        default void onGetPropertyDone(int errCode, IotDevice device) {}

        /**
         * @brief 设备端固件版本获取完成
         * @param device : 相应的设备信息
         * @param mcuVerInfo : 固件版本信息
         */
        default void onGetMcuVerInfoDone(int errCode, final IotDevice device,
                                         final McuVersionInfo mcuVerInfo) {}

        /**
         * @brief 设备端固件版本升级完成
         * @param device : 相应的设备信息
         * @param upgradeId : 升级的Id号
         * @param decide : 升级方式
         */
        default void onUpgradeMcuVerDone(int errCode, final IotDevice device,
                                         long upgradeId, int decide) {}

        /**
         * @brief 获取到固件版本升级状态
         * @param device : 相应的设备信息
         * @param status : 升级的状态
         */
        default void onUpgradeStatusDone(int errCode, final IotDevice device,
                                          final McuUpgradeStatus status) { }

        /*
         * @brief 设备端属性值更新事件
         * @param device : 相应的设备信息，device.mProperty字段包含了设置的属性值
         */
        default void onReceivedDeviceProperty(IotDevice device, Map<String, Object> properties) {}

        /*
         * @brief 设备上下线事件
         * @param iotDevice : 相应的设备信息
         * @param online : 上线还是下线事件
         * @param bindedDevList : 更新状态后的当前绑定设备列表
         */
        default void onDeviceOnOffLine(IotDevice iotDevice, boolean online,
                                       List<IotDevice> bindedDevList ) {}

        /*
         * @brief 设备属性更新事件
         * @param iotDevice : 相应的设备信息,
         * @param properties : 属性列表
         */
        default void onDevicePropertyUpdate(IotDevice iotDevice, Map<String, Object> properties) {}

        /*
         * @brief 查询产品完成事件
         * @param iotDevice : 相应的设备信息, 仅 mDeviceMac和 mDeviceId 两个字段有效
         * @param properties : 属性列表
         */
        default void onQueryProductDone(ProductQueryResult queryResult) {}

        /*
         * @brief 设备分享完成事件
         * @param errCode : 错误码
         * @param force : 是否强制分享
         * @param iotDevice : 将要分享出去的设备
         * @param sharingAccount : 分享的目标账号
         * @param permission : 分享的权限：2--可以传导分享； 3--不能传导分享
         */
        default void onShareDeviceDone(int errCode, boolean force, final IotDevice iotDevice,
                                       final String sharingAccount, int permission) { }

        /*
         * @brief 完成设备的分享取消
         * @param errCode : 错误码
         * @param IotOutSharer : 要取消的分享
         */
        default void onDeshareDeviceDone(int errCode, final IotOutSharer outSharer) {}

        /*
         * @brief 接收来自其他账号的设备分享
         * @param deviceName : 设备名字
         * @param order : 分享口令
         * @return 错误码
         */
        default void onAcceptDeviceDone(int errCode, final String deviceName,
                                        final String order) { }


        /*
         * @brief 完成查询可分享的设备列表
         * @param errCode : 错误码
         * @param deviceList : 查询到的可分享设备列表
         */
        default void onQuerySharableDevListDone(int errCode, final List<IotDevice> deviceList) { }

        /*
         * @brief 完成查询查询分享出去的账号信息
         * @param errCode : 错误码
         * @param deviceNumber : 要查询的设备Number
         * @param outSharerList : 查询到的分享出去的账号信息
         */
        default void onQueryOutSharerListDone(int errCode, final String deviceNumber,
                                                  final List<IotOutSharer> outSharerList) { }

        /*
         * @brief 完成查询查询来自其他账号分享的设备列表
         * @param errCode : 错误码
         * @param deviceList : 查询到的分享进来的设备列表
         */
        default void onQueryInSharedDevList(int errCode, final List<IotDevice> deviceList) { }


        /*
         * @brief 分页查询设备分享消息
         * @param errCode : 错误码
         * @param shareMsgPage : 分页的分享消息新
         * @return 错误码
         */
        default void onQueryShareMsgPageDone(int errCode, final IotShareMsgPage shareMsgPage) { }

        /*
         * @brief 查询单个分享消息详情
         * @param errCode : 错误码
         * @param shareMessage : 当查询成功时，输出的消息详情
         * @return 错误码
         */
        default void onQueryShareMsgDetailDone(int errCode, final IotShareMessage shareMessage) { }

        /*
         * @brief 删除单个分享消息
         * @param errCode : 错误码
         * @param messageId : 删除的分享消息ID
         * @return 错误码
         */
        default void onDeleteShareMsgDone(int errCode, long messageId) { }
    }




    ////////////////////////////////////////////////////////////////////////
    //////////////////////////// Public Methods ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief 获取当前设备管理状态机
     * @return 返回状态机
     */
    int getStateMachine();

    /*
     * @brief 注册回调接口
     * @param callback : 回调接口
     * @return 错误码
     */
    int registerListener(IDeviceMgr.ICallback callback);

    /*
     * @brief 注销回调接口
     * @param callback : 回调接口
     * @return 错误码
     */
    int unregisterListener(IDeviceMgr.ICallback callback);

    /*
     * @brief 查询当前用户名下所有设备，触发 onDeviceQueryAllDone() 回调
     * @return 错误码
     */
    int queryAllDevices();

    /*
     * @brief 直接返回当前绑定的设备列表，该接口不从服务器查询，直接返回缓存的绑定设备列表
     *        通常需要至少通过 queryAllDevices() 查询过一次
     * @return 绑定设备列表
     */
    List<IotDevice> getBindDevList();


    /*
     * @brief 添加设备，触发 onDeviceAddDone() 回调
     * @param productNumber : 设备制造商Number
     * @param deviceID: 设备唯一(MAC地址)
     * @return 错误码
     */
    int addDevice(String productNumber, String deviceID);

    /*
     * @brief 移除设备，触发 onDeviceRemoveDone() 回调
     * @param removingDev : 要移除的设备信息
     * @return 错误码
     */
    int removeDevice(IotDevice removingDev);

    /*
     * @brief 设备重命名，触发 onDeviceRenameDone() 回调
     * @param iotDevice : 要重命名的设备信息
     * @param newName : 新的名字
     * @return 错误码
     */
    int renameDevice(IotDevice iotDevice, String newName);

    /*
     * @brief 设置设备属性，触发 onDeviceSetPropertyDone() 回调
     * @param iotDevice : 要设置属性的设备
     * @param properties : 要设置的属性列表
     * @return 错误码
     */
    int setDeviceProperty(IotDevice iotDevice, Map<String, Object> properties);

    /*
     * @brief 获取设备属性，触发 onGetPropertyDone() 和 onReceivedDeviceProperty() 回调
     * @param iotDevice : 要获取属性的设备
     * @return 错误码
     */
    int getDeviceProperty(IotDevice iotDevice);

    /**
     * @brief 获取设备古版本信息属性，触发 onGetMcuVerInfoDone() 回调
     * @param iotDevice : 要获取固件的设备
     * @return 错误码
     */
    int getMcuVersionInfo(IotDevice iotDevice);

    /**
     * @brief 升级设备固件版本，触发 onUpgradeMcuVerDone() 回调
     * @param iotDevice : 要获取固件的设备
     * @param upgradeId : 指定升级Id
     * @param decide : 1不升级(忽略本次升级); 2升级(确定升级); 0无效的决定
     * @return 错误码
     */
    int upgradeMcuVersion(IotDevice iotDevice, long upgradeId, int decide);

    /**
     * @brief 获取，触发 onUpgradeStatusDone() 回调
     * @param upgradeId : 指定升级Id
     * @return 错误码
     */
    int getMcuUpgradeStatus(IotDevice iotDevice, long upgradeId);


    /*
     * @brief 获取设备属性，触发 onQueryProductDone() 回调
     * @param queryParam : 产品查询参数
     * @return 错误码
     */
    int queryProductList(final ProductQueryParam queryParam);


    /*
     * @brief 分享设备给其他人，需要对方接受，触发 onShareDeviceDone() 回调
     * @param iotDevice : 将要分享出去的设备
     * @param sharingAccount : 分享的目标账号
     * @param permission : 分享的权限：2--可以传导分享； 3--不能传导分享
     * @param needPeerAgree : 是否需要对端同意，为false时表示强制分享，无需对端同意
     * @return 错误码
     */
    int shareDevice(final IotDevice iotDevice, final String sharingAccount,
                    int permission, boolean needPeerAgree);

    /*
     * @brief 取消设备的分享权限，触发 onDeshareDeviceDone() 回调
     * @param deviceId : 要取消的已经分享的设备Id
     * @param unsharingAccount : 要取消权限的账号
     * @return 错误码
     */
    int deshareDevice(final IotOutSharer outSharer);

    /*
     * @brief 接收来自其他账号的设备分享， 触发 onAcceptDeviceDone() 回调
     * @param deviceName : 设备名字
     * @param order : 分享口令
     * @return 错误码
     */
    int acceptDevice(final String deviceName, final String order);


    /*
     * @brief 查询可分享的设备列表，触发 onQuerySharableDevListDone() 回调
     * @return 错误码
     */
    int querySharableDevList();

    /*
     * @brief 查询单个设备分享出去的账号列表，触发 onQueryOutSharerListDone() 回调
     * @param deviceNumber : 设备Number
     * @return 错误码
     */
    int queryOutSharerList(final String deviceNumber);

    /*
     * @brief 查询来自其他账号分享的设备列表，触发 onQueryInSharedDevList() 回调
     * @return 错误码
     */
    int queryInSharedDevList();



    /*
     * @brief 分页查询设备分享消息，触发 onQueryShareMsgPageDone() 回调
     * @param pageNumber : 要查询的页号，-1表示不设置
     * @param pageSize : 每页列表最大数量，-1表示不设置
     * @param auditStatus : 0：查询所有消息；  1：查询已处理消息；  2：查询未处理消息
     * @return 错误码
     */
    int queryShareMsgByPage(int pageNumber, int pageSize, int auditStatus);

    /*
     * @brief 查询单个分享消息详情，触发 onQueryShareMsgDetailDone() 回调
     * @param messageId : 要查询的分享消息ID
     * @return 错误码
     */
    int queryShareMsgById(long messageId);

    /*
     * @brief 删除单个分享消息，触发 onDeleteShareMsgDone() 回调
     * @param messageId : 要删除的分享消息ID
     * @return 错误码
     */
    int deleteShareMsg(long messageId);

}
