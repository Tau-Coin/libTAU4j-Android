package io.taucoin.torrent.publishing.core.model;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import org.libTAU4j.Account;
import org.libTAU4j.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgStatus;
import io.taucoin.torrent.publishing.core.model.data.UserHeadPic;
import io.taucoin.torrent.publishing.core.model.data.UserInfo;
import io.taucoin.torrent.publishing.core.model.data.FriendStatus;
import io.taucoin.torrent.publishing.core.model.data.message.AirdropStatus;
import io.taucoin.torrent.publishing.core.model.data.message.MessageType;
import io.taucoin.torrent.publishing.core.model.data.message.MsgContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.model.data.message.QueueOperation;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Device;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.DeviceRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxQueueRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.torrent.publishing.ui.TauNotifier;
import io.taucoin.torrent.publishing.ui.chat.ChatViewModel;

/**
 * MsgListener处理程序
 */
class MsgAlertHandler {
    private static final Logger logger = LoggerFactory.getLogger("MsgAlertHandler");
    private final ChatRepository chatRepo;
    private final FriendRepository friendRepo;
    private final DeviceRepository deviceRepo;
    private final UserRepository userRepo;
    private final TxQueueRepository txQueueRepo;
    private final MemberRepository memberRepo;
    private final CommunityRepository communityRepo;
    private final TauDaemon daemon;
    private final Context appContext;

    MsgAlertHandler(Context appContext, TauDaemon daemon) {
        this.appContext = appContext;
        this.daemon = daemon;
        chatRepo = RepositoryHelper.getChatRepository(appContext);
        friendRepo = RepositoryHelper.getFriendsRepository(appContext);
        deviceRepo = RepositoryHelper.getDeviceRepository(appContext);
        userRepo = RepositoryHelper.getUserRepository(appContext);
        txQueueRepo = RepositoryHelper.getTxQueueRepository(appContext);
        memberRepo = RepositoryHelper.getMemberRepository(appContext);
        communityRepo = RepositoryHelper.getCommunityRepository(appContext);
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
            long receivedTime = daemon.getSessionTime();

            ChatMsg chatMsg = chatRepo.queryChatMsg(senderPk, hash);
            logger.info("TAU messaging onNewMessage senderPk::{}, receiverPk::{}, hash::{}, " +
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
                byte[] encoded = message.payload();
                MsgContent msgContent = new MsgContent(encoded);
                String logicMsgHash = msgContent.getLogicHash();
                byte[] content = msgContent.getContent();
                chatMsg = new ChatMsg(hash, senderPk, receiverPk, content, msgContent.getType(),
                        sentTime, logicMsgHash, msgContent.getAirdropChain());
                chatRepo.addChatMsg(chatMsg);

                // 标记消息未读, 更新上次交流的时间
                Friend friend = friendRepo.queryFriend(user.publicKey, friendPkStr);
                boolean isNeedUpdate = false;
                if (friend != null) {
                    if (friend.status != FriendStatus.CONNECTED.getStatus()) {
                        friend.status = FriendStatus.CONNECTED.getStatus();
                        isNeedUpdate = true;
                    }
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
                } else {
                    // 更新libTAU朋友信息
                    boolean isSuccess = daemon.addNewFriend(user.publicKey);
                    if (isSuccess) {
                        // 发送默认消息
                        String msg = appContext.getString(R.string.contacts_have_added);
                        logger.info("AddFriendsLocally, syncSendMessageTask userPk::{}, friendPk::{}, msg::{}",
                                userPk, friendPkStr, msg);
                        ChatViewModel.syncSendMessageTask(appContext, userPk, friendPkStr, msg, MessageType.TEXT.getType());
                        // 更新本地朋友关系
                        friend = new Friend(userPk, friendPkStr);
                        friend.status = FriendStatus.CONNECTED.getStatus();
                        friend.msgUnread = 1;
                        friend.lastCommTime = sentTime;
                        friendRepo.addFriend(friend);
                        // 更新朋友信息
                        daemon.requestFriendInfo(friendPkStr);
                    }
                }

                // 如果是Airdrop, 则给此消息的发送者airdrop coins
                if (msgContent.getType() == MessageType.AIRDROP.getType()) {
                    String chainID = msgContent.getAirdropChain();
                    handleAirdropCoins(chainID, userPk, senderPk);
                }

                // 创建通知栏消息
                User friendUser = userRepo.getUserByPublicKey(senderPk);
                if (friendUser != null && content != null) {
                    TauNotifier.getInstance().makeChatNotify(friendUser, Utils.textBytesToString(content));
                }
            }
        } catch (Exception e) {
            logger.error("onNewMessage error", e);
        }
    }

    /**
     * 给消息的发送者airdrop coins
     * @param currentPk 当前用户
     * @param friendPk 接受airdrop的朋友
     */
    private void handleAirdropCoins(String chainID, String currentPk, String friendPk) {
        logger.info("handleAirdropCoins: yourself::{}, currentPk::{}, friendPk::{}, chainID::{}",
                StringUtil.isEquals(currentPk, friendPk), currentPk, friendPk, chainID);
        if (StringUtil.isEquals(currentPk, friendPk)) {
           return;
        }
        Member member = memberRepo.getMemberByChainIDAndPk(chainID, currentPk);
        if (null == member) {
            logger.info("handleAirdropCoins: not a member of the community");
            return;
        }
        if (member.airdropStatus != AirdropStatus.ON.getStatus()) {
            AirdropStatus status = AirdropStatus.valueOf(member.airdropStatus);
            logger.info("handleAirdropCoins: airdrop status::{}", status.getName());
            return;
        }
        // 一个朋友只能airdrop一次
        TxQueue txQueue = txQueueRepo.getAirdropTxQueue(chainID, currentPk, friendPk);
        if (txQueue != null) {
            logger.info("handleAirdropCoins: A peer can only be sent once!");
            return;
        }
        long currentTime = member.airdropTime;
        int airdropCount = txQueueRepo.getAirdropCount(chainID, currentPk, currentTime);
        logger.info("handleAirdropCoins: airdrop progress::{}/{}", airdropCount, member.airdropMembers);
        // airdrop朋友数是否完成
        if (airdropCount >= member.airdropMembers) {
            return;
        }
        String memo = appContext.getString(R.string.tx_memo_airdrop);
        long amount = member.airdropCoins;
        long fee = 0L;
        TxContent txContent = new TxContent(TxType.WIRING_TX.getType(), memo);
        TxQueue tx = new TxQueue(chainID, currentPk, friendPk, amount, fee, 1,
                TxType.WIRING_TX.getType(), txContent.getEncoded());
        txQueueRepo.addQueue(tx);

        // 余额不足不发送点对点消息
        Account account = daemon.getAccountInfo(ChainIDUtil.encode(chainID), tx.senderPk);
        if (account != null) {
            long medianFee = Constants.WIRING_MIN_FEE.longValue();
            if (tx.amount + medianFee <= account.getBalance()) {
                ChatViewModel.syncSendMessageTask(appContext, tx, QueueOperation.INSERT);
            }
        }
        daemon.updateTxQueue(tx.chainID);
    }

    /**
     * Sent to Internet 桔黄色 (traversal complete > 1)
     * @param msgHash 消息hash
     * @param timestamp 时间
     * @param userPk 用户公钥
     */
    void onMsgSync(byte[] msgHash, BigInteger timestamp, String userPk) {
        try {
            String hash = ByteUtil.toHexString(msgHash);
            ChatMsg msg = chatRepo.queryChatMsg(hash);
            String senderPk = msg != null ? msg.senderPk : null;
            String receiverPk = msg != null ? msg.receiverPk : null;
            if (StringUtil.isNotEquals(senderPk, userPk)) {
                logger.info("onMsgSync MessageHash::{}, senderPk::{}, not mine", hash, senderPk);
                return;
            }
            ChatMsgLog msgLog = chatRepo.queryChatMsgLog(hash,
                    ChatMsgStatus.SENT_INTERNET.getStatus());
            logger.info("onMsgSync MessageHash::{}, exist::{}", hash, msgLog != null);
            if (null == msgLog) {
                msgLog = new ChatMsgLog(hash, ChatMsgStatus.SENT_INTERNET.getStatus(),
                        timestamp.longValue());
                chatRepo.addChatMsgLogs(receiverPk, msgLog);
            }

        } catch (SQLiteConstraintException ignore) {
        } catch (Exception e) {
            logger.error("onMsgSync error", e);
        }
    }

    /**
     * Arrived Prefix Swarm 绿色(等价网络节点>1)
     * @param msgHash 消息hash
     * @param timestamp 时间
     * @param userPk 用户公钥
     */
    void onMsgArrived(byte[] msgHash, BigInteger timestamp, String userPk) {
        try {
            String hash = ByteUtil.toHexString(msgHash);
            ChatMsg msg = chatRepo.queryChatMsg(hash);
            String senderPk = msg != null ? msg.senderPk : null;
            String receiverPk = msg != null ? msg.receiverPk : null;
            if (StringUtil.isNotEquals(senderPk, userPk)) {
                logger.info("onMsgArrived MessageHash::{}, senderPk::{}, not mine", hash, senderPk);
                return;
            }
            ChatMsgLog msgLog = chatRepo.queryChatMsgLog(hash,
                    ChatMsgStatus.ARRIVED_SWARM.getStatus());
            logger.info("onMsgArrived MessageHash::{}, exist::{}", hash, msgLog != null);
            if (null == msgLog) {
                msgLog = new ChatMsgLog(hash, ChatMsgStatus.ARRIVED_SWARM.getStatus(),
                        timestamp.longValue());
                chatRepo.addChatMsgLogs(receiverPk, msgLog);
            }

        } catch (SQLiteConstraintException ignore) {
        } catch (Exception e) {
            logger.error("onMsgArrived error", e);
        }
    }

    /**
     * 消息已被接收
     * @param hashList 消息root
     */
    void onReadMessageRoot(List<byte[]> hashList, BigInteger timestamp, String userPk) {
        try {
            logger.info("onReadMessageRoot hashList.size::{}", hashList.size());
            for (byte[] root : hashList) {
                String hash = ByteUtil.toHexString(root);
                ChatMsg msg = chatRepo.queryChatMsg(hash);
                String senderPk = msg != null ? msg.senderPk : null;
                String receiverPk = msg != null ? msg.receiverPk : null;
                if (StringUtil.isNotEquals(senderPk, userPk)) {
                    logger.debug("onReadMessageRoot MessageHash::{}, senderPk::{}, not mine", hash, senderPk);
                    continue;
                }
                ChatMsgLog msgLog = chatRepo.queryChatMsgLog(hash,
                        ChatMsgStatus.CONFIRMED.getStatus());
                logger.info("onReadMessageRoot MessageHash::{}, exist::{}", hash, msgLog != null);
                if (null == msgLog) {
                    msgLog = new ChatMsgLog(hash, ChatMsgStatus.CONFIRMED.getStatus(),
                            timestamp.longValue());
                   chatRepo.addChatMsgLogs(receiverPk, msgLog);
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
        if (StringUtil.isEmpty(userPk)) {
            return;
        }
        User user = userRepo.getUserByPublicKey(friendPk);
        // 多设备朋友同步
        if (null == user) {
            user = new User(friendPk);
            userRepo.addUser(user);
        }

        Friend friend = friendRepo.queryFriend(userPk, friendPk);
        if (null == friend) {
            // 更新libTAU朋友信息
            boolean isSuccess = daemon.addNewFriend(user.publicKey);
            if (isSuccess) {
                // 发送默认消息
                String msg = appContext.getString(R.string.contacts_have_added);
                logger.info("AddFriendsLocally, syncSendMessageTask userPk::{}, friendPk::{}, msg::{}",
                        userPk, friendPk, msg);
                ChatViewModel.syncSendMessageTask(appContext, userPk, friendPk, msg, MessageType.TEXT.getType());
                // 更新本地朋友关系
                friend = new Friend(userPk, friendPk);
                friend.status = FriendStatus.ADDED.getStatus();
                friend.lastSeenTime = lastSeenTime;
                friend.onlineCount = 1;
                friendRepo.addFriend(friend);
                // 更新朋友信息
                daemon.requestFriendInfo(friendPk);
            }
        } else {
            boolean isUpdate = false;
            if (friend.status == FriendStatus.DISCOVERED.getStatus()) {
                friend.status = FriendStatus.ADDED.getStatus();
                isUpdate = true;
            }
            // 当前时间比上次更新时间大于1s
            if (lastSeenTime > friend.lastSeenTime) {
                friend.lastSeenTime = lastSeenTime;
                if (friend.onlineCount < Constants.MAX_ONLINE_COUNT) {
                    friend.onlineCount += 1;
                } else {
                    friend.onlineCount = 0;
                }
                isUpdate = true;
            }
            if (isUpdate) {
                friendRepo.updateFriend(friend);
            }
        }
        logger.info("onDiscoveryFriend userPk::{}, friendPk::{}, lastSeenTime::{}, {}",
                userPk, friendPk, DateUtil.format(lastSeenTime, DateUtil.pattern9), lastSeenTime);
    }

    /**
     * 添加新的device ID
     * @param deviceID device id
     * @param userPk 当前用户的公钥
     */
    void addNewDeviceID(String deviceID, String userPk) {
        logger.info("addNewDeviceID userPk::{}, deviceID::{}",
                userPk, deviceID);
        if (StringUtil.isNotEmpty(deviceID) && StringUtil.isNotEmpty(userPk)) {
            Device device = new Device(userPk, deviceID, DateUtil.getTime());
            deviceRepo.addDevice(device);
        }
    }

    /**
     * 节点用户信息
     * @param peer 节点用户信息
     * @param bean 朋友信息
     */
    void onUserInfo(String userPk, byte[] peer, UserInfo bean) {
        if (null == peer) {
            return;
        }
        String friendPkStr = ByteUtil.toHexString(peer);
        String nickname = Utils.textBytesToString(bean.getNickname());
        long onlineTime = bean.getOnlineTime().longValue();
        double longitude = bean.getLongitude();
        double latitude = bean.getLatitude();
        long updateNNTime = bean.getUpdateNNTime().longValue();
        long updateLocationTime = bean.getUpdateLocationTime().longValue();
        String profile = Utils.textBytesToString(bean.getProfile());
        long updateProfileTime = bean.getUpdateProfileTime().longValue();
        List<byte[]> communities = bean.getCommunities();
        logger.info("onUserInfo friendPk::{}, updaterNNTime::{}, nickname::{}, longitude::{}, " +
                        "latitude::{}, updateProfileTime::{}, communities::{}",
                friendPkStr, updateNNTime, nickname, longitude, latitude, updateProfileTime, communities.size());
        User user = userRepo.getUserByPublicKey(friendPkStr);

        // 更新last time
        Friend friend = friendRepo.queryFriend(userPk, friendPkStr);
        if (friend != null && (friend.lastSeenTime == 0 || onlineTime > friend.lastSeenTime)) {
            friend.lastSeenTime = onlineTime;
            friendRepo.updateFriend(friend);
        }

        // 多设备朋友同步
        if (null == user) {
            user = new User(friendPkStr);
            user.nickname = nickname;
            user.updateNNTime = updateNNTime;
            user.longitude = longitude;
            user.latitude = latitude;
            user.updateLocationTime = updateLocationTime;
            user.profile = profile;
            user.updatePFTime = updateProfileTime;
            userRepo.addUser(user);
        } else {
            boolean isNeedUpdate = false;
            if (updateNNTime > user.updateNNTime) {
                user.nickname = nickname;
                user.updateNNTime = updateNNTime;
                isNeedUpdate = true;
            }
            if (updateLocationTime > user.updateLocationTime) {
                user.longitude = longitude;
                user.latitude = latitude;
                user.updateLocationTime = updateLocationTime;
                isNeedUpdate = true;
            }
            if (updateProfileTime > user.updatePFTime) {
                user.profile = profile;
                user.updatePFTime = updateProfileTime;
                isNeedUpdate = true;
            }
            if (isNeedUpdate) {
                userRepo.updateUser(user);
            }
        }
        // 添加朋友的社区到本地
        for (byte[] chainIDBytes : communities) {
            String chainID = ChainIDUtil.decode(chainIDBytes);
            Community community = communityRepo.getCommunityByChainID(chainID);
            if (null == community) {
                String communityName = ChainIDUtil.getName(chainID);
                community = new Community(chainID, communityName);
                communityRepo.addCommunity(community);
            }
            Member member = memberRepo.getMemberByChainIDAndPk(chainID, friendPkStr);
            if (null == member) {
                member = new Member(chainID, friendPkStr);
                memberRepo.addMember(member);
            }
        }
        // 如果是自己的信息，添加设备ID
        boolean isMyself = StringUtil.isEquals(userPk, friendPkStr);
        if (isMyself) {
            byte[] deviceID = bean.getDeviceID();
            addNewDeviceID(Utils.textBytesToString(deviceID), userPk);
        }
    }

    /**
     * 节点用户信息
     * @param peer 节点用户信息
     * @param bean 朋友信息
     */
    void onUserHeadPic(byte[] peer, UserHeadPic bean) {
        String friendPkStr = ByteUtil.toHexString(peer);
        long updateHPTime = bean.getUpdateHPTime().longValue();
        logger.info("onUserInfo friendPk::{}, updateHPTime::{}", friendPkStr, updateHPTime);
        User user = userRepo.getUserByPublicKey(friendPkStr);

        // 多设备朋友同步
        if (null == user) {
            user = new User(friendPkStr);
            user.updateHPTime = updateHPTime;
            userRepo.addUser(user);
        } else {
            if (updateHPTime > user.updateHPTime) {
               user.headPic = bean.getHeadPic();
                userRepo.updateUser(user);
            }
        }
    }
    /**
     * 处理朋友事件逻辑
     * @param peer 节点用户信息
     */
    void onUserEvent(String userPk, byte[] peer) {
        String friendPkStr = ByteUtil.toHexString(peer);
        logger.info("onUserEvent friendPk::{}", friendPkStr);
        Friend friend = friendRepo.queryFriend(userPk, friendPkStr);
        if (friend != null) {
            friend.focused = 1;
            friend.lastSeenTime = DateUtil.getTime();
            friendRepo.updateFriend(friend);
        }
    }
}