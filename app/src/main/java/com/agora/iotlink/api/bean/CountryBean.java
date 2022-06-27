package com.agora.iotlink.api.bean;

import com.agora.baselibrary.base.BaseBean;

public class CountryBean extends BaseBean {
    public boolean isSelect;
    public String countryName;
    public int countryId;

    public CountryBean(String countryName, int countryId) {
        this.countryName = countryName;
        this.countryId = countryId;
    }
}
