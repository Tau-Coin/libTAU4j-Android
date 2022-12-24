package io.taucoin.news.publishing.core.model.data;

import androidx.annotation.NonNull;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Member;

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
