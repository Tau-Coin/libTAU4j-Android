package io.taucbd.news.publishing.core.model.data;

/**
 * 分叉点信息
 */
public class ForkPoint {

    private String hash;
    private long number;

    public ForkPoint(String hash, long number) {
        this.hash = hash;
        this.number = number;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }
}
