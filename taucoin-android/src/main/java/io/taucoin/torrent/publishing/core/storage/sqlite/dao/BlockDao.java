package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.ChainStatus;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.BlockInfo;

/**
 * Room:BlockInfo操作接口
 */
@Dao
public interface BlockDao {

    String QUERY_BLOCK = "SELECT * FROM Blocks " +
            " WHERE chainID = :chainID and blockHash= :blockHash";

    String QUERY_CHAIN_STATUS = "SELECT a.syncingHeadBlock, a.headBlock, a.tailBlock, a.consensusBlock, a.difficulty," +
            " c.peerBlocks, c.totalRewards, d.totalPeers, d.totalCoin" +
            " FROM Communities a" +
            " LEFT JOIN (SELECT bb.chainID, count(*) AS peerBlocks, SUM(rewards) AS totalRewards" +
            " FROM Blocks bb" +
            " LEFT JOIN Communities cc ON bb.chainID = cc.chainID" +
            " WHERE bb.chainID = :chainID" +
            " AND miner =(" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND status = 1" +
            " AND (cc.headBlock - bb.blockNumber < "+ Constants.BLOCKS_NOT_PERISHABLE +")) AS c " +
            " ON a.chainID = c.chainID" +
            " LEFT JOIN (SELECT mm.chainID, count(*) totalPeers, SUM(balance) AS totalCoin FROM Members mm" +
            " LEFT JOIN Communities cc ON mm.chainID = cc.chainID" +
            " WHERE mm.chainID = :chainID AND "+ MemberDao.WHERE_ON_CHAIN +") AS d" +
            " ON a.chainID = d.chainID" +
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
    @Query(QUERY_CHAIN_STATUS)
    Flowable<ChainStatus> observerChainStatus(String chainID);

    /**
     * 观察链上同步状态信息
     */
    @Query(QUERY_CHAIN_SYNC_STATUS)
    Flowable<List<BlockInfo>> observeCommunitySyncStatus(String chainID);

    @Query(QUERY_COMMUNITY_BLOCKS)
    List<BlockInfo> queryCommunityBlocks(String chainID, int startPosition, int loadSize);
}