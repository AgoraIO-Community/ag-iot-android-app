package io.agora.iotlinkdemo.utils;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

import com.agora.baselibrary.listener.ISingleCallback;
import com.agora.baselibrary.utils.StringUtils;
import com.agora.baselibrary.utils.ThreadManager;
import io.agora.iotlinkdemo.api.bean.AlbumBean;
import io.agora.iotlinkdemo.base.AgoraApplication;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileUtils {
    //    public static final String STR_SD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "agora";
    //    String strSdPath = AgoraApplication.mInstance.getExternalFilesDir("media").getAbsolutePath();
    public static String getBaseStrPath() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
        } else {
            return AgoraApplication.mInstance.getExternalFilesDir("media").getAbsolutePath() + File.separator;
        }
    }

    public static String getStrSDPath() {
        return getBaseStrPath() + "agora" + File.separator;
    }

    public static String getTempSDPath() {
        return getBaseStrPath() + "ag" + File.separator;
    }

    public static String getFileSavePath(String baseName, boolean isPic) {
        String name = isPic ? ".jpg" : ".mp4";
        Time time = new Time("GMT+8");
        time.setToNow();
        File dir = new File(FileUtils.getStrSDPath());
        if (!dir.exists()) {
            dir.mkdir();
        }
        return FileUtils.getStrSDPath() + baseName + "_shot_" +
                StringUtils.INSTANCE.getDetailTime("yyyy-MM-dd_HH&mm_ss", System.currentTimeMillis() / 1000) + name;
    }

    /**
     * 保存截图
     */
    public static boolean saveScreenshotToSD(Bitmap bmp, String fileName) {
        File f = new File(fileName);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        Log.d("cwtsw", "save path = " + f.getAbsolutePath());
        return true;
    }

    /**
     * 保存视频
     */
    public static void saveVideoToSD() {

    }
    /**
     * 将文件转换成byte数组
     * @param filePath
     * @return
     */
    public static byte[] file2byte(File filePath){
        byte[] buffer = null;
        try
        {
            FileInputStream fis = new FileInputStream(filePath);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1)
            {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return buffer;
    }

    /**
     * 读取媒体文件
     */
    public static void readMediaFilesFromSD(ISingleCallback<Integer, List<AlbumBean>> iSingleCallback) {
        ThreadManager.executeOnSubThread(() -> {
            String tempDate = "";
            File[] files = new File(getStrSDPath()).listFiles();
            if (files == null) {
                iSingleCallback.onSingleCallback(1, null);
                return;
            }
            List fileList = Arrays.asList(files);
            Collections.sort(fileList, (Comparator<File>) (file, t1) -> file.getName().split("_")[2].compareTo(t1.getName().split("_")[2]));
            if (files.length <= 0) {
                iSingleCallback.onSingleCallback(1, null);
            }

            ArrayList<AlbumBean> albumBeans = new ArrayList<>(files.length);
            for (File file : files) {
                AlbumBean bean = new AlbumBean();
                bean.mediaCover = file.getAbsolutePath();
                bean.date = file.getName().split("_")[2];
                bean.filePath = file.getAbsolutePath();
                if (!tempDate.equals(bean.date)) {
                    tempDate = bean.date;
                    AlbumBean dateBean = new AlbumBean();
                    dateBean.itemType = 0;
                    dateBean.date = tempDate;
                    albumBeans.add(dateBean);
                }
                bean.time = file.getName().split("_")[3].replace("&", ":");
                if (file.getName().contains("jpg")) {
                    bean.mediaType = 0;
                } else {
                    bean.mediaType = 1;
                    bean.duration = Integer.parseInt(file.getName().split("_")[5].split("\\.")[0]);
                }
                albumBeans.add(bean);
            }
            iSingleCallback.onSingleCallback(0, albumBeans);
        });
    }

    /**
     * 删除文件
     */
    public static void deleteFile(String filePath) {
        if (TextUtils.isEmpty(filePath)) return;
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 删除多个文件
     */
    public static void deleteFiles(List<AlbumBean> list, ISingleCallback<Integer, List<AlbumBean>> iSingleCallback) {
        ThreadManager.executeOnSubThread(() -> {
            for (AlbumBean albumBean : list) {
                deleteFile(albumBean.filePath);
            }
            iSingleCallback.onSingleCallback(0, null);
        });
    }

    public static byte[] saveBmpToBuffer(Bitmap bmp) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();

            byte[] outBuffer = out.toByteArray();
            return outBuffer;

        } catch (IllegalArgumentException agruExp) {
            agruExp.printStackTrace();
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
}
