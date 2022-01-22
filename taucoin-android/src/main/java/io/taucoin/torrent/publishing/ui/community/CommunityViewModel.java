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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Collection;
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
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.ChainStatus;
import io.taucoin.torrent.publishing.core.model.data.DrawBean;
import io.taucoin.torrent.publishing.core.model.data.FriendStatus;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndMember;
import io.taucoin.torrent.publishing.core.model.data.MemberAndFriend;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.model.data.Statistics;
import io.taucoin.torrent.publishing.core.model.data.Result;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.BlockRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.utils.BitmapUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.ChainUrlUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
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
    private FriendRepository friendRepo;
    private UserRepository userRepo;
    private SettingsRepository settingsRepo;
    private TauDaemon daemon;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<Result> addCommunityState = new MutableLiveData<>();
    private MutableLiveData<Boolean> setBlacklistState = new MutableLiveData<>();
    private MutableLiveData<List<Community>> blackList = new MutableLiveData<>();
    private MutableLiveData<List<Community>> joinedList = new MutableLiveData<>();
    private MutableLiveData<Bitmap> qrBitmap = new MutableLiveData<>();
    private MutableLiveData<UserAndFriend> largestCoinHolder = new MutableLiveData<>();

    public CommunityViewModel(@NonNull Application application) {
        super(application);
        communityRepo = RepositoryHelper.getCommunityRepository(getApplication());
        memberRepo = RepositoryHelper.getMemberRepository(getApplication());
        userRepo = RepositoryHelper.getUserRepository(getApplication());
        friendRepo = RepositoryHelper.getFriendsRepository(getApplication());
        blockRepo = RepositoryHelper.getBlockRepository(getApplication());
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplication());
        daemon = TauDaemon.getInstance(getApplication());
    }

    public void observeNeedStartDaemon () {
        disposables.add(daemon.observeNeedStartDaemon()
                .subscribeOn(Schedulers.io())
                .filter((needStart) -> needStart)
                .subscribe((needStart) -> daemon.start()));
    }

    MutableLiveData<List<Community>> getJoinedList() {
        return joinedList;
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
                    Community community = new Community(chainID, communityName);
                    communityRepo.addCommunity(community);
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
            byte[] chainID = ChainIDUtil.encode(community.chainID);
            boolean isCreateSuccess = daemon.createNewCommunity(chainID, accounts);
            if (!isCreateSuccess) {
                result.setFailMsg(getApplication().getString(R.string.community_creation_failed));
                return result;
            }
            communityRepo.addCommunity(community);
            logger.debug("Add community to database: communityName={}, chainID={}",
                    community.communityName, community.chainID);
            // 把社区创建者添加为社区成员
            User currentUser = userRepo.getCurrentUser();
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
        String key = activity.getString(R.string.pref_key_show_blacklist_dialog);
        boolean isShowDialog = settingsRepo.getBooleanValue(key, true);
        if (!isShowDialog) {
            setCommunityBlacklist(chainID, true);
            return null;
        }
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
            if (dialogBinding.cbDoNotShow.isChecked()) {
                settingsRepo.setBooleanValue(key, true);
            }
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
        Disposable disposable = Flowable.create((FlowableOnSubscribe<List<Community>>) emitter -> {
            List<Community> list = communityRepo.getJoinedCommunityList();
            emitter.onNext(list);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> joinedList.postValue(list));
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
    public Single<List<String>> getCommunityMembersLimit(String chainID, int limit) {
        return memberRepo.getCommunityMembersLimit(chainID, limit);
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


    public Single<Community> getCommunityByChainIDSingle(String chainID) {
        return communityRepo.getCommunityByChainIDSingle(chainID);
    }

    public Observable<Community> observerCommunityByChainID(String chainID) {
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
    Observable<CommunityAndMember> observerCurrentMember(String chainID) {
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
     * 获取tip block列表
     * @param chainID 社区ID
     * @param topNum 返回的数目
     * @return List<Block>
     */
    public List<Block> getTopTipBlock(String chainID, int topNum) {
        return daemon.getTopTipBlock(chainID, topNum);
    }

    /**
     * 获取区块
     * @param chainID 社区ID
     * @param blockNumber 区块号
     * @return Block
     */
    public Block getBlockByNumber(String chainID, long blockNumber) {
        return daemon.getBlockByNumber(chainID, blockNumber);
    }

    /**
     * 观察链上币量前topNum的成员
     * @param chainID 链ID
     * @param topNum 查询数目
     * @return Observable<List<Member>>
     */
    Observable<List<Member>> observeChainTopCoinMembers(String chainID, int topNum) {
        return communityRepo.observeChainTopCoinMembers(chainID, topNum);
    }

    /**
     * 观察链上Power前topNum的成员
     * @param chainID 链ID
     * @param topNum 查询数目
     * @return Observable<List<Member>>
     */
    Observable<List<Member>> observeChainTopPowerMembers(String chainID, int topNum) {
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
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            List<String> list = queryCommunityMembersLimit(chainID, Constants.CHAIN_LINK_BS_LIMIT);
            Set<String> peers = new HashSet<>(list);
            boolean success = daemon.followChain(chainID, peers);
            if (success) {
                String publicKey = MainApplication.getInstance().getPublicKey();
                addMemberInfoToLocal(chainID, publicKey);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
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
}
