package io.taucbd.news.publishing.core.model.data;

import androidx.annotation.NonNull;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Member;

/**
 * Room: 成员和Pending金额
 */
public class MemberAndAmount extends Member {
    public long txIncomePending;            // 未上链转账交易收入
    public long txExpenditurePending;       // 未上链转账交易支出

    public MemberAndAmount(@NonNull String chainID, @NonNull String publicKey) {
        super(chainID, publicKey);
    }
}
