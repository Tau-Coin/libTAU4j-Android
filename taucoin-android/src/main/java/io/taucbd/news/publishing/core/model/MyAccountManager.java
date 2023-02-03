package io.taucbd.news.publishing.core.model;

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
import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.core.storage.RepositoryHelper;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Community;
import io.taucbd.news.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucbd.news.publishing.core.utils.StringUtil;

/**
 * 社区我的账户管理
 * 主要维护账户的是否过期
 */
public class MyAccountManager {
    private static final Logger logger = LoggerFactory.getLogger("MyAccountManager");
    private final LinkedBlockingQueue<String> chainsQueue = new LinkedBlockingQueue<>();
    private final CopyOnWriteArraySet<String> chainSet = new CopyOnWriteArraySet<>();
    public final MutableLiveData<CopyOnWriteArraySet<String>> notExpiredChain = new MutableLiveData<>();
    private final Disposable handlerDisposable;
    private final CommunityRepository communityRepo;

    MyAccountManager() {
        Context appContext = MainApplication.getInstance();
        communityRepo = RepositoryHelper.getCommunityRepository(appContext);
        handlerDisposable = createNotExpiredChainObserver();
        notExpiredChain.postValue(chainSet);
    }

    public MutableLiveData<CopyOnWriteArraySet<String>> getNotExpiredChain() {
        return notExpiredChain;
    }

    public CopyOnWriteArraySet<String> getSet() {
        return chainSet;
    }

    public boolean isNotExpired(String chainID) {
        logger.debug("chainID::{}, isNotExpired::{}, size::{}", chainID, chainSet.contains(chainID),
                chainSet.size());
        return chainSet.contains(chainID);
    }

    void onCleared() {
        chainSet.clear();
        notExpiredChain.postValue(chainSet);
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
        chainSet.clear();
        notExpiredChain.postValue(chainSet);
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
                    logger.debug("set::-----------::{}", chainSet.size());
                    boolean contains = chainSet.contains(chainID);
                    logger.debug("add chain::{}, contains::{}, size::{}", chainID, contains, chainSet.size());
                    if (onChain) {
                        if (!contains) {
                            chainSet.add(chainID);
                            notExpiredChain.postValue(chainSet);
                            logger.debug("add chain::{}, size::{}", chainID, chainSet.size());
                        }
                    } else {
                        if (contains) {
                            chainSet.remove(chainID);
                            notExpiredChain.postValue(chainSet);
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
