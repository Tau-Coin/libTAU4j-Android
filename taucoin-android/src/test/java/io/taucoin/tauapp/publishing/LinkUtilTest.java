package io.taucoin.tauapp.publishing;

import org.junit.Test;

import io.taucoin.tauapp.publishing.core.utils.LinkUtil;

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
        String link = LinkUtil.encodeChain(peer, chainID);
        System.out.println("Chain link::" + link);
    }

    @Test
    public void encodeAirdropLink() {
        String chainID = "55a10b4a30303030yang";
        String peer = "2d74d85bc8ef4db0485d38ccb035425190e1d1478cb77c5b6ee81e79cf64506d";
        String link = LinkUtil.encodeAirdrop(peer, chainID);
        System.out.println("Airdrop link::" + link);
    }

    @Test
    public void encodeReferralLink() {
        String chainID = "55a10b4a30303030yang";
        String peer = "2d74d85bc8ef4db0485d38ccb035425190e1d1478cb77c5b6ee81e79cf64506d";
        String chainLink = LinkUtil.encodeChain(peer, chainID);
        String link = LinkUtil.encodeAirdropReferral(LinkUtil.decode(chainLink), peer);
        System.out.println("Referral link::" + link);
        LinkUtil.Link decode = LinkUtil.decode(link);

        System.out.println("Referral link decode type::" + decode.getType());
        System.out.println("Referral link decode referralPeer::" + decode.getReferralPeer());
    }
}
