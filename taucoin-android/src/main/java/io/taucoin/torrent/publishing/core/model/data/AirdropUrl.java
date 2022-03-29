package io.taucoin.torrent.publishing.core.model.data;

import org.libTAU4j.ChainURL;

import java.util.ArrayList;
import java.util.List;

import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.ChainUrlUtil;

public class AirdropUrl {

    private String airdropPeer;
    private String chainID;
    private List<String> peers;
    private String chainUrl;
    private String airdropUrl;

    public AirdropUrl(String url) {
        this.airdropUrl = url;
        airdropPeer = url.substring(6, 70);
        chainUrl = url.substring(79);
        ChainURL chainURL = ChainUrlUtil.decode(chainUrl);
        if (chainURL != null) {
            chainID = chainURL.getChainID();
            peers = new ArrayList<>(chainURL.getPeers());
        }
    }

    public String getAirdropPeer() {
        return airdropPeer;
    }

    public String getChainID() {
        return chainID;
    }

    public List<String> getPeers() {
        return peers;
    }

    public String getChainUrl() {
        return chainUrl;
    }

    public String getAirdropUrl() {
        return airdropUrl;
    }
}
