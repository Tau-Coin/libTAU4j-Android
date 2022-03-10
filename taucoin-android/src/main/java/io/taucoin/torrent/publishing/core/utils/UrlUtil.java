package io.taucoin.torrent.publishing.core.utils;

import org.libTAU4j.ChainURL;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.core.model.data.AirdropUrl;

public class UrlUtil {

    private static final String URL_SCHEME = "tau:";
    public static final String CHAIN_PATTERN = "tauchain:\\?(bs=[a-f0-9]{64}&)+(dn=[a-f0-9]{16}\\S{1,24})";
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
            if (!isTauUrl(url)) {
                return null;
            }
            Pattern airdrop = Pattern.compile(AIRDROP_PATTERN);
            Matcher matcher = airdrop.matcher(url);
            if (matcher.matches()) {
                AirdropUrl airdropUrl = new AirdropUrl();
                airdropUrl.setAirdropPeer(url.substring(6, 70));
                airdropUrl.setChainUrl(url.substring(79));
                ChainURL chainURL = ChainUrlUtil.decode(airdropUrl.getChainUrl());
                if (chainURL != null) {
                    airdropUrl.setChainID(ChainIDUtil.decode(chainURL.getChainID()));
                    airdropUrl.setPeers(new ArrayList<>(chainURL.getPeers()));
                    return airdropUrl;
                }
            }
        } catch (Exception ignore) {
        }
        return null;
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
}