package io.taucoin.torrent.publishing.core.model;

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
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.ForkPoint;
import io.taucoin.torrent.publishing.core.model.data.TxLogStatus;
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
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxLog;
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
import io.taucoin.torrent.publishing.core.utils.LinkUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.torrent.publishing.ui.TauNotifier;
import io.taucoin.torrent.publishing.ui.chat.ChatViewModel;
import io.taucoin.torrent.publishing.ui.transaction.TxUtils;

/**
 * TauListener????????????
 */
public class TauListenHandler {
    private static final Logger logger = LoggerFactory.getLogger("TauListenHandler");
    private final UserRepository userRepo;
    private final MemberRepository memberRepo;
    private final TxRepository txRepo;
    private final TxQueueRepository txQueueRepo;
    private final CommunityRepository communityRepo;
    private final BlockRepository blockRepository;
    private final SettingsRepository settingsRepo;
    private final TauDaemon daemon;
    private Disposable autoRenewalDisposable;
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
        blockRepository = RepositoryHelper.getBlockRepository(appContext);
        settingsRepo = RepositoryHelper.getSettingsRepository(appContext);
    }

    /**
     * ?????????????????????
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
//                // ????????????????????????
//                // 1???miner????????????balance??????
//                // 2???sender????????????nonce??????
//                // 3???receiver???????????? balance??????
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
     * libTAU?????????head block
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

        handleBlockData(block, BlockStatus.NEW_BLOCK);

        updateTxQueue(block);
        // ?????????????????????????????????????????????????????????????????????
        if (block.getBlockNumber() % Constants.AUTO_RENEWAL_PERIOD_BLOCKS == 0) {
            logger.info("accountAutoRenewal chainID::{}, block number::{}",
                    ChainIDUtil.decode(block.getChainID()), block.getBlockNumber());
            accountAutoRenewal();
        }
    }

    /**
     * ??????????????????????????????
     * @param block Block
     */
    void handleRollbackBlock(Block block) {
        logger.info("handleRollBack");
        handleBlockData(block, BlockStatus.ROLL_BACK);
    }

    /**
     * ?????????????????????????????????
     * @param block Block
     */
    void handleSyncingBlock(Block block) {
        logger.info("handleSyncingBlock");
        handleBlockData(block, BlockStatus.SYNCING);
    }

    /**
     * ?????????????????????????????????
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
     * ??????Block???????????????Block????????????????????????????????????????????????????????????
     * 1?????????????????????
     * 2???????????????????????????
     * 3?????????????????????????????????????????????????????????
     * @param block ????????????
     * @param status ??????
     */
    protected void handleBlockData(Block block, BlockStatus status) {
        String chainID = ChainIDUtil.decode(block.getChainID());
        logger.info("handleBlockData:: chainID::{}???blockNum::{}, blockHash::{}", chainID,
                block.getBlockNumber(), block.Hash());
        // ??????????????????
        saveBlockInfo(block, status);
        // ?????????????????????
        saveUserInfo(block.getMiner());
        // ???????????????????????????
        addMemberInfo(block.getChainID(), block.getMiner());
        // ??????????????????
        handleTransactionData(block, status);
    }

    /**
     * ????????????????????????UI???????????????
     * @param block ????????????
     * @param blockStatus ??????
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
            long rewards = null == transaction ? 0L : transaction.getFee();
            // ???????????????
            int status = blockStatus == BlockStatus.ON_CHAIN || blockStatus == BlockStatus.NEW_BLOCK ? 1 : 0;
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
            // ?????????????????????????????????
            // ????????????????????????????????????????????????, ?????????????????????
            int status;
            if (blockInfo.status == 1 && blockStatus == BlockStatus.SYNCING) {
                status = 1;
            } else {
                status = blockStatus == BlockStatus.ON_CHAIN || blockStatus == BlockStatus.NEW_BLOCK ? 1 : 0;
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
     * ??????Block???????????????Block???????????????????????????????????????????????????
     * @param block ??????
     * @param blockStatus ??????
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
            // ??????????????????
            handleUserInfo(txMsg);
            // ????????????????????????
            handleMemberInfo(txMsg);

            // ?????????????????????, ?????????????????????
            if (tx != null) {
                // ?????????????????????????????????
                // ????????????????????????????????????????????????, ?????????????????????
                int status;
                if (tx.txStatus == 1 && blockStatus == BlockStatus.SYNCING) {
                    status = 1;
                } else {
                    status = blockStatus == BlockStatus.ON_CHAIN || blockStatus == BlockStatus.NEW_BLOCK ? 1 : 0;
                }
                //  ??????????????????
                if (status != tx.txStatus && tx.queueID >= 0) {
                    TxQueue txQueue = txQueueRepo.getTxQueueByID(tx.queueID);
                    if (txQueue != null) {
                        if (status == 1) {
                            ChatViewModel.syncSendMessageTask(appContext, tx, txQueue.queueTime, QueueOperation.ON_CHAIN);
                        } else {
                            ChatViewModel.syncSendMessageTask(appContext, tx, txQueue.queueTime, QueueOperation.ROLL_BACK);
                        }
                    }
                }
                tx.txStatus = status;
                tx.blockNumber = block.getBlockNumber();
                tx.blockHash = block.Hash();
                txRepo.updateTransaction(tx);
            } else {
                handleTransactionData(block.getBlockNumber(), block.Hash(), txMsg,
                        blockStatus);
            }
        }
    }

    /**
     * ????????????????????????
     * @param blockNumber ?????????
     * @param blockHash ????????????
     * @param txMsg ??????
     * @param status ????????????
     */
    private void handleTransactionData(long blockNumber, String blockHash, Transaction txMsg, BlockStatus status) {
        if (isTransactionEmpty(txMsg)) {
            logger.info("handleTransactionData transaction empty");
            return;
        }
        boolean onChain = status == BlockStatus.ON_CHAIN || status == BlockStatus.NEW_BLOCK;
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
            // ??????Trust??????
            tx.receiverPk = trustContent.getTrustedPkStr();
        } else if (tx.txType == TxType.SELL_TX.getType()) {
            SellTxContent sellTxContent = new SellTxContent(txMsg.getPayload());
            // ??????Sell??????
            tx.coinName = sellTxContent.getCoinName();
            tx.quantity = sellTxContent.getQuantity();
            tx.link = sellTxContent.getLink();
            tx.location = sellTxContent.getLocation();
        } else if (tx.txType == TxType.AIRDROP_TX.getType()) {
            AirdropTxContent sellTxContent = new AirdropTxContent(txMsg.getPayload());
            // ??????Airdrop??????
            tx.link = sellTxContent.getLink();
        } else if (tx.txType == TxType.ANNOUNCEMENT.getType()) {
            AnnouncementContent sellTxContent = new AnnouncementContent(txMsg.getPayload());
            // ?????????????????????????????????
            tx.coinName = sellTxContent.getTitle();
        }

        txRepo.addTransaction(tx);
        logger.info("Add transaction to local, txID::{}, txType::{}", txID, tx.txType);

        // ??????????????????
        if (tx.txType != TxType.WIRING_TX.getType() && (status == BlockStatus.NEW_TX ||
                status == BlockStatus.NEW_BLOCK)) {
            String userPk = MainApplication.getInstance().getPublicKey();
            // ????????????????????????
            if (StringUtil.isNotEmpty(userPk) && StringUtil.isNotEquals(tx.senderPk, userPk)) {
                Member member = memberRepo.getMemberByChainIDAndPk(chainID, userPk);
                if (member != null && member.msgUnread == 0) {
                    member.msgUnread = 1;
                    memberRepo.updateMember(member);
                }
                // ???????????????????????????
                SpannableStringBuilder msg = TxUtils.createTxSpan(tx, TxType.NOTE_TX.getType());
                TauNotifier.getInstance().makeCommunityNotify(tx.chainID, msg);
            }
        }
    }

    /**
     * ????????????????????????
     * @param txMsg ??????
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
     * ????????????????????????
     * @param txMsg ??????
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
     * ???????????????????????????
     * @param publicKey ??????
     */
    private void saveUserInfo(byte[] publicKey) {
        if (null == publicKey) {
            return;
        }
        saveUserInfo(ByteUtil.toHexString(publicKey));
    }

    /**
     * ???????????????????????????
     * @param publicKey ??????
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
     * ???????????????????????????
     * @param publicKey ??????
     * @param chainID chainID
     */
    private void addMemberInfo(byte[] chainID, byte[] publicKey) {
        addMemberInfo(chainID, ByteUtil.toHexString(publicKey));
    }

    /**
     * ???????????????????????????
     * @param publicKey ??????
     * @param chainID chainID
     */
    private void addMemberInfo(byte[] chainID, String publicKey) {
        String chainIDStr = ChainIDUtil.decode(chainID);
        Member member = memberRepo.getMemberByChainIDAndPk(chainIDStr, publicKey);
        Account account = daemon.getAccountInfo(chainID, publicKey);
        long balance = 0;
        long nonce = 0;
        if (account != null) {
            balance = account.getBalance();
            nonce = account.getNonce();
        }

        if (null == member) {
            member = new Member(chainIDStr, publicKey, balance, nonce);
            memberRepo.addMember(member);
            logger.info("AddMemberInfo to local, chainID::{}, publicKey::{}, balance::{}",
                    chainIDStr, publicKey, balance);
        } else {
            // ????????????????????????onAccountState?????? TODO: ?????????
//            if (blockNumber > 0 || balance > 0) {
            if (balance > 0) {
                member.balance = balance;
            }
            member.nonce = nonce;
            memberRepo.updateMember(member);
            logger.info("Update Member's balance and power, chainID::{}, publicKey::{}, " +
                    "balance::{}", chainIDStr, publicKey, member.balance);
        }
    }

    /**
     * libTAU????????????Tail??????
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
     * libTAU???????????????????????????
     * @param block ???????????????
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
     * ??????libTAU?????????????????????????????????????????????????????????
     * @param txMsg ??????
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
            handleTransactionData(0, null, txMsg, BlockStatus.NEW_TX);
        }
    }

    /**
     * ??????libTAU???????????????
     * @param block ?????????
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
     * ??????????????????
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
                    long medianTxFree = Constants.WIRING_MIN_FEE.longValue();
                    long txFree = medianTxFree;
                    TxQueueAndStatus txQueue = txQueueRepo.getAccountRenewalTxQueue(member.chainID,
                            member.publicKey);
                    if (txQueue != null) {
                        float hours = DateUtil.timeDiffHours(txQueue.timestamp, DateUtil.getMillisTime());
                        // ?????????????????????
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
                    long amount = 0;
                    logger.info("accountAutoRenewal chainID::{}, publicKey::{} (balance::{}, " +
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
                        logger.info("accountAutoRenewal updateTxQueue");
                    } else {
                        txQueue.amount = amount;
                        txQueue.fee = txFree;
                        txQueueRepo.updateQueue(txQueue);
                        ChatViewModel.syncSendMessageTask(appContext, txQueue, QueueOperation.UPDATE);
                        daemon.updateTxQueue(txQueue.chainID);
                        logger.info("accountAutoRenewal resendTxQueue txFree::{}", txFree);
                    }
                }
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
    }

    /**
     * Sent to Internet ????????? (traversal complete > 1)
     * @param txHash ??????Hash
     */
    void onTxSent(byte[] txHash) {
        String hash = ByteUtil.toHexString(txHash);
        TxLog log = txRepo.getTxLog(hash, TxLogStatus.SENT_INTERNET.getStatus());
        logger.info("onTxSent txID::{}, exist::{}", hash, log != null);
        if (null == log) {
            log = new TxLog(hash, TxLogStatus.SENT_INTERNET.getStatus(), DateUtil.getMillisTime());
            txRepo.addTxLog(log);
        }
    }

    /**
     * Arrived Prefix Swarm ??????(??????????????????>1)
     * @param txHash ??????Hash
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
        if (autoRenewalDisposable != null && !autoRenewalDisposable.isDisposed()) {
            autoRenewalDisposable.dispose();
        }
        autoRenewalDisposable = null;
    }

    /**
     * ????????????
     * @param chainURL ???URL
     */
    public void addCommunity(String chainURL) {
        if (StringUtil.isEmpty(chainURL)) {
            return;
        }
        LinkUtil.Link url = LinkUtil.decode(chainURL);
        String chainID = url.getData();
        Set<String> peers = new HashSet<>();
        peers.add(url.getPeer());
        boolean isSuccess = daemon.followChain(chainID, peers);
        if (isSuccess) {
            Community community = new Community(chainID, ChainIDUtil.getName(chainID));
            communityRepo.addCommunity(community);

            String userPK = MainApplication.getInstance().getPublicKey();
            saveUserInfo(userPK);
            addMemberInfo(ChainIDUtil.encode(chainID), userPK);
            for (String peer : peers) {
                saveUserInfo(peer);
                addMemberInfo(ChainIDUtil.encode(chainID), peer);
            }
        }
    }

    /**
     * ??????????????????
     * @param chainIDBytes ???ID
     * @param accounts ????????????
     */
    public void onStateArray(byte[] chainIDBytes, List<Account> accounts) {
        String chainID = ChainIDUtil.decode(chainIDBytes);
        int accountSize = null == accounts ? 0 : accounts.size();
        logger.info("onStateArray chainID::{}, accounts::{}", chainID, accountSize);
        if (accountSize > 0) {
            for (Account account : accounts) {
                String peer = ByteUtil.toHexString(account.getPeer());
                Member member = memberRepo.getMemberByChainIDAndPk(chainID, peer);
                if (member != null) {
                    if (member.balance == 0 && member.nonce == 0) {
                        member.balance = account.getBalance();
                        member.nonce = account.getNonce();
                        memberRepo.updateMember(member);
                    }
                } else {
                    saveUserInfo(peer);
                    member = new Member(chainID, peer);
                    member.balance = account.getBalance();
                    member.nonce = account.getNonce();
                    memberRepo.addMember(member);
                }
            }
        }
    }
}
