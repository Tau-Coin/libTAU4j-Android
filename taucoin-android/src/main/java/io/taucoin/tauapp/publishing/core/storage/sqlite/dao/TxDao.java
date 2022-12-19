package io.taucoin.tauapp.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.taucoin.tauapp.publishing.core.Constants;
import io.taucoin.tauapp.publishing.core.model.data.IncomeAndExpenditure;
import io.taucoin.tauapp.publishing.core.model.data.TxFreeStatistics;
import io.taucoin.tauapp.publishing.core.model.data.UserAndTx;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.TxLog;

/**
 * Room:Transaction操作接口
 */
@Dao
public interface TxDao {

    String QUERY_GET_TXS_ORDER =
            " AND tx.senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " AND tx.repliedHash IS NULL" +
            " ORDER BY tx.timestamp DESC" +
            " limit :loadSize offset :startPosition";

    // SQL:查询社区里的交易(MARKET交易，排除Trust Tx, 并且上链)
    String QUERY_GET_MARKET_SELECT = "SELECT tx.*, t.repliesNum, m.balance, m.power" +
            " FROM Txs AS tx" +
            " LEFT JOIN Members m ON tx.chainID = m.chainID AND tx.senderPk = m.publicKey" +
            " LEFT JOIN (SELECT count(txID) AS repliesNum, repliedHash FROM Txs" +
            " WHERE chainID = :chainID AND repliedHash IS NOT NULL" +
            " AND senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " GROUP BY repliedHash) t" +
            " ON tx.txID = t.repliedHash" +
            " WHERE tx.chainID = :chainID";

    String QUERY_GET_ALL_MARKET = QUERY_GET_MARKET_SELECT +
            " AND tx.txType = 2" + QUERY_GET_TXS_ORDER;

    // SQL:查询社区news对应的notes交易
    String QUERY_GET_ALL_NOTES = "SELECT tx.*, 0 AS repliesNum, m.balance, m.power" +
            " FROM Txs AS tx" +
            " LEFT JOIN Members m ON tx.chainID = m.chainID AND tx.senderPk = m.publicKey" +
            " WHERE txType = 1" +
            " AND tx.repliedHash = :repliesHash" +
            " AND tx.senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " ORDER BY tx.timestamp DESC" +
            " limit :loadSize offset :startPosition";

    // SQL:查询社区里的交易
    String QUERY_GET_CHAIN_TXS_SELECT = "SELECT tx.*, 0 AS repliesNum, m.balance, m.power" +
            " FROM Txs AS tx" +
            " LEFT JOIN Members m ON tx.chainID = m.chainID AND tx.senderPk = m.publicKey" +
            " WHERE tx.chainID = :chainID AND tx.nonce >= 1";

    // SQL:查询社区里的交易(上链)
    String QUERY_GET_CHAIN_ALL_TXS = QUERY_GET_CHAIN_TXS_SELECT + QUERY_GET_TXS_ORDER;

    // SQL:查询社区里的置顶交易(目前只显示News交易)
    String QUERY_GET_MARKET_PINNED_TXS = QUERY_GET_MARKET_SELECT +
            " AND tx.senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " AND tx.txType = 2 AND pinnedTime > 0" +
            " ORDER BY tx.pinnedTime DESC";

    String QUERY_GET_ALL_MARKET_PINNED_TXS = "SELECT tx.*, t.repliesNum, m.balance, m.power" +
        " FROM Txs AS tx" +
        " LEFT JOIN Members m ON tx.chainID = m.chainID AND tx.senderPk = m.publicKey" +
        " LEFT JOIN Communities c ON tx.chainID = c.chainID" +
        " LEFT JOIN (SELECT count(txID) AS repliesNum, chainID, repliedHash FROM Txs" +
        " WHERE repliedHash IS NOT NULL" +
        " AND senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
        " GROUP BY repliedHash) t" +
        " ON tx.txID = t.repliedHash  " +
        " WHERE tx.txType = 2 AND pinnedTime > 0" +
        " AND c.isBanned = 0" +
        " AND tx.senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
        " ORDER BY tx.pinnedTime DESC";

    // 查询钱包交易记录
    String QUERY_GET_WALLET_TRANSACTIONS = "SELECT " +
            " t.txID AS hash, t.senderPk AS senderOrMiner, t.receiverPk, -1 AS blockNumber, t.txType, t.amount, t.fee," +
            " t.timestamp / 1000 AS createTime, b.timestamp AS onlineTime, t.txStatus AS onlineStatus" +
            " FROM Txs AS t" +
            " LEFT JOIN (SELECT txID, timestamp FROM Blocks WHERE chainID = :chainID" +
            " AND txID IS NOT NULL AND status = 1 GROUP BY txID) AS b ON t.txID = b.txID" +
            " WHERE t.chainID = :chainID" +
            " AND (t.senderPk = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " OR t.receiverPk = (" + UserDao.QUERY_GET_CURRENT_USER_PK + "))" +
            " AND t.txType IN (2, 3)";

    // 查询钱包的收入和支出
    String QUERY_GET_WALLET_INCOME_AND_EXPENDITURE = "SELECT * FROM" +
            " (" + QUERY_GET_WALLET_TRANSACTIONS +")" +
            " ORDER BY createTime DESC" +
            " LIMIT :loadSize OFFSET :startPosition";

    String QUERY_GET_ALL_NEWS_REPLIES = "SELECT tx.*, 0 AS repliesNum, m.balance, m.power" +
            " FROM Txs AS tx" +
            " LEFT JOIN Members m ON tx.chainID = m.chainID AND tx.senderPk = m.publicKey" +
            " WHERE tx.repliedHash = :txID AND txType =" + Constants.NEWS_TX_TYPE +
            " AND tx.senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " ORDER BY tx.timestamp DESC" +
            " LIMIT :loadSize OFFSET :startPosition";

    String QUERY_GET_ALL_NEWS = "SELECT tx.*, t.repliesNum, m.balance, m.power" +
            " FROM Txs AS tx" +
            " LEFT JOIN Members m ON tx.chainID = m.chainID AND tx.senderPk = m.publicKey" +
            " LEFT JOIN Communities c ON tx.chainID = c.chainID" +
            " LEFT JOIN (SELECT count(txID) AS repliesNum, chainID, repliedHash FROM Txs" +
            " WHERE repliedHash IS NOT NULL" +
            " AND senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " GROUP BY repliedHash) t" +
            " ON tx.txID = t.repliedHash  " +
            " WHERE tx.txType = 2" +
            " AND c.isBanned = 0" +
            QUERY_GET_TXS_ORDER;

    String QUERY_GET_NEWS_DETAIL = "SELECT tx.*, t.repliesNum, m.balance, m.power" +
            " FROM Txs AS tx" +
            " LEFT JOIN Members m ON tx.chainID = m.chainID AND tx.senderPk = m.publicKey" +
            " LEFT JOIN Communities c ON tx.chainID = c.chainID" +
            " LEFT JOIN (SELECT count(txID) AS repliesNum, repliedHash FROM Txs" +
            " WHERE repliedHash = :txID" +
            " AND senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST + ") t" +
            " ON tx.txID = t.repliedHash  " +
            " WHERE tx.txID = :txID AND c.isBanned = 0";

    String QUERY_UNREAD_NEWS = "SELECT SUM(newsUnread) FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " WHERE m.publicKey = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND c.isBanned = 0";

    // SQL:查询挖矿收入
    String QUERY_MINING_INCOME = "SELECT " +
            " blockHash AS hash, miner AS senderOrMiner, '' AS receiverPk, blockNumber, -1 AS txType, rewards AS amount," +
            " 0 AS fee, timestamp AS createTime, timestamp AS onlineTime, status AS onlineStatus" +
            " FROM Blocks" +
            " WHERE chainID = :chainID AND miner = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND status = 1 AND datetime(timestamp, 'unixepoch', 'localtime') > datetime('now','-3 hour','localtime')" +
            " ORDER BY createTime DESC";

    // SQL:查询未上链并且已过期的条件语句
    String QUERY_PENDING_TXS_NOT_EXPIRED_WHERE = " WHERE senderPk = :senderPk AND chainID = :chainID" +
            " and txStatus = 0 and timestamp > :expireTimePoint ";

    // SQL:查询未上链并且未过期的txID
    String QUERY_PENDING_TX_IDS_NOT_EXPIRED = "SELECT txID FROM Txs" + QUERY_PENDING_TXS_NOT_EXPIRED_WHERE;

    String QUERY_GET_TX_BY_TX_ID = "SELECT * FROM Txs" +
            " WHERE txID = :txID";

    String QUERY_GET_TX_BY_TX_QUEUE = "SELECT * FROM Txs" +
            " WHERE queueID = :queueID";
    //        " WHERE queueID = :queueID AND timestamp = :timestamp";

    String QUERY_GET_NOT_ON_CHAIN_TX = "SELECT * FROM Txs" +
            " WHERE chainID = :chainID AND txType = :txType AND nonce = :nonce" +
            " ORDER BY timestamp DESC LIMIT 1";

    String QUERY_CHAIN_MAX_NONCE = "SELECT max(nonce) FROM Txs" +
            " WHERE chainID = :chainID AND senderPk = :senderPk" ;

    String QUERY_SET_MESSAGE_PINNED = "UPDATE Txs SET pinnedTime = :pinnedTime" +
            " WHERE txID = :txID";

    String QUERY_SET_MESSAGE_FAVORITE = "UPDATE Txs SET favoriteTime = :favoriteTime" +
            " WHERE txID = :txID";

    String QUERY_GET_FAVORITE_TXS = "SELECT tx.*, 0 AS repliesNum, m.balance, m.power" +
            " FROM Txs AS tx" +
            " LEFT JOIN Members m ON tx.chainID = m.chainID AND tx.senderPk = m.publicKey" +
            " WHERE favoriteTime > 0 " +
            " AND tx.senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " ORDER BY tx.favoriteTime DESC";

    String QUERY_ON_CHAIN_TXS_BY_BLOCK_HASH = "SELECT * FROM Txs" +
            " WHERE txStatus = 1 AND blockHash = :blockHash";

    String QUERY_TX_SENDERS_IN_RECEIVER = "SELECT senderPk FROM Txs" +
            " WHERE txStatus = 1 AND chainID = :chainID AND receiverPk = :receiverPk " +
			" GROUP BY senderPk";

    String DELETE_TX_BY_QUEUEID = "DELETE FROM Txs WHERE queueID = :queueID";

    String QUERY_TX_LOG = "SELECT * FROM TxLogs WHERE hash = :hash AND status = :status";

    String QUERY_TX_LOGS = "SELECT * FROM TxLogs WHERE hash = :hash ORDER BY status DESC";

    String UPDATE_ALL_OFF_CHAIN_TXS = "UPDATE Txs SET txStatus = 0" +
            " WHERE chainID = :chainID AND senderPk = :userPk" +
            " AND txType IN (2, 3) AND nonce > :nonce";

    String UPDATE_ALL_ON_CHAIN_TXS = "UPDATE Txs SET txStatus = 2" +
            " WHERE chainID = :chainID AND senderPk = :userPk" +
            " AND txType IN (2, 3) AND nonce <= :nonce";

    String QUERY_AVERAGE_TXS_FEE = "SELECT count(*) AS total, ifnull(sum(ifnull(fee, 0)), 0) AS totalFee," +
            " ifnull(sum(CASE WHEN ifnull(txType, 0) = 2 THEN 1 ELSE 0 END), 0) AS wiringCount, " +
            " ifnull(sum(CASE WHEN ifnull(txType, 0) in (2, 3) THEN 1 ELSE 0 END), 0) AS txsCount" +
            " FROM (SELECT t.fee, t.txType" +
            " FROM Blocks b" +
            " LEFT JOIN Txs t ON b.txID = t.txID" +
            " WHERE b.chainID = :chainID AND b.status = 1" +
            " ORDER BY b.blockNumber DESC LIMIT 50)";

    String QUERY_LATEST_NOTE_TX_HASH = "SELECT txID FROM Txs" +
            " WHERE chainID = :chainID AND txType = 1" +
			" AND senderPk = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " ORDER BY timestamp DESC limit 1";

    /**
     * 添加新的交易
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addTransaction(Tx tx);

    /**
     * 更新交易
     */
    @Update
    int updateTransaction(Tx tx);

    @Transaction
    @Query(QUERY_GET_ALL_NEWS)
    List<UserAndTx> loadNewsData(int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_GET_ALL_NEWS_REPLIES)
    List<UserAndTx> loadNewsRepliesData(String txID, int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_GET_NEWS_DETAIL)
    Observable<UserAndTx> observeNewsDetail(String txID);
    /**
     * 根据chainID获取社区中的交易的被观察者
     * @param chainID 社区链id
     */
    @Transaction
    @Query(QUERY_GET_CHAIN_ALL_TXS)
    List<UserAndTx> loadChainTxsData(String chainID, int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_GET_ALL_MARKET)
    List<UserAndTx> loadAllMarketData(String chainID, int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_GET_ALL_NOTES)
    List<UserAndTx> loadAllNotesData(String repliesHash, int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_GET_MARKET_PINNED_TXS)
    List<UserAndTx> queryCommunityMarketPinnedTxs(String chainID);

    @Transaction
    @Query(QUERY_GET_ALL_MARKET_PINNED_TXS)
    List<UserAndTx> queryCommunityMarketPinnedTxs();

    @Transaction
    @Query(QUERY_GET_MARKET_PINNED_TXS + " limit 1")
    Flowable<List<UserAndTx>> queryCommunityMarketLatestPinnedTx(String chainID);

    @Transaction
    @Query(QUERY_GET_ALL_MARKET_PINNED_TXS + " limit 1")
    Flowable<List<UserAndTx>> queryCommunityMarketLatestPinnedTx();

    @Transaction
    @Query(QUERY_GET_WALLET_INCOME_AND_EXPENDITURE)
    List<IncomeAndExpenditure> observeWalletTransactions(String chainID, int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_MINING_INCOME)
    Flowable<List<IncomeAndExpenditure>> observeMiningIncome(String chainID);

    /**
     * 根据txID查询交易
     * @param txID 交易ID
     */
    @Query(QUERY_GET_TX_BY_TX_ID)
    Tx getTxByTxID(String txID);

    @Query(QUERY_GET_TX_BY_TX_ID)
    Observable<Tx> observeTxByTxID(String txID);

    @Query(QUERY_GET_TX_BY_TX_QUEUE)
    Tx getTxByQueueID(long queueID);

    @Query(QUERY_SET_MESSAGE_PINNED)
    void setMessagePinned(String txID, long pinnedTime);

    @Query(QUERY_SET_MESSAGE_FAVORITE)
    void setMessageFavorite(String txID, long favoriteTime);

    @Transaction
    @Query(QUERY_GET_FAVORITE_TXS)
    DataSource.Factory<Integer, UserAndTx> queryFavorites();

    @Query(QUERY_ON_CHAIN_TXS_BY_BLOCK_HASH)
    List<Tx> getOnChainTxsByBlockHash(String blockHash);

    @Query(QUERY_TX_SENDERS_IN_RECEIVER)
    List<String> queryTxSendersReceived(String chainID, String receiverPk);

    @Query(DELETE_TX_BY_QUEUEID)
    void deleteTxByQueueID(long queueID);

    @Insert()
    void addTxLog(TxLog log);

    @Query(QUERY_TX_LOG)
    TxLog getTxLog(String hash, int status);

    @Query(QUERY_TX_LOGS)
    Observable<List<TxLog>> observerTxLogs(String hash);

    /**
     * 处理由于回滚未置为offChain状态的区块
     * @param chainID
     * @param userPk
     * @param nonce
     */
    @Query(UPDATE_ALL_OFF_CHAIN_TXS)
    int updateAllOffChainTxs(String chainID, String userPk, long nonce);

	//update status into ON_CHAIN
    @Query(UPDATE_ALL_ON_CHAIN_TXS)
    int updateAllOnChainTxs(String chainID, String userPk, long nonce);

	//获取最大的nonce
    @Query(QUERY_CHAIN_MAX_NONCE)
	long getChainMaxNonce(String chainID, String senderPk);

    /**
     * 交易费统计
     * @param chainID
     * @return
     */
    @Query(QUERY_AVERAGE_TXS_FEE)
    TxFreeStatistics queryAverageTxsFee(String chainID);

    @Query(QUERY_LATEST_NOTE_TX_HASH)
    String getLatestNoteTxHash(String chainID);

    @Query(QUERY_UNREAD_NEWS)
    Flowable<Integer> observeUnreadNews();
}
