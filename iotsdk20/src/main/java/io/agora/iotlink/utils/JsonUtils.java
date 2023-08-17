
package io.agora.iotlink.utils;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource.PSpecified;

import io.agora.iotlink.logger.ALog;

/**
 * @brief RSA加密解密算法类
 *
 */
public class JsonUtils {
	private static final String TAG = "IOTSDK/JsonUtils";


	public static JSONObject generateJsonObject(final String content) {
		try {
			JSONObject newJsonObj = new JSONObject(content);
			return newJsonObj;

		} catch (JSONException jsonExp) {
			jsonExp.printStackTrace();
			ALog.getInstance().e(TAG, "<generateJsonObject> [EXCEPTION] jsonExp=" + jsonExp);
			return null;
		}
	}


	public static JSONObject parseJsonObject(JSONObject jsonState, String fieldName, JSONObject defVal) {
		try {
			JSONObject value = jsonState.getJSONObject(fieldName);
			return value;

		} catch (JSONException e) {
			ALog.getInstance().e(TAG, "<parseJsonObject> "
					+ ", fieldName=" + fieldName + ", exp=" + e.toString());
			return defVal;
		}
	}

	public static int parseJsonIntValue(JSONObject jsonState, String fieldName, int defVal) {
		try {
			int value = jsonState.getInt(fieldName);
			return value;

		} catch (JSONException e) {
			ALog.getInstance().e(TAG, "<parseJsonIntValue> "
					+ ", fieldName=" + fieldName + ", exp=" + e.toString());
			return defVal;
		}
	}

	public static long parseJsonLongValue(JSONObject jsonState, String fieldName, long defVal) {
		try {
			long value = jsonState.getLong(fieldName);
			return value;

		} catch (JSONException e) {
			ALog.getInstance().e(TAG, "<parseJsonLongValue> "
					+ ", fieldName=" + fieldName + ", exp=" + e.toString());
			return defVal;
		}
	}

	public static boolean parseJsonBoolValue(JSONObject jsonState, String fieldName, boolean defVal) {
		try {
			boolean value = jsonState.getBoolean(fieldName);
			return value;

		} catch (JSONException e) {
			ALog.getInstance().e(TAG, "<parseJsonBoolValue> "
					+ ", fieldName=" + fieldName + ", exp=" + e.toString());
			return defVal;
		}
	}

	public static String parseJsonStringValue(JSONObject jsonState, String fieldName, String defVal) {
		try {
			String value = jsonState.getString(fieldName);
			return value;

		} catch (JSONException e) {
			ALog.getInstance().e(TAG, "<parseJsonIntValue> "
					+ ", fieldName=" + fieldName + ", exp=" + e.toString());
			return defVal;
		}
	}

	public static JSONArray parseJsonArray(JSONObject jsonState, String fieldName) {
		try {
			JSONArray jsonArray = jsonState.getJSONArray(fieldName);
			return jsonArray;

		} catch (JSONException e) {
			ALog.getInstance().e(TAG, "<parseJsonArray> "
					+ ", fieldName=" + fieldName + ", exp=" + e.toString());
			return null;
		}
	}

}

