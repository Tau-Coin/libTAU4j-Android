package io.taucoin.tauapp.publishing.core.model;

import android.content.Context;

import org.libTAU4j.Account;
import org.libTAU4j.Ed25519;
import org.libTAU4j.Pair;
import org.libTAU4j.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.core.Constants;
import io.taucoin.tauapp.publishing.core.model.data.TxFreeStatistics;
import io.taucoin.tauapp.publishing.core.model.data.TxLogStatus;
import io.taucoin.tauapp.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.tauapp.publishing.core.model.data.message.NewsContent;
import io.taucoin.tauapp.publishing.core.model.data.message.QueueOperation;
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
import io.taucoin.tauapp.publishing.core.utils.ObservableUtil;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.tauapp.publishing.ui.chat.ChatViewModel;
import io.taucoin.tauapp.publishing.ui.transaction.TxViewModel;

import static io.taucoin.tauapp.publishing.core.model.data.message.TxType.NEWS_TX;
import static io.taucoin.tauapp.publishing.core.model.data.message.TxType.WIRING_TX;

/**
 * 交易队列管理
 */
class TxQueueManager {
    private static final Logger logger = LoggerFactory.getLogger("TxQueueManager");
    private final TauDaemon daemon;
    private final TxQueueRepository txQueueRepos;
    private final UserRepository userRepos;
    private final TxRepository txRepo;
    private final UserRepository userRepo;
    private final MemberRepository memberRepo;
    private final LinkedBlockingQueue<String> chainIDQueue = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<String, Boolean> chainResendTx = new ConcurrentHashMap<>();
    // 链发生回滚后1分钟内不触发发交易, 只有大于这个时间交易继续
    private final ConcurrentHashMap<String, Long> chainTxStoppedTime = new ConcurrentHashMap<>();
    private Disposable queueDisposable;
    private Disposable txRecoveryDisposable;
    private final Context appContext;

    TxQueueManager(TauDaemon daemon) {
        this.daemon = daemon;
        appContext = MainApplication.getInstance();
        txQueueRepos = RepositoryHelper.getTxQueueRepository(appContext);
        userRepos = RepositoryHelper.getUserRepository(appContext);
        txRepo = RepositoryHelper.getTxRepository(appContext);
        userRepo = RepositoryHelper.getUserRepository(appContext);
        memberRepo = RepositoryHelper.getMemberRepository(appContext);
        createQueueConsumer();
        txRecoveryHandler();
    }

    /**
     * 交易恢复程序
     * 20s检查一次，防止时间交叉错过
     */
    private void txRecoveryHandler() {
        txRecoveryDisposable = ObservableUtil.interval(20 * 1000)
                .subscribeOn(Schedulers.io()).subscribe( l -> {
                    Set<String> keySet = chainTxStoppedTime.keySet();
                    long currentTime = DateUtil.getMillisTime();
                    for (String key : keySet) {
                        Long stoppedTime = chainTxStoppedTime.get(key);
                        logger.info("txRecoveryHandler chainID::{}, stoppedTime::{}, currentTime::{}",
                                key, stoppedTime != null ? stoppedTime : 0, currentTime);
                         if (stoppedTime != null && currentTime > stoppedTime) {
                             chainTxStoppedTime.remove(key);
                             updateTxQueue(key);
                         }
                    }
                });
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

    void updateChainTxStoppedTime(String chainID) {
        long currentTime = DateUtil.getMillisTime();
        currentTime += 60 * 1000;
        chainTxStoppedTime.put(chainID, currentTime);
        logger.info("updateTxQueue updateTxStoppedTime chainID::{}, time::{}", chainID, currentTime);
    }

    void updateTxQueue(String chainID) {
        // 链发生回滚，等待一分钟再发新交易, delete by tc
		/*
        if (chainTxStoppedTime.containsKey(chainID)) {
            chainIDQueue.remove(chainID);
            logger.info("updateTxQueue tx waiting... chainID::{}", chainID);
            return;
        }
		*/
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
    private boolean sendTxQueue(String chainID) {
		//获取当前用户
        User currentUser = userRepos.getCurrentUser();
        if (null == currentUser) {
            logger.info("sendTxQueue current user null");
            return true;
        }
        // 获取当前用户在社区中链上nonce值
        byte[] chainIDBytes = ChainIDUtil.encode(chainID);
        Account account = daemon.getAccountInfo(chainIDBytes, currentUser.publicKey);
        if (null == account) {
            return true;
        }
        logger.info("sendTxQueue account nonce::{}, balance::{}", account.getNonce(), account.getBalance());
		long nonce = account.getNonce() + 1;
        try {
			//1. 先按照nonce搜索一遍, 没有则搜一个没有nonce的txQueue交易
            TxQueueAndStatus txQueue = txQueueRepos.getNonceFirstTx(chainID, currentUser.publicKey, nonce);
            if (null == txQueue) {
                logger.info("sendTxQueue queue null");
				return false;
            }
            logger.info("sendTxQueue id::{}, nonce::{}, timestamp::{}", txQueue.queueID, txQueue.nonce, txQueue.timestamp);

            return sendTxQueue(account, txQueue, 2);
        } catch (Exception e) {
            logger.warn("Error adding transaction::{}", e.getMessage());
        }
        return false;
    }

    private boolean sendTxQueue(Account account, TxQueueAndStatus txQueue, int mode) {
        boolean isSendMessage = false;
		//1. 自动airdrop tx; 2. 自动referal tx
        if (txQueue.queueType == 1 || txQueue.queueType == 2) {
            long medianFee = getAverageTxFee(txQueue.chainID, TxType.WIRING_TX);
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
        sendTxQueue(account, txQueue, true, 0, mode);
        return false;
    }

    private long getAverageTxFee(String chainID, TxType type) {
        long txFee;
        try {
            TxFreeStatistics statistics = txRepo.queryAverageTxsFee(chainID);
            if (type == WIRING_TX) {
                txFee = Constants.WIRING_MIN_FEE.longValue();
                if (statistics != null) {
                    float wiringRate = statistics.getWiringCount() * 100f / statistics.getTotal();
                    if (wiringRate >= 50) {
                        long averageTxsFee = statistics.getTotalFee() / statistics.getTxsCount();
                        txFee = averageTxsFee + Constants.COIN.longValue();
                    }
                }
            } else {
                txFee = Constants.NEWS_MIN_FEE.longValue();
                if (statistics != null) {
                    long averageTxsFee = statistics.getTotalFee() / statistics.getTxsCount();
                    if (averageTxsFee > Constants.NEWS_MIN_FEE.longValue()) {
                        txFee = averageTxsFee + Constants.COIN.longValue();
                    }
                }
            }
        } catch (Exception e) {
            txFee = Constants.MIN_FEE.longValue();
        }
        return txFee;
    }

    /**
     * 发送交易队列
     * @param txQueue 队列信息
     * @return 是否需要重发
     */
    boolean sendTxQueue(TxQueue txQueue, long pinnedTime, int mode) {
        byte[] chainID = ChainIDUtil.encode(txQueue.chainID);
        Account account = daemon.getAccountInfo(chainID, txQueue.senderPk);
        TxQueueAndStatus tx = txQueueRepos.getTxQueueByID(txQueue.queueID);
        logger.info("sendTxQueue chainID::{}, queueID::{}, status::{}, mode::{}",
                txQueue.chainID, txQueue.queueID, tx.status, mode);
        if (tx.status > 0) {
            // 如果上链成功直接返回
            return false;
        }
        return sendTxQueue(account, txQueue, true, pinnedTime, mode);
    }

    /**
     * 构建交易
     * @param account 账户信息
     * @param txQueue 队列信息
     * @return Transaction
     */
    private Transaction createTransaction(Account account, TxQueue txQueue, long timestamp, long nonce) {
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
		if(0 == nonce) //nonce为0代表新交易
			nonce = txRepo.getChainMaxNonce(txQueue.chainID, txQueue.senderPk) + 1;
        byte[] chainIDBytes = ChainIDUtil.encode(txQueue.chainID);
        Transaction transaction = new Transaction(chainIDBytes, timestamp, senderPk, receiverPk,
                nonce, txQueue.amount, txQueue.fee, txEncoded);
        transaction.sign(ByteUtil.toHexString(senderPk), ByteUtil.toHexString(secretKey));
        return transaction;
    }

    /**
     * 发送交易队列
     * @param account 账户信息
     * @param txQueue 队列信息
     * @param mode 1: 需要构建交易, 2: 直接重新发送, 3:老的交易行为，编辑后重发
     * @return 是否需要重发
     */
    private boolean sendTxQueue(Account account, TxQueue txQueue, boolean isDirectSend, long pinnedTime, int mode) {
		//处理已有的数据, tc
		Transaction transaction = null;
		if(mode == 2) {
			Tx tx = txRepo.getTxByQueueID(txQueue.queueID);
			transaction = createTransaction(account, txQueue, tx.timestamp, tx.nonce);
			boolean isSubmitSuccess = daemon.submitTransaction(transaction);
			if (!isSubmitSuccess) {
				return true;
			}
            //为了重发交易的时候兼容版本的更新
            tx.txID = transaction.getTxID().to_hex();
            tx.version = transaction.getVersion();
			txRepo.deleteTxByQueueID(txQueue.queueID);
            txRepo.addTransaction(tx);
			return true; //已发送的交易，直接退出
		}
		//以下需要构建新的tx
		long timestamp = 0;
		long nonce = 0;
		if (mode == 3) {
			Tx tx = txRepo.getTxByQueueID(txQueue.queueID);
			//时间重新构建
			timestamp = daemon.getSessionTime();
			nonce = tx.nonce;
			transaction = createTransaction(account, txQueue, timestamp, tx.nonce);
			boolean isSubmitSuccess = daemon.submitTransaction(transaction);
			if (!isSubmitSuccess) {
				return true;
			}
			//删除txrepo中对应的交易
			txRepo.deleteTxByQueueID(txQueue.queueID);
			logger.info("sendTxQueue update editted tx in repo");
		} else {
			//mode==1，构建有nonce的交易，继续
			timestamp = daemon.getSessionTime();
			transaction = createTransaction(account, txQueue, timestamp, 0);
			nonce = transaction.getNonce();
			boolean isSubmitSuccess = daemon.submitTransaction(transaction);
			if (!isSubmitSuccess) {
				return true;
			}
		}		

        // 创建新的本地交易
		byte[] txEncoded = transaction.getPayload();
        TxContent txContent = new TxContent(txEncoded);
        int txType = txContent.getType();
        Tx tx = null;
        if (txType == WIRING_TX.getType()) {
            tx = new Tx(txQueue.chainID, txQueue.receiverPk, txQueue.amount, txQueue.fee,
                    WIRING_TX.getType(), txContent.getMemo());
        } else if (txType == NEWS_TX.getType()) {
            NewsContent content = new NewsContent(txEncoded);
            tx = new Tx(txQueue.chainID, txQueue.fee, NEWS_TX.getType(),
					content.getMemo(), content.getLinkStr(), content.getRepliedHashStr(), content.getRepliedKeyStr());
        }
        if (tx != null) {
            // 保存交易数据到本地数据库
            tx.txID = transaction.getTxID().to_hex();
            tx.timestamp = timestamp;
            tx.senderPk = txQueue.senderPk;
            tx.nonce = nonce;
            tx.queueID = txQueue.queueID;
            tx.version = transaction.getVersion();
            if (pinnedTime > 0) {
                tx.pinnedTime = pinnedTime;
            }
            txRepo.addTransaction(tx);
            logger.info("sendWiringTx createTransaction chainID::{}, txID::{}, senderPk::{}, " +
                            "receiverPk::{}, nonce::{}, memo::{}", tx.chainID, tx.txID, tx.senderPk, tx.receiverPk, tx.nonce, tx.memo);
            addUserInfoToLocal(tx);
            addMemberInfoToLocal(tx);
            //if (isDirectSend) {
            TxLog log = new TxLog(tx.txID, TxLogStatus.SENT.getStatus(), DateUtil.getMillisTime());
            txRepo.addTxLog(log);
            //}
        }
        return false;
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
        if (txRecoveryDisposable != null && !txRecoveryDisposable.isDisposed()) {
            txRecoveryDisposable.dispose();
        }
    }
}
