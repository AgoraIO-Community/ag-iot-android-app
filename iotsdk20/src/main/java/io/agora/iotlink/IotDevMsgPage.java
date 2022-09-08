
package io.agora.iotlink;


import java.util.ArrayList;



/*
 * @brief 分页的设备消息
 */
public class IotDevMsgPage {

    public ArrayList<IotDevMessage> mDevMsgList = new ArrayList<>();  ///< 当前页的设备消息记录列表
    public int mPageIndex;                  ///< 当前页索引，从1开始
    public int mPageSize;                   ///< 当前页最多消息数量
    public int mTotalPage;                  ///< 总页数

    @Override
    public String toString() {
        String infoText = "{ mDevMsgList=" + mDevMsgList.size()
                + ", mPageIndex=" + mPageIndex
                + ", mPageSize=" + mPageSize
                + ", mTotalPage=" + mTotalPage + " }";
        return infoText;
    }

}
