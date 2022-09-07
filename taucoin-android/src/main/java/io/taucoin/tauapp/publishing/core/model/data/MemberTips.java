package io.taucoin.tauapp.publishing.core.model.data;

/**
 * Room: 成员余额变动提示
 */
public class MemberTips {
    public long rewardTime;             // 成员挖矿奖励时间
    public long incomeTime;             // 成员收到转账收入时间
    public long pendingTime;            // 成员收到朋友转账动作（未上链）
}
