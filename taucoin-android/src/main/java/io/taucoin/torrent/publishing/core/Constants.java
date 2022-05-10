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
    // 创建新社区空投币的数量 1000 TOTAL_COIN
    public static final BigInteger CREATION_AIRDROP_COIN = new BigInteger("1000", 10).multiply(COIN);
    // 小数保留2位(0.01 COIN), 最大为8
    public static final int COIN_DECIMAL = 2;
    // 最小值交易费 0.01 COIN
    public static final BigInteger MIN_FEE = COIN.divide(new BigInteger("10", 10).pow(COIN_DECIMAL));
    // 转账交易最小为1 COIN
    public static final BigInteger WIRING_MIN_FEE = COIN;
    // 消息交易最小为10 COIN
    public static final BigInteger NEWS_MIN_FEE = new BigInteger("10", 10).multiply(COIN);
    // note交易最小为0.01 COIN
    public static final BigInteger NOTES_MIN_FEE = MIN_FEE;

    // 社区名最大字节长度
    public static final int MAX_COMMUNITY_NAME_LENGTH = 24;
    // APP分享URL
    public static final String APP_HOME_URL = "https://taucoin.io";

    public static final String APP_SHARE_URL = "https://www.taucoin.io/download";

    // 官方telegram链接
    public static final String OFFICIAL_TELEGRAM_URL = "https://t.me/taucoin";

    // Chain link中bs默认数
    public static final int CHAIN_LINK_BS_LIMIT = 5;

    public static final int AIRDROP_TX_BS_LIMIT = 1;

    public static final int AIRDROP_LINK_BS_LIMIT = 2;

    // 区块未过期的数目（半年）
    public static final int BLOCKS_NOT_PERISHABLE = 288 * 30;

    // 自动更新账户开始点（7天）
    public static final int AUTO_RENEWAL_MAX_BLOCKS = 288 * 7;

    // 自动更新账户周期（1天）（给自己发转账交易）
    public static final int AUTO_RENEWAL_PERIOD_BLOCKS = 288;

    // 发送失败消息重发时间 (24Hours)
    public static final int MSG_RESEND_PERIOD = 24;

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

    // 交易最大字节大小
    public static final int TX_MAX_BYTE_SIZE = 500;

    // 消息最大字节大小
    public static final int MSG_MAX_BYTE_SIZE = 821;

}
