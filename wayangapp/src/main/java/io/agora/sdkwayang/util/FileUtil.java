package io.agora.sdkwayang.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class FileUtil {

    private static final int BUFFER = 4096;

    private static final String[] RAW_FILES = {"music.zip"};

    public static void copyRawFilesToSDCardAndUnzip(Context ctx) {
        Resources resources = ctx.getResources();
        AssetManager assetManager = resources.getAssets();

        File fileApp = ctx.getExternalFilesDir(null);
        String appCachePath = fileApp.getAbsolutePath();
        File path = new File(appCachePath + File.separator + "temp" + File.separator);
        if (!path.exists()) {
            path.mkdirs();
        }
        for (String name : RAW_FILES) {
            FileOutputStream dos = null;
            InputStream is = null;
            try {
                dos = new FileOutputStream(new File(path, name));
                is = assetManager.open(name);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    dos.write(buffer, 0, len);
                }
                is.close();
                dos.close();
            } catch (IOException e) {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Exception a) {
                    }
                }
                if (dos != null) {
                    try {
                        dos.close();
                    } catch (Exception a) {
                    }
                }
            }

            if (name.endsWith(".zip")) {
                unzipFile(path + File.separator + name, appCachePath);
                String systemMusicDir = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"Music";
                File file = new File(systemMusicDir);
                if(!file.exists()){
                    file.mkdirs();
                }
                if(file.exists()) {
                    unzipFile(path + File.separator + name, systemMusicDir);
                }
            }
        }

//        removeUnusedAssets(ctx);
    }

//    private static void removeUnusedAssets(Context ctx) {
//        String[] mixing = ctx.getResources().getStringArray(R.array.music_list_for_mix);
//        String[] effecting = ctx.getResources().getStringArray(R.array.music_list_for_effect);
//
//        ArrayList<String> musicList = new ArrayList<>();
//        musicList.addAll(Arrays.asList(mixing));
//        musicList.addAll(Arrays.asList(effecting));
//
//        File fileApp = ctx.getExternalFilesDir(null);
//        String appCachePath = fileApp.getAbsolutePath();
//        File musicPath = new File(appCachePath + File.separator + "music");
//        if (musicPath.exists()) {
//            File[] items = musicPath.listFiles();
//            if (items != null && items.length > 0) {
//                for (File item : items) {
//                    if (!musicList.contains(item.getName())) {
//                        item.delete();
//                    }
//                }
//            }
//        }
//    }

    public static void unzipFile(String zipFile, String destPath) {
        if (!destPath.endsWith(File.separator)) {
            destPath += File.separator;
        }
        FileOutputStream fos;
        ZipInputStream zipIn = null;
        ZipEntry zipEntry;
        File file;
        int buffer;
        byte buf[] = new byte[BUFFER];
        try {
            zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
            while ((zipEntry = zipIn.getNextEntry()) != null) {
                if(zipEntry.getName().contains("/.")) {
                    continue;
                }
                file = new File(destPath + zipEntry.getName());
                if(file.exists()) continue;
                if (zipEntry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    fos = new FileOutputStream(file);
                    while ((buffer = zipIn.read(buf)) > 0) {
                        fos.write(buf, 0, buffer);
                    }
                    fos.close();
                }
                zipIn.closeEntry();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (zipIn != null) {
                    zipIn.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public static String compressLogs(Context context) {
        File fileApp = context.getExternalFilesDir(null);
        String path = fileApp.getAbsolutePath();
        String logPath = path + File.separator + "log";
        String zippedFile = path + File.separator + "Agora-Premium-" +
                android.os.Build.VERSION.RELEASE + "-" + AppUtil.OS + "-"
                + AppUtil.MANUFACTURER + "-" + AppUtil.MODEL + "-" + AppUtil.getDeviceID(context)
                + ".zip";

        File ko = new File(zippedFile);
        File parent = ko.getParentFile();
        if (!parent.exists())
            parent.mkdirs();
        ko.delete(); // clean it first

        // get a list of files from log path directory
        File f = new File(logPath);
        String[] files = f.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.startsWith("app") ||
                        name.startsWith("agora-rtc-sdk.log")) {
                    return true;
                }
                return false;
            }
        });

        // no file need to upload
        if (files == null || files.length == 0)
            return null;

        return zipFiles(zippedFile, logPath, files);
    }

    public static String zipFiles(String zippedFile, String path, String[] files) {
        ZipOutputStream out = null;
        BufferedInputStream origin = null;
        try {
            out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zippedFile)));
            byte[] buffer = new byte[BUFFER];

            for (int i = 0; i < files.length; i++) {
                origin = new BufferedInputStream(new FileInputStream(path + File.separator + files[i]), BUFFER);
                ZipEntry entry = new ZipEntry(files[i]);
                out.putNextEntry(entry);
                int count = -1;
                while ((count = origin.read(buffer, 0, BUFFER)) != -1) {
                    out.write(buffer, 0, count);
                }
                origin.close();
                origin = null;
            }

            return zippedFile;
        } catch (Exception e) {
            Log.e("FileUtil", "zipFiles: ", e);
            new File(zippedFile).delete();
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            if (origin != null) {
                try {
                    origin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public static String compressPCMs(Context context) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        String zippedFile = path + File.separator + "io.agora.falcondemo" + File.separator
                + "IotLink-" + android.os.Build.VERSION.RELEASE
                + "-" + AppUtil.OS + "-" + AppUtil.MANUFACTURER + "-" + AppUtil.MODEL + "-"
                + AppUtil.getDeviceID(context) + "-pcm.zip";

        File ko = new File(zippedFile);
        File parent = ko.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        ko.delete();// clean it first

        // get a list of files from log path directory
        File f = new File(path);
        String[] files = f.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith("far_in.pcm") || name.endsWith("near_in.pcm") || name.endsWith("near_out.pcm"))
                    return true;
                return false;
            }
        });

        if (files == null || files.length == 0) { // no file need to upload
            return null;
        }
        return FileUtil.zipFiles(zippedFile, path, files);
    }


    public static void logcatWriteTofile(String line,String fileName) {
        String content = line + "\r\n";
        File file = new File(fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
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
}
