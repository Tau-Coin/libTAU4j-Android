package io.taucbd.news.publishing.core.utils.bus;

import java.util.ArrayList;

import io.taucbd.news.publishing.core.model.data.UserAndFriend;

public class Members {

    private ArrayList<UserAndFriend> list;

    public Members(ArrayList<UserAndFriend> list) {
        this.list = list;
    }

    public ArrayList<UserAndFriend> getList() {
        if (list != null) {
            return list;
        }
        return new ArrayList<>();
    }
}
