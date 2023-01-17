package io.taucoin.news.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Ignore;
import androidx.room.Relation;
import io.taucoin.news.publishing.core.Constants;
import io.taucoin.news.publishing.core.storage.sqlite.entity.User;
import io.taucoin.news.publishing.core.utils.DateUtil;

/**
 * Room: 包含被回复的交易信息的实体类
 */
public class UserAndTxReply extends UserAndTx {
    public String replyTxID;
    public String replySenderPk;
    public String replyMemo;
    public String replyLink;
    public long replyTimestamp;
    public long replyBalance;
    public long replyPower;

    @Relation(parentColumn = "replySenderPk",
            entityColumn = "publicKey")
    public User replySender;

    @Ignore
    public long currentTime = DateUtil.getTime() / 60;

    public UserAndTxReply(@NonNull String chainID, String receiverPk, long amount, long fee, int txType, String memo) {
        super(chainID, receiverPk, amount, fee, txType, memo);
    }

    @Ignore
    public UserAndTxReply(@NonNull String chainID, long fee, int txType, String memo) {
        super(chainID, fee, txType, memo);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    public long getReplyInterimBalance() {
        return replyBalance + Constants.TX_MAX_OVERDRAFT;
    }
}
