package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.taucoin.torrent.publishing.core.model.data.DataChanged;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.ui.community.CommunityTabs;

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

    /**
     * 根据chainID查询社区交易
     * @param chainID 社区链ID
     */
    @Override
    public List<UserAndTx> queryCommunityTxs(String chainID, int currentTab, int startPos, int loadSize) {
        if (currentTab == CommunityTabs.CHAIN.getIndex()) {
            return db.txDao().queryCommunityOnChainTxs(chainID, startPos, loadSize);
        } else if (currentTab == CommunityTabs.MARKET.getIndex()) {
            return db.txDao().queryCommunityMarketTxs(chainID, startPos, loadSize);
        } else {
            return db.txDao().queryCommunityNoteTxs(chainID, startPos, loadSize);
        }
    }

    @Override
    public Flowable<List<UserAndTx>> observeLatestPinnedMsg(int currentTab, String chainID) {
        if (currentTab == CommunityTabs.CHAIN.getIndex()) {
            return db.txDao().observeOnChainLatestPinnedTx(chainID);
        } else if (currentTab == CommunityTabs.MARKET.getIndex()) {
            return db.txDao().queryCommunityMarketLatestPinnedTx(chainID);
        } else {
            return db.txDao().queryCommunityNoteLatestPinnedTx(chainID);
        }
    }

    /**
     * 加载交易固定数据
     * @param chainID 社区链ID
     */
    @Override
    public List<UserAndTx> queryCommunityPinnedTxs(String chainID, int currentTab) {
        if (currentTab == CommunityTabs.CHAIN.getIndex()) {
            return db.txDao().queryCommunityOnChainPinnedTxs(chainID);
        } else if (currentTab == CommunityTabs.MARKET.getIndex()) {
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
    public void setMessagePinned(String txID, int pinned, long pinnedTime) {
        db.txDao().setMessagePinned(txID, pinned, pinnedTime);
    }
}
