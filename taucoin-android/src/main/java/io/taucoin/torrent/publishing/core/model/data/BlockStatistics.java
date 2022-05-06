package io.taucoin.torrent.publishing.core.model.data;

/**
 * 社区成员相关统计
 */
public class BlockStatistics {
    private int total;              // 总数目
    private int onChain;            // 上链数目
    private long maxCreateTime;     // 最大区块本地创建时间（不含自己，接受的别人的区块）

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getOnChain() {
        return onChain;
    }

    public void setOnChain(int onChain) {
        this.onChain = onChain;
    }

    public long getMaxCreateTime() {
        return maxCreateTime;
    }

    public void setMaxCreateTime(long maxCreateTime) {
        this.maxCreateTime = maxCreateTime;
    }
}
