package com.agora.iotlink.huanxin;

//
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.media.AudioManager;
//import android.media.Ringtone;
//import android.media.RingtoneManager;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Vibrator;
//import android.text.TextUtils;
//
//import androidx.core.app.NotificationCompat;
//
//import com.hyphenate.util.EMLog;
//import com.hyphenate.util.EasyUtils;
//
///**
// * 离线或者APP在后台时的消息通知
// * 在Android 8.0之前的设备上:
// * 通知栏通知的声音和震动可以被demo设置中的'声音'和'震动'开关控制
// * 在Android 8.0设备上:
// * 通知栏通知的声音和震动不受demo设置中的'声音'和'震动'开关控制
// */
//public class EaseNotifier {
//    private final static String TAG = "LINK/EaseNotifier";
//
//    private int NOTIFY_ID = 0505;
//    private String CHANNEL_ID = "high_system";
//    private NotificationManager notificationManager = null;
//
//    private static EaseNotifier instance;
//    private static final long[] VIBRATION_PATTERN = new long[]{0, 500, 200, 200, 500, 500, 200, 200};
//
//    private Context appContext;
//    private String packageName;
//    private Ringtone ringtone = null;
//    private AudioManager audioManager;
//    private Vibrator vibrator;
//
//    public static EaseNotifier getInstance() {
//        if(instance == null) {
//            synchronized (EaseNotifier.class) {
//                if(instance == null) {
//                    instance = new EaseNotifier();
//                }
//            }
//        }
//        return instance;
//    }
//
//    public void init(Context context, Bundle metaData) {
//        //获取消息通知配置
//        CHANNEL_ID = metaData.getString("EASE_NOTIFIER_CHANNEL_ID", "high_system");
//        NOTIFY_ID = metaData.getInt("EASE_NOTIFIER_NOTIFY_ID", 0505);
//        //初始化消息通知
//        appContext = context.getApplicationContext();
//        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        if (Build.VERSION.SDK_INT >= 26) {
//            // Create the notification channel for Android 8.0
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
//                    "呼叫提醒通知", NotificationManager.IMPORTANCE_HIGH);
//            channel.setSound(null, null);
//            channel.enableVibration(true);
//            channel.setVibrationPattern(VIBRATION_PATTERN);
//            notificationManager.createNotificationChannel(channel);
//        }
//        packageName = appContext.getApplicationInfo().packageName;
//        audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
//        vibrator = (Vibrator) appContext.getSystemService(Context.VIBRATOR_SERVICE);
//    }
//
//    /**
//     * this function can be override
//     */
//    public void reset() {
//        if (notificationManager != null) {
//            notificationManager.cancel(NOTIFY_ID);
//        }
//    }
//
//    /**
//     * 适用于android10以后，从后台启动 Activity 的限制
//     * @param fullScreenIntent
//     * @param title
//     * @param content
//     */
//    public synchronized void notify(Intent fullScreenIntent, String title, String content) {
//        if (!EasyUtils.isAppRunningForeground(appContext)) {
//            try {
//                NotificationCompat.Builder builder = generateBaseBuilder(fullScreenIntent, content);
//                if(!TextUtils.isEmpty(title)) {
//                    builder.setContentTitle(title);
//                }
//                Notification notification = builder.build();
//                notificationManager.notify(NOTIFY_ID, notification);
//                if (Build.VERSION.SDK_INT < 26) {
//                    vibrateAndPlayTone();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    /**
//     * Generate a base Notification#Builder, contains:
//     * 1.Use the app icon as default icon
//     * 2.Use the app name as default title
//     * 3.This notification would be sent immediately
//     * 4.Can be cancelled by user
//     * 5.Would launch the default activity when be clicked
//     *
//     * @return
//     */
//    private NotificationCompat.Builder generateBaseBuilder(Intent fullScreenIntent, String content) {
//        PackageManager pm = appContext.getPackageManager();
//        String title = pm.getApplicationLabel(appContext.getApplicationInfo()).toString();
//        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(appContext, NOTIFY_ID,
//                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//        return new NotificationCompat.Builder(appContext, CHANNEL_ID)
//                .setSmallIcon(appContext.getApplicationInfo().icon)
//                .setContentTitle(title)
//                .setTicker(content)
//                .setContentText(content)
//                .setPriority(NotificationCompat.PRIORITY_MAX)
//                .setCategory(NotificationCompat.CATEGORY_CALL)
//                .setWhen(System.currentTimeMillis())
//                .setAutoCancel(true)
//                .setContentIntent(fullScreenPendingIntent);
//    }
//
//    /**
//     * vibrate and  play tone
//     */
//    private void vibrateAndPlayTone() {
//        try {
//            //手机静音模式不提示
//            if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
//                EMLog.e(TAG, "in slient mode now");
//                return;
//            }
//            //创建铃声控制
//            if (ringtone == null) {
//                Uri notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
//                ringtone = RingtoneManager.getRingtone(appContext, notificationUri);
//                if (ringtone == null) {
//                    EMLog.d(TAG, "can not find ringtone at:" + notificationUri.getPath());
//                    return;
//                }
//            }
//            //没有正在播放铃声状态才能播放提示音
//            if (!ringtone.isPlaying()) {
//                String vendor = Build.MANUFACTURER;
//                ringtone.play();
//                // 重复震动7遍，大概3秒
//                vibrator.vibrate(VIBRATION_PATTERN, 7);
//                // for samsung S3, we meet a bug that the phone will
//                // continue ringtone without stop
//                // so add below special handler to stop it after 3s if
//                // needed
//                if (vendor != null && vendor.toLowerCase().contains("samsung")) {
//                    Thread ctlThread = new Thread() {
//                        public void run() {
//                            try {
//                                Thread.sleep(3000);
//                                if (ringtone.isPlaying()) {
//                                    ringtone.stop();
//                                }
//                                vibrator.cancel();
//                            } catch (Exception e) {
//                            }
//                        }
//                    };
//                    ctlThread.run();
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}