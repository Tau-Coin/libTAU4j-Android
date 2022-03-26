package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import io.taucoin.torrent.publishing.core.utils.StringUtil;

/**
 * Room: 数据库存储TxConfirm实体类
 */
@Entity(tableName = "TxConfirms", primaryKeys = {"txID", "peer"})
public class TxConfirm implements Parcelable, Comparable<TxConfirm>{
    @NonNull
    public String txID;                  // 交易所属社区chainID
    @NonNull
    public String peer;                 // 交易发送者的公钥
    public long timestamp;                  // 添加队列的时间

    public TxConfirm(@NonNull String txID, @NonNull String peer, long timestamp) {
        this.txID = txID;
        this.peer = peer;
        this.timestamp = timestamp;
    }


    protected TxConfirm(Parcel in) {
        txID = in.readString();
        peer = in.readString();
        timestamp = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(txID);
        dest.writeString(peer);
        dest.writeLong(timestamp);
    }

    public static final Creator<TxConfirm> CREATOR = new Creator<TxConfirm>() {
        @Override
        public TxConfirm createFromParcel(Parcel in) {
            return new TxConfirm(in);
        }

        @Override
        public TxConfirm[] newArray(int size) {
            return new TxConfirm[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int hashCode() {
        return txID.hashCode() + peer.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TxConfirm) {
            TxConfirm newObj = (TxConfirm) o;
            if (o == this || (StringUtil.isEquals(this.txID, newObj.txID) &&
                    StringUtil.isEquals(this.peer, newObj.peer)) ) {

            }
        }
        return false;
    }

    @Override
    public int compareTo(TxConfirm o) {
        if (this.timestamp > o.timestamp) {
            return -1;
        } else if (this.timestamp < o.timestamp) {
            return 1;
        }
        return 0;
    }
}
