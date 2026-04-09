package mega.privacy.android.data.database.spec

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Auto migration spec from version 118 to 119.
 * Seeds the recently_used_type lookup table with the initial content types.
 */
internal class AutoMigrationSpec118to119 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT OR IGNORE INTO recently_used_type (type_id, name) VALUES (1, 'PDF')")
        db.execSQL("INSERT OR IGNORE INTO recently_used_type (type_id, name) VALUES (2, 'Video')")
        db.execSQL("INSERT OR IGNORE INTO recently_used_type (type_id, name) VALUES (3, 'Audio')")
        db.execSQL("INSERT OR IGNORE INTO recently_used_type (type_id, name) VALUES (4, 'TextEditor')")
        db.execSQL("INSERT OR IGNORE INTO recently_used_type (type_id, name) VALUES (5, 'FileLink')")
        db.execSQL("INSERT OR IGNORE INTO recently_used_type (type_id, name) VALUES (6, 'FolderLink')")
    }
}
