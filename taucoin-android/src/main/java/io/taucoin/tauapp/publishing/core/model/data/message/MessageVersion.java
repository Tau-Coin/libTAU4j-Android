package io.taucoin.tauapp.publishing.core.model.data.message;

/**
 * 消息版本
 */
public enum MessageVersion {
    VERSION1(1),
    VERSION2(2); // 添加referral节点

    private int version;
    MessageVersion(int version) {
        this.version = version;
    }

    public int getV() {
        return version;
    }
}
