package io.taucbd.news.publishing.core.utils.rlp;

import org.libTAU4j.Vectors;
import org.libTAU4j.swig.byte_vector;
import org.libTAU4j.swig.libTAU;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtil {

    private static final String KEY_ALGORITHM = "AES";

    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    private static final byte[] iv = {0x01, 0x23, 0x45, 0x67, 0x89 - 0xFF, 0xAB - 0xFF, 0xCD - 0xFF, 0xEF - 0xFF,
            0x01, 0x23, 0x45, 0x67, 0x89 - 0xFF, 0xAB - 0xFF, 0xCD - 0xFF, 0xEF - 0xFF};

    /**
     * 秘钥分发
     * @param publicKey 对方公钥
     * @param secretKey 我的私钥
     * @return 加密用的秘钥
     */
    public static byte[] keyExchange(byte[] publicKey, byte[] secretKey) {
        if (publicKey != null && publicKey.length == 32) {
            if (secretKey != null && secretKey.length == 64) {
                byte_vector secret = libTAU.ed25519_key_exchange(Vectors.bytes2byte_vector(publicKey), Vectors.bytes2byte_vector(secretKey));
                return Vectors.byte_vector2bytes(secret);
            } else {
                throw new IllegalArgumentException("private key must be not null and of size 64");
            }
        } else {
            throw new IllegalArgumentException("public key must be not null and of size 32");
        }
    }

    /**
     * 解密数据
     *
     * @param data
     *            待解密数据
     * @param key
     *            密钥
     * @return byte[] 解密后的数据
     * */
    public static byte[] decrypt(byte[] data, byte[] key) throws Exception {
        // 欢迎密钥
        Key k = toKey(key);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        // 初始化，设置为解密模式
        cipher.init(Cipher.DECRYPT_MODE, k, new IvParameterSpec(iv));
        // 执行操作
        return cipher.doFinal(data);
    }

    /**
     * 加密数据
     *
     * @param data
     *            待加密数据
     * @param key
     *            密钥
     * @return byte[] 加密后的数据
     * */
    public static byte[] encrypt(byte[] data, byte[] key) throws Exception {
        // 还原密钥
        Key k = toKey(key);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        // 初始化，设置为加密模式
        cipher.init(Cipher.ENCRYPT_MODE, k, new IvParameterSpec(iv));
        // 执行操作
        return cipher.doFinal(data);
    }

    /**
     *
     * 生成密钥，java6只支持256位密钥，bouncycastle支持64位密钥
     *
     * @return byte[] 二进制密钥
     * */
    public static byte[] initkey() throws Exception {

        // 实例化密钥生成器
        KeyGenerator kg = KeyGenerator.getInstance(KEY_ALGORITHM);
        // 初始化密钥生成器，AES要求密钥长度为128位、192位、256位
        kg.init(256);
        // 生成密钥
        SecretKey secretKey = kg.generateKey();
        // 获取二进制密钥编码形式
        return secretKey.getEncoded();
    }

    /**
     * 转换密钥
     *
     * @param key
     *            二进制密钥
     * @return Key 密钥
     * */
    public static Key toKey(byte[] key) throws Exception {
        // 实例化DES密钥
        // 生成密钥
        return new SecretKeySpec(key, KEY_ALGORITHM);
    }
}
