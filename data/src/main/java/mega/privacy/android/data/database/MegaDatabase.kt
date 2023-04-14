package mega.privacy.android.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.database.entity.ContactEntity

@Database(
    entities = [ContactEntity::class],
    version = MegaDatabaseConstant.DATABASE_VERSION,
    exportSchema = true,
)
internal abstract class MegaDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao

    companion object {
        val MIGRATION_67_68 = object : Migration(67, 68) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }
    }
}