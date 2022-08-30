package io.taucoin.tauapp.publishing.core.model.data;

/**
 * 消息状态枚举
 */
public enum ChatMsgStatus {
    SENT(0, "Try to Send"),
    ARRIVED_SWARM(1, "Arrived on Receiver Swarm"),
    CONFIRMED(2, "Showed on Receiver Device");

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
