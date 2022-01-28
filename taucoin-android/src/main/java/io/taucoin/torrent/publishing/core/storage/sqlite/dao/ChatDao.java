package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Observable;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgAndLog;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;

/**
 * Room:User操作接口
 */
@Dao
public interface ChatDao {
    String QUERY_GET_CHAT_MSG = "SELECT * FROM ChatMessages WHERE senderPk = :senderPk AND hash = :hash";
    String QUERY_GET_CHAT_MSG_BY_HASH = "SELECT * FROM ChatMessages WHERE hash = :hash";

    String QUERY_MESSAGES_WHERE = " WHERE ((msg.senderPk = :senderPk AND msg.receiverPk = :receiverPk)" +
            " OR (msg.senderPk = :receiverPk AND msg.receiverPk = :senderPk))";

    String QUERY_MESSAGES_BY_FRIEND_PK = "SELECT msg.*" +
            " FROM ChatMessages msg" +
            QUERY_MESSAGES_WHERE +
            " ORDER BY msg.timestamp DESC, msg.logicMsgHash COLLATE UNICODE DESC" +
            " LIMIT :loadSize OFFSET :startPosition ";

    // 查询消息的所有日志
    String QUERY_CHAT_MSG_LOGS = "SELECT * FROM ChatMsgLogs WHERE hash = :hash" +
            " ORDER BY status DESC";

    // 查询消息单独状态日志数据
    String QUERY_CHAT_MSG_LOG = "SELECT * FROM ChatMsgLogs WHERE hash = :hash AND status = :status";

    // 获取发送的最后一条消息的时间
    String QUERY_LAST_SEND_TIME = "SELECT timestamp FROM ChatMessages" +
            " WHERE senderPk = :senderPk AND receiverPk = :receiverPk" +
            " ORDER BY timestamp DESC" +
            " LIMIT 1";

    /**
     * 添加聊天信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addChat(ChatMsg msg);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] addChats(ChatMsg... msg);

    /**
     * 更新聊天信息
     */
    @Update
    int updateChat(ChatMsg msg);

    /**
     * 获取当前的用户
     */
    @Query(QUERY_GET_CHAT_MSG)
    ChatMsg queryChatMsg(String senderPk, String hash);

    @Query(QUERY_GET_CHAT_MSG_BY_HASH)
    ChatMsg queryChatMsg(String hash);

    /**
     * 获取聊天的消息
     * @param receiverPk 朋友公钥
     * @param startPosition 数据开始位置
     * @param loadSize 加载数据大小
     * @return List<Chat>
     */
    @Query(QUERY_MESSAGES_BY_FRIEND_PK)
    @Transaction
    List<ChatMsgAndLog> getMessages(String senderPk, String receiverPk, int startPosition, int loadSize);

    /**
     * 添加消息日志
     */
    @Insert()
    void addChatMsgLogs(ChatMsgLog... msgLog);

    @Query(QUERY_CHAT_MSG_LOGS)
    Observable<List<ChatMsgLog>> observerMsgLogs(String hash);

    /**
     * 查询聊天消息日志
     */
    @Query(QUERY_CHAT_MSG_LOG)
    ChatMsgLog queryChatMsgLog(String hash, int status);

    /**
     * 获取发送的最后一条消息的时间
     */
    @Query(QUERY_LAST_SEND_TIME)
    long getLastSendTime(String senderPk, String receiverPk);
}