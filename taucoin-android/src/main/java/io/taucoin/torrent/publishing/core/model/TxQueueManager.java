package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.libTAU4j.Account;
import org.libTAU4j.Ed25519;
import org.libTAU4j.Pair;
import org.libTAU4j.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.model.data.message.AirdropTxContent;
import io.taucoin.torrent.publishing.core.model.data.message.AnnouncementContent;
import io.taucoin.torrent.publishing.core.model.data.message.QueueOperation;
import io.taucoin.torrent.publishing.core.model.data.message.SellTxContent;
import io.taucoin.torrent.publishing.core.model.data.message.TrustContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxQueueRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.torrent.publishing.ui.chat.ChatViewModel;
import io.taucoin.torrent.publishing.ui.transaction.TxViewModel;

import static io.taucoin.torrent.publishing.core.model.data.message.TxType.AIRDROP_TX;
import static io.taucoin.torrent.publishing.core.model.data.message.TxType.ANNOUNCEMENT;
import static io.taucoin.torrent.publishing.core.model.data.message.TxType.SELL_TX;
import static io.taucoin.torrent.publishing.core.model.data.message.TxType.TRUST_TX;
import static io.taucoin.torrent.publishing.core.model.data.message.TxType.WIRING_TX;

/**
 * 交易队列管理
 */
class TxQueueManager {
    private static final Logger logger = LoggerFactory.getLogger("TxQueueManager");
    private TauDaemon daemon;
    private TxQueueRepository txQueueRepos;
    private UserRepository userRepos;
    private TxRepository txRepo;
    private UserRepository userRepo;
    private MemberRepository memberRepo;
    private LinkedBlockingQueue<String> chainIDQueue = new LinkedBlockingQueue<>();
    private ConcurrentHashMap<String, Boolean> chainResendTx = new ConcurrentHashMap<String, Boolean>();
    private Disposable queueDisposable;
    private Context appContext;

    TxQueueManager(TauDaemon daemon) {
        this.daemon = daemon;
        appContext = MainApplication.getInstance();
        txQueueRepos = RepositoryHelper.getTxQueueRepository(appContext);
        userRepos = RepositoryHelper.getUserRepository(appContext);
        txRepo = RepositoryHelper.getTxRepository(appContext);
        userRepo = RepositoryHelper.getUserRepository(appContext);
        memberRepo = RepositoryHelper.getMemberRepository(appContext);
        createQueueConsumer();
    }

    private void createQueueConsumer() {
        queueDisposable = Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            Thread.currentThread().setName("TxQueueManager");
            // 初始化需要转账的社区
            User currentUser = userRepos.getCurrentUser();
            if (currentUser != null) {
                List<String> list = txQueueRepos.getNeedWiringTxCommunities(currentUser.publicKey);
                logger.debug("init queue size::{}", null == list ? 0 : list.size());
                if (list != null && list.size() > 0) {
                    for (String chainID : list) {
                        updateTxQueue(chainID, true);
                    }
                }
            }
            while (!emitter.isDisposed()) {
                try {
                    String chainID = chainIDQueue.take();
                    logger.debug("QueueConsumer size::{}, chainID::{}", chainIDQueue.size(), chainID);
                    boolean isResend = sendTxQueue(chainID);
                    if (isResend) {
                        updateTxQueue(chainID);
                        Thread.sleep(Interval.INTERVAL_RETRY.getInterval());
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    logger.error("QueueConsumer error::", e);
                }
            }
        }).subscribeOn(Schedulers.io())
            .subscribe();
    }

    void updateTxQueue(String chainID) {
        if (!chainIDQueue.contains(chainID)) {
            chainIDQueue.offer(chainID);
            logger.debug("updateTxQueue chainID::{}", chainID);
        }
    }

    /**
     * 更新转账交易队列
     * @param chainID 链ID
     * @param isResendTx 是否重新发送等待上链的交易（防止由于libTAU丢弃交易而上不了链）
     */
    void updateTxQueue(String chainID, boolean isResendTx) {
        if (isResendTx) {
            chainResendTx.put(chainID, true);
        }
        updateTxQueue(chainID);
    }

    /**
     * 发送转账交易
     * @param chainID 发送社区链ID
     * @return 是否需要重发
     */
    private boolean sendTxQueue(String chainID) {
        User currentUser = userRepos.getCurrentUser();
        if (null == currentUser) {
            logger.debug("sendTxQueue current user null");
            return true;
        }
        try {
            TxQueueAndStatus txQueue = txQueueRepos.getQueueFirstTx(chainID, currentUser.publicKey, 0);
            if (null == txQueue) {
                logger.debug("sendTxQueue queue null");
                return false;
            }
            logger.debug("sendTxQueue status::{}, sendCount::{}, timestamp::{}, offset::{}", txQueue.status,
                    txQueue.sendCount, txQueue.timestamp, 0);
            // 获取当前用户在社区中链上nonce值
            byte[] chainIDBytes = ChainIDUtil.encode(chainID);
            Account account = daemon.getAccountInfo(chainIDBytes, currentUser.publicKey);
            if (null == account) {
                return true;
            }
            logger.debug("sendTxQueue account nonce::{}, balance::{}, blockNumber::{}", account.getNonce(),
                    account.getBalance(), account.getBlockNumber());
            // 交易已创建
            if (txQueue.status == 0) {
                logger.debug("sendTxQueue account nonce::{}, queue nonce::{}", account.getNonce(),
                        txQueue.nonce);
                // 判断是否nonce冲突, 如果冲突重建按最新nonce重新创建交易
                if (account.getNonce() >= txQueue.nonce) {
                    return sendTxQueue(account, txQueue);
                } else {
                    resendTxQueue(txQueue);
                    return false;
                }
            }
            return sendTxQueue(account, txQueue);
        } catch (Exception e) {
            logger.debug("Error adding transaction::{}", e.getMessage());
        }
        return false;
    }

    /**
     * 重新发送等待上链的交易（防止由于libTAU丢弃交易而上不了链）
     */
    private void resendTxQueue(TxQueueAndStatus txQueue) {
        String chainID = txQueue.chainID;
        if (chainResendTx.containsKey(chainID)) {
            Boolean isResend = chainResendTx.get(chainID);
            if (isResend != null && !isResend) {
                logger.debug("resendTransaction:: No need to resend");
                return;
            }
        }
        Tx tx = txRepo.getTxByQueueID(txQueue.queueID, txQueue.timestamp);
        if ( null == tx) {
            logger.debug("resendTransaction:: tx not found");
            return;
        }
        String result = TxViewModel.createTransaction(appContext, tx, true);
        logger.debug("resendTransaction chainID::{}, txID::{}, result::{}",
                tx.chainID, tx.txID, result);

        if (StringUtil.isNotEmpty(result)) {
            chainResendTx.put(chainID, false);
        }
    }

    private boolean sendTxQueue(Account account, TxQueueAndStatus txQueue) {
        long fee = txQueue.fee;
        if (txQueue.queueType == 1) {
            fee = getMedianTxFree(txQueue.chainID);
            txQueue.fee = fee;
            // 更新airdrop的交易费
            txQueueRepos.updateQueue(txQueue);
            ChatViewModel.syncSendMessageTask(appContext, txQueue, QueueOperation.UPDATE);
        }
        if (txQueue.amount + fee > account.getBalance()) {
            logger.debug("sendWiringTx amount({}) + fee({}) > balance({})", txQueue.amount,
                    fee, account.getBalance());
            return false;
        }
        User user = userRepos.getUserByPublicKey(txQueue.senderPk);
        byte[] senderSeed = ByteUtil.toByte(user.seed);
        Pair<byte[], byte[]> keypair = Ed25519.createKeypair(senderSeed);
        byte[] senderPk = keypair.first;
        byte[] secretKey = keypair.second;
        byte[] receiverPk = ByteUtil.toByte(txQueue.receiverPk);
        long timestamp = daemon.getSessionTime();
        byte[] txEncoded;
        if (txQueue.txType == TxType.WIRING_TX.getType() && null == txQueue.content) {
            TxContent txContent = new TxContent(TxType.WIRING_TX.getType(), txQueue.memo);
            txEncoded = txContent.getEncoded();
        } else {
            txEncoded = txQueue.content;
        }
        long nonce = account.getNonce() + 1;
        byte[] chainIDBytes = ChainIDUtil.encode(txQueue.chainID);
        Transaction transaction = new Transaction(chainIDBytes, 0, timestamp, senderPk, receiverPk,
                nonce, txQueue.amount, fee, txEncoded);
        transaction.sign(ByteUtil.toHexString(senderPk), ByteUtil.toHexString(secretKey));
        boolean isSubmitSuccess = daemon.submitTransaction(transaction);
        if (!isSubmitSuccess) {
            return true;
        }
        TxContent txContent = new TxContent(txEncoded);
        int txType = txContent.getType();
        Tx tx = null;
        if (txType == WIRING_TX.getType()) {
            tx = new Tx(txQueue.chainID, txQueue.receiverPk, txQueue.amount, fee,
                    WIRING_TX.getType(), txContent.getMemo());
        } else if (txType == SELL_TX.getType()) {
            SellTxContent content = new SellTxContent(txEncoded);
            tx = new Tx(txQueue.chainID, txQueue.receiverPk, fee, SELL_TX.getType(),
                    content.getCoinName(), content.getQuantity(), content.getLink(),
                    content.getLocation(), content.getMemo());
        } else if (txType == AIRDROP_TX.getType()) {
            AirdropTxContent content = new AirdropTxContent(txEncoded);
            tx = new Tx(txQueue.chainID, txQueue.receiverPk, fee, AIRDROP_TX.getType(), content.getMemo());
            tx.link = content.getLink();
        } else if (txType == ANNOUNCEMENT.getType()) {
            AnnouncementContent content = new AnnouncementContent(txEncoded);
            tx = new Tx(txQueue.chainID, txQueue.receiverPk, fee, ANNOUNCEMENT.getType(), content.getMemo());
            tx.coinName = content.getTitle();
        } else if (txType == TRUST_TX.getType()) {
            TrustContent content = new TrustContent(txEncoded);
            tx = new Tx(txQueue.chainID, content.getTrustedPkStr(), fee, TRUST_TX.getType(), content.getMemo());
        }
        if (tx != null) {
            // 保存交易数据到本地数据库
            tx.txID = transaction.getTxID().to_hex();
            tx.timestamp = timestamp;
            tx.senderPk = txQueue.senderPk;
            tx.nonce = nonce;
            tx.queueID = txQueue.queueID;
            txRepo.addTransaction(tx);
            logger.debug("sendWiringTx createTransaction chainID::{}, txID::{}, senderPk::{}, receiverPk::{}, nonce::{}, memo::{}",
                    tx.chainID, tx.txID, tx.senderPk, tx.receiverPk, tx.nonce, tx.memo);
            addUserInfoToLocal(tx);
            addMemberInfoToLocal(tx);
        }
        return false;
    }

    /**
     * 0、默认为最小交易费
     * 1、从交易池中获取前10名交易费的中位数
     * 2、如果交易池返回小于等于0，用上次交易用的交易费
     * @param chainID 交易所属的社区chainID
     */
    private long getMedianTxFree(String chainID) {
        long free = Constants.WIRING_MIN_FEE.longValue();
        long medianFree = daemon.getMedianTxFree(chainID);
        if (medianFree > free) {
            free = medianFree;
        }
        return free;
    }

    /**
     * 如果是Wiring交易,添加用户信息到本地
     * @param tx 交易
     */
    private void addUserInfoToLocal(Tx tx) {
        if (tx.txType == WIRING_TX.getType() || tx.txType == TRUST_TX.getType()) {
            User receiverUser = userRepo.getUserByPublicKey(tx.receiverPk);
            if (null == receiverUser) {
                receiverUser = new User(tx.receiverPk);
                userRepo.addUser(receiverUser);
                logger.info("addUserInfoToLocal, publicKey::{}", tx.receiverPk);
            }
        }
    }

    /**
     * 添加社区成员信息
     * @param tx 交易
     */
    private void addMemberInfoToLocal(Tx tx) {
        long txType = tx.txType;
        Member member = memberRepo.getMemberByChainIDAndPk(tx.chainID, tx.senderPk);
        if (null == member) {
            member = new Member(tx.chainID, tx.senderPk);
            memberRepo.addMember(member);
            logger.info("addMemberInfoToLocal, senderPk::{}", tx.senderPk);
        }
        if ((txType == WIRING_TX.getType() || txType == TRUST_TX.getType()) && StringUtil.isNotEquals(tx.senderPk, tx.receiverPk)) {
            Member receiverMember = memberRepo.getMemberByChainIDAndPk(tx.chainID, tx.receiverPk);
            if (null == receiverMember) {
                receiverMember = new Member(tx.chainID, tx.receiverPk);
                memberRepo.addMember(receiverMember);
                logger.info("addMemberInfoToLocal, receiverPk::{}", tx.receiverPk);
            }
        }
    }

    public void onCleared() {
        chainIDQueue.clear();
        if (queueDisposable != null && !queueDisposable.isDisposed()) {
            queueDisposable.dispose();
        }
    }
}
