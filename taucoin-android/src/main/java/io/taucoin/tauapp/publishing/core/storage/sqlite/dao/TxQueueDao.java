package io.taucoin.tauapp.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.taucoin.tauapp.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.tauapp.publishing.core.model.data.AirdropHistory;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.TxQueue;

/**
 * Room:TxQueue操作接口
 */
@Dao
public interface TxQueueDao {
    String QUERY_COMMUNITY_TX_QUEUE_SELECT = "SELECT * FROM" +
            " (SELECT tq.*, t.timestamp, t.sendCount, t.nonce," +
            " (CASE WHEN t.status IS NULL THEN -1 ELSE t.status END) AS status, " +
            " (CASE WHEN t.sendStatus IS NULL THEN -1 ELSE t.sendStatus END) AS sendStatus" +
            " FROM TxQueues tq" +
            " LEFT JOIN (SELECT txStatus AS status, COUNT(txID) AS sendCount," +
            " timestamp, nonce, queueID, sendStatus" +
            " From Txs" +
            " WHERE chainID = :chainID AND senderPk = :senderPk AND queueID IS NOT NULL" +
            " AND txStatus <= 0 AND version > 0" +
            " GROUP BY queueID) AS t" +
            " ON tq.queueID = t.queueID" +
            " WHERE tq.chainID = :chainID AND tq.senderPk = :senderPk)";

    String QUERY_COMMUNITY_TX_QUEUE = QUERY_COMMUNITY_TX_QUEUE_SELECT +
            " ORDER BY nonce";
            //" ORDER BY fee DESC, queueID ASC";

    String QUERY_QUEUE_FIRST_TX = QUERY_COMMUNITY_TX_QUEUE + " LIMIT 1";

    String QUERY_NONCE_FIRST_TX = "SELECT * FROM" +
            " (SELECT tq.*, t.timestamp, t.sendCount, t.nonce," +
            " (CASE WHEN t.status IS NULL THEN -1 ELSE t.status END) AS status, " +
            " (CASE WHEN t.sendStatus IS NULL THEN -1 ELSE t.sendStatus END) AS sendStatus" +
            " FROM TxQueues tq" +
            " LEFT JOIN (SELECT txStatus AS status, COUNT(txID) AS sendCount," +
            " timestamp, nonce, queueID, sendStatus" +
            " From Txs" +
            " WHERE chainID = :chainID AND senderPk = :senderPk AND nonce = :nonce) AS t" +
            " ON tq.queueID = t.queueID" +
            " WHERE tq.chainID = :chainID AND tq.senderPk = :senderPk)";

    String QUERY_TX_QUEUE_BY_ID = "SELECT * FROM" +
            " (SELECT tq.*, t.timestamp, t.sendCount, t.nonce," +
            " (CASE WHEN t.status IS NULL THEN -1 ELSE t.status END) AS status," +
            " (CASE WHEN t.sendStatus IS NULL THEN -1 ELSE t.sendStatus END) AS sendStatus" +
            " FROM TxQueues tq" +
            " LEFT JOIN (SELECT SUM(txStatus) AS status, COUNT(txID) AS sendCount," +
            " MAX(timestamp) AS timestamp, MAX(nonce) AS nonce, queueID, MAX(sendStatus) AS sendStatus" +
            " From Txs" +
            " WHERE queueID = :queueID" +
            " GROUP BY queueID) AS t" +
            " ON tq.queueID = t.queueID" +
            " WHERE tq.queueID = :queueID)";

    String QUERY_NEED_WIRING_TX_COMMUNITIES = "SELECT chainID FROM" +
            " (SELECT tq.chainID, tq.queueID," +
            " (CASE WHEN t.status IS NULL THEN -1 ELSE t.status END) AS status" +
            " FROM TxQueues tq" +
            " LEFT JOIN (SELECT SUM(txStatus) AS status, queueID From Txs" +
            " WHERE senderPk = :senderPk AND queueID IS NOT NULL" +
            " AND txStatus <= 0 AND version > 0" +
            " GROUP BY queueID) AS t" +
            " ON tq.queueID = t.queueID" +
            " WHERE tq.senderPk = :senderPk)" +
            " WHERE status <= 0" +
            " GROUP BY chainID";

    String QUERY_AIRDROP_COUNT_ON_CHAIN = "SELECT count(*) FROM" +
            " (SELECT tq.chainID, tq.queueID," +
            " (CASE WHEN t.status IS NULL THEN -1 ELSE t.status END) AS status," +
            " (CASE WHEN t.sendStatus IS NULL THEN -1 ELSE t.sendStatus END) AS sendStatus" +
            " FROM TxQueues tq" +
            " LEFT JOIN (SELECT SUM(txStatus) AS status, queueID, MAX(sendStatus) AS sendStatus" +
            " From Txs" +
            " WHERE senderPk = :senderPk AND queueID IS NOT NULL" +
            " GROUP BY queueID) AS t" +
            " ON tq.queueID = t.queueID" +
            " WHERE tq.senderPk = :senderPk AND tq.chainID = :chainID" +
            " AND queueTime >= :currentTime AND queueType = 1)" +
            " WHERE status > 0";

    String QUERY_AIRDROP_HISTORY_ON_CHAIN = "SELECT queueID, receiverPk FROM" +
            " (SELECT tq.chainID, tq.queueID, tq.receiverPk," +
            " (CASE WHEN t.status IS NULL THEN -1 ELSE t.status END) AS status," +
            " (CASE WHEN t.sendStatus IS NULL THEN -1 ELSE t.sendStatus END) AS sendStatus" +
            " FROM TxQueues tq" +
            " LEFT JOIN (SELECT SUM(txStatus) AS status, queueID, MAX(sendStatus) AS sendStatus" +
            " From Txs" +
            " WHERE senderPk = :senderPk AND queueID IS NOT NULL" +
            " GROUP BY queueID) AS t" +
            " ON tq.queueID = t.queueID" +
            " WHERE tq.senderPk = :senderPk AND tq.chainID = :chainID" +
            " AND queueTime >= :currentTime AND queueType = 1)" +
            " WHERE status > 0";

    String QUERY_AIRDROP_TX_QUEUE = "SELECT * FROM TxQueues" +
            " WHERE chainID = :chainID AND senderPk = :currentPk" +
            " AND receiverPk = :friendPk" +
            " AND queueType = 1";

    String QUERY_AIRDROP_COUNT = "SELECT count(*) FROM TxQueues" +
            " WHERE chainID = :chainID AND senderPk = :currentPk" +
            " AND queueTime >= :currentTime AND queueType = 1";

    String QUERY_REFERRAL_COUNT = "SELECT count(*) FROM TxQueues" +
            " WHERE chainID = :chainID AND senderPk = :currentPk" +
            " AND receiverPk = :friendPk " +
            " AND queueTime >= :currentTime AND queueType = 2";

    /**
     * 入队列
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addQueue(TxQueue tx);

    /**
     * 删除交易队列
     */
    @Delete
    void deleteQueue(TxQueue tx);

    /**
     * 更新交易队列
     */
    @Update
    void updateQueue(TxQueue tx);

    @Query(QUERY_COMMUNITY_TX_QUEUE)
    Flowable<List<TxQueueAndStatus>> observeCommunityTxQueue(String chainID, String senderPk);

    @Query(QUERY_COMMUNITY_TX_QUEUE)
    List<TxQueueAndStatus> getCommunityTxQueue(String chainID, String senderPk);

    @Query(QUERY_NONCE_FIRST_TX)
    TxQueueAndStatus getNonceFirstTx(String chainID, String senderPk, long nonce);

    @Query(QUERY_QUEUE_FIRST_TX)
    TxQueueAndStatus getQueueFirstTx(String chainID, String senderPk);

    @Query(QUERY_TX_QUEUE_BY_ID)
    TxQueueAndStatus getTxQueueByID(long queueID);

    @Query(QUERY_NEED_WIRING_TX_COMMUNITIES)
    List<String> getNeedWiringTxCommunities(String senderPk);

    @Query(QUERY_AIRDROP_TX_QUEUE)
    TxQueue getAirdropTxQueue(String chainID, String currentPk, String friendPk);

    @Query(QUERY_AIRDROP_COUNT)
    int getAirdropCount(String chainID, String currentPk, long currentTime);

    @Query(QUERY_REFERRAL_COUNT)
    int getReferralCount(String chainID, String currentPk, String friendPk, long currentTime);

    @Query(QUERY_AIRDROP_COUNT_ON_CHAIN)
    Flowable<Integer> observeAirdropCountOnChain(String chainID, String senderPk, long currentTime);

    @Query(QUERY_AIRDROP_HISTORY_ON_CHAIN)
    @Transaction
    Flowable<List<AirdropHistory>> observeAirdropHistoryOnChain(String chainID, String senderPk, long currentTime);
}
