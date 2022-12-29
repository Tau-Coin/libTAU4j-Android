package io.taucoin.news.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.taucoin.news.publishing.core.model.data.BlockAndTx;
import io.taucoin.news.publishing.core.model.data.BlockStatistics;
import io.taucoin.news.publishing.core.model.data.ChainStatus;
import io.taucoin.news.publishing.core.storage.sqlite.entity.BlockInfo;

/**
 * Room:BlockInfo操作接口
 */
@Dao
public interface BlockDao {

    String QUERY_BLOCK = "SELECT * FROM Blocks " +
            " WHERE chainID = :chainID and blockHash= :blockHash";

    String QUERY_BLOCKS = "SELECT * FROM Blocks " +
            " WHERE chainID = :chainID and blockNumber= :blockNumber";

    String QUERY_COMMUNITY_PUBLIC_KEY_ORDER = "SELECT publicKey FROM Members" +
            " WHERE chainID = :chainID AND (balance > 0 OR nonce > 0)" +
            " ORDER BY balance DESC, nonce DESC, publicKey COLLATE UNICODE DESC";

    String QUERY_CHAIN_STATUS = "SELECT a.syncingHeadBlock, a.headBlock, a.consensusBlock, a.difficulty, a.forkPoint," +
            " c.peerBlocks, d.totalPeers, d.totalCoin, (e.consensusBalance-e.totalPendingCoins) as balance, e.power, e.balUpdateTime" +
            " FROM Communities a" +
            " LEFT JOIN (SELECT bb.chainID, count(*) AS peerBlocks" +
            " FROM Blocks bb" +
            " LEFT JOIN Communities cc ON bb.chainID = cc.chainID" +
            " WHERE bb.chainID = :chainID AND bb.blockNumber >= 0" +
            " AND miner =(" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND status = 1" +
//            " AND (cc.tailBlock <= bb.blockNumber)" +
            ") AS c " +
            " ON a.chainID = c.chainID" +
            " LEFT JOIN (SELECT mm.chainID, count(*) totalPeers, SUM(consensusBalance) AS totalCoin FROM Members mm" +
            " WHERE mm.chainID = :chainID AND mm.publicKey IN (" + QUERY_COMMUNITY_PUBLIC_KEY_ORDER + " LIMIT :limit)" +
            ") AS d" +
            " ON a.chainID = d.chainID" +
            " LEFT JOIN (SELECT chainID, publicKey, consensusBalance, totalPendingCoins, power, balUpdateTime FROM Members WHERE chainID = :chainID" +
            " AND publicKey = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")) AS e" +
            " ON a.chainID = e.chainID" +
            " WHERE a.chainID = :chainID";

    String QUERY_CHAIN_SYNC_STATUS = "SELECT * FROM Blocks" +
            " WHERE chainID = :chainID AND status = 0" +
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
            " ) AS a" +
            " ON a.chainID = b.chainID" +
            " LEFT JOIN " +
            " (SELECT chainID, MAX(createTime) AS maxCreateTime" +
            " FROM Blocks WHERE chainID =:chainID" +
            " AND miner != (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")) AS c" +
            " ON a.chainID = c.chainID";

    String QUERY_BLOCKS_LIMIT = "SELECT * FROM Blocks " +
            " WHERE chainID = :chainID AND status == 1" +
            " ORDER BY blockNumber DESC LIMIT :num";

    String UPDATE_ALL_OFF_CHAIN_BLOCKS = "UPDATE Blocks SET status = 0" +
            " WHERE chainID = :chainID AND blockNumber >= :headBlock";

    String QUERY_LATEST_BLOCK = "SELECT * FROM Blocks" +
            " WHERE chainID = :chainID AND status == 1" +
            " ORDER BY blockNumber DESC LIMIT 1";

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

    @Query(QUERY_BLOCK)
    Observable<BlockInfo> observeBlock(String chainID, String blockHash);

    @Query(QUERY_BLOCKS)
    List<BlockInfo> getBlocks(String chainID, long blockNumber);

    /**
     * 观察链上状态信息
     */
    @Query(QUERY_CHAIN_STATUS)
    ChainStatus queryChainStatus(String chainID, int limit);

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

    @Query(QUERY_BLOCKS_LIMIT)
    Flowable<List<BlockInfo>> observerCommunityTopBlocks(String chainID, int num);

    /**
     * 处理由于回滚未置为offChain状态的区块
     * @param chainID
     * @param headBlock
     */
    @Query(UPDATE_ALL_OFF_CHAIN_BLOCKS)
    int updateAllOffChainBlocks(String chainID, long headBlock);

    @Query(QUERY_LATEST_BLOCK)
    BlockInfo queryLatestBlock(String chainID);
}
