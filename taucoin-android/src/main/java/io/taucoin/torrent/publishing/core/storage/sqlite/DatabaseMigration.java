package io.taucoin.torrent.publishing.core.storage.sqlite;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.utils.DateUtil;

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
            // 添加交易类型字段，兼容老版本，默认值为TxType.
            int txType = TxType.WIRING_TX.getType();
            database.execSQL("ALTER TABLE TxQueues ADD COLUMN txType INTEGER NOT NULL DEFAULT " + txType);
            // 交易队列添加新的字段存储新类型
            database.execSQL("ALTER TABLE TxQueues ADD COLUMN content BLOB");

            // 添加社区消息未读、置顶字段
            database.execSQL("ALTER TABLE Members ADD COLUMN msgUnread INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE Members ADD COLUMN stickyTop INTEGER NOT NULL DEFAULT 0");
            // 朋友置顶字段
            database.execSQL("ALTER TABLE Friends ADD COLUMN stickyTop INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 添加交易发送状态, 默认老数据都是已发送
            database.execSQL("ALTER TABLE Txs ADD COLUMN sendStatus INTEGER NOT NULL DEFAULT 0");
            // 添加社区和区块入库时间
            long createTime = DateUtil.getMillisTime();
            database.execSQL("ALTER TABLE Blocks ADD COLUMN createTime INTEGER NOT NULL DEFAULT " + createTime);
        }
    };
}