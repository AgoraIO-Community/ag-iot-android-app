
package io.agora.iotlink;


import java.util.ArrayList;



/*
 * @brief 分页的分享消息
 */
public class IotShareMsgPage {

    public ArrayList<IotShareMessage> mShareMsgList = new ArrayList<>();  ///< 当前页的分享消息记录列表
    public int mPageIndex;                  ///< 当前页索引，从1开始
    public int mPageSize;                   ///< 当前页最多消息数量
    public int mTotalPage;                  ///< 总页数

    @Override
    public String toString() {
        String infoText = "{ mShareMsgCount=" + mShareMsgList.size()
                + ", mPageIndex=" + mPageIndex
                + ", mPageSize=" + mPageSize
                + ", mTotalPage=" + mTotalPage + " }";
        return infoText;
    }

}
