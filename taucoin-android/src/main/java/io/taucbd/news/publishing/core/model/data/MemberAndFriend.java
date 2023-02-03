package io.taucbd.news.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Member;
import io.taucbd.news.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 用户和成员联合查询
 */
public class MemberAndFriend extends Member {
    @Relation(parentColumn = "publicKey", entityColumn = "publicKey")
    public User user;
    public int notExpired;

    public MemberAndFriend(@NonNull String chainID, @NonNull String publicKey) {
        super(chainID, publicKey);
    }

    public boolean onChain() {
        return (nonce > 0 || balance > 0) && notExpired == 1;
    }
}
