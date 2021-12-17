package io.taucoin.torrent.publishing.core.model;

/**
 * 时间间隔枚举类
 */
public enum Interval {
    // 单位ms
    BACK_MAIN_LOOP_MIN(200),               // libTAU后台主循环最小时间间隔
    BACK_MAIN_LOOP_MAX(5000),              // libTAU主循环最大时间间隔
    MAIN_LOOP_NO_DATA(86400000),           // 没有可用流量数据，libTAU主循环时间间隔设置为24h

    FORE_DEFAULT_WIFI_FREQUENCY(10),       // libTAU前台默认WIFI网络主循环频率
    FORE_DEFAULT_METERED_FREQUENCY(5),     // libTAU前台默认计费网络主循环频率

    // Worker中失败异常重试频率，单位ms
    INTERVAL_RETRY(1000);

    private int interval;
    Interval(int interval) {
        this.interval = interval;
    }

    public int getInterval() {
        return interval;
    }
}
