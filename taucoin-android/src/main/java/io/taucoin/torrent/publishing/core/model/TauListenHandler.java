package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import com.google.gson.Gson;

import org.libTAU4j.Account;
import org.libTAU4j.Block;
import org.libTAU4j.Transaction;
import org.libTAU4j.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.core.model.data.ConsensusInfo;
import io.taucoin.torrent.publishing.core.model.data.ForkPoint;
import io.taucoin.torrent.publishing.core.model.data.message.TxContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;

/**
 * TauListener处理程序
 */
class TauListenHandler {
    private static final Logger logger = LoggerFactory.getLogger("TauListenHandler");
    private UserRepository userRepo;
    private MemberRepository memberRepo;
    private TxRepository txRepo;
    private CommunityRepository communityRepo;
    private TauDaemon daemon;

    TauListenHandler(Context appContext, TauDaemon daemon) {
        this.daemon = daemon;
        userRepo = RepositoryHelper.getUserRepository(appContext);
        memberRepo = RepositoryHelper.getMemberRepository(appContext);
        txRepo = RepositoryHelper.getTxRepository(appContext);
        communityRepo = RepositoryHelper.getCommunityRepository(appContext);
    }

    /**
     * 保存社区：查询本地是否有此社区，没有则添加到本地
     * @param block 链上区块
     */
    private void saveCommunityInfo(String chainID, Block block, boolean isSync) {
        Community community = communityRepo.getCommunityByChainID(chainID);

        if (null == community) {
            community = new Community();
            community.chainID = chainID;
            community.communityName = ChainIDUtil.getName(community.chainID);
            community.totalBlocks = block.getBlockNumber();
            community.syncBlock = block.getBlockNumber();
            communityRepo.addCommunity(community);
            logger.info("SaveCommunity to local, communityName::{}, chainID::{}, " +
                            "totalBlocks::{}, syncBlock::{}", community.communityName,
                    community.chainID, community.totalBlocks + 1, community.syncBlock);
        } else {
            if (isSync) {
                community.syncBlock = block.getBlockNumber();
                if(community.totalBlocks < community.syncBlock){
                    community.totalBlocks = community.syncBlock;
                }
            } else {
                community.totalBlocks = block.getBlockNumber();
                if(community.totalBlocks < community.syncBlock){
                    community.syncBlock = community.totalBlocks;
                }
            }
            communityRepo.addCommunity(community);
            logger.info("Update Community Info, communityName::{}, chainID::{}, " +
                            "totalBlocks::{}, syncBlock::{}", community.communityName,
                    community.chainID, community.totalBlocks + 1, community.syncBlock);
        }
    }

    /**
     * 处理Block数据：解析Block数据，处理社区、交易、用户、社区成员数据
     * 0、更新社区信息
     * 1、处理矿工用户数据
     * 2、本地不存在该交易，添加交易数据、用户数据、社区成员数据
     * 3、本地存在该交易，更新交易状态、以及成员的balance和power值
     * @param block 链上区块
     * @param isRollback 区块回滚
     * @param isSync 区块向前同步
     */
    private void handleBlockData(Block block, boolean isRollback, boolean isSync) {
        String chainID = ChainIDUtil.decode(block.getChainID());
        logger.debug("handleBlockData:: chainID::{}，blockNum::{}, blockHash::{}", chainID,
                block.getBlockNumber(), ByteUtil.toHexString(block.getGenerationSignature()));
        // 更新社区信息
        saveCommunityInfo(chainID, block, isSync);
        Transaction txMsg = block.getTx();
        // 更新矿工的信息
        saveUserInfo(block.getMiner());
        // 添加发送者为社区成员
        addMemberInfo(block.getChainID(), block.getMiner(), isSync);
        if (txMsg != null) {
            String txID = txMsg.getTxID().to_hex();
            Tx tx = txRepo.getTxByTxID(txID);
            logger.debug("handleBlockData txID::{}, exist::{}, payload::{}", txID, tx != null,
                    txMsg.getPayload());
            // 本地不存在此交易
            if (null == tx) {
                handleTransactionData(txMsg, isRollback, isSync);
            } else {
                tx.txStatus = isRollback ? 0 : 1;
                txRepo.updateTransaction(tx);
                handleMemberInfo(txMsg);
            }
        }
    }

    /**
     * 处理上报新的区块
     * @param block Block
     * @param userPk 当前用户公钥
     */
    void handleNewBlock(Block block, String userPk) {
        logger.debug("handleNewBlock");
        handleBlockData(block, false, false);
    }

    /**
     * 处理上报被回滚的区块
     * @param block Block
     * @param userPk 当前用户公钥
     */
    void handleRollbackBlock(Block block, String userPk) {
        logger.debug("handleRollBack");
        handleBlockData(block, true, false);
    }

    /**
     * 处理上报向前同步的区块
     * @param block Block
     * @param userPk 当前用户公钥
     */
    void handleSyncBlock(Block block, String userPk) {
        logger.debug("handleSyncBlock");
        handleBlockData(block, false, true);
    }

    /**
     * 处理Block数据：解析Block数据，更新用户、社区成员、交易数据
     * @param txMsg Transaction
     */
    private void handleTransactionData(@NonNull Transaction txMsg, boolean isRollback, boolean isSync) {
        String txID = txMsg.getTxID().to_hex();
        String chainID = ChainIDUtil.decode(txMsg.getChainID());
        long fee = txMsg.getFee();
        byte[] payload = txMsg.getPayload();
        Tx tx = new Tx(txID, chainID, fee);
        if (null == payload || payload.length == 0) {
            logger.info("transaction payload empty");
            return;
        }
        TxContent txContent = new TxContent(txMsg.getPayload());
        tx.txType = txContent.getType();
        tx.memo = Utils.textBytesToString(txContent.getContent());
        tx.senderPk = ByteUtil.toHexString(txMsg.getSender());
        tx.txStatus = isRollback ? 0 : 1;
        // 保存发送者信息
        saveUserInfo(txMsg.getSender());
        // 添加发送者为社区成员
        addMemberInfo(txMsg.getChainID(), txMsg.getSender(), isSync);
        if (tx.txType == TxType.WRING_TX.getType()) {
            // 保存接受者信息
            saveUserInfo(txMsg.getReceiver());
            // 添加接受者为社区成员
            addMemberInfo(txMsg.getChainID(), txMsg.getReceiver(), isSync);
            // 添加交易
            tx.receiverPk = ByteUtil.toHexString(txMsg.getReceiver());
            tx.amount = txMsg.getAmount();
        }
        txRepo.addTransaction(tx);
        logger.info("Add transaction to local, txID::{}, txType::{}", txID, tx.txType);
    }

    /**
     * 处理社区成员信息
     * @param txMsg 交易
     */
    private void handleMemberInfo(@NonNull Transaction txMsg) {
        byte[] payload = txMsg.getPayload();
        if (null == payload || payload.length == 0) {
            logger.info("transaction payload empty");
            return;
        }
        TxContent txContent = new TxContent(txMsg.getPayload());
        int txType = txContent.getType();
        if (txType == TxType.CHAIN_NOTE.getType()) {
            addMemberInfo(txMsg.getChainID(), txMsg.getSender(), false);
        } else if (txType == TxType.WRING_TX.getType()) {
            addMemberInfo(txMsg.getChainID(), txMsg.getSender(), false);
            addMemberInfo(txMsg.getChainID(), txMsg.getReceiver(), false);
        }
    }

    /**
     * 保存用户信息到本地
     * @param publicKey 公钥
     */
    private void saveUserInfo(byte[] publicKey) {
        if (null == publicKey) {
            return;
        }
        saveUserInfo(ByteUtil.toHexString(publicKey));
    }

    /**
     * 保存用户信息到本地
     * @param publicKey 公钥
     */
    private void saveUserInfo(String publicKey) {
        if (StringUtil.isEmpty(publicKey)) {
            return;
        }
        User user = userRepo.getUserByPublicKey(publicKey);
        if (null == user) {
            user = new User(publicKey);
            userRepo.addUser(user);
            logger.info("SaveUserInfo to local, publicKey::{}", publicKey);
        }
    }

    /**
     * 添加社区成员到本地
     * @param publicKey 公钥
     * @param chainID chainID
     */
    private void addMemberInfo(byte[] chainID, byte[] publicKey, boolean isSync) {
        addMemberInfo(chainID, ByteUtil.toHexString(publicKey), isSync);
    }

    /**
     * 添加社区成员到本地
     * @param publicKey 公钥
     * @param chainID chainID
     */
    private void addMemberInfo(byte[] chainID, String publicKey, boolean isSync) {
        String chainIDStr = ChainIDUtil.decode(chainID);
        Member member = memberRepo.getMemberByChainIDAndPk(chainIDStr, publicKey);
        Account account = daemon.getAccountInfo(chainID, publicKey);
        long balance = 0;
        long power = 0;
        if (account != null) {
            balance = account.getBalance();
            power = account.getEffectivePower();
        }

        if (null == member) {
            member = new Member(chainIDStr, publicKey, balance, power);
            memberRepo.addMember(member);
            logger.info("AddMemberInfo to local, chainID::{}, publicKey::{}, balance::{}, power::{}",
                    chainIDStr, publicKey, balance, power);
        } else {
            if(!isSync){
                member.balance = balance;
                member.power = power;
                memberRepo.updateMember(member);
                logger.info("Update Member's balance and power, chainID::{}, publicKey::{}, " +
                        "balance::{}, power::{}", chainIDStr, publicKey, member.balance, member.power);
            }
        }
    }

    /**
     * 处理libTAU上报接收到新的交易（未上链在交易池中）
     * @param tx 交易
     */
    void handleNewTransaction(Transaction tx) {
        logger.info("handleNewTransaction");
        handleTransactionData(tx, true, false);
    }

    /**
     * 处理libTAU上报分叉点
     * @param block 分叉点
     */
    void handleNewForkPoint(Block block) {
        String chainID = ChainIDUtil.decode(block.getChainID());
        Community community = communityRepo.getCommunityByChainID(chainID);
        if (community != null) {
            String hash = block.Hash();
            long number = block.getBlockNumber();
            ForkPoint point = new ForkPoint(hash, number);
            community.forkPoint = new Gson().toJson(point);
            communityRepo.updateCommunity(community);
            logger.info("handleNewForkPoint chainID::{}, hash::{}, number::{}", chainID, hash, number);
        }
    }

    /**
     * 处理libTAU上报节点投票
     * @param chainID 链的ID
     * @param votes 投票结果
     */
    void handleNewTopVotes(byte[] chainID, List<Vote> votes) {
        logger.info("handleNewForkPoint");
        String chainIDStr = ChainIDUtil.decode(chainID);
        Community community = communityRepo.getCommunityByChainID(chainIDStr);
        if (community != null) {
            List<ConsensusInfo> list = new ArrayList<>();
            for (Vote vote : votes) {
                String hash = vote.getBlockHash().to_hex();
                long number = vote.getBlockNumber();
                long count = vote.getVoteCount();
                ConsensusInfo info = new ConsensusInfo(hash, number, count);
                list.add(info);
            }
            community.topConsensus = new Gson().toJson(list);
            communityRepo.updateCommunity(community);
            logger.info("handleNewForkPoint chainID::{}, topConsensus::{}", chainIDStr,
                    community.topConsensus);
        }
    }
}
