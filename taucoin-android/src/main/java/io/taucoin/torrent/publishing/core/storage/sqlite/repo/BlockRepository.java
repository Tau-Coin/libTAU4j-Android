package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.taucoin.torrent.publishing.core.model.data.ChainStatus;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.BlockInfo;

/**
 * 提供操作Block数据的接口
 */
public interface BlockRepository {

    /**
     * 添加新的Block
     * @param block Block对象
     * @return 结果
     */
    long addBlock(@NonNull BlockInfo block);

    /**
     * 更新Block数据
     * @param block Block对象
     * @return 结果
     */
    int updateBlock(@NonNull BlockInfo block);

    Observable<String> observeDataSetChanged();

    void submitDataSetChanged();

    /**
     * 查询区块Block
     * @param chainID 链ID
     * @param blockHash 区块哈希
     * @return Block
     */
    BlockInfo getBlock(String chainID, String blockHash);

    /**
     * 观察链上状态信息
     * @param chainID 链ID
     * @return Flowable<ChainStatus>
     */
    Flowable<ChainStatus> observerChainStatus(String chainID);

    Flowable<List<BlockInfo>> observeCommunitySyncStatus(String chainID);
}
