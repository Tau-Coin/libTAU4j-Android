package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 社区成员和最近活跃时间联合查询
 */
public class MemberAndTime extends Member {
    public long latestTxTime;
    public long latestMiningTime;

    public MemberAndTime(@NonNull String chainID, @NonNull String publicKey) {
        super(chainID, publicKey);
    }
}
