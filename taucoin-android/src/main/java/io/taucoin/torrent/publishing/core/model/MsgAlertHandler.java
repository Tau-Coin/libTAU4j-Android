package io.taucoin.torrent.publishing.core.model;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgStatus;
import io.taucoin.torrent.publishing.core.model.data.FriendStatus;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Device;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.DeviceRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.ui.TauNotifier;
import io.taucoin.types.Message;
import io.taucoin.types.MessageType;
import io.taucoin.util.ByteUtil;

/**
 * MsgListener处理程序
 */
class MsgAlertHandler {
    private static final Logger logger = LoggerFactory.getLogger("MsgListenHandler");
    private ChatRepository chatRepo;
    private FriendRepository friendRepo;
    private DeviceRepository deviceRepo;
    private UserRepository userRepo;
    private TauDaemon daemon;

    MsgAlertHandler(Context appContext, TauDaemon daemon) {
        this.daemon = daemon;
        chatRepo = RepositoryHelper.getChatRepository(appContext);
        friendRepo = RepositoryHelper.getFriendsRepository(appContext);
        deviceRepo = RepositoryHelper.getDeviceRepository(appContext);
        userRepo = RepositoryHelper.getUserRepository(appContext);
    }
    /**
     * 处理新的消息
     * 0、如果没和朋友建立Chat, 创建Chat
     * 1、更新朋友状态
     * 2、保存Chat的聊天信息
     * @param friendPk byte[] 朋友公钥
     * @param messages List<Message>
     */
    public void onNewMessage(byte[] friendPk, List<Message> messages) {
        try {
            logger.trace("onNewMessage messages.size::{}", messages.size());
            for (Message message : messages) {
                // 朋友默认为发送者
                String senderPk = ByteUtil.toHexString(message.getSender());
                String receiverPk = ByteUtil.toHexString(message.getReceiver());
                String hash = ByteUtil.toHexString(message.getHash());
                String logicMsgHash = ByteUtil.toHexString(message.getLogicMsgHash());

                long sentTime = message.getTimestamp().longValue();
                long receivedTime = DateUtil.getTime();

                ChatMsg chatMsg = chatRepo.queryChatMsg(senderPk, hash);
                logger.debug("TAU messaging onNewMessage senderPk::{}, receiverPk::{}, hash::{}, " +
                                "SentTime::{}, ReceivedTime::{}, DelayTime::{}s, exist::{}",
                        senderPk, receiverPk, hash,
                        DateUtil.formatTime(sentTime, DateUtil.pattern6),
                        DateUtil.formatTime(receivedTime, DateUtil.pattern6),
                        receivedTime - sentTime, chatMsg != null);
                // 上报的Message有可能重复, 如果本地已存在不处理
                if (null == chatMsg) {
                    User user = userRepo.getCurrentUser();
                    // 判断消息的发送者是否是自己
                    String friendPkStr;
                    if (StringUtil.isEquals(senderPk, user.publicKey)) {
                        friendPkStr = receiverPk;
                    } else {
                        friendPkStr = senderPk;
                    }
                    // 原始数据解密
                    byte[] cryptoKey = Utils.keyExchange(friendPkStr, user.seed);
                    message.decrypt(cryptoKey);

                    // 保存消息数据
                    byte[] encryptedContent = message.getEncryptedContent();
                    ChatMsg msg = new ChatMsg(hash, senderPk, receiverPk, encryptedContent,
                            message.getType().ordinal(), sentTime, message.getNonce().longValue(),
                            logicMsgHash);
                    msg.unsent = 1;
                    chatRepo.addChatMsg(msg);

                    // 标记消息未读, 更新上次交流的时间
                    Friend friend = friendRepo.queryFriend(user.publicKey, friendPkStr);
                    boolean isNeedUpdate = false;
                    if (friend != null) {
                        if (friend.msgUnread == 0) {
                            friend.msgUnread = 1;
                            isNeedUpdate = true;
                        }
                        long lastCommTime = friend.lastCommTime;
                        if (sentTime > lastCommTime) {
                            friend.lastCommTime = sentTime;
                            isNeedUpdate = true;
                        }
                        if (isNeedUpdate) {
                            friendRepo.updateFriend(friend);
                        }
                    }
                    // 只通知朋友的消息
                    if (StringUtil.isNotEquals(senderPk, user.publicKey)) {
                        // 通知栏消息通知
                        User friendUser = userRepo.getUserByPublicKey(senderPk);
                        if (msg.contentType == MessageType.TEXT.ordinal()) {
                            String content = Utils.textBytesToString(message.getRawContent());
                            TauNotifier.getInstance().makeChatMsgNotify(friendUser, content);
                        } else if (msg.contentType == MessageType.PICTURE.ordinal()) {
                            TauNotifier.getInstance().makeChatMsgNotify(friendUser,
                                    R.string.main_pic_messages);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("onNewMessage error", e);
        }
    }

    /**
     * 消息正在同步
     * @param message 当前同步的消息
     * @param timestamp 开始同步时间
     */
    public void onSyncMessage(Message message, BigInteger timestamp) {
        try {
            String hash = ByteUtil.toHexString(message.getHash());
            ChatMsgLog msgLog = chatRepo.queryChatMsgLog(hash,
                    ChatMsgStatus.SYNCING.getStatus());
            logger.trace("onSyncMessage MessageHash::{}, exist::{}", hash, msgLog != null);
            if (null == msgLog) {
                msgLog = new ChatMsgLog(hash, ChatMsgStatus.SYNCING.getStatus(),
                        timestamp.longValue());
                chatRepo.addChatMsgLogs(msgLog);
            }
        } catch (SQLiteConstraintException ignore) {
        } catch (Exception e) {
            logger.error("onSyncMessage error", e);
        }
    }

    /**
     * 消息已被接收
     * @param friendPk byte[] 朋友公钥
     * @param hashList 消息root
     */
    public void onReadMessageRoot(byte[] friendPk, List<byte[]> hashList, BigInteger timestamp) {
        try {
            logger.trace("onReadMessageRoot hashList.size::{}", hashList.size());
            for (byte[] root : hashList) {
                String hash = ByteUtil.toHexString(root);
                ChatMsgLog msgLog = chatRepo.queryChatMsgLog(hash,
                        ChatMsgStatus.SYNC_CONFIRMED.getStatus());
                logger.trace("onReadMessageRoot MessageHash::{}, exist::{}", hash, msgLog != null);
                if (null == msgLog) {
                    msgLog = new ChatMsgLog(hash, ChatMsgStatus.SYNC_CONFIRMED.getStatus(),
                            timestamp.longValue());
                   chatRepo.addChatMsgLogs(msgLog);
                }
            }
        } catch (SQLiteConstraintException ignore) {
        } catch (Exception e) {
            logger.error("onReadMessageRoot error", e);
        }
    }

    /**
     * 发现朋友
     * @param friendPk 朋友公钥
     * @param lastSeenTime 和朋友通信时间
     */
    void onDiscoveryFriend(String friendPk, long lastSeenTime) {
        logger.debug("onDiscoveryFriend friendPk::{}", friendPk);
        User user = userRepo.getCurrentUser();
        String userPk = user.publicKey;
        Friend friend = friendRepo.queryFriend(userPk, friendPk);
        if (friend != null) {
            boolean isUpdate = false;
            if (friend.status != FriendStatus.CONNECTED.getStatus()) {
                friend.status = FriendStatus.CONNECTED.getStatus();
                isUpdate = true;
            }
            // 当前时间大于上次更新时间
            if (lastSeenTime > friend.lastSeenTime) {
                friend.lastSeenTime = lastSeenTime;
                isUpdate = true;
            }
            if (isUpdate) {
                friendRepo.updateFriend(friend);
            }
            logger.info("onDiscoveryFriend friendPk::{}, lastSeenTime::{}", friendPk,
                    DateUtil.formatTime(lastSeenTime, DateUtil.pattern6));
        }
    }

    /**
     * 新device ID通知
     * @param deviceID device id
     * @param userPk 当前用户的公钥
     */
    void onNewDeviceID(byte[] deviceID, String userPk) {
        String deviceIDStr = new String(deviceID);
        logger.debug("onNewDeviceID userPk::{}, deviceID::{}",
                userPk, deviceIDStr);
        Device device = new Device(userPk, deviceIDStr, DateUtil.getTime());
        deviceRepo.addDevice(device);
    }

    /**
     * 多设备的新朋友通知
     * @param userPk 当前用户的公钥
     * @param friendPk 发现的新朋友公钥
     * @param nickname 昵称
     * @param timestamp 起名字的时间戳
     */
    void onNewFriendFromMultiDevice(String userPk, byte[] friendPk, byte[] nickname, BigInteger timestamp) {
        String friendPkStr = ByteUtil.toHexString(friendPk);
        logger.debug("onNewFriend userPk::{}, friendPk::{}",
                userPk, friendPkStr);
        User user = userRepo.getUserByPublicKey(friendPkStr);
        // 多设备朋友同步
        if (null == user) {
            user = new User(friendPkStr);
            if (nickname != null && timestamp != null) {
                user.nickname = Utils.textBytesToString(nickname);
                user.updateTime = timestamp.longValue();
            }
            userRepo.addUser(user);
        } else {
            // 多设备朋友昵称同步
            if (nickname != null && timestamp != null &&
                    timestamp.compareTo(BigInteger.valueOf(user.updateTime)) > 0) {
                user.nickname = Utils.textBytesToString(nickname);
                user.updateTime = timestamp.longValue();
                userRepo.updateUser(user);
            }
        }
        // 更新libTAU朋友信息
        // 多设备同步，可直接更新朋友信息
        daemon.updateFriendInfo(user);
        // 多设备朋友关系状态同步
        if (StringUtil.isNotEquals(userPk, friendPkStr)) {
            Friend friend = friendRepo.queryFriend(userPk, friendPkStr);
            if (friend != null) {
                if (friend.status == FriendStatus.DISCOVERED.getStatus()) {
                    friend.status = FriendStatus.ADDED.getStatus();
                    friendRepo.updateFriend(friend);
                }
            } else {
                friend = new Friend(userPk, friendPkStr, FriendStatus.ADDED.getStatus());
                friendRepo.addFriend(friend);
            }
        }
    }
}
