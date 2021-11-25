package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.taucoin.torrent.publishing.core.model.data.ChainStatus;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.BlockInfo;
import io.taucoin.torrent.publishing.core.utils.DateUtil;

/**
 * BlockRepository接口实现
 */
public class BlockRepositoryImpl implements BlockRepository{

    private Context appContext;
    private AppDatabase db;
    private PublishSubject<String> dataSetChangedPublish = PublishSubject.create();
    private ExecutorService sender = Executors.newSingleThreadExecutor();

    /**
     * MemberRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public BlockRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    /**
     * 添加新的Block
     * @param block Block对象
     * @return 结果
     */
    @Override
    public long addBlock(@NonNull BlockInfo block) {
        return db.blockDao().addBlock(block);
    }

    /**
     * 更新Block数据
     * @param block Block对象
     * @return 结果
     */
    @Override
    public int updateBlock(@NonNull BlockInfo block) {
        return db.blockDao().updateBlock(block);
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
     * 查询区块Block
     * @param chainID 链ID
     * @param blockHash 区块哈希
     * @return Block
     */
    @Override
    public BlockInfo getBlock(String chainID, String blockHash) {
        return db.blockDao().getBlock(chainID, blockHash);
    }

    /**
     * 观察链上状态信息
     * @param chainID 链ID
     * @return Flowable<ChainStatus>
     */
    @Override
    public Flowable<ChainStatus> observerChainStatus(String chainID) {
        return db.blockDao().observerChainStatus(chainID);
    }
}