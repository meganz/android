package mega.privacy.android.data.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import mega.privacy.android.data.database.dao.ActiveTransferDao
import mega.privacy.android.data.database.dao.BackupDao
import mega.privacy.android.data.database.dao.CameraUploadsRecordDao
import mega.privacy.android.data.database.dao.CompletedTransferDao
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.database.dao.OfflineDao
import mega.privacy.android.data.database.dao.SdTransferDao
import mega.privacy.android.data.database.dao.SyncSolvedIssuesDao
import mega.privacy.android.data.database.dao.UserPausedSyncsDao
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.data.database.entity.BackupEntity
import mega.privacy.android.data.database.entity.CameraUploadsRecordEntity
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.data.database.entity.ContactEntity
import mega.privacy.android.data.database.entity.OfflineEntity
import mega.privacy.android.data.database.entity.SdTransferEntity
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity
import mega.privacy.android.data.database.entity.UserPausedSyncEntity
import mega.privacy.android.data.database.spec.AutoMigrationSpec73to74
import mega.privacy.android.data.database.spec.AutoMigrationSpec81to82
import timber.log.Timber

@Database(
    entities = [
        ContactEntity::class,
        CompletedTransferEntity::class,
        ActiveTransferEntity::class,
        SdTransferEntity::class,
        BackupEntity::class,
        OfflineEntity::class,
        SyncSolvedIssueEntity::class,
        UserPausedSyncEntity::class,
        CameraUploadsRecordEntity::class,
    ],
    version = MegaDatabaseConstant.DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(69, 70),
        AutoMigration(72, 73),
        AutoMigration(73, 74, spec = AutoMigrationSpec73to74::class),
        AutoMigration(78, 79),
        AutoMigration(79, 80),
        AutoMigration(80, 81),
        AutoMigration(81, 82, spec = AutoMigrationSpec81to82::class),
    ],
)
internal abstract class MegaDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao

    abstract fun completedTransferDao(): CompletedTransferDao

    abstract fun activeTransfersDao(): ActiveTransferDao

    abstract fun sdTransferDao(): SdTransferDao

    abstract fun backupDao(): BackupDao

    abstract fun offlineDao(): OfflineDao

    abstract fun syncSolvedIssuesDao(): SyncSolvedIssuesDao

    abstract fun userPausedSyncDao(): UserPausedSyncsDao

    abstract fun cameraUploadsRecordDao(): CameraUploadsRecordDao

    companion object {

        /**
         * Init MegaDatabase
         *
         * @param context Context
         * @param factory SupportSQLiteOpenHelper.Factory
         * @param legacyDatabaseMigration LegacyDatabaseMigration
         */
        fun init(
            context: Context,
            factory: SupportSQLiteOpenHelper.Factory,
            legacyDatabaseMigration: LegacyDatabaseMigration,
        ): MegaDatabase = Room.databaseBuilder(
            context,
            MegaDatabase::class.java, MegaDatabaseConstant.DATABASE_NAME
        ).fallbackToDestructiveMigrationFrom(
            *(1..66).toList().toIntArray() // allow destructive migration for version 1 to 66
        ).addMigrations(*MIGRATIONS)
            .openHelperFactory(
                MegaOpenHelperFactor(
                    factory,
                    legacyDatabaseMigration
                )
            )
            .build()

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

        private val MIGRATION_75_76 = object : Migration(75, 76) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }

        private val MIGRATION_76_77 = object : Migration(76, 77) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migrate Type and Rename column backups.delete_empty_subolders and backups.exclude_subolders and backups.outdated from BOOLEAN to TEXT type
                try {
                    database.execSQL("ALTER TABLE offline RENAME TO offline_old")
                    database.execSQL("CREATE TABLE IF NOT EXISTS offline (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `handle` TEXT, `path` TEXT, `name` TEXT, `parentId` INTEGER, `type` TEXT, `incoming` INTEGER, `incomingHandle` TEXT, `lastModifiedTime` INTEGER)")
                    database.execSQL("INSERT INTO offline (id, handle, path, name, parentId, type, incoming, incomingHandle) SELECT id, handle, path, name, parentId, type, incoming, incomingHandle FROM offline_old")
                    database.execSQL("DROP TABLE offline_old")
                } catch (e: Exception) {
                    Timber.e(e)
                    database.execSQL("DROP TABLE IF EXISTS offline")
                    database.execSQL("CREATE TABLE IF NOT EXISTS offline (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `handle` TEXT, `path` TEXT, `name` TEXT, `parentId` INTEGER, `type` TEXT, `incoming` INTEGER, `incomingHandle` TEXT, `lastModifiedTime` INTEGER)")
                }
            }
        }

        private val MIGRATION_77_78 = object : Migration(77, 78) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //Hotfix for lastModifiedTime missing column in offline table issue
                //Certain users where missing the lastModifiedTime column in the offline table because of a bug in the migration from 76 to 77
                //This migration will add the missing column for those users. And for existing users it will throw an error that can be ignored
                //Since there are changes in offline table in room, some fields are INTEGER in legacy database, but TEXT in room database
                //For that we added the alter table and related queries below
                try {
                    database.execSQL("ALTER TABLE offline ADD COLUMN lastModifiedTime INTEGER")
                    database.execSQL("ALTER TABLE offline RENAME TO offline_old")
                    database.execSQL("CREATE TABLE IF NOT EXISTS offline (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `handle` TEXT, `path` TEXT, `name` TEXT, `parentId` INTEGER, `type` TEXT, `incoming` INTEGER, `incomingHandle` TEXT, `lastModifiedTime` INTEGER)")
                    database.execSQL("INSERT INTO offline (id, handle, path, name, parentId, type, incoming, incomingHandle) SELECT id, handle, path, name, parentId, type, incoming, incomingHandle FROM offline_old")
                    database.execSQL("DROP TABLE offline_old")
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }

        val MIGRATIONS = arrayOf(
            MIGRATION_67_68,
            MIGRATION_68_69,
            MIGRATION_70_71,
            MIGRATION_71_72,
            MIGRATION_74_75,
            MIGRATION_75_76,
            MIGRATION_76_77,
            MIGRATION_77_78,
        )
    }
}
