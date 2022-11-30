package io.taucoin.tauapp.publishing.core;

import java.math.BigInteger;

/**
 * core模块用到的所有常量定义类
 */
public class Constants {
    // 1 COIN
    public static final BigInteger COIN = new BigInteger("1", 10);
    // 默认社区链总共的coin值 1000000 COIN
    public static final BigInteger TOTAL_COIN = new BigInteger("1000000", 10).multiply(COIN);
    // 给朋友空投币的数量 10 COIN
    public static final BigInteger AIRDROP_COIN = new BigInteger("10", 10).multiply(COIN);
    // 创建新社区空投币的数量 1000 TOTAL_COIN
    public static final BigInteger CREATION_AIRDROP_COIN = new BigInteger("1000", 10).multiply(COIN);
    // 最小值交易费 1 COIN
    public static final BigInteger MIN_FEE = COIN;
    // 转账交易最小为1 COIN
    public static final BigInteger WIRING_MIN_FEE = COIN;
    // 消息交易最小为5 COIN
    public static final BigInteger NEWS_MIN_FEE = new BigInteger("5", 10).multiply(COIN);
    // 挖矿奖励
    public static final BigInteger MINING_REWARDS = new BigInteger("10", 10).multiply(COIN);

    // 社区名最大字节长度
    public static final int MAX_COMMUNITY_NAME_LENGTH = 24;
    // APP分享URL
    public static final String APP_HOME_URL = "https://taucoin.io";

    public static final String APP_SHARE_URL = "https://www.taucoin.io/download";

    // 官方telegram链接
    public static final String OFFICIAL_TELEGRAM_URL = "https://t.me/taucoin";

    // Chain link中bs默认数
    public static final int CHAIN_LINK_BS_LIMIT = 5;

    // 链的创建区块区块大小
    public static int CHAIN_EPOCH_BLOCK_SIZE = 50;

    // 社区在线最大账户数据
    public static int MAX_ACCOUNT_SIZE = 860;

    // 社区在线接近过期账户数据
    public static int NEAR_EXPIRED_ACCOUNT_SIZE = MAX_ACCOUNT_SIZE - 100;

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

    // news交易内容最大字节大小309，取整数300
    public static final int NEWS_TX_MAX_BYTE_SIZE = 300;

    // 消息最大字节大小
    public static final int MSG_MAX_BYTE_SIZE = 821;

    public static final BigInteger PERCENTAGE = new BigInteger("100", 10);

    // 默认字体缩放大小
    public static final float DEFAULT_FONT_SCALE_SIZE = 1.1f;

    // 上链
    public static final int STATUS_ON_CHAIN = 2;

	// Settled
    public static final int STATUS_SETTLED = 1;

	// Pending
    public static final int STATUS_PENDING = 0;

}
