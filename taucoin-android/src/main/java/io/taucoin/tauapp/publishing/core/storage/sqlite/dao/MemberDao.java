package io.taucoin.tauapp.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.taucoin.tauapp.publishing.core.model.data.MemberAndFriend;
import io.taucoin.tauapp.publishing.core.model.data.MemberAndTime;
import io.taucoin.tauapp.publishing.core.model.data.MemberAndUser;
import io.taucoin.tauapp.publishing.core.model.data.Statistics;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Member;

/**
 * Room:Member操作接口
 */
@Dao
public interface MemberDao {
    String QUERY_GET_MEMBER_BY_CHAIN_ID_PK = "SELECT * FROM Members WHERE chainID = :chainID AND publicKey = :publicKey";
    String QUERY_GET_MEMBERS_BY_CHAIN_ID = "SELECT * FROM Members WHERE chainID = :chainID";

    String QUERY_GET_MEMBERS_IN_COMMUNITY = "SELECT m.*, " +
            " (CASE WHEN rank.publicKey IS NULL THEN 0 ELSE 1 END) AS notExpired" +
            " FROM Members m" +
            " LEFT JOIN (" + CommunityDao.QUERY_COMMUNITY_ACCOUNT_ORDER + " LIMIT :limit) AS rank" +
            " ON m.chainID = rank.chainID AND m.publicKey = rank.publicKey" +
            " WHERE m.chainID = :chainID" +
            " ORDER BY m.balance DESC";

    String QUERY_COMMUNITY_MEMBERS_LIMIT = "SELECT m.publicKey FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " WHERE m.chainID = :chainID" +
            " AND m.publicKey != '0000000000000000000000000000000000000000000000000000000000000000'" +
            " ORDER BY m.balance DESC, m.nonce DESC LIMIT :limit";

    String QUERY_MEMBERS_STATISTICS = "SELECT a.onChain, b.total" +
            " FROM " +
            " (SELECT chainID, COUNT(publicKey) AS total" +
            " FROM Members WHERE chainID =:chainID) AS b" +
            " LEFT JOIN " +
            " (SELECT chainID, COUNT(publicKey) AS onChain" +
            " FROM (" + CommunityDao.QUERY_COMMUNITY_ACCOUNT_ORDER + " LIMIT :limit)" +
            ") AS a" +
            " ON a.chainID = b.chainID";

    String QUERY_DELETE_COMMUNITY_MEMBERS = "DELETE FROM Members where chainID =:chainID";

    // 获取跟随的社区列表
    String QUERY_FOLLOWED_COMMUNITIES = "SELECT m.chainID" +
            " FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " WHERE m.publicKey = :userPk AND c.isBanned = 0";

    // 获取社区最大币持有者
    String QUERY_LARGEST_COIN_HOLDER = "SELECT publicKey FROM Members" +
            " WHERE chainID = :chainID" +
            " ORDER BY balance DESC LIMIT 1";

    String QUERY_JOINED_COMMUNITY = "SELECT m.*, tx.latestTxTime, b.latestMiningTime" +
            " FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " LEFT JOIN (SELECT chainID, MAX(timestamp) AS latestTxTime FROM Txs " +
            " WHERE senderPk = :userPk GROUP BY chainID) tx" +
            " ON m.chainID = tx.chainID" +
            " LEFT JOIN (SELECT chainID, MAX(timestamp) AS latestMiningTime FROM Blocks " +
            " WHERE miner = :userPk GROUP BY chainID) b" +
            " ON m.chainID = b.chainID" +
            " WHERE m.publicKey = :userPk AND c.isBanned = 0" +
            " ORDER BY balance DESC";

    String QUERY_AIRDROP_DETAIL = "SELECT * FROM Members" +
            " WHERE chainID = :chainID AND" +
            " publicKey = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")";

    String RESET_MEMBER_STATE= 
            " UPDATE Members SET newsUnread = 0, balance = 0, power = 0, nonce = 0" +
            " WHERE chainID = :chainID AND balUpdateTime < :time";

    String UPDATE_PENDING_OFFCHAIN_COINS= 
            " UPDATE Members SET totalPendingCoins = totalPendingCoins + :amount," +
            " totalOffchainCoins = totalOffchainCoins + :amount" +
            " WHERE chainID = :chainID AND publicKey = :publicKey";

    String QUERY_CLEAR_NEWS_UNREAD = "UPDATE Members SET newsUnread = 0" +
            " WHERE publicKey = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")";

    /**
     * 添加新社区成员
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addMember(Member member);

    /**
     * 更新社区成员
     */
    @Update
    int updateMember(Member member);

    /**
     * 获取Member根据公钥和链ID
     * @param chainID 社区链ID
     * @param publicKey 公钥
     * @return Member
     */
    @Query(QUERY_GET_MEMBER_BY_CHAIN_ID_PK)
    Member getMemberByChainIDAndPk(@NonNull String chainID, @NonNull String publicKey);

    @Query(QUERY_GET_MEMBERS_BY_CHAIN_ID)
    @Transaction
    Flowable<List<MemberAndUser>> observeCommunityMembers(String chainID);

    /**
     * 查询社区上链的成员
     * @param chainID 社区链ID
     * @return DataSource.Factory
     */
    @Query(QUERY_GET_MEMBERS_IN_COMMUNITY)
    @Transaction
    DataSource.Factory<Integer, MemberAndFriend> queryCommunityMembers(String chainID, int limit);

    /**
     * 获取社区limit个成员
     * @param chainID
     * @param limit
     */
    @Query(QUERY_COMMUNITY_MEMBERS_LIMIT)
    List<String> queryCommunityMembersLimit(String chainID, int limit);

    @Query(QUERY_MEMBERS_STATISTICS)
    Flowable<Statistics> getMembersStatistics(String chainID, int limit);

    @Query(QUERY_DELETE_COMMUNITY_MEMBERS)
    void deleteCommunityMembers(String chainID);

    /**
     * 获取跟随的社区列表
     */
    @Query(QUERY_FOLLOWED_COMMUNITIES)
    List<String> queryFollowedCommunities(String userPk);

    /**
     * 获取社区最大币持有者
     */
    @Query(QUERY_LARGEST_COIN_HOLDER)
    String getCommunityLargestCoinHolder(String chainID);

    /**
     * 获取自己未加入的或者过期社区列表
     */
    @Query(QUERY_JOINED_COMMUNITY)
    List<MemberAndTime> getJoinedCommunityList(String userPk);

    @Query(QUERY_AIRDROP_DETAIL)
    Flowable<Member> observeCommunityAirdropDetail(String chainID);

    @Query(QUERY_GET_MEMBER_BY_CHAIN_ID_PK)
    Single<Member> getMemberSingle(String chainID, String publicKey);

    @Query(UPDATE_PENDING_OFFCHAIN_COINS)
    void addPendingAndOffchainCoins(String chainID, String publicKey, long amount);

    @Query(RESET_MEMBER_STATE)
    void resetMembers(String chainID, long time);

    @Query(QUERY_CLEAR_NEWS_UNREAD)
    void clearNewsUnread();
}
