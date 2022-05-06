package io.taucoin.torrent.publishing.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.core.model.data.AirdropUrl;

public class UrlUtil {

    private static final String URL_SCHEME = "tau:";
    public static final String CHAIN_PATTERN = "tauchain:\\?(dn=[a-f0-9]{16}[A-Za-z0-9\\s-%]+)(&bs=[a-f0-9]{64})+";
    public static final String AIRDROP_PATTERN = URL_SCHEME + "//[a-f0-9]{64}/airdrop/" + CHAIN_PATTERN;
    private static final String AIRDROP_URL = URL_SCHEME + "//%s/airdrop/%s";
    public static final String HASH_PATTERN = "[a-f0-9]{64}";

    public static boolean isTauUrl(String url) {
        return StringUtil.isNotEmpty(url) && url.startsWith("tau");
    }
    /**
     * Airdrop URL 解码
     * @param url 链URL
     * @return AirdropUrl
     */
    public static AirdropUrl decodeAirdropUrl(String url) {
        try {
            Pattern airdrop = Pattern.compile(AIRDROP_PATTERN);
            Matcher matcher = airdrop.matcher(url);
            Logger logger = LoggerFactory.getLogger("decodeAirdropUrl");
            logger.debug("url::{}", url);
            if (matcher.find()) {
                logger.debug("group::{}", matcher.group());
                url = matcher.group();
            }
            Matcher newMatcher = airdrop.matcher(url);
            if (newMatcher.matches()) {
                return new AirdropUrl(url);
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    public static boolean verifyAirdropUrl(String url) {
        try {
            if (StringUtil.isNotEmpty(url)) {
                Pattern airdrop = Pattern.compile(AIRDROP_PATTERN);
                Matcher matcher = airdrop.matcher(url);
                Logger logger = LoggerFactory.getLogger("decodeAirdropUrl");
                logger.debug("url::{}", url);
                return matcher.matches();
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    /**
     * Airdrop URL 编码
     * @param airdropPeer 发布airdrop的节点
     * @param chainID 链ID
     * @param peers 社区节点
     * @return AirdropUrl
     */
    public static String encodeAirdropUrl(@NonNull String airdropPeer, @NonNull String chainID,
                                          List<String> peers) {
        String chainUrl = ChainUrlUtil.encode(chainID, peers);
        return String.format(AIRDROP_URL, airdropPeer, chainUrl);
    }

    public static String encodeOnePeerAirdropUrl(@NonNull String airdropPeer, @NonNull String chainID,
                                          List<String> peers) {

        List<String> onePeerList = new ArrayList<>();
        if (peers != null && peers.size() > 0) {
            String onePeer = peers.get(0);
            if (peers.size() > 1 && StringUtil.isEquals(onePeer, airdropPeer)) {
                onePeer = peers.get(1);
            }
            onePeerList.add(onePeer);
        }
        return encodeAirdropUrl(airdropPeer, chainID, onePeerList);
    }
}