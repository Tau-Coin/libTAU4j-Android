package io.taucoin.news.publishing.core.model;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import org.libTAU4j.Account;
import org.libTAU4j.Ed25519;
import org.libTAU4j.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.Constants;
import io.taucoin.news.publishing.core.model.data.ChatMsgStatus;
import io.taucoin.news.publishing.core.model.data.TxFreeStatistics;
import io.taucoin.news.publishing.core.model.data.UserHeadPic;
import io.taucoin.news.publishing.core.model.data.UserInfo;
import io.taucoin.news.publishing.core.model.data.FriendStatus;
import io.taucoin.news.publishing.core.model.data.message.AirdropStatus;
import io.taucoin.news.publishing.core.model.data.message.MessageType;
import io.taucoin.news.publishing.core.model.data.message.MsgContent;
import io.taucoin.news.publishing.core.model.data.message.TxContent;
import io.taucoin.news.publishing.core.model.data.message.TxType;
import io.taucoin.news.publishing.core.model.data.message.QueueOperation;
import io.taucoin.news.publishing.core.storage.RepositoryHelper;
import io.taucoin.news.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.news.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Device;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.news.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.news.publishing.core.storage.sqlite.entity.User;
import io.taucoin.news.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.news.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.news.publishing.core.storage.sqlite.repo.DeviceRepository;
import io.taucoin.news.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucoin.news.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.news.publishing.core.storage.sqlite.repo.TxQueueRepository;
import io.taucoin.news.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.news.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.news.publishing.core.utils.ChainIDUtil;
import io.taucoin.news.publishing.core.utils.DateUtil;
import io.taucoin.news.publishing.core.utils.FmtMicrometer;
import io.taucoin.news.publishing.core.utils.LinkUtil;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.Utils;
import io.taucoin.news.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.news.publishing.ui.TauNotifier;
import io.taucoin.news.publishing.ui.chat.ChatViewModel;

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
    private final TxRepository txRepo;
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
        txRepo = RepositoryHelper.getTxRepository(appContext);
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
                        sentTime, logicMsgHash, msgContent.getChainID(), msgContent.getReferralPeer());
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

                // 如果是Airdrop, 则给此消息的发送者airdrop coins, 目前airdrop机制不在了
                /*
                if (msgContent.getType() == MessageType.AIRDROP.getType()) {
                    String chainID = msgContent.getChainID();
                    handleAirdropCoins(chainID, userPk, senderPk, msgContent.getReferralPeer());
                }
    `           */

                String contentStr = Utils.textBytesToString(content);

                //如果是转账交易下的点对点, 数据库存入转账消息, 目前是紧绑定 TODO: TC
                if (msgContent.getType() == MessageType.WIRING.getType()) {
                    String endl = "\n";
                    String space = " ";
                    //获取txid
                    String txIDTag = "Transmission ID: ";
                    int indexStart = contentStr.indexOf(txIDTag) + txIDTag.length();
                    int indexEnd = contentStr.indexOf(endl, indexStart);
                    String txID = contentStr.substring(indexStart, indexEnd);
                    //获取chainID
                    String chainID = msgContent.getChainID();
                    //获取sender senderPk
                    //获取receiver userPk
                    //获取交易金额
                    String amountTag = "Amount: ";
                    indexStart = contentStr.indexOf(amountTag) + txIDTag.length();
                    indexEnd = contentStr.indexOf(space, indexStart);
                    long  amount = FmtMicrometer.fmtTxLongValue(contentStr.substring(indexStart, indexEnd));
                    //获取交易费
                    String feeTag = "Fee: ";
                    indexStart = contentStr.indexOf(feeTag) + feeTag.length();
                    indexEnd = contentStr.indexOf(space, indexStart);
                    long  fee = FmtMicrometer.fmtTxLongValue(contentStr.substring(indexStart, indexEnd));
                    Tx tx = new Tx(chainID, userPk, amount, fee, TxType.WIRING_TX.getType(), "p2p income");
                    tx.txID = txID;
                    tx.senderPk = senderPk;
                    txRepo.addTransaction(tx);
                }

                // 创建通知栏消息
                User friendUser = userRepo.getUserByPublicKey(senderPk);
                if (friendUser != null && !friendUser.isBanned && contentStr.length() > 0) {
                    TauNotifier.getInstance().makeChatNotify(friendUser, contentStr);
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
    private void handleAirdropCoins(String chainID, String currentPk, String friendPk, String referralPeer) {
        /*
        logger.info("handleAirdropCoins: yourself::{}, currentPk::{}, friendPk::{}, chainID::{}, referralPeer::{}",
                StringUtil.isEquals(currentPk, friendPk), currentPk, friendPk, chainID, referralPeer);
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
            ChatViewModel.syncSendMessageTask(appContext, currentPk, friendPk,
                    appContext.getString(R.string.tx_memo_airdrop_finished),  MessageType.TEXT.getType());
            return;
        }

        // airdrop coins
        String memo = appContext.getString(R.string.tx_memo_airdrop);
        long amount = member.airdropCoins;
        long fee = getAverageTxFee(chainID);
        TxContent txContent = new TxContent(TxType.WIRING_TX.getType(), memo);
        TxQueue tx = new TxQueue(chainID, currentPk, friendPk, amount, fee, 1,
                TxType.WIRING_TX.getType(), txContent.getEncoded());
		long queueID = txQueueRepo.addQueue(tx);
		tx.queueID = queueID;
        logger.info("automatic airdrop queueID::{}", queueID);
        daemon.sendTxQueue(tx, 1, 1);// not pinned tx, 1

        Account account = daemon.getAccountInfo(ChainIDUtil.encode(chainID), tx.senderPk);

        // 给推荐者发送金额
        boolean isSendReferralBonus = StringUtil.isNotEmpty(referralPeer) && StringUtil.isNotEquals(friendPk, referralPeer);
        if (isSendReferralBonus) {
            // 最多发送10次
            int referralCount = txQueueRepo.getReferralCount(chainID, currentPk, referralPeer, currentTime);
            if (referralCount > 10) {
                logger.info("handleAirdropCoins: referral count::{}, chainID::{}, currentPk::{}, referralPeer::{}",
                        referralCount, chainID, currentPk, referralPeer);
            } else {
                String referralMemo = appContext.getString(R.string.tx_memo_referral);
                long referralBonus = member.airdropCoins / 2;
                referralBonus = Math.max(1, referralBonus);
                TxContent referralContent = new TxContent(TxType.WIRING_TX.getType(), referralMemo);
                TxQueue referralTx = new TxQueue(chainID, currentPk, referralPeer, referralBonus, fee, 2,
                        TxType.WIRING_TX.getType(), referralContent.getEncoded());
                txQueueRepo.addQueue(referralTx);
				long queueRTxID = txQueueRepo.addQueue(referralTx);
				referralTx.queueID = queueRTxID;
				logger.info("automatic referal queueID::{}", queueRTxID);
				daemon.sendTxQueue(referralTx, 1, 1);// not pinned tx, 1


                logger.info("handleAirdropCoins: referral count::{}, bonus::{}, chainID::{}, currentPk::{}, referralPeer::{}",
                        referralCount, referralBonus, chainID, currentPk, referralPeer);

                // 余额不足不发送点对点消息
                if (tx.amount + referralBonus + fee + fee <= account.getBalance()) {
                    ChatViewModel.syncSendMessageTask(appContext, referralTx, QueueOperation.INSERT);
                }
            }
        }
        // 余额不足不发送点对点消息
        if (account != null) {
            if (tx.amount + fee <= account.getBalance()) {
                long airdropTime = member.airdropTime / 60 / 1000;
                long airdropCoins = member.airdropCoins;
                String referralLink = LinkUtil.encodeAirdropReferral(member.publicKey, member.chainID,
                        airdropCoins, airdropTime, friendPk);
                logger.debug("handleAirdropCoins referralLink::{}", referralLink);
                long referralBonus = member.airdropCoins / 2;
                referralBonus = Math.max(1, referralBonus);
                referralLink += appContext.getString(R.string.bot_airdrop_referral_link_tips,
                        FmtMicrometer.fmtLong(referralBonus), FmtMicrometer.fmtLong(referralBonus * 10));
                ChatViewModel.syncSendMessageTask(appContext, tx, referralLink, QueueOperation.INSERT);
            }
        }
        daemon.updateTxQueue(tx.chainID);
        */
    }

    private long getAverageTxFee(String chainID) {
        long txFee;
        try {
            TxFreeStatistics statistics = txRepo.queryAverageTxsFee(chainID);
            txFee = Constants.WIRING_MIN_FEE.longValue();
            if (statistics != null) {
                float wiringRate = statistics.getWiringCount() * 100f / statistics.getTotal();
                if (wiringRate >= 50) {
                    long averageTxsFee = statistics.getTotalFee() / statistics.getTxsCount();
                    txFee = averageTxsFee + Constants.COIN.longValue();
                }
            }
        } catch (Exception e) {
            txFee = Constants.MIN_FEE.longValue();
        }
        return txFee;
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
    void onPeerAttention(String userPk, byte[] peer) {
        String friendPkStr = ByteUtil.toHexString(peer);
        logger.info("onPeerAttention friendPk::{}", friendPkStr);
        Friend friend = friendRepo.queryFriend(userPk, friendPkStr);
        if (friend != null) {
            friend.focused = 1;
            friend.lastSeenTime = DateUtil.getTime();
            friendRepo.updateFriend(friend);
        }
    }
}
