package io.agora.iotlinkdemo.deviceconfig;

import static android.content.Context.LOCATION_SERVICE;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationManagerCompat;

import com.agora.baselibrary.utils.SPUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IAccountMgr;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.deviceconfig.espressif.BlufiCallback;
import io.agora.iotlinkdemo.deviceconfig.espressif.BlufiClient;
import io.agora.iotlinkdemo.deviceconfig.espressif.BlufiConstants;
import io.agora.iotlinkdemo.deviceconfig.espressif.params.BlufiConfigureParams;
import io.agora.iotlinkdemo.deviceconfig.espressif.params.BlufiParameter;
import io.agora.iotlinkdemo.deviceconfig.espressif.response.BlufiScanResult;
import io.agora.iotlinkdemo.deviceconfig.espressif.response.BlufiStatusResponse;
import io.agora.iotlinkdemo.deviceconfig.espressif.response.BlufiVersionResponse;
import io.agora.iotlinkdemo.thirdpartyaccount.ThirdAccountMgr;


public class DeviceBtCfg {

    //
    // error code
    //
    public static final int XERR_CFG_OK = 0;
    public static final int XERR_CFG_DISCONNECT = -110001;      ///< 设备链接断开
    public static final int XERR_CFG_DEVOPT = -110002;          ///< 设备操作错误
    public static final int XERR_CFG_NETWORK = -110003;         ///< 设备配网失败
    public static final int XERR_CFG_CUSTOMDATA = -110004;      ///< 设备设置用户数据失败


    //
    // 配置状态机，CFG_STATUS_DONE 状态下调用deviceCfgStop()后切换成 CFG_STATUS_IDLE
    //
    public static final int CFG_STATUS_IDLE = 0x0000;           ///< 空闲未配置状态
    public static final int CFG_STATUS_PREPARING = 0x0001;      ///< 配置准备中
    public static final int CFG_STATUS_CONNECTED = 0x0002;      ///< 设备已经连接
    public static final int CFG_STATUS_NETWORK = 0x0003;        ///< WIFI已经配置
    public static final int CFG_STATUS_CUSTOMDATA = 0x0004;     ///< 用户数据已经配置
    public static final int CFG_STATUS_DONE = 0x0005;           ///< 当前配置完成


    /**
     * @brief 蓝牙配网的回调接口
     */
    public interface IBtCfgCallback {

        /**
         * @brief 蓝牙扫描进度回调事件
         * @param devices : 当前扫描到的蓝牙设备
         */
        default void onScanProgress(List<android.bluetooth.le.ScanResult> devices) { }

        /**
         * @brief 蓝牙扫描结束
         * @param devices : 当前扫描到的蓝牙设备
         */
        default void onScanDone(List<android.bluetooth.le.ScanResult> devices) { }

        /**
         * @brief 蓝牙扫描错误
         */
        default void onScanError(int errCode) { }

        /**
         * @brief 设备配网状态变化
         * @param cfgStatus : 新的配置状态
         */
        default void onConfigProgress(int cfgStatus) { }

        /**
         * @brief 设备配网完成
         * @param errCode : 错误码，0表示配网成功，否则表示配网失败
         */
        default void onConfigDone(int errCode) { }

    }


    /**
     * @brief 蓝牙配网的参数
     */
    public static class BtCfgParam {
        public String mSsid;
        public String mPassword;
        public String mProductId;
        public String mUserId;
    }


    ////////////////////////////////////////////////////////////////////
    ///////////////////////////// Constant /////////////////////////////
    ////////////////////////////////////////////////////////////////////
    private static final String TAG = "LINK/DeviceBtCfg";


    public static final String DEV_PREFIX_ESP32 = "BLUFI";
    public static final String DEV_PREFIX_BL808 = "BL808";
    private static final long SCAN_TIMEOUT = 60000L;


    //////////////////////////////////////////////////////////////////////////////
    ///////////////////////////// Variable Definition /////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////
    private static DeviceBtCfg mInstance = null;
    private static final Object mDataLock = new Object();       ///< 同步访问锁,类中相关变量需要进行加锁处理
    private ArrayList<IBtCfgCallback> mCallbackList = new ArrayList<>();
    private final Handler mUIHandler;
    private ExecutorService mScanThreadPool;        ///< 扫描线程池
    private Future<Boolean> mScanFuture;            ///< 扫描任务
    private BtScanCallback mScanCallback;           ///< 蓝牙扫描回调
    private Map<String, ScanResult> mScanDevMap = new HashMap<>();    ///< 扫描到的设备
    private long mScanStartTime;                    ///< 扫描开始时间戳


    private BlufiClient mBlufiClient;               ///< 蓝牙配网客户端
    private BluetoothDevice mCfgingDevice;          ///< 正在配网的蓝牙设备
    private BtCfgParam mCfgingParam;                ///< 当前蓝牙配网参数
    private volatile int mCfgStatus = CFG_STATUS_IDLE;  ///< 当前配置状态


    ////////////////////////////////////////////////////////////////////
    //////////////////////// Public Methods ////////////////////////////
    ///////////////////////////////////////////////////////////////////
    public static DeviceBtCfg getInstance() {
        if (mInstance == null) {
            synchronized (DeviceBtCfg.class) {
                if (mInstance == null) {
                    mInstance = new DeviceBtCfg();
                }
            }
        }
        return mInstance;
    }

    private DeviceBtCfg() {
        mUIHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * @brief 检测蓝牙设备是否已经就绪
     */
    public boolean isBtDeviceReady(Context context) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "<isBtDeviceReady> bluetooth is disabled!");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check location enable
            LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            boolean locationEnable = locationManager != null && LocationManagerCompat.isLocationEnabled(locationManager);
            if (!locationEnable) {
                Log.e(TAG, "<isBtDeviceReady> location is disabled!");
                return false;
            }
        }

        Log.d(TAG, "<isBtDeviceReady> READY");
        return true;
    }


    public int registerListener(DeviceBtCfg.IBtCfgCallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.add(callback);
        }
        return ErrCode.XOK;
    }

    public int unregisterListener(DeviceBtCfg.IBtCfgCallback callback) {
        synchronized (mCallbackList) {
            mCallbackList.remove(callback);
        }
        return ErrCode.XOK;
    }


    ////////////////////////////////////////////////////////////////////////////////
    //////////////////////// Methods for Device Scanning ////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 获取当前扫描到的设备数量
     */
    public int getScannedDevCount() {
        synchronized (mScanDevMap) {
            return mScanDevMap.size();
        }
    }

    /**
     * @brief 获取当前扫描到的设备列表
     */
    public List<android.bluetooth.le.ScanResult> getScannedDevices() {
        List<android.bluetooth.le.ScanResult> devices;
        synchronized (mScanDevMap) {
            devices = new ArrayList<>(mScanDevMap.values());
        }
        Collections.sort(devices, (dev1, dev2) -> {
            Integer rssi1 = dev1.getRssi();
            Integer rssi2 = dev2.getRssi();
            return rssi2.compareTo(rssi1);
        });

        return devices;
    }

    /**
     * @brief 开始蓝牙设备扫描，前提确保有蓝牙权限并且蓝牙已经打开
     */
    public boolean scanStart() {
        if (mScanFuture != null) {
            Log.d(TAG, "<scanStart> already in scanning");
            return true;
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothLeScanner btScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (btScanner == null) {
            Log.e(TAG, "<scanStart> BluetoothLeScanner is NULL");
            return false;
        }

        // 清除旧记录
        synchronized (mScanDevMap) {
            mScanDevMap.clear();
        }

        // 启动蓝牙设备扫描
        try {
            ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            mScanCallback = new BtScanCallback();
            btScanner.startScan(null, scanSettings, mScanCallback);
        } catch (SecurityException securityExp) {
            securityExp.printStackTrace();
            Log.e(TAG, "<scanStart> securityExp=" + securityExp.toString());
            return false;
        }

        // 启动检测线程
        mScanThreadPool = Executors.newSingleThreadExecutor();
        mScanStartTime = SystemClock.elapsedRealtime();
        mScanFuture = mScanThreadPool.submit(() -> {

            int loop = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException interruptExp) {
                    loop = 100;
                    interruptExp.printStackTrace();
                    break;
                }
                loop++;
                if (loop < 10) {
                    continue;
                }
                loop = 0;

                long scanCost = SystemClock.elapsedRealtime() - mScanStartTime;
                if (scanCost > SCAN_TIMEOUT) {
                    Log.d(TAG, "<scanStart.Thread> scanning timeout");
                    break;
                }

                onScanIntervalUpdate(false);
            }

            BluetoothLeScanner scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
            if (scanner != null) {
                try {
                    scanner.stopScan(mScanCallback);
                }  catch (SecurityException stopSecurityExp) {
                    stopSecurityExp.printStackTrace();
                }
            }

            Log.d(TAG, "<scanStart.Thread> Scan thread is interrupted");
            onScanIntervalUpdate(true);
            return true;
        });

        Log.d(TAG, "<scanStart> done");
        return true;
    }

    /**
     * @brief 停止蓝牙设备扫描，前提确保有蓝牙权限并且蓝牙已经打开
     */
    public void scanStop()  {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothLeScanner btScanner = btAdapter.getBluetoothLeScanner();
        if (btScanner != null) {
            try {
                btScanner.stopScan(mScanCallback);
            }  catch (SecurityException stopSecurityExp) {
                stopSecurityExp.printStackTrace();
            }
        }
        if (mScanFuture != null) {
            mScanFuture.cancel(true);
            mScanFuture = null;
        }
        if (mScanThreadPool != null) {
            mScanThreadPool.shutdownNow();
        }

        Log.d(TAG, "<scanStop> done");
    }

    /**
     * @brief 判断当前是否正在蓝牙扫描
     */
    public boolean isScanning() {
        return (mScanFuture != null);
    }

    /**
     * @brief 蓝牙设备扫描过程中，定时1秒刷新一次状态
     * @param bScanDone : 扫描任务是否结束, true表示扫描结束时候的更新
     */
    private void onScanIntervalUpdate(boolean bScanDone) {
        List<android.bluetooth.le.ScanResult> devices;
        synchronized (mScanDevMap) {
            devices = new ArrayList<>(mScanDevMap.values());
        }
        Collections.sort(devices, (dev1, dev2) -> {
            Integer rssi1 = dev1.getRssi();
            Integer rssi2 = dev2.getRssi();
            return rssi2.compareTo(rssi1);
        });

        synchronized (mCallbackList) {
            for (IBtCfgCallback listener : mCallbackList) {
                if (bScanDone) {
                    listener.onScanDone(devices);
                } else {
                    listener.onScanProgress(devices);
                }
            }
        }

    }

    /**
     * @brief 蓝牙扫描回调处理
     */
    private class BtScanCallback extends android.bluetooth.le.ScanCallback {
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "<onScanFailed> errCode=" + errorCode);
            synchronized (mCallbackList) {
                for (IBtCfgCallback listener : mCallbackList) {
                    listener.onScanError(errorCode);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<android.bluetooth.le.ScanResult> results) {
            for (ScanResult result : results) {
                onLeScan(result);
            }
        }

        @Override
        public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
            onLeScan(result);
        }

        private void onLeScan(android.bluetooth.le.ScanResult scanResult) {
            try {
                String name = scanResult.getDevice().getName();
                if (TextUtils.isEmpty(name)) {
                    return;
                }

                if (name.startsWith(DEV_PREFIX_ESP32)) {
                    synchronized (mScanDevMap) {
                        mScanDevMap.put(scanResult.getDevice().getAddress(), scanResult);
                    }

                } else if (name.startsWith(DEV_PREFIX_BL808)) {
                    synchronized (mScanDevMap) {
                        mScanDevMap.put(scanResult.getDevice().getAddress(), scanResult);
                    }

                } else {
                    Log.d(TAG, "<BtScanCallback.onLeScan> filter device=" + name);
                }

            } catch (SecurityException securityExp) {
                securityExp.printStackTrace();
                Log.e(TAG, "<BtScanCallback.onLeScan> securityExp=" + securityExp.toString());
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////////
    //////////////////////// Methods for Device Network Config ///////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 开始蓝牙设备配网
     */
    public boolean deviceCfgStart(BluetoothDevice cfgDevice, final BtCfgParam cfgParam) {
        mCfgingDevice = cfgDevice;
        mCfgingParam = cfgParam;

        setDeviceCfgStatus(CFG_STATUS_PREPARING);
        deviceCfgProgressCallback(CFG_STATUS_PREPARING);

        mBlufiClient = new BlufiClient(null, mCfgingDevice);
        mBlufiClient.setGattCallback(new GattCallback());
        mBlufiClient.setBlufiCallback(new BlufiCallbackMain());
        mBlufiClient.setGattWriteTimeout(BlufiConstants.GATT_WRITE_TIMEOUT);
        mBlufiClient.connect();

        Log.d(TAG, "<deviceCfgStart> done");
        return true;
    }

    /**
     * @brief 停止蓝牙设备配网
     */
    public void deviceCfgStop()  {
        if (mBlufiClient != null) {
            mBlufiClient.close();
            mBlufiClient = null;
            Log.d(TAG, "<deviceCfgStop> done");
        }
        setDeviceCfgStatus(CFG_STATUS_IDLE);
        //deviceCfgProgressCallback(CFG_STATUS_IDLE);
    }

    /**
     * @brief 获取当前配置状态
     */
    public int getDeviceCfgStatus()  {
        synchronized (mDataLock) {
            return mCfgStatus;
        }
    }

    /**
     * @brief 设置当前配置状态
     */
    private void setDeviceCfgStatus(int newStatus)  {
        synchronized (mDataLock) {
            mCfgStatus = newStatus;
        }
    }


    /**
     * @brief 蓝牙Gatt的回调
     */
    private class GattCallback extends BluetoothGattCallback {
        @Override
        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String devName = gatt.getDevice().getName();
            Log.d(TAG, "<onConnectionStateChange> devName=" + devName
                        + ", status=" + status +", newState=" + newState);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED: {
                        Log.d(TAG, "<onConnectionStateChange> CONNECTED");
                        onGattConnected();
                    } break;

                    case BluetoothProfile.STATE_DISCONNECTED: {
                        Log.d(TAG, "<onConnectionStateChange> DISCONNECTED");
                        gatt.close();
                        int cfgStatus = getDeviceCfgStatus();
                        if (cfgStatus == CFG_STATUS_CONNECTED || cfgStatus == CFG_STATUS_NETWORK) {
                            Log.d(TAG, "<onConnectionStateChange> BT disconnected");
                            onGattDisconnected();
                        }
                    } break;
                }

            } else {
                gatt.close();
                onGattDisconnected();
            }
        }

        @Override
        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            String devName = gatt.getDevice().getName();
            Log.d(TAG, "<onMtuChanged> devName=" + devName
                    + ", mtu=" + mtu +", status=" + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {

            } else {
                mBlufiClient.setPostPackageLengthLimit(20);
            }

            onGattServiceCharacteristicDiscovered();
        }

        @Override
        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            String devName = gatt.getDevice().getName();
            Log.d(TAG, "<onServicesDiscovered> devName=" + devName + ", status=" + status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "<onServicesDiscovered> failure");
                gatt.disconnect();
             }
        }

        @Override
        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            String devName = gatt.getDevice().getName();
            Log.d(TAG, "<onDescriptorWrite> devName=" + devName + ", status=" + status);

            if (descriptor.getUuid().equals(BlufiParameter.UUID_NOTIFICATION_DESCRIPTOR) &&
                    descriptor.getCharacteristic().getUuid().equals(BlufiParameter.UUID_NOTIFICATION_CHARACTERISTIC)) {
                String msg = String.format(Locale.ENGLISH, "Set notification enable %s", (status == BluetoothGatt.GATT_SUCCESS ? " complete" : " failed"));
                Log.d(TAG, "<onDescriptorWrite> " + msg);
            }
        }

        @Override
        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String devName = gatt.getDevice().getName();
            Log.d(TAG, "<onCharacteristicWrite> devName=" + devName + ", status=" + status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.disconnect();
            }
        }
    }


    /**
     * @brief 封装的Blufi接口回调
     */
    private class BlufiCallbackMain extends BlufiCallback {
        @Override
        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        public void onGattPrepared(
                BlufiClient client,
                BluetoothGatt gatt,
                BluetoothGattService service,
                BluetoothGattCharacteristic writeChar,
                BluetoothGattCharacteristic notifyChar   ) {
            String devName = gatt.getDevice().getName();

            if (service == null) {
                Log.d(TAG, "<onGattPrepared> devName=" + devName + ", service NOT found");
                gatt.disconnect();
                return;
            }

            if (writeChar == null) {
                Log.d(TAG, "<onGattPrepared> devName=" + devName + ", Get write characteristic failed");
                gatt.disconnect();
                return;
            }

            if (notifyChar == null) {
                Log.d(TAG, "<onGattPrepared> devName=" + devName + ", Get notification characteristic failed");
                gatt.disconnect();
                return;
            }

            int mtu = BlufiConstants.DEFAULT_MTU_LENGTH;
            Log.d(TAG, "<onGattPrepared> devName=" + devName + ", requestMtu=" + mtu);
            boolean requestMtu = gatt.requestMtu(mtu);
            if (!requestMtu) {
                Log.d(TAG, "<onGattPrepared> devName=" + devName + ", fail to requestMtu()");
                onGattServiceCharacteristicDiscovered();
            }
        }

        @Override
        public void onNegotiateSecurityResult(BlufiClient client, int status) {
            if (status == STATUS_SUCCESS) {
                Log.d(TAG, "<onNegotiateSecurityResult> success");
            } else {
                Log.d(TAG, "<onNegotiateSecurityResult> failed, status=" + status);
            }
        }

        @Override
        public void onPostConfigureParams(BlufiClient client, int status) {
            if (status == STATUS_SUCCESS) {
                Log.d(TAG, "<onPostConfigureParams> success");
            } else {
                Log.d(TAG, "<onPostConfigureParams> failed, status=" + status);
            }
            onGattPostConfigureParams(status);
        }

        @Override
        public void onDeviceStatusResponse(BlufiClient client, int status,
                                           BlufiStatusResponse response) {
            if (status == STATUS_SUCCESS) {
                Log.d(TAG, "<onDeviceStatusResponse> success, response=" + response.generateValidInfo());
            } else {
                Log.d(TAG, "<onDeviceStatusResponse> failed, status=" + status);
            }
        }

        @Override
        public void onDeviceScanResult(BlufiClient client, int status, List<BlufiScanResult> results) {
            if (status == STATUS_SUCCESS) {
                StringBuilder msg = new StringBuilder();
                msg.append("Device scan result:\n");
                for (BlufiScanResult scanResult : results) {
                    msg.append(scanResult.toString()).append("\n");
                }
                Log.d(TAG, "<onDeviceScanResult> success, msg=" + msg);

            } else {
                Log.d(TAG, "<onDeviceScanResult> failed, status=" + status);
            }
        }

        @Override
        public void onDeviceVersionResponse(BlufiClient client, int status, BlufiVersionResponse response) {
            if (status == STATUS_SUCCESS) {
                Log.d(TAG, "<onDeviceVersionResponse> success, response.version=" + response.getVersionString());
            } else {
                Log.d(TAG, "<onDeviceVersionResponse> failed, status=" + status);
            }
        }

        @Override
        public void onPostCustomDataResult(BlufiClient client, int status, byte[] data) {
            if (status == STATUS_SUCCESS) {
                Log.d(TAG, "<onPostCustomDataResult> success, data=" + new String(data));
            } else {
                Log.d(TAG, "<onPostCustomDataResult> failed, status=" + status);
            }
            onGattPostCustomData(status);
        }

        @Override
        public void onReceiveCustomData(BlufiClient client, int status, byte[] data) {
            if (status == STATUS_SUCCESS) {
                Log.d(TAG, "<onReceiveCustomData> success, data=" + new String(data));
            } else {
                Log.d(TAG, "<onReceiveCustomData> failed, status=" + status);
            }
        }

        @Override
        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        public void onError(BlufiClient client, int errCode) {
            Log.d(TAG, "<onError> errCode=" + errCode);

            if (errCode == CODE_GATT_WRITE_TIMEOUT) {
                Log.d(TAG, "<onError> Gatt write timeout!");
                client.close();
                onGattError(errCode);
            }
        }
    }


    void onGattConnected() {
        setDeviceCfgStatus(CFG_STATUS_CONNECTED);
        deviceCfgProgressCallback(CFG_STATUS_CONNECTED);
    }

    void onGattDisconnected() {
        mUIHandler.post(() -> {
            setDeviceCfgStatus(CFG_STATUS_DONE);
            deviceCfgProgressCallback(CFG_STATUS_DONE);
            if (mBlufiClient != null) {
                mBlufiClient.close();
                mBlufiClient = null;
            }
            deviceCfgDoneCallback(XERR_CFG_DISCONNECT);
        });
    }

    void onGattError(int errCode) {
        mUIHandler.post(() -> {
            setDeviceCfgStatus(CFG_STATUS_DONE);
            deviceCfgProgressCallback(CFG_STATUS_DONE);
            if (mBlufiClient != null) {
                mBlufiClient.close();
                mBlufiClient = null;
            }
            deviceCfgDoneCallback(XERR_CFG_DEVOPT+errCode);
        });
    }

    /**
     * @brief 查询到服务和特征描述, 进行配网操作
     */
    void onGattServiceCharacteristicDiscovered() {
        mUIHandler.post(() -> {
            BlufiConfigureParams params = new BlufiConfigureParams();

            // 设置需要配置的模式：1 为 Station 模式，2 为 SoftAP 模式，3 为 Station 和 SoftAP 共存模式
            params.setOpMode(BlufiParameter.OP_MODE_STA);

            // 设置 Station 配置信息
            params.setStaBSSID(mCfgingParam.mSsid); // 设置 Wi-Fi SSID
            params.setStaSSIDBytes(mCfgingParam.mSsid.getBytes());
            params.setStaPassword(mCfgingParam.mPassword); // 设置 Wi-Fi 密码，若是开放 Wi-Fi 则不设或设空
            // 注意：Device 不支持连接 5G Wi-Fi，建议提前检查一下是不是 5G Wi-Fi

            mBlufiClient.configure(params);
            Log.d(TAG, "<onGattServiceCharacteristicDiscovered> ssid=" + mCfgingParam.mSsid
                    + ", password=" + mCfgingParam.mPassword);
        });

        Log.d(TAG, "<onGattServiceCharacteristicDiscovered> done");
    }

    /**
     * @brief 配网完成回调
     * @param status : 0表示配网成功； 其他表示配网失败
     */
    void onGattPostConfigureParams(int status) {
        if (status != BlufiCallback.STATUS_SUCCESS) {
            Log.d(TAG, "<onGattPostConfigureParams> failed with CFG_NETWORK");
            mUIHandler.post(() -> {
                setDeviceCfgStatus(CFG_STATUS_DONE);
                deviceCfgProgressCallback(CFG_STATUS_DONE);
                if (mBlufiClient != null) {
                    mBlufiClient.close();
                    mBlufiClient = null;
                }
                deviceCfgDoneCallback(XERR_CFG_NETWORK);
            });
            return;
        }

        // 在用户线程发送用户数据
        mUIHandler.post(() -> {
            setDeviceCfgStatus(CFG_STATUS_NETWORK);
            deviceCfgProgressCallback(CFG_STATUS_NETWORK);

            //构建二维码内容，json格式
            JSONObject body = new JSONObject();
            try {
                body.put("s", mCfgingParam.mSsid);
                body.put("p", mCfgingParam.mPassword);
                body.put("u", mCfgingParam.mUserId);
                body.put("k", mCfgingParam.mProductId);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d(TAG, "<onGattPostConfigureParams> fail to generate custom data");
                setDeviceCfgStatus(CFG_STATUS_DONE);
                deviceCfgProgressCallback(CFG_STATUS_DONE);
                if (mBlufiClient != null) {
                    mBlufiClient.close();
                    mBlufiClient = null;
                }
                deviceCfgDoneCallback(XERR_CFG_CUSTOMDATA);
                return;
            }
            String qrCodeContent = String.valueOf(body);
            Log.d(TAG, "<onGattPostConfigureParams> qrCodeContent=" + qrCodeContent);
            byte[] customData = qrCodeContent.getBytes(StandardCharsets.UTF_8);
            mBlufiClient.postCustomData(customData);
        });
        Log.d(TAG, "<onGattPostConfigureParams> done");
    }

    /**
     * @brief 用户数据设置完成回调
     * @param status : 0表示用户数据设置成功； 其他表示用户数据设置失败
     */
    void onGattPostCustomData(int status) {
        Log.d(TAG, "<onGattPostCustomData> status=" + status);
        int errCode = (status == BlufiCallback.STATUS_SUCCESS) ? XERR_CFG_OK : XERR_CFG_CUSTOMDATA;

        if (errCode == XERR_CFG_OK) {
            setDeviceCfgStatus(CFG_STATUS_CUSTOMDATA);
            deviceCfgProgressCallback(CFG_STATUS_CUSTOMDATA);
        }

        mUIHandler.postDelayed(() -> {
            setDeviceCfgStatus(CFG_STATUS_DONE);
            deviceCfgProgressCallback(CFG_STATUS_DONE);
            if (mBlufiClient != null) {
                mBlufiClient.close();
                mBlufiClient = null;
            }
            deviceCfgDoneCallback(errCode);
        }, 2000L);
    }


    /**
     * @brief 回调设备蓝牙配置结束
     */
    void deviceCfgProgressCallback(int cfgStatus) {
        synchronized (mCallbackList) {
            for (IBtCfgCallback listener : mCallbackList) {
                listener.onConfigProgress(cfgStatus);
            }
        }
    }


    /**
     * @brief 回调设备蓝牙配置结束
     */
    void deviceCfgDoneCallback(int errCode) {
        synchronized (mCallbackList) {
            for (IBtCfgCallback listener : mCallbackList) {
                listener.onConfigDone(errCode);
            }
        }
    }


 }
