package io.agora.falcondemo.utils;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

import com.agora.baselibrary.utils.StringUtils;

import io.agora.falcondemo.base.PushApplication;
import io.agora.iotlink.IConnectionObj;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {
     public static String getBaseStrPath() {
//         File file = AgoraApplication.mInstance.getFilesDir();
//         String basePath = file.getAbsolutePath() + File.separator;
//         return basePath;

         File file = PushApplication.getInstance().getExternalFilesDir(null);
         String basePath = file.getAbsolutePath() + File.separator;
         return basePath;
    }

    public static String getStrSDPath() {
        return getBaseStrPath() + "agora" + File.separator;
    }

    public static String getTempSDPath() {
        return getBaseStrPath() + "ag" + File.separator;
    }

    public static String getFileSavePath(String baseName, IConnectionObj.STREAM_ID streamId, boolean isPic) {
        String name = isPic ? ".jpg" : ".mp4";
        Time time = new Time("GMT+8");
        time.setToNow();
        File dir = new File(FileUtils.getStrSDPath());
        if (!dir.exists()) {
            dir.mkdir();
        }
        String streamName = DevStreamUtils.getStreamName(streamId);
        return FileUtils.getStrSDPath() + baseName + "_shot_" + streamName + "_" +
                StringUtils.INSTANCE.getDetailTime("yyyy-MM-dd_HH_mm_ss", System.currentTimeMillis() / 1000) + name;
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
     * 删除文件
     */
    public static void deleteFile(String filePath) {
        if (TextUtils.isEmpty(filePath)) return;
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
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


    /**
     * @brief 将字节流转换成 Hex字符串
     */
    public static String bytesToHexText(final byte[] bytesBuffer) {
        String text = "";
        for (int i = 0; i < bytesBuffer.length; i++) {
            text = text + bytesBuffer[i];
        }
        return text;
    }

    /**
     * @brief 将字节流转换成 Hex字符串
     */
    public static String bytesToHexString(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        String temp = null;

        for (int i = 0; i < bytes.length; i++) {
            temp = Integer.toHexString(bytes[i] & 0xFF);
            if (temp.length() == 1) {
                stringBuffer.append("0");
            }
            stringBuffer.append(temp);
        }

        return stringBuffer.toString().toUpperCase();
    }

}
