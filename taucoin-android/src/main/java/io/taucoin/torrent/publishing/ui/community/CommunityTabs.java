package io.taucoin.torrent.publishing.ui.community;

import java.util.HashSet;
import java.util.Set;

import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.StringUtil;

/**
 * 社区页面Tabs枚举类
 */
public enum CommunityTabs {
    NOTE(0, R.string.community_chain_note),
    MARKET(1, R.string.community_chain_market),
    QUEUE(2, R.string.community_tx_queue),
    CHAIN(3, R.string.community_on_chain);
    private int index;
    private int name;
    CommunityTabs(int index, int name) {
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getName() {
        return name;
    }

    public void setName(int name) {
        this.name = name;
    }

    public static Set<String> getIndexSet() {
        CommunityTabs[] values = CommunityTabs.values();
        Set<String> set = new HashSet<>();
        for (CommunityTabs value : values) {
            set.add(String.valueOf(value.getIndex()));
        }
        return set;
    }

    public static int getNameByIndex(int index) {
        CommunityTabs[] values = CommunityTabs.values();
        for (CommunityTabs value : values) {
            if (index == value.getIndex()) {
                return value.getName();
            }
        }
        return 0;
    }
}
