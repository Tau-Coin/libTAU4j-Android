package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;

/**
 * Room: 成员和社区联合查询
 */
public class MemberAndCommunity extends Member {
    @Relation(parentColumn = "chainID",
            entityColumn = "chainID")
    public Community community;              // 成员所在社区

    public MemberAndCommunity(@NonNull String chainID, @NonNull String publicKey) {
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
        return (balance <= 0 && power <= 0) || (community != null &&
                community.headBlock - blockNumber >= Constants.BLOCKS_NOT_PERISHABLE);
    }
}
