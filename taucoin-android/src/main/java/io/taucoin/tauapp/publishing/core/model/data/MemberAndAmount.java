package io.taucoin.tauapp.publishing.core.model.data;

import androidx.annotation.NonNull;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Member;

/**
 * Room: 成员和Pending金额
 */
public class MemberAndAmount extends Member {
    public long amount;

    public MemberAndAmount(@NonNull String chainID, @NonNull String publicKey) {
        super(chainID, publicKey);
    }
}
