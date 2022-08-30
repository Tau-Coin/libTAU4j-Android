package io.taucoin.tauapp.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.taucoin.tauapp.publishing.core.model.data.FriendAndUser;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Friend;

/**
 * Room:Friend操作接口
 */
@Dao
public interface FriendDao {
    String QUERY_FRIEND = "SELECT * FROM Friends" +
            " WHERE userPK = :userPK AND friendPK = :friendPK";

    String QUERY_FRIENDS_BY_USER_PK = "SELECT * FROM Friends" +
            " WHERE userPK = :userPK" +
            " AND friendPK NOT IN " + UserDao.QUERY_GET_USER_PKS_IN_BAN_LIST +
            " AND status != 0";

    /**
     * 添加新社区成员
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addFriend(Friend friend);

    @Update
    void updateFriend(Friend friend);

    @Query(QUERY_FRIEND)
    Friend queryFriend(String userPK, String friendPK);

    /**
     * 查询自己的朋友
     */
    @Query(QUERY_FRIENDS_BY_USER_PK)
    @Transaction
    List<FriendAndUser> queryFriendsByUserPk(String userPK);
}
