package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Observable;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxQueue;

/**
 * Room:TxQueue操作接口
 */
@Dao
public interface TxQueueDao {
    String QUERY_COMMUNITY_TX_QUEUE = "SELECT * FROM" +
            " (SELECT tq.*, t.timestamp, t.sendCount," +
            " (CASE WHEN t.status IS NULL THEN -1 ELSE t.status END) AS status" +
            " FROM TxQueues tq" +
            " LEFT JOIN (SELECT SUM(txStatus) AS status, COUNT(txID) AS sendCount," +
            " MAX(timestamp) AS timestamp, queueID From Txs" +
            " WHERE chainID = :chainID AND senderPk = :senderPk AND txType = 2" +
            " GROUP BY queueID) AS t" +
            " ON tq.queueID = t.queueID" +
            " WHERE tq.chainID = :chainID AND tq.senderPk = :senderPk)" +
            " WHERE status <= 0 ORDER BY queueID";

    String QUERY_QUEUE_FIRST_TX = QUERY_COMMUNITY_TX_QUEUE + " LIMIT 1";

    String QUERY_NEED_WIRING_TX_COMMUNITIES = "SELECT chainID FROM" +
            " (SELECT tq.chainID, tq.queueID," +
            " (CASE WHEN t.status IS NULL THEN -1 ELSE t.status END) AS status" +
            " FROM TxQueues tq" +
            " LEFT JOIN (SELECT SUM(txStatus) AS status, queueID From Txs" +
            " WHERE senderPk = :senderPk AND txType = 2" +
            " GROUP BY queueID) AS t" +
            " ON tq.queueID = t.queueID" +
            " WHERE tq.senderPk = :senderPk)" +
            " WHERE status <= 0" +
            " GROUP BY chainID" +
            " ORDER BY queueID";

    /**
     * 入队列
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addQueue(TxQueue tx);

    /**
     * 更新交易队列
     */
    @Update
    void updateQueue(TxQueue tx);

    @Query(QUERY_COMMUNITY_TX_QUEUE)
    Observable<List<TxQueueAndStatus>> observeCommunityTxQueue(String chainID, String senderPk);

    @Query(QUERY_QUEUE_FIRST_TX)
    TxQueueAndStatus getQueueFirstTx(String chainID, String senderPk);

    @Query(QUERY_NEED_WIRING_TX_COMMUNITIES)
    List<String> getNeedWiringTxCommunities(String senderPk);
}