package io.agora.iotlinkdemo.manager;

import android.util.Log;

import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAgoraIotAppSdk;
import io.agora.iotlink.IDeviceMgr;
import io.agora.iotlink.IotDevice;
import io.agora.iotlink.sdkimpl.AgoraIotAppSdk;

import java.util.ArrayList;
import java.util.List;

public class DevicesListManager implements IDeviceMgr.ICallback {
    private static final String TAG = "LINK/DevListMgr";

    //
    // 产品列表状态机
    //
    private static final int PRODUCT_LIST_INVALID = 0x0000;     ///< 产品列表无效
    private static final int PRODUCT_LIST_QUERYING = 0x0001;    ///< 产品列表查询中
    private static final int PRODUCT_LIST_READY = 0x0002;       ///< 产品列表就绪


    public static ArrayList<IotDevice> devicesList = new ArrayList();
    public static int deviceSize = 0;



    private static final Object mDataLock = new Object();       ///< 同步访问锁
    private static DevicesListManager mDevListMgrInstance = null;
    private static List<IDeviceMgr.ProductInfo> mProductList = new ArrayList<>();
    private static volatile int mProductListState = PRODUCT_LIST_INVALID;



    ////////////////////////////////////////////////////////////////////
    ///////////////////////////// Public Methods ////////////////////////
    /////////////////////////////////////////////////////////////////////
    public static DevicesListManager getInstance() {
        if (mDevListMgrInstance == null) {
            synchronized (DevicesListManager.class) {
                if(mDevListMgrInstance == null) {
                    mDevListMgrInstance = new DevicesListManager();
                }
            }
        }

        return mDevListMgrInstance;
    }

    private DevicesListManager() {
        AIotAppSdkFactory.getInstance().getDeviceMgr().registerListener(this);
    }

    ////////////////////////////////////////////////////////////////
    ///////////////////////////// 产品列表操作 ////////////////////////
    ////////////////////////////////////////////////////////////////
    private int getProductListState() {
        synchronized (mDataLock) {
            return mProductListState;
        }
    }

    private void setProductListState(int state) {
        synchronized (mDataLock) {
            mProductListState = state;
        }
    }

    /**
     * @brief 查询产品列表，通常只需要查询一次即可，查询结果保留在内存中
     */
    public void queryAllProductList() {
        int productState = getProductListState();
        if (productState == PRODUCT_LIST_READY || productState == PRODUCT_LIST_QUERYING) {
            Log.e(TAG, "<queryAllProductList> product list ready or querying");
            return;
        }

        Log.d(TAG, "<queryAllProductList> querying...");
        setProductListState(PRODUCT_LIST_QUERYING);  // 产品列表 查询状态
        IDeviceMgr.ProductQueryParam queryParam = new IDeviceMgr.ProductQueryParam();
        queryParam.mPageNo = 1;
        queryParam.mPageSize = 128;


        int errCode = AIotAppSdkFactory.getInstance().getDeviceMgr().queryProductList(queryParam);
        if (errCode != ErrCode.XOK) {
            Log.e(TAG, "<queryAllProductList> failed, errCode=" + errCode);
            setProductListState(PRODUCT_LIST_INVALID);   // 产品列表 无效状态
        }
    }

    @Override
    public void onQueryProductDone(IDeviceMgr.ProductQueryResult queryResult) {
        Log.d(TAG, "<onQueryProductDone> errCode=" + queryResult.mErrCode
                + ", productCount=" + queryResult.mProductList.size());

        if (queryResult.mErrCode != ErrCode.XOK) {
            synchronized (mDataLock) {
                mProductListState = PRODUCT_LIST_INVALID;  // 产品列表 无效状态
            }
            return;
        }

        synchronized (mDataLock) {
            mProductListState = PRODUCT_LIST_READY;
            mProductList.clear();
            mProductList.addAll(queryResult.mProductList); // 产品列表 有效状态
        }
    }


    /**
     * @brief 根据 productID 来查询相应的产品小图片
     *        如果没有查询到则返回 null
     */
    public String getProductSmallImg(final String productID) {
        if (getProductListState() == PRODUCT_LIST_INVALID) {
            Log.e(TAG, "<getProductSmallImg> product list invalid");
            queryAllProductList();
            return null;
        }

        if (getProductListState() == PRODUCT_LIST_QUERYING) {
            Log.e(TAG, "<getProductSmallImg> product list querying");
            return null;
        }

        synchronized (mDataLock) {
            int productCount = mProductList.size();
            for (int i = 0; i < productCount; i++) {
                IDeviceMgr.ProductInfo productInfo = mProductList.get(i);
                if (productInfo.mProductID.compareToIgnoreCase(productID) == 0) {
                    return productInfo.mImgSmall;
                }
            }
        }

        Log.e(TAG, "<getProductSmallImg> not found, productID=" + productID);
        return null;
    }

    /**
     * @brief 根据 productID 来查询相应的产品大图片
     *        如果没有查询到则返回 null
     */
    public String getProductBigImg(final String productID) {
        if (getProductListState() == PRODUCT_LIST_INVALID) {
            Log.e(TAG, "<getProductBigImg> product list invalid");
            queryAllProductList();
            return null;
        }

        if (getProductListState() == PRODUCT_LIST_QUERYING) {
            Log.e(TAG, "<getProductBigImg> product list querying");
            return null;
        }

        synchronized (mDataLock) {
            int productCount = mProductList.size();
            for (int i = 0; i < productCount; i++) {
                IDeviceMgr.ProductInfo productInfo = mProductList.get(i);
                if (productInfo.mProductID.compareToIgnoreCase(productID) == 0) {
                    return productInfo.mImgBig;
                }
            }
        }

        Log.e(TAG, "<getProductBigImg> not found, productID=" + productID);
        return null;
    }


}
