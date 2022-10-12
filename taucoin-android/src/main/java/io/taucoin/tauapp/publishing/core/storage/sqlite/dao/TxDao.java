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
import io.taucoin.tauapp.publishing.core.model.data.IncomeAndExpenditure;
import io.taucoin.tauapp.publishing.core.model.data.UserAndTx;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.TxLog;

/**
 * Room:Transaction操作接口
 */
@Dao
public interface TxDao {

    String QUERY_GET_SELL_DETAIL = "SELECT tx.*, t.trusts" +
            " FROM Txs AS tx" +
            " LEFT JOIN (SELECT count(receiverPk) AS trusts, receiverPk FROM Txs" +
            " WHERE chainID = :chainID AND txType = 4 AND txStatus = 1 GROUP BY receiverPk) t" +
            " ON tx.senderPk = t.receiverPk" +
            " WHERE tx.chainID = :chainID AND tx.txID = :txID";

    String QUERY_GET_TXS_ORDER =
            " AND tx.senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " ORDER BY tx.timestamp DESC" +
            " limit :loadSize offset :startPosition";

    // SQL:查询社区里的交易(MARKET交易，排除Trust Tx, 并且上链)
    String QUERY_GET_MARKET_SELECT = "SELECT tx.*, t.trusts" +
            " FROM Txs AS tx" +
            " LEFT JOIN (SELECT count(receiverPk) AS trusts, receiverPk FROM Txs" +
            " WHERE chainID = :chainID AND txStatus = 1 AND txType = 4 " +
            " GROUP BY receiverPk" +
            ") t" +
            " ON tx.senderPk = t.receiverPk" +
            " WHERE tx.chainID = :chainID";

    String QUERY_GET_ALL_MARKET = QUERY_GET_MARKET_SELECT +
            " AND tx.txType IN (3, 5, 6)" + QUERY_GET_TXS_ORDER;

    String QUERY_GET_AIRDROP_MARKET = QUERY_GET_MARKET_SELECT +
            " AND tx.txType = 5" + QUERY_GET_TXS_ORDER;

    String QUERY_GET_SELL_MARKET = QUERY_GET_MARKET_SELECT +
            " AND tx.txType = 3" + QUERY_GET_TXS_ORDER;

    String QUERY_GET_ANNOUNCEMENT_MARKET = QUERY_GET_MARKET_SELECT +
            " AND tx.txType = 6" + QUERY_GET_TXS_ORDER;

    // SQL:查询社区里的交易(所有，排除WIRING Tx)
    String QUERY_GET_NOTES_SELECT = "SELECT tx.*, 0 AS trusts" +
            " FROM Txs AS tx" +
            " WHERE tx.chainID = :chainID AND txType IN (1, 3, 4, 5, 6)" +
            " AND sendStatus = 0";

    String QUERY_GET_ALL_NOTES = QUERY_GET_NOTES_SELECT +
            QUERY_GET_TXS_ORDER;

    String QUERY_GET_ON_CHAIN_NOTES = QUERY_GET_NOTES_SELECT +
            " AND txStatus = 1" +
            QUERY_GET_TXS_ORDER;

    // SQL:查询社区里的交易(上链)
    String QUERY_GET_CHAIN_TXS_SELECT = "SELECT tx.*, 0 AS trusts" +
            " FROM Txs AS tx" +
            " WHERE tx.chainID = :chainID AND sendStatus = 0";

    // SQL:查询社区里的交易(上链)
    String QUERY_GET_CHAIN_ALL_TXS = QUERY_GET_CHAIN_TXS_SELECT +
            QUERY_GET_TXS_ORDER;

    // SQL:查询社区里的置顶交易(所有，排除WIRING Tx)
    String QUERY_GET_NOTE_PINNED_TXS = QUERY_GET_NOTES_SELECT +
            " AND tx.senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " AND pinnedTime > 0" +
            " ORDER BY tx.pinnedTime DESC";

    // SQL:查询社区里的置顶交易(MARKET交易，排除Trust Tx, 并且上链)
    String QUERY_GET_MARKET_PINNED_TXS = QUERY_GET_MARKET_SELECT +
            " AND tx.senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " AND tx.txType IN (3, 5, 6) AND pinnedTime > 0" +
            " ORDER BY tx.pinnedTime DESC";

    // SQL:查询社区里的置顶交易(上链)
    String QUERY_GET_CHAIN_PINNED_TXS = QUERY_GET_CHAIN_TXS_SELECT +
            " AND tx.senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " AND pinnedTime > 0" +
            " ORDER BY tx.pinnedTime DESC";

    // SQL:查询社区里用户Trust交易(上链)
    String QUERY_GET_TRUST_TXS = "SELECT * FROM Txs WHERE chainID = :chainID" +
            " AND txType = 4 AND txStatus = 1 AND receiverPk = :trustPk" +
            " ORDER BY timestamp DESC" +
            " limit :loadSize offset :startPosition";

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
            " AND t.txType IN (2, 3, 4, 5, 6)";

    // 查询钱包挖矿记录
    String QUERY_GET_BLOCK_MINED = "SELECT " +
            " blockHash AS hash, miner AS senderOrMiner, '' AS receiverPk, blockNumber, -1 AS txType, rewards AS amount," +
            " 0 AS fee, timestamp AS createTime, timestamp AS onlineTime, status AS onlineStatus" +
            " FROM Blocks" +
            " WHERE chainID = :chainID AND miner = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")";

    // 查询钱包的收入和支出
    String QUERY_GET_WALLET_INCOME_AND_EXPENDITURE = "SELECT * FROM" +
            " (" + QUERY_GET_WALLET_TRANSACTIONS + " UNION ALL " + QUERY_GET_BLOCK_MINED +")" +
            " ORDER BY createTime DESC" +
            " LIMIT :loadSize OFFSET :startPosition";

    // SQL:查询未上链并且已过期的条件语句
    String QUERY_PENDING_TXS_NOT_EXPIRED_WHERE = " WHERE senderPk = :senderPk AND chainID = :chainID" +
            " and txStatus = 0 and timestamp > :expireTimePoint ";

    // SQL:查询未上链、未过期的交易
    String QUERY_PENDING_TXS_NOT_EXPIRED = "SELECT count(*) FROM Txs" + QUERY_PENDING_TXS_NOT_EXPIRED_WHERE;

    // SQL:查询未上链并且未过期的txID
    String QUERY_PENDING_TX_IDS_NOT_EXPIRED = "SELECT txID FROM Txs" + QUERY_PENDING_TXS_NOT_EXPIRED_WHERE;

    // SQL:查询未上链、已过期的并且nonce值未被再次使用的最早的交易
    String QUERY_USERS_EARLIEST_EXPIRE_TX = "SELECT * FROM Txs" +
            " WHERE senderPk = :senderPk AND chainID = :chainID AND txStatus = 0 AND timestamp <= :expireTimePoint" +
            " AND nonce NOT IN (" + QUERY_PENDING_TX_IDS_NOT_EXPIRED + ")" +
            " ORDER BY nonce LIMIT 1";

    String QUERY_GET_TX_BY_TX_ID = "SELECT * FROM Txs" +
            " WHERE txID = :txID";

    String QUERY_GET_TX_BY_TX_QUEUE = "SELECT * FROM Txs" +
            " WHERE queueID = :queueID AND timestamp = :timestamp";

    String QUERY_GET_NOT_ON_CHAIN_TX = "SELECT * FROM Txs" +
            " WHERE chainID = :chainID AND txType = :txType AND nonce = :nonce" +
            " ORDER BY timestamp DESC LIMIT 1";

    String QUERY_SET_MESSAGE_PINNED = "UPDATE Txs SET pinnedTime = :pinnedTime" +
            " WHERE txID = :txID";

    String QUERY_SET_MESSAGE_FAVORITE = "UPDATE Txs SET favoriteTime = :favoriteTime" +
            " WHERE txID = :txID";

    String QUERY_GET_FAVORITE_TXS = "SELECT tx.*, 0 AS trusts" +
            " FROM Txs AS tx" +
            " WHERE favoriteTime > 0 " +
            " AND tx.senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " ORDER BY tx.favoriteTime DESC";

    String QUERY_ON_CHAIN_TXS_BY_BLOCK_HASH = "SELECT * FROM Txs" +
            " WHERE txStatus = 1 AND blockHash = :blockHash";


    String QUERY_UNSENT_TX = "SELECT * FROM Txs WHERE queueID = :queueID AND sendStatus = 1 LIMIT 1";

    String DELETE_UNSENT_TX = "DELETE FROM Txs WHERE queueID = :queueID AND sendStatus = 1";

    String QUERY_TX_LOG = "SELECT * FROM TxLogs WHERE hash = :hash AND status = :status";

    String QUERY_TX_LOGS = "SELECT * FROM TxLogs WHERE hash = :hash ORDER BY status DESC";

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

    /**
     * 根据chainID获取社区中的交易的被观察者
     * @param chainID 社区链id
     */
    @Transaction
    @Query(QUERY_GET_CHAIN_ALL_TXS)
    List<UserAndTx> loadChainTxsData(String chainID, int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_GET_AIRDROP_MARKET)
    List<UserAndTx> loadAirdropMarketData(String chainID, int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_GET_SELL_MARKET)
    List<UserAndTx> loadSellMarketData(String chainID, int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_GET_ANNOUNCEMENT_MARKET)
    List<UserAndTx> loadAnnouncementMarketData(String chainID, int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_GET_ALL_MARKET)
    List<UserAndTx> loadAllMarketData(String chainID, int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_GET_ON_CHAIN_NOTES)
    List<UserAndTx> loadOnChainNotesData(String chainID, int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_GET_ALL_NOTES)
    List<UserAndTx> loadAllNotesData(String chainID, int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_GET_CHAIN_PINNED_TXS)
    List<UserAndTx> queryCommunityOnChainPinnedTxs(String chainID);

    @Transaction
    @Query(QUERY_GET_MARKET_PINNED_TXS)
    List<UserAndTx> queryCommunityMarketPinnedTxs(String chainID);

    @Transaction
    @Query(QUERY_GET_NOTE_PINNED_TXS)
    List<UserAndTx> queryCommunityNotePinnedTxs(String chainID);

    @Transaction
    @Query(QUERY_GET_CHAIN_PINNED_TXS + " limit 1")
    Flowable<List<UserAndTx>> observeOnChainLatestPinnedTx(String chainID);

    @Transaction
    @Query(QUERY_GET_MARKET_PINNED_TXS + " limit 1")
    Flowable<List<UserAndTx>> queryCommunityMarketLatestPinnedTx(String chainID);

    @Transaction
    @Query(QUERY_GET_NOTE_PINNED_TXS + " limit 1")
    Flowable<List<UserAndTx>> queryCommunityNoteLatestPinnedTx(String chainID);

    @Query(QUERY_GET_TRUST_TXS)
    List<Tx> queryCommunityTrustTxs(String chainID, String trustPk, int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_GET_WALLET_INCOME_AND_EXPENDITURE)
    List<IncomeAndExpenditure> observeWalletTransactions(String chainID, int startPosition, int loadSize);

    /**
     * 获取社区里用户未上链并且未过期的交易数
     * @param chainID chainID
     * @param senderPk 公钥
     * @param expireTimePoint 过期的时间点
     * @return int
     */
    @Query(QUERY_PENDING_TXS_NOT_EXPIRED)
    int getPendingTxsNotExpired(String chainID, String senderPk, long expireTimePoint);

    /**
     * 获取社区里用户未上链并且过期的最早的交易
     * @param chainID chainID
     * @param senderPk 公钥
     * @param expireTimePoint 过期的时间点
     * @return Tx
     */
    @Transaction
    @Query(QUERY_USERS_EARLIEST_EXPIRE_TX)
    Tx getEarliestExpireTx(String chainID, String senderPk, long expireTimePoint);

    /**
     * 根据txID查询交易
     * @param txID 交易ID
     */
    @Query(QUERY_GET_TX_BY_TX_ID)
    Tx getTxByTxID(String txID);

    @Query(QUERY_GET_TX_BY_TX_ID)
    Observable<Tx> observeTxByTxID(String txID);

    @Query(QUERY_GET_TX_BY_TX_QUEUE)
    Tx getTxByQueueID(long queueID, long timestamp);

    @Transaction
    @Query(QUERY_GET_SELL_DETAIL)
    Flowable<UserAndTx> observeSellTxDetail(String chainID, String txID);

    /**
     * 获取在当前nonce上是否有未上链的转账交易
     * @param chainID 链ID
     * @param txType 类型
     * @param nonce nonce
     * @return Tx
     */
    @Query(QUERY_GET_NOT_ON_CHAIN_TX)
    Tx getNotOnChainTx(String chainID, int txType, long nonce);

    @Query(QUERY_SET_MESSAGE_PINNED)
    void setMessagePinned(String txID, long pinnedTime);

    @Query(QUERY_SET_MESSAGE_FAVORITE)
    void setMessageFavorite(String txID, long favoriteTime);

    @Transaction
    @Query(QUERY_GET_FAVORITE_TXS)
    DataSource.Factory<Integer, UserAndTx> queryFavorites();

    @Query(QUERY_ON_CHAIN_TXS_BY_BLOCK_HASH)
    List<Tx> getOnChainTxsByBlockHash(String blockHash);

    @Query(QUERY_UNSENT_TX)
    Tx queryUnsentTx(long queueID);

    @Query(DELETE_UNSENT_TX)
    void deleteUnsentTx(long queueID);

    @Insert()
    void addTxLog(TxLog log);

    @Query(QUERY_TX_LOG)
    TxLog getTxLog(String hash, int status);

    @Query(QUERY_TX_LOGS)
    Observable<List<TxLog>> observerTxLogs(String hash);
}