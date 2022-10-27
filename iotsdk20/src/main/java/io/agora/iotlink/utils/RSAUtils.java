
package io.agora.iotlink.utils;


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
public class RSAUtils {
	private static final String TAG = "IOTSDK/RSAUtils";

	/**
	 * @brief 生成秘钥对
	 * @return {@link Map}
	 */
	public static Map<String, byte[]> generateKeyPair() {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(1024);
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			Map<String, byte[]> map = new HashMap<>();
			map.put("public", keyPair.getPublic().getEncoded());
			map.put("private", keyPair.getPrivate().getEncoded());
			return map;

		} catch (Exception exp) {
			exp.printStackTrace();
			ALog.getInstance().e(TAG, "<generateKeyPair> exp=" + exp.toString());
		}

		return null;
	}

	/**
	 * @biref 使用公钥对数据进行加密
	 * @param content        要加密的内容
	 * @param publicKeyBytes 公钥
	 * @return 返回加密后的内容
	 */
	public static byte[] publicEncrypt(byte[] content, byte[] publicKeyBytes) {
		try {
			PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(
																new X509EncodedKeySpec(publicKeyBytes));
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] encrypted = cipher.doFinal(content);
			return encrypted;

		} catch (Exception exp) {
			exp.printStackTrace();
			ALog.getInstance().e(TAG, "<publicEncrypt> exp=" + exp.toString());
		}

		return null;
	}

	/**
	 * @biref 使用私钥对数据进行解密
	 * @param content         要加密的内容
	 * @param privateKeyBytes 私钥
	 * @return 返回解密后的内容
	 */
	public static byte[] privateDecrypt(byte[] content, byte[] privateKeyBytes) {

		try {
			PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(
												new PKCS8EncodedKeySpec(privateKeyBytes));
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			byte[] decrypted = cipher.doFinal(content);
			return decrypted;

		} catch (Exception exp) {
			exp.printStackTrace();
			ALog.getInstance().e(TAG, "<privateDecrypt> exp=" + exp.toString());
		}

		return null;
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
			String dataHex = String.format("0x%02x ", data[j]);
			text += dataHex;
		}

		return text;
	}
}

