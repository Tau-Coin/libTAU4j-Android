package io.taucoin.tauapp.publishing.core.model.data;

/**
 * Room: 查询链的状态信息
 */
public class ChainStatus {

    public long syncingHeadBlock;                // 正在同步（下载）头部块号
    public long headBlock;                       // 头部块号
    public long consensusBlock;                  // 共识区块号
    public long difficulty;                      // 区块难度
    public long peerBlocks;                      // 当前peer的出块数
    public long totalRewards;                    // 当前peer的出块总奖励
    public long totalPeers;                      // 当前链的总peers
    public long totalCoin;                       // 当前链的总流通币量
    public long balance;                         // 当前链用户余额
    public long balUpdateTime;                   // 当前链用户余额更新时间
    public String forkPoint;                     // 当前链分叉点

}
