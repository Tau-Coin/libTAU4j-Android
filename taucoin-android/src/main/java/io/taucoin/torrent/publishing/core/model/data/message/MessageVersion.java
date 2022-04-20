package io.taucoin.torrent.publishing.core.model.data.message;

/**
 * 消息版本
 */
public enum MessageVersion {
    VERSION1(1),
    VERSION2(2); // 添加转账

    private int version;
    MessageVersion(int version) {
        this.version = version;
    }

    public int getV() {
        return version;
    }
}
