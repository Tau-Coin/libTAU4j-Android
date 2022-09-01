package io.taucoin.tauapp.publishing.ui.main;

import android.app.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.core.model.TauDaemon;
import io.taucoin.tauapp.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.tauapp.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.tauapp.publishing.core.storage.RepositoryHelper;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;

/**
 * 主页的ViewModel
 */
public class MainViewModel extends AndroidViewModel {

    private final static Logger logger = LoggerFactory.getLogger("MainViewModel");
    private final CommunityRepository communityRepo;
    private final TauDaemon daemon;
    private MutableLiveData<ArrayList<CommunityAndFriend>> homeAllData = new MutableLiveData<>();
    private MutableLiveData<ArrayList<CommunityAndFriend>> homeCommunityData = new MutableLiveData<>();
    private MutableLiveData<ArrayList<CommunityAndFriend>> homeFriendData = new MutableLiveData<>();
    private CompositeDisposable disposables = new CompositeDisposable();
    private Disposable homeDisposable;
    public MainViewModel(@NonNull Application application) {
        super(application);
        communityRepo = RepositoryHelper.getCommunityRepository(getApplication());
        daemon = TauDaemon.getInstance(application);
    }

    /**
     * 观察不在黑名单的社区列表数据变化
     * @return 被观察的社区数据列表
     */
    Flowable<List<CommunityAndFriend>> observeCommunitiesAndFriends() {
        return communityRepo.observeCommunitiesAndFriends();
    }

    Flowable<Object> observeHomeChanged() {
        return communityRepo.observeHomeChanged();
    }

    public MutableLiveData<ArrayList<CommunityAndFriend>> getHomeAllData() {
        return homeAllData;
    }

    public MutableLiveData<ArrayList<CommunityAndFriend>> getHomeCommunityData() {
        return homeCommunityData;
    }

    public MutableLiveData<ArrayList<CommunityAndFriend>> getHomeFriendData() {
        return homeFriendData;
    }

    /**
     * 查询首页数据
     * homeDisposable、observeHomeChanged和intervalSeconds防止多次查询数据
     */
    void queryHomeData() {
        if (homeDisposable != null && !homeDisposable.isDisposed()) {
            return;
        }
        homeDisposable = Observable.create((ObservableOnSubscribe<ArrayList<CommunityAndFriend>>)
                 emitter -> {
            long start = DateUtil.getMillisTime();
            List<CommunityAndFriend> dataList = communityRepo.queryCommunitiesAndFriends();
            ArrayList<CommunityAndFriend> communities = new ArrayList<>();
            ArrayList<CommunityAndFriend> friends = new ArrayList<>();
            ArrayList<CommunityAndFriend> list = new ArrayList<>();
            if (dataList != null && dataList.size() > 0) {
                list.addAll(dataList);
                for(CommunityAndFriend data : dataList) {
                    if (data.type == 0) {
                        if (data.joined == 1) {
                            communities.add(data);
                        }
                    } else if (data.type == 1) {
                        friends.add(data);
                    }
                }
            }
            homeCommunityData.postValue(communities);
            homeFriendData.postValue(friends);
            long end = DateUtil.getMillisTime();
            logger.debug("queryHomeData time::{}, list::{}", end - start, list.size());
            emitter.onNext(list);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                 .subscribe(list -> homeAllData.postValue(list));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
        if (homeDisposable != null && !homeDisposable.isDisposed()) {
            homeDisposable.dispose();
        }
    }

    /**
     * 观察是否需要启动Daemon
     */
    void observeNeedStartDaemon() {
        disposables.add(daemon.observeNeedStartDaemon()
                .subscribeOn(Schedulers.io())
                .filter((needStart) -> needStart)
                .subscribe((needStart) -> daemon.start()));
    }

}