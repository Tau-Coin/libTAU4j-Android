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
    // 创建新社区空投币的数量 1% TOTAL_COIN
    public static final BigInteger CREATION_AIRDROP_COIN = TOTAL_COIN.divide(new BigInteger("100", 10));
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

    // 社区成员在线时间在多少小时内都认为在线
    public static final int ONLINE_HOURS = 12;

    // 区块未过期的数目（半年）
    public static final int BLOCKS_NOT_PERISHABLE = 288 * 180;

    // 自动更新账户开始点（7天）
    public static final int AUTO_RENEWAL_MAX_BLOCKS = 288 * 7;

    // 自动更新账户周期（1天）（给自己发转账交易）
    public static final int AUTO_RENEWAL_PERIOD_BLOCKS = 288;

    // 默认社区链平均出块时间，单位:s
    public static final int BLOCK_IN_AVG = 300;

    // 最大在线统计次数
    public static final int MAX_ONLINE_COUNT = 99;

    // 昵称长度限制 单位：byte
    public static final int NICKNAME_LENGTH = 24;

    // 统计老数据多久清理一次，单位：秒
    public static final int STATISTICS_CLEANING_PERIOD = 10 * 60;

    // 统计显示周期，单位：秒
    public static final int STATISTICS_DISPLAY_PERIOD = 60;

    // 保存的字体大小缩放比例
    public static final String PREF_KEY_FONT_SCALE_SIZE = "pref_key_font_scale_size";
}
