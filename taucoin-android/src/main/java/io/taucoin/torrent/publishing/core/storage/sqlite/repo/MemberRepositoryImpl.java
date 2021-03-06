package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import io.taucoin.torrent.publishing.core.model.data.BlockStatistics;
import io.taucoin.torrent.publishing.core.model.data.MemberAndFriend;
import io.taucoin.torrent.publishing.core.model.data.MemberAndTime;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.model.data.Statistics;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.model.data.MemberAutoRenewal;
import io.taucoin.torrent.publishing.core.utils.DateUtil;

/**
 * CommunityRepository接口实现
 */
public class MemberRepositoryImpl implements MemberRepository {

    private Context appContext;
    private AppDatabase db;
    private PublishSubject<String> dataSetChangedPublish = PublishSubject.create();
    private ExecutorService sender = Executors.newSingleThreadExecutor();

    /**
     * MemberRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public MemberRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    /**
     * 添加新的Member
     * @param member User对象
     * @return 结果
     */
    @Override
    public long addMember(@NonNull Member member) {
        long result = db.memberDao().addMember(member);
        submitDataSetChanged();
        return result;
    }

    /**
     * 更新Member数据
     * @param member User对象
     * @return 结果
     */
    @Override
    public int updateMember(@NonNull Member member) {
        int result = db.memberDao().updateMember(member);
        submitDataSetChanged();
        return result;
    }

    /**
     * 获取Member根据公钥和链ID
     * @param chainID 社区链ID
     * @param publicKey 公钥
     * @return Member
     */
    @Override
    public Member getMemberByChainIDAndPk(@NonNull String chainID, @NonNull String publicKey){
        return db.memberDao().getMemberByChainIDAndPk(chainID, publicKey);
    }

    @Override
    public Flowable<List<MemberAndUser>> observeCommunityMembers(String chainID){
        return db.memberDao().observeCommunityMembers(chainID);
    }

    @Override
    public Flowable<Member> observeCommunityAirdropDetail(String chainID) {
        return db.memberDao().observeCommunityAirdropDetail(chainID);
    }

    /**
     * 查询社区成员
     * @param chainID 社区链ID
     * @return DataSource.Factory
     */
    @Override
    public DataSource.Factory<Integer, MemberAndFriend> queryCommunityMembers(String chainID){
        return db.memberDao().queryCommunityMembers(chainID);
    }

    /**
     * 获取和社区成员共在的社区数
     * @param currentUserPk
     * @param memberPk
     */
    @Override
    public Single<List<String>> getCommunityNumInCommon(String currentUserPk, String memberPk){
        return db.memberDao().getCommunityNumInCommon(currentUserPk, memberPk);
    }

    /**
     * 获取社区limit个成员
     * 先选取链上成员，如果链上成员不够，再选取链下成员
     * @param chainID
     * @param limit
     */
    @Override
    public List<String> queryCommunityMembersLimit(String chainID, int limit) {
        // 获取链上成员
        List<String> members = db.memberDao().queryCommunityMembersLimit(chainID, limit);
        if (null == members) {
            members = new ArrayList<>();
        }
        int offChainLimit = limit - members.size();
        if (offChainLimit > 0) {
            // 获取链下成员
            List<String> offChainMembers = db.memberDao().queryCommunityOffChainMembersLimit(chainID,
                    offChainLimit);
            if (offChainMembers != null && offChainMembers.size() > 0) {
                members.addAll(offChainMembers);
            }
        }
        return members;
    }

    @Override
    public Flowable<Statistics> getMembersStatistics(String chainID) {
        return db.memberDao().getMembersStatistics(chainID);
    }

    @Override
    public Flowable<BlockStatistics> getBlocksStatistics(String chainID) {
        return db.blockDao().getBlocksStatistics(chainID);
    }

    @Override
    public void deleteCommunityMembers(String chainID) {
        db.memberDao().deleteCommunityMembers(chainID);
        submitDataSetChanged();
    }

    @Override
    public Observable<String> observeDataSetChanged() {
        return dataSetChangedPublish;
    }

    @Override
    public void submitDataSetChanged() {
        String dateTime = DateUtil.getDateTime();
        sender.submit(() -> dataSetChangedPublish.onNext(dateTime));
    }

    /**
     * 获取社区成员统计
     */
    @Override
    public List<MemberAutoRenewal> queryAutoRenewalAccounts() {
        return db.memberDao().queryAutoRenewalAccounts();
    }

    /**
     * 获取跟随的社区列表
     */
    @Override
    public List<String> queryFollowedCommunities(String userPk) {
        return db.memberDao().queryFollowedCommunities(userPk);
    }

    @Override
    public String getCommunityLargestCoinHolder(String chainID) {
        return db.memberDao().getCommunityLargestCoinHolder(chainID);
    }

    /**
     * 获取自己加入的未过期社区列表
     */
    @Override
    public List<Member> getJoinedUnexpiredCommunityList(String userPk) {
        return db.memberDao().getJoinedUnexpiredCommunityList(userPk);
    }

    @Override
    public List<MemberAndTime> getJoinedCommunityList(String userPk) {
        return db.memberDao().getJoinedCommunityList(userPk);
    }
}
