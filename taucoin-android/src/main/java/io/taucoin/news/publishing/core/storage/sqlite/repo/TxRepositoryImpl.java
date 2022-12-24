package io.taucoin.news.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import androidx.room.RxRoom;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.taucoin.news.publishing.core.model.data.DataChanged;
import io.taucoin.news.publishing.core.model.data.IncomeAndExpenditure;
import io.taucoin.news.publishing.core.model.data.TxFreeStatistics;
import io.taucoin.news.publishing.core.model.data.UserAndTx;
import io.taucoin.news.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.news.publishing.core.storage.sqlite.entity.TxLog;
import io.taucoin.news.publishing.core.utils.DateUtil;

/**
 * TxRepository接口实现
 */
public class TxRepositoryImpl implements TxRepository{

    private Context appContext;
    private AppDatabase db;
    private PublishSubject<DataChanged> dataSetChangedPublish = PublishSubject.create();
    private ExecutorService sender = Executors.newSingleThreadExecutor();

    /**
     * CommunityRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public TxRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    /**
     * 添加新的交易
     */
    @Override
    public long addTransaction(Tx transaction){
        long result = db.txDao().addTransaction(transaction);
        submitDataSetChanged(transaction);
        return result;
    }

    /**
     * 更新交易
     */
    @Override
    public int updateTransaction(Tx transaction){
        int result = db.txDao().updateTransaction(transaction);
        submitDataSetChanged(transaction);
        return result;
    }

    @Override
    public Flowable<List<UserAndTx>> observeLatestPinnedMsg(String chainID) {
        return db.txDao().queryCommunityLatestPinnedTx(chainID);
    }

    @Override
    public Flowable<List<UserAndTx>> observeLatestPinnedMsg() {
        return db.txDao().queryCommunityLatestPinnedTx();
    }

    @Override
    public List<UserAndTx> loadAllNotesData(String repliesHash, int startPos, int loadSize) {
        return db.txDao().loadAllNotesData(repliesHash, startPos, loadSize);
    }

    @Override
    public List<UserAndTx> loadAllMarketData(String chainID, int startPos, int loadSize) {
        return db.txDao().loadAllMarketData(chainID, startPos, loadSize);
    }

    @Override
    public List<UserAndTx> loadChainTxsData(String chainID, int startPos, int loadSize) {
        return db.txDao().loadChainTxsData(chainID, startPos, loadSize);
    }

    @Override
    public List<UserAndTx> loadNewsData(int startPos, int loadSize) {
        return db.txDao().loadNewsData(startPos, loadSize);
    }

    @Override
    public List<UserAndTx> loadNewsRepliesData(String txID, int startPos, int loadSize) {
        return db.txDao().loadNewsRepliesData(txID, startPos, loadSize);
    }

    /**
     * 加载交易固定数据
     * @param chainID 社区链ID
     */
    @Override
    public List<UserAndTx> queryCommunityPinnedTxs(String chainID) {
        return db.txDao().queryCommunityPinnedTxs(chainID);
    }

    @Override
    public List<UserAndTx> queryCommunityPinnedTxs() {
        return db.txDao().queryCommunityPinnedTxs();
    }

    /**
     * 根据txID查询交易
     * @param txID 交易ID
     */
    @Override
    public Tx getTxByTxID(String txID){
        return db.txDao().getTxByTxID(txID);
    }

    @Override
    public Observable<Tx> observeTxByTxID(String txID) {
        return db.txDao().observeTxByTxID(txID);
    }

    @Override
    public Tx getTxByQueueID(long queueID) {
        return db.txDao().getTxByQueueID(queueID);
    }

    @Override
    public Observable<DataChanged> observeDataSetChanged() {
        return dataSetChangedPublish;
    }

    @Override
    public void submitDataSetChanged() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(DateUtil.getDateTime());
        submitDataSetChangedDirect(stringBuilder);
    }

    @Override
    public void submitDataSetChanged(Tx tx) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tx.senderPk);
        stringBuilder.append(tx.receiverPk);
        stringBuilder.append(DateUtil.getDateTime());
        submitDataSetChangedDirect(stringBuilder);
    }

    private void submitDataSetChangedDirect(StringBuilder msg) {
        sender.submit(() -> {
            DataChanged result = new DataChanged();
            result.setMsg(msg.toString());
            dataSetChangedPublish.onNext(result);
        });
    }

    @Override
    public void setMessagePinned(String txID, long pinnedTime, boolean isRefresh) {
        db.txDao().setMessagePinned(txID, pinnedTime);
        if (isRefresh) {
            submitDataSetChanged();
        }
    }

    @Override
    public void setMessageFavorite(String txID, long pinnedTime, boolean isRefresh) {
        db.txDao().setMessageFavorite(txID, pinnedTime);
        if (isRefresh) {
            submitDataSetChanged();
        }
    }

    @Override
    public DataSource.Factory<Integer, UserAndTx> queryFavorites() {
        return db.txDao().queryFavorites();
    }

    @Override
    public List<Tx> getOnChainTxsByBlockHash(String blockHash) {
        return db.txDao().getOnChainTxsByBlockHash(blockHash);
    }

    @Override
    public List<String> queryTxSendersReceived(String ChainID, String pk) {
        return db.txDao().queryTxSendersReceived(ChainID, pk);
    }

    @Override
    public void addTxLog(TxLog txLog) {
        db.txDao().addTxLog(txLog);
        submitDataSetChanged();
    }

    @Override
    public TxLog getTxLog(String txID, int status) {
        return db.txDao().getTxLog(txID, status);
    }

    @Override
    public Observable<List<TxLog>> observerTxLogs(String txID) {
        return db.txDao().observerTxLogs(txID);
    }

    @Override
    public void deleteTxByQueueID(long queueID) {
        db.txDao().deleteTxByQueueID(queueID);
    }

    @Override
    public List<IncomeAndExpenditure> observeWalletTransactions(String chainID, int startPosition, int loadSize) {
        return db.txDao().observeWalletTransactions(chainID, startPosition, loadSize);
    }

    @Override
    public Flowable<List<IncomeAndExpenditure>> observeMiningIncome(String chainID) {
        return db.txDao().observeMiningIncome(chainID);
    }

    @Override
    public Flowable<Object> observeWalletChanged() {
        String[] tables = new String[]{"Users", "Txs","Blocks"};
        return RxRoom.createFlowable(db, tables);
    }

    /**
     * 处理由于回滚未置为offChain状态的区块
     * @param chainID
     * @param userPk
     * @param nonce
     */
    @Override
    public int updateAllOffChainTxs(String chainID, String userPk, long nonce) {
        return db.txDao().updateAllOffChainTxs(chainID, userPk, nonce);
    }

    @Override
    public int updateAllOnChainTxs(String chainID, String userPk, long nonce) {
        return db.txDao().updateAllOnChainTxs(chainID, userPk, nonce);
    }

    //nonce前的交易总和
    @Override
    public long getChainTotalCoinsByNonce(String chainID, String userPk, long nonce) {
        return db.txDao().getChainTotalCoinsByNonce(chainID, userPk, nonce);
    }

    @Override
    public long getChainMaxNonce(String chainID, String userPk) {
        return db.txDao().getChainMaxNonce(chainID, userPk);
    }

    @Override
    public TxFreeStatistics queryAverageTxsFee(String chainID) {
        return db.txDao().queryAverageTxsFee(chainID);
    }

    @Override
    public String getLatestNoteTxHash(String chainID) {
        return db.txDao().getLatestNoteTxHash(chainID);
    }

    @Override
    public Flowable<Integer> observeUnreadNews() {
        return db.txDao().observeUnreadNews();
    }

    @Override
    public Observable<UserAndTx> observeNewsDetail(String txID) {
        return db.txDao().observeNewsDetail(txID);
    }
}
