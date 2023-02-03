package io.taucbd.news.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.taucbd.news.publishing.core.model.data.FriendAndUser;
import io.taucbd.news.publishing.core.model.data.UserAndFriend;
import io.taucbd.news.publishing.core.storage.sqlite.entity.User;

/**
 * Room:User操作接口
 */
@Dao
public interface UserDao {
    String QUERY_GET_CURRENT_USER = "SELECT * FROM Users WHERE isCurrentUser = 1";
    String QUERY_GET_CURRENT_USER_SEED = "SELECT seed FROM Users WHERE isCurrentUser = 1";
    String QUERY_GET_CURRENT_USER_PK = "SELECT publicKey FROM Users WHERE isCurrentUser = 1";
    String QUERY_GET_USER_LIST = "SELECT * FROM Users";
    String QUERY_GET_USERS_PKS_IN_BAN_LIST = "SELECT publicKey FROM Users where isBanned = 1 and isCurrentUser != 1";
    String QUERY_GET_USERS_IN_BAN_LIST = "SELECT * FROM Users where isBanned = 1 and isCurrentUser != 1";
    String QUERY_GET_COMMUNITY_USERS_IN_BAN_LIST = "SELECT * FROM Users where isMemBanned = 1 and isCurrentUser != 1";

    // 查询所有用户数据的详细sql
    String QUERY_GET_USERS_NOT_IN_BAN_LIST = "SELECT u.*, f.lastCommTime AS lastCommTime," +
            " f.lastSeenTime AS lastSeenTime, f.status, f.onlineCount" +
            " FROM Users u" +
            " LEFT JOIN Friends f ON u.publicKey = f.friendPK AND f.userPK = (" +
            UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " WHERE (u.isBanned = 0 OR u.isCurrentUser = 1) AND" +
            " u.publicKey != :friendPK";

    String QUERY_USER_LIST_BY_LAST_SEEN_TIME = QUERY_GET_USERS_NOT_IN_BAN_LIST +
            " ORDER BY f.lastSeenTime DESC, u.rowid DESC LIMIT :pageSize OFFSET :pos";

    String QUERY_USER_LIST_BY_LAST_COMM_TIME = QUERY_GET_USERS_NOT_IN_BAN_LIST +
            " ORDER BY f.lastCommTime DESC, u.rowid DESC LIMIT :pageSize OFFSET :pos";

    String QUERY_SET_CURRENT_USER = "UPDATE Users SET isCurrentUser = :isCurrentUser WHERE publicKey = :publicKey";
    String QUERY_ADD_USER_BLACKLIST = "UPDATE Users SET isBanned = :isBanned WHERE publicKey = :publicKey";
    String QUERY_ADD_COMMUNITY_USER_BLACKLIST = "UPDATE Users SET isMemBanned = :isBanned WHERE publicKey = :publicKey";
    String QUERY_SEED_HISTORY_LIST = "SELECT * FROM Users WHERE seed not null";
    String QUERY_USER_BY_PUBLIC_KEY = "SELECT * FROM Users WHERE publicKey = :publicKey";

    String QUERY_FRIEND_INFO_BY_PUBLIC_KEY = "SELECT u.*, f.lastCommTime AS lastCommTime," +
            " f.lastSeenTime AS lastSeenTime, f.status, f.onlineCount" +
            " FROM Users u" +
            " LEFT JOIN Friends f ON u.publicKey = f.friendPK and f.userPK = (" + QUERY_GET_CURRENT_USER_PK + ")" +
            " where u.publicKey = :publicKey";

    String QUERY_GET_USER_PKS_IN_BAN_LIST = " (SELECT publicKey FROM Users WHERE isBanned = 1 and isCurrentUser != 1) ";
    String QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST = " (SELECT publicKey FROM Users WHERE isMemBanned = 1 and isCurrentUser != 1) ";

    String QUERY_FRIEND_BY_PUBLIC_KEY = "SELECT * FROM Friends" +
            " where friendPk =:friendPk and userPK = (" + QUERY_GET_CURRENT_USER_PK + ")";

    /**
     * 添加新的User/Seed
     */
    @Insert()
    long addUser(User user);

    /**
     * 添加新的多个User
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] addUsers(User... user);

    /**
     * 更新的User/Seed
     */
    @Update
    int updateUser(User user);

    /**
     * 获取当前的用户
     */
    @Query(QUERY_GET_CURRENT_USER)
    User getCurrentUser();

    @Query(QUERY_GET_USER_LIST)
    List<User> getUserList();

    /**
     * 观察当前用户信息是否变化
     */
    @Query(QUERY_GET_CURRENT_USER)
    Flowable<User> observeCurrentUser();

    @Query(QUERY_GET_CURRENT_USER_SEED)
    Flowable<String> observeCurrentUserSeed();

    /**
     * 设置当前用户是否是当前用户
     * @param isCurrentUser 是否是当前用户
     */
    @Query(QUERY_SET_CURRENT_USER)
    void setCurrentUser(String publicKey, boolean isCurrentUser);

    /**
     * 获取在黑名单的用户列表
     * @return  List<User>
     */
    @Query(QUERY_GET_USERS_IN_BAN_LIST)
    List<User> getUsersInBlacklist();

    @Query(QUERY_GET_COMMUNITY_USERS_IN_BAN_LIST)
    List<User> getCommunityUsersInBlacklist();

    /**
     * 设置用户是否加入黑名单
     */
    @Query(QUERY_ADD_USER_BLACKLIST)
    void setUserBlacklist(String publicKey, int isBanned);

    /**
     * 设置社区用户是否加入黑名单
     */
    @Query(QUERY_ADD_COMMUNITY_USER_BLACKLIST)
    void setCommunityUserBlacklist(String publicKey, int isBanned);

    /**
     * 观察Sees历史列表
     */
    @Query(QUERY_SEED_HISTORY_LIST)
    Flowable<List<User>> observeSeedHistoryList();

    /**
     * 根据公钥获取用户
     * @param publicKey 公钥
     * @return 当前用户User实例
     */
    @Query(QUERY_USER_BY_PUBLIC_KEY)
    User getUserByPublicKey(String publicKey);

    /**
     * 观察不在黑名单的列表中
     */
    @Transaction
    @Query(QUERY_USER_LIST_BY_LAST_COMM_TIME)
    List<UserAndFriend> queryUserListByLastCommTime(@NonNull String friendPK, int pos, int pageSize);

    @Transaction
    @Query(QUERY_USER_LIST_BY_LAST_SEEN_TIME)
    List<UserAndFriend> queryUserListByLastSeenTime(@NonNull String friendPK, int pos, int pageSize);

    /**
     * 获取用户和朋友的信息
     */
    @Transaction
    @Query(QUERY_FRIEND_INFO_BY_PUBLIC_KEY)
    UserAndFriend getFriend(String publicKey);

    /**
     * 观察朋友信息变化
     */
    @Query(QUERY_FRIEND_BY_PUBLIC_KEY)
    @Transaction
    Flowable<FriendAndUser> observeFriend(String friendPk);
}