package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

/**
 * 数据库存储社区Member实体类
 */
@Entity(tableName = "Members", primaryKeys = {"chainID", "publicKey"})
public class Member implements Parcelable {
    @NonNull
    public String chainID;              // 成员所属社区的chainID
    @NonNull
    public String publicKey;            // 成员的公钥
    public long balance;                // 成员的balance
    public long nonce;                  // 成员的nonce
    public int airdropStatus;           // 发币开关状态
    public int airdropMembers;          // 总的发币成员数
    public long airdropCoins;           // 每次发币的coins
    public long airdropTime;            // 开始发币的时间
    public int msgUnread;               // 是否存在消息未读 0：已读，1：未读
    public int stickyTop;               // 是否置顶 0：不置顶，1：置顶

    public Member(@NonNull String chainID, @NonNull String publicKey){
        this.chainID = chainID;
        this.publicKey = publicKey;
    }

    @Ignore
    public Member(@NonNull String chainID, @NonNull String publicKey, long balance, long nonce) {
        this.chainID = chainID;
        this.publicKey = publicKey;
        this.balance = balance;
        this.nonce = nonce;
    }

    protected Member(Parcel in) {
        chainID = in.readString();
        publicKey = in.readString();
        balance = in.readLong();
        nonce = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(chainID);
        dest.writeString(publicKey);
        dest.writeLong(balance);
        dest.writeLong(nonce);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Member> CREATOR = new Creator<Member>() {
        @Override
        public Member createFromParcel(Parcel in) {
            return new Member(in);
        }

        @Override
        public Member[] newArray(int size) {
            return new Member[size];
        }
    };

    @Override
    public int hashCode() {
        return publicKey.hashCode() + chainID.hashCode();
    }

    @Override
    public boolean equals(Object o) {

        return o instanceof Member && (o == this || (
                publicKey.equals(((Member)o).publicKey) &&
                        chainID.equals(((Member)o).chainID)));
    }
}
