package io.agora.iotlinkdemo.base;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewbinding.ViewBinding;

import com.agora.baselibrary.base.BaseBindingActivity;
import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.dialog.CommonDialog;
import io.agora.iotlink.IAccountMgr;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import kotlin.jvm.internal.Intrinsics;

/**
 * @brief 权限处理
 *
 */
public class PermissionHandler {

    public static interface ICallback {

        /**
         * @brief 所有权限申请完成回调
         */
        default void onAllPermisonReqDone(boolean allGranted, final PermissionItem[] permItems) {}
    }


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "LINK/PermHandler";
    public static final int FULL_REQ_INDEX = 9999;

    //
    // Permission ID
    //
    public static final int PERM_ID_RECORD_AUDIO = 0x1001;
    public static final int PERM_ID_CAMERA = 0x1002;
    public static final int PERM_ID_WIFISTATE = 0x1003;
    public static final int PERM_ID_FINELOCAL = 0x1004;
    public static final int PERM_ID_READ_STORAGE = 0x1005;


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private Activity mActivity;
    private ICallback mCallback;
    private PermissionItem[] mPermissionArray;
    private int mReqIndex = -1;




    ///////////////////////////////////////////////////////////////////////////
    //////////////////////// Methods of Permission ////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    /*
     * @brief 初始化操作
     * @param activity
     * @param permIdList : 需要申请的权限Id
     * @return
     */
    public PermissionHandler(Activity activity, ICallback callback, int[] permIdArray) {
        mActivity = activity;
        mCallback = callback;

        int permCount = permIdArray.length;
        mPermissionArray = new PermissionItem[permCount];
        for (int i = 0; i < permCount; i++)  {
            String permissionName = getPermNameById(permIdArray[i]);
            if ((permissionName == null) || (permissionName.isEmpty())) {
                continue;
            }
            mPermissionArray[i] = new PermissionItem(permissionName, permIdArray[i]);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // 所有权限全部赋值
            for (PermissionItem item : mPermissionArray) {
                item.granted = true;
            }

        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            for (PermissionItem item : mPermissionArray) {
                item.granted = (ContextCompat.checkSelfPermission(mActivity, item.permissionName) == PackageManager.PERMISSION_GRANTED);
            }

        } else {
            for (PermissionItem item : mPermissionArray) {
                item.granted = (ContextCompat.checkSelfPermission(mActivity, item.permissionName) == PackageManager.PERMISSION_GRANTED);
            }
        }
        mReqIndex = -1;
    }

    /**
     * @brief 判断是否所有相应权限都已经获取
     * @return true: 所有权限都已经获取； false: 有权限未获取
     */
    public boolean isAllPermissionGranted() {
        for (PermissionItem item : mPermissionArray) {
            if (!item.granted) {
                return false;
            }
        }
        return true;
    }

    /**
     * @brief 进行下一个需要的权限申请
     * @return 申请权限的索引, 9999表示所有权限都有了，不再需要申请
     */
    public int requestNextPermission() {

       for (;;) {  // 找到下一个需要申请的权限
           mReqIndex++;

           if (mReqIndex >= mPermissionArray.length) {
               return FULL_REQ_INDEX;
           }

            if (!mPermissionArray[mReqIndex].granted) {
               break;
            }
        }

       // 发送权限请求申请
        String permission = mPermissionArray[mReqIndex].permissionName;
        int requestCode = mPermissionArray[mReqIndex].requestId;
        ActivityCompat.requestPermissions(mActivity, new String[]{permission}, requestCode);

        return mReqIndex;
    }

    /**
     * @brief 权限返回结果处理，通常在 Activity.onRequestPermissionsResult()中调用
     *
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {

        Log.d(TAG, "<onRequestPermissionsResult> requestCode=" + requestCode);

        if (grantResults.length > 0) {
            boolean granted = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
            setPermGrantedByReqId(requestCode, granted);
            Log.d(TAG, "<onRequestPermissionsResult> granted");

        } else { // 拒绝了该权限
            Log.d(TAG, "<onRequestPermissionsResult> denied");
        }

        // 检测是否要动态申请相应的权限
        int reqIndex = requestNextPermission();
        if (reqIndex >= FULL_REQ_INDEX) {
            boolean isCallPermGranted = isAllPermissionGranted();
            Log.d(TAG, "<onRequestPermissionsResult> requesting done"
                    + ", isCallPermGranted=" + isCallPermGranted);
            mCallback.onAllPermisonReqDone(isCallPermGranted, mPermissionArray);
            return;
        }
    }


    /*
     * @brief 根据权限Id来获取权限的名字
     *
     */
    private String getPermNameById(int permId) {
        switch (permId) {
            case PERM_ID_RECORD_AUDIO:
                return Manifest.permission.RECORD_AUDIO;

            case PERM_ID_CAMERA:
                return Manifest.permission.CAMERA;

            case PERM_ID_WIFISTATE:
                return Manifest.permission.ACCESS_WIFI_STATE;

            case PERM_ID_FINELOCAL:
                return Manifest.permission.ACCESS_FINE_LOCATION;

            case PERM_ID_READ_STORAGE:
                return Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        return "";
    }


    /*
     * @brief 根据requestId 标记相应的权限值
     * @param reqId :  request Id
     * @return 相应的索引, -1表示没有找到 request Id 对应的项
     */
    private int setPermGrantedByReqId(int reqId, boolean granted) {
        for (int i = 0; i < mPermissionArray.length; i++) {
            if (mPermissionArray[i].requestId == reqId) {
                mPermissionArray[i].granted = granted;
                return i;
            }
        }

        return -1;
    }



}
