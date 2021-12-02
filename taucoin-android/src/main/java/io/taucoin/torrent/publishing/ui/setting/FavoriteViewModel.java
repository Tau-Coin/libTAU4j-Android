package io.taucoin.torrent.publishing.ui.setting;

import android.app.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.core.model.data.FavoriteAndUser;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FavoriteRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Favorite;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.ui.constant.Page;

/**
 * 收藏相关的ViewModel
 */
public class FavoriteViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("FavoriteViewModel");
    private TxRepository txRepo;
    private FavoriteRepository favoriteRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    public FavoriteViewModel(@NonNull Application application) {
        super(application);
        txRepo = RepositoryHelper.getTxRepository(getApplication());
        favoriteRepo = RepositoryHelper.getFavoriteRepository(getApplication());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    /**
     * 添加交易收藏
     * @param txID 交易ID
     */
    public void addTxFavorite(String txID){
        Disposable disposable = Flowable.create((FlowableOnSubscribe<List<UserAndTx>>) emitter -> {
            Favorite favorite = favoriteRepo.queryFavoriteByID(txID);
            if(null == favorite){
                Tx tx = txRepo.getTxByTxID(txID);
                if(tx != null){
                    favorite = new Favorite(tx.txID, tx.chainID, tx.senderPk, tx.receiverPk,
                            tx.amount, tx.fee, tx.txType, tx.memo);
                    favorite.timestamp = DateUtil.getMillisTime();
                    favoriteRepo.addFavorite(favorite);
                    logger.info("AddTxFavorite txID::{}, txType::{}, chainID::{}, memo::{}", tx.txID,
                            tx.txType, tx.chainID, tx.memo);
                }
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 删除收藏
     * @param ID 交易或Chat消息ID
     */
    public void delFavoriteByID(String ID){
        favoriteRepo.queryFavorites();
        Disposable disposable = Flowable.create((FlowableOnSubscribe<List<UserAndTx>>) emitter -> {
            favoriteRepo.delFavoriteByID(ID);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 查询收藏
     * @return DataSource
     */
    DataSource.Factory<Integer, FavoriteAndUser> queryFavorites(){
        return favoriteRepo.queryFavorites();
    }

    LiveData<PagedList<FavoriteAndUser>> observerFavorites() {
        return new LivePagedListBuilder<>(
                queryFavorites(), Page.getPageListConfig()).build();
    }
}