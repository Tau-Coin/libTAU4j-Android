package io.taucoin.news.publishing.core.storage.sqlite.repo;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.taucoin.news.publishing.core.model.data.CommunityAndAccount;
import io.taucoin.news.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.news.publishing.core.model.data.CommunityAndMember;
import io.taucoin.news.publishing.core.model.data.HomeStatistics;
import io.taucoin.news.publishing.core.model.data.MemberAndAmount;
import io.taucoin.news.publishing.core.model.data.MemberTips;
import io.taucoin.news.publishing.core.storage.sqlite.entity.BlockInfo;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Member;

/**
 * 提供操作Community数据的接口
 */
public interface CommunityRepository {

    /**
     * 添加新的社区
     * @param community 社区数据
     */
    long addCommunity(@NonNull Community community);

    int updateCommunity(@NonNull Community community);
//
    Community getCommunityByChainID(@NonNull String chainID);

    Community getChatByFriendPk(@NonNull String friendPk);

    /**
     * 观察不在黑名单的社区列表数据变化
     * @return 被观察的社区数据列表
     */
    Flowable<List<CommunityAndFriend>> observeCommunitiesAndFriends();

    /**
     * 观察首页数据是否变化
     * @return Flowable<Object>
     */
    Flowable<Object> observeHomeChanged();

    List<CommunityAndFriend> queryCommunitiesAndFriends();

    /**
     * 获取在黑名单的社区列表
     * @return List<Community>
     */
    List<Community> getCommunitiesInBlacklist();

    /**
     * 添加社区黑名单实现
     * @param chainID 社区chainID
     * @param blacklist 是否加入黑名单
     */
    void setCommunityBlacklist(String chainID, boolean blacklist);

    /**
     * 获取用户加入的社区列表
     */
    Flowable<MemberTips> observeMemberTips();

    /**
     * 获取用户加入的社区列表
     */
    Flowable<List<Member>> observerJoinedCommunityList();

    /**
     * 获取用户加入的所有社区列表
     */
    List<Community> getAllJoinedCommunityList();

    /**
     * 根据chainID查询社区
     * @param chainID 社区chainID
     */
    Single<Community> getCommunityByChainIDSingle(String chainID);

    Single<Member> getMemberSingle(String chainID);

    Flowable<Community> observerCommunityByChainID(String chainID);

    Flowable<List<BlockInfo>> observerCommunityTopBlocks(String chainID, int num);

    /**
     * 清除社区状态，totalBlocks, syncBlock数据
     * @param chainID
     */
    void clearCommunityState(String chainID);

    /**
     * 观察当前登陆的社区成员
     * @param chainID
     * @param publicKey
     * @return
     */
    Flowable<CommunityAndMember> observerCurrentMember(String chainID, String publicKey);

    Flowable<MemberAndAmount> observerMemberAndAmount(String chainID);

    String queryCommunityAccountExpired(String chainID);

    /**
     * 观察链上币量前topNum的成员
     * @param chainID 链ID
     * @param topNum 查询数目
     * @return Observable<List<Member>>
     */
    Flowable<List<Member>> observeChainTopCoinMembers(String chainID, int topNum);

    Flowable<List<CommunityAndAccount>> observeCommunities();

    /**
     * 获取和朋友的共同社区
     * @param userPk
     * @param friendPk
     * @return
     */
    List<Community> getSameCommunity(String userPk, String friendPk);

    Observable<HomeStatistics> observeCommunitiesAndContacts();
}
