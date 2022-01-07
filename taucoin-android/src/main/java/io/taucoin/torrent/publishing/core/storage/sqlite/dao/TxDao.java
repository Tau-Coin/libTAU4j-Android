package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Observable;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;

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

    // SQL:查询社区里的交易(MARKET交易，排除Trust Tx)
    String QUERY_GET_MARKET_TXS = "SELECT tx.*, t.trusts" +
            " FROM Txs AS tx" +
            " LEFT JOIN (SELECT count(receiverPk) AS trusts, receiverPk FROM Txs" +
            " WHERE chainID = :chainID AND txType = 4 AND txStatus = 1 GROUP BY receiverPk) t" +
            " ON tx.senderPk = t.receiverPk" +
            " WHERE tx.chainID = :chainID" +
            " AND tx.senderPk NOT IN " + UserDao.QUERY_GET_USER_PKS_IN_BAN_LIST +
            " AND (tx.txType = 3 OR tx.txType = 5)" +
            " ORDER BY tx.timestamp DESC" +
            " limit :loadSize offset :startPosition";

    // SQL:查询社区里的交易(所有，排除Trust Tx)
    String QUERY_GET_TXS = "SELECT tx.*, t.trusts" +
            " FROM Txs AS tx" +
            " LEFT JOIN (SELECT count(receiverPk) AS trusts, receiverPk FROM Txs" +
            " WHERE chainID = :chainID AND txType = 4 AND txStatus = 1 GROUP BY receiverPk) t" +
            " ON tx.senderPk = t.receiverPk" +
            " WHERE tx.chainID = :chainID" +
            " AND tx.senderPk NOT IN " + UserDao.QUERY_GET_USER_PKS_IN_BAN_LIST +
            " ORDER BY tx.timestamp DESC" +
            " limit :loadSize offset :startPosition";

    // SQL:查询社区里的交易(上链，排除Trust Tx)
    String QUERY_GET_ON_CHAIN_TXS = "SELECT tx.*, t.trusts" +
            " FROM Txs AS tx" +
            " LEFT JOIN (SELECT count(receiverPk) AS trusts, receiverPk FROM Txs" +
            " WHERE chainID = :chainID AND txType = 4 AND txStatus = 1 GROUP BY receiverPk) t" +
            " ON tx.senderPk = t.receiverPk" +
            " WHERE tx.chainID = :chainID AND tx.txStatus = 1" +
            " AND tx.senderPk NOT IN " + UserDao.QUERY_GET_USER_PKS_IN_BAN_LIST +
            " ORDER BY tx.timestamp DESC" +
            " limit :loadSize offset :startPosition";

    // SQL:查询社区里用户Trust交易(上链)
    String QUERY_GET_TRUST_TXS = "SELECT * FROM Txs WHERE chainID = :chainID" +
            " AND txType = 4 AND txStatus = 1 AND receiverPk = :trustPk" +
            " ORDER BY timestamp DESC" +
            " limit :loadSize offset :startPosition";

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
    @Query(QUERY_GET_ON_CHAIN_TXS)
    List<UserAndTx> queryCommunityOnChainTxs(String chainID, int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_GET_MARKET_TXS)
    List<UserAndTx> queryCommunityMarketTxs(String chainID, int startPosition, int loadSize);

    @Transaction
    @Query(QUERY_GET_TXS)
    List<UserAndTx> queryCommunityTxs(String chainID, int startPosition, int loadSize);

    @Query(QUERY_GET_TRUST_TXS)
    List<Tx> queryCommunityTrustTxs(String chainID, String trustPk, int startPosition, int loadSize);

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

    @Transaction
    @Query(QUERY_GET_SELL_DETAIL)
    Observable<UserAndTx> observeSellTxDetail(String chainID, String txID);
}