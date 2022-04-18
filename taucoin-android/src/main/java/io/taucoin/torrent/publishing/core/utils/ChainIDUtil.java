package io.taucoin.torrent.publishing.core.utils;

import org.libTAU4j.ChainURL;

/**
 * ChainID工具类
 */

public class ChainIDUtil {

    public static String decode(byte[] chainID) {
        if (chainID != null && chainID.length > 8) {
            return ChainURL.chainIDBytesToString(chainID);
        }
        return Utils.textBytesToString(chainID);
    }

    public static byte[] encode(String chainID) {
        if (chainID.length() > 16) {
            return ChainURL.chainIDStringToBytes(chainID);
        }

        return Utils.textStringToBytes(chainID);
    }

    public static String getName(String chainID) {
        if (chainID != null && chainID.length() > 16) {
            return chainID.substring(16);
        }
        return chainID;
    }

    public static String getName(byte[] chainID) {
        return getName(decode(chainID));
    }

    public static String getCoinName(String chainID) {
        String name = getName(chainID);
        String firstLetters = StringUtil.getFirstLettersOfName(name);
        return firstLetters + "coin";
    }

    /**
     * 获取默认截取的名字
     * 规则：以字母开头，然后取前3个数字
     * @param chainID 截取前的名字
     * @return 截取后的名字
     */
    public static String getCode(String chainID) {
        StringBuilder defaultName = new StringBuilder();
        if(StringUtil.isNotEmpty(chainID)){
            for (int i = 0; i < chainID.length(); i++) {
                if (!Character.isDigit(chainID.charAt(i))) {
                    defaultName.append(chainID.charAt(i));
                    break;
                }
            }
            for (int i = 0; i < chainID.length(); i++) {
                if (Character.isDigit(chainID.charAt(i))) {
                    defaultName.append(chainID.charAt(i));
                    if(defaultName.length() == 4) {
                        break;
                    }
                }
            }
        }
        return defaultName.toString().toUpperCase();
    }
}