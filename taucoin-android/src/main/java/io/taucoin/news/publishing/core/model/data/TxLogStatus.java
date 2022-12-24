package io.taucoin.news.publishing.core.model.data;

/**
 * 交易日志状态枚举
 */
public enum TxLogStatus {
    SENT(0, "Try to Send"),
    ARRIVED_SWARM(1, "Arrived on swarm");

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
