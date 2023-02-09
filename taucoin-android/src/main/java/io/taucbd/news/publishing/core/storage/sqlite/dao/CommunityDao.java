package io.taucbd.news.publishing.core.storage.sqlite.dao;

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
import io.taucbd.news.publishing.core.Constants;
import io.taucbd.news.publishing.core.model.data.CommunityAndAccount;
import io.taucbd.news.publishing.core.model.data.CommunityAndFriend;
import io.taucbd.news.publishing.core.model.data.CommunityAndMember;
import io.taucbd.news.publishing.core.model.data.HomeStatistics;
import io.taucbd.news.publishing.core.model.data.MemberAndAmount;
import io.taucbd.news.publishing.core.model.data.MemberTips;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Community;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Member;

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

    String QUERY_COMMUNITIES_ASC = "SELECT a.chainID AS ID, a.headBlock, (b.consensusBalance - b.totalPendingCoins) AS balance, b.balUpdateTime, b.power, b.nonce," +
            " (CASE WHEN b.publicKey IS NULL THEN 0 ELSE 1 END) AS joined," +
            " 0 AS type, '' AS senderPk, '' AS receiverPk," +
//            " (CASE WHEN b.msgUnread = 1 THEN b.msgUnread ELSE b.newsUnread END) AS msgUnread," +
            " b.newsUnread," +
            " b.stickyTop AS stickyTop, 0 AS focused, null AS msg, c.memo, c.timestamp" +
            " FROM Communities AS a" +
            " LEFT JOIN Members AS b ON a.chainID = b.chainID" +
            " AND b.publicKey = " + QUERY_GET_CURRENT_USER_PK +
            " LEFT JOIN (SELECT timestamp, memo, chainID FROM (SELECT timestamp, memo, chainID FROM Txs" +
            " WHERE deleted = 0 AND txType = " + Constants.NEWS_TX_TYPE + " AND senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " ORDER BY timestamp) GROUP BY chainID) AS c" +
            " ON a.chainID = c.chainID" +
            " WHERE isBanned == 0";

    String QUERY_COMMUNITIES_DESC = "SELECT a.chainID AS ID, a.headBlock, (b.consensusBalance - b.totalPendingCoins) AS balance, b.balUpdateTime, b.power, b.nonce," +
            " (CASE WHEN b.publicKey IS NULL THEN 0 ELSE 1 END) AS joined," +
            " 0 AS type, '' AS senderPk, '' AS receiverPk," +
//            " (CASE WHEN b.msgUnread = 1 THEN b.msgUnread ELSE b.newsUnread END) AS msgUnread," +
            " b.newsUnread," +
            " b.stickyTop AS stickyTop, 0 AS focused, null AS msg, c.memo, c.timestamp" +
            " FROM Communities AS a" +
            " LEFT JOIN Members AS b ON a.chainID = b.chainID" +
            " AND b.publicKey = " + QUERY_GET_CURRENT_USER_PK +
            " LEFT JOIN (SELECT timestamp, memo, chainID FROM (SELECT timestamp, memo, chainID FROM Txs" +
            " WHERE deleted = 0 AND txType = " + Constants.NEWS_TX_TYPE + " AND senderPk NOT IN " + UserDao.QUERY_GET_COMMUNITY_USER_PKS_IN_BAN_LIST +
            " ORDER BY timestamp DESC) GROUP BY chainID) AS c" +
            " ON a.chainID = c.chainID" +
            " WHERE isBanned == 0";

    String QUERY_FRIENDS_ASC = "SELECT f.friendPK AS ID, 0 AS headBlock, 0 AS balance, 0 AS balUpdateTime," +
            " 0 AS power, 0 AS nonce, 0 AS joined, 1 AS type," +
            " cm.senderPk AS senderPk, cm.receiverPk AS receiverPk," +
            " f.msgUnread AS msgUnread, f.stickyTop AS stickyTop, f.focused AS focused," +
            " cm.content AS msg, '' AS memo, cm.timestamp AS timestamp" +
            " FROM Friends f" +
            " LEFT JOIN " + QUERY_NEWEST_MSG_ASC + " AS cm" +
            " ON (f.userPK = cm.senderPk AND f.friendPK = cm.receiverPk)" +
            " OR (f.userPK = cm.receiverPk AND f.friendPK = cm.senderPk)" +
            " WHERE f.userPk = " + QUERY_GET_CURRENT_USER_PK +
            " AND f.friendPK NOT IN " + UserDao.QUERY_GET_USER_PKS_IN_BAN_LIST;

    String QUERY_FRIENDS_DESC = "SELECT f.friendPK AS ID, 0 AS headBlock, 0 AS balance, 0 AS balUpdateTime," +
            " 0 AS power, 0 AS nonce, 0 AS joined, 1 AS type," +
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

    String QUERY_MEMBER_TIPS = "SELECT MAX(m.pendingTime) pendingTime FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " WHERE m.publicKey = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND c.isBanned = 0";

    String QUERY_JOINED_COMMUNITY = "SELECT m.* FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " WHERE m.publicKey = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND c.isBanned = 0";

    String QUERY_COMMUNITY_MEMBER_STATE = "SELECT m.*, e.txIncomePending, f.txExpenditurePending" +
            " FROM Members m" +
            // 收入包括: 别人的转账收入，onchain + offchain
            " LEFT JOIN (SELECT chainID, SUM(amount) AS txIncomePending FROM Txs" +
            " WHERE chainID = :chainID AND txType = " + Constants.WIRING_TX_TYPE + " AND receiverPk =(" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND txStatus <= " + Constants.TX_STATUS_ON_CHAIN +
            ") AS e ON m.chainID = e.chainID" +
            // 支出包括: 交易费+转账金额，onchain + offchain
            " LEFT JOIN (SELECT chainID, SUM(amount + fee) AS txExpenditurePending FROM Txs" +
            " WHERE chainID = :chainID AND txType IN (" + Constants.WIRING_TX_TYPE + ", " + Constants.NEWS_TX_TYPE + ")" +
            " AND senderPk =(" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND txStatus <= " + Constants.TX_STATUS_ON_CHAIN +
            ") AS f ON m.chainID = f.chainID" +

            " WHERE m.chainID =:chainID AND m.publicKey = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")";

    String QUERY_ALL_JOINED_COMMUNITY = "SELECT c.*" +
            " FROM Communities c" +
            " LEFT JOIN Members m ON c.chainID = m.chainID AND m.publicKey = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " WHERE m.publicKey IS NOT NULL";

    String QUERY_CLEAR_COMMUNITY_STATE = "UPDATE Communities SET headBlock = 0" +
            " WHERE chainID = :chainID";

    String QUERY_COMMUNITY_ACCOUNT_ORDER = "SELECT chainID, publicKey FROM Members" +
            " WHERE chainID = :chainID AND (balance > 0 OR nonce > 0)" +
            " ORDER BY balance DESC, nonce DESC, publicKey COLLATE UNICODE DESC";

    String QUERY_COMMUNITY_ACCOUNT_EXPIRED = "SELECT rank.publicKey FROM (" + QUERY_COMMUNITY_ACCOUNT_ORDER + " LIMIT :limit) AS rank" +
            " WHERE rank.publicKey = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")";

    String QUERY_CURRENT_COMMUNITY_MEMBER = "SELECT c.*, (m.consensusBalance - m.totalPendingCoins) AS balance," +
            " (m.consensusBalance - m.totalPendingCoins) AS paymentBalance, m.nonce, m.msgUnread, m.newsUnread," +
            " (CASE WHEN m.publicKey IS NULL THEN 0 ELSE 1 END) AS joined, " +
            " (CASE WHEN order1.publicKey IS NULL THEN 0 ELSE 1 END) AS notExpired," +
            " (CASE WHEN order2.publicKey IS NULL THEN 1 ELSE 0 END) AS nearExpired" +
            " FROM Communities c" +
            " LEFT JOIN Members m ON c.chainID = m.chainID AND m.publicKey = :publicKey" +
            " LEFT JOIN (" + QUERY_COMMUNITY_ACCOUNT_ORDER + " LIMIT :expiredLimit) AS order1 " +
            " ON c.chainID = order1.chainID AND order1.publicKey = :publicKey" +
            " LEFT JOIN (" + QUERY_COMMUNITY_ACCOUNT_ORDER + " LIMIT :nearLimit) AS order2 " +
            " ON c.chainID = order2.chainID AND order2.publicKey = :publicKey" +
            " WHERE c.chainID = :chainID";

    String QUERY_CHAIN_TOP_COIN_MEMBERS = "SELECT * FROM Members" +
            " WHERE chainID = :chainID" +
            " ORDER BY balance DESC, nonce DESC, publicKey COLLATE UNICODE DESC LIMIT :topNum";

    String QUERY_COMMUNITIES = "SELECT c.*, (m.consensusBalance - m.totalPendingCoins) AS balance, m.balUpdateTime, m.nonce, m.msgUnread," +
            " (CASE WHEN m.publicKey IS NULL THEN 0 ELSE 1 END) AS joined" +
            " FROM Communities c" +
            " LEFT JOIN Members m ON c.chainID = m.chainID AND m.publicKey = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " WHERE isBanned = 0";

    String QUERY_GET_SAME_COMMUNITY = "SELECT c.*" +
            " FROM Communities c" +
            " LEFT JOIN (SELECT chainID, count(publicKey) AS num FROM Members" +
            " WHERE publicKey = :userPk OR publicKey = :friendPk GROUP BY chainID) m" +
            " ON c.chainID = m.chainID" +
            " WHERE c.isBanned = 0 AND m.num = 2";

    String QUERY_COMMUNITIES_CONTACTS = "SELECT a.communities, b.contacts" +
            " FROM (SELECT count(*) AS communities FROM Communities WHERE isBanned = 0) AS a," +
            " (SELECT count(*) AS contacts FROM Users WHERE isBanned = 0 AND isCurrentUser != 1) AS b";

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
    Community getCommunityByChainID(String chainID);

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

    @Query(QUERY_MEMBER_TIPS)
    Flowable<MemberTips> observeMemberTips();
    /**
     * 获取用户加入的社区列表
     */
    @Query(QUERY_JOINED_COMMUNITY)
    Flowable<List<Member>> observeJoinedCommunityList();

    /**
     * 获取当前用户当前链的状态(wallet显示, txIncomePending, txExpenditurePending)
     */
    @Query(QUERY_COMMUNITY_MEMBER_STATE)
    Flowable<MemberAndAmount> observerMemberAndAmount(String chainID);

    @Query(QUERY_ALL_JOINED_COMMUNITY)
    List<Community> getAllJoinedCommunityList();

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
    Flowable<CommunityAndMember> observerCurrentMember(String chainID, String publicKey,
                                                       int expiredLimit, int nearLimit);

    @Query(QUERY_COMMUNITY_ACCOUNT_EXPIRED)
    String queryCommunityAccountExpired(String chainID, int limit);

    /**
     * 观察链上币量前topNum的成员
     */
    @Query(QUERY_CHAIN_TOP_COIN_MEMBERS)
    Flowable<List<Member>> observeChainTopCoinMembers(String chainID, int topNum);

    @Query(QUERY_COMMUNITIES)
    Flowable<List<CommunityAndAccount>> observeCommunities();

    @Query(QUERY_GET_SAME_COMMUNITY)
    List<Community> getSameCommunity(String userPk, String friendPk);

    @Query(QUERY_COMMUNITIES_CONTACTS)
    Observable<HomeStatistics> observeCommunitiesAndContacts();
}
