package io.taucbd.news.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 数据库存储Community实体类
 */
@Entity(tableName = "Communities")
public class Community implements Parcelable {
    @NonNull
    @PrimaryKey
    public String chainID;                  // 社区的chainID
    @NonNull
    public String communityName;            // 社区名字

    public long syncingHeadBlock;           // 正在同步（下载）的头部区块
    public long headBlock;                  // 真实的头部区块号
    public long difficulty;                 // 社区最大难度值
    public long consensusBlock;             // 共识点区块
    public boolean isBanned = false;        // 社区是否被用户拉入黑名单
    public String forkPoint;                // 社区分叉点区块号

    public Community(@NonNull String chainID, @NonNull String communityName){
        this.communityName = communityName;
        this.chainID = chainID;
    }

    @Ignore
    protected Community(Parcel in) {
        chainID = in.readString();
        communityName = in.readString();
        syncingHeadBlock = in.readLong();
        headBlock = in.readLong();
        consensusBlock = in.readLong();
        isBanned = in.readByte() != 0;
        forkPoint = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(chainID);
        dest.writeString(communityName);
        dest.writeLong(syncingHeadBlock);
        dest.writeLong(headBlock);
        dest.writeLong(consensusBlock);
        dest.writeByte((byte) (isBanned ? 1 : 0));
        dest.writeString(forkPoint);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Community> CREATOR = new Creator<Community>() {
        @Override
        public Community createFromParcel(Parcel in) {
            return new Community(in);
        }

        @Override
        public Community[] newArray(int size) {
            return new Community[size];
        }
    };

    @Override
    public int hashCode() {
        return chainID.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Community && (o == this || chainID.equals(((Community)o).chainID));
    }
}
