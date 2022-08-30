package io.taucoin.tauapp.publishing.core.model.data;

import androidx.annotation.NonNull;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 用户和朋友联合查询
 */
public class UserAndFriend extends User {
    public long lastCommTime;
    public long lastSeenTime;
    public int status;
    public int onlineCount;

    public UserAndFriend(@NonNull String publicKey) {
        super(publicKey);
    }

    public boolean isDiscovered() {
        return status == 0;
    }

    public boolean isAdded() {
        return !isDiscovered();
    }
}
