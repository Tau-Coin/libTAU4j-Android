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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
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
import io.taucoin.torrent.publishing.core.model.data.message.TxContent;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.MoneyValueFilter;
import io.taucoin.torrent.publishing.ui.community.CommunityTabs;
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
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.EditFeeDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;

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
    private TauDaemon daemon;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<List<UserAndTx>> chainTxs = new MutableLiveData<>();
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

    public void setAddState(MutableLiveData<String> addState) {
        this.addState = addState;
    }

    /**
     * 获取社区链交易的被观察者
     * @return 被观察者
     */
    public MutableLiveData<List<UserAndTx>> observerChainTxs() {
        return chainTxs;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
        if(editFeeDialog != null){
            editFeeDialog.closeDialog();
        }
    }

    public MutableLiveData<Result> getAirdropState(){
        return airdropState;
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
            byte[] receiverPk;
            if (tx.txType == TxType.WRING_TX.getType()) {
                receiverPk = ByteUtil.toByte(tx.receiverPk);
            } else {
                // Note交易无接受者时，默认为senderPk
                receiverPk = senderPk;
            }
            TxContent txContent = new TxContent(tx.txType, Utils.textStringToBytes(tx.memo));
            long nonce = account.getNonce() + 1;
            Transaction transaction = new Transaction(chainID, 0, timestamp, senderPk, receiverPk,
                    nonce, tx.amount, tx.fee, txContent.getEncoded());
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
            tx.nonce = account.getNonce();
            txRepo.addTransaction(tx);
            logger.debug("createTransaction txID::{}, senderPk::{}, receiverPk::{}, nonce::{}, memo::{}",
                    tx.txID, tx.senderPk, tx.receiverPk, tx.nonce, tx.memo);
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
        if (tx.txType == TxType.WRING_TX.getType()) {
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
        if (txType == TxType.WRING_TX.getType() && StringUtil.isNotEquals(tx.senderPk, tx.receiverPk)) {
            Member receiverMember = memberRepo.getMemberByChainIDAndPk(tx.chainID, tx.receiverPk);
            if (null == receiverMember) {
                receiverMember = new Member(tx.chainID, tx.receiverPk);
                memberRepo.addMember(receiverMember);
                logger.info("addMemberInfoToLocal, receiverPk::{}", tx.receiverPk);
            }
        }
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
        if (msgType == TxType.CHAIN_NOTE.getType()) {
            if (StringUtil.isEmpty(tx.memo)) {
                ToastUtils.showShortToast(R.string.tx_error_invalid_message);
                return false;
            } else if (tx.fee > balance) {
                ToastUtils.showShortToast(R.string.tx_error_no_enough_coins_for_fee);
                return false;
            } else if (tx.fee <= 0) {
                ToastUtils.showShortToast(R.string.tx_error_invalid_free);
                return false;
            }
        } else if (msgType == TxType.WRING_TX.getType()) {
            if (StringUtil.isEmpty(tx.receiverPk) ||
                    ByteUtil.toByte(tx.receiverPk).length != Ed25519.PUBLIC_KEY_SIZE) {
                ToastUtils.showShortToast(R.string.tx_error_invalid_pk);
                return false;
            } else if (tx.amount <= 0) {
                ToastUtils.showShortToast(R.string.tx_error_invalid_amount);
                return false;
            } else if (tx.fee <= 0) {
                ToastUtils.showShortToast(R.string.tx_error_invalid_free);
                return false;
            } else if (tx.amount > balance || tx.amount + tx.fee > balance) {
                ToastUtils.showShortToast(R.string.tx_error_no_enough_coins);
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
        editFeeBinding.tvMedianFee.setText(activity.getString(R.string.tx_median_fee_tips, medianFee));
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
     * 空投币给朋友
     * @param chainID
     * @param friendPks
     */
    public void airdropToFriends(String chainID, Map<String, String> friendPks, String fee) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            try {
//                String senderPk = MainApplication.getInstance().getPublicKey();
//                long balance = daemon.getUserBalance(chainID, senderPk);
//                long airdropCoin = Constants.AIRDROP_COIN.longValue();
//                long totalPay = (airdropCoin + medianFee) * friendPks.size();
//                if(totalPay > balance){
//                    result = application.getString(R.string.tx_error_no_enough_coins_for_airdrop);
//                }else{
                    Set<String> friends = friendPks.keySet();
                    for (String friend : friends) {
                        String amount = friendPks.get(friend);
                        logger.debug("airdropToFriends chainID::{}, friend::{}, amount::{}, fee::{}",
                                chainID, friend, amount, fee);
                        String airdropResult = airdropToFriend(chainID, friend,
                                FmtMicrometer.fmtTxLongValue(amount),
                                FmtMicrometer.fmtTxLongValue(fee));
                        if (StringUtil.isNotEmpty(airdropResult)) {
                            result.setFailMsg(airdropResult);
                            break;
                        }
                    }
//                }
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
     * 空投币给朋友
     * @param chainID
     * @param friendPk
     */
    public String airdropToFriend(String chainID, String friendPk, long amount, long fee) {
        Tx tx = new Tx(chainID, friendPk, amount, fee, TxType.WRING_TX.getType(),
                getApplication().getString(R.string.tx_memo_airdrop));
        return createTransaction(tx);
    }

    /**
     * 加载交易分页数据
     * @param currentTab 用户选择的tab页
     * @param chainID 社区链ID
     * @param pos 分页位置
     */
    void loadTxsData(int currentTab, String chainID, int pos) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<List<UserAndTx>>) emitter -> {
            List<UserAndTx> txs = new ArrayList<>();
            try {
                int txType;
                if (currentTab == CommunityTabs.ON_CHAIN_NOTE.getIndex()) {
                    txType = TxType.CHAIN_NOTE.getType();
                } else if (currentTab == CommunityTabs.WRING_TX.getIndex()) {
                    txType = TxType.WRING_TX.getType();
                } else {
                    txType = -1;
                }
                long startTime = System.currentTimeMillis();
                int pageSize = pos == 0 ? Page.PAGE_SIZE * 2 : Page.PAGE_SIZE;
                txs = txRepo.queryCommunityTxs(chainID, txType, pos, pageSize);
                long getMessagesTime = System.currentTimeMillis();
                logger.trace("loadTxsData pos::{}, pageSize::{}, messages.size::{}",
                        pos, pageSize, txs.size());
                logger.trace("loadTxsData getMessagesTime::{}", getMessagesTime - startTime);
                Collections.reverse(txs);
                long endTime = System.currentTimeMillis();
                logger.trace("loadTxsData reverseTime Time::{}", endTime - getMessagesTime);
            } catch (Exception e) {
                logger.error("loadTxsData error::", e);
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
     * 观察社区的消息的变化
     */
    Observable<DataChanged> observeDataSetChanged() {
        return txRepo.observeDataSetChanged();
    }
}
