package io.taucoin.torrent.publishing.core.model.data;

import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;

/**
 * Room: 成员和社区联合查询
 */
public class CommunityAndMember extends Community {
    public long balance;
    public long power;
    public long nonce;
    public long blockNumber;
    public int joined;

    /**
     * 判断社区成员是否是read only
     * 判断条件：
     * 1、区块余额和power都小于等于0
     * 2、最新区块和成员状态时的区块相差Constants.BLOCKS_NOT_PERISHABLE
     * @return read only
     */
    public boolean isReadOnly() {
        return (balance <= 0 && power <= 0) || (headBlock - blockNumber >= Constants.BLOCKS_NOT_PERISHABLE);
    }

    public boolean isJoined() {
        return joined == 1;
    }
}
