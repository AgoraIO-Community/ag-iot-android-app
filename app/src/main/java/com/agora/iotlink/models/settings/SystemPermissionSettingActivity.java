package com.agora.iotlink.models.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;

import com.agora.iotlink.R;
import com.agora.iotlink.base.BaseViewBindingActivity;
import com.agora.iotlink.databinding.ActivitySystemPermissionSettingBinding;
import com.agora.iotlink.manager.PagePathConstant;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 系统权限设置
 */
@Route(path = PagePathConstant.pageSystemPermissionSetting)
public class SystemPermissionSettingActivity extends BaseViewBindingActivity<ActivitySystemPermissionSettingBinding> {

    @Override
    protected ActivitySystemPermissionSettingBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivitySystemPermissionSettingBinding.inflate(inflater);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void checkAuthorized() {
        NotificationManagerCompat notification = NotificationManagerCompat.from(this);
        boolean isEnabled = notification.areNotificationsEnabled();
        if (isEnabled) {
            getBinding().tvHasAuthorized1.setText(getString(R.string.has_authorized));
        } else {
            getBinding().tvHasAuthorized1.setText(getString(R.string.no_authorized));
        }
        PowerManager power = getSystemService(PowerManager.class);
        if (power.isIgnoringBatteryOptimizations(getPackageName())) {
            getBinding().tvHasAuthorized2.setText(getString(R.string.has_authorized));
        } else {
            getBinding().tvHasAuthorized2.setText(getString(R.string.no_authorized));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        checkAuthorized();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void initListener() {
        getBinding().notificationReminderLayout.setOnClickListener(view -> openNotificationSettingsForApp(SystemPermissionSettingActivity.this));
        getBinding().allowBackgroundOperationLayout.setOnClickListener(view -> openRequestIgnoreBatteryOptimizationsForApp(SystemPermissionSettingActivity.this));
    }

    /**
     * 打开通知权限
     */
    public static void openNotificationSettingsForApp(Context context) {
        // Links to this app's notification settings.
        Intent intent = new Intent();
        intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
        intent.putExtra("app_package", context.getPackageName());
        intent.putExtra("app_uid", context.getApplicationInfo().uid);
        // for Android 8 and above
        intent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());
        context.startActivity(intent);
    }

    /**
     * 打开通知权限
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void openRequestIgnoreBatteryOptimizationsForApp(Context context) {
        Intent intent = new Intent();
        String packageName = context.getPackageName();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm.isIgnoringBatteryOptimizations(packageName))
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        else {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
        }
        context.startActivity(intent);
    }
}
