package io.taucoin.tauapp.publishing.core.storage.sqlite.repo;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.taucoin.tauapp.publishing.core.model.data.BlockAndTx;
import io.taucoin.tauapp.publishing.core.model.data.ChainStatus;
import io.taucoin.tauapp.publishing.core.model.data.DataChanged;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.BlockInfo;

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

    Observable<DataChanged> observeDataSetChanged();

    void submitDataSetChanged();

    /**
     * 查询区块Block
     * @param chainID 链ID
     * @param blockHash 区块哈希
     * @return Block
     */
    BlockInfo getBlock(String chainID, String blockHash);

    Observable<BlockInfo> observeBlock(String chainID, String blockHash);

    /**
     * 查询区块Blocks
     * @param chainID 链ID
     * @param blockNumber 区块哈希
     * @return Block
     */
    List<BlockInfo> getBlocks(String chainID, long blockNumber);

    /**
     * 查询链上状态信息
     * @param chainID 链ID
     * @return Flowable<ChainStatus>
     */
    ChainStatus queryChainStatus(String chainID);

    Flowable<List<BlockAndTx>> observeCommunitySyncStatus(String chainID);

    List<BlockAndTx> queryCommunityBlocks(String chainID, int pos, int pageSize);

    /**
     * 处理由于回滚未置为offChain状态的区块
     * @param chainID
     * @param headBlock
     */
    int updateAllOffChainBlocks(String chainID, long headBlock);
}
