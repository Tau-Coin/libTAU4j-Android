package io.taucoin.torrent.publishing.core.utils;

import org.libTAU4j.ChainURL;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;

public class ChainUrlUtil {
    private static String SPACES_REPLACE = "%20";
    private static String SPACES = " ";
    /**
     * TAUChain 编码
     * @param url 链URL
     * @return ChainURL
     */
    public static ChainURL decode(String url) {
        try {
            if (!UrlUtil.isTauUrl(url)) {
                return null;
            }
            if (StringUtil.isNotEmpty(url)) {
                if (url.contains(SPACES_REPLACE)) {
                    url = url.replaceAll(SPACES_REPLACE, SPACES);
                }
                return ChainURL.chainURLStringToChainURL(url);
            }
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
        Set<String> peersSet = new HashSet<>(peers);
        ChainURL chainURL = new ChainURL(chainID, peersSet);

        String chainUrl = chainURL.getStringURL();

        if (StringUtil.isNotEmpty(chainUrl)) {
            if (chainUrl.contains(SPACES)) {
                chainUrl = chainUrl.replaceAll(SPACES, SPACES_REPLACE);
            }
        }
        return chainUrl;
    }
}
