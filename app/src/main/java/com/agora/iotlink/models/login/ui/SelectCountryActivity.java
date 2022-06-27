package com.agora.iotlink.models.login.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.agora.baselibrary.listener.OnRvItemClickListener;
import com.agora.iotlink.api.bean.CountryBean;
import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.common.Constant;
import com.agora.iotlink.databinding.ActivitySelectCountryBinding;
import com.agora.iotlink.models.login.LoginViewModel;
import com.agora.iotlink.models.login.ui.adapter.CountryAdapter;

import java.util.ArrayList;

public class SelectCountryActivity extends BaseViewBindingActivity<ActivitySelectCountryBinding> {
    private ArrayList<CountryBean> countries = new ArrayList();
    private CountryAdapter countryAdapter;
    private LoginViewModel loginViewModel;
    private CountryBean selectCountry;

    @Override
    protected ActivitySelectCountryBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivitySelectCountryBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        loginViewModel.setLifecycleOwner(this);
        countryAdapter = new CountryAdapter(countries);
        getBinding().rvCountries.setAdapter(countryAdapter);
        getBinding().rvCountries.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public void requestData() {
        countries.add(new CountryBean("中国", 10));
        countries.add(new CountryBean("美国", 11));
        countryAdapter.notifyItemInserted(4);
    }

    @Override
    public void initListener() {
        countryAdapter.setMRVItemClickListener((view, position, data) -> {
            selectCountry = data;
            getBinding().btnFinish.setVisibility(View.VISIBLE);
        });
        getBinding().btnFinish.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.putExtra(Constant.COUNTRY, selectCountry);
            setResult(RESULT_OK, intent);
            mHealthActivityManager.popActivity();
        });
    }
}
