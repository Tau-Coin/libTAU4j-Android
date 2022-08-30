package io.taucoin.tauapp.publishing.core.storage.sqlite.repo;

import java.util.List;

import io.reactivex.Observable;
import io.taucoin.tauapp.publishing.core.model.data.ChatMsgAndLog;
import io.taucoin.tauapp.publishing.core.model.data.DataChanged;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.ChatMsgLog;

/**
 * 提供操作Friend数据的接口
 */
public interface ChatRepository {

    /**
     * 添加Chats
     */
    void addChatMsg(ChatMsg chat);

    /**
     * 添加多条Chats
     */
    void addChatMessages(ChatMsg... chats);

    /**
     * 更新Chat
     */
    void updateChatMsg(ChatMsg chat);

    /**
     * 查询ChatMsg
     * @param senderPk
     * @param hash
     * @return
     */
    ChatMsg queryChatMsg(String senderPk, String hash);

    /**
     * 查询ChatMsg
     * @param hash
     * @return
     */
    ChatMsg queryChatMsg(String hash);

    /**
     * 观察社区的消息的变化
     */
    Observable<DataChanged> observeDataSetChanged();

    /**
     * 提交数据变
     */
    void submitDataSetChanged(ChatMsg chat);

    List<ChatMsgAndLog> getMessages(String friendPk, int pos, int loadSize);

    /**
     * 添加消息日志
     * @param msgLogs
     */
    void addChatMsgLogs(String friendPk, ChatMsgLog... msgLogs);

    /**
     * 观察消息日志
     * @param hash
     * @return
     */
    Observable<List<ChatMsgLog>> observerMsgLogs(String hash);

    /**
     * 查询聊天消息日志
     * @param hash
     * @param status
     * @return
     */
    ChatMsgLog queryChatMsgLog(String hash, int status);

    /**
     * 获取发送的最后一条消息的时间
     * @param senderPk 发送者
     * @param receiverPk 接收者
     * @return 时间
     */
    long getLastSendTime(String senderPk, String receiverPk);
}
