package mega.privacy.android.data.database

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Legacy database migration
 *
 */
interface LegacyDatabaseMigration {

    /**
     * On upgrade
     *
     * @param db
     * @param oldVersion old version of database
     * @param newVersion new version of database
     */
    fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int)

    /**
     * On create
     *
     * @param db
     */
    fun onCreate(db: SupportSQLiteDatabase)
}