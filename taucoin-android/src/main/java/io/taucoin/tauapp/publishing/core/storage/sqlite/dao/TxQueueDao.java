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
import io.taucoin.tauapp.publishing.core.Constants;
import io.taucoin.tauapp.publishing.core.model.data.AirdropHistory;
import io.taucoin.tauapp.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.tauapp.publishing.core.model.data.message.TxType;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.TxQueue;

/**
 * Room:TxQueue操作接口
 */
@Dao
public interface TxQueueDao {
    String QUERY_TX_BY_NONCE = "SELECT * FROM" +
            " (SELECT tq.*, t.timestamp, t.nonce, t.txStatus as status" +
            " FROM TxQueues tq" +
            " LEFT JOIN Txs t" +
            " ON tq.queueID = t.queueID" +
            " WHERE t.chainID = :chainID AND t.senderPk = :senderPk AND t.nonce = :nonce)";

    String QUERY_TX_QUEUE_BY_ID = "SELECT * FROM" +
            " (SELECT tq.*, t.timestamp, t.nonce, t.txStatus as status" +
            " FROM TxQueues tq" +
            " LEFT JOIN Txs t" +
            " ON tq.queueID = t.queueID" +
            " WHERE tq.queueID = :queueID)";

    String QUERY_COMMUNITY_TX_QUEUE = "SELECT * FROM" +
            " (SELECT tq.*, t.timestamp, t.nonce as nonce, t.txStatus as status" +
            " FROM TxQueues tq" +
            " LEFT JOIN Txs t" +
            " ON tq.queueID = t.queueID" +
            " WHERE t.chainID = :chainID AND t.senderPk = :senderPk AND t.txStatus = 0)" +
			" ORDER BY nonce";

    String QUERY_RESEND_NOTE_TX_QUEUE = "SELECT * FROM" +
            " (SELECT tq.*, t.timestamp, t.nonce, t.txStatus as status" +
            " FROM TxQueues tq" +
            " LEFT JOIN Txs t" +
            " ON tq.queueID = t.queueID" +
            " WHERE t.senderPk = :senderPk AND t.txType = " + Constants.NOTE_TX_TYPE +
			" ORDER BY t.timestamp LIMIT 10)"; //1 note tx, 10笔

    String QUERY_RESEND_NEWS_TX_QUEUE = "SELECT * FROM" +
            " (SELECT tq.*, t.timestamp, t.nonce, t.txStatus as status" +
            " FROM TxQueues tq" +
            " LEFT JOIN Txs t" +
            " ON tq.queueID = t.queueID" +
            " WHERE t.senderPk = :senderPk AND t.txStatus = 0" +
            " AND t.txType = " + Constants.NEWS_TX_TYPE + ")"; //2 news tx, 未确认

    String QUERY_NEED_SENDING_TX_COMMUNITIES = "SELECT chainID FROM" +
            " (SELECT tq.*, t.timestamp, t.nonce as nonce, t.txStatus as status" +
            " FROM TxQueues tq" +
            " LEFT JOIN Txs t" +
            " ON tq.queueID = t.queueID" +
            " WHERE t.senderPk = :senderPk AND t.txStatus = 0)" +
            " GROUP BY chainID";

    String QUERY_AIRDROP_COUNT_ON_CHAIN = "SELECT count(*) FROM" +
            " (SELECT tq.chainID, tq.queueID," +
            " (CASE WHEN t.status IS NULL THEN -1 ELSE t.status END) AS status" +
            " FROM TxQueues tq" +
            " LEFT JOIN (SELECT SUM(txStatus) AS status, queueID" +
            " From Txs" +
            " WHERE senderPk = :senderPk AND queueID IS NOT NULL" +
            " GROUP BY queueID) AS t" +
            " ON tq.queueID = t.queueID" +
            " WHERE tq.senderPk = :senderPk AND tq.chainID = :chainID" +
            " AND queueTime >= :currentTime AND queueType = 1)" +
            " WHERE status > 0";

    String QUERY_AIRDROP_HISTORY_ON_CHAIN = "SELECT queueID, receiverPk FROM" +
            " (SELECT tq.chainID, tq.queueID, tq.receiverPk," +
            " (CASE WHEN t.status IS NULL THEN -1 ELSE t.status END) AS status" +
            " FROM TxQueues tq" +
            " LEFT JOIN (SELECT SUM(txStatus) AS status, queueID" +
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
     * 更新交易队列
     */
    @Update
    void updateQueue(TxQueue tx);

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
     * 通过Nonce获取Tx
     */
    @Query(QUERY_TX_BY_NONCE)
    TxQueueAndStatus getTxByNonce(String chainID, String senderPk, long nonce);

    /** 
     * 通过QueueID获取Tx
     */
    @Query(QUERY_TX_QUEUE_BY_ID)
    TxQueueAndStatus getTxQueueByID(long queueID);

    @Query(QUERY_RESEND_NOTE_TX_QUEUE)
    List<TxQueueAndStatus> getResendNoteTxQueue(String senderPk);

    @Query(QUERY_RESEND_NEWS_TX_QUEUE)
    List<TxQueueAndStatus> getResendNewsTxQueue(String senderPk);

    @Query(QUERY_NEED_SENDING_TX_COMMUNITIES)
    List<String> getNeedSendingTxCommunities(String senderPk);

    /**
     * 获取某个key下在特定chainid下的所有未上链的交易
     */
    @Query(QUERY_COMMUNITY_TX_QUEUE)
    List<TxQueueAndStatus> getCommunityTxQueue(String chainID, String senderPk);
    @Query(QUERY_COMMUNITY_TX_QUEUE)
    Flowable<List<TxQueueAndStatus>> observeCommunityTxQueue(String chainID, String senderPk);

    @Query(QUERY_AIRDROP_TX_QUEUE)
    TxQueue getAirdropTxQueue(String chainID, String currentPk, String friendPk);

    @Query(QUERY_AIRDROP_COUNT)
    int getAirdropCount(String chainID, String currentPk, long currentTime);

    @Query(QUERY_REFERRAL_COUNT)
    int getReferralCount(String chainID, String currentPk, String friendPk, long currentTime);

    @Query(QUERY_AIRDROP_COUNT_ON_CHAIN)
    Flowable<Integer> observeAirdropCountOnChain(String chainID, String senderPk, long currentTime);

    @Transaction
    @Query(QUERY_AIRDROP_HISTORY_ON_CHAIN)
    Flowable<List<AirdropHistory>> observeAirdropHistoryOnChain(String chainID, String senderPk, long currentTime);
}
