package io.agora.iotlinkdemo.models.device.add;

import android.annotation.SuppressLint;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ToastUtils;

import io.agora.iotlink.IotAlarmPage;
import io.agora.iotlink.IotDevMessage;
import io.agora.iotlink.IotDevice;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.base.PermissionHandler;
import io.agora.iotlinkdemo.base.PushApplication;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityAddDeviceResultBinding;
import io.agora.iotlinkdemo.databinding.ActivityDeviceBtCfgResultBinding;
import io.agora.iotlinkdemo.databinding.ActivityDeviceBtScanResultBinding;
import io.agora.iotlinkdemo.deviceconfig.DeviceBtCfg;
import io.agora.iotlinkdemo.dialog.EditNameDialog;
import io.agora.iotlinkdemo.event.ResetAddDeviceEvent;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.manager.PagePilotManager;
import io.agora.iotlinkdemo.models.device.DeviceViewModel;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import io.agora.iotlinkdemo.models.device.add.adapter.BtDevListAdapter;
import io.agora.iotlinkdemo.models.message.adapter.MessageNotifyAdapter;
import kotlin.jvm.JvmField;

/**
 * 蓝牙扫描失败
 */
@Route(path = PagePathConstant.pageDeviceBtScanRslt)
public class DeviceAddStepBtScanResultActivity extends BaseViewBindingActivity<ActivityDeviceBtScanResultBinding>
        implements DeviceBtCfg.IBtCfgCallback   {
    private final String TAG = "IOTLINK/BtScanRsltAct";


    @Override
    protected ActivityDeviceBtScanResultBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDeviceBtScanResultBinding.inflate(inflater);
    }


    @Override
    public void initListener() {
        getBinding().titleView.setRightIconClick(view -> resetAddDevice());
        getBinding().btnNextStep.setOnClickListener(view -> resetAddDevice());
    }

    /**
     * 回到首页重新开始流程
     */
    private void resetAddDevice() {
        EventBus.getDefault().post(new ResetAddDeviceEvent());
        mHealthActivityManager.popActivity();
    }

}
