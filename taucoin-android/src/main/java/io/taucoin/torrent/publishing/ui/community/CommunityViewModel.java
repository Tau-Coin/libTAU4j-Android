package io.taucoin.torrent.publishing.ui.community;

import android.app.Application;
import android.content.Context;
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
import org.libTAU4j.ChainURL;
import org.libTAU4j.Ed25519;
import org.libTAU4j.Pair;
import org.libTAU4j.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
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
import io.reactivex.Completable;
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
import io.taucoin.torrent.publishing.core.model.data.ChainStatus;
import io.taucoin.torrent.publishing.core.model.data.DataChanged;
import io.taucoin.torrent.publishing.core.model.data.DrawBean;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndMember;
import io.taucoin.torrent.publishing.core.model.data.MemberAndFriend;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.model.data.Statistics;
import io.taucoin.torrent.publishing.core.model.data.Result;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.model.data.message.AirdropStatus;
import io.taucoin.torrent.publishing.core.model.data.AirdropHistory;
import io.taucoin.torrent.publishing.core.model.data.message.SellTxContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.BlockInfo;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxConfirm;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.BlockRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxQueueRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.utils.BitmapUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.ChainUrlUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.ObservableUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.torrent.publishing.databinding.BlacklistDialogBinding;
import io.taucoin.torrent.publishing.ui.constant.Page;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;

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
    private MutableLiveData<Result> addCommunityState = new MutableLiveData<>();
    private MutableLiveData<Boolean> setBlacklistState = new MutableLiveData<>();
    private MutableLiveData<Boolean> airdropResult = new MutableLiveData<>();
    private MutableLiveData<Result> joinedResult = new MutableLiveData<>();
    private MutableLiveData<List<Community>> blackList = new MutableLiveData<>();
    private MutableLiveData<List<CommunityAndMember>> joinedList = new MutableLiveData<>();
    private MutableLiveData<List<Member>> joinedUnexpiredList = new MutableLiveData<>();
    private MutableLiveData<Bitmap> qrBitmap = new MutableLiveData<>();
    private MutableLiveData<UserAndFriend> largestCoinHolder = new MutableLiveData<>();
    private MutableLiveData<List<BlockAndTx>> chainBlocks = new MutableLiveData<>();
    private Disposable visitDisposable;
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

    public MutableLiveData<Boolean> getAirdropResult() {
        return airdropResult;
    }

    MutableLiveData<List<CommunityAndMember>> getJoinedList() {
        return joinedList;
    }

    public MutableLiveData<List<Member>> getJoinedUnexpiredList() {
        return joinedUnexpiredList;
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
    LiveData<Boolean> getSetBlacklistState() {
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
     * @param chainUrl
     */
    public void addCommunity(String chainID, String chainUrl){
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            String communityName = ChainIDUtil.getName(chainID);
            if (StringUtil.isNotEmpty(communityName)) {
                // 链端follow community
                ChainURL url = ChainUrlUtil.decode(chainUrl);
                boolean success = false;
                Set<String> peers = null;
                if (url != null) {
                    peers = url.getPeers();
                    success = daemon.followChain(chainID, peers);
                }
                if (success) {
                    Community community = communityRepo.getCommunityByChainID(chainID);
                    if (null == community) {
                        community = new Community(chainID, communityName);
                        communityRepo.addCommunity(community);
                    }
                    if (peers != null) {
                        for (String peer : peers) {
                            addUserInfoToLocal(peer);
                            addMemberInfoToLocal(chainID, peer);
                            addMemberInfoToLocal(chainID, MainApplication.getInstance().getPublicKey());
                        }
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
            // chainID为空，再次尝试创建一次
            if (StringUtil.isEmpty(community.chainID)) {
                community.chainID = createNewChainID(community.communityName);
            }
            if (StringUtil.isEmpty(community.chainID)) {
                result.setFailMsg(getApplication().getString(R.string.community_creation_failed));
                return result;
            }
            result.setMsg(community.chainID);
            Map<String, Account> accounts = new HashMap<>();
            if (selectedMap != null) {
                Collection<String> keys = selectedMap.keySet();
                for (String key : keys) {
                    long balance = FmtMicrometer.fmtTxLongValue(selectedMap.get(key));
                    Account account = new Account(balance, 0, 1, 0);
                    accounts.put(key, account);
                }
            }
            // 把社区创建者添加为社区成员
            User currentUser = userRepo.getCurrentUser();
            byte[] senderSeed = ByteUtil.toByte(currentUser.seed);
            Pair<byte[], byte[]> keypair = Ed25519.createKeypair(senderSeed);
            byte[] senderPk = keypair.first;
            byte[] secretKey = keypair.second;
            String coinName = ChainIDUtil.getCoinName(community.chainID);
            byte[] chainID = ChainIDUtil.encode(community.chainID);
            long timestamp = DateUtil.getMillisTime();
            String description = getApplication().getString(R.string.tx_community_creator_selling, coinName);
            SellTxContent sellTxContent = new SellTxContent(coinName, 0,
                    null, null, description);
            byte[] txEncoded = sellTxContent.getEncoded();
            Transaction transaction = new Transaction(chainID, 0, timestamp, senderPk, 0, txEncoded);
            transaction.sign(currentUser.publicKey, ByteUtil.toHexString(secretKey));

            boolean isCreateSuccess = daemon.createNewCommunity(chainID, accounts, transaction);
            if (!isCreateSuccess) {
                result.setFailMsg(getApplication().getString(R.string.community_creation_failed));
                return result;
            }
            Tx tx = new Tx(community.chainID, currentUser.publicKey, 0, TxType.SELL_TX.getType(), coinName,
                    0, null, null, description);
            tx.txID = transaction.getTxID().to_hex();
            tx.timestamp = timestamp;
            tx.senderPk = currentUser.publicKey;
            txRepo.addTransaction(tx);

            communityRepo.addCommunity(community);
            logger.debug("Add community to database: communityName={}, chainID={}",
                    community.communityName, community.chainID);

            Account account = daemon.getAccountInfo(chainID, currentUser.publicKey);
            if (account != null) {
                Member member = new Member(community.chainID, currentUser.publicKey,
                        account.getBalance(), account.getEffectivePower(), account.getNonce(),
                        account.getBlockNumber());
                memberRepo.addMember(member);
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
        if (visitDisposable != null && !visitDisposable.isDisposed()) {
            visitDisposable.dispose();
        }
        if (clearDisposable != null && !clearDisposable.isDisposed()) {
            clearDisposable.dispose();
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
     * 获取用户加入的社区列表
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
    public Flowable<List<CommunityAndMember>> observeCommunities() {
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
     * 查询社区成员
     * @param chainID
     * @return DataSource.Factory
     */
    public DataSource.Factory<Integer, MemberAndFriend> queryCommunityMembers(String chainID) {
        return memberRepo.queryCommunityMembers(chainID);
    }

    /**
     * 获取和社区成员共在的社区数
     * @param currentUserPk
     * @param memberPk
     */
    public Single<List<String>> getCommunityNumInCommon(String currentUserPk, String memberPk) {
        return memberRepo.getCommunityNumInCommon(currentUserPk, memberPk);
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
    public Flowable<Statistics> getBlocksStatistics(String chainID) {
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

    LiveData<PagedList<MemberAndFriend>> observerCommunityMembers(String chainID) {
        return new LivePagedListBuilder<>(queryCommunityMembers(chainID),
                Page.getPageListConfig()).build();
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
        return daemon.createNewChainID(communityName);
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
     * 观察链上Power前topNum的成员
     * @param chainID 链ID
     * @param topNum 查询数目
     * @return Observable<List<Member>>
     */
    Flowable<List<Member>> observeChainTopPowerMembers(String chainID, int topNum) {
        return communityRepo.observeChainTopPowerMembers(chainID, topNum);
    }

    /**
     * 观察链上状态信息
     * @param chainID 链ID
     * @return Flowable<ChainStatus>
     */
    public Flowable<ChainStatus> observerChainStatus(String chainID) {
        return blockRepo.observerChainStatus(chainID);
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
     * 删除Airdrop Bot
     * @param chainID 链ID
     */
    public void deleteAirdropBot(String chainID) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
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
                logger.trace("loadBlocksData pos::{}, pageSize::{}, blocks.size::{}",
                        pos, pageSize, blocks.size());
                logger.trace("loadBlocksData getMessagesTime::{}", getMessagesTime - startTime);
                Collections.reverse(blocks);
                long endTime = System.currentTimeMillis();
                logger.trace("loadBlocksData reverseTime Time::{}", endTime - getMessagesTime);
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
                List<String> list;
                if (type == AccessListActivity.ACCESS_LIST_TYPE) {
                    list = daemon.getCommunityAccessList(ChainIDUtil.encode(chainID));
                } else {
                    list = daemon.getGossipList(ChainIDUtil.encode(chainID));
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

    /**
     * 设置优先级链（用户正在查看的链）
     * @param chainID 链ID
     */
    public void setVisitChain(String chainID) {
        if (StringUtil.isEmpty(chainID)) {
            return;
        }
        if (null == visitDisposable) {
            daemon.setPriorityChain(chainID);
        }
        visitDisposable = ObservableUtil.intervalSeconds(1)
            .subscribeOn(Schedulers.io())
            .subscribe(l -> daemon.setPriorityChain(chainID));
    }

    /**
     * 取消设置优先级链（用户正在查看的链）
     */
    public void unsetVisitChain(String chainID) {
        if (visitDisposable != null && !visitDisposable.isDisposed()) {
            visitDisposable.dispose();
        }
        daemon.unsetPriorityChain(chainID);
    }

    public Observable<Integer> batchAddCommunities(String name, int num) {
        return Observable.create(emitter -> {
            if (num > 0) {
                for (int i = 0; i < num; i++) {
                    String communityName = name + (i + 1);
                    String chainID = daemon.createNewChainID(communityName);
                    Community community = new Community(chainID, communityName);
                    createCommunity(community, null);
                    emitter.onNext(i + 1);
                }
            }
            emitter.onComplete();
        });
    }

    /**
     * 观察交易确认信息
     */
    public Observable<List<TxConfirm>> observerTxConfirms(String txID) {
        return txRepo.observerTxConfirms(txID);
    }

    /**
     * 清除社区的消息未读状态
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
                        if (account != null && account.getBlockNumber() > member.blockNumber) {
                            member.blockNumber = account.getBlockNumber();
                            member.power = account.getEffectivePower();
                            member.balance = account.getBalance();
                            member.nonce = account.getNonce();
                            memberRepo.updateMember(member);
                        }
                        logger.debug("getAccessList blockNumber::{}, balance::{}, power::{}, nonce::{}",
                                member.blockNumber, member.balance, member.power, member.nonce);
                        List<String> connected = daemon.getCommunityAccessList(chainIDBytes);
                        List<String> gossip = daemon.getGossipList(chainIDBytes);
                        newList.setConnected(connected);
                        newList.setGossip(gossip);
                        if (oldList.getConnectedSize() != newList.getConnectedSize() ||
                                oldList.getGossipSize() != newList.getGossipSize()) {
                            oldList = newList;
                        }
                        logger.debug("getAccessList connectedSize::{}, gossipSize::{}",
                                newList.getConnectedSize(), newList.getGossipSize());
                        emitter.onNext(oldList);
                    }
                    try {
                        if (oldList.getConnectedSize() > 0 && oldList.getGossipSize() > 0) {
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
}
