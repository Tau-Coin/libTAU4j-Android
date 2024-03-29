package io.taucbd.news.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room: 数据库存储Transaction实体类
 */
@Entity(tableName = "Txs", indices = {
        @Index(value = {"chainID", "timestamp"})})
public class Tx implements Parcelable {
    @NonNull
    @PrimaryKey
    public String txID;                     // 交易ID
    @NonNull
    public String chainID;                  // 交易所属社区chainID
    @NonNull
    public String senderPk;                 // 交易发送者的公钥
    public long fee;                        // 交易费
    public long timestamp;                  // 交易时间戳
    public long nonce;                      // 交易nonce
    public int txType;                      // 交易类型，同TxType中枚举类型
    public String memo;                     // 交易的备注、描述等
    public int txStatus;                    // 交易的状态 0：未上链（在交易池中）；1：上链成功 (不上链)
    public long blockNumber;                // 交易所属的区块号
    public String blockHash;                // 交易所属的区块哈希

    public int version;                     // 交易version;
    public String previousHash;             // notes交易上一个消息hash;
    public String receiverPk;               // 交易接收者的公钥 只针对TxType.WRING_TX类型, news交易为针对回复的pubkey
    public long amount;                     // 交易金额 只针对TxType.WRING_TX类型

    public String link;                     // 用户link 只针对TxType.SELL_TX类型,  notes交易, news交易
    public String repliedHash;              // 针对与notes交易和news交易回复的hash
    public String repliedKey;               // 针对news交易回复的key
    public long queueID = -1;               // 交易对应的队列ID
    public long pinnedTime;                 // 置顶固定时间
    public long favoriteTime;               // 收藏时间
    public int deleted;                     // news是否被删除 0：未删除； 1：已删除
    public String picturePath;              // news图片路径；

    // wiring
    public Tx(@NonNull String chainID, String receiverPk, long amount, long fee, int txType, String memo){
        this.chainID = chainID;
        this.receiverPk = receiverPk;
        this.amount = amount;
        this.fee = fee;
        this.txType = txType;
        this.memo = memo;
    }

    @Ignore
    public Tx(@NonNull String txid, @NonNull String chainID, String senderPk, String receiverPk,
              long amount, long fee, long timestamp, int txType){
        this.txID = txid;
        this.chainID = chainID;
        this.senderPk = senderPk;
        this.receiverPk = receiverPk;
        this.amount = amount;
        this.fee = fee;
        this.txType = txType;
        this.timestamp = timestamp;
    }

    // notes
    @Ignore
    public Tx(@NonNull String chainID, long fee, int txType, String memo){
        this.chainID = chainID;
        this.fee = fee;
        this.txType = txType;
        this.memo = memo;
    }
    @Ignore
    public Tx(@NonNull String chainID, long fee, int txType, String memo, String link, String repliedHash){
        this.chainID = chainID;
        this.fee = fee;
        this.txType = txType;
        this.memo = memo;
        this.link = link;
        this.repliedHash = repliedHash;
    }

	// news
    @Ignore
    public Tx(@NonNull String chainID, long fee, int txType, String memo, String link, String repliedHash, String repliedKey){
        this.chainID = chainID;
        this.fee = fee;
        this.txType = txType;
        this.memo = memo;
        this.link = link;
        this.repliedHash = repliedHash;
        this.repliedKey = repliedKey; //当作@的key使用
    }
	
    @Ignore
    public Tx( @NonNull String txID, @NonNull String chainID, long fee){
        this.txID = txID;
        this.chainID = chainID;
        this.fee = fee;
    }

    @Ignore
    private Tx(Parcel in) {
        txID = in.readString();
        chainID = in.readString();
        amount = in.readLong();
        fee = in.readLong();
        senderPk = in.readString();
        receiverPk = in.readString();
        memo = in.readString();
        timestamp = in.readLong();
        nonce = in.readLong();
        txType = in.readInt();
        txStatus = in.readInt();
        blockNumber = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(txID);
        dest.writeString(chainID);
        dest.writeLong(amount);
        dest.writeLong(fee);
        dest.writeString(senderPk);
        dest.writeString(receiverPk);
        dest.writeString(memo);
        dest.writeLong(timestamp);
        dest.writeLong(nonce);
        dest.writeInt(txType);
        dest.writeInt(txStatus);
        dest.writeLong(blockNumber);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Tx> CREATOR = new Creator<Tx>() {
        @Override
        public Tx createFromParcel(Parcel in) {
            return new Tx(in);
        }

        @Override
        public Tx[] newArray(int size) {
            return new Tx[size];
        }
    };

    @Override
    public int hashCode() {
        return txID.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Tx && (o == this || txID.equals(((Tx)o).txID));
    }
}
