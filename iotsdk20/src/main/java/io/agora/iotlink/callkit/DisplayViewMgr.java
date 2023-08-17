
package io.agora.iotlink.callkit;


import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.agora.iotlink.ICallkitMgr;


/**
 * @brief 显示控件管理器
 */
public class DisplayViewMgr {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/DisplayViewMgr";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private HashMap<String, View> mViewMap = new HashMap<>();  ///< 控件映射表


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 设置设备的显示控件
     * @param deviceId : 要设置的设备Id
     * @param displayView : 设备视频帧显示控件，如果未null表示不显示
     * @return None
     */
    public void setDisplayView(final String deviceId, final View displayView) {
        synchronized (mViewMap) {
            if (displayView == null) {  // 直接移除相应的映射
                mViewMap.remove(deviceId);

            } else {    // 设置设备与控件的映射
                mViewMap.put(deviceId, displayView);
            }
        }
    }

    /**
     * @brief 获取设备相应的显示控件
     * @param deviceId : 要获取的设备Id
     * @return 返回显示控件，如果没有则返回null
     */
    public View getDisplayView(final String deviceId) {
        synchronized (mViewMap) {
            View displayView = mViewMap.get(deviceId);
            return displayView;
        }
    }


    /**
     * @brief  获取映射表数量
     * @return
     */
    public int size() {
        synchronized (mViewMap) {
            int count = mViewMap.size();
            return count;
        }
    }

    /**
     * @brief 清空映射表
     * @return None
     */
    public void clear() {
        synchronized (mViewMap) {
            mViewMap.clear();
        }
    }




}