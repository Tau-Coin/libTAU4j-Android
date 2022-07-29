package io.taucoin.torrent.publishing.core.model.data;

import java.util.List;

/**
 * 社区成员相关统计
 */
public class AccessList {
    private List<String> connected;       // 访问列表

    public int getConnectedSize() {
        return null == connected ? 0 : connected.size();
    }

    public void setConnected(List<String> connected) {
        this.connected = connected;
    }

    public List<String> getConnected() {
        return connected;
    }
}
