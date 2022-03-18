package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;

/**
 * 数据库存储区块的实体类
 */
@Entity(tableName = "Blocks", primaryKeys = {"chainID", "blockHash"},
        indices = {@Index(value = {"chainID", "blockNumber"})})
public class BlockInfo implements Parcelable {
    @NonNull
    public String chainID;             // 区块所属的链ID
    @NonNull
    public String blockHash;           // 区块hash
    public long blockNumber;           // 区块号
    @NonNull
    public String miner;               // 出块者
    public long rewards;               // 出块奖励
    public long difficulty;            // 区块难度
    public int status;                 // 区块的状态 0：未上链；1：上链成功;
    public long timestamp;             // 区块时间戳
    public String previousBlockHash;   // 上一个区块hash


    public BlockInfo(@NonNull String chainID, @NonNull String blockHash, long blockNumber,
                     @NonNull String miner, long rewards, long difficulty, int status,
                     long timestamp, String previousBlockHash) {
        this.chainID = chainID;
        this.blockHash = blockHash;
        this.blockNumber = blockNumber;
        this.miner = miner;
        this.rewards = rewards;
        this.difficulty = difficulty;
        this.status = status;
        this.timestamp = timestamp;
        this.previousBlockHash = previousBlockHash;
    }

    protected BlockInfo(Parcel in) {
        chainID = Objects.requireNonNull(in.readString());
        blockHash = Objects.requireNonNull(in.readString());
        blockNumber = in.readLong();
        miner = Objects.requireNonNull(in.readString());
        rewards = in.readLong();
        difficulty = in.readLong();
        status = in.readInt();
        timestamp = in.readLong();
        previousBlockHash = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(chainID);
        dest.writeString(blockHash);
        dest.writeLong(blockNumber);
        dest.writeString(miner);
        dest.writeLong(rewards);
        dest.writeLong(difficulty);
        dest.writeInt(status);
        dest.writeLong(timestamp);
        dest.writeString(previousBlockHash);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BlockInfo> CREATOR = new Creator<BlockInfo>() {
        @Override
        public BlockInfo createFromParcel(Parcel in) {
            return new BlockInfo(in);
        }

        @Override
        public BlockInfo[] newArray(int size) {
            return new BlockInfo[size];
        }
    };

    @Override
    public int hashCode() {
        return chainID.hashCode() + blockHash.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockInfo && (o == this || (
                chainID.equals(((BlockInfo) o).chainID) &&
                        blockHash.equals(((BlockInfo) o).blockHash)));
    }
}
