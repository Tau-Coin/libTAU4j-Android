package io.taucoin.news.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.room.RxRoom;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.taucoin.news.publishing.core.Constants;
import io.taucoin.news.publishing.core.model.data.BlockStatistics;
import io.taucoin.news.publishing.core.model.data.MemberAndFriend;
import io.taucoin.news.publishing.core.model.data.MemberAndTime;
import io.taucoin.news.publishing.core.model.data.MemberAndUser;
import io.taucoin.news.publishing.core.model.data.Statistics;
import io.taucoin.news.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.news.publishing.core.utils.DateUtil;

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
     */
    @Override
    public List<MemberAndFriend> queryCommunityMembers(String chainID, int pos, int pageSize){
        return db.memberDao().queryCommunityMembers(chainID, pos, pageSize, Constants.MAX_ACCOUNT_SIZE);
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
        return members;
    }

    @Override
    public Flowable<Statistics> getMembersStatistics(String chainID) {
        return db.memberDao().getMembersStatistics(chainID, Constants.MAX_ACCOUNT_SIZE);
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
    public void deleteCommunityMember(String chainID, String userPk) {
        db.memberDao().deleteCommunityMembers(chainID, userPk);
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

    @Override
    public List<MemberAndTime> getJoinedCommunityList(String userPk) {
        return db.memberDao().getJoinedCommunityList(userPk);
    }

    @Override
    public void addPendingAndOffchainCoins(String chainID, String publicKey, long amount) {
        db.memberDao().addPendingAndOffchainCoins(chainID, publicKey, amount);
    }

    @Override
    public void resetMembers(String chainID, long time) {
        db.memberDao().resetMembers(chainID, time);
    }

    @Override
    public void clearNewsUnread() {
        db.memberDao().clearNewsUnread();
    }

    @Override
    public Flowable<Object> observeMembersDataSetChanged() {
        String[] tables = new String[]{"Members"};
        return RxRoom.createFlowable(db, tables);
    }
}
