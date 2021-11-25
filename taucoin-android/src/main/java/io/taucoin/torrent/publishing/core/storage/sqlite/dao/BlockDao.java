package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.model.data.ChainStatus;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.BlockInfo;

/**
 * Room:BlockInfo操作接口
 */
@Dao
public interface BlockDao {

    String QUERY_BLOCK = "SELECT * FROM Blocks " +
            " WHERE chainID = :chainID and blockHash= :blockHash";

    String QUERY_CHAIN_STATUS_DESC = "SELECT a.* , b.peerBlocks, b.totalRewards, c.totalPeers, c.totalCoin FROM" +
            " (SELECT blockNumber, difficulty FROM Blocks " +
            " WHERE chainID = :chainID AND status = 1 ORDER BY blockNumber DESC" +
            " LIMIT 1) AS a," +
            " (SELECT count(*) peerBlocks, SUM(rewards) AS totalRewards FROM Blocks" +
            " WHERE chainID = :chainID" +
            " AND miner =(" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND status = 1) AS b," +
            " (SELECT count(*) totalPeers, SUM(balance) AS totalCoin FROM Members" +
            " WHERE chainID = :chainID AND balance > 0 AND power > 0) AS c";

    /**
     * 添加用户设备信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addBlock(BlockInfo block);

    @Update
    int updateBlock(BlockInfo block);

    /**
     * 查询区块Block
     */
    @Query(QUERY_BLOCK)
    BlockInfo getBlock(String chainID, String blockHash);

    /**
     * 观察链上状态信息
     */
    @Query(QUERY_CHAIN_STATUS_DESC)
    Flowable<ChainStatus> observerChainStatus(String chainID);
}
