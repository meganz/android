package mega.privacy.android.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import mega.privacy.android.data.database.dao.ActiveTransferDao
import mega.privacy.android.data.database.dao.BackupDao
import mega.privacy.android.data.database.dao.CompletedTransferDao
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.database.dao.SdTransferDao
import mega.privacy.android.data.database.dao.SyncRecordDao
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.data.database.entity.BackupEntity
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.data.database.entity.ContactEntity
import mega.privacy.android.data.database.entity.SdTransferEntity
import mega.privacy.android.data.database.entity.SyncRecordEntity
import mega.privacy.android.data.database.spec.AutoMigrationSpec73to74

@Database(
    entities = [
        ContactEntity::class,
        CompletedTransferEntity::class,
        ActiveTransferEntity::class,
        SyncRecordEntity::class,
        SdTransferEntity::class,
        BackupEntity::class,
    ],
    version = MegaDatabaseConstant.DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(69, 70),
        AutoMigration(72, 73),
        AutoMigration(73, 74, spec = AutoMigrationSpec73to74::class),
    ],
)
internal abstract class MegaDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao

    abstract fun completedTransferDao(): CompletedTransferDao

    abstract fun activeTransfersDao(): ActiveTransferDao

    abstract fun syncRecordDao(): SyncRecordDao

    abstract fun sdTransferDao(): SdTransferDao

    abstract fun backupDao(): BackupDao

    companion object {
        private val MIGRATION_67_68 = object : Migration(67, 68) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }
        private val MIGRATION_68_69 = object : Migration(68, 69) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migrate column completedtransfers.transferoffline from BOOLEAN to TEXT type
                database.beginTransaction()
                try {
                    database.execSQL("ALTER TABLE completedtransfers RENAME TO completedtransfers_old")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `completedtransfers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `transferfilename` TEXT, `transfertype` TEXT, `transferstate` TEXT, `transfersize` TEXT, `transferhandle` TEXT, `transferpath` TEXT, `transferoffline` TEXT, `transfertimestamp` TEXT, `transfererror` TEXT, `transferoriginalpath` TEXT, `transferparenthandle` TEXT)")
                    database.execSQL("INSERT INTO completedtransfers(id, transferfilename, transfertype, transferstate, transfersize, transferhandle, transferpath, transferoffline, transfertimestamp, transfererror, transferoriginalpath, transferparenthandle) SELECT id, transferfilename, transfertype, transferstate, transfersize, transferhandle, transferpath, transferoffline, transfertimestamp, transfererror, transferoriginalpath, transferparenthandle FROM completedtransfers_old")
                    database.execSQL("DROP TABLE completedtransfers_old")
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
            }
        }
        private val MIGRATION_70_71 = object : Migration(70, 71) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS megacontacts")
            }
        }

        private val MIGRATION_71_72 = object : Migration(71, 72) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migrate column syncrecords.sync_copyonly and sync_secondary from BOOLEAN to TEXT type
                database.beginTransaction()
                try {
                    database.execSQL("ALTER TABLE syncrecords RENAME TO syncrecords_old")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `syncrecords` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `sync_filepath_origin` TEXT, `sync_filepath_new` TEXT, `sync_fingerprint_origin` TEXT, `sync_fingerprint_new` TEXT, `sync_timestamp` TEXT, `sync_filename` TEXT, `sync_longitude` TEXT, `sync_latitude` TEXT, `sync_state` INTEGER, `sync_type` INTEGER, `sync_handle` TEXT, `sync_copyonly` TEXT, `sync_secondary` TEXT)")
                    database.execSQL("INSERT INTO syncrecords(id, sync_filepath_origin, sync_filepath_new, sync_fingerprint_origin, sync_fingerprint_new, sync_timestamp, sync_state, sync_filename, sync_handle, sync_copyonly, sync_secondary, sync_type, sync_latitude, sync_longitude) SELECT id, sync_filepath_origin, sync_filepath_new, sync_fingerprint_origin, sync_fingerprint_new, sync_timestamp, sync_state, sync_filename, sync_handle, sync_copyonly, sync_secondary, sync_type, sync_latitude, sync_longitude FROM syncrecords_old")
                    database.execSQL("DROP TABLE syncrecords_old")
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
            }
        }

        private val MIGRATION_74_75 = object : Migration(74, 75) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migrate Type and Rename column backups.delete_empty_subolders and backups.exclude_subolders and backups.outdated from BOOLEAN to TEXT type
                database.beginTransaction()
                try {
                    database.execSQL("ALTER TABLE backups RENAME TO backups_old")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `backups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `backup_id` TEXT NOT NULL, `backup_type` INTEGER NOT NULL, `target_node` TEXT NOT NULL, `local_folder` TEXT NOT NULL, `backup_name` TEXT NOT NULL, `state` INTEGER NOT NULL, `sub_state` INTEGER NOT NULL, `extra_data` TEXT NOT NULL, `start_timestamp` TEXT NOT NULL, `last_sync_timestamp` TEXT NOT NULL, `target_folder_path` TEXT NOT NULL, `exclude_subFolders` TEXT NOT NULL, `delete_empty_subFolders` TEXT NOT NULL, `outdated` TEXT NOT NULL)")
                    database.execSQL("INSERT INTO backups(id, backup_id, backup_type, target_node, local_folder, backup_name, state, sub_state, extra_data, start_timestamp, last_sync_timestamp, target_folder_path, exclude_subFolders, delete_empty_subFolders, outdated) SELECT id, backup_id, backup_type, target_node, local_folder, backup_name, state, sub_state, extra_data, start_timestamp, last_sync_timestamp, target_folder_path, exclude_subolders, delete_empty_subolders, outdated FROM backups_old")
                    database.execSQL("DROP TABLE backups_old")
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
            }
        }
        val MIGRATIONS = arrayOf(
            MIGRATION_67_68,
            MIGRATION_68_69,
            MIGRATION_70_71,
            MIGRATION_71_72,
            MIGRATION_74_75
        )
    }
}
