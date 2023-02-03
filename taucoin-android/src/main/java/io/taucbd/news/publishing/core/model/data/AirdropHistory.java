package io.taucbd.news.publishing.core.model.data;

import androidx.room.Relation;
import io.taucbd.news.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 数据库存储TxQueues实体类
 */
public class AirdropHistory {
    public long queueID;                    // 队列ID
    public String receiverPk;               // 交易接收者的公钥

    @Relation(parentColumn = "receiverPk",
            entityColumn = "publicKey")
    public User friend;

    @Override
    public int hashCode() {
        return String.valueOf(queueID).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AirdropHistory && (o == this || ((AirdropHistory) o).queueID == queueID);
    }
}
