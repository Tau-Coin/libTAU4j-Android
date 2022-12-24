package io.taucoin.news.publishing.core.model.data;

/**
 * 最近50个区块交易费统计
 */
public class TxFreeStatistics {
    private int total;               // 总区块数
    private long totalFee;           // 总交易费
    private int wiringCount;         // 转账交易数
    private int txsCount;            // 转账交易和news总数

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public long getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(long totalFee) {
        this.totalFee = totalFee;
    }

    public int getWiringCount() {
        return wiringCount;
    }

    public void setWiringCount(int wiringCount) {
        this.wiringCount = wiringCount;
    }

    public int getTxsCount() {
        return txsCount;
    }

    public void setTxsCount(int txsCount) {
        this.txsCount = txsCount;
    }
}
