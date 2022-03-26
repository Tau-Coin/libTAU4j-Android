package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.taucoin.torrent.publishing.core.model.data.DataChanged;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxConfirm;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.ui.transaction.CommunityTabFragment;

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
    public List<UserAndTx> loadOffChainNotesData(String chainID, int startPos, int loadSize) {
        return db.txDao().loadOffChainNotesData(chainID, startPos, loadSize);
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
    public List<UserAndTx> loadAllMarketData(String chainID, int startPos, int loadSize) {
        return db.txDao().loadAllMarketData(chainID, startPos, loadSize);
    }

    @Override
    public List<UserAndTx> loadOnChainAllTxs(String chainID, int startPos, int loadSize) {
        return db.txDao().loadOnChainAllTxs(chainID, startPos, loadSize);
    }

    @Override
    public List<UserAndTx> loadAllWiringTxs(String chainID, int startPos, int loadSize) {
        return db.txDao().loadAllWiringTxs(chainID, startPos, loadSize);
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

    public void addTxConfirm(TxConfirm txConfirm) {
        db.txConfirmDao().addTxConfirm(txConfirm);
        submitDataSetChanged();
    }

    public TxConfirm getTxConfirm(String txID, String peer) {
        return db.txConfirmDao().getTxConfirm(txID, peer);
    }

    @Override
    public Observable<List<TxConfirm>> observerTxConfirms(String txID) {
        return db.txConfirmDao().observerTxConfirms(txID);
    }
}
