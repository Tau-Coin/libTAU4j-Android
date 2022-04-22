package io.taucoin.torrent.publishing.core.model.data;

import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 查询Communities 和Friends, 首页显示
 */
public class CommunityAndFriend {
    // 社区相关数据
    public long balance;                    // 成员在此社区的balance
    public long power;                      // 成员在此社区的power
    public long blockNumber;                // 成员状态改变的区块号
    public long headBlock;                  // 社区头区块号
    public long tailBlock;                  // 社区尾区块号
    public int joined;                      // 是否加入此社区

    // 朋友相关数据
    public String senderPk;                 // ChatMsg中的消息发送者
    public String receiverPk;               // ChatMsg中的消息接收者

    // 社区和朋友共有数据
    public String ID;                       // 社区ID或朋友公钥
    public int type;                        // 消息类型 0: 社区， 1：朋友
    public int msgUnread;                   // 消息是否未读
    public int stickyTop;                   // 是否置顶
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

    public boolean isReadOnly() {
        return power <=0 || blockNumber < tailBlock;
    }
}
