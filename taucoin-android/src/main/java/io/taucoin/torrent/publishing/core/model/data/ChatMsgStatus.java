package io.taucoin.torrent.publishing.core.model.data;

/**
 * 消息状态枚举
 */
public enum ChatMsgStatus {
    BUILT(-1,
            "Message built"),
    RESEND(0,
            "Message resent"),
    SYNC_CONFIRMED(1,
            "Message sync confirmed");

    private int status;
    private String statusInfo;
    ChatMsgStatus(int status, String statusInfo) {
        this.status = status;
        this.statusInfo = statusInfo;
    }

    public int getStatus() {
        return status;
    }

    public String getStatusInfo() {
        return statusInfo;
    }

    public static String getStatusInfo(int status) {
        for (ChatMsgStatus s : ChatMsgStatus.values()) {
            if (s.getStatus() == status) {
                return s.getStatusInfo();
            }
        }
        return null;
    }
}
