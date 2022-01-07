package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room: 数据库存储Transaction实体类
 */
@Entity(tableName = "Txs")
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

    public String receiverPk;               // 交易接收者的公钥 只针对TxType.WRING_TX类型
    public long amount;                     // 交易金额 只针对TxType.WRING_TX类型

    public int autoRenewal;                 // 交易是否为账户自动更新创建的交易 0：用户创建；1：自动创建

    public String coinName;                 // 币名 只针对TxType.SELL_TX类型
    public String link;                     // 用户link 只针对TxType.SELL_TX类型
    public String location;                 // 位置信息 只针对TxType.SELL_TX类型

    public Tx(@NonNull String chainID, String receiverPk, long amount, long fee, int txType, String memo){
        this.chainID = chainID;
        this.receiverPk = receiverPk;
        this.amount = amount;
        this.fee = fee;
        this.txType = txType;
        this.memo = memo;
    }

    @Ignore
    public Tx(@NonNull String chainID, long fee, int txType, String memo){
        this.chainID = chainID;
        this.fee = fee;
        this.txType = txType;
        this.memo = memo;
    }

    @Ignore
    public Tx(@NonNull String chainID, String receiverPk, long fee, int txType){
        this.chainID = chainID;
        this.receiverPk = receiverPk;
        this.fee = fee;
        this.txType = txType;
    }

    @Ignore
    public Tx(@NonNull String chainID, long fee, int txType, String coinName,
              String link, String location, String memo){
        this.chainID = chainID;
        this.fee = fee;
        this.txType = txType;
        this.coinName = coinName;
        this.link = link;
        this.location = location;
        this.memo = memo;
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
        autoRenewal = in.readInt();
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
        dest.writeInt(autoRenewal);
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
