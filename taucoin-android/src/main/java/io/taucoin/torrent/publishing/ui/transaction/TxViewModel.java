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
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.model.data.message.SellTxContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxContent;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxQueueRepository;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.MoneyValueFilter;
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

import static io.taucoin.torrent.publishing.core.model.data.message.TxType.NOTE_TX;
import static io.taucoin.torrent.publishing.core.model.data.message.TxType.SELL_TX;
import static io.taucoin.torrent.publishing.core.model.data.message.TxType.TRUST_TX;
import static io.taucoin.torrent.publishing.core.model.data.message.TxType.WIRING_TX;

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
     * 获取社区链交易的被观察者
     * @return 被观察者
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
    }

    public MutableLiveData<Result> getAirdropState(){
        return airdropState;
    }

    /**
     * 添加新的转账交易
     * @param tx 根据用户输入构建的用户数据
     */
    void addWringTransaction(TxQueue tx, boolean isAdd) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            String result = addWringTransactionTask(tx, isAdd);
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> addState.postValue(state));
        disposables.add(disposable);
    }

    private String addWringTransactionTask(TxQueue tx, boolean isAdd) {
        if (isAdd) {
            txQueueRepo.addQueue(tx);
            daemon.updateTxQueue(tx.chainID);
            return "";
        } else {
            // 重发交易队列
            txQueueRepo.updateQueue(tx);
            return daemon.resendTxQueue(tx);
        }
    }

    /**
     * 添加新的交易
     * @param tx 根据用户输入构建的用户数据
     */
    void addTransaction(Tx tx) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            String result = createTransaction(tx);
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> addState.postValue(state));
        disposables.add(disposable);
    }

    private String createTransaction(Tx tx) {
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
            Account account = daemon.getAccountInfo(chainID, currentUser.publicKey);
            if (null == account) {
                result = getApplication().getString(R.string.tx_error_send_failed);
                return result;
            }
            long timestamp = daemon.getSessionTime();
            byte[] memo = Utils.textStringToBytes(tx.memo);
            byte[] txEncoded = null;
            switch (TxType.valueOf(tx.txType)) {
                case NOTE_TX:
                case WIRING_TX:
                case TRUST_TX:
                    TxContent txContent = new TxContent(tx.txType, memo);
                    txEncoded = txContent.getEncoded();
                    break;
                case SELL_TX:
                    SellTxContent sellTxContent = new SellTxContent(tx.coinName, tx.quantity,
                            tx.link, tx.location, tx.memo);
                    txEncoded = sellTxContent.getEncoded();
                    break;
                default:
                    break;
            }
            if (null == txEncoded) {
                return result;
            }
            Transaction transaction = new Transaction(chainID, 0, timestamp, senderPk, tx.fee, txEncoded);
            transaction.sign(ByteUtil.toHexString(senderPk), ByteUtil.toHexString(secretKey));
            boolean isSubmitSuccess = daemon.submitTransaction(transaction);
            if (!isSubmitSuccess) {
                result = getApplication().getString(R.string.tx_error_send_failed);
                return result;
            }
            // 保存交易数据到本地数据库
            tx.txID = transaction.getTxID().to_hex();
            tx.timestamp = timestamp;
            tx.senderPk = currentUser.publicKey;
            txRepo.addTransaction(tx);
            logger.debug("createTransaction txID::{}, senderPk::{}, receiverPk::{}, memo::{}",
                    tx.txID, tx.senderPk, tx.receiverPk, tx.memo);
            addUserInfoToLocal(tx);
            addMemberInfoToLocal(tx);
            settingsRepo.lastTxFee(tx.chainID, tx.fee);
        } catch (Exception e) {
            result = e.getMessage();
            logger.debug("Error adding transaction::{}", result);
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

    boolean validateTx(TxQueue tx) {
        byte[] chainID = ChainIDUtil.encode(tx.chainID);
        String senderPk = MainApplication.getInstance().getPublicKey();
        Account account = daemon.getAccountInfo(chainID, senderPk);
        long balance = account != null ? account.getBalance() : 0;
        if (StringUtil.isEmpty(tx.receiverPk) ||
                ByteUtil.toByte(tx.receiverPk).length != Ed25519.PUBLIC_KEY_SIZE) {
            ToastUtils.showShortToast(R.string.tx_error_invalid_pk);
            return false;
        } else if (tx.amount < 0) {
            ToastUtils.showShortToast(R.string.tx_error_invalid_amount);
            return false;
        } else if (tx.fee < 0) {
            ToastUtils.showShortToast(R.string.tx_error_invalid_free);
            return false;
        } else if (tx.amount > balance || tx.amount + tx.fee > balance) {
            ToastUtils.showShortToast(R.string.tx_error_no_enough_coins);
            return false;
        }
        return true;
    }

    /**
     * 验证交易
     * @param tx 交易数据
     */
    boolean validateTx(Tx tx) {
        if (null == tx) {
            return false;
        }
        long msgType = tx.txType;
        byte[] chainID = ChainIDUtil.encode(tx.chainID);
        String senderPk = MainApplication.getInstance().getPublicKey();
        Account account = daemon.getAccountInfo(chainID, senderPk);
        long balance = account != null ? account.getBalance() : 0;
        if (msgType == NOTE_TX.getType()) {
            if (StringUtil.isEmpty(tx.memo)) {
                ToastUtils.showShortToast(R.string.tx_error_invalid_message);
                return false;
            } else if (tx.fee > balance) {
                ToastUtils.showShortToast(R.string.tx_error_no_enough_coins_for_fee);
                return false;
            } else if (tx.fee < 0) {
                ToastUtils.showShortToast(R.string.tx_error_invalid_free);
                return false;
            }
        } else if (msgType == SELL_TX.getType()) {
            if (StringUtil.isEmpty(tx.coinName)) {
                ToastUtils.showShortToast(R.string.tx_error_invalid_coin_name);
                return false;
            } else if (tx.fee < 0) {
                ToastUtils.showShortToast(R.string.tx_error_invalid_free);
                return false;
            }
        } else if (msgType == TRUST_TX.getType()) {
            if (tx.fee < 0) {
                ToastUtils.showShortToast(R.string.tx_error_invalid_free);
                return false;
            }
        }
        return true;
    }

    public long getLastTxFee(String chainID){
        return settingsRepo.lastTxFee(chainID);
    }

    /**
     * 显示编辑交易费的对话框
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
                FmtMicrometer.fmtFeeValue(medianFee)));
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
     * 0、默认为最小交易费
     * 1、从交易池中获取前10名交易费的中位数
     * 2、如果交易池返回小于等于0，用上次交易用的交易费
     * @param chainID 交易所属的社区chainID
     */
    public long getTxFee(String chainID) {
        long free = Constants.MIN_FEE.longValue();
        long medianFree = daemon.getMedianTxFree(chainID);
        if (medianFree > 0) {
            free = medianFree;
        } else {
            long lastTxFee = getLastTxFee(chainID);
            if (lastTxFee > 0) {
                free = lastTxFee;
            }
        }
        return free;
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
                        logger.debug("airdropToFriends chainID::{}, friend::{}, amount::{}, fee::{}",
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
        TxQueue tx = new TxQueue(chainID, senderPk, friendPk, amount, fee, memo);
        return addWringTransactionTask(tx, true);
    }


    /**
     * 加载交易Notes分页数据
     * @param filterItem 用户过滤条件
     * @param chainID 社区链ID
     * @param pos 分页位置
     * @param initSize 刷新时第一页数据大小
     */
    void loadNotesData(int filterItem, String chainID, int pos, int initSize) {
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
                if (filterItem == R.string.community_view_onchain_notes) {
                    txs = txRepo.loadOnChainNotesData(chainID, pos, pageSize);
                } else if (filterItem == R.string.community_view_offchain_notes) {
                    txs = txRepo.loadOffChainNotesData(chainID, pos, pageSize);
                } else {
                    txs = txRepo.loadAllNotesData(chainID, pos, pageSize);
                }
                long getMessagesTime = System.currentTimeMillis();
                logger.trace("loadNotesData filterItem::{}, pos::{}, pageSize::{}, messages.size::{}",
                        application.getString(filterItem), pos, pageSize, txs.size());
                logger.trace("loadNotesData getMessagesTime::{}", getMessagesTime - startTime);
                Collections.reverse(txs);
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
     * 加载交易Market分页数据
     * @param filterItem 用户过滤条件
     * @param chainID 社区链ID
     * @param pos 分页位置
     * @param initSize 刷新时第一页数据大小
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
                } else {
                    txs = txRepo.loadAllMarketData(chainID, pos, pageSize);
                }
                logger.trace("loadMarketData filterItem::{}, pos::{}, pageSize::{}, messages.size::{}",
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
     * 加载转账交易分页数据
     * @param onlyWring 仅显示转账交易
     * @param chainID 社区链ID
     * @param pos 分页位置
     * @param initSize 刷新时第一页数据大小
     */
    void loadChainTxsData(boolean onlyWring, String chainID, int pos, int initSize) {
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
                if (!onlyWring) {
                    txs = txRepo.loadOnChainAllTxs(chainID, pos, pageSize);
                } else {
                    txs = txRepo.loadAllWiringTxs(chainID, pos, pageSize);
                }
                logger.trace("loadChainTxsData onlyWring::{}, pos::{}, pageSize::{}, messages.size::{}",
                        onlyWring, pos, pageSize, txs.size());
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
     * 加载交易固定数据
     * @param currentTab 用户选择的tab页
     * @param chainID 社区链ID
     */
    void loadPinnedTxsData(int currentTab, String chainID) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<List<UserAndTx>>) emitter -> {
            List<UserAndTx> txs = new ArrayList<>();
            try {
                txs = txRepo.queryCommunityPinnedTxs(chainID, currentTab);
                logger.trace("loadPinnedTxsData, currentTab::{}, messages.size::{}",
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
     * 加载Trust交易分页数据
     * @param chainID 社区链ID
     * @param trustPk trust用户
     * @param pos 分页位置
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
                logger.trace("loadTrustTxsData pos::{}, pageSize::{}, messages.size::{}",
                        pos, pageSize, trustTxs.size());
                logger.trace("loadTrustTxsData getMessagesTime::{}", getMessagesTime - startTime);
                Collections.reverse(trustTxs);
                long endTime = System.currentTimeMillis();
                logger.trace("loadTrustTxsData reverseTime Time::{}", endTime - getMessagesTime);
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
     * 观察社区的消息的变化
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
}
