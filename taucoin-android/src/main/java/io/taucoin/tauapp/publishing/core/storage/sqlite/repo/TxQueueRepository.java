package io.taucoin.tauapp.publishing.core.storage.sqlite.repo;

import java.util.List;

import io.reactivex.Flowable;
import io.taucoin.tauapp.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.tauapp.publishing.core.model.data.AirdropHistory;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.TxQueue;

/**
 * 提供外部操作TxQueue数据的接口
 */
public interface TxQueueRepository {

    /**
     * 更新交易队列
     */
    void updateQueue(TxQueue tx);

    /**
     * 添加交易进队列
     */
    long addQueue(TxQueue tx);

    /**
     * 删除交易进队列
     */
    void deleteQueue(TxQueue tx);

    TxQueueAndStatus getNonceFirstTx(String chainID, String userPk, long nonce);

    TxQueueAndStatus getTxQueueByID(long queueID);

    List<TxQueueAndStatus> getResendNoteTxQueue(String senderPk);

    List<TxQueueAndStatus> getResendNewsTxQueue(String senderPk);

    List<TxQueueAndStatus> getCommunityTxQueue(String chainID, String userPk);

    Flowable<List<TxQueueAndStatus>> observeCommunityTxQueue(String chainID, String userPk);

    List<String> getNeedWiringTxCommunities(String userPk);

    int getAirdropCount(String chainID, String currentPk, long currentTime);

    int getReferralCount(String chainID, String currentPk, String friendPk, long currentTime);

    TxQueue getAirdropTxQueue(String chainID, String currentPk, String friendPk);

    Flowable<Integer> observeAirdropCountOnChain(String chainID, String senderPk, long currentTime);

    Flowable<List<AirdropHistory>> observeAirdropHistoryOnChain(String chainID, String senderPk, long currentTime);

}
