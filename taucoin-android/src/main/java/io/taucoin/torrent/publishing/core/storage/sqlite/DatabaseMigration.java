package io.taucoin.torrent.publishing.core.storage.sqlite;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;

/**
 * 数据库升级迁移类
 */
class DatabaseMigration {

    static Migration[] getMigrations(@NonNull Context appContext) {
        return new Migration[] {
                MIGRATION_1_2,
                MIGRATION_2_3
        };
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 兼容老版本，默认值为TxType.
            int txType = TxType.WIRING_TX.getType();
            database.execSQL("ALTER TABLE TxQueues ADD COLUMN txType INTEGER NOT NULL DEFAULT " + txType);
            database.execSQL("ALTER TABLE TxQueues ADD COLUMN content BLOB");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 添加社区消息未读
            database.execSQL("ALTER TABLE Members ADD COLUMN msgUnread INTEGER NOT NULL DEFAULT 0");
        }
    };
}