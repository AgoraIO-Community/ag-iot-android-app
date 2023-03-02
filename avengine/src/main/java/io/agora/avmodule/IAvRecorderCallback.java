package io.agora.avmodule;

import java.util.List;


/*
 * @breief 录像回调接口
 */
public interface IAvRecorderCallback {

    /**
     * @breief 录像过程中产生错误
     * @param recorderParam : 录像参数
     * @param errCode      ：转换结果错误代码，0表示正常
     * @return None
     */
    void onRecorderError(AvRecorderParam recorderParam, int errCode);


}