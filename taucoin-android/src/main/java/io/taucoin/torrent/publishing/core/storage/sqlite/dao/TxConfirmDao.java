package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import io.reactivex.Observable;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxConfirm;

/**
 * Room:TxConfirm操作接口
 */
@Dao
public interface TxConfirmDao {

    String QUERY_TX_CONFIRM = "SELECT * FROM TxConfirms" +
            " WHERE txID = :txID AND peer = :peer";

    String QUERY_TX_CONFIRMS = "SELECT * FROM TxConfirms WHERE txID = :txID";

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addTxConfirm(TxConfirm txConfirm);

    @Query(QUERY_TX_CONFIRM)
    TxConfirm getTxConfirm(String txID, String peer);

    @Query(QUERY_TX_CONFIRMS)
    Observable<List<TxConfirm>> observerTxConfirms(String txID);
}