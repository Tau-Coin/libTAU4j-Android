package io.taucoin.torrent.publishing.core.model.data.message;

/**
 * 消息类型
 */
public enum MessageType {
    TEXT(0),
    AIRDROP(2),
    UNKNOWN(-1);

    private int type;
    MessageType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
