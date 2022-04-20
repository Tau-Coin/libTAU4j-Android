package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import com.google.gson.Gson;

import org.libTAU4j.Account;
import org.libTAU4j.Block;
import org.libTAU4j.Transaction;
import org.libTAU4j.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.ConsensusInfo;
import io.taucoin.torrent.publishing.core.model.data.ForkPoint;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.model.data.message.AirdropTxContent;
import io.taucoin.torrent.publishing.core.model.data.message.AnnouncementContent;
import io.taucoin.torrent.publishing.core.model.data.message.QueueOperation;
import io.taucoin.torrent.publishing.core.model.data.message.SellTxContent;
import io.taucoin.torrent.publishing.core.model.data.message.TrustContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.BlockInfo;
import io.taucoin.torrent.publishing.core.model.data.MemberAutoRenewal;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxConfirm;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.BlockRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxQueueRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.torrent.publishing.ui.chat.ChatViewModel;

/**
 * TauListener处理程序
 */
public class TauListenHandler {
    private static final Logger logger = LoggerFactory.getLogger("TauListenHandler");
    private UserRepository userRepo;
    private MemberRepository memberRepo;
    private TxRepository txRepo;
    private TxQueueRepository txQueueRepo;
    private CommunityRepository communityRepo;
    private BlockRepository blockRepository;
    private SettingsRepository settingsRepo;
    private TauDaemon daemon;
    private Disposable autoRenewalDisposable;
    private Context appContext;

    public enum BlockStatus {
        OFF_CHAIN,
        ON_CHAIN,
        SYNCING,
    }

    TauListenHandler(Context appContext, TauDaemon daemon) {
        this.appContext = appContext;
        this.daemon = daemon;
        userRepo = RepositoryHelper.getUserRepository(appContext);
        memberRepo = RepositoryHelper.getMemberRepository(appContext);
        txRepo = RepositoryHelper.getTxRepository(appContext);
        txQueueRepo = RepositoryHelper.getTxQueueRepository(appContext);
        communityRepo = RepositoryHelper.getCommunityRepository(appContext);
        blockRepository = RepositoryHelper.getBlockRepository(appContext);
        settingsRepo = RepositoryHelper.getSettingsRepository(appContext);
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
    void onNewHeadBlock(Block block) {
        logger.info("onNewHeadBlock");
        String chainID = ChainIDUtil.decode(block.getChainID());
        Community community = communityRepo.getCommunityByChainID(chainID);
        if (community != null) {
            community.headBlock = block.getBlockNumber();
            community.difficulty = block.getCumulativeDifficulty().longValue();
            logger.info("onNewHeadBlock, chainID::{}, difficulty::{}, blockNumber::{}, blockHash::{}",
                    chainID, community.difficulty, block.getBlockNumber(), block.Hash());
            communityRepo.updateCommunity(community);
        }

        handleBlockData(block, BlockStatus.ON_CHAIN);

        updateTxQueue(block);
        // 每个账户自动更新周期，检测是否需要更新账户信息
        if (block.getBlockNumber() % Constants.AUTO_RENEWAL_PERIOD_BLOCKS == 0) {
            logger.debug("accountAutoRenewal chainID::{}, block number::{}",
                    ChainIDUtil.decode(block.getChainID()), block.getBlockNumber());
            accountAutoRenewal();
        }
    }

    /**
     * 处理上报被回滚的区块
     * @param block Block
     */
    void handleRollbackBlock(Block block) {
        logger.debug("handleRollBack");
        handleBlockData(block, BlockStatus.OFF_CHAIN);
    }

    /**
     * 处理上报向前同步的区块
     * @param block Block
     */
    void handleSyncingBlock(Block block) {
        logger.debug("handleSyncingBlock");
        handleBlockData(block, BlockStatus.SYNCING);
    }

    /**
     * 处理上报向前同步的区块
     * @param block Block
     */
    void handleSyncingHeadBlock(Block block) {
        logger.debug("handleSyncingHeadBlock");
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
        logger.debug("handleBlockData:: chainID::{}，blockNum::{}, blockHash::{}", chainID,
                block.getBlockNumber(), block.Hash());
        // 更新区块信息
        saveBlockInfo(block, status);
        // 更新矿工的信息
        saveUserInfo(block.getMiner());
        // 添加矿工为社区成员
        addMemberInfo(block.getChainID(), block.getMiner());
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
        BlockInfo blockInfo = blockRepository.getBlock(chainID, blockHash);
        if (null == blockInfo) {
            long blockNumber = block.getBlockNumber();
            String miner = ByteUtil.toHexString(block.getMiner());
            long difficulty = block.getCumulativeDifficulty().longValue();
            Transaction transaction = block.getTx();
            long rewards;
            if (block.getBlockNumber() <= 0) {
                rewards = block.getMinerBalance();
            } else {
                rewards = null == transaction ? 0L : transaction.getFee();
            }
            // 第一次创建
            int status = blockStatus == BlockStatus.ON_CHAIN ? 1 : 0;
            long timestamp = block.getTimestamp();
            String previousBlockHash = null;
            if (block.getPreviousBlockHash() != null) {
                previousBlockHash = ByteUtil.toHexString(block.getPreviousBlockHash());
            }
            String txID = isTransactionEmpty(transaction) ? null : transaction.getTxID().to_hex();
            blockInfo = new BlockInfo(chainID, blockHash, blockNumber, miner, rewards, difficulty, 
                    status, timestamp, previousBlockHash, txID);
            blockRepository.addBlock(blockInfo);
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
                status = blockStatus == BlockStatus.ON_CHAIN ? 1 : 0;
            }
            blockInfo.status = status;
            blockRepository.updateBlock(blockInfo);
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
        logger.debug("handleTransactionData txID::{}, timestamp::{}, nonce::{}, exist::{}, transaction empty::{}", txID,
                txMsg.getTimestamp(), txMsg.getNonce(), tx != null, isEmpty);
        if (!isEmpty) {
            // 处理用户信息
            handleUserInfo(txMsg);
            // 处理社区成员信息
            handleMemberInfo(txMsg);

            // 本地存在此交易, 更新交易状态值
            if (tx != null) {
                // 由于第一次同步共识区块
                // 如果是同步，并且已经是上链状态了, 则保持上链状态
                int status;
                if (tx.txStatus == 1 && blockStatus == BlockStatus.SYNCING) {
                    status = 1;
                } else {
                    status = blockStatus == BlockStatus.ON_CHAIN ? 1 : 0;
                }
                tx.txStatus = status;
                tx.blockNumber = block.getBlockNumber();
                tx.blockHash = block.Hash();
                txRepo.updateTransaction(tx);
            } else {
                handleTransactionData(block.getBlockNumber(), block.Hash(), txMsg,
                        blockStatus == BlockStatus.ON_CHAIN);
            }
        }
    }

    /**
     * 直接添加新的交易
     * @param blockNumber 区块号
     * @param blockHash 区块哈希
     * @param txMsg 交易
     * @param onChain 是否上链
     */
    private void handleTransactionData(long blockNumber, String blockHash, Transaction txMsg, boolean onChain) {
        if (isTransactionEmpty(txMsg)) {
            logger.info("handleTransactionData transaction empty");
            return;
        }
        String txID = txMsg.getTxID().to_hex();
        String chainID = ChainIDUtil.decode(txMsg.getChainID());
        long fee = txMsg.getFee();
        Tx tx = new Tx(txID, chainID, fee);
        TxContent txContent = new TxContent(txMsg.getPayload());
        tx.txType = txContent.getType();
        tx.memo = txContent.getMemo();
        tx.senderPk = ByteUtil.toHexString(txMsg.getSender());
        tx.txStatus = onChain ? 1 : 0;
        tx.blockNumber = blockNumber;
        tx.blockHash = blockHash;
        tx.timestamp = txMsg.getTimestamp();
        tx.nonce = txMsg.getNonce();
        tx.receiverPk = ByteUtil.toHexString(txMsg.getReceiver());
        tx.amount = txMsg.getAmount();

        if (tx.txType == TxType.TRUST_TX.getType()) {
            TrustContent trustContent = new TrustContent(txMsg.getPayload());
            // 添加Trust信息
            tx.receiverPk = trustContent.getTrustedPkStr();
        } else if (tx.txType == TxType.SELL_TX.getType()) {
            SellTxContent sellTxContent = new SellTxContent(txMsg.getPayload());
            // 添加Sell信息
            tx.coinName = sellTxContent.getCoinName();
            tx.quantity = sellTxContent.getQuantity();
            tx.link = sellTxContent.getLink();
            tx.location = sellTxContent.getLocation();
        } else if (tx.txType == TxType.AIRDROP_TX.getType()) {
            AirdropTxContent sellTxContent = new AirdropTxContent(txMsg.getPayload());
            // 添加Airdrop信息
            tx.link = sellTxContent.getLink();
        } else if (tx.txType == TxType.ANNOUNCEMENT.getType()) {
            AnnouncementContent sellTxContent = new AnnouncementContent(txMsg.getPayload());
            // 添加社区领导者邀请信息
            tx.coinName = sellTxContent.getTitle();
        }
        txRepo.addTransaction(tx);
        logger.info("Add transaction to local, txID::{}, txType::{}", txID, tx.txType);
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
        } else if (txType == TxType.TRUST_TX.getType()) {
            TrustContent trustContent = new TrustContent(txMsg.getPayload());
            saveUserInfo(trustContent.getTrustedPk());
        }
    }

    /**
     * 处理社区成员信息
     * @param txMsg 交易
     */
    private void handleMemberInfo(@NonNull Transaction txMsg) {
        if (isTransactionEmpty(txMsg)) {
            logger.info("handleMemberInfo transaction empty");
            return;
        }
        TxContent txContent = new TxContent(txMsg.getPayload());
        addMemberInfo(txMsg.getChainID(), txMsg.getSender());
        int txType = txContent.getType();
        if (txType == TxType.WIRING_TX.getType()) {
            addMemberInfo(txMsg.getChainID(), txMsg.getReceiver());
        } else if (txType == TxType.TRUST_TX.getType()) {
            TrustContent trustContent = new TrustContent(txMsg.getPayload());
            addMemberInfo(txMsg.getChainID(), trustContent.getTrustedPk());
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
        String userPk = ByteUtil.toHexString(publicKey);
        User user = userRepo.getUserByPublicKey(userPk);
        if (null == user) {
            user = new User(userPk);
            userRepo.addUser(user);
            logger.info("SaveUserInfo to local, publicKey::{}", userPk);
        }
    }

    /**
     * 添加社区成员到本地
     * @param publicKey 公钥
     * @param chainID chainID
     */
    private void addMemberInfo(byte[] chainID, byte[] publicKey) {
        addMemberInfo(chainID, ByteUtil.toHexString(publicKey));
    }

    /**
     * 添加社区成员到本地
     * @param publicKey 公钥
     * @param chainID chainID
     */
    private void addMemberInfo(byte[] chainID, String publicKey) {
        String chainIDStr = ChainIDUtil.decode(chainID);
        Member member = memberRepo.getMemberByChainIDAndPk(chainIDStr, publicKey);
        Account account = daemon.getAccountInfo(chainID, publicKey);
        long balance = 0;
        long power = 0;
        long blockNumber = 0;
        long nonce = 0;
        if (account != null) {
            balance = account.getBalance();
            power = account.getEffectivePower();
            blockNumber = account.getBlockNumber();
            nonce = account.getNonce();
        }

        if (null == member) {
            member = new Member(chainIDStr, publicKey, balance, power, nonce, blockNumber);
            memberRepo.addMember(member);
            logger.info("AddMemberInfo to local, chainID::{}, publicKey::{}, balance::{}, power::{}",
                    chainIDStr, publicKey, balance, power);
        } else {
            member.balance = balance;
            member.power = power;
            member.blockNumber = blockNumber;
            member.nonce = nonce;
            memberRepo.updateMember(member);
            logger.info("Update Member's balance and power, chainID::{}, publicKey::{}, " +
                    "balance::{}, power::{}", chainIDStr, publicKey, member.balance, member.power);
        }
    }

    /**
     * libTAU上报新的Tail区块
     * @param block tail block
     */
    void handleNewTailBlock(Block block) {
        logger.info("handleNewTailBlock");
        String chainID = ChainIDUtil.decode(block.getChainID());
        Community community = communityRepo.getCommunityByChainID(chainID);
        if (community != null) {
            community.tailBlock = block.getBlockNumber();
            logger.info("handleNewTailBlock, chainID::{}, blockNumber::{}, blockHash::{}",
                    chainID, block.getBlockNumber(), block.Hash());
            communityRepo.updateCommunity(community);
        }

        handleBlockData(block, BlockStatus.ON_CHAIN);
    }

    /**
     * libTAU上报当前共识点区块
     * @param block 共识点区块
     */
    void handleNewConsensusBlock(Block block) {
        logger.info("handleNewConsensusBlock");
        String chainID = ChainIDUtil.decode(block.getChainID());
        Community community = communityRepo.getCommunityByChainID(chainID);
        if (community != null) {
            community.consensusBlock = block.getBlockNumber();
            logger.info("handleNewConsensusBlock, chainID::{}, blockNumber::{}, blockHash::{}",
                    chainID, block.getBlockNumber(), block.Hash());
            communityRepo.updateCommunity(community);
        }
        handleBlockData(block, BlockStatus.ON_CHAIN);
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
        logger.debug("handleNewTransaction txID::{}, timestamp::{}, nonce::{}, exist::{}, transaction empty::{}",
                txID, txMsg.getTimestamp(), txMsg.getNonce(), tx != null, isTransactionEmpty(txMsg));
        if (null == tx) {
            handleUserInfo(txMsg);
            handleTransactionData(0, null, txMsg, false);
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
     * 处理libTAU上报节点投票
     * @param chainID 链的ID
     * @param votes 投票结果
     */
    void handleNewTopVotes(byte[] chainID, List<Vote> votes) {
        logger.info("handleNewTopVotes");
        String chainIDStr = ChainIDUtil.decode(chainID);
        Community community = communityRepo.getCommunityByChainID(chainIDStr);
        if (community != null) {
            List<ConsensusInfo> list = new ArrayList<>();
            for (Vote vote : votes) {
                String hash = vote.getBlockHash().to_hex();
                long number = vote.getBlockNumber();
                long count = vote.getVoteCount();
                ConsensusInfo info = new ConsensusInfo(hash, number, count);
                list.add(info);
            }
            community.topConsensus = new Gson().toJson(list);
            communityRepo.updateCommunity(community);
            logger.info("handleNewTopVotes chainID::{}, topConsensus::{}", chainIDStr,
                    community.topConsensus);
        }
    }

    /**
     * 账户自动更新
     */
    void accountAutoRenewal() {
        if (autoRenewalDisposable != null && !autoRenewalDisposable.isDisposed()) {
            autoRenewalDisposable.dispose();
        }
        autoRenewalDisposable = Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            List<MemberAutoRenewal> members = memberRepo.queryAutoRenewalAccounts();
            logger.debug("accountAutoRenewal, members size::{}",
                    members != null ? members.size() : 0);
            if (members != null) {
                for (int i = 0; i < members.size(); i++) {
                    MemberAutoRenewal member = members.get(i);
                    if (emitter.isDisposed()) {
                        break;
                    }
                    if (member.balance <= 0) {
                        logger.debug("accountAutoRenewal chainID::{}, publicKey::{}, Insufficient Balance::{}",
                                member.chainID, member.publicKey, FmtMicrometer.fmtBalance(member.balance));
                        continue;
                    }
                    long medianTxFree = getTxFee(member.chainID);
                    long txFree = medianTxFree;
                    TxQueueAndStatus txQueue = txQueueRepo.getAccountRenewalTxQueue(member.chainID,
                            member.publicKey);
                    if (txQueue != null) {
                        float hours = DateUtil.timeDiffHours(txQueue.timestamp, DateUtil.getMillisTime());
                        // 一天内只发一次
                        if (hours < 24) {
                            logger.debug("accountAutoRenewal, chainID::{}, publicKey::{}, Balance::{}, Hours::{}",
                                    member.chainID, member.publicKey, FmtMicrometer.fmtBalance(member.balance), hours);
                            continue;
                        }
                        txFree += txQueue.fee;
                    }
                    if (member.balance <= txFree) {
                        txFree = member.balance;
                    }
                    long amount = member.balance - txFree;
                    logger.debug("accountAutoRenewal chainID::{}, publicKey::{} (balance::{}, " +
                                    "amount::{}, fee::{}), medianTxFree::{}",
                            member.chainID, member.publicKey, FmtMicrometer.fmtBalance(member.balance),
                            FmtMicrometer.fmtBalance(amount), FmtMicrometer.fmtFeeValue(txFree),
                            FmtMicrometer.fmtFeeValue(medianTxFree));
                    if (null == txQueue) {
                        String memo = appContext.getString(R.string.tx_memo_auto_renewal);
                        TxContent txContent = new TxContent(TxType.WIRING_TX.getType(), memo);
                        TxQueue tx = new TxQueue(member.chainID, member.publicKey, member.publicKey,
                                amount, txFree, 2, TxType.WIRING_TX.getType(), txContent.getEncoded());
                        txQueueRepo.addQueue(tx);
                        ChatViewModel.syncSendMessageTask(appContext, tx, QueueOperation.INSERT);
                        daemon.updateTxQueue(tx.chainID);
                        logger.debug("accountAutoRenewal updateTxQueue");
                    } else {
                        txQueue.amount = amount;
                        txQueue.fee = txFree;
                        txQueueRepo.updateQueue(txQueue);
                        daemon.updateTxQueue(txQueue.chainID);
                        logger.debug("accountAutoRenewal resendTxQueue txFree::{}", txFree);
                    }
                }
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
    }

    /**
     * 节点账户状态
     * @param chainIDBytes 链ID
     * @param userPk 用户公钥
     * @param account 账户状态
     */
    void onAccountState(byte[] chainIDBytes, String userPk, Account account) {
        String chainID = ChainIDUtil.decode(chainIDBytes);
        logger.debug("onAccountState chainID::{}, userPk::{}, account empty::{}",
                chainID, userPk, null == account);
        if (account != null) {
            Member member = memberRepo.getMemberByChainIDAndPk(chainID, userPk);
            if (member != null && account.getBlockNumber() >= member.blockNumber) {
                member.blockNumber = account.getBlockNumber();
                member.balance = account.getBalance();
                member.power = account.getEffectivePower();
                member.nonce = account.getNonce();
                memberRepo.updateMember(member);
            }
        }
    }

    /**
     * 交易确认
     * @param txHash 交易Hash
     * @param peer peerID
     */
    void onTxConfirmed(byte[] txHash, String peer) {
        String txID = ByteUtil.toHexString(txHash);
        TxConfirm txConfirm = txRepo.getTxConfirm(txID, peer);
        logger.trace("onTxConfirmed txID::{}, exist::{}", txID, txConfirm != null);
        if (null == txConfirm) {
            txConfirm = new TxConfirm(txID, peer, DateUtil.getMillisTime());
            txRepo.addTxConfirm(txConfirm);
        }
    }

    /**
     * 0、默认为最小交易费
     * 1、从交易池中获取前10名交易费的中位数
     * 2、如果交易池返回小于等于0，用上次交易用的交易费
     * @param chainID 交易所属的社区chainID
     */
    private long getTxFee(String chainID) {
        long free = Constants.MIN_FEE.longValue();
        long medianFree = daemon.getMedianTxFree(chainID);
        if (medianFree > 0) {
            free = medianFree;
        } else {
            long lastTxFee = settingsRepo.lastTxFee(chainID);
            if (lastTxFee > 0) {
                free = lastTxFee;
            }
        }
        return free;
    }

    public void onCleared() {
        if (autoRenewalDisposable != null && !autoRenewalDisposable.isDisposed()) {
            autoRenewalDisposable.dispose();
        }
        autoRenewalDisposable = null;
    }
}
