package io.agora.sdkwayang.util;

import android.graphics.Rect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

import io.agora.sdkwayang.protocol.BaseData;


public class JsonUtil {
    //private static Gson gson = new Gson();
    private static Gson gson = new GsonBuilder().serializeNulls().create();

    public static BaseData packageToBaseData(String info) {
        return gson.fromJson(info, BaseData.class);
    }

    public static String packageToJson(Object object) {
        return gson.toJson(object);
    }

}
