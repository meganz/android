package mega.privacy.android.data.database.spec

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Auto migration spec from version 118 to 119.
 * Seeds the recently_used_type lookup table with the initial content types.
 */
internal class AutoMigrationSpec118to119 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT OR IGNORE INTO recently_used_type (type_id, name) VALUES (1, 'pdf')")
        db.execSQL("INSERT OR IGNORE INTO recently_used_type (type_id, name) VALUES (2, 'video')")
        db.execSQL("INSERT OR IGNORE INTO recently_used_type (type_id, name) VALUES (3, 'audio')")
        db.execSQL("INSERT OR IGNORE INTO recently_used_type (type_id, name) VALUES (4, 'text_editor')")
    }
}
