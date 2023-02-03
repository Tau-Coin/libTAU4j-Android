package io.taucbd.news.publishing.core.utils;

/*
 * Hash utils.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    /**
     * 对文本做Sha256Hash
     * @param str
     * @return
     */
    public static String makeSha256Hash(String str) {
        return makeSha256Hash(str.getBytes(StandardCharsets.UTF_8));
    }

    public static String makeSha256Hash(byte[] data) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        messageDigest.update(data);
        return bytesToString(messageDigest.digest());
    }

    /**
     * 对文本做Sha256Hash, 带时间戳
     * @param str
     * @return
     */
    public static String makeSha256HashWithTimeStamp(String str) {
        str = System.currentTimeMillis() + str;
        return makeSha256Hash(str);
    }

    private static byte[] byteMergerAll(byte[]... args) {
        int length_byte = 0;
        for (byte[] b : args) {
            length_byte += b.length;
        }
        byte[] all_byte = new byte[length_byte];
        int countLength = 0;
        for (byte[] b : args) {
            System.arraycopy(b, 0, all_byte, countLength, b.length);
            countLength += b.length;
        }
        return all_byte;
    }

    public static String makeSha256HashWithTimeStamp(byte[] data) {
        byte[] time = String.valueOf(System.currentTimeMillis()).getBytes();
        return makeSha256Hash(byteMergerAll(data, time));
    }

    private static String bytesToString(byte[] bytes) {
        StringBuilder sha1 = new StringBuilder();
        for (byte b : bytes) {
            if ((0xff & b) < 0x10)
                sha1.append("0");
            sha1.append(Integer.toHexString(0xff & b));
        }
        return sha1.toString();
    }

    /**
     * 对文件做Sha256Hash
     * @param filePath
     * @return
     */
    public static String makeFileSha256Hash(String filePath) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        File file = new File(filePath);
        if (file.exists()) {
            try {
                FileInputStream in = new FileInputStream(file);
                FileChannel ch = in.getChannel();
                MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0,
                        file.length());
                messageDigest.update(byteBuffer);
                return bytesToString(messageDigest.digest());
            } catch (IOException ignore) {
            }
        }
        return null;
    }

    /**
     * 对文件做Sha256Hash, 带时间戳
     * @param filePath
     * @return
     */
    public static String makeFileSha256HashWithTimeStamp(String filePath) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        File file = new File(filePath);
        if (file.exists()) {
            try {
                FileInputStream in = new FileInputStream(file);
                FileChannel ch = in.getChannel();
                MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0,
                        file.length());
                byteBuffer.put(String.valueOf(System.currentTimeMillis()).getBytes());
                messageDigest.update(byteBuffer);
                return bytesToString(messageDigest.digest());
            } catch (IOException ignore) {
            }
        }
        return null;
    }

    public static String hashMiddleHide(String hash) {
        int startNum = 3;
        int lastNum = 6;
        if(StringUtil.isNotEmpty(hash) && hash.length() > startNum + lastNum){
            String middleHide = hash.substring(0, startNum);
            middleHide += "***";
            middleHide += hash.substring(hash.length() - lastNum);
            return middleHide;
        }
        return hash;
    }
}