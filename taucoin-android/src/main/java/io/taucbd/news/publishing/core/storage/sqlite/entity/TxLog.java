package io.taucbd.news.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

/**
 * Room: 数据库存储ChatMsgLog实体类
 * 社区交易日志
 */
@Entity(tableName = "TxLogs", primaryKeys = {"hash", "status", "timestamp"})
public class TxLog implements Parcelable, Comparable<TxLog> {
    @NonNull
    public String hash;                    // 消息的Hash
    @NonNull
    public int status;                     // 消息状态 -1: 消息构建，0: 1: 消息同步确认
    public long timestamp;                 // 消息状态对应的时间

    public TxLog(@NonNull String hash, int status, long timestamp){
        this.hash = hash;
        this.status = status;
        this.timestamp = timestamp;
    }

    @Ignore
    private TxLog(Parcel in) {
        hash = in.readString();
        status = in.readInt();
        timestamp = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(hash);
        dest.writeInt(status);
        dest.writeLong(timestamp);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TxLog> CREATOR = new Creator<TxLog>() {
        @Override
        public TxLog createFromParcel(Parcel in) {
            return new TxLog(in);
        }

        @Override
        public TxLog[] newArray(int size) {
            return new TxLog[size];
        }
    };

    @Override
    public int hashCode() {
        return hash.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TxLog && (o == this || hash .equals (((TxLog)o).hash));
    }

    @Override
    public int compareTo(TxLog o) {
        if (this.status > o.status) {
            return -1;
        } else if (this.status < o.status) {
            return 1;
        }
        return 0;
    }
}