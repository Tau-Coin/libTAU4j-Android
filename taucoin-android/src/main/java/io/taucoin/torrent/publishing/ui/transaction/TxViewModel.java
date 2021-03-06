package io.taucoin.torrent.publishing.ui.transaction;

import android.app.Application;
import android.content.Context;
import android.text.Html;
import android.text.InputFilter;
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
import io.taucoin.torrent.publishing.core.model.data.DataChanged;
import io.taucoin.torrent.publishing.core.model.data.Result;
import io.taucoin.torrent.publishing.core.model.data.TxLogStatus;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.model.data.message.AirdropTxContent;
import io.taucoin.torrent.publishing.core.model.data.message.AnnouncementContent;
import io.taucoin.torrent.publishing.core.model.data.message.SellTxContent;
import io.taucoin.torrent.publishing.core.model.data.message.TrustContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxContent;
import io.taucoin.torrent.publishing.core.model.data.message.QueueOperation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxLog;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxQueueRepository;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.LinkUtil;
import io.taucoin.torrent.publishing.core.utils.MoneyValueFilter;
import io.taucoin.torrent.publishing.ui.chat.ChatViewModel;
import io.taucoin.torrent.publishing.ui.constant.Page;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.EditFeeDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;

import static io.taucoin.torrent.publishing.core.model.data.message.TxType.AIRDROP_TX;
import static io.taucoin.torrent.publishing.core.model.data.message.TxType.ANNOUNCEMENT;
import static io.taucoin.torrent.publishing.core.model.data.message.TxType.NOTE_TX;
import static io.taucoin.torrent.publishing.core.model.data.message.TxType.SELL_TX;
import static io.taucoin.torrent.publishing.core.model.data.message.TxType.WIRING_TX;

/**
 * ?????????????????????ViewModel
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
    private MutableLiveData<List<Tx>> trustTxs = new MutableLiveData<>();
    private MutableLiveData<Result> airdropState = new MutableLiveData<>();
    private MutableLiveData<String> addState = new MutableLiveData<>();
    private CommonDialog editFeeDialog;
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

    MutableLiveData<String> getAddState() {
        return addState;
    }

    /**
     * ????????????????????????????????????
     * @return ????????????
     */
    public MutableLiveData<List<UserAndTx>> observerChainTxs() {
        return chainTxs;
    }

    public MutableLiveData<List<Tx>> observerTrustTxs() {
        return trustTxs;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
        if(editFeeDialog != null){
            editFeeDialog.closeDialog();
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
     * ????????????????????????
     * @param tx ???????????????????????????????????????
     */
    void addTransaction(TxQueue tx, TxQueue oldTx) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<String>) emitter -> {
            // ??????????????????????????????????????????
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
        if (null == oldTx) {
            long queueID = txQueueRepo.addQueue(tx);
            tx.queueID = queueID;
            logger.info("addTransactionTask insert queueID::{}", queueID);
            daemon.sendTxQueue(tx, pinnedTime);
            ChatViewModel.syncSendMessageTask(getApplication(), tx, QueueOperation.INSERT);
            daemon.updateTxQueue(tx.chainID);
        } else {
            // ??????????????????
            txQueueRepo.updateQueue(tx);
            logger.info("addTransactionTask update queueID::{}", tx.queueID);
            daemon.sendTxQueue(tx, 0);
            // ????????????????????????????????????????????????????????????
            ChatViewModel.syncSendMessageTask(getApplication(), tx, QueueOperation.UPDATE);
            daemon.updateTxQueue(tx.chainID);
        }
        return "";
    }

    /**
     * ??????????????????
     * @param tx ???????????????????????????????????????
     */
    void addTransaction(Tx tx) {
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
        // ?????????????????????Seed, ???????????????
        User currentUser = userRepo.getCurrentUser();
        byte[] senderSeed = ByteUtil.toByte(currentUser.seed);
        Pair<byte[], byte[]> keypair = Ed25519.createKeypair(senderSeed);
        byte[] senderPk = keypair.first;
        byte[] secretKey = keypair.second;
        String result = "";
        try {
            // ????????????????????????????????????nonce???
            byte[] chainID = ChainIDUtil.encode(tx.chainID);
            Account account = daemon.getAccountInfo(chainID, currentUser.publicKey);
            if (null == account) {
                result = context.getString(R.string.tx_error_send_failed);
                return result;
            }
            byte[] receiverPk = StringUtil.isEmpty(tx.receiverPk) ? null : ByteUtil.toByte(tx.receiverPk);
            long timestamp = isResend ? tx.timestamp : daemon.getSessionTime();
            byte[] txEncoded = null;
            switch (TxType.valueOf(tx.txType)) {
                case NOTE_TX:
                case WIRING_TX:
                    TxContent txContent = new TxContent(tx.txType, tx.memo);
                    txEncoded = txContent.getEncoded();
                    break;
                case TRUST_TX:
                    // trust UserPk??????receiverPk
                    TrustContent trustContent = new TrustContent(tx.memo, tx.receiverPk);
                    txEncoded = trustContent.getEncoded();
                    receiverPk = senderPk;
                    break;
                case SELL_TX:
                    SellTxContent sellTxContent = new SellTxContent(tx.coinName, tx.quantity,
                            tx.link, tx.location, tx.memo);
                    txEncoded = sellTxContent.getEncoded();
                    break;
                case AIRDROP_TX:
                    AirdropTxContent airdropContent = new AirdropTxContent(tx.link, tx.memo);
                    txEncoded = airdropContent.getEncoded();
                    break;
                case ANNOUNCEMENT:
                    AnnouncementContent invitationContent = new AnnouncementContent(tx.coinName, tx.memo);
                    txEncoded = invitationContent.getEncoded();
                    break;
                default:
                    break;
            }
            if (null == txEncoded) {
                return result;
            }
            Transaction transaction;
            if (tx.txType == NOTE_TX.getType()) {
                transaction = new Transaction(chainID, 0, timestamp, senderPk, txEncoded);
            } else {
                transaction = new Transaction(chainID, 0, timestamp, senderPk, receiverPk,
                        tx.nonce, tx.amount, tx.fee, txEncoded);
            }
            transaction.sign(ByteUtil.toHexString(senderPk), ByteUtil.toHexString(secretKey));
            logger.info("createTransaction txID::{}, limit::{}, transaction::{}",
                    transaction.getTxID().to_hex(), Constants.TX_MAX_BYTE_SIZE, transaction.Size());
            // ????????????????????????????????????
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
                // ????????????????????????????????????
                tx.txID = transaction.getTxID().to_hex();
                tx.timestamp = timestamp;
                tx.senderPk = currentUser.publicKey;
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
     * ?????????Wiring??????,???????????????????????????
     * @param tx ??????
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
     * ????????????????????????
     * @param tx ??????
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
        Transaction transaction = new Transaction(chainIDBytes, 0, timestamp, senderPk, receiverPk,
                nonce, tx.amount, tx.fee, tx.content);
        transaction.sign(ByteUtil.toHexString(senderPk), ByteUtil.toHexString(secretKey));
        logger.info("createTransaction txID::{}, limit::{}, transaction::{}",
                transaction.getTxID().to_hex(), Constants.TX_MAX_BYTE_SIZE, transaction.Size());
        // ????????????????????????????????????
        if (transaction.Size() > Constants.TX_MAX_BYTE_SIZE) {
            return getApplication().getString(R.string.tx_error_memo_too_large);
        }
        return "";
    }

    boolean validateTx(TxQueue tx) {
        byte[] chainID = ChainIDUtil.encode(tx.chainID);
        String senderPk = MainApplication.getInstance().getPublicKey();
        Account account = daemon.getAccountInfo(chainID, senderPk);
        long balance = account != null ? account.getBalance() : 0;
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
                ToastUtils.showShortToast(R.string.tx_error_no_enough_coins);
                return false;
            }
        } else {
            if (type == SELL_TX.getType()) {
                SellTxContent txContent = new SellTxContent(tx.content);
                if (StringUtil.isEmpty(txContent.getCoinName())) {
                    ToastUtils.showShortToast(R.string.tx_error_invalid_item_name);
                    return false;
                }
            } else if (type == AIRDROP_TX.getType()) {
                AirdropTxContent txContent = new AirdropTxContent(tx.content);
                LinkUtil.Link link = LinkUtil.decode(txContent.getLink());
                if (!link.isAirdropLink()) {
                    ToastUtils.showShortToast(R.string.tx_error_invalid_airdrop_link);
                    return false;
                }
            } else if (type == ANNOUNCEMENT.getType()) {
                AnnouncementContent txContent = new AnnouncementContent(tx.content);
                if (StringUtil.isEmpty(txContent.getTitle())) {
                    ToastUtils.showShortToast(R.string.tx_error_invalid_title);
                    return false;
                }
            }
            if (tx.fee < 0) {
                ToastUtils.showShortToast(R.string.tx_error_invalid_free);
                return false;
            } else if (tx.fee < Constants.NEWS_MIN_FEE.longValue()) {
                String minFee = getApplication().getString(R.string.tx_error_min_free,
                        FmtMicrometer.fmtFeeValue(Constants.NEWS_MIN_FEE.longValue()));
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
     * ????????????
     * @param tx ????????????
     */
    boolean validateNoteTx(Tx tx) {
        if (null == tx) {
            return false;
        }
        if (StringUtil.isEmpty(tx.memo)) {
            ToastUtils.showShortToast(R.string.tx_error_invalid_message);
            return false;
        }
        return true;
    }

    public long getLastTxFee(String chainID){
        return settingsRepo.lastTxFee(chainID);
    }

    /**
     * ?????????????????????????????????
     */
    void showEditFeeDialog(BaseActivity activity, TextView tvFee, String chainID) {
        if(StringUtil.isEmpty(chainID)){
            return;
        }
        EditFeeDialogBinding editFeeBinding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.edit_fee_dialog, null, false);
        String fee = tvFee.getTag().toString();
        String medianFee = tvFee.getTag(R.id.median_fee).toString();
        editFeeBinding.etFee.setText(fee);
        editFeeBinding.etFee.setFilters(new InputFilter[]{new MoneyValueFilter()});
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
     * 0???????????????????????????
     * 1???????????????????????????10????????????????????????
     * 2????????????????????????????????????0?????????????????????????????????
     * @param chainID ?????????????????????chainID
     */
    public long getTxFee(String chainID, TxType type) {
        long free;
        if (type == WIRING_TX) {
            free = Constants.WIRING_MIN_FEE.longValue();
        } else if (type == NOTE_TX) {
            free = Constants.NOTES_MIN_FEE.longValue();
        } else {
            free = Constants.NEWS_MIN_FEE.longValue();
        }
        return free;
    }

    /**
     * ???????????????????????????
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
                Account account = daemon.getAccountInfo(ChainIDUtil.encode(chainID), senderPk);
                long balance = 0L;
                if (account != null) {
                    balance = account.getBalance();
                }
                if (totalPay > balance) {
                    result.setFailMsg(application.getString(R.string.tx_error_insufficient_balance));
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
     * ???????????????????????????
     */
    private String addMembers(String chainID, String senderPk, String friendPk,
                                  long amount, long fee) {
        String memo = getApplication().getString(R.string.community_added_members);
        TxContent txContent = new TxContent(WIRING_TX.getType(), Utils.textStringToBytes(memo));
        TxQueue tx = new TxQueue(chainID, senderPk, friendPk, amount, fee, TxType.WIRING_TX, txContent.getEncoded());
        return addTransactionTask(tx, null);
    }


    /**
     * ????????????Notes????????????
     * @param onChain ??????????????????
     * @param chainID ?????????ID
     * @param pos ????????????
     * @param initSize ??????????????????????????????
     */
    void loadNotesData(boolean onChain, String chainID, int pos, int initSize) {
        if (loadViewDisposable != null && !loadViewDisposable.isDisposed()) {
            loadViewDisposable.dispose();
        }
        loadViewDisposable = Observable.create((ObservableOnSubscribe<List<UserAndTx>>) emitter -> {
            List<UserAndTx> txs = new ArrayList<>();
            try {
                long startTime = System.currentTimeMillis();
                int pageSize = pos == 0 ? Page.PAGE_SIZE * 2 : Page.PAGE_SIZE;
                if (pos == 0 && initSize > pageSize) {
                    pageSize = initSize;
                }
                if (onChain) {
                    txs = txRepo.loadOnChainNotesData(chainID, pos, pageSize);
                } else {
                    txs = txRepo.loadAllNotesData(chainID, pos, pageSize);
                }
                long getMessagesTime = System.currentTimeMillis();
                logger.debug("loadNotesData onChain::{}, pos::{}, pageSize::{}, messages.size::{}",
                        onChain, pos, pageSize, txs.size());
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
     * ????????????Market????????????
     * @param filterItem ??????????????????
     * @param chainID ?????????ID
     * @param pos ????????????
     * @param initSize ??????????????????????????????
     */
    void loadMarketData(int filterItem, String chainID, int pos, int initSize) {
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
                if (filterItem == R.string.community_view_airdrop) {
                    txs = txRepo.loadAirdropMarketData(chainID, pos, pageSize);
                } else if (filterItem == R.string.community_view_sell) {
                    txs = txRepo.loadSellMarketData(chainID, pos, pageSize);
                } else if (filterItem == R.string.community_view_announcement) {
                    txs = txRepo.loadAnnouncementMarketData(chainID, pos, pageSize);
                } else {
                    txs = txRepo.loadAllMarketData(chainID, pos, pageSize);
                }
                logger.debug("loadMarketData filterItem::{}, pos::{}, pageSize::{}, messages.size::{}",
                        application.getString(filterItem), pos, pageSize, txs.size());
                Collections.reverse(txs);
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
     * ??????????????????????????????
     * @param chainID ?????????ID
     * @param pos ????????????
     * @param initSize ??????????????????????????????
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

    public Flowable<List<UserAndTx>> observeLatestPinnedMsg(int currentTab, String chainID) {
        return txRepo.observeLatestPinnedMsg(currentTab, chainID);
    }

    /**
     * ????????????????????????
     * @param currentTab ???????????????tab???
     * @param chainID ?????????ID
     */
    void loadPinnedTxsData(int currentTab, String chainID) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<List<UserAndTx>>) emitter -> {
            List<UserAndTx> txs = new ArrayList<>();
            try {
                txs = txRepo.queryCommunityPinnedTxs(chainID, currentTab);
                logger.debug("loadPinnedTxsData, currentTab::{}, messages.size::{}",
                        currentTab, txs.size());
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
     * ??????Trust??????????????????
     * @param chainID ?????????ID
     * @param trustPk trust??????
     * @param pos ????????????
     */
    void loadTrustTxsData(String chainID, String trustPk, int pos) {
        int pageSize = pos == 0 ? Page.PAGE_SIZE * 2 : Page.PAGE_SIZE;
        loadTrustTxsData(chainID, trustPk, pos, pageSize);

    }

    void loadTrustTxsData(String chainID, String trustPk, int pos, int pageSize) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<List<Tx>>) emitter -> {
            List<Tx> trustTxs = new ArrayList<>();
            try {
                long startTime = System.currentTimeMillis();
                trustTxs = txRepo.queryCommunityTrustTxs(chainID, trustPk, pos, pageSize);
                long getMessagesTime = System.currentTimeMillis();
                logger.debug("loadTrustTxsData pos::{}, pageSize::{}, messages.size::{}",
                        pos, pageSize, trustTxs.size());
                logger.debug("loadTrustTxsData getMessagesTime::{}", getMessagesTime - startTime);
                Collections.reverse(trustTxs);
                long endTime = System.currentTimeMillis();
                logger.debug("loadTrustTxsData reverseTime Time::{}", endTime - getMessagesTime);
            } catch (Exception e) {
                logger.error("loadTrustTxsData error::", e);
            }
            emitter.onNext(trustTxs);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(txs -> {
                    trustTxs.postValue(txs);
                });
        disposables.add(disposable);
    }

    /**
     * ??????????????????????????????
     */
    Observable<DataChanged> observeDataSetChanged() {
        return txRepo.observeDataSetChanged();
    }

    public Flowable<UserAndTx> observeSellTxDetail(String chainID, String txID) {
        return txRepo.observeSellTxDetail(chainID, txID);
    }

    public Flowable<List<TxQueueAndStatus>> observeCommunityTxQueue(String chainID) {
        String userPk = MainApplication.getInstance().getPublicKey();
        return txQueueRepo.observeCommunityTxQueue(chainID, userPk);
    }

    void deleteTxQueue(TxQueue tx) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Void>) emitter -> {
            try {
                txQueueRepo.deleteQueue(tx);
                txRepo.deleteUnsentTx(tx.queueID);
                ChatViewModel.syncSendMessageTask(getApplication(), tx, QueueOperation.DELETE);
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
     * ????????????
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

    public Observable<List<UserAndTx>> observeWalletTransactions(String chainID) {
        return txRepo.observeWalletTransactions(chainID);
    }
}
