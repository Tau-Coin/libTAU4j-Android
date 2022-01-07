package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.model.data.DataChanged;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;

/**
 * 提供外部操作User数据的接口
 */
public interface TxRepository {

    /**
     * 添加新的交易
     */
    long addTransaction(Tx transaction);

    /**
     * 更新交易
     */
    int updateTransaction(Tx transaction);

    /**
     * 根据chainID查询社区交易
     * @param chainID 社区链ID
     */
    List<UserAndTx> queryCommunityTxs(String chainID, int currentTab, int startPos, int loadSize);

    /**
     * 查询社区用户被Trust列表
     * @param chainID 社区链ID
     */
    List<Tx> queryCommunityTrustTxs(String chainID, String trustPk, int startPos, int loadSize);

    /**
     * 获取社区里用户未上链并且未过期的交易数
     * @param chainID chainID
     * @param publicKey 公钥
     * @param expireTime 过期时间时长
     * @return int
     */
    @Deprecated
    int getPendingTxsNotExpired(String chainID, String publicKey, long expireTime);
    /**
     * 获取社区里用户未上链并且未过期的交易数
     * @param chainID chainID
     * @param senderPk 公钥
     * @param expireTime 过期时间时长
     * @return int
     */
    @Deprecated
    Tx getEarliestExpireTx(String chainID, String senderPk, long expireTime);


    /**
     * 根据txID查询交易
     * @param txID 交易ID
     */
    Tx getTxByTxID(String txID);

    /**
     * 观察社区的交易的变化
     */
    Observable<DataChanged> observeDataSetChanged();

    /**
     * 提交数据变化
     */
    void submitDataSetChanged(Tx tx);

    /**
     * 提交数据变化
     */
    void submitDataSetChanged();

    Observable<UserAndTx> observeSellTxDetail(String chainID, String txID);
}
