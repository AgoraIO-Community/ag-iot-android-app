package io.agora.sdkwayang.logger.crashDeal;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.agora.sdkwayang.constant.ConstantApp;
import io.agora.sdkwayang.logger.WLog;


/**
 * Created by yong on 2018/9/8.
 */

public class NormalLogService extends Service {
    private final static String TAG = "IOTWY/NormLogSrv";
    private Thread thread;
    private boolean isFileCreated = false;
    private boolean isLogThreadOnFlag = false;
    private boolean isLogThreadRunning = false;
    public class LogBinder extends Binder {
        public NormalLogService getService() {
            return NormalLogService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LogBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread serviceThread = new HandlerThread("normal_log_service",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        serviceThread.start();
        deleteNormalLogFile();
    }

    public void startLogThread(){
        WLog.getInstance().d(TAG, "startLogThread");
        if(thread!=null&&!isLogThreadRunning){
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    isLogThreadRunning = true;
                    WLog.getInstance().d(TAG,"startLogThread thread start");
                    log2();
                    WLog.getInstance().d(TAG,"startLogThread thread stop");
                    isLogThreadRunning = false;
                }
            });
            isLogThreadOnFlag = true;
            thread.start();
        }
    }

    public void stopLogFile(){
        isLogThreadOnFlag = false;
        while(isLogThreadRunning){
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        thread = null;
        WLog.getInstance().d(TAG,"stopLogFile");
    }

    public void deleteNormalLogFile(){
        WLog.getInstance().d(TAG,"deleteNormalLogFile");
        File file = new File(ConstantApp.NORMAL_LOG_FILE);
        if (file.exists()) {
            file.delete();
        }
        isFileCreated = false;
    }

    public boolean isLogThreadRunning(){
        return isLogThreadRunning;
    }



    private void  log2() {
        WLog.getInstance().d(TAG,"normalLog start");
        String[] cmds = {"logcat", "-c"};
        String shellCmd = "logcat -v time -s *:W "; // adb logcat -v time *:W
        Process process = null;
        Runtime runtime = Runtime.getRuntime();
        BufferedReader reader = null;
        try {
            runtime.exec(cmds).waitFor();
            process = runtime.exec(shellCmd);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null&&isLogThreadOnFlag) {
                if (line.contains(String.valueOf(android.os.Process.myPid()))) {
                    // line = new String(line.getBytes("iso-8859-1"), "utf-8");
                    writeTofile(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        WLog.getInstance().d(TAG,"normalLog finished");
    }


    private void writeTofile(String line) {
        String content = line + "\r\n";
        File file = new File(ConstantApp.NORMAL_LOG_FILE);
        if (!file.exists()) {
            try {
                file.createNewFile();
                isFileCreated = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file, true);
            fos.write(content.getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();

    }

    @Override
    public boolean onUnbind(Intent intent) {
        WLog.getInstance().d(TAG,"onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        WLog.getInstance().d(TAG,"onRebind");
        super.onRebind(intent);
    }

}