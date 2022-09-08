package io.agora.iotlinkdemo.models.device.setting.mydevice;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ToastUtils;

import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IDeviceMgr;
import io.agora.iotlink.IotDevice;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.databinding.ActivityDeviceFirmwareUpgradeBinding;
import io.agora.iotlinkdemo.dialog.CheckUpdateDialog;
import io.agora.iotlinkdemo.dialog.ImportantTipsDialog;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import io.agora.iotlinkdemo.models.device.DeviceViewModel;

import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 固件升级
 */
@Route(path = PagePathConstant.pageDeviceFirmwareUpgrade)
public class DeviceFirmwareUpgradeActivity extends
        BaseViewBindingActivity<ActivityDeviceFirmwareUpgradeBinding>
        implements IDeviceMgr.ICallback    {

    private final String TAG = "IOTLINK/UpgradeAct";
    private final long QUERY_INTERVAL = 4000;       ///< 定时4秒查询一次更新状态
    private final int MAX_QUERY_COUNT = 76;         ///< 最多轮询76次，304秒

    //
    // Message Id
    //
    public static final int MSGID_UPGRADE_REQUEST = 0x1001;
    public static final int MSGID_UPGRADE_QUERY = 0x1002;

    //
    // 设备升级状态
    //
    public static final int UPGRADE_STATUS_DONE = 1;        ///< 升级完成
    public static final int UPGRADE_STATUS_FAIL = 2;        ///< 升级失败
    public static final int UPGRADE_STATUS_CANCEL = 3;      ///< 升级取消
    public static final int UPGRADE_STATUS_READY = 4;       ///< 待升级
    public static final int UPGRADE_STATUS_ONGOING = 5;     ///< 升级中


    private IDeviceMgr.McuVersionInfo mMcuVersionInfo;
    private Handler mMsgHandler = null;                 ///< 主线程中的消息处理
    private volatile int mQueryCount = 0;               ///< 轮询次数统计


    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////// Override Methods of Base Activity //////////////////////
    ////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected ActivityDeviceFirmwareUpgradeBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDeviceFirmwareUpgradeBinding.inflate(inflater);
    }

    @Override
    public void requestData() {
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        mMcuVersionInfo = AgoraApplication.getInstance().getLivingMcuVersion();
        if (mMcuVersionInfo == null) {  // 没有获取到固件版本信息
            Log.e(TAG, "<initView> NOT get mcu version!");
            finish();
            return;
        }
        mMsgHandler = new Handler(Looper.myLooper()) {
            @SuppressLint("HandlerLeak")
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSGID_UPGRADE_REQUEST:
                        onMsgUpgradeRequest();
                        break;

                    case MSGID_UPGRADE_QUERY:
                        onMsgUpgradeQuery();
                        break;
                }
            }
        };
        hideUpdatingWgts();
        progressHide();
        AIotAppSdkFactory.getInstance().getDeviceMgr().registerListener(this);

        if (mMcuVersionInfo.mIsupgradable && (mMcuVersionInfo.mUpgradeId > 0)) {
            // 有新版本要升级，显示新版本可升级对话框
            popupUpgradeVersionDlg();

        } else {
            // 无新版本升级，显示当前版本信息
            popupLastVersionDlg(mMcuVersionInfo.mCurrVersion);
        }

    }

    @Override
    public void initListener() {
        getBinding().titleView.setLeftClick(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AIotAppSdkFactory.getInstance().getDeviceMgr().unregisterListener(this);

        if (mMsgHandler != null) {
            mMsgHandler.removeMessages(MSGID_UPGRADE_REQUEST);
            mMsgHandler.removeMessages(MSGID_UPGRADE_QUERY);
            mMsgHandler = null;
        }
    }


    /**
     * @brief 显示当前已经是最新版本
     */
    void popupLastVersionDlg(final String version) {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View upgradeView = inflater.inflate(R.layout.dialog_firmware_lastversion, null);
        final TextView tvTitle = (TextView) upgradeView.findViewById(R.id.tvFirmLastVerTitle);

        String newVersion = "当前已是最新版本，版本号: " + version;
        tvTitle.setText(newVersion);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("")
                .setView(upgradeView)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }});
        AlertDialog dlg = builder.show();
        dlg.setCanceledOnTouchOutside(false);
    }


    /**
     * @brief 弹出显示要升级版本
     */
    void popupUpgradeVersionDlg() {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View upgradeView = inflater.inflate(R.layout.dialog_firmware_upgrade, null);
        final TextView tvTitle = (TextView) upgradeView.findViewById(R.id.tvFirmUpgradeTitle);
        final TextView tvContent = (TextView) upgradeView.findViewById(R.id.tvFirmUpgradeContent);
        final Button btnCanel = (Button) upgradeView.findViewById(R.id.btnFirmUpgradeCancel);
        final Button btnUpgrade = (Button) upgradeView.findViewById(R.id.btnFirmUpgradeUpdate);

        String newVersion = "最新版本号: " + mMcuVersionInfo.mUpgradeVersion;
        tvTitle.setText(newVersion);

        String remark = "";
        if (!TextUtils.isEmpty(mMcuVersionInfo.mRemark)) {
            remark = mMcuVersionInfo.mRemark;
        }
        String newContent = "更新内容: \n" + remark;
        tvContent.setText(newContent);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("")
                .setView(upgradeView);
        AlertDialog dlg = builder.show();
        dlg.setCanceledOnTouchOutside(false);

        btnCanel.setOnClickListener(view -> {
            dlg.dismiss();
            finish();
        });

        btnUpgrade.setOnClickListener(view -> {
            dlg.dismiss();
            if (mMsgHandler != null) {
                mMsgHandler.sendEmptyMessage(MSGID_UPGRADE_REQUEST);
            }
        });

    }

    /**
     * @brief 固件升级请求
     */
    void onMsgUpgradeRequest() {
        showUpdatingWgts();
        progressShow();

        IotDevice iotDevice = AgoraApplication.getInstance().getLivingDevice();

        int errCode = AIotAppSdkFactory.getInstance().getDeviceMgr().upgradeMcuVersion(
                iotDevice,   mMcuVersionInfo.mUpgradeId, 2);
        if (errCode != ErrCode.XOK) {
            popupMessage("固件版本升级失败，错误码=" + errCode);
            progressHide();
            finish();
            return;
        }
    }

    @Override
    public void  onUpgradeMcuVerDone(int errCode, final IotDevice iotDevice,
                                     long upgradeId, int decide) {
        Log.d(TAG, "<onUpgradeMcuVerDone> errCode=" + errCode + ", upgradeId=" + upgradeId);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (errCode == ErrCode.XERR_DEVMGR_UPGRADE_INVALID) {
                    progressHide();
                    popupMessage("设备升级信息不存在或无效");
                    finish();

                } else if ((errCode != ErrCode.XOK) && (errCode != ErrCode.XERR_DEVMGR_NOT_ALLOW)) {
                    progressHide();
                    popupMessage("设备: " + iotDevice.mDeviceID + " 固件版本升级失败，错误码=" + errCode);
                    finish();

                } else {
                    // 开始定时轮询设备更新状态
                    mQueryCount = 0;
                    if (mMsgHandler != null) {
                        mMsgHandler.sendEmptyMessageDelayed(MSGID_UPGRADE_QUERY, QUERY_INTERVAL);
                    }
                }
            }
        });
    }


    /**
     * @brief 固件升级状态轮询
     */
    void onMsgUpgradeQuery() {
        IotDevice iotDevice = AgoraApplication.getInstance().getLivingDevice();
        int errCode = AIotAppSdkFactory.getInstance().getDeviceMgr().getMcuUpgradeStatus(
                iotDevice,   mMcuVersionInfo.mUpgradeId);
        mQueryCount++;

        if (errCode != ErrCode.XOK) {
            Log.e(TAG, "<onMsgUpgradeQuery> errCode=" + errCode);
            mMsgHandler.sendEmptyMessageDelayed(MSGID_UPGRADE_QUERY, QUERY_INTERVAL);
        }
    }

    @Override
    public void onUpgradeStatusDone(int errCode, final IotDevice device,
                                     final IDeviceMgr.McuUpgradeStatus status) {
        Log.d(TAG, "<onUpgradeStatusDone> errCode=" + errCode + ", status=" + status.toString());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (errCode != ErrCode.XOK) {  // 升级状态查询失败，继续轮询或者结束
                    if (mQueryCount >= MAX_QUERY_COUNT) {
                        progressHide();
                        popupMessage("设备升级超时!");
                        Log.e(TAG, "<onUpgradeStatusDone> upgrade timeout");
                        finish();
                    } else {
                        mMsgHandler.sendEmptyMessageDelayed(MSGID_UPGRADE_QUERY, QUERY_INTERVAL);
                    }
                    return;
                }


                if (status.mStatus == UPGRADE_STATUS_READY || status.mStatus == UPGRADE_STATUS_ONGOING) {
                    // 正在升级中，继续轮询
                    if (mQueryCount >= MAX_QUERY_COUNT) {
                        progressHide();
                        popupMessage("设备升级超时!");
                        Log.e(TAG, "<onUpgradeStatusDone> upgrade timeout");
                        finish();
                    } else {
                        mMsgHandler.sendEmptyMessageDelayed(MSGID_UPGRADE_QUERY, QUERY_INTERVAL);
                    }

                } else if (status.mStatus == UPGRADE_STATUS_DONE) {
                    // 升级成功
                    progressHide();
                    popupMessage("设备升级成功!");
                    finish();

                } else if (status.mStatus == UPGRADE_STATUS_CANCEL) {
                    // 升级成功
                    progressHide();
                    popupMessage("设备升级被取消!");
                    finish();

                } else {
                    // 升级失败
                    progressHide();
                    popupMessage("设备升级失败!");
                    finish();
                }
            }
        });
    }


    /**
     * @brief 隐藏更新进度组件
     */
    void hideUpdatingWgts() {
        getBinding().tvUpdateTitle.setVisibility(View.INVISIBLE);
        getBinding().tvUpdateContent.setVisibility(View.INVISIBLE);
        getBinding().tvTipsUpdating.setVisibility(View.INVISIBLE);
        getBinding().tvTipsImportant.setVisibility(View.INVISIBLE);
        //getBinding().pbUpdateProgress.setVisibility(View.INVISIBLE);
    }

    /**
     * @brief 显示更新进度组件
     */
    void showUpdatingWgts() {
        String newVersion = "最新版本号: " + mMcuVersionInfo.mUpgradeVersion;
        getBinding().tvUpdateTitle.setText(newVersion);

        String remark = "";
        if (!TextUtils.isEmpty(mMcuVersionInfo.mRemark)) {
            remark = mMcuVersionInfo.mRemark;
        }
        String newContent = "更新内容: \n" + remark;
        getBinding().tvUpdateContent.setText(newContent);

        getBinding().tvUpdateTitle.setVisibility(View.VISIBLE);
        getBinding().tvUpdateContent.setVisibility(View.VISIBLE);
        getBinding().tvTipsUpdating.setVisibility(View.VISIBLE);
        getBinding().tvTipsImportant.setVisibility(View.VISIBLE);
        //getBinding().pbUpdateProgress.setVisibility(View.VISIBLE);
    }


    protected void progressShow() {
        getBinding().pbUpdateProgress.setVisibility(View.VISIBLE);
    }

    protected void progressHide() {
        getBinding().pbUpdateProgress.setVisibility(View.INVISIBLE);
    }
}
