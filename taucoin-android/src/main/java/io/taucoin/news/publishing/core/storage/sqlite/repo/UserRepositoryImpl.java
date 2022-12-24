package io.taucoin.news.publishing.core.storage.sqlite.repo;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.room.RxRoom;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.taucoin.news.publishing.core.model.data.FriendAndUser;
import io.taucoin.news.publishing.core.model.data.UserAndFriend;
import io.taucoin.news.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.news.publishing.core.storage.sqlite.entity.User;
import io.taucoin.news.publishing.core.utils.DateUtil;

/**
 * UserRepository接口实现
 */
public class UserRepositoryImpl implements UserRepository {

    private final PublishSubject<String> dataSetChangedPublish = PublishSubject.create();
    private final ExecutorService sender = Executors.newSingleThreadExecutor();
    private Context appContext;
    private final AppDatabase db;

    /**
     * CommunityRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public UserRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    /**
     * 添加新的User/Seed
     * @param user User实例
     * @return 结果
     */
    @Override
    public void addUser(@NonNull User user) {
        try {
            db.userDao().addUser(user);
            submitDataSetChanged();
        } catch (SQLiteConstraintException ignore) { }
    }

    /**
     * 更新的User/Seed
     * @param user User实例
     * @return 结果
     */
    @Override
    public int updateUser(@NonNull User user) {
        int result = db.userDao().updateUser(user);
        submitDataSetChanged();
        return result;
    }

    /**
     * 获取当前的用户
     * @return 当前用户User实例
     */
    @Override
    public User getCurrentUser() {
        return db.userDao().getCurrentUser();
    }

    @Override
    public List<User> getUserList() {
        return db.userDao().getUserList();
    }

    /**
     * 观察当前用户信息是否变化
     * @return 当前用户最新User实例
     */
    @Override
    public Flowable<User> observeCurrentUser() {
        return db.userDao().observeCurrentUser();
    }

    /**
     * 设置当前用户是否是当前用户
     * @param isCurrentUser 是否是当前用户
     */
    @Override
    public void setCurrentUser(String publicKey, boolean isCurrentUser){
        db.userDao().setCurrentUser(publicKey, isCurrentUser);
    }

    /**
     * 获取在黑名单的用户列表
     * @return  List<User>
     */
    @Override
    public List<User> getUsersInBlacklist(){
        return db.userDao().getUsersInBlacklist();
    }

    @Override
    public List<User> getCommunityUsersInBlacklist(){
        return db.userDao().getCommunityUsersInBlacklist();
    }


    /**
     * 设置用户是否加入黑名单
     * @param publicKey 公钥
     * @param blacklist 是否加入黑名单
     */
    @Override
    public void setUserBlacklist(String publicKey, boolean blacklist) {
        db.userDao().setUserBlacklist(publicKey, blacklist ? 1 : 0);
        submitDataSetChanged();
    }

    @Override
    public void setCommunityUserBlacklist(String publicKey, boolean blacklist) {
        db.userDao().setCommunityUserBlacklist(publicKey, blacklist ? 1 : 0);
        submitDataSetChanged();
    }

    /**
     * 观察Sees历史列表
     */
    @Override
    public Flowable<List<User>> observeSeedHistoryList(){
        return db.userDao().observeSeedHistoryList();
    }

    /**
     * 根据公钥获取用户
     * @param publicKey 公钥
     * @return 当前用户User实例
     */
    @Override
    public User getUserByPublicKey(String publicKey){
        return db.userDao().getUserByPublicKey(publicKey);
    }

    /**
     * 添加新的多个User
     */
    @Override
    public long[] addUsers(User... user){
        long[] result = db.userDao().addUsers(user);
        submitDataSetChanged();
        return result;
    }

    @Override
    public List<UserAndFriend> getUsers(int order, @NonNull String friendPk, int pos, int pageSize) {
        List<UserAndFriend> list;
        if (order == 0) {
            list = db.userDao().queryUserListByLastSeenTime(friendPk, pos, pageSize);
        } else {
            list = db.userDao().queryUserListByLastCommTime(friendPk, pos, pageSize);
        }
        if (null == list) {
            list = new ArrayList<>();
        }
        return list;
    }

    /**
     * 获取用户和朋友的信息
     */
    @Override
    public UserAndFriend getFriend(String publicKey){
        return db.userDao().getFriend(publicKey);
    }


    /**
     * 观察朋友信息变化
     */
    @Override
    public Flowable<FriendAndUser> observeFriend(String friendPk) {
        return db.userDao().observeFriend(friendPk);
    }

    @Override
    public Observable<String> observeDataSetChanged() {
        return dataSetChangedPublish;
    }

    @Override
    public Flowable<Object> observeUsersChanged() {
        String[] tables = new String[]{"Users", "Friends","Members"};
        return RxRoom.createFlowable(db, tables);
    }

    @Override
    public void submitDataSetChanged() {
        String dateTime = DateUtil.getDateTime();
        sender.submit(() -> dataSetChangedPublish.onNext(dateTime));
    }
}
