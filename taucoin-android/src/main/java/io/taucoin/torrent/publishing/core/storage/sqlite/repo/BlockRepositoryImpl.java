package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.taucoin.torrent.publishing.core.model.data.BlockAndTx;
import io.taucoin.torrent.publishing.core.model.data.ChainStatus;
import io.taucoin.torrent.publishing.core.model.data.DataChanged;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.BlockInfo;
import io.taucoin.torrent.publishing.core.utils.DateUtil;

/**
 * BlockRepository接口实现
 */
public class BlockRepositoryImpl implements BlockRepository{

    private Context appContext;
    private AppDatabase db;
    private PublishSubject<DataChanged> dataSetChangedPublish = PublishSubject.create();
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
        long result = db.blockDao().addBlock(block);
        submitDataSetChanged();
        return result;
    }

    /**
     * 更新Block数据
     * @param block Block对象
     * @return 结果
     */
    @Override
    public int updateBlock(@NonNull BlockInfo block) {
        int result = db.blockDao().updateBlock(block);
        submitDataSetChanged();
        return result;
    }

    @Override
    public Observable<DataChanged> observeDataSetChanged() {
        return dataSetChangedPublish;
    }

    @Override
    public void submitDataSetChanged() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(DateUtil.getDateTime());
        submitDataSetChangedDirect(stringBuilder);
    }

    private void submitDataSetChangedDirect(StringBuilder msg) {
        sender.submit(() -> {
            DataChanged result = new DataChanged();
            result.setMsg(msg.toString());
            dataSetChangedPublish.onNext(result);
        });
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

    @Override
    public List<BlockInfo> getBlocks(String chainID, long blockNumber) {
        return db.blockDao().getBlocks(chainID, blockNumber);
    }

    /**
     * 查询链上状态信息
     * @param chainID 链ID
     * @return Flowable<ChainStatus>
     */
    @Override
    public ChainStatus queryChainStatus(String chainID) {
        return db.blockDao().queryChainStatus(chainID);
    }

    @Override
    public Flowable<List<BlockAndTx>> observeCommunitySyncStatus(String chainID) {
        return db.blockDao().observeCommunitySyncStatus(chainID);
    }

    @Override
    public List<BlockAndTx> queryCommunityBlocks(String chainID, int pos, int pageSize) {
        return db.blockDao().queryCommunityBlocks(chainID, pos, pageSize);
    }
}