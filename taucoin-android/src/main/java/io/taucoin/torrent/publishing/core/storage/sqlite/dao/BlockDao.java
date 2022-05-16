package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.BlockAndTx;
import io.taucoin.torrent.publishing.core.model.data.BlockStatistics;
import io.taucoin.torrent.publishing.core.model.data.ChainStatus;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.BlockInfo;

/**
 * Room:BlockInfo操作接口
 */
@Dao
public interface BlockDao {

    String QUERY_BLOCK = "SELECT * FROM Blocks " +
            " WHERE chainID = :chainID and blockHash= :blockHash";

    String QUERY_BLOCKS = "SELECT * FROM Blocks " +
            " WHERE chainID = :chainID and blockNumber= :blockNumber";

    String QUERY_CHAIN_STATUS = "SELECT a.syncingHeadBlock, a.headBlock, a.tailBlock, a.consensusBlock, a.difficulty," +
            " c.peerBlocks, c.totalRewards, d.totalPeers, d.totalCoin, e.balance" +
            " FROM Communities a" +
            " LEFT JOIN (SELECT bb.chainID, count(*) AS peerBlocks, SUM(rewards) AS totalRewards" +
            " FROM Blocks bb" +
            " LEFT JOIN Communities cc ON bb.chainID = cc.chainID" +
            " WHERE bb.chainID = :chainID AND bb.blockNumber > 0" +
            " AND miner =(" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND status = 1" +
            " AND (cc.tailBlock <= bb.blockNumber)) AS c " +
            " ON a.chainID = c.chainID" +
            " LEFT JOIN (SELECT mm.chainID, count(*) totalPeers, SUM(balance) AS totalCoin FROM Members mm" +
            " LEFT JOIN Communities cc ON mm.chainID = cc.chainID" +
            " WHERE mm.chainID = :chainID AND "+ MemberDao.WHERE_ON_CHAIN +") AS d" +
            " ON a.chainID = d.chainID" +
            " LEFT JOIN (SELECT chainID, publicKey, balance FROM Members WHERE chainID = :chainID" +
            " AND publicKey = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")) AS e" +
            " ON a.chainID = e.chainID" +
            " WHERE a.chainID = :chainID";

    String QUERY_CHAIN_SYNC_STATUS = "SELECT * FROM Blocks" +
            " WHERE chainID = :chainID AND status = 0" +
//            " AND blockNumber >" +
//            " (SELECT headBlock FROM Communities WHERE chainID = :chainID)" +
            " ORDER BY blockNumber";

    String QUERY_COMMUNITY_BLOCKS = "SELECT * FROM Blocks" +
            " WHERE chainID = :chainID" +
            " ORDER BY blockNumber DESC" +
            " LIMIT :loadSize OFFSET :startPosition";

    String QUERY_BLOCKS_STATISTICS = "SELECT a.onChain, b.total, c.maxCreateTime" +
            " FROM " +
            " (SELECT chainID, COUNT(blockHash) AS total" +
            " FROM Blocks WHERE chainID =:chainID) AS b" +
            " LEFT JOIN " +
            " (SELECT chainID, COUNT(blockHash) AS onChain" +
            " FROM Blocks WHERE chainID =:chainID AND status == 1" +
            " LIMIT " + Constants.BLOCKS_NOT_PERISHABLE +
            " ) AS a" +
            " ON a.chainID = b.chainID" +
            " LEFT JOIN " +
            " (SELECT chainID, MAX(createTime) AS maxCreateTime" +
            " FROM Blocks WHERE chainID =:chainID" +
            " AND miner != (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")) AS c" +
            " ON a.chainID = c.chainID";

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

    @Query(QUERY_BLOCKS)
    List<BlockInfo> getBlocks(String chainID, long blockNumber);

    /**
     * 观察链上状态信息
     */
    @Query(QUERY_CHAIN_STATUS)
    Flowable<ChainStatus> observerChainStatus(String chainID);

    /**
     * 观察链上同步状态信息
     */
    @Query(QUERY_CHAIN_SYNC_STATUS)
    @Transaction
    Flowable<List<BlockAndTx>> observeCommunitySyncStatus(String chainID);

    @Query(QUERY_COMMUNITY_BLOCKS)
    @Transaction
    List<BlockAndTx> queryCommunityBlocks(String chainID, int startPosition, int loadSize);

    @Query(QUERY_BLOCKS_STATISTICS)
    Flowable<BlockStatistics> getBlocksStatistics(String chainID);
}