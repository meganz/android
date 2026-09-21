package mega.privacy.android.data.database.spec

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * Auto migration spec from version 126 to 127.
 *
 * Marks the existing top level offline rows as offline roots. Which node the user actually chose
 * was never recorded, so the rows that are currently shown at the top of the Offline screen are
 * the closest available approximation and keep already downloaded content visible where users
 * expect it.
 */
internal class AutoMigrationSpec126to127 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "UPDATE `${MegaDatabaseConstant.TABLE_OFFLINE}` SET `isOfflineRoot` = 1 WHERE `parentId` = -1"
        )
    }
}
