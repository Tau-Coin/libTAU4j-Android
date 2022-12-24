package io.taucoin.news.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucoin.news.publishing.core.storage.sqlite.entity.BlockInfo;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Tx;

/**
 * 数据库存储区块的实体类
 */
public class BlockAndTx extends BlockInfo {

    @Relation(parentColumn = "txID",
            entityColumn = "txID")
    public Tx tx;

    public BlockAndTx(@NonNull String chainID, @NonNull String blockHash, long blockNumber, @NonNull String miner,
                      long rewards, long difficulty, int status, long timestamp, String previousBlockHash, String txID) {
        super(chainID, blockHash, blockNumber, miner, rewards, difficulty, status, timestamp, previousBlockHash, txID);
    }
}