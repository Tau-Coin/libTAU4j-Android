package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import java.util.List;

import io.reactivex.Observable;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxQueue;

/**
 * 提供外部操作TxQueue数据的接口
 */
public interface TxQueueRepository {

    /**
     * 更新交易队列
     */
    void updateQueue(TxQueue tx);

    /**
     * 添加新的交易进队列
     */
    void addQueue(TxQueue tx);

    void deleteQueue(TxQueue tx);

    Observable<List<TxQueueAndStatus>> observeCommunityTxQueue(String chainID, String userPk);

    List<TxQueueAndStatus> getCommunityTxQueue(String chainID, String userPk);

    TxQueueAndStatus getQueueFirstTx(String chainID, String userPk);

    List<String> getNeedWiringTxCommunities(String userPk);

    int getAirdropCount(String chainID, String currentPk, long currentTime);

    TxQueue getAirdropTxQueue(String chainID, String currentPk, String friendPk);

    Observable<Integer> observeAirdropCountOnChain(String chainID, String senderPk, long currentTime);
}
