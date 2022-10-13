package io.taucoin.tauapp.publishing.core.model.data;

import androidx.annotation.NonNull;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Community;

/**
 * Room: 成员和社区联合查询
 */
public class CommunityAndMember extends Community {
    public long balance;
    public long nonce;
    public int joined;
    public int msgUnread;
    public int newsUnread;
    public int notExpired;
    public int nearExpired;

    public CommunityAndMember(@NonNull String chainID, @NonNull String communityName) {
        super(chainID, communityName);
    }

    public boolean noBalance() {
        return balance <= 0;
    }

    public boolean isJoined() {
        return joined == 1;
    }

    public boolean onChain() {
        return (balance > 0 || nonce > 0) && notExpired == 1;
    }

    public boolean nearExpired() {
        return onChain() && nearExpired == 1;
    }
}