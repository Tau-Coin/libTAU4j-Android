package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.Nullable;

/**
 * 共识点信息
 */
public class ConsensusInfo implements Comparable<ConsensusInfo> {

    private String hash;
    private long number;
    private Long votes;

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

    public Long getVotes() {
        return votes;
    }

    public void setVotes(Long votes) {
        this.votes = votes;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof ConsensusInfo && (o == this || (
                number == (((ConsensusInfo)o).number) &&
                        votes == (((ConsensusInfo)o).votes)));
    }

    @Override
    public int compareTo(ConsensusInfo o) {

        if (this.getVotes() > o.getVotes()) {
            return -1;
        } else if (this.getVotes().equals(o.getVotes())) {
            return 0;
        } else {
            return 1;
        }
    }
}
