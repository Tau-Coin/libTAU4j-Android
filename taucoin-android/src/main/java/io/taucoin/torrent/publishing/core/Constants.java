package io.taucoin.torrent.publishing.core;

import java.math.BigInteger;

/**
 * core模块用到的所有常量定义类
 */
public class Constants {
    // 1 COIN
    public static final BigInteger COIN = new BigInteger("100000000", 10);
    // 默认社区链总共的coin值 1000000 COIN
    public static final BigInteger TOTAL_COIN = new BigInteger("1000000", 10).multiply(COIN);
    // 给朋友空投币的数量 10 COIN
    public static final BigInteger AIRDROP_COIN = new BigInteger("10", 10).multiply(COIN);
    // 小数保留2位(0.01 COIN), 最大为8
    public static final int COIN_DECIMAL = 2;
    // 最小值交易费 0.01 COIN
    public static final BigInteger MIN_FEE = COIN.divide(new BigInteger("1", 10).pow(COIN_DECIMAL));

    // 社区名最大字节长度
    public static final int MAX_COMMUNITY_NAME_LENGTH = 24;
    // APP分享URL
    public static final String APP_SHARE_URL = "https://taucoin.io/androidwallet.html";

    // Chain link中bs默认数
    public static final int CHAIN_LINK_BS_LIMIT = 10;

    public static final int ONLINE_HOURS = 12;

    // 最大在线统计次数
    public static final int MAX_ONLINE_COUNT = 99;
}
