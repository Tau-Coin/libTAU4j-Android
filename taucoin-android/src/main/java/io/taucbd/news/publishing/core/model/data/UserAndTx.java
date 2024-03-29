package io.taucbd.news.publishing.core.model.data;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.Ignore;
import androidx.room.Relation;
import io.taucbd.news.publishing.core.Constants;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Tx;
import io.taucbd.news.publishing.core.storage.sqlite.entity.TxLog;
import io.taucbd.news.publishing.core.storage.sqlite.entity.User;
import io.taucbd.news.publishing.core.utils.DateUtil;

/**
 * Room: 包含被回复的交易信息的实体类
 */
public class UserAndTx extends Tx {

    @Relation(parentColumn = "senderPk",
            entityColumn = "publicKey")
    public User sender;                     // 交易发送者对应的用户信息
    @Relation(parentColumn = "receiverPk",
            entityColumn = "publicKey")
    public User receiver;                   // 交易接受者对应的用户信息

    public int repliesNum; //news回复记录数
    public int chatsNum; //聊天回复记录数
    public long balance;
    public long power;

    @Relation(parentColumn = "txID",
            entityColumn = "hash")
    public List<TxLog> logs;

    @Ignore
    public long currentTime = DateUtil.getTime() / 60;

    public UserAndTx(@NonNull String chainID, String receiverPk, long amount, long fee, int txType, String memo) {
        super(chainID, receiverPk, amount, fee, txType, memo);
    }

    @Ignore
    public UserAndTx(@NonNull String chainID, long fee, int txType, String memo) {
        super(chainID, fee, txType, memo);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    public long getInterimBalance() {
        return balance + Constants.TX_MAX_OVERDRAFT;
    }
}
