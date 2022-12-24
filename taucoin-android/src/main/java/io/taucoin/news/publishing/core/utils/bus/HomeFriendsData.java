package io.taucoin.news.publishing.core.utils.bus;

import java.util.ArrayList;

import io.taucoin.news.publishing.core.model.data.CommunityAndFriend;

public class HomeFriendsData {

    private ArrayList<CommunityAndFriend> list;

    public HomeFriendsData(ArrayList<CommunityAndFriend> list) {
        this.list = list;
    }

    public ArrayList<CommunityAndFriend> getList() {
        if (list != null) {
            return list;
        }
        return new ArrayList<>();
    }
}
