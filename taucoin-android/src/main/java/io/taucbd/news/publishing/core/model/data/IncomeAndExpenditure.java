package io.taucbd.news.publishing.core.model.data;

import io.taucbd.news.publishing.core.storage.sqlite.entity.User;

import androidx.room.Relation;

/**
 * Room: 钱包的收入和支出实体类
 */
public class IncomeAndExpenditure {

    @Relation(parentColumn = "senderOrMiner",
            entityColumn = "publicKey")
    public User sender;                     // 交易发送者对应的用户信息
    @Relation(parentColumn = "receiverPk",
            entityColumn = "publicKey")
    public User receiver;                   // 交易接受者对应的用户信息

    public String hash;                     // 交易hash
    public String senderOrMiner;            // 交易发生者或矿工
    public String receiverPk;               // 交易的接收者
    public long blockNumber;                // 区块号
    public int txType;                      // 交易类型
    public long amount;                     // 交易金额
    public long fee;                        // 交易费
    public long createTime;                 // 交易或区块创建时间
    public long onlineTime;                 // 区块时间（交易或区块上链时间）
    public int onlineStatus;                // 交易或区块是否上链

    //add for mining rewards
    public IncomeAndExpenditure(String senderOrMiner, long amount, long createTime) {
        this.senderOrMiner = senderOrMiner;
        this.txType = -1;
        this.amount = amount;
        this.createTime = createTime;
        this.hash = "0000000000000000000000000000000000000000";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IncomeAndExpenditure && (o == this ||
                hash.equals(((IncomeAndExpenditure)o).hash));
    }
}
