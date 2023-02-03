package io.taucbd.news.publishing.core.storage.sqlite.repo;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.taucbd.news.publishing.core.model.data.BlockStatistics;
import io.taucbd.news.publishing.core.model.data.MemberAndFriend;
import io.taucbd.news.publishing.core.model.data.MemberAndTime;
import io.taucbd.news.publishing.core.model.data.MemberAndUser;
import io.taucbd.news.publishing.core.model.data.Statistics;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Member;

/**
 * 提供操作Member数据的接口
 */
public interface MemberRepository {

    /**
     * 添加新的Member
     * @param member User对象
     * @return 结果
     */
    long addMember(@NonNull Member member);

    /**
     * 更新Member数据
     * @param member User对象
     * @return 结果
     */
    int updateMember(@NonNull Member member);

    /**
     * 获取Member根据公钥和链ID
     * @param chainID 社区链ID
     * @param publicKey 公钥
     * @return Member
     */
    Member getMemberByChainIDAndPk(@NonNull String chainID, @NonNull String publicKey);

    Flowable<List<MemberAndUser>> observeCommunityMembers(String chainID);

    Flowable<Member> observeCommunityAirdropDetail(String chainID);

    List<MemberAndFriend> queryCommunityMembers(String chainID, int pos, int pageSize);

    /**
     * 获取社区limit个成员
     * @param chainID
     * @param limit
     */
    List<String> queryCommunityMembersLimit(String chainID, int limit);

    /**
     * 获取社区成员统计
     * @param chainID
     */
    Flowable<Statistics> getMembersStatistics(String chainID);

    /**
     * 获取社区区块统计
     * @param chainID
     */
    Flowable<BlockStatistics> getBlocksStatistics(String chainID);

    /**
     * 删除社区成员数据
     * @param chainID
     */
    void deleteCommunityMembers(String chainID);

    public void deleteCommunityMember(String chainID, String userPk);

    Observable<String> observeDataSetChanged();

    void submitDataSetChanged();

    /**
     * 获取跟随的社区列表
     */
    List<String> queryFollowedCommunities(String publicKey);

    String getCommunityLargestCoinHolder(String chainID);

    List<MemberAndTime> getJoinedCommunityList(String publicKey);

    /**
     * 重置成员状态（小于传入时间的重置）
     */
    void resetMembers(String chainID, long time);
    
    /**
     * 成员PendingAndOffchain Coins加 amount
     */
    void addPendingAndOffchainCoins(String chainID, String publicKey, long amount);
    
    void clearNewsUnread();

    Flowable<Object> observeMembersDataSetChanged();
}
