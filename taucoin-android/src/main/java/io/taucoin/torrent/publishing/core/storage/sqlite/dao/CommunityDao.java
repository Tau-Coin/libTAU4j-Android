package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Flowable;
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

    String QUERY_COMMUNITIES_ASC = "SELECT a.chainID AS ID, a.headBlock, a.tailBlock, b.balance, b.nonce," +
            " (CASE WHEN b.publicKey IS NULL THEN 0 ELSE 1 END) AS joined," +
            " 0 AS type, '' AS senderPk, '' AS receiverPk," +
            " b.msgUnread AS msgUnread, b.stickyTop AS stickyTop, 0 AS focused, null AS msg, c.memo, c.timestamp" +
            " FROM Communities AS a" +
            " LEFT JOIN Members AS b ON a.chainID = b.chainID" +
            " AND b.publicKey = " + QUERY_GET_CURRENT_USER_PK +
            " LEFT JOIN (SELECT timestamp, memo, chainID FROM (SELECT timestamp, memo, chainID FROM Txs" +
            " WHERE senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " ORDER BY timestamp) GROUP BY chainID) AS c" +
            " ON a.chainID = c.chainID" +
            " WHERE isBanned == 0";

    String QUERY_COMMUNITIES_DESC = "SELECT a.chainID AS ID, a.headBlock, a.tailBlock, b.balance, b.nonce," +
            " (CASE WHEN b.publicKey IS NULL THEN 0 ELSE 1 END) AS joined," +
            " 0 AS type, '' AS senderPk, '' AS receiverPk," +
            " b.msgUnread AS msgUnread, b.stickyTop AS stickyTop, 0 AS focused, null AS msg, c.memo, c.timestamp" +
            " FROM Communities AS a" +
            " LEFT JOIN Members AS b ON a.chainID = b.chainID" +
            " AND b.publicKey = " + QUERY_GET_CURRENT_USER_PK +
            " LEFT JOIN (SELECT timestamp, memo, chainID FROM (SELECT timestamp, memo, chainID FROM Txs" +
            " WHERE senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " ORDER BY timestamp DESC) GROUP BY chainID) AS c" +
            " ON a.chainID = c.chainID" +
            " WHERE isBanned == 0";

    String QUERY_FRIENDS_ASC = "SELECT f.friendPK AS ID, 0 AS headBlock, 0 AS tailBlock, 0 AS balance, 0 AS nonce," +
            " 0 AS joined, 1 AS type," +
            " cm.senderPk AS senderPk, cm.receiverPk AS receiverPk," +
            " f.msgUnread AS msgUnread, f.stickyTop AS stickyTop, f.focused AS focused," +
            " cm.content AS msg, '' AS memo, cm.timestamp AS timestamp" +
            " FROM Friends f" +
            " LEFT JOIN " + QUERY_NEWEST_MSG_ASC + " AS cm" +
            " ON (f.userPK = cm.senderPk AND f.friendPK = cm.receiverPk)" +
            " OR (f.userPK = cm.receiverPk AND f.friendPK = cm.senderPk)" +
            " WHERE f.userPk = " + QUERY_GET_CURRENT_USER_PK +
            " AND f.friendPK NOT IN " + UserDao.QUERY_GET_USER_PKS_IN_BAN_LIST;

    String QUERY_FRIENDS_DESC = "SELECT f.friendPK AS ID, 0 AS headBlock, 0 AS tailBlock, 0 AS balance, 0 AS nonce," +
            " 0 AS joined, 1 AS type," +
            " cm.senderPk AS senderPk, cm.receiverPk AS receiverPk," +
            " f.msgUnread AS msgUnread, f.stickyTop AS stickyTop, f.focused AS focused," +
            " cm.content AS msg, '' AS memo, cm.timestamp AS timestamp" +
            " FROM Friends f" +
            " LEFT JOIN " + QUERY_NEWEST_MSG_DESC + " AS cm" +
            " ON (f.userPK = cm.senderPk AND f.friendPK = cm.receiverPk)" +

            " OR (f.userPK = cm.receiverPk AND f.friendPK = cm.senderPk)" +
            " WHERE f.userPk = " + QUERY_GET_CURRENT_USER_PK +
            " AND f.friendPK NOT IN " + UserDao.QUERY_GET_USER_PKS_IN_BAN_LIST;

    String QUERY_COMMUNITIES_AND_FRIENDS_DESC = "SELECT * FROM (" + QUERY_FRIENDS_DESC +
            " UNION ALL " + QUERY_COMMUNITIES_DESC + ") ORDER BY stickyTop DESC, timestamp DESC";

    String QUERY_COMMUNITIES_AND_FRIENDS_ASC = "SELECT * FROM (" + QUERY_FRIENDS_ASC +
            " UNION ALL " + QUERY_COMMUNITIES_ASC + ")" +
            " ORDER BY stickyTop DESC, timestamp DESC";

    String QUERY_GET_COMMUNITIES_IN_BLACKLIST = "SELECT * FROM Communities WHERE isBanned = 1";
    String QUERY_GET_COMMUNITY_BY_CHAIN_ID = "SELECT * FROM Communities WHERE chainID = :chainID";
    String QUERY_ADD_COMMUNITY_BLACKLIST = "Update Communities set isBanned =:isBanned WHERE chainID = :chainID";
    String QUERY_JOINED_COMMUNITY = "SELECT c.*, m.balance, m.nonce, m.msgUnread," +
            " (CASE WHEN m.publicKey IS NULL THEN 0 ELSE 1 END) AS joined" +
            " FROM Communities c" +
            " LEFT JOIN Members m ON c.chainID = m.chainID AND m.publicKey = (" +
            UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " WHERE isBanned = 0";

    String QUERY_CLEAR_COMMUNITY_STATE = "UPDATE Communities SET headBlock = 0, tailBlock = 0" +
            " WHERE chainID = :chainID";

    String QUERY_CURRENT_COMMUNITY_MEMBER = "SELECT c.*, m.balance, m.nonce, m.msgUnread," +
            " (CASE WHEN m.publicKey IS NULL THEN 0 ELSE 1 END) AS joined" +
            " FROM Communities c" +
            " LEFT JOIN Members m ON c.chainID = m.chainID AND m.publicKey = :publicKey" +
            " WHERE c.chainID = :chainID";

    String QUERY_CHAIN_TOP_COIN_MEMBERS = "SELECT m.* FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " WHERE m.chainID = :chainID" +
//            " AND " + MemberDao.WHERE_ON_CHAIN  +
            " ORDER BY m.balance DESC LIMIT :topNum";

    String QUERY_COMMUNITIES = "SELECT c.*, m.balance, m.nonce, m.msgUnread," +
            " (CASE WHEN m.publicKey IS NULL THEN 0 ELSE 1 END) AS joined" +
            " FROM Communities c" +
            " LEFT JOIN Members m ON c.chainID = m.chainID AND m.publicKey = (" +
            UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " WHERE isBanned = 0";

    String QUERY_GET_SAME_COMMUNITY = "SELECT c.*" +
            " FROM Communities c" +
            " LEFT JOIN (SELECT chainID, count(publicKey) AS num FROM Members" +
            " WHERE publicKey = :userPk OR publicKey = :friendPk GROUP BY chainID) m" +
            " ON c.chainID = m.chainID" +
            " WHERE c.isBanned = 0 AND m.num = 2";

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

    @Query(QUERY_COMMUNITIES_AND_FRIENDS_DESC)
    @Transaction
    List<CommunityAndFriend> queryCommunitiesAndFriendsDESC();

    /**
     * 查询不在黑名单中的社区列表（正序）
     */
    @Query(QUERY_COMMUNITIES_AND_FRIENDS_ASC)
    @Transaction
    Flowable<List<CommunityAndFriend>> observeCommunitiesAndFriendsASC();

    @Query(QUERY_COMMUNITIES_AND_FRIENDS_ASC)
    @Transaction
    List<CommunityAndFriend> queryCommunitiesAndFriendsASC();

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
    List<CommunityAndMember> getJoinedCommunityList();

    /**
     * 根据chainID查询社区
     * @param chainID 社区chainID
     */
    @Query(QUERY_GET_COMMUNITY_BY_CHAIN_ID)
    Single<Community> getCommunityByChainIDSingle(String chainID);

    @Query(QUERY_GET_COMMUNITY_BY_CHAIN_ID)
    Flowable<Community> observerCommunityByChainID(String chainID);

    @Query(QUERY_CLEAR_COMMUNITY_STATE)
    void clearCommunityState(String chainID);

    @Query(QUERY_CURRENT_COMMUNITY_MEMBER)
    Flowable<CommunityAndMember> observerCurrentMember(String chainID, String publicKey);

    /**
     * 观察链上币量前topNum的成员
     */
    @Query(QUERY_CHAIN_TOP_COIN_MEMBERS)
    Flowable<List<Member>> observeChainTopCoinMembers(String chainID, int topNum);

    @Query(QUERY_COMMUNITIES)
    Flowable<List<CommunityAndMember>> observeCommunities();

    @Query(QUERY_GET_SAME_COMMUNITY)
    List<Community> getSameCommunity(String userPk, String friendPk);
}