package mega.privacy.android.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import mega.privacy.android.data.database.dao.ActiveTransferDao
import mega.privacy.android.data.database.dao.CompletedTransferDao
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.data.database.entity.ContactEntity

@Database(
    entities = [
        ContactEntity::class,
        CompletedTransferEntity::class,
        ActiveTransferEntity::class,
    ],
    version = MegaDatabaseConstant.DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(69, 70)
    ],
)
internal abstract class MegaDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao

    abstract fun completedTransferDao(): CompletedTransferDao

    abstract fun activeTransfersDao(): ActiveTransferDao

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
        val MIGRATIONS = arrayOf(MIGRATION_67_68, MIGRATION_68_69)
    }
}
