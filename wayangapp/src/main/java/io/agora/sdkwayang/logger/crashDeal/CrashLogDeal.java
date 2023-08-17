package io.agora.sdkwayang.logger.crashDeal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.TooManyListenersException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.agora.sdkwayang.constant.ConstantApp;
import io.agora.sdkwayang.logger.WLog;
import io.agora.sdkwayang.util.FileUtil;
import io.agora.sdkwayang.util.ToolUtil;


/**
 * UncaughtException处理类,当程序发生Uncaught异常的时候,有该类来接管程序,并记录发送错误报告.
 */

public class CrashLogDeal implements UncaughtExceptionHandler {
    private final static String TAG = "IOTWY/CrashDeal";

    private Thread.UncaughtExceptionHandler mDefaultHandler;
    private static CrashLogDeal INSTANCE = null;
    private static Context mContext;
    private static int anrIndex = 0;
    private Handler anrHandler = null;
    private   AnrWatchDogThread anrWatchDogThread = null;
    //use to store device info and exception info
    private Map<String, String> infos = new HashMap<String, String>();

    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");


    public void releaseContext() {
        this.mContext = null;
    }

    //ANR异常
    private static MyReceiver myReceiver;

    public static CrashLogDeal getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CrashLogDeal();
        }
        return INSTANCE;
    }


    /**
     * @param context
     */
    public void init(Context context) {
        WLog.getInstance().d(TAG, "<init>");
        mContext = context;
        registerANRReceiver();
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        anrHandler = new Myhandle();
        Thread.setDefaultUncaughtExceptionHandler(this);
        anrWatchDogThread = new AnrWatchDogThread();
        Thread anrThread = new Thread(anrWatchDogThread);
        anrThread.setName("anrWatchDogThread");
        anrThread.start();
        signalFlag = false;
    }


    private final String ACTION_ANR = "android.intent.action.ANR";

    public void registerANRReceiver() {
        WLog.getInstance().d(TAG, "<registerANRReceiver>");
        myReceiver = new MyReceiver();
        mContext.registerReceiver(myReceiver, new IntentFilter(ACTION_ANR));
    }

    public void unregisterANRReceiver() {
        if (myReceiver == null) {
            return;
        }
        mContext.unregisterReceiver(myReceiver);
    }

    public class AnrWatchDogThread implements Runnable {
        //deal with anr
        private CountDownLatch lock = null;
        private boolean watchDogFlag = false;
        public AnrWatchDogThread() {
            lock = new CountDownLatch(1);
            watchDogFlag = true;
            anrIndex = 1;
        }


        public void waitForCondition(CountDownLatch x, int tInMS){
            try{
                x.await(tInMS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(x.getCount()==0){
                anrIndex = 0;
            }else{
                anrIndex = 1;
            }
        }

        @Override
        public void run() {
            WLog.getInstance().d(TAG,"anr watchdog thread start to run");
            Looper.prepare();
            while (watchDogFlag){
                anrHandler.postDelayed(mSendUpdateMsg, 5000);
                waitForCondition(lock,35000);
                if(watchDogFlag){
                    if(anrIndex ==1){
                        WLog.getInstance().d(TAG, "anr watchdog get an anr");
                        anrLogCollection();
                        watchDogFlag = false;
                    }else {
                        //log.info("main thread update anr flag in a circle");
                    }
                    lock = new CountDownLatch(1);
                }
            }
            Looper.loop();
            WLog.getInstance().d(TAG,"end anr watchdog thread");
        }

        private Runnable mSendUpdateMsg = new Runnable() {
            @Override
            public void run() {
                if (watchDogFlag) {
                    Message msgCheck = Message.obtain();
                    msgCheck.what = 1;
                    anrHandler.sendMessage(msgCheck);
                    //log.info("send anr check msg to main thread");
                }
            }
        };

        public void resumAnrThread() {
            synchronized (lock) {
                lock.countDown();
            }
        }
        public void stopThread(){
            watchDogFlag = false;
            this.resumAnrThread();
            lock = null;
        }

    }
    public class Myhandle extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                anrWatchDogThread.resumAnrThread();
            }
        }
    }

    public void anrLogCollection(){
        long timestamp = System.currentTimeMillis();
        String time = formatter.format(new Date());
        final String fileName = time + "-" + timestamp;
        collectDeviceInfo(mContext);
        //save anr log file
        traceToFile(fileName);
        ToolUtil.compressZipLogs(fileName, true, true);
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_ANR)) {
                WLog.getInstance().d(TAG,"ANR recived");
                //anrLogCollection();
            }

        }
    }

    /**
     * when UncaughtException happend ,and deal in this function
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            //use system default exception handle
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                WLog.getInstance().e(TAG,"[EXP] e="+e);
            }
            //quit application
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        Thread handleExceptionThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "程序出现JAVA异常,即将退出.", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        };
        handleExceptionThread.setName("handleException java");
        handleExceptionThread.start();
        long timestamp = System.currentTimeMillis();
        String time = formatter.format(new Date());
        final String fileName = time + "-" + timestamp;
        collectDeviceInfo(mContext);
        //save log info
        saveJavaCrashInfo2File(ex, fileName);
        //compress as zip
        ToolUtil.compressZipLogs(fileName, true, false);
        return true;
    }

    /**
     * collect device info
     *
     * @param ctx
     */
    public void collectDeviceInfo(Context ctx) {
        try {
            infos.clear();
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            WLog.getInstance().e(TAG,"an error occured when collect package info="+e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
                //log.debug( field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                WLog.getInstance().e(TAG, "[EXP] an error occured when collect crash info="+e);
            }
        }
    }

    /**
     * save error info into file
     */
    private String saveJavaCrashInfo2File(Throwable ex, String fileName) {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            fileName = ConstantApp.CRASH_JAVA_LOG_FILE + "-" + fileName;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                FileOutputStream fos = new FileOutputStream(fileName);
                fos.write(sb.toString().getBytes());
                fos.close();
            }
            return fileName;
        } catch (Exception e) {
            WLog.getInstance().e(TAG,"[EXP] an error occured while writing file="+ e);
        }
        return null;
    }

    private boolean signalFlag = false;


//    public static void testSignal(int signal) {
//        AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
//        Intent intent = new Intent(mContext, ViewActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.putExtra(ConstantApp.CRASH_FLAG, true);
//        PendingIntent restartIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);
//        mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 500, restartIntent); // 1秒钟后重启应用
//        log.debug("SignalHandle *************************Logs about wayang below is not used for analysis************************************");
//        android.os.Process.killProcess(android.os.Process.myPid());
//        System.exit(0);
//        System.gc();
//    }


    private void traceToFile(String fileName) {
        WLog.getInstance().d(TAG,"traceToFile start");

        String finalFileName = ConstantApp.ARN_JAVA_LOG_FILE + "-" + fileName;
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }
        FileUtil.logcatWriteTofile(sb.toString(), finalFileName);
        String shellCmd = "cat /data/anr/traces.txt ";
        Process process = null;
        Runtime runtime = Runtime.getRuntime();
        BufferedReader reader = null;
        try {
            process = runtime.exec(shellCmd);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                line = new String(line.getBytes("iso-8859-1"), "utf-8");
                //log.debug("traceToFile line:" + line);
                FileUtil.logcatWriteTofile(line, finalFileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        WLog.getInstance().d(TAG,"traceToFile finished");
    }

}
