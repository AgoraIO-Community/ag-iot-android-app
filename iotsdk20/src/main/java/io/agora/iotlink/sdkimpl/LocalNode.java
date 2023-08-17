
package io.agora.iotlink.sdkimpl;




/*
 * @brief 本地节点的信息
 */
public class LocalNode {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public boolean mReady;                  ///< 是否已经登录成功
    public String mUserId;                  ///< 本地登录用户Id, 与 NodeId是一一对应的
    public String mNodeId;                  ///< 本地 NodeId
    public String mRegion;                  ///< 登录后获取到的区域信息
    public String mToken;                   ///< 登录后获取到的Token


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Public Methods ////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        String infoText = "{ mReady=" + mReady
                + ", mUserId=" + mUserId
                + ", mNodeId=" + mNodeId
                + ", mRegion=" + mRegion
                + ", mToken=" + mToken  + " }";
        return infoText;
    }

}
