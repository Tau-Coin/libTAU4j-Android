package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 用户和成员联合查询
 */
public class MemberAndFriend extends Member {
    @Relation(parentColumn = "publicKey", entityColumn = "publicKey")
    public User user;
    public long headBlock;

    public MemberAndFriend(@NonNull String chainID, @NonNull String publicKey) {
        super(chainID, publicKey);
    }

    public boolean onChain() {
        return nonce > 0 || balance > 0;
    }
}
