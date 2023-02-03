package io.taucbd.news.publishing;

import org.junit.Test;

import java.util.Date;

import io.taucbd.news.publishing.core.utils.LinkUtil;
import io.taucbd.news.publishing.core.utils.base.Base58;
import io.taucbd.news.publishing.core.utils.rlp.ByteUtil;

public class LinkUtilTest {
    @Test
    public void encodeFriendLink() {
        String peer = "2d74d85bc8ef4db0485d38ccb035425190e1d1478cb77c5b6ee81e79cf64506d";
        String link = LinkUtil.encodeFriend(peer, "yang");
        System.out.println("Friend link1::" + link);

        LinkUtil.Link decode = LinkUtil.decode(link);
        link = LinkUtil.encodeFriend(decode.getPeer(), decode.getData());

        System.out.println("Friend link2::" + link);
    }

    @Test
    public void encodeChainLink() {
        String chainID = "55a10b4a30303030yang";
        String peer = "2d74d85bc8ef4db0485d38ccb035425190e1d1478cb77c5b6ee81e79cf64506d";
        String link = LinkUtil.encodeChain(peer, chainID, peer);
        System.out.println("********");
        System.out.println("Chain link1::" + link);
        System.out.println("********");

        LinkUtil.Link decode = LinkUtil.decode(link);
        link = LinkUtil.encodeChain(decode.getPeer(), decode.getData(), decode.getMiner());
        System.out.println("Chain link2::" + link);
        System.out.println("********");
    }

    @Test
    public void encodeAirdropLink() {
        String chainID = "55a10b4a30303030yang";
        String peer = "2d74d85bc8ef4db0485d38ccb035425190e1d1478cb77c5b6ee81e79cf64506d";
        long coins = 100;
        long time = new Date().getTime() / 60 / 1000;
        String link = LinkUtil.encodeAirdrop(peer, chainID, coins, time);
        System.out.println("Airdrop link1::" + link);
        LinkUtil.Link decode1 = LinkUtil.decode(link);
        link = LinkUtil.encodeAirdrop(decode1.getPeer(), decode1.getData(), decode1.getCoins(), decode1.getTimestamp());
        System.out.println("Airdrop link2::" + link);

        String link1 = "tau://1xS5Xxyp9BicHcjjb9RJZHpKuu7gzGcRkfc5TZiwrn3/airdrop/2edf342a30303030Happy&100&27796775";
        LinkUtil.Link decode = LinkUtil.decode(link1);
        if (decode != null) {
            System.out.println("Airdrop type::" + decode.getType());
            System.out.println("Airdrop chainID::" + decode.getData());
            System.out.println("Airdrop size::" + Base58.encode(ByteUtil.toByte(decode.getPeer())).length());
        }

        LinkUtil.Link link3 = LinkUtil.decode(link1);
        if (link3 != null) {
            System.out.println("Airdrop type::" + link3.getType());
            System.out.println("Airdrop chainID::" + link3.getData());
        } else {
            System.out.println("Airdrop null::" + link1);
        }
    }

    @Test
    public void encodeReferralLink() {
        String chainID = "55a10b4a30303030yang";
        String peer = "2d74d85bc8ef4db0485d38ccb035425190e1d1478cb77c5b6ee81e79cf64506d";
        long coins = 100;
        long time = new Date().getTime() / 60 / 1000;
        String airdropLink = LinkUtil.encodeAirdrop(peer, chainID, coins, time);
        System.out.println("airdrop link1::" + airdropLink);
        LinkUtil.Link airdropObj = LinkUtil.decode(airdropLink);
        airdropLink = LinkUtil.encodeAirdrop(airdropObj.getPeer(), airdropObj.getData(),
                airdropObj.getCoins(), airdropObj.getTimestamp());
        System.out.println("airdrop link2::" + airdropLink);

        String referralLink = LinkUtil.encodeAirdropReferral(airdropObj, peer);
        System.out.println("\nReferral link1::" + referralLink);
        LinkUtil.Link referralObj = LinkUtil.decode(referralLink);

        referralLink = LinkUtil.encodeAirdropReferral(referralObj.getPeer(), referralObj.getData(),
                referralObj.getCoins(), referralObj.getTimestamp(), referralObj.getReferralPeer());
        System.out.println("\nReferral link2::" + referralLink);

        System.out.println("Referral link decode type::" + referralObj.getType());
        System.out.println("Referral link decode Peer::" + referralObj.getPeer());
        System.out.println("Referral link decode chainID::" + referralObj.getData());
        System.out.println("Referral link decode coins::" + referralObj.getCoins());
        System.out.println("Referral link decode time::" + referralObj.getTimestamp());
        System.out.println("Referral link decode referralPeer::" + referralObj.getReferralPeer());
    }
}
