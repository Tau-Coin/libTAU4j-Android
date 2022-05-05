package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

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
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.MemberAndFriend;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.model.data.Statistics;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.model.data.MemberAutoRenewal;

/**
 * Room:Member操作接口
 */
@Dao
public interface MemberDao {
    String WHERE_NOT_PERISHABLE = " (blockNumber >= tailBlock)";
    String WHERE_ON_CHAIN = " (power > 0 AND" + WHERE_NOT_PERISHABLE + ")";

    String WHERE_OFF_CHAIN = " (power <= 0 OR blockNumber < tailBlock)";

    String QUERY_GET_MEMBER_BY_CHAIN_ID_PK = "SELECT * FROM Members WHERE chainID = :chainID AND publicKey = :publicKey";
    String QUERY_GET_MEMBERS_BY_CHAIN_ID = "SELECT * FROM Members WHERE chainID = :chainID";

    String QUERY_GET_MEMBERS_IN_COMMUNITY = "SELECT m.*, c.headBlock, c.tailBlock" +
            " FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " WHERE m.chainID = :chainID" +
            " ORDER BY m.power DESC";

    String QUERY_COMMUNITY_NUM_IN_COMMON = "SELECT chainID FROM " +
            " (Select count(*) AS num, m.chainID FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " where (m.publicKey =:currentUserPk OR m.publicKey =:memberPk) AND " + WHERE_ON_CHAIN +
            " GROUP BY m.chainID)" +
            " WHERE num >= 2";

    String QUERY_COMMUNITY_MEMBERS_LIMIT = "SELECT m.publicKey FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " WHERE m.chainID = :chainID" +
            " AND m.publicKey != '0000000000000000000000000000000000000000000000000000000000000000'" +
            " AND " + WHERE_ON_CHAIN +
            " ORDER BY m.power DESC LIMIT :limit";

    String QUERY_COMMUNITY_OFF_CHAIN_MEMBERS_LIMIT = "SELECT m.publicKey FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " WHERE m.chainID = :chainID" +
            " AND m.publicKey != '0000000000000000000000000000000000000000000000000000000000000000'" +
            " AND " + WHERE_OFF_CHAIN +
            " ORDER BY m.power limit :limit";

    String QUERY_MEMBERS_STATISTICS = "SELECT a.onChain, b.total" +
            " FROM " +
            " (SELECT chainID, COUNT(publicKey) AS total" +
            " FROM Members WHERE chainID =:chainID) AS b" +
            " LEFT JOIN " +
            " (SELECT m.chainID, COUNT(m.publicKey) AS onChain" +
            " FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " WHERE m.chainID =:chainID and " + WHERE_ON_CHAIN + ") AS a" +
            " ON a.chainID = b.chainID";

    String QUERY_DELETE_COMMUNITY_MEMBERS = "DELETE FROM Members where chainID =:chainID";

    // 查询当前设备可以自动更新的账户信息
    // 计算是否还有7天过期，触发auto renewal
    // headBlock - blockNumber >= Constants.BLOCKS_NOT_PERISHABLE - Constants.AUTO_RENEWAL_MAX_BLOCKS
    String QUERY_AUTO_RENEWAL_ACCOUNTS = "SELECT m.*, u.seed" +
            " FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " LEFT JOIN Users u ON m.publicKey = u.publicKey" +
            " WHERE c.isBanned = 0 AND u.seed NOT NULL" +
            " AND " + WHERE_ON_CHAIN +
            " AND (c.headBlock - m.blockNumber >= " +
            (Constants.BLOCKS_NOT_PERISHABLE - Constants.AUTO_RENEWAL_MAX_BLOCKS) + ")";

    // 获取跟随的社区列表
    String QUERY_FOLLOWED_COMMUNITIES = "SELECT m.chainID" +
            " FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " WHERE m.publicKey = :userPk AND c.isBanned = 0" +
            " AND " + WHERE_ON_CHAIN;

    // 获取社区最大币持有者
    String QUERY_LARGEST_COIN_HOLDER = "SELECT publicKey FROM Members" +
            " WHERE chainID = :chainID" +
            " ORDER BY balance DESC LIMIT 1";

    String QUERY_JOINED_UNEXPIRED_COMMUNITY = "SELECT m.*" +
            " FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " WHERE m.publicKey = :userPk AND c.isBanned = 0" +
            " AND " + WHERE_ON_CHAIN;

    String QUERY_JOINED_UNEXPIRED_CHAIN_ID = "SELECT m.chainID" +
            " FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " WHERE m.publicKey = :userPk AND c.isBanned = 0" +
            " AND " + WHERE_ON_CHAIN;

    String QUERY_UN_JOINED_EXPIRED_COMMUNITY = "SELECT m.*" +
            " FROM Members m" +
            " LEFT JOIN Communities c ON m.chainID = c.chainID" +
            " WHERE m.publicKey = :userPk AND c.isBanned = 0" +
            " AND m.chainID NOT IN (" + QUERY_JOINED_UNEXPIRED_CHAIN_ID + ")";

    String QUERY_AIRDROP_DETAIL = "SELECT * FROM Members" +
            " WHERE chainID = :chainID AND" +
            " publicKey = (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")";

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
    DataSource.Factory<Integer, MemberAndFriend> queryCommunityMembers(String chainID);

    /**
     * 获取和社区成员共在的社区数
     * @param currentUserPk
     * @param memberPk
     */
    @Query(QUERY_COMMUNITY_NUM_IN_COMMON)
    Single<List<String>> getCommunityNumInCommon(String currentUserPk, String memberPk);

    /**
     * 获取社区limit个成员
     * @param chainID
     * @param limit
     */
    @Query(QUERY_COMMUNITY_MEMBERS_LIMIT)
    List<String> queryCommunityMembersLimit(String chainID, int limit);

    @Query(QUERY_COMMUNITY_OFF_CHAIN_MEMBERS_LIMIT)
    List<String> queryCommunityOffChainMembersLimit(String chainID, int limit);

    @Query(QUERY_MEMBERS_STATISTICS)
    Flowable<Statistics> getMembersStatistics(String chainID);

    @Query(QUERY_DELETE_COMMUNITY_MEMBERS)
    void deleteCommunityMembers(String chainID);

    /**
     * 查询当前设备可以自动更新的账户信息
     */
    @Query(QUERY_AUTO_RENEWAL_ACCOUNTS)
    List<MemberAutoRenewal> queryAutoRenewalAccounts();

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
     * 获取自己加入的未过期社区列表
     */
    @Query(QUERY_JOINED_UNEXPIRED_COMMUNITY)
    List<Member> getJoinedUnexpiredCommunityList(String userPk);

    /**
     * 获取自己未加入的或者过期社区列表
     */
    @Query(QUERY_UN_JOINED_EXPIRED_COMMUNITY)
    List<Member> getUnJoinedExpiredCommunityList(String userPk);

    @Query(QUERY_AIRDROP_DETAIL)
    Flowable<Member> observeCommunityAirdropDetail(String chainID);

    @Query(QUERY_GET_MEMBER_BY_CHAIN_ID_PK)
    Single<Member> getMemberSingle(String chainID, String publicKey);
}
