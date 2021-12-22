package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.taucoin.torrent.publishing.core.model.data.FriendAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.utils.DateUtil;

/**
 * FriendRepository接口实现
 */
public class FriendRepositoryImpl implements FriendRepository{

    private Context appContext;
    private AppDatabase db;
    private PublishSubject<String> dataSetChangedPublish = PublishSubject.create();
    private ExecutorService sender = Executors.newSingleThreadExecutor();

    /**
     * FriendRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public FriendRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    @Override
    public void addFriend(Friend friend) {
        db.friendDao().addFriend(friend);
        submitDataSetChanged();
    }

    @Override
    public void updateFriend(Friend friend) {
        db.friendDao().updateFriend(friend);
        submitDataSetChanged();
    }

    @Override
    public Friend queryFriend(String userPK, String friendPK) {
        return db.friendDao().queryFriend(userPK, friendPK);
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
     * 查询自己的朋友
     * @return 朋友列表
     */
    @Override
    public List<FriendAndUser> queryFriendsByUserPk(String userPk) {
        return db.friendDao().queryFriendsByUserPk(userPk);
    }
}
