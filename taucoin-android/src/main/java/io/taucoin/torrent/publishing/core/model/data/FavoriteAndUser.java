package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Favorite;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 查询Favorite, 返回发送者的信息
 */
public class FavoriteAndUser extends Favorite {

    @Relation(parentColumn = "senderPk",
            entityColumn = "publicKey")
    public User sender;

    public FavoriteAndUser(@NonNull String ID, @NonNull String chainID, @NonNull String senderPk, long type, String memo) {
        super(ID, chainID, senderPk, type, memo);
    }
}
