package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.libTAU4j.Account;
import org.libTAU4j.Ed25519;
import org.libTAU4j.Pair;
import org.libTAU4j.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.model.data.message.TxContent;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxQueueRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;

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
    private Disposable queueDisposable;

    TxQueueManager(TauDaemon daemon) {
        this.daemon = daemon;
        Context context = MainApplication.getInstance();
        txQueueRepos = RepositoryHelper.getTxQueueRepository(context);
        userRepos = RepositoryHelper.getUserRepository(context);
        txRepo = RepositoryHelper.getTxRepository(context);
        userRepo = RepositoryHelper.getUserRepository(context);
        memberRepo = RepositoryHelper.getMemberRepository(context);
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
                        updateTxQueue(chainID);
                    }
                }
            }
            while (!emitter.isDisposed()) {
                try {
                    String chainID = chainIDQueue.take();
                    logger.debug("QueueConsumer size::{}, chainID::{}", chainIDQueue.size(), chainID);
                    boolean isResend = sendWiringTx(chainID, -1);
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
        }
    }

    /**
     * 发送转账交易
     * @param chainID 发送社区链ID
     * @return 是否需要重发
     */
    private boolean sendWiringTx(String chainID, long editedID) {
        User currentUser = userRepos.getCurrentUser();
        if (null == currentUser) {
            logger.debug("sendWiringTx current user null");
            return true;
        }
        TxQueueAndStatus txQueue = txQueueRepos.getQueueFirstTx(chainID, currentUser.publicKey);
        if (null == txQueue) {
            logger.debug("sendWiringTx queue null");
            return false;
        }
        try {
            logger.debug("sendWiringTx status::{}, sendCount::{}, timestamp::{}", txQueue.status,
                    txQueue.sendCount, txQueue.timestamp);
            if (txQueue.status <= 0 && txQueue.queueID != editedID) {
                logger.debug("sendWiringTx status::{}, waiting...", txQueue.status);
                return false;
            }
            // 获取当前用户在社区中链上nonce值
            byte[] chainIDBytes = ChainIDUtil.encode(chainID);
            Account account = daemon.getAccountInfo(chainIDBytes, currentUser.publicKey);
            if (null == account) {
                return true;
            }
            long fee = txQueue.fee;
            if (txQueue.amount + fee > account.getBalance()) {
                logger.debug("sendWiringTx amount({}) + fee({}) > balance({})", txQueue.amount,
                        fee, account.getBalance());
                return false;
            }
            byte[] senderSeed = ByteUtil.toByte(currentUser.seed);
            Pair<byte[], byte[]> keypair = Ed25519.createKeypair(senderSeed);
            byte[] senderPk = keypair.first;
            byte[] secretKey = keypair.second;
            byte[] receiverPk = ByteUtil.toByte(txQueue.receiverPk);
            long timestamp = daemon.getSessionTime();
            byte[] memo = Utils.textStringToBytes(txQueue.memo);
            TxContent txContent = new TxContent(WIRING_TX.getType(), memo);
            byte[] txEncoded = txContent.getEncoded();
            long nonce = account.getNonce() + 1;
            Transaction transaction = new Transaction(chainIDBytes, 0, timestamp, senderPk, receiverPk,
                    nonce, txQueue.amount, fee, txEncoded);
            transaction.sign(ByteUtil.toHexString(senderPk), ByteUtil.toHexString(secretKey));
            boolean isSubmitSuccess = daemon.submitTransaction(transaction);
            if (!isSubmitSuccess) {
                return true;
            }
            Tx tx = new Tx(chainID, txQueue.receiverPk, txQueue.amount, fee,
                    WIRING_TX.getType(), txQueue.memo);
            // 保存交易数据到本地数据库
            tx.txID = transaction.getTxID().to_hex();
            tx.timestamp = timestamp;
            tx.senderPk = txQueue.senderPk;
            tx.nonce = nonce;
            tx.queueID = txQueue.queueID;
            txRepo.addTransaction(tx);
            logger.debug("createTransaction chainID::{}, txID::{}, senderPk::{}, receiverPk::{}, nonce::{}, memo::{}",
                    tx.chainID, tx.txID, tx.senderPk, tx.receiverPk, tx.nonce, tx.memo);
            addUserInfoToLocal(tx);
            addMemberInfoToLocal(tx);
        } catch (Exception e) {
            logger.debug("Error adding transaction::{}", e.getMessage());
        }
        return false;
    }

    /**
     * 修改交易队列后重发交易
     * @param tx
     */
    void resendTxQueue(TxQueue tx) {
        sendWiringTx(tx.chainID, tx.queueID);
    }

    /**
     * 如果是Wiring交易,添加用户信息到本地
     * @param tx 交易
     */
    private void addUserInfoToLocal(Tx tx) {
        if (tx.txType == WIRING_TX.getType()) {
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
        if (txType == WIRING_TX.getType() && StringUtil.isNotEquals(tx.senderPk, tx.receiverPk)) {
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
