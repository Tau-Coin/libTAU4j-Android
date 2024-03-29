package io.taucbd.news.publishing.ui.transaction;

import android.app.Application;
import android.content.Context;
import android.text.Html;
import android.text.InputType;
import android.view.LayoutInflater;
import android.widget.TextView;

import org.libTAU4j.Account;
import org.libTAU4j.Ed25519;
import org.libTAU4j.Pair;
import org.libTAU4j.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucbd.news.publishing.core.model.data.DataChanged;
import io.taucbd.news.publishing.core.model.data.IncomeAndExpenditure;
import io.taucbd.news.publishing.core.model.data.Result;
import io.taucbd.news.publishing.core.model.data.TxFreeStatistics;
import io.taucbd.news.publishing.core.model.data.TxLogStatus;
import io.taucbd.news.publishing.core.model.data.TxQueueAndStatus;
import io.taucbd.news.publishing.core.model.data.UserAndTxReply;
import io.taucbd.news.publishing.core.model.data.message.NewsContent;
import io.taucbd.news.publishing.core.model.data.message.NoteContent;
import io.taucbd.news.publishing.core.model.data.message.TxContent;
import io.taucbd.news.publishing.core.storage.sqlite.entity.TxLog;
import io.taucbd.news.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucbd.news.publishing.core.storage.sqlite.repo.TxQueueRepository;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.core.utils.DateUtil;
import io.taucbd.news.publishing.core.utils.FmtMicrometer;
import io.taucbd.news.publishing.core.utils.HashUtil;
import io.taucbd.news.publishing.databinding.DeleteDialogBinding;
import io.taucbd.news.publishing.ui.constant.Page;
import io.taucbd.news.publishing.core.model.data.message.TxType;
import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.Constants;
import io.taucbd.news.publishing.core.model.TauDaemon;
import io.taucbd.news.publishing.core.model.data.UserAndTx;
import io.taucbd.news.publishing.core.storage.sp.SettingsRepository;
import io.taucbd.news.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucbd.news.publishing.core.storage.RepositoryHelper;
import io.taucbd.news.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucbd.news.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Member;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Tx;
import io.taucbd.news.publishing.core.storage.sqlite.entity.User;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.ToastUtils;
import io.taucbd.news.publishing.core.utils.Utils;
import io.taucbd.news.publishing.databinding.EditFeeDialogBinding;
import io.taucbd.news.publishing.ui.BaseActivity;
import io.taucbd.news.publishing.ui.customviews.CommonDialog;
import io.taucbd.news.publishing.core.utils.rlp.ByteUtil;
import io.taucbd.news.publishing.ui.user.UserViewModel;

import static io.taucbd.news.publishing.core.model.data.message.TxType.NOTE_TX;
import static io.taucbd.news.publishing.core.model.data.message.TxType.WIRING_TX;

/**
 * 交易模块相关的ViewModel
 */
public class TxViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("TxViewModel");
    private Context application;
    private TxRepository txRepo;
    private UserRepository userRepo;
    private MemberRepository memberRepo;
    private SettingsRepository settingsRepo;
    private TxQueueRepository txQueueRepo;
    private TauDaemon daemon;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Disposable loadViewDisposable;
    private Disposable addTxDisposable;
    private MutableLiveData<List<UserAndTx>> chainTxs = new MutableLiveData<>();
    private MutableLiveData<List<UserAndTxReply>> txsReply = new MutableLiveData<>();
    private MutableLiveData<List<IncomeAndExpenditure>> walletTxs = new MutableLiveData<>();
    private MutableLiveData<Result> airdropState = new MutableLiveData<>();
    private MutableLiveData<String> addState = new MutableLiveData<>();
    private MutableLiveData<String> deletedResult = new MutableLiveData<>();
    private CommonDialog editFeeDialog;
    private CommonDialog deleteDialog;
    private List<String> msgHashList = new ArrayList<>();
    public TxViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        txRepo = RepositoryHelper.getTxRepository(application);
        userRepo = RepositoryHelper.getUserRepository(application);
        daemon  = TauDaemon.getInstance(application);
        settingsRepo  = RepositoryHelper.getSettingsRepository(application);
        memberRepo = RepositoryHelper.getMemberRepository(application);
        txQueueRepo = RepositoryHelper.getTxQueueRepository(application);
    }

    public void observeNeedStartDaemon () {
        disposables.add(daemon.observeNeedStartDaemon()
                .subscribeOn(Schedulers.io())
                .filter((needStart) -> needStart)
                .subscribe((needStart) -> daemon.start()));
    }

    public MutableLiveData<String> getDeletedResult() {
        return deletedResult;
    }

    public MutableLiveData<String> getAddState() {
        return addState;
    }

    /**
     * 获取社区链交易的被观察者
     * @return 被观察者
     */
    public MutableLiveData<List<UserAndTx>> observerChainTxs() {
        return chainTxs;
    }

    public MutableLiveData<List<UserAndTxReply>> observerTxsReply() {
        return txsReply;
    }

    public MutableLiveData<List<IncomeAndExpenditure>> getWalletTxs() {
        return walletTxs;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        msgHashList.clear();
        disposables.clear();
        if(editFeeDialog != null){
            editFeeDialog.closeDialog();
        }
        if(deleteDialog != null){
            deleteDialog.closeDialog();
        }
        if (loadViewDisposable != null && !loadViewDisposable.isDisposed()) {
            loadViewDisposable.dispose();
        }
        if (addTxDisposable != null && !addTxDisposable.isDisposed()) {
            addTxDisposable.dispose();
        }
    }

    public MutableLiveData<Result> getAirdropState(){
        return airdropState;
    }

    /**
     * 添加新的转账交易
     * @param tx 根据用户输入构建的用户数据
     */
    void addTransaction(TxQueue tx, TxQueue oldTx) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<String>) emitter -> {
            // 需要验证交易内容是否超出限制
            String result = validateTxSize(tx);
            if (StringUtil.isEmpty(result)) {
                result = addTransactionTask(tx, oldTx);
            }
            emitter.onNext(result);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> addState.postValue(state));
        disposables.add(disposable);
    }

    public String addTransactionTask(TxQueue tx, TxQueue oldTx) {
        return addTransactionTask(tx, oldTx, 0);
    }

    public String addTransactionTask(TxQueue tx, TxQueue oldTx, long pinnedTime) {
        // 转账交易: 如果对方不是好友，先加对方为好友
        if (tx.txType == WIRING_TX.getType() && StringUtil.isNotEquals(tx.senderPk, tx.receiverPk)) {
            Result result = UserViewModel.addFriendTask(getApplication(), tx.receiverPk, null, null, null);
            logger.info("addTransactionTask: add friend success::{}, isExist::{}, msg::{}",
                    result.isSuccess(), result.isExist(), result.getMsg());
        }
        if (null == oldTx) {
            long queueID = txQueueRepo.addQueue(tx);
            tx.queueID = queueID;
            logger.info("addTransactionTask insert queueID::{}", queueID);
            daemon.sendTxQueue(tx, pinnedTime, 1);
            daemon.updateTxQueue(tx.chainID);
        } else {
            // 重发交易队列
            txQueueRepo.updateQueue(tx);
            logger.info("addTransactionTask update queueID::{}", tx.queueID);
            daemon.sendTxQueue(tx, 0, 3); //编辑重发
            daemon.updateTxQueue(tx.chainID);
        }
        return "";
    }

    /**
     * 添加新的note交易
     * @param tx 根据用户输入构建的用户数据
     */
    public void addTransaction(Tx tx) {
        if (addTxDisposable != null && !addTxDisposable.isDisposed()) {
            return;
        }
        addTxDisposable = Observable.create((ObservableOnSubscribe<String>) emitter -> {
            String result = createTransaction(getApplication(), tx, false);
            emitter.onNext(result);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(state -> addState.postValue(state));
    }

    public static String createTransaction(Context context, Tx tx, boolean isResend) {
        UserRepository userRepo = RepositoryHelper.getUserRepository(context);
        TxRepository txRepo = RepositoryHelper.getTxRepository(context);
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(context);
        MemberRepository memberRepo = RepositoryHelper.getMemberRepository(context);
        TauDaemon daemon = TauDaemon.getInstance(context);
        // 获取当前用户的Seed, 获取公私钥
        User currentUser = userRepo.getCurrentUser();
        byte[] senderSeed = ByteUtil.toByte(currentUser.seed);
        Pair<byte[], byte[]> keypair = Ed25519.createKeypair(senderSeed);
        byte[] senderPk = keypair.first;
        byte[] secretKey = keypair.second;
        String result = "";
        try {
            // 获取当前用户在社区中链上nonce值
            byte[] chainID = ChainIDUtil.encode(tx.chainID);
//            Account account = daemon.getAccountInfo(chainID, currentUser.publicKey);
//            if (null == account) {
//                result = context.getString(R.string.tx_error_send_failed);
//                return result;
//            }
            byte[] receiverPk = StringUtil.isEmpty(tx.receiverPk) ? null : ByteUtil.toByte(tx.receiverPk);
            long timestamp = isResend ? tx.timestamp : daemon.getSessionTime();
            byte[] txEncoded = null;
            switch (TxType.valueOf(tx.txType)) {
                case NOTE_TX:
					NoteContent noteContent = new NoteContent(tx.memo, tx.link, tx.repliedHash);
                    txEncoded = noteContent.getEncoded();
					break;
                case NEWS_TX:
					NewsContent newsContent = new NewsContent(tx.memo, tx.link, tx.repliedHash, tx.repliedKey);
                    txEncoded = newsContent.getEncoded();
					break;
                case WIRING_TX:
                    TxContent txContent = new TxContent(tx.txType, tx.memo);
                    txEncoded = txContent.getEncoded();
                    break;
                default:
                    break;
            }
            if (null == txEncoded) {
                return result;
            }
            Transaction transaction;
            if (tx.txType == NOTE_TX.getType()) {
                String previousHash;
                if (isResend) {
                    previousHash = tx.previousHash;
                } else {
                    previousHash = txRepo.getLatestNoteTxHash(tx.chainID);
                    // 保存到本地
                    tx.previousHash = previousHash;
                }
                byte[] emptyHash = new byte[20];
                byte[] previousHashBytes = StringUtil.isNotEmpty(previousHash) ? ByteUtil.toByte(previousHash) : emptyHash;
                transaction = new Transaction(chainID, timestamp, senderPk, previousHashBytes, txEncoded);
            } else {
                transaction = new Transaction(chainID, timestamp, senderPk, receiverPk,
                        tx.nonce, tx.amount, tx.fee, txEncoded);
            }
            transaction.sign(ByteUtil.toHexString(senderPk), ByteUtil.toHexString(secretKey));
            logger.info("createTransaction txID::{}, txType::{}, previousHash::{}, limit::{}, transaction::{}",
                    transaction.getTxID().to_hex(), tx.txType, tx.previousHash, Constants.TX_MAX_BYTE_SIZE, transaction.Size());
            // 判断交易大小是否超出限制
            if (transaction.Size() > Constants.TX_MAX_BYTE_SIZE) {
                return context.getString(R.string.tx_error_memo_too_large);
            }
            boolean isSubmitSuccess = daemon.submitTransaction(transaction);
            if (!isSubmitSuccess) {
                result = isResend ? context.getString(R.string.tx_resend_failed) :
                        context.getString(R.string.tx_error_send_failed);
                return result;
            }
            if (!isResend) {
                // 保存交易数据到本地数据库
                tx.txID = transaction.getTxID().to_hex();
                tx.timestamp = timestamp;
                tx.senderPk = currentUser.publicKey;
                tx.version = transaction.getVersion();
                txRepo.addTransaction(tx);
                logger.info("createTransaction txID::{}, senderPk::{}, receiverPk::{}, memo::{}",
                        tx.txID, tx.senderPk, tx.receiverPk, tx.memo);
                addUserInfoToLocal(tx, userRepo);
                addMemberInfoToLocal(tx, memberRepo);
                settingsRepo.lastTxFee(tx.chainID, tx.fee);

                TxLog log = new TxLog(tx.txID, TxLogStatus.SENT.getStatus(), DateUtil.getMillisTime());
                txRepo.addTxLog(log);
            } else {
                String txID = transaction.getTxID().to_hex();
                logger.info("resendTransaction txID::{}, senderPk::{}, receiverPk::{}, memo::{}",
                        txID, tx.senderPk, tx.receiverPk, tx.memo);
                TxLog log = txRepo.getTxLog(txID, TxLogStatus.SENT.getStatus());
                if (null == log) {
                    log = new TxLog(tx.txID, TxLogStatus.SENT.getStatus(), DateUtil.getMillisTime());
                    txRepo.addTxLog(log);
                }
            }
        } catch (Exception e) {
            result = e.getMessage();
            logger.warn("Error adding transaction::{}", result);
        }
        return result;
    }

    /**
     * 如果是Wiring交易,添加用户信息到本地
     * @param tx 交易
     */
    private static void addUserInfoToLocal(Tx tx, UserRepository userRepo) {
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
    private static void addMemberInfoToLocal(Tx tx, MemberRepository memberRepo) {
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

    String validateTxSize(TxQueue tx) {
        User user = userRepo.getUserByPublicKey(tx.senderPk);
        byte[] chainIDBytes = ChainIDUtil.encode(tx.chainID);
        Account account = daemon.getAccountInfo(chainIDBytes, user.publicKey);
        if (null == account) {
            return getApplication().getString(R.string.tx_error_send_failed);
        }
        byte[] senderSeed = ByteUtil.toByte(user.seed);
        Pair<byte[], byte[]> keypair = Ed25519.createKeypair(senderSeed);
        byte[] senderPk = keypair.first;
        byte[] secretKey = keypair.second;
        byte[] receiverPk = ByteUtil.toByte(tx.receiverPk);
        long timestamp = daemon.getSessionTime();
        long nonce = account.getNonce() + 1;
        Transaction transaction = new Transaction(chainIDBytes, timestamp, senderPk, receiverPk,
                nonce, tx.amount, tx.fee, tx.content);
        transaction.sign(ByteUtil.toHexString(senderPk), ByteUtil.toHexString(secretKey));
        logger.info("createTransaction txID::{}, limit::{}, transaction::{}",
                transaction.getTxID().to_hex(), Constants.TX_MAX_BYTE_SIZE, transaction.Size());
        // 判断交易大小是否超出限制
        if (transaction.Size() > Constants.TX_MAX_BYTE_SIZE) {
            return getApplication().getString(R.string.tx_error_memo_too_large);
        }
        return "";
    }

    boolean validateTx(TxQueue tx, long balance) {
        if (StringUtil.isEmpty(tx.chainID)) {
            ToastUtils.showShortToast(R.string.tx_error_no_select_community);
            return false;
        }
        int type = tx.txType;
        if (type == WIRING_TX.getType()) {
            if (StringUtil.isEmpty(tx.receiverPk) ||
                    ByteUtil.toByte(tx.receiverPk).length != Ed25519.PUBLIC_KEY_SIZE) {
                ToastUtils.showShortToast(R.string.tx_error_invalid_pk);
                return false;
            } else if (tx.amount <= 0) {
                ToastUtils.showShortToast(R.string.tx_error_invalid_amount);
                return false;
            } else if (tx.fee < 0) {
                ToastUtils.showShortToast(R.string.tx_error_invalid_free);
                return false;
            } else if (tx.fee < Constants.WIRING_MIN_FEE.longValue()) {
                String minFee = getApplication().getString(R.string.tx_error_min_free,
                        FmtMicrometer.fmtFeeValue(Constants.WIRING_MIN_FEE.longValue()));
                ToastUtils.showShortToast(minFee);
                return false;
            } else if (tx.amount > balance || tx.amount + tx.fee > balance) {
                ToastUtils.showShortToast(getApplication().getString(R.string.tx_error_no_enough_coins));
                return false;
            }
        } else {
            NewsContent content = new NewsContent(tx.content);
            if (StringUtil.isEmpty(content.getMemo())) {
                ToastUtils.showShortToast(getApplication().getString(R.string.tx_error_empty_news));
                return false;
            }
            if (tx.newsLineCount + tx.linkLineCount > Constants.NEWS_MAX_LINE_COUNT) {
                ToastUtils.showShortToast(getApplication().getString(R.string.tx_error_invalid_news,
                        Constants.NEWS_TX_MAX_BYTE_SIZE, Constants.NEWS_MAX_LINE_COUNT));
                return false;
            }
            byte[] newsBytes = Utils.textStringToBytes(content.getMemo());
            byte[] linkBytes = Utils.textStringToBytes(content.getLinkStr());
            long bytesCount = newsBytes != null ? newsBytes.length : 0;
            bytesCount += linkBytes != null ? linkBytes.length : 0;
            if (bytesCount > Constants.NEWS_TX_MAX_BYTE_SIZE) {
                ToastUtils.showShortToast(getApplication().getString(R.string.tx_error_invalid_news,
                        Constants.NEWS_TX_MAX_BYTE_SIZE, Constants.NEWS_MAX_LINE_COUNT));
                return false;
            }
            long minTxFee;
            if (tx.queueType == 3) {
                minTxFee = Constants.RETWITT_MIN_FEE.longValue();
            } else {
                if (StringUtil.isNotEmpty(content.getRepliedHashStr())) {
                    minTxFee = Constants.REPLY_MIN_FEE.longValue();
                } else {
                    minTxFee = Constants.NEWS_MIN_FEE.longValue();
                }
            }
            if (tx.fee < 0) {
                ToastUtils.showShortToast(R.string.tx_error_invalid_free);
                return false;
            } else if (tx.fee < minTxFee) {
                String minFee = getApplication().getString(R.string.tx_error_min_free,
                        FmtMicrometer.fmtFeeValue(minTxFee));
                ToastUtils.showShortToast(minFee);
                return false;
            } else if (tx.fee > balance) {
                ToastUtils.showShortToast(R.string.tx_error_no_enough_coins_for_fee);
                return false;
            }
        }
        return true;
    }

    /**
     * 验证交易
     * @param tx 交易数据
     */
    public boolean validateNoteTx(Tx tx) {
        if (null == tx) {
            return false;
        }
        if (StringUtil.isEmpty(tx.chainID)) {
            ToastUtils.showShortToast(R.string.tx_error_no_select_community);
            return false;
        }
        if (StringUtil.isEmpty(tx.memo)) {
            ToastUtils.showShortToast(R.string.tx_error_invalid_message);
            return false;
        }
        return true;
    }

    /**
     * 显示编辑交易费的对话框
     */
    void showEditFeeDialog(BaseActivity activity, TextView tvFee, String chainID) {
        EditFeeDialogBinding editFeeBinding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.edit_fee_dialog, null, false);
        String fee = tvFee.getTag().toString();
        String medianFee = tvFee.getTag(R.id.median_fee).toString();
        editFeeBinding.etFee.setText(fee);
//        editFeeBinding.etFee.setFilters(new InputFilter[]{new MoneyValueFilter()});
        editFeeBinding.etFee.setInputType(InputType.TYPE_CLASS_NUMBER);
        editFeeBinding.tvMedianFee.setText(activity.getString(R.string.tx_median_fee_tips,
                FmtMicrometer.fmtFeeValue(medianFee), ChainIDUtil.getCoinName(chainID)));
        editFeeDialog = new CommonDialog.Builder(activity)
                .setContentView(editFeeBinding.getRoot())
                .setPositiveButton(R.string.common_submit, (dialog, which) -> {
                    dialog.cancel();
                    String etFee = editFeeBinding.etFee.getText().toString();
                    if (StringUtil.isNotEmpty(etFee)) {
                        String medianFree = activity.getString(R.string.tx_median_fee, etFee,
                                ChainIDUtil.getCoinName(chainID));
                        tvFee.setText(Html.fromHtml(medianFree));
                        tvFee.setTag(etFee);
                    }
                })
                .create();
        editFeeDialog.show();
    }

    /**
     * 1、转账交易费默认还是1；只有达到50%才平均交易费+1；
     * 2、news最小交易费为10，平均交易费超过10，就+1；
     * 3、这里只是设置默认值，只能动态修改airdrop和referral的交易费；
     * @param chainID 交易所属的社区chainID
     */
    public Observable<Long> observeAverageTxFee(String chainID, TxType type) {
        return observeAverageTxFee(chainID, type, false, false);
    }
    public Observable<Long> observeAverageTxFee(String chainID, TxType type, boolean isRetwitt, boolean isReply) {
        return Observable.create((ObservableOnSubscribe<Long>) emitter -> {
            long txFee;
            try {
                TxFreeStatistics statistics = null;
                if (StringUtil.isNotEmpty(chainID)) {
                    statistics = txRepo.queryAverageTxsFee(chainID);
                }
                if (statistics != null) {
                    logger.debug("observeAverageTxFee Total::{}, TotalFee::{}, TxsCount::{}, WiringCount::{}",
                            statistics.getTotal(), statistics.getTotalFee(), statistics.getTxsCount(), statistics.getWiringCount());
                }
                if (type == WIRING_TX) {
                    txFee = Constants.WIRING_MIN_FEE.longValue();
                    if (statistics != null) {
                        if (statistics.getTotal() > 0) {
                            float wiringRate = statistics.getWiringCount() * 100f / statistics.getTotal();
                            if (wiringRate >= 50) {
                                if (statistics.getTxsCount() > 0) {
                                    long averageTxsFee = statistics.getTotalFee() / statistics.getTxsCount();
                                    txFee = averageTxsFee + Constants.COIN.longValue();
                                }
                            }
                        }
                    }
                } else {
                    if (isRetwitt) {
                        txFee = Constants.RETWITT_MIN_FEE.longValue();
                    } else {
                        if (isReply) {
                            txFee = Constants.REPLY_MIN_FEE.longValue();
                        } else {
                            txFee = Constants.NEWS_MIN_FEE.longValue();
                        }
                    }
                    if (statistics != null && statistics.getTxsCount() > 0) {
                        long averageTxsFee = statistics.getTotalFee() / statistics.getTxsCount();
                        if (averageTxsFee > txFee) {
                            txFee = averageTxsFee + Constants.COIN.longValue();
                        }
                    }
                }
            } catch (Exception e) {
                txFee = Constants.MIN_FEE.longValue();
            }
            logger.debug("observeAverageTxFee txFee::{}", txFee);
            emitter.onNext(txFee);
            emitter.onComplete();
        });
    }

    /**
     * 添加朋友为社区成员
     * @param chainID
     * @param friendPks
     */
    public void addMembers(String chainID, Map<String, String> friendPks, String fee) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            try {
                Collection<String> values = friendPks.values();
                long totalPay = 0L;
                long txFee = FmtMicrometer.fmtTxLongValue(fee);
                for (String value : values) {
                    totalPay += FmtMicrometer.fmtTxLongValue(value);
                }
                totalPay += friendPks.size() * txFee;

                String senderPk = MainApplication.getInstance().getPublicKey();
                Member member = memberRepo.getMemberByChainIDAndPk(chainID, senderPk);
                long paymentBalance = 0L;
                if (member != null) {
                    paymentBalance = member.getPaymentBalance();
                }
                if (totalPay > paymentBalance) {
                    result.setFailMsg(application.getString(R.string.tx_error_add_members_no_enough_coins));
                } else {
                    Set<String> friends = friendPks.keySet();
                    for (String friend : friends) {
                        String amount = friendPks.get(friend);
                        logger.info("airdropToFriends chainID::{}, friend::{}, amount::{}, fee::{}",
                                chainID, friend, amount, fee);
                        String airdropResult = addMembers(chainID, senderPk, friend,
                                FmtMicrometer.fmtTxLongValue(amount),
                                FmtMicrometer.fmtTxLongValue(fee));
                        if (StringUtil.isNotEmpty(airdropResult)) {
                            result.setFailMsg(airdropResult);
                            break;
                        }
                    }
                }
            } catch (Exception e){
                result.setFailMsg(e.getMessage());
            }
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> airdropState.postValue(result));
        disposables.add(disposable);
    }

    /**
     * 添加朋友为社区成员
     */
    private String addMembers(String chainID, String senderPk, String friendPk,
                                  long amount, long fee) {
        String memo = getApplication().getString(R.string.community_added_members);
        TxContent txContent = new TxContent(WIRING_TX.getType(), Utils.textStringToBytes(memo));
        TxQueue tx = new TxQueue(chainID, senderPk, friendPk, amount, fee, TxType.WIRING_TX, txContent.getEncoded());
        return addTransactionTask(tx, null);
    }


    /**
     * 加载交易Notes分页数据
     * @param repliesHash 回复的news hash
     * @param pos 分页位置
     * @param initSize 刷新时第一页数据大小
     */
    void loadNotesData(String repliesHash, int pos, int initSize, String newTxID) {
        if (loadViewDisposable != null && !loadViewDisposable.isDisposed()) {
            loadViewDisposable.dispose();
        }
        loadViewDisposable = Observable.create((ObservableOnSubscribe<List<UserAndTx>>) emitter -> {
                    List<UserAndTx> txs = new ArrayList<>();
                    try {
                        long startTime = System.currentTimeMillis();
                        int pageSize = Page.PAGE_SIZE;
                        int txPos = 0;
                        if (StringUtil.isNotEmpty(newTxID)) {
                            txPos = txRepo.getRepliesTxPosition(repliesHash, newTxID);
                        }
                        if (pos == 0) {
                            if (initSize >= pageSize) {
                                pageSize = initSize;
                            } else {
                                pageSize = txPos + Page.PAGE_SIZE - txPos % Page.PAGE_SIZE;
                            }
                        }
                        logger.debug("loadNotesData txPos::{}, pageSize::{}", txPos, pageSize);
                        txs = txRepo.loadAllNotesData(repliesHash, pos, pageSize);
                        long getMessagesTime = System.currentTimeMillis();
                        logger.debug("loadNotesData repliesHash::{}, pos::{}, pageSize::{}, messages.size::{}",
                                repliesHash, pos, pageSize, txs.size());
                        logger.debug("loadNotesData getMessagesTime::{}", getMessagesTime - startTime);
                        Collections.reverse(txs);

                for (UserAndTx tx : txs) {
                    if (tx.logs != null && tx.logs.size() > 0) {
                        Collections.sort(tx.logs);
                    }
                }
            } catch (Exception e) {
                logger.error("loadNotesData error::", e);
            }
            emitter.onNext(txs);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(messages -> {
                    chainTxs.postValue(messages);
                });
    }

    /**
     * 加载首页所有news交易分页数据
     * @param pos 分页位置
     * @param initSize 刷新时第一页数据大小
     */
    void loadNewsData(int pos, int initSize) {
        if (loadViewDisposable != null && !loadViewDisposable.isDisposed()) {
            loadViewDisposable.dispose();
        }
        loadViewDisposable = Observable.create((ObservableOnSubscribe<List<UserAndTxReply>>) emitter -> {
            List<UserAndTxReply> txs = new ArrayList<>();
            try {
                int pageSize = Page.PAGE_SIZE;
                if (pos == 0 && initSize > pageSize) {
                    pageSize = initSize;
                }
                long startTime = System.currentTimeMillis();
                txs = txRepo.loadNewsData(pos, pageSize);
                long endTime = System.currentTimeMillis();
                logger.debug("loadNewsData time::{}ms", endTime - startTime);
                logger.debug("loadNewsData pos::{}, pageSize::{}, messages.size::{}",
                        pos, pageSize, txs.size());
                if (pos == 0) {
                    msgHashList.clear();
                }
                for (UserAndTxReply tx : txs) {
                    String msg = "" + tx.memo + tx.link;
                    String hash = HashUtil.makeSha256Hash(msg);
                    if (!msgHashList.contains(hash)) {
                        msgHashList.add(hash);
                        tx.setShow(true);
                        logger.debug("loadNewsData true msg::{}", tx.txID);
                    } else {
                        tx.setShow(false);
                        logger.debug("loadNewsData false msg::{}", tx.txID);
                    }

                }
                long hashEndTime = System.currentTimeMillis();
                logger.debug("loadNewsData hashTime::{}ms", hashEndTime - endTime);
            } catch (Exception e) {
                logger.error("loadNewsData error::", e);
            }
            emitter.onNext(txs);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(messages -> {
            txsReply.postValue(messages);
        });
    }

    /**
     * 加载搜索的news交易分页数据
     * @param pos 分页位置
     * @param initSize 刷新时第一页数据大小
     */
    void loadSearchNewsData(String keywords, int pos, int initSize) {
        if (loadViewDisposable != null && !loadViewDisposable.isDisposed()) {
            loadViewDisposable.dispose();
        }
        loadViewDisposable = Observable.create((ObservableOnSubscribe<List<UserAndTx>>) emitter -> {
            List<UserAndTx> txs = new ArrayList<>();
            try {
                int pageSize = Page.PAGE_SIZE;
                if (pos == 0 && initSize > pageSize) {
                    pageSize = initSize;
                }
                long startTime = System.currentTimeMillis();
                txs = txRepo.loadSearchNewsData(keywords, pos, pageSize);
                long endTime = System.currentTimeMillis();
                logger.debug("loadSearchNewsData time::{}ms", endTime - startTime);
                logger.debug("loadSearchNewsData pos::{}, pageSize::{}, messages.size::{}",
                        pos, pageSize, txs.size());
            } catch (Exception e) {
                logger.error("loadSearchNewsData error::", e);
            }
            emitter.onNext(txs);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(messages -> {
            chainTxs.postValue(messages);
        });
    }

    /**
     * 加载news的回复交易分页数据
     * @param pos 分页位置
     * @param initSize 刷新时第一页数据大小
     */
    void loadNewsRepliesData(String repliesHash, int pos, int initSize, String newTxID) {
        if (loadViewDisposable != null && !loadViewDisposable.isDisposed()) {
            loadViewDisposable.dispose();
        }
        loadViewDisposable = Observable.create((ObservableOnSubscribe<List<UserAndTx>>) emitter -> {
            List<UserAndTx> txs = new ArrayList<>();
            try {
                int pageSize = Page.PAGE_SIZE;
                int txPos = 0;
                if (StringUtil.isNotEmpty(newTxID)) {
                    txPos = txRepo.getRepliesTxPosition(repliesHash, newTxID);
                }
                if (pos == 0) {
                    if (initSize >= pageSize) {
                        pageSize = initSize;
                    } else {
                        pageSize = txPos + Page.PAGE_SIZE - txPos % Page.PAGE_SIZE;
                    }
                }
                logger.debug("loadNewsRepliesData txPos::{}, pageSize::{}", txPos, pageSize);
                txs = txRepo.loadNewsRepliesData(repliesHash, pos, pageSize);
                logger.debug("loadNewsRepliesData txID::{}, pos::{}, pageSize::{}, messages.size::{}",
                        repliesHash, pos, pageSize, txs.size());
            } catch (Exception e) {
                logger.error("loadNewsRepliesData error::", e);
            }
            emitter.onNext(txs);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(messages -> {
            chainTxs.postValue(messages);
        });
    }

    /**
     * 加载交易Market分页数据
     * @param chainID 社区链ID
     * @param pos 分页位置
     * @param initSize 刷新时第一页数据大小
     */
    void loadMarketData(String chainID, int pos, int initSize) {
        if (loadViewDisposable != null && !loadViewDisposable.isDisposed()) {
            loadViewDisposable.dispose();
        }
        loadViewDisposable = Observable.create((ObservableOnSubscribe<List<UserAndTx>>) emitter -> {
            List<UserAndTx> txs = new ArrayList<>();
            try {
                int pageSize = Page.PAGE_SIZE;
                if (pos == 0 && initSize > pageSize) {
                    pageSize = initSize;
                }
                long startTime = System.currentTimeMillis();
                txs = txRepo.loadAllMarketData(chainID, pos, pageSize);
                long endTime = System.currentTimeMillis();
                logger.debug("loadMarketData time::{}ms", endTime - startTime);
                logger.debug("loadMarketData chainID::{}, pos::{}, pageSize::{}, messages.size::{}",
                        chainID, pos, pageSize, txs.size());
            } catch (Exception e) {
                logger.error("loadMarketData error::", e);
            }
            emitter.onNext(txs);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(messages -> {
                    chainTxs.postValue(messages);
                });
    }

    /**
     * 加载转账交易分页数据
     * @param chainID 社区链ID
     * @param pos 分页位置
     * @param initSize 刷新时第一页数据大小
     */
    void loadChainTxsData(String chainID, int pos, int initSize) {
        if (loadViewDisposable != null && !loadViewDisposable.isDisposed()) {
            loadViewDisposable.dispose();
        }
        loadViewDisposable = Observable.create((ObservableOnSubscribe<List<UserAndTx>>) emitter -> {
            List<UserAndTx> txs = new ArrayList<>();
            try {
                int pageSize = pos == 0 ? Page.PAGE_SIZE * 2 : Page.PAGE_SIZE;
                if (pos == 0 && initSize > pageSize) {
                    pageSize = initSize;
                }
                txs = txRepo.loadChainTxsData(chainID, pos, pageSize);
                logger.debug("loadChainTxsData, pos::{}, pageSize::{}, messages.size::{}",
                        pos, pageSize, txs.size());
                Collections.reverse(txs);
            } catch (Exception e) {
                logger.error("loadChainTxsData error::", e);
            }
            emitter.onNext(txs);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(messages -> {
                    chainTxs.postValue(messages);
                });
    }

    public Flowable<List<UserAndTx>> observeLatestPinnedMsg(String chainID) {
        return txRepo.observeLatestPinnedMsg(chainID);
    }

    public Flowable<List<UserAndTx>> observeLatestPinnedMsg() {
        return txRepo.observeLatestPinnedMsg();
    }

    /**
     * 加载交易固定数据
     * @param chainID 社区链ID
     */
    void loadPinnedTxsData(String chainID) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<List<UserAndTx>>) emitter -> {
            List<UserAndTx> txs = new ArrayList<>();
            try {
                if (StringUtil.isNotEmpty(chainID)) {
                    txs = txRepo.queryCommunityPinnedTxs(chainID);
                } else {
                    txs = txRepo.queryCommunityPinnedTxs();
                }
                logger.debug("loadPinnedTxsData messages.size::{}", txs.size());
            } catch (Exception e) {
                logger.error("loadPinnedTxsData error::", e);
            }
            emitter.onNext(txs);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(messages -> {
                    chainTxs.postValue(messages);
                });
        disposables.add(disposable);
    }

    /**
     * 周期性重发交易，12小时，30分钟一次
	 * note tx: 最近10笔
	 * new tx: 没有上链的所有消息
     */
	public static void resendRegularTxs(Context context) {
        TxQueueRepository txQueueRepo = RepositoryHelper.getTxQueueRepository(context);
        TauDaemon daemon = TauDaemon.getInstance(context);
		//重发note tx
		List<TxQueueAndStatus> txQueues =  txQueueRepo.getResendNoteTxQueue(MainApplication.getInstance().getPublicKey());
		if (txQueues.size() > 0) {
			for (TxQueueAndStatus txQueue : txQueues) {
				txQueueRepo.updateQueue(txQueue);
				logger.info("resend regular note tx queueID::{}", txQueue.queueID);
				daemon.sendTxQueue(txQueue, 0, 2); //直接重发
			}
		}

		//重发news tx
		txQueues =  txQueueRepo.getResendNewsTxQueue(MainApplication.getInstance().getPublicKey());
		if (txQueues.size() > 0) {
			for (TxQueueAndStatus txQueue : txQueues) {
				txQueueRepo.updateQueue(txQueue);
				logger.info("resend regular news tx queueID::{}", txQueue.queueID);
				daemon.sendTxQueue(txQueue, 0, 2); //直接重发
			}
		}
	}

    /**
     * 观察社区的消息的变化
     */
    Observable<DataChanged> observeDataSetChanged() {
        return txRepo.observeDataSetChanged();
    }

    public Flowable<List<TxQueueAndStatus>> observeCommunityTxQueue(String chainID) {
        String userPk = MainApplication.getInstance().getPublicKey();
        return txQueueRepo.observeCommunityTxQueue(chainID, userPk);
    }

    void deleteTxQueue(TxQueue tx) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            try {
                txQueueRepo.deleteQueue(tx);
                txRepo.deleteTxByQueueID(tx.queueID);
            } catch (Exception e) {
                logger.error("deleteTxQueue error::", e);
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    public void setMessagePinned(UserAndTx tx, boolean isRefresh) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            tx.pinnedTime = tx.pinnedTime > 0 ? 0 : DateUtil.getMillisTime();
            txRepo.setMessagePinned(tx.txID, tx.pinnedTime, isRefresh);
            ToastUtils.showShortToast(tx.pinnedTime > 0 ?
                    application.getString(R.string.community_pinned_successfully) :
                    application.getString(R.string.community_unpinned_successfully));
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    public void setMessageFavorite(UserAndTx tx, boolean isRefresh) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            tx.favoriteTime = tx.favoriteTime > 0 ? 0 : DateUtil.getMillisTime();
            txRepo.setMessageFavorite(tx.txID, tx.favoriteTime, isRefresh);
            ToastUtils.showShortToast(tx.favoriteTime > 0 ?
                    application.getString(R.string.community_favorite_add_successfully) :
                    application.getString(R.string.community_favorite_delete_successfully));
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 查询收藏
     * @return DataSource
     */
    DataSource.Factory<Integer, UserAndTx> queryFavorites(){
        return txRepo.queryFavorites();
    }

    public LiveData<PagedList<UserAndTx>> observerFavorites() {
        return new LivePagedListBuilder<>(
                queryFavorites(), Page.getPageListConfig()).build();
    }

    void resendTransaction(String txID) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<String>) emitter -> {
            logger.debug("resendTransaction txID::{}", txID);
            Tx tx = txRepo.getTxByTxID(txID);
            if (tx != null) {
                String result = createTransaction(getApplication(), tx, true);
                if (StringUtil.isNotEmpty(result)) {
                    ToastUtils.showShortToast(result);
                } else {
                    ToastUtils.showShortToast(R.string.tx_resend_successful);
                }
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    public void queryWalletIncomeAndExpenditure(String chainID, int pos, int initSize) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<List<IncomeAndExpenditure>>) emitter -> {
            List<IncomeAndExpenditure> list = new ArrayList<>();
            try {
                int pageSize = pos == 0 ? Page.PAGE_SIZE * 2 : Page.PAGE_SIZE;
                if (pos == 0 && initSize > pageSize) {
                    pageSize = initSize;
                }
                list = txRepo.observeWalletTransactions(chainID, pos, pageSize);

                //walletTxs加入pending 状态的mining rewards
                User currentUser = userRepo.getCurrentUser();
                Member member = memberRepo.getMemberByChainIDAndPk(chainID, currentUser.publicKey);
                long miningRewards = member.getMiningRewards();
                if( miningRewards > 0) {
                    IncomeAndExpenditure miningRewardEntry = new IncomeAndExpenditure(currentUser.publicKey, miningRewards, daemon.getSessionTime()/1000);
                    list.add(0, miningRewardEntry);
                    miningRewardEntry.sender = userRepo.getCurrentUser();
                }
                logger.debug("queryWalletIncomeAndExpenditure pos::{}, pageSize::{}, size::{}", pos, pageSize, list.size());

            } catch (Exception e) {
                logger.error("queryWalletIncomeAndExpenditure error::", e);
            }
            emitter.onNext(list);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    walletTxs.postValue(list);
                });

        disposables.add(disposable);
    }

    public Flowable<Object> observeWalletChanged() {
        return txRepo.observeWalletChanged();
    }

    public Observable<Tx> observeTxByTxID(String hash) {
        return txRepo.observeTxByTxID(hash);
    }

    public Observable<UserAndTx> observeNewsDetail(String txID) {
        return txRepo.observeNewsDetail(txID);
    }

    public Flowable<UserAndTxReply> observeMaxChatNumNews() {
        return txRepo.observeMaxChatNumNews();
    }

    /**
     * 显示删除消息的对话框
     */
    public void deleteThisNews(BaseActivity activity, String txID) {
        DeleteDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.delete_dialog, null, false);
        binding.ivClose.setOnClickListener(v -> {
            if (deleteDialog != null) {
                deleteDialog.closeDialog();
            }
        });
        binding.tvSubmit.setOnClickListener(v -> {
            if (deleteDialog != null) {
                deleteDialog.closeDialog();
            }
            deleteThisNews(txID);
        });
        deleteDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .create();
        deleteDialog.show();
    }

    private void deleteThisNews(String txID) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<String>) emitter -> {
            txRepo.deleteThisNews(txID);
            emitter.onNext(txID);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(result -> deletedResult.postValue(result));
        disposables.add(disposable);
    }
}
