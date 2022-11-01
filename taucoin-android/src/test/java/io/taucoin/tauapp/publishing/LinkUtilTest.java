package io.taucoin.tauapp.publishing;

import org.junit.Test;

import java.util.Date;

import io.taucoin.tauapp.publishing.core.utils.LinkUtil;
import io.taucoin.tauapp.publishing.core.utils.base.Base58;
import io.taucoin.tauapp.publishing.core.utils.rlp.ByteUtil;

public class LinkUtilTest {
    @Test
    public void encodeFriendLink() {
        String peer = "2d74d85bc8ef4db0485d38ccb035425190e1d1478cb77c5b6ee81e79cf64506d";
        String link = LinkUtil.encodeFriend(peer, "yang");
        System.out.println("Friend link::" + link);
    }

    @Test
    public void encodeChainLink() {
        String chainID = "55a10b4a30303030yang";
        String peer = "2d74d85bc8ef4db0485d38ccb035425190e1d1478cb77c5b6ee81e79cf64506d";
        String peerBase58 = Base58.encode(ByteUtil.toByte(peer));
        System.out.println("Chain link peerBase58::" + peerBase58);
        System.out.println("Chain link peerBase58 size::" + peerBase58.length());
        String link = LinkUtil.encodeChain(peer, chainID, peer);

        System.out.println("********");
        System.out.println("Chain link::" + link);
        System.out.println("********");
    }

    @Test
    public void encodeAirdropLink() {
        String chainID = "55a10b4a30303030yang";
        String peer = "2d74d85bc8ef4db0485d38ccb035425190e1d1478cb77c5b6ee81e79cf64506d";
        long coins = 100;
        long time = new Date().getTime() / 60 / 1000;
        String link = LinkUtil.encodeAirdrop(peer, chainID, coins, time, peer);
        System.out.println("Airdrop link::" + link);
    }

    @Test
    public void encodeReferralLink() {
        String chainID = "55a10b4a30303030yang";
        String peer = "2d74d85bc8ef4db0485d38ccb035425190e1d1478cb77c5b6ee81e79cf64506d";
        long coins = 100;
        long time = new Date().getTime() / 60 / 1000;
        String airdropLink = LinkUtil.encodeAirdrop(peer, chainID, coins, time, peer);
        System.out.println("airdrop link1::" + airdropLink);
        LinkUtil.Link airdropObj = LinkUtil.decode(airdropLink);
        airdropLink = LinkUtil.encodeAirdrop(airdropObj.getPeer(), airdropObj.getData(),
                airdropObj.getCoins(), airdropObj.getTimestamp(), airdropObj.getMiner());
        System.out.println("airdrop link2::" + airdropLink);

        String referralLink = LinkUtil.encodeAirdropReferral(airdropObj, peer);
        System.out.println("\nReferral link::" + referralLink);
        LinkUtil.Link referralObj = LinkUtil.decode(referralLink);

        System.out.println("Referral link decode type::" + referralObj.getType());
        System.out.println("Referral link decode Miner::" + referralObj.getMiner());
        System.out.println("Referral link decode referralPeer::" + referralObj.getReferralPeer());
    }
}
