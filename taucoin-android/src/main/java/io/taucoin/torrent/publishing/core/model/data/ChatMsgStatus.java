package io.taucoin.torrent.publishing.core.model.data;

/**
 * 消息状态枚举
 */
public enum ChatMsgStatus {
    SENT(0, "Try to Send"),
    SENT_INTERNET(1, "Sent to Internet"),
    ARRIVED_SWARM(2, "Arrived on Receiver Swarm"),
    CONFIRMED(3, "Showed on Receiver Device");

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
