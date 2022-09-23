package io.agora.iotlinkdemo.models.home.homeindex;

import android.os.Bundle;
import android.util.Log;
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

import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.IDeviceMgr;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingFragment;
import io.agora.iotlinkdemo.base.PermissionHandler;
import io.agora.iotlinkdemo.base.PermissionItem;
import io.agora.iotlinkdemo.databinding.FragmentHomeIndexBinding;
import io.agora.iotlinkdemo.manager.DevicesListManager;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.manager.UserManager;
import io.agora.iotlinkdemo.models.home.homeindex.adapter.DevicesAdapter;
import io.agora.iotlinkdemo.utils.ErrorToastUtils;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IotDevice;

import java.util.ArrayList;
import java.util.List;

public class HomeIndexFragment extends BaseViewBindingFragment<FragmentHomeIndexBinding>
        implements PermissionHandler.ICallback  {

    private static final String TAG = "LINK/HomeIndexFrag";

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

    private PermissionHandler mPermHandler;             ///< 权限申请处理
    private IotDevice mSelectedDev;


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
            // PagePilotManager.pageDeviceAddScanning();

            //
            // Camera权限判断处理
            //
            int[] permIdArray = new int[1];
            permIdArray[0] = PermissionHandler.PERM_ID_CAMERA;
            mPermHandler = new PermissionHandler(getActivity(), this, permIdArray);
            if (!mPermHandler.isAllPermissionGranted()) {
                Log.d(TAG, "<startAddDevice> requesting permission...");
                mPermHandler.requestNextPermission();
            } else {
                Log.d(TAG, "<startAddDevice> swith page");
                PagePilotManager.pageDeviceAddScanning();
            }
        }

//
//        TODO: Test code for query product list
//        IDeviceMgr.ProductQueryParam queryParam = new IDeviceMgr.ProductQueryParam();
//        queryParam.mPageNo = 1;
//        queryParam.mPageSize = 64;
//
//        showLoadingView();
//        int errCode = AIotAppSdkFactory.getInstance().getDeviceMgr().queryProductList(queryParam);
//        if (errCode != ErrCode.XOK) {
//            hideLoadingView();
//            popupMessage("查询所有产品列表失败，错误码=" + errCode);
//        }
    }

    void onBtnDevItemClick(View view, int position, IotDevice iotDevice) {
        //
        // Microphone权限判断处理
        //
        int[] permIdArray = new int[1];
        permIdArray[0] = PermissionHandler.PERM_ID_RECORD_AUDIO;
        mPermHandler = new PermissionHandler(getActivity(), this, permIdArray);
        if (!mPermHandler.isAllPermissionGranted()) {
            Log.d(TAG, "<onBtnDevItemClick> requesting permission...");
            mSelectedDev = iotDevice;
            mPermHandler.requestNextPermission();
        } else {
            Log.d(TAG, "<onBtnDevItemClick> permission ready");
            doCallDial(iotDevice);
        }
    }

    void doCallDial(IotDevice iotDevice) {
        showLoadingView();
        int errCode = homeIndexViewModel.callDial(iotDevice, "home list call");
        if (errCode != ErrCode.XOK) {
            hideLoadingView();
            ErrorToastUtils.showCallError(errCode);
        }
    }

    public void onFragRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "<onFragRequestPermissionsResult> requestCode=" + requestCode);
        if (mPermHandler != null) {
            mPermHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public void onAllPermisonReqDone(boolean allGranted, final PermissionItem[] permItems) {
        Log.d(TAG, "<onAllPermisonReqDone> allGranted = " + allGranted);

        if (permItems[0].requestId == PermissionHandler.PERM_ID_CAMERA) {  // Camera权限结果
            if (allGranted) {
                PagePilotManager.pageDeviceAddScanning();
            } else {
                popupMessage(getString(R.string.no_permission));
            }

        } else if (permItems[0].requestId == PermissionHandler.PERM_ID_RECORD_AUDIO) { // 麦克风权限结果
            if (allGranted) {
                doCallDial(mSelectedDev);
            } else {
                popupMessage(getString(R.string.no_permission));
            }
        }
    }


    private void initAdapter() {
        if (mAdapter == null) {
            mAdapter = new DevicesAdapter(devices);
            getBinding().rvDevices.setLayoutManager(new LinearLayoutManager(getActivity()));
            getBinding().rvDevices.setAdapter(mAdapter);
            mAdapter.setMRVItemClickListener((view, position, data) -> {
                onBtnDevItemClick(view, position, data);
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
