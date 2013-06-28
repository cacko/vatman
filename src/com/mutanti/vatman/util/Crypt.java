package com.mutanti.vatman.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Crypt {

	final static String AES_V1_KEY = "6FE3F6E12EE0E51A9AFDBB90D83A4FC5";

	static String nullPadString(String original) {
		String output = original;
		int remain = output.length() % 16;
		if (remain != 0) {
			remain = 16 - remain;
			for (int i = 0; i < remain; i++) {
				output += (char) 0;
			}
		}
		return output;
	}

	public static String encrypt(final String RAWDATA, boolean ENCODE)
			throws Exception { // This was a custom
		String encrypted = null;
		byte[] encryptedBytes = null;
		byte[] key;
		key = AES_V1_KEY.getBytes();
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
		Cipher cipher = null;
		try {
			String input = Integer.toString(RAWDATA.length()) + '|' + RAWDATA;
			cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
			encryptedBytes = cipher.doFinal(nullPadString(input).getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (ENCODE) {
			encrypted = new String(Base64.encode(encryptedBytes));
		} else {
			encrypted = new String(encryptedBytes);
		}
		return encrypted;
	}

//	static String decryptString(final String ENCRYPTEDDATA, final boolean DECODE)
//			throws Exception {
//		String raw = null;
//		byte[] rawBytes = null;
//		byte[] encryptedBytes;
//		if (DECODE) {
//			encryptedBytes = Base64.decodeBase64(ENCRYPTEDDATA.getBytes());
//		} else {
//			encryptedBytes = ENCRYPTEDDATA.getBytes();
//		}
//		byte[] key;
//		key = AES_V1_KEY.getBytes();
//		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
//		// Inizializzo il cipher
//		Cipher cipher = null;
//		try {
//			cipher = Cipher.getInstance("AES/ECB/NoPadding");
//			cipher.init(Cipher.DECRYPT_MODE, skeySpec);
//			rawBytes = cipher.doFinal(encryptedBytes);
//		} catch (Exception e) {
//		}
//		raw = new String(rawBytes);
//		int delimiter = raw.indexOf('|');
//		int length = Integer.valueOf(raw.substring(0, delimiter));
//		raw = raw.substring(delimiter + 1, length + delimiter + 1);
//		return raw;
//	}
}
