package io.taucoin.news.publishing.core.model.data;

public class PeersAndInvoked {
    private long timeKey;
    private long timestamp;
    private long peers;
    private long invokedRequests;

    public long getTimeKey() {
        return timeKey;
    }

    public void setTimeKey(long timeKey) {
        this.timeKey = timeKey;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getPeers() {
        return peers;
    }

    public void setPeers(long peers) {
        this.peers = peers;
    }

    public long getInvokedRequests() {
        return invokedRequests;
    }

    public void setInvokedRequests(long invokedRequests) {
        this.invokedRequests = invokedRequests;
    }
}
