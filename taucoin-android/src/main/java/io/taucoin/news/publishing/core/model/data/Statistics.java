package io.taucoin.news.publishing.core.model.data;

/**
 * 社区成员相关统计
 */
public class Statistics {
    private int total;       // 总数目
    private int onChain;     // 上链数目

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
}
