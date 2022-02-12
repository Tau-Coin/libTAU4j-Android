package io.taucoin.torrent.publishing.core.model.data;

import java.util.List;

public class AirdropUrl {

    private String airdropPeer;
    private String chainID;
    private List<String> peers;
    private String chainUrl;

    public String getAirdropPeer() {
        return airdropPeer;
    }

    public void setAirdropPeer(String airdropPeer) {
        this.airdropPeer = airdropPeer;
    }

    public String getChainID() {
        return chainID;
    }

    public void setChainID(String chainID) {
        this.chainID = chainID;
    }

    public List<String> getPeers() {
        return peers;
    }

    public void setPeers(List<String> peers) {
        this.peers = peers;
    }

    public String getChainUrl() {
        return chainUrl;
    }

    public void setChainUrl(String chainUrl) {
        this.chainUrl = chainUrl;
    }
}
