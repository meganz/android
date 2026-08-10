package mega.privacy.android.data.database.spec

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * Auto migration spec from version 118 to 119.
 * Seeds the recently_used_type lookup table with the initial content types.
 */
internal class AutoMigrationSpec118to119 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        MegaDatabaseConstant.SEED_RECENTLY_USED_TYPE_SQL.forEach {
            db.execSQL(it)
        }
    }
}
