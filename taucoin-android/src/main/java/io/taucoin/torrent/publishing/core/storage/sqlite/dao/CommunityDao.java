package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndMember;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;

/**
 * Room:Community操作接口
 */
@Dao
public interface CommunityDao {
    String QUERY_GET_CURRENT_USER_PK = " (SELECT publicKey FROM Users WHERE isCurrentUser = 1 limit 1) ";
    String QUERY_GET_BANNED_USER_PK = " (SELECT publicKey FROM Users WHERE isBanned == 1 and isCurrentUser != 1) ";
    String QUERY_NEWEST_MSG_DESC = " (SELECT * FROM (SELECT * FROM (" +
            " SELECT timestamp, content, contentType, logicMsgHash, senderPk, receiverPk, receiverPk AS receiverPkTemp" +
            " FROM (SELECT rowid, * FROM ChatMessages" +
            " WHERE senderPk = " + QUERY_GET_CURRENT_USER_PK +
            " ORDER BY timestamp DESC, logicMsgHash COLLATE UNICODE DESC) GROUP BY receiverPk" +
            " UNION ALL" +
            " SELECT timestamp, content, contentType, logicMsgHash, senderPk, receiverPk, senderPk AS receiverPkTemp" +
            " FROM (SELECT rowid, * FROM ChatMessages" +
            " WHERE receiverPk = "+ QUERY_GET_CURRENT_USER_PK +
            " ORDER BY timestamp DESC, logicMsgHash COLLATE UNICODE DESC) GROUP BY senderPk)" +

            " ORDER BY timestamp DESC, logicMsgHash COLLATE UNICODE DESC)" +
            " GROUP BY receiverPkTemp)";

    String QUERY_NEWEST_MSG_ASC = " (SELECT * FROM (SELECT * FROM (" +
            " SELECT timestamp, content, contentType, logicMsgHash, senderPk, receiverPk, receiverPk AS receiverPkTemp" +
            " FROM (SELECT rowid, * FROM ChatMessages" +
            " WHERE senderPk = " + QUERY_GET_CURRENT_USER_PK +
            " ORDER BY timestamp, logicMsgHash COLLATE UNICODE) GROUP BY receiverPk" +
            " UNION ALL" +
            " SELECT timestamp, content, contentType, logicMsgHash, senderPk, receiverPk, senderPk AS receiverPkTemp" +
            " FROM (SELECT rowid, * FROM ChatMessages" +
            " WHERE receiverPk = "+ QUERY_GET_CURRENT_USER_PK +
            " ORDER BY timestamp, logicMsgHash COLLATE UNICODE) GROUP BY senderPk)" +

            " ORDER BY timestamp, logicMsgHash COLLATE UNICODE)" +
            " GROUP BY receiverPkTemp)";

    String QUERY_COMMUNITIES_ASC = "SELECT a.chainID AS ID, a.headBlock, b.balance, b.power, b.blockNumber," +
            " (CASE WHEN b.publicKey IS NULL THEN 0 ELSE 1 END) AS joined," +
            " 0 AS type, 0 AS msgType, '' AS senderPk, '' AS receiverPk, " +
            " 0 AS msgUnread, '' AS msg, c.memo, c.timestamp" +
            " FROM Communities AS a" +
            " LEFT JOIN Members AS b ON a.chainID = b.chainID" +
            " AND b.publicKey = " + QUERY_GET_CURRENT_USER_PK +
            " LEFT JOIN (SELECT timestamp, memo, chainID FROM (SELECT timestamp, memo, chainID FROM Txs" +
            " WHERE senderPk NOT IN " + QUERY_GET_BANNED_USER_PK +
            " ORDER BY timestamp) GROUP BY chainID) AS c" +
            " ON a.chainID = c.chainID" +
            " WHERE isBanned == 0";

    String QUERY_COMMUNITIES_DESC = "SELECT a.chainID AS ID, a.headBlock, b.balance, b.power, b.blockNumber," +
            " (CASE WHEN b.publicKey IS NULL THEN 0 ELSE 1 END) AS joined," +
            " 0 AS type, 0 AS msgType, '' AS senderPk, '' AS receiverPk, " +
            " 0 AS msgUnread, '' AS msg, c.memo, c.timestamp" +
            " FROM Communities AS a" +
            " LEFT JOIN Members AS b ON a.chainID = b.chainID" +
            " AND b.publicKey = " + QUERY_GET_CURRENT_USER_PK +
            " LEFT JOIN (SELECT timestamp, memo, chainID FROM (SELECT timestamp, memo, chainID FROM Txs" +
            " WHERE senderPk NOT IN " + QUERY_GET_BANNED_USER_PK +
            " ORDER BY timestamp DESC) GROUP BY chainID) AS c" +
            " ON a.chainID = c.chainID" +
            " WHERE isBanned == 0";

    String QUERY_FRIENDS_ASC = "SELECT f.friendPK AS ID, 0 AS headBlock, 0 AS balance, 0 AS power, 0 AS blockNumber," +
            " 0 AS joined, 1 AS type, cm.contentType AS msgType," +
            " cm.senderPk AS senderPk, cm.receiverPk AS receiverPk," +
            " f.msgUnread AS msgUnread," +
            " cm.content AS msg, '' AS memo, cm.timestamp AS timestamp" +
            " FROM Friends f" +
            " LEFT JOIN " + QUERY_NEWEST_MSG_ASC + " AS cm" +
            " ON (f.userPK = cm.senderPk AND f.friendPK = cm.receiverPk)" +
            " OR (f.userPK = cm.receiverPk AND f.friendPK = cm.senderPk)" +
            " WHERE f.userPk = " + QUERY_GET_CURRENT_USER_PK +
            " AND f.friendPK NOT IN " + QUERY_GET_BANNED_USER_PK;

    String QUERY_FRIENDS_DESC = "SELECT f.friendPK AS ID,0 AS headBlock, 0 AS balance, 0 AS power, 0 AS blockNumber," +
            " 0 AS joined, 1 AS type, cm.contentType AS msgType," +
            " cm.senderPk AS senderPk, cm.receiverPk AS receiverPk," +
            " f.msgUnread AS msgUnread," +
            " cm.content AS msg, '' AS memo, cm.timestamp AS timestamp" +
            " FROM Friends f" +
            " LEFT JOIN " + QUERY_NEWEST_MSG_DESC + " AS cm" +
            " ON (f.userPK = cm.senderPk AND f.friendPK = cm.receiverPk)" +
            " OR (f.userPK = cm.receiverPk AND f.friendPK = cm.senderPk)" +
            " WHERE f.userPk = " + QUERY_GET_CURRENT_USER_PK +
            " AND f.friendPK NOT IN " + QUERY_GET_BANNED_USER_PK;

    String QUERY_COMMUNITIES_AND_FRIENDS_DESC = "SELECT * FROM (" + QUERY_FRIENDS_DESC +
            " UNION ALL " + QUERY_COMMUNITIES_DESC + ")" +
            " ORDER BY timestamp DESC";

    String QUERY_COMMUNITIES_AND_FRIENDS_ASC = "SELECT * FROM (" + QUERY_FRIENDS_ASC +
            " UNION ALL " + QUERY_COMMUNITIES_ASC + ")" +
            " ORDER BY timestamp DESC";

    String QUERY_GET_COMMUNITIES_IN_BLACKLIST = "SELECT * FROM Communities where isBanned = 1";
    String QUERY_GET_COMMUNITY_BY_CHAIN_ID = "SELECT * FROM Communities WHERE chainID = :chainID";
    String QUERY_ADD_COMMUNITY_BLACKLIST = "Update Communities set isBanned =:isBanned WHERE chainID = :chainID";
    String QUERY_JOINED_COMMUNITY = "SELECT * FROM Communities";
    String QUERY_CLEAR_COMMUNITY_STATE = "UPDATE Communities SET headBlock = 0, tailBlock = 0" +
            " WHERE chainID = :chainID";

    String QUERY_CURRENT_COMMUNITY_MEMBER = "SELECT c.*, m.balance, m.balance, m.power, m.blockNumber," +
            " (CASE WHEN m.publicKey IS NULL THEN 0 ELSE 1 END) AS joined" +
            " FROM Communities c" +
            " LEFT JOIN Members m ON c.chainID = m.chainID AND m.publicKey = :publicKey" +
            " WHERE c.chainID = :chainID";

    String QUERY_CHAIN_TOP_COIN_MEMBERS = "SELECT * FROM Members" +
            " WHERE chainID = :chainID" +
            " ORDER BY balance DESC LIMIT :topNum";

    String QUERY_CHAIN_TOP_POWER_MEMBERS = "SELECT * FROM Members" +
            " WHERE chainID = :chainID"  +
            " ORDER BY power DESC LIMIT :topNum";

    /**
     * 添加新的社区
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addCommunity(Community community);

    /**
     * 更新社区
     */
    @Update
    int updateCommunity(Community community);

    /**
     * 根据chainIDc查询社区
     */
    @Query(QUERY_GET_COMMUNITY_BY_CHAIN_ID)
    Community getCommunityBychainID(String chainID);

    /**
     * 查询不在黑名单中的社区列表（逆序）
     */
    @Query(QUERY_COMMUNITIES_AND_FRIENDS_DESC)
    @Transaction
    Flowable<List<CommunityAndFriend>> observeCommunitiesAndFriendsDESC();

    /**
     * 查询不在黑名单中的社区列表（正序）
     */
    @Query(QUERY_COMMUNITIES_AND_FRIENDS_ASC)
    @Transaction
    Flowable<List<CommunityAndFriend>> observeCommunitiesAndFriendsASC();

    /**
     * 获取在黑名单的社区列表
     */
    @Query(QUERY_GET_COMMUNITIES_IN_BLACKLIST)
    List<Community> getCommunitiesInBlacklist();

    /**
     * 添加社区黑名单
     */
    @Query(QUERY_ADD_COMMUNITY_BLACKLIST)
    void setCommunityBlacklist(String chainID, int isBanned);

    /**
     * 获取用户加入的社区列表
     */
    @Query(QUERY_JOINED_COMMUNITY)
    List<Community> getJoinedCommunityList();

    /**
     * 根据chainID查询社区
     * @param chainID 社区chainID
     */
    @Query(QUERY_GET_COMMUNITY_BY_CHAIN_ID)
    Single<Community> getCommunityByChainIDSingle(String chainID);

    @Query(QUERY_GET_COMMUNITY_BY_CHAIN_ID)
    Observable<Community> observerCommunityByChainID(String chainID);

    @Query(QUERY_CLEAR_COMMUNITY_STATE)
    void clearCommunityState(String chainID);

    @Query(QUERY_CURRENT_COMMUNITY_MEMBER)
    Observable<CommunityAndMember> observerCurrentMember(String chainID, String publicKey);

    /**
     * 观察链上币量前topNum的成员
     */
    @Query(QUERY_CHAIN_TOP_COIN_MEMBERS)
    Observable<List<Member>> observeChainTopCoinMembers(String chainID, int topNum);

    /**
     * 观察链上Power前topNum的成员
     */
    @Query(QUERY_CHAIN_TOP_POWER_MEMBERS)
    Observable<List<Member>> observeChainTopPowerMembers(String chainID, int topNum);
}