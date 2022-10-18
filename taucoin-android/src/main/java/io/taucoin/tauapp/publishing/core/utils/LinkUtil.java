package io.taucoin.tauapp.publishing.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import io.taucoin.tauapp.publishing.core.utils.base.Base58;
import io.taucoin.tauapp.publishing.core.utils.rlp.ByteUtil;

public class LinkUtil {
    private static final Logger logger = LoggerFactory.getLogger("LinkUtil");

    private static final String URL_SCHEME = "tau:";
    private static final String LINK_FORMAT = URL_SCHEME + "//%s/%s/%s&%d";
    private static final String REFERRAL_LINK_FORMAT = URL_SCHEME + "//%s/%s/%s&%d&%s";
    private static final String LINK_FRIEND = "friend";
    private static final String LINK_CHAIN = "chain";
    private static final String LINK_AIRDROP = "airdrop";

    private static final String SPACES_REPLACE = "%20";
    private static final String SPACES = " ";

    // Base58编码：包括9个数字，24个大写字母，25个小写字母
    private static final String PATTERN_KEY = "([123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]+)";
    private static final String PATTERN_PREFIX = URL_SCHEME + "//" + PATTERN_KEY + "/";
    private static final String PATTERN_SUFFIX = "/([a-f0-9]{16}[A-Za-z0-9\\s-%]+)&[0-9]{8}";
    private static final String FRIEND_PATTERN_SUFFIX = "/([^&]+)&[0-9]{8}";
    public static final String FRIEND_PATTERN = PATTERN_PREFIX + LINK_FRIEND + FRIEND_PATTERN_SUFFIX;
    public static final String CHAIN_PATTERN = PATTERN_PREFIX + LINK_CHAIN + PATTERN_SUFFIX;
    public static final String AIRDROP_PATTERN = PATTERN_PREFIX + LINK_AIRDROP + PATTERN_SUFFIX;
    public static final String REFERRAL_PATTERN = PATTERN_PREFIX + LINK_AIRDROP + PATTERN_SUFFIX + "&" + PATTERN_KEY;

    public static boolean isTauUrl(String url) {
        return StringUtil.isNotEmpty(url) && url.startsWith("tau");
    }

    /**
     * Airdrop link 解码
     * @param link 链link
     * @return Link
     */
    public static Link decodeAirdropLink(String link) {
        String newLink = matcherLink(link, AIRDROP_PATTERN);
        if (StringUtil.isNotEmpty(newLink)) {
            decode(newLink);
        }
        return decode(link);
    }

    private static String matcherLink(String link, String regex) {
        try {
            Pattern airdrop = Pattern.compile(regex);
            Matcher matcher = airdrop.matcher(link);
            logger.debug("link::{}", link);
            if (matcher.find()) {
                String newLink = matcher.group();
                logger.debug("group::{}", newLink);
                Matcher newMatcher = airdrop.matcher(newLink);
                if (newMatcher.matches()) {
                    return newLink;
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    public static Link decodeLink(String link) {
        String newLink = matcherLink(link, REFERRAL_PATTERN);
        if (StringUtil.isEmpty(newLink)) {
            newLink = matcherLink(link, AIRDROP_PATTERN);
        }
        if (StringUtil.isEmpty(newLink)) {
            newLink = matcherLink(link, CHAIN_PATTERN);
        }
        if (StringUtil.isNotEmpty(newLink)) {
            return decode(newLink);
        }
        return decode(link);
    }

    /**
     * TAUChain 解码
     * @param url 链URL
     * @return ChainURL
     */
    public static Link decode(String url) {
        Link link = new Link();
        link.setLink(url);
        try {
            if (!isTauUrl(url)) {
                return link;
            }
            if (StringUtil.isNotEmpty(url)) {
                if (url.contains(SPACES_REPLACE)) {
                    url = url.replaceAll(SPACES_REPLACE, SPACES);
                }
                String[] splits = url.split("/");
                if (splits != null && splits.length == 5) {
                    String timeAndData = splits[4];
                    String[] data = timeAndData.split("&");
                    if (data != null && data.length >= 2) {
                        link.setData(data[0]);
                        link.setTimestamp(Long.parseLong(data[1]));
                    }
                    if (data != null && data.length >= 3) {
                        byte[] referralPeer = Base58.decode(data[2]);
                        String referralPeerStr = ByteUtil.toHexString(referralPeer);
                        link.setReferralPeer(referralPeerStr);
                    }
                    logger.debug("splits[2]::{}", splits[2]);
                    byte[] peer = Base58.decode(splits[2]);
                    String peerStr = ByteUtil.toHexString(peer);
                    logger.debug("peerStr::{}", peerStr);
                    link.setPeer(peerStr);
                    link.setType(splits[3]);

                    logger.debug("peer::{}, linkType::{}, data::{}, timestamp::{}", link.getPeer(),
                            link.getType(), link.getData(), link.getTimestamp());
                }
                return link;
            }
        } catch (Exception ignore) {
        }
        return link;
    }

    /**
     * 朋友 编码
     * @param peer 当前节点
     * @param nickname 昵称
     * @return 链URL
     */
    public static String encodeFriend(@NonNull String peer, @NonNull String nickname) {
        return encode(peer, LINK_FRIEND, nickname, null);
    }

    /**
     * chain 编码
     * @param peer 当前节点
     * @param chainID 链ID
     * @return 链URL
     */
    public static String encodeChain(@NonNull String peer, @NonNull String chainID) {
        return encode(peer, LINK_CHAIN, chainID, null);
    }

    /**
     * Airdrop 编码
     * @param peer 当前节点
     * @param chainID 链ID
     * @return 链URL
     */
    public static String encodeAirdrop(@NonNull String peer, @NonNull String chainID) {
        return encode(peer, LINK_AIRDROP, chainID, null);
    }

    public static String encodeAirdrop(@NonNull String peer, @NonNull String chainID, long timestamp) {
        return encode(peer, LINK_AIRDROP, chainID, timestamp, null);
    }

    /**
     * 创建Airdrop推荐
     * @param link airdrop Link
     * @param referralPeer 链ID
     * @return 链URL
     */
    public static String encodeAirdropReferral(LinkUtil.Link link, String referralPeer) {
        return encode(link.getPeer(), LINK_AIRDROP, link.getData(), link.getTimestamp(), referralPeer);
    }

    public static String encodeAirdropReferral(@NonNull String peer, @NonNull String chainID, long timestamp, String referralPeer) {
        return encode(peer, LINK_AIRDROP, chainID, timestamp, referralPeer);
    }

    private static String encode(@NonNull String peer, @NonNull String linkType, @NonNull String data,
                                 String referralPeer) {
        long timestamp = DateUtil.getTime() / 60;
        return encode(peer, linkType, data, timestamp, referralPeer);
    }

    private static String encode(@NonNull String peer, @NonNull String linkType, @NonNull String data,
                                 long timestamp, String referralPeer) {
        String peerBase58 = Base58.encode(ByteUtil.toByte(peer));
        logger.debug("peerBase58::{}, size::{}", peerBase58, peerBase58.length());
        String link;
        if (StringUtil.isNotEmpty(referralPeer)) {
            String referralPeerBase58 = Base58.encode(ByteUtil.toByte(referralPeer));
            link = String.format(Locale.ENGLISH, REFERRAL_LINK_FORMAT, peerBase58, linkType, data, timestamp, referralPeerBase58);
        } else {
            link = String.format(Locale.ENGLISH, LINK_FORMAT, peerBase58, linkType, data, timestamp);
        }
        if (link.contains(SPACES)) {
            link = link.replaceAll(SPACES, SPACES_REPLACE);
        }
        logger.debug("peer::{}, linkType::{}, data::{}, timestamp::{}, referralPeer::{}", peer,
                linkType, data, timestamp, referralPeer);
        logger.debug("link::{}", link);
        decode(link);
        return link;
    }

    public static class Link {
        private String link;
        private String peer;
        private String referralPeer;
        private String type;
        private String data;
        private long timestamp;

        public String getPeer() {
            return peer;
        }

        public void setPeer(String peer) {
            this.peer = peer;
        }

        public String getReferralPeer() {
            return referralPeer;
        }

        public void setReferralPeer(String referralPeer) {
            this.referralPeer = referralPeer;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public boolean isAirdropLink() {
            return StringUtil.isEquals(this.type, LINK_AIRDROP);
        }

        public boolean isChainLink() {
            return StringUtil.isEquals(this.type, LINK_CHAIN);
        }

        public boolean isFriendLink() {
            return StringUtil.isEquals(this.type, LINK_FRIEND);
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }
    }

    public static void main(String[] args) {
        String chainID = "55a10b4a30303030yang";
        String peer = "2d74d85bc8ef4db0485d38ccb035425190e1d1478cb77c5b6ee81e79cf64506d";
        encodeFriend(peer, "yang");
        System.out.println("encodeFriend::" + encodeFriend(peer, "yang"));
    }
}
