package io.taucoin.torrent.publishing.core.model;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import org.libTAU4j.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

import io.taucoin.torrent.publishing.core.model.data.ChatMsgStatus;
import io.taucoin.torrent.publishing.core.model.data.FriendStatus;
import io.taucoin.torrent.publishing.core.model.data.message.MsgContent;
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
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.torrent.publishing.core.utils.rlp.CryptoUtil;

/**
 * MsgListener处理程序
 */
class MsgAlertHandler {
    private static final Logger logger = LoggerFactory.getLogger("MsgAlertHandler");
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
     * @param message 消息
     * @param userPk byte[] 当前用户的公钥
     */
    void onNewMessage(Message message, String userPk) {
        try {
            // 朋友默认为发送者
            String hash = message.msgId();
            long sentTime = message.timestamp();
            String senderPk = ByteUtil.toHexString(message.sender());
            String receiverPk = ByteUtil.toHexString(message.receiver());
            long receivedTime = DateUtil.getMillisTime();

            ChatMsg chatMsg = chatRepo.queryChatMsg(senderPk, hash);
            logger.debug("TAU messaging onNewMessage senderPk::{}, receiverPk::{}, hash::{}, " +
                            "SentTime::{}, ReceivedTime::{}, DelayTime::{}ms, exist::{}",
                    senderPk, receiverPk, hash,
                    DateUtil.format(sentTime, DateUtil.pattern9),
                    DateUtil.format(receivedTime, DateUtil.pattern9),
                    receivedTime - sentTime, chatMsg != null);
            // 上报的Message有可能重复, 如果本地已存在不处理
            if (null == chatMsg) {
                User user = userRepo.getUserByPublicKey(userPk);
                // 判断消息的发送者是否是自己
                String friendPkStr;
                if (StringUtil.isEquals(senderPk, user.publicKey)) {
                    friendPkStr = receiverPk;
                } else {
                    friendPkStr = senderPk;
                }
                logger.error("payload.size::{}", message.payload().length);
                byte[] encryptedEncoded = message.payload();
                // 原始数据解密
                byte[] cryptoKey = Utils.keyExchange(friendPkStr, user.seed);
                byte[] encoded = CryptoUtil.decrypt(encryptedEncoded, cryptoKey);
                MsgContent msgContent = new MsgContent(encoded);
                String logicMsgHash = msgContent.getLogicHash();
                byte[] content = msgContent.getContent();
                chatMsg = new ChatMsg(hash, senderPk, receiverPk, content, msgContent.getType(),
                        sentTime, msgContent.getNonce(), logicMsgHash, 1);
                chatRepo.addChatMsg(chatMsg);

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
            }
        } catch (Exception e) {
            logger.error("onNewMessage error", e);
        }
    }

    /**
     * 消息正在同步
     * @param msgHash 当前同步的消息
     * @param timestamp 开始同步时间
     */
    void onSyncMessage(byte[] msgHash, long timestamp, String userPk) {
        try {
            String hash = ByteUtil.toHexString(msgHash);
            ChatMsg msg = chatRepo.queryChatMsg(hash);
            String senderPk = msg != null ? msg.senderPk : null;
            if (StringUtil.isNotEquals(senderPk, userPk)) {
                logger.debug("onSyncMessage MessageHash::{}, senderPk::{}, not mine", hash, senderPk);
                return;
            }
            ChatMsgLog msgLog = chatRepo.queryChatMsgLog(hash,
                    ChatMsgStatus.SYNCING.getStatus());
            logger.trace("onSyncMessage MessageHash::{}, exist::{}", hash, msgLog != null);
            if (null == msgLog) {
                msgLog = new ChatMsgLog(hash, ChatMsgStatus.SYNCING.getStatus(), timestamp);
                chatRepo.addChatMsgLogs(msgLog);
            }
        } catch (SQLiteConstraintException ignore) {
        } catch (Exception e) {
            logger.error("onSyncMessage error", e);
        }
    }

    /**
     * 消息已被接收
     * @param hashList 消息root
     */
    void onReadMessageRoot(List<byte[]> hashList, BigInteger timestamp, String userPk) {
        try {
            logger.trace("onReadMessageRoot hashList.size::{}", hashList.size());
            for (byte[] root : hashList) {
                String hash = ByteUtil.toHexString(root);
                ChatMsg msg = chatRepo.queryChatMsg(hash);
                String senderPk = msg != null ? msg.senderPk : null;
                if (StringUtil.isNotEquals(senderPk, userPk)) {
                    logger.debug("onReadMessageRoot MessageHash::{}, senderPk::{}, not mine", hash, senderPk);
                    break;
                }
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
     * @param userPk 当前用户公钥
     */
    void onDiscoveryFriend(String friendPk, long lastSeenTime, String userPk) {
        logger.debug("onDiscoveryFriend friendPk::{}", friendPk);
        Friend friend = friendRepo.queryFriend(userPk, friendPk);
        if (friend != null) {
            boolean isUpdate = false;
            if (friend.status != FriendStatus.CONNECTED.getStatus()) {
                friend.status = FriendStatus.CONNECTED.getStatus();
                isUpdate = true;
            }
            // 当前时间比上次更新时间大于1s
            if (lastSeenTime - friend.lastSeenTime >= 1000) {
                friend.lastSeenTime = lastSeenTime;
                isUpdate = true;
            }
            if (isUpdate) {
                friendRepo.updateFriend(friend);
            }

            logger.info("onDiscoveryFriend friendPk::{}, lastSeenTime::{}, {}", friendPk,
                    DateUtil.format(lastSeenTime, DateUtil.pattern9), lastSeenTime);
        }
    }

    /**
     * 新device ID通知
     * @param deviceID device id
     * @param userPk 当前用户的公钥
     */
    void onNewDeviceID(String deviceID, String userPk) {
        logger.debug("onNewDeviceID userPk::{}, deviceID::{}",
                userPk, deviceID);
        Device device = new Device(userPk, deviceID, DateUtil.getTime());
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
        // 多设备同步，可直接更新朋友信息,
        // 如果又把数据更新到libTAU，则状态为ADDED，否则为DISCOVERED
        boolean isSuccess = daemon.updateFriendInfo(user);
        // 多设备朋友关系状态同步
        if (StringUtil.isNotEquals(userPk, friendPkStr)) {
            Friend friend = friendRepo.queryFriend(userPk, friendPkStr);
            if (friend != null) {
                if (friend.status == FriendStatus.DISCOVERED.getStatus()) {
                    friend.status = FriendStatus.ADDED.getStatus();
                    friendRepo.updateFriend(friend);
                }
            } else {
                int status = isSuccess ? FriendStatus.ADDED.getStatus() : FriendStatus.DISCOVERED.getStatus();
                friend = new Friend(userPk, friendPkStr, status);
                friendRepo.addFriend(friend);
            }
        }
    }
}
