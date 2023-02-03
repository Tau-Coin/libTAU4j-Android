package io.taucbd.news.publishing.core.storage.sqlite.repo;

import java.util.List;

import io.reactivex.Flowable;
import io.taucbd.news.publishing.core.model.data.TxQueueAndStatus;
import io.taucbd.news.publishing.core.model.data.AirdropHistory;
import io.taucbd.news.publishing.core.storage.sqlite.entity.TxQueue;

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

    /**
     * 通过Nonce获取Tx
     */
    TxQueueAndStatus getTxByNonce(String chainID, String userPk, long nonce);

    /**
     * 通过QueueID获取Tx
     */
    TxQueueAndStatus getTxQueueByID(long queueID);

    /**
     * 获取当前用户需要重新发送的note tx
     */
    List<TxQueueAndStatus> getResendNoteTxQueue(String senderPk);

    List<TxQueueAndStatus> getResendNewsTxQueue(String senderPk);

    List<String> getNeedSendingTxCommunities(String userPk);

    /**
     * 获取某个key下在特定chainid下的所有未上链的交易, 目前只用于Airdrop的机制
     */
    List<TxQueueAndStatus> getCommunityTxQueue(String chainID, String userPk);

    /**
     * 获取某个key下在特定chainid下的所有未上链的交易, 显示于R.string.community_view_own_txs, 利于编辑
     */
    Flowable<List<TxQueueAndStatus>> observeCommunityTxQueue(String chainID, String userPk);

    //以下是airdrop机制，暂时不管
    int getAirdropCount(String chainID, String currentPk, long currentTime);

    int getReferralCount(String chainID, String currentPk, String friendPk, long currentTime);

    TxQueue getAirdropTxQueue(String chainID, String currentPk, String friendPk);

    Flowable<Integer> observeAirdropCountOnChain(String chainID, String senderPk, long currentTime);

    Flowable<List<AirdropHistory>> observeAirdropHistoryOnChain(String chainID, String senderPk, long currentTime);
}
