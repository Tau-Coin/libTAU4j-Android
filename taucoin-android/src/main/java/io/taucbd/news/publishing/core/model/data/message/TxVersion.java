package io.taucbd.news.publishing.core.model.data.message;

/**
 * 交易版本
 */
public enum TxVersion {
    VERSION1(1);

    private int version;
    TxVersion(int version) {
        this.version = version;
    }

    public int getV() {
        return version;
    }
}
