package com.agora.iotlink.models.home.homeindex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.agora.baselibrary.utils.NetUtils;
import com.agora.baselibrary.utils.ToastUtils;
import com.agora.baselibrary.utils.UiUtils;
import com.agora.iotlink.base.BaseViewBindingFragment;
import com.agora.iotlink.databinding.FragmentHomeIndexBinding;
import com.agora.iotlink.manager.DevicesListManager;
import com.agora.iotlink.manager.PagePilotManager;
import com.agora.iotlink.manager.UserManager;
import com.agora.iotlink.models.home.homeindex.adapter.DevicesAdapter;
import com.agora.iotlink.utils.ErrorToastUtils;
import com.agora.iotsdk20.ErrCode;
import com.agora.iotsdk20.IotDevice;

import java.util.ArrayList;
import java.util.List;

public class HomeIndexFragment extends BaseViewBindingFragment<FragmentHomeIndexBinding> {
    /**
     * 首页viewModel
     */
    private HomeIndexViewModel homeIndexViewModel;
    /**
     * 设备列表adapter
     */
    private DevicesAdapter mAdapter;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private AppCompatTextView textNoDevices;
    private AppCompatButton btnAddDevice;

    private ArrayList<IotDevice> devices = new ArrayList<>();

    @NonNull
    @Override
    protected FragmentHomeIndexBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomeIndexBinding.inflate(inflater);
    }

    @Override
    public void initView() {
        homeIndexViewModel = new ViewModelProvider(this).get(HomeIndexViewModel.class);
        homeIndexViewModel.setLifecycleOwner(this);
        initAdapter();
//        homeIndexViewModel.getAllDevices().observe(this, (Observer) o -> refreshAdapter());
        mSwipeRefreshLayout = getBinding().srlDevList;
        textNoDevices = getBinding().textNoDevices;
        btnAddDevice = getBinding().btnAddDevice;
    }

    @Override
    public void initListener() {
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            homeIndexViewModel.requestDeviceList();
            mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(false));
        });

        getBinding().titleView.setRightIconClick(view -> {
            startAddDevice();
        });
        getBinding().btnAddDevice.setOnClickListener(view -> {
            startAddDevice();
        });
        homeIndexViewModel.setISingleCallback((type, var2) -> {
            if (type == 999) {
                getActivity().finish();
            } else if (type == 2) {
                mSwipeRefreshLayout.post(() -> {
                    devices.clear();
                    if (UserManager.isLogin()) {
                        for (IotDevice device : (List<IotDevice>) var2) {
                            if ("0".equals(device.mSharer)) {
                                devices.add(device);
                            }
                        }
                        DevicesListManager.devicesList=devices;
                        DevicesListManager.deviceSize = devices.size();
                        if (devices.size() != ((List<IotDevice>) var2).size()) {
                            IotDevice titleDevice = new IotDevice();
                            titleDevice.mDeviceNumber = "199";
                            devices.add(titleDevice);
                            for (IotDevice device : (List<IotDevice>) var2) {
                                if (device.mSharer != null && !device.mSharer.equals("0")) {
                                    devices.add(device);
                                }
                            }
                            DevicesListManager.devicesList = devices;
                            DevicesListManager.deviceSize = devices.size() - 1;
                        }

                        mAdapter.notifyDataSetChanged();
                        initViewStatus();
                    }
                });

            } else if (type == 0) {
                getBinding().btnAddDevice.post(this::hideLoadingView);
            }
        });
    }

    private void startAddDevice() {
        if (UserManager.isLogin() && !UiUtils.INSTANCE.isFastClick()) {
            PagePilotManager.pageDeviceAddScanning();
        }
    }

    private void initAdapter() {
        if (mAdapter == null) {
            mAdapter = new DevicesAdapter(devices);
            getBinding().rvDevices.setLayoutManager(new LinearLayoutManager(getActivity()));
            getBinding().rvDevices.setAdapter(mAdapter);
            mAdapter.setMRVItemClickListener((view, position, data) -> {
                showLoadingView();
                int errCode = homeIndexViewModel.callDial(data, "home list call");
                if (errCode != ErrCode.XOK) {
                    hideLoadingView();
                    ErrorToastUtils.showCallError(errCode);
                }
            });
        }
    }

    private void initViewStatus() {
        if (mAdapter.getDatas() == null || mAdapter.getDatas().isEmpty()) {
            textNoDevices.setVisibility(View.VISIBLE);
            btnAddDevice.setVisibility(View.VISIBLE);
            mSwipeRefreshLayout.setVisibility(View.GONE);
        } else {
            textNoDevices.setVisibility(View.GONE);
            btnAddDevice.setVisibility(View.GONE);
            mSwipeRefreshLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (NetUtils.INSTANCE.isNetworkConnected()) {
            mSwipeRefreshLayout.post(() -> homeIndexViewModel.requestDeviceList());
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        homeIndexViewModel.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        homeIndexViewModel.onStop();
    }
}
