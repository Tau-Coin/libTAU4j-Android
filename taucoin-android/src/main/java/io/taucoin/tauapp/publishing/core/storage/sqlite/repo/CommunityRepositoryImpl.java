package io.taucoin.tauapp.publishing.core.storage.sqlite.repo;

import android.content.Context;
import android.os.Build;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.RxRoom;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.core.Constants;
import io.taucoin.tauapp.publishing.core.model.data.CommunityAndAccount;
import io.taucoin.tauapp.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.tauapp.publishing.core.model.data.CommunityAndMember;
import io.taucoin.tauapp.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.BlockInfo;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Member;

/**
 * CommunityRepository接口实现
 */
public class CommunityRepositoryImpl implements CommunityRepository{

    private Context appContext;
    private AppDatabase db;

    /**
     * CommunityRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public CommunityRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    /**
     * 添加新的社区
     * @param community 社区数据
     */
    @Override
    public long addCommunity(@NonNull Community community) {
        return db.communityDao().addCommunity(community);
    }

    @Override
    public int updateCommunity(@NonNull Community community) {
        return db.communityDao().updateCommunity(community);
    }

    @Override
    public Community getCommunityByChainID(@NonNull String chainID) {
        return db.communityDao().getCommunityBychainID(chainID);
    }

    @Override
    public Community getChatByFriendPk(@NonNull String friendPk) {
        return db.communityDao().getCommunityBychainID(friendPk);
    }

    /**
     * 观察不在黑名单的社区列表数据变化
     * @return 被观察的社区数据列表
     */
    @Override
    public Flowable<List<CommunityAndFriend>> observeCommunitiesAndFriends() {
        int sdkVersion = Build.VERSION.SDK_INT;
        // android11中SQLite版本为3.28.0, group by取第一条记录，低版本取最后一条记录

        if (sdkVersion >= 30) {
            return db.communityDao().observeCommunitiesAndFriendsDESC();
        } else {
            return db.communityDao().observeCommunitiesAndFriendsASC();
        }
    }

    /**
     * 观察首页数据是否变化
     * @return Flowable<Object>
     */
    public Flowable<Object> observeHomeChanged() {
        String[] tables = new String[]{"Users","Friends","ChatMessages","Communities","Members","Txs"};
        return RxRoom.createFlowable(db, tables);
    }

    @Override
    public List<CommunityAndFriend> queryCommunitiesAndFriends() {
        int sdkVersion = Build.VERSION.SDK_INT;
        // android11中SQLite版本为3.28.0, group by取第一条记录，低版本取最后一条记录
        if (sdkVersion >= 30) {
            return db.communityDao().queryCommunitiesAndFriendsDESC();
        } else {
            return db.communityDao().queryCommunitiesAndFriendsASC();
        }
    }

    /**
     * 获取在黑名单的社区列表
     * @return List<Community>
     */
    public List<Community> getCommunitiesInBlacklist(){
        return db.communityDao().getCommunitiesInBlacklist();
    }

    /**
     * 添加社区黑名单实现
     * @param chainID 社区chainID
     * @param blacklist 是否加入黑名单
     */
    @Override
    public void setCommunityBlacklist(String chainID, boolean blacklist) {
        db.communityDao().setCommunityBlacklist(chainID, blacklist ? 1 : 0);
    }

    /**
     * 获取用户加入的社区列表
     */
    public List<CommunityAndAccount> getJoinedCommunityList(){
        return db.communityDao().getJoinedCommunityList();
    }

    /**
     * 获取用户加入的所有社区列表
     */
    public List<Community> getAllJoinedCommunityList() {
        return db.communityDao().getAllJoinedCommunityList();
    }

    /**
     * 根据chainID查询社区
     * @param chainID 社区chainID
     */
    public Single<Community> getCommunityByChainIDSingle(String chainID){
        return db.communityDao().getCommunityByChainIDSingle(chainID);
    }

    @Override
    public Single<Member> getMemberSingle(String chainID) {
        return db.memberDao().getMemberSingle(chainID, MainApplication.getInstance().getPublicKey());
    }

    @Override
    public Flowable<Community> observerCommunityByChainID(String chainID) {
        return db.communityDao().observerCommunityByChainID(chainID);
    }

    @Override
    public Flowable<List<BlockInfo>> observerCommunityTopBlocks(String chainID, int num) {
        return db.blockDao().observerCommunityTopBlocks(chainID, num);
    }

    @Override
    public void clearCommunityState(String chainID) {
        db.communityDao().clearCommunityState(chainID);
    }

    @Override
    public Flowable<CommunityAndMember> observerCurrentMember(String chainID, String publicKey) {
        return db.communityDao().observerCurrentMember(chainID, publicKey, Constants.MAX_ACCOUNT_SIZE,
                Constants.NEAR_EXPIRED_ACCOUNT_SIZE);
    }

    @Override
    public String queryCommunityAccountExpired(String chainID) {
        return db.communityDao().queryCommunityAccountExpired(chainID, Constants.MAX_ACCOUNT_SIZE);
    }

    /**
     * 观察链上币量前topNum的成员
     * @param chainID 链ID
     * @param topNum 查询数目
     * @return Observable<List<Member>>
     */
    @Override
    public Flowable<List<Member>> observeChainTopCoinMembers(String chainID, int topNum) {
        return db.communityDao().observeChainTopCoinMembers(chainID, topNum);
    }

    @Override
    public Flowable<List<CommunityAndAccount>> observeCommunities() {
        return db.communityDao().observeCommunities();
    }

    /**
     * 获取和朋友的共同社区
     * @param userPk
     * @param friendPk
     * @return
     */
    @Override
    public List<Community> getSameCommunity(String userPk, String friendPk) {
        return db.communityDao().getSameCommunity(userPk, friendPk);
    }
}