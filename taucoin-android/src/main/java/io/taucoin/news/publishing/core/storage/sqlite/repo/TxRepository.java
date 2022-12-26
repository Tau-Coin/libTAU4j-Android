package io.taucoin.news.publishing.core.storage.sqlite.repo;

import java.util.List;

import androidx.paging.DataSource;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.taucoin.news.publishing.core.model.data.DataChanged;
import io.taucoin.news.publishing.core.model.data.IncomeAndExpenditure;
import io.taucoin.news.publishing.core.model.data.TxFreeStatistics;
import io.taucoin.news.publishing.core.model.data.UserAndTx;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.news.publishing.core.storage.sqlite.entity.TxLog;

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
     * 提交数据变化
     */
    void submitDataSetChanged(Tx tx);

    /**
     * 提交数据变化
     */
    void submitDataSetChanged();

    /**
     * 观察社区的交易的变化
     */
    Observable<DataChanged> observeDataSetChanged();

    /**
     * 根据txID查询交易
     * @param txID 交易ID
     */
    Tx getTxByTxID(String txID);

    /**
     * 根据TXID观察社区的交易的变化
     */
    Observable<Tx> observeTxByTxID(String txID);

    /**
     * 根据queueID查询交易
     * @param queueID 交易queueID
     */
    Tx getTxByQueueID(long queueID);

    /**
     * 根据queueID删除交易
     * @param queueID 交易queueID
     */
    void deleteTxByQueueID(long queueID);

    /**
     * 设置置顶信息
     */
    void setMessagePinned(String txID, long pinnedTime, boolean isRefresh);

    /**
     * 设置喜爱信息
     */
    void setMessageFavorite(String txID, long pinnedTime, boolean isRefresh);

    /**
     * 获取置顶消息
     */
    List<UserAndTx> queryCommunityPinnedTxs();

    /**
     * 获取某条链下的置顶消息
     * @param chainID 社区链ID
     */
    List<UserAndTx> queryCommunityPinnedTxs(String chainID);

    /**
     * 获取最近置顶消息
     */
    Flowable<List<UserAndTx>> observeLatestPinnedMsg();

    /**
     * 获取某条链下最近的置顶消息
     * @param chainID 社区链ID
     */
    Flowable<List<UserAndTx>> observeLatestPinnedMsg(String chainID);

    /** 
     * 根据chainID获取社区中的有nonce交易(包括正需要上链的news and wiring coins)
     * @param chainID 社区链id
     */
    List<UserAndTx> loadChainTxsData(String chainID, int pos, int pageSize);

    /**
     * 获取所有链的news消息
     * @param chainID 社区链ID
     */
    List<UserAndTx> loadNewsData(int pos, int pageSize);

    List<UserAndTx> loadAllNotesData(String repliesHash, int pos, int pageSize);

    List<UserAndTx> loadAllMarketData(String chainID, int pos, int pageSize);

    List<UserAndTx> loadNewsRepliesData(String txID, int pos, int pageSize);

    DataSource.Factory<Integer, UserAndTx> queryFavorites();

    List<Tx> getOnChainTxsByBlockHash(String blockHash);

    List<String> queryTxSendersReceived(String ChainID, String pk);

    void addTxLog(TxLog txLog);

    TxLog getTxLog(String txID, int status);

    Observable<List<TxLog>> observerTxLogs(String txID);

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

	//state前的交易上链
    int updateAllOnChainTxs(String chainID, String userPk, long nonce);

	//nonce前的交易总和
    long getChainTotalCoinsByNonce(String chainID, String userPk, long nonce);

	//获取当前链上的最大nonce
    long getChainMaxNonce(String chainID, String userPk);

    /**
     * 交易费统计
     * @param chainID
     * @return
     */
    TxFreeStatistics queryAverageTxsFee(String chainID);

    String getLatestNoteTxHash(String chainID);

    /**
     * 观察各个社区未读消息
     * @return
     */
    Flowable<Integer> observeUnreadNews();

    /**
     * 获取社区中最大chatnum的news
     * @return
     */
    Flowable<UserAndTx> observeMaxChatNumNews();

    Observable<UserAndTx> observeNewsDetail(String txID);
}
