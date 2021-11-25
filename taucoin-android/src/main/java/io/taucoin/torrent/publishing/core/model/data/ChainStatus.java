package io.taucoin.torrent.publishing.core.model.data;

/**
 * Room: 查询链的状态信息
 */
public class ChainStatus {

    public long blockNumber;                     // 区块号
    public long difficulty;                      // 区块难度
    public long peerBlocks;                      // 当前peer的出块数
    public long totalRewards;                    // 当前peer的出块总奖励
    public long totalPeers;                      // 当前链的总peers
    public long totalCoin;                       // 当前链的总流通币量

}
