package io.taucoin.torrent.publishing.core.utils.bus;

import java.util.ArrayList;

import io.taucoin.torrent.publishing.core.model.data.CommunityAndFriend;

public class HomeCommunitiesData {

    private ArrayList<CommunityAndFriend> list;

    public HomeCommunitiesData(ArrayList<CommunityAndFriend> list) {
        this.list = list;
    }

    public ArrayList<CommunityAndFriend> getList() {
        if (list != null) {
            return list;
        }
        return new ArrayList<>();
    }
}
