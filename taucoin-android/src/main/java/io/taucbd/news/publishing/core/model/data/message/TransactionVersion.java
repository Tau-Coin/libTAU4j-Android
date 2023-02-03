package io.taucbd.news.publishing.core.model.data.message;

/**
 * libTAU Transaction版本
 */
public enum TransactionVersion {
    VERSION0(0),
    VERSION1(1);

    private final int version;
    TransactionVersion(int version) {
        this.version = version;
    }

    public int getV() {
        return version;
    }
}
