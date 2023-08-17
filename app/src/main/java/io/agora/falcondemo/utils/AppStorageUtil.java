package io.agora.falcondemo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.agora.baselibrary.utils.SPUtil;

import java.nio.charset.StandardCharsets;


public class AppStorageUtil {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////

    //
    // 应用存储参数键值
    //
    public static final String KEY_ACCOUNT = "KEY_ACCOUNT";
    public static final String KEY_PASSWORD = "KEY_PASSWORD";
    public static final String KEY_TOKEN = "KEY_TOKEN";
    public static final String KEY_IDENTITYID = "KEY_IDENTITYID";
    public static final String KEY_ENDPOINT = "KEY_ENDPOINT";
    public static final String KEY_POOLTOKEN = "KEY_POOLTOKEN";
    public static final String KEY_IDENTIFIER = "KEY_IDENTIFIER";
    public static final String KEY_IDENTIFIERPOOLID = "KEY_IDENTIFIERPOOLID";
    public static final String KEY_REGION = "KEY_REGION";


    //////////////////////////////////////////////////////////////////
    ////////////////////// Public Methods ///////////////////////////
    //////////////////////////////////////////////////////////////////

    private static SharedPreferences sharedPreferences;
    private volatile static AppStorageUtil instance;

    public static AppStorageUtil init(Context context) {
        if (instance == null) {
            synchronized (AppStorageUtil.class) {
                if (instance == null) {
                    instance = new AppStorageUtil(context);
                }
            }
        }
        return instance;
    }

    private AppStorageUtil(Context context) {
        sharedPreferences = context.getSharedPreferences("IoTDemo", Context.MODE_PRIVATE);
    }

    public static void keepShared(String key, String value) {
        Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void keepShared(String key, Integer value) {
        Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static void keepShared(String key, long value) {
        Editor editor = sharedPreferences.edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public static void keepShared(String key, int value) {
        Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static void keepShared(String key, boolean value) {
        Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static String queryValue(String key, String defvalue) {
        String value = sharedPreferences.getString(key, defvalue);
        // if ("".equals(value)) {
        // return "";
        // }
        return value;
    }

    public static String queryValue(String key) {
        String value = sharedPreferences.getString(key, "");
        if ("".equals(value)) {
            return "";
        }

        return value;
    }

    public static Integer queryIntValue(String key) {
        int value = sharedPreferences.getInt(key, 0);
        return value;
    }

    public static Integer queryIntValue(String key, int defalut) {
        int value = sharedPreferences.getInt(key, defalut);
        return value;
    }

    public static boolean queryBooleanValue(String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    public static long queryLongValue(String key) {
        return sharedPreferences.getLong(key, 0);
    }

    public static boolean deleteAllValue() {

        return sharedPreferences.edit().clear().commit();
    }

    public static void deleteValue(String key) {
        sharedPreferences.edit().remove(key).commit();
    }


    public static void safePutString(Context ctx, String key, String value) {
        if (TextUtils.isEmpty(value)) {
            SPUtil.Companion.getInstance(ctx).putString(key, value);
            return;
        }
        String encryptValue = encryptString(value);
        SPUtil.Companion.getInstance(ctx).putString(key, encryptValue);
    }

    public static String safeGetString(Context ctx,String key, String defValue) {
        String encryptValue = SPUtil.Companion.getInstance(ctx).getString(key, defValue);
        if (encryptValue == null) {
            return null;
        }
        if (encryptValue.isEmpty()) {
            return "";
        }
        String value = decryptString(encryptValue);
        return value;
    }


    /**
     * @biref 字符串加密
     */
    public static String encryptString(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }

        // 转换成 Utf-8 字节流
        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);

        byte[] encryptData = enDeCrypt(utf8);

        // 每个字节转换成 xxx, 数字
        String encrypted = bytesToString(encryptData);
        return encrypted;
    }

    /**
     * @biref 字符串解密
     */
    public static String decryptString(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }

        // 每个字节转换成utf8字节流
        byte[] utf8 = stringToBytes(value);

        byte[] decrypted = enDeCrypt(utf8);

        // 文本字节由utf8字节流创建
        String text = new String(decrypted, StandardCharsets.UTF_8);
        return text;
    }



    /**
     * @biref 将字节流数据转换成16进制
     * @param data    字节流数据
     * @return 返回转换后的文本
     */
    public static String bytesToString(byte[] data) {
        if (data == null) {
            return "";
        }
        String text = "";
        for (int j = 0; j < data.length; j++) {
            String dataHex = String.format("%03d,", data[j]);
            text += dataHex;
        }

        return text;
    }

    /**
     * @biref 将字符串转换成字节流
     * @param text 字符串
     * @return 返回转换后的文本
     */
    public static byte[] stringToBytes(final String text) {
        if (text == null) {
            return null;
        }

        String[] elemArray = text.split(",");
        if (elemArray == null || elemArray.length <= 0) {
            return null;
        }

        byte[] data = new byte[elemArray.length];
        for (int i = 0; i < elemArray.length; i++) {
            data[i] = Byte.valueOf(elemArray[i]);
        }

        return data;
    }




    public static byte[] enDeCrypt(byte[] inData) {
        if (inData == null) {
            return null;
        }

        int [] key = new int[] { 0x05, 0xEF, 0x4F, 0x28, 0x61, 0x46, 0x43, 0x6C, 0x73, 0x23, 0x22, 0x43, 0x7E, 0x7D, 0x96, 0xB4};
        int keyLen = key.length;
        int dataSize = inData.length;
        byte[] outData = new byte[dataSize];
        int i, j = 0;
        for (i = 0; i < dataSize; i++) {
            outData[i] = (byte)(inData[i] ^ key[j]);
            j = (j + 1) % keyLen;
        }

        return outData;
    }

//    public static void UnitTest() {
//        String orgText = "ABCDefgh012346+-*%_@!#$%^&()华叔[好人]<>";
//        String encryptText = AppStorageUtil.encryptString(orgText);
//        String decryptText = AppStorageUtil.decryptString(encryptText);
//
//        if (orgText.compareToIgnoreCase(decryptText) != 0) {
//            Log.d("UT", "encrypt error");
//        } else {
//            Log.d("UT", "encrypt OK");
//        }
//    }
}
