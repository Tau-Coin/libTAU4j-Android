package io.taucoin.torrent.publishing.ui.user;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.luck.picture.lib.entity.LocalMedia;

import org.libTAU4j.Ed25519;
import org.libTAU4j.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.FriendAndUser;
import io.taucoin.torrent.publishing.core.model.data.FriendStatus;
import io.taucoin.torrent.publishing.core.model.data.Result;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.AppUtil;
import io.taucoin.torrent.publishing.core.utils.BitmapUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FileUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.core.utils.media.MediaUtil;
import io.taucoin.torrent.publishing.databinding.AddFriendDialogBinding;
import io.taucoin.torrent.publishing.databinding.BanDialogBinding;
import io.taucoin.torrent.publishing.databinding.ContactsDialogBinding;
import io.taucoin.torrent.publishing.databinding.SeedDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.ScanTriggerActivity;
import io.taucoin.torrent.publishing.ui.chat.ChatViewModel;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.ui.constant.PublicKeyQRContent;
import io.taucoin.torrent.publishing.ui.constant.SeedQRContent;
import io.taucoin.torrent.publishing.ui.constant.QRContent;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.customviews.GuideDialog;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.core.model.data.message.MessageType;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;

/**
 * 用户相关的ViewModel
 */
public class UserViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("UserViewModel");
    private static final String QR_CODE_NAME = "QRCode%s.jpg";
    private UserRepository userRepo;
    private FriendRepository friendRepo;
    private SettingsRepository settingsRepo;
    private TxRepository txRepo;
    private MemberRepository memRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<String> changeResult = new MutableLiveData<>();
    private MutableLiveData<Result> addFriendResult = new MutableLiveData<>();
    private MutableLiveData<Boolean> editRemarkResult = new MutableLiveData<>();
    private MutableLiveData<List<User>> blackList = new MutableLiveData<>();
    private MutableLiveData<Result> editBlacklistResult = new MutableLiveData<>();
    private MutableLiveData<UserAndFriend> userDetail = new MutableLiveData<>();
    private MutableLiveData<QRContent> qrContent = new MutableLiveData<>();
    private MutableLiveData<Bitmap> qrBitmap = new MutableLiveData<>();
    private MutableLiveData<Bitmap> qrBlurBitmap = new MutableLiveData<>();
    private MutableLiveData<List<UserAndFriend>> userList = new MutableLiveData<>();
    private CommonDialog commonDialog;
    private CommonDialog editNameDialog;
    private GuideDialog guideDialog;
    private TauDaemon daemon;
    private ChatViewModel chatViewModel;
    private Disposable clearDisposable;
    public UserViewModel(@NonNull Application application) {
        super(application);
        userRepo = RepositoryHelper.getUserRepository(getApplication());
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplication());
        txRepo = RepositoryHelper.getTxRepository(getApplication());
        friendRepo = RepositoryHelper.getFriendsRepository(getApplication());
        memRepo = RepositoryHelper.getMemberRepository(getApplication());
        daemon = TauDaemon.getInstance(application);
        chatViewModel = new ChatViewModel(application);
    }

    public void observeNeedStartDaemon () {
        disposables.add(daemon.observeNeedStartDaemon()
                .subscribeOn(Schedulers.io())
                .filter((needStart) -> needStart)
                .subscribe((needStart) -> daemon.start()));
    }

    @Override
    public void onCleared() {
        super.onCleared();
        disposables.clear();
        if (commonDialog != null) {
            commonDialog.closeDialog();
            commonDialog = null;
        }
        if (editNameDialog != null) {
            editNameDialog.closeDialog();
            editNameDialog = null;
        }
        if (guideDialog != null) {
            guideDialog.closeDialog();
            guideDialog = null;
        }
        if (clearDisposable != null && !clearDisposable.isDisposed()) {
            clearDisposable.dispose();
        }

    }

    /**
     * 显示保存Seed的对话框，也起到确认效果,确认后执行后续操作
     * @param generate：false导入; true:生成新的seed
     */
    public void showSaveSeedDialog(ScanTriggerActivity activity, boolean generate){
        SeedDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.seed_dialog, null, false);
        if(generate){
            binding.etSeed.setVisibility(View.GONE);
            binding.llScanQrCode.setVisibility(View.GONE);
        } else {
            binding.llScanQrCode.setOnClickListener(v -> {
                if(commonDialog != null){
                    commonDialog.closeDialog();
                }
                activity.openScanQRActivity(this);
            });
        }
        binding.ivClose.setOnClickListener(v -> {
            if(commonDialog != null){
                commonDialog.closeDialog();
            }
        });
        binding.tvSubmit.setOnClickListener(v -> {
            String name = ViewUtils.getText(binding.etName);
            if (StringUtil.isNotEmpty(name)) {
                int nicknameLength = Utils.textStringToBytes(name).length;
                if (nicknameLength > Constants.NICKNAME_LENGTH) {
                    ToastUtils.showShortToast(R.string.user_new_name_too_long);
                    return;
                }
            }
            if(generate){
                if(commonDialog != null){
                    commonDialog.closeDialog();
                }
                generateSeed(name);
            }else{
                String seed = ViewUtils.getText(binding.etSeed);
                if(StringUtil.isEmpty(seed)){
                    ToastUtils.showShortToast(R.string.user_seed_empty);
                }else {
                    importSeed(seed, name);
                }
            }
        });
        commonDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .create();
        commonDialog.show();
    }

    /**
     * 导入并切换Seed
     */
    public void importSeed(String seed, String name) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            String result = "";
            try {
                // 必须顺序执行以下逻辑, 保证数据不会错乱
                // 0、清除libTAU中的朋友关系和跟随的社区链
                User oldUser = userRepo.getCurrentUser();
                if (oldUser != null) {
                    // 清除libTAU中的朋友关系
                    List<FriendAndUser> friends = friendRepo.queryFriendsByUserPk(oldUser.publicKey);
                    if (friends != null && friends.size() > 0) {
                        for (Friend friend : friends) {
                            boolean isSuccess = daemon.deleteFriend(friend.friendPK);
                            logger.debug("importSeed deleteFriend::{}, isSuccess::{}, size::{}",
                                    friend.friendPK, isSuccess, friends.size());
                        }
                    }
                    // 清除libTAU中跟随的社区链
                    List<String> communities = memRepo.queryFollowedCommunities(oldUser.publicKey);
                    if (communities != null && communities.size() > 0) {
                        for (String chainID : communities) {
                            boolean isSuccess = daemon.unfollowChain(chainID);
                            logger.debug("importSeed unfollowChain::{}, isSuccess::{}, size::{}",
                                    chainID, isSuccess, communities.size());
                        }
                    }
                }
                byte[] seedBytes = ByteUtil.toByte(seed);
                Pair<byte[], byte[]> keypair = Ed25519.createKeypair(seedBytes);
                String publicKey = ByteUtil.toHexString(keypair.first);
                logger.debug("importSeed publicKey::{}, size::{}", publicKey, publicKey.length());
                User newUser = userRepo.getUserByPublicKey(publicKey);
                if (oldUser != null) {
                    userRepo.setCurrentUser(oldUser.publicKey, false);
                }
                // 1、更新本地数据库数据, TauService中观察者自动触发updateSeed
                if (null == newUser) {
                    newUser = new User(publicKey, seed, name, true);
                    // 直接更新新用户的经纬度
                    if (oldUser != null) {
                        newUser.longitude = oldUser.longitude;
                        newUser.latitude = oldUser.latitude;
                    }
                    userRepo.addUser(newUser);
                } else {
                    if (StringUtil.isNotEmpty(name)) {
                        newUser.nickname = name;
                        newUser.updateNNTime = daemon.getSessionTime() / 1000;
                    }
                    newUser.seed = seed;
                    newUser.isCurrentUser = true;
                    userRepo.updateUser(newUser);
                }
                // 2、把自己当作自己的朋友
                Friend friend = friendRepo.queryFriend(publicKey, publicKey);
                if (null == friend) {
                    friend = new Friend(publicKey, publicKey, FriendStatus.CONNECTED.getStatus());
                    friendRepo.addFriend(friend);
                }
                // 3、更新本地的用户公钥
                MainApplication.getInstance().setCurrentUser(newUser);
                logger.info("Update userPk::{}", newUser.publicKey);
                // 4、更新本地的用户公钥
                daemon.updateSeed(seed);

                // 5、向libTAU中添加当前用户的朋友关系和其跟随的社区链

                // 向libTAU中添加当前用户的朋友关系
                List<FriendAndUser> friends = friendRepo.queryFriendsByUserPk(newUser.publicKey);
                if (friends != null && friends.size() > 0) {
                    for (FriendAndUser fau : friends) {
                        boolean isSuccess = daemon.updateFriendInfo(fau.user);
                        logger.debug("importSeed updateFriendInfo::{}, isSuccess::{}, size::{}",
                                fau.friendPK, isSuccess, friends.size());
                    }
                }
                // 向libTAU中添加当前用户跟随的社区链
                List<String> communities = memRepo.queryFollowedCommunities(newUser.publicKey);
                if (communities != null && communities.size() > 0) {
                    for (String chainID : communities) {
                        List<String> list = memRepo.queryCommunityMembersLimit(chainID, Constants.CHAIN_LINK_BS_LIMIT);
                        Set<String> peers = new HashSet<>(list);
                        boolean isSuccess = daemon.followChain(chainID, peers);
                        logger.debug("importSeed followChain::{}, peers size::{}, isSuccess::{}, size::{}", chainID,
                                peers.size(), isSuccess, communities.size());
                    }
                }
            } catch (Exception e){
                result = getApplication().getString(R.string.user_seed_invalid);
                logger.debug("import seed error::{}", result);
            }
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::importSeedResult);
        disposables.add(disposable);
    }

    /**
     * 导入Seed结果
     */
    private void importSeedResult(String result){
        if (StringUtil.isEmpty(result) && commonDialog != null && commonDialog.isShowing()) {
            commonDialog.closeDialog();
        }
        changeResult.postValue(result);
    }

    /**
     * 生成新的Seed
     */
    private void generateSeed(String name) {
        byte[] seedBytes = Ed25519.createSeed();
        String seed = ByteUtil.toHexString(seedBytes);
        importSeed(seed, name);
    }

    /**
     * 观察当前用户是否变化
     */
    public MutableLiveData<UserAndFriend> getUserDetail() {
        return userDetail;
    }

    /**
     * 观察当前用户需要生成的QR内容
     */
    public MutableLiveData<QRContent> getQRContent() {
        return qrContent;
    }

    /**
     * 观察生成的二维码
     */
    public MutableLiveData<Bitmap> getQRBitmap() {
        return qrBitmap;
    }

    /**
     * 观察生成的模糊二维码
     */
    public MutableLiveData<Bitmap> getQRBlurBitmap() {
        return qrBlurBitmap;
    }

    /**
     * 观察当前用户是否变化
     */
    public Flowable<User> observeCurrentUser() {
        return userRepo.observeCurrentUser();
    }

    /**
     * 观察当前用户和其朋友列表
     */
    public void queryCurrentUserAndFriends() {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<QRContent>) emitter -> {
            try {
                User user = userRepo.getCurrentUser();
                String showName = UsersUtil.getShowName(user);
                PublicKeyQRContent content = new PublicKeyQRContent();
                content.setPublicKey(user.publicKey);
                content.setNickName(showName);
                emitter.onNext(content);
            } catch (Exception e) {
                logger.error("queryCurrentUserAndFriends error ", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> qrContent.postValue(result));
        disposables.add(disposable);
    }

    /**
     * 观察当前用户
     */
    public void queryCurrentUser() {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<QRContent>) emitter -> {
            try {
                User user = userRepo.getCurrentUser();
                String showName = UsersUtil.getShowName(user);
                SeedQRContent content = new SeedQRContent();
                content.setSeed(user.seed);
                content.setNickName(showName);
                emitter.onNext(content);
            }catch (Exception e){
                logger.error("queryCurrentUserAndFriends error ", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> qrContent.postValue(result));
        disposables.add(disposable);
    }

    /**
     * 观察Sees历史列表
     */
    Flowable<List<User>> observeSeedHistoryList() {
        return userRepo.observeSeedHistoryList();
    }

    /**
     * 获取切换Seed后结果
     */
    public MutableLiveData<String> getChangeResult() {
        return changeResult;
    }

    /**
     * 获取添加联系人的结果
     */
    public MutableLiveData<Result> getAddFriendResult() {
        return addFriendResult;
    }

    /**
     * 获取修改黑名单的结果
     */
    public MutableLiveData<Result> getEditBlacklistResult() {
        return editBlacklistResult;
    }

    /**
     * 获取用户黑名单的被观察者
     * @return 被观察者
     */
    public MutableLiveData<List<User>> getBlackList() {
        return blackList;
    }

    public MutableLiveData<Boolean> getEditRemarkResult() {
        return editRemarkResult;
    }

    public MutableLiveData<List<UserAndFriend>> getUserList() {
        return userList;
    }

    /**
     * 获取在黑名单的用户列表
     */
    public void getUsersInBlacklist() {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<List<User>>) emitter -> {
            List<User> list = userRepo.getUsersInBlacklist();
            emitter.onNext(list);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> blackList.postValue(list));
        disposables.add(disposable);
    }

    /**
     * 设置用户是否加入黑名单
     * @param publicKey 用户publicKey
     * @param blacklist 是否加入黑名单
     */
    public void setUserBlacklist(String publicKey, boolean blacklist) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            boolean isSuccess;
            if (blacklist) {
                isSuccess = daemon.deleteFriend(publicKey);
            } else {
                User friend = userRepo.getFriend(publicKey);
                isSuccess = daemon.updateFriendInfo(friend);
            }
            if (isSuccess) {
                userRepo.setUserBlacklist(publicKey, blacklist);
            }
            result.setMsg(DateUtil.getDateTime());
            result.setSuccess(isSuccess);
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    editBlacklistResult.postValue(result);
                    submitDataSetChanged();
                });
        disposables.add(disposable);
    }

    /**
     * 检查当前用户
     */
    public void checkCurrentUser() {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            User user = userRepo.getCurrentUser();
            if(null == user){
                logger.info("Create default user");
                generateSeed(null);
            } else {
                MainApplication.getInstance().setCurrentUser(user);
                logger.info("Update userPk::{}", user.publicKey);
            }
            emitter.onNext(true);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 保存用户名
     */
    private void saveUserName(String publicKey, String name) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            User user;
            if (StringUtil.isNotEmpty(publicKey)) {
                user = userRepo.getUserByPublicKey(publicKey);
            } else {
                user = userRepo.getCurrentUser();
            }
            if (user != null) {
                if (StringUtil.isNotEmpty(name)) {
                    user.nickname = name;
                    user.updateNNTime = daemon.getSessionTime() / 1000;
                }
                String currentUserPk = userRepo.getCurrentUser().publicKey;
                userRepo.updateUser(user);
                Friend friend = friendRepo.queryFriend(currentUserPk, publicKey);
                // 必须是朋友关系，才能更新朋友
                if (friend != null && friend.status != FriendStatus.DISCOVERED.getStatus()) {
                    daemon.updateFriendInfo(user);
                }
            }
            emitter.onNext(true);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    submitDataSetChanged();
                });
        disposables.add(disposable);
    }

    /**
     * 保存备注
     */
    private void saveRemarkName(String publicKey, String name) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            User user;
            if (StringUtil.isNotEmpty(publicKey)) {
                user = userRepo.getUserByPublicKey(publicKey);
            } else {
                user = userRepo.getCurrentUser();
            }
            if (user != null) {
                if (StringUtil.isNotEmpty(name)) {
                    user.remark = name;
                }
                String currentUserPk = userRepo.getCurrentUser().publicKey;
                userRepo.updateUser(user);
                Friend friend = friendRepo.queryFriend(currentUserPk, publicKey);
                // 必须是朋友关系，才能更新朋友
                if (friend != null && friend.status != FriendStatus.DISCOVERED.getStatus()) {
                    daemon.updateFriendInfo(user);
                }
            }
            emitter.onNext(true);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    submitDataSetChanged();
                    editRemarkResult.postValue(result);
                });
        disposables.add(disposable);
    }

    /**
     * 在社区页面，刷新数据
     */
    private void submitDataSetChanged(){
        txRepo.submitDataSetChanged();
    }

    /**
     * 查询用户列表
     * @return DataSource
     * @param order 排序字段
     * @param isAll 是否查询所有用户
     * @param scannedFriendPk 扫描的朋友公钥
     */
    public void loadUsersList(int order, boolean isAll, String scannedFriendPk) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<List<UserAndFriend>>) emitter -> {
            List<UserAndFriend> users = new ArrayList<>();
            try {
                String friendPk = StringUtil.isEmpty(scannedFriendPk) ? "" : scannedFriendPk;
                long startTime = System.currentTimeMillis();
                users = userRepo.getUsers(isAll, order, friendPk);
                long getUsersTime = System.currentTimeMillis();
                logger.trace("loadUsersList getUsers::{}ms, users.size::{}",
                        getUsersTime - startTime, users.size());
            } catch (Exception e) {
                logger.error("loadUsersList error::", e);
            }
            emitter.onNext(users);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(users -> {
                    userList.postValue(users);
                });
        disposables.add(disposable);
    }

    /**
     * 添加朋友
     * 根据BuildConfig.DEBUG 添加测试代码，多次调用添加测试朋友
     * @param publicKey
     */
    public void addFriend(String publicKey) {
        addFriend(publicKey, null);
    }

    /**
     * 添加发布Airdrop的peer为朋友
     * @param publicKey
     * @param airdropChain
     */
    public void addAirdropFriend(String publicKey, String airdropChain) {
        addFriend(publicKey, null, null, airdropChain);
    }

    /**
     * 添加朋友
     * 根据BuildConfig.DEBUG 添加测试代码，多次调用添加测试朋友
     * @param publicKey
     * @param nickname
     */
    public void addFriend(String publicKey, String nickname) {
        addFriend(publicKey, nickname, null, null);
    }

    public void addFriendFromLocal(String publicKey, String remark) {
        addFriend(publicKey, null, remark, null);
    }
    public void addFriend(String publicKey, String nickname, String remark,  String airdropChain) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = addFriendTask(publicKey, nickname, remark, airdropChain);

            result.setSuccess(result.isExist());
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isExist -> addFriendResult.postValue(isExist));
        disposables.add(disposable);
    }

    private Result addFriendTask(String publicKey, String nickname, String remark, String airdropChain) {
        logger.debug("AddFriendsLocally, publicKey::{}, nickname::{}", publicKey, nickname);
        Result result = new Result();
        result.setKey(publicKey);
        User user = userRepo.getUserByPublicKey(publicKey);
        if(null == user){
            logger.debug("AddFriendsLocally, new user");
            user = new User(publicKey);
            if (StringUtil.isNotEmpty(nickname)) {
                user.nickname = nickname;
                user.updateNNTime = daemon.getSessionTime() / 1000;
            }
            if (StringUtil.isNotEmpty(remark)) {
                user.remark = remark;
            }
            userRepo.addUser(user);
        } else {
            logger.debug("AddFriendsLocally, user exist");
            boolean isUpdate = false;
            if (StringUtil.isEmpty(user.nickname) && StringUtil.isNotEmpty(nickname)) {
                user.nickname = nickname;
                user.updateNNTime = daemon.getSessionTime() / 1000;
                isUpdate = true;
            }
            if (StringUtil.isNotEmpty(remark)) {
                user.remark = remark;
                isUpdate = true;
            }
            if (isUpdate) {
                userRepo.updateUser(user);
            }
        }
        String userPK = MainApplication.getInstance().getPublicKey();
        Friend friend = friendRepo.queryFriend(userPK, publicKey);

        // 更新libTAU朋友信息
        boolean isSuccess = daemon.updateFriendInfo(user);
        logger.debug("AddFriendsLocally, libTAU updateFriendInfo success::{}", isSuccess);
        boolean isExist = false;
        if (null == friend) {
            // 添加朋友
            int status = isSuccess ? FriendStatus.ADDED.getStatus() : FriendStatus.DISCOVERED.getStatus();
            friend = new Friend(userPK, publicKey, status);
            friendRepo.addFriend(friend);
            if (isSuccess) {
                result.setMsg(publicKey);
                // 发送默认消息
                sendDefaultMessage(publicKey, airdropChain);
                // 更新朋友信息
                daemon.requestFriendInfo(publicKey);
            } else {
                result.setMsg(getApplication().getString(R.string.contacts_friend_add_failed));
                logger.debug("AddFriendsLocally, {}", result.getMsg());
            }
        } else {
            // 防止与libTAU第一次不通，造成的数据异常
            if (friend.status == FriendStatus.DISCOVERED.getStatus()) {
                // 如果再次尝试成功，则触发发送默认消息
                if (isSuccess) {
                    result.setMsg(publicKey);
                    friend.status = FriendStatus.ADDED.getStatus();
                    friendRepo.updateFriend(friend);
                    // 发送默认消息
                    sendDefaultMessage(publicKey, airdropChain);
                    // 更新朋友信息
                    daemon.requestFriendInfo(publicKey);
                } else {
                    result.setMsg(getApplication().getString(R.string.contacts_friend_add_failed));
                    logger.debug("AddFriendsLocally, {}", result.getMsg());
                }
            } else {
                isExist = true;
                result.setMsg(getApplication().getString(R.string.contacts_friend_already_exists));
                logger.debug("AddFriendsLocally, {}", result.getMsg());
                if (StringUtil.isNotEmpty(airdropChain)) {
                    // 发送接受airdrop消息
                    sendDefaultMessage(publicKey, airdropChain);
                }
            }
        }
        result.setExist(isExist);
        return result;
    }

    private void sendDefaultMessage(String friendPk, String airdropChain) {
        boolean isAirdrop = StringUtil.isNotEmpty(airdropChain);
        String msg = getApplication().getString(!isAirdrop ? R.string.contacts_have_added : R.string.contacts_accepting_airdrop);
        int type = !isAirdrop ? MessageType.TEXT.getType() : MessageType.AIRDROP.getType();
        String senderPk = MainApplication.getInstance().getPublicKey();
        chatViewModel.syncSendMessageTask(senderPk, friendPk, msg, type, airdropChain);
        logger.debug("AddFriendsLocally, syncSendMessageTask::{}", msg);
    }

    /**
     * 显示用户信息的对话框
     * @param publicKey 用户公钥
     */
    public void getUserDetail(String publicKey) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<UserAndFriend>) emitter -> {
            UserAndFriend userAndFriend = userRepo.getFriend(publicKey);
            if(null == userAndFriend){
                userAndFriend = new UserAndFriend(publicKey);
            }
            emitter.onNext(userAndFriend);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(user -> {
                    userDetail.postValue(user);
                });
        disposables.add(disposable);
    }

    /**
     * 显示编辑名字的对话框
     */
    public void showEditNameDialog(AppCompatActivity activity, String publicKey) {
        ContactsDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.contacts_dialog, null, false);
        binding.etPublicKey.setHint(R.string.user_new_name_hint);
        binding.ivClose.setOnClickListener(v -> {
            if (editNameDialog != null) {
                editNameDialog.closeDialog();
            }
        });
        binding.tvSubmit.setOnClickListener(v -> {
            String newName = StringUtil.getText(binding.etPublicKey);
            if (StringUtil.isNotEmpty(newName)) {
                byte[] nameBytes = Utils.textStringToBytes(newName);
                int nicknameLength = null == nameBytes ? 0 : nameBytes.length;
                if (nicknameLength > Constants.NICKNAME_LENGTH) {
                    ToastUtils.showShortToast(R.string.user_new_name_too_long);
                } else {
                    saveUserName(publicKey, newName);
                    if (editNameDialog != null) {
                        editNameDialog.closeDialog();
                    }
                }
            } else {
                ToastUtils.showShortToast(R.string.user_invalid_new_name);
            }
        });
        if (editNameDialog != null && editNameDialog.isShowing()) {
            editNameDialog.closeDialog();
        }
        editNameDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .setCanceledOnTouchOutside(false)
                .setButtonWidth(R.dimen.widget_size_240)
                .create();
        editNameDialog.show();
    }

    /**
     * 显示编辑名字的对话框
     */
    public void showRemarkDialog(AppCompatActivity activity, String publicKey) {
        ContactsDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.contacts_dialog, null, false);
        binding.etPublicKey.setHint(R.string.user_remark);
        binding.ivClose.setOnClickListener(v -> {
            if (editNameDialog != null) {
                editNameDialog.closeDialog();
            }
        });
        binding.tvSubmit.setOnClickListener(v -> {
            String newName = StringUtil.getText(binding.etPublicKey);
            byte[] nameBytes = Utils.textStringToBytes(newName);
            int nicknameLength = null == nameBytes ? 0 : nameBytes.length;
            if (nicknameLength > Constants.NICKNAME_LENGTH) {
                ToastUtils.showShortToast(R.string.user_remark_too_long);
            } else {
                saveRemarkName(publicKey, newName);
                if (editNameDialog != null) {
                    editNameDialog.closeDialog();
                }
            }
        });
        editNameDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .setCanceledOnTouchOutside(false)
                .setButtonWidth(R.dimen.widget_size_240)
                .create();
        editNameDialog.show();
    }

    /**
     * 显示添加朋友的对话框
     */
    public void showAddFriendDialog(AppCompatActivity activity) {
        AddFriendDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.add_friend_dialog, null, false);
        binding.ivClose.setOnClickListener(v -> {
            if (commonDialog != null) {
                commonDialog.closeDialog();
            }
        });
        binding.tvSubmit.setOnClickListener(v -> {
            String publicKey = StringUtil.getText(binding.etPublicKey);
            try {
                if (StringUtil.isNotEmpty(publicKey) &&
                        ByteUtil.toByte(publicKey).length == Ed25519.PUBLIC_KEY_SIZE) {
                    String remark = StringUtil.getText(binding.etRemark);
                    addFriendFromLocal(publicKey, remark);
                    return;
                }
            } catch (Exception ignore) { }
            ToastUtils.showShortToast(R.string.user_invalid_friend_pk);
        });
        commonDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .setCanceledOnTouchOutside(false)
                .setButtonWidth(R.dimen.widget_size_240)
                .create();
        commonDialog.show();
    }

    /**
     * 显示Ban User的对话框
     */
    public void showBanDialog(BaseActivity activity, String publicKey, String showName) {
        if (settingsRepo.doNotShowBanDialog()) {
            setUserBlacklist(publicKey, true);
            return;
        }
        BanDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.ban_dialog, null, false);
        binding.tvName.setText(showName);
        binding.ivClose.setOnClickListener(v -> {
            if (commonDialog != null) {
                commonDialog.closeDialog();
            }
        });
        binding.tvSubmit.setOnClickListener(v -> {
            if (commonDialog != null) {
                commonDialog.closeDialog();
                if (binding.cbDoNotShow.isChecked()) {
                    settingsRepo.doNotShowBanDialog(true);
                }
            }
            setUserBlacklist(publicKey, true);
        });
        commonDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .create();
        commonDialog.show();
    }

    /**
     * 分享QRCOde
     */
    public void shareQRCode(AppCompatActivity activity, Drawable drawable, int size) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            try {
                if (drawable != null) {
                    Bitmap bitmap = BitmapUtil.drawableToBitmap(drawable);
                    logger.debug("shareQRCode bitmap::{}", bitmap);
                    if (bitmap != null) {
                        Bitmap.createBitmap(bitmap);
                        int w = bitmap.getWidth();
                        int h = bitmap.getHeight();
                        float sx = (float) size / w;
                        float sy = (float) size / h;
                        Matrix matrix = new Matrix();
                        matrix.postScale(sx, sy);
                        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0,
                                w, h, matrix, true);

                        // 清除旧二维码
                        String filePath = FileUtil.getQRCodeFilePath();
                        File dir = new File(filePath);
                        File[] files = dir.listFiles();
                        for (File file : files) {
                            file.delete();
                        }
                        // 添加时间戳，防止数据分享的还是旧数据
                        String fileName = String.format(QR_CODE_NAME, DateUtil.getDateTime());
                        filePath += fileName;

                        logger.debug("shareQRCode filePath::{}", filePath);

                        FileUtil.saveFilesDirBitmap(filePath, resizeBmp);
                        Context context = MainApplication.getInstance();
                        ActivityUtil.sharePic(activity, filePath, context
                                .getString(R.string.contacts_share_qr_code));
                    }
                }
            } catch (Exception e) {
                logger.error("shareQRCode error", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 第一次APP启动，提示用户操作：先提示引导页，再设置昵称和每日流量限制
     * @param activity
     * @param user
     */
    public void promptUserFirstStartApp(AppCompatActivity activity, User user) {
        String firstStartKey = activity.getString(R.string.pref_key_first_start);
        boolean isFirstStart = settingsRepo.getBooleanValue(firstStartKey, true);
        boolean isForeground = AppUtil.isForeground(activity, MainActivity.class.getName());
        // 如果APP是第一次启动, 并且MainActivity也在前台
        if (isFirstStart && isForeground) {
            GuideDialog.Builder builder = new GuideDialog.Builder(activity);
            builder.setCanceledOnTouchOutside(false);
            builder.setGuideListener(() -> {
                // 如果用户没有nickname
                settingsRepo.setBooleanValue(firstStartKey, false);
                String showName = UsersUtil.getCurrentUserName(user);
                String defaultName = UsersUtil.getDefaultName(user.publicKey);
                logger.trace("promptUserFirstStart showName::{}, defaultName::{}", showName, defaultName);
                if (StringUtil.isEquals(showName, defaultName)) {
                    showEditNameDialog(activity, user.publicKey);
                    showEditNameDialog(activity, user.publicKey);
                }
            });
            if (guideDialog != null && guideDialog.isShowing()) {
                guideDialog.closeDialog();
            }
            guideDialog = builder.create();
            guideDialog.show();
        }
    }

    /**
     * 生成二维码
     * @param context
     * @param qrContent 二维码内容
     */
    public void generateQRCode(Context context, QRContent qrContent) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Bitmap>) emitter -> {
            try {
                String publicKey = null;
                if (qrContent instanceof SeedQRContent) {
                    String seed = ((SeedQRContent) qrContent).getSeed();
                    Pair<byte[], byte[]> keypair = Ed25519.createKeypair(ByteUtil.toByte(seed));
                    publicKey = ByteUtil.toHexString(keypair.first);
                } else if (qrContent instanceof PublicKeyQRContent) {
                    publicKey = ((PublicKeyQRContent) qrContent).getPublicKey();
                }
                String content = new Gson().toJson(qrContent);
                int bgColor = Utils.getGroupColor(publicKey);
                String firstLettersName = UsersUtil.getQRCodeName(qrContent.getNickName());
                Bitmap logoBitmap = BitmapUtil.createLogoBitmap(bgColor, firstLettersName);

                int heightPix = context.getApplicationContext().getResources().getDimensionPixelSize(R.dimen.widget_size_300);
                int widthPix = heightPix;
                //Generate the QR Code.
                HashMap<EncodeHintType, Object> hints = new HashMap<>();
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
                hints.put(EncodeHintType.MARGIN, 0);
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                QRCodeWriter writer = new QRCodeWriter();
                Bitmap bitmap = writer.encode(content, BarcodeFormat.QR_CODE, widthPix, heightPix,
                        hints, logoBitmap, context);
                logger.debug("shareQRCode bitmap::{}", bitmap);
                if (bitmap != null) {
                    emitter.onNext(bitmap);
                }
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
     * 把二维码转化为模糊二维码
     * @param bitmap
     */
    public void generateBlurQRCode(Bitmap bitmap) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Bitmap>) emitter -> {
            try {
//                Bitmap blurBitmap = BitmapUtil.blurBitmap(bitmap, 80, false);
                Bitmap blurBitmap = BitmapUtil.blurBitmap(bitmap, false);
                emitter.onNext(blurBitmap);
            } catch (Exception e) {
                logger.error("queryCurrentUserAndFriends error ", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> qrBlurBitmap.postValue(result));
        disposables.add(disposable);
    }

    /**
     * 观察朋友信息变化
     */
    public Flowable<FriendAndUser> observeFriend(String friendPk) {
        return userRepo.observeFriend(friendPk);
    }

    /**
     * 清除朋友的消息未读状态
     */
    public void clearMsgUnread(String friendPK) {
        markReadOrUnread(friendPK, 0);
    }
    public void markReadOrUnread(String friendPK, int status) {
        if (clearDisposable != null && !clearDisposable.isDisposed()) {
            return;
        }
        clearDisposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            try {
                String userPk = MainApplication.getInstance().getPublicKey();
                Friend friend = friendRepo.queryFriend(userPk, friendPK);
                if (friend != null && friend.msgUnread != status) {
                    friend.msgUnread = status;
                    friendRepo.updateFriend(friend);
                }
            } catch (Exception e) {
                logger.error("markReadOrUnread error ", e);
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    public void topStickyOrRemove(String friendPK, int status) {
        if (clearDisposable != null && !clearDisposable.isDisposed()) {
            return;
        }
        clearDisposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            try {
                String userPk = MainApplication.getInstance().getPublicKey();
                Friend friend = friendRepo.queryFriend(userPk, friendPK);
                if (friend != null && friend.stickyTop != status) {
                    friend.stickyTop = status;
                    friendRepo.updateFriend(friend);
                }
            } catch (Exception e) {
                logger.error("topStickyOrRemove error ", e);
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    public void closeDialog() {
        if (commonDialog != null) {
            commonDialog.closeDialog();
        }
    }

    public Observable<String> observeUserDataSetChanged() {
        return userRepo.observeDataSetChanged();
    }

    public Observable<String> observeMemberDataSetChanged() {

        return memRepo.observeDataSetChanged();
    }

    public Observable<String> observeFriendDataSetChanged() {
        return friendRepo.observeDataSetChanged();
    }

    public Observable<Integer> batchAddFriends(String name, int num) {
        return Observable.create(emitter -> {
            if (num > 0) {
                for (int i = 0; i < num; i++) {
                    String friendName = name + (i + 1);
                    byte[] seedBytes = Ed25519.createSeed();
                    Pair<byte[], byte[]> keypair = Ed25519.createKeypair(seedBytes);
                    String friendPk = ByteUtil.toHexString(keypair.first);
                    addFriend(friendPk, friendName);
                    emitter.onNext(i + 1);
                }
            }
            emitter.onComplete();
        });
    }

    /**
     * 请求更新朋友信息
     */
    public void requestFriendInfo(String friendPk) {
        if (StringUtil.isNotEmpty(friendPk)) {
            daemon.requestFriendInfo(friendPk);
        }
    }

    public void updateHeadPic(LocalMedia media) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            byte[] headPic = MediaUtil.media2Bytes(media);
            User user = userRepo.getCurrentUser();
            if (headPic != null) {
                user.headPic = headPic;
                user.updateHPTime = daemon.getSessionTime() / 1000;
                daemon.updateFriendInfo(user);
                userRepo.updateUser(user);
            }
            MediaUtil.deleteAllCacheImageFile();
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }
}