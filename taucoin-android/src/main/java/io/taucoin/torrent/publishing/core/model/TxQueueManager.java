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
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.model.data.message.TxContent;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxQueueRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
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
    private SettingsRepository settingsRepo;
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
        settingsRepo = RepositoryHelper.getSettingsRepository(context);
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
                    boolean isResend = sendWiringTx(chainID, 0);
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
     * 发送转账交易
     * @param chainID 发送社区链ID
     * @param offset 查询数据偏移量
     * @return 是否需要重发
     */
    private boolean sendWiringTx(String chainID, int offset) {
        User currentUser = userRepos.getCurrentUser();
        if (null == currentUser) {
            logger.debug("sendWiringTx current user null");
            return true;
        }
        try {
            TxQueueAndStatus txQueue = txQueueRepos.getQueueFirstTx(chainID, currentUser.publicKey, offset);
            if (null == txQueue) {
                logger.debug("sendWiringTx queue null");
                return false;
            }
            logger.debug("sendWiringTx status::{}, sendCount::{}, timestamp::{}, offset::{}", txQueue.status,
                    txQueue.sendCount, txQueue.timestamp, offset);
            // 获取当前用户在社区中链上nonce值
            byte[] chainIDBytes = ChainIDUtil.encode(chainID);
            Account account = daemon.getAccountInfo(chainIDBytes, currentUser.publicKey);
            if (null == account) {
                return true;
            }
            logger.debug("sendWiringTx account nonce::{}, balance::{}, blockNumber::{}", account.getNonce(),
                    account.getBalance(), account.getBlockNumber());
            // 交易已创建
            if (txQueue.status == 0) {
                logger.debug("sendWiringTx account nonce::{}, queue nonce::{}", account.getNonce(),
                        txQueue.nonce);
                // 判断是否nonce冲突, 直接跳过执行队列下一条
                if (account.getNonce() >= txQueue.nonce) {
                    return sendWiringTx(chainID, offset + 1);
                } else {
                    return false;
                }
            }
            return sendWiringTx(account, txQueue);
        } catch (Exception e) {
            logger.debug("Error adding transaction::{}", e.getMessage());
        }
        return false;
    }

    private boolean sendWiringTx(Account account, TxQueueAndStatus txQueue) {
        long fee = txQueue.fee;
        if (txQueue.queueType == 1) {
            fee = getMedianTxFree(txQueue.chainID);
            if (fee < Constants.COIN.longValue()) {
                fee = Constants.COIN.longValue();
            }
            txQueue.fee = fee;
            // 更新airdrop的交易费
            txQueueRepos.updateQueue(txQueue);
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
        byte[] memo = Utils.textStringToBytes(txQueue.memo);
        TxContent txContent = new TxContent(WIRING_TX.getType(), memo);
        byte[] txEncoded = txContent.getEncoded();
        long nonce = account.getNonce() + 1;
        byte[] chainIDBytes = ChainIDUtil.encode(txQueue.chainID);
        Transaction transaction = new Transaction(chainIDBytes, 0, timestamp, senderPk, receiverPk,
                nonce, txQueue.amount, fee, txEncoded);
        transaction.sign(ByteUtil.toHexString(senderPk), ByteUtil.toHexString(secretKey));
        boolean isSubmitSuccess = daemon.submitTransaction(transaction);
        if (!isSubmitSuccess) {
            return true;
        }
        Tx tx = new Tx(txQueue.chainID, txQueue.receiverPk, txQueue.amount, fee,
                WIRING_TX.getType(), txQueue.memo);
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
        return false;
    }

    /**
     * 0、默认为最小交易费
     * 1、从交易池中获取前10名交易费的中位数
     * 2、如果交易池返回小于等于0，用上次交易用的交易费
     * @param chainID 交易所属的社区chainID
     */
    private long getMedianTxFree(String chainID) {
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

    /**
     * 修改交易队列后重发交易
     * @param tx
     */
    String resendTxQueue(TxQueue tx) {
        String result = "";
        String chainID = tx.chainID;
        long queueID = tx.queueID;
        TxQueueAndStatus txQueue = txQueueRepos.getTxQueueByID(queueID);
        if (txQueue != null && txQueue.status <= 0 && tx.queueID > 0) {
            // 队列等待中，还未创建交易，直接更新
            if (txQueue.status < 0) {
                txQueueRepos.updateQueue(tx);
                return result;
            }
            // 获取当前用户在社区中链上nonce值
            byte[] chainIDBytes = ChainIDUtil.encode(chainID);
            Account account = daemon.getAccountInfo(chainIDBytes, tx.senderPk);
            Context context = MainApplication.getInstance();
            if (null == account) {
                result = context.getResources().getString(R.string.tx_resend_failed);
                return result;
            }
            logger.debug("resendTxQueue account nonce::{}, balance::{}, blockNumber::{}", account.getNonce(),
                    account.getBalance(), account.getBlockNumber());
            // 已创建交易，重发
            if (account.getNonce() >= txQueue.nonce) {
                // nonce冲突，重新入队列
                txQueueRepos.deleteQueue(tx);
                tx.queueID = 0;
                txQueueRepos.addQueue(tx);
                // 更新交易队列
                updateTxQueue(chainID);
            } else {
                // 直接更新，然后重新创建交易
                boolean isResend = sendWiringTx(account, txQueue);
                if (isResend) {
                    result = context.getResources().getString(R.string.tx_resend_failed);
                }
            }
        }
        return result;
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
