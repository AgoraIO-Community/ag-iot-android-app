package io.agora.sdkwayang.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.Base64;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import io.agora.sdkwayang.constant.ConstantApp;
import io.agora.sdkwayang.logger.WLog;
import io.agora.sdkwayang.protocol.BaseData;


public class ToolUtil {
    final static String TAG = "IOTWY/ToolUtil";

    public static String WEBSOCKET_URL_BASE = "ws://114.236.93.153:8083/iov/websocket/dual?topic=";
    public static String DEVICE_NAME_BASE = null;
    public static String WEBSOCKET_URL_SHAREPREFERENCE = "WEBSOCKET_URL_SHAREPREFERENCE";
    public static String DEVICE_NAME_SHAREPREFERENCE = "DEVICE_NAME_SHAREPREFERENCE";



    public static void saveServerUrl(Context mContext, String serverUrl) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(WEBSOCKET_URL_SHAREPREFERENCE, serverUrl);
        editor.apply();
    }

    public static void saveDeviceInfo(Context mContext, String deviceInfo) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(DEVICE_NAME_SHAREPREFERENCE, deviceInfo);
        editor.apply();
    }

    public static String getServerUrl(Context mContext) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        return pref.getString(WEBSOCKET_URL_SHAREPREFERENCE, WEBSOCKET_URL_BASE);
    }

    public static String getDeviceInfo(Context mContext) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        return pref.getString(DEVICE_NAME_SHAREPREFERENCE, null);
    }

    private static int specialViewId = 10000;

    public static int getSpecialViewId() {
        specialViewId = specialViewId + 1;
        return specialViewId;
    }

    public static Class getObjectClassType(Object param) {
        if (param instanceof Integer) {
            return Integer.class;
        } else if (param instanceof String) {
            return String.class;
        } else if (param instanceof Double) {
            return Double.class;
        } else if (param instanceof Float) {
            return Float.class;
        } else if (param instanceof Long) {
            return Long.class;
        } else if (param instanceof Boolean) {
            return Boolean.class;
        } else if (param instanceof Date) {
            return Date.class;
        }
        return null;
    }

    public static String assembleAppActiveCallback(String deviceNmae,
                                                   String cmd,
                                                   ConcurrentHashMap<String, Object> info,
                                                   ConcurrentHashMap<String, Object> extra) {
        BaseData sendData = new BaseData();
        sendData.setType(EnumClass.CommandType.TYPE_4.value());
        sendData.setDevice(deviceNmae);
        sendData.setCmd(cmd);
        sendData.setInfo(info);
        sendData.setExtra(extra);
        return JsonUtil.packageToJson(sendData);
    }

    public static String assembleAppSelfCollectionCallback(String deviceNmae,
                                                   String cmd,
                                                   ConcurrentHashMap<String, Object> info,
                                                   ConcurrentHashMap<String, Object> extra) {
        BaseData sendData = new BaseData();
        sendData.setType(EnumClass.CommandType.TYPE_9.value());
        sendData.setDevice(deviceNmae);
        sendData.setCmd(cmd);
        sendData.setInfo(info);
        sendData.setExtra(extra);
        return JsonUtil.packageToJson(sendData);
    }


    public static String assembleFormateErrorCallback(String deviceNmae,
                                                      String cmd,
                                                      ConcurrentHashMap<String, Object> info,
                                                      ConcurrentHashMap<String, Object> extra) {
        BaseData sendData = new BaseData();
        sendData.setType(EnumClass.CommandType.TYPE_11.value());
        sendData.setDevice(deviceNmae);
        sendData.setCmd(cmd);
        sendData.setInfo(info);
        sendData.setExtra(extra);
        return JsonUtil.packageToJson(sendData);
    }

    public static String assembleSdkExecute(String deviceNmae,
                                            String cmd, long sequence,
                                            EnumClass.ErrorType errorType,
                                            Object errorValue,
                                            ConcurrentHashMap<String, Object> extra) {
        BaseData sendData = new BaseData();
        sendData.setType(EnumClass.CommandType.TYPE_5.value());
        sendData.setDevice(deviceNmae);
        sendData.setCmd(cmd);
        sendData.setSequence(sequence);
        ConcurrentHashMap<String, Object> info = new ConcurrentHashMap<>();
        info.put("error", errorType.ordinal());
        if(errorValue == null){errorValue = "null";}
        switch (errorType) {
            case TYPE_0:
                info.put("return", errorValue);
                break;
            case TYPE_1:
                info.put("reason", errorValue);
                break;
            case TYPE_2:
                info.put("para", errorValue);
                break;
            case TYPE_3:
                info.put("reason", errorValue);
                break;
            default:
                break;
        }
        sendData.setInfo(info);
        sendData.setExtra(extra);
        return JsonUtil.packageToJson(sendData);
    }

    public static String assembleComplexApiCallback(String deviceNmae,
                                                    String cmd, long sequence,
                                                    EnumClass.ErrorType errorType,
                                                    Object errorValue,
                                                    ConcurrentHashMap<String, Object> extra) {
        BaseData sendData = new BaseData();
        sendData.setType(EnumClass.CommandType.TYPE_10.value());
        sendData.setDevice(deviceNmae);
        sendData.setCmd(cmd);
        sendData.setSequence(sequence);
        ConcurrentHashMap<String, Object> info = new ConcurrentHashMap<>();
        info.put("error", errorType.ordinal());
        switch (errorType) {
            case TYPE_0:
                if(cmd.equals("createDefaultRenderer")){
                    info.put("return", ((Object[])errorValue)[0]);
                    info.put("view", ((Object[])errorValue)[1]);
                }else{
                    info.put("return", errorValue);
                }
                break;
            case TYPE_1:
                break;
            case TYPE_2:
                info.put("para", errorValue);
                break;
            case TYPE_3:
                info.put("reason", errorValue);
                break;
            default:
                break;
        }
        sendData.setInfo(info);
        sendData.setExtra(extra);
        return JsonUtil.packageToJson(sendData);
    }


    public static String assembleNonSdkAPIExecute(String deviceNmae,
                                                  String cmd, long sequence,
                                                  EnumClass.ErrorType errorType,
                                                  Object extralValue,
                                                  ConcurrentHashMap<String, Object> extra) {
        BaseData sendData = new BaseData();
        sendData.setType(EnumClass.CommandType.TYPE_7.value());
        sendData.setDevice(deviceNmae);
        sendData.setCmd(cmd);
        sendData.setSequence(sequence);
        ConcurrentHashMap<String, Object> info = new ConcurrentHashMap<>();
        info.put("error", errorType.ordinal());
        if (cmd.equals("getImageOfView")||cmd.contains("Log")||cmd.contains("getDownloadPathByURL")
                ||cmd.contains("pathToUri")||cmd.contains("isExternalStorageLegacy")) {
            info.put("return", extralValue);
        }
        sendData.setInfo(info);
        sendData.setExtra(extra);
        return JsonUtil.packageToJson(sendData);
    }


    public static String assembleReportCallback(String deviceNmae,
                                                String cmd, long sequence,
                                                ConcurrentHashMap<String, Object> info,
                                                ConcurrentHashMap<String, Object> extra) {
        BaseData sendData = new BaseData();
        sendData.setType(EnumClass.CommandType.TYPE_6.value());
        sendData.setDevice(deviceNmae);
        sendData.setCmd(cmd);
        sendData.setSequence(sequence);
        sendData.setInfo(info);
        sendData.setExtra(extra);
        return JsonUtil.packageToJson(sendData);
    }




    public static String bitmapToBase64(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    public static String fileToBase64(String path) throws Exception{
        String base64 = null;
        InputStream in = null;
        File file = new File(path);
        if(!file.exists()){
            throw new Exception("on exit file and path error");
        }
        in = new FileInputStream(file);
        byte[] bytes = new byte[in.available()];
        in.read(bytes);
        in.close();
        base64 = Base64.encodeToString(bytes,Base64.DEFAULT);
        return base64;
    }

    public static String compressZipLogs(final String fileName,final boolean isJavaCrash,final boolean isAnrCrash) {

        String appPath = AppUtil.APP_DIRECTORY;

        String logPath = appPath + File.separator + "log";
        String zippedFile = null;
        if(!isJavaCrash){
            moveSDKLog(appPath);
            zippedFile = ConstantApp.CRASH_CPP_LOG_ZIP+"-"+fileName+".zip";
        }else {
            if(isAnrCrash){
                zippedFile = ConstantApp.ARN_JAVA_LOG_FILE+"-"+fileName+".zip";
            }else{
                zippedFile = ConstantApp.CRASH_JAVA_LOG_ZIP+"-"+fileName+".zip";
            }
        }

        File ko = new File(zippedFile);
        File parent = ko.getParentFile();
        if (!parent.exists())
            parent.mkdirs();
        ko.delete(); // clean it first

        // get a list of files from log path directory
        File crashLogFile = new File(logPath);
        String[] files = crashLogFile.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if(!isJavaCrash){
                    if (name.contains("app") ||name.startsWith("agorasdk")||
                            name.contains(fileName)) {
                        return true;
                    }
                }else{
                    if (name.contains(fileName)){
                        return true;
                    }
                }

                return false;
            }
        });

        // no file need to upload
        if (files == null || files.length == 0)
            return null;

        String zipFilePath = FileUtil.zipFiles(zippedFile, logPath, files);
        if(zipFilePath!=null){
            //deleteUselessLogs(logPath,files);
            return zipFilePath;
        }else {
            return null;
        }

    }

    private static void moveSDKLog(String appPath) {
        String shellCmd_1 = "cp "+appPath+ File.separator + "agorasdk_1.log"+" "+appPath+ File.separator +"log";
        String shellCmd = "cp "+appPath+ File.separator + "agorasdk.log"+" "+appPath+ File.separator +"log";
        Runtime runtime = Runtime.getRuntime();
        BufferedReader reader = null;
        try {
            runtime.exec(shellCmd);
            runtime.exec(shellCmd_1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void clearLogcat() {
        WLog.getInstance().d(TAG, "clearLogcat start");
        String[] cmds = {"logcat", "-c"};
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(cmds).waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        WLog.getInstance().d(TAG, "clearLogcat finished");
    }


}
