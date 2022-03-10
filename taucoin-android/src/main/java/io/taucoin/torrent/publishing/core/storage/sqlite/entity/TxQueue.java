package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import io.taucoin.torrent.publishing.core.utils.DateUtil;

/**
 * Room: 数据库存储TxQueues实体类
 */
@Entity(tableName = "TxQueues")
public class TxQueue implements Parcelable {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    public long queueID;                    // 队列ID
    @NonNull
    public String chainID;                  // 交易所属社区chainID
    @NonNull
    public String senderPk;                 // 交易发送者的公钥
    @NonNull
    public String receiverPk;               // 交易接收者的公钥
    public int queueType;                   // 队列类型，0：手动触发，1：airdrop自动触发 2: account renewal自动触发
    public long queueTime;                  // 添加队列的时间
    public long amount;                     // 交易金额
    public long fee;                        // 交易费
    public String memo;                     // 交易的备注、描述等

    public TxQueue(@NonNull String chainID, @NonNull String senderPk, @NonNull String receiverPk,
                   long amount, long fee, String memo) {
        this(chainID, senderPk, receiverPk, amount, fee, 0, memo);
    }

    @Ignore
    public TxQueue(@NonNull String chainID, @NonNull String senderPk, @NonNull String receiverPk,
                   long amount, long fee, int queueType, String memo) {
        this.chainID = chainID;
        this.senderPk = senderPk;
        this.receiverPk = receiverPk;
        this.amount = amount;
        this.fee = fee;
        this.memo = memo;
        this.queueType = queueType;
        this.queueTime = DateUtil.getMillisTime();
    }

    protected TxQueue(Parcel in) {
        queueID = in.readLong();
        chainID = in.readString();
        senderPk = in.readString();
        receiverPk = in.readString();
        amount = in.readLong();
        fee = in.readLong();
        memo = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(queueID);
        dest.writeString(chainID);
        dest.writeString(senderPk);
        dest.writeString(receiverPk);
        dest.writeLong(amount);
        dest.writeLong(fee);
        dest.writeString(memo);
    }

    public static final Creator<TxQueue> CREATOR = new Creator<TxQueue>() {
        @Override
        public TxQueue createFromParcel(Parcel in) {
            return new TxQueue(in);
        }

        @Override
        public TxQueue[] newArray(int size) {
            return new TxQueue[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int hashCode() {
        return String.valueOf(queueID).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TxQueue && (o == this || ((TxQueue) o).queueID == queueID);
    }
}
