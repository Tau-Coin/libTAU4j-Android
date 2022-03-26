package io.taucoin.torrent.publishing.ui.transaction;

import android.widget.TextView;

import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

public interface CommunityListener  {

    void onTrustClicked(User user);
    void onBanClicked(UserAndTx tx);
    void onEditNameClicked(String senderPk);
    void onUserClicked(String senderPk);
    void onItemLongClicked(TextView view, UserAndTx tx);
    void onItemClicked(UserAndTx tx);
    void onLinkClick(String link);
    void onResendClick(String txID);

}
