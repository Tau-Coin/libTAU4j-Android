package io.taucoin.torrent.publishing.core.model.data;

/**
 * 共识点信息
 */
public class ConsensusInfo {

    private String hash;
    private long number;
    private long votes;

    public ConsensusInfo(String hash, long number, long votes) {
        this.hash = hash;
        this.number = number;
        this.votes = votes;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public long getVotes() {
        return votes;
    }

    public void setVotes(long votes) {
        this.votes = votes;
    }
}
