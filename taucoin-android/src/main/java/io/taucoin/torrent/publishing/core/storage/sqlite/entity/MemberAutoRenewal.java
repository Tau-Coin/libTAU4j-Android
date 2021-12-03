package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import androidx.annotation.NonNull;

/**
 * 自动更新用户账户信息类
 */
public class MemberAutoRenewal extends Member {
    public String seed;
    public String count;

    public MemberAutoRenewal(@NonNull String chainID, @NonNull String publicKey) {
        super(chainID, publicKey);
    }
}
