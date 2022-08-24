package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;

import androidx.lifecycle.MutableLiveData;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.utils.StringUtil;

/**
 * 社区我的账户管理
 * 主要维护账户的是否过期
 */
public class MyAccountManager {
    private static final Logger logger = LoggerFactory.getLogger("MyAccountManager");
    private final LinkedBlockingQueue<String> chainsQueue = new LinkedBlockingQueue<>();
    public final MutableLiveData<CopyOnWriteArraySet<String>> notExpiredChain = new MutableLiveData<>();
    private final Disposable handlerDisposable;
    private final CommunityRepository communityRepo;

    MyAccountManager() {
        Context appContext = MainApplication.getInstance();
        communityRepo = RepositoryHelper.getCommunityRepository(appContext);
        handlerDisposable = createNotExpiredChainObserver();
        CopyOnWriteArraySet<String> set = new CopyOnWriteArraySet<>();
        notExpiredChain.postValue(set);
    }

    public MutableLiveData<CopyOnWriteArraySet<String>> getNotExpiredChain() {
        return notExpiredChain;
    }

    public boolean isNotExpired(String chainID) {
        CopyOnWriteArraySet<String> set = notExpiredChain.getValue();
        return set != null && set.contains(chainID);
    }

    void onCleared() {
        if (notExpiredChain.getValue() != null) {
            notExpiredChain.getValue().clear();
        }
        if (handlerDisposable != null && !handlerDisposable.isDisposed()) {
            handlerDisposable.dispose();
        }
    }

    /**
     * 更新社区
     * @param chainID
     */
    public void update(String chainID) {
        if (!chainsQueue.contains(chainID)) {
            logger.debug("update chainID::{}", chainID);
            chainsQueue.add(chainID);
        }
    }

    /**
     * 重置
     */
    public void reset() {
        logger.debug("reset all chain");
        // 清除所有待检查的链
        chainsQueue.clear();
        if (notExpiredChain.getValue() != null) {
            notExpiredChain.getValue().clear();
        }
        updateAllCommunities();
    }

    private void updateAllCommunities() {
        List<Community> communityList = communityRepo.getAllJoinedCommunityList();
        if (communityList != null) {
            for(Community community : communityList) {
                update(community.chainID);
            }
        }
    }

    public Disposable createNotExpiredChainObserver() {
        return Observable.create(emitter -> {
            updateAllCommunities();
            while (!emitter.isDisposed()) {
                try {
                    String chainID = chainsQueue.take();
                    String publicKey = communityRepo.queryCommunityAccountExpired(chainID);
                    boolean onChain = StringUtil.isNotEmpty(publicKey);
                    logger.debug("check chain::{}, onChain::{}, eventsQueue::{}", chainID, onChain, chainsQueue.size());
                    CopyOnWriteArraySet<String> set = notExpiredChain.getValue();
                    if (null == set) {
                        set = new CopyOnWriteArraySet<>();
                    }
                    boolean contains = set.contains(chainID);
                    if (onChain) {
                        if (!contains) {
                            set.add(chainID);
                            notExpiredChain.postValue(set);
                        }
                    } else {
                        if (contains) {
                            set.remove(chainID);
                            notExpiredChain.postValue(set);
                        }
                    }
                } catch (InterruptedException ignore) {
                    break;
                } catch (Exception e) {
                    logger.error("check chain:: ", e);
                    break;
                }
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
    }
}
