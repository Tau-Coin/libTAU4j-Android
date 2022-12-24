package io.taucoin.news.publishing.core.model.data;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucoin.news.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.news.publishing.core.storage.sqlite.entity.ChatMsgLog;

/**
 * Room: 数据库存储Chat实体类
 */
public class ChatMsgAndLog extends ChatMsg {

    @Relation(parentColumn = "hash",
            entityColumn = "hash")
    public List<ChatMsgLog> logs;

    public ChatMsgAndLog(@NonNull String hash, String senderPk, String receiverPk, int contentType, long timestamp, String logicMsgHash) {
        super(hash, senderPk, receiverPk, contentType, timestamp, logicMsgHash);
    }
}