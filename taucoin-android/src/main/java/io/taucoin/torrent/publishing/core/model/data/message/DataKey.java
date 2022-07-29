/**
Copyright 2020 taucoin developer

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
(the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
OR OTHER DEALINGS IN THE SOFTWARE.
*/
package io.taucoin.torrent.publishing.core.model.data.message;

import java.util.Arrays;

/**
 * libTAU publish的数据Key管理类
 */
public class DataKey {

    public enum Suffix {
        UNKNOWN("unknown"),
        INFO("info"),
        PIC("pic");

        private final String suffix;
        Suffix(String suffix) {
            this.suffix = suffix;
        }

        public String getValue() {
            return suffix;
        }

        static Suffix parse(byte[] suffix) {
            Suffix[] suffixArr = Suffix.values();
            for (Suffix s : suffixArr) {
                if (Arrays.equals(suffix, s.getValue().getBytes())) {
                    return s;
                }
            }
            return UNKNOWN;
        }
    }
    private static final int PREFIX_SIZE = 12;      // 前缀的长度

    /**
     * 获取完整的key
     * @param keySrc key基础
     * @param suffix 后缀
     * @return 完整的key
     */
    public static byte[] getKey(byte[] keySrc, Suffix suffix) {
        byte[] keySuffix = suffix.getValue().getBytes();
        byte[] key = new byte[PREFIX_SIZE + keySuffix.length];
        System.arraycopy(keySrc, 0, key, 0, PREFIX_SIZE);
        System.arraycopy(keySuffix, 0, key, PREFIX_SIZE, keySuffix.length);
        return key;
    }

    /**
     * 获取后缀
     * @param key 完整的key
     * @return 后缀
     */
    public static Suffix getSuffix(byte[] key) {
        if (null == key || key.length <= PREFIX_SIZE) {
            return Suffix.UNKNOWN;
        }
        int suffixSize = key.length - PREFIX_SIZE;
        byte[] suffix = new byte[suffixSize];
        System.arraycopy(key, PREFIX_SIZE, suffix, 0, suffixSize);
        return Suffix.parse(suffix);
    }
}