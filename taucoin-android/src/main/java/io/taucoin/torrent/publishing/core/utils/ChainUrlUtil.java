package io.taucoin.torrent.publishing.core.utils;

import org.libTAU4j.ChainURL;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;

public class ChainUrlUtil {
    /**
     * TAUChain 编码
     * @param url 链URL
     * @return ChainURL
     */
    public static ChainURL decode(String url) {
        try {
            return ChainURL.chainURLStringToChainURL(url);
        } catch (Exception ignore) { }
        return null;
    }

    /**
     * TAUChain 解码
     * @param chainID 链ID
     * @param peers 社区节点
     * @return 链URL
     */
    public static String encode(@NonNull String chainID, @NonNull List<String> peers) {
        byte[] chainIDBytes = ChainIDUtil.encode(chainID);
        Set<String> peersSet = new HashSet<>(peers);
        ChainURL chainURL = new ChainURL(chainIDBytes, peersSet);
        byte[] url = chainURL.getURL();
        return ChainURL.chainURLBytesToString(url);
    }
}