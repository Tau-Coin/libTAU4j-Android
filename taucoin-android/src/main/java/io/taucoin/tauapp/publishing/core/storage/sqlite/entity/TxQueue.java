package io.taucoin.tauapp.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import io.taucoin.tauapp.publishing.core.model.data.message.TxType;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;

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
    public int queueType;                   // 队列类型，0：手动触发，1：airdrop自动触发
    public long queueTime;                  // 添加队列的时间
    public long amount;                     // 交易金额
    public long fee;                        // 交易费
    public String memo;                     // 数据库版本为1字段，新版本无用
    public byte[] content;                  // 交易内容编码后内容
    public int txType;                      // 交易类型

    @Ignore
    public TxQueue(@NonNull String chainID, @NonNull String senderPk, @NonNull String receiverPk,
                   long amount, long fee, TxType txType, byte[] content) {
        this(chainID, senderPk, receiverPk, amount, fee, 0, txType.getType(), content);
    }

    public TxQueue(@NonNull String chainID, @NonNull String senderPk, @NonNull String receiverPk,
                   long amount, long fee, int queueType, int txType, byte[] content) {
        this.chainID = chainID;
        this.senderPk = senderPk;
        this.receiverPk = receiverPk;
        this.amount = amount;
        this.fee = fee;
        this.queueType = queueType;
        this.txType = txType;
        this.queueTime = DateUtil.getMillisTime();
        this.content = content;
    }

    protected TxQueue(Parcel in) {
        queueID = in.readLong();
        chainID = in.readString();
        senderPk = in.readString();
        receiverPk = in.readString();
        amount = in.readLong();
        fee = in.readLong();
        queueType = in.readInt();
        txType = in.readInt();
        queueTime = in.readLong();
        int len = in.readInt();
        if (len > 0) {
            content = new byte[len];
            in.readByteArray(content);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(queueID);
        dest.writeString(chainID);
        dest.writeString(senderPk);
        dest.writeString(receiverPk);
        dest.writeLong(amount);
        dest.writeLong(fee);
        dest.writeInt(queueType);
        dest.writeInt(txType);
        dest.writeLong(queueTime);
        dest.writeInt(content != null ? content.length : 0);
        dest.writeByteArray(content);
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
