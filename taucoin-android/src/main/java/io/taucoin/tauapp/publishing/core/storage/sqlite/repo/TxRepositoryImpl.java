package io.taucoin.tauapp.publishing.core.storage.sqlite.repo;

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
import io.taucoin.tauapp.publishing.core.model.data.DataChanged;
import io.taucoin.tauapp.publishing.core.model.data.IncomeAndExpenditure;
import io.taucoin.tauapp.publishing.core.model.data.TxFreeStatistics;
import io.taucoin.tauapp.publishing.core.model.data.UserAndTx;
import io.taucoin.tauapp.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.TxLog;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;
import io.taucoin.tauapp.publishing.ui.transaction.CommunityTabFragment;

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
    public Flowable<List<UserAndTx>> observeLatestPinnedMsg(int currentTab, String chainID) {
        if (currentTab == CommunityTabFragment.TAB_CHAIN) {
            return db.txDao().observeOnChainLatestPinnedTx(chainID);
        } else if (currentTab == CommunityTabFragment.TAB_MARKET) {
            return db.txDao().queryCommunityMarketLatestPinnedTx(chainID);
        } else {
            return db.txDao().queryCommunityNoteLatestPinnedTx(chainID);
        }
    }

    @Override
    public List<UserAndTx> loadOnChainNotesData(String chainID, int startPos, int loadSize) {
        return db.txDao().loadOnChainNotesData(chainID, startPos, loadSize);
    }

    @Override
    public List<UserAndTx> loadAllNotesData(String chainID, int startPos, int loadSize) {
        return db.txDao().loadAllNotesData(chainID, startPos, loadSize);
    }

    @Override
    public List<UserAndTx> loadAirdropMarketData(String chainID, int startPos, int loadSize) {
        return db.txDao().loadAirdropMarketData(chainID, startPos, loadSize);
    }

    @Override
    public List<UserAndTx> loadSellMarketData(String chainID, int startPos, int loadSize) {
        return db.txDao().loadSellMarketData(chainID, startPos, loadSize);
    }

    @Override
    public List<UserAndTx> loadAnnouncementMarketData(String chainID, int startPos, int loadSize) {
        return db.txDao().loadAnnouncementMarketData(chainID, startPos, loadSize);
    }

    @Override
    public List<UserAndTx> loadAllMarketData(String chainID, int startPos, int loadSize) {
        return db.txDao().loadAllMarketData(chainID, startPos, loadSize);
    }

    @Override
    public List<UserAndTx> loadChainTxsData(String chainID, int startPos, int loadSize) {
        return db.txDao().loadChainTxsData(chainID, startPos, loadSize);
    }

    /**
     * 加载交易固定数据
     * @param chainID 社区链ID
     */
    @Override
    public List<UserAndTx> queryCommunityPinnedTxs(String chainID, int currentTab) {
        if (currentTab == CommunityTabFragment.TAB_CHAIN) {
            return db.txDao().queryCommunityOnChainPinnedTxs(chainID);
        } else if (currentTab == CommunityTabFragment.TAB_MARKET) {
            return db.txDao().queryCommunityMarketPinnedTxs(chainID);
        } else {
            return db.txDao().queryCommunityNotePinnedTxs(chainID);
        }
    }

    /**
     * 查询社区用户被Trust列表
     * @param chainID 社区链ID
     */
    @Override
    public List<Tx> queryCommunityTrustTxs(String chainID, String trustPk, int startPos, int loadSize) {
        return db.txDao().queryCommunityTrustTxs(chainID, trustPk, startPos, loadSize);
    }

    /**
     * 获取社区里用户未上链并且未过期的交易数
     * @param chainID chainID
     * @param senderPk 公钥
     * @param expireTime 过期时间时长
     * @return int
     */
    @Override
    public int getPendingTxsNotExpired(String chainID, String senderPk, long expireTime){
        long expireTimePoint = DateUtil.getTime() - expireTime;
        return db.txDao().getPendingTxsNotExpired(chainID, senderPk, expireTimePoint);
    }

    /**
     * 获取社区里用户未上链并且过期的最早的交易
     * @param chainID chainID
     * @param senderPk 公钥
     * @param expireTime 过期时间时长
     * @return int
     */
    @Override
    public Tx getEarliestExpireTx(String chainID, String senderPk, long expireTime){
        long expireTimePoint = DateUtil.getTime() - expireTime;
        return db.txDao().getEarliestExpireTx(chainID, senderPk, expireTimePoint);
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
    public Tx getTxByQueueID(long queueID, long timestamp) {
        return db.txDao().getTxByQueueID(queueID, timestamp);
    }

    @Override
    public Flowable<UserAndTx> observeSellTxDetail(String chainID, String txID) {
        return db.txDao().observeSellTxDetail(chainID, txID);
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

    /**
     * 获取在当前nonce上是否有未上链的转账交易
     * @param chainID 链ID
     * @param txType 类型
     * @param nonce nonce
     * @return Tx
     */
    @Override
    public Tx getNotOnChainTx(String chainID, int txType, long nonce) {
        return db.txDao().getNotOnChainTx(chainID, txType, nonce);
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
    public void deleteUnsentTx(long queueID) {
        db.txDao().deleteUnsentTx(queueID);
    }

    @Override
    public Tx queryUnsentTx(long queueID) {
        return db.txDao().queryUnsentTx(queueID);
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
    public TxFreeStatistics queryAverageTxsFee(String chainID) {
        return db.txDao().queryAverageTxsFee(chainID);
    }

    @Override
    public String getLatestNoteTxHash(String chainID) {
        return db.txDao().getLatestNoteTxHash(chainID);
    }
}
