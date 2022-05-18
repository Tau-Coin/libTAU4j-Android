package io.taucoin.torrent.publishing.ui.chat;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import org.libTAU4j.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgAndLog;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgStatus;
import io.taucoin.torrent.publishing.core.model.data.DataChanged;
import io.taucoin.torrent.publishing.core.model.data.Result;
import io.taucoin.torrent.publishing.core.model.data.message.MsgContent;
import io.taucoin.torrent.publishing.core.model.data.message.QueueOperation;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.HashUtil;
import io.taucoin.torrent.publishing.core.utils.MsgSplitUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.torrent.publishing.ui.constant.Page;
import io.taucoin.torrent.publishing.core.model.data.message.MessageType;
import io.taucoin.torrent.publishing.core.utils.rlp.CryptoUtil;
import io.taucoin.torrent.publishing.ui.transaction.TxUtils;

/**
 * 聊天相关的ViewModel
 */
public class ChatViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("ChatViewModel");
    private ChatRepository chatRepo;
    private UserRepository userRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<Result> chatResult = new MutableLiveData<>();
    private MutableLiveData<Result> resentResult = new MutableLiveData<>();
    private MutableLiveData<List<ChatMsgAndLog>> chatMessages = new MutableLiveData<>();
    private TauDaemon daemon;
    public ChatViewModel(@NonNull Application application) {
        super(application);
        chatRepo = RepositoryHelper.getChatRepository(getApplication());
        userRepo = RepositoryHelper.getUserRepository(getApplication());
        daemon = TauDaemon.getInstance(application);
    }

    MutableLiveData<Result> getChatResult() {
        return chatResult;
    }

    /**
     * 获取消息重发的结果
     */
    MutableLiveData<Result> getResentResult() {
        return resentResult;
    }

    public void observeNeedStartDaemon () {
        disposables.add(daemon.observeNeedStartDaemon()
                .subscribeOn(Schedulers.io())
                .filter((needStart) -> needStart)
                .subscribe((needStart) -> daemon.start()));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    /**
     * 观察查询的聊天信息
     */
    LiveData<List<ChatMsgAndLog>> observerChatMessages() {
        return chatMessages;
    }

    /**
     * 观察社区的消息的变化
     */
    Observable<DataChanged> observeDataSetChanged() {
        return chatRepo.observeDataSetChanged();
    }

    /**
     * 异步给朋友发信息任务
     * @param friendPk 朋友公钥
     * @param msg 消息
     * @param type 消息类型
     */
    void sendMessage(String friendPk, String msg, int type) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Result>) emitter -> {
            String senderPk = MainApplication.getInstance().getPublicKey();
            Result result = syncSendMessageTask(senderPk, friendPk, msg, type);
            emitter.onNext(result);
            emitter.onComplete();
        }).subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> chatResult.postValue(result));
        disposables.add(disposable);
    }

    /**
     * 批量测试入口
     * 异步给朋友发信息任务
     * @param friendPk 朋友公钥
     */
    void sendBatchDebugMessage(String friendPk, int time, int msgSize) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            InputStream inputStream = null;
            try {
                inputStream = getApplication().getAssets().open("HarryPotter1-8.txt");
                byte[] bytes = new byte[msgSize];
                StringBuilder msg = new StringBuilder();
                for (int i = 0; i < time; i++) {
                    if (emitter.isDisposed()) {
                        break;
                    }
                    logger.debug("sendBatchDebugMessage available::{}", inputStream.available());
                    if (inputStream.available() < bytes.length) {
                        inputStream.reset();
                        logger.debug("sendBatchDebugMessage reset");
                    }
                    inputStream.read(bytes);
                    logger.debug("sendBatchDebugMessage read");
                    msg.append(i + 1);
                    msg.append("、");
                    msg.append(new String(bytes, StandardCharsets.UTF_8));
                    long startTime = System.currentTimeMillis();

                    String senderPk = MainApplication.getInstance().getPublicKey();
                    syncSendMessageTask(senderPk, friendPk, msg.toString(), MessageType.TEXT.getType());
                    long endTime = System.currentTimeMillis();
                    logger.debug("sendBatchDebugMessage no::{}, time::{}", i, endTime - startTime);
                    msg.setLength(0);
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        break;
                    }
                }
                inputStream.close();
            } catch (Exception e) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignore) {
                    }
                }
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    private char getMsgPosition() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        if (testChatPos >= chars.length()) {
            testChatPos = 0;
        }
        char randomChar = chars.charAt(testChatPos);
        testChatPos += 1;
        return randomChar;
    }

    /**
     * 批量测试入口
     * 异步给朋友发数字信息任务
     * @param friendPk 朋友公钥
     */
    private int testChatPos = 0;
    void sendBatchDebugDigitMessage(String friendPk, int time) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            try {
                char randomChar = getMsgPosition();
                for (int i = 0; i < time; i++) {
                    if (emitter.isDisposed()) {
                        break;
                    }
                    String msg = randomChar + FmtMicrometer.fmtTestData( i + 1);
                    String senderPk = MainApplication.getInstance().getPublicKey();
                    syncSendMessageTask(senderPk, friendPk, msg, MessageType.TEXT.getType());

                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        break;
                    }
                }
            } catch (Exception ignore) {
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 同步给朋友发信息任务
     * @param senderPk 发送者公钥
     * @param friendPk 朋友公钥
     * @param text 消息
     * @param type 消息类型
     */
    public Result syncSendMessageTask(String senderPk, String friendPk, String text, int type) {
        return syncSendMessageTask(getApplication(), senderPk, friendPk, text, type, null);
    }

    /**
     * 同步给朋友发信息任务
     * @param senderPk 发送者公钥
     * @param friendPk 朋友公钥
     * @param text 消息
     * @param type 消息类型
     * @param airdropChain 发币的链
     */
    public Result syncSendMessageTask(String senderPk, String friendPk, String text, int type, String airdropChain) {
        return syncSendMessageTask(getApplication(), senderPk, friendPk, text, type, airdropChain);
    }

    /**
     * 同步给朋友发信息任务
     * @param context Context
     * @param senderPk 发送者公钥
     * @param friendPk 朋友公钥
     * @param text 消息
     * @param type 消息类型
     */
    public static Result syncSendMessageTask(Context context, String senderPk, String friendPk,
                                             String text, int type) {
        return syncSendMessageTask(context, senderPk, friendPk, text, type, null);
    }

    /**
     * 同步给朋友发转账任务
     * @param context Context
     */
    public static Result syncSendMessageTask(Context context, TxQueue tx, QueueOperation operation) {
        // 自己发给自己的wiring交易，并且交易金额为0；不发送点对点消息
        if (null == tx || (operation == QueueOperation.INSERT && tx.txType == TxType.WIRING_TX.getType() &&
                StringUtil.isEquals(tx.senderPk, tx.receiverPk) && tx.amount <= 0 )) {
            return null;
        }
        String text = TxUtils.createSpanTxQueue(tx, operation).toString();
        return syncSendMessageTask(context, tx.senderPk, tx.receiverPk, text,
                MessageType.WIRING.getType(), tx.chainID);
    }

    /**
     * 同步给朋友发转账任务
     * @param context Context
     */
    public static Result syncSendMessageTask(Context context, Tx tx, long timestamp, QueueOperation operation) {
        String text = TxUtils.createSpanTxQueue(tx, timestamp, operation).toString();
        return syncSendMessageTask(context, tx.senderPk, tx.receiverPk, text,
                MessageType.WIRING.getType(), tx.chainID);
    }

    /**
     * 同步给朋友发信息任务
     * @param context Context
     * @param senderPk 发送者公钥
     * @param friendPk 朋友公钥
     * @param text 消息
     * @param type 消息类型
     * @param chainID 发币的链
     */
    public static Result syncSendMessageTask(Context context, String senderPk, String friendPk,
                     String text, int type, String chainID) {
        Result result = new Result();
        UserRepository userRepo = RepositoryHelper.getUserRepository(context);
        ChatRepository chatRepo = RepositoryHelper.getChatRepository(context);
        TauDaemon daemon = TauDaemon.getInstance(context);
        AppDatabase.getInstance(context).runInTransaction(() -> {
            try {
                String logicMsgHash = HashUtil.makeSha256HashWithTimeStamp(text);
                List<byte[]> contents = MsgSplitUtil.splitTextMsg(text);
                User user = userRepo.getUserByPublicKey(senderPk);
                ChatMsg[] messages = new ChatMsg[contents.size()];
                ChatMsgLog[] chatMsgLogs = new ChatMsgLog[contents.size()];
                int contentSize = contents.size();
                byte[] key = Utils.keyExchange(friendPk, user.seed);
                // 朋友的最后一条信息时间
                long friendLastSendTime = chatRepo.getLastSendTime(friendPk, senderPk);
                // 自己的最后一条信息的时间
                long myLastSendTime = chatRepo.getLastSendTime(senderPk, friendPk);
                for (int nonce = 0; nonce < contentSize; nonce++) {
                    byte[] content = contents.get(nonce);
                    long currentTime = daemon.getSessionTime();
                    // 1、先取当前时间和朋友的最后一条信息时间加1的最大值（防止朋友的时钟比自己本地的快）
                    currentTime = Math.max(currentTime, friendLastSendTime + 1);
                    // 2、再取第一步的是时间和自己的最后一条信息的时间的最大值（防止本地历史时间已比第一步的时间还大）
                    currentTime = Math.max(currentTime, myLastSendTime + 1);
                    // 3、更新自己发送的最后一条的时间
                    myLastSendTime = currentTime;
                    MsgContent msgContent = MsgContent.createContent(logicMsgHash, type, content, chainID);
                    byte[] encoded = msgContent.getEncoded();
                    // 加密填充模式为16的倍数896, 最大控制为895
                    byte[] encryptedEncoded = CryptoUtil.encrypt(encoded, key);
                    Message message = new Message(currentTime, ByteUtil.toByte(senderPk),
                            ByteUtil.toByte(friendPk), encryptedEncoded);
                    String hash = message.msgId();
                    logger.debug("sendMessageTask newMsgHash::{}, contentType::{}, " +
                                    "nonce::{}, rawLength::{}, encryptedEncoded::{}, " +
                                    "logicMsgHash::{}, millisTime::{}",
                            hash, type, nonce, content.length, encryptedEncoded.length,
                            logicMsgHash, DateUtil.format(currentTime, DateUtil.pattern9));

                    boolean isSuccess = daemon.addNewMessage(message);
                    // 组织Message的结构，并发送到DHT和数据入库
                    ChatMsg chatMsg = new ChatMsg(hash, senderPk, friendPk, content, type,
                            currentTime, logicMsgHash, chainID);
                    messages[nonce] = chatMsg;

                    // 更新消息日志信息
                    // 如何是自己给自己发，直接确认接收
                    ChatMsgStatus status;
                    if (isSuccess) {
                        if (StringUtil.isEquals(senderPk, friendPk)) {
                            status = ChatMsgStatus.CONFIRMED;
                        } else {
                            status = ChatMsgStatus.SENT;
                        }
                    } else {
                        status = ChatMsgStatus.SEND_FAIL;
                    }
                    // 确认接收的时间精确到秒
                    chatMsgLogs[nonce] = new ChatMsgLog(hash,
                            status.getStatus(), currentTime);

                }
                // 批量添加到数据库
                chatRepo.addChatMsgLogs(friendPk, chatMsgLogs);
                chatRepo.addChatMessages(messages);
            } catch (Throwable e) {
                logger.error("sendMessageTask error", e);
                result.setFailMsg(e.getMessage());
            }
        });
        return result;
    }

    /**
     * 重发消息
     */
    void resendMessage(ChatMsgAndLog msg, int pos) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Result>) emitter -> {
            ChatMsg chatMsg = chatRepo.queryChatMsg(msg.hash);
            Result result = new Result();
            long timestamp = chatMsg.timestamp;
            String sender = chatMsg.senderPk;
            String receiver = chatMsg.receiverPk;
            String logicMsgHash = chatMsg.logicMsgHash;
            User user = userRepo.getUserByPublicKey(sender);
            MsgContent msgContent = MsgContent.createTextContent(logicMsgHash, chatMsg.content, chatMsg.airdropChain);
            byte[] encoded = msgContent.getEncoded();
            byte[] key = Utils.keyExchange(receiver, user.seed);
            byte[] encryptedEncoded = CryptoUtil.encrypt(encoded, key);
            Message message = new Message(timestamp, ByteUtil.toByte(sender),
                    ByteUtil.toByte(receiver), encryptedEncoded);
            boolean isSuccess = daemon.addNewMessage(message);
            if (isSuccess) {
                try {
                    boolean isMyself = StringUtil.isEquals(chatMsg.senderPk, chatMsg.receiverPk);
                    if (isMyself) {
                        // 如何是自己给自己发，直接确认接收
                        ChatMsgLog chatMsgLog = new ChatMsgLog(chatMsg.hash,
                                ChatMsgStatus.CONFIRMED.getStatus(), daemon.getSessionTime());
                        chatRepo.addChatMsgLogs(chatMsg.receiverPk, chatMsgLog);
                    } else {
                        ChatMsgLog chatMsgLog = new ChatMsgLog(chatMsg.hash,
                                ChatMsgStatus.RESEND.getStatus(), daemon.getSessionTime());
                        chatRepo.addChatMsgLogs(chatMsg.receiverPk, chatMsgLog);
                        msg.logs.add(chatMsgLog);
                    }
                } catch (SQLiteConstraintException ignore) {
                }
            }
            result.setMsg(String.valueOf(pos));
            result.setSuccess(isSuccess);
            emitter.onNext(result);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> resentResult.postValue(result));
        disposables.add(disposable);
    }

    /**
     * 观察消息日志信息
     */
    Observable<List<ChatMsgLog>> observerMsgLogs(String hash) {
        return chatRepo.observerMsgLogs(hash);
    }

    void loadMessagesData(String friendPk, int pos) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<List<ChatMsgAndLog>>) emitter -> {
            List<ChatMsgAndLog> messages = new ArrayList<>();
            try {
                long startTime = System.currentTimeMillis();
                int pageSize = pos == 0 ? Page.PAGE_SIZE * 2 : Page.PAGE_SIZE;
                messages = chatRepo.getMessages(friendPk, pos, pageSize);
                long getMessagesTime = System.currentTimeMillis();
                logger.trace("loadMessagesData pos::{}, pageSize::{}, messages.size::{}",
                        pos, pageSize, messages.size());
                logger.trace("loadMessagesData getMessagesTime::{}", getMessagesTime - startTime);
                Collections.reverse(messages);
                for (ChatMsgAndLog msg : messages) {
                    if (msg.logs != null && msg.logs.size() > 0) {
                        Collections.sort(msg.logs);
                    }
                }
                long endTime = System.currentTimeMillis();
                logger.trace("loadMessagesData reverseTime Time::{}", endTime - getMessagesTime);
            } catch (Exception e) {
                logger.error("loadMessagesData error::", e);
            }
            emitter.onNext(messages);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(messages -> {
                chatMessages.postValue(messages);
            });
        disposables.add(disposable);
    }
}