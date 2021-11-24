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
}