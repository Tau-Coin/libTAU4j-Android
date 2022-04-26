package io.taucoin.torrent.publishing.core.model.data;

import java.util.List;

/**
 * 社区成员相关统计
 */
public class AccessList {
    private List<String> connected;       // 访问列表
    private List<String> gossip;          // gossip列表

    public int getConnectedSize() {
        return null == connected ? 0 : connected.size();
    }

    public void setConnected(List<String> connected) {
        this.connected = connected;
    }

    public int getGossipSize() {
        return null == gossip ? 0 : gossip.size();
    }

    public void setGossip(List<String> gossip) {
        this.gossip = gossip;
    }

    public List<String> getConnected() {
        return connected;
    }

    public List<String> getGossip() {
        return gossip;
    }
}
