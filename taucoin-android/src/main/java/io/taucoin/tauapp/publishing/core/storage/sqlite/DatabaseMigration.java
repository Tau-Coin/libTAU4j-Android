package io.taucoin.tauapp.publishing.core.storage.sqlite;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

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
            // 添加挖矿奖励时间
            database.execSQL("ALTER TABLE Members ADD COLUMN rewardTime INTEGER NOT NULL DEFAULT 0");
            // 添加转账收入时间（已上链）
            database.execSQL("ALTER TABLE Members ADD COLUMN incomeTime INTEGER NOT NULL DEFAULT 0");
            // 添加朋友转账的时间（未上链）
            database.execSQL("ALTER TABLE Members ADD COLUMN pendingTime INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 添加mining power
            database.execSQL("ALTER TABLE Members ADD COLUMN power INTEGER NOT NULL DEFAULT 0");
        }
    };
}