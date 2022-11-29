package io.taucoin.tauapp.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.core.model.data.ChatMsgAndLog;
import io.taucoin.tauapp.publishing.core.model.data.DataChanged;
import io.taucoin.tauapp.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;

/**
 * FriendRepository接口实现
 */
public class ChatRepositoryImpl implements ChatRepository{

    private Context appContext;
    private AppDatabase db;
    private PublishSubject<DataChanged> dataSetChangedPublish = PublishSubject.create();
    private ExecutorService sender = Executors.newSingleThreadExecutor();

    /**
     * FriendRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public ChatRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    @Override
    public void addChatMsg(ChatMsg chat) {
        db.chatDao().addChat(chat);
        submitDataSetChanged(chat);
    }

    @Override
    public void addChatMessages(ChatMsg... chats) {
        db.chatDao().addChats(chats);
        submitDataSetChanged(chats[0]);
    }

    @Override
    public void updateChatMsg(ChatMsg chat) {
        db.chatDao().updateChat(chat);
        submitDataSetChanged(chat);
    }

    @Override
    public ChatMsg queryChatMsg(String senderPk, String hash) {
        return db.chatDao().queryChatMsg(senderPk, hash);
    }

    /**
     * 查询ChatMsg
     * @param hash
     * @return
     */
    @Override
    public ChatMsg queryChatMsg(String hash) {
        return db.chatDao().queryChatMsg(hash);
    }

    @Override
    public Observable<DataChanged> observeDataSetChanged() {
        return dataSetChangedPublish;
    }

    @Override
    public void submitDataSetChanged(ChatMsg chat) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(chat.senderPk);
        stringBuilder.append(chat.receiverPk);
        stringBuilder.append(chat.receiverPk);
        stringBuilder.append(DateUtil.getDateTime());
        submitDataSetChangedDirect(stringBuilder);
    }

    private void submitDataSetChangedDirect(StringBuilder msg) {
        sender.submit(() -> {
            DataChanged result = new DataChanged();
            result.setMsg(msg.toString());
            dataSetChangedPublish.onNext(result);
        });
    }

    @Override
    public List<ChatMsgAndLog> getMessages(String friendPk, int startPosition, int loadSize) {
        String senderPk = MainApplication.getInstance().getPublicKey();
        return db.chatDao().getMessages(senderPk, friendPk, startPosition, loadSize);
    }

    @Override
    public List<ChatMsgLog> getResendMessages(String friendPk) {
        String senderPk = MainApplication.getInstance().getPublicKey();
        return db.chatDao().getResendMessages(senderPk, friendPk);
    }

    /**
     * 添加消息日志
     * @param msgLogs
     */
    @Override
    public void addChatMsgLogs(String friendPk, ChatMsgLog... msgLogs) {
        db.chatDao().addChatMsgLogs(msgLogs);
        StringBuilder stringBuilder = new StringBuilder();
        ChatMsgLog log = msgLogs[0];
        stringBuilder.append(friendPk);
        stringBuilder.append(log.hash);
        stringBuilder.append(log.status);
        stringBuilder.append(log.timestamp);
        stringBuilder.append(DateUtil.getDateTime());
        submitDataSetChangedDirect(stringBuilder);
    }

    /**
     * 观察消息日志
     * @param hash
     * @return
     */
    @Override
    public Observable<List<ChatMsgLog>> observerMsgLogs(String hash) {
        return db.chatDao().observerMsgLogs(hash);
    }

    /**
     * 查询聊天消息日志
     * @param hash
     * @param status
     * @return
     */
    @Override
    public ChatMsgLog queryChatMsgLog(String hash, int status) {
        return db.chatDao().queryChatMsgLog(hash, status);
    }

    /**
     * 获取发送的最后一条消息的时间
     * @param senderPk 发送者
     * @param receiverPk 接收者
     * @return 时间
     */
    @Override
    public long getLastSendTime(String senderPk, String receiverPk) {
        return db.chatDao().getLastSendTime(senderPk, receiverPk);
    }
}
