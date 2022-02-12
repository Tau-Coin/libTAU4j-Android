package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.taucoin.torrent.publishing.core.model.data.DataChanged;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.ui.community.CommunityTabs;

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
    public void addQueue(TxQueue tx) {
        db.txQueueDao().addQueue(tx);
    }

    @Override
    public void deleteQueue(TxQueue tx) {
        db.txQueueDao().deleteQueue(tx);
    }

    @Override
    public Observable<List<TxQueueAndStatus>> observeCommunityTxQueue(String chainID, String userPk) {
        return db.txQueueDao().observeCommunityTxQueue(chainID, userPk);
    }

    @Override
    public List<TxQueueAndStatus> getCommunityTxQueue(String chainID, String userPk) {
        return db.txQueueDao().getCommunityTxQueue(chainID, userPk);
    }

    @Override
    public TxQueueAndStatus getQueueFirstTx(String chainID, String userPk) {
        return db.txQueueDao().getQueueFirstTx(chainID, userPk);
    }

    @Override
    public List<String> getNeedWiringTxCommunities(String userPk) {
        return db.txQueueDao().getNeedWiringTxCommunities(userPk);
    }

    @Override
    public int getAirdropCount(String chainID, String currentPk, long currentTime) {
        return db.txQueueDao().getAirdropCount(chainID, currentPk, currentTime);
    }

    @Override
    public TxQueue getAirdropTxQueue(String chainID, String currentPk, String friendPk) {
        return db.txQueueDao().getAirdropTxQueue(chainID, currentPk, friendPk);
    }

    @Override
    public Observable<Integer> observeAirdropCountOnChain(String chainID, String senderPk, long currentTime) {
        return db.txQueueDao().observeAirdropCountOnChain(chainID, senderPk, currentTime);
    }
}
