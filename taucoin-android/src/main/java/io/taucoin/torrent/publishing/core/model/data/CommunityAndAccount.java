package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;

/**
 * Room: 成员和社区联合查询
 */
public class CommunityAndAccount extends Community {
    public long balance;
    public long nonce;
    public int joined;
    public int msgUnread;

    public CommunityAndAccount(@NonNull String chainID, @NonNull String communityName) {
        super(chainID, communityName);
    }
}
