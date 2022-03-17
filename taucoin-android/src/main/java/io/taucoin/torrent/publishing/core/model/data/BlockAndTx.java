package io.taucoin.torrent.publishing.core.model.data;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.BlockInfo;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;

/**
 * 数据库存储区块的实体类
 */
public class BlockAndTx extends BlockInfo {

    @Relation(parentColumn = "blockHash",
            entityColumn = "blockHash")
    public List<Tx> txs;

    public BlockAndTx(@NonNull String chainID, @NonNull String blockHash, long blockNumber, @NonNull String miner, long rewards, long difficulty, int status, long timestamp, String previousBlockHash) {
        super(chainID, blockHash, blockNumber, miner, rewards, difficulty, status, timestamp, previousBlockHash);
    }
}
