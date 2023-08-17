package io.agora.sdkwayang.protocol;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;



public class BaseData implements Serializable {

    //
    // 定义命令调用的接口类型
    //
    public final static int TYPE_INVOKE_SDK = 1;            ///< SDK调用



    private int type;               ///< 1：调用SDK接口
    private String device;
    private long sequence;
    private String cmd;
    private ConcurrentHashMap<String, Object> info;
    private ConcurrentHashMap<String, Object> extra;


    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public ConcurrentHashMap<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(ConcurrentHashMap<String, Object> extra) {
        this.extra = extra;
    }

    public ConcurrentHashMap<String, Object> getInfo() {
        return info;
    }

    public void setInfo(ConcurrentHashMap<String, Object> info) {
        this.info = info;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    @Override
    public String toString() {
        return "BaseData{" +
                "type=" + type +
                ", device='" + device + '\'' +
                ", sequence='" + sequence + '\'' +
                ", cmd='" + cmd + '\'' +
                ", info=" + info +
                ", extra=" + extra +
                '}';
    }
}
