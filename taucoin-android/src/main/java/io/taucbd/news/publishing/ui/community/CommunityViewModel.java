package io.taucbd.news.publishing.ui.community;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;

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
import java.util.concurrent.CopyOnWriteArraySet;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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
import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.Constants;
import io.taucbd.news.publishing.core.model.TauDaemon;
import io.taucbd.news.publishing.core.model.TauDaemonAlertHandler;
import io.taucbd.news.publishing.core.model.TauListenHandler;
import io.taucbd.news.publishing.core.model.data.BlockAndTx;
import io.taucbd.news.publishing.core.model.data.BlockStatistics;
import io.taucbd.news.publishing.core.model.data.ChainStatus;
import io.taucbd.news.publishing.core.model.data.CommunityAndAccount;
import io.taucbd.news.publishing.core.model.data.DataChanged;
import io.taucbd.news.publishing.core.model.data.CommunityAndMember;
import io.taucbd.news.publishing.core.model.data.HomeStatistics;
import io.taucbd.news.publishing.core.model.data.MemberAndAmount;
import io.taucbd.news.publishing.core.model.data.MemberAndFriend;
import io.taucbd.news.publishing.core.model.data.MemberAndTime;
import io.taucbd.news.publishing.core.model.data.MemberAndUser;
import io.taucbd.news.publishing.core.model.data.MemberTips;
import io.taucbd.news.publishing.core.model.data.Statistics;
import io.taucbd.news.publishing.core.model.data.Result;
import io.taucbd.news.publishing.core.model.data.TxQueueAndStatus;
import io.taucbd.news.publishing.core.model.data.UserAndFriend;
import io.taucbd.news.publishing.core.model.data.message.AirdropStatus;
import io.taucbd.news.publishing.core.model.data.AirdropHistory;
import io.taucbd.news.publishing.core.model.data.message.NewsContent;
import io.taucbd.news.publishing.core.model.data.message.TxType;
import io.taucbd.news.publishing.core.storage.sqlite.entity.BlockInfo;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Tx;
import io.taucbd.news.publishing.core.storage.sqlite.entity.TxLog;
import io.taucbd.news.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucbd.news.publishing.core.storage.sqlite.entity.User;
import io.taucbd.news.publishing.core.storage.sqlite.repo.BlockRepository;
import io.taucbd.news.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucbd.news.publishing.core.storage.sqlite.repo.TxQueueRepository;
import io.taucbd.news.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucbd.news.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Member;
import io.taucbd.news.publishing.core.utils.BitmapUtil;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.core.utils.CopyManager;
import io.taucbd.news.publishing.core.utils.LinkUtil;
import io.taucbd.news.publishing.core.utils.DateUtil;
import io.taucbd.news.publishing.core.utils.FmtMicrometer;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.ToastUtils;
import io.taucbd.news.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucbd.news.publishing.core.storage.RepositoryHelper;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Community;
import io.taucbd.news.publishing.core.utils.UsersUtil;
import io.taucbd.news.publishing.core.utils.Utils;
import io.taucbd.news.publishing.core.utils.ViewUtils;
import io.taucbd.news.publishing.core.utils.rlp.ByteUtil;
import io.taucbd.news.publishing.databinding.BlacklistDialogBinding;
import io.taucbd.news.publishing.databinding.ExternalAirdropLinkDialogBinding;
import io.taucbd.news.publishing.ui.ExternalLinkActivity;
import io.taucbd.news.publishing.ui.constant.Page;
import io.taucbd.news.publishing.ui.customviews.CommonDialog;
import io.taucbd.news.publishing.ui.customviews.ConfirmDialog;
import io.taucbd.news.publishing.ui.transaction.TxViewModel;

/**
 * Community模块的ViewModel
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
    private Disposable loadViewDisposable;
    private MutableLiveData<Result> addCommunityState = new MutableLiveData<>();
    private MutableLiveData<Boolean> setBlacklistState = new MutableLiveData<>();
    private MutableLiveData<Result> airdropResult = new MutableLiveData<>();
    private MutableLiveData<Result> joinedResult = new MutableLiveData<>();
    private MutableLiveData<List<Community>> blackList = new MutableLiveData<>();
    private MutableLiveData<List<Member>> joinedUnexpiredList = new MutableLiveData<>();
    private MutableLiveData<List<MemberAndTime>> joinedCommunity = new MutableLiveData<>();
    private MutableLiveData<Bitmap> qrBitmap = new MutableLiveData<>();
    private MutableLiveData<UserAndFriend> largestCoinHolder = new MutableLiveData<>();
    private MutableLiveData<List<BlockAndTx>> chainBlocks = new MutableLiveData<>();
    private MutableLiveData<List<MemberAndFriend>> memberList = new MutableLiveData<>();
    private Disposable clearDisposable;

    public CommunityViewModel(@NonNull Application application) {
        super(application);
        communityRepo = RepositoryHelper.getCommunityRepository(getApplication());
        memberRepo = RepositoryHelper.getMemberRepository(getApplication());
        userRepo = RepositoryHelper.getUserRepository(getApplication());
        txQueueRepo = RepositoryHelper.getTxQueueRepository(getApplication());
        blockRepo = RepositoryHelper.getBlockRepository(getApplication());
        txRepo = RepositoryHelper.getTxRepository(getApplication());
        daemon = TauDaemon.getInstance(getApplication());
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

    public MutableLiveData<Result> getAirdropResult() {
        return airdropResult;
    }

    public MutableLiveData<List<Member>> getJoinedUnexpiredList() {
        return joinedUnexpiredList;
    }

    public MutableLiveData<List<MemberAndTime>> getJoinedCommunity() {
        return joinedCommunity;
    }

    public MutableLiveData<List<MemberAndFriend>> getMemberList() {
        return memberList;
    }

    /**
     * 获取添加社区状态的被观察者
     * @return 被观察者
     */
    public LiveData<Result> getAddCommunityState() {
        return addCommunityState;
    }

    /**
     * 获取设置黑名单状态的被观察者
     * @return 被观察者
     */
    public LiveData<Boolean> getSetBlacklistState() {
        return setBlacklistState;
    }

    public LiveData<Result> getJoinedResult() {
        return joinedResult;
    }

    /**
     * 获取社区黑名单的被观察者
     * @return 被观察者
     */
    public MutableLiveData<List<Community>> getBlackList() {
        return blackList;
    }

    /**
     * 观察生成的二维码
     */
    public MutableLiveData<Bitmap> getQRBitmap() {
        return qrBitmap;
    }

    /**
     * 观察社区最大币持有者
     */
    public MutableLiveData<UserAndFriend> getLargestCoinHolder() {
        return largestCoinHolder;
    }

    /**
     * 添加新的社区到数据库
     * @param chainID
     * @param link
     */
    public void addCommunity(String chainID, LinkUtil.Link link) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            String communityName = ChainIDUtil.getName(chainID);
            if (StringUtil.isNotEmpty(communityName)) {
                String userPk = MainApplication.getInstance().getPublicKey();

                Set<String> peers = new HashSet<>();
                peers.add(link.getPeer());
                if (link.getMiner() != null) {
                    peers.add(link.getMiner());
                }
                boolean success = false;
                Community community = communityRepo.getCommunityByChainID(chainID);
                // 向对方peer请求publish区块链数据
                daemon.requestChainData(link.getPeer(), chainID);

                if (null == community) {
                    // 链端follow community
                    success = daemon.followChain(chainID, peers);
                    logger.info("addCommunity chainID::{}, peers::{}, success::{}", chainID,
                            peers.size(), success);
                    if (success) {
                        community = new Community(chainID, communityName);
                        communityRepo.addCommunity(community);
                        // 更新用户在线信号
                        daemon.updateUserInfo(userRepo.getCurrentUser());
                        addMemberInfoToLocal(chainID, userPk);
                    }
                } else {
                    // 如果已经被banned
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
                        // 没被banned未加入，判断是否join, 没join也需要follow
                        Member member = memberRepo.getMemberByChainIDAndPk(chainID, userPk);
                        logger.info("addCommunity isBanned::false, chainID::{}, peers::{}, joined::{}",
                                chainID, peers.size(), member != null);
                        if (null == member) {
                            daemon.followChain(chainID, peers);
                            addMemberInfoToLocal(chainID, userPk);
                        } else {
                            // 防止libTAU没有此社区chain
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
     * 添加用户信息到本地
     * @param publicKey 用户公钥
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
     * 添加社区成员信息
     * @param chainID 链ID
     * @param publicKey 用户公钥
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
     * 添加新的社区到数据库
     * @param community 社区数据
     * @param selectedMap 创建社区的初始成员
     */
    void addCommunity(@NonNull Community community, Map<String, String> selectedMap) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            // TauController:创建Community社区
            Result result = createCommunity(getApplication(), community, selectedMap, false);
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> addCommunityState.postValue(state));
        disposables.add(disposable);
    }

    public static  Result createCommunity(Context appContext, Community community, Map<String, String> selectedMap, boolean stickyTop) {
        Result result = new Result();
        try {
            TauDaemon daemon = TauDaemon.getInstance(appContext);
            UserRepository userRepo = RepositoryHelper.getUserRepository(appContext);
            CommunityRepository communityRepo = RepositoryHelper.getCommunityRepository(appContext);
            MemberRepository memberRepo = RepositoryHelper.getMemberRepository(appContext);
            // chainID为空，再次尝试创建一次
            if (StringUtil.isEmpty(community.chainID)) {
                String type = "0000";
                community.chainID = daemon.createNewChainID(type, community.communityName);
            }
            if (StringUtil.isEmpty(community.chainID)) {
                result.setFailMsg(appContext.getString(R.string.community_creation_failed));
                return result;
            }
            result.setMsg(community.chainID);
            Set<Account> accounts = new HashSet<>();
            if (selectedMap != null) {
                Collection<String> keys = selectedMap.keySet();
                for (String key : keys) {
                    long balance = FmtMicrometer.fmtTxLongValue(selectedMap.get(key));
                    Account account = new Account(ByteUtil.toByte(key), balance, 0, 0);
                    accounts.add(account);
                }
            }
            // 把社区创建者添加为社区成员
            User currentUser = userRepo.getCurrentUser();
            byte[] chainID = ChainIDUtil.encode(community.chainID);
            boolean isCreateSuccess = daemon.createNewCommunity(chainID, accounts);
            if (!isCreateSuccess) {
                result.setFailMsg(appContext.getString(R.string.community_creation_failed));
                return result;
            }

            communityRepo.addCommunity(community);
            logger.info("Add community to database: communityName={}, chainID={}",
                    community.communityName, community.chainID);

            Account account = daemon.getAccountInfo(chainID, currentUser.publicKey);
            Member member;
            if (account != null) {
                member = new Member(community.chainID, currentUser.publicKey,
                        account.getBalance(), account.getNonce());
                member.stickyTop = stickyTop ? 1 : 0;
            } else {
                // 防止与libTAU交互失败的情况！！！
                member = new Member(community.chainID, currentUser.publicKey, 0, 0);
                member.stickyTop = stickyTop ? 1 : 0;
            }
            memberRepo.addMember(member);

            // 添加朋友信息后，更新用户在线信号
            daemon.updateUserInfo(currentUser);

            // 自动发送一笔Sell交易
            String communityName = ChainIDUtil.getName(community.chainID);
            String description = appContext.getString(R.string.tx_community_creator_selling, communityName);
            NewsContent newsContent = new NewsContent(description, null, null, null);
            TxQueue newsTxQueue = new TxQueue(community.chainID, currentUser.publicKey, currentUser.publicKey, 0L,
                    Constants.NEWS_MIN_FEE.longValue(), TxType.NEWS_TX, newsContent.getEncoded());

            TxViewModel txViewModel = new TxViewModel(MainApplication.getInstance());
            txViewModel.addTransactionTask(newsTxQueue, null, DateUtil.getMillisTime());

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
        if (loadViewDisposable != null && !loadViewDisposable.isDisposed()) {
            loadViewDisposable.dispose();
        }
    }

    /**
     * 验证Community实体类中的数据
     * @param community view数据对象
     * @return 是否验证通过
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
     * 显示拉黑社区提示对话框
     * @param chainID 社区chainID
     */
    public CommonDialog showBanCommunityTipsDialog(FragmentActivity activity, String chainID) {
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
     * 设置社区黑名单
     * @param chainID 社区chainID
     * @param blacklist 是否加入黑名单
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
     * 获取在黑名单的社区列表
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
     * 获取用户加入的社区列表
     */
    public Flowable<List<Member>> observerJoinedCommunityList() {
        return communityRepo.observerJoinedCommunityList();
    }

    /**
     * 获取用户加入的社区列表
     */
    public Flowable<MemberTips> observeMemberTips() {
        return communityRepo.observeMemberTips();
    }

    /**
     * 获取用户加入的社区列表
     */
    public void getJoinedUnexpiredCommunityList(String userPk) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<List<Member>>) emitter -> {
            List<Member> list = new ArrayList<>();
            CopyOnWriteArraySet<String> set = daemon.getMyAccountManager().getSet();
            if (set != null && set.size() > 0) {
                for (String chainID : set) {
                    Member member = memberRepo.getMemberByChainIDAndPk(chainID, userPk);
                    if (member != null) {
                        list.add(member);
                    }
                }
            }
            emitter.onNext(list);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> joinedUnexpiredList.postValue(list));
        disposables.add(disposable);
    }

    /**
     * 获取用户加入的社区列表
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
     * 观察社区成员变化
     * @param chainID
     * @return
     */
    public Flowable<List<MemberAndUser>> observeCommunityMembers(String chainID) {
        return memberRepo.observeCommunityMembers(chainID);
    }

    /**
     * 观察社区信息变化
     */
    public Flowable<List<CommunityAndAccount>> observeCommunities() {
        return communityRepo.observeCommunities();
    }

    /**
     * 观察社区发币详情
     * @param chainID 链ID
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
     * 获取社区limit个成员
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
     * 获取社区成员统计
     * @param chainID
     */
    public Flowable<Statistics> getMembersStatistics(String chainID) {
        return memberRepo.getMembersStatistics(chainID);
    }

    /**
     * 获取社区成员统计
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

    void loadCommunityMembers(String chainID, int pos, int initSize) {
        if (loadViewDisposable != null && !loadViewDisposable.isDisposed()) {
            loadViewDisposable.dispose();
        }
        loadViewDisposable = Observable.create((ObservableOnSubscribe<List<MemberAndFriend>>) emitter -> {
            List<MemberAndFriend> txs = new ArrayList<>();
            try {
                int pageSize = Page.PAGE_SIZE;
                if (pos == 0 && initSize > pageSize) {
                    pageSize = initSize;
                }
                long startTime = System.currentTimeMillis();
                txs = memberRepo.queryCommunityMembers(chainID, pos, pageSize);
                long endTime = System.currentTimeMillis();
                logger.debug("loadCommunityMembers time::{}ms", endTime - startTime);
                logger.debug("loadCommunityMembers pos::{}, pageSize::{}, messages.size::{}",
                        pos, pageSize, txs.size());
            } catch (Exception e) {
                logger.error("loadCommunityMembers error::", e);
            }
            emitter.onNext(txs);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(messages -> {
            memberList.postValue(messages);
        });
    }

    /**
     * 观察当前登陆的社区成员
     * @param chainID
     * @return
     */
    public Flowable<CommunityAndMember> observerCurrentMember(String chainID) {
        String publicKey = MainApplication.getInstance().getPublicKey();
        return communityRepo.observerCurrentMember(chainID, publicKey);
    }

    /**
     * 观察当前登陆的社区成员
     * @param chainID
     * @return
     */
    public Flowable<MemberAndAmount> observerMemberAndAmount(String chainID) {
        return communityRepo.observerMemberAndAmount(chainID);
    }

    /**
     * 生成二维码
     * @param context
     * @param QRContent 二维码内容
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

                // 二维码背景样式
//                Bitmap bgBitmap = BitmapFactory.decodeResource(context.getResources(),
//                        R.mipmap.icon_community_qr_bg);
//                bgBitmap.getWidth();
//                DrawBean bean = new DrawBean();
//                bean.setSize(resources.getDimensionPixelSize(R.dimen.widget_size_210));
//                bean.setX(resources.getDimensionPixelSize(R.dimen.widget_size_60));
//                bean.setY(resources.getDimensionPixelSize(R.dimen.widget_size_60));
//                Bitmap lastBitmap = BitmapUtil.drawStyleQRcode(bgBitmap,
//                        null, bitmap, bean);
                if (bitmap != null) {
                    emitter.onNext(bitmap);
                }
                // 生成二维码触发pub链数据
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
     * 创建新的社区链ID
     * @param communityName 社区名称
     * @return chainID
     */
    String createNewChainID(String communityName) {
        String type = "0000";
        return daemon.createNewChainID(type, communityName);
    }

    /**
     * 观察链上币量前topNum的成员
     * @param chainID 链ID
     * @param topNum 查询数目
     * @return Observable<List<Member>>
     */
    Flowable<List<Member>> observeChainTopCoinMembers(String chainID, int topNum) {
        return communityRepo.observeChainTopCoinMembers(chainID, topNum);
    }

    /**
     * 观察链上状态信息
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
     * 加入社区
     * @param chainID 链ID
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
     * 设置Airdrop Bot
     * @param chainID 链ID
     * @param members airdrop个数
     * @param coins 每次发币的coins
     */
    public void setupAirdropBot(String chainID, int members, float coins, BigInteger airdropTotalCoins) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            result.setSuccess(true);
            User user = userRepo.getCurrentUser();
            if (user != null) {
                Member member = memberRepo.getMemberByChainIDAndPk(chainID, user.publicKey);
                if (member != null) {
                    if (airdropTotalCoins.compareTo(BigInteger.valueOf(member.balance)) > 0) {
                        result.setFailMsg(getApplication().getString(R.string.tx_error_insufficient_balance));
                    } else {
                        member.airdropStatus = AirdropStatus.ON.getStatus();
                        member.airdropMembers = members;
                        member.airdropCoins = FmtMicrometer.fmtTxLongValue(String.valueOf(coins));
                        member.airdropTime = DateUtil.getMillisTime();
                        memberRepo.updateMember(member);
                    }
                }
            }
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    airdropResult.postValue(result);
                });
        disposables.add(disposable);
    }

    /**
     * 删除Airdrop Bot
     * @param chainID 链ID
     */
    public void deleteAirdropBot(String chainID) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            result.setSuccess(true);
            User user = userRepo.getCurrentUser();
            if (user != null) {
                Member member = memberRepo.getMemberByChainIDAndPk(chainID, user.publicKey);
                if (member != null) {
                    List<TxQueueAndStatus> queueList = txQueueRepo.getCommunityTxQueue(chainID, user.publicKey);
                    if (queueList != null && queueList.size() > 0) {
                        // 删除未被发送的交易
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
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    airdropResult.postValue(result);
                });
        disposables.add(disposable);
    }

    /**
     * 观察社区同步状态
     * @return
     */
    public Flowable<List<BlockAndTx>> observeCommunitySyncStatus(String chainID) {
        return blockRepo.observeCommunitySyncStatus(chainID);
    }

    /**
     * 加载区块分页数据
     * @param chainID 社区链ID
     * @param pos 分页位置
     * @param initSize 刷新时第一页数据大小
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
            TauDaemonAlertHandler tauDaemonHandler = TauDaemon.getInstance(getApplication()).getTauDaemonHandler();
            while (!emitter.isDisposed()) {
                boolean isStopped = tauDaemonHandler.isChainStopped(chainID);
                long time;
                if (isStopped) {
                    time = -1;
                } else {
                    time = daemon.getMiningTime(ChainIDUtil.encode(chainID));
                }
                logger.debug("Mining Time::{}, chainID::{}, isRunning::{}", time,
                        chainID, daemon.isRunning());
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

    Flowable<Float> reloadChain(String chainID) {
        return Flowable.create(emitter -> {
            Community community = communityRepo.getCommunityByChainID(chainID);
            if (community != null) {
                long headBlock = community.headBlock;
                long consensusBlock = community.consensusBlock;
                logger.debug("reloadChain chainID::{}, headBlock::{}", chainID, headBlock);
                Block block;
                for (long i = headBlock; i >= consensusBlock; i--) {
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
                    if (headBlock > consensusBlock && !emitter.isCancelled()) {
                        float progress = (headBlock - i) * 100f / (headBlock - consensusBlock);
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
                    createCommunity(getApplication(), community, null, false);
                    emitter.onNext(i + 1);
                }
            }
            emitter.onComplete();
        });
    }

    /**
     * 观察交易确认信息
     */
    public Observable<List<TxLog>> observerTxLogs(String txID) {
        return txRepo.observerTxLogs(txID);
    }

    public void clearNewsUnread() {
        if (clearDisposable != null && !clearDisposable.isDisposed()) {
            return;
        }
        clearDisposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            try {
                memberRepo.clearNewsUnread();
            } catch (Exception e) {
                logger.error("markReadOrUnread error ", e);
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();
    }
    /**
     * 清除社区的消息未读状态
     */
    public void clearMsgUnread(String chainID) {
        markReadOrUnread(chainID, 0, 0);
    }

    public void clearNewsUnread(String chainID) {
        markReadOrUnread(chainID, 0, 1);
    }

    public void markReadOrUnread(String chainID, int status) {
        markReadOrUnread(chainID, status, -1);
    }

    private void markReadOrUnread(String chainID, int status, int type) {
        if (clearDisposable != null && !clearDisposable.isDisposed()) {
            return;
        }
        clearDisposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            try {
                if (StringUtil.isNotEmpty(chainID)) {
                    String userPk = MainApplication.getInstance().getPublicKey();
                    Member member = memberRepo.getMemberByChainIDAndPk(chainID, userPk);
                    if (null == member) {
                        if (type == -1) {
                            member = new Member(chainID, userPk);
                            member.msgUnread = status;
                            member.newsUnread = status;
                            memberRepo.addMember(member);
                        }
                    } else {
                        boolean isUpdate = false;
                        if (type == 0 && member.msgUnread != status) {
                            member.msgUnread = status;
                            isUpdate = true;
                        } else if (type == 1 && member.newsUnread != status) {
                            member.newsUnread = status;
                            isUpdate = true;
                        } else if (type == -1 && (member.msgUnread != status || member.newsUnread != status)) {
                            member.msgUnread = status;
                            member.newsUnread = status;
                            isUpdate = true;
                        }
                        logger.debug("markReadOrUnread msgUnread::{}, newsUnread::{}, status::{}",
                                member.msgUnread, member.newsUnread, status);
                        if (isUpdate) {
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

    public ConfirmDialog showLongTimeCreateDialog(FragmentActivity activity, LinkUtil.Link link,
                                                  ConfirmDialog.ClickListener listener) {
        return showLongTimeCreateDialog(activity, link, false, listener);
    }

    public static ConfirmDialog showLongTimeCreateDialog(FragmentActivity activity, LinkUtil.Link link,
                                  boolean isQrCode, ConfirmDialog.ClickListener listener) {
        if (link.getTimestamp() <= 0) {
            if (listener != null) {
                listener.proceed();
            }
            return null;
        }
        String currentUser = MainApplication.getInstance().getPublicKey();
        boolean isNeedCreateReferral = link.isAirdropLink() && StringUtil.isNotEquals(link.getPeer(), currentUser) &&
                StringUtil.isNotEquals(link.getReferralPeer(), currentUser);
        long createTime = DateUtil.getTime() / 60 - link.getTimestamp();
        Context context = activity.getApplicationContext();
        ExternalAirdropLinkDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(context),
                R.layout.external_airdrop_link_dialog, null, false);
        int contentRid;
        if (link.isAirdropLink()) {
            contentRid = isNeedCreateReferral ? R.string.referral_link_long_time : R.string.airdrop_link_long_time;
        } else if (link.isChainLink()) {
            contentRid = isQrCode ? R.string.chain_qr_long_time : R.string.chain_link_long_time;
        } else {
            contentRid = isQrCode ? R.string.friend_qr_long_time : R.string.friend_link_long_time;
        }
        dialogBinding.tvPeer.setText(activity.getString(contentRid, createTime));
        dialogBinding.tvPeer.setTextColor(context.getResources().getColor(R.color.color_black));
        dialogBinding.tvJoin.setText(R.string.common_proceed);
        dialogBinding.llReferralLink.setVisibility(isNeedCreateReferral ? View.VISIBLE : View.GONE);
        if (isNeedCreateReferral) {
            dialogBinding.tvJoin.setVisibility(View.GONE);
            long referralBonus = link.getCoins() / 2;
            referralBonus = Math.max(1, referralBonus);
            dialogBinding.tvReferralBonus.setText(activity.getString(R.string.main_referral_link_text,
                    FmtMicrometer.fmtLong(referralBonus), FmtMicrometer.fmtLong( 10 * referralBonus)));
            String linkStr = LinkUtil.encodeAirdropReferral(link, currentUser);
            dialogBinding.tvReferralLink.setText(linkStr);
        }
        ConfirmDialog longTimeCreateDialog = new ConfirmDialog.Builder(activity)
                .setContentView(dialogBinding.getRoot())
                .setCanceledOnTouchOutside(false)
                .create();
        longTimeCreateDialog.show();

        dialogBinding.ivSkip.setOnClickListener(view -> {
            longTimeCreateDialog.closeDialog();
            if (listener != null) {
                if (isNeedCreateReferral) {
                    listener.proceed();
                } else {
                    listener.close();
                }
            }
            if (activity instanceof ExternalLinkActivity) {
                activity.finish();
            }
        });
        dialogBinding.tvJoin.setOnClickListener(view -> {
            longTimeCreateDialog.closeDialog();
            if (listener != null) {
                listener.proceed();
            }
        });
        dialogBinding.tvReferralCopy.setOnClickListener(view -> {
            longTimeCreateDialog.closeDialog();
            CopyManager.copyText(ViewUtils.getText(dialogBinding.tvReferralLink));
            ToastUtils.showShortToast(R.string.main_referral_link_copy);
            if (listener != null) {
                listener.proceed();
            }
        });
        return longTimeCreateDialog;
    }

    /**
     * 关注链时，尝试连接链
     * @param chainID 链ID
     */
    public void connectChain(String chainID) {
        daemon.connectChain(chainID);
    }

    /**
     * 获取用户加入的社区列表
     */
    void clearCommunityAccountTips(String chainID) {
        if (clearDisposable != null && !clearDisposable.isDisposed()) {
            return;
        }
        clearDisposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            try {
                if (StringUtil.isNotEmpty(chainID)) {
                    String userPk = MainApplication.getInstance().getPublicKey();
                    Member member = memberRepo.getMemberByChainIDAndPk(chainID, userPk);
                    if (member != null) {
//                        if (member.pendingTime > 0 || member.rewardTime > 0 || member.incomeTime > 0) {
                        if (member.pendingTime > 0) {
                            member.pendingTime = 0;
//                            member.rewardTime = 0;
//                            member.incomeTime = 0;
                            memberRepo.updateMember(member);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("clearCommunityAccountTips error ", e);
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    public Observable<BlockInfo> observeBlockByHash(String chainID, String hash) {
        return blockRepo.observeBlock(chainID, hash);
    }

    public Observable<String> observeLatestMiner(String chainID) {
        return Observable.create((ObservableOnSubscribe<String>) emitter -> {
            String miner = MainApplication.getInstance().getPublicKey();
            try {
                BlockInfo block = blockRepo.queryLatestBlock(chainID);
                if (block != null && StringUtil.isNotEmpty(block.miner)) {
                    miner = block.miner;
                }
            } catch (Exception e) {
                logger.error("observeLatestMiner error ", e);
            }
            emitter.onNext(miner);
            emitter.onComplete();
        });
    }

    public void touchChain(String chainID) {
        if (StringUtil.isEmpty(chainID)) {
            return;
        }
        daemon.touchChain(chainID);
    }

    public Observable<HomeStatistics> observeCommunitiesAndContacts() {
        return communityRepo.observeCommunitiesAndContacts();
    }

    public Flowable<Object> observeMembersDataSetChanged() {
        return memberRepo.observeMembersDataSetChanged();
    }
}
