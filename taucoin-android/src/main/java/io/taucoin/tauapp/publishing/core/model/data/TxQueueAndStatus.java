package io.taucoin.tauapp.publishing.core.model.data;

import androidx.annotation.NonNull;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.TxQueue;

/**
 * Room: 数据库存储TxQueues实体类
 */
public class TxQueueAndStatus extends TxQueue {
    public int status;                      // 交易发送状态
    public long nonce;                      // 最新一笔未上链交易的nonce
    public long timestamp;                  // 最新一笔未上链交易的时间

    public TxQueueAndStatus(@NonNull String chainID, @NonNull String senderPk, @NonNull String receiverPk,
                            long amount, long fee, int queueType, int txType, byte[] content) {
        super(chainID, senderPk, receiverPk, amount, fee, queueType, txType, content);
    }

//    public TxQueueAndStatus(@NonNull String chainID, @NonNull String senderPk, @NonNull String receiverPk,
//                            long amount, long fee, TxType txType, byte[] content) {
//        super(chainID, senderPk, receiverPk, amount, fee, txType, content);
//    }
	/* delete by tc
    public boolean isProcessing() {
        //return status == 0;
        return nonce > 0;
    }
	*/
}
