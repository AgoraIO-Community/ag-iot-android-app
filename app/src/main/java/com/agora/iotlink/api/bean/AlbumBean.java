package com.agora.iotlink.api.bean;

import com.agora.baselibrary.base.BaseBean;

/**
 * 相册数据
 */
public class AlbumBean extends BaseBean {
    /**
     * layout 类型 0是日期 1 是数据
     */
    public int itemType = 1;
    /**
     * 日期
     */
    public String date = "2022-05-16";
    /**
     * 时间
     */
    public String time = "10:31";
    /**
     * 封面
     */
    public String mediaCover;
    /**
     * 0 图片 1 视频
     */
    public int mediaType;
    /**
     * 视频时长 秒
     */
    public int duration;

    /**
     * 是否被选中
     */
    public boolean isSelect = false;
    /**
     * 文件名
     */
    public String filePath;

}
