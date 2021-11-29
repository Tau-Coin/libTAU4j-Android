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
    public long lastSeenTime;
    public long headBlock;

    public MemberAndFriend(@NonNull String chainID, @NonNull String publicKey) {
        super(chainID, publicKey);
    }

    /**
     * 判断社区成员是否是read only
     * 判断条件：
     * 1、区块余额和power都小于等于0
     * 2、最新区块和成员状态时的区块相差Constants.BLOCKS_NOT_PERISHABLE
     * @return read only
     */
    public boolean isReadOnly() {
        return (balance <= 0 && power <= 0) ||
                (headBlock - blockNumber >= Constants.BLOCKS_NOT_PERISHABLE);
    }
}
