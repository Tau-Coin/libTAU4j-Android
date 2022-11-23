package io.taucoin.tauapp.publishing.core.storage.sqlite.repo;

import java.util.List;

import androidx.paging.DataSource;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.taucoin.tauapp.publishing.core.model.data.DataChanged;
import io.taucoin.tauapp.publishing.core.model.data.IncomeAndExpenditure;
import io.taucoin.tauapp.publishing.core.model.data.TxFreeStatistics;
import io.taucoin.tauapp.publishing.core.model.data.UserAndTx;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.TxLog;

/**
 * 提供外部操作User数据的接口
 */
public interface TxRepository {

    /**
     * 添加新的交易
     */
    long addTransaction(Tx transaction);

    /**
     * 更新交易
     */
    int updateTransaction(Tx transaction);

    /**
     * 加载交易固定数据
     * @param chainID 社区链ID
     */
    List<UserAndTx> queryCommunityPinnedTxs(String chainID, int currentTab);

    /**
     * 查询社区用户被Trust列表
     * @param chainID 社区链ID
     */
    List<Tx> queryCommunityTrustTxs(String chainID, String trustPk, int startPos, int loadSize);

    /**
     * 获取社区里用户未上链并且未过期的交易数
     * @param chainID chainID
     * @param publicKey 公钥
     * @param expireTime 过期时间时长
     * @return int
     */
    @Deprecated
    int getPendingTxsNotExpired(String chainID, String publicKey, long expireTime);
    /**
     * 获取社区里用户未上链并且未过期的交易数
     * @param chainID chainID
     * @param senderPk 公钥
     * @param expireTime 过期时间时长
     * @return int
     */
    @Deprecated
    Tx getEarliestExpireTx(String chainID, String senderPk, long expireTime);


    /**
     * 根据txID查询交易
     * @param txID 交易ID
     */
    Tx getTxByTxID(String txID);

    Observable<Tx> observeTxByTxID(String txID);

    Tx getTxByQueueID(long queueID);

    /**
     * 观察社区的交易的变化
     */
    Observable<DataChanged> observeDataSetChanged();

    /**
     * 提交数据变化
     */
    void submitDataSetChanged(Tx tx);

    /**
     * 提交数据变化
     */
    void submitDataSetChanged();

    Flowable<UserAndTx> observeSellTxDetail(String chainID, String txID);

    /**
     * 获取在当前nonce上是否有未上链的转账交易
     * @param chainID 链ID
     * @param txType 类型
     * @param nonce nonce
     * @return Tx
     */
    Tx getNotOnChainTx(String chainID, int txType, long nonce);

    void setMessagePinned(String txID, long pinnedTime, boolean isRefresh);

    void setMessageFavorite(String txID, long pinnedTime, boolean isRefresh);

    Flowable<List<UserAndTx>> observeLatestPinnedMsg(int currentTab, String chainID);

    List<UserAndTx> loadOnChainNotesData(String chainID, int pos, int pageSize);

    List<UserAndTx> loadAllNotesData(String chainID, int pos, int pageSize);

    List<UserAndTx> loadAirdropMarketData(String chainID, int pos, int pageSize);

    List<UserAndTx> loadSellMarketData(String chainID, int pos, int pageSize);

    List<UserAndTx> loadAnnouncementMarketData(String chainID, int pos, int pageSize);

    List<UserAndTx> loadAllMarketData(String chainID, int pos, int pageSize);

    List<UserAndTx> loadChainTxsData(String chainID, int pos, int pageSize);

    List<UserAndTx> loadNewsData(int pos, int pageSize);


    DataSource.Factory<Integer, UserAndTx> queryFavorites();

    List<Tx> getOnChainTxsByBlockHash(String blockHash);

    void addTxLog(TxLog txLog);

    TxLog getTxLog(String txID, int status);

    Observable<List<TxLog>> observerTxLogs(String txID);

    void deleteUnsentTx(long queueID);

    Tx queryUnsentTx(long queueID);

    List<IncomeAndExpenditure> observeWalletTransactions(String chainID, int startPosition, int loadSize);

    Flowable<List<IncomeAndExpenditure>> observeMiningIncome(String chainID);

    Flowable<Object> observeWalletChanged();

    /**
     * 处理由于回滚未置为offChain状态的区块
     * @param chainID
     * @param userPk
     * @param nonce
     */
    int updateAllOffChainTxs(String chainID, String userPk, long nonce);

    /**
     * 交易费统计
     * @param chainID
     * @return
     */
    TxFreeStatistics queryAverageTxsFee(String chainID);


    String getLatestNoteTxHash(String chainID);
}
