package com.mutanti.vatman.util;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
        String encrypted;
        byte[] encryptedBytes = null;
        byte[] key;
        key = AES_V1_KEY.getBytes();
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher;
        try {
            String input = Integer.toString(RAWDATA.length()) + '|' + RAWDATA;
            cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            encryptedBytes = cipher.doFinal(nullPadString(input).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ENCODE) {
            encrypted = Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } else {
            encrypted = new String(encryptedBytes);
        }
        return encrypted;
    }

    public static String decrypt(final String ENCRYPTEDDATA, final boolean DECODE)
            throws Exception {
        String raw = null;
        byte[] rawBytes = null;
        byte[] encryptedBytes;
        if (DECODE) {
            encryptedBytes = Base64.decode(ENCRYPTEDDATA.getBytes(), Base64.DEFAULT);
        } else {
            encryptedBytes = ENCRYPTEDDATA.getBytes();
        }
        byte[] key;
        key = AES_V1_KEY.getBytes();
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            rawBytes = cipher.doFinal(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        raw = new String(rawBytes);
        int delimiter = raw.indexOf('|');
        int length = Integer.valueOf(raw.substring(0, delimiter));
        raw = raw.substring(delimiter + 1, length + delimiter + 1);
        return raw;
    }

    public static String getMD5(final String text) {
        StringBuilder hexString = new StringBuilder();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(text.getBytes());
        byte[] hash = md.digest();

        for (int i = 0; i < hash.length; i++) {
            if ((0xff & hash[i]) < 0x10) {
                hexString.append("0"
                        + Integer.toHexString((0xFF & hash[i])));
            } else {
                hexString.append(Integer.toHexString(0xFF & hash[i]));
            }
        }
        return hexString.toString();
    }
}
