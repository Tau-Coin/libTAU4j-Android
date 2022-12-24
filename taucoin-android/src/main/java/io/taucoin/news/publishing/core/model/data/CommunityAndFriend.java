package io.taucoin.news.publishing.core.model.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Ignore;
import androidx.room.Relation;
import io.taucoin.news.publishing.core.Constants;
import io.taucoin.news.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 查询Communities 和Friends, 首页显示
 */
public class CommunityAndFriend implements Parcelable {
    // 社区相关数据
    public long balance;                    // 成员在此社区的balance
    public long balUpdateTime;              // 成员在此社区的balance更新时间
    public long power;                      // 成员在此社区的power
    public long nonce;                      // 成员在此社区的nonce
    public long headBlock;                  // 社区头区块号
    public int joined;                      // 是否加入此社区

    // 朋友相关数据
    public String senderPk;                 // ChatMsg中的消息发送者
    public String receiverPk;               // ChatMsg中的消息接收者

    // 社区和朋友共有数据
    public String ID;                       // 社区ID或朋友公钥
    public int type;                        // 消息类型 0: 社区， 1：朋友
    public int msgUnread;                   // 消息是否未读
    public int stickyTop;                   // 是否置顶
    public int focused;                     // 是否被关注
    public byte[] msg;                      // 交易备注信息
    public String memo;                     // 交易备注信息
    public long timestamp;                  // 交易时间戳

    @Relation(parentColumn = "ID",
            entityColumn = "publicKey")
    public User friend;

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CommunityAndFriend && (o == this || ID.equals(((CommunityAndFriend)o).ID));
    }

    public CommunityAndFriend() {

    }

    @Ignore
    protected CommunityAndFriend(Parcel in) {
        balance = in.readLong();
        headBlock = in.readLong();
        joined = in.readInt();
        senderPk = in.readString();
        receiverPk = in.readString();
        ID = in.readString();
        type = in.readInt();
        msgUnread = in.readInt();
        stickyTop = in.readInt();
        msg = in.createByteArray();
        memo = in.readString();
        timestamp = in.readLong();
        friend = in.readParcelable(User.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(balance);
        dest.writeLong(headBlock);
        dest.writeInt(joined);
        dest.writeString(senderPk);
        dest.writeString(receiverPk);
        dest.writeString(ID);
        dest.writeInt(type);
        dest.writeInt(msgUnread);
        dest.writeInt(stickyTop);
        dest.writeByteArray(msg);
        dest.writeString(memo);
        dest.writeLong(timestamp);
        dest.writeParcelable(friend, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CommunityAndFriend> CREATOR = new Creator<CommunityAndFriend>() {
        @Override
        public CommunityAndFriend createFromParcel(Parcel in) {
            return new CommunityAndFriend(in);
        }

        @Override
        public CommunityAndFriend[] newArray(int size) {
            return new CommunityAndFriend[size];
        }
    };

    public boolean onChain() {
        return balance > 0 || nonce > 0;
    }

    public long getInterimBalance() {
        return balance + Constants.TX_MAX_OVERDRAFT;
    }
}
