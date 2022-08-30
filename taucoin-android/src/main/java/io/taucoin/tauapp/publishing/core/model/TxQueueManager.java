package io.taucoin.tauapp.publishing.core.model;

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
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.core.Constants;
import io.taucoin.tauapp.publishing.core.model.data.TxLogStatus;
import io.taucoin.tauapp.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.tauapp.publishing.core.model.data.message.AirdropTxContent;
import io.taucoin.tauapp.publishing.core.model.data.message.AnnouncementContent;
import io.taucoin.tauapp.publishing.core.model.data.message.QueueOperation;
import io.taucoin.tauapp.publishing.core.model.data.message.SellTxContent;
import io.taucoin.tauapp.publishing.core.model.data.message.TrustContent;
import io.taucoin.tauapp.publishing.core.model.data.message.TxContent;
import io.taucoin.tauapp.publishing.core.model.data.message.TxType;
import io.taucoin.tauapp.publishing.core.storage.RepositoryHelper;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.TxLog;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.User;
import io.taucoin.tauapp.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.tauapp.publishing.core.storage.sqlite.repo.TxQueueRepository;
import io.taucoin.tauapp.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.tauapp.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.tauapp.publishing.ui.chat.ChatViewModel;
import io.taucoin.tauapp.publishing.ui.transaction.TxViewModel;

import static io.taucoin.tauapp.publishing.core.model.data.message.TxType.AIRDROP_TX;
import static io.taucoin.tauapp.publishing.core.model.data.message.TxType.ANNOUNCEMENT;
import static io.taucoin.tauapp.publishing.core.model.data.message.TxType.SELL_TX;
import static io.taucoin.tauapp.publishing.core.model.data.message.TxType.TRUST_TX;
import static io.taucoin.tauapp.publishing.core.model.data.message.TxType.WIRING_TX;

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
    private ConcurrentHashMap<String, Boolean> chainResendTx = new ConcurrentHashMap<>();
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
                logger.info("init queue size::{}", null == list ? 0 : list.size());
                if (list != null && list.size() > 0) {
                    for (String chainID : list) {
                        updateTxQueue(chainID, true);
                    }
                }
            }
            while (!emitter.isDisposed()) {
                try {
                    String chainID = chainIDQueue.take();
                    logger.info("QueueConsumer size::{}, chainID::{}", chainIDQueue.size(), chainID);
                    boolean isResend = sendTxQueue(chainID, 0);
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
            logger.info("updateTxQueue chainID::{}", chainID);
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
    private boolean sendTxQueue(String chainID, int offset) {
        User currentUser = userRepos.getCurrentUser();
        if (null == currentUser) {
            logger.info("sendTxQueue current user null");
            return true;
        }
        try {
            TxQueueAndStatus txQueue = txQueueRepos.getQueueFirstTx(chainID, currentUser.publicKey, offset);
            if (null == txQueue) {
                logger.info("sendTxQueue queue null");
                return false;
            }
            logger.info("sendTxQueue status::{}, sendCount::{}, timestamp::{}, offset::{}", txQueue.status,
                    txQueue.sendCount, txQueue.timestamp, 0);
            // 获取当前用户在社区中链上nonce值
            byte[] chainIDBytes = ChainIDUtil.encode(chainID);
            Account account = daemon.getAccountInfo(chainIDBytes, currentUser.publicKey);
            if (null == account) {
                return true;
            }
            logger.info("sendTxQueue account nonce::{}, balance::{}", account.getNonce(), account.getBalance());

            if (txQueue.amount + txQueue.fee > account.getBalance()) {
                logger.info("sendWiringTx amount({}) + fee({}) > balance({})", txQueue.amount,
                        txQueue.fee, account.getBalance());
                return sendTxQueue(chainID, offset + 1);
            }
            // 交易已创建
            if (txQueue.status == 0) {
                logger.info("sendTxQueue account nonce::{}, queue nonce::{}", account.getNonce(),
                        txQueue.nonce);
                // 判断是否nonce冲突, 如果冲突重建按最新nonce重新创建交易
                if (account.getNonce() >= txQueue.nonce || queueChanged(account, txQueue)) {
                    return sendTxQueue(account, txQueue);
                } else {
                    resendTxQueue(account, txQueue);
                    return false;
                }
            }
            return sendTxQueue(account, txQueue);
        } catch (Exception e) {
            logger.warn("Error adding transaction::{}", e.getMessage());
        }
        return false;
    }

    /**
     * 队列数据已改变
     * @param account
     * @param txQueue
     * @return
     */
    private boolean queueChanged(Account account, TxQueueAndStatus txQueue) {
        boolean changed = false;
        Tx tx = txRepo.getTxByQueueID(txQueue.queueID, txQueue.timestamp);
        if (tx != null) {
            Transaction transaction = createTransaction(account, txQueue, txQueue.timestamp);
            String newTxID = transaction.getTxID().to_hex();
            logger.debug("sendTxQueue txID::{}, newTxID::{}", tx.txID, newTxID);
            changed = StringUtil.isNotEquals(tx.txID, newTxID);
        }
        logger.info("sendTxQueue queueChanged::{}", changed);
        return changed;
    }

    /**
     * 重新发送等待上链的交易（防止由于libTAU丢弃交易而上不了链）
     */
    private void resendTxQueue(Account account, TxQueueAndStatus txQueue) {
        String chainID = txQueue.chainID;
        if (chainResendTx.containsKey(chainID)) {
            Boolean isResend = chainResendTx.get(chainID);
            if (isResend != null && !isResend && txQueue.sendStatus != 1) {
                logger.info("resendTransaction:: No need to resend");
                return;
            }
        }
        Tx tx = txRepo.getTxByQueueID(txQueue.queueID, txQueue.timestamp);
        if ( null == tx) {
            logger.info("resendTransaction:: tx not found");
            return;
        }

        if (tx.amount + tx.fee > account.getBalance()) {
            logger.info("resendTransaction amount({}) + fee({}) > balance({})", txQueue.amount,
                    txQueue.fee, account.getBalance());
            return;
        }
        String result = TxViewModel.createTransaction(appContext, tx, true);
        logger.debug("resendTransaction chainID::{}, txID::{}, result::{}",
                tx.chainID, tx.txID, result);

        if (StringUtil.isEmpty(result)) {
            chainResendTx.put(chainID, false);
            // 已发送
            tx.sendStatus = 0;
            txRepo.updateTransaction(tx);
        }
    }

    private boolean sendTxQueue(Account account, TxQueueAndStatus txQueue) {
        boolean isSendMessage = false;
        if (txQueue.queueType == 1) {
            long medianFee = Constants.WIRING_MIN_FEE.longValue();
            if (txQueue.fee != medianFee) {
                txQueue.fee = medianFee;
                // 更新airdrop的交易费
                txQueueRepos.updateQueue(txQueue);
                isSendMessage = true;
            }
        }
        if (txQueue.amount + txQueue.fee > account.getBalance()) {
            logger.debug("sendWiringTx amount({}) + fee({}) > balance({})", txQueue.amount,
                    txQueue.fee, account.getBalance());
            return false;
        }
        if (isSendMessage) {
            ChatViewModel.syncSendMessageTask(appContext, txQueue, QueueOperation.UPDATE);
        }
        sendTxQueue(account, txQueue, true, 0);
        return false;
    }

    /**
     * 发送交易队列
     * @param txQueue 队列信息
     * @return 是否需要重发
     */
    boolean sendTxQueue(TxQueue txQueue, long pinnedTime) {
        byte[] chainID = ChainIDUtil.encode(txQueue.chainID);
        Account account = daemon.getAccountInfo(chainID, txQueue.senderPk);
        TxQueueAndStatus tx = txQueueRepos.getTxQueueByID(txQueue.queueID);
        logger.info("sendTxQueue chainID::{}, queueID::{}, status::{}, sendCount::{}",
                txQueue.chainID, txQueue.queueID, tx.status, tx.sendCount);
        if (tx.status > 0) {
            // 如果上链成功直接返回
            return false;
        } else if (tx.status == 0 && tx.sendCount == 1 && tx.sendStatus != 1) {
            // 如果只创建了一条, 并且不是未发送状态直接返回
            return false;
        }
        return sendTxQueue(account, txQueue, false, pinnedTime);
    }

    /**
     * 构建交易
     * @param account 账户信息
     * @param txQueue 队列信息
     * @return Transaction
     */
    private Transaction createTransaction(Account account, TxQueue txQueue, long timestamp) {
        User user = userRepos.getUserByPublicKey(txQueue.senderPk);
        byte[] senderSeed = ByteUtil.toByte(user.seed);
        Pair<byte[], byte[]> keypair = Ed25519.createKeypair(senderSeed);
        byte[] senderPk = keypair.first;
        byte[] secretKey = keypair.second;
        byte[] receiverPk = ByteUtil.toByte(txQueue.receiverPk);
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
                nonce, txQueue.amount, txQueue.fee, txEncoded);
        transaction.sign(ByteUtil.toHexString(senderPk), ByteUtil.toHexString(secretKey));
        return transaction;
    }

    /**
     * 发送交易队列
     * @param account 账户信息
     * @param txQueue 队列信息
     * @return 是否需要重发
     */
    private boolean sendTxQueue(Account account, TxQueue txQueue, boolean isDirectSend, long pinnedTime) {
        long timestamp = daemon.getSessionTime();
        Transaction transaction = createTransaction(account, txQueue, timestamp);
        long nonce = transaction.getNonce();
        byte[] txEncoded = transaction.getPayload();
        if (isDirectSend) {
            boolean isSubmitSuccess = daemon.submitTransaction(transaction);
            if (!isSubmitSuccess) {
                return true;
            }
        }
        // 更新未发送的交易是否置顶
        if (pinnedTime == 0) {
            Tx tx = txRepo.queryUnsentTx(txQueue.queueID);
            if (tx != null) {
                pinnedTime = tx.pinnedTime;
            }
        }
        // 删除未发送的交易
        txRepo.deleteUnsentTx(txQueue.queueID);
        logger.info("sendTxQueue delete unsent tx");

        // 创建新的交易
        TxContent txContent = new TxContent(txEncoded);
        int txType = txContent.getType();
        Tx tx = null;
        if (txType == WIRING_TX.getType()) {
            tx = new Tx(txQueue.chainID, txQueue.receiverPk, txQueue.amount, txQueue.fee,
                    WIRING_TX.getType(), txContent.getMemo());
        } else if (txType == SELL_TX.getType()) {
            SellTxContent content = new SellTxContent(txEncoded);
            tx = new Tx(txQueue.chainID, txQueue.receiverPk, txQueue.fee, SELL_TX.getType(),
                    content.getCoinName(), content.getQuantity(), content.getLink(),
                    content.getLocation(), content.getMemo());
        } else if (txType == AIRDROP_TX.getType()) {
            AirdropTxContent content = new AirdropTxContent(txEncoded);
            tx = new Tx(txQueue.chainID, txQueue.receiverPk, txQueue.fee, AIRDROP_TX.getType(), content.getMemo());
            tx.link = content.getLink();
        } else if (txType == ANNOUNCEMENT.getType()) {
            AnnouncementContent content = new AnnouncementContent(txEncoded);
            tx = new Tx(txQueue.chainID, txQueue.receiverPk, txQueue.fee, ANNOUNCEMENT.getType(), content.getMemo());
            tx.coinName = content.getTitle();
        } else if (txType == TRUST_TX.getType()) {
            TrustContent content = new TrustContent(txEncoded);
            tx = new Tx(txQueue.chainID, content.getTrustedPkStr(), txQueue.fee, TRUST_TX.getType(), content.getMemo());
        }
        if (tx != null) {
            // 保存交易数据到本地数据库
            tx.txID = transaction.getTxID().to_hex();
            tx.timestamp = timestamp;
            tx.senderPk = txQueue.senderPk;
            tx.nonce = nonce;
            tx.queueID = txQueue.queueID;
            tx.sendStatus = isDirectSend ? 0 : 1;
            if (pinnedTime > 0) {
                tx.pinnedTime = pinnedTime;
            }
            txRepo.addTransaction(tx);
            logger.info("sendWiringTx createTransaction chainID::{}, txID::{}, senderPk::{}, " +
                            "receiverPk::{}, nonce::{}, memo::{}", tx.chainID, tx.txID, tx.senderPk, tx.receiverPk, tx.nonce, tx.memo);
            addUserInfoToLocal(tx);
            addMemberInfoToLocal(tx);
            if (isDirectSend) {
                TxLog log = new TxLog(tx.txID, TxLogStatus.SENT.getStatus(), DateUtil.getMillisTime());
                txRepo.addTxLog(log);
            }
        }
        return false;
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
