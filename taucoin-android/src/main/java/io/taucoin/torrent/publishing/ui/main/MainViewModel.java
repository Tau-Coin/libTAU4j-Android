package io.taucoin.torrent.publishing.ui.main;

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
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.utils.DateUtil;

/**
 * 主页的ViewModel
 */
public class MainViewModel extends AndroidViewModel {

    private final static Logger logger = LoggerFactory.getLogger("MainViewModel");
    private CommunityRepository communityRepo;
    private TauDaemon daemon;
    private MutableLiveData<List<CommunityAndFriend>> homeData = new MutableLiveData<>();
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

    public MutableLiveData<List<CommunityAndFriend>> getHomeData() {
        return homeData;
    }

    /**
     * 查询首页数据
     * homeDisposable、observeHomeChanged和intervalSeconds防止多次查询数据
     */
    void queryHomeData() {
        if (homeDisposable != null && !homeDisposable.isDisposed()) {
            return;
        }
        homeDisposable = Observable.create((ObservableOnSubscribe<List<CommunityAndFriend>>)
                 emitter -> {
            long start = DateUtil.getMillisTime();
            List<CommunityAndFriend> list = communityRepo.queryCommunitiesAndFriends();
            if (null == list) {
                list = new ArrayList<>();
            }
            long end = DateUtil.getMillisTime();
            logger.debug("queryHomeData time::{}, list::{}", end - start, list.size());
            emitter.onNext(list);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                 .subscribe(list -> homeData.postValue(list));
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