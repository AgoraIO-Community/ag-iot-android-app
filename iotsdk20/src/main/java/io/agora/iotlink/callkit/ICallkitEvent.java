package io.agora.iotlink.callkit;

import java.util.List;

public interface ICallkitEvent {
    /*
     * @breief 主叫时拨号成功状态，等待对端接听
     * @param callState : 本地当前呼叫状态
     * @param errCode : 错误代码，< 0 表示拨号错误
     */
    default void onDial(CallkitContext callState, int errCode) {}

    /*
     * @breief 被叫方时才会收到该消息，表明有远端的呼叫
     * @param callState : 本地当前呼叫状态
     * @param attachMsg : 呼叫时附带的消息
     */
    default void onIncoming(CallkitContext callState, String attachMsg) {}

    /*
     * @breief 通话接通，表明可以开始实时通话
     * @param callState : 本地当前呼叫状态
     * @param errCode : 错误代码，< 0 表示响应失败
     */
    default void onAnswer(CallkitContext callState, int errCode) {}

    /*
     * @breief 忙状态，双方无法建立通话
     * @param callState : 本地当前呼叫状态
     */
    default void onBusy(CallkitContext callState) {}

    /*
     * @breief 通话被挂断，退出通话
     * @param callState : 本地当前呼叫状态
     */
    default void onHangup(CallkitContext callState) {}

    /*
     * @breief 呼叫等待超时，退出呼叫状态
     * @param callState : 本地当前呼叫状态
     */
    default void onTimeout(CallkitContext callState) {}
}
