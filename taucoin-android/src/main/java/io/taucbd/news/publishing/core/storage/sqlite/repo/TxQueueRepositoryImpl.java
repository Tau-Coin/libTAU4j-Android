package io.taucbd.news.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.taucbd.news.publishing.core.model.data.TxQueueAndStatus;
import io.taucbd.news.publishing.core.storage.sqlite.AppDatabase;
import io.taucbd.news.publishing.core.model.data.AirdropHistory;
import io.taucbd.news.publishing.core.storage.sqlite.entity.TxQueue;

/**
 * TxQueueRepository接口实现
 */
public class TxQueueRepositoryImpl implements TxQueueRepository {

    private Context appContext;
    private AppDatabase db;

    /**
     * CommunityRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public TxQueueRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    @Override
    public void updateQueue(TxQueue tx) {
        db.txQueueDao().updateQueue(tx);
    }

    @Override
    public long addQueue(TxQueue tx) {
        return db.txQueueDao().addQueue(tx);
    }

    @Override
    public void deleteQueue(TxQueue tx) {
        db.txQueueDao().deleteQueue(tx);
    }

    public TxQueueAndStatus getTxByNonce(String chainID, String userPk, long nonce) {
        return db.txQueueDao().getTxByNonce(chainID, userPk, nonce);
    }

    @Override
    public TxQueueAndStatus getTxQueueByID(long queueID) {
        return db.txQueueDao().getTxQueueByID(queueID);
    }

    @Override
    public List<TxQueueAndStatus> getResendNoteTxQueue(String senderPk) {
        return db.txQueueDao().getResendNoteTxQueue(senderPk);
    }

    @Override
    public List<TxQueueAndStatus> getResendNewsTxQueue(String senderPk) {
        return db.txQueueDao().getResendNewsTxQueue(senderPk);
    }

    @Override
    public List<String> getNeedSendingTxCommunities(String userPk) {
        return db.txQueueDao().getNeedSendingTxCommunities(userPk);
    }

    @Override
    public List<TxQueueAndStatus> getCommunityTxQueue(String chainID, String userPk) {
        return db.txQueueDao().getCommunityTxQueue(chainID, userPk);
    }

    @Override
    public Flowable<List<TxQueueAndStatus>> observeCommunityTxQueue(String chainID, String userPk) {
        return db.txQueueDao().observeCommunityTxQueue(chainID, userPk);
    }

    @Override
    public int getAirdropCount(String chainID, String currentPk, long currentTime) {
        return db.txQueueDao().getAirdropCount(chainID, currentPk, currentTime);
    }

    @Override
    public int getReferralCount(String chainID, String currentPk, String friendPk, long currentTime) {
        return db.txQueueDao().getReferralCount(chainID, currentPk, friendPk, currentTime);
    }

    @Override
    public TxQueue getAirdropTxQueue(String chainID, String currentPk, String friendPk) {
        return db.txQueueDao().getAirdropTxQueue(chainID, currentPk, friendPk);
    }

    @Override
    public Flowable<Integer> observeAirdropCountOnChain(String chainID, String senderPk, long currentTime) {
        return db.txQueueDao().observeAirdropCountOnChain(chainID, senderPk, currentTime);
    }

    @Override
    public Flowable<List<AirdropHistory>> observeAirdropHistoryOnChain(String chainID, String senderPk, long currentTime) {
        return db.txQueueDao().observeAirdropHistoryOnChain(chainID, senderPk, currentTime);
    }
}
