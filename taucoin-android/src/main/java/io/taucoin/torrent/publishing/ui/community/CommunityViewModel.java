package io.taucoin.torrent.publishing.ui.community;

import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.view.LayoutInflater;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.libTAU4j.Account;
import org.libTAU4j.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
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
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.TauListenHandler;
import io.taucoin.torrent.publishing.core.model.data.AccessList;
import io.taucoin.torrent.publishing.core.model.data.BlockAndTx;
import io.taucoin.torrent.publishing.core.model.data.BlockStatistics;
import io.taucoin.torrent.publishing.core.model.data.ChainStatus;
import io.taucoin.torrent.publishing.core.model.data.DataChanged;
import io.taucoin.torrent.publishing.core.model.data.DrawBean;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndMember;
import io.taucoin.torrent.publishing.core.model.data.MemberAndFriend;
import io.taucoin.torrent.publishing.core.model.data.MemberAndTime;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.model.data.Statistics;
import io.taucoin.torrent.publishing.core.model.data.Result;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.model.data.message.AirdropStatus;
import io.taucoin.torrent.publishing.core.model.data.AirdropHistory;
import io.taucoin.torrent.publishing.core.model.data.message.QueueOperation;
import io.taucoin.torrent.publishing.core.model.data.message.SellTxContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.BlockInfo;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxLog;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.BlockRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxQueueRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.utils.BitmapUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.LinkUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.torrent.publishing.databinding.BlacklistDialogBinding;
import io.taucoin.torrent.publishing.databinding.ExternalAirdropLinkDialogBinding;
import io.taucoin.torrent.publishing.ui.chat.ChatViewModel;
import io.taucoin.torrent.publishing.ui.constant.Page;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.transaction.TxViewModel;

/**
 * Community?????????ViewModel
 */
public class CommunityViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("CommunityViewModel");
    private CommunityRepository communityRepo;
    private MemberRepository memberRepo;
    private BlockRepository blockRepo;
    private TxRepository txRepo;
    private TxQueueRepository txQueueRepo;
    private UserRepository userRepo;
    private TauDaemon daemon;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<Result> addCommunityState = new MutableLiveData<>();
    private MutableLiveData<Boolean> setBlacklistState = new MutableLiveData<>();
    private MutableLiveData<Boolean> airdropResult = new MutableLiveData<>();
    private MutableLiveData<Result> joinedResult = new MutableLiveData<>();
    private MutableLiveData<List<Community>> blackList = new MutableLiveData<>();
    private MutableLiveData<List<CommunityAndMember>> joinedList = new MutableLiveData<>();
    private MutableLiveData<List<Member>> joinedUnexpiredList = new MutableLiveData<>();
    private MutableLiveData<List<MemberAndTime>> joinedCommunity = new MutableLiveData<>();
    private MutableLiveData<Bitmap> qrBitmap = new MutableLiveData<>();
    private MutableLiveData<UserAndFriend> largestCoinHolder = new MutableLiveData<>();
    private MutableLiveData<List<BlockAndTx>> chainBlocks = new MutableLiveData<>();
    private Disposable clearDisposable;
    private TxViewModel txViewModel;

    public CommunityViewModel(@NonNull Application application) {
        super(application);
        communityRepo = RepositoryHelper.getCommunityRepository(getApplication());
        memberRepo = RepositoryHelper.getMemberRepository(getApplication());
        userRepo = RepositoryHelper.getUserRepository(getApplication());
        txQueueRepo = RepositoryHelper.getTxQueueRepository(getApplication());
        blockRepo = RepositoryHelper.getBlockRepository(getApplication());
        txRepo = RepositoryHelper.getTxRepository(getApplication());
        daemon = TauDaemon.getInstance(getApplication());
        txViewModel = new TxViewModel(getApplication());
    }

    public void observeNeedStartDaemon () {
        disposables.add(daemon.observeNeedStartDaemon()
                .subscribeOn(Schedulers.io())
                .filter((needStart) -> needStart)
                .subscribe((needStart) -> daemon.start()));
    }

    public MutableLiveData<List<BlockAndTx>> observerChainBlocks() {
        return chainBlocks;
    }

    public MutableLiveData<Boolean> getAirdropResult() {
        return airdropResult;
    }

    MutableLiveData<List<CommunityAndMember>> getJoinedList() {
        return joinedList;
    }

    public MutableLiveData<List<Member>> getJoinedUnexpiredList() {
        return joinedUnexpiredList;
    }

    public MutableLiveData<List<MemberAndTime>> getJoinedCommunity() {
        return joinedCommunity;
    }

    /**
     * ???????????????????????????????????????
     * @return ????????????
     */
    public LiveData<Result> getAddCommunityState() {
        return addCommunityState;
    }

    /**
     * ??????????????????????????????????????????
     * @return ????????????
     */
    LiveData<Boolean> getSetBlacklistState() {
        return setBlacklistState;
    }

    public LiveData<Result> getJoinedResult() {
        return joinedResult;
    }

    /**
     * ????????????????????????????????????
     * @return ????????????
     */
    public MutableLiveData<List<Community>> getBlackList() {
        return blackList;
    }

    /**
     * ????????????????????????
     */
    public MutableLiveData<Bitmap> getQRBitmap() {
        return qrBitmap;
    }

    /**
     * ??????????????????????????????
     */
    public MutableLiveData<UserAndFriend> getLargestCoinHolder() {
        return largestCoinHolder;
    }

    /**
     * ??????????????????????????????
     * @param chainID
     * @param chainUrl
     */
    public void addCommunity(String chainID, LinkUtil.Link chainUrl) {
        addCommunity(null, chainID, chainUrl);
    }

    /**
     * ??????????????????????????????
     * @param airdropPeer
     * @param chainID
     * @param link
     */
    public void addCommunity(String airdropPeer, String chainID, LinkUtil.Link link) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            String communityName = ChainIDUtil.getName(chainID);
            if (StringUtil.isNotEmpty(communityName)) {
                String userPk = MainApplication.getInstance().getPublicKey();

                Set<String> peers = new HashSet<>();
                peers.add(link.getPeer());
                boolean success = false;
                Community community = communityRepo.getCommunityByChainID(chainID);
                // ?????????peer??????publish???????????????
                daemon.requestChainData(link.getPeer(), chainID);

                if (null == community) {
                    // ??????follow community
                    success = daemon.followChain(chainID, peers);
                    logger.info("addCommunity chainID::{}, peers::{}, success::{}", chainID,
                            peers.size(), success);
                    if (success) {
                        community = new Community(chainID, communityName);
                        communityRepo.addCommunity(community);
                        // ????????????????????????
                        daemon.updateUserInfo(userRepo.getCurrentUser());
                        addMemberInfoToLocal(chainID, userPk);
                    }
                } else {
                    // ???????????????banned
                    if (community.isBanned) {
                        success = daemon.followChain(chainID, peers);
                        logger.info("addCommunity isBanned::true, chainID::{}, peers::{}, success::{}",
                                chainID, peers.size(), success);
                        if (success) {
                            community.isBanned = false;
                            communityRepo.updateCommunity(community);
                            addMemberInfoToLocal(chainID, userPk);
                        }
                    } else {
                        success = true;
                        // ??????banned????????????????????????join, ???join?????????follow
                        Member member = memberRepo.getMemberByChainIDAndPk(chainID, userPk);
                        logger.info("addCommunity isBanned::false, chainID::{}, peers::{}, joined::{}",
                                chainID, peers.size(), member != null);
                        if (null == member) {
                            daemon.followChain(chainID, peers);
                            addMemberInfoToLocal(chainID, userPk);
                        } else {
                            // ??????libTAU???????????????chain
                            daemon.followChain(chainID, peers);
                            daemon.addNewBootstrapPeers(chainID, peers);
                        }
                    }
                }
                if (peers.size() > 0) {
                    for (String peer : peers) {
                        addUserInfoToLocal(peer);
                        addMemberInfoToLocal(chainID, peer);
                    }
                }
                result.setSuccess(success);
                result.setMsg(chainID);
            } else {
                result.setSuccess(false);
            }
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
                    addCommunityState.postValue(state);
                });
        disposables.add(disposable);
    }

    /**
     * ???????????????????????????
     * @param publicKey ????????????
     */
    private void addUserInfoToLocal(String publicKey) {
        User receiverUser = userRepo.getUserByPublicKey(publicKey);
        if (null == receiverUser) {
            receiverUser = new User(publicKey);
            userRepo.addUser(receiverUser);
            logger.info("addUserInfoToLocal, publicKey::{}", publicKey);
        }
    }

    /**
     * ????????????????????????
     * @param chainID ???ID
     * @param publicKey ????????????
     */
    private void addMemberInfoToLocal(String chainID, String publicKey) {
        Member member = memberRepo.getMemberByChainIDAndPk(chainID, publicKey);
        if (null == member) {
            member = new Member(chainID, publicKey);
            memberRepo.addMember(member);
            logger.info("addMemberInfoToLocal, publicKey::{}", publicKey);
        }
    }

    /**
     * ??????????????????????????????
     * @param community ????????????
     * @param selectedMap ???????????????????????????
     */
    void addCommunity(@NonNull Community community, Map<String, String> selectedMap) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            // TauController:??????Community??????
            Result result = createCommunity(community, selectedMap);
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> addCommunityState.postValue(state));
        disposables.add(disposable);
    }

    private Result createCommunity(Community community, Map<String, String> selectedMap) {
        Result result = new Result();
        try {
            // chainID?????????????????????????????????
            if (StringUtil.isEmpty(community.chainID)) {
                community.chainID = createNewChainID(community.communityName);
            }
            if (StringUtil.isEmpty(community.chainID)) {
                result.setFailMsg(getApplication().getString(R.string.community_creation_failed));
                return result;
            }
            result.setMsg(community.chainID);
            Set<Account> accounts = new HashSet<>();
            if (selectedMap != null) {
                Collection<String> keys = selectedMap.keySet();
                for (String key : keys) {
                    long balance = FmtMicrometer.fmtTxLongValue(selectedMap.get(key));
                    Account account = new Account(ByteUtil.toByte(key), balance, 0);
                    accounts.add(account);
                }
            }
            // ???????????????????????????????????????
            User currentUser = userRepo.getCurrentUser();
            byte[] chainID = ChainIDUtil.encode(community.chainID);
            boolean isCreateSuccess = daemon.createNewCommunity(chainID, accounts);
            if (!isCreateSuccess) {
                result.setFailMsg(getApplication().getString(R.string.community_creation_failed));
                return result;
            }

            communityRepo.addCommunity(community);
            // ????????????????????????
            daemon.updateUserInfo(currentUser);
            logger.info("Add community to database: communityName={}, chainID={}",
                    community.communityName, community.chainID);

            Account account = daemon.getAccountInfo(chainID, currentUser.publicKey);
            Member member;
            if (account != null) {
                member = new Member(community.chainID, currentUser.publicKey,
                        account.getBalance(), account.getNonce());
            } else {
                // ?????????libTAU??????????????????????????????
                member = new Member(community.chainID, currentUser.publicKey, 0, 0);
            }
            memberRepo.addMember(member);

            // ??????????????????Sell??????
            String coinName = ChainIDUtil.getCoinName(community.chainID);
            String description = getApplication().getString(R.string.tx_community_creator_selling, coinName);
            SellTxContent sellContent = new SellTxContent(coinName, 0, null, null, description);
            TxQueue sellTxQueue = new TxQueue(community.chainID, currentUser.publicKey, currentUser.publicKey, 0L,
                    Constants.NEWS_MIN_FEE.longValue(), TxType.SELL_TX, sellContent.getEncoded());
            txViewModel.addTransactionTask(sellTxQueue, null, DateUtil.getMillisTime());

            // ????????????
            if (accounts.size() > 0) {
                String memo = getApplication().getString(R.string.community_added_members);
                TxContent txContent = new TxContent(TxType.WIRING_TX.getType(), memo);
                byte[] content = txContent.getEncoded();
                for (Account acc : accounts) {
                    String key = ByteUtil.toHexString(acc.getPeer());
                    long amount = acc.getBalance();
                    TxQueue txQueue = new TxQueue(community.chainID, currentUser.publicKey, key,
                            amount, 0L, TxType.WIRING_TX, content);
                    ChatViewModel.syncSendMessageTask(getApplication(), txQueue, QueueOperation.ON_CHAIN);
                }
            }
        } catch (Exception e) {
            result.setFailMsg(e.getMessage());
        }
        return result;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
        if (clearDisposable != null && !clearDisposable.isDisposed()) {
            clearDisposable.dispose();
        }
    }

    /**
     * ??????Community?????????????????????
     * @param community view????????????
     * @return ??????????????????
     */
    boolean validateCommunity(@NonNull Community community, Map<String, String> selectedMap) {
        String communityName = community.communityName;
        if (StringUtil.isEmpty(communityName)) {
            ToastUtils.showLongToast(R.string.error_community_name_empty);
            return false;
        }
        byte[] nameBytes = Utils.textStringToBytes(communityName);
        if (nameBytes != null && nameBytes.length > Constants.MAX_COMMUNITY_NAME_LENGTH) {
            ToastUtils.showLongToast(R.string.error_community_name_too_long);
            return false;
        }
        BigInteger totalCoins = BigInteger.ZERO;
        if (selectedMap != null) {
            Collection<String> values = selectedMap.values();
            for (String value : values) {
                long coin = FmtMicrometer.fmtTxLongValue(value);
                if (coin == 0) {
                    ToastUtils.showLongToast(R.string.error_airdrop_coins_empty);
                    return false;
                }
                totalCoins = totalCoins.add(BigInteger.valueOf(coin));
            }
            if (totalCoins.compareTo(Constants.TOTAL_COIN) > 0) {
                ToastUtils.showLongToast(R.string.error_airdrop_coins_too_greater);
                return false;
            }
        }
        return true;
    }

    /**
     * ?????????????????????????????????
     * @param chainID ??????chainID
     */
    CommonDialog showBanCommunityTipsDialog(FragmentActivity activity, String chainID) {
        BlacklistDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.blacklist_dialog, null, false);
        String blacklistTip = activity.getString(R.string.community_blacklist_tip, ChainIDUtil.getName(chainID));
        dialogBinding.tvMsg.setText(Html.fromHtml(blacklistTip));
        CommonDialog blacklistDialog = new CommonDialog.Builder(activity)
                .setContentView(dialogBinding.getRoot())
                .enableWarpWidth(true)
                .setCanceledOnTouchOutside(false)
                .create();
        blacklistDialog.show();
        dialogBinding.tvSubmit.setOnClickListener(v -> {
            blacklistDialog.closeDialog();
            setCommunityBlacklist(chainID, true);
        });
        dialogBinding.ivClose.setOnClickListener(v -> {
            blacklistDialog.closeDialog();
        });
        return blacklistDialog;
    }

    /**
     * ?????????????????????
     * @param chainID ??????chainID
     * @param blacklist ?????????????????????
     */
    public void setCommunityBlacklist(String chainID, boolean blacklist) {
        setCommunityBlacklistTask(chainID, blacklist);
    }

    private void setCommunityBlacklistTask(String chainID, boolean blacklist) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            boolean success;
            if (blacklist) {
                success = daemon.unfollowChain(chainID);
            } else {
                List<String> list = queryCommunityMembersLimit(chainID, Constants.CHAIN_LINK_BS_LIMIT);
                Set<String> peers = new HashSet<>(list);
                success = daemon.followChain(chainID, peers);
            }
            if (success) {
                Community community = communityRepo.getCommunityByChainID(chainID);
                if (community != null) {
                    community.isBanned = blacklist;
                    communityRepo.updateCommunity(community);
                }
            }
            emitter.onNext(success);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> setBlacklistState.postValue(state));
        disposables.add(disposable);
    }

    /**
     * ?????????????????????????????????
     */
    public void getCommunitiesInBlacklist() {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<List<Community>>) emitter -> {
            List<Community> list = communityRepo.getCommunitiesInBlacklist();
            emitter.onNext(list);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> blackList.postValue(list));
        disposables.add(disposable);
    }

    /**
     * ?????????????????????????????????
     */
    void getJoinedCommunityList() {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<List<CommunityAndMember>>) emitter -> {
            List<CommunityAndMember> list = communityRepo.getJoinedCommunityList();
            emitter.onNext(list);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> joinedList.postValue(list));
        disposables.add(disposable);
    }

    /**
     * ?????????????????????????????????
     */
    public void getJoinedUnexpiredCommunityList(String userPk) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<List<Member>>) emitter -> {
            List<Member> list = memberRepo.getJoinedUnexpiredCommunityList(userPk);
            emitter.onNext(list);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> joinedUnexpiredList.postValue(list));
        disposables.add(disposable);
    }

    /**
     * ?????????????????????????????????
     */
    public void getJoinedCommunityList(String userPk) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<List<MemberAndTime>>) emitter -> {
            List<MemberAndTime> list = memberRepo.getJoinedCommunityList(userPk);
            emitter.onNext(list);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(list -> joinedCommunity.postValue(list));
        disposables.add(disposable);
    }

    /**
     * ????????????????????????
     * @param chainID
     * @return
     */
    public Flowable<List<MemberAndUser>> observeCommunityMembers(String chainID) {
        return memberRepo.observeCommunityMembers(chainID);
    }

    /**
     * ????????????????????????
     */
    public Flowable<List<CommunityAndMember>> observeCommunities() {
        return communityRepo.observeCommunities();
    }

    /**
     * ????????????????????????
     * @param chainID ???ID
     * @return Flowable<Member>
     */
    public Flowable<Member> observeCommunityAirdropDetail(String chainID) {
        return memberRepo.observeCommunityAirdropDetail(chainID);
    }

    public Flowable<Integer> observeAirdropCountOnChain(String chainID, String senderPk, long currentTime) {
        return txQueueRepo.observeAirdropCountOnChain(chainID, senderPk, currentTime);
    }

    public Flowable<List<AirdropHistory>> observeAirdropHistoryOnChain(String chainID, String senderPk, long currentTime) {
        return txQueueRepo.observeAirdropHistoryOnChain(chainID, senderPk, currentTime);
    }

    /**
     * ??????????????????
     * @param chainID
     * @return DataSource.Factory
     */
    public DataSource.Factory<Integer, MemberAndFriend> queryCommunityMembers(String chainID) {
        return memberRepo.queryCommunityMembers(chainID);
    }

    /**
     * ???????????????????????????????????????
     * @param currentUserPk
     * @param memberPk
     */
    public Single<List<String>> getCommunityNumInCommon(String currentUserPk, String memberPk) {
        return memberRepo.getCommunityNumInCommon(currentUserPk, memberPk);
    }
    /**
     * ????????????limit?????????
     * @param chainID
     * @param limit
     */
    public Observable<List<String>> getCommunityMembersLimit(String chainID, int limit) {
        return Observable.create(emitter -> {
            List<String> list = queryCommunityMembersLimit(chainID, limit);
            if (null == list) {
                list = new ArrayList<>();
            }
            emitter.onNext(list);
            emitter.onComplete();
        });
    }

    public List<String> queryCommunityMembersLimit(String chainID, int limit) {
        return memberRepo.queryCommunityMembersLimit(chainID, limit);
    }

    /**
     * ????????????????????????
     * @param chainID
     */
    public Flowable<Statistics> getMembersStatistics(String chainID) {
        return memberRepo.getMembersStatistics(chainID);
    }

    /**
     * ????????????????????????
     * @param chainID
     */
    public Flowable<BlockStatistics> getBlocksStatistics(String chainID) {
        return memberRepo.getBlocksStatistics(chainID);
    }


    public Single<Community> getCommunityByChainIDSingle(String chainID) {
        return communityRepo.getCommunityByChainIDSingle(chainID);
    }

    public Single<Member> getMemberSingle(String chainID) {
        return communityRepo.getMemberSingle(chainID);
    }

    public Flowable<Community> observerCommunityByChainID(String chainID) {
        return communityRepo.observerCommunityByChainID(chainID);
    }

    Flowable<List<BlockInfo>> observerCommunityTopBlocks(String chainID, int num) {
        return communityRepo.observerCommunityTopBlocks(chainID, num);
    }

    LiveData<PagedList<MemberAndFriend>> observerCommunityMembers(String chainID) {
        return new LivePagedListBuilder<>(queryCommunityMembers(chainID),
                Page.getPageListConfig()).build();
    }

    /**
     * ?????????????????????????????????
     * @param chainID
     * @return
     */
    public Flowable<CommunityAndMember> observerCurrentMember(String chainID) {
        String publicKey = MainApplication.getInstance().getPublicKey();
        return communityRepo.observerCurrentMember(chainID, publicKey);
    }

    /**
     * ???????????????
     * @param context
     * @param QRContent ???????????????
     * @param chainID
     * @param showName
     */
    public void generateQRCode(Context context, String QRContent, String chainID, String showName) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Bitmap>) emitter -> {
            try {
                String qrName = UsersUtil.getQRCodeName(showName);
                int bgColor = Utils.getGroupColor(chainID);
                Bitmap logoBitmap = BitmapUtil.createLogoBitmap(bgColor, qrName);

                Resources resources = context.getApplicationContext().getResources();
                int heightPix = resources.getDimensionPixelSize(R.dimen.widget_size_480);
                int widthPix = heightPix;
                //Generate the QR Code.
                HashMap<EncodeHintType, Object> hints = new HashMap<>();
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
                hints.put(EncodeHintType.MARGIN, 0);
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                QRCodeWriter writer = new QRCodeWriter();
                Bitmap bitmap = writer.encode(QRContent, BarcodeFormat.QR_CODE, widthPix, heightPix,
                        hints, logoBitmap, context);

                // ?????????????????????
                Bitmap bgBitmap = BitmapFactory.decodeResource(context.getResources(),
                        R.mipmap.icon_community_qr_bg);
                bgBitmap.getWidth();
                DrawBean bean = new DrawBean();
                bean.setSize(resources.getDimensionPixelSize(R.dimen.widget_size_210));
                bean.setX(resources.getDimensionPixelSize(R.dimen.widget_size_60));
                bean.setY(resources.getDimensionPixelSize(R.dimen.widget_size_60));
                Bitmap lastBitmap = BitmapUtil.drawStyleQRcode(bgBitmap,
                        null, bitmap, bean);
                emitter.onNext(lastBitmap);
                // ?????????????????????pub?????????
                daemon.pubChainData(chainID);
            } catch (Exception e) {
                logger.error("generateTAUIDQRCode error ", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> qrBitmap.postValue(result));
        disposables.add(disposable);
    }

    /**
     * ?????????????????????ID
     * @param communityName ????????????
     * @return chainID
     */
    String createNewChainID(String communityName) {
        String type = "0000";
        return daemon.createNewChainID(type, communityName);
    }

    /**
     * ?????????????????????topNum?????????
     * @param chainID ???ID
     * @param topNum ????????????
     * @return Observable<List<Member>>
     */
    Flowable<List<Member>> observeChainTopCoinMembers(String chainID, int topNum) {
        return communityRepo.observeChainTopCoinMembers(chainID, topNum);
    }

    /**
     * ????????????????????????
     * @return Flowable<ChainStatus>
     */
    public Flowable<ChainStatus> observerChainStatus(String chainID) {
        return Flowable.create(emitter -> {
            while (!emitter.isCancelled() && !Thread.interrupted()) {
                try {
                    ChainStatus status = blockRepo.queryChainStatus(chainID);
                    if (status != null) {
                        emitter.onNext(status);
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {}
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST);
    }

    /**
     * ????????????
     * @param chainID ???ID
     */
    public void joinCommunity(String chainID) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            result.setMsg(chainID);

            String publicKey = MainApplication.getInstance().getPublicKey();
            Member member = memberRepo.getMemberByChainIDAndPk(chainID, publicKey);
            if (null == member) {
                List<String> list = queryCommunityMembersLimit(chainID, Constants.CHAIN_LINK_BS_LIMIT);
                Set<String> peers = new HashSet<>(list);
                boolean success = daemon.followChain(chainID, peers);
                if (success) {
                    addMemberInfoToLocal(chainID, publicKey);
                }
                result.setSuccess(success);
            } else {
                result.setSuccess(true);
            }
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> joinedResult.postValue(result));
        disposables.add(disposable);
    }

    public void getCommunityLargestCoinHolder(String chainID) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<UserAndFriend>) emitter -> {
            String coinHolder = memberRepo.getCommunityLargestCoinHolder(chainID);
            UserAndFriend user = userRepo.getFriend(coinHolder);
            emitter.onNext(user);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userAndFriend -> {
                    largestCoinHolder.postValue(userAndFriend);
                });
        disposables.add(disposable);
    }

    /**
     * ??????Airdrop Bot
     * @param chainID ???ID
     * @param members airdrop??????
     * @param coins ???????????????coins
     */
    public void setupAirdropBot(String chainID, int members, float coins) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            User user = userRepo.getCurrentUser();
            if (user != null) {
                Member member = memberRepo.getMemberByChainIDAndPk(chainID, user.publicKey);
                if (member != null) {
                    member.airdropStatus = AirdropStatus.ON.getStatus();
                    member.airdropMembers = members;
                    member.airdropCoins = FmtMicrometer.fmtTxLongValue(String.valueOf(coins));
                    member.airdropTime = DateUtil.getMillisTime();
                    memberRepo.updateMember(member);
                }
            }
            emitter.onNext(true);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userAndFriend -> {
                    airdropResult.postValue(userAndFriend);
                });
        disposables.add(disposable);
    }

    /**
     * ??????Airdrop Bot
     * @param chainID ???ID
     */
    public void deleteAirdropBot(String chainID) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            User user = userRepo.getCurrentUser();
            if (user != null) {
                Member member = memberRepo.getMemberByChainIDAndPk(chainID, user.publicKey);
                if (member != null) {
                    List<TxQueueAndStatus> queueList = txQueueRepo.getCommunityTxQueue(chainID, user.publicKey);
                    if (queueList != null && queueList.size() > 0) {
                        // ???????????????????????????
                        for (TxQueueAndStatus tx :queueList) {
                            if (tx.status < 0) {
                                txQueueRepo.deleteQueue(tx);
                            }
                        }
                    }
                    member.airdropStatus = AirdropStatus.SETUP.getStatus();
                    memberRepo.updateMember(member);
                }
            }
            emitter.onNext(true);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userAndFriend -> {
                    airdropResult.postValue(userAndFriend);
                });
        disposables.add(disposable);
    }

    /**
     * ????????????????????????
     * @return
     */
    public Flowable<List<BlockAndTx>> observeCommunitySyncStatus(String chainID) {
        return blockRepo.observeCommunitySyncStatus(chainID);
    }

    /**
     * ????????????????????????
     * @param chainID ?????????ID
     * @param pos ????????????
     * @param initSize ??????????????????????????????
     */
    public void loadBlocksData(String chainID, int pos, int initSize) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<List<BlockAndTx>>) emitter -> {
            List<BlockAndTx> blocks = new ArrayList<>();
            try {
                long startTime = System.currentTimeMillis();
                int pageSize = pos == 0 ? Page.PAGE_SIZE * 2 : Page.PAGE_SIZE;
                if (pos == 0 && initSize > pageSize) {
                    pageSize = initSize;
                }
                blocks = blockRepo.queryCommunityBlocks(chainID, pos, pageSize);
                long getMessagesTime = System.currentTimeMillis();
                logger.debug("loadBlocksData pos::{}, pageSize::{}, blocks.size::{}",
                        pos, pageSize, blocks.size());
                logger.debug("loadBlocksData getMessagesTime::{}", getMessagesTime - startTime);
                Collections.reverse(blocks);
                long endTime = System.currentTimeMillis();
                logger.debug("loadBlocksData reverseTime Time::{}", endTime - getMessagesTime);
            } catch (Exception e) {
                logger.error("loadBlocksData error::", e);
            }
            emitter.onNext(blocks);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(blocks -> {
                    chainBlocks.postValue(blocks);
                });
        disposables.add(disposable);
    }

    public Observable<DataChanged> observeBlocksSetChanged() {
        return blockRepo.observeDataSetChanged();
    }

    Observable<Long> observerCommunityMiningTime(String chainID) {
        return Observable.create(emitter -> {
            while (!emitter.isDisposed()) {
                long time = daemon.getMiningTime(ChainIDUtil.encode(chainID));
                logger.debug("Mining Time::{}, chainID::{}, isRunning::{}", time, chainID, daemon.isRunning());
                emitter.onNext(time);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            emitter.onComplete();
        });
    }

    Observable<List<String>> observerCommunityAccessList(String chainID, int type) {
        return Observable.create(emitter -> {
            while (!emitter.isDisposed()) {
                List<String> list = null;
                if (type == AccessListActivity.ACCESS_LIST_TYPE) {
                    list = daemon.getCommunityAccessList(ChainIDUtil.encode(chainID));
                }
                if (null == list) {
                    list = new ArrayList<>();
                }
                logger.debug("getCommunityAccessList::{}, type::{}, chainID::{}, isRunning::{}",
                        type, list.size(), chainID, daemon.isRunning());
                emitter.onNext(list);
                try {
                    if (list.size() > 0) {
                        Thread.sleep(10000);
                    } else {
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
            emitter.onComplete();
        });
    }

    Flowable<Float> reloadChain(String chainID) {
        return Flowable.create(emitter -> {
            Community community = communityRepo.getCommunityByChainID(chainID);
            if (community != null) {
                long headBlock = community.headBlock;
                long tailBlock = community.tailBlock;
                logger.debug("reloadChain chainID::{}, tailBlock::{}, headBlock::{}",
                        chainID, tailBlock, headBlock);
                Block block;
                for (long i = headBlock; i >= tailBlock; i--) {
                    if (emitter.isCancelled()) {
                        break;
                    }
                    block = daemon.getBlockByNumber(chainID, i);
                    if (null == block) {
                        break;
                    }
                    String blockHash = block.Hash();
                    List<BlockInfo> localBlocks = blockRepo.getBlocks(chainID, i);
                    if (localBlocks != null && localBlocks.size() > 0) {
                        for (BlockInfo localBlock : localBlocks) {
                            if (StringUtil.isEquals(blockHash, localBlock.blockHash)) {
                                if (localBlock.status != 1) {
                                    daemon.handleBlockData(block, TauListenHandler.BlockStatus.ON_CHAIN);
                                }
                            } else {
                                if (localBlock.status == 1) {
                                    List<Tx> txList = txRepo.getOnChainTxsByBlockHash(localBlock.blockHash);
                                    if (txList != null && txList.size() > 0) {
                                        for (Tx tx : txList) {
                                            tx.txStatus = 0;
                                            txRepo.updateTransaction(tx);
                                        }
                                    }
                                    localBlock.status = 0;
                                    blockRepo.updateBlock(localBlock);
                                }
                            }
                        }
                    } else {
                        daemon.handleBlockData(block, TauListenHandler.BlockStatus.ON_CHAIN);
                    }
                    if (headBlock > tailBlock && !emitter.isCancelled()) {
                        float progress = (headBlock - i) * 100f / (headBlock - tailBlock);
                        emitter.onNext(progress);
                        logger.debug("reloadChain chainID::{}, blockNumber::{}, localBlocks size::{}, progress::{}",
                                chainID, i, null == localBlocks ? 0 : localBlocks.size(), progress);
                    }
                }
            }
            if (!emitter.isCancelled()) {
                emitter.onNext(100f);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST);
    }

    public Observable<Integer> batchAddCommunities(String name, int num) {
        return Observable.create(emitter -> {
            if (num > 0) {
                for (int i = 0; i < num; i++) {
                    String communityName = name + (i + 1);
                    String chainID = createNewChainID(communityName);
                    Community community = new Community(chainID, communityName);
                    createCommunity(community, null);
                    emitter.onNext(i + 1);
                }
            }
            emitter.onComplete();
        });
    }

    /**
     * ????????????????????????
     */
    public Observable<List<TxLog>> observerTxLogs(String txID) {
        return txRepo.observerTxLogs(txID);
    }

    /**
     * ?????????????????????????????????
     */
    public void clearMsgUnread(String chainID) {
        markReadOrUnread(chainID, 0, false);
    }

    public void markReadOrUnread(String chainID, int status) {
        markReadOrUnread(chainID, status, true);
    }

    private void markReadOrUnread(String chainID, int status, boolean isAutoAdd) {
        if (clearDisposable != null && !clearDisposable.isDisposed()) {
            return;
        }
        clearDisposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            try {
                if (StringUtil.isNotEmpty(chainID)) {
                    String userPk = MainApplication.getInstance().getPublicKey();
                    Member member = memberRepo.getMemberByChainIDAndPk(chainID, userPk);
                    if (null == member) {
                        if (isAutoAdd) {
                            member = new Member(chainID, userPk);
                            member.msgUnread = status;
                            memberRepo.addMember(member);
                        }
                    } else {
                        logger.debug("markReadOrUnread::{}, status::{}", member.msgUnread, status);
                        if (member.msgUnread != status) {
                            member.msgUnread = status;
                            memberRepo.updateMember(member);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("markReadOrUnread error ", e);
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe();
    }

    public void topStickyOrRemove(String chainID, int top) {
        if (clearDisposable != null && !clearDisposable.isDisposed()) {
            return;
        }
        clearDisposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            try {
                if (StringUtil.isNotEmpty(chainID)) {
                    String userPk = MainApplication.getInstance().getPublicKey();
                    Member member = memberRepo.getMemberByChainIDAndPk(chainID, userPk);
                    if (null == member) {
                        member = new Member(chainID, userPk);
                        member.stickyTop = top;
                        memberRepo.addMember(member);
                    } else {
                        logger.debug("markReadOrUnread::{}, stickyTop::{}", member.stickyTop, top);
                        if (member.stickyTop != top) {
                            member.stickyTop = top;
                            memberRepo.updateMember(member);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("topStickyOrRemove error ", e);
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    Observable<AccessList> observeAccessList(String chainID) {
        return Observable.create(emitter -> {
            AccessList oldList = new AccessList();
            AccessList newList = new AccessList();
            if (StringUtil.isNotEmpty(chainID)) {
                byte[] chainIDBytes = ChainIDUtil.encode(chainID);
                String userPk = MainApplication.getInstance().getPublicKey();
                while (!emitter.isDisposed()) {
                    Member member = memberRepo.getMemberByChainIDAndPk(chainID, userPk);
                    logger.debug("getAccessList non member::{}",  null == member);
                    if (member != null) {
                        Account account = daemon.getAccountInfo(chainIDBytes, userPk);
                        if (account != null && (account.getBalance() != member.balance ||
                                account.getNonce() != member.nonce)) {
                            member.balance = account.getBalance();
                            member.nonce = account.getNonce();
                            memberRepo.updateMember(member);
                        }
                        logger.debug("getAccessList balance::{}, nonce::{}", member.balance, member.nonce);
                        List<String> connected = daemon.getCommunityAccessList(chainIDBytes);
                        newList.setConnected(connected);
                        if (oldList.getConnectedSize() != newList.getConnectedSize()) {
                            oldList = newList;
                        }
                        logger.debug("getAccessList connectedSize::{}", newList.getConnectedSize());
                        emitter.onNext(oldList);
                    }
                    try {
                        if (oldList.getConnectedSize() > 0) {
                            Thread.sleep(10000);
                        } else {
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            emitter.onComplete();
        });
    }

    public CommonDialog showLongTimeCreateDialog(FragmentActivity activity, LinkUtil.Link link,
                                          CommonDialog.ClickListener listener) {
        if (link.getTimestamp() <= 0) {
            if (listener != null) {
                listener.proceed();
            }
            return null;
        }
        long createTime = DateUtil.getTime() / 60 - link.getTimestamp();
        // ??????5???????????????
        if (createTime <= 1) {
            if (listener != null) {
                listener.proceed();
            }
            return null;
        }
        Context context = activity.getApplicationContext();
        ExternalAirdropLinkDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(context),
                R.layout.external_airdrop_link_dialog, null, false);
        long hours = createTime / 60;
        long minutes = createTime % 60;
        if (hours > 0) {
            dialogBinding.tvPeer.setText(activity.getString(R.string.chain_link_long_time_hour_min,
                    minutes, hours));
        } else {
            dialogBinding.tvPeer.setText(activity.getString(R.string.chain_link_long_time_min, minutes));
        }
        dialogBinding.tvPeer.setTextColor(context.getResources().getColor(R.color.color_black));
        dialogBinding.tvJoin.setText(R.string.common_proceed);
        CommonDialog longTimeCreateDialog = new CommonDialog.Builder(activity)
                .setContentView(dialogBinding.getRoot())
                .setCanceledOnTouchOutside(false)
                .create();
        longTimeCreateDialog.show();

        dialogBinding.tvSkip.setOnClickListener(view -> {
            longTimeCreateDialog.closeDialog();
            if (listener != null) {
                listener.close();
            }
        });
        dialogBinding.tvJoin.setOnClickListener(view -> {
                longTimeCreateDialog.closeDialog();
            if (listener != null) {
                listener.proceed();
            }
        });
        return longTimeCreateDialog;
    }
}
