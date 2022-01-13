package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;

/**
 * 自动更新用户账户信息类
 */
public class MemberAutoRenewal extends Member {
    public String seed;
    public int count;

    public MemberAutoRenewal(@NonNull String chainID, @NonNull String publicKey) {
        super(chainID, publicKey);
    }
}
