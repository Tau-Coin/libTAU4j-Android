package io.taucbd.news.publishing.core.model;

import android.content.Context;
import android.text.SpannableStringBuilder;

import com.google.gson.Gson;

import org.libTAU4j.Account;
import org.libTAU4j.Block;
import org.libTAU4j.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.core.Constants;
import io.taucbd.news.publishing.core.model.data.ForkPoint;
import io.taucbd.news.publishing.core.model.data.TxLogStatus;
import io.taucbd.news.publishing.core.model.data.UserAndFriend;
import io.taucbd.news.publishing.core.model.data.message.NewsContent;
import io.taucbd.news.publishing.core.model.data.message.NoteContent;
import io.taucbd.news.publishing.core.model.data.message.TxContent;
import io.taucbd.news.publishing.core.model.data.message.TxType;
import io.taucbd.news.publishing.core.storage.RepositoryHelper;
import io.taucbd.news.publishing.core.storage.sqlite.entity.BlockInfo;
import io.taucbd.news.publishing.core.storage.sqlite.entity.TxLog;
import io.taucbd.news.publishing.core.storage.sqlite.entity.User;
import io.taucbd.news.publishing.core.storage.sqlite.repo.BlockRepository;
import io.taucbd.news.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucbd.news.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucbd.news.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucbd.news.publishing.core.storage.sqlite.repo.TxQueueRepository;
import io.taucbd.news.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucbd.news.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Community;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Member;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Tx;
import io.taucbd.news.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.core.utils.LinkUtil;
import io.taucbd.news.publishing.core.utils.DateUtil;
import io.taucbd.news.publishing.core.utils.PictureSplitUtil;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.UsersUtil;
import io.taucbd.news.publishing.core.utils.rlp.ByteUtil;
import io.taucbd.news.publishing.ui.TauNotifier;
import io.taucbd.news.publishing.ui.transaction.TxUtils;

/**
 * TauListener处理程序
 */
public class TauListenHandler {
    private static final Logger logger = LoggerFactory.getLogger("TauListenHandler");
    private final UserRepository userRepo;
    private final MemberRepository memberRepo;
    private final TxRepository txRepo;
    private final TxQueueRepository txQueueRepo;
    private final CommunityRepository communityRepo;
    private final BlockRepository blockRepo;
    private final FriendRepository friendRepo;
    private final TauDaemon daemon;
    private final Context appContext;

    public enum BlockStatus {
        ROLL_BACK,
        ON_CHAIN,
        SYNCING,
        NEW_TX,
        NEW_BLOCK,
    }

    TauListenHandler(Context appContext, TauDaemon daemon) {
        this.appContext = appContext;
        this.daemon = daemon;
        userRepo = RepositoryHelper.getUserRepository(appContext);
        memberRepo = RepositoryHelper.getMemberRepository(appContext);
        txRepo = RepositoryHelper.getTxRepository(appContext);
        txQueueRepo = RepositoryHelper.getTxQueueRepository(appContext);
        communityRepo = RepositoryHelper.getCommunityRepository(appContext);
        blockRepo = RepositoryHelper.getBlockRepository(appContext);
        friendRepo = RepositoryHelper.getFriendsRepository(appContext);
    }

    /**
     * 更新发交易队列
     * @param block Block
     */
    private void updateTxQueue(Block block) {
        String chainID = ChainIDUtil.decode(block.getChainID());
        daemon.updateTxQueue(chainID, true);
        logger.debug("updateTxQueue chainID::{}", chainID);
//        User currentUser = userRepo.getCurrentUser();
//        if (currentUser != null) {
//            Transaction transaction = block.getTx();
//            String miner = ByteUtil.toHexString(block.getMiner());
//            if (transaction != null) {
//                String sender = ByteUtil.toHexString(transaction.getSender());
//                String receiver = null;
//                if (transaction.getReceiver() != null) {
//                    receiver = ByteUtil.toHexString(transaction.getReceiver());
//                }
//                // 检查更新交易队列
//                // 1、miner是自己，balance变化
//                // 2、sender是自己，nonce变化
//                // 3、receiver是自己， balance变化
//                if (StringUtil.isEquals(currentUser.publicKey, miner) ||
//                        StringUtil.isEquals(currentUser.publicKey, sender)
//                        || StringUtil.isEquals(currentUser.publicKey, receiver)) {
//                    daemon.updateTxQueue(chainID);
//                    logger.debug("updateTxQueue chainID::{}, sender::{}", chainID, sender);
//                }
//            }
//        }
    }

    /**
     * libTAU上报新head block
     * @param block head Block
     */
    void onNewHeadBlock(Block block, String userPk, List<Account> accounts) {
        logger.info("onNewHeadBlock");
        String chainID = ChainIDUtil.decode(block.getChainID());
        Community community = communityRepo.getCommunityByChainID(chainID);
        if (community != null) {
            community.headBlock = block.getBlockNumber();
            community.difficulty = block.getCumulativeDifficulty().longValue();
            String consensusBlockHash = block.GenesisBlockHash();
            Block consensusBlock = daemon.getBlockByHash(chainID, consensusBlockHash);
            if (consensusBlock != null) {
                community.consensusBlock = consensusBlock.getBlockNumber();
            }
            logger.info("onNewHeadBlock, chainID::{}, difficulty::{}, headBlock::{}, consensusBlock::{}",
                    chainID, community.difficulty, community.headBlock, community.consensusBlock);
            communityRepo.updateCommunity(community);
        }

        //add for update txs user received, and sended, modified tc
        List<String> senderPks = txRepo.queryTxSendersReceived(chainID, userPk); //receiver is user
        senderPks.add(userPk); //sender is user
        for(String senderPk : senderPks) {
            Account account = daemon.getAccountInfo(block.getChainID(), senderPk);
            if (account != null) {
                // 处理区块回滚，
                // 1、交易nonce大于libTAU的nonce，都置为未上链；
                long nonce = account.getNonce();
                int count = txRepo.updateAllOffChainTxs(chainID, senderPk, nonce);
                logger.info("onNewHeadBlock updateAllOffChainTxs count::{}, chainID::{}, userPk::{}, nonce::{}",
                    count, chainID, senderPk, nonce);
            }
            // 2、区块blockNumber大于最新head block的区块状态置为未上链；
            long headBlock = block.getBlockNumber();
            long count = blockRepo.updateAllOffChainBlocks(chainID, headBlock);
            logger.info("onNewHeadBlock updateAllOffChainBlocks count::{}, chainID::{}, headBlock::{}",
                        count, chainID, headBlock);
        }

        handleBlockData(block, BlockStatus.NEW_BLOCK);

        updateTxQueue(block);

        // 更新账户的信息
        if(accounts.size() > 0) {
            // consensus state，更新所有用户
            onStateArray(block.getChainID(), accounts, true);
        } else {
            // 3.调整total offchain coins, 只调整当前用户
            Account account = daemon.getAccountInfo(block.getChainID(), userPk);
            if (account != null) {
                long offchainCoins = txRepo.getChainTotalCoinsByNonce(chainID, userPk, account.getNonce());
                Member member = memberRepo.getMemberByChainIDAndPk(chainID, userPk);
                if (member != null) {
                        member.balance = account.getBalance();
                        member.totalOffchainCoins = offchainCoins;
                        member.nonce = account.getNonce();
                        member.power = account.getPower();
                        memberRepo.updateMember(member);
                    }
                }
        }

        // 更新社区用户账户信息
        daemon.getMyAccountManager().update(chainID);
    }

    /**
     * 处理上报被回滚的区块
     * @param block Block
     */
    void handleRollbackBlock(Block block) {
        logger.info("handleRollBack");
        String chainID = ChainIDUtil.decode(block.getChainID());
        Community community = communityRepo.getCommunityByChainID(chainID);
        if (community != null) {
            String consensusBlockHash = block.GenesisBlockHash();
            Block consensusBlock = daemon.getBlockByHash(chainID, consensusBlockHash);
            if (consensusBlock != null && community.consensusBlock != consensusBlock.getBlockNumber()) {
                community.consensusBlock = consensusBlock.getBlockNumber();
                logger.info("handleRollBack chainID::{}, difficulty::{}, headBlock::{}, consensusBlock::{}",
                        chainID, community.difficulty, community.headBlock, community.consensusBlock);
                communityRepo.updateCommunity(community);
            }
        }
        handleBlockData(block, BlockStatus.ROLL_BACK);

        daemon.updateChainTxStoppedTime(chainID);
        logger.debug("updateChainTxStoppedTime chainID::{}", chainID);
    }

    /**
     * 处理上报向前同步的区块
     * @param block Block
     */
    void handleSyncingBlock(Block block) {
        logger.info("handleSyncingBlock");
        handleBlockData(block, BlockStatus.SYNCING);
    }

    /**
     * 处理上报向前同步的区块
     * @param block Block
     */
    void handleSyncingHeadBlock(Block block) {
        logger.info("handleSyncingHeadBlock");
        String chainID = ChainIDUtil.decode(block.getChainID());
        Community community = communityRepo.getCommunityByChainID(chainID);
        if (community != null) {
            community.syncingHeadBlock = block.getBlockNumber();
            logger.info("handleSyncingHeadBlock chainID::{}, blockNumber::{}, blockHash::{}",
                    chainID, block.getBlockNumber(), block.Hash());
            communityRepo.updateCommunity(community);
        }
        handleBlockData(block, BlockStatus.SYNCING);
    }

    /**
     * 处理Block数据：解析Block数据，处理社区、交易、用户、社区成员数据
     * 1、更新区块信息
     * 2、处理矿工用户数据
     * 3、处理交易数据、用户数据、社区成员数据
     * @param block 链上区块
     * @param status 状态
     */
    protected void handleBlockData(Block block, BlockStatus status) {
        String chainID = ChainIDUtil.decode(block.getChainID());
        String miner = ByteUtil.toHexString(block.getMiner());
        logger.info("handleBlockData:: chainID::{}，blockNum::{}, blockHash::{}, miner::{}", chainID,
                block.getBlockNumber(), block.Hash(), miner);
        // 更新区块信息
        saveBlockInfo(block, status);
        // 更新矿工的信息
        saveUserInfo(miner);
        // 添加矿工为社区成员
        Transaction tx = block.getTx();
//        long rewards = null == tx ? 0L : tx.getFee();
//        long rewardTime = rewards > 0 ? DateUtil.getMillisTime() : 0;
        addMemberInfo(block.getChainID(), miner, 0);
        // 处理交易信息
        handleTransactionData(block, status);
    }

    /**
     * 保存区块信息，供UI上统计使用
     * @param block 链上区块
     * @param blockStatus 状态
     */
    private void saveBlockInfo(Block block, BlockStatus blockStatus) {
        String chainID = ChainIDUtil.decode(block.getChainID());
        String blockHash = block.Hash();
        BlockInfo blockInfo = blockRepo.getBlock(chainID, blockHash);
        if (null == blockInfo) {
            long blockNumber = block.getBlockNumber();
            String miner = ByteUtil.toHexString(block.getMiner());
            long difficulty = block.getCumulativeDifficulty().longValue();
            Transaction transaction = block.getTx();
            long rewards = null == transaction ? 0L : transaction.getFee();
            rewards += Constants.MINING_REWARDS.longValue();
            // 第一次创建
            int status = blockStatus == BlockStatus.ON_CHAIN || blockStatus == BlockStatus.NEW_BLOCK ? 1 : 0;
            long timestamp = block.getTimestamp();
            String previousBlockHash = null;
            if (block.getPreviousBlockHash() != null) {
                previousBlockHash = ByteUtil.toHexString(block.getPreviousBlockHash());
            }
            String txID = isTransactionEmpty(transaction) ? null : transaction.getTxID().to_hex();
            blockInfo = new BlockInfo(chainID, blockHash, blockNumber, miner, rewards, difficulty,
                    status, timestamp, previousBlockHash, txID);
            blockRepo.addBlock(blockInfo);
            logger.info("Save Block Info, chainID::{}, blockHash::{}, blockNumber::{}, rewards::{}, " +
                            "txID::{}, status::{}",
                    chainID, blockHash, blockNumber, rewards, txID, status);
        } else {
            // 由于第一次同步共识区块
            // 如果是同步，并且已经是上链状态了, 则保持上链状态
            int status;
            if (blockInfo.status == 1 && blockStatus == BlockStatus.SYNCING) {
                status = 1;
            } else {
                status = blockStatus == BlockStatus.ON_CHAIN || blockStatus == BlockStatus.NEW_BLOCK ? 1 : 0;
            }
            blockInfo.status = status;
            blockRepo.updateBlock(blockInfo);
            logger.info("Update Block Info, chainID::{}, blockHash::{}, blockNumber::{}, rewards::{}, status::{}",
                    chainID, blockInfo.blockHash, blockInfo.blockNumber, blockInfo.rewards, status);
        }
    }

    private boolean isTransactionEmpty(Transaction txMsg) {
        if (null == txMsg) {
            return true;
        }
        return null == txMsg.getPayload() || txMsg.getPayload().length == 0;
    }

    /**
     * 处理Block数据：解析Block数据，更新用户、社区成员、交易数据
     * @param block 区块
     * @param blockStatus 状态
     */
    private void handleTransactionData(Block block, BlockStatus blockStatus) {
        if (null == block || null == block.getTx()) {
            return;
        }
        Transaction txMsg = block.getTx();
        String txID = txMsg.getTxID().to_hex();
        Tx tx = txRepo.getTxByTxID(txID);
        boolean isEmpty = isTransactionEmpty(txMsg);
        logger.info("handleTransactionData txID::{}, timestamp::{}, nonce::{}, exist::{}, transaction empty::{}", txID,
                txMsg.getTimestamp(), txMsg.getNonce(), tx != null, isEmpty);
        if (!isEmpty) {
            // 处理用户信息
            handleUserInfo(txMsg);
            // 处理社区成员信息
            handleMemberInfo(txMsg, blockStatus);

            // 本地存在此交易, 更新交易状态值
            if (tx != null) {
                // 由于第一次同步共识区块
                // 如果是同步，并且已经是上链状态了, 则保持上链状态
                tx.txStatus = Constants.TX_STATUS_ON_CHAIN;
                //之所以要更新这么多，是因为P2P点对点转账有部分交易要补全信息
                tx.version = txMsg.getVersion();
                tx.blockNumber = block.getBlockNumber();
                tx.blockHash = block.Hash();
                TxContent txContent = new TxContent(txMsg.getPayload());
                tx.txType = txContent.getType();
                tx.memo = txContent.getMemo();
                tx.previousHash = txMsg.getPreviousHash().to_hex();
                tx.timestamp = txMsg.getTimestamp();
                tx.nonce = txMsg.getNonce();
                tx.receiverPk = ByteUtil.toHexString(txMsg.getReceiver());
                tx.amount = txMsg.getAmount();
                txRepo.updateTransaction(tx);
            } else {
                handleTransactionData(block.getBlockNumber(), block.Hash(), txMsg,
                        blockStatus);
            }
        }
    }

    /**
     * 直接添加新的交易
     * @param blockNumber 区块号
     * @param blockHash 区块哈希
     * @param txMsg 交易
     * @param status 区块状态
     */
    private void handleTransactionData(long blockNumber, String blockHash, Transaction txMsg, BlockStatus status) {
        if (isTransactionEmpty(txMsg)) {
            logger.info("handleTransactionData transaction empty");
            return;
        }
        boolean newTx = status == BlockStatus.NEW_TX;
        String txID = txMsg.getTxID().to_hex();
        String chainID = ChainIDUtil.decode(txMsg.getChainID());
        long fee = txMsg.getFee();
        Tx tx = new Tx(txID, chainID, fee);
        TxContent txContent = new TxContent(txMsg.getPayload());
        tx.txType = txContent.getType();
        tx.memo = txContent.getMemo();
        tx.senderPk = ByteUtil.toHexString(txMsg.getSender());
        tx.txStatus = newTx ? Constants.TX_STATUS_PENDING : Constants.TX_STATUS_ON_CHAIN;
        tx.version = txMsg.getVersion();
        tx.previousHash = txMsg.getPreviousHash().to_hex();
        tx.blockNumber = blockNumber;
        tx.blockHash = blockHash;
        tx.timestamp = txMsg.getTimestamp();
        tx.nonce = txMsg.getNonce();
        tx.receiverPk = ByteUtil.toHexString(txMsg.getReceiver());
        tx.amount = txMsg.getAmount();

        if (tx.txType == TxType.NOTE_TX.getType()) {
            NoteContent noteContent = new NoteContent(txMsg.getPayload());
            // 添加Note信息(link, repliedHash)
            tx.link = noteContent.getLinkStr();
            tx.repliedHash = noteContent.getRepliedHashStr();
        } else if (tx.txType == TxType.NEWS_TX.getType()) {
            NewsContent newsContent = new NewsContent(txMsg.getPayload());
            // 添加News信息(link, repliedHash, repliedKey)
            tx.link = newsContent.getLinkStr();
            tx.repliedHash = newsContent.getRepliedHashStr();
            tx.repliedKey = newsContent.getRepliedKeyStr();
        }

        txRepo.addTransaction(tx);
        logger.info("Add transaction to local, txID::{}, txType::{}", txID, tx.txType);

        // 更新未读状态
        if (tx.txType != TxType.WIRING_TX.getType() && (status == BlockStatus.NEW_TX ||
                status == BlockStatus.NEW_BLOCK)) {
            String userPk = MainApplication.getInstance().getPublicKey();
            // 不是自己发的交易
            if (StringUtil.isNotEmpty(userPk) && StringUtil.isNotEquals(tx.senderPk, userPk)) {
                Member member = memberRepo.getMemberByChainIDAndPk(chainID, userPk);
                if (tx.txType == TxType.NOTE_TX.getType()) {
                    if (member != null && member.msgUnread == 0) {
                        member.msgUnread = 1;
                        memberRepo.updateMember(member);
                    }
                } else {
                    if (member != null && member.newsUnread == 0) {
                        member.newsUnread = 1;
                        memberRepo.updateMember(member);
                    }
                }
                UserAndFriend friend = userRepo.getFriend(tx.senderPk);
                if (friend != null && !friend.isMemBanned) {
                    // 发送社区新交易通知
                    if (tx.txType == Constants.NOTE_TX_TYPE) {
                        Tx repliedTx = txRepo.getTxByTxID(tx.repliedHash);
						if(repliedTx != null) {
							SpannableStringBuilder news = TxUtils.createTxSpan(repliedTx, TxType.NOTE_TX.getType());
							String friendName = UsersUtil.getShowName(friend);
							String msg = friendName + "-" + TxUtils.createTxSpan(tx, TxType.NOTE_TX.getType());
							TauNotifier.getInstance().makeCommunityNotify(tx.chainID, repliedTx.txID, tx.txID, news, msg);
						}
                    } else {
                        SpannableStringBuilder news = TxUtils.createTxSpan(tx, TxType.NOTE_TX.getType());
                        String newsHash = StringUtil.isNotEmpty(tx.repliedHash) ? tx.repliedHash : tx.txID;
                        String repliesID = StringUtil.isNotEmpty(tx.repliedHash) ? tx.txID : null;
                        TauNotifier.getInstance().makeCommunityNotify(tx.chainID, newsHash, repliesID, news, null);
                    }
                }
            }
        }
        // 读取交易发送者的nickname和图标
        UserAndFriend friend = userRepo.getFriend(tx.senderPk);
        // 60分钟刷新请求一次
        int refreshTime = 3600;
        if (friend != null && DateUtil.getTime() - friend.updateNNTime > refreshTime) {
            daemon.subFriendInfo(tx.senderPk);
        }
        if (friend != null && DateUtil.getTime() - friend.updateHPTime > refreshTime) {
            daemon.subFriendHeadPic(tx.senderPk);
        }
    }

    /**
     * 处理用户成员信息
     * @param txMsg 交易
     */
    private void handleUserInfo(@NonNull Transaction txMsg) {
        if (isTransactionEmpty(txMsg)) {
            logger.info("handleUserInfo transaction empty");
            return;
        }
        TxContent txContent = new TxContent(txMsg.getPayload());
        int txType = txContent.getType();
        saveUserInfo(txMsg.getSender());

        if (txType == TxType.WIRING_TX.getType()) {
            saveUserInfo(txMsg.getReceiver());
        }
    }

    /**
     * 处理社区成员信息
     * @param txMsg 交易
     */
    private void handleMemberInfo(@NonNull Transaction txMsg, BlockStatus status) {
        if (isTransactionEmpty(txMsg)) {
            logger.info("handleMemberInfo transaction empty");
            return;
        }
        TxContent txContent = new TxContent(txMsg.getPayload());
        addMemberInfo(txMsg.getChainID(), ByteUtil.toHexString(txMsg.getSender()));
        int txType = txContent.getType();
        if (txType == TxType.WIRING_TX.getType()) {
//            long incomeTime = 0;
            long pendingTime = 0;
            if (txMsg.getAmount() > 0) {
//                incomeTime = status == BlockStatus.NEW_BLOCK ? DateUtil.getMillisTime() : 0;
                pendingTime = status == BlockStatus.NEW_TX || status == BlockStatus.SYNCING ? DateUtil.getMillisTime() : 0;
            }
            addMemberInfo(txMsg.getChainID(), ByteUtil.toHexString(txMsg.getReceiver()), pendingTime);
        }
    }

    /**
     * 保存用户信息到本地
     * @param publicKey 公钥
     */
    private void saveUserInfo(byte[] publicKey) {
        if (null == publicKey) {
            return;
        }
        saveUserInfo(ByteUtil.toHexString(publicKey));
    }

    /**
     * 保存用户信息到本地
     * @param publicKey 公钥
     */
    private void saveUserInfo(String publicKey) {
        if (StringUtil.isEmpty(publicKey)) {
            return;
        }
        User user = userRepo.getUserByPublicKey(publicKey);
        if (null == user) {
            user = new User(publicKey);
            userRepo.addUser(user);
            logger.info("SaveUserInfo to local, publicKey::{}", publicKey);
        }
    }

    /**
     * 添加社区成员到本地
     * @param publicKey 公钥
     * @param chainID chainID
     */
    private void addMemberInfo(byte[] chainID, String publicKey) {
        addMemberInfo(chainID, publicKey, 0);
    }

    /**
     * 添加社区成员到本地
     * @param publicKey 公钥
     * @param chainID chainID
     */
    private void addMemberInfo(byte[] chainID, String publicKey, long pendingTime) {
        String chainIDStr = ChainIDUtil.decode(chainID);
        Member member = memberRepo.getMemberByChainIDAndPk(chainIDStr, publicKey);
        Account account = daemon.getAccountInfo(chainID, publicKey);
        long balance = 0;
        long power = 0;
        long nonce = 0;
        if (account != null) {
            balance = account.getBalance();
            nonce = account.getNonce();
            power = account.getPower();
        }

        logger.info("UpdateMember chainID::{}, publicKey::{}, balance::{}, nonce::{}, power::{}",
                chainIDStr, publicKey, balance, nonce, power);

        if (null == member) {
            member = new Member(chainIDStr, publicKey);
            member.balance = balance;
            member.balUpdateTime = DateUtil.getTime();
            member.nonce = nonce;
            member.power = power;
            if (pendingTime > 0) {
                member.pendingTime = pendingTime;
            }
            memberRepo.addMember(member);
            logger.info("AddMemberInfo to local, chainID::{}, publicKey::{}, balance::{}, pendingTime::{}",
                    chainIDStr, publicKey, balance, pendingTime);
        } else {
            boolean isUpdate = false;
            if (member.balUpdateTime == 0 || member.balance != balance || member.nonce != nonce || member.power != power) {
                member.balance = balance;
                member.balUpdateTime = DateUtil.getTime();
                member.nonce = nonce;
                member.power = power;
                isUpdate = true;
                logger.info("Update Member's balance and power, chainID::{}, publicKey::{}, " +
                        "balance::{}", chainIDStr, publicKey, member.balance);
            }
            if (pendingTime > 0) {
                member.pendingTime = pendingTime;
                isUpdate = true;
                logger.info("Update Member's pendingTime chainID::{}, publicKey::{}, " +
                        "pendingTime::{}", chainIDStr, publicKey, member.pendingTime);
            }
            if (isUpdate) {
                memberRepo.updateMember(member);
            }
        }
    }

    /**
     * 处理libTAU上报接收到新的交易（未上链在交易池中）
     * @param txMsg 交易
     */
    void handleNewTransaction(Transaction txMsg) {
        logger.info("handleNewTransaction");
        if (null == txMsg) {
            return;
        }
        String txID = txMsg.getTxID().to_hex();
        Tx tx = txRepo.getTxByTxID(txID);
        logger.info("handleNewTransaction txID::{}, timestamp::{}, nonce::{}, exist::{}, transaction empty::{}",
                txID, txMsg.getTimestamp(), txMsg.getNonce(), tx != null, isTransactionEmpty(txMsg));
        if (null == tx) {
            handleUserInfo(txMsg);
            handleMemberInfo(txMsg, BlockStatus.NEW_TX);
            handleTransactionData(0, null, txMsg, BlockStatus.NEW_TX);
        }
    }

    /**
     * 处理libTAU上报分叉点
     * @param block 分叉点
     */
    void handleNewForkPoint(Block block) {
        String chainID = ChainIDUtil.decode(block.getChainID());
        Community community = communityRepo.getCommunityByChainID(chainID);
        if (community != null) {
            String hash = block.Hash();
            long number = block.getBlockNumber();
            ForkPoint point = new ForkPoint(hash, number);
            community.forkPoint = new Gson().toJson(point);
            communityRepo.updateCommunity(community);
            logger.info("handleNewForkPoint chainID::{}, hash::{}, number::{}", chainID, hash, number);
        }
    }

    /**
     * Arrived Prefix Swarm 绿色(等价网络节点>1)
     * @param txHash 交易Hash
     */
    void onTxArrived(byte[] txHash) {
        String hash = ByteUtil.toHexString(txHash);
        TxLog log = txRepo.getTxLog(hash, TxLogStatus.ARRIVED_SWARM.getStatus());
        logger.info("onTxArrived txID::{}, exist::{}", hash, log != null);
        if (null == log) {
            log = new TxLog(hash, TxLogStatus.ARRIVED_SWARM.getStatus(), DateUtil.getMillisTime());
            txRepo.addTxLog(log);
        }
    }

    public void onCleared() {
    }

    /**
     * 添加社区
     * @param chainURL 链URL
     */
    public void addCommunity(String chainURL) {
        if (StringUtil.isEmpty(chainURL)) {
            return;
        }
        LinkUtil.Link url = LinkUtil.decode(chainURL);
        String chainID = url.getData();
        String peer = url.getPeer();
        logger.debug("addCommunity::{}, peer::{}, chainURL::{}", chainID, peer, chainURL);
        Set<String> peers = new HashSet<>();
        peers.add(peer);
        if (url.getMiner() != null) {
            peers.add(url.getMiner());
        }
        Community community = communityRepo.getCommunityByChainID(chainID);
        if (community != null && community.isBanned) {
            logger.info("addCommunity chainID::{}, Banned::true", chainID);
            return;
        }
        boolean isSuccess = daemon.followChain(chainID, peers);
        if (isSuccess) {
            if (null == community) {
                community = new Community(chainID, ChainIDUtil.getName(chainID));
                communityRepo.addCommunity(community);
            }
            String userPK = MainApplication.getInstance().getPublicKey();
            Member member = memberRepo.getMemberByChainIDAndPk(chainID, userPK);
            if (null == member) {
                saveUserInfo(userPK);
                addMemberInfo(ChainIDUtil.encode(chainID), userPK);
                // 默认置顶
                member = memberRepo.getMemberByChainIDAndPk(chainID, userPK);
                if (member != null) {
                    member.stickyTop = 0;
                    memberRepo.updateMember(member);
                }
                saveUserInfo(peer);
                addMemberInfo(ChainIDUtil.encode(chainID), peer);
            }
        }
    }

    /**
     * 处理用户状态
     * @param chainIDBytes 链ID
     * @param accounts 账户列表
     */
    public void onStateArray(byte[] chainIDBytes, List<Account> accounts, boolean reset) {
        String chainID = ChainIDUtil.decode(chainIDBytes);
        int accountSize = null == accounts ? 0 : accounts.size();
        logger.info("onStateArray chainID::{}, accounts::{}", chainID, accountSize);
        if (accountSize > 0) {
            long modifiedTime = DateUtil.getTime();
            for (Account account : accounts) {
                String peer = ByteUtil.toHexString(account.getPeer());
                logger.info("UpdateMember onStateArray chainID::{}, publicKey::{}, balance::{}, nonce::{}, power::{}",
                        chainID, peer, account.getBalance(), account.getNonce(), account.getPower());

                //txrepo交互
                long offchainCoins = 0;
                //只更新当前用户
                if(peer.equals(MainApplication.getInstance().getPublicKey()))
                    offchainCoins = txRepo.getChainTotalCoinsByNonce(chainID, peer, account.getNonce());

                //Update txs
				if(reset) {
					txRepo.updateAllOffChainTxs(chainID, peer, account.getNonce()); //该链所有的交易 -> Pending
					txRepo.updateAllOnChainTxs(chainID, peer, account.getNonce()); //该链所有的交易 -> Settled
				}

                Member member = memberRepo.getMemberByChainIDAndPk(chainID, peer);
                if (member != null) {
                    member.consensusBalance = account.getBalance(); //consensusBalance只在这里更新
                    member.balance = account.getBalance();
                    member.totalPendingCoins = offchainCoins;
                    member.totalOffchainCoins = offchainCoins;
                    member.balUpdateTime = modifiedTime;
                    member.nonce = account.getNonce();
                    member.consensusPower = account.getPower();
                    member.power = account.getPower();
                    memberRepo.updateMember(member);
                } else {
                    saveUserInfo(peer);
                    member = new Member(chainID, peer);
                    member.consensusBalance = account.getBalance(); //consensusBalance只在这里更新
                    member.balance = account.getBalance();
                    member.totalPendingCoins = offchainCoins;
                    member.totalOffchainCoins = offchainCoins;
                    member.balUpdateTime = modifiedTime;
                    member.nonce = account.getNonce();
                    member.consensusPower = account.getPower();
                    member.power = account.getPower();
                    memberRepo.addMember(member);
                }
            }

            //遇到consensus block state清空
            if(reset) {
                //未修改的member说明不在consensus state中，重置为0
                memberRepo.resetMembers(chainID, modifiedTime);
                return;
            }
        }
    }

    /**
     * 上报图片内容
     */
    public void onPicSlice(byte[] chainID, byte[] newsHash, byte[] key, byte[] slice) {
		String hash = ByteUtil.toHexString(newsHash);
		logger.info("pic slice alert coming, news hash::{}", hash);
		//如果图片完成切片，直接return
		if(PictureSplitUtil.isSlicesGetCompleted(hash))
			return;
		//图片未完成切片，看是否需要存储
		String sliceName = ByteUtil.toHexString(key);
		if(!PictureSplitUtil.isSliceExists(hash, sliceName)){
			try {
				logger.info("save pic slice alert news hash::{}, key::{}", hash, sliceName);
				String picturePath = PictureSplitUtil.savePictureSlices(hash, sliceName, slice);
				//看以下是否完成图片内容，更新txQueue, tx
				if(PictureSplitUtil.isSlicesGetCompleted(hash)) {
					logger.info("save pic slice successfully, news hash::{}", hash);
					Tx tx = txRepo.getTxByTxID(hash);
					if(tx != null && StringUtil.isNotEmpty(picturePath) && StringUtil.isEmpty(tx.picturePath)) {
						tx.picturePath = picturePath;
						TxQueue txqueue = txQueueRepo.getTxQueueByID(tx.queueID);
						if(txqueue != null && StringUtil.isEmpty(txqueue.picturePath)) {
							txqueue.picturePath = picturePath;
							txQueueRepo.updateQueue(txqueue);
						}
						txRepo.updateTransaction(tx);
					}
				}
			} catch (Exception e) {
				logger.error("pic slice alert save pic error::{}", e);
			}
		}
    }

    /**
     * 在线peer（判断peer是否在本地存在）
     * @param chainId 链ID
     * @param peer 在线节点
     */
    public void onlinePeer(byte[] chainId, String peer) {
        logger.info("onlinePeer chainID::{}, peer::{}", ChainIDUtil.decode(chainId), peer);
        saveUserInfo(peer);
        addMemberInfo(chainId, peer);
    }
}
