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
        };
    }

//    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
//        @Override
//        public void migrate(@NonNull SupportSQLiteDatabase database) {
//            // 添加挖矿奖励时间
//            database.execSQL("ALTER TABLE Members ADD COLUMN rewardTime INTEGER NOT NULL DEFAULT 0");
//            // 添加转账收入时间（已上链）
//            database.execSQL("ALTER TABLE Members ADD COLUMN incomeTime INTEGER NOT NULL DEFAULT 0");
//            // 添加朋友转账的时间（未上链）
//            database.execSQL("ALTER TABLE Members ADD COLUMN pendingTime INTEGER NOT NULL DEFAULT 0");
//        }
//    };
//
//    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
//        @Override
//        public void migrate(@NonNull SupportSQLiteDatabase database) {
//            // 添加mining power
//            database.execSQL("ALTER TABLE Members ADD COLUMN power INTEGER NOT NULL DEFAULT 0");
//        }
//    };
//
//    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
//        @Override
//        public void migrate(@NonNull SupportSQLiteDatabase database) {
//            // 添加mining power
//            database.execSQL("ALTER TABLE Members ADD COLUMN newsUnread INTEGER NOT NULL DEFAULT 0");
//            database.execSQL("ALTER TABLE ChatMessages ADD COLUMN referralPeer TEXT");
//        }
//    };
//
//    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
//        @Override
//        public void migrate(@NonNull SupportSQLiteDatabase database) {
//            // 添加note交易previousHash供底层链式结构使用
//            database.execSQL("ALTER TABLE Txs ADD COLUMN previousHash TEXT");
//            database.execSQL("ALTER TABLE Txs ADD COLUMN version INTEGER NOT NULL DEFAULT 0");
//        }
//    };
//    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
//        @Override
//        public void migrate(@NonNull SupportSQLiteDatabase database) {
//            // 添加note交易previousHash供底层链式结构使用
//            database.execSQL("ALTER TABLE Txs ADD COLUMN repliedHash TEXT");
//        }
//    };
}