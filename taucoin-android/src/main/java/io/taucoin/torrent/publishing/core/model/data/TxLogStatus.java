package io.taucoin.torrent.publishing.core.model.data;

/**
 * 交易日志状态枚举
 */
public enum TxLogStatus {
    SENT(0, "Try to Send"),
    SENT_INTERNET(1, "Sent to Internet"),
    ARRIVED_SWARM(2, "Arrived on Community Swarm");

    private int status;
    private String statusInfo;
    TxLogStatus(int status, String statusInfo) {
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
        for (TxLogStatus s : TxLogStatus.values()) {
            if (s.getStatus() == status) {
                return s.getStatusInfo();
            }
        }
        return null;
    }
}
